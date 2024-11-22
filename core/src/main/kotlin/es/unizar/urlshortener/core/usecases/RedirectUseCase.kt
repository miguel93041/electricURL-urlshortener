package es.unizar.urlshortener.core.usecases

import RedirectionLimitUseCase
import es.unizar.urlshortener.core.Redirection
import es.unizar.urlshortener.core.RedirectionNotFound
import es.unizar.urlshortener.core.ShortUrlRepositoryService
import es.unizar.urlshortener.core.safeCall

/**
 * Given a key returns a [Redirection] that contains a [URI target][Redirection.target]
 * and an [HTTP redirection mode][Redirection.mode].
 */
interface RedirectUseCase {
    /**
     * Redirects to the target URL associated with the given key.
     *
     * @param key The key associated with the target URL.
     * @return The [Redirection] containing the target URL and redirection mode.
     * @throws RedirectionNotFound if no redirection is found for the given key.
     */
    fun redirectTo(key: String): Redirection
}

/**
 * Implementation of [RedirectUseCase].
 *
 * Retrieves redirection details from a [ShortUrlRepositoryService], while ensuring compliance with
 * redirection limits defined by the [RedirectionLimitUseCase].
 *
 * @property shortUrlService The repository used to retrieve redirection details.
 * @property redirectionLimitUseCase The use case that enforces limits on redirections.
 */
class RedirectUseCaseImpl(
    private val shortUrlService: ShortUrlRepositoryService,
    private val redirectionLimitUseCase: RedirectionLimitUseCase
) : RedirectUseCase {
    /**
     * Redirects to the target URL associated with the given key.
     *
     * @param key The key associated with the target URL.
     * @return The [Redirection] containing the target URL and redirection mode.
     * @throws RedirectionNotFound if no redirection is found for the given key.
     */
    override fun redirectTo(key: String): Redirection {
        redirectionLimitUseCase.checkRedirectionLimit(key)

        val redirection = safeCall {
            shortUrlService.findByKey(key)
        }?.redirection

        if (redirection == null) {
            throw RedirectionNotFound(key)
        }

        return redirection
    }
}
