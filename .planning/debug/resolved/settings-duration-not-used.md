---
status: resolved
trigger: "settings-duration-not-used: Settings default session duration not used when starting forwarding"
created: 2026-02-09T00:00:00Z
updated: 2026-02-09T00:00:00Z
---

## Current Focus

hypothesis: CONFIRMED - ForwardingControlViewModel was using hardcoded default
test: Verified by reading code and confirming SettingsRepository was not injected
expecting: N/A - fix applied
next_action: Mark as resolved and archive

## Symptoms

expected: When user starts a forwarding session, the duration slider should be initialized to the value they saved in Settings (e.g., 10 minutes if they set it to 10).
actual: Duration slider always shows 5 minutes (hardcoded default) regardless of what the user saved in Settings.
errors: No error messages - just wrong default value displayed.
reproduction:
1. Go to Settings
2. Change default forwarding duration to 10 minutes (or any value other than 5)
3. Go back, navigate to start a forwarding session
4. Duration slider shows 5 minutes instead of 10
started: Discovered during UAT. May never have worked.

## Eliminated

## Evidence

- timestamp: 2026-02-09T00:01:00Z
  checked: ForwardingControlViewModel.kt initialization
  found: ForwardingUiState data class has durationMinutes = SettingsDefaults.DEFAULT_FORWARDING_DURATION (line 113), which is the hardcoded constant 5. The ViewModel does NOT inject SettingsRepository and does NOT read defaultForwardingDurationMinutes flow.
  implication: Root cause confirmed - ViewModel uses hardcoded default instead of user's saved preference

- timestamp: 2026-02-09T00:01:30Z
  checked: SettingsRepository.kt
  found: Has defaultForwardingDurationMinutes Flow<Int> (lines 30-34) that correctly reads from DataStore with fallback to SettingsDefaults.DEFAULT_FORWARDING_DURATION. Also has setDefaultForwardingDuration() method.
  implication: Settings infrastructure is correct - the problem is ForwardingControlViewModel not using it

## Resolution

root_cause: ForwardingControlViewModel initializes ForwardingUiState.durationMinutes with SettingsDefaults.DEFAULT_FORWARDING_DURATION (hardcoded 5) instead of reading the user's saved preference from SettingsRepository.defaultForwardingDurationMinutes. The SettingsRepository is not injected into ForwardingControlViewModel.
fix: Added SettingsRepository as a dependency to ForwardingControlViewModel. Added init block that loads the user's saved default duration from settingsRepository.defaultForwardingDurationMinutes.first() and updates the UI state. Updated the Factory class to accept and pass SettingsRepository. Updated NavGraph to pass settingsRepository when creating the ForwardingControlViewModel.Factory.
verification: All ForwardingControlViewModelTest tests pass (13 tests). Added new test `duration is loaded from settings on init` that verifies custom duration (15 min) is loaded from settings repository. spotlessCheck passes.
files_changed:
  - app/src/main/java/dev/notyouraverage/smscourier/viewmodels/ForwardingControlViewModel.kt
  - app/src/main/java/dev/notyouraverage/smscourier/navigation/NavGraph.kt
  - app/src/test/java/dev/notyouraverage/smscourier/viewmodels/ForwardingControlViewModelTest.kt
