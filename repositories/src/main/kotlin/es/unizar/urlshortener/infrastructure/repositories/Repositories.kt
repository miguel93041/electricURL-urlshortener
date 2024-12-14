package es.unizar.urlshortener.infrastructure.repositories

import org.springframework.data.r2dbc.repository.R2dbcRepository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.OffsetDateTime

/**
 * Specification of the reactive repository of [ShortUrlEntity].
 */
interface ShortUrlEntityRepository : R2dbcRepository<ShortUrlEntity, String> {
    /**
     * Finds a [ShortUrlEntity] by its hash.
     *
     * @param hash The hash of the [ShortUrlEntity].
     * @return A [Mono] emitting the found [ShortUrlEntity], or [Mono.empty] if not found.
     */
    fun findByHash(hash: String): Mono<ShortUrlEntity>
}

/**
 * Specification of the reactive repository of [ClickEntity].
 */
interface ClickEntityRepository : R2dbcRepository<ClickEntity, Long> {

    /**
     * Finds all [ClickEntity] instances associated with the given hash.
     *
     * @param hash The hash associated with the shortened URL.
     * @return A [Flux] emitting [ClickEntity] instances.
     */
    fun findAllByHash(hash: String): Flux<ClickEntity>

    /**
     * Counts the number of clicks associated with a specific hash created after a certain date and time.
     *
     * @param hash The hash associated with the shortened URL.
     * @param createdAfter The date and time after which clicks are counted.
     * @return A [Mono] emitting the number of clicks.
     */
    fun countByHashAndCreatedAfter(hash: String, createdAfter: OffsetDateTime): Mono<Long>
}
