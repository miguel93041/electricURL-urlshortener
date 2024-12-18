package es.unizar.urlshortener.core

import com.github.michaelbull.result.Result
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.OffsetDateTime

/**
 * [ClickRepositoryService] is the port to the repository that provides persistence to [Click].
 */
interface ClickRepositoryService {

    /**
     * Finds all [Click] entities associated with the given hash.
     *
     * @param hash The hash associated with the shortened URL.
     * @return A [Flux] of [Click] entities.
     */
    fun findAllByHash(hash: String): Flux<Click>

    /**
     * Saves a [Click] entity to the repository.
     *
     * @param cl The [Click] entity to be saved.
     * @return A [Mono] emitting the saved [Click] entity.
     */
    fun save(cl: Click): Mono<Click>

    /**
     * Counts the number of clicks associated with a specific hash created after a certain date and time.
     *
     * @param hash The hash associated with the shortened URL.
     * @param createdAfter The date and time after which clicks are counted.
     * @return A [Mono] emitting the number of clicks for the specified hash created after the given date and time.
     */
    fun countClicksByHashAfter(hash: String, createdAfter: OffsetDateTime): Mono<Long>

    /**
     * Updates the geolocation details of a [Click] entity in the repository and clears the cache for
     * the updated entity.
     *
     * This method updates the fields `ip` and `country` of a [Click] identified by its `id` in the database.
     *
     * @param id The unique id identifying the [Click] to be updated.
     * @param geolocation The [GeoLocation] object containing the new geolocation details (IP and country).
     * @return A Mono<Void> indicating the completion of the operation.
     */
    fun updateGeolocation(id: Long, geolocation: GeoLocation): Mono<Void>

    /**
     * Updates the browser and platform details of a [Click] entity in the repository and clears the cache for
     * the updated entity.
     *
     * This method updates the fields `browser` and `platform` of a [Click] identified by its `id` in the database.
     *
     * @param id The unique id identifying the [Click] to be updated.
     * @param browserPlatform The [BrowserPlatform] object containing the new browser and platform details.
     * @return A Mono<Void> indicating the completion of the operation.
     */
    fun updateBrowserPlatform(id: Long, browserPlatform: BrowserPlatform): Mono<Void>
}

/**
 * [ShortUrlRepositoryService] is the port to the repository that provides management to [ShortUrl].
 */
interface ShortUrlRepositoryService {

    /**
     * Finds a [ShortUrl] entity by its key.
     *
     * @param id The key of the [ShortUrl] entity.
     * @return A [Mono] emitting the found [ShortUrl] entity or empty if not found.
     */
    fun findByKey(id: String): Mono<ShortUrl>

    /**
     * Creates a new [ShortUrl] entity to the repository.
     *
     * @param su The [ShortUrl] entity to be created.
     * @return A [Mono] emitting the created [ShortUrl] entity.
     */
    fun create(su: ShortUrl): Mono<ShortUrl>

    /**
     * Updates the validation status of a [ShortUrl] entity in the repository.
     *
     * This method updates the fields `reachable`, `safe`, and `validated` of a [ShortUrl] identified by its `hash`
     * in the database.
     *
     * @param hash The unique hash identifying the [ShortUrl] to be updated.
     * @param validation The [ShortUrlValidation] object containing the new validation status.
     * @return A Mono<Void> indicating the completion of the operation.
     */
    fun updateValidation(hash: String, validation: ShortUrlValidation): Mono<Void>

    /**
     * Updates the geolocation details of a [ShortUrl] entity in the repository and clears the cache for
     * the updated entity.
     *
     * This method updates the fields `ip` and `country` of a [ShortUrl] identified by its `hash` in the database.
     * Additionally, it removes the corresponding cache entry for the given `hash` after a successful update.
     *
     * @param hash The unique hash identifying the [ShortUrl] to be updated.
     * @param geolocation The [GeoLocation] object containing the new geolocation details (IP and country).
     * @return A Mono<Void> indicating the completion of the operation.
     */
    fun updateGeolocation(hash: String, geolocation: GeoLocation): Mono<Void>
}

/**
 * [UrlValidatorService] is the port to the service that validates if an url can be shortened.
 */
fun interface UrlValidatorService {

    /**
     * Validates the given URL.
     *
     * @param url The URL to validate.
     * @return A [Mono] emitting the [Result] indicating the validation result.
     */
    fun validate(url: String): Mono<Result<Unit, UrlError>>
}

/**
 * [HashValidatorService] is the port for the service that validates if a hash is valid.
 */
fun interface HashValidatorService {

    /**
     * Validates if the given hash is valid.
     *
     * @param hash The hash to be validated.
     * @return A [Mono] emitting a [Result] indicating the validation result.
     */
    fun validate(hash: String): Mono<Result<Unit, HashError>>
}

/**
 * [HashService] is the port to the service that creates a hash from a URL.
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

    /**
     * Retrieves geolocation details for a given IP address.
     *
     * @param ip The IP address for which geolocation information is requested.
     * @return A [Mono] emitting the [GeoLocation] containing the retrieved information.
     */
    fun get(ip: String): Mono<GeoLocation>
}

/**
 * [UrlSafetyService] defines the contract for a service that checks the safety of a given URL.
 *
 * This service is responsible for validating if a URL is safe to access, ensuring that it does not pose
 * any security risks.
 */
interface UrlSafetyService {

    /**
     * Checks if a URL is safe.
     *
     * @param url The URL to be checked.
     * @return A [Mono] emitting a boolean value indicating whether the URL is safe or not.
     */
    fun isSafe(url: String): Mono<Boolean>
}
