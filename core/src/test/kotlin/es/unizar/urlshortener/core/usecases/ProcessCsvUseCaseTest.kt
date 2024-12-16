@file:Suppress("WildcardImport")
package es.unizar.urlshortener.core.usecases

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

    private fun createTestCase(
        inputUrls: List<String>,
        shortUrls: List<ShortUrlDataOut>,
        qrRequested: Boolean = false,
        expectedOutput: String
    ) {
        // Create a mock sequence for short URL generation
        val shortUrlGenerator = shortUrls.map { Mono.just(it) }.iterator()

        // Prepare mock service to return short URLs sequentially
        `when`(generateShortUrlService.generate(
            any<ShortUrlDataIn>(),
            any()
        )).thenAnswer {
            // Return the next short URL in the sequence
            shortUrlGenerator.next()
        }

        // Prepare input buffer
        val inputCsv = inputUrls.joinToString("\n")
        val inputBuffer = DefaultDataBufferFactory().wrap(inputCsv.toByteArray())
        val request = mock<ServerHttpRequest>()

        // Process CSV
        val result = processCsvUseCase.processCsv(Flux.just(inputBuffer), request, qrRequested)
            .map { it.asByteBuffer().array().toString(Charsets.UTF_8) }
            .collectList()
            .block()
        val resultText = result?.joinToString(separator = "")

        // Assert
        assertEquals(expectedOutput + "\n", resultText)
    }

    @Test
    fun `should process valid URLs correctly`() {
        createTestCase(
            inputUrls = listOf(URL, "http://another-example.com"),
            shortUrls = listOf(
                ShortUrlDataOut(shortUrl = URI(SHORT_URL), qrCodeUrl = null),
                ShortUrlDataOut(shortUrl = URI("http://short.ly/xyz789"), qrCodeUrl = null)
            ),
            expectedOutput = """
                http://example.com,http://short.ly/abc123,QR not generated
                http://another-example.com,http://short.ly/xyz789,QR not generated
            """.trimIndent()
        )
    }

    @Test
    fun `should generate QR code URLs if requested`() {
        createTestCase(
            inputUrls = listOf(URL),
            shortUrls = listOf(
                ShortUrlDataOut(
                    shortUrl = URI(SHORT_URL),
                    qrCodeUrl = URI("http://short.ly/qr/abc123")
                )
            ),
            qrRequested = true,
            expectedOutput = """
                http://example.com,http://short.ly/abc123,http://short.ly/qr/abc123
            """.trimIndent()
        )
    }

    @Test
    fun `should process URLs with special characters correctly`() {
        createTestCase(
            inputUrls = listOf(
                "http://example.com/?q=hello%20world",
                "http://example.com/path/to/resource"
            ),
            shortUrls = listOf(
                ShortUrlDataOut(shortUrl = URI("http://short.ly/special1"), qrCodeUrl = null),
                ShortUrlDataOut(shortUrl = URI("http://short.ly/special2"), qrCodeUrl = null)
            ),
            expectedOutput = """
                http://example.com/?q=hello%20world,http://short.ly/special1,QR not generated
                http://example.com/path/to/resource,http://short.ly/special2,QR not generated
            """.trimIndent()
        )
    }

    @Test
    fun `should process mix of http and https URLs`() {
        createTestCase(
            inputUrls = listOf(URL, "https://secure-example.com"),
            shortUrls = listOf(
                ShortUrlDataOut(shortUrl = URI(SHORT_URL), qrCodeUrl = null),
                ShortUrlDataOut(shortUrl = URI("http://short.ly/secure456"), qrCodeUrl = null)
            ),
            expectedOutput = """
                http://example.com,http://short.ly/abc123,QR not generated
                https://secure-example.com,http://short.ly/secure456,QR not generated
            """.trimIndent()
        )
    }

    companion object {
        const val URL = "http://example.com"
        const val SHORT_URL = "http://short.ly/abc123"
    }
}
