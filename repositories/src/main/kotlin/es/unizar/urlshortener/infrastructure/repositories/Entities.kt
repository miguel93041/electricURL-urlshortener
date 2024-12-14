@file:Suppress("WildcardImport")

package es.unizar.urlshortener.infrastructure.repositories

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime

/**
 * The [ClickEntity] entity logs clicks.
 */
@Table("click")
data class ClickEntity(
    @Id
    @Transient
    val id: Long? = null,
    val hash: String,
    val created: OffsetDateTime,
    val ip: String?,
    val referrer: String?,
    val browser: String?,
    val platform: String?,
    val country: String?
)

/**
 * The [ShortUrlEntity] entity stores short URLs.
 */
@Table("shorturl")
data class ShortUrlEntity(
    @Id
    val hash: String,
    val target: String,
    val created: OffsetDateTime,
    val owner: String?,
    val mode: Int,
    val safe: Boolean,
    val ip: String?,
    val country: String?
)
