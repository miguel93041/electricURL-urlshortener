@file:Suppress("WildcardImport")
package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.*
import jakarta.servlet.http.HttpServletRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.kotlin.*
import java.io.StringReader
import java.io.StringWriter
import java.net.URI

class ProcessCsvUseCaseTest {

    private lateinit var processCsvUseCase: ProcessCsvUseCase
    private lateinit var generateEnhancedShortUrlUseCase: GenerateEnhancedShortUrlUseCase
    private lateinit var mockRequest: HttpServletRequest

    @BeforeEach
    fun setup() {
        generateEnhancedShortUrlUseCase = mock()
        processCsvUseCase = ProcessCsvUseCaseImpl(generateEnhancedShortUrlUseCase)
        mockRequest = mock()

        `when`(mockRequest.remoteAddr).thenReturn("127.0.0.1")
    }

    @Test
    fun `should process valid URLs correctly`() {
        // Given
        val inputCsv = "http://example.com\nhttp://another-example.com"
        val reader = StringReader(inputCsv)
        val writer = StringWriter()

        val shortUrl1 = ShortUrlDataOut(
            shortUrl = URI("http://short.ly/abc123"),
            qrCodeUrl = null
        )
        val shortUrl2 = ShortUrlDataOut(
            shortUrl = URI("http://short.ly/xyz789"),
            qrCodeUrl = null
        )

        `when`(generateEnhancedShortUrlUseCase.generate(any(), eq(mockRequest)))
            .thenReturn(shortUrl1)
            .thenReturn(shortUrl2)

        // When
        processCsvUseCase.processCsv(reader, writer, mockRequest, ShortUrlDataIn("", false))

        // Then
        val expectedOutput = """
            original-url,shortened-url
            http://example.com,http://short.ly/abc123
            http://another-example.com,http://short.ly/xyz789
        """.trimIndent()

        assertEquals(expectedOutput, writer.toString().trim())
    }

    @Test
    fun `should handle invalid URLs`() {
        // Given
        val inputCsv = "invalid-url\nhttp://example.com"
        val reader = StringReader(inputCsv)
        val writer = StringWriter()

        `when`(generateEnhancedShortUrlUseCase.generate(
            eq(ShortUrlDataIn("invalid-url", false)),
            eq(mockRequest)
        )).thenThrow(InvalidUrlException("Invalid URL"))

        val shortUrl = ShortUrlDataOut(
            shortUrl = URI("http://short.ly/abc123"),
            qrCodeUrl = null
        )
        `when`(generateEnhancedShortUrlUseCase.generate(
            eq(ShortUrlDataIn("http://example.com", false)),
            eq(mockRequest)
        )).thenReturn(shortUrl)

        // When
        processCsvUseCase.processCsv(reader, writer, mockRequest, ShortUrlDataIn("", false))

        // Then
        val expectedOutput = """
            original-url,shortened-url
            invalid-url,ERROR: Invalid URL,ERROR: QR not generated
            http://example.com,http://short.ly/abc123
        """.trimIndent()

        assertEquals(expectedOutput, writer.toString().trim())
    }

    @Test
    fun `should handle unsafe URLs`() {
        // Given
        val inputCsv = "http://unsafe-url.com"
        val reader = StringReader(inputCsv)
        val writer = StringWriter()

        `when`(generateEnhancedShortUrlUseCase.generate(
            eq(ShortUrlDataIn("http://unsafe-url.com", false)),
            eq(mockRequest)
        )).thenThrow(UnsafeUrlException("Unsafe URL"))

        // When
        processCsvUseCase.processCsv(reader, writer, mockRequest, ShortUrlDataIn("", false))

        // Then
        val expectedOutput = """
            original-url,shortened-url
            http://unsafe-url.com,ERROR: Unsafe URL,ERROR: QR not generated
        """.trimIndent()

        assertEquals(expectedOutput, writer.toString().trim())
    }

    @Test
    fun `should handle unreachable URLs`() {
        // Given
        val inputCsv = "http://unreachable-url.com"
        val reader = StringReader(inputCsv)
        val writer = StringWriter()

        `when`(generateEnhancedShortUrlUseCase.generate(
            eq(ShortUrlDataIn("http://unreachable-url.com", false)),
            eq(mockRequest)
        )).thenThrow(UrlUnreachableException("URL unreachable"))

        // When
        processCsvUseCase.processCsv(reader, writer, mockRequest, ShortUrlDataIn("", false))

        // Then
        val expectedOutput = """
            original-url,shortened-url
            http://unreachable-url.com,ERROR: URL unreachable,ERROR: QR not generated
        """.trimIndent()

        assertEquals(expectedOutput, writer.toString().trim())
    }

    @Test
    fun `should generate QR code URLs if requested`() {
        // Given
        val inputCsv = "http://example.com"
        val reader = StringReader(inputCsv)
        val writer = StringWriter()

        val shortUrl = ShortUrlDataOut(
            shortUrl = URI("http://short.ly/abc123"),
            qrCodeUrl = URI("http://short.ly/qr/abc123")
        )
        `when`(generateEnhancedShortUrlUseCase.generate(
            eq(ShortUrlDataIn("http://example.com", true)),
            eq(mockRequest)
        )).thenReturn(shortUrl)

        // When
        processCsvUseCase.processCsv(reader, writer, mockRequest, ShortUrlDataIn("", true))

        // Then
        val expectedOutput = """
            original-url,shortened-url,qr-code-url
            http://example.com,http://short.ly/abc123,http://short.ly/qr/abc123
        """.trimIndent()

        assertEquals(expectedOutput, writer.toString().trim())
    }
}
