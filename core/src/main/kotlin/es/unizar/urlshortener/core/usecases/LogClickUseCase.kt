package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.Click
import es.unizar.urlshortener.core.ClickProperties
import es.unizar.urlshortener.core.ClickRepositoryService

/**
 * Log that somebody has requested the redirection identified by a key.
 */
interface LogClickUseCase {
    /**
     * Logs a click event for the given key and click properties.
     *
     * @param key The key associated with the redirection.
     * @param data The properties of the click event.
     */
    fun logClick(key: String, data: ClickProperties)
}

/**
 * Implementation of [LogClickUseCase].
 *
 * Saves click events into a repository using [ClickRepositoryService].
 *
 * @property clickRepository The repository service used for saving click events.
 */
class LogClickUseCaseImpl(
    private val clickRepository: ClickRepositoryService
) : LogClickUseCase {
    /**
     * Logs a click event for the given key and click properties.
     *
     * @param key The key associated with the redirection.
     * @param data The properties of the click event.
     */
    override fun logClick(key: String, data: ClickProperties) {
        val cl = Click(
            hash = key,
            properties = ClickProperties(
                ip = data.ip,
                country = data.country,
                browser = data.browser,
                platform = data.platform
            )
        )
        runCatching {
            clickRepository.save(cl)
        }
    }
}
