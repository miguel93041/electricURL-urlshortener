@file:Suppress("WildcardImport")

package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.*
import reactor.core.publisher.Mono

/**
 * Given an url returns the key that is used to create a short URL.
 * When the url is created optional data may be added.
 */
fun interface CreateShortUrlUseCase {

    /**
     * Creates a short URL for the given URL.
     *
     * @param url The URL to be shortened.
     * @return A [Mono] emitting the created [ShortUrl] entity.
     */
    fun create(url: String): Mono<ShortUrl>
}

/**
 * Implementation of [CreateShortUrlUseCase].
 *
 * Validates the target URL, generates a unique hash for the URL,
 * and saves the resulting short URL in the repository.
 *
 * @property shortUrlRepository The repository service responsible for saving and retrieving short URLs.
 * @property hashService The service responsible for generating a unique hash for the URL.
 */
class CreateShortUrlUseCaseImpl(
    private val shortUrlRepository: ShortUrlRepositoryService,
    private val hashService: HashService
) : CreateShortUrlUseCase {

    /**
     * Creates a short URL for the given URL.
     *
     * @param url The URL to be shortened.
     * @return A [Mono] emitting the created [ShortUrl] entity.
     */
    override fun create(url: String): Mono<ShortUrl> {
        val id = hashService.generateRandomHash()
        val su = ShortUrl(
            hash = id,
            redirection = Redirection(target = url),
            properties = ShortUrlProperties()
        )

        val shortUrl = shortUrlRepository.create(su)
        return shortUrl
    }
}
