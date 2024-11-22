@file:Suppress("WildcardImport", "SwallowedException")
package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.*
import jakarta.servlet.http.HttpServletRequest
import java.io.*

/**
 * Interface defining the contract for processing CSV files containing URLs.
 *
 * @param reader The source of CSV data containing URLs.
 * @param writer The destination to write the results of URL shortening.
 */
interface ProcessCsvUseCase {
    /**
     * Processes the input CSV from the provided Reader, creates shortened URLs for each entry,
     * and writes the results in CSV format to the provided Writer.
     *
     * Each line of the input is expected to be a URL, which is processed to generate a short URL.
     * In case of an error, an error message is recorded for the respective URL.
     *
     * @param reader The source of CSV data containing URLs.
     * @param writer The destination to write the results of URL shortening or error messages.
     */
    fun processCsv(reader: Reader, writer: Writer, request: HttpServletRequest, data: ShortUrlDataIn)
}

/**
 * Implementation of the ProcessCsvUseCase interface.
 * Responsible for reading URLs from a CSV, creating short URLs,
 * and writing the results or errors to the provided Writer.
 *
 * @param generateEnhancedShortUrlUseCaseImpl A use case for creating short URLs.
 */
@Suppress("TooGenericExceptionCaught")
class ProcessCsvUseCaseImpl (
    private val generateEnhancedShortUrlUseCaseImpl: GenerateEnhancedShortUrlUseCase
) : ProcessCsvUseCase {

    /**
     * Processes the input CSV from the provided Reader, creates shortened URLs for each entry,
     * and writes the results in CSV format to the provided Writer.
     *
     * Each line of the input is expected to be a URL, which is processed to generate a short URL.
     * In case of an error, an error message is recorded for the respective URL.
     *
     * @param reader The source of CSV data containing URLs.
     * @param writer The destination to write the results of URL shortening or error messages.
     */
    override fun processCsv(reader: Reader, writer: Writer, request: HttpServletRequest, data: ShortUrlDataIn) {
        writer.append("original-url,shortened-url")
        if (data.qrRequested) {
            writer.append(",qr-code-url")
        }
        writer.append("\n")

        BufferedReader(reader).use { br ->
            br.forEachLine { line ->
                val originalUrl = line.trim()
                try {
                    val result = generateEnhancedShortUrlUseCaseImpl.generate(
                        ShortUrlDataIn(originalUrl, data.qrRequested),
                        request
                    )

                    writer.append("$originalUrl,${result.shortUrl}")
                    if (data.qrRequested) {
                        writer.append("," + result.qrCodeUrl)
                    }
                    writer.append("\n")
                } catch (e: InvalidUrlException) {
                    writer.append("$originalUrl,ERROR: Invalid URL,ERROR: QR not generated\n")
                } catch (e: UnsafeUrlException) {
                    writer.append("$originalUrl,ERROR: Unsafe URL,ERROR: QR not generated\n")
                } catch (e: UrlUnreachableException) {
                    writer.append("$originalUrl,ERROR: URL unreachable,ERROR: QR not generated\n")
                } catch (e: IllegalArgumentException) {
                    writer.append("$originalUrl,ERROR: Invalid URL,ERROR: QR not generated\n")
                }
            }
        }
    }

}
