@file:Suppress("WildcardImport")
package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*

class GetAnalyticsUseCaseImplTest {

    private lateinit var clickRepository: ClickRepositoryService
    private lateinit var shortUrlRepository: ShortUrlRepositoryService
    private lateinit var getAnalyticsUseCase: GetAnalyticsUseCase

    @BeforeEach
    fun setUp() {
        clickRepository = mock()
        shortUrlRepository = mock()
        getAnalyticsUseCase = GetAnalyticsUseCaseImpl(clickRepository, shortUrlRepository)
    }

    @Test
    fun `should throw RedirectionNotFound when ShortUrl does not exist`() {
        // Configuración
        val id = "nonexistent"
        whenever(shortUrlRepository.findByKey(id)).thenReturn(null)

        // Ejecución y verificación
        assertThrows<RedirectionNotFound> {
            getAnalyticsUseCase.getAnalytics(id)
        }
    }

    @ParameterizedTest
    @MethodSource("parameterPermutations")
    fun `should return correct analytics data for all parameter permutations`(
        includeBrowser: Boolean,
        includeCountry: Boolean,
        includePlatform: Boolean,
        includeReferrer: Boolean
    ) {
        // Configuración
        val id = "abc123"
        val shortUrl = mock<ShortUrl>()
        whenever(shortUrlRepository.findByKey(id)).thenReturn(shortUrl)

        val click1 = Click(
            hash = id,
            properties = ClickProperties(
                browser = "Chrome",
                country = "Spain",
                platform = "Windows",
                referrer = "Google"
            )
        )
        val click2 = Click(
            hash = id,
            properties = ClickProperties(
                browser = "Firefox",
                country = "France",
                platform = "Linux",
                referrer = "Bing"
            )
        )
        val clicks = listOf(click1, click2)
        whenever(clickRepository.findAllByHash(id)).thenReturn(clicks)

        // Ejecución
        val analyticsData = getAnalyticsUseCase.getAnalytics(
            id,
            includeBrowser = includeBrowser,
            includeCountry = includeCountry,
            includePlatform = includePlatform,
            includeReferrer = includeReferrer
        )

        // Verificación
        assertEquals(2, analyticsData.totalClicks)

        if (includeBrowser) {
            assertEquals(mapOf("Chrome" to 1, "Firefox" to 1), analyticsData.byBrowser)
        } else {
            assertNull(analyticsData.byBrowser)
        }

        if (includeCountry) {
            assertEquals(mapOf("Spain" to 1, "France" to 1), analyticsData.byCountry)
        } else {
            assertNull(analyticsData.byCountry)
        }

        if (includePlatform) {
            assertEquals(mapOf("Windows" to 1, "Linux" to 1), analyticsData.byPlatform)
        } else {
            assertNull(analyticsData.byPlatform)
        }

        if (includeReferrer) {
            assertEquals(mapOf("Google" to 1, "Bing" to 1), analyticsData.byReferrer)
        } else {
            assertNull(analyticsData.byReferrer)
        }
    }

    companion object {
        @Suppress("NestedBlockDepth")
        @JvmStatic
        fun parameterPermutations(): List<Array<Any>> {
            val booleans = listOf(true, false)
            val permutations = mutableListOf<Array<Any>>()
            for (browser in booleans) {
                for (country in booleans) {
                    for (platform in booleans) {
                        for (referrer in booleans) {
                            permutations.add(arrayOf(browser, country, platform, referrer))
                        }
                    }
                }
            }
            return permutations
        }
    }
}
