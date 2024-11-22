@file:Suppress("WildcardImport")

package es.unizar.urlshortener.infrastructure.delivery

import es.unizar.urlshortener.core.*
import es.unizar.urlshortener.core.usecases.*
import jakarta.servlet.http.HttpServletRequest
import org.springframework.hateoas.server.mvc.linkTo
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
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
    fun redirectTo(id: String, request: HttpServletRequest): ResponseEntity<Unit>

    /**
     * Creates a short url from details provided in [data].
     *
     * @param data Input data containing the original URL and optional metadata.
     * @param request The HTTP request for capturing client context.
     * @return A [ResponseEntity] with the details of the created short URL.
     */
    fun shortener(data: ShortUrlDataIn, request: HttpServletRequest): ResponseEntity<ShortUrlDataOut>
}

/**
 * The implementation of the controller.
 */
@Suppress("LongParameterList")
@RestController
class UrlShortenerControllerImpl(
    val redirectUseCase: RedirectUseCase,
    val logClickUseCase: LogClickUseCase,
    val qrUseCase: CreateQRUseCase,
    val geoLocationService: GeoLocationService,
    val browserPlatformIdentificationUseCase: BrowserPlatformIdentificationUseCase,
    val processCsvUseCase: ProcessCsvUseCase,
    val getAnalyticsUseCase: GetAnalyticsUseCase,
    val generateEnhancedShortUrlUseCaseImpl: GenerateEnhancedShortUrlUseCase,
    val shortUrlRepositoryService: ShortUrlRepositoryService,
) : UrlShortenerController {

    /**
     * Redirects and logs a short url identified by its [id].
     *
     * @param id the identifier of the short url
     * @param request the HTTP request
     * @return a ResponseEntity with the redirection details
     */
    @GetMapping("/{id:(?!api|index|favicon\\.ico).*}")
    override fun redirectTo(@PathVariable id: String, request: HttpServletRequest): ResponseEntity<Unit> {
        val geoLocation = geoLocationService.get(request.remoteAddr)
        val userAgent = request.getHeader("User-Agent")
        var browserPlatform: BrowserPlatform? = null
        if (userAgent != null) {
            browserPlatform = browserPlatformIdentificationUseCase.parse(userAgent)
        }

        return redirectUseCase.redirectTo(id).run {
            logClickUseCase.logClick(
                id,
                ClickProperties(
                    ip = geoLocation.ip,
                    country = geoLocation.country,
                    browser = browserPlatform?.browser,
                    platform = browserPlatform?.platform
                )
            )
            val h = HttpHeaders()
            h.location = URI.create(target)
            ResponseEntity<Unit>(h, HttpStatus.valueOf(mode))
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
    fun redirectToQrCode(@RequestParam id: String, request: HttpServletRequest): ResponseEntity<ByteArray> {
        val shortUrl: ShortUrl? = safeCall {
            shortUrlRepositoryService.findByKey(id)
        }

        if (shortUrl == null) {
            throw RedirectionNotFound(id)
        }

        val qrCode = qrUseCase.create(
            linkTo<UrlShortenerControllerImpl> {
                redirectTo(
                    id,
                    request
                )
            }.toUri().toString(),
            QR_SIZE
        )

        return ResponseEntity.ok()
            .contentType(MediaType.IMAGE_PNG)
            .body(qrCode)
    }

    /**
     * Processes a CSV file containing URLs and generates a CSV with shortened URLs and its QR code URLs if requested.
     *
     * @param file The uploaded CSV file containing URLs.
     * @param request The HTTP request.
     * @return A [ResponseEntity] with the processed CSV file as a downloadable response.
     */
    @PostMapping("/api/upload-csv", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun shortenUrlsFromCsv(data: ShortUrlDataIn, request: HttpServletRequest): ResponseEntity<StreamingResponseBody> {
        if (data.file == null || data.file!!.isEmpty) {
            return ResponseEntity
                .badRequest()
                .body(StreamingResponseBody { outputStream ->
                    outputStream.writer().use {
                        it.write("Error: No file provided or file is empty.")
                    }
                })
        }

        val reader = InputStreamReader(data.file!!.inputStream.buffered())

        val responseBody = StreamingResponseBody { outputStream ->
            BufferedWriter(OutputStreamWriter(outputStream)).use { writer ->
                processCsvUseCase.processCsv(reader, writer, request, data)
            }
        }

        val headers = HttpHeaders().apply {
            add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=shortened_urls.csv")
            contentType = MediaType.parseMediaType("text/csv")
        }

        return ResponseEntity.ok()
            .headers(headers)
            .body(responseBody)
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
    override fun shortener(data: ShortUrlDataIn, request: HttpServletRequest): ResponseEntity<ShortUrlDataOut> {
        val response = generateEnhancedShortUrlUseCaseImpl.generate(data, request)

        val h = HttpHeaders()
        h.location = response.shortUrl

        return ResponseEntity(response, h, HttpStatus.CREATED)
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
    ): ResponseEntity<AnalyticsData> {
        val analyticsData = getAnalyticsUseCase.getAnalytics(
            id = id,
            includeBrowser = browser,
            includeCountry = country,
            includePlatform = platform,
            includeReferrer = referrer
        )
        return ResponseEntity.ok(analyticsData)
    }

    companion object {
        const val QR_SIZE = 256
    }
}
