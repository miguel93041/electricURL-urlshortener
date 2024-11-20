package es.unizar.urlshortener.core.usecases

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertFalse
import kotlin.test.Test
import kotlin.test.assertFailsWith

class RedirectionLimitUseCaseTest {

    private val repository = InMemoryRedirectionCountRepository()
    private val useCase = RedirectionLimitUseCaseImpl(3, 60, repository)

    @Test
    fun `should limit redirections when limit is reached`() {
        val urlId = "abc123"

        useCase.incrementRedirectionCount(urlId)
        useCase.incrementRedirectionCount(urlId)
        useCase.incrementRedirectionCount(urlId)

        assertTrue(useCase.isRedirectionLimit(urlId))
    }

    @Test
    fun `should not limit redirections when limit is not reached`() {

        val urlId = "abc123"

        useCase.incrementRedirectionCount(urlId)
        useCase.incrementRedirectionCount(urlId)

        assertFalse(useCase.isRedirectionLimit(urlId))
    }

    @Test
    fun `should handle multiple URLs independently`() {

        val url1 = "url1"
        val url2 = "url2"

        useCase.incrementRedirectionCount(url1)
        useCase.incrementRedirectionCount(url1)

        useCase.incrementRedirectionCount(url2)

        assertFalse(useCase.isRedirectionLimit(url1))  // Límite no alcanzado para url1
        assertFalse(useCase.isRedirectionLimit(url2))  // Límite no alcanzado para url2

        useCase.incrementRedirectionCount(url1)
        assertTrue(useCase.isRedirectionLimit(url1))  // Límite alcanzado para url1
    }

    @Test
    fun `should reset counts after time frame has passed`() {

        val urlId = "abc123"

        useCase.incrementRedirectionCount(urlId)
        useCase.incrementRedirectionCount(urlId)

        // Simular el paso del tiempo (este caso requiere una implementación más avanzada si necesitas forzar el tiempo)
        Thread.sleep(61000) // Espera para que pase el marco de tiempo

        assertFalse(useCase.isRedirectionLimit(urlId))  // Debería resetearse
    }

    @Test
    fun `should throw exception when limit is exceeded`() {

        val urlId = "abc123"

        useCase.incrementRedirectionCount(urlId)
        useCase.incrementRedirectionCount(urlId)
        useCase.incrementRedirectionCount(urlId)

        assertFailsWith<TooManyRequestsException> {
            useCase.incrementRedirectionCount(urlId)
        }
    }


}
