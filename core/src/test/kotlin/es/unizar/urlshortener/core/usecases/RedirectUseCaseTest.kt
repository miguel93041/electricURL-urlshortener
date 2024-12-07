@file:Suppress("WildcardImport")
package es.unizar.urlshortener.core.usecases

import RedirectionLimitUseCase
import es.unizar.urlshortener.core.*
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.kotlin.*
import kotlin.test.Test
import kotlin.test.assertEquals

class RedirectUseCaseTest {

    @Test
    fun `redirectTo returns a redirect when the key exists and limit not exceeded`() {
        val repository = mock<ShortUrlRepositoryService>()
        val redirection = mock<Redirection>()
        val redlimit = mock<RedirectionLimitUseCase>()
        val shortUrl = ShortUrl("key", redirection)
        whenever(repository.findByKey("key")).thenReturn(shortUrl)
        doNothing().whenever(redlimit).isRedirectionLimit("key")
        val useCase = RedirectUseCaseImpl(repository, redlimit)

        val result = useCase.redirectTo("key")

        assertEquals(redirection, result)
        verify(redlimit, times(1)).isRedirectionLimit("key")
        verify(repository, times(1)).findByKey("key")
    }

    @Test
    fun `redirectTo throws TooManyRequestsException when redirection limit is exceeded`() {
        val repository = mock<ShortUrlRepositoryService>()
        val redlimit = mock<RedirectionLimitUseCase>()
        doThrow(TooManyRequestsException("key")).whenever(redlimit).isRedirectionLimit("key")
        val useCase = RedirectUseCaseImpl(repository, redlimit)

        assertThrows<TooManyRequestsException> {
            useCase.redirectTo("key")
        }

        verify(redlimit, times(1)).isRedirectionLimit("key")
        verify(repository, never()).findByKey(any())
    }

    @Test
    fun `redirectTo throws RedirectionNotFound when the key does not exist`() {
        val repository = mock<ShortUrlRepositoryService>()
        val redlimit = mock<RedirectionLimitUseCase>()
        doNothing().whenever(redlimit).isRedirectionLimit("key")
        whenever(repository.findByKey("key")).thenReturn(null)
        val useCase = RedirectUseCaseImpl(repository, redlimit)

        assertThrows<RedirectionNotFound> {
            useCase.redirectTo("key")
        }

        verify(redlimit, times(1)).isRedirectionLimit("key")
        verify(repository, times(1)).findByKey("key")
    }

    @Test
    fun `redirectTo throws InternalError when repository throws an exception`() {
        val repository = mock<ShortUrlRepositoryService>()
        val redlimit = mock<RedirectionLimitUseCase>()
        doNothing().whenever(redlimit).isRedirectionLimit("key")
        whenever(repository.findByKey("key")).thenThrow(RuntimeException())
        val useCase = RedirectUseCaseImpl(repository, redlimit)

        assertThrows<InternalError> {
            useCase.redirectTo("key")
        }

        verify(redlimit, times(1)).isRedirectionLimit("key")
        verify(repository, times(1)).findByKey("key")
    }
}
