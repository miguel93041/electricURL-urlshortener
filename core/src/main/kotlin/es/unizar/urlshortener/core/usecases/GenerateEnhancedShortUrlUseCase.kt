@file:Suppress("WildcardImport")

package es.unizar.urlshortener.core.usecases

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import es.unizar.urlshortener.core.*
import jakarta.servlet.http.HttpServletRequest
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
    fun generate(data: ShortUrlDataIn, request: HttpServletRequest): Result<ShortUrlDataOut, UrlError>
}

/**
 * Implementation of the `GenerateEnhancedShortUrlUseCase` interface.
 *
 * This class acts as the controller to handle requests for generating enhanced short URLs.
 */
class GenerateEnhancedShortUrlUseCaseImpl(
    private val urlValidatorService: UrlValidatorService,
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
    override fun generate(data: ShortUrlDataIn, request: HttpServletRequest): Result<ShortUrlDataOut, UrlError> {
        // Validate URL
        val validationResult = urlValidatorService.validate(data.url);
        if (validationResult.isErr) {
            return Err(validationResult.error)
        }

        // Enhancement data
        val geoLocation = geoLocationService.get(request.remoteAddr)
        val enrichedData = ShortUrlProperties(
            ip = geoLocation.ip,
            country = geoLocation.country
        )

        // Generate a new short-URL in the system
        val shortUrlModel = createShortUrlUseCase.create(data.url, enrichedData)

        // Create links
        val shortUrl = safeCall { URI.create("${baseUrlProvider.get()}/${shortUrlModel.hash}") }
        var qrCodeUrl: URI? = null
        if (data.qrRequested) {
            qrCodeUrl = safeCall { URI.create("${baseUrlProvider.get()}/api/qr?id=${shortUrlModel.hash}") }
        }

        return Ok(ShortUrlDataOut(shortUrl, qrCodeUrl))
    }
}
