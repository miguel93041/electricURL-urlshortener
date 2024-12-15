package es.unizar.urlshortener.infrastructure.repositories

import com.github.benmanes.caffeine.cache.AsyncCache
import es.unizar.urlshortener.core.*
import org.slf4j.LoggerFactory
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate
import org.springframework.data.relational.core.query.Criteria
import org.springframework.data.relational.core.query.Query
import org.springframework.data.relational.core.query.Update
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.OffsetDateTime
import java.util.concurrent.CompletableFuture

class ClickRepositoryServiceImpl(
    private val clickEntityRepository: ClickEntityRepository,
    private val entityTemplate: R2dbcEntityTemplate,
    private val cache: AsyncCache<String, List<Click>>
) : ClickRepositoryService {

    companion object {
        private val logger = LoggerFactory.getLogger(ClickRepositoryServiceImpl::class.java)
    }

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
                .doOnNext {
                    cache.put(hash, CompletableFuture.completedFuture(it))
                    logger.info("Cache updated with clicks for hash=${hash}")
                }
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
                logger.info("Click saved for hash=${cl}, cache invalidated")
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

    /**
     * Updates the geolocation details of a [Click] entity in the repository and clears the cache for
     * the updated entity.
     *
     * This method updates the fields `ip` and `country` of a [Click] identified by its `id` in the database.
     *
     * @param id The unique id identifying the [Click] to be updated.
     * @param geolocation The [GeoLocation] object containing the new geolocation details (IP and country).
     * @return A [Mono<Void>] indicating the completion of the operation.
     */
    override fun updateGeolocation(id: Long, geolocation: GeoLocation): Mono<Void> {
        return entityTemplate.update(ClickEntity::class.java)
            .matching(Query.query(Criteria.where("id").`is`(id)))
            .apply(
                Update
                    .update("ip", geolocation.ip)
                    .set("country", geolocation.country)
            )
            .flatMap { updateResult ->
                if (updateResult > 0) {
                    entityTemplate.selectOne(
                        Query.query(Criteria.where("id").`is`(id)),
                        ClickEntity::class.java
                    )
                } else {
                    Mono.empty()
                }
            }
            .doOnNext { updatedEntity ->
                val hash = updatedEntity.hash
                cache.asMap().remove(hash)
                logger.info("Geolocation updated for hash=${hash} with geolocation=${geolocation}, cache invalidated")
            }
            .then()
    }

    /**
     * Updates the browser and platform details of a [Click] entity in the repository and clears the cache for
     * the updated entity.
     *
     * This method updates the fields `browser` and `platform` of a [Click] identified by its `id` in the database.
     *
     * @param id The unique id identifying the [Click] to be updated.
     * @param geolocation The [GeoLocation] object containing the new browser and platform details.
     * @return A [Mono<Void>] indicating the completion of the operation.
     */
    override fun updateBrowserPlatform(id: Long, browserPlatform: BrowserPlatform): Mono<Void> {
        return entityTemplate.update(ClickEntity::class.java)
            .matching(Query.query(Criteria.where("id").`is`(id)))
            .apply(
                Update
                    .update("browser", browserPlatform.browser)
                    .set("platform", browserPlatform.platform)
            )
            .flatMap { updateResult ->
                if (updateResult > 0) {
                    entityTemplate.selectOne(
                        Query.query(Criteria.where("id").`is`(id)),
                        ClickEntity::class.java
                    )
                } else {
                    Mono.empty()
                }
            }
            .doOnNext { updatedEntity ->
                val hash = updatedEntity.hash
                cache.asMap().remove(hash)
                logger.info("Browser and platform updated for hash=${hash} with browserPlatform=${browserPlatform}, cache invalidated")
            }
            .then()
    }
}

class ShortUrlRepositoryServiceImpl(
    private val shortUrlEntityRepository: ShortUrlEntityRepository,
    private val entityTemplate: R2dbcEntityTemplate,
    private val cache: AsyncCache<String, ShortUrl>
) : ShortUrlRepositoryService {

    companion object {
        private val logger = LoggerFactory.getLogger(ShortUrlRepositoryServiceImpl::class.java)
    }

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
                    logger.info("Cache updated for ShortUrl with hash=${id}")
                }
        }
    }

    /**
     * Creates a new [ShortUrl] entity to the repository.
     *
     * @param su The [ShortUrl] entity to be created.
     * @return A [Mono] emitting the created [ShortUrl] entity.
     */
    override fun create(su: ShortUrl): Mono<ShortUrl> {
        return entityTemplate.insert(ShortUrlEntity::class.java)
            .using(su.toEntity())
            .map { it.toDomain() }
            .doOnNext { savedShortUrl ->
                cache.asMap().remove(savedShortUrl.hash) // Invalidate cache after create
                logger.info("ShortUrl created with hash=${savedShortUrl}, cache invalidated")
            }
    }

    /**
     * Updates the validation status of a [ShortUrl] entity in the repository.
     *
     * This method updates the fields `reachable`, `safe`, and `validated` of a [ShortUrl] identified by its `hash`
     * in the database.
     *
     * @param hash The unique hash identifying the [ShortUrl] to be updated.
     * @param validation The [ShortUrlValidation] object containing the new validation status.
     * @return A [Mono<Void>] indicating the completion of the operation.
     */
    override fun updateValidation(hash: String, validation: ShortUrlValidation): Mono<Void> {
         return entityTemplate.update(ShortUrlEntity::class.java)
            .matching(Query.query(Criteria.where("hash").`is`(hash)))
            .apply(
                Update
                .update("reachable", validation.reachable)
                .set("safe", validation.safe)
                .set("validated", validation.validated)
            )
            .doOnSuccess {
                cache.asMap().remove(hash) // Invalidate cache after update
                logger.info("Validation updated for hash=${hash} with validation=${validation}, cache invalidated")
            }
            .then()
    }

    /**
     * Updates the geolocation details of a [ShortUrl] entity in the repository and clears the cache for
     * the updated entity.
     *
     * This method updates the fields `ip` and `country` of a [ShortUrl] identified by its `hash` in the database.
     * Additionally, it removes the corresponding cache entry for the given `hash` after a successful update.
     *
     * @param hash The unique hash identifying the [ShortUrl] to be updated.
     * @param geolocation The [GeoLocation] object containing the new geolocation details (IP and country).
     * @return A [Mono<Void>] indicating the completion of the operation.
     */
    override fun updateGeolocation(hash: String, geolocation: GeoLocation): Mono<Void> {
        return entityTemplate.update(ShortUrlEntity::class.java)
            .matching(Query.query(Criteria.where("hash").`is`(hash)))
            .apply(
                Update
                    .update("ip", geolocation.ip)
                    .set("country", geolocation.country)
            )
            .doOnSuccess {
                cache.asMap().remove(hash) // Invalidate cache after update
                logger.info("Geolocation updated for hash=${hash} with geolocation=${geolocation}, cache invalidated")
            }
            .then()
    }
}
