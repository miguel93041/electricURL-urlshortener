package es.unizar.urlshortener.thirdparties.ipinfo

/**
 * [IpAddress] is a utility class that represents an IP address and provides methods
 * to validate its format and classify it as IPv4, IPv6, or bogon.
 *
 * @property ip The string representation of the IP address.
 * @property isValid Indicates whether the IP address is valid as either IPv4 or IPv6.
 * @property isIPv4 Checks if the IP address matches the IPv4 format.
 * @property isIPv6 Checks if the IP address matches the IPv6 format.
 * @property isBogon Identifies if the IP address is a bogon (special-use address).
 */
class IpAddress(
    val ip: String
) {
    private val ipv4Regex = (
            """^(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\."""
                    + """(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\."""
                    + """(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\."""
                    + """(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"""
            ).toRegex()
    private val ipv6Regex = """^([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$""".toRegex()

    val isValid: Boolean
        get() = isIPv4 || isIPv6

    val isIPv6: Boolean
        get() = ip == "::1" || ipv6Regex.matches(ip)

    val isIPv4: Boolean
        get() = ipv4Regex.matches(ip)

    val isBogon: Boolean
        get() = isValid && ip in listOf("0.0.0.0", "127.0.0.1", "::1", "169.254.0.0")
}
