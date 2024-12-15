package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.BrowserPlatform
import org.junit.jupiter.api.Assertions.assertThrows
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever
import ua_parser.Client
import ua_parser.OS
import ua_parser.Parser
import ua_parser.UserAgent
import kotlin.test.Test
import kotlin.test.assertEquals

class BrowserPlatformIdentificationUseCaseTest {
    private val parser: Parser = mock()
    private val browserPlatformIdentificationUseCase = BrowserPlatformIdentificationUseCaseImpl(parser)

    @Test
    fun `should return correct browser and platform for valid user agent`() {
        val userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/91.0.4472.124 Safari/537.36"

        val userAgent = UserAgent("Chrome", "91", "0", "4472")
        val os = OS("Windows", "10", "", "", "")
        val client = Client(userAgent, os, null)

        whenever(parser.parse(userAgentString)).thenReturn(client)

        val result = browserPlatformIdentificationUseCase.parse(userAgentString)

        assertEquals(BrowserPlatform("Chrome", "Windows"), result)
    }

    @Test
    fun `should return Unknown for empty user agent`() {
        val result = browserPlatformIdentificationUseCase.parse(null)

        assertEquals(BrowserPlatform("Unknown", "Unknown"), result)
    }

    @Test
    fun `should return Unknown for blank user agent`() {
        val result = browserPlatformIdentificationUseCase.parse("")

        assertEquals(BrowserPlatform("Unknown", "Unknown"), result)
    }

    @Test
    fun `should handle unknown browser and platform when parser returns nulls`() {
        val userAgent = "SomeRandomUserAgentString"
        whenever(parser.parse(userAgent)).thenReturn(Client(null, null, null))

        val result = browserPlatformIdentificationUseCase.parse(userAgent)

        assertEquals("Unknown", result.browser)
        assertEquals("Unknown", result.platform)
    }

    @Test
    fun `should identify Safari on macOS`() {
        val userAgentString =
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko)" +
                    "Version/14.0 Safari/605.1.15"

        val userAgent = UserAgent("Safari", "14", "0", null)
        val os = OS("macOS", "10.15.7", "", "", "")
        val client = Client(userAgent, os, null)

        whenever(parser.parse(userAgentString)).thenReturn(client)

        val result = browserPlatformIdentificationUseCase.parse(userAgentString)

        assertEquals(BrowserPlatform("Safari", "macOS"), result)
    }

    @Test
    fun `should identify Firefox on Linux`() {
        val userAgentString = "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:91.0) Gecko/20100101 Firefox/91.0"

        val userAgent = UserAgent("Firefox", "91", "0", null)
        val os = OS("Linux", null, "", "", "")
        val client = Client(userAgent, os, null)

        whenever(parser.parse(userAgentString)).thenReturn(client)

        val result = browserPlatformIdentificationUseCase.parse(userAgentString)

        assertEquals(BrowserPlatform("Firefox", "Linux"), result)
    }

    @Test
    fun `should identify Edge on Windows`() {
        val userAgentString =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) " +
                    "Chrome/91.0.4472.124 Safari/537.36 Edg/91.0.864.59"

        val userAgent = UserAgent("Edge", "91", "0", "864")
        val os = OS("Windows", "10", "", "", "")
        val client = Client(userAgent, os, null)

        whenever(parser.parse(userAgentString)).thenReturn(client)

        val result = browserPlatformIdentificationUseCase.parse(userAgentString)

        assertEquals(BrowserPlatform("Edge", "Windows"), result)
    }

    @Test
    fun `should identify Chrome on Android`() {
        val userAgentString =
            "Mozilla/5.0 (Linux; Android 11; Pixel 4 XL) AppleWebKit/537.36 (KHTML, like Gecko) " +
                    "Chrome/91.0.4472.124 Mobile Safari/537.36"

        val userAgent = UserAgent("Chrome", "91", "0", "4472")
        val os = OS("Android", "11", "", "", "")
        val client = Client(userAgent, os, null)

        whenever(parser.parse(userAgentString)).thenReturn(client)

        val result = browserPlatformIdentificationUseCase.parse(userAgentString)

        assertEquals(BrowserPlatform("Chrome", "Android"), result)
    }

    @Test
    fun `should identify Safari on iOS`() {
        val userAgentString =
            "Mozilla/5.0 (iPhone; CPU iPhone OS 14_6 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) " +
                    "Version/14.1 Mobile/15E148 Safari/604.1"

        val userAgent = UserAgent("Safari", "14", "1", null)
        val os = OS("iOS", "14.6", "", "", "")
        val client = Client(userAgent, os, null)

        whenever(parser.parse(userAgentString)).thenReturn(client)

        val result = browserPlatformIdentificationUseCase.parse(userAgentString)

        assertEquals(BrowserPlatform("Safari", "iOS"), result)
    }
}
