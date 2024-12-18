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
     * @return A [Mono] emitting a [ResponseEntity] with redirection information.
     */
    fun redirectTo(id: String, request: ServerHttpRequest): Mono<ResponseEntity<Any>>

    /**
     * Creates a short url from details provided in [data].
     *
     * @param data Input data containing the original URL and optional metadata.
     * @param request The HTTP request for capturing client context.
     * @return A [Mono] emitting a [ResponseEntity] with the details of the created short URL.
     */
    fun shortener(data: ShortUrlDataIn, request: ServerHttpRequest): Mono<ResponseEntity<ShortUrlDataOut>>

    /**
     * Generates a QR code for the given short URL.
     *
     * @param id Identifier of the short URL.
     * @param request The HTTP request.
     * @return A [Mono] emitting a [ResponseEntity] with the QR code image as a PNG image in a byte array format.
     */
    fun redirectToQrCode(id: String, request: ServerHttpRequest): Mono<ResponseEntity<Any>>

    /**
     * Processes a CSV file containing URLs and generates a CSV with shortened URLs and its QR code URLs if requested.
     *
     * @param data The uploaded CSV file containing URLs.
     * @param request The HTTP request.
     * @return A [Mono] emitting a [ResponseEntity] with the processed CSV file as a downloadable response.
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
     * @return A [Mono] emitting a [ResponseEntity] containing the analytics data. Returns 200 OK with the data
     *         on success, 404 NOT FOUND if the hash is invalid, or other appropriate error codes such as
     *         400 BAD REQUEST or 403 FORBIDDEN for validation or security-related issues.
     */
    fun getAnalytics(id: String, browser: Boolean, country: Boolean, platform: Boolean): Mono<ResponseEntity<Any>>
}

/**
 * [UrlShortenerControllerImpl] is an implementation of the [UrlShortenerController] interface.
 * This controller handles URL shortening, redirection, QR code generation, CSV processing, and analytics.
 *
 * @param csvService The service responsible for processing CSV files with URLs.
 * @param analyticsService The service for handling analytics data related to shortened URLs.
 * @param generateShortUrlServiceImpl The service for generating shortened URLs.
 * @param redirectService The service managing URL redirection and click logging.
 * @param qrService The service for generating QR codes associated with shortened URLs.
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
     * @return A [Mono] emitting a [ResponseEntity] with redirection information.
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
                        val (status, message, headers) = when (error) {
                            RedirectionError.InvalidFormat -> Triple(HttpStatus.BAD_REQUEST, INVALID_HASH_FORMAT, null)
                            RedirectionError.NotFound -> Triple(HttpStatus.NOT_FOUND, HASH_DONT_EXIST, null)
                            RedirectionError.TooManyRequests -> Triple(
                                HttpStatus.TOO_MANY_REQUESTS,
                                "This shortened hash is under load",
                                null
                            )
                            RedirectionError.NotValidated -> {
                                val retryAfterSeconds = 10
                                val headers = HttpHeaders()
                                headers.add(HttpHeaders.RETRY_AFTER, retryAfterSeconds.toString())
                                Triple(HttpStatus.BAD_REQUEST, HASH_VALIDATING, headers)
                            }
                            RedirectionError.Unreachable -> Triple(
                                HttpStatus.BAD_REQUEST,
                                ORIGINAL_URL_UNREACHABLE,
                                null
                            )
                            RedirectionError.Unsafe -> Triple(HttpStatus.FORBIDDEN, ORIGINAL_URL_UNSAFE, null)
                        }
                        ResponseEntity<Any>(message, headers, status)
                    }
                )
            }
    }

    /**
     * Generates a QR code for the given short URL.
     *
     * @param id Identifier of the short URL.
     * @param request The HTTP request.
     * @return A [Mono] emitting a [ResponseEntity] with the QR code image as a PNG image in a byte array format.
     */
    @GetMapping("/api/qr/{id}", produces = [MediaType.IMAGE_PNG_VALUE])
    override fun redirectToQrCode(@PathVariable id: String, request: ServerHttpRequest): Mono<ResponseEntity<Any>> {
        return qrService.getQrImage(id, request)
            .map { result ->
                result.fold(
                    success = { qr ->
                        ResponseEntity.ok()
                            .contentType(MediaType.IMAGE_PNG)
                            .body(qr)
                    },
                    failure = { error ->
                        val (status, message, headers) = when (error) {
                            HashError.InvalidFormat -> Triple(HttpStatus.BAD_REQUEST, INVALID_HASH_FORMAT, null)
                            HashError.NotFound -> Triple(HttpStatus.NOT_FOUND, HASH_DONT_EXIST, null)
                            HashError.NotValidated -> {
                                val retryAfterSeconds = 10
                                val headers = HttpHeaders()
                                headers.add(HttpHeaders.RETRY_AFTER, retryAfterSeconds.toString())
                                Triple(HttpStatus.BAD_REQUEST, HASH_VALIDATING, headers)
                            }
                            HashError.Unreachable -> Triple(HttpStatus.BAD_REQUEST, ORIGINAL_URL_UNREACHABLE, null)
                            HashError.Unsafe -> Triple(HttpStatus.FORBIDDEN, ORIGINAL_URL_UNSAFE, null)
                        }
                        ResponseEntity.status(status).headers(headers).body(message)
                    }
                )
            }
    }

    /**
     * Processes a CSV file containing URLs and generates a CSV with shortened URLs and its QR code URLs if requested.
     *
     * @param data The uploaded CSV file containing URLs.
     * @param request The HTTP request.
     * @return A [Mono] emitting a [ResponseEntity] with the processed CSV file as a downloadable response.
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
     * @return A [Mono] emitting a [ResponseEntity] containing the analytics data. Returns 200 OK with the data
     *          on success, 404 NOT FOUND if the hash is invalid, or other appropriate error codes such as
     *          400 BAD REQUEST or 403 FORBIDDEN for validation or security-related issues.
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
                        val (status, message, headers) = when (error) {
                            HashError.InvalidFormat -> Triple(HttpStatus.BAD_REQUEST, INVALID_HASH_FORMAT, null)
                            HashError.NotFound -> Triple(HttpStatus.NOT_FOUND, HASH_DONT_EXIST, null)
                            HashError.NotValidated -> {
                                val retryAfterSeconds = 10
                                val headers = HttpHeaders()
                                headers.add(HttpHeaders.RETRY_AFTER, retryAfterSeconds.toString())
                                Triple(HttpStatus.BAD_REQUEST, HASH_VALIDATING, headers)
                            }
                            HashError.Unreachable -> Triple(HttpStatus.BAD_REQUEST, ORIGINAL_URL_UNREACHABLE, null)
                            HashError.Unsafe -> Triple(HttpStatus.FORBIDDEN, ORIGINAL_URL_UNSAFE, null)
                        }
                        ResponseEntity.status(status).headers(headers).body(message)
                    }
                )
            }
    }

    companion object {
        const val INVALID_HASH_FORMAT = "Invalid shortened hash format"
        const val HASH_DONT_EXIST = "The given shortened hash does not exist"
        const val HASH_VALIDATING = "This shortened hash is still being validated. Wait a few seconds and try again"
        const val ORIGINAL_URL_UNREACHABLE = "The original url is unreachable"
        const val ORIGINAL_URL_UNSAFE = "The original url is unsafe"
    }
}
