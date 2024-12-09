@file:Suppress("WildcardImport")

package es.unizar.urlshortener.core.services

import RedirectionLimitUseCase
import com.github.michaelbull.result.*
import es.unizar.urlshortener.core.*
import es.unizar.urlshortener.core.usecases.BrowserPlatformIdentificationUseCase
import es.unizar.urlshortener.core.usecases.LogClickUseCase
import es.unizar.urlshortener.core.usecases.RedirectUseCase
import org.springframework.http.server.reactive.ServerHttpRequest
import reactor.core.publisher.Mono

/**
 * Defines the specification for the use case responsible for generating
 * enhanced short URLs with additional properties like geolocation data.
 */
interface RedirectService {
    /**
     * Obtains the original url of a short url.
     *
     * @param hash The identifier of the short url.
     * @param request The HTTP request object, used for extracting contextual information (e.g., IP address).
     * @return A data object containing the generated short URL and optional QR code URL.
     */
    fun getRedirectionAndLogClick(hash: String, request: ServerHttpRequest): Mono<Result<Redirection, RedirectionError>>
}

/**
 * Implementation of the `GenerateEnhancedShortUrlUseCase` interface.
 *
 * This class acts as the controller to handle requests for generating enhanced short URLs.
 */
class RedirectServiceImpl(
    private val hashValidatorService: HashValidatorService,
    private val redirectUseCase: RedirectUseCase,
    private val logClickUseCase: LogClickUseCase,
    private val geoLocationService: GeoLocationService,
    private val browserPlatformIdentificationUseCase: BrowserPlatformIdentificationUseCase,
    private val redirectionLimitUseCase: RedirectionLimitUseCase,
) : RedirectService {


    /**
     * Handles the redirection and logs the click with enhanced data.
     *
     * @param hash The identifier of the short URL.
     * @param request The HTTP request, used to extract the client's IP address for geolocation purposes.
     * @return A Mono emitting a Result containing the Redirection or a RedirectionError.
     */
    override fun getRedirectionAndLogClick(
        hash: String,
        request: ServerHttpRequest
    ): Mono<Result<Redirection, RedirectionError>> {
        return hashValidatorService.validate(hash)
            .flatMap { validationResult ->
                if (validationResult.isErr) {
                    val error = validationResult.unwrapError()
                    val mappedError = when (error) {
                        is HashError.InvalidFormat -> RedirectionError.InvalidFormat
                        is HashError.NotFound -> RedirectionError.NotFound
                    }
                    Mono.just(Err(mappedError))
                } else {
                    redirectionLimitUseCase.isRedirectionLimit(hash)
                        .flatMap { isLimitReached ->
                            if (isLimitReached) {
                                Mono.just(Err(RedirectionError.TooManyRequests))
                            } else {
                                geoLocationService.get(request.remoteAddress.toString())
                                    .zipWith(
                                        Mono.defer {
                                            Mono.just(browserPlatformIdentificationUseCase.parse(request.headers.getFirst("User-Agent")))
                                        }
                                    )
                                    .flatMap { tuple ->
                                        val geoLocation = tuple.t1
                                        val browserPlatform = tuple.t2

                                        val enrichedData = ClickProperties(
                                            ip = geoLocation.ip,
                                            country = geoLocation.country,
                                            browser = browserPlatform.browser,
                                            platform = browserPlatform.platform
                                        )

                                        redirectUseCase.redirectTo(hash)
                                            .flatMap { redirection ->
                                                logClickUseCase.logClick(hash, enrichedData)
                                                    .then(Mono.just(Ok(redirection)))
                                            }
                                    }
                            }
                        }
                }
            }
    }
}