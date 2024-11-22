@file:Suppress("WildcardImport")

package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.*

/**
 * Given an url returns the key that is used to create a short URL.
 * When the url is created optional data may be added.
 *
 * **Note**: This is an example of functionality.
 */
interface CreateShortUrlUseCase {
    /**
     * Creates a short URL for the given URL and optional data.
     *
     * @param url The URL to be shortened.
     * @param data The optional properties for the short URL.
     * @return The created [ShortUrl] entity.
     */
    fun create(url: String, data: ShortUrlProperties): ShortUrl
}

/**
 * Implementation of [CreateShortUrlUseCase].
 */
class CreateShortUrlUseCaseImpl(
    private val shortUrlRepository: ShortUrlRepositoryService,
    private val validatorService: ValidatorService,
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
    override fun create(url: String, data: ShortUrlProperties): ShortUrl {
        val validationResult = validatorService.validate(url)

        when (validationResult) {
            ValidatorResult.NOT_VALID_FORMAT -> throw InvalidUrlException(url)
            ValidatorResult.NOT_SAFE -> throw UnsafeUrlException(url)
            ValidatorResult.NOT_REACHABLE -> throw UrlUnreachableException(url)
            ValidatorResult.VALID -> {
                return safeCall{
                    val id = hashService.hasUrl(url)
                    val su = ShortUrl(
                        hash = id,
                        redirection = Redirection(target = url),
                        properties = data
                    )
                    shortUrlRepository.save(su)
                }
            }
        }
    }

}
