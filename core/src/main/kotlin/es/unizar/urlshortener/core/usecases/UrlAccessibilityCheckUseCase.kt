package es.unizar.urlshortener.core.usecases

import com.github.benmanes.caffeine.cache.AsyncCache
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.util.concurrent.CompletableFuture

/**
 * Interface for checking the accessibility of a given URL.
 */
fun interface UrlAccessibilityCheckUseCase {

    /**
     * Verifies if a URL is reachable.
     *
     * @param url The URL to check.
     * @return A [Mono] emitting true if the URL is reachable, false otherwise.
     */
    fun isUrlReachable(url: String): Mono<Boolean>
}

/**
 * [UrlAccessibilityCheckUseCaseImpl] is an implementation of [UrlAccessibilityCheckUseCase].
 *
 * Attempts to access a URL using an HTTP GET request through the provided [WebClient] instance.
 * If the request is successful, it returns `true`. If the URL is unreachable or an error occurs during the request,
 * it returns `false`.
 *
 * @property webClient The [WebClient] used to send the HTTP request.
 * @property cache The [AsyncCache] used for storing URL reachability results.
 */
@Suppress("TooGenericExceptionCaught", "SwallowedException")
class UrlAccessibilityCheckUseCaseImpl(
    private val webClient: WebClient,
    private val cache: AsyncCache<String, Boolean>
) : UrlAccessibilityCheckUseCase {

    /**
     * Verifies if a URL is reachable by checking the cache first and falling back to an HTTP GET request if necessary.
     *
     * @param url The URL to check.
     * @return A [Mono] emitting true if the URL is reachable, false otherwise.
     */
    override fun isUrlReachable(url: String): Mono<Boolean> {
        val cachedValue = cache.getIfPresent(url)

        return if (cachedValue != null) {
            Mono.fromFuture(cachedValue)
        } else {
            fetchUrlAccessibility(url).doOnSuccess { result ->
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
     * Performs an HTTP HEAD request to verify if the URL is reachable.
     *
     * @param url The URL to check.
     * @return A [Mono] emitting true if the URL is reachable, false otherwise.
     */
    private fun fetchUrlAccessibility(url: String): Mono<Boolean> {
        return webClient.head()
            .uri(url)
            .retrieve()
            .toBodilessEntity()
            .map { true }
            .onErrorResume {
                Mono.just(false)
            }
    }
}
