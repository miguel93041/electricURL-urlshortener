@file:Suppress("WildcardImport")

package es.unizar.urlshortener.core.services

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.unwrapError
import es.unizar.urlshortener.core.*
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
    private val urlValidatorService: UrlValidatorService,
    private val createShortUrlUseCase: CreateShortUrlUseCase,
    private val geoLocationService: GeoLocationService,
    private val shortUrlRepositoryService: ShortUrlRepositoryService,
    private val baseUrlProvider: BaseUrlProvider
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
                    URI.create("${baseUrlProvider.get(request)}/api/qr?id=${shortUrlModel.hash}")
                } else null

                // Validate URL in a background task
                urlValidatorService.validate(data.url)
                    .doOnSuccess { validationResult ->
                        if (validationResult.isErr) {
                            val error = validationResult.unwrapError()
                            when (error) {
                                UrlError.InvalidFormat, UrlError.Unreachable ->
                                    shortUrlRepositoryService.updateValidation(
                                        shortUrlModel.hash,
                                        ShortUrlValidation(safe = true, reachable = false, validated = true)
                                    ).subscribe()
                                UrlError.Unsafe ->  shortUrlRepositoryService.updateValidation(
                                    shortUrlModel.hash,
                                    ShortUrlValidation(safe = false, reachable = true, validated = true)
                                ).subscribe()
                            }
                        } else {
                            shortUrlRepositoryService.updateValidation(
                                shortUrlModel.hash,
                                ShortUrlValidation(safe = true, reachable = true, validated = true)
                            ).subscribe()
                        }
                    }.subscribe()

                // Enrich Shortened URL in a background task
                val ipAddress = ClientHostResolver.resolve(request)
                if (ipAddress != null) {
                    geoLocationService.get(ipAddress)
                        .doOnSuccess { geoLocation ->
                            shortUrlRepositoryService.updateGeolocation(shortUrlModel.hash, geoLocation).subscribe()
                        }.subscribe()
                }

                Mono.just(ShortUrlDataOut(shortUrl, qrCodeUrl))
            }
    }
}
