@file:Suppress("WildcardImport")

package es.unizar.urlshortener.core.services

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import es.unizar.urlshortener.core.*
import es.unizar.urlshortener.core.usecases.CreateQRUseCase
import org.springframework.http.server.reactive.ServerHttpRequest
import reactor.core.publisher.Mono
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
    fun getQrImage(hash: String, request: ServerHttpRequest): Mono<Result<ByteArray, HashError>>
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
    override fun getQrImage(hash: String, request: ServerHttpRequest): Mono<Result<ByteArray, HashError>> {
        return hashValidatorService.validate(hash)
            .flatMap { validationResult ->
                if (validationResult.isErr) {
                    Mono.just(Err(validationResult.error))
                } else {
                    val shortUrl = URI.create("${baseUrlProvider.get(request)}/${hash}").toString()
                    val qrCode = qrUseCase.create(shortUrl, QR_SIZE)
                    Mono.just(Ok(qrCode))
                }
            }
    }

    companion object {
        const val QR_SIZE = 256
    }
}
