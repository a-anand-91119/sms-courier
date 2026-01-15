package dev.notyouraverage.smscourier.services

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import dev.notyouraverage.smscourier.activities.MainActivity
import dev.notyouraverage.smscourier.constants.Constants.CODE_FOREGROUND_SERVICE
import dev.notyouraverage.smscourier.receivers.ServiceNotificationReceiver
import dev.notyouraverage.smscourier.services.foreground.MasterService
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MasterServiceTest {

    private lateinit var service: MasterService
    private lateinit var context: Context

    @Before
    fun setup() {
        val serviceController = Robolectric.buildService(MasterService::class.java)
        service = serviceController.get()
        context = service.applicationContext
    }

    @Test
    fun `notification channel is created on service onCreate`() {
        // When service is created
        service.onCreate()

        // Then notification channel should be created
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        val channel = notificationManager.getNotificationChannel("ListeningNotificationChannel")

        assertNotNull("Service Status channel should be created", channel)
        assertEquals("Service Status", channel.name)
        assertEquals(NotificationManager.IMPORTANCE_LOW, channel.importance)
        assertFalse("Badge should be disabled", channel.canShowBadge())
    }

    @Test
    fun `foreground notification has content intent to MainActivity`() {
        // Given service is created
        service.onCreate()

        // When notification is built (via reflection to access private method)
        val method = MasterService::class.java.getDeclaredMethod("buildForegroundNotification")
        method.isAccessible = true
        val notification = method.invoke(service) as android.app.Notification

        // Then notification should have content intent
        assertNotNull("Notification should have content intent", notification.contentIntent)

        // Verify the intent points to MainActivity
        val shadowPendingIntent = shadowOf(notification.contentIntent)
        val intent = shadowPendingIntent.savedIntent
        assertEquals(MainActivity::class.java.name, intent.component?.className)

        // Verify intent flags
        assertTrue(
            "Should have FLAG_ACTIVITY_NEW_TASK",
            (intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK) != 0,
        )
        assertTrue(
            "Should have FLAG_ACTIVITY_CLEAR_TOP",
            (intent.flags and Intent.FLAG_ACTIVITY_CLEAR_TOP) != 0,
        )
    }

    @Test
    fun `foreground notification has delete intent`() {
        // Given service is created
        service.onCreate()

        // When notification is built
        val method = MasterService::class.java.getDeclaredMethod("buildForegroundNotification")
        method.isAccessible = true
        val notification = method.invoke(service) as android.app.Notification

        // Then notification should have delete intent
        assertNotNull("Notification should have delete intent", notification.deleteIntent)

        // Verify the intent points to ServiceNotificationReceiver
        val shadowPendingIntent = shadowOf(notification.deleteIntent)
        val intent = shadowPendingIntent.savedIntent
        assertEquals(
            ServiceNotificationReceiver.ACTION_NOTIFICATION_DISMISSED,
            intent.action,
        )
    }

    @Test
    fun `foreground notification has correct properties`() {
        // Given service is created
        service.onCreate()

        // When notification is built
        val method = MasterService::class.java.getDeclaredMethod("buildForegroundNotification")
        method.isAccessible = true
        val notification = method.invoke(service) as android.app.Notification

        // Then notification should have correct properties
        val shadowNotification = shadowOf(notification)

        assertEquals("SMS Courier", shadowNotification.contentTitle)
        assertEquals("SMS Courier is running", shadowNotification.contentText)

        // Verify ongoing flag (non-dismissible)
        assertTrue(
            "Notification should be ongoing",
            (notification.flags and android.app.Notification.FLAG_ONGOING_EVENT) != 0,
        )

        // Verify category
        assertEquals(NotificationCompat.CATEGORY_SERVICE, notification.category)
    }

    @Test
    fun `recreate notification posts notification when service is running`() {
        // Given service is created and running
        service.onCreate()
        val startIntent = Intent(context, MasterService::class.java).apply {
            action = MasterService.START_SELF
        }
        service.onStartCommand(startIntent, 0, 1)

        // When recreate notification is called
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        val recreateIntent = Intent(context, MasterService::class.java).apply {
            action = MasterService.RECREATE_NOTIFICATION
        }
        service.onStartCommand(recreateIntent, 0, 2)

        // Then notification should be posted
        val shadowNotificationManager = shadowOf(notificationManager)
        val notification = shadowNotificationManager.getNotification(CODE_FOREGROUND_SERVICE)
        assertNotNull("Notification should be recreated", notification)
    }

    @Test
    fun `pending intent flags are correct for content intent`() {
        // Given service is created
        service.onCreate()

        // When notification is built
        val method = MasterService::class.java.getDeclaredMethod("buildForegroundNotification")
        method.isAccessible = true
        val notification = method.invoke(service) as android.app.Notification

        // Then pending intent should have correct flags
        val shadowPendingIntent = shadowOf(notification.contentIntent)
        val flags = shadowPendingIntent.flags

        assertTrue(
            "Should have FLAG_IMMUTABLE",
            (flags and PendingIntent.FLAG_IMMUTABLE) != 0,
        )
        assertTrue(
            "Should have FLAG_UPDATE_CURRENT",
            (flags and PendingIntent.FLAG_UPDATE_CURRENT) != 0,
        )
    }

    @Test
    fun `pending intent flags are correct for delete intent`() {
        // Given service is created
        service.onCreate()

        // When notification is built
        val method = MasterService::class.java.getDeclaredMethod("buildForegroundNotification")
        method.isAccessible = true
        val notification = method.invoke(service) as android.app.Notification

        // Then pending intent should have correct flags
        val shadowPendingIntent = shadowOf(notification.deleteIntent)
        val flags = shadowPendingIntent.flags

        assertTrue(
            "Should have FLAG_IMMUTABLE",
            (flags and PendingIntent.FLAG_IMMUTABLE) != 0,
        )
        assertTrue(
            "Should have FLAG_UPDATE_CURRENT",
            (flags and PendingIntent.FLAG_UPDATE_CURRENT) != 0,
        )
    }
}
