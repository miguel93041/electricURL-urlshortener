package es.unizar.urlshortener.core.usecases

import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

/**
 * Interface for checking the accessibility of a given URL.
 */
interface UrlAccessibilityCheckUseCase {
    /**
     * Verifies if a URL is reachable.
     *
     * @param url The URL to check.
     * @return A [Mono] emitting true if the URL is reachable, false otherwise.
     */
    fun isUrlReachable(url: String): Mono<Boolean>
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
     * @return A [Mono] emitting true if the URL is reachable, false otherwise.
     */
    override fun isUrlReachable(url: String): Mono<Boolean> {
        return webClient.get()
            .uri(url)
            .retrieve()
            .toBodilessEntity()
            .map { true }
            .onErrorResume {
                Mono.just(false)
            }
    }
}
