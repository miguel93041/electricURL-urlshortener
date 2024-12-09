package es.unizar.urlshortener.thirdparties.ipinfo


import es.unizar.urlshortener.core.UrlSafetyService
import io.github.cdimascio.dotenv.Dotenv
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

/**
 * Implementation of [UrlSafetyService] that uses Google's Safe Browsing API to
 * check if a given URL is safe or potentially malicious.
 *
 * @property webClient The WebClient instance used to make HTTP requests.
 * @param dotenv The dotenv library instance for managing environment variables.
 */
class UrlSafetyServiceImpl (
    private val webClient: WebClient,
    dotenv: Dotenv
) : UrlSafetyService {

    private val accessToken = System.getenv(DOTENV_SAFEBROWSING_KEY) ?: dotenv[DOTENV_SAFEBROWSING_KEY]

    /**
     * Checks if a given URL is safe by querying the Google Safe Browsing API.
     *
     * @param url The URL to be checked.
     * @return True if the URL is safe or not present in the threat database, false otherwise.
     * @throws RuntimeException if there is an error response from the API.
     */
    @Suppress("TooGenericExceptionThrown")
    override fun isSafe(url: String): Mono<Boolean> {
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
            .map { response -> response.isNullOrEmpty() } // Returns true if response is empty (URL is safe)
            .onErrorResume { Mono.just(false) } // In case of an error, assume URL is unsafe
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
