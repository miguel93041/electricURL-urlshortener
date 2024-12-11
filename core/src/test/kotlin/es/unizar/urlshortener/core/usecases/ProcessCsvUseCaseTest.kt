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
        processCsvUseCase.processCsv(reader, writer, mockRequest, ShortUrlDataIn("", true))

        val expectedOutput = """
            original-url,shortened-url,qr-code-url
            http://example.com,http://short.ly/abc123,http://short.ly/qr/abc123
        """.trimIndent()

        assertEquals(expectedOutput, writer.toString().trim())
    }

    @Test
    fun `should process URLs with special characters correctly`() {
        val inputCsv = "http://example.com/?q=hello%20world\nhttp://example.com/path/to/resource"
        val reader = StringReader(inputCsv)
        val writer = StringWriter()

        val shortUrl1 = ShortUrlDataOut(
            shortUrl = URI("http://short.ly/special1"),
            qrCodeUrl = null
        )
        val shortUrl2 = ShortUrlDataOut(
            shortUrl = URI("http://short.ly/special2"),
            qrCodeUrl = null
        )

        `when`(generateEnhancedShortUrlUseCase.generate(any(), eq(mockRequest)))
            .thenReturn(shortUrl1)
            .thenReturn(shortUrl2)

        processCsvUseCase.processCsv(reader, writer, mockRequest, ShortUrlDataIn("", false))

        val expectedOutput = """
        original-url,shortened-url
        http://example.com/?q=hello%20world,http://short.ly/special1
        http://example.com/path/to/resource,http://short.ly/special2
    """.trimIndent()

        assertEquals(expectedOutput, writer.toString().trim())
    }

    @Test
    fun `should handle mix of valid and invalid URLs`() {
        val inputCsv = "http://valid-url.com\ninvalid-url\nhttp://another-valid-url.com"
        val reader = StringReader(inputCsv)
        val writer = StringWriter()

        val shortUrl1 = ShortUrlDataOut(
            shortUrl = URI("http://short.ly/valid1"),
            qrCodeUrl = null
        )
        val shortUrl2 = ShortUrlDataOut(
            shortUrl = URI("http://short.ly/valid2"),
            qrCodeUrl = null
        )

        `when`(generateEnhancedShortUrlUseCase.generate(
            eq(ShortUrlDataIn("http://valid-url.com", false)),
            eq(mockRequest))
        ).thenReturn(shortUrl1)

        `when`(generateEnhancedShortUrlUseCase.generate(
            eq(ShortUrlDataIn("http://another-valid-url.com", false)),
            eq(mockRequest))
        ).thenReturn(shortUrl2)

        `when`(generateEnhancedShortUrlUseCase.generate(
            eq(ShortUrlDataIn("invalid-url", false)),
            eq(mockRequest))
        ).thenThrow(InvalidUrlException("Invalid URL"))

        processCsvUseCase.processCsv(reader, writer, mockRequest, ShortUrlDataIn("", false))

        val expectedOutput = """
        original-url,shortened-url
        http://valid-url.com,http://short.ly/valid1
        invalid-url,ERROR: Invalid URL,ERROR: QR not generated
        http://another-valid-url.com,http://short.ly/valid2
    """.trimIndent()

        assertEquals(expectedOutput, writer.toString().trim())
    }

    @Test
    fun `should handle intermittent errors during processing`() {
        val inputCsv = "http://example.com\nhttp://flaky-url.com\nhttp://valid-url.com"
        val reader = StringReader(inputCsv)
        val writer = StringWriter()

        val shortUrl1 = ShortUrlDataOut(
            shortUrl = URI("http://short.ly/abc123"),
            qrCodeUrl = null
        )
        val shortUrl2 = ShortUrlDataOut(
            shortUrl = URI("http://short.ly/valid1"),
            qrCodeUrl = null
        )

        `when`(generateEnhancedShortUrlUseCase.generate(eq(ShortUrlDataIn("http://example.com", false)), eq(mockRequest)))
            .thenReturn(shortUrl1)
        `when`(generateEnhancedShortUrlUseCase.generate(eq(ShortUrlDataIn("http://flaky-url.com", false)), eq(mockRequest)))
            .thenThrow(UrlUnreachableException("Temporary failure"))
        `when`(generateEnhancedShortUrlUseCase.generate(eq(ShortUrlDataIn("http://valid-url.com", false)), eq(mockRequest)))
            .thenReturn(shortUrl2)

        processCsvUseCase.processCsv(reader, writer, mockRequest, ShortUrlDataIn("", false))

        val expectedOutput = """
        original-url,shortened-url
        http://example.com,http://short.ly/abc123
        http://flaky-url.com,ERROR: URL unreachable,ERROR: QR not generated
        http://valid-url.com,http://short.ly/valid1
    """.trimIndent()

        assertEquals(expectedOutput, writer.toString().trim())
    }

    @Test
    fun `should process mix of http and https URLs`() {
        val inputCsv = "http://example.com\nhttps://secure-example.com"
        val reader = StringReader(inputCsv)
        val writer = StringWriter()

        val shortUrl1 = ShortUrlDataOut(
            shortUrl = URI("http://short.ly/abc123"),
            qrCodeUrl = null
        )
        val shortUrl2 = ShortUrlDataOut(
            shortUrl = URI("http://short.ly/secure456"),
            qrCodeUrl = null
        )

        `when`(generateEnhancedShortUrlUseCase.generate(eq(ShortUrlDataIn("http://example.com", false)), eq(mockRequest)))
            .thenReturn(shortUrl1)
        `when`(generateEnhancedShortUrlUseCase.generate(eq(ShortUrlDataIn("https://secure-example.com", false)), eq(mockRequest)))
            .thenReturn(shortUrl2)

        processCsvUseCase.processCsv(reader, writer, mockRequest, ShortUrlDataIn("", false))

        val expectedOutput = """
        original-url,shortened-url
        http://example.com,http://short.ly/abc123
        https://secure-example.com,http://short.ly/secure456
    """.trimIndent()

        assertEquals(expectedOutput, writer.toString().trim())
    }
}
