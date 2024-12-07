package es.unizar.urlshortener.infrastructure.delivery

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.google.common.hash.Hashing
import es.unizar.urlshortener.core.*
import org.apache.commons.validator.routines.UrlValidator
import java.nio.charset.StandardCharsets
import es.unizar.urlshortener.core.usecases.UrlAccessibilityCheckUseCase

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
    override fun validate(url: String): Result<Unit, UrlError> {
        return when {
            !urlValidator.isValid(url) -> Err(UrlError.InvalidFormat)
            !urlSafetyService.isSafe(url) -> Err(UrlError.Unsafe)
            !urlAccessibilityCheckUseCase.isUrlReachable(url) -> Err(UrlError.Unreachable)
            else -> Ok(Unit)
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
     * @return The result of the validation.
     */
    override fun validate(hash: String): Result<Unit, HashError> {
        return when {
            !isValidHash(hash) -> Err(HashError.InvalidFormat)
            safeCall { shortUrlRepositoryService.findByKey(hash) } == null -> Err(HashError.NotFound)
            else -> Ok(Unit)
        }
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
     * Generates a hash for the given URL using the Murmur3 32-bit hashing algorithm.
     *
     * @param url the URL to hash
     * @return the hash of the URL as a string
     */
    override fun hashUrl(url: String) = Hashing.murmur3_32_fixed().hashString(url, StandardCharsets.UTF_8).toString()
}
