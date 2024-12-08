@file:Suppress("WildcardImport")

package es.unizar.urlshortener.core.services

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import es.unizar.urlshortener.core.*
import es.unizar.urlshortener.core.usecases.GetAnalyticsUseCase

/**
 * Defines the specification for the use case responsible for generating
 * enhanced short URLs with additional properties like geolocation data.
 */
interface AnalyticsService {
    /**
     * Obtains the original url of a short url.
     *
     * @param hash The identifier of the short url.
     * @param request The HTTP request object, used for extracting contextual information (e.g., IP address).
     * @return A data object containing the generated short URL and optional QR code URL.
     */
    fun get(
        hash: String,
        includeBrowser: Boolean,
        includeCountry: Boolean,
        includePlatform: Boolean,
        includeReferrer: Boolean
    ): Result<AnalyticsData, HashError>
}

/**
 * Implementation of the `GenerateEnhancedShortUrlUseCase` interface.
 *
 * This class acts as the controller to handle requests for generating enhanced short URLs.
 */
class AnalyticsServiceImpl(
    private val hashValidatorService: HashValidatorService,
    private val analyticsUseCase: GetAnalyticsUseCase,
) : AnalyticsService {

    /**
     * Generates an enhanced short URL using geolocation information and other properties.
     *
     * @param hash The identifier of the short url.
     * @param request The HTTP request, used to extract the client's IP address for geolocation purposes.
     * @return A data object containing the short URL and optionally a QR code URL.
     */
    override fun get(
        hash: String,
        includeBrowser: Boolean,
        includeCountry: Boolean,
        includePlatform: Boolean,
        includeReferrer: Boolean
    ): Result<AnalyticsData, HashError> {
        // Validate hash
        val validationResult = hashValidatorService.validate(hash);
        if (validationResult.isErr) {
            return Err(validationResult.error)
        }

        // Get the analytics data
        val analyticsData = analyticsUseCase.getAnalytics(
            hash,
            includeBrowser,
            includeCountry,
            includePlatform,
            includeReferrer
        )

        return Ok(analyticsData)
    }
}