package es.unizar.urlshortener.core.usecases

import com.github.benmanes.caffeine.cache.AsyncCache
import org.junit.jupiter.api.BeforeEach
import org.mockito.Mockito.*
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.util.concurrent.CompletableFuture
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UrlAccessibilityCheckUseCaseTest {

    private lateinit var webClient: WebClient
    private lateinit var cache: AsyncCache<String, Boolean>
    private lateinit var useCase: UrlAccessibilityCheckUseCaseImpl

    @BeforeEach
    fun setUp() {
        webClient = mock(WebClient::class.java)
        cache = mock(AsyncCache::class.java) as AsyncCache<String, Boolean>
        useCase = UrlAccessibilityCheckUseCaseImpl(webClient, cache)
    }

    @Test
    fun `should return true if URL is reachable and cached`() {
        val url = "http://example.com"
        val cachedResult = CompletableFuture.completedFuture(true)
        `when`(cache.getIfPresent(url)).thenReturn(cachedResult)

        val result = useCase.isUrlReachable(url).block()

        assertTrue(result!!)
    }

    @Test
    fun `should return false if URL is not reachable and cached result is false`() {
        val url = "http://example.com"
        val cachedResult = CompletableFuture.completedFuture(false)
        `when`(cache.getIfPresent(url)).thenReturn(cachedResult)

        val result = useCase.isUrlReachable(url).block()

        assertFalse(result!!)
    }

    @Test
    fun `should perform HTTP GET request if URL is not cached`() {
        val url = "http://example.com"
        `when`(cache.getIfPresent(url)).thenReturn(null)

        `when`(webClient.get()).thenReturn(webClient.get())
        `when`(webClient.get().uri(url)).thenReturn(webClient.get())
        `when`(webClient.get().retrieve()).thenReturn(webClient.get().retrieve())
        `when`(webClient.get().retrieve().bodyToMono(Boolean::class.java)).thenReturn(Mono.just(true))

        val result = useCase.isUrlReachable(url).block()

        assertTrue(result!!)
    }

    @Test
    fun `should return false if HTTP GET request fails`() {
        val url = "http://example.com"
        `when`(cache.getIfPresent(url)).thenReturn(null)

        `when`(webClient.get()).thenReturn(webClient.get())
        `when`(webClient.get().uri(url)).thenReturn(webClient.get())
        `when`(webClient.get().retrieve()).thenReturn(webClient.get().retrieve())
        `when`(
            webClient.get().retrieve().bodyToMono(Boolean::class.java)
        ).thenReturn(Mono.error(RuntimeException("Error")))

        val result = useCase.isUrlReachable(url).block()

        assertFalse(result!!)
    }

    @Test
    fun `should cache the result after checking URL accessibility`() {
        val url = "http://example.com"
        `when`(cache.getIfPresent(url)).thenReturn(null)

        `when`(webClient.get()).thenReturn(webClient.get())
        `when`(webClient.get().uri(url)).thenReturn(webClient.get())
        `when`(webClient.get().retrieve()).thenReturn(webClient.get().retrieve())
        `when`(webClient.get().retrieve().bodyToMono(Boolean::class.java)).thenReturn(Mono.just(true))

        val result = useCase.isUrlReachable(url).block()

        assertTrue(result!!)
        verify(cache).put(eq(url), any())
    }
}
