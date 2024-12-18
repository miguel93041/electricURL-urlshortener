package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.BrowserPlatform
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
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

    @ParameterizedTest
    @MethodSource("provideUserAgentData")
    fun `should identify commons browser and platform correctly`(
        userAgentString: String,
        expectedUserAgent: UserAgent,
        expectedOs: OS,
        expectedResult: BrowserPlatform
    ) {
        val client = Client(expectedUserAgent, expectedOs, null)

        whenever(parser.parse(userAgentString)).thenReturn(client)

        val result = browserPlatformIdentificationUseCase.parse(userAgentString)

        assertEquals(expectedResult, result)
    }

    companion object {
        @JvmStatic
        fun provideUserAgentData() = listOf(
            Arguments.of(
                // Safari on macOS
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko)" +
                    "Version/14.0 Safari/605.1.15",
                UserAgent("Safari", "14", "0", null),
                OS("macOS", "10.15.7", "", "", ""),
                BrowserPlatform("Safari", "macOS")
            ),
            Arguments.of(
                // Firefox on Linux
                "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:91.0) Gecko/20100101 Firefox/91.0",
                UserAgent("Firefox", "91", "0", null),
                OS("Linux", null, "", "", ""),
                BrowserPlatform("Firefox", "Linux")
            ),
            Arguments.of(
                // Edge on Windows
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) " +
                    "Chrome/91.0.4472.124 Safari/537.36 Edg/91.0.864.59",
                UserAgent("Edge", "91", "0", "864"),
                OS("Windows", "10", "", "", ""),
                BrowserPlatform("Edge", "Windows")
            ),
            Arguments.of(
                // Chrome on Android
                "Mozilla/5.0 (Linux; Android 11; Pixel 4 XL) AppleWebKit/537.36 (KHTML, like Gecko) " +
                    "Chrome/91.0.4472.124 Mobile Safari/537.36",
                UserAgent("Chrome", "91", "0", "4472"),
                OS("Android", "11", "", "", ""),
                BrowserPlatform("Chrome", "Android")
            ),
            Arguments.of(
                // Safari on iOS
                "Mozilla/5.0 (iPhone; CPU iPhone OS 14_6 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) " +
                    "Version/14.1 Mobile/15E148 Safari/604.1",
                UserAgent("Safari", "14", "1", null),
                OS("iOS", "14.6", "", "", ""),
                BrowserPlatform("Safari", "iOS")
            )
        )
    }

    @Test
    fun `should handle null UserAgent but non-null OS`() {
        val userAgentString = "SomeRandomUserAgentString"

        val os = OS("Linux", null, "", "", "")
        val client = Client(null, os, null)

        whenever(parser.parse(userAgentString)).thenReturn(client)

        val result = browserPlatformIdentificationUseCase.parse(userAgentString)

        assertEquals(BrowserPlatform("Unknown", "Linux"), result)
    }

    @Test
    fun `should handle null OS but non-null UserAgent`() {
        val userAgentString = "SomeRandomUserAgentString"

        val userAgent = UserAgent("Chrome", "91", "0", "4472")
        val client = Client(userAgent, null, null)

        whenever(parser.parse(userAgentString)).thenReturn(client)

        val result = browserPlatformIdentificationUseCase.parse(userAgentString)

        assertEquals(BrowserPlatform("Chrome", "Unknown"), result)
    }
}
