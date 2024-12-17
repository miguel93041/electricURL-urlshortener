@file:Suppress("WildcardImport", "UnusedPrivateProperty")

package es.unizar.urlshortener.infrastructure.delivery

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import es.unizar.urlshortener.core.*
import es.unizar.urlshortener.core.services.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DefaultDataBufferFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.BodyInserters
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.net.URI
import java.util.stream.Stream

@WebFluxTest(UrlShortenerControllerImpl::class)
@ContextConfiguration(
    classes = [
        UrlShortenerControllerImpl::class
    ]
)
class UrlShortenerControllerTest {
    @Autowired
    private lateinit var webTestClient: WebTestClient

    @MockBean
    private lateinit var csvService: CsvService

    @MockBean
    private lateinit var analyticsService: AnalyticsService

    @MockBean
    private lateinit var generateShortUrlServiceImpl: GenerateShortUrlService

    @MockBean
    private lateinit var redirectService: RedirectService

    @MockBean
    private lateinit var qrService: QrService

    @Test
    fun `redirectTo - redirects with 301`() {
        val shortId = "abc123"
        val redirection = Redirection(target = "http://example.com", mode = 301)
        `when`(redirectService.getRedirectionAndLogClick(eq(shortId), any())).thenReturn(
            Mono.just(Ok(redirection))
        )

        webTestClient.get().uri("/$shortId")
            .exchange()
            .expectStatus().isEqualTo(301)
            .expectHeader().valueEquals("Location", "http://example.com")
    }

    @Test
    fun `redirectTo - hash not found`() {
        val shortId = "notfound"
        `when`(redirectService.getRedirectionAndLogClick(eq(shortId), any())).thenReturn(
            Mono.just(Err(RedirectionError.NotFound))
        )

        webTestClient.get().uri("/$shortId")
            .exchange()
            .expectStatus().isNotFound
            .expectBody(String::class.java)
            .isEqualTo(HASH_DONT_EXIST)
    }

    @Test
    fun `redirectTo - bad hash format`() {
        val shortId = "a123"
        `when`(redirectService.getRedirectionAndLogClick(eq(shortId), any())).thenReturn(
            Mono.just(Err(RedirectionError.InvalidFormat))
        )

        webTestClient.get()
            .uri("/$shortId")
            .exchange()
            .expectStatus().isBadRequest
            .expectBody(String::class.java).isEqualTo(INVALID_HASH_FORMAT)
    }

