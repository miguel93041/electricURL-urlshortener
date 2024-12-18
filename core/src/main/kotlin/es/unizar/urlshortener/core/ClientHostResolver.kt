package es.unizar.urlshortener.core

import org.springframework.http.server.reactive.ServerHttpRequest

/**
 * Utility class for resolving the client's IP address, even when the application is behind multiple proxies.
 *
 * This class provides methods to extract the client's IP address from common HTTP headers such as
 * `X-Real-IP` or `X-Forwarded-For`, or directly from the server connection.
 */
class ClientHostResolver private constructor() {
    companion object {

        /**
         * Resolves the client's IP address, even if the application is behind multiple proxies.
         *
         * @param request The [ServerHttpRequest] containing the headers and remote address information.
         * @return The resolved client IP address, or `null` if none could be determined.
         */
        fun resolve(request: ServerHttpRequest): String? {
            val xRealIp = request.headers.getFirst("X-Real-IP") // Header commonly used by Nginx
            val xForwardedFor = request.headers.getFirst("X-Forwarded-For") // Header used by most load balancers
            val remoteAddr =
                request.remoteAddress?.address?.hostAddress // IP address directly from the server connection

            // Process X-Forwarded-For to extract the first IP, if present
            val clientIp = extractClientIp(xForwardedFor)

            // Return the first non-null value
            return listOfNotNull(xRealIp, clientIp, remoteAddr).firstOrNull()
        }

        /**
         * Extracts the first IP address from the X-Forwarded-For header, if present.
         * If the header contains multiple IPs, the first one is the original client IP.
         *
         * @param xForwardedFor The `X-Forwarded-For` header value.
         * @return The first IP address in the header, or null if the header is empty or not present.
         */
        private fun extractClientIp(xForwardedFor: String?): String? {
            if (xForwardedFor.isNullOrBlank()) {
                return null
            }
            // Split the header by commas and return the first non-blank value
            return xForwardedFor.split(",").firstOrNull()?.trim()
        }
    }
}
