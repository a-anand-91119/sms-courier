package dev.notyouraverage.smscourier.security

import android.util.Base64
import android.util.Log
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Utility for encrypting/decrypting forwarded SMS messages.
 * Uses AES-256-CBC with PBKDF2 key derivation.
 */
object MessageEncryption {
    private const val TAG = "OTPC:Encryption"
    private const val ALGORITHM = "AES/CBC/PKCS5Padding"
    private const val KEY_ALGORITHM = "AES"
    private const val KEY_DERIVATION = "PBKDF2WithHmacSHA256"
    private const val KEY_LENGTH = 256
    private const val ITERATION_COUNT = 10000
    private const val IV_LENGTH = 16
    private const val SALT_LENGTH = 16

    /**
     * Encrypts a message using the provided password.
     * Returns Base64 encoded string: salt(16) + iv(16) + ciphertext
     */
    fun encrypt(message: String, password: String): String? {
        return try {
            val salt = ByteArray(SALT_LENGTH).also { SecureRandom().nextBytes(it) }
            val iv = ByteArray(IV_LENGTH).also { SecureRandom().nextBytes(it) }

            val key = deriveKey(password, salt)
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.ENCRYPT_MODE, key, IvParameterSpec(iv))

            val encrypted = cipher.doFinal(message.toByteArray(Charsets.UTF_8))

            // Combine: salt + iv + ciphertext
            val combined = salt + iv + encrypted
            Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e(TAG, "Encryption failed: ${e.message}", e)
            null
        }
    }

    /**
     * Decrypts a Base64 encoded encrypted message using the provided password.
     */
    fun decrypt(encryptedBase64: String, password: String): String? {
        return try {
            val combined = Base64.decode(encryptedBase64, Base64.NO_WRAP)

            if (combined.size < SALT_LENGTH + IV_LENGTH + 1) {
                Log.e(TAG, "Encrypted data too short")
                return null
            }

            val salt = combined.copyOfRange(0, SALT_LENGTH)
            val iv = combined.copyOfRange(SALT_LENGTH, SALT_LENGTH + IV_LENGTH)
            val ciphertext = combined.copyOfRange(SALT_LENGTH + IV_LENGTH, combined.size)

            val key = deriveKey(password, salt)
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.DECRYPT_MODE, key, IvParameterSpec(iv))

            val decrypted = cipher.doFinal(ciphertext)
            String(decrypted, Charsets.UTF_8)
        } catch (e: Exception) {
            Log.e(TAG, "Decryption failed: ${e.message}", e)
            null
        }
    }

    private fun deriveKey(password: String, salt: ByteArray): SecretKeySpec {
        val factory = SecretKeyFactory.getInstance(KEY_DERIVATION)
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH)
        val secretKey = factory.generateSecret(spec)
        return SecretKeySpec(secretKey.encoded, KEY_ALGORITHM)
    }
}
