package es.unizar.urlshortener.infrastructure.repositories

import es.unizar.urlshortener.core.Click
import es.unizar.urlshortener.core.ClickRepositoryService
import es.unizar.urlshortener.core.ShortUrl
import es.unizar.urlshortener.core.ShortUrlRepositoryService

class ClickRepositoryServiceImpl(
    private val clickEntityRepository: ClickEntityRepository
) : ClickRepositoryService {
    /**
     * Finds all [Click] entities associated with the given hash.
     *
     * @param hash The hash associated with the shortened URL.
     * @return A list of [Click] entities.
     */
    override fun findAllByHash(hash: String): List<Click> {
        val clickEntities = clickEntityRepository.findAllByHash(hash)
        val clicks = clickEntities.map { it.toDomain() }
        System.out.println("Retrieved Click entities for hash $hash: $clicks")
        return clicks
    }

    /**
     * Saves a [Click] entity to the repository.
     *
     * @param cl The [Click] entity to be saved.
     * @return The saved [Click] entity.
     */
    override fun save(cl: Click): Click {
        val savedClick = clickEntityRepository.save(cl.toEntity()).toDomain()
        System.out.println("Saved Click entity: $savedClick")
        return savedClick
    }
}

class ShortUrlRepositoryServiceImpl(
    private val shortUrlEntityRepository: ShortUrlEntityRepository
) : ShortUrlRepositoryService {

    /**
     * Finds a [ShortUrl] entity by its key.
     *
     * @param id The key of the [ShortUrl] entity.
     * @return The found [ShortUrl] entity or null if not found.
     */
    override fun findByKey(id: String): ShortUrl? {
        val foundShortUrl = shortUrlEntityRepository.findByHash(id)?.toDomain()
        System.out.println("Retrieved ShortUrl entity with id $id: $foundShortUrl")
        return foundShortUrl
    }

    /**
     * Saves a [ShortUrl] entity to the repository.
     *
     * @param su The [ShortUrl] entity to be saved.
     * @return The saved [ShortUrl] entity.
     */
    override fun save(su: ShortUrl): ShortUrl {
        val savedShortUrl = shortUrlEntityRepository.save(su.toEntity()).toDomain()
        System.out.println("Saved ShortUrl entity: $savedShortUrl")
        return savedShortUrl
    }
}
