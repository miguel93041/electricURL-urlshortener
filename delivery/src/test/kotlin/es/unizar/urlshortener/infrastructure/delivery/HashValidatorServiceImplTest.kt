@file:Suppress("WildcardImport")

package es.unizar.urlshortener.infrastructure.delivery

import es.unizar.urlshortener.core.*
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.ContextConfiguration
import reactor.core.publisher.Mono

@WebFluxTest(HashValidatorServiceImpl::class)
@ContextConfiguration(
    classes = [
        HashValidatorServiceImpl::class
    ]
)
class HashValidatorServiceImplTest {
    @MockBean
    lateinit var shortUrlRepositoryService: ShortUrlRepositoryService

    lateinit var validator: HashValidatorServiceImpl

    @BeforeEach
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        validator = HashValidatorServiceImpl(shortUrlRepositoryService)
    }

    @Test
    fun `validate - returns InvalidFormat for invalid hash`() {
        val invalidHash = "123" // not 8 hex chars
        val result = validator.validate(invalidHash).block()
        assertTrue(result!!.isErr && result.error == HashError.InvalidFormat)
    }

    @Test
    fun `validate - returns NotFound if no shortUrl for hash`() {
        val hash = "abcdef12"
        `when`(shortUrlRepositoryService.findByKey(hash)).thenReturn(Mono.empty())

        val result = validator.validate(hash).block()
        assertTrue(result!!.isErr && result.error == HashError.NotFound)
    }

    @Test
    fun `validate - returns NotValidated if shortUrl not validated`() {
        val hash = "abcdef12"
        val shortUrl = ShortUrl(
            hash = hash,
            redirection = Redirection("http://example.com"),
            properties = ShortUrlProperties(validation = ShortUrlValidation(false, false, false))
        )
        `when`(shortUrlRepositoryService.findByKey(hash)).thenReturn(Mono.just(shortUrl))

        val result = validator.validate(hash).block()
        assertTrue(result!!.isErr && result.error == HashError.NotValidated)
    }

    @Test
    fun `validate - returns Unsafe if validated but not safe`() {
        val hash = "abcdef12"
        val shortUrl = ShortUrl(
            hash = hash,
            redirection = Redirection("http://example.com"),
            properties = ShortUrlProperties(validation = ShortUrlValidation(false, false, true))
        )
        `when`(shortUrlRepositoryService.findByKey(hash)).thenReturn(Mono.just(shortUrl))

        val result = validator.validate(hash).block()
        assertTrue(result!!.isErr && result.error == HashError.Unsafe)
    }

    @Test
    fun `validate - returns Unreachable if validated, safe but not reachable`() {
        val hash = "abcdef12"
        val shortUrl = ShortUrl(
            hash = hash,
            redirection = Redirection("http://example.com"),
            properties = ShortUrlProperties(validation = ShortUrlValidation(true, false, true))
        )
        `when`(shortUrlRepositoryService.findByKey(hash)).thenReturn(Mono.just(shortUrl))

        val result = validator.validate(hash).block()
        assertTrue(result!!.isErr && result.error == HashError.Unreachable)
    }

    @Test
    fun `validate - returns Ok if validated, safe and reachable`() {
        val hash = "abcdef12"
        val shortUrl = ShortUrl(
            hash = hash,
            redirection = Redirection("http://example.com"),
            properties = ShortUrlProperties(validation = ShortUrlValidation(true, true, true))
        )
        `when`(shortUrlRepositoryService.findByKey(hash)).thenReturn(Mono.just(shortUrl))

        val result = validator.validate(hash).block()
        assertTrue(result!!.isOk && result.value == Unit)
    }
}
