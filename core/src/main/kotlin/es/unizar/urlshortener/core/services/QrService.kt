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
 * Defines the specification for generating QR codes.
 */
fun interface QrService {

    /**
     * Generates a QR image for a given short URL.
     *
     * @param hash The identifier of the short URL.
     * @param request The HTTP request, used for extracting contextual information.
     * @return A [Mono] emitting a [Result] containing the QR image as a [ByteArray] or a [HashError].
     */
    fun getQrImage(hash: String, request: ServerHttpRequest): Mono<Result<ByteArray, HashError>>
}

/**
 * [QrServiceImpl] is an implementation of the [QrService] interface.
 *
 * This class handles the generation of QR codes for short URLs.
 *
 * @property hashValidatorService Service for validating hash formats and existence.
 * @property qrUseCase Use case for creating QR codes from URLs.
 * @property baseUrlProvider Service for obtaining the base URL from the request.
 */
class QrServiceImpl(
    private val hashValidatorService: HashValidatorService,
    private val qrUseCase: CreateQRUseCase,
    private val baseUrlProvider: BaseUrlProvider,
) : QrService {

    /**
     * Generates a QR image for the given short URL hash.
     *
     * @param hash The identifier of the short URL.
     * @param request The HTTP request, used for extracting contextual information such as the base URL.
     * @return A [Mono] emitting a [Result] containing the QR image as a [ByteArray] or a [HashError].
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
