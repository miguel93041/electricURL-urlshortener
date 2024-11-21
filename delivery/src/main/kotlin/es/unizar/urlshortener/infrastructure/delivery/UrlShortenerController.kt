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
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
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
     * **Note**: Delivery of use cases [RedirectUseCase] and [LogClickUseCase].
     */
    fun redirectTo(id: String, request: HttpServletRequest): ResponseEntity<Unit>

    /**
     * Creates a short url from details provided in [data].
     *
     * **Note**: Delivery of use case [CreateShortUrlUseCase].
     */
    fun shortener(data: ShortUrlDataIn, request: HttpServletRequest): ResponseEntity<ShortUrlDataOut>
}

/**
 * Data required to create a short url.
 */
data class ShortUrlDataIn(
    val url: String,
    val sponsor: String? = null,
    val qrRequested: Boolean = false
)

/**
 * Data returned after the creation of a short url.
 */
data class ShortUrlDataOut(
    val url: URI? = null,
    val properties: Map<String, Any> = emptyMap(),
    val qrCodeUrl: URI? = null
)

/**
 * The implementation of the controller.
 *
 * **Note**: Spring Boot is able to discover this [RestController] without further configuration.
 */
@Suppress("LongParameterList")
@RestController
class UrlShortenerControllerImpl(
    val redirectUseCase: RedirectUseCase,
    val logClickUseCase: LogClickUseCase,
    val createShortUrlUseCase: CreateShortUrlUseCase,
    val qrUseCase: CreateQRUseCase,
    val geoLocationService: GeoLocationService,
    val redirectionLimitUseCase: RedirectionLimitUseCase,
    val browserPlatformIdentificationUseCase: BrowserPlatformIdentificationUseCase,
    val processCsvUseCase: ProcessCsvUseCase,
    val urlAccessibilityCheckUseCase: UrlAccessibilityCheckUseCase,
    val urlValidationService: UrlSafetyService,
    val getAnalyticsUseCase: GetAnalyticsUseCase,
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
        if (redirectionLimitUseCase.isRedirectionLimit(id)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build()
        }
        val geoLocation = geoLocationService.get(request.remoteAddr)
        val browserPlatform = browserPlatformIdentificationUseCase.parse(request.getHeader("User-Agent"))

        return redirectUseCase.redirectTo(id).run {
            logClickUseCase.logClick(
                id,
                ClickProperties(
                    ip = geoLocation.ip,
                    country = geoLocation.country,
                    browser = browserPlatform.browser,
                    platform = browserPlatform.platform
                )
            )
            val h = HttpHeaders()
            h.location = URI.create(target)
            ResponseEntity<Unit>(h, HttpStatus.valueOf(mode))
        }
    }

    @GetMapping("/api/qr", produces = [MediaType.IMAGE_PNG_VALUE])
    fun redirectToQrCode(@RequestParam id: String, request: HttpServletRequest): ResponseEntity<ByteArray> {
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

    @PostMapping("/api/upload-csv", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun shortenUrlsFromCsv(
        @RequestParam("file") file: MultipartFile,
        request: HttpServletRequest
    ): ResponseEntity<StreamingResponseBody> {
        val reader = InputStreamReader(file.inputStream.buffered())

        val responseBody = StreamingResponseBody { outputStream ->
            BufferedWriter(OutputStreamWriter(outputStream)).use { writer ->
                processCsvUseCase.processCsv(reader, writer, request)
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
        if (!urlAccessibilityCheckUseCase.isUrlReachable(data.url)) {
            return ResponseEntity(ShortUrlDataOut(), HttpStatus.BAD_REQUEST)
        }

        if (!urlValidationService.isSafe(data.url)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ShortUrlDataOut())
        }

        val geoLocation = geoLocationService.get(request.remoteAddr)

        return createShortUrlUseCase.create(
            url = data.url,
            data = ShortUrlProperties(
                ip = geoLocation.ip,
                sponsor = data.sponsor,
                country = geoLocation.country
            )
        ).run {
            val h = HttpHeaders()
            val url = linkTo<UrlShortenerControllerImpl> { redirectTo(hash, request) }.toUri()
            val qrUrl = if (data.qrRequested) {
                linkTo<UrlShortenerControllerImpl> {
                    redirectToQrCode(
                        hash,
                        request
                    )
                }.toUri()
            } else {
                null
            }
            h.location = url
            val response = ShortUrlDataOut(
                url = url,
                properties = mapOf(
                    "safe" to properties.safe,
                ),
                qrCodeUrl = qrUrl
            )
            ResponseEntity<ShortUrlDataOut>(response, h, HttpStatus.CREATED)
        }
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
    @GetMapping("/api/analytics")
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
