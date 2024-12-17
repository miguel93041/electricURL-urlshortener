package es.unizar.urlshortener.core.queues

import es.unizar.urlshortener.core.GeoLocationEvent
import kotlinx.coroutines.channels.Channel

class GeolocationChannelService(
    private val channel: Channel<GeoLocationEvent>
) {
    fun enqueue(event: GeoLocationEvent): Boolean {
        val result = channel.trySend(event)
        return result.isSuccess
    }
}
