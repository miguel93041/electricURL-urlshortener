@file:Suppress("WildcardImport")

package es.unizar.urlshortener.core.services

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import es.unizar.urlshortener.core.*
import es.unizar.urlshortener.core.usecases.BrowserPlatformIdentificationUseCase
import es.unizar.urlshortener.core.usecases.CreateQRUseCase
import es.unizar.urlshortener.core.usecases.LogClickUseCase
import es.unizar.urlshortener.core.usecases.RedirectUseCase
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import java.net.URI

/**
 * Defines the specification for the use case responsible for generating
 * enhanced short URLs with additional properties like geolocation data.
 */
interface QrService {
    /**
     * Obtains the original url of a short url.
     *
     * @param hash The identifier of the short url.
     * @param request The HTTP request object, used for extracting contextual information (e.g., IP address).
     * @return A data object containing the generated short URL and optional QR code URL.
     */
    fun getQrImage(hash: String): Result<ByteArray, HashError>
}

/**
 * Implementation of the `GenerateEnhancedShortUrlUseCase` interface.
 *
 * This class acts as the controller to handle requests for generating enhanced short URLs.
 */
class QrServiceImpl(
    private val hashValidatorService: HashValidatorService,
    private val qrUseCase: CreateQRUseCase,
    private val baseUrlProvider: BaseUrlProvider,
) : QrService {

    /**
     * Generates an enhanced short URL using geolocation information and other properties.
     *
     * @param hash The identifier of the short url.
     * @param request The HTTP request, used to extract the client's IP address for geolocation purposes.
     * @return A data object containing the short URL and optionally a QR code URL.
     */
    override fun getQrImage(hash: String): Result<ByteArray, HashError> {
        // Validate hash
        val validationResult = hashValidatorService.validate(hash);
        if (validationResult.isErr) {
            return Err(validationResult.error)
        }

        // Generate the QR image
        val shortUrl = safeCall { URI.create("${baseUrlProvider.get()}/${hash}").toString() }
        val qrCode = safeCall { qrUseCase.create(shortUrl, QR_SIZE) }

        return Ok(qrCode)
    }

    companion object {
        const val QR_SIZE = 256
    }
}