@file:Suppress("WildcardImport")

package es.unizar.urlshortener.core.services

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import es.unizar.urlshortener.core.*
import es.unizar.urlshortener.core.usecases.BrowserPlatformIdentificationUseCase
import es.unizar.urlshortener.core.usecases.LogClickUseCase
import es.unizar.urlshortener.core.usecases.RedirectUseCase
import jakarta.servlet.http.HttpServletRequest

/**
 * Defines the specification for the use case responsible for generating
 * enhanced short URLs with additional properties like geolocation data.
 */
interface RedirectService {
    /**
     * Obtains the original url of a short url.
     *
     * @param hash The identifier of the short url.
     * @param request The HTTP request object, used for extracting contextual information (e.g., IP address).
     * @return A data object containing the generated short URL and optional QR code URL.
     */
    fun getRedirectionAndLogClick(hash: String, request: HttpServletRequest): Result<Redirection, HashError>
}

/**
 * Implementation of the `GenerateEnhancedShortUrlUseCase` interface.
 *
 * This class acts as the controller to handle requests for generating enhanced short URLs.
 */
class RedirectServiceImpl(
    private val hashValidatorService: HashValidatorService,
    private val redirectUseCase: RedirectUseCase,
    private val logClickUseCase: LogClickUseCase,
    private val geoLocationService: GeoLocationService,
    private val browserPlatformIdentificationUseCase: BrowserPlatformIdentificationUseCase,
) : RedirectService {

    /**
     * Generates an enhanced short URL using geolocation information and other properties.
     *
     * @param hash The identifier of the short url.
     * @param request The HTTP request, used to extract the client's IP address for geolocation purposes.
     * @return A data object containing the short URL and optionally a QR code URL.
     */
    override fun getRedirectionAndLogClick(hash: String, request: HttpServletRequest): Result<Redirection, HashError> {
        // Validate hash
        val validationResult = hashValidatorService.validate(hash);
        if (validationResult.isErr) {
            return Err(validationResult.error)
        }

        // Enhancement data
        val geoLocation = geoLocationService.get(request.remoteAddr)
        val browserPlatform = browserPlatformIdentificationUseCase.parse(request.getHeader("User-Agent"))

        val enrichedData = ClickProperties(
            ip = geoLocation.ip,
            country = geoLocation.country,
            browser = browserPlatform.browser,
            platform = browserPlatform.platform
        )

        // Obtain the redirection with the original URL
        val redirectionResult = redirectUseCase.redirectTo(hash)
        if (redirectionResult.isErr) {
            return Err(validationResult.error)
        }

        // Log the click in the system
        logClickUseCase.logClick(hash, enrichedData)

        return Ok(redirectionResult.value)
    }
}