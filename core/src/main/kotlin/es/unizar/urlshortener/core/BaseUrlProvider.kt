@file:Suppress("MagicNumber")

package es.unizar.urlshortener.core

import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.web.util.UriComponentsBuilder

/**
 * Interface for providing the base URL used for generating shortened URLs.
 */
fun interface BaseUrlProvider {

    /**
     * Returns the base URL for the current request context.
     *
     * @param request The current server HTTP request.
     * @return The base URL as a [String].
     */
    fun get(request: ServerHttpRequest): String
}

/**
 * Default implementation of the [BaseUrlProvider] interface.
 * Provides the base URL for the current request using UriComponentsBuilder.
 */
class BaseUrlProviderImpl : BaseUrlProvider {

    /**
     * Retrieves the base URL for the current request context.
     *
     * @param request The current server HTTP request.
     * @return The base URL as a [String].
     */
    override fun get(request: ServerHttpRequest): String {
        val uri = request.uri
        val scheme = request.headers.getFirst("X-Forwarded-Proto") ?: uri.scheme
        val host = request.headers.getFirst("X-Forwarded-Host") ?: uri.host
        val port = request.headers.getFirst("X-Forwarded-Port")?.toIntOrNull() ?: uri.port

        // Ensure port is valid and pass -1 to clear default ports
        return UriComponentsBuilder.newInstance()
            .scheme(scheme)
            .host(host)
            .port(if (port == -1 || port == 80 || port == 443) -1 else port) // Exclude default ports
            .build()
            .toUriString()
    }
}
