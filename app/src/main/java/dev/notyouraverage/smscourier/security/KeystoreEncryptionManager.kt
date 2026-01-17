package dev.notyouraverage.smscourier.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Manages encryption/decryption of sensitive data using Android Keystore.
 *
 * Uses AES-256-GCM for authenticated encryption. The encryption key is stored
 * in the Android Keystore, which provides hardware-backed protection on supported
 * devices. The key cannot be extracted from the Keystore.
 *
 * Used to encrypt the activeEncryptionKey before storing in Room database.
 */
object KeystoreEncryptionManager {
    private const val TAG = "SMSC:KeystoreEncryption"
    private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
    private const val KEY_ALIAS = "smscourier_storage_key"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH = 128
    private const val GCM_IV_LENGTH = 12

    private val keyStore: KeyStore by lazy {
        KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
    }

    /**
     * Encrypts plaintext using the Keystore-backed AES key.
     *
     * @param plaintext The string to encrypt
     * @return Base64 encoded string: iv(12) + ciphertext+tag, or null on failure
     */
    fun encrypt(plaintext: String): String? {
        return try {
            val key = getOrCreateKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, key)

            val iv = cipher.iv
            val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))

            // Combine IV + ciphertext (GCM tag is appended to ciphertext by Cipher)
            val combined = iv + ciphertext
            Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e(TAG, "Encryption failed: ${e.message}", e)
            null
        }
    }

    /**
     * Decrypts a Base64 encoded encrypted string using the Keystore-backed AES key.
     *
     * @param encryptedBase64 Base64 encoded iv + ciphertext
     * @return The decrypted plaintext string, or null on failure
     */
    fun decrypt(encryptedBase64: String): String? {
        return try {
            val combined = Base64.decode(encryptedBase64, Base64.NO_WRAP)

            if (combined.size < GCM_IV_LENGTH + 1) {
                Log.e(TAG, "Encrypted data too short")
                return null
            }

            val iv = combined.copyOfRange(0, GCM_IV_LENGTH)
            val ciphertext = combined.copyOfRange(GCM_IV_LENGTH, combined.size)

            val key = getKey() ?: run {
                Log.e(TAG, "No encryption key found in Keystore")
                return null
            }

            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH, iv))

            val decrypted = cipher.doFinal(ciphertext)
            String(decrypted, Charsets.UTF_8)
        } catch (e: Exception) {
            Log.e(TAG, "Decryption failed: ${e.message}", e)
            null
        }
    }

    /**
     * Check if an encryption key exists in the Keystore.
     */
    fun hasKey(): Boolean {
        return try {
            keyStore.containsAlias(KEY_ALIAS)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking key existence: ${e.message}", e)
            false
        }
    }

    private fun getKey(): SecretKey? {
        return try {
            keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving key: ${e.message}", e)
            null
        }
    }

    private fun getOrCreateKey(): SecretKey {
        return getKey() ?: createKey()
    }

    private fun createKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            KEYSTORE_PROVIDER,
        )

        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()

        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }
}
