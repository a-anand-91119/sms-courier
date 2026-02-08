package dev.notyouraverage.smscourier.activities

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.core.app.ActivityCompat
import androidx.navigation.compose.rememberNavController
import dev.notyouraverage.smscourier.data.settings.AppTheme
import dev.notyouraverage.smscourier.navigation.Screen
import dev.notyouraverage.smscourier.navigation.SmsCourierNavGraph
import dev.notyouraverage.smscourier.receivers.PairingActionReceiver
import dev.notyouraverage.smscourier.repository.SettingsRepository
import dev.notyouraverage.smscourier.ui.theme.smscourierTheme
import dev.notyouraverage.smscourier.utils.WorkManagerHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    companion object {
        private val TAG by lazy { MainActivity::class.java.simpleName }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i(TAG, "MainActivity::onCreate")
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestSmsPermission()

        // Schedule cleanup on app launch if enabled
        CoroutineScope(Dispatchers.IO).launch {
            val settingsRepository = SettingsRepository(applicationContext)
            val autoCleanupEnabled = settingsRepository.autoCleanupEnabled.first()
            val retentionDays = settingsRepository.historyRetentionDays.first()

            // Only schedule if auto-cleanup is enabled AND retention is not Forever
            if (autoCleanupEnabled && retentionDays != 0) {
                WorkManagerHelper.scheduleCleanup(applicationContext)
            }
        }

        // Determine start destination from deep link intent
        val startDestination = getStartDestinationFromIntent(intent)

        setContent {
            val settingsRepository = remember { SettingsRepository(applicationContext) }
            val themeSetting by settingsRepository.theme.collectAsState(initial = AppTheme.SYSTEM)

            val darkTheme = when (themeSetting) {
                AppTheme.LIGHT -> false
                AppTheme.DARK -> true
                AppTheme.SYSTEM -> isSystemInDarkTheme()
            }

            smscourierTheme(darkTheme = darkTheme) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    val navController = rememberNavController()
                    SmsCourierNavGraph(
                        navController = navController,
                        startDestination = startDestination,
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Log.i(TAG, "MainActivity::onNewIntent")
        // For new intents while activity is running, we would need to navigate
        // This is handled by FLAG_ACTIVITY_CLEAR_TOP which recreates the activity
    }

    private fun getStartDestinationFromIntent(intent: Intent?): String {
        val navigateTo = intent?.getStringExtra(PairingActionReceiver.EXTRA_NAVIGATE_TO)
        return when (navigateTo) {
            PairingActionReceiver.NAVIGATE_TO_PAIRING_REQUESTS -> {
                Log.i(TAG, "Deep link: navigating to PairingRequests")
                Screen.PairingRequests.route
            }
            else -> Screen.Home.route
        }
    }

    override fun onDestroy() {
        Log.i(TAG, "MainActivity::onDestroy")
        super.onDestroy()
    }

    private fun requestSmsPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.RECEIVE_SMS,
                Manifest.permission.SEND_SMS,
                Manifest.permission.POST_NOTIFICATIONS,
            ),
            0,
        )
    }
}
