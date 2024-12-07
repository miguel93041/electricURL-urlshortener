package es.unizar.urlshortener.core.usecases

import RedirectionLimitUseCase
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
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
    fun redirectTo(key: String): Result<Redirection, HashError>
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
    override fun redirectTo(key: String): Result<Redirection, HashError> {
        val isRedirectionLimit = redirectionLimitUseCase.isRedirectionLimit(key)
        if (isRedirectionLimit) {
            return Err(HashError.TooManyRequests)
        }

        val redirection = safeCall {
            shortUrlService.findByKey(key)
        }!!.redirection

        return Ok(redirection)
    }
}
