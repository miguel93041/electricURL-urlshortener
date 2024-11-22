@file:Suppress("WildcardImport")

package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.*
import jakarta.servlet.http.HttpServletRequest
import org.springframework.web.bind.annotation.RestController
import java.net.URI


/**
 * The specification of the controller.
 */
interface GenerateEnhancedShortUrlUseCase {

    fun generate(url: String, data: ShortUrlProperties, qrRequested: Boolean, request: HttpServletRequest):
            GeneratedShortUrlResult

}

/**
 * The implementation of the controller.
 *
 * **Note**: Spring Boot is able to discover this [RestController] without further configuration.
 */
@Suppress("LongParameterList")
@RestController
class GenerateEnhancedShortUrlUseCaseImpl(
    private val createShortUrlUseCase: CreateShortUrlUseCase,
    private val geoLocationService: GeoLocationService,
    private val baseUrlProvider: BaseUrlProvider
) : GenerateEnhancedShortUrlUseCase {

    override fun generate(
        url: String, data: ShortUrlProperties, qrRequested: Boolean,
        request: HttpServletRequest
    ): GeneratedShortUrlResult {
        val geoLocation = geoLocationService.get(request.remoteAddr)

        val enrichedData = ShortUrlProperties(
            ip = geoLocation.ip,
            sponsor = data.sponsor,
            country = geoLocation.country
        )

        val shortUrl = createShortUrlUseCase.create(url, enrichedData)
        var url: URI?
        url = URI.create("${baseUrlProvider.get()}/${shortUrl.hash}")
        var qrCodeUrl: URI? = null


        // Generate url of the qr
        if (qrRequested) {
            qrCodeUrl = URI.create("${baseUrlProvider.get()}/api/qr?id=${shortUrl.hash}")
        }

        return GeneratedShortUrlResult(shortUrl, qrCodeUrl, url)
    }

    companion object {
        const val QR_SIZE = 256
    }
}
