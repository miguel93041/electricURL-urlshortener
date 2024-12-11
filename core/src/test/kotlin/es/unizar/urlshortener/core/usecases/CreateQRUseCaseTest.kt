@file:Suppress("WildcardImport")
package es.unizar.urlshortener.core.usecases

import com.google.zxing.BarcodeFormat
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import es.unizar.urlshortener.core.InvalidUrlException
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.*
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CreateQRUseCaseTest {

    private val qrCodeWriter = mock(QRCodeWriter::class.java)
    private val createQRUseCase = CreateQRUseCaseImpl(qrCodeWriter)

    @Test
    fun `should create QR code successfully when given a valid URL and size`() {
        val url = "https://example.com"
        val size = 300

        val bitMatrix = BitMatrix(size, size)
        `when`(qrCodeWriter.encode(url, BarcodeFormat.QR_CODE, size, size)).thenReturn(bitMatrix)

        val qrCodeBytes = createQRUseCase.create(url, size)

        verify(qrCodeWriter).encode(url, BarcodeFormat.QR_CODE, size, size)

        assertNotNull(qrCodeBytes)
        assertTrue(qrCodeBytes.isNotEmpty())
    }

    @Test
    fun `should throw InvalidUrlException when given an empty URL`() {
        val url = ""
        val size = 300

        assertThrows<InvalidUrlException> { createQRUseCase.create(url, size) }
        verify(qrCodeWriter, never()).encode(anyString(), any(), anyInt(), anyInt())
    }

    @Test
    fun `should throw InvalidUrlException when given a null URL`() {
        val url: String? = null
        val size = 300

        assertThrows<InvalidUrlException> { createQRUseCase.create(url ?: "", size) }
        verify(qrCodeWriter, never()).encode(anyString(), any(), anyInt(), anyInt())
    }

    @Test
    fun `should not create QR code because of the size of the URL`() {
        val maxUrlSize = 2084
        val baseUrl = "https://example.com?"
        val paddingChar = "a"
        val url = baseUrl + paddingChar.repeat(maxUrlSize - baseUrl.length)
        val size = 300

        assertThrows<InvalidUrlException> { createQRUseCase.create(url, size) }

        verify(qrCodeWriter, never()).encode(anyString(), any(), anyInt(), anyInt())
    }

}
