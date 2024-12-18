package es.unizar.urlshortener.core.queues

import es.unizar.urlshortener.core.UrlValidationEvent
import kotlinx.coroutines.channels.Channel

/**
 * Service that handles the channelization of URL validation events.
 * @param channel The channel from which URL validation events will be received.
 */
class UrlValidationChannelService(
    private val channel: Channel<UrlValidationEvent>
) {
    /**
     * Enqueues a URL validation event to the channel.
     * @param event The URL validation event to enqueue.
     * @return `true` if successfully enqueued, `false` otherwise.
     */
    fun enqueue(event: UrlValidationEvent): Boolean {
        val result = channel.trySend(event)
        return result.isSuccess
    }
}
