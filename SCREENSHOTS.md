# Manual Screenshot Guide

Since AVD automation needs more work, here's how to create screenshots manually for the Play Store.

## Quick Steps

1. **Install the app on an emulator or device**
   ```bash
   ./gradlew installDebug
   ```

2. **Set up demo data** (optional but recommended)
   - Create a few paired devices
   - Set up a forwarding session
   - Add some session history

3. **Disable animations** (for cleaner screenshots)
   ```bash
   adb shell settings put global window_animation_scale 0
   adb shell settings put global transition_animation_scale 0
   adb shell settings put global animator_duration_scale 0
   ```

4. **Take screenshots**
   - Recommended screens:
     - Home screen (main dashboard)
     - Paired devices list
     - Pairing request/approval flow
     - Forwarding control screen
     - Session history
   - Use Android Studio's screenshot tool or:
     ```bash
     adb shell screencap -p /sdcard/screenshot.png
     adb pull /sdcard/screenshot.png ./screenshot_name.png
     ```

5. **Place screenshots in the correct folder**
   ```
   fastlane/metadata/android/en-US/images/phoneScreenshots/
   ```
   - Name them: `01.png`, `02.png`, `03.png`, etc.
   - Play Store requires: PNG or JPEG, 16:9 or 9:16 aspect ratio, 320-3840px

6. **Upload to Play Store**
   ```bash
   bundle exec fastlane upload_screenshots
   ```
   Or commit them to git and the CI will upload them with the next release.

## Screenshot Recommendations

### Required Dimensions
- **Phone**: Min 320px, Max 3840px on the long side
- **Aspect ratio**: 16:9 or 9:16
- **Format**: PNG or JPEG

### Recommended Screens to Capture
1. **Home Screen** - Shows app status, service running state
2. **Paired Devices** - List of devices (can be empty or with demo data)
3. **Pairing Flow** - Either the pairing request or approval screen
4. **Forwarding Control** - Session management screen
5. **Session History** - Past forwarding sessions

### Tips for Better Screenshots
- Use a clean device/emulator with good demo data
- Avoid showing personal information or real phone numbers
- Use consistent demo data (e.g., "+1234567890" for phone numbers)
- Pixel 6 emulator profile recommended (same as automated setup)
- Screenshot at 1080x2400 resolution for best quality

## Commit Screenshots to Git

Screenshots should be committed to version control:

```bash
git add fastlane/metadata/android/en-US/images/phoneScreenshots/
git commit -m "assets: Add Play Store screenshots"
git push
```

## Future: Automated Screenshots

The CI is configured for automated screenshots but currently disabled.
To retry automation, trigger the `screenshots` job manually in GitLab CI.
