@file:Suppress("WildcardImport")

package es.unizar.urlshortener.infrastructure.delivery

import es.unizar.urlshortener.core.*
import es.unizar.urlshortener.core.usecases.*
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.never
import org.mockito.Mockito
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import kotlin.test.Test

@WebMvcTest
@ContextConfiguration(
    classes = [
        UrlShortenerControllerImpl::class,
        RestResponseEntityExceptionHandler::class
    ]
)
class UrlShortenerControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var redirectUseCase: RedirectUseCase

    @MockBean
    private lateinit var logClickUseCase: LogClickUseCase

    @MockBean
    private lateinit var createShortUrlUseCase: CreateShortUrlUseCase

    @MockBean
    private lateinit var createQRUseCase: CreateQRUseCase

    @MockBean
    private lateinit var geoLocationService: GeoLocationService

    @Suppress("UnusedPrivateProperty")
    @MockBean
    private lateinit var processCsvUseCase: ProcessCsvUseCase

    @MockBean
    private lateinit var browserPlatformIdentificationUseCase: BrowserPlatformIdentificationUseCase

    @MockBean
    private lateinit var urlAccessibilityCheckUseCase: UrlAccessibilityCheckUseCase

    @MockBean
    private lateinit var urlSafetyService: UrlSafetyService

    /**
     * Tests that `redirectTo` returns a redirect when the key exists.
     */
    @Test
    fun `redirectTo returns a redirect when the key exists`() {
        // Mock the behavior of redirectUseCase to return a redirection URL
        given(urlAccessibilityCheckUseCase.isUrlReachable(Mockito.anyString())).willReturn(true)
        given(urlSafetyService.isSafe(Mockito.anyString())).willReturn(true)
        given(redirectUseCase.redirectTo("key")).willReturn(Redirection("http://example.com/"))
        given(geoLocationService.get(Mockito.anyString())).willReturn(GeoLocation("127.0.0.1", "Bogon"))
        given(browserPlatformIdentificationUseCase.parse(Mockito.anyString()))
            .willReturn(BrowserPlatform("Chrome", "Windows"))

        // Perform a GET request and verify the response status and redirection URL
        mockMvc.perform(get("/{id}", "key").header("User-Agent", "some-user-agent"))
            .andExpect(status().isTemporaryRedirect)
            .andExpect(redirectedUrl("http://example.com/"))

        // Verify that logClickUseCase logs the click with the correct IP address
        verify(logClickUseCase).logClick(
            "key",
            ClickProperties(ip = "127.0.0.1", country = "Bogon", browser = "Chrome", platform = "Windows"))
    }

    /**
     * Tests that `redirectTo` returns a not found status when the key does not exist.
     */
    @Test
    fun `redirectTo returns a not found when the key does not exist`() {
        // Mock the behavior of redirectUseCase to throw a RedirectionNotFound exception
        given(urlAccessibilityCheckUseCase.isUrlReachable(Mockito.anyString())).willReturn(true)
        given(urlSafetyService.isSafe(Mockito.anyString())).willReturn(true)
        given(redirectUseCase.redirectTo("key"))
            .willAnswer { throw RedirectionNotFound("key") }
        given(geoLocationService.get(Mockito.anyString())).willReturn(GeoLocation("127.0.0.1", "Bogon"))
        given(browserPlatformIdentificationUseCase.parse(Mockito.anyString()))
            .willReturn(BrowserPlatform("Chrome", "Windows"))

        // Perform a GET request and verify the response status and error message
        mockMvc.perform(get("/{id}", "key").header("User-Agent", "some-user-agent"))
            .andDo(print())
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.statusCode").value(404))

        // Verify that logClickUseCase does not log the click
        verify(logClickUseCase, never()).logClick("key",
            ClickProperties(ip = "127.0.0.1", country = "Bogon", browser = "Chrome", platform = "Windows"))
    }

    /**
     * Tests that `creates` returns a basic redirect if it can compute a hash.
     */
    @Test
    fun `creates returns a basic redirect if it can compute a hash`() {
        // Mock the behavior of createShortUrlUseCase to return a ShortUrl object
        given(urlAccessibilityCheckUseCase.isUrlReachable(Mockito.anyString())).willReturn(true)
        given(urlSafetyService.isSafe(Mockito.anyString())).willReturn(true)
        given(geoLocationService.get(Mockito.anyString())).willReturn(GeoLocation("127.0.0.1", "Bogon"))
        given(createQRUseCase.create(Mockito.anyString(), Mockito.anyInt())).willReturn(byteArrayOf())
        given(
            createShortUrlUseCase.create(
                url = "http://example.com/",
                data = ShortUrlProperties(ip = "127.0.0.1", country = "Bogon")
            )
        ).willReturn(ShortUrl("f684a3c4", Redirection("http://example.com/")))

        // Perform a POST request and verify the response status, redirection URL, and JSON response
        mockMvc.perform(
            post("/api/link")
                .param("url", "http://example.com/")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
        )
            .andDo(print())
            .andExpect(status().isCreated)
            .andExpect(redirectedUrl("http://localhost/f684a3c4"))
            .andExpect(jsonPath("$.url").value("http://localhost/f684a3c4"))
    }

    /**
     * Tests that `creates` returns a bad request status if it cannot compute a hash.
     */
    @Test
    fun `creates returns bad request if it can compute a hash`() {
        // Mock the behavior of createShortUrlUseCase to throw an InvalidUrlException
        given(urlAccessibilityCheckUseCase.isUrlReachable(Mockito.anyString())).willReturn(true)
        given(urlSafetyService.isSafe(Mockito.anyString())).willReturn(true)
        given(geoLocationService.get(Mockito.anyString())).willReturn(GeoLocation("127.0.0.1", "Bogon"))
        given(createQRUseCase.create(Mockito.anyString(), Mockito.anyInt())).willReturn(byteArrayOf())
        given(
            createShortUrlUseCase.create(
                url = "ftp://example.com/",
                data = ShortUrlProperties(ip = "127.0.0.1", country = "Bogon")
            )
        ).willAnswer { throw InvalidUrlException("ftp://example.com/") }

        // Perform a POST request and verify the response status and error message
        mockMvc.perform(
            post("/api/link")
                .param("url", "ftp://example.com/")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.statusCode").value(400))
    }
}
