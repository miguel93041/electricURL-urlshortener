import es.unizar.urlshortener.core.ClickRepositoryService
import es.unizar.urlshortener.core.TooManyRequestsException
import es.unizar.urlshortener.core.safeCall
import java.time.OffsetDateTime

/**
 * Interface for handling redirection limits for shortened URLs.
 */
interface RedirectionLimitUseCase {

    /**
     * Checks whether the redirection limit has been reached for a specific URL identifier.
     *
     * @param urlId The identifier of the shortened URL.
     * @throws TooManyRequestsException if limit is reached
     */
    fun isRedirectionLimit(urlId: String): Boolean
}

/**
 * Implementation of [RedirectionLimitUseCase].
 *
 * Checks and updates the redirection count for shortened URLs within a time frame using the ClickRepositoryService.
 *
 * @property redirectionLimit The maximum number of allowed redirections within the time frame.
 * @property timeFrameInSeconds The time frame in seconds for which the limit applies.
 * @property clickRepositoryService The service used to count clicks.
 */
class RedirectionLimitUseCaseImpl(
    private val redirectionLimit: Int = 10,
    private val timeFrameInSeconds: Long = 60,
    private val clickRepositoryService: ClickRepositoryService
) : RedirectionLimitUseCase {

    /**
     * Checks if the current redirection count for a URL identifier has reached or exceeded the limit
     * within the defined time frame.
     *
     * @param urlId The identifier of the shortened URL.
     * @throws TooManyRequestsException if limit is reached
     */
    override fun isRedirectionLimit(urlId: String): Boolean {
        val count = safeCall {
            val now = OffsetDateTime.now()
            val startTime = now.minusSeconds(timeFrameInSeconds)

            clickRepositoryService.countClicksByHashAfter(urlId, startTime)
        }

        return count >= redirectionLimit
    }
}
