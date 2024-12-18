@file:Suppress("WildcardImport")
package es.unizar.urlshortener.core.queues

import com.github.michaelbull.result.unwrapError
import es.unizar.urlshortener.core.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

class UrlValidationConsumerService(
    private val urlValidatorService: UrlValidatorService,
    private val shortUrlRepositoryService: ShortUrlRepositoryService
): CoroutineScope {
    override val coroutineContext = Dispatchers.Default

    fun startProcessing(channel: Channel<UrlValidationEvent>): Job = launch {
        for (event in channel) {
            val validationMono = urlValidatorService.validate(event.url)

            val updateMono = validationMono.flatMap { validationResult ->
                if (validationResult.isErr) {
                    val error = validationResult.unwrapError()
                    val validation = when (error) {
                        UrlError.InvalidFormat, UrlError.Unreachable ->
                            ShortUrlValidation(safe = true, reachable = false, validated = true)
                        UrlError.Unsafe ->
                            ShortUrlValidation(safe = false, reachable = true, validated = true)
                    }
                    shortUrlRepositoryService.updateValidation(event.hash, validation)
                } else {
                    val validation = ShortUrlValidation(safe = true, reachable = true, validated = true)
                    shortUrlRepositoryService.updateValidation(event.hash, validation)
                }
            }

            updateMono.subscribe()
        }
    }
}
