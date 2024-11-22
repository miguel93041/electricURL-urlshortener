package es.unizar.urlshortener.infrastructure.delivery

import com.google.common.hash.Hashing
import es.unizar.urlshortener.core.HashService
import es.unizar.urlshortener.core.ValidatorService
import org.apache.commons.validator.routines.UrlValidator
import java.nio.charset.StandardCharsets
import es.unizar.urlshortener.core.usecases.UrlAccessibilityCheckUseCase
import es.unizar.urlshortener.core.UrlSafetyService
import es.unizar.urlshortener.core.ValidatorResult

/**
 * Implementation of the ValidatorService interface.
 *
 * Validates URLs to ensure they follow the correct format, are safe, and are reachable.
 *
 * @param urlAccessibilityCheckUseCase Use case to check if a URL is reachable.
 * @param urlSafetyService Service to verify if a URL is safe.
 */
class ValidatorServiceImpl(
    private val urlAccessibilityCheckUseCase: UrlAccessibilityCheckUseCase,
    private val urlSafetyService: UrlSafetyService
) : ValidatorService {

    /**
     * Validates the given URL.
     *
     * @param url The URL to validate.
     * @return A [ValidatorResult] indicating the validation result.
     */
    override fun validate(url: String): ValidatorResult {
        return when {
            !urlValidator.isValid(url) -> ValidatorResult.NOT_VALID_FORMAT
            !urlSafetyService.isSafe(url) -> ValidatorResult.NOT_SAFE
            !urlAccessibilityCheckUseCase.isUrlReachable(url) -> ValidatorResult.NOT_REACHABLE
            else -> ValidatorResult.VALID
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
 * Implementation of the port [HashService].
 */
class HashServiceImpl : HashService {
    /**
     * Generates a hash for the given URL using the Murmur3 32-bit hashing algorithm.
     *
     * @param url the URL to hash
     * @return the hash of the URL as a string
     */
    override fun hasUrl(url: String) = Hashing.murmur3_32_fixed().hashString(url, StandardCharsets.UTF_8).toString()
}
