package es.unizar.urlshortener.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.kotlin.mock
import org.springframework.http.HttpHeaders
import org.springframework.http.server.reactive.ServerHttpRequest
import java.net.URI

class BaseUrlProviderImplTest {

    private lateinit var baseUrlProvider: BaseUrlProvider

    @BeforeEach
    fun setUp() {
        baseUrlProvider = BaseUrlProviderImpl()
    }

    @Test
    fun `get should return correct URL when no forwarded headers are present`() {
        val mockRequest = mock<ServerHttpRequest> {
            `when`(it.uri).thenReturn(URI("http://localhost:8080"))
            `when`(it.headers).thenReturn(HttpHeaders())
        }

        val result = baseUrlProvider.get(mockRequest)

        assertEquals("http://localhost:8080", result)
    }

    @Test
    fun `get should use X-Forwarded-Proto header if present`() {
        val headers = HttpHeaders().apply {
            set("X-Forwarded-Proto", "https")
        }
        val mockRequest = mock<ServerHttpRequest> {
            `when`(it.uri).thenReturn(URI("http://localhost:8080"))
            `when`(it.headers).thenReturn(headers)
        }

        val result = baseUrlProvider.get(mockRequest)

        assertEquals("https://localhost:8080", result)
    }

    @Test
    fun `get should use X-Forwarded-Host header if present`() {
        val headers = HttpHeaders().apply {
            set("X-Forwarded-Host", "example.com")
        }
        val mockRequest = mock<ServerHttpRequest> {
            `when`(it.uri).thenReturn(URI("http://localhost:8080"))
            `when`(it.headers).thenReturn(headers)
        }

        val result = baseUrlProvider.get(mockRequest)

        assertEquals("http://example.com:8080", result)
    }

    @Test
    fun `get should use X-Forwarded-Port header if present`() {
        val headers = HttpHeaders().apply {
            set("X-Forwarded-Port", "443")
        }
        val mockRequest = mock<ServerHttpRequest> {
            `when`(it.uri).thenReturn(URI("http://localhost:8080"))
            `when`(it.headers).thenReturn(headers)
        }

        val result = baseUrlProvider.get(mockRequest)

        assertEquals("http://localhost", result) // 443 removes default port
    }

    @Test
    fun `get should handle all forwarded headers together`() {
        val headers = HttpHeaders().apply {
            set("X-Forwarded-Proto", "https")
            set("X-Forwarded-Host", "example.com")
            set("X-Forwarded-Port", "8443")
        }
        val mockRequest = mock<ServerHttpRequest> {
            `when`(it.uri).thenReturn(URI("http://localhost:8080"))
            `when`(it.headers).thenReturn(headers)
        }

        val result = baseUrlProvider.get(mockRequest)

        assertEquals("https://example.com:8443", result)
    }

    @Test
    fun `get should return URL without default port 80 for HTTP`() {
        val headers = HttpHeaders().apply {
            set("X-Forwarded-Proto", "http")
            set("X-Forwarded-Host", "example.com")
            set("X-Forwarded-Port", "80")
        }
        val mockRequest = mock<ServerHttpRequest> {
            `when`(it.uri).thenReturn(URI("http://localhost:8080"))
            `when`(it.headers).thenReturn(headers)
        }

        val result = baseUrlProvider.get(mockRequest)

        assertEquals("http://example.com", result)
    }

    @Test
    fun `get should return URL without default port 443 for HTTPS`() {
        val headers = HttpHeaders().apply {
            set("X-Forwarded-Proto", "https")
            set("X-Forwarded-Host", "example.com")
            set("X-Forwarded-Port", "443")
        }
        val mockRequest = mock<ServerHttpRequest> {
            `when`(it.uri).thenReturn(URI("http://localhost:8080"))
            `when`(it.headers).thenReturn(headers)
        }

        val result = baseUrlProvider.get(mockRequest)

        assertEquals("https://example.com", result)
    }
}
