@file:Suppress("WildcardImport")

package es.unizar.urlshortener.infrastructure.delivery

import com.github.michaelbull.result.fold
import es.unizar.urlshortener.core.*
import es.unizar.urlshortener.core.services.*
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DefaultDataBufferFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.net.URI

/**
 * The specification of the controller.
 */
interface UrlShortenerController {

    /**
     * Redirects and logs a short url identified by its [id].
     *
     * @param id Identifier of the short URL.
     * @param request The HTTP request containing client details.
     * @return A [ResponseEntity] with redirection information.
     */
    fun redirectTo(id: String, request: ServerHttpRequest): Mono<ResponseEntity<Any>>

    /**
     * Creates a short url from details provided in [data].
     *
     * @param data Input data containing the original URL and optional metadata.
     * @param request The HTTP request for capturing client context.
     * @return A [ResponseEntity] with the details of the created short URL.
     */
    fun shortener(data: ShortUrlDataIn, request: ServerHttpRequest): Mono<ResponseEntity<ShortUrlDataOut>>

    /**
     * Generates a QR code for the given short URL.
     *
     * @param id Identifier of the short URL.
     * @param request The HTTP request.
     * @return A [ResponseEntity] with the QR code image as a PNG image in a byte array format.
     */
    fun redirectToQrCode(id: String, request: ServerHttpRequest): Mono<ResponseEntity<Any>>

    /**
     * Processes a CSV file containing URLs and generates a CSV with shortened URLs and its QR code URLs if requested.
     *
     * @param file The uploaded CSV file containing URLs.
     * @param request The HTTP request.
     * @return A [ResponseEntity] with the processed CSV file as a downloadable response.
     */
    fun shortenUrlsFromCsv(data: CsvDataIn, request: ServerHttpRequest): Mono<ResponseEntity<Flux<DataBuffer>>>

    /**
     * Endpoint to retrieve aggregated analytics data for a shortened URL.
     *
     * This method provides a way to fetch total clicks and optionally include breakdowns by browser,
     * country, platform based on the provided query parameters.
     *
     * @param id The hash of the shortened URL for which analytics data is requested. (Required)
     * @param browser Indicates whether to include a breakdown of clicks by browser. Defaults to false. (Optional)
     * @param country Indicates whether to include a breakdown of clicks by country. Defaults to false. (Optional)
     * @param platform Indicates whether to include a breakdown of clicks by platform. Defaults to false. (Optional)
     * @return A ResponseEntity containing the analytics data. Returns 200 OK with the data on success,
     *         or 404 NOT FOUND if hash is invalid.
     */
    fun getAnalytics(id: String, browser: Boolean, country: Boolean, platform: Boolean): Mono<ResponseEntity<Any>>
}

/**
 * The implementation of the controller.
 */
