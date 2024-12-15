package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.*
import reactor.core.publisher.Mono

/**
 * Interface defining the contract for fetching analytics data for a shortened URL.
 */
interface GetAnalyticsUseCase {
    /**
     * Retrieves aggregated analytics data for a given URL identified by its [id].
     *
     * @param id The hash of the shortened URL to analyze.
     * @param includeBrowser Whether to include analytics data grouped by browser. Default is false.
     * @param includeCountry Whether to include analytics data grouped by country. Default is false.
     * @param includePlatform Whether to include analytics data grouped by platform. Default is false.
     * @return An instance of [AnalyticsData] containing the requested analytics details.
     * @throws RedirectionNotFound If no ShortUrl entity is found with the given [id].
     */
    fun getAnalytics(
        id: String,
        includeBrowser: Boolean = false,
        includeCountry: Boolean = false,
        includePlatform: Boolean = false
    ): Mono<AnalyticsData>
}


/**
 * Implementation of [GetAnalyticsUseCase].
 *
 * Fetches analytics data from a [ClickRepositoryService] and calculates the requested breakdowns
 * for a shortened URL.
 *
 * @property clickRepository The repository used to fetch click data for the URL.
 */
class GetAnalyticsUseCaseImpl(private val clickRepository: ClickRepositoryService) : GetAnalyticsUseCase {

    override fun getAnalytics(
        id: String,
        includeBrowser: Boolean,
        includeCountry: Boolean,
        includePlatform: Boolean
    ): Mono<AnalyticsData> {
        return clickRepository.findAllByHash(id)
            .collectList()
            .map { clicks ->
                val totalClicks = clicks.size

                val byBrowser = if (includeBrowser) {
                    clicks.groupingBy { it.properties.browserPlatform.browser ?: "Unknown" }.eachCount()
                } else null

                val byCountry = if (includeCountry) {
                    clicks.groupingBy { it.properties.geoLocation.country ?: "Unknown" }.eachCount()
                } else null

                val byPlatform = if (includePlatform) {
                    clicks.groupingBy { it.properties.browserPlatform.platform ?: "Unknown" }.eachCount()
                } else null

                AnalyticsData(totalClicks, byBrowser, byCountry, byPlatform)
            }
            .defaultIfEmpty(
                AnalyticsData(
                    totalClicks = 0,
                    byBrowser = if (includeBrowser) emptyMap() else null,
                    byCountry = if (includeCountry) emptyMap() else null,
                    byPlatform = if (includePlatform) emptyMap() else null
                )
            )
    }
}
