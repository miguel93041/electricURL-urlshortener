@file:Suppress("WildcardImport")
package es.unizar.urlshortener.core.usecases

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import es.unizar.urlshortener.core.*
import es.unizar.urlshortener.core.queues.UrlValidationConsumerService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import reactor.core.publisher.Mono
import kotlin.test.assertEquals

class UrlValidationConsumerServiceTest {

    private lateinit var urlValidatorService: UrlValidatorService
    private lateinit var shortUrlRepositoryService: ShortUrlRepositoryService
    private lateinit var urlValidationConsumerService: UrlValidationConsumerService
    private lateinit var channel: Channel<UrlValidationEvent>

    @BeforeEach
    fun setUp() {
        urlValidatorService = mock()
        shortUrlRepositoryService = mock()
        channel = Channel(10)
        urlValidationConsumerService = UrlValidationConsumerService(
            urlValidatorService,
            shortUrlRepositoryService
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `startProcessing should update validation as unreachable when URL format is invalid`() = runTest {
        val url = "invalid-url"
        val hash = "123abc"
        val event = UrlValidationEvent(url, hash)

        // Simulate an invalid URL format
        whenever(urlValidatorService.validate(url))
            .thenReturn(Mono.just(Err(UrlError.InvalidFormat)))

        // Prepare a mock for updateValidation
        whenever(shortUrlRepositoryService.updateValidation(
            eq(hash),
            check { validation ->
                assertEquals(false, validation.reachable)
                assertEquals(true, validation.validated)
                assertEquals(true, validation.safe)
            }
        )).thenReturn(Mono.empty())

        // Send event to channel
        channel.send(event)
        channel.close()

        // Start processing
        val job = urlValidationConsumerService.startProcessing(channel)

        // Wait for processing to complete
        job.join()

        // Verify interactions
        verify(urlValidatorService).validate(url)
        verify(shortUrlRepositoryService).updateValidation(
            eq(hash),
            check { validation ->
                assertEquals(false, validation.reachable)
                assertEquals(true, validation.validated)
                assertEquals(true, validation.safe)
            }
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `startProcessing should update validation as unsafe when URL is marked unsafe`() = runTest {
        val url = "http://malicious.com"
        val hash = "456def"
        val event = UrlValidationEvent(url, hash)

        // Simulate an unsafe URL
        whenever(urlValidatorService.validate(url))
            .thenReturn(Mono.just(Err(UrlError.Unsafe)))

        // Prepare a mock for updateValidation
        whenever(shortUrlRepositoryService.updateValidation(
            eq(hash),
            check { validation ->
                assertEquals(true, validation.reachable)
                assertEquals(true, validation.validated)
                assertEquals(false, validation.safe)
            }
        )).thenReturn(Mono.empty())

        // Send event to channel
        channel.send(event)
        channel.close()

        // Start processing
        val job = urlValidationConsumerService.startProcessing(channel)

        // Wait for processing to complete
        job.join()

        // Verify interactions
        verify(urlValidatorService).validate(url)
        verify(shortUrlRepositoryService).updateValidation(
            eq(hash),
            check { validation ->
                assertEquals(true, validation.reachable)
                assertEquals(true, validation.validated)
                assertEquals(false, validation.safe)
            }
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `startProcessing should update validation as reachable and safe when URL is valid`() = runTest {
        val url = "http://example.com"
        val hash = "789ghi"
        val event = UrlValidationEvent(url, hash)

        // Simulate a valid URL
        whenever(urlValidatorService.validate(url))
            .thenReturn(Mono.just(Ok(Unit)))

        // Prepare a mock for updateValidation
        whenever(shortUrlRepositoryService.updateValidation(
            eq(hash),
            check { validation ->
                assertEquals(true, validation.reachable)
                assertEquals(true, validation.validated)
                assertEquals(true, validation.safe)
            }
        )).thenReturn(Mono.empty())

        // Send event to channel
        channel.send(event)
        channel.close()

        // Start processing
        val job = urlValidationConsumerService.startProcessing(channel)

        // Wait for processing to complete
        job.join()

        // Verify interactions
        verify(urlValidatorService).validate(url)
        verify(shortUrlRepositoryService).updateValidation(
            eq(hash),
            check { validation ->
                assertEquals(true, validation.reachable)
                assertEquals(true, validation.validated)
                assertEquals(true, validation.safe)
            }
        )
    }
}