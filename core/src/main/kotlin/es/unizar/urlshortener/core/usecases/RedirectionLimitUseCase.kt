package es.unizar.urlshortener.core.usecases

import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * Interface for handling redirection limits for shortened URLs.
 */
interface RedirectionLimitUseCase {
    /**
     * Checks whether the redirection limit has been reached for a specific URL identifier.
     *
     * @param urlId The identifier of the shortened URL.
     * @return True if the redirection limit is reached, false otherwise.
     */
    fun isRedirectionLimit(urlId: String): Boolean

    /**
     * Increments the redirection count for a given URL identifier.
     *
     * @param urlId The identifier of the shortened URL.
     */
    fun incrementRedirectionCount(urlId: String)
}

/**
 * Interface for tracking redirection counts in a repository.
 */
interface RedirectionCountRepository {
    /**
     * Retrieves the current redirection count for a given URL identifier.
     *
     * @param urlId The identifier of the shortened URL.
     * @return The redirection count, or null if no count exists.
     */
    fun getCount(urlId: String): Int?

    /**
     * Increments the redirection count for a given URL identifier.
     *
     * @param urlId The identifier of the shortened URL.
     */
    fun incrementCount(urlId: String)
}

/**
 * Implementation of [RedirectionLimitUseCase] that checks and updates
 * the redirection count for shortened URLs within a time frame.
 *
 * @param redirectionLimit The maximum number of allowed redirections within the time frame.
 * @param timeFrameInSeconds The time frame in seconds for which the limit applies.
 * @param redirectionCountRepository The repository used to store and retrieve redirection counts.
 */
class RedirectionLimitUseCaseImpl(
    private val redirectionLimit: Int = 10,
    private val timeFrameInSeconds: Long = 60,
    private val redirectionCountRepository: RedirectionCountRepository
) : RedirectionLimitUseCase {

    private val redirectionTimestamps = ConcurrentHashMap<String, MutableList<Instant>>()

    /**
     * Checks if the current redirection count for a URL identifier has reached or exceeded the limit
     * within the defined time frame.
     *
     * @param urlId The identifier of the shortened URL.
     * @return True if the redirection limit is reached, false otherwise.
     */
    override fun isRedirectionLimit(urlId: String): Boolean {
        val now = Instant.now()
        val timestamps = redirectionTimestamps.getOrPut(urlId) { mutableListOf() }

        // Remove timestamps outside the time frame
        timestamps.removeIf { it.plusSeconds(timeFrameInSeconds).isBefore(now) }

        return timestamps.size >= redirectionLimit
    }

    /**
     * Increments the redirection count for the specified URL identifier and throws an exception
     * with a 429 status code if the limit is exceeded.
     *
     * @param urlId The identifier of the shortened URL.
     */
    override fun incrementRedirectionCount(urlId: String) {
        val now = Instant.now()
        val timestamps = redirectionTimestamps.getOrPut(urlId) { mutableListOf() }

        // Check if the limit is reached before incrementing
        if (isRedirectionLimit(urlId)) {
            throw TooManyRequestsException("Redirection limit exceeded for URL ID: $urlId")
        }

        // Add the current timestamp
        timestamps.add(now)
        // Also update the repository (for compatibility)
        redirectionCountRepository.incrementCount(urlId)
    }
}

/**
 * Exception thrown when the redirection limit is exceeded.
 * Maps to HTTP 429 Too Many Requests.
 */
class TooManyRequestsException(message: String) : RuntimeException(message)