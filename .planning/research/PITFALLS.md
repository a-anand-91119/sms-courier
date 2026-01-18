# Domain Pitfalls: Android Settings Screen

**Domain:** Android app settings with foreground service, security configuration, and boot receiver
**Project:** SMS Courier
**Researched:** 2026-01-18
**Confidence:** HIGH (verified via official Android documentation and codebase analysis)

## Critical Pitfalls

Mistakes that cause rewrites or major issues.

### Pitfall 1: Notification Channel Settings Cannot Be Changed Programmatically

**What goes wrong:** Developer tries to update notification channel importance, sound, or vibration settings based on user preferences in the app. The changes silently fail.

**Why it happens:** Android locks notification channel behavior after creation. From the [official documentation](https://developer.android.com/develop/ui/views/notifications/channels): "After you create a notification channel, you cannot change the notification channel's visual and auditory behaviors programmatically. Only the user can change the channel behaviors from the system settings."

**Consequences:**
- Users complain settings don't work
- App appears broken
- Cannot fix without uninstall/reinstall which loses user data

**Prevention:**
1. Design notification channels with final settings from the start
2. For "notification persistence" toggle: create TWO channels (persistent and non-persistent) and switch which one you use
3. Never try to modify channel settings after creation
4. If you need to change channel behavior, create a NEW channel with a different ID

**Detection:**
- Testing notification settings doesn't produce expected behavior
- Settings appear to save but notifications remain unchanged

**Phase to address:** Phase 1 (Data Layer) - Design channel strategy before implementation

---

### Pitfall 2: BOOT_COMPLETED Receiver Not Working

**What goes wrong:** Auto-start on boot feature doesn't work. Service doesn't start after device restart.

**Why it happens:** Multiple causes based on [community reports](https://codingtechroom.com/question/-broadcastreceiver-boot-complete-issues):
1. Missing `RECEIVE_BOOT_COMPLETED` permission
2. App never launched (stopped state on Android 3.1+)
3. Device not unlocked (Direct Boot restriction)
4. Vendor-specific auto-launch restrictions (Xiaomi, Huawei, Samsung)
5. Battery optimization killing the receiver
6. Trying to start foreground service from background (Android 12+)

**Consequences:**
- Core feature completely broken
- Users must manually open app after every reboot
- Support tickets and bad reviews

**Prevention:**
1. Add permission: `<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />`
2. Declare receiver with `android:enabled="true"` and `android:exported="true"`
3. Use `ACTION_LOCKED_BOOT_COMPLETED` for Direct Boot compatibility
4. Handle vendor auto-start screens (consider [AutoStarter](https://github.com/AniketSharma/AutoStarter) library)
5. Guide users to disable battery optimization for the app
6. Use WorkManager instead of direct service start for Android 12+ compatibility

**Detection:**
- Boot receiver works on emulator but fails on real devices
- Works on Pixel but not on Samsung/Xiaomi/Huawei
- Works when phone is unlocked at restart but not when locked

**Phase to address:** Phase 3 (Auto-Start Feature) - Critical path item

---

### Pitfall 3: Service Doesn't See Updated Settings

**What goes wrong:** User changes settings (e.g., security timeout from 15 to 30 minutes), but MasterService continues using old values.

**Why it happens:** SMS Courier's `SecurityManager` and `MasterService` have hardcoded constants:
```kotlin
// From SecurityManager.kt lines 19-21:
const val MAX_FAILED_ATTEMPTS = 5
const val LOCKOUT_DURATION_MS = 15 * 60 * 1000L // 15 minutes
private const val CHALLENGE_EXPIRY_MS = 2 * 60 * 1000L // 2 minutes
```
Service reads these at startup but has no mechanism to receive updates.

**Consequences:**
- Settings screen shows one value, service uses another
- User confusion and trust issues
- Security settings particularly problematic (user thinks they're protected by longer lockout)

**Prevention:**
1. Use DataStore with Flow/StateFlow that service observes
2. Inject settings repository into SecurityManager instead of companion object constants
3. For long-running services: collect Flow updates or use broadcast/event mechanism
4. Consider service restart for critical security changes
5. Make SecurityManager instance-based with injected settings, not companion object constants

**Detection:**
- Unit tests pass with hardcoded values but integration tests fail with configurable values
- QA reports that changing settings has no effect until app restart

**Phase to address:** Phase 1 (Data Layer) + Phase 2 (Settings Screen) - Architecture decision

---

### Pitfall 4: DataStore Lacks Built-in Encryption

**What goes wrong:** Security-sensitive settings (passwords, API keys, auth tokens) stored in plain text. Rooted devices can read settings file.

**Why it happens:** Per [droidcon migration guide](https://www.droidcon.com/2025/12/16/goodbye-encryptedsharedpreferences-a-2026-migration-guide/): "One major issue with DataStore is the lack of built-in encryption. Google has been completely silent on this issue despite years of developer concerns."

**Consequences:**
- Security vulnerability on rooted devices
- Compliance issues (GDPR, SOC2)
- Potential credential theft

**Prevention:**
1. Use DataStore + Tink library for sensitive data
2. Use third-party [encrypted-datastore](https://github.com/osipxd/encrypted-datastore) library
3. Separate sensitive settings from non-sensitive ones
4. For SMS Courier: security timeouts are not sensitive; if storing password-related config, encrypt it

**Detection:**
- Security audit finds plaintext credentials in `/data/data/package/files/datastore/`
- Static analysis tools flag sensitive data storage

**Phase to address:** Phase 1 (Data Layer) if storing sensitive settings

---

## Moderate Pitfalls

Mistakes that cause delays or technical debt.

### Pitfall 5: SharedPreferences Migration Not Deleted

**What goes wrong:** App migrates from SharedPreferences to DataStore but old XML file persists, causing confusion and potential data inconsistencies.

**Why it happens:** Per [official documentation](https://developer.android.com/reference/kotlin/androidx/datastore/migrations/SharedPreferencesMigration): "SharedPreferencesMigration does not automatically delete the old XML file (to prevent data loss if migration fails)."

**Consequences:**
- Duplicate data storage
- Confusion about source of truth
- Wasted storage space

**Prevention:**
```kotlin
// After verifying migration success:
context.deleteSharedPreferences("old_prefs_name")
```
1. Track migration via analytics
2. Only delete after confirming migration succeeded
3. Add migration version tracking

**Detection:**
- Both DataStore and SharedPreferences files exist
- Old settings reappear after being changed

**Phase to address:** Phase 1 (Data Layer) - Migration strategy

---

### Pitfall 6: ViewModel Scoping in Jetpack Compose Settings

**What goes wrong:** Same ViewModel instance shared across settings screens, causing state pollution between screens.

**Why it happens:** Per [Android Developers](https://developer.android.com/develop/ui/compose/migrate/other-considerations): "ViewModel elements follow View-lifecycle scopes. The same instance of a ViewModel type is used in all composables unless the composable is a destination of the navigation graph."

**Consequences:**
- State from one screen bleeds into another
- Difficulty isolating and testing settings sections
- Unexpected behavior on navigation

**Prevention:**
1. Make each settings subscreens a Navigation destination
2. Or use separate ViewModel types for distinct settings categories
3. Don't pass ViewModel instances to child composables; pass data and callbacks instead
4. Use `viewModel()` at screen-level only

**Detection:**
- State unexpectedly persists between screens
- Values from one settings section appear in another

**Phase to address:** Phase 2 (Settings Screen) - UI architecture

---

### Pitfall 7: Settings Input Validation Bypass

**What goes wrong:** User enters invalid value (e.g., lockout duration of 0 minutes, negative rate limit), app crashes or behaves unexpectedly.

**Why it happens:** Per [OWASP](https://owasp.org/www-project-mobile-top-10/2023-risks/m4-insufficient-input-output-validation): "Insufficient input validation is one of the most common security problems affecting applications."

For SMS Courier settings:
- Default forwarding duration: 0 or negative causes infinite loop or crash
- Security timeouts: 0 disables security entirely
- Pairing rate limits: 0 allows unlimited pairing requests (DoS vector)

**Consequences:**
- App crashes
- Security bypass
- Unexpected behavior

**Prevention:**
1. Validate in ViewModel before saving
2. Use allowlist validation (define what IS valid)
3. Provide sensible min/max bounds for numeric settings
4. Show clear error messages for invalid input
5. Never trust UI-only validation; validate before persisting

**Detection:**
- Crash reports with NumberFormatException or ArithmeticException
- Security testing reveals bypass vectors

**Phase to address:** Phase 2 (Settings Screen) - Input handling

---

### Pitfall 8: Foreground Service Notification Priority Too Low

**What goes wrong:** User complains about foreground service notification or system warns about "app using foreground service."

**Why it happens:** Per [official documentation](https://developer.android.com/develop/background-work/services/fgs/launch): "The status bar notification must use a priority of PRIORITY_LOW or higher. If your app attempts to use a notification that has a lower priority, the system adds a message to the notification drawer."

Current SMS Courier code (line 191 in MasterService.kt):
```kotlin
.setPriority(NotificationCompat.PRIORITY_LOW)
```
This is the minimum. Going lower triggers system warnings.

**Consequences:**
- System-added warnings about foreground service
- User confusion
- Potential Play Store policy issues

**Prevention:**
1. Keep priority at PRIORITY_LOW or higher
2. For notification persistence toggle: use ongoing vs non-ongoing, not priority
3. If user wants to "hide" notification, use `setShowBadge(false)` and low importance channel

**Detection:**
- Extra system text appears in notification drawer
- User reports "app is using battery" warnings

**Phase to address:** Phase 2 (Settings Screen) - When implementing notification preferences

---

## Minor Pitfalls

Mistakes that cause annoyance but are fixable.

### Pitfall 9: Android 12+ Background Start Restrictions

**What goes wrong:** Trying to start foreground service from BOOT_COMPLETED receiver throws ForegroundServiceStartNotAllowedException.

**Why it happens:** Per [Android documentation](https://developer.android.com/about/versions/12/behavior-changes-12#foreground-service-launch-restrictions): "Apps that target Android 12 or higher are not allowed to start a foreground service while the app is in the background, with a few specific exceptions."

**Consequences:**
- Auto-start feature broken on Android 12+
- Crash on boot

**Prevention:**
1. Use WorkManager expedited work instead of direct service start
2. Leverage exemptions (high-priority FCM, alarm, etc.)
3. Request `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` permission
4. Use `ACTION_LOCKED_BOOT_COMPLETED` with appropriate handling

**Detection:**
- Works on Android 11, crashes on Android 12+
- Logs show ForegroundServiceStartNotAllowedException

**Phase to address:** Phase 3 (Auto-Start Feature)

---

### Pitfall 10: Settings Screen Stretching on Large Screens

**What goes wrong:** Settings list items stretch to full screen width on tablets, looking unprofessional.

**Why it happens:** Per [Android design guidelines](https://developer.android.com/design/ui/mobile/guides/patterns/settings): "Don't allow single pane settings items to stretch to full width, instead set a max-width or add supplementary content."

**Consequences:**
- Poor UX on tablets/foldables
- Unprofessional appearance

**Prevention:**
1. Set max-width on settings container (typically 600-840dp)
2. Use list-detail layout on large screens
3. Consider using Jetpack Compose adaptive layouts

**Detection:**
- Run on tablet or foldable device
- Visual review on different screen sizes

**Phase to address:** Phase 2 (Settings Screen) - Polish/UX

---

### Pitfall 11: Theme Settings Not Applied Without Restart

**What goes wrong:** User changes theme (light/dark/system) but app doesn't update immediately.

**Why it happens:** Theme changes require activity recreation or explicit state management.

**Consequences:**
- User must restart app to see theme change
- Poor UX compared to competitors

**Prevention:**
1. Use `setContent` with key based on theme preference
2. Use `AppCompatDelegate.setDefaultNightMode()` with automatic recreation
3. Collect theme Flow in MainActivity and recreate on change

**Detection:**
- QA reports theme changes require app restart

**Phase to address:** Phase 2 (Settings Screen) - Theme implementation

---

### Pitfall 12: Deleting Notification Channel to Reset Settings

**What goes wrong:** Developer tries to delete and recreate notification channel to reset settings to defaults. System shows "X deleted channels" spam prevention message.

**Why it happens:** Per [official documentation](https://developer.android.com/develop/ui/views/notifications/channels): "The notification settings screen displays the number of deleted channels, as a spam prevention mechanism."

**Consequences:**
- User sees deleted channel count
- App appears spammy
- Trust issues

**Prevention:**
1. Never delete channels to reset settings
2. Create new channels with different IDs if needed
3. Direct users to system settings to reset channel preferences

**Detection:**
- "X deleted channels" appears in notification settings

**Phase to address:** Phase 2 (Settings Screen) - Notification preferences

---

## Phase-Specific Warnings

| Phase | Likely Pitfall | Mitigation |
|-------|---------------|------------|
| Phase 1: Data Layer | DataStore migration issues, encryption gaps | Design migration strategy, evaluate Tink for sensitive data |
| Phase 2: Settings Screen | Service not seeing updates, channel settings locked, input validation | Use Flow/StateFlow, design channel strategy, implement validation |
| Phase 3: Auto-Start | BOOT_COMPLETED failures, Android 12+ restrictions | Use WorkManager, handle vendor quirks, test on multiple OEMs |
| Phase 4: Integration | Settings propagation to service, notification channel conflicts | Design observer pattern, thorough integration testing |

## SMS Courier-Specific Concerns

Based on codebase analysis:

1. **SecurityManager hardcoded values** (lines 19-21): Must refactor to inject configurable settings
2. **MasterService session timeout** (line 49): Hardcoded `stopDelay: Long = 5 * 60 * 1000` should be configurable
3. **Challenge expiry** (line 21): Security setting that users may want to configure
4. **No BOOT_COMPLETED receiver**: Must add receiver with proper vendor compatibility handling
5. **Single notification channel strategy**: Current design with NOTIFICATION_CHANNEL_GENERAL needs expansion for persistence toggle

## Sources

**Official Documentation:**
- [Android Notification Channels](https://developer.android.com/develop/ui/views/notifications/channels)
- [Android Settings Design Guidelines](https://developer.android.com/design/ui/mobile/guides/patterns/settings)
- [Android Foreground Service Requirements](https://developer.android.com/about/versions/14/changes/fgs-types-required)
- [Android DataStore Documentation](https://developer.android.com/topic/libraries/architecture/datastore)
- [SharedPreferencesMigration API](https://developer.android.com/reference/kotlin/androidx/datastore/migrations/SharedPreferencesMigration)

**Migration and Encryption:**
- [DataStore + Tink Migration Guide (2026)](https://www.droidcon.com/2025/12/16/goodbye-encryptedsharedpreferences-a-2026-migration-guide/)
- [encrypted-datastore library](https://github.com/osipxd/encrypted-datastore)
- [DataStore vs SharedPreferences 2025](https://www.atipik.ch/en/blog/android-jetpack-datastore-vs-sharedpreferences)

**BOOT_COMPLETED and Service Issues:**
- [BroadcastReceiver Boot Complete Issues](https://codingtechroom.com/question/-broadcastreceiver-boot-complete-issues)
- [Foreground Services Android 14](https://medium.com/@domen.lanisnik/guide-to-foreground-services-on-android-9d0127dc8f9a)

**Jetpack Compose and ViewModel:**
- [ViewModel Scoping in Compose](https://developer.android.com/develop/ui/compose/migrate/other-considerations)
- [DataStore with Jetpack Compose](https://proandroiddev.com/demystifying-datastore-a-comprehensive-guide-to-using-datastore-with-jetpack-compose-d89c813232d7)

**Input Validation:**
- [OWASP Mobile Input Validation](https://owasp.org/www-project-mobile-top-10/2023-risks/m4-insufficient-input-output-validation)
- [Android Security Checklist](https://developer.android.com/training/articles/security-tips)
