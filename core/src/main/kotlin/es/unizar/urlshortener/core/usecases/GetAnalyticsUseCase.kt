package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.AnalyticsData
import es.unizar.urlshortener.core.ClickRepositoryService
import es.unizar.urlshortener.core.RedirectionNotFound
import es.unizar.urlshortener.core.ShortUrlRepositoryService

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
     * @param includeReferrer Whether to include analytics data grouped by referrer. Default is false.
     * @return An instance of [AnalyticsData] containing the requested analytics details.
     * @throws RedirectionNotFound If no ShortUrl entity is found with the given [id].
     */
    fun getAnalytics(
        id: String,
        includeBrowser: Boolean = false,
        includeCountry: Boolean = false,
        includePlatform: Boolean = false,
        includeReferrer: Boolean = false
    ): AnalyticsData
}


/**
 * Implementation of [GetAnalyticsUseCase].
 *
 * Fetches analytics data from a [ClickRepositoryService] and calculates the requested breakdowns
 * for a shortened URL.
 *
 * @property clickRepository The repository used to fetch click data for the URL.
 * @property shortUrlRepository The repository used to verify the existence of the shortened URL by its hash.
 */
class GetAnalyticsUseCaseImpl(
    private val clickRepository: ClickRepositoryService,
    private val shortUrlRepository: ShortUrlRepositoryService
) : GetAnalyticsUseCase {
    /**
     * Retrieves aggregated analytics data for a given URL identified by its [id].
     *
     * @param id The hash of the shortened URL to analyze.
     * @param includeBrowser Whether to include analytics data grouped by browser. Default is false.
     * @param includeCountry Whether to include analytics data grouped by country. Default is false.
     * @param includePlatform Whether to include analytics data grouped by platform. Default is false.
     * @param includeReferrer Whether to include analytics data grouped by referrer. Default is false.
     * @return An instance of [AnalyticsData] containing the requested analytics details.
     * @throws RedirectionNotFound If no ShortUrl entity is found with the given [id].
     */
    override fun getAnalytics(
        id: String,
        includeBrowser: Boolean,
        includeCountry: Boolean,
        includePlatform: Boolean,
        includeReferrer: Boolean
    ): AnalyticsData {
        shortUrlRepository.findByKey(id) ?: throw RedirectionNotFound(id)

        val clicks = clickRepository.findAllByHash(id)

        val totalClicks = clicks.size

        val byBrowser = if (includeBrowser) {
            clicks.groupingBy { it.properties.browser ?: "Unknown" }
                .eachCount()
        } else null

        val byCountry = if (includeCountry) {
            clicks.groupingBy { it.properties.country ?: "Unknown" }
                .eachCount()
        } else null

        val byPlatform = if (includePlatform) {
            clicks.groupingBy { it.properties.platform ?: "Unknown" }
                .eachCount()
        } else null

        val byReferrer = if (includeReferrer) {
            clicks.groupingBy { it.properties.referrer ?: "Unknown" }
                .eachCount()
        } else null

        return AnalyticsData(
            totalClicks = totalClicks,
            byBrowser = byBrowser,
            byCountry = byCountry,
            byPlatform = byPlatform,
            byReferrer = byReferrer
        )
    }
}
