package es.unizar.urlshortener.core.services

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import es.unizar.urlshortener.core.*
import es.unizar.urlshortener.core.usecases.CreateShortUrlUseCase
import org.springframework.http.server.reactive.ServerHttpRequest
import reactor.core.publisher.Mono
import java.net.URI

/**
 * Defines the specification for the use case responsible for generating
 * enhanced short URLs with additional properties like geolocation data.
 */
interface GenerateShortUrlService {
    /**
     * Generates a short URL based on the input data and the HTTP request context.
     *
     * @param data The input data containing the original URL and optional settings.
     * @param request The HTTP request object, used for extracting contextual information (e.g., IP address).
     * @return A [Mono] emitting a [Result] containing the generated short URL and optional QR code URL or an error.
     */
    fun generate(data: ShortUrlDataIn, request: ServerHttpRequest): Mono<Result<ShortUrlDataOut, UrlError>>
}

/**
 * Implementation of the `GenerateEnhancedShortUrlUseCase` interface.
 *
 * This class acts as the controller to handle requests for generating enhanced short URLs.
 */
class GenerateShortUrlServiceImpl(
    private val urlValidatorService: UrlValidatorService,
    private val createShortUrlUseCase: CreateShortUrlUseCase,
    private val geoLocationService: GeoLocationService,
    private val baseUrlProvider: BaseUrlProvider
) : GenerateShortUrlService {

    /**
     * Generates an enhanced short URL using geolocation information and other properties.
     *
     * @param data The input data containing the original URL and a flag indicating whether a QR code is required.
     * @param request The HTTP request, used to extract the client's IP address for geolocation purposes.
     * @return A [Mono] emitting a [Result] containing the short URL and optionally a QR code URL or an error.
     */
    override fun generate(data: ShortUrlDataIn, request: ServerHttpRequest): Mono<Result<ShortUrlDataOut, UrlError>> {
        return urlValidatorService.validate(data.url)
            .flatMap { validationResult ->
                if (validationResult.isErr) {
                    return@flatMap Mono.just(Err(validationResult.error))
                }

                val forwardedFor = request.headers["X-Forwarded-For"]?.firstOrNull()
                val ipAddress = forwardedFor ?: request.remoteAddress?.address?.hostAddress ?: "unknown"
                geoLocationService.get(ipAddress)
                    .flatMap { geoLocation ->
                        val enrichedData = ShortUrlProperties(
                            ip = geoLocation.ip,
                            country = geoLocation.country
                        )

                        createShortUrlUseCase.create(data.url, enrichedData)
                            .flatMap { shortUrlModel ->
                                val shortUrl = URI.create("${baseUrlProvider.get(request)}/${shortUrlModel.hash}")
                                val qrCodeUrl = if (data.qrRequested) {
                                    URI.create("${baseUrlProvider.get(request)}/api/qr?id=${shortUrlModel.hash}")
                                } else null

                                Mono.just(Ok(ShortUrlDataOut(shortUrl, qrCodeUrl)))
                            }
                    }
            }
    }
}
