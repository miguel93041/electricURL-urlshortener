@file:Suppress("ForbiddenComment", "ReturnCount")
package es.unizar.urlshortener.thirdparties.ipinfo

import com.github.benmanes.caffeine.cache.AsyncCache
import es.unizar.urlshortener.core.GeoLocation
import es.unizar.urlshortener.core.GeoLocationService
import io.github.cdimascio.dotenv.Dotenv
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.util.concurrent.CompletableFuture

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
    dotenv: Dotenv,
    private val cache: AsyncCache<String, GeoLocation>
) : GeoLocationService {

    private val accessToken = System.getenv(DOTENV_IPINFO_KEY) ?: dotenv[DOTENV_IPINFO_KEY]

    /**
     * Retrieves geographical information for the specified IP address.
     *
     * @param ip The IP address for which to obtain geographical data.
     * @return A [Mono] emitting a [GeoLocation] object containing the IP address and associated country.
     */
    override fun get(ip: String): Mono<GeoLocation> {
        val cachedValue = cache.getIfPresent(ip)

        return if (cachedValue != null) {
            Mono.fromFuture(cachedValue)
        } else {
            fetchGeoLocation(ip).doOnSuccess { result ->
                cache.put(ip, CompletableFuture.completedFuture(result))
            }
        }
    }

    fun clearCache() {
        cache.synchronous().invalidateAll()
    }

    /**
     * Fetches geographical information for the specified IP address from the IPInfo API.
     *
     * @param ip The IP address for which to fetch geographical data.
     * @return A [Mono] emitting a [GeoLocation] object containing the IP address and associated country.
     */
    private fun fetchGeoLocation(ip: String): Mono<GeoLocation> {
        val ipAddress = IpAddress(ip)
        if (!ipAddress.isValid) {
            return Mono.just(GeoLocation(ip, "Unknown"))
        }

        if (ipAddress.isBogon) {
            return Mono.just(GeoLocation(ipAddress.ip, "Bogon"))
        }

        val url = buildRequestUrl(ipAddress)

        return webClient.get()
            .uri(url)
            .retrieve()
            .bodyToMono(Map::class.java)
            .map { response ->
                val country = response?.get("country") as String? ?: "Unknown"
                GeoLocation(ipAddress.ip, country)
            }
            .onErrorResume {
                Mono.just(GeoLocation(ip, "Unknown"))
            }
    }

    /**
     * Builds the request URL for the IPInfo API using the provided IP address.
     *
     * @param ipAddress The IP address to be used in the request URL.
     * @return The complete URL string for the API request.
     */
    private fun buildRequestUrl(ipAddress: IpAddress): String {
        return "${IPINFO_BASE_URL}${ipAddress.ip}?token=${accessToken}"
    }

    companion object {
        const val DOTENV_IPINFO_KEY = "IPINFO_API_KEY"
        const val IPINFO_BASE_URL = "https://ipinfo.io/"
    }
}
