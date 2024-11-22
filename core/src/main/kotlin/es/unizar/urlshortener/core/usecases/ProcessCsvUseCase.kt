@file:Suppress("WildcardImport")
package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.BaseUrlProvider
import es.unizar.urlshortener.core.GeoLocationService
import es.unizar.urlshortener.core.ShortUrlProperties
import jakarta.servlet.http.HttpServletRequest
import java.io.*
import es.unizar.urlshortener.core.InvalidUrlException
import es.unizar.urlshortener.core.UnsafeUrlException
import es.unizar.urlshortener.core.UrlUnreachableException
import org.slf4j.LoggerFactory


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
    fun processCsv(reader: Reader, writer: Writer, request: HttpServletRequest)
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
    private val baseUrlProvider: BaseUrlProvider,
    private val geoLocationService: GeoLocationService
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
    override fun processCsv(reader: Reader, writer: Writer, request: HttpServletRequest) {
        val logger = LoggerFactory.getLogger(ProcessCsvUseCaseImpl::class.java)
        val geoLocation = geoLocationService.get(request.remoteAddr)
        writer.append("original-url,shortened-url")
        val qrRequested = request.getParameter("qrRequested")?.toBoolean() ?: false
        if (qrRequested) {
            writer.append(",qr-code-url")
        }
        writer.append("\n")
        BufferedReader(reader).use { br ->
            br.forEachLine { line ->
                val originalUrl = line.trim()
                try {
                    val shortUrl = createShortUrlUseCase.create(originalUrl, ShortUrlProperties(
                        ip = geoLocation.ip,
                        country = geoLocation.country
                    ))

                    val shortenedUrl = buildShortenedUrl(shortUrl.hash)
                    writer.append("$originalUrl,$shortenedUrl")
                    if (qrRequested) {
                        val qrCodeUrl = buildQrCodeUrl(shortUrl.hash)
                        writer.append(",$qrCodeUrl")
                    }
                    writer.append("\n")
                } catch (e: InvalidUrlException) {
                    logger.warn("Failed to process URL: $originalUrl due to InvalidUrlException", e)
                    writer.append("$originalUrl, ERROR: Invalid URL\n")
                } catch (e: UnsafeUrlException) {
                    logger.warn("Failed to process URL: $originalUrl due to InvalidUrlException", e)
                    writer.append("$originalUrl,ERROR: Unsafe URL\n")
                } catch (e: UrlUnreachableException) {
                    logger.warn("Failed to process URL: $originalUrl due to InvalidUrlException", e)
                    writer.append("$originalUrl,ERROR: URL unreachable\n")
                } catch (e: IllegalArgumentException) {
                    logger.warn("Failed to process URL: $originalUrl due to InvalidUrlException", e)
                    writer.append("$originalUrl,ERROR: Invalid URL\n")
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

    /**
     * Builds the URL for the QR code for a specific shortened URL hash.
     *
     * @param hashUrl The hash generated for the short URL.
     * @return The complete URL for the QR code.
     */
    fun buildQrCodeUrl(hashUrl: String): String {
        return "${baseUrlProvider.get()}/api/qr?id=${hashUrl}"
    }
}
