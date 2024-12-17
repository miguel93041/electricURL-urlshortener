package es.unizar.urlshortener.core.queues

import es.unizar.urlshortener.core.UrlValidationEvent
import kotlinx.coroutines.channels.Channel

class UrlValidationChannelService(
    private val channel: Channel<UrlValidationEvent>
) {
    fun enqueue(event: UrlValidationEvent): Boolean {
        val result = channel.trySend(event)
        return result.isSuccess
    }
}
