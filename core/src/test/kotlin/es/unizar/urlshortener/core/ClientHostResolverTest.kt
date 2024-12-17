package es.unizar.urlshortener.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.http.HttpCookie
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.server.RequestPath
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.http.server.reactive.SslInfo
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import reactor.core.publisher.Flux
import java.net.InetSocketAddress
import java.net.URI

class ClientHostResolverTest {

    @Test
    fun `resolve should return X-Real-IP if present`() {
        val request = mockServerHttpRequest(
            headers = HttpHeaders().apply { set("X-Real-IP", "192.168.1.1") }
        )

        val result = ClientHostResolver.resolve(request)

        assertEquals("192.168.1.1", result)
    }

    @Test
    fun `resolve should return first IP from X-Forwarded-For if present`() {
        val request = mockServerHttpRequest(
            headers = HttpHeaders().apply { set("X-Forwarded-For", "203.0.113.195, 70.41.3.18") }
        )

        val result = ClientHostResolver.resolve(request)

        assertEquals("203.0.113.195", result)
    }

    @Test
    fun `resolve should return remote address if no headers are present`() {
        val request = mockServerHttpRequest(remoteAddress = InetSocketAddress("10.0.0.1", 8080))

        val result = ClientHostResolver.resolve(request)

        assertEquals("10.0.0.1", result)
    }

    @Test
    fun `resolve should prioritize X-Real-IP over X-Forwarded-For and remote address`() {
        val request = mockServerHttpRequest(
            headers = HttpHeaders().apply {
                set("X-Real-IP", "192.168.1.1")
                set("X-Forwarded-For", "203.0.113.195, 70.41.3.18")
            },
            remoteAddress = InetSocketAddress("10.0.0.1", 8080)
        )

        val result = ClientHostResolver.resolve(request)

        assertEquals("192.168.1.1", result)
    }

    @Test
    fun `resolve should return null if no IP information is available`() {
        val request = mockServerHttpRequest()

        val result = ClientHostResolver.resolve(request)

        assertEquals(null, result)
    }

    // Helper function to mock ServerHttpRequest
    private fun mockServerHttpRequest(
        headers: HttpHeaders = HttpHeaders(),
        remoteAddress: InetSocketAddress? = null
    ): ServerHttpRequest {
        return object : ServerHttpRequest {
            override fun getHeaders(): HttpHeaders = headers
            override fun getRemoteAddress(): InetSocketAddress? = remoteAddress
            override fun getURI(): URI = URI("http://localhost")
            override fun getMethod(): HttpMethod = HttpMethod.GET
            override fun getId(): String = "mock-id"
            override fun getPath(): RequestPath = RequestPath.parse("/mock", null)
            override fun getQueryParams(): MultiValueMap<String, String> = LinkedMultiValueMap()
            override fun getCookies(): MultiValueMap<String, HttpCookie> = LinkedMultiValueMap()
            override fun getBody(): Flux<DataBuffer> = Flux.empty()
            override fun getSslInfo(): SslInfo? = null
        }
    }
}