@Suppress("LongParameterList")
@RestController
class UrlShortenerControllerImpl(
    val csvService: CsvService,
    val analyticsService: AnalyticsService,
    val generateShortUrlServiceImpl: GenerateShortUrlService,
    val redirectService: RedirectService,
    val qrService: QrService,
) : UrlShortenerController {

    /**
     * Redirects and logs a short url identified by its [id].
     *
     * @param id the identifier of the short url
     * @param request the HTTP request
     * @return a ResponseEntity with the redirection details
     */
    @GetMapping("/{id:(?!api|index|favicon\\.ico).*}")
    override fun redirectTo(@PathVariable id: String, request: ServerHttpRequest): Mono<ResponseEntity<Any>> {
        return redirectService.getRedirectionAndLogClick(id, request)
            .map { result ->
                result.fold(
                    success = { redirection ->
                        val headers = HttpHeaders()
                        headers.location = URI.create(redirection.target)
                        ResponseEntity<Any>(null, headers, HttpStatus.valueOf(redirection.mode))
                    },
                    failure = { error ->
                        val (status, message) = when (error) {
                            RedirectionError.InvalidFormat -> HttpStatus.BAD_REQUEST to "Invalid shortened hash format"
                            RedirectionError.NotFound ->
                                HttpStatus.NOT_FOUND to "The given shortened hash does not exist"
                            RedirectionError.TooManyRequests ->
                                HttpStatus.TOO_MANY_REQUESTS to "This shortened hash is under load"
                            RedirectionError.NotValidated ->
                                HttpStatus.BAD_REQUEST to
                                        "This shortened hash is still being validated. Wait a few seconds and try again"
                            RedirectionError.Unreachable -> HttpStatus.BAD_REQUEST to "The original url is unreachable"
                            RedirectionError.Unsafe -> HttpStatus.FORBIDDEN to "The original url is unsafe"
                        }
                        ResponseEntity<Any>(message, null, status)
                    }
                )
            }
    }

    /**
     * Generates a QR code for the given short URL.
     *
     * @param id Identifier of the short URL.
     * @param request The HTTP request.
     * @return A [ResponseEntity] with the QR code image as a PNG image in a byte array format.
     */
    @GetMapping("/api/qr", produces = [MediaType.IMAGE_PNG_VALUE])
    override fun redirectToQrCode(@RequestParam id: String, request: ServerHttpRequest): Mono<ResponseEntity<Any>> {
        return qrService.getQrImage(id, request)
            .map { result ->
                result.fold(
                    success = { qr ->
                        ResponseEntity.ok()
                            .contentType(MediaType.IMAGE_PNG)
                            .body(qr)
                    },
                    failure = { error ->
                        val (status, message) = when (error) {
                            HashError.InvalidFormat -> HttpStatus.BAD_REQUEST to "Invalid shortened hash format"
                            HashError.NotFound -> HttpStatus.NOT_FOUND to "The given shortened hash does not exist"
                            HashError.NotValidated ->
                                HttpStatus.BAD_REQUEST to
                                        "This shortened hash is still being validated. Wait a few seconds and try again"
                            HashError.Unreachable -> HttpStatus.BAD_REQUEST to "The original url is unreachable"
                            HashError.Unsafe -> HttpStatus.FORBIDDEN to "The original url is unsafe"
                        }
                        ResponseEntity.status(status).body(message)
                    }
                )
            }
    }


    /**
     * Processes a CSV file containing URLs and generates a CSV with shortened URLs and its QR code URLs if requested.
     *
     * @param file The uploaded CSV file containing URLs.
     * @param request The HTTP request.
     * @return A [ResponseEntity] with the processed CSV file as a downloadable response.
     */
    @PostMapping("/api/upload-csv", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    override fun shortenUrlsFromCsv(
        data: CsvDataIn,
        request: ServerHttpRequest
    ): Mono<ResponseEntity<Flux<DataBuffer>>> {
        return csvService.process(data, request)
            .map { result ->
                result.fold(
                    success = { csvFlux ->
                        val headers = HttpHeaders().apply {
                            add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=shortened_urls.csv")
                            contentType = MediaType.parseMediaType("text/csv")
                        }
                        ResponseEntity.ok()
                            .headers(headers)
                            .body(csvFlux)
                    },
                    failure = { error ->
                        val (status, message) = when (error) {
                            CsvError.InvalidFormat -> HttpStatus.BAD_REQUEST to "Invalid CSV format"
                        }
                        ResponseEntity
                            .status(status)
                            .body(Flux.just(DefaultDataBufferFactory().wrap(message.toByteArray())))
                    }
                )
            }
    }

    /**
     * Creates a short url from details provided in [data].
     *
     * @param data the data required to create a short url
     * @param request the HTTP request
     * @return a Mono<ResponseEntity> with the created short url details or an error
     */
    @PostMapping("/api/link", consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE])
    override fun shortener(data: ShortUrlDataIn, request: ServerHttpRequest): Mono<ResponseEntity<ShortUrlDataOut>> {
        return generateShortUrlServiceImpl.generate(data, request)
            .map { result ->
                val headers = HttpHeaders()
                headers.location = result.shortUrl
                ResponseEntity(result, headers, HttpStatus.CREATED)
            }
    }

    /**
     * Endpoint to retrieve aggregated analytics data for a shortened URL.
     *
     * This method provides a way to fetch total clicks and optionally include breakdowns by browser,
     * country, platform based on the provided query parameters.
     *
     * @param id The hash of the shortened URL for which analytics data is requested. (Required)
     * @param browser Indicates whether to include a breakdown of clicks by browser. Defaults to false. (Optional)
     * @param country Indicates whether to include a breakdown of clicks by country. Defaults to false. (Optional)
     * @param platform Indicates whether to include a breakdown of clicks by platform. Defaults to false. (Optional)
     * @return A ResponseEntity containing the analytics data. Returns 200 OK with the data on success,
     *         or 404 NOT FOUND if hash is invalid.
     */
    @GetMapping("/api/analytics", produces = [MediaType.APPLICATION_JSON_VALUE])
    override fun getAnalytics(
        @RequestParam id: String,
        @RequestParam(required = false, defaultValue = "false") browser: Boolean,
        @RequestParam(required = false, defaultValue = "false") country: Boolean,
        @RequestParam(required = false, defaultValue = "false") platform: Boolean
    ): Mono<ResponseEntity<Any>> {
        return analyticsService.get(id, browser, country, platform)
            .map { result ->
                result.fold(
                    success = { analytics ->
                        ResponseEntity.ok(analytics)
                    },
                    failure = { error ->
                        val (status, message) = when (error) {
                            HashError.InvalidFormat -> HttpStatus.BAD_REQUEST to "Invalid shortened hash format"
                            HashError.NotFound -> HttpStatus.NOT_FOUND to "The given shortened hash does not exist"
                            HashError.NotValidated ->
                                HttpStatus.BAD_REQUEST to
                                        "This shortened hash is still being validated. Wait a few seconds and try again"
                            HashError.Unreachable -> HttpStatus.BAD_REQUEST to "The original url is unreachable"
                            HashError.Unsafe -> HttpStatus.FORBIDDEN to "The original url is unsafe"
                        }
                        ResponseEntity.status(status).body(message)
                    }
                )
            }
    }
}
