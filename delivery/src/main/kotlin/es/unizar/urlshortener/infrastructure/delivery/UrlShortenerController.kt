@file:Suppress("WildcardImport")
package es.unizar.urlshortener.infrastructure.delivery

import RedirectionLimitUseCase
import es.unizar.urlshortener.core.ClickProperties
import es.unizar.urlshortener.core.GeoLocationService

import es.unizar.urlshortener.core.ShortUrlProperties
import es.unizar.urlshortener.core.UrlSafetyService
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
import org.springframework.web.bind.annotation.RestController
import java.net.URI

import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter

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
    val sponsor: String? = null
)

/**
 * Data returned after the creation of a short url.
 */
data class ShortUrlDataOut(
    val url: URI? = null,
    val properties: Map<String, Any> = emptyMap(),
    val qrCode: ByteArray = byteArrayOf()
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
    val browserPlatformIdentificationUseCase: BrowserPlatformIdentificationUseCase,
    val processCsvUseCase: ProcessCsvUseCase,
    val urlAccessibilityCheckUseCase: UrlAccessibilityCheckUseCase,
    val urlValidationService: UrlSafetyService,
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
        val browserPlatform = browserPlatformIdentificationUseCase.parse(request.getHeader("User-Agent"))

            return redirectUseCase.redirectTo(id).run {
            logClickUseCase.logClick(id, ClickProperties(
                ip = geoLocation.ip,
                country = geoLocation.country,
                browser = browserPlatform.browser,
                platform = browserPlatform.platform
            ))
            val h = HttpHeaders()
            h.location = URI.create(target)
            ResponseEntity<Unit>(h, HttpStatus.valueOf(mode))
        }
    }

    @PostMapping("/api/upload-csv", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun shortenUrlsFromCsv(@RequestParam("file") file: MultipartFile,
                           request: HttpServletRequest): ResponseEntity<StreamingResponseBody> {
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
            h.location = url
            val response = ShortUrlDataOut(
                url = url,
                properties = mapOf(
                    "safe" to properties.safe,
                ),
                qrCode = qrUseCase.create(url.toString(), QR_SIZE)
            )
            ResponseEntity<ShortUrlDataOut>(response, h, HttpStatus.CREATED)
        }
    }

    companion object {
        const val QR_SIZE = 256
    }
}
