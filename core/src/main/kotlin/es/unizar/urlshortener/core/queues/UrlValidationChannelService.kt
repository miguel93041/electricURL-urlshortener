package es.unizar.urlshortener.core.queues

import es.unizar.urlshortener.core.UrlValidationEvent
import kotlinx.coroutines.channels.Channel

class UrlValidationChannelService(private val queueCapacity: Int = 10000) {
    private val channel = Channel<UrlValidationEvent>(capacity = queueCapacity)

    fun enqueue(event: UrlValidationEvent): Boolean {
        val result = channel.trySend(event)
        return result.isSuccess
    }

    fun getChannel() = channel
}
