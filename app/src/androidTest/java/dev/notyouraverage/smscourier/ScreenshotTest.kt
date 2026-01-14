package dev.notyouraverage.smscourier

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import dev.notyouraverage.smscourier.activities.MainActivity
import org.junit.Before
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import tools.fastlane.screengrab.Screengrab
import tools.fastlane.screengrab.UiAutomatorScreenshotStrategy
import tools.fastlane.screengrab.locale.LocaleTestRule

@RunWith(AndroidJUnit4::class)
class ScreenshotTest {

    companion object {
        @ClassRule
        @JvmField
        val localeTestRule = LocaleTestRule()
    }

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private lateinit var device: UiDevice

    @Before
    fun setUp() {
        // Initialize UiDevice for more control
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        // Set screenshot strategy to use UiAutomator (better for Material UI)
        Screengrab.setDefaultScreenshotStrategy(UiAutomatorScreenshotStrategy())

        // Ensure screen is on and unlocked
        if (!device.isScreenOn) {
            device.wakeUp()
        }
        device.pressHome()

        // Wait for app to be ready
        composeTestRule.waitForIdle()
    }

    @Test
    fun captureScreenshots() {
        // Give the app time to fully render
        Thread.sleep(2000)

        // Screenshot 1: Home Screen
        // This is the main dashboard showing app status
        Screengrab.screenshot("01_home_screen")

        // Wait between screenshots
        Thread.sleep(1000)

        // Screenshot 2: Paired Devices Screen
        // Navigate by looking for common UI elements
        // Try multiple strategies to find and click
        navigateToScreenSafely("Paired Devices", "View Devices", "Devices")
        Thread.sleep(1000)
        Screengrab.screenshot("02_paired_devices")

        // Go back to home
        device.pressBack()
        Thread.sleep(500)

        // Screenshot 3: Pairing Requests Screen
        navigateToScreenSafely("Pairing Requests", "Requests")
        Thread.sleep(1000)
        Screengrab.screenshot("03_pairing_requests")

        // Go back to home
        device.pressBack()
        Thread.sleep(500)

        // Screenshot 4: Add Device Screen
        navigateToScreenSafely("Add Device", "Add", "New Device")
        Thread.sleep(1000)
        Screengrab.screenshot("04_add_device")

        // Go back to home
        device.pressBack()
        Thread.sleep(500)

        // Screenshot 5: Forwarding Control Screen
        navigateToScreenSafely("Forwarding Control", "Forwarding", "Forward")
        Thread.sleep(1000)
        Screengrab.screenshot("05_forwarding")
    }

    /**
     * Tries multiple text options to navigate to a screen
     * Silently fails if none of the options work
     */
    private fun navigateToScreenSafely(vararg textOptions: String) {
        for (text in textOptions) {
            try {
                // Try to find and click the element
                composeTestRule.onNodeWithText(text, useUnmergedTree = true).performClick()
                composeTestRule.waitForIdle()
                return // Success, exit
            } catch (e: Exception) {
                // Try next option
                continue
            }
        }
        // If all options fail, just continue (screen might not be accessible without data)
    }
}
