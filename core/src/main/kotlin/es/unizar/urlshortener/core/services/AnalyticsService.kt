@file:Suppress("WildcardImport")

package es.unizar.urlshortener.core.services

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import es.unizar.urlshortener.core.*
import es.unizar.urlshortener.core.usecases.GetAnalyticsUseCase
import reactor.core.publisher.Mono

/**
 * Defines the specification for retrieving analytics data based on a short URL hash.
 */
fun interface AnalyticsService {

    /**
     * Retrieves analytics data for a given short URL hash.
     *
     * @param hash The identifier of the short URL.
     * @param includeBrowser Whether to include browser information in the analytics.
     * @param includeCountry Whether to include country information in the analytics.
     * @param includePlatform Whether to include platform information in the analytics.
     * @return A [Mono] emitting a [Result] containing the [AnalyticsData] or a [HashError].
     */
    fun get(
        hash: String,
        includeBrowser: Boolean,
        includeCountry: Boolean,
        includePlatform: Boolean
    ): Mono<Result<AnalyticsData, HashError>>
}

/**
 * [AnalyticsServiceImpl] is an implementation of the [AnalyticsService] interface.
 *
 * This class handles the retrieval of analytics data for short URLs using the provided [GetAnalyticsUseCase].
 *
 * @property hashValidatorService Service for validating the short URL hash.
 * @property analyticsUseCase Use case for retrieving analytics data.
 */
class AnalyticsServiceImpl(
    private val hashValidatorService: HashValidatorService,
    private val analyticsUseCase: GetAnalyticsUseCase,
) : AnalyticsService {

    /**
     * Retrieves analytics data for a given short URL hash with optional filters for browser, country
     * and platform information.
     *
     * @param hash The identifier of the short URL.
     * @param includeBrowser Whether to include browser information.
     * @param includeCountry Whether to include country information.
     * @param includePlatform Whether to include platform information.
     * @return A [Mono] emitting a [Result] containing the [AnalyticsData] or a [HashError].
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
