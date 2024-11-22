@file:Suppress("ForbiddenComment")
package es.unizar.urlshortener.thirdparties.ipinfo

import es.unizar.urlshortener.core.GeoLocation
import es.unizar.urlshortener.core.GeoLocationService
import io.github.cdimascio.dotenv.Dotenv
import org.springframework.web.reactive.function.client.WebClient

/**
 * [GeoLocationServiceImpl] is an implementation of the [GeoLocationService] interface.
 * It provides functionality to retrieve geographical information based on an IP address
 * using the IPInfo API. This class utilizes a [WebClient] for making HTTP requests.
 *
 * @property webClient The WebClient instance used for HTTP communication.
 * @param dotenv The dotenv library instance for loading API keys and other configurations.
 */
class GeoLocationServiceImpl(
    private val webClient: WebClient,
    dotenv: Dotenv
) : GeoLocationService {

    private val accessToken = System.getenv(DOTENV_IPINFO_KEY) ?: dotenv[DOTENV_IPINFO_KEY]

    /**
     * Retrieves geographical information for the specified IP address.
     *
     * @param ip The IP address for which to obtain geographical data.
     * @return A [GeoLocation] object containing the IP address and associated country.
     *
     * **TODO**: Implement a custom IP class that validates and checks for IPv4 or IPv6 format.
     */
    override fun get(ip: String): GeoLocation {
        val url = buildRequestUrl(ip)

        // TODO: In the custom IP class auto-detect if ip is Bogon and return
        val response = webClient.get()
            .uri(url)
            .retrieve()
            .bodyToMono(Map::class.java)
            .block()

        val ipAddress = response?.get("ip") as String
        val country = if (response.containsKey("bogon")) {
            "Bogon"
        } else {
            response["country"] as String
        }

        return GeoLocation(ipAddress, country)
    }

    /**
     * Builds the request URL for the IPInfo API using the provided IP address.
     *
     * @param ip The IP address to be used in the request URL.
     * @return The complete URL string for the API request.
     *
     * **TODO**: Adapt request URL to handle different formats for IPv4 and IPv6,
     * as the IPInfo endpoint may differ based on the format.
     */
    private fun buildRequestUrl(ip: String): String {
        return "${IPINFO_BASE_URL}${ip}?token=${accessToken}"
    }

    companion object {
        const val DOTENV_IPINFO_KEY = "IPINFO_API_KEY"
        const val IPINFO_BASE_URL = "https://ipinfo.io/"
    }
}
