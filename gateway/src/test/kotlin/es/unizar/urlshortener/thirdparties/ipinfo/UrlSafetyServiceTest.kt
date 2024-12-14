package es.unizar.urlshortener.thirdparties.ipinfo

import es.unizar.urlshortener.core.UrlSafetyService
import io.github.cdimascio.dotenv.Dotenv
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.Cacheable
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.concurrent.ConcurrentMapCache
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.test.context.ContextConfiguration
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@ExtendWith(MockitoExtension::class)
@ContextConfiguration
class UrlSafetyServiceTest {

    interface MyUrlSafetyService : UrlSafetyService {
        @Cacheable("urlSafe")
        override fun isSafe(url: String): Boolean
    }

    @Configuration
    @EnableCaching
    class Config {
        @Bean
        fun cacheManager() = ConcurrentMapCache("urlSafe")

        @Bean
        fun myUrlSafetyService() = Mockito.mock(MyUrlSafetyService::class)
    }

    @Mock
    private lateinit var webClient: WebClient

    @Suppress("UnusedPrivateProperty")
    @Mock
    private lateinit var webClientBuilder: WebClient.Builder

    @Suppress("UnusedPrivateProperty")
    @Mock
    private lateinit var requestBodyUriSpec: WebClient.RequestBodyUriSpec

    @Suppress("UnusedPrivateProperty")
    @Mock
    private lateinit var responseSpec: WebClient.ResponseSpec

    @Suppress("UnusedPrivateProperty")
    @Mock
    private lateinit var dotenv: Dotenv

    @Mock
    private lateinit var cacheManager: CacheManager

    @InjectMocks
    private lateinit var urlSafetyService: UrlSafetyServiceImpl

    private val testUrl = "http://example.com"

    @Test
    fun `isSafe should return true when API response has no matches`() {
        val responseBody = emptyMap<String, Any>()
        mockWebClientPostResponse(responseBody)

        val result = urlSafetyService.isSafe(testUrl)

        assertTrue(result)
    }

    @Test
    fun `isSafe should return false when API response contains matches`() {
        val responseBody = mapOf("matches" to listOf(mapOf("threatType" to "MALWARE")))
        mockWebClientPostResponse(responseBody)

        val result = urlSafetyService.isSafe(testUrl)

        assertFalse(result)
    }

    @Test
    fun `isSafe should cache result`() {
        val responseBody = emptyMap<String, Any>()
        mockWebClientPostResponse(responseBody)

        val cache = ConcurrentMapCache("urlSafe")
        Mockito.`when`(cacheManager.getCache("urlSafe")).thenReturn(cache)

        val firstCallResult = urlSafetyService.isSafe(testUrl)
        Mockito.verify(webClient, Mockito.times(1)).post()

        assertNotNull(cache.get(testUrl))

        val secondCallResult = urlSafetyService.isSafe(testUrl)
        Mockito.verify(webClient, Mockito.times(1)).post()

        assertTrue(firstCallResult)
        assertTrue(secondCallResult)
    }

    private fun mockWebClientPostResponse(response: Map<String, Any>) {
        val requestBodyUriSpec = Mockito.mock(WebClient.RequestBodyUriSpec::class.java)
        val requestBodySpec = Mockito.mock(WebClient.RequestBodySpec::class.java)
        val responseSpec = Mockito.mock(WebClient.ResponseSpec::class.java)

        Mockito.`when`(webClient.post()).thenReturn(requestBodyUriSpec)

        Mockito.`when`(requestBodyUriSpec.uri(Mockito.anyString())).thenReturn(requestBodySpec)

        Mockito.`when`(requestBodySpec.bodyValue(Mockito.any())).thenReturn(requestBodySpec)

        Mockito.`when`(requestBodySpec.retrieve()).thenReturn(responseSpec)

        Mockito.`when`(responseSpec.onStatus(Mockito.any(), Mockito.any())).thenReturn(responseSpec)

        Mockito.`when`(responseSpec.bodyToMono(Map::class.java)).thenReturn(Mono.just(response))
    }

}
