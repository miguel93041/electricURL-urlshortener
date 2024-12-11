package es.unizar.urlshortener.infrastructure.repositories

import com.github.benmanes.caffeine.cache.AsyncCache
import es.unizar.urlshortener.core.Click
import es.unizar.urlshortener.core.ClickRepositoryService
import es.unizar.urlshortener.core.ShortUrl
import es.unizar.urlshortener.core.ShortUrlRepositoryService
import org.slf4j.LoggerFactory
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.OffsetDateTime
import java.util.concurrent.CompletableFuture

class ClickRepositoryServiceImpl(
    private val clickEntityRepository: ClickEntityRepository,
    private val cache: AsyncCache<String, List<Click>>
) : ClickRepositoryService {

    /**
     * Finds all [Click] entities associated with the given hash.
     *
     * @param hash The hash associated with the shortened URL.
     * @return A [Flux] emitting [Click] entities.
     */
    override fun findAllByHash(hash: String): Flux<Click> {
        val cachedValue = cache.getIfPresent(hash)

        return if (cachedValue != null) {
            Flux.fromIterable(cachedValue.get())
        } else {
            clickEntityRepository.findAllByHash(hash)
                .map { it.toDomain() }
                .collectList()
                .doOnNext { cache.put(hash, CompletableFuture.completedFuture(it)) }
                .flatMapMany { Flux.fromIterable(it) }
        }
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
            .doOnNext {
                cache.asMap().remove(cl.hash) // Invalidate cache
            }
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
    }
}

class ShortUrlRepositoryServiceImpl(
    private val shortUrlEntityRepository: ShortUrlEntityRepository,
    private val entityTemplate: R2dbcEntityTemplate,
    private val cache: AsyncCache<String, ShortUrl>
) : ShortUrlRepositoryService {

    /**
     * Finds a [ShortUrl] entity by its key.
     *
     * @param id The key of the [ShortUrl] entity.
     * @return A [Mono] emitting the found [ShortUrl] entity or empty if not found.
     */
    override fun findByKey(id: String): Mono<ShortUrl> {
        val cachedValue = cache.getIfPresent(id)

        return if (cachedValue != null) {
            Mono.fromFuture(cachedValue)
        } else {
            shortUrlEntityRepository.findByHash(id)
                .map { it.toDomain() }
                .doOnNext { shortUrl ->
                    cache.put(id, CompletableFuture.completedFuture(shortUrl))
                }
        }
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
            .doOnNext { savedShortUrl ->
                cache.asMap().remove(savedShortUrl.hash) // Invalidate cache after save
            }
    }
}
