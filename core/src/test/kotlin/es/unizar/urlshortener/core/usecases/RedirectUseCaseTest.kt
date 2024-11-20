package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.InternalError
import es.unizar.urlshortener.core.Redirection
import es.unizar.urlshortener.core.RedirectionNotFound
import es.unizar.urlshortener.core.ShortUrl
import es.unizar.urlshortener.core.ShortUrlRepositoryService
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RedirectUseCaseTest {

    @Test
    fun `redirectTo returns a redirect when the key exists`() {
        val repository = mock<ShortUrlRepositoryService> ()
        val redirection = mock<Redirection>()
        val redlimit = mock<RedirectionLimitUseCase>()
        val shortUrl = ShortUrl("key", redirection)
        whenever(repository.findByKey("key")).thenReturn(shortUrl)
        val useCase = RedirectUseCaseImpl(repository, redlimit)

        assertEquals(redirection, useCase.redirectTo("key"))
    }

    @Test
    fun `redirectTo returns a not found when the key does not exist`() {
        val repository = mock<ShortUrlRepositoryService> ()
        val redlimit = mock<RedirectionLimitUseCase>()
        whenever(repository.findByKey("key")).thenReturn(null)
        val useCase = RedirectUseCaseImpl(repository, redlimit)

        assertFailsWith<RedirectionNotFound> {
            useCase.redirectTo("key")
        }
    }

    @Test
    fun `redirectTo returns a not found when find by key fails`() {
        val repository = mock<ShortUrlRepositoryService> ()
        val redlimit = mock<RedirectionLimitUseCase>()
        whenever(repository.findByKey("key")).thenThrow(RuntimeException())
        val useCase = RedirectUseCaseImpl(repository, redlimit)

        assertFailsWith<InternalError> {
            useCase.redirectTo("key")
        }
    }
}

