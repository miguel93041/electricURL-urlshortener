package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.Redirection
import es.unizar.urlshortener.core.RedirectionNotFound
import es.unizar.urlshortener.core.ShortUrlRepositoryService
import es.unizar.urlshortener.core.safeCall

/**
 * Given a key returns a [Redirection] that contains a [URI target][Redirection.target]
 * and an [HTTP redirection mode][Redirection.mode].
 *
 * **Note**: This is an example of functionality.
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
 */
class RedirectUseCaseImpl(
    private val shortUrlRepository: ShortUrlRepositoryService,
    private val redirectionLimitUseCase: RedirectionLimitUseCase
) : RedirectUseCase {
    /**
     * Redirects to the target URL associated with the given key.
     *
     * @param key The key associated with the target URL.
     * @return The [Redirection] containing the target URL and redirection mode.
     * @throws RedirectionNotFound if no redirection is found for the given key.
     */
    override fun redirectTo(key: String) = safeCall {
        try {
            // Verificar y aumentar el contador de redirecciones
            redirectionLimitUseCase.incrementRedirectionCount(key)
        } catch (e: TooManyRequestsException) {
            // Manejar el caso en el que se excede el límite de redirecciones
            throw TooManyRequestsException("Redirection limit exceeded for key: $key")
        }
        shortUrlRepository.findByKey(key)
    }?.redirection ?: throw RedirectionNotFound(key)
}
