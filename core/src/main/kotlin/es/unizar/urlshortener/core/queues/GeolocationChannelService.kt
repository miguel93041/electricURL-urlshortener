package es.unizar.urlshortener.core.queues

import es.unizar.urlshortener.core.GeoLocationEvent
import kotlinx.coroutines.channels.Channel

class GeolocationChannelService (private val queueCapacity: Int = 10000) {
    private val channel = Channel<GeoLocationEvent>(capacity = queueCapacity)

    fun enqueue(event: GeoLocationEvent): Boolean {
        val result = channel.trySend(event)
        return result.isSuccess
    }

    fun getChannel() = channel
}
