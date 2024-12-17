@file:Suppress("WildcardImport", "LongParameterList")

package es.unizar.urlshortener.core.services

import com.github.michaelbull.result.*
import es.unizar.urlshortener.core.*
import es.unizar.urlshortener.core.queues.GeolocationChannelService
import es.unizar.urlshortener.core.usecases.BrowserPlatformIdentificationUseCase
import es.unizar.urlshortener.core.usecases.LogClickUseCase
import es.unizar.urlshortener.core.usecases.RedirectUseCase
import es.unizar.urlshortener.core.usecases.RedirectionLimitUseCase
import org.springframework.http.server.reactive.ServerHttpRequest
import reactor.core.publisher.Mono

/**
 * Defines the specification for the use case responsible for generating
 * enhanced short URLs with additional properties like geolocation data.
 */
fun interface RedirectService {
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
    private val browserPlatformIdentificationUseCase: BrowserPlatformIdentificationUseCase,
    private val redirectionLimitUseCase: RedirectionLimitUseCase,
    private val clickRepositoryService: ClickRepositoryService,
    private val geolocationChannelService: GeolocationChannelService

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
                    handleValidationError(validationResult.unwrapError())
                } else {
                    handleRedirection(hash, request)
                }
            }
    }

    private fun handleValidationError(error: HashError): Mono<Result<Redirection, RedirectionError>> {
        val mappedError = when (error) {
            is HashError.InvalidFormat -> RedirectionError.InvalidFormat
            is HashError.NotFound -> RedirectionError.NotFound
            is HashError.NotValidated -> RedirectionError.NotValidated
            is HashError.Unreachable -> RedirectionError.Unreachable
            is HashError.Unsafe -> RedirectionError.Unsafe
        }
        return Mono.just(Err(mappedError))
    }

    private fun handleRedirection(
        hash: String,
        request: ServerHttpRequest
    ): Mono<Result<Redirection, RedirectionError>> {
        return redirectionLimitUseCase.isRedirectionLimit(hash)
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
                                        geolocationChannelService.enqueue(ClickEvent(ip=ipAddress, clickId=click.id!!))
                                    }

                                    val userAgent = request.headers.getFirst("User-Agent")
                                    Mono.fromCallable {
                                        val browserPlatform =
                                            browserPlatformIdentificationUseCase.parse(userAgent)
                                        clickRepositoryService
                                            .updateBrowserPlatform(click.id!!, browserPlatform).subscribe()
                                    }.subscribe()

                                    Mono.just(Ok(redirection))
                                }
                        }
                }
            }
    }
}
