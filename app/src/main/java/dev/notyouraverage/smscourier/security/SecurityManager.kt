package dev.notyouraverage.smscourier.security

import android.util.Base64
import android.util.Log
import dev.notyouraverage.smscourier.data.entities.PairedDevice
import dev.notyouraverage.smscourier.repository.PairedDeviceRepository
import org.mindrot.jbcrypt.BCrypt
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class SecurityManager(
    private val deviceRepository: PairedDeviceRepository,
) {
    companion object {
        private const val TAG = "SMSC:SecurityManager"
        private const val BCRYPT_COST = 12
        const val MAX_FAILED_ATTEMPTS = 5
        const val LOCKOUT_DURATION_MS = 15 * 60 * 1000L // 15 minutes
        private const val CHALLENGE_EXPIRY_MS = 2 * 60 * 1000L // 2 minutes
        private const val NONCE_LENGTH = 16

        fun hashPassword(password: String): PasswordHash {
            val salt = BCrypt.gensalt(BCRYPT_COST)
            val hash = BCrypt.hashpw(password, salt)
            return PasswordHash(hash = hash, salt = salt)
        }

        fun verifyPasswordStatic(password: String, storedHash: String): Boolean {
            return try {
                BCrypt.checkpw(password, storedHash)
            } catch (e: Exception) {
                false
            }
        }

        /**
         * Derive an authentication key from password using SHA-256.
         * This key is stored on TARGET and computed on SOURCE for HMAC verification.
         */
        fun deriveAuthKey(password: String): String {
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(password.toByteArray(Charsets.UTF_8))
            return Base64.encodeToString(hash, Base64.NO_WRAP)
        }

        /**
         * Compute HMAC-SHA256 of data using the given key.
         */
        fun computeHmac(authKey: String, data: String): String {
            val keyBytes = Base64.decode(authKey, Base64.NO_WRAP)
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(SecretKeySpec(keyBytes, "HmacSHA256"))
            val hmac = mac.doFinal(data.toByteArray(Charsets.UTF_8))
            return Base64.encodeToString(hmac, Base64.NO_WRAP)
        }

        /**
         * Generate a secure random nonce for challenge-response.
         */
        fun generateNonce(): String {
            val bytes = ByteArray(NONCE_LENGTH)
            SecureRandom().nextBytes(bytes)
            return Base64.encodeToString(bytes, Base64.NO_WRAP)
        }
    }

    // Pending challenges: phoneNumber -> (nonce, expiresAt)
    private val pendingChallenges = mutableMapOf<String, Pair<String, Long>>()

    data class PasswordHash(
        val hash: String,
        val salt: String,
    )

    fun verifyPassword(password: String, device: PairedDevice): Boolean {
        val storedHash = device.passwordHash ?: return false
        return try {
            BCrypt.checkpw(password, storedHash)
        } catch (e: Exception) {
            false
        }
    }

    fun isDeviceLocked(device: PairedDevice): Boolean {
        val lockedUntil = device.lockedUntil ?: return false
        return System.currentTimeMillis() < lockedUntil
    }

    fun getRemainingLockoutTimeMs(device: PairedDevice): Long {
        val lockedUntil = device.lockedUntil ?: return 0
        val remaining = lockedUntil - System.currentTimeMillis()
        return if (remaining > 0) remaining else 0
    }

    suspend fun recordFailedAttempt(device: PairedDevice) {
        val newAttempts = device.failedAttempts + 1
        val lockedUntil = if (newAttempts >= MAX_FAILED_ATTEMPTS) {
            System.currentTimeMillis() + LOCKOUT_DURATION_MS
        } else {
            null
        }

        deviceRepository.updateFailedAttempts(device.phoneNumber, device.role, newAttempts, lockedUntil)
    }

    suspend fun resetFailedAttempts(device: PairedDevice) {
        if (device.failedAttempts > 0 || device.lockedUntil != null) {
            deviceRepository.updateFailedAttempts(device.phoneNumber, device.role, 0, null)
        }
    }

    /**
     * Generate a challenge for the given phone number.
     * Returns the nonce that should be sent to the source device.
     */
    fun generateChallenge(phoneNumber: String): String {
        // Clean up any expired challenges
        cleanupExpiredChallenges()

        val nonce = generateNonce()
        val expiresAt = System.currentTimeMillis() + CHALLENGE_EXPIRY_MS
        pendingChallenges[phoneNumber] = Pair(nonce, expiresAt)
        Log.d(TAG, "Generated challenge for $phoneNumber, expires in ${CHALLENGE_EXPIRY_MS / 1000}s")
        return nonce
    }

    /**
     * Validate a challenge response from the source device.
     * Returns true if the response is valid and matches the pending challenge.
     */
    fun validateChallengeResponse(phoneNumber: String, response: String, authKey: String): Boolean {
        val pending = pendingChallenges[phoneNumber]
        if (pending == null) {
            Log.w(TAG, "No pending challenge for $phoneNumber")
            return false
        }

        val (nonce, expiresAt) = pending

        if (System.currentTimeMillis() > expiresAt) {
            Log.w(TAG, "Challenge expired for $phoneNumber")
            pendingChallenges.remove(phoneNumber)
            return false
        }

        val expected = computeHmac(authKey, nonce)
        val valid = response == expected

        if (valid) {
            Log.i(TAG, "Challenge response valid for $phoneNumber")
            pendingChallenges.remove(phoneNumber)
        } else {
            Log.w(TAG, "Challenge response invalid for $phoneNumber")
        }

        return valid
    }

    /**
     * Check if there's a pending challenge for the given phone number.
     */
    fun hasPendingChallenge(phoneNumber: String): Boolean {
        val pending = pendingChallenges[phoneNumber] ?: return false
        return System.currentTimeMillis() <= pending.second
    }

    private fun cleanupExpiredChallenges() {
        val now = System.currentTimeMillis()
        pendingChallenges.entries.removeIf { (_, value) -> now > value.second }
    }

    /**
     * Validate challenge response and authenticate the device.
     * This replaces password-based auth for the challenge-response flow.
     */
    suspend fun validateChallengeAndAuthenticate(
        senderPhoneNumber: String,
        response: String,
    ): AuthenticationResult {
        val device = deviceRepository.getApprovedSourceDevice(senderPhoneNumber)
            ?: return AuthenticationResult.DeviceNotFound

        if (isDeviceLocked(device)) {
            return AuthenticationResult.DeviceLocked(
                remainingMs = getRemainingLockoutTimeMs(device),
            )
        }

        val authKey = device.authKey
        if (authKey.isNullOrBlank()) {
            Log.e(TAG, "No auth key stored for device $senderPhoneNumber - device needs to be re-paired")
            return AuthenticationResult.AuthKeyMissing
        }

        return if (validateChallengeResponse(senderPhoneNumber, response, authKey)) {
            resetFailedAttempts(device)
            deviceRepository.updateLastActivity(senderPhoneNumber, device.role)
            AuthenticationResult.Success(device)
        } else {
            recordFailedAttempt(device)
            val updatedDevice = deviceRepository.getByPhoneNumberAndRole(senderPhoneNumber, device.role)
            AuthenticationResult.InvalidResponse(
                attemptsRemaining = MAX_FAILED_ATTEMPTS - (updatedDevice?.failedAttempts ?: 0),
            )
        }
    }

    sealed class AuthenticationResult {
        data class Success(val device: PairedDevice) : AuthenticationResult()
        data object DeviceNotFound : AuthenticationResult()
        data class DeviceLocked(val remainingMs: Long) : AuthenticationResult()
        data class InvalidResponse(val attemptsRemaining: Int) : AuthenticationResult()
        data object AuthKeyMissing : AuthenticationResult()
        data object NoPendingChallenge : AuthenticationResult()
    }
}
