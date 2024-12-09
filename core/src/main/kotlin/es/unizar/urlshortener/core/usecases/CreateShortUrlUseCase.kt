@file:Suppress("WildcardImport")

package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.*
import reactor.core.publisher.Mono

/**
 * Given an url returns the key that is used to create a short URL.
 * When the url is created optional data may be added.
 */
interface CreateShortUrlUseCase {
    /**
     * Creates a short URL for the given URL and optional data.
     *
     * @param url The URL to be shortened.
     * @param data The optional properties for the short URL.
     * @return The created [ShortUrl] entity.
     * @throws InvalidUrlException if the URL is not valid.
     */
    fun create(url: String, data: ShortUrlProperties): Mono<ShortUrl>
}

/**
 * Implementation of [CreateShortUrlUseCase].
 *
 * Validates the target URL, generates a unique hash for the URL,
 * and saves the resulting short URL along with any provided metadata in the repository.
 *
 * @property shortUrlRepository The repository service responsible for saving and retrieving short URLs.
 * @property hashService The service responsible for generating a unique hash for the URL.
 */
class CreateShortUrlUseCaseImpl(
    private val shortUrlRepository: ShortUrlRepositoryService,
    private val hashService: HashService
) : CreateShortUrlUseCase {
    /**
     * Creates a short URL for the given URL and optional data.
     *
     * @param url The URL to be shortened.
     * @param data The optional properties for the short URL.
     * @return The created [ShortUrl] entity.
     * @throws InvalidUrlException if the URL is not valid.
     */
    override fun create(url: String, data: ShortUrlProperties): Mono<ShortUrl> {
        val id = hashService.hashUrl(url)
        val su = ShortUrl(
            hash = id,
            redirection = Redirection(target = url),
            properties = data
        )

        val shortUrl = shortUrlRepository.save(su)
            .doOnNext { println("Saved ShortUrl: $it") }
            .doOnError { println("Error saving ShortUrl: ${it.message}") }
        return shortUrl
    }
}
