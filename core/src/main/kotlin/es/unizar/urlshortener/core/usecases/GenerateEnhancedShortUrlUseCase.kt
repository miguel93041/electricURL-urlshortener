@file:Suppress("WildcardImport")

package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.*
import jakarta.servlet.http.HttpServletRequest
import org.springframework.web.bind.annotation.RestController
import java.net.URI

/**
 * Defines the specification for the use case responsible for generating
 * enhanced short URLs with additional properties like geolocation data.
 */
interface GenerateEnhancedShortUrlUseCase {
    /**
     * Generates a short URL based on the input data and the HTTP request context.
     *
     * @param data The input data containing the original URL and optional settings.
     * @param request The HTTP request object, used for extracting contextual information (e.g., IP address).
     * @return A data object containing the generated short URL and optional QR code URL.
     */
    fun generate(data: ShortUrlDataIn, request: HttpServletRequest): ShortUrlDataOut
}

/**
 * Implementation of the `GenerateEnhancedShortUrlUseCase` interface.
 *
 * This class acts as the controller to handle requests for generating enhanced short URLs.
 */
@RestController
class GenerateEnhancedShortUrlUseCaseImpl(
    private val createShortUrlUseCase: CreateShortUrlUseCase,
    private val geoLocationService: GeoLocationService,
    private val baseUrlProvider: BaseUrlProvider
) : GenerateEnhancedShortUrlUseCase {

    /**
     * Generates an enhanced short URL using geolocation information and other properties.
     *
     * @param data The input data containing the original URL and a flag indicating whether a QR code is required.
     * @param request The HTTP request, used to extract the client's IP address for geolocation purposes.
     * @return A data object containing the short URL and optionally a QR code URL.
     */
    override fun generate(data: ShortUrlDataIn, request: HttpServletRequest): ShortUrlDataOut {
        val geoLocation = geoLocationService.get(request.remoteAddr)

        val enrichedData = ShortUrlProperties(
            ip = geoLocation.ip,
            country = geoLocation.country
        )

        val shortUrlModel = createShortUrlUseCase.create(data.url!!, enrichedData)
        val shortUrl = URI.create("${baseUrlProvider.get()}/${shortUrlModel.hash}")

        var qrCodeUrl: URI? = null
        if (data.qrRequested) {
            qrCodeUrl = URI.create("${baseUrlProvider.get()}/api/qr?id=${shortUrlModel.hash}")
        }

        return ShortUrlDataOut(shortUrl, qrCodeUrl)
    }
}
