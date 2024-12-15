package es.unizar.urlshortener.thirdparties.ipinfo

import com.github.benmanes.caffeine.cache.AsyncCache
import io.github.cdimascio.dotenv.Dotenv
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.util.concurrent.CompletableFuture
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@ExtendWith(MockitoExtension::class)
class UrlSafetyServiceTest {

    @Mock
    private lateinit var webClient: WebClient

    @Suppress("UnusedPrivateProperty")
    @Mock
    private lateinit var dotenv: Dotenv

    @Mock
    private lateinit var cache: AsyncCache<String, Boolean>

    @InjectMocks
    private lateinit var urlSafetyService: UrlSafetyServiceImpl

    private val testUrl = "http://example.com"

    @Test
    fun `isSafe should return true when API response has no matches`() {
        val responseBody = emptyMap<String, Any>()
        mockWebClientPostResponse(responseBody)

        Mockito.`when`(cache.getIfPresent(testUrl)).thenReturn(null)

        val result = urlSafetyService.isSafe(testUrl)

        assertTrue(result.block() ?: false)
    }

    @Test
    fun `isSafe should return false when API response contains matches`() {
        val responseBody = mapOf("matches" to listOf(mapOf("threatType" to "MALWARE")))
        mockWebClientPostResponse(responseBody)

        Mockito.`when`(cache.getIfPresent(testUrl)).thenReturn(null)

        val result = urlSafetyService.isSafe(testUrl)

        assertFalse(result.block() ?: true)
    }

    @Test
    fun `isSafe should cache result`() {
        val responseBodyFirstCall = emptyMap<String, Any>()
        mockWebClientPostResponse(responseBodyFirstCall)
        Mockito.`when`(cache.getIfPresent(testUrl)).thenReturn(null)

        val resultFirstCall = urlSafetyService.isSafe(testUrl)
        Mockito.verify(webClient).post()

        Mockito.`when`(cache.getIfPresent(testUrl)).thenReturn(CompletableFuture.completedFuture(true))

        val resultSecondCall = urlSafetyService.isSafe(testUrl)
        Mockito.verify(webClient).post()

        assertTrue(resultFirstCall.block() ?: false)
        assertTrue(resultSecondCall.block() ?: false)
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
