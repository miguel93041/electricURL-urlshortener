@file:Suppress("WildcardImport")
package es.unizar.urlshortener.core.queues

import es.unizar.urlshortener.core.*
import jakarta.annotation.PostConstruct
import kotlinx.coroutines.launch

class GeolocationConsumerService(
    private val channelService: GeolocationChannelService,
    private val geoLocationService: GeoLocationService,
    private val clickRepositoryService: ClickRepositoryService,
    private val shortUrlRepositoryService: ShortUrlRepositoryService,
    private val coroutineScopeManager: CoroutineScopeManager
) {

    @PostConstruct
    fun startConsuming() {
        coroutineScopeManager.applicationScope.launch {
            for (event in channelService.getChannel()) {
                val geoLocationMono = geoLocationService.get(event.ip)
                val updateMono = when (event) {
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
}
