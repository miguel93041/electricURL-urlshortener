@file:Suppress("WildcardImport")
package es.unizar.urlshortener.core.queues

import es.unizar.urlshortener.core.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import reactor.core.publisher.Mono

/**
 * Asynchronous service for processing geolocation events.
 * @param geoLocationService Service to obtain geolocation information.
 * @param clickRepositoryService Service to manage clicks associated with shortened URLs.
 * @param shortUrlRepositoryService Service to update geolocation information on shortened URLs.
 */
class GeolocationConsumerService(
    private val geoLocationService: GeoLocationService,
    private val clickRepositoryService: ClickRepositoryService,
    private val shortUrlRepositoryService: ShortUrlRepositoryService
): CoroutineScope {
    override val coroutineContext = Dispatchers.Default

    /**
     * Starts processing geolocation events.
     * @param channel Channel from which geolocation events will be received.
     * @return A [Job] handling the event processing.
     */
    fun startProcessing(channel: Channel<GeoLocationEvent>): Job = launch {
        for (event in channel) {
            val geoLocationMono: Mono<GeoLocation> = geoLocationService.get(event.ip)
            val updateMono: Mono<Void> = when (event) {
                is ClickEvent -> geoLocationMono.flatMap { geo ->
                    clickRepositoryService.updateGeolocation(event.clickId, geo)
                }
                is HashEvent -> geoLocationMono.flatMap { geo ->
                    shortUrlRepositoryService.updateGeolocation(event.hash, geo)
                }
            }
            updateMono.subscribe()
        }
    }
}
