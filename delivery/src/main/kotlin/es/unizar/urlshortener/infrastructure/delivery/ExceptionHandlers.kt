@file:Suppress("MatchingDeclarationName", "MagicNumber", "UnusedPrivateProperty")

package es.unizar.urlshortener.infrastructure.delivery

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

/**
 * [GlobalErrorHandler] is an implementation of the [ErrorWebExceptionHandler] interface.
 * It is designed to handle exceptions globally and provide a consistent error response.
 *
 * This handler captures any exceptions thrown during the processing of requests and returns
 * a standardized error response with appropriate HTTP status and message.
 *
 * @param objectMapper Used to serialize the error message to a JSON response.
 */
@Configuration
@Order(-2)
class GlobalErrorHandler(private val objectMapper: ObjectMapper) : ErrorWebExceptionHandler {

    /**
     * Handles exceptions that occur during the processing of a request.
     *
     * @param serverWebExchange The server exchange that encapsulates the request and response details.
     * @param throwable The exception thrown during request processing.
     * @return A Mono<Void> representing the completion of the error handling process.
     */
    override fun handle(serverWebExchange: ServerWebExchange, throwable: Throwable): Mono<Void> {
        log.error("An error occurred while processing the request", throwable)

        val response = serverWebExchange.response
        response.statusCode = HttpStatus.INTERNAL_SERVER_ERROR
        response.headers.contentType = MediaType.TEXT_PLAIN

        val dataBufferFactory = response.bufferFactory()
        val dataBuffer = dataBufferFactory.wrap(throwable.message?.toByteArray() ?:
            "An unexpected error has occurred".toByteArray())

        return response.writeWith(Mono.just(dataBuffer))
    }

    companion object {
        private val log = LoggerFactory.getLogger(GlobalErrorHandler::class.java)
    }
}