    @Test
    fun `redirectTo - too many requests`() {
        val shortId = "abc123"
        `when`(redirectService.getRedirectionAndLogClick(eq(shortId), any())).thenReturn(
            Mono.just(Err(RedirectionError.TooManyRequests))
        )

        webTestClient.get()
            .uri("/$shortId")
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.TOO_MANY_REQUESTS)
            .expectBody(String::class.java).isEqualTo("This shortened hash is under load")
    }

    @Test
    fun `redirectTo - original url not validated`() {
        val shortId = "abc123"
        `when`(redirectService.getRedirectionAndLogClick(eq(shortId), any())).thenReturn(
            Mono.just(Err(RedirectionError.NotValidated))
        )

        webTestClient.get()
            .uri("/$shortId")
            .exchange()
            .expectStatus().isBadRequest
            .expectBody(String::class.java)
            .isEqualTo(HASH_VALIDATING)
    }

    @Test
    fun `redirectTo - original url unreachable`() {
        val shortId = "abc123"
        `when`(redirectService.getRedirectionAndLogClick(eq(shortId), any())).thenReturn(
            Mono.just(Err(RedirectionError.Unreachable))
        )

        webTestClient.get()
            .uri("/$shortId")
            .exchange()
            .expectStatus().isBadRequest
            .expectBody(String::class.java).isEqualTo(ORIGINAL_URL_UNREACHABLE)
    }

    @Test
    fun `redirectTo - original url unsafe`() {
        val shortId = "abc123"
        `when`(redirectService.getRedirectionAndLogClick(eq(shortId), any())).thenReturn(
            Mono.just(Err(RedirectionError.Unsafe))
        )

        webTestClient.get()
            .uri("/$shortId")
            .exchange()
            .expectStatus().isForbidden
            .expectBody(String::class.java).isEqualTo(ORIGINAL_URL_UNSAFE)
    }

    @Test
    fun `redirectTo - 500 code when exception occurs`() {
        val shortId = "abc123"
        `when`(redirectService.getRedirectionAndLogClick(eq(shortId), any()))
            .thenReturn(Mono.error(Exception("Error")))

        webTestClient.get()
            .uri("/$shortId")
            .exchange()
            .expectStatus().is5xxServerError
    }

    @Test
    fun `shortener - creates a shortUrl without QR`() {
        val inputData = ShortUrlDataIn(rawUrl = VALID_URL)
        createShortUrlWithoutQr("http://validurl.com", inputData)
    }

    @Test
    fun `shortener - creates a shortUrl without QR explicitly`() {
        val inputData = ShortUrlDataIn(rawUrl = VALID_URL, qrRequested = false)
        createShortUrlWithoutQr("http://validurl.com&qrRequested=false", inputData)
    }

    @Test
    fun `shortener - creates a shortUrl with QR url`() {
        val inputData = ShortUrlDataIn(rawUrl = VALID_URL, qrRequested = true)
        val outputData = ShortUrlDataOut(
            shortUrl = URI(LOCALHOST_URL), qrCodeUrl = URI("http://localhost/api/qr/abc123")
        )

        `when`(generateShortUrlServiceImpl.generate(eq(inputData), any())).thenReturn(Mono.just(outputData))

        webTestClient.post()
            .uri(LINK_ENDPOINT)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .bodyValue("rawUrl=http://validurl.com&qrRequested=true")
            .exchange()
            .expectStatus().isCreated
            .expectHeader().valueEquals(HttpHeaders.LOCATION, LOCALHOST_URL)
            .expectBody()
            .jsonPath(SHORTURL_PATH).isEqualTo(LOCALHOST_URL)
            .jsonPath(QRCODEURL_PATH).isEqualTo("http://localhost/api/qr/abc123")
    }

    @Test
    fun `shortener - 500 code when exception occurs`() {
        `when`(generateShortUrlServiceImpl.generate(any(), any())).thenReturn(Mono.error(Exception("Error")))

        webTestClient.post()
            .uri(LINK_ENDPOINT)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .bodyValue("rawUrl=http://validurl.com&qrRequested=true")
            .exchange()
            .expectStatus().is5xxServerError
            .expectHeader().doesNotExist(HttpHeaders.LOCATION)
            .expectBody()
            .jsonPath(SHORTURL_PATH).doesNotExist()
            .jsonPath(QRCODEURL_PATH).doesNotExist()
    }

    @Test
    fun `redirectToQrCode - returns png`() {
        val shortId = "abc123"
        val qrBytes = byteArrayOf(1, 2, 3, 4)

        `when`(qrService.getQrImage(eq(shortId), any())).thenReturn(Mono.just(Ok(qrBytes)))

        webTestClient.get().uri { uriBuilder ->
            uriBuilder.path(QR_ENDPOINT).build(shortId)
        }
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType("image/png")
            .expectBody()
            .consumeWith { response ->
                assert(response.responseBody!!.isNotEmpty())
            }
    }

    @Test
    fun `redirectToQrCode - hash not found`() {
        val shortId = "abc999"
        `when`(qrService.getQrImage(eq(shortId), any())).thenReturn(Mono.just(Err(HashError.NotFound)))

        webTestClient.get()
            .uri { it.path(QR_ENDPOINT).build(shortId) }
            .exchange()
            .expectStatus().isNotFound
            .expectBody(String::class.java).isEqualTo(HASH_DONT_EXIST)
    }

    @Test
    fun `redirectToQrCode - invalid format`() {
        val shortId = "abc999"
        `when`(qrService.getQrImage(eq(shortId), any())).thenReturn(Mono.just(Err(HashError.InvalidFormat)))

        webTestClient.get()
            .uri { it.path(QR_ENDPOINT).build(shortId) }
            .exchange()
            .expectStatus().isBadRequest
            .expectBody(String::class.java).isEqualTo(INVALID_HASH_FORMAT)
    }

    @Test
    fun `redirectToQrCode - original url not validated`() {
        val shortId = "abc999"
        `when`(qrService.getQrImage(eq(shortId), any())).thenReturn(Mono.just(Err(HashError.NotValidated)))

        webTestClient.get()
            .uri { it.path(QR_ENDPOINT).build(shortId) }
            .exchange()
            .expectStatus().isBadRequest
            .expectBody(String::class.java)
            .isEqualTo(HASH_VALIDATING)
    }

    @Test
    fun `redirectToQrCode - original url is not reachable`() {
        val shortId = "abc999"
        `when`(qrService.getQrImage(eq(shortId), any())).thenReturn(Mono.just(Err(HashError.Unreachable)))

        webTestClient.get()
            .uri { it.path(QR_ENDPOINT).build(shortId) }
            .exchange()
            .expectStatus().isBadRequest
            .expectBody(String::class.java).isEqualTo(ORIGINAL_URL_UNREACHABLE)
    }

    @Test
    fun `redirectToQrCode - original url is not safe`() {
        val shortId = "abc999"
        `when`(qrService.getQrImage(eq(shortId), any())).thenReturn(Mono.just(Err(HashError.Unsafe)))

        webTestClient.get()
            .uri { it.path(QR_ENDPOINT).build(shortId) }
            .exchange()
            .expectStatus().isForbidden
            .expectBody(String::class.java).isEqualTo(ORIGINAL_URL_UNSAFE)
    }

    @Test
    fun `redirectToQrCode - 500 code when exception occurs`() {
        val shortId = "abc999"
        `when`(qrService.getQrImage(eq(shortId), any())).thenReturn(Mono.error(Exception("Error")))

        webTestClient.get()
            .uri { it.path(QR_ENDPOINT).build(shortId) }
            .exchange()
            .expectStatus().is5xxServerError
    }

    @Test
    fun `shortenUrlsFromCsv - returns a CSV`() {
        val csvResult: Flux<DataBuffer> = Flux.just(
            DefaultDataBufferFactory().wrap("short_url,qr_url\nhttp://loc/abc,http://loc/qr/abc".toByteArray())
        )

        `when`(csvService.process(any(), any())).thenReturn(Mono.just(Ok(csvResult)))

        val inputData = "http://example.com\nhttp://another.com".toByteArray()
        val builder = MultipartBodyBuilder()
        builder.part("file", inputData).filename("input.csv")

        webTestClient.post()
            .uri("/api/upload-csv")
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .body(BodyInserters.fromMultipartData(builder.build()))
            .exchange()
            .expectStatus().isOk
            .expectHeader()
            .valueEquals(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=shortened_urls.csv")
            .expectBody(String::class.java).isEqualTo("short_url,qr_url\nhttp://loc/abc,http://loc/qr/abc")
    }

    @Test
    fun `shortenUrlsFromCsv - CSV inválido`() {
        `when`(csvService.process(any(), any())).thenReturn(Mono.just(Err(CsvError.InvalidFormat)))

        val inputData = "contenido_no_valido".toByteArray()
        val builder = MultipartBodyBuilder()
        builder.part("file", inputData).filename("invalid.csv")

        webTestClient.post()
            .uri("/api/upload-csv")
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .body(BodyInserters.fromMultipartData(builder.build()))
            .exchange()
            .expectStatus().isBadRequest
            .expectBody(String::class.java)
            .isEqualTo("Invalid CSV format")
    }

    @ParameterizedTest(name = "getAnalytics with browser={0}, country={1}, platform={2}")
    @MethodSource("booleanPermutations")
    fun `getAnalytics - all boolean permutations`(browser: Boolean, country: Boolean, platform: Boolean) {
        val shortId = "abc123"

        val byBrowserData = if (browser) mapOf("Chrome" to 5, "Firefox" to 5) else emptyMap()
        val byCountryData = if (country) mapOf("ES" to 10) else emptyMap()
        val byPlatformData = if (platform) mapOf("Windows" to 7, "Linux" to 3) else emptyMap()

        val analyticsData = AnalyticsData(
            totalClicks = 10,
            byBrowser = byBrowserData,
            byCountry = byCountryData,
            byPlatform = byPlatformData
        )

        `when`(analyticsService.get(eq(shortId), eq(browser), eq(country), eq(platform)))
            .thenReturn(Mono.just(Ok(analyticsData)))

        webTestClient.get()
            .uri { builder ->
                builder.path(ANALYTICS_ENDPOINT)
                    .queryParam("id", shortId)
                    .queryParam("browser", browser)
                    .queryParam("country", country)
                    .queryParam("platform", platform)
                    .build()
            }
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.totalClicks").isEqualTo(10)
            .apply {
                if (browser) {
                    jsonPath("$.byBrowser.Chrome").isEqualTo(5)
                    jsonPath("$.byBrowser.Firefox").isEqualTo(5)
                } else {
                    jsonPath("$.byBrowser").isEmpty()
                }

                if (country) {
                    jsonPath("$.byCountry.ES").isEqualTo(10)
                } else {
                    jsonPath("$.byCountry").isEmpty()
                }

                if (platform) {
                    jsonPath("$.byPlatform.Windows").isEqualTo(7)
                    jsonPath("$.byPlatform.Linux").isEqualTo(3)
                } else {
                    jsonPath("$.byPlatform").isEmpty()
                }
            }
    }

    @Test
    fun `getAnalytics - hash not found`() {
        val shortId = "abc999"
        `when`(analyticsService.get(shortId, false, false, false)).thenReturn(Mono.just(Err(HashError.NotFound)))

        webTestClient.get()
            .uri { it.path(ANALYTICS_ENDPOINT).queryParam("id", shortId).build() }
            .exchange()
            .expectStatus().isNotFound
            .expectBody(String::class.java).isEqualTo(HASH_DONT_EXIST)
    }

    @Test
    fun `getAnalytics - hash invalid format`() {
        val shortId = "abc999"
        `when`(analyticsService.get(shortId, false, false, false)).thenReturn(Mono.just(Err(HashError.InvalidFormat)))

        webTestClient.get()
            .uri { it.path(ANALYTICS_ENDPOINT).queryParam("id", shortId).build() }
            .exchange()
            .expectStatus().isBadRequest
            .expectBody(String::class.java).isEqualTo(INVALID_HASH_FORMAT)
    }

    @Test
    fun `getAnalytics - original url not validated`() {
        val shortId = "abc999"
        `when`(analyticsService.get(shortId, false, false, false)).thenReturn(Mono.just(Err(HashError.NotValidated)))

        webTestClient.get()
            .uri { it.path(ANALYTICS_ENDPOINT).queryParam("id", shortId).build() }
            .exchange()
            .expectStatus().isBadRequest
            .expectBody(String::class.java)
            .isEqualTo(HASH_VALIDATING)
    }

    @Test
    fun `getAnalytics - original url not reachable`() {
        val shortId = "abc999"
        `when`(analyticsService.get(shortId, false, false, false)).thenReturn(Mono.just(Err(HashError.Unreachable)))

        webTestClient.get()
            .uri { it.path(ANALYTICS_ENDPOINT).queryParam("id", shortId).build() }
            .exchange()
            .expectStatus().isBadRequest
            .expectBody(String::class.java).isEqualTo(ORIGINAL_URL_UNREACHABLE)
    }

    @Test
    fun `getAnalytics - original url not safe`() {
        val shortId = "abc999"
        `when`(analyticsService.get(shortId, false, false, false)).thenReturn(Mono.just(Err(HashError.Unsafe)))

        webTestClient.get()
            .uri { it.path(ANALYTICS_ENDPOINT).queryParam("id", shortId).build() }
            .exchange()
            .expectStatus().isForbidden
            .expectBody(String::class.java).isEqualTo(ORIGINAL_URL_UNSAFE)
    }

    private fun createShortUrlWithoutQr(rawUrl: String, inputData: ShortUrlDataIn) {
        val outputData = ShortUrlDataOut(shortUrl = URI(LOCALHOST_URL))

        `when`(generateShortUrlServiceImpl.generate(eq(inputData), any())).thenReturn(Mono.just(outputData))

        webTestClient.post()
            .uri(LINK_ENDPOINT)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .bodyValue("rawUrl=$rawUrl")
            .exchange()
            .expectStatus().isCreated
            .expectHeader().valueEquals(HttpHeaders.LOCATION, LOCALHOST_URL)
            .expectBody()
            .jsonPath(SHORTURL_PATH).isEqualTo(LOCALHOST_URL)
            .jsonPath(QRCODEURL_PATH).doesNotExist()
    }

    companion object {
        @JvmStatic
        fun booleanPermutations(): Stream<Arguments> = Stream.of(
            arguments(false, false, false),
            arguments(false, false, true),
            arguments(false, true, false),
            arguments(false, true, true),
            arguments(true, false, false),
            arguments(true, false, true),
            arguments(true, true, false),
            arguments(true, true, true)
        )

        const val LINK_ENDPOINT = "/api/link"
        const val QR_ENDPOINT = "/api/qr/{id}"
        const val ANALYTICS_ENDPOINT = "/api/analytics"
        const val INVALID_HASH_FORMAT = "Invalid shortened hash format"
        const val HASH_DONT_EXIST = "The given shortened hash does not exist"
        const val HASH_VALIDATING = "This shortened hash is still being validated. Wait a few seconds and try again"
        const val ORIGINAL_URL_UNREACHABLE = "The original url is unreachable"
        const val ORIGINAL_URL_UNSAFE = "The original url is unsafe"
        const val VALID_URL = "http://validurl.com"
        const val LOCALHOST_URL = "http://localhost/abc123"
        const val SHORTURL_PATH = "$.shortUrl"
        const val QRCODEURL_PATH = "$.qrCodeUrl"
    }
}
