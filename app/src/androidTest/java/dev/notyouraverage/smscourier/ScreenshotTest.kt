package dev.notyouraverage.smscourier

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.notyouraverage.smscourier.activities.MainActivity
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

    @Test
    fun takeScreenshots() {
        Screengrab.setDefaultScreenshotStrategy(UiAutomatorScreenshotStrategy())

        // Wait for the app to settle
        Thread.sleep(1000)

        // Screenshot 1: Home screen
        Screengrab.screenshot("01_home")

        // Screenshot 2: Paired Devices screen
        try {
            composeTestRule.onNodeWithText("Paired Devices").performClick()
            Thread.sleep(500)
            Screengrab.screenshot("02_paired_devices")
        } catch (e: Exception) {
            // Screen might not exist or be accessible
        }

        // Screenshot 3: Pairing Requests screen
        try {
            composeTestRule.onNodeWithText("Pairing Requests").performClick()
            Thread.sleep(500)
            Screengrab.screenshot("03_pairing_requests")
        } catch (e: Exception) {
            // Screen might not exist or be accessible
        }

        // Screenshot 4: Add Device screen
        try {
            composeTestRule.onNodeWithText("Add Device").performClick()
            Thread.sleep(500)
            Screengrab.screenshot("04_add_device")
        } catch (e: Exception) {
            // Screen might not exist or be accessible
        }

        // Screenshot 5: Forwarding Control screen
        try {
            composeTestRule.onNodeWithText("Forwarding").performClick()
            Thread.sleep(500)
            Screengrab.screenshot("05_forwarding_control")
        } catch (e: Exception) {
            // Screen might not exist or be accessible
        }
    }
}
