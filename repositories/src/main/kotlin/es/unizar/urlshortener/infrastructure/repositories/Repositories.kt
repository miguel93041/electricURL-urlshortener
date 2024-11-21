package es.unizar.urlshortener.infrastructure.repositories

import org.springframework.data.jpa.repository.JpaRepository
import java.time.OffsetDateTime

/**
 * Specification of the repository of [ShortUrlEntity].
 *
 * **Note**: Spring Boot is able to discover this [JpaRepository] without further configuration.
 */
interface ShortUrlEntityRepository : JpaRepository<ShortUrlEntity, String> {
    /**
     * Finds a [ShortUrlEntity] by its hash.
     *
     * @param hash The hash of the [ShortUrlEntity].
     * @return The found [ShortUrlEntity] or null if not found.
     */
    fun findByHash(hash: String): ShortUrlEntity?
}

/**
 * Specification of the repository of [ClickEntity].
 *
 * **Note**: Spring Boot is able to discover this [JpaRepository] without further configuration.
 */
interface ClickEntityRepository : JpaRepository<ClickEntity, Long> {

    /**
     * Finds all [ClickEntity] instances associated with the given hash.
     *
     * @param hash The hash associated with the shortened URL.
     * @return A list of [ClickEntity] instances.
     */
    fun findAllByHash(hash: String): List<ClickEntity>

    /**
     * Counts the number of clicks associated with a specific hash created after a certain date and time.
     *
     * @param hash The hash associated with the shortened URL.
     * @param createdAfter The date and time after which clicks are counted.
     * @return The number of clicks for the specified hash created after the given date and time.
     */
    fun countByHashAndCreatedAfter(hash: String, createdAfter: OffsetDateTime): Long
}
