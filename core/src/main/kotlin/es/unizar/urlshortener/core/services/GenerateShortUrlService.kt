@file:Suppress("WildcardImport")

package es.unizar.urlshortener.core.services

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.unwrapError
import es.unizar.urlshortener.core.*
import es.unizar.urlshortener.core.queues.GeolocationChannelService
import es.unizar.urlshortener.core.queues.UrlValidationChannelService
import es.unizar.urlshortener.core.usecases.CreateShortUrlUseCase
import org.springframework.http.server.reactive.ServerHttpRequest
import reactor.core.publisher.Mono
import java.net.URI

/**
 * Defines the specification for the use case responsible for generating
 * enhanced short URLs with additional properties like geolocation data.
 */
fun interface GenerateShortUrlService {
    /**
     * Generates a short URL based on the input data and the HTTP request context.
     *
     * @param data The input data containing the original URL and optional settings.
     * @param request The HTTP request object, used for extracting contextual information (e.g., IP address).
     * @return A [Mono] emitting a [Result] containing the generated short URL and optional QR code URL or an error.
     */
    fun generate(data: ShortUrlDataIn, request: ServerHttpRequest): Mono<ShortUrlDataOut>
}

/**
 * Implementation of the `GenerateEnhancedShortUrlUseCase` interface.
 *
 * This class acts as the controller to handle requests for generating enhanced short URLs.
 */
class GenerateShortUrlServiceImpl(
    private val createShortUrlUseCase: CreateShortUrlUseCase,
    private val baseUrlProvider: BaseUrlProvider,
    private val geolocationChannelService: GeolocationChannelService,
    private val urlValidationChannelService: UrlValidationChannelService
) : GenerateShortUrlService {

    /**
     * Generates an enhanced short URL using geolocation information and other properties.
     *
     * @param data The input data containing the original URL and a flag indicating whether a QR code is required.
     * @param request The HTTP request, used to extract the client's IP address for geolocation purposes.
     * @return A [Mono] emitting a [ShortUrlDataOut] and optionally a QR code URL or an error.
     */
    override fun generate(data: ShortUrlDataIn, request: ServerHttpRequest): Mono<ShortUrlDataOut> {
        return createShortUrlUseCase.create(data.url)
            .flatMap { shortUrlModel ->
                val shortUrl = URI.create("${baseUrlProvider.get(request)}/${shortUrlModel.hash}")
                val qrCodeUrl = if (data.qrRequested) {
                    URI.create("${baseUrlProvider.get(request)}/api/qr?id=${shortUrlModel.hash}")
                } else null

                // Validate URL in a background task
                urlValidationChannelService.enqueue(
                    UrlValidationEvent(
                        url = data.url,
                        hash = shortUrlModel.hash
                    )
                )

                // Enrich Shortened URL in a background task
                val ipAddress = ClientHostResolver.resolve(request)
                if (ipAddress != null) {
                    geolocationChannelService.enqueue(HashEvent(ip=ipAddress, hash=shortUrlModel.hash))
                }

                Mono.just(ShortUrlDataOut(shortUrl, qrCodeUrl))
            }
    }
}
