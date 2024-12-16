@file:Suppress("WildcardImport")

package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.ClickRepositoryService
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.`when`
import org.mockito.kotlin.*
import reactor.core.publisher.Mono
import kotlin.test.Test

class RedirectionLimitUseCaseTest {

    private lateinit var clickRepositoryService: ClickRepositoryService
    private lateinit var redirectionLimitUseCase: RedirectionLimitUseCaseImpl

    private val urlId = "testUrlId"
    private val redirectionLimit = 10
    private val timeFrameInSeconds = 60L

    @BeforeEach
    fun setUp() {
        clickRepositoryService = mock()
        redirectionLimitUseCase = RedirectionLimitUseCaseImpl(
            redirectionLimit,
            timeFrameInSeconds,
            clickRepositoryService
        )
    }

    @Test
    fun `should return false if redirection limit is not reached`() {
        `when`(
            clickRepositoryService.countClicksByHashAfter(eq(urlId), any())
        ).thenReturn(Mono.just((redirectionLimit - 1).toLong()))

        val result = redirectionLimitUseCase.isRedirectionLimit(urlId).block()

        assertFalse(result ?: true)
    }

    @Test
    fun `should return true if redirection limit is reached`() {
        `when`(
            clickRepositoryService.countClicksByHashAfter(eq(urlId), any())
        ).thenReturn(Mono.just(redirectionLimit.toLong()+1))

        val result = redirectionLimitUseCase.isRedirectionLimit(urlId).block()

        assertTrue(result ?: false)
    }

    @Test
    fun `should handle count exceeding redirection limit`() {
        `when`(
            clickRepositoryService.countClicksByHashAfter(eq(urlId), any())
        ).thenReturn(Mono.just((redirectionLimit + 1).toLong()))

        val result = redirectionLimitUseCase.isRedirectionLimit(urlId).block()

        assertTrue(result ?: false)
    }

    @Test
    fun `checkRedirectionLimit throws RuntimeException when an exception occurs inside the service`() {
        `when`(clickRepositoryService.countClicksByHashAfter(any(), any()))
            .thenThrow(RuntimeException("Database error"))

        assertThrows<RuntimeException> {
            redirectionLimitUseCase.isRedirectionLimit(urlId)
        }

        verify(clickRepositoryService, times(1)).countClicksByHashAfter(eq(urlId), any())
    }
}
