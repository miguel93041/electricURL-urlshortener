@file:Suppress("WildcardImport")

package es.unizar.urlshortener.core.usecases

import com.google.zxing.BarcodeFormat
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import org.junit.jupiter.api.Assertions.assertEquals
import org.mockito.Mockito.*
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO
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
    fun `should generate a PNG format QR code`() {
        val url = "https://example.com"
        val size = 300

        val bitMatrix = BitMatrix(size, size)
        `when`(qrCodeWriter.encode(url, BarcodeFormat.QR_CODE, size, size)).thenReturn(bitMatrix)

        val qrCodeBytes = createQRUseCase.create(url, size)

        val inputStream = ByteArrayInputStream(qrCodeBytes)
        val bufferedImage: BufferedImage = ImageIO.read(inputStream)

        assertNotNull(bufferedImage)
        val formatName = ImageIO.getImageReadersBySuffix("png").next().formatName.toLowerCase()

        assertEquals("png", formatName)
    }

    @Test
    fun `should return QR code with correct size`() {
        val url = "https://example.com"
        val size = 300

        val bitMatrix = BitMatrix(size, size)
        `when`(qrCodeWriter.encode(url, BarcodeFormat.QR_CODE, size, size)).thenReturn(bitMatrix)

        val qrCodeBytes = createQRUseCase.create(url, size)

        val image = ImageIO.read(qrCodeBytes.inputStream())
        assertEquals(size, image.width)
        assertEquals(size, image.height)
    }
}
