@file:Suppress("WildcardImport")

package es.unizar.urlshortener.infrastructure.repositories

import es.unizar.urlshortener.core.*

/**
 * Extension method to convert a [ClickEntity] into a domain [Click].
 */
fun ClickEntity.toDomain() = Click(
    id = id,
    hash = hash,
    created = created,
    properties = ClickProperties(
        geoLocation = GeoLocation(
            ip = ip,
            country = country
        ),
        browserPlatform = BrowserPlatform(
            browser = browser,
            platform = platform
        )
    )
)

/**
 * Extension method to convert a domain [Click] into a [ClickEntity].
 */
fun Click.toEntity() = ClickEntity(
    id = id,
    hash = hash,
    created = created,
    ip = properties.geoLocation.ip,
    browser = properties.browserPlatform.browser,
    platform = properties.browserPlatform.platform,
    country = properties.geoLocation.country
)

/**
 * Extension method to convert a [ShortUrlEntity] into a domain [ShortUrl].
 */
fun ShortUrlEntity.toDomain() = ShortUrl(
    hash = hash,
    redirection = Redirection(
        target = target,
        mode = mode
    ),
    created = created,
    properties = ShortUrlProperties(
        geoLocation = GeoLocation(
            ip = ip,
            country = country
        ),
        validation = ShortUrlValidation(
            reachable = reachable,
            safe = safe,
            validated = validated
        )
    )
)

/**
 * Extension method to convert a domain [ShortUrl] into a [ShortUrlEntity].
 */
fun ShortUrl.toEntity() = ShortUrlEntity(
    hash = hash,
    target = redirection.target,
    created = created,
    mode = redirection.mode,
    ip = properties.geoLocation.ip,
    country = properties.geoLocation.country,
    reachable = properties.validation.reachable,
    safe = properties.validation.safe,
    validated = properties.validation.validated
)
