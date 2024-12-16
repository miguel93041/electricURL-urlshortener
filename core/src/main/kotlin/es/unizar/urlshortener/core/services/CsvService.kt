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
 * Defines the specification for processing CSV files.
 */
fun interface CsvService {

    /**
     * Processes the given CSV data.
     *
     * @param data The input data containing the CSV file and optional QR code request.
     * @param request The HTTP request used for contextual information.
     * @return A [Mono] emitting a [Result] containing a [Flux] of [DataBuffer] or a [CsvError].
     */
    fun process(data: CsvDataIn, request: ServerHttpRequest): Mono<Result<Flux<DataBuffer>, CsvError>>
}

/**
 * [CsvServiceImpl] is an implementation of the [CsvService] interface.
 *
 * This class handles the processing of CSV files using the provided [ProcessCsvUseCase].
 *
 * @property processCsvUseCase Service for processing CSV data.
 */
class CsvServiceImpl(private val processCsvUseCase: ProcessCsvUseCase) : CsvService {

    /**
     * Processes the CSV file data.
     *
     * @param data The input data containing the CSV file and optional QR code request.
     * @param request The HTTP request used for contextual information, like IP address and other metadata.
     * @return A [Mono] emitting a [Result] containing a [Flux] of [DataBuffer] or a [CsvError].
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
