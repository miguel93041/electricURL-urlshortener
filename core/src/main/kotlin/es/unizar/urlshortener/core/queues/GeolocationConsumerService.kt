@file:Suppress("WildcardImport")
package es.unizar.urlshortener.core.queues

import es.unizar.urlshortener.core.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import reactor.core.publisher.Mono

class GeolocationConsumerService(
    private val geoLocationService: GeoLocationService,
    private val clickRepositoryService: ClickRepositoryService,
    private val shortUrlRepositoryService: ShortUrlRepositoryService
): CoroutineScope {
    override val coroutineContext = Dispatchers.Default

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
