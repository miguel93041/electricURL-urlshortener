@file:Suppress("WildcardImport")
package es.unizar.urlshortener.core.usecases

import RedirectionLimitUseCaseImpl
import es.unizar.urlshortener.core.ClickRepositoryService
import es.unizar.urlshortener.core.InternalError
import es.unizar.urlshortener.core.TooManyRequestsException
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.`when`
import org.mockito.kotlin.*
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
    fun `checkRedirectionLimit does not throw when count is below limit`() {
        `when`(clickRepositoryService.countClicksByHashAfter(any(), any()))
            .thenReturn((redirectionLimit - 1).toLong())

        assertDoesNotThrow {
            redirectionLimitUseCase.checkRedirectionLimit(urlId)
        }

        verify(clickRepositoryService, times(1)).countClicksByHashAfter(eq(urlId), any())
    }

    @Test
    fun `checkRedirectionLimit throws when count equals limit`() {
        // Arrange
        `when`(clickRepositoryService.countClicksByHashAfter(any(), any()))
            .thenReturn(redirectionLimit.toLong())

        assertThrows<TooManyRequestsException> {
            redirectionLimitUseCase.checkRedirectionLimit(urlId)
        }

        verify(clickRepositoryService, times(1)).countClicksByHashAfter(eq(urlId), any())
    }

    @Test
    fun `checkRedirectionLimit throws when count exceeds limit`() {
        `when`(clickRepositoryService.countClicksByHashAfter(any(), any()))
            .thenReturn((redirectionLimit + 5).toLong())

        assertThrows<TooManyRequestsException> {
            redirectionLimitUseCase.checkRedirectionLimit(urlId)
        }

        verify(clickRepositoryService, times(1)).countClicksByHashAfter(eq(urlId), any())
    }

    @Test
    fun `checkRedirectionLimit throws InternalError when an exception occurs inside the service`() {
        `when`(clickRepositoryService.countClicksByHashAfter(any(), any()))
            .thenThrow(RuntimeException("Database error"))

        assertThrows<InternalError> {
            redirectionLimitUseCase.checkRedirectionLimit(urlId)
        }

        verify(clickRepositoryService, times(1)).countClicksByHashAfter(eq(urlId), any())
    }
}
