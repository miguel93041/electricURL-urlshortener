@file:Suppress("WildcardImport")

package es.unizar.urlshortener.thirdparties.ipinfo

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class IpAddressTest {

    @Test
    fun `should validate IPv4 addresses correctly`() {
        val ipv4Address = IpAddress("192.168.1.1")

        assertTrue(ipv4Address.isIPv4)
        assertFalse(ipv4Address.isIPv6)
        assertTrue(ipv4Address.isValid)
        assertFalse(ipv4Address.isBogon)
    }

    @Test
    fun `should validate IPv6 addresses correctly`() {
        val ipv6Address = IpAddress("2001:0db8:85a3:0000:0000:8a2e:0370:7334")

        assertTrue(ipv6Address.isIPv6)
        assertFalse(ipv6Address.isIPv4)
        assertTrue(ipv6Address.isValid)
        assertFalse(ipv6Address.isBogon)
    }

    @Test
    fun `should identify valid but bogon IPv4 addresses`() {
        val bogonIPv4Address = IpAddress("0.0.0.0")

        assertTrue(bogonIPv4Address.isBogon)
        assertTrue(bogonIPv4Address.isIPv4)
        assertFalse(bogonIPv4Address.isIPv6)
        assertTrue(bogonIPv4Address.isValid)
    }

    @Test
    fun `should identify valid but bogon IPv6 addresses`() {
        val bogonIPv6Address = IpAddress("::1")

        assertTrue(bogonIPv6Address.isBogon)
        assertTrue(bogonIPv6Address.isIPv6)
        assertFalse(bogonIPv6Address.isIPv4)
        assertTrue(bogonIPv6Address.isValid)
    }

    @Test
    fun `should identify invalid IP addresses`() {
        val invalidIPv4Address = IpAddress("999.999.999.999")
        val invalidIPv6Address = IpAddress("2001:0db8:85a3:0000:0000:8a2e:0370:zzzz")

        assertFalse(invalidIPv4Address.isValid)
        assertFalse(invalidIPv4Address.isIPv4)
        assertFalse(invalidIPv4Address.isIPv6)
        assertFalse(invalidIPv4Address.isBogon)

        assertFalse(invalidIPv6Address.isValid)
        assertFalse(invalidIPv6Address.isIPv4)
        assertFalse(invalidIPv6Address.isIPv6)
        assertFalse(invalidIPv6Address.isBogon)
    }
}
