@file:Suppress("MatchingDeclarationName")
package es.unizar.urlshortener.core

sealed class UrlError {
    object InvalidFormat : UrlError()
    object Unreachable : UrlError()
    object Unsafe : UrlError()
}

sealed class HashError {
    object InvalidFormat : HashError()
    object NotFound : HashError()
    object NotValidated: HashError()
    object Unsafe: HashError()
    object Unreachable: HashError()
}

sealed class CsvError {
    object InvalidFormat : CsvError()
}

sealed class RedirectionError {
    object TooManyRequests: RedirectionError()
    object InvalidFormat : RedirectionError()
    object NotFound : RedirectionError()
    object NotValidated: RedirectionError()
    object Unsafe: RedirectionError()
    object Unreachable: RedirectionError()
}
