@file:Suppress("WildcardImport", "SwallowedException")
package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.*
import es.unizar.urlshortener.core.services.GenerateShortUrlService
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.core.io.buffer.DefaultDataBufferFactory
import org.springframework.http.server.reactive.ServerHttpRequest
import reactor.core.publisher.Flux
import java.io.*

/**
 * Interface defining the contract for processing CSV files containing URLs.
 */
interface ProcessCsvUseCase {
    /**
     * Processes the input CSV from the provided Reader, creates shortened URLs for each entry and its QR code URLs
     * if requested, and writes the results in CSV format to the provided Writer.
     *
     * Each line of the input is expected to be a URL, which is processed to generate a short URL and its QR code URL
     * if requested. In case of an error, an error message is recorded for the respective URL.
     *
     * @param reader The source of CSV data containing URLs.
     * @param writer The destination to write the results of URL shortening or error messages.
     * @param request The HTTP request providing client context
     * @param data Data of the HTTP request
     */
    fun processCsv(inputBuffer: Flux<DataBuffer>, request: ServerHttpRequest, qrRequested: Boolean): Flux<DataBuffer>
}

/**
 * Implementation of [ProcessCsvUseCase].
 *
 * Responsible for reading URLs from a CSV, creating short URLs and its QR code URLs if requested,
 * and writing the results or errors to the provided Writer.
 *
 * @param generateShortUrlServiceImpl A use case for creating short URLs.
 */
@Suppress("TooGenericExceptionCaught")
class ProcessCsvUseCaseImpl (
    private val generateShortUrlServiceImpl: GenerateShortUrlService
) : ProcessCsvUseCase {
    /**
     * Processes the input CSV from the provided Reader, creates shortened URLs for each entry and its QR code URLs
     * if requested, and writes the results in CSV format to the provided Writer.
     *
     * Each line of the input is expected to be a URL, which is processed to generate a short URL and its QR code URL
     * if requested. In case of an error, an error message is recorded for the respective URL.
     *
     * @param reader The source of CSV data containing URLs.
     * @param writer The destination to write the results of URL shortening or error messages.
     * @param request The HTTP request providing client context
     * @param data Data of the HTTP request
     */
    override fun processCsv(inputBuffer: Flux<DataBuffer>, request: ServerHttpRequest, qrRequested: Boolean): Flux<DataBuffer> {
        return DataBufferUtils.join(inputBuffer)
            .flatMapMany { dataBuffer ->
                val content = dataBuffer.asByteBuffer().array().inputStream().bufferedReader().use { it.readText() }
                Flux.fromIterable(content.lines())
            }
            .flatMap { processedLine ->
                val originalUrl = processedLine.trim()

                generateShortUrlServiceImpl.generate(ShortUrlDataIn(originalUrl, qrRequested), request)
                    .map { result ->
                        val shortUrl = result.shortUrl.toString()
                        val qrCodeUrl = result.qrCodeUrl?.toString() ?: "QR not generated"
                        "$originalUrl,$shortUrl,$qrCodeUrl\n"
                    }
                    .map { newLine -> DefaultDataBufferFactory().wrap(newLine.toByteArray()) }
            }
    }

}
