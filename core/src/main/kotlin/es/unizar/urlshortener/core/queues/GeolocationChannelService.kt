package es.unizar.urlshortener.core.queues

import es.unizar.urlshortener.core.GeoLocationEvent
import kotlinx.coroutines.channels.Channel

/**
 * Service that handles the channelization of geolocation events.
 * @param channel The channel from which geolocation events will be received.
 */
class GeolocationChannelService(
    private val channel: Channel<GeoLocationEvent>
) {
    /**
     * Enqueues a geolocation event to the channel.
     * @param event The geolocation event to enqueue.
     * @return `true` if successfully enqueued, `false` otherwise.
     */
    fun enqueue(event: GeoLocationEvent): Boolean {
        val result = channel.trySend(event)
        return result.isSuccess
    }
}
