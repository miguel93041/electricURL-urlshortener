@file:Suppress("WildcardImport")

package es.unizar.urlshortener.infrastructure.delivery

import com.github.michaelbull.result.fold
import es.unizar.urlshortener.core.*
import es.unizar.urlshortener.core.services.AnalyticsService
import es.unizar.urlshortener.core.services.CsvService
import es.unizar.urlshortener.core.services.QrService
import es.unizar.urlshortener.core.services.RedirectService
import es.unizar.urlshortener.core.usecases.*
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
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
    fun redirectTo(id: String, request: HttpServletRequest)

    /**
     * Creates a short url from details provided in [data].
     *
     * @param data Input data containing the original URL and optional metadata.
     * @param request The HTTP request for capturing client context.
     * @return A [ResponseEntity] with the details of the created short URL.
     */
    fun shortener(data: ShortUrlDataIn, request: HttpServletRequest)

    fun redirectToQrCode(id: String, request: HttpServletRequest)
}

/**
 * The implementation of the controller.
 */
@Suppress("LongParameterList")
@RestController
class UrlShortenerControllerImpl(
    val csvService: CsvService,
    val analyticsService: AnalyticsService,
    val generateEnhancedShortUrlUseCaseImpl: GenerateEnhancedShortUrlUseCase,
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
    override fun redirectTo(@PathVariable id: String, request: HttpServletRequest) {
        val redirectionResult = redirectService.getRedirectionAndLogClick(id, request)

        return redirectionResult.fold(
            success = { redirection ->
                val headers = HttpHeaders()
                headers.location = safeCall { URI.create(redirection.target) }
                ResponseEntity<Unit>(headers, HttpStatus.valueOf(redirection.mode))
            },
            failure = { error ->
                val (status, message) = when (error) {
                    RedirectionError.InvalidFormat -> HttpStatus.BAD_REQUEST to "Invalid shortened hash format"
                    RedirectionError.NotFound -> HttpStatus.NOT_FOUND to "The given shortened hash does not exist"
                    RedirectionError.TooManyRequests -> HttpStatus.TOO_MANY_REQUESTS to "This shortened hash is under load"
                }
                ResponseEntity.status(status).body(message)
            }
        )
    }

    /**
     * Generates a QR code for the given short URL.
     *
     * @param id Identifier of the short URL.
     * @param request The HTTP request.
     * @return A [ResponseEntity] with the QR code image as a PNG image in a byte array format.
     */
    @GetMapping("/api/qr", produces = [MediaType.IMAGE_PNG_VALUE])
    override fun redirectToQrCode(@RequestParam id: String, request: HttpServletRequest) {
        val qrResult = qrService.getQrImage(id)

        return qrResult.fold(
            success = { qr ->
                ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .body(qr)
            },
            failure = { error ->
                val (status, message) = when (error) {
                    HashError.InvalidFormat -> HttpStatus.BAD_REQUEST to "Invalid shortened hash format"
                    HashError.NotFound -> HttpStatus.NOT_FOUND to "The given shortened hash does not exist"
                }
                ResponseEntity.status(status).body(message)
            }
        )
    }

    /**
     * Processes a CSV file containing URLs and generates a CSV with shortened URLs and its QR code URLs if requested.
     *
     * @param file The uploaded CSV file containing URLs.
     * @param request The HTTP request.
     * @return A [ResponseEntity] with the processed CSV file as a downloadable response.
     */
    @PostMapping("/api/upload-csv", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun shortenUrlsFromCsv(data: CsvDataIn, request: HttpServletRequest) {
        val processCsvResult = csvService.process(data, request)

        return processCsvResult.fold(
            success = { stream ->
                val headers = HttpHeaders().apply {
                    add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=shortened_urls.csv")
                    contentType = MediaType.parseMediaType("text/csv")
                }

                ResponseEntity.ok()
                    .headers(headers)
                    .body(stream)
            },
            failure = { error ->
                val (status, message) = when (error) {
                    CsvError.InvalidFormat -> HttpStatus.BAD_REQUEST to "Invalid CSV format"
                }
                ResponseEntity.status(status).body(message)
            }
        )
    }

    /**
     * Creates a short url from details provided in [data].
     *
     * @param data the data required to create a short url
     * @param request the HTTP request
     * @return a ResponseEntity with the created short url details
     */
    @Suppress("ReturnCount")
    @PostMapping("/api/link", consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE])
    override fun shortener(data: ShortUrlDataIn, request: HttpServletRequest) {
        val generationResult = generateEnhancedShortUrlUseCaseImpl.generate(data, request)

        return generationResult.fold(
            success = { shortUrlDataOut ->
                val headers = HttpHeaders()
                headers.location = shortUrlDataOut.shortUrl
                ResponseEntity(shortUrlDataOut, headers, HttpStatus.CREATED)
            },
            failure = { error ->
                val (status, message) = when (error) {
                    UrlError.InvalidFormat -> HttpStatus.BAD_REQUEST to "Invalid URL format"
                    UrlError.Unreachable -> HttpStatus.BAD_REQUEST to "URL is unreachable"
                    UrlError.Unsafe -> HttpStatus.FORBIDDEN to "URL is flagged as unsafe"
                }
                ResponseEntity.status(status).body(message)
            }
        )
    }

    /**
     * Endpoint to retrieve aggregated analytics data for a shortened URL.
     *
     * This method provides a way to fetch total clicks and optionally include breakdowns by browser,
     * country, platform, and referrer based on the provided query parameters.
     *
     * @param id The hash of the shortened URL for which analytics data is requested. (Required)
     * @param browser Indicates whether to include a breakdown of clicks by browser. Defaults to false. (Optional)
     * @param country Indicates whether to include a breakdown of clicks by country. Defaults to false. (Optional)
     * @param platform Indicates whether to include a breakdown of clicks by platform. Defaults to false. (Optional)
     * @param referrer Indicates whether to include a breakdown of clicks by referrer. Defaults to false. (Optional)
     * @return A ResponseEntity containing the analytics data. Returns 200 OK with the data on success,
     *         or 404 NOT FOUND if hash is invalid.
     */
    @GetMapping("/api/analytics", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getAnalytics(
        @RequestParam id: String,
        @RequestParam(required = false, defaultValue = "false") browser: Boolean,
        @RequestParam(required = false, defaultValue = "false") country: Boolean,
        @RequestParam(required = false, defaultValue = "false") platform: Boolean,
        @RequestParam(required = false, defaultValue = "false") referrer: Boolean
    ) {
        val analyticsResult = analyticsService.get(id, browser, country, platform, referrer)

        return analyticsResult.fold(
            success = { analytics -> ResponseEntity.ok(analytics) },
            failure = { error ->
                val (status, message) = when (error) {
                    HashError.InvalidFormat -> HttpStatus.BAD_REQUEST to "Invalid shortened hash format"
                    HashError.NotFound -> HttpStatus.NOT_FOUND to "The given shortened hash does not exist"
                }
                ResponseEntity.status(status).body(message)
            }
        )
    }
}
