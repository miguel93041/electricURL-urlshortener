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
    private val clickRepositoryService: ClickRepositoryService
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
                        is HashError.NotValidated -> RedirectionError.NotValidated
                        is HashError.Unreachable -> RedirectionError.Unreachable
                        is HashError.Unsafe -> RedirectionError.Unsafe
                    }
                    Mono.just(Err(mappedError))
                } else {
                    redirectionLimitUseCase.isRedirectionLimit(hash)
                        .flatMap { isLimitReached ->
                            if (isLimitReached) {
                                Mono.just(Err(RedirectionError.TooManyRequests))
                            } else {
                                redirectUseCase.redirectTo(hash)
                                    .flatMap { redirection ->
                                        logClickUseCase.logClick(hash)
                                            .flatMap { click ->
                                                val ipAddress = ClientHostResolver.resolve(request)
                                                if (ipAddress != null) {
                                                    geoLocationService.get(ipAddress)
                                                        .doOnSuccess { geoLocation ->
                                                            clickRepositoryService.updateGeolocation(click.id!!, geoLocation).subscribe()
                                                        }.subscribe()
                                                }

                                                val userAgent = request.headers.getFirst("User-Agent")
                                                Mono.fromCallable {
                                                    val browserPlatform = browserPlatformIdentificationUseCase.parse(userAgent)
                                                    clickRepositoryService.updateBrowserPlatform(click.id!!, browserPlatform).subscribe()
                                                }.subscribe()

                                                Mono.just(Ok(redirection))
                                            }
                                    }
                            }
                        }
                }
            }
    }
}