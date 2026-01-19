package dev.notyouraverage.smscourier.services.foreground

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Telephony
import android.telephony.SmsManager
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import dev.notyouraverage.smscourier.R
import dev.notyouraverage.smscourier.activities.MainActivity
import dev.notyouraverage.smscourier.constants.Constants.CODE_FOREGROUND_SERVICE
import dev.notyouraverage.smscourier.constants.Constants.NOTIFICATION_CHANNEL_GENERAL
import dev.notyouraverage.smscourier.data.SmsCourierDatabase
import dev.notyouraverage.smscourier.data.entities.DeviceRole
import dev.notyouraverage.smscourier.handlers.SmsCommandHandler
import dev.notyouraverage.smscourier.models.SmsMessageData
import dev.notyouraverage.smscourier.notifications.PairingNotificationManager
import dev.notyouraverage.smscourier.receivers.ServiceNotificationReceiver
import dev.notyouraverage.smscourier.receivers.SmsReceiver
import dev.notyouraverage.smscourier.repository.ForwardingSessionRepository
import dev.notyouraverage.smscourier.repository.PairedDeviceRepository
import dev.notyouraverage.smscourier.data.settings.SettingsDefaults
import dev.notyouraverage.smscourier.repository.SettingsRepository
import dev.notyouraverage.smscourier.security.SecurityManager
import dev.notyouraverage.smscourier.services.SmsSender
import dev.notyouraverage.smscourier.services.background.SmsService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MasterService : Service() {

    private lateinit var smsReceiver: SmsReceiver
    private lateinit var smsSentReceiver: BroadcastReceiver
    private var backgroundServiceRunning = false

    private val stopDelay: Long = 5 * 60 * 1000
    private val handler: Handler = Handler(Looper.getMainLooper())

    // Coroutine scope for async operations
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // New components for pairing system
    private lateinit var database: SmsCourierDatabase
    private lateinit var deviceRepository: PairedDeviceRepository
    private lateinit var sessionRepository: ForwardingSessionRepository
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var smsSender: SmsSender
    private lateinit var notificationManager: PairingNotificationManager
    private lateinit var securityManager: SecurityManager
    private lateinit var commandHandler: SmsCommandHandler

    // Track active forwarding sessions with their timeout handlers
    private val sessionHandlers = mutableMapOf<String, Runnable>()

    // Track pending auth requests on SOURCE side: targetPhone -> (password, duration)
    private val pendingAuthRequests = mutableMapOf<String, PendingAuthRequest>()

    // Cached auth request timeout (updated via Flow)
    private var authRequestTimeoutMinutes = SettingsDefaults.AUTH_REQUEST_TIMEOUT
    private val authRequestTimeoutMs: Long get() = authRequestTimeoutMinutes * 60 * 1000L

    data class PendingAuthRequest(
        val password: String,
        val durationMinutes: Int,
        val requestedAt: Long = System.currentTimeMillis(),
    )

    companion object {
        private const val TAG = "SMSC:MasterService"

        // Track service running state
        @Volatile
        var isRunning: Boolean = false
            private set

        const val SMS_DATA = "SMS_DATA"
        const val START_SELF = "START_SELF"
        const val STOP_SELF = "STOP_SELF"
        const val START_BACKGROUND = "START_BACKGROUND"
        const val STOP_BACKGROUND = "STOP_BACKGROUND"
        const val SEND_DATA = "SEND_DATA"
        const val RECREATE_NOTIFICATION = "RECREATE_NOTIFICATION"

        // New actions for pairing system
        const val PAIRING_APPROVE_REQUESTED = "PAIRING_APPROVE_REQUESTED"
        const val PAIRING_REJECT_REQUESTED = "PAIRING_REJECT_REQUESTED"
        const val EXTRA_PHONE_NUMBER = "EXTRA_PHONE_NUMBER"
        const val EXTRA_PASSWORD = "EXTRA_PASSWORD"

        // Command processing actions
        const val PROCESS_COMMAND = "PROCESS_COMMAND"
        const val FORWARD_SMS = "FORWARD_SMS"
        const val EXTRA_SENDER = "EXTRA_SENDER"
        const val EXTRA_RAW_MESSAGE = "EXTRA_RAW_MESSAGE"
        const val EXTRA_COMMAND_TYPE = "EXTRA_COMMAND_TYPE"
        const val EXTRA_DURATION = "EXTRA_DURATION"
        const val EXTRA_ORIGINAL_SENDER = "EXTRA_ORIGINAL_SENDER"
        const val EXTRA_FORWARDED_CONTENT = "EXTRA_FORWARDED_CONTENT"
        const val EXTRA_NONCE = "EXTRA_NONCE"
        const val EXTRA_UNPAIR_ROLE = "EXTRA_UNPAIR_ROLE"

        // Action for handling auth challenge on SOURCE side
        const val AUTH_CHALLENGE_RECEIVED = "AUTH_CHALLENGE_RECEIVED"

        // Action to initiate auth request (stores pending request and sends AUTH_REQUEST)
        const val INITIATE_AUTH_REQUEST = "INITIATE_AUTH_REQUEST"
    }

    override fun onBind(p0: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "MasterService::onCreate")

        // Initialize new pairing system components
        database = SmsCourierDatabase.getDatabase(this)
        deviceRepository = PairedDeviceRepository(database.pairedDeviceDao())
        sessionRepository = ForwardingSessionRepository(database.forwardingSessionDao())
        settingsRepository = SettingsRepository(this)
        smsSender = SmsSender(this)
        notificationManager = PairingNotificationManager(this)
        securityManager = SecurityManager(deviceRepository, settingsRepository)
        commandHandler = SmsCommandHandler(
            deviceRepository = deviceRepository,
            sessionRepository = sessionRepository,
            smsSender = smsSender,
            notificationManager = notificationManager,
            securityManager = securityManager,
            onForwardingStateChanged = { state -> handleForwardingStateChanged(state) },
        )

        // Create notification channels
        notificationManager.createNotificationChannels()
        createForegroundServiceChannel()
    }

    private fun createForegroundServiceChannel() {
        val notificationManager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_GENERAL,
            "Service Status",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Shows when SMS Courier service is running"
            setShowBadge(false)
        }
        notificationManager.createNotificationChannel(channel)
    }

    private fun buildForegroundNotification(): android.app.Notification {
        // Create delete intent for when user dismisses the notification
        val deleteIntent = Intent(this, ServiceNotificationReceiver::class.java).apply {
            action = ServiceNotificationReceiver.ACTION_NOTIFICATION_DISMISSED
        }
        val deletePendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            deleteIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        // Create content intent to open app when notification is tapped
        val contentIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            0,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_GENERAL)
            .setTicker(null)
            .setContentTitle("SMS Courier")
            .setContentText("SMS Courier is running")
            .setAutoCancel(false)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setWhen(System.currentTimeMillis())
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(contentPendingIntent)
            .setDeleteIntent(deletePendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun handleRecreateNotification() {
        serviceScope.launch {
            val shouldPersist = settingsRepository.notificationPersistence.first()
            if (shouldPersist) {
                recreateForegroundNotification()
            } else {
                Log.i(TAG, "Notification persistence disabled, not recreating")
            }
        }
    }

    private fun recreateForegroundNotification() {
        if (!isRunning) return
        Log.i(TAG, "Recreating foreground notification")
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(CODE_FOREGROUND_SERVICE, buildForegroundNotification())
    }

    override fun onDestroy() {
        isRunning = false
        Toast.makeText(this, "Killing Foreground Service", Toast.LENGTH_SHORT).show()
        Log.i(TAG, "MasterService::onDestroy")
        if (this::smsReceiver.isInitialized) {
            unregisterReceiver(smsReceiver)
        }
        if (this::smsSentReceiver.isInitialized) {
            unregisterReceiver(smsSentReceiver)
        }
        stopBackgroundService()
        // Cancel all session handlers
        sessionHandlers.values.forEach { handler.removeCallbacks(it) }
        sessionHandlers.clear()
        // Cancel coroutine scope
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "MasterService::onStartCommand action=${intent?.action}")
        when (intent?.action) {
            START_SELF -> startSelf(intent.extras)
            STOP_SELF -> stopSelf()
            START_BACKGROUND -> startBackgroundService()
            STOP_BACKGROUND -> stopBackgroundService()
            SEND_DATA -> sendToBackgroundService(
                intent.getParcelableExtra(
                    SMS_DATA,
                    SmsMessageData::class.java,
                ),
            )
            // New pairing actions
            PAIRING_APPROVE_REQUESTED -> handlePairingApproveRequested(
                intent.getStringExtra(EXTRA_PHONE_NUMBER),
            )
            PAIRING_REJECT_REQUESTED -> handlePairingRejectRequested(
                intent.getStringExtra(EXTRA_PHONE_NUMBER),
            )
            // Command processing from SmsReceiver
            PROCESS_COMMAND -> handleProcessCommand(intent)
            FORWARD_SMS -> handleForwardSms(
                intent.getParcelableExtra(SMS_DATA, SmsMessageData::class.java),
            )
            // Initiate auth request from ViewModel
            INITIATE_AUTH_REQUEST -> handleInitiateAuthRequest(
                intent.getStringExtra(EXTRA_PHONE_NUMBER),
                intent.getStringExtra(EXTRA_PASSWORD),
                intent.getIntExtra(EXTRA_DURATION, 30),
            )
            RECREATE_NOTIFICATION -> handleRecreateNotification()
        }
        return START_STICKY
    }

    private fun handleForwardingStateChanged(state: SmsCommandHandler.ForwardingState) {
        when (state) {
            is SmsCommandHandler.ForwardingState.Started -> {
                Log.i(
                    TAG,
                    "Forwarding started for ${state.phoneNumber} for ${state.durationMinutes} min",
                )
                notificationManager.showForwardingActiveNotification(
                    state.phoneNumber,
                    state.durationMinutes,
                )
                // Set up timeout handler
                val timeoutRunnable = Runnable {
                    serviceScope.launch {
                        sessionRepository.endSession(state.sessionId, "TIMEOUT")
                        notificationManager.cancelForwardingNotification()
                        sessionHandlers.remove(state.phoneNumber)
                    }
                }
                sessionHandlers[state.phoneNumber] = timeoutRunnable
                handler.postDelayed(timeoutRunnable, state.durationMinutes * 60 * 1000L)
            }
            is SmsCommandHandler.ForwardingState.Stopped -> {
                Log.i(TAG, "Forwarding stopped for ${state.phoneNumber}: ${state.reason}")
                notificationManager.cancelForwardingNotification()
                sessionHandlers[state.phoneNumber]?.let { handler.removeCallbacks(it) }
                sessionHandlers.remove(state.phoneNumber)
            }
        }
    }

    private fun handlePairingApproveRequested(phoneNumber: String?) {
        if (phoneNumber.isNullOrBlank()) {
            Log.e(TAG, "handlePairingApproveRequested: Missing phone number")
            return
        }
        // Note: Notification approve now launches MainActivity directly via PairingActionReceiver.
        // This method is kept for potential programmatic approval requests.
        Log.i(TAG, "Pairing approve requested for: $phoneNumber")
    }

    private fun handlePairingRejectRequested(phoneNumber: String?) {
        if (phoneNumber.isNullOrBlank()) {
            Log.e(TAG, "handlePairingRejectRequested: Missing phone number")
            return
        }
        Log.i(TAG, "Pairing reject requested for: $phoneNumber")
        serviceScope.launch {
            commandHandler.rejectPairing(phoneNumber)
        }
    }

    private fun handleProcessCommand(intent: Intent) {
        val sender = intent.getStringExtra(EXTRA_SENDER) ?: return
        val commandType = intent.getStringExtra(EXTRA_COMMAND_TYPE) ?: return
        val rawMessage = intent.getStringExtra(EXTRA_RAW_MESSAGE) ?: ""

        Log.i(TAG, "Processing command: $commandType from $sender")

        serviceScope.launch {
            when (commandType) {
                "PAIR_REQUEST" -> {
                    commandHandler.handlePairRequest(sender)
                }
                "PAIR_APPROVED" -> {
                    commandHandler.handlePairApproved(sender)
                }
                "PAIR_REJECTED" -> {
                    commandHandler.handlePairRejected(sender)
                }
                "UNPAIR" -> {
                    val roleStr = intent.getStringExtra(EXTRA_UNPAIR_ROLE)
                    val role = roleStr?.let {
                        when (it) {
                            "SOURCE" -> DeviceRole.SOURCE
                            "TARGET" -> DeviceRole.TARGET
                            else -> null
                        }
                    }
                    commandHandler.handleUnpair(sender, role)
                }
                "AUTH_REQUEST" -> {
                    commandHandler.handleAuthRequest(sender)
                }
                "AUTH_CHALLENGE" -> {
                    val nonce = intent.getStringExtra(EXTRA_NONCE) ?: ""
                    handleAuthChallengeReceived(sender, nonce)
                }
                "START_FORWARD" -> {
                    val password = intent.getStringExtra(EXTRA_PASSWORD) ?: ""
                    val duration = intent.getIntExtra(EXTRA_DURATION, 30)
                    commandHandler.handleStartForward(sender, password, duration)
                }
                "STOP_FORWARD" -> {
                    commandHandler.handleStopForward(sender)
                }
                "FWD" -> {
                    val originalSender = intent.getStringExtra(EXTRA_ORIGINAL_SENDER) ?: ""
                    val content = intent.getStringExtra(EXTRA_FORWARDED_CONTENT) ?: ""
                    handleForwardedMessageReceived(sender, originalSender, content)
                }
                "FWDE" -> {
                    val encryptedContent = intent.getStringExtra(EXTRA_FORWARDED_CONTENT) ?: ""
                    commandHandler.handleForwardedDataEncrypted(sender, encryptedContent)
                }
                "LEGACY_START" -> {
                    // Handle legacy start command (backward compatibility)
                    val password = intent.getStringExtra(EXTRA_PASSWORD) ?: ""
                    commandHandler.handleStartForward(sender, password, 30)
                }
                "LEGACY_STOP" -> {
                    // Handle legacy stop command (backward compatibility)
                    commandHandler.handleStopForward(sender)
                }
                else -> {
                    Log.w(TAG, "Unknown command type: $commandType")
                }
            }
        }
    }

    private fun handleForwardedMessageReceived(sender: String, originalSender: String, content: String) {
        // This is called when we (as source) receive a forwarded SMS from target
        Log.i(TAG, "Received forwarded SMS from $sender, original sender: $originalSender")
        // Display notification or store the message
        notificationManager.showForwardedMessageNotification(originalSender, content, sender)
    }

    private fun handleForwardSms(smsData: SmsMessageData?) {
        if (smsData == null || smsData.targetPhoneNumber.isNullOrBlank()) {
            Log.e(TAG, "handleForwardSms: Invalid SMS data")
            return
        }

        val targetPhone = smsData.targetPhoneNumber
        val originalSender = smsData.sender ?: "Unknown"
        val message = smsData.rawMessage ?: ""
        val encryptionKey = smsData.encryptionKey

        Log.i(TAG, "Forwarding SMS to $targetPhone from $originalSender (encrypted=${encryptionKey != null})")

        // Use sendForwardedSms with encryption if key is available
        smsSender.sendForwardedSms(targetPhone, originalSender, message, encryptionKey)
    }

    /**
     * Called from ViewModel to initiate challenge-response auth flow.
     * Stores the pending request and sends AUTH_REQUEST to target.
     */
    private fun handleInitiateAuthRequest(targetPhone: String?, password: String?, duration: Int) {
        if (targetPhone.isNullOrBlank() || password.isNullOrBlank()) {
            Log.e(TAG, "handleInitiateAuthRequest: Missing parameters")
            return
        }

        Log.i(TAG, "Initiating auth request to $targetPhone")

        // Store pending request for when challenge arrives
        pendingAuthRequests[targetPhone] = PendingAuthRequest(password, duration)

        // Also store encryption key for later decryption (use authKey for consistency with TARGET)
        val authKey = SecurityManager.deriveAuthKey(password)
        serviceScope.launch {
            // We are SOURCE, they are TARGET
            deviceRepository.updateEncryptionKey(targetPhone, DeviceRole.SOURCE, authKey)
        }

        // Send AUTH_REQUEST
        smsSender.sendAuthRequest(targetPhone)
    }

    /**
     * Called when we (SOURCE) receive AUTH_CHALLENGE from TARGET.
     * Computes HMAC response and sends START_FORWARD.
     */
    private fun handleAuthChallengeReceived(senderPhone: String, nonce: String) {
        val pending = pendingAuthRequests[senderPhone]
        if (pending == null) {
            Log.w(TAG, "Received AUTH_CHALLENGE from $senderPhone but no pending request")
            return
        }

        // Check if request is too old
        val age = System.currentTimeMillis() - pending.requestedAt
        if (age > authRequestTimeoutMs) {
            Log.w(TAG, "Pending auth request for $senderPhone has expired (>${authRequestTimeoutMinutes}min)")
            pendingAuthRequests.remove(senderPhone)
            return
        }

        Log.i(TAG, "Computing auth response for $senderPhone")

        // Compute HMAC response
        val authKey = SecurityManager.deriveAuthKey(pending.password)
        val response = SecurityManager.computeHmac(authKey, nonce)

        // Send START_FORWARD with the response
        smsSender.sendStartForwardWithResponse(senderPhone, response, pending.durationMinutes)

        // Remove pending request
        pendingAuthRequests.remove(senderPhone)
    }

    private fun sendToBackgroundService(smsMessageData: SmsMessageData?) {
        if (!backgroundServiceRunning || smsMessageData == null) return
        Intent(this, SmsService::class.java).also {
            it.action = SmsService.SEND_SMS
            it.putExtra(SmsService.SMS_DATA, smsMessageData)
            startService(it)
        }
    }

    private fun stopBackgroundService() {
        if (!backgroundServiceRunning) return
        Intent(this, SmsService::class.java).also {
            baseContext.stopService(it)
            backgroundServiceRunning = false

            handler.removeCallbacksAndMessages(null)
        }
    }

    private fun startBackgroundService() {
        if (backgroundServiceRunning) return
        Intent(this, SmsService::class.java).also {
            baseContext.startService(it)
            backgroundServiceRunning = true

            handler.postDelayed({
                stopBackgroundService()
            }, stopDelay)
        }
    }

    private fun startSelf(extras: Bundle?) {
        // Register the SMS receiver to process all incoming SMS
        smsReceiver = SmsReceiver()
        baseContext.registerReceiver(
            smsReceiver,
            IntentFilter(Telephony.Sms.Intents.SMS_RECEIVED_ACTION),
            RECEIVER_EXPORTED,
        )

        // Register receiver to track SMS send results
        smsSentReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val phoneNumber = intent?.getStringExtra(SmsSender.EXTRA_PHONE_NUMBER) ?: "unknown"
                val part = intent?.getIntExtra("part", -1) ?: -1
                val partInfo = if (part >= 0) " (part $part)" else ""

                when (resultCode) {
                    Activity.RESULT_OK -> {
                        Log.i(TAG, "SMS SENT OK to $phoneNumber$partInfo")
                    }
                    SmsManager.RESULT_ERROR_GENERIC_FAILURE -> {
                        Log.e(TAG, "SMS FAILED (generic) to $phoneNumber$partInfo")
                    }
                    SmsManager.RESULT_ERROR_NO_SERVICE -> {
                        Log.e(TAG, "SMS FAILED (no service) to $phoneNumber$partInfo")
                    }
                    SmsManager.RESULT_ERROR_NULL_PDU -> {
                        Log.e(TAG, "SMS FAILED (null PDU) to $phoneNumber$partInfo")
                    }
                    SmsManager.RESULT_ERROR_RADIO_OFF -> {
                        Log.e(TAG, "SMS FAILED (radio off) to $phoneNumber$partInfo")
                    }
                    else -> {
                        Log.e(TAG, "SMS FAILED (code: $resultCode) to $phoneNumber$partInfo")
                    }
                }
            }
        }
        baseContext.registerReceiver(
            smsSentReceiver,
            IntentFilter(SmsSender.SMS_SENT_ACTION),
            RECEIVER_NOT_EXPORTED,
        )

        // Resume any active forwarding sessions from database
        serviceScope.launch {
            resumeActiveForwardingSessions()
        }

        // Start observing settings changes for SecurityManager
        securityManager.startObservingSettings(serviceScope)

        // Observe auth request timeout setting
        serviceScope.launch {
            settingsRepository.authRequestTimeoutMinutes.collect { value ->
                authRequestTimeoutMinutes = value
            }
        }

        Log.i(TAG, "MasterService::startingForegroundService")
        isRunning = true
        Toast.makeText(this, "Starting Foreground Service", Toast.LENGTH_SHORT).show()
        startForeground(CODE_FOREGROUND_SERVICE, buildForegroundNotification())
    }

    private suspend fun resumeActiveForwardingSessions() {
        // Check for any active sessions that haven't expired
        val now = System.currentTimeMillis()
        val activeSessions = sessionRepository.getActiveSessionsList()

        activeSessions.forEach { session ->
            if (session.expiresAt > now) {
                val remainingMinutes = ((session.expiresAt - now) / 60000).toInt()
                Log.i(TAG, "Resuming forwarding session for ${session.devicePhoneNumber}, $remainingMinutes min remaining")

                // Set up timeout handler for remaining time
                val timeoutRunnable = Runnable {
                    serviceScope.launch {
                        sessionRepository.endSession(session.id, "TIMEOUT")
                        notificationManager.cancelForwardingNotification()
                        sessionHandlers.remove(session.devicePhoneNumber)
                    }
                }
                sessionHandlers[session.devicePhoneNumber] = timeoutRunnable
                handler.postDelayed(timeoutRunnable, session.expiresAt - now)

                // Show notification
                notificationManager.showForwardingActiveNotification(
                    session.devicePhoneNumber,
                    remainingMinutes,
                )
            } else {
                // Session has expired while app was closed
                Log.i(TAG, "Ending expired session for ${session.devicePhoneNumber}")
                sessionRepository.endSession(session.id, "EXPIRED")
            }
        }
    }
}
