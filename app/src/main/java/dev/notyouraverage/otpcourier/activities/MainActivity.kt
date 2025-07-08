package dev.notyouraverage.otpcourier.activities

import android.Manifest
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import dev.notyouraverage.otpcourier.composables.MainScreen
import dev.notyouraverage.otpcourier.managers.DataStoreManager

class MainActivity : ComponentActivity() {
    companion object {
        private val TAG by lazy { MainActivity::class.java.simpleName }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i(TAG, "MainActivity::onCreate")
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestSmsPermission()
        setContent {
            MainScreen()
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
                Manifest.permission.READ_SMS,
                Manifest.permission.POST_NOTIFICATIONS,
            ),
            0,
        )
    }
}
