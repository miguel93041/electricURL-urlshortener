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
    object TooManyRequests: HashError()
}