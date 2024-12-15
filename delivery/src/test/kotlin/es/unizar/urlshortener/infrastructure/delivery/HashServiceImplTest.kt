package es.unizar.urlshortener.infrastructure.delivery

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.test.context.ContextConfiguration

@WebFluxTest(HashServiceImpl::class)
@ContextConfiguration(
    classes = [
        HashServiceImpl::class
    ]
)
class HashServiceImplBlockTest {

    @Test
    fun `generateRandomHash - returns an 8-char hex string`() {
        val hashService = HashServiceImpl()
        val hash = hashService.generateRandomHash()

        assertNotNull(hash)
        assertEquals(8, hash.length)
        assertTrue(hash.matches(Regex("^[a-f0-9]{8}$")), "Hash should be 8 hex characters")
    }

    @Test
    fun `generateRandomHash - consecutive calls should differ`() {
        val hashService = HashServiceImpl()
        val hash1 = hashService.generateRandomHash()
        val hash2 = hashService.generateRandomHash()
        assertNotEquals(hash1, hash2, "Hashes should be different across calls")
    }
}