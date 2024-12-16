package es.unizar.urlshortener.thirdparties.ipinfo

import com.github.benmanes.caffeine.cache.AsyncCache
import es.unizar.urlshortener.core.UrlSafetyService
import io.github.cdimascio.dotenv.Dotenv
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.util.concurrent.CompletableFuture

/**
 * Implementation of [UrlSafetyService] that uses Google's Safe Browsing API to
 * check if a given URL is safe or potentially malicious, with Caffeine for caching results.
 *
 * @property webClient The WebClient instance used to make HTTP requests.
 * @param dotenv The dotenv library instance for managing environment variables.
 * @param cache The Caffeine AsyncCache used to cache results of URL safety checks.
 */
class UrlSafetyServiceImpl(
    private val webClient: WebClient,
    dotenv: Dotenv,
    private val cache: AsyncCache<String, Boolean>
) : UrlSafetyService {

    private val accessToken = System.getenv(DOTENV_SAFEBROWSING_KEY) ?: dotenv[DOTENV_SAFEBROWSING_KEY]

    /**
     * Checks if a given URL is safe by first checking the cache, and if not cached,
     * querying the Google Safe Browsing API.
     *
     * @param url The URL to be checked.
     * @return A [Mono] emitting `true` if the URL is safe, `false` otherwise.
     */
    override fun isSafe(url: String): Mono<Boolean> {
        val cachedValue = cache.getIfPresent(url)
        return if (cachedValue != null) {
            Mono.fromFuture(cachedValue)
        } else {
            fetchUrlSafety(url).doOnSuccess { result ->
                cache.put(url, CompletableFuture.completedFuture(result))
            }
        }
    }

    /**
     * Clears all entries from the cache by invalidating them synchronously.
     */
    fun clearCache() {
        cache.synchronous().invalidateAll()
    }

    /**
     * Fetches the safety status of a URL from the Google Safe Browsing API.
     *
     * @param url The URL to be checked.
     * @return A [Mono] emitting `true` if the URL is safe, `false` otherwise.
     */
    private fun fetchUrlSafety(url: String): Mono<Boolean> {
        val requestUrl = buildRequestUrl()

        return webClient.post()
            .uri(requestUrl)
            .bodyValue(createRequestBody(url))
            .retrieve()
            .onStatus({ status -> status.isError }) { response ->
                response.bodyToMono(String::class.java).flatMap { errorResponse ->
                    Mono.error(RuntimeException("Error from API: $errorResponse"))
                }
            }
            .bodyToMono(Map::class.java)
            .map { response -> response.isNullOrEmpty() } // True if the response is empty (URL is safe)
            .onErrorResume { Mono.just(false) } // Assume URL is unsafe on errors
    }

    /**
     * Constructs the request body for the Safe Browsing API query.
     *
     * @param url The URL to be checked for potential threats.
     * @return A map representing the request body in the format expected by the API.
     */
    private fun createRequestBody(url: String): Map<String, Any> {
        return mapOf(
            "client" to mapOf(
                "clientId" to "shortener",
                "clientVersion" to "1.0"
            ),
            "threatInfo" to mapOf(
                "threatTypes" to listOf("MALWARE", "SOCIAL_ENGINEERING"),
                "platformTypes" to listOf("ANY_PLATFORM"),
                "threatEntryTypes" to listOf("URL"),
                "threatEntries" to listOf(mapOf("url" to url))
            )
        )
    }

    /**
     * Builds the complete URL for the Safe Browsing API request.
     *
     * @return The full API endpoint with the API key appended as a query parameter.
     */
    private fun buildRequestUrl(): String {
        return "${SAFEBROWSING_BASE_URL}v4/threatMatches:find?key=$accessToken"
    }

    companion object {
        const val DOTENV_SAFEBROWSING_KEY = "GOOGLE_API_KEY"
        const val SAFEBROWSING_BASE_URL = "https://safebrowsing.googleapis.com/"
    }
}
