package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.ShortUrlProperties
import es.unizar.urlshortener.core.ShortUrlRepositoryService
import es.unizar.urlshortener.core.UrlValidatorService
import es.unizar.urlshortener.core.HashService
import es.unizar.urlshortener.core.ValidatorResult
import es.unizar.urlshortener.core.InvalidUrlException
import es.unizar.urlshortener.core.InternalError
import es.unizar.urlshortener.core.ShortUrl
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.whenever
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CreateShortUrlUseCaseTest {

    @Test
    fun `creates returns a basic redirect if it can compute a hash`() {
        val shortUrlRepository = mock<ShortUrlRepositoryService>()
        val urlValidatorService = mock<UrlValidatorService>()
        val hashService = mock<HashService>()
        val shortUrlProperties = ShortUrlProperties() // No debe ser un mock si se espera usar valores reales.

        // Configuración correcta de los mocks
        whenever(urlValidatorService.validate("http://example.com/")).thenReturn(ValidatorResult.VALID)
        whenever(hashService.hashUrl("http://example.com/")).thenReturn("f684a3c4")
        whenever(shortUrlRepository.create(any())).doAnswer { it.arguments[0] as ShortUrl }

        val createShortUrlUseCase = CreateShortUrlUseCaseImpl(shortUrlRepository, urlValidatorService, hashService)
        val shortUrl = createShortUrlUseCase.create("http://example.com/", shortUrlProperties)

        // Verificación
        assertEquals("f684a3c4", shortUrl.hash)
    }

    @Test
    fun `creates returns invalid URL exception if the URL is not valid`() {
        val shortUrlRepository = mock<ShortUrlRepositoryService>()
        val urlValidatorService = mock<UrlValidatorService>()
        val hashService = mock<HashService>()
        val shortUrlProperties = mock<ShortUrlProperties>()

        whenever(urlValidatorService.validate("ftp://example.com/")).thenReturn(ValidatorResult.NOT_VALID_FORMAT)

        val createShortUrlUseCase = CreateShortUrlUseCaseImpl(shortUrlRepository, urlValidatorService, hashService)

        assertFailsWith<InvalidUrlException> {
            createShortUrlUseCase.create("ftp://example.com/", shortUrlProperties)
        }
    }

    @Test
    fun `creates returns invalid URL exception if the URI cannot be validated`() {
        val shortUrlRepository = mock<ShortUrlRepositoryService>()
        val urlValidatorService = mock<UrlValidatorService>()
        val hashService = mock<HashService>()
        val shortUrlProperties = mock<ShortUrlProperties>()

        whenever(urlValidatorService.validate("http://example.com/")).thenReturn(ValidatorResult.VALID)

        val createShortUrlUseCase = CreateShortUrlUseCaseImpl(shortUrlRepository, urlValidatorService, hashService)

        assertFailsWith<InternalError> {
            createShortUrlUseCase.create("http://example.com/", shortUrlProperties)
        }
    }

    @Test
    fun `creates returns invalid URL exception if the hash cannot be computed`() {
        val shortUrlRepository = mock<ShortUrlRepositoryService>()
        val urlValidatorService = mock<UrlValidatorService>()
        val hashService = mock<HashService>()
        val shortUrlProperties = mock<ShortUrlProperties>()

        whenever(urlValidatorService.validate("http://example.com/")).thenReturn(ValidatorResult.VALID)
        whenever(hashService.hashUrl("http://example.com/")).thenThrow(RuntimeException())

        val createShortUrlUseCase = CreateShortUrlUseCaseImpl(shortUrlRepository, urlValidatorService, hashService)

        assertFailsWith<InternalError> {
            createShortUrlUseCase.create("http://example.com/", shortUrlProperties)
        }
    }

    @Test
    fun `creates returns invalid URL exception if the short URL cannot be saved`() {
        val shortUrlRepository = mock<ShortUrlRepositoryService>()
        val urlValidatorService = mock<UrlValidatorService>()
        val hashService = mock<HashService>()
        val shortUrlProperties = mock<ShortUrlProperties>()

        whenever(urlValidatorService.validate("http://example.com/")).thenReturn(ValidatorResult.VALID)
        whenever(hashService.hashUrl("http://example.com/")).thenReturn("f684a3c4")
        whenever(shortUrlRepository.create(any())).thenThrow(RuntimeException())

        val createShortUrlUseCase = CreateShortUrlUseCaseImpl(shortUrlRepository, urlValidatorService, hashService)

        assertFailsWith<InternalError> {
            createShortUrlUseCase.create("http://example.com/", shortUrlProperties)
        }
    }
}
