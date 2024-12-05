@file:Suppress("ForbiddenComment")
package es.unizar.urlshortener.thirdparties.ipinfo

import es.unizar.urlshortener.core.GeoLocationService
import io.github.cdimascio.dotenv.Dotenv
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono

// TODO: Test if API key is wrong
// TODO: Test ip4 and ip6 independently
class GeoLocationServiceTest {

    private var webClient: WebClient = Mockito.mock(WebClient::class.java)

    private var dotenv: Dotenv = Mockito.mock(Dotenv::class.java)

    private var geoLocationService: GeoLocationService = GeoLocationServiceImpl(webClient, dotenv)

    @BeforeEach
    fun setup() {
        Mockito.`when`(dotenv[GeoLocationServiceImpl.DOTENV_IPINFO_KEY]).thenReturn("test-token")
    }

    @Test
    fun `should return GeoLocation when API returns valid response`() {
        val response = mapOf("ip" to "123.123.123.123", "country" to "ES")
        mockWebClientResponse(response)

        val result = geoLocationService.get("123.123.123.123")
        assertEquals("123.123.123.123", result.ip)
        assertEquals("ES", result.country)
    }

    @Test
    fun `should return GeoLocation with country Bogon for bogon IPv4 address`() {
        val result = geoLocationService.get("127.0.0.1")
        assertEquals("127.0.0.1", result.ip)
        assertEquals("Bogon", result.country)
    }

    @Test
    fun `should return GeoLocation with country Bogon for bogon IPv6 address`() {
        val result = geoLocationService.get("::1")
        assertEquals("::1", result.ip)
        assertEquals("Bogon", result.country)
    }

    @Test
    fun `should throw exception when IP is invalid`() {
        val invalidIp = "999.999.999.999" // IP incorrect

        assertThrows<IllegalArgumentException> {
            geoLocationService.get(invalidIp)
        }
    }

    @Test
    fun `should throw exception when API returns an error`() {
        val requestHeadersUriSpec = Mockito.mock(WebClient.RequestHeadersUriSpec::class.java)
        Mockito.`when`(webClient.get()).thenReturn(requestHeadersUriSpec)
        Mockito.`when`(requestHeadersUriSpec.uri(Mockito.anyString()))
            .thenThrow(WebClientResponseException.create(404, "Not Found", null, null, null))

        assertThrows<WebClientResponseException> {
            geoLocationService.get("123.123.123.123")
        }
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
}
