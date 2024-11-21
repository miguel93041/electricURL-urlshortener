package es.unizar.urlshortener.thirdparties.ipinfo


import es.unizar.urlshortener.core.UrlSafetyService
import io.github.cdimascio.dotenv.Dotenv
import org.springframework.web.reactive.function.client.WebClient

class UrlSafetyServiceImpl (
    private val webClient: WebClient,
    dotenv: Dotenv
) : UrlSafetyService {

    private val accessToken = System.getenv(DOTENV_SAFEBROWSING_KEY) ?: dotenv[DOTENV_SAFEBROWSING_KEY]

    @Suppress("TooGenericExceptionThrown")
    override fun isSafe(url: String): Boolean {
        val requestUrl = buildRequestUrl()

        val response = webClient.post()
            .uri(requestUrl)
            .bodyValue(createRequestBody(url))
            .retrieve()
            .onStatus({ status -> status.isError }) { response ->
                response.bodyToMono(String::class.java).flatMap { errorResponse ->
                    throw RuntimeException("Error from API: $errorResponse")
                }
            }
            .bodyToMono(Map::class.java)
            .block()

        // Returns true if response is empty ({}) which means URL is safe or does not exist
        return response.isNullOrEmpty()
    }

    private fun createRequestBody(url: String): Map<String, Any> {
        return mapOf(
            "client" to mapOf(
                "clientId" to "shortener",
                "clientVersion" to "1.0"
            ),
            "threatInfo" to mapOf(
                "threatTypes" to listOf("MALWARE", "SOCIAL_ENGINEERING"),
                "platformTypes" to listOf("ANY_PLATFORM"),
                "threatEntryTypes" to listOf("URL"),
                "threatEntries" to listOf(mapOf("url" to url))
            )
        )
    }

    private fun buildRequestUrl(): String {
        return "${SAFEBROWSING_BASE_URL}v4/threatMatches:find?key=$accessToken"
    }

    companion object {
        const val DOTENV_SAFEBROWSING_KEY = "GOOGLE_API_KEY"
        const val SAFEBROWSING_BASE_URL = "https://safebrowsing.googleapis.com/"
    }
}
