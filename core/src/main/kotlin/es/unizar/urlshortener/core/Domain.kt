package es.unizar.urlshortener.core

import org.springframework.http.codec.multipart.FilePart
import java.net.URI
import java.time.OffsetDateTime

/**
 * [Click] represents a redirection request for a [ShortUrl], identified by its [hash].
 *
 * @property id The unique identifier of the click (nullable).
 * @property hash The hash associated with the shortened URL.
 * @property properties Additional properties related to the click.
 * @property created The timestamp when the click occurred. Defaults to the current time.
 */
data class Click(
    val id: Long? = null,
    val hash: String,
    val properties: ClickProperties = ClickProperties(),
    val created: OffsetDateTime = OffsetDateTime.now()
)

/**
 * [ShortUrl] represents the mapping between a remote URL ([redirection]) and a local shortened URL ([hash]).
 *
 * @property hash The unique hash associated with the shortened URL.
 * @property redirection Details about the target URL and redirection mode.
 * @property created The timestamp when the short URL was created. Defaults to the current time.
 * @property properties Additional properties related to the short URL.
 */
data class ShortUrl(
    val hash: String,
    val redirection: Redirection,
    val created: OffsetDateTime = OffsetDateTime.now(),
    val properties: ShortUrlProperties = ShortUrlProperties()
)

/**
 * [Redirection] specifies the [target] URL and the HTTP [status code][mode] for redirection.
 *
 * By default, the redirection mode is 301 (Permanent Redirect).
 *
 * @property target The target URL to which the user will be redirected.
 * @property mode The HTTP status code for the redirection (default is 301).
 */
data class Redirection(
    val target: String,
    val mode: Int = 301
)

/**
 * [ShortUrlProperties] holds properties related to a [ShortUrl].
 *
 * @property geoLocation The geographical information associated with the short URL.
 * @property validation Validation details such as safety and reachability.
 */
data class ShortUrlProperties(
    val geoLocation: GeoLocation = GeoLocation(),
    val validation: ShortUrlValidation = ShortUrlValidation()
)

/**
 * [ClickProperties] holds properties related to a [Click].
 *
 * @property geoLocation The geographical information associated with the click.
 * @property browserPlatform Information about the user's browser and platform.
 */
data class ClickProperties(
    val geoLocation: GeoLocation = GeoLocation(),
    val browserPlatform: BrowserPlatform = BrowserPlatform()
)

/**
 * [GeoLocation] represents the geographical information associated with an IP address.
 *
 * @property ip The IP address.
 * @property country The country associated with the IP address.
 */
data class GeoLocation(
    val ip: String? = null,
    val country: String? = null
)

/**
 * [BrowserPlatform] represents information about the user's browser and platform.
 *
 * @property browser The name of the browser (e.g., "Chrome", "Firefox").
 * @property platform The name of the platform (e.g., "Windows", "macOS").
 */
data class BrowserPlatform(
    val browser: String? = null,
    val platform: String? = null
)

/**
 * [ShortUrlValidation] represents validation-related properties of a [ShortUrl].
 *
 * @property safe Indicates whether the URL is considered safe.
 * @property reachable Indicates whether the URL is reachable.
 * @property validated Indicates whether the URL has been fully validated.
 */
data class ShortUrlValidation(
    val safe: Boolean = false,
    val reachable: Boolean = false,
    val validated: Boolean = false
)

/**
 * [AnalyticsData] represents aggregated analytics data for a shortened URL.
 *
 * @property totalClicks The total number of clicks for the URL.
 * @property byBrowser A map containing the breakdown of clicks by browser. The key is the browser name,
 *                     and the value is the number of clicks. Default is null if not requested.
 * @property byCountry A map containing the breakdown of clicks by country. The key is the country name,
 *                     and the value is the number of clicks. Default is null if not requested.
 * @property byPlatform A map containing the breakdown of clicks by platform. The key is the platform name
 *                      (e.g., "Windows", "macOS"), and the value is the number of clicks. Default is null
 *                      if not requested.
 */
data class AnalyticsData(
    val totalClicks: Int,
    val byBrowser: Map<String, Int>? = null,
    val byCountry: Map<String, Int>? = null,
    val byPlatform: Map<String, Int>? = null
)

/**
 * [ShortUrlDataIn] represents the input data required to create a shortened URL.
 *
 * @property rawUrl The original URL to be shortened.
 * @property qrRequested Indicates whether a QR code should be generated for the shortened URL.
 */
data class ShortUrlDataIn(
    private val rawUrl: String,
    val qrRequested: Boolean = false
) {
    val url: String
        get() = if (rawUrl.endsWith("/")) rawUrl else "$rawUrl/"
}

/**
 * [CsvDataIn] represents the input data required to process a CSV file for creating multiple shortened URLs.
 *
 * @property file The CSV file containing the URLs to be shortened.
 * @property qrRequested Indicates whether QR codes should be generated for the shortened URLs.
 */
data class CsvDataIn(
    val file: FilePart,
    val qrRequested: Boolean = false
)

/**
 * [ShortUrlDataOut] represents the output data returned after successfully creating a shortened URL.
 *
 * @property shortUrl The URI of the newly created shortened URL.
 * @property qrCodeUrl The URI of the QR code associated with the shortened URL (optional).
 */
data class ShortUrlDataOut(
    val shortUrl: URI,
    val qrCodeUrl: URI? = null
)
