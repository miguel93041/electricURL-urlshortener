@file:Suppress("MatchingDeclarationName", "WildcardImport", "LargeClass", "LongMethod", "LongParameterList")
package es.unizar.urlshortener

import es.unizar.urlshortener.core.ShortUrlDataOut
import es.unizar.urlshortener.core.usecases.UrlAccessibilityCheckUseCaseImpl
import es.unizar.urlshortener.infrastructure.repositories.ClickRepositoryServiceImpl
import es.unizar.urlshortener.infrastructure.repositories.ShortUrlEntity
import es.unizar.urlshortener.infrastructure.repositories.ShortUrlRepositoryServiceImpl
import es.unizar.urlshortener.thirdparties.ipinfo.GeoLocationServiceImpl
import es.unizar.urlshortener.thirdparties.ipinfo.UrlSafetyServiceImpl
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.io.ByteArrayResource
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate
import org.springframework.data.relational.core.query.Criteria
import org.springframework.data.relational.core.query.Query
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.reactive.function.BodyInserters

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class ReactiveIntegrationTest(
    @Autowired val webTestClient: WebTestClient,
    @Autowired val r2dbcEntityTemplate: R2dbcEntityTemplate,
    @Autowired var clickRepositoryService: ClickRepositoryServiceImpl,
    @Autowired var shortUrlRepositoryService: ShortUrlRepositoryServiceImpl,
    @Autowired var geoLocationService: GeoLocationServiceImpl,
    @Autowired var urlAccessibilityCheckUseCase: UrlAccessibilityCheckUseCaseImpl,
    @Autowired var safetyServiceImpl: UrlSafetyServiceImpl
) {

    @BeforeEach
    fun setup() {
        cleanDatabase()
        clearCache()
    }

    @AfterEach
    fun tearDown() {
        cleanDatabase()
        clearCache()
    }

    private fun cleanDatabase() {
        r2dbcEntityTemplate.databaseClient.sql("DELETE FROM click").fetch().rowsUpdated().block()
        r2dbcEntityTemplate.databaseClient.sql("DELETE FROM shorturl").fetch().rowsUpdated().block()
    }

    private fun clearCache() {
        clickRepositoryService.clearCache()
        shortUrlRepositoryService.clearCache()
        geoLocationService.clearCache()
        urlAccessibilityCheckUseCase.clearCache()
        safetyServiceImpl.clearCache()
    }

    private fun findShortUrlById(hash: String): ShortUrlEntity? {
        return r2dbcEntityTemplate.selectOne(
            Query.query(Criteria.where("hash").`is`(hash)),
            ShortUrlEntity::class.java
        ).block()
    }

    private fun countRowsInTable(tableName: String): Int {
        return r2dbcEntityTemplate.databaseClient
            .sql("SELECT COUNT(*) AS cnt FROM $tableName")
            .map { row -> row.get("cnt", java.lang.Long::class.java)!!.toInt() }
            .one()
            .block()!!
    }

    @Test
    fun `main page works`() {
        webTestClient.get()
            .uri("/")
            .exchange()
            .expectStatus().isOk
            .expectBody(String::class.java)
            .consumeWith { response ->
                assertThat(response.responseBody).contains("Shorten URLs or Upload a CSV File")
            }
    }


    @Test
    fun `main page includes jquery`() {
        webTestClient.get()
            .uri("/")
            .exchange()
            .expectStatus().isOk
            .expectBody(String::class.java)
            .consumeWith { response ->
                assertThat(response.responseBody).contains("webjars/jquery")
            }
    }

    @Test
    fun `main page includes bootstrap`() {
        webTestClient.get()
            .uri("/")
            .exchange()
            .expectStatus().isOk
            .expectBody(String::class.java)
            .consumeWith { response ->
                assertThat(response.responseBody).contains("webjars/bootstrap")
            }
    }

    @Test
    fun `redirectTo returns a redirect when the key exists`() {
        val target = shortUrl("http://example.com/")
        assertThat(target.statusCode.is2xxSuccessful).isTrue
        val redirectUri = target.body!!.shortUrl

        // Wait for URL validation
        Thread.sleep(1000)

        webTestClient.get()
            .uri(redirectUri)
            .exchange()
            .expectStatus().isEqualTo(301)
            .expectHeader().valueEquals(HttpHeaders.LOCATION, "http://example.com/")

        assertThat(countRowsInTable("click")).isEqualTo(1)
    }

    @Test
    fun `redirectTo returns a not found when the key does not exist`() {
        val response = webTestClient.get()
            .uri("/f684a3c4")
            .exchange()
            .expectStatus().isNotFound
            .expectBody(String::class.java)
            .returnResult()

        assertThat(response.responseBody).isEqualTo(
            "The given shortened hash does not exist"
        )
        assertThat(countRowsInTable("click")).isEqualTo(0)
    }

    @Test
    fun `redirectTo returns a bad request when the key does not follow a valid format`() {
        val response = webTestClient.get()
            .uri("/f684a3c41234567")
            .exchange()
            .expectStatus().isBadRequest
            .expectBody(String::class.java)
            .returnResult()

        assertThat(response.responseBody).isEqualTo(
            "Invalid shortened hash format"
        )
        assertThat(countRowsInTable("click")).isEqualTo(0)
    }

    @Test
    fun `creating a shortened URL and accessing it immediately returns 400 with validation in progress message`() {
        val target = shortUrl("http://example.com/")
        assertThat(target.statusCode.is2xxSuccessful).isTrue
        val redirectUri = target.body!!.shortUrl

        val immediateResponse = webTestClient.get()
            .uri(redirectUri)
            .exchange()
            .expectStatus().isBadRequest
            .expectBody(String::class.java)
            .returnResult()

        assertThat(immediateResponse.responseBody).isEqualTo(
            "This shortened hash is still being validated. Wait a few seconds and try again"
        )
        assertThat(countRowsInTable("click")).isEqualTo(0)
    }

    @Test
    fun `creating a shortened URL based on an unreachable URL and accessing it returns 400 with unreachable message`() {
        val target = shortUrl("http://mgerkgerkfgjfsdgjdk.com/")
        assertThat(target.statusCode.is2xxSuccessful).isTrue
        val redirectUri = target.body!!.shortUrl

        // Wait for URL validation
        Thread.sleep(1000)

        val response = webTestClient.get()
            .uri(redirectUri)
            .exchange()
            .expectStatus().isBadRequest
            .expectBody(String::class.java)
            .returnResult()

        assertThat(response.responseBody).isEqualTo(
            "The original url is unreachable"
        )
        assertThat(countRowsInTable("click")).isEqualTo(0)
    }

    @Test
    fun `creating a shortened URL with more than 2083 characters returns 500`() {
        val longUrl = "http://example.com/" + "a".repeat(2084)

        val data: MultiValueMap<String, String> = LinkedMultiValueMap()
        data["rawUrl"] = longUrl
        data["qrRequested"] = false.toString()

        webTestClient.post()
            .uri("/api/link")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .bodyValue(data)
            .exchange()
            .expectStatus().is5xxServerError
            .expectBody(String::class.java)
            .returnResult()

        assertThat(countRowsInTable("shorturl")).isEqualTo(0)
        assertThat(countRowsInTable("click")).isEqualTo(0)
    }

    @Test
    fun `exceeding redirection limit returns 429 with hold up message`() {
        val target = shortUrl("http://example.com/")
        assertThat(target.statusCode.is2xxSuccessful).isTrue
        val redirectUri = target.body!!.shortUrl

        // Wait for URL validation
        Thread.sleep(1000)

        for (i in 0..10) {
            webTestClient.get()
                .uri(redirectUri)
                .exchange()
                .expectStatus().isEqualTo(301)
                .expectHeader().valueEquals(HttpHeaders.LOCATION, "http://example.com/")

            assertThat(countRowsInTable("click")).isEqualTo(i+1)
        }

        val response = webTestClient.get()
            .uri(redirectUri)
            .exchange()
            .expectStatus().isEqualTo(429)
            .expectBody(String::class.java)
            .returnResult()

        assertThat(response.responseBody).isEqualTo(
            "This shortened hash is under load"
        )
        assertThat(countRowsInTable("click")).isEqualTo(11)
    }

    @Test
    fun `creating a shortened URL based on an unsafe URL and accessing it returns 403 with unsafe message`() {
        val target = shortUrl("https://malware.wicar.org/")
        assertThat(target.statusCode.is2xxSuccessful).isTrue
        val redirectUri = target.body!!.shortUrl

        // Wait for URL validation
        Thread.sleep(1000)

        val response = webTestClient.get()
            .uri(redirectUri)
            .exchange()
            .expectStatus().isForbidden
            .expectBody(String::class.java)
            .returnResult()

        assertThat(response.responseBody).isEqualTo(
            "The original url is unsafe"
        )
        assertThat(countRowsInTable("click")).isEqualTo(0)
    }

    @Test
    fun `creates returns a basic redirect without QR if it can compute a hash`() {
        val response = shortUrl("http://example.com/")

        assertThat(response.statusCode.value()).isEqualTo(201)
        assertThat(response.headers.location).isNotNull
        assertThat(response.body?.shortUrl).isNotNull
        assertThat(response.body?.qrCodeUrl).isNull()

        val regexPattern = Regex(".*/[a-zA-Z0-9]{8}$").toPattern()
        assertThat(response.body!!.shortUrl.toString()).matches(regexPattern)

        assertThat(countRowsInTable("shorturl")).isEqualTo(1)
        assertThat(countRowsInTable("click")).isEqualTo(0)
    }

    @Test
    fun `creates registers client ip and country`() {
        val data: MultiValueMap<String, String> = LinkedMultiValueMap()
        data["rawUrl"] = "http://example.com"
        data["qrRequested"] = false.toString()

        val response = webTestClient.post()
            .uri("/api/link")
            .header("X-Forwarded-For", "8.8.8.8")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .bodyValue(data)
            .exchange()
            .expectBody(ShortUrlDataOut::class.java)
            .returnResult()
        val hash = response.responseBody!!.shortUrl.path.split("/").last()

        assertThat(response.status.value()).isEqualTo(201)
        assertThat(response.responseHeaders.location).isNotNull
        assertThat(response.responseBody?.shortUrl).isNotNull
        assertThat(response.responseBody?.qrCodeUrl).isNull()

        val regexPattern = Regex(".*/[a-zA-Z0-9]{8}$").toPattern()
        assertThat(response.responseBody!!.shortUrl.toString()).matches(regexPattern)

        assertThat(countRowsInTable("shorturl")).isEqualTo(1)
        assertThat(countRowsInTable("click")).isEqualTo(0)

        // Wait for URL validation
        Thread.sleep(1000)

        val shortUrlEntity = findShortUrlById(hash)
        assertThat(shortUrlEntity).isNotNull
        assertThat(shortUrlEntity!!.ip).isEqualTo("8.8.8.8")
        assertThat(shortUrlEntity.country).isEqualTo("US")
    }

    @Test
    fun `creates returns a basic redirect with QR requested if it can compute a hash`() {
        val response = shortUrl("http://example.com/", true)

        assertThat(response.statusCode.value()).isEqualTo(201)
        assertThat(response.headers.location).isNotNull
        assertThat(response.body?.shortUrl).isNotNull

        val shortUrlRegexPattern = Regex(".*/[a-zA-Z0-9]{8}$").toPattern()
        assertThat(response.body!!.shortUrl.toString()).matches(shortUrlRegexPattern)

        val qrCodeUrlRegexPattern = Regex(".*/api/qr\\?id=[a-zA-Z0-9]{8}$").toPattern()
        assertThat(response.body!!.qrCodeUrl.toString()).matches(qrCodeUrlRegexPattern)

        assertThat(countRowsInTable("shorturl")).isEqualTo(1)
        assertThat(countRowsInTable("click")).isEqualTo(0)
    }

    @Test
    fun `get analytics - returns empty aggregated data`() {
        val response = shortUrl("http://example.com/")
        val hash = response.body!!.shortUrl.path.split("/").last()

        // Wait for URL validation
        Thread.sleep(1000)

        webTestClient.get()
            .uri { it.path("/api/analytics").queryParam("id", hash).build() }
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.totalClicks").isEqualTo(0)
    }

    @Test
    fun `get analytics - returns correct data for all parameter permutations with user-agent simulation`() {
        val response = shortUrl("http://example.com/")
        val hash = response.body!!.shortUrl.path.split("/").last()

        // Wait for URL validation
        Thread.sleep(1000)

        webTestClient.get()
            .uri(response.body!!.shortUrl)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Gecko/20100101 Firefox/94.0")
            .exchange()
            .expectStatus().isEqualTo(301)

        // Wait for Click processing
        Thread.sleep(1000)

        val parameters = listOf(
            "browser" to listOf(true, false),
            "country" to listOf(true, false),
            "platform" to listOf(true, false)
        )

        // Generate all permutations of parameters
        val allCombinations = parameters.flatMap { (key, values) ->
            values.map { key to it }
        }.groupBy({ it.first }, { it.second })
            .let { map ->
                sequence {
                    val keys = map.keys.toList()
                    val values = keys.map { map[it]!! }
                    for (combination in cartesianProduct(values)) {
                        yield(keys.zip(combination).toMap())
                    }
                }
            }

        // Test each permutation
        allCombinations.forEach { paramMap ->
            webTestClient.get()
                .uri {
                    val builder = it.path("/api/analytics").queryParam("id", hash)
                    paramMap.forEach { (key, value) ->
                        builder.queryParam(key, value.toString())
                    }
                    builder.build()
                }
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Gecko/20100101 Firefox/94.0")
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.totalClicks").isEqualTo(1) // Verifica totalClicks
                .apply {
                    if (paramMap["browser"] == true) {
                        jsonPath("$.byBrowser.Firefox").isEqualTo(1)
                    } else {
                        jsonPath("$.byBrowser").doesNotExist()
                    }

                    if (paramMap["country"] == true) {
                        jsonPath("$.byCountry.Unknown").isEqualTo(1)
                    } else {
                        jsonPath("$.byCountry").doesNotExist()
                    }

                    if (paramMap["platform"] == true) {
                        jsonPath("$.byPlatform.Windows").isEqualTo(1)
                    } else {
                        jsonPath("$.byPlatform").doesNotExist()
                    }
                }
        }
    }

    // Helper to generate cartesian product
    private fun <T> cartesianProduct(lists: List<List<T>>): List<List<T>> {
        if (lists.isEmpty()) return listOf(emptyList())
        val first = lists[0]
        val rest = cartesianProduct(lists.drop(1))
        return first.flatMap { value -> rest.map { listOf(value) + it } }
    }

    @Test
    fun `get analytics - returns correct country for simulated Google IP`() {
        val response = shortUrl("http://example.com/")
        val hash = response.body!!.shortUrl.path.split("/").last()

        // Wait for URL validation
        Thread.sleep(1000)

        webTestClient.get()
            .uri(response.body!!.shortUrl)
            .header("X-Forwarded-For", "8.8.8.8")
            .exchange()
            .expectStatus().isEqualTo(301)

        // Wait for Click processing
        Thread.sleep(1000)

        webTestClient.get()
            .uri { it.path("/api/analytics").queryParam("id", hash).queryParam("country", "true").build() }
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.byCountry").exists()
            .jsonPath("$.byCountry.US").isEqualTo(1)
    }

    @Test
    fun `get analytics - returns 404 for non-existent ID`() {
        webTestClient.get()
            .uri { it.path("/api/analytics").queryParam("id", "fa123456").build() }
            .exchange()
            .expectStatus().isNotFound
            .expectBody(String::class.java)
            .consumeWith { response ->
                assertThat(response.responseBody).isEqualTo(
                    "The given shortened hash does not exist"
                )
            }
    }

    @Test
    fun `get analytics - returns 400 for invalid ID format`() {
        webTestClient.get()
            .uri { it.path("/api/analytics").queryParam("id", "invalid#id123").build() }
            .exchange()
            .expectStatus().isBadRequest
            .expectBody(String::class.java)
            .consumeWith { response ->
                assertThat(response.responseBody).isEqualTo(
                    "Invalid shortened hash format"
                )
            }
    }

    @Test
    fun `get analytics - returns 403 for unsafe URL`() {
        val response = shortUrl("https://malware.wicar.org/")
        val hash = response.body!!.shortUrl.path.split("/").last()

        // Wait for URL validation
        Thread.sleep(1000)

        webTestClient.get()
            .uri { it.path("/api/analytics").queryParam("id", hash).build() }
            .exchange()
            .expectStatus().isForbidden
            .expectBody(String::class.java)
            .consumeWith { result ->
                assertThat(result.responseBody).isEqualTo(
                    "The original url is unsafe"
                )
            }
    }

    @Test
    fun `get analytics - returns 400 for unreachable URL`() {
        val response = shortUrl("http://adsfadfafxchtrrtdfs.com/")
        val hash = response.body!!.shortUrl.path.split("/").last()

        Thread.sleep(1000)

        webTestClient.get()
            .uri { it.path("/api/analytics").queryParam("id", hash).build() }
            .exchange()
            .expectStatus().isBadRequest
            .expectBody(String::class.java)
            .consumeWith { result ->
                assertThat(result.responseBody).isEqualTo(
                    "The original url is unreachable"
                )
            }
    }

    @Test
    fun `get analytics - returns 400 when the URL is still being validated`() {
        val response = shortUrl("http://example.com/")
        val hash = response.body!!.shortUrl.path.split("/").last()

        webTestClient.get()
            .uri { it.path("/api/analytics").queryParam("id", hash).build() }
            .exchange()
            .expectStatus().isBadRequest
            .expectBody(String::class.java)
            .consumeWith { result ->
                assertThat(result.responseBody).isEqualTo(
                    "This shortened hash is still being validated. Wait a few seconds and try again"
                )
            }
    }

    @Test
    fun `qr page - returns 404 when the id does not exist`() {
        webTestClient.get()
            .uri("/api/qr?id=fa123456")
            .exchange()
            .expectStatus().isNotFound
            .expectBody(String::class.java)
            .consumeWith { response ->
                assertThat(response.responseBody).isEqualTo("The given shortened hash does not exist")
            }
    }

    @Test
    fun `qr page - returns 400 when the id format is invalid`() {
        webTestClient.get()
            .uri("/api/qr?id=fa123456789")
            .exchange()
            .expectStatus().isBadRequest
            .expectBody(String::class.java)
            .consumeWith { response ->
                assertThat(response.responseBody).isEqualTo("Invalid shortened hash format")
            }
    }

    @Test
    fun `qr page - returns 403 when the URL is unsafe`() {
        // Simulate creating a short URL pointing to an unsafe link
        val response = shortUrl("https://malware.wicar.org/")
        val hash = response.body!!.shortUrl.path.split("/").last()

        // Wait for validation
        Thread.sleep(1000)

        webTestClient.get()
            .uri("/api/qr?id=$hash")
            .exchange()
            .expectStatus().isForbidden
            .expectBody(String::class.java)
            .consumeWith { result ->
                assertThat(result.responseBody).isEqualTo("The original url is unsafe")
            }
    }

    @Test
    fun `qr page - returns 400 when the URL is unreachable`() {
        val response = shortUrl("http://asdfasdfasdfasdgerthrt/")
        val hash = response.body!!.shortUrl.path.split("/").last()

        // Wait for validation
        Thread.sleep(1000)

        webTestClient.get()
            .uri("/api/qr?id=$hash")
            .exchange()
            .expectStatus().isBadRequest
            .expectBody(String::class.java)
            .consumeWith { result ->
                assertThat(result.responseBody).isEqualTo("The original url is unreachable")
            }
    }

    @Test
    fun `qr page - returns 400 when the URL is still being validated`() {
        val response = shortUrl("http://example.com/")
        val hash = response.body!!.shortUrl.path.split("/").last()

        webTestClient.get()
            .uri("/api/qr?id=$hash")
            .exchange()
            .expectStatus().isBadRequest
            .expectBody(String::class.java)
            .consumeWith { result ->
                assertThat(result.responseBody)
                    .isEqualTo("This shortened hash is still being validated. Wait a few seconds and try again")
            }
    }

    @Test
    fun `qr page - returns PNG image for valid short URL`() {
        val response = shortUrl("http://example.com/", true)
        val apiUrl = response.body!!.qrCodeUrl.toString()

        // Wait for URL validation
        Thread.sleep(1000)

        webTestClient.get()
            .uri(apiUrl)
            .exchange()
            .expectStatus().isOk // Expect HTTP 200
            .expectHeader().contentType(MediaType.IMAGE_PNG) // Expect the content type to be image/png
            .expectBody() // Validate the response body
            .consumeWith { result ->
                val body = result.responseBody
                assertThat(body).isNotNull
                // Additional validation: check if the body starts with PNG signature bytes
                val pngSignature = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
                assertThat(body!!.take(pngSignature.size).toByteArray()).isEqualTo(pngSignature)
            }
    }

    @Test
    fun `upload valid CSV with QR requested and validate hash and QR code URL`() {
        val csvContent = """
        http://example1.com
        http://example2.com
    """.trimIndent()

        val multipartData = MultipartBodyBuilder().apply {
            part("file", ByteArrayResource(csvContent.toByteArray()), MediaType.TEXT_PLAIN)
                .header("Content-Disposition", "form-data; name=file; filename=urls.csv")
            part("qrRequested", "true")
        }.build()

        webTestClient.post()
            .uri("/api/upload-csv")
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .body(BodyInserters.fromMultipartData(multipartData))
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType("text/csv")
            .expectBody(String::class.java)
            .consumeWith { result ->
                val response = result.responseBody!!
                println(response)

                val lines = response.lines()
                assert(lines.size == 3) // 2 lines of data + 1 empty line at the end

                // Validate line 0
                val line0Parts = lines[0].split(",")
                assert(line0Parts[0] == "http://example1.com")
                assert(line0Parts[1].matches(Regex(".*/[a-zA-Z0-9]{8}"))) // Hash validation
                assert(line0Parts[2].matches(Regex(".*/api/qr\\?id=[a-zA-Z0-9]{8}"))) // QR code URL validation

                // Validate line 1
                val line1Parts = lines[1].split(",")
                assert(line1Parts[0] == "http://example2.com")
                assert(line1Parts[1].matches(Regex(".*/[a-zA-Z0-9]{8}"))) // Hash validation
                assert(line1Parts[2].matches(Regex(".*/api/qr\\?id=[a-zA-Z0-9]{8}"))) // QR code URL validation

                // Validate the last line is empty
                assert(lines[2].isEmpty())
            }
    }


    @Test
    fun `upload invalid non-CSV file and receive error`() {
        val invalidContent = "This is not a CSV file."

        val multipartData = MultipartBodyBuilder().apply {
            part("file", ByteArrayResource(invalidContent.toByteArray()), MediaType.TEXT_PLAIN)
                .header("Content-Disposition", "form-data; name=file; filename=not_a_csv.txt")
        }.build()

        webTestClient.post()
            .uri("/api/upload-csv")
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .body(BodyInserters.fromMultipartData(multipartData))
            .exchange()
            .expectStatus().isBadRequest
            .expectBody(String::class.java)
            .consumeWith { result ->
                assert(result.responseBody == "Invalid CSV format")
            }
    }

    @Test
    fun `upload empty CSV and receive error`() {
        val emptyCsvContent = ""

        val multipartData = MultipartBodyBuilder().apply {
            part("file", ByteArrayResource(emptyCsvContent.toByteArray()), MediaType.TEXT_PLAIN)
                .header("Content-Disposition", "form-data; name=file; filename=empty.csv")
        }.build()

        webTestClient.post()
            .uri("/api/upload-csv")
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .body(BodyInserters.fromMultipartData(multipartData))
            .exchange()
            .expectStatus().isBadRequest
            .expectBody(String::class.java)
            .consumeWith { result ->
                assert(result.responseBody == "Invalid CSV format")
            }
    }

    private fun shortUrl(url: String, qrRequested: Boolean = false): ResponseEntity<ShortUrlDataOut> {
        val data: MultiValueMap<String, String> = LinkedMultiValueMap()
        data["rawUrl"] = url
        data["qrRequested"] = qrRequested.toString()

        val result = webTestClient.post()
            .uri("/api/link")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .bodyValue(data)
            .exchange()
            .expectBody(ShortUrlDataOut::class.java)
            .returnResult()

        return ResponseEntity(
            result.responseBody,
            result.responseHeaders,
            result.status
        )
    }
}
