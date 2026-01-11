package dev.notyouraverage.otpcourier.activities

import android.Manifest
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.core.app.ActivityCompat
import androidx.navigation.compose.rememberNavController
import dev.notyouraverage.otpcourier.navigation.SmsCourierNavGraph
import dev.notyouraverage.otpcourier.ui.theme.OTPCourierTheme

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
            OTPCourierTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    val navController = rememberNavController()
                    SmsCourierNavGraph(navController = navController)
                }
            }
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
