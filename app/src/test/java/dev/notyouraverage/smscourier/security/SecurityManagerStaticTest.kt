package dev.notyouraverage.smscourier.security

import android.util.Base64
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SecurityManagerStaticTest {

    // ==================== hashPassword ====================

    @Test
    fun `hashPassword creates valid bcrypt hash`() {
        val password = "testPassword123"
        val result = SecurityManager.hashPassword(password)

        assertNotNull(result.hash)
        assertNotNull(result.salt)
        assertTrue(result.hash.startsWith("\$2a\$12\$"))
    }

    @Test
    fun `hashPassword includes salt`() {
        val result = SecurityManager.hashPassword("password")

        assertNotNull(result.salt)
        assertTrue(result.salt.isNotEmpty())
    }

    @Test
    fun `hashPassword produces different hashes for same password due to random salt`() {
        val password = "samePassword"
        val hash1 = SecurityManager.hashPassword(password)
        val hash2 = SecurityManager.hashPassword(password)

        assertNotEquals(hash1.hash, hash2.hash)
    }

    @Test
    fun `hashPassword produces different hashes for different passwords`() {
        val hash1 = SecurityManager.hashPassword("password1")
        val hash2 = SecurityManager.hashPassword("password2")

        assertNotEquals(hash1.hash, hash2.hash)
    }

    // ==================== verifyPasswordStatic ====================

    @Test
    fun `verifyPasswordStatic returns true for correct password`() {
        val password = "mySecret"
        val hash = SecurityManager.hashPassword(password)

        assertTrue(SecurityManager.verifyPasswordStatic(password, hash.hash))
    }

    @Test
    fun `verifyPasswordStatic returns false for wrong password`() {
        val hash = SecurityManager.hashPassword("correctPassword")

        assertFalse(SecurityManager.verifyPasswordStatic("wrongPassword", hash.hash))
    }

    @Test
    fun `verifyPasswordStatic returns false for similar but different password`() {
        val hash = SecurityManager.hashPassword("password123")

        assertFalse(SecurityManager.verifyPasswordStatic("password124", hash.hash))
    }

    @Test
    fun `verifyPasswordStatic returns false for invalid hash`() {
        assertFalse(SecurityManager.verifyPasswordStatic("password", "invalid-hash"))
    }

    @Test
    fun `verifyPasswordStatic returns false for empty hash`() {
        assertFalse(SecurityManager.verifyPasswordStatic("password", ""))
    }

    // ==================== deriveAuthKey ====================

    @Test
    fun `deriveAuthKey is deterministic`() {
        val password = "testPassword"
        val key1 = SecurityManager.deriveAuthKey(password)
        val key2 = SecurityManager.deriveAuthKey(password)

        assertEquals(key1, key2)
    }

    @Test
    fun `deriveAuthKey produces different output for different passwords`() {
        val key1 = SecurityManager.deriveAuthKey("password1")
        val key2 = SecurityManager.deriveAuthKey("password2")

        assertNotEquals(key1, key2)
    }

    @Test
    fun `deriveAuthKey returns valid base64`() {
        val key = SecurityManager.deriveAuthKey("testPassword")

        assertNotNull(key)
        assertTrue(key.isNotEmpty())

        // Should not throw when decoded
        val decoded = Base64.decode(key, Base64.NO_WRAP)
        assertEquals(32, decoded.size) // SHA-256 produces 32 bytes
    }

    @Test
    fun `deriveAuthKey handles empty password`() {
        val key = SecurityManager.deriveAuthKey("")

        assertNotNull(key)
        assertTrue(key.isNotEmpty())
    }

    @Test
    fun `deriveAuthKey handles unicode password`() {
        val key = SecurityManager.deriveAuthKey("密码🔒пароль")

        assertNotNull(key)
        assertTrue(key.isNotEmpty())
    }

    // ==================== computeHmac ====================

    @Test
    fun `computeHmac is deterministic`() {
        val authKey = SecurityManager.deriveAuthKey("password")
        val data = "testData"

        val hmac1 = SecurityManager.computeHmac(authKey, data)
        val hmac2 = SecurityManager.computeHmac(authKey, data)

        assertEquals(hmac1, hmac2)
    }

    @Test
    fun `computeHmac produces different output for different data`() {
        val authKey = SecurityManager.deriveAuthKey("password")

        val hmac1 = SecurityManager.computeHmac(authKey, "data1")
        val hmac2 = SecurityManager.computeHmac(authKey, "data2")

        assertNotEquals(hmac1, hmac2)
    }

    @Test
    fun `computeHmac produces different output for different keys`() {
        val key1 = SecurityManager.deriveAuthKey("password1")
        val key2 = SecurityManager.deriveAuthKey("password2")
        val data = "sameData"

        val hmac1 = SecurityManager.computeHmac(key1, data)
        val hmac2 = SecurityManager.computeHmac(key2, data)

        assertNotEquals(hmac1, hmac2)
    }

    @Test
    fun `computeHmac returns valid base64`() {
        val authKey = SecurityManager.deriveAuthKey("password")
        val hmac = SecurityManager.computeHmac(authKey, "data")

        assertNotNull(hmac)
        assertTrue(hmac.isNotEmpty())

        // Should not throw when decoded
        val decoded = Base64.decode(hmac, Base64.NO_WRAP)
        assertEquals(32, decoded.size) // HMAC-SHA256 produces 32 bytes
    }

    // ==================== generateNonce ====================

    @Test
    fun `generateNonce returns valid base64`() {
        val nonce = SecurityManager.generateNonce()

        assertNotNull(nonce)
        assertTrue(nonce.isNotEmpty())

        // Should not throw when decoded
        val decoded = Base64.decode(nonce, Base64.NO_WRAP)
        assertEquals(16, decoded.size) // NONCE_LENGTH = 16
    }

    @Test
    fun `generateNonce produces unique values`() {
        val nonces = (1..100).map { SecurityManager.generateNonce() }.toSet()

        assertEquals(100, nonces.size)
    }

    @Test
    fun `generateNonce produces different values on consecutive calls`() {
        val nonce1 = SecurityManager.generateNonce()
        val nonce2 = SecurityManager.generateNonce()

        assertNotEquals(nonce1, nonce2)
    }

    // ==================== Integration: Challenge-Response Flow ====================

    @Test
    fun `challenge-response flow works end-to-end`() {
        val password = "testPassword123"

        // TARGET derives and stores authKey during pairing
        val authKey = SecurityManager.deriveAuthKey(password)

        // TARGET generates nonce for challenge
        val nonce = SecurityManager.generateNonce()

        // SOURCE computes same authKey from password and creates HMAC response
        val sourceAuthKey = SecurityManager.deriveAuthKey(password)
        val response = SecurityManager.computeHmac(sourceAuthKey, nonce)

        // TARGET verifies response
        val expectedResponse = SecurityManager.computeHmac(authKey, nonce)

        assertEquals(response, expectedResponse)
    }

    @Test
    fun `wrong password produces different HMAC response`() {
        val correctPassword = "correctPassword"
        val wrongPassword = "wrongPassword"

        val authKey = SecurityManager.deriveAuthKey(correctPassword)
        val nonce = SecurityManager.generateNonce()

        val wrongAuthKey = SecurityManager.deriveAuthKey(wrongPassword)
        val wrongResponse = SecurityManager.computeHmac(wrongAuthKey, nonce)

        val expectedResponse = SecurityManager.computeHmac(authKey, nonce)

        assertNotEquals(wrongResponse, expectedResponse)
    }
}
