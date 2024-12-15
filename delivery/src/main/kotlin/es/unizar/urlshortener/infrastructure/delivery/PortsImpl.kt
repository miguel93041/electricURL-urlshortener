package es.unizar.urlshortener.infrastructure.delivery

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.google.common.hash.Hashing
import es.unizar.urlshortener.core.*
import org.apache.commons.validator.routines.UrlValidator
import java.nio.charset.StandardCharsets
import es.unizar.urlshortener.core.usecases.UrlAccessibilityCheckUseCase
import reactor.core.publisher.Mono
import java.util.*

/**
 * Implementation of the ValidatorService interface.
 *
 * Validates URLs to ensure they follow the correct format, are safe, and are reachable.
 *
 * @param urlAccessibilityCheckUseCase Use case to check if a URL is reachable.
 * @param urlSafetyService Service to verify if a URL is safe.
 */
class UrlValidatorServiceImpl(
    private val urlAccessibilityCheckUseCase: UrlAccessibilityCheckUseCase,
    private val urlSafetyService: UrlSafetyService
) : UrlValidatorService {

    /**
     * Validates the given URL.
     *
     * @param url The URL to validate.
     * @return A [Result] indicating the validation result.
     */
    override fun validate(url: String): Mono<Result<Unit, UrlError>> {
        if (!urlValidator.isValid(url)) {
            return Mono.just(Err(UrlError.InvalidFormat))
        }

        return urlSafetyService.isSafe(url)
            .flatMap { isSafe ->
                if (!isSafe) {
                    Mono.just(Err(UrlError.Unsafe))
                } else {
                    urlAccessibilityCheckUseCase.isUrlReachable(url)
                        .map { isReachable ->
                            if (isReachable) Ok(Unit) else Err(UrlError.Unreachable)
                        }
                }
            }
    }

    companion object {
        /**
         * A URL validator that supports HTTP and HTTPS schemes.
         */
        private val urlValidator = UrlValidator(arrayOf("http", "https"))
    }
}

/**
 * Implementation of the ValidatorService interface.
 *
 * Validates URLs to ensure they follow the correct format, are safe, and are reachable.
 *
 * @param shortUrlRepositoryService The [ShortUrl] repository.
 */
class HashValidatorServiceImpl(
    private val shortUrlRepositoryService: ShortUrlRepositoryService
) : HashValidatorService {

    /**
     * Validates if the given hash is valid.
     *
     * @param hash The hash to be validated.
     * @return A Mono emitting the result of the validation.
     */
    override fun validate(hash: String): Mono<Result<Unit, HashError>> {
        if (!isValidHash(hash)) {
            Mono.just(Err(HashError.InvalidFormat))
        }

        return shortUrlRepositoryService.findByKey(hash)
            .map { shorturl ->
                if (!shorturl.properties.validation.validated) {
                    Err(HashError.NotValidated)
                } else if (!shorturl.properties.validation.safe) {
                    Err(HashError.Unsafe)
                } else if (!shorturl.properties.validation.reachable) {
                    Err(HashError.Unreachable)
                } else {
                    Ok(Unit)
                }
            }
            .switchIfEmpty(Mono.just(Err(HashError.NotFound)))
    }

    /**
     * Validates the given hash format.
     *
     * @param hash The hash to check.
     * @return A [Boolean] indicating if hash format is valid.
     */
    fun isValidHash(hash: String): Boolean {
        val hashPattern = Regex("^[a-fA-F0-9]{8}$")
        return hashPattern.matches(hash)
    }
}

/**
 * Implementation of the port [HashService].
 */
class HashServiceImpl : HashService {
    /**
     * Generates a random hash using a UUID and the Murmur3 32-bit hashing algorithm.
     *
     * @return a randomly generated hash as a string
     */
    override fun generateRandomHash(): String {
        val randomUUID = UUID.randomUUID().toString()
        return Hashing.murmur3_32_fixed().hashString(randomUUID, StandardCharsets.UTF_8).toString()
    }
}
