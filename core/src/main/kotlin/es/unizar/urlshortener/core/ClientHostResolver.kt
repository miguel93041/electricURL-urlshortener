package es.unizar.urlshortener.core

import org.springframework.http.server.reactive.ServerHttpRequest

class ClientHostResolver private constructor() {
    companion object {

        /**
         * Resolves the client's IP address, even if the application is behind multiple proxies.
         */
        fun resolve(request: ServerHttpRequest): String? {
            val xRealIp = request.headers.getFirst("X-Real-IP") // Header commonly used by Nginx
            val xForwardedFor = request.headers.getFirst("X-Forwarded-For") // Header used by most load balancers
            val remoteAddr = request.remoteAddress?.address?.hostAddress // IP address directly from the server connection

            // Process X-Forwarded-For to extract the first IP, if present
            val clientIp = extractClientIp(xForwardedFor)

            // Return the first non-null value
            return listOfNotNull(xRealIp, clientIp, remoteAddr).firstOrNull()
        }

        /**
         * Extracts the first IP address from the X-Forwarded-For header, if present.
         * If the header contains multiple IPs, the first one is the original client IP.
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