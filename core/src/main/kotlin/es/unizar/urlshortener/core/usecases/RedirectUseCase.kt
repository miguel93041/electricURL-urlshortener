package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.*

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
 * Retrieves redirection details from a [ShortUrlRepositoryService].
 *
 * @property shortUrlService The repository used to retrieve redirection details.
 */
class RedirectUseCaseImpl(
    private val shortUrlService: ShortUrlRepositoryService,
) : RedirectUseCase {
    /**
     * Redirects to the target URL associated with the given key.
     *
     * @param key The key associated with the target URL.
     * @return The [Redirection] containing the target URL and redirection mode.
     * @throws RedirectionNotFound if no redirection is found for the given key.
     */
    override fun redirectTo(key: String): Redirection {
        val redirection = safeCall {
            shortUrlService.findByKey(key)
        }!!.redirection

        return redirection
    }
}
