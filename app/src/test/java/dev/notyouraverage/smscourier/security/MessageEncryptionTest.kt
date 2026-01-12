package dev.notyouraverage.smscourier.security

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MessageEncryptionTest {

    // ==================== Round-trip Encryption/Decryption ====================

    @Test
    fun `encrypt returns non-null for valid input`() {
        val result = MessageEncryption.encrypt("Hello World", "password123")
        assertNotNull(result)
    }

    @Test
    fun `decrypt returns original message for correct password`() {
        val original = "Hello, this is a test message!"
        val password = "secretPassword123"

        val encrypted = MessageEncryption.encrypt(original, password)
        assertNotNull(encrypted)

        val decrypted = MessageEncryption.decrypt(encrypted!!, password)
        assertEquals(original, decrypted)
    }

    @Test
    fun `encrypt and decrypt round-trip with simple message`() {
        val original = "Test"
        val password = "pass"

        val encrypted = MessageEncryption.encrypt(original, password)
        val decrypted = MessageEncryption.decrypt(encrypted!!, password)

        assertEquals(original, decrypted)
    }

    // ==================== Wrong Password ====================

    @Test
    fun `decrypt returns null for wrong password`() {
        val encrypted = MessageEncryption.encrypt("Test message", "correctPassword")
        val decrypted = MessageEncryption.decrypt(encrypted!!, "wrongPassword")

        assertNull(decrypted)
    }

    @Test
    fun `decrypt returns null for similar but different password`() {
        val encrypted = MessageEncryption.encrypt("Test", "password123")
        val decrypted = MessageEncryption.decrypt(encrypted!!, "password124")

        assertNull(decrypted)
    }

    // ==================== Malformed Input ====================

    @Test
    fun `decrypt returns null for invalid base64`() {
        val result = MessageEncryption.decrypt("not-valid-base64!!!", "password")
        assertNull(result)
    }

    @Test
    fun `decrypt returns null for too-short encrypted data`() {
        // Need at least 33 bytes (16 salt + 16 iv + 1 ciphertext)
        // "dG9vLXNob3J0" is "too-short" in base64, only ~9 bytes
        val result = MessageEncryption.decrypt("dG9vLXNob3J0", "password")
        assertNull(result)
    }

    @Test
    fun `decrypt returns null for empty string`() {
        val result = MessageEncryption.decrypt("", "password")
        assertNull(result)
    }

    @Test
    fun `decrypt returns null for truncated ciphertext`() {
        val encrypted = MessageEncryption.encrypt("Hello World", "password")
        // Truncate the encrypted string
        val truncated = encrypted!!.take(encrypted.length / 2)
        val result = MessageEncryption.decrypt(truncated, "password")
        assertNull(result)
    }

    // ==================== Randomness (Salt/IV) ====================

    @Test
    fun `encrypt produces different output for same input due to random salt and IV`() {
        val message = "Same message"
        val password = "samePassword"

        val encrypted1 = MessageEncryption.encrypt(message, password)
        val encrypted2 = MessageEncryption.encrypt(message, password)

        assertNotNull(encrypted1)
        assertNotNull(encrypted2)
        assertNotEquals(encrypted1, encrypted2)
    }

    @Test
    fun `both different encryptions decrypt to same message`() {
        val message = "Test message"
        val password = "testPass"

        val encrypted1 = MessageEncryption.encrypt(message, password)
        val encrypted2 = MessageEncryption.encrypt(message, password)

        assertEquals(message, MessageEncryption.decrypt(encrypted1!!, password))
        assertEquals(message, MessageEncryption.decrypt(encrypted2!!, password))
    }

    // ==================== Special Characters and Unicode ====================

    @Test
    fun `encrypt and decrypt handles unicode characters`() {
        val original = "Hello 世界 🌍 привет مرحبا"
        val password = "unicodeTest"

        val encrypted = MessageEncryption.encrypt(original, password)
        val decrypted = MessageEncryption.decrypt(encrypted!!, password)

        assertEquals(original, decrypted)
    }

    @Test
    fun `encrypt and decrypt handles special characters`() {
        val original = "Code: 123-456 | Expires: 5min! @#\$%^&*()"
        val password = "specialChars"

        val encrypted = MessageEncryption.encrypt(original, password)
        val decrypted = MessageEncryption.decrypt(encrypted!!, password)

        assertEquals(original, decrypted)
    }

    @Test
    fun `encrypt and decrypt handles pipe character`() {
        val original = "+1234567890|Your OTP is 123456"
        val password = "pipeTest"

        val encrypted = MessageEncryption.encrypt(original, password)
        val decrypted = MessageEncryption.decrypt(encrypted!!, password)

        assertEquals(original, decrypted)
    }

    @Test
    fun `encrypt and decrypt handles newlines`() {
        val original = "Line 1\nLine 2\r\nLine 3"
        val password = "newlineTest"

        val encrypted = MessageEncryption.encrypt(original, password)
        val decrypted = MessageEncryption.decrypt(encrypted!!, password)

        assertEquals(original, decrypted)
    }

    // ==================== Edge Cases ====================

    @Test
    fun `encrypt and decrypt handles empty message`() {
        val original = ""
        val password = "test"

        val encrypted = MessageEncryption.encrypt(original, password)
        val decrypted = MessageEncryption.decrypt(encrypted!!, password)

        assertEquals(original, decrypted)
    }

    @Test
    fun `encrypt and decrypt handles long message`() {
        val original = "A".repeat(10000)
        val password = "test"

        val encrypted = MessageEncryption.encrypt(original, password)
        val decrypted = MessageEncryption.decrypt(encrypted!!, password)

        assertEquals(original, decrypted)
    }

    @Test
    fun `encrypt and decrypt handles short password`() {
        val original = "Test message"
        val password = "a"

        val encrypted = MessageEncryption.encrypt(original, password)
        val decrypted = MessageEncryption.decrypt(encrypted!!, password)

        assertEquals(original, decrypted)
    }

    @Test
    fun `encrypt and decrypt handles long password`() {
        val original = "Test message"
        val password = "x".repeat(1000)

        val encrypted = MessageEncryption.encrypt(original, password)
        val decrypted = MessageEncryption.decrypt(encrypted!!, password)

        assertEquals(original, decrypted)
    }
}
