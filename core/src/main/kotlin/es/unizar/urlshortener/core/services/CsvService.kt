@file:Suppress("WildcardImport")

package es.unizar.urlshortener.core.services

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import es.unizar.urlshortener.core.*
import es.unizar.urlshortener.core.usecases.ProcessCsvUseCase
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.http.server.reactive.ServerHttpRequest
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.io.*

/**
 * Defines the specification for the use case responsible for generating
 * enhanced short URLs with additional properties like geolocation data.
 */
interface CsvService {
    /**
     * Obtains the original url of a short url.
     *
     * @param hash The identifier of the short url.
     * @param request The HTTP request object, used for extracting contextual information (e.g., IP address).
     * @return A data object containing the generated short URL and optional QR code URL.
     */
    fun process(data: CsvDataIn, request: ServerHttpRequest): Mono<Result<Flux<DataBuffer>, CsvError>>
}

/**
 * Implementation of the `GenerateEnhancedShortUrlUseCase` interface.
 *
 * This class acts as the controller to handle requests for generating enhanced short URLs.
 */
class CsvServiceImpl (private val processCsvUseCase: ProcessCsvUseCase) : CsvService {
    /**
     * Generates an enhanced short URL using geolocation information and other properties.
     *
     * @param hash The identifier of the short url.
     * @param request The HTTP request, used to extract the client's IP address for geolocation purposes.
     * @return A data object containing the short URL and optionally a QR code URL.
     */
    override fun process(data: CsvDataIn, request: ServerHttpRequest): Mono<Result<Flux<DataBuffer>, CsvError>> {
        if (data.file.filename().isBlank() || !data.file.filename().endsWith(".csv")) {
            return Mono.just(Err(CsvError.InvalidFormat))
        }

        return data.file.content()
            .next()
            .flatMap { firstBuffer ->
                if (firstBuffer.readableByteCount() == 0) {
                    Mono.just(Err(CsvError.InvalidFormat))
                } else {
                    Mono.just(Ok(processCsvUseCase.processCsv(data.file.content(), request, data.qrRequested)))
                }
            }
    }
}