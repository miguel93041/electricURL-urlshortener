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
    fun checkRedirectionLimit(urlId: String)
}

/**
 * Implementation of [RedirectionLimitUseCase] that checks and updates
 * the redirection count for shortened URLs within a time frame using the ClickRepositoryService.
 *
 * @param redirectionLimit The maximum number of allowed redirections within the time frame.
 * @param timeFrameInSeconds The time frame in seconds for which the limit applies.
 * @param clickRepositoryService The service used to count clicks.
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
    override fun checkRedirectionLimit(urlId: String) {
        val count = safeCall {
            val now = OffsetDateTime.now()
            val startTime = now.minusSeconds(timeFrameInSeconds)

            clickRepositoryService.countClicksByHashAfter(urlId, startTime)
        }

        if (count >= redirectionLimit) {
            throw TooManyRequestsException(urlId)
        }
    }
}
