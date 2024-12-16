@file:Suppress("WildcardImport")

package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.*
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import reactor.core.publisher.Mono

class CreateShortUrlUseCaseTest {

    private lateinit var createShortUrlUseCase: CreateShortUrlUseCase
    private lateinit var shortUrlRepository: ShortUrlRepositoryService
    private lateinit var hashService: HashService
    private lateinit var shortUrlProperties: ShortUrlProperties

    @BeforeEach
    fun setUp() {
        shortUrlRepository = mock()
        hashService = mock()
        createShortUrlUseCase = CreateShortUrlUseCaseImpl(shortUrlRepository, hashService)
        shortUrlProperties = mock()
    }

    @Test
    fun `creates returns a basic redirect if it can compute a hash`() {
        val url = "https://example.com"
        val generatedHash = "f684a3c4"

        whenever(hashService.generateRandomHash()).thenReturn(generatedHash)
        val shortUrl = ShortUrl(
            hash = generatedHash,
            redirection = Redirection(target = url),
            properties = shortUrlProperties
        )
        whenever(shortUrlRepository.create(any<ShortUrl>())).thenReturn(Mono.just(shortUrl))

        val result = createShortUrlUseCase.create(url).block()

        assertNotNull(result)
        assertEquals(generatedHash, result.hash)
        assertEquals(url, result.redirection.target)
        assertEquals(shortUrlProperties, result.properties)
    }

    @Test
    fun `creates returns invalid URL exception if the short URL cannot be saved`() {
        val url = "https://example.com"
        val generatedHash = "f684a3c4"

        whenever(hashService.generateRandomHash()).thenReturn(generatedHash)
        val shortUrl = ShortUrl(
            hash = generatedHash,
            redirection = Redirection(target = url),
            properties = shortUrlProperties
        )
        whenever(shortUrlRepository.create(shortUrl)).thenReturn(Mono.error(RuntimeException("Database error")))

        assertThrows(RuntimeException::class.java) {
            createShortUrlUseCase.create(url).block()
        }
    }
}
