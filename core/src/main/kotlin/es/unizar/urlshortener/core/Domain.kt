package es.unizar.urlshortener.core

import java.net.URI
import java.time.OffsetDateTime

/**
 * A [Click] captures a request of redirection of a [ShortUrl] identified by its [hash].
 */
data class Click(
    val hash: String,
    val properties: ClickProperties = ClickProperties(),
    val created: OffsetDateTime = OffsetDateTime.now()
)

/**
 * A [ShortUrl] is the mapping between a remote url identified by [redirection]
 * and a local short url identified by [hash].
 */
data class ShortUrl(
    val hash: String,
    val redirection: Redirection,
    val created: OffsetDateTime = OffsetDateTime.now(),
    val properties: ShortUrlProperties = ShortUrlProperties()
)

/**
 * A [Redirection] specifies the [target] and the [status code][mode] of a redirection.
 * By default, the [status code][mode] is 307 TEMPORARY REDIRECT.
 */
data class Redirection(
    val target: String,
    val mode: Int = 307
)

/**
 * A [ShortUrlProperties] is the bag of properties that a [ShortUrl] may have.
 */
data class ShortUrlProperties(
    val ip: String? = null,
    val sponsor: String? = null,
    val safe: Boolean = true,
    val owner: String? = null,
    val country: String? = null
)

/**
 * A [ClickProperties] is the bag of properties that a [Click] may have.
 */
data class ClickProperties(
    val ip: String? = null,
    val referrer: String? = null,
    val browser: String? = null,
    val platform: String? = null,
    val country: String? = null
)

/**
 * A [GeoLocation] represents the geographical information associated with an IP address.
 */
data class GeoLocation(
    val ip: String,
    val country: String
)

data class GeneratedShortUrlResult(
    val shortUrl: ShortUrl,
    val qrCodeUrl: URI?,
    val url: URI?

)

/**
 * A [BrowserPlatform] represents information about the user's browser and platform.
 */
data class BrowserPlatform(
    val browser: String,
    val platform: String
)

/**
 * Data class representing aggregated analytics data for a shortened URL.
 *
 * @property totalClicks The total number of clicks for the URL.
 * @property byBrowser A map containing the breakdown of clicks by browser. The key is the browser name,
 *                     and the value is the number of clicks. Default is null if not requested.
 * @property byCountry A map containing the breakdown of clicks by country. The key is the country name,
 *                     and the value is the number of clicks. Default is null if not requested.
 * @property byPlatform A map containing the breakdown of clicks by platform. The key is the platform name
 *                      (e.g., "Windows", "macOS"), and the value is the number of clicks. Default is null
 *                      if not requested.
 * @property byReferrer A map containing the breakdown of clicks by referrer. The key is the referrer
 *                      (e.g., "Google", "Facebook"),
 *                      and the value is the number of clicks. Default is null if not requested.
 */
data class AnalyticsData(
    val totalClicks: Int,
    val byBrowser: Map<String, Int>? = null,
    val byCountry: Map<String, Int>? = null,
    val byPlatform: Map<String, Int>? = null,
    val byReferrer: Map<String, Int>? = null
)
