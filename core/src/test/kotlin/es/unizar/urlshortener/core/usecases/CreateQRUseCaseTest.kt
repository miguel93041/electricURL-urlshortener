@file:Suppress("WildcardImport")

package es.unizar.urlshortener.core.usecases

import com.google.zxing.BarcodeFormat
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.*
import org.mockito.kotlin.whenever
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
}
