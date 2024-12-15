@file:Suppress("WildcardImport")
package es.unizar.urlshortener.core.usecases

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import es.unizar.urlshortener.core.*
import es.unizar.urlshortener.core.services.GenerateShortUrlService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.kotlin.*
import org.springframework.core.io.buffer.DefaultDataBufferFactory
import org.springframework.http.server.reactive.ServerHttpRequest
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.net.URI

class ProcessCsvUseCaseTest {

    private lateinit var processCsvUseCase: ProcessCsvUseCase
    private lateinit var generateShortUrlService: GenerateShortUrlService

    @BeforeEach
    fun setup() {
        generateShortUrlService = mock()
        processCsvUseCase = ProcessCsvUseCaseImpl(generateShortUrlService)
    }

    @Test
    fun `should process valid URLs correctly`() {
        val inputCsv = "http://example.com\nhttp://another-example.com"

        val shortUrl1 = ShortUrlDataOut(
            shortUrl = URI("http://short.ly/abc123"),
            qrCodeUrl = null
        )
        val shortUrl2 = ShortUrlDataOut(
            shortUrl = URI("http://short.ly/xyz789"),
            qrCodeUrl = null
        )

        `when`(generateShortUrlService.generate(any(), any()))
            .thenReturn(
                Mono.just(shortUrl1),
                Mono.just(shortUrl2)
        )

        val inputBuffer = DefaultDataBufferFactory().wrap(inputCsv.toByteArray())
        val request = mock<ServerHttpRequest>()

        val result = processCsvUseCase.processCsv(Flux.just(inputBuffer), request, qrRequested = false)
            .map { it.asByteBuffer().array().toString(Charsets.UTF_8) }
            .collectList()
            .block()
        val resultText = result?.joinToString(separator = "")

        val expectedOutput = """
            http://example.com,http://short.ly/abc123,QR not generated
            http://another-example.com,http://short.ly/xyz789,QR not generated
        """.trimIndent() + "\n"

        assertEquals(expectedOutput, resultText)
    }

    @Test
    fun `should generate QR code URLs if requested`() {
        val inputCsv = "http://example.com"

        val shortUrl = ShortUrlDataOut(
            shortUrl = URI("http://short.ly/abc123"),
            qrCodeUrl = URI("http://short.ly/qr/abc123")
        )

        `when`(generateShortUrlService.generate(any(), any()))
            .thenReturn(Mono.just(shortUrl))

        val inputBuffer = DefaultDataBufferFactory().wrap(inputCsv.toByteArray())
        val request = mock<ServerHttpRequest>()

        val result = processCsvUseCase.processCsv(Flux.just(inputBuffer), request, qrRequested = false)
            .map { it.asByteBuffer().array().toString(Charsets.UTF_8) }
            .collectList()
            .block()
        val resultText = result?.joinToString(separator = "")

        val expectedOutput = """
            http://example.com,http://short.ly/abc123,http://short.ly/qr/abc123
        """.trimIndent() + "\n"

        assertEquals(expectedOutput, resultText)
    }

    @Test
    fun `should process URLs with special characters correctly`() {
        val inputCsv = "http://example.com/?q=hello%20world\nhttp://example.com/path/to/resource"

        val shortUrl1 = ShortUrlDataOut(
            shortUrl = URI("http://short.ly/special1"),
            qrCodeUrl = null
        )
        val shortUrl2 = ShortUrlDataOut(
            shortUrl = URI("http://short.ly/special2"),
            qrCodeUrl = null
        )

        `when`(generateShortUrlService.generate(any(), any()))
            .thenReturn(
                Mono.just(shortUrl1),
                Mono.just(shortUrl2)
            )

        val inputBuffer = DefaultDataBufferFactory().wrap(inputCsv.toByteArray())
        val request = mock<ServerHttpRequest>()

        val result = processCsvUseCase.processCsv(Flux.just(inputBuffer), request, qrRequested = false)
            .map { it.asByteBuffer().array().toString(Charsets.UTF_8) }
            .collectList()
            .block()
        val resultText = result?.joinToString(separator = "")

        val expectedOutput = """
            http://example.com/?q=hello%20world,http://short.ly/special1,QR not generated
            http://example.com/path/to/resource,http://short.ly/special2,QR not generated
        """.trimIndent() + "\n"

        assertEquals(expectedOutput, resultText)
    }

    @Test
    fun `should process mix of http and https URLs`() {
        val inputCsv = "http://example.com\nhttps://secure-example.com"

        val shortUrl1 = ShortUrlDataOut(
            shortUrl = URI("http://short.ly/abc123"),
            qrCodeUrl = null
        )
        val shortUrl2 = ShortUrlDataOut(
            shortUrl = URI("http://short.ly/secure456"),
            qrCodeUrl = null
        )

        `when`(generateShortUrlService.generate(any(), any()))
            .thenReturn(
                Mono.just(shortUrl1),
                Mono.just(shortUrl2)
            )

        val inputBuffer = DefaultDataBufferFactory().wrap(inputCsv.toByteArray())
        val request = mock<ServerHttpRequest>()

        val result = processCsvUseCase.processCsv(Flux.just(inputBuffer), request, qrRequested = false)
            .map { it.asByteBuffer().array().toString(Charsets.UTF_8) }
            .collectList()
            .block()
        val resultText = result?.joinToString(separator = "")

        val expectedOutput = """
            http://example.com,http://short.ly/abc123,QR not generated
            https://secure-example.com,http://short.ly/secure456,QR not generated
        """.trimIndent() + "\n"

        assertEquals(expectedOutput, resultText)
    }
}
