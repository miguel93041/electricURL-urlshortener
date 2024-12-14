package es.unizar.urlshortener.core

import com.github.michaelbull.result.Result
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.OffsetDateTime

/**
 * [ClickRepositoryService] is the port to the repository that provides persistence to [Clicks][Click].
 */
interface ClickRepositoryService {
    /**
     * Finds all [Click] entities associated with the given hash.
     *
     * @param hash The hash associated with the shortened URL.
     * @return A list of [Click] entities.
     */
    fun findAllByHash(hash: String): Flux<Click>

    /**
     * Saves a [Click] entity to the repository.
     *
     * @param cl The [Click] entity to be saved.
     * @return The saved [Click] entity.
     */
    fun save(cl: Click): Mono<Click>

    /**
     * Counts the number of clicks associated with a specific hash created after a certain date and time.
     *
     * @param hash The hash associated with the shortened URL.
     * @param createdAfter The date and time after which clicks are counted.
     * @return The number of clicks for the specified hash created after the given date and time.
     */
    fun countClicksByHashAfter(hash: String, createdAfter: OffsetDateTime): Mono<Long>
}

/**
 * [ShortUrlRepositoryService] is the port to the repository that provides management to [ShortUrl][ShortUrl].
 */
interface ShortUrlRepositoryService {
    /**
     * Finds a [ShortUrl] entity by its key.
     *
     * @param id The key of the [ShortUrl] entity.
     * @return The found [ShortUrl] entity or null if not found.
     */
    fun findByKey(id: String): Mono<ShortUrl>

    /**
     * Saves a [ShortUrl] entity to the repository.
     *
     * @param su The [ShortUrl] entity to be saved.
     * @return The saved [ShortUrl] entity.
     */
    fun save(su: ShortUrl): Mono<ShortUrl>
}

/**
 * [UrlValidatorService] is the port to the service that validates if an url can be shortened.
 *
 */
interface UrlValidatorService {
    /**
     * Validates if the given URL can be shortened.
     *
     * @param url The URL to be validated.
     * @return The result of the validation.
     */
    fun validate(url: String): Mono<Result<Unit, UrlError>>
}

/**
 * [UrlValidatorService] is the port to the service that validates if an url can be shortened.
 *
 */
interface HashValidatorService {
    /**
     * Validates if the given hash is valid.
     *
     * @param hash The hash to be validated.
     * @return The result of the validation.
     */
    fun validate(hash: String): Mono<Result<Unit, HashError>>
}

/**
 * [HashService] is the port to the service that creates a hash from a URL.
 *
 * **Note**: It is a design decision to create this port. It could be part of the core.
 */
interface HashService {
    /**
     * Generates a random hash using a UUID and the Murmur3 32-bit hashing algorithm.
     *
     * @return a randomly generated hash as a string
     */
    fun generateRandomHash(): String
}

/**
 * [GeoLocationService] defines the contract for a service that retrieves geographical
 * information based on an IP address.
 */
interface GeoLocationService {
    fun get(ip: String): Mono<GeoLocation>
}

interface UrlSafetyService {
    fun isSafe(url: String): Mono<Boolean>
}
