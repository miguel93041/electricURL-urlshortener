@file:Suppress("WildcardImport")
package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.*
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.kotlin.*
import reactor.core.publisher.Mono
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class RedirectUseCaseTest {

    private val shortUrlService: ShortUrlRepositoryService = mock(ShortUrlRepositoryService::class.java)
    private val redirectUseCase = RedirectUseCaseImpl(shortUrlService = shortUrlService)
    private val redirection: Redirection = mock(Redirection::class.java)

    @Test
    fun `redirectTo returns a redirect when the key exists`() {
        val key = "key"
        val shortUrl = ShortUrl(key, redirection)
        whenever(shortUrlService.findByKey(key)).thenReturn(Mono.just(shortUrl))

        val result = redirectUseCase.redirectTo(key).block()

        assertNotNull(result)
        assertEquals(redirection, result)
    }

    @Test
    fun `redirectTo throws RedirectionNotFound when the key does not exist`() {
        val key = "invalidKey"
        whenever(shortUrlService.findByKey(key)).thenReturn(Mono.empty())

        assertThrows<RedirectionNotFound> {
            redirectUseCase.redirectTo(key).block()
        }
    }

    @Test
    fun `should handle service error gracefully`() {
        val key = "errorKey"

        whenever(shortUrlService.findByKey(key)).thenReturn(Mono.error(RuntimeException("Service error")))

        assertThrows<RuntimeException> {
            redirectUseCase.redirectTo(key).block()
        }
    }
}
