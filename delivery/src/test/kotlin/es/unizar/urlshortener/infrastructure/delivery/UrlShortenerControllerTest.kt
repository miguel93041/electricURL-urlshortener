@file:Suppress("WildcardImport", "UnusedPrivateProperty")

package es.unizar.urlshortener.infrastructure.delivery

import es.unizar.urlshortener.core.*
import es.unizar.urlshortener.core.services.GenerateShortUrlService
import es.unizar.urlshortener.core.usecases.*
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.never
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.net.URI
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
    private lateinit var generateShortUrlServiceImpl: GenerateShortUrlService

    @MockBean
    private lateinit var createQRUseCase: CreateQRUseCase

    @MockBean
    private lateinit var geoLocationService: GeoLocationService

    @MockBean
    private lateinit var processCsvUseCase: ProcessCsvUseCase

    @MockBean
    private lateinit var browserPlatformIdentificationUseCase: BrowserPlatformIdentificationUseCase

    @MockBean
    private lateinit var getAnalyticsUseCase: GetAnalyticsUseCase

    @MockBean
    private lateinit var shortUrlRepositoryService: ShortUrlRepositoryService

    /**
     * Tests that `redirectTo` returns a redirect when the key exists.
     */
    @Test
    fun `redirectTo returns a redirect when the key exists`() {
        // Mock the behavior of redirectUseCase to return a redirection URL
        given(redirectUseCase.redirectTo("key")).willReturn(Redirection("http://example.com/"))
        given(geoLocationService.get(Mockito.anyString())).willReturn(GeoLocation("127.0.0.1", "Bogon"))

        // Perform a GET request and verify the response status and redirection URL
        mockMvc.perform(get("/{id}", "key"))
            .andExpect(status().isTemporaryRedirect)
            .andExpect(redirectedUrl("http://example.com/"))

        // Verify that logClickUseCase logs the click with the correct IP address
        verify(logClickUseCase).logClick(
            "key",
            ClickProperties(ip = "127.0.0.1", browser = null, platform = null, country = "Bogon")
        )
    }

    /**
     * Tests that `redirectTo` returns a not found status when the key does not exist.
     */
    @Test
    fun `redirectTo returns a not found when the key does not exist`() {
        // Mock the behavior of redirectUseCase to throw a RedirectionNotFound exception
        given(redirectUseCase.redirectTo("key"))
            .willAnswer { throw RedirectionNotFound("key") }
        given(geoLocationService.get(Mockito.anyString())).willReturn(GeoLocation("127.0.0.1", "Bogon"))

        // Perform a GET request and verify the response status and error message
        mockMvc.perform(get("/{id}", "key"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.statusCode").value(404))

        // Verify that logClickUseCase does not log the click
        verify(logClickUseCase, never()).logClick(eq("key"), any())
    }

    /**
     * Tests that `creates` returns a basic redirect if it can compute a hash.
     */
    @Test
    fun `creates returns a basic redirect if it can compute a hash`() {
        // Mock the behavior of generateEnhancedShortUrlUseCaseImpl to return a ShortUrlDataOut
        given(generateShortUrlServiceImpl.generate(any(), any())).willReturn(
            ShortUrlDataOut(
                shortUrl = URI.create("http://localhost/f684a3c4"),
                qrCodeUrl = null
            )
        )
        given(geoLocationService.get(Mockito.anyString())).willReturn(GeoLocation("127.0.0.1", "Bogon"))

        // Perform a POST request and verify the response status, redirection URL, and JSON response
        mockMvc.perform(
            post("/api/link")
                .param("url", "http://example.com/")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.shortUrl").value("http://localhost/f684a3c4"))
    }

    /**
     * Tests that `creates` returns a basic redirect with QR requested if it can compute a hash.
     */
    @Test
    fun `creates returns a basic redirect with QR requested if it can compute a hash`() {
        // Mock the behavior of generateEnhancedShortUrlUseCaseImpl to return a ShortUrlDataOut
        given(generateShortUrlServiceImpl.generate(any(), any())).willReturn(
            ShortUrlDataOut(
                shortUrl = URI.create("http://localhost/f684a3c4"),
                qrCodeUrl = URI.create("http://localhost/api/qr?id=f684a3c4")
            )
        )
        given(geoLocationService.get(Mockito.anyString())).willReturn(GeoLocation("127.0.0.1", "Bogon"))

        // Perform a POST request and verify the response status, redirection URL, and JSON response
        mockMvc.perform(
            post("/api/link")
                .param("url", "http://example.com/")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.shortUrl").value("http://localhost/f684a3c4"))
            .andExpect(jsonPath("$.qrCodeUrl").value("http://localhost/api/qr?id=f684a3c4"))
    }

    /**
     * Tests that `creates` returns a bad request status if it cannot compute a hash.
     */
    @Test
    fun `creates returns bad request if the URL is invalid`() {
        // Mock the behavior of generateEnhancedShortUrlUseCaseImpl to throw an InvalidUrlException
        given(generateShortUrlServiceImpl.generate(any(), any())).willAnswer {
            throw InvalidUrlException("ftp://example.com/")
        }
        given(geoLocationService.get(Mockito.anyString())).willReturn(GeoLocation("127.0.0.1", "Bogon"))

        // Perform a POST request and verify the response status and error message
        mockMvc.perform(
            post("/api/link")
                .param("url", "ftp://example.com/")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.statusCode").value(400))
            .andExpect(jsonPath("$.message").value("[ftp://example.com/] does not follow a supported schema"))
    }

    /**
     * Tests that `creates` returns a bad request status if it cannot compute a hash with QR request.
     */
    @Test
    fun `creates returns bad request if the URL is invalid with QR request`() {
        // Mock the behavior of generateEnhancedShortUrlUseCaseImpl to throw an InvalidUrlException
        given(generateShortUrlServiceImpl.generate(any(), any())).willAnswer {
            throw InvalidUrlException("ftp://example.com/")
        }
        given(geoLocationService.get(Mockito.anyString())).willReturn(GeoLocation("127.0.0.1", "Bogon"))

        // Perform a POST request and verify the response status and error message
        mockMvc.perform(
            post("/api/link")
                .param("url", "ftp://example.com/")
                .param("qrRequested", true.toString())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.statusCode").value(400))
            .andExpect(jsonPath("$.message").value("[ftp://example.com/] does not follow a supported schema"))
    }
}
