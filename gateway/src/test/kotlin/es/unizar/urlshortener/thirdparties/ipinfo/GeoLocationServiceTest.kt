@file:Suppress("ForbiddenComment")
package es.unizar.urlshortener.thirdparties.ipinfo

import com.github.benmanes.caffeine.cache.AsyncCache
import es.unizar.urlshortener.core.GeoLocation
import es.unizar.urlshortener.core.GeoLocationService
import io.github.cdimascio.dotenv.Dotenv
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.http.HttpHeaders
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono

class GeoLocationServiceTest {

    private var webClient: WebClient = Mockito.mock(WebClient::class.java)

    private var dotenv: Dotenv = Mockito.mock(Dotenv::class.java)

    private var geoLocationService: GeoLocationService = GeoLocationServiceImpl(webClient, dotenv, mockCache())

    @BeforeEach
    fun setup() {
        Mockito.`when`(dotenv[GeoLocationServiceImpl.DOTENV_IPINFO_KEY]).thenReturn("test-token")
    }

    @Test
    fun `should return GeoLocation when API returns valid response for IPv4`() {
        val response = mapOf("ip" to "123.123.123.123", "country" to "ES")
        mockWebClientResponse(response)

        val result = geoLocationService.get("123.123.123.123").block()
        assertEquals("123.123.123.123", result?.ip)
        assertEquals("ES", result?.country)
    }

    @Test
    fun `should return GeoLocation when API returns valid response for IPv6`() {
        val ip = "2606:2800:220:1:248:1893:25c8:1946";
        val response = mapOf("ip" to ip, "country" to "ES")
        mockWebClientResponse(response)

        val result = geoLocationService.get(ip).block()

        assertEquals(ip, result?.ip)
        assertEquals("ES", result?.country)
    }

    @Test
    fun `should return GeoLocation with country Bogon for bogon IPv4 address`() {
        val result = geoLocationService.get("127.0.0.1").block()

        assertEquals("127.0.0.1", result?.ip)
        assertEquals("Bogon", result?.country)
    }

    @Test
    fun `should return GeoLocation with country Bogon for bogon IPv6 address`() {
        val result = geoLocationService.get("::1").block()

        assertEquals("::1", result?.ip)
        assertEquals("Bogon", result?.country)
    }

    @Test
    fun `should return GeoLocation with country Unknown for invalid IP`() {
        val invalidIp = "999.999.999.999"

        val result = geoLocationService.get(invalidIp).block()

        assertEquals("999.999.999.999", result?.ip)
        assertEquals("Unknown", result?.country)
    }

    @Test
    fun `should return GeoLocation with country Unknown when API returns an error`() {
        mockWebClientError()

        val result = geoLocationService.get("123.123.123.123").block()

        assertEquals("123.123.123.123", result?.ip)
        assertEquals("Unknown", result?.country)
    }

    @Test
    fun `should return GeoLocation with country Unknown when API key is wrong`() {
        mockWebClientInvalidApiKey()

        val result = geoLocationService.get("123.123.123.123").block()

        assertEquals("123.123.123.123", result?.ip)
        assertEquals("Unknown", result?.country)
    }

    private fun mockWebClientResponse(response: Map<String, Any>) {
        val requestHeadersUriSpec = Mockito.mock(WebClient.RequestHeadersUriSpec::class.java)
        val requestHeadersSpec = Mockito.mock(WebClient.RequestHeadersSpec::class.java)
        val responseSpec = Mockito.mock(WebClient.ResponseSpec::class.java)

        Mockito.`when`(webClient.get()).thenReturn(requestHeadersUriSpec)
        Mockito.`when`(requestHeadersUriSpec.uri(Mockito.anyString())).thenReturn(requestHeadersSpec)
        Mockito.`when`(requestHeadersSpec.retrieve()).thenReturn(responseSpec)
        Mockito.`when`(responseSpec.bodyToMono(Map::class.java)).thenReturn(Mono.just(response))
    }

    private fun mockCache(): AsyncCache<String, GeoLocation> {
        val cache = Mockito.mock(AsyncCache::class.java) as AsyncCache<String, GeoLocation>
        Mockito.`when`(cache.getIfPresent(Mockito.anyString())).thenReturn(null)
        return cache
    }

    private fun mockWebClientError() {
        val requestHeadersUriSpec = Mockito.mock(WebClient.RequestHeadersUriSpec::class.java)
        val requestHeadersSpec = Mockito.mock(WebClient.RequestHeadersSpec::class.java)
        val responseSpec = Mockito.mock(WebClient.ResponseSpec::class.java)

        Mockito.`when`(webClient.get()).thenReturn(requestHeadersUriSpec)
        Mockito.`when`(requestHeadersUriSpec.uri(Mockito.anyString())).thenReturn(requestHeadersSpec)
        Mockito.`when`(requestHeadersSpec.retrieve()).thenReturn(responseSpec)
        Mockito.`when`(responseSpec.bodyToMono(Map::class.java))
            .thenReturn(Mono.error(WebClientResponseException.create(
                404,
                "Not Found",
                HttpHeaders.EMPTY,
                ByteArray(0), null)))
    }

    private fun mockWebClientInvalidApiKey() {
        val requestHeadersUriSpec = Mockito.mock(WebClient.RequestHeadersUriSpec::class.java)
        val requestHeadersSpec = Mockito.mock(WebClient.RequestHeadersSpec::class.java)
        val responseSpec = Mockito.mock(WebClient.ResponseSpec::class.java)

        Mockito.`when`(webClient.get()).thenReturn(requestHeadersUriSpec)
        Mockito.`when`(requestHeadersUriSpec.uri(Mockito.anyString())).thenReturn(requestHeadersSpec)
        Mockito.`when`(requestHeadersSpec.retrieve()).thenReturn(responseSpec)
        Mockito.`when`(responseSpec.bodyToMono(Map::class.java))
            .thenReturn(Mono.error(WebClientResponseException.create(
                401,
                "Unauthorized",
                HttpHeaders.EMPTY,
                ByteArray(0), null)))
    }
}
