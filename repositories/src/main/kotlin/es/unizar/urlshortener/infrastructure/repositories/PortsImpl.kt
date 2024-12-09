package es.unizar.urlshortener.infrastructure.repositories

import es.unizar.urlshortener.core.Click
import es.unizar.urlshortener.core.ClickRepositoryService
import es.unizar.urlshortener.core.ShortUrl
import es.unizar.urlshortener.core.ShortUrlRepositoryService
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.OffsetDateTime

class ClickRepositoryServiceImpl(
    private val clickEntityRepository: ClickEntityRepository
) : ClickRepositoryService {
    /**
     * Finds all [Click] entities associated with the given hash.
     *
     * @param hash The hash associated with the shortened URL.
     * @return A [Flux] emitting [Click] entities.
     */
    override fun findAllByHash(hash: String): Flux<Click> {
        return clickEntityRepository.findAllByHash(hash)
            .map { it.toDomain() }
            .doOnNext { println("Retrieved Click entity for hash $hash: $it") }
    }
    /**
     * Saves a [Click] entity to the repository.
     *
     * @param cl The [Click] entity to be saved.
     * @return A [Mono] emitting the saved [Click] entity.
     */
    override fun save(cl: Click): Mono<Click> {
        return clickEntityRepository.save(cl.toEntity())
            .map { it.toDomain() }
            .doOnNext { println("Saved Click entity: $it") }
    }

    /**
     * Counts the number of clicks associated with a specific hash created after a certain date and time.
     *
     * @param hash The hash associated with the shortened URL.
     * @param createdAfter The date and time after which clicks are counted.
     * @return A [Mono] emitting the number of clicks.
     */
    override fun countClicksByHashAfter(hash: String, createdAfter: OffsetDateTime): Mono<Long> {
        return clickEntityRepository.countByHashAndCreatedAfter(hash, createdAfter)
            .doOnNext { println("Counted $it clicks for hash $hash after $createdAfter") }
    }
}

class ShortUrlRepositoryServiceImpl(
    private val shortUrlEntityRepository: ShortUrlEntityRepository,
    private val entityTemplate: R2dbcEntityTemplate
) : ShortUrlRepositoryService {

    /**
     * Finds a [ShortUrl] entity by its key.
     *
     * @param id The key of the [ShortUrl] entity.
     * @return A [Mono] emitting the found [ShortUrl] entity or empty if not found.
     */
    override fun findByKey(id: String): Mono<ShortUrl> {
        return shortUrlEntityRepository.findByHash(id)
            .map { it.toDomain() }
            .doOnNext { println("Retrieved ShortUrl entity with id $id: $it") }
    }

    /**
     * Saves a [ShortUrl] entity to the repository.
     *
     * @param su The [ShortUrl] entity to be saved.
     * @return A [Mono] emitting the saved [ShortUrl] entity.
     */
    override fun save(su: ShortUrl): Mono<ShortUrl> {
        return entityTemplate.insert(ShortUrlEntity::class.java)
            .using(su.toEntity())
            .map { it.toDomain() }
            .doOnNext { println("Saved ShortUrl entity: $it") }
    }
}
