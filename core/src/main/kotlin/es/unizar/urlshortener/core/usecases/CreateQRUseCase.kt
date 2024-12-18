package es.unizar.urlshortener.core.usecases

import com.google.zxing.BarcodeFormat
import com.google.zxing.client.j2se.MatrixToImageWriter
import com.google.zxing.qrcode.QRCodeWriter
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import java.io.ByteArrayOutputStream

/**
 * Interface for generating a base64-encoded QR code for a shortened URL.
 */
fun interface CreateQRUseCase {

    /**
     * Generates a QR code for a given shortened URL with specified size (in pixels).
     *
     * @param url The shortened URL to encode in the QR code.
     * @param size The width and height (in pixels) of the QR code.
     * @return A base64-encoded [ByteArray] representation of the QR code.
     */
    fun create(url: String, size: Int): ByteArray
}

/**
 * [CreateQRUseCaseImpl] is an implementation of [CreateQRUseCase].
 *
 * Generates QR codes created as PNG images and returns them as byte arrays for easy transmission or storage.
 *
 * @property qrCodeWriter An instance of [QRCodeWriter] used for encoding URLs into QR codes.
 */
class CreateQRUseCaseImpl(
    private val qrCodeWriter: QRCodeWriter
) : CreateQRUseCase {

    /**
     * Generates a QR code for a given shortened URL with specified size (in pixels).
     *
     * @param url The shortened URL to encode in the QR code.
     * @param size The width and height (in pixels) of the QR code.
     * @return A base64-encoded [ByteArray] representation of the QR code.
     */
    override fun create(url: String, size: Int): ByteArray {
        val bitMatrix = qrCodeWriter.encode(url, BarcodeFormat.QR_CODE, size, size)
        val bufferedImage: BufferedImage = MatrixToImageWriter.toBufferedImage(bitMatrix)
        val outputByteArray = ByteArrayOutputStream()
        ImageIO.write(bufferedImage, "PNG", outputByteArray)
        return outputByteArray.toByteArray()
    }
}
