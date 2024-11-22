package es.unizar.urlshortener.core.usecases

import org.springframework.web.reactive.function.client.WebClient

/**
 * Interface for checking the accessibility of a given URL.
 */
interface UrlAccessibilityCheckUseCase {
    /**
     * Verifies if a URL is reachable.
     *
     * @param url The URL to check.
     * @return True if the URL is reachable, false otherwise.
     * @throws Exception If an error occurs during the request, the method will catch it and return false.
     */
    fun isUrlReachable(url: String): Boolean
}

/**
 * Implementation of [UrlAccessibilityCheckUseCase].
 *
 * Attempts to access a URL using an HTTP GET request through the provided [WebClient] instance.
 * If the request is successful, it returns `true`. If the URL is unreachable or an error occurs during the request,
 * it returns `false`.
 *
 * @property webClient The [WebClient] used to send the HTTP request.
 */
@Suppress("TooGenericExceptionCaught", "SwallowedException")
class UrlAccessibilityCheckUseCaseImpl(
    private val webClient: WebClient
) : UrlAccessibilityCheckUseCase {
    /**
     * Verifies if a URL is reachable by making a GET request to the URL.
     *
     * @param url The URL to check.
     * @return True if the URL is reachable, false otherwise.
     * @throws Exception If an error occurs during the request, the method will catch it and return false.
     */
    override fun isUrlReachable(url: String): Boolean {
        return try {
            webClient.get()
                .uri(url)
                .retrieve()
                .toBodilessEntity()
                .block()

            true
        } catch (e: Exception) {
            false
        }
    }
}
