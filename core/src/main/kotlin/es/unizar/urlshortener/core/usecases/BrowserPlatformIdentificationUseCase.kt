package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.BrowserPlatform
import ua_parser.Parser

/**
 * Interface for identifying the browser and platform from a user agent string.
 */
interface BrowserPlatformIdentificationUseCase {

    /**
     * Parses the user agent string received in a redirection request to identify
     * the browser and platform being used.
     *
     * @param userAgent The user agent header from the request.
     * @return A [BrowserPlatform] object containing the identified browser and platform.
     */
    fun parse(userAgent: String?): BrowserPlatform
}

/**
 * [BrowserPlatformIdentificationUseCaseImpl] is an implementation of [BrowserPlatformIdentificationUseCase].
 *
 * Parses user agent strings and extracts browser and platform details.
 *
 * @property parser An instance of [Parser] used to process user agent strings.
 */
class BrowserPlatformIdentificationUseCaseImpl(
    private val parser: Parser
) : BrowserPlatformIdentificationUseCase {

    /**
     * Parses the user agent string to identify the browser and platform.
     *
     * @param userAgent The user agent header from the request.
     * @return A [BrowserPlatform] object containing the identified browser and platform or "Unknown" as
     * the identified browser name and as the identified platform name if not found.
     */
    override fun parse(userAgent: String?): BrowserPlatform {
        var browser = "Unknown"
        var platform = "Unknown"

        if (userAgent.isNullOrBlank()) {
            return BrowserPlatform(browser, platform)
        }

        val client = parser.parse(userAgent)

        if (client.userAgent != null && !client.userAgent.family.isNullOrBlank()) {
            browser = client.userAgent.family
        }

        if (client.os != null && !client.os.family.isNullOrBlank()) {
            platform = client.os.family
        }

        return BrowserPlatform(browser, platform)
    }
}
