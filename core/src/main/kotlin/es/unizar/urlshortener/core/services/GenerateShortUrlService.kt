@file:Suppress("WildcardImport")

package es.unizar.urlshortener.core.services

import es.unizar.urlshortener.core.*
import es.unizar.urlshortener.core.queues.GeolocationChannelService
import es.unizar.urlshortener.core.queues.UrlValidationChannelService
import es.unizar.urlshortener.core.usecases.CreateShortUrlUseCase
import org.springframework.http.server.reactive.ServerHttpRequest
import reactor.core.publisher.Mono
import java.net.URI

/**
 * Defines the specification for generating short URLs.
 */
fun interface GenerateShortUrlService {
    /**
     * Generates a shortened URL.
     *
     * @param data The input data containing the original URL and optional QR code request.
     * @param request The HTTP request, used for extracting contextual information (e.g., base URL).
     * @return A [Mono] emitting the output data containing the shortened URL and optional QR code URL.
     */
    fun generate(data: ShortUrlDataIn, request: ServerHttpRequest): Mono<ShortUrlDataOut>
}

/**
 * [GenerateShortUrlServiceImpl] is an implementation of the [GenerateShortUrlService] interface.
 *
 * This class handles the creation of shortened URLs with additional features such as QR code generation
 * and geolocation enrichment.
 *
 * @property urlValidatorService Service for validating URLs.
 * @property createShortUrlUseCase Use case for creating short URLs.
 * @property geoLocationService Service for retrieving geolocation data.
 * @property shortUrlRepositoryService Service for managing short URL repository data.
 * @property baseUrlProvider Service for providing the base URL.
 */
class GenerateShortUrlServiceImpl(
    private val createShortUrlUseCase: CreateShortUrlUseCase,
    private val baseUrlProvider: BaseUrlProvider,
    private val geolocationChannelService: GeolocationChannelService,
    private val urlValidationChannelService: UrlValidationChannelService
) : GenerateShortUrlService {

    /**
     * Generates a shortened URL based on the input data and the request context.
     *
     * @param data The input data containing the original URL and optional QR code request.
     * @param request The HTTP request, used for extracting contextual information such as the base URL.
     * @return A [Mono] emitting the output data containing the shortened URL and optional QR code URL.
     */
    override fun generate(data: ShortUrlDataIn, request: ServerHttpRequest): Mono<ShortUrlDataOut> {
        return createShortUrlUseCase.create(data.url)
            .flatMap { shortUrlModel ->
                val shortUrl = URI.create("${baseUrlProvider.get(request)}/${shortUrlModel.hash}")
                val qrCodeUrl = if (data.qrRequested) {
                    URI.create("${baseUrlProvider.get(request)}/api/qr/${shortUrlModel.hash}")
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
