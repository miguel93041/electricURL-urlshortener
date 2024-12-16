package es.unizar.urlshortener.core

/**
 * Represents errors related to URL validation and operations.
 */
sealed class UrlError {
    object InvalidFormat : UrlError()
    object Unreachable : UrlError()
    object Unsafe : UrlError()
}

/**
 * Represents errors related to hash validation and operations.
 */
sealed class HashError {
    object InvalidFormat : HashError()
    object NotFound : HashError()
    object NotValidated : HashError()
    object Unsafe : HashError()
    object Unreachable : HashError()
}

/**
 * Represents errors related to CSV file processing.
 */
sealed class CsvError {
    object InvalidFormat : CsvError()
}

/**
 * Represents errors related to redirection operations.
 */
sealed class RedirectionError {
    object TooManyRequests : RedirectionError()
    object InvalidFormat : RedirectionError()
    object NotFound : RedirectionError()
    object NotValidated : RedirectionError()
    object Unsafe : RedirectionError()
    object Unreachable : RedirectionError()
}
