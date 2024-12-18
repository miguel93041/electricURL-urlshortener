@file:Suppress("WildcardImport")

package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.*
import reactor.core.publisher.Mono

/**
 * Given a key returns a [Redirection] that contains a URI target ([Redirection.target])
 * and an HTTP redirection mode ([Redirection.mode]).
 */
fun interface RedirectUseCase {

    /**
     * Redirects to the target URL associated with the given key.
     *
     * @param key The key associated with the target URL.
     * @return A [Mono] emitting the [Redirection] containing the target URL and redirection mode.
     *         Emits an error if no redirection is found for the given key.
     */
    fun redirectTo(key: String): Mono<Redirection>
}

/**
 * [RedirectUseCaseImpl] is an implementation of [RedirectUseCase].
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
     * @return A [Mono] emitting the [Redirection] containing the target URL and redirection mode.
     *         Emits an error if no redirection is found for the given key.
     */
    override fun redirectTo(key: String): Mono<Redirection> {
        return shortUrlService.findByKey(key)
            .flatMap { shortUrl ->
                Mono.just(shortUrl.redirection)
            }
            .switchIfEmpty(
                Mono.error(Exception("No redirection found for key: $key"))
            )
    }
}
