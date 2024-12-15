package es.unizar.urlshortener.core.usecases

import com.github.benmanes.caffeine.cache.AsyncCache
import org.junit.jupiter.api.BeforeEach
import org.mockito.Mockito.*
import org.springframework.web.reactive.function.client.WebClient
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
}
