package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.Click
import es.unizar.urlshortener.core.ClickProperties
import es.unizar.urlshortener.core.ClickRepositoryService
import reactor.core.publisher.Mono

/**
 * Logs that a user has requested the redirection identified by a key.
 */
interface LogClickUseCase {

    /**
     * Logs a click event for the given key.
     *
     * @param key The key associated with the redirection.
     * @return A [Mono] emitting the logged [Click] object.
     */
    fun logClick(key: String): Mono<Click>
}

/**
 * [LogClickUseCaseImpl] is an implementation of [LogClickUseCase].
 *
 * Saves click events into a repository using [ClickRepositoryService].
 *
 * @property clickRepository The repository service used for saving click events.
 */
class LogClickUseCaseImpl(
    private val clickRepository: ClickRepositoryService
) : LogClickUseCase {

    /**
     * Logs a click event for the specified key and saves it into the repository.
     *
     * @param key The key associated with the redirection.
     * @return A [Mono] emitting the saved [Click] object.
     */
    override fun logClick(key: String): Mono<Click> {
        val cl = Click(
            hash = key,
            properties = ClickProperties()
        )

        return clickRepository.save(cl)
    }
}
