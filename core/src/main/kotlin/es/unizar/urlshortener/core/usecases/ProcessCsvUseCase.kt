@file:Suppress("WildcardImport")
package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.BaseUrlProvider
import es.unizar.urlshortener.core.ShortUrlProperties
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
    fun processCsv(reader: Reader, writer: Writer)
}

/**
 * Implementation of the ProcessCsvUseCase interface.
 * Responsible for reading URLs from a CSV, creating short URLs,
 * and writing the results or errors to the provided Writer.
 *
 * @param createShortUrlUseCase A use case for creating short URLs.
 * @param baseUrlProvider The base URL used for generating shortened URLs.
 */
@Suppress("TooGenericExceptionCaught")
class ProcessCsvUseCaseImpl (
    private val createShortUrlUseCase: CreateShortUrlUseCase,
    private val baseUrlProvider: BaseUrlProvider
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
    override fun processCsv(reader: Reader, writer: Writer) {
        writer.append("original-url,shortened-url\n")

        BufferedReader(reader).use { br ->
            br.forEachLine { line ->
                val originalUrl = line.trim()
                try {
                    val shortUrl = createShortUrlUseCase.create(originalUrl, ShortUrlProperties())
                    val shortenedUrl = buildShortenedUrl(shortUrl.hash)
                    writer.append("$originalUrl,$shortenedUrl\n")
                } catch (e: Exception) {
                    writer.append("$originalUrl,ERROR: ${e.message}\n")
                }
            }
        }
    }

    /**
     * Builds the full shortened URL by appending the hash to the base URL of the servlet.
     *
     * @param hashUrl The hash generated for the short URL.
     * @return The complete shortened URL.
     */
    fun buildShortenedUrl(hashUrl: String): String {
        return "${baseUrlProvider.get()}/${hashUrl}"
    }
}
