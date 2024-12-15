package es.unizar.urlshortener.infrastructure.delivery

import es.unizar.urlshortener.core.UrlError
import es.unizar.urlshortener.core.UrlSafetyService
import es.unizar.urlshortener.core.usecases.UrlAccessibilityCheckUseCase
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.ContextConfiguration
import reactor.core.publisher.Mono

@WebFluxTest(UrlValidatorServiceImpl::class)
@ContextConfiguration(
    classes = [
        UrlValidatorServiceImpl::class
    ]
)
class UrlValidatorServiceImplTest {

    @MockBean
    private lateinit var urlAccessibilityCheckUseCase: UrlAccessibilityCheckUseCase

    @MockBean
    private lateinit var urlSafetyService: UrlSafetyService

    private lateinit var validator: UrlValidatorServiceImpl

    @BeforeEach
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        validator = UrlValidatorServiceImpl(urlAccessibilityCheckUseCase, urlSafetyService)
    }

    @Test
    fun `validate - returns InvalidFormat for invalid url`() {
        val invalidUrl = "not-a-valid-url"
        val result = validator.validate(invalidUrl).block()
        assertTrue(result!!.isErr && result.error == UrlError.InvalidFormat)
    }

    @Test
    fun `validate - returns Unsafe if url is not safe`() {
        val url = "http://example.com"
        `when`(urlSafetyService.isSafe(url)).thenReturn(Mono.just(false))

        val result = validator.validate(url).block()
        assertTrue(result!!.isErr && result.error == UrlError.Unsafe)
    }

    @Test
    fun `validate - returns Unreachable if url is safe but not reachable`() {
        val url = "http://example.com"
        `when`(urlSafetyService.isSafe(url)).thenReturn(Mono.just(true))
        `when`(urlAccessibilityCheckUseCase.isUrlReachable(url)).thenReturn(Mono.just(false))

        val result = validator.validate(url).block()
        assertTrue(result!!.isErr && result.error == UrlError.Unreachable)
    }

    @Test
    fun `validate - returns Ok if url is safe and reachable`() {
        val url = "http://example.com"
        `when`(urlSafetyService.isSafe(url)).thenReturn(Mono.just(true))
        `when`(urlAccessibilityCheckUseCase.isUrlReachable(url)).thenReturn(Mono.just(true))

        val result = validator.validate(url).block()
        assertTrue(result!!.isOk && result.value == Unit)
    }
}
