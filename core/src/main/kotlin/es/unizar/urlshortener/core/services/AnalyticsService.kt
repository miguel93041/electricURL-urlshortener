@file:Suppress("WildcardImport")

package es.unizar.urlshortener.core.services

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import es.unizar.urlshortener.core.*
import es.unizar.urlshortener.core.usecases.GetAnalyticsUseCase
import reactor.core.publisher.Mono

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
        includePlatform: Boolean
    ): Mono<Result<AnalyticsData, HashError>>
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
     * Retrieves analytics data for a given short URL.
     *
     * @param hash The identifier of the short URL.
     * @param includeBrowser Whether to include analytics data grouped by browser.
     * @param includeCountry Whether to include analytics data grouped by country.
     * @param includePlatform Whether to include analytics data grouped by platform.
     * @return A Mono emitting a Result containing either the analytics data or a HashError.
     */
    override fun get(
        hash: String,
        includeBrowser: Boolean,
        includeCountry: Boolean,
        includePlatform: Boolean
    ): Mono<Result<AnalyticsData, HashError>> {
        return hashValidatorService.validate(hash)
            .flatMap { validationResult ->
                if (validationResult.isErr) {
                    return@flatMap Mono.just(Err(validationResult.error))
                }

                analyticsUseCase.getAnalytics(
                    hash,
                    includeBrowser,
                    includeCountry,
                    includePlatform
                ).map { analyticsData ->
                    Ok(analyticsData)
                }
            }
    }
}
