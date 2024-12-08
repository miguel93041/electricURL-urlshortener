@file:Suppress("WildcardImport")

package es.unizar.urlshortener.core.services

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import es.unizar.urlshortener.core.*
import es.unizar.urlshortener.core.usecases.GetAnalyticsUseCase
import es.unizar.urlshortener.core.usecases.ProcessCsvUseCase
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
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
    fun process(data: CsvDataIn, request: HttpServletRequest): Result<StreamingResponseBody, CsvError>
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
    override fun process(data: CsvDataIn, request: HttpServletRequest): Result<StreamingResponseBody, CsvError> {
        // Validate hash
        if (data.file.isEmpty) {
            return Err(CsvError.InvalidFormat)
        }

        val reader = InputStreamReader(data.file.inputStream.buffered())

        val responseBody = StreamingResponseBody { outputStream ->
            BufferedWriter(OutputStreamWriter(outputStream)).use { writer ->
                processCsvUseCase.processCsv(reader, writer, request, data)
            }
        }

        return Ok(responseBody)
    }
}