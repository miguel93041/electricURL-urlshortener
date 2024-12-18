@file:Suppress("WildcardImport")

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
fun interface ProcessCsvUseCase {

    /**
     * Processes the input CSV from the provided Reader, creates shortened URLs for each entry and its QR code URLs
     * if requested, and writes the results in CSV format to the provided Writer.
     *
     * Each line of the input is expected to be a URL, which is processed to generate a short URL and its QR code URL
     * if requested. In case of an error, an error message is recorded for the respective URL.
     *
     * @param inputBuffer The Flux stream of data buffers containing CSV lines.
     * @param request The ServerHttpRequest for contextual information.
     * @param qrRequested Flag indicating whether QR code generation is requested.
     * @return A Flux of DataBuffer containing processed lines with original URLs, short URLs,
     *          and optionally QR code URLs.
     */
    fun processCsv(inputBuffer: Flux<DataBuffer>, request: ServerHttpRequest, qrRequested: Boolean): Flux<DataBuffer>
}

/**
 * [ProcessCsvUseCaseImpl] is an implementation of the [ProcessCsvUseCase].
 *
 * Processes CSV input by converting each line to a short URL using the GenerateShortUrlService.
 *
 * @property generateShortUrlServiceImpl The service used for generating short URLs.
 */
@Suppress("TooGenericExceptionCaught")
class ProcessCsvUseCaseImpl(
    private val generateShortUrlServiceImpl: GenerateShortUrlService
) : ProcessCsvUseCase {

    /**
     * Processes a CSV input stream by generating short URLs for each line.
     *
     * The original URLs are converted to short URLs, and if requested, QR codes are also generated.
     *
     * @param inputBuffer The Flux stream of data buffers containing CSV lines.
     * @param request The ServerHttpRequest for contextual information.
     * @param qrRequested Flag indicating whether QR code generation is requested.
     * @return A Flux of DataBuffer containing processed lines with original URLs, short URLs,
     *          and optionally QR code URLs.
     */
    override fun processCsv(
        inputBuffer: Flux<DataBuffer>,
        request: ServerHttpRequest,
        qrRequested: Boolean
    ): Flux<DataBuffer> {
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
