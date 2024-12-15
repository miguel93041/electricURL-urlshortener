@file:Suppress("WildcardImport")
package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.*
import reactor.core.publisher.Flux

class GetAnalyticsUseCaseImplTest {

    private lateinit var clickRepository: ClickRepositoryService
    private lateinit var getAnalyticsUseCase: GetAnalyticsUseCase

    @BeforeEach
    fun setUp() {
        clickRepository = mock()
        getAnalyticsUseCase = GetAnalyticsUseCaseImpl(clickRepository)
    }

    @Test
    fun `should handle non-existent id gracefully`() {
        val id = "nonExistentId"

        whenever(clickRepository.findAllByHash(id)).thenReturn(Flux.empty())

        val analyticsData = getAnalyticsUseCase.getAnalytics(id).block()

        assertNotNull(analyticsData)
        assertEquals(0, analyticsData?.totalClicks)
        assertNull(analyticsData?.byBrowser)
        assertNull(analyticsData?.byCountry)
        assertNull(analyticsData?.byPlatform)
    }

    @ParameterizedTest
    @MethodSource("parameterPermutations")
    fun `should return correct analytics data for all parameter permutations`(
        includeBrowser: Boolean,
        includeCountry: Boolean,
        includePlatform: Boolean
    ) {
        val id = "testId"
        val clicks = listOf(
            Click(hash = id, properties = ClickProperties(browser = "Chrome", country = "Spain", platform = "Windows", referrer = "Google")),
            Click(hash = id, properties = ClickProperties(browser = "Firefox", country = "France", platform = "Linux", referrer = null)),
            Click(hash = id, properties = ClickProperties(null, null, null, null, null))
        )

        whenever(clickRepository.findAllByHash(id)).thenReturn(Flux.fromIterable(clicks))

        val analyticsData = getAnalyticsUseCase.getAnalytics(
            id,
            includeBrowser,
            includeCountry,
            includePlatform
        ).block()

        kotlin.test.assertNotNull(analyticsData)
        assertEquals(3, analyticsData?.totalClicks)

        if (includeBrowser) {
            assertEquals(mapOf("Chrome" to 1, "Firefox" to 1, "Unknown" to 1), analyticsData?.byBrowser)
        } else {
            assertNull(analyticsData?.byBrowser)
        }

        if (includeCountry) {
            assertEquals(mapOf("Spain" to 1, "France" to 1, "Unknown" to 1), analyticsData?.byCountry)
        } else {
            assertNull(analyticsData?.byCountry)
        }

        if (includePlatform) {
            assertEquals(mapOf("Windows" to 1, "Linux" to 1, "Unknown" to 1), analyticsData?.byPlatform)
        } else {
            assertNull(analyticsData?.byPlatform)
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
                        permutations.add(arrayOf(browser, country, platform))
                    }
                }
            }
            return permutations
        }
    }
}
