@file:Suppress("WildcardImport")

package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.*
import es.unizar.urlshortener.core.queues.GeolocationChannelService
import es.unizar.urlshortener.core.queues.GeolocationConsumerService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import reactor.core.publisher.Mono
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GeolocationServiceTest {

    private lateinit var geoLocationService: GeoLocationService
    private lateinit var clickRepositoryService: ClickRepositoryService
    private lateinit var shortUrlRepositoryService: ShortUrlRepositoryService
    private lateinit var channel: Channel<GeoLocationEvent>
    private lateinit var geolocationChannelService: GeolocationChannelService
    private lateinit var geolocationConsumerService: GeolocationConsumerService
    private var ip = "127.0.0.1"

    @BeforeEach
    fun setUp() {
        geoLocationService = mock()
        clickRepositoryService = mock()
        shortUrlRepositoryService = mock()
        channel = Channel(10)
        geolocationChannelService = GeolocationChannelService(channel)
        geolocationConsumerService = GeolocationConsumerService(
            geoLocationService,
            clickRepositoryService,
            shortUrlRepositoryService
        )
    }


    @Test
    fun `enqueue should add a ClickEvent to the channel`() {

        val event = ClickEvent(ip = ip, clickId = 123L)

        val result = geolocationChannelService.enqueue(event)

        // Assert that the event was successfully enqueued
        assertTrue(result)
        assertEquals(1, channel.tryReceive().getOrNull()?.let { listOf(it) }?.size)
    }


    @Test
    fun `enqueue should fail when the channel is full`() {
        // Fill the channel with events
        repeat(10) {
            geolocationChannelService.enqueue(ClickEvent(ip = "127.0.0.$it", clickId = it.toLong()))
        }

        // Attempt to enqueue one more event
        val result = geolocationChannelService.enqueue(ClickEvent(ip = "127.0.0.11", clickId = 11L))

        // Assert that enqueue fails
        assertFalse(result)
    }


    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `startProcessing should process ClickEvent and update geolocation`() {

        val event = ClickEvent(ip = ip, clickId = 123L)
        channel.trySend(event)

        whenever(geoLocationService.get(ip))
            .thenReturn(Mono.just(GeoLocation(ip = ip, country = "Spain")))
        whenever(clickRepositoryService.updateGeolocation(123L, GeoLocation(ip, "Spain")))
            .thenReturn(Mono.empty())

        // Start processing events
        val job = geolocationConsumerService.startProcessing(channel)

        // Allow time for processing
        Thread.sleep(100)

        // Verify interactions
        verify(geoLocationService).get(ip)
        verify(clickRepositoryService).updateGeolocation(123L, GeoLocation(ip, "Spain"))

        // Assert that the event has been processed
        assertTrue(channel.isEmpty)

        job.cancel()
    }



    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `startProcessing should process HashEvent and update geolocation`() {

        val event = HashEvent(ip = ip, hash = "hash123")
        channel.trySend(event)

        whenever(geoLocationService.get(ip))
            .thenReturn(Mono.just(GeoLocation(ip = ip, country = "USA")))
        whenever(shortUrlRepositoryService.updateGeolocation("hash123", GeoLocation(ip, "USA")))
            .thenReturn(Mono.empty())

        // Start processing events
        val job = geolocationConsumerService.startProcessing(channel)

        // Give the consumer time to process the event
        Thread.sleep(1000) // A small delay to allow processing

        // Verify interactions
        verify(geoLocationService).get(ip)
        verify(shortUrlRepositoryService).updateGeolocation("hash123", GeoLocation(ip, "USA"))

        assertTrue(channel.isEmpty) // Ensure the channel has no pending events

        job.cancel() // Cancel the job to stop the coroutine
    }
}
