---
status: complete
phase: 14-advanced-settings-service-integration
source: [14-01-SUMMARY.md, 14-02-SUMMARY.md, 14-03-SUMMARY.md]
started: 2026-01-19T17:30:00Z
updated: 2026-01-19T18:00:00Z
---

## Current Test

[testing complete]

## Tests

### 1. Access Advanced Section
expected: In Settings screen, scroll below Preferences section. Find "Advanced" header with warning icon. Tap to expand. Section shows 6 numeric input fields grouped into "Security" and "Pairing" sub-sections.
result: pass

### 2. Collapse/Expand Advanced Section
expected: With Advanced section expanded, tap the header again. Section collapses. Tap again to re-expand.
result: pass

### 3. Configure Lockout Duration
expected: In Security sub-section, find "Lockout duration" field showing "15" (default). Change to another value like 30. Field accepts input and shows new value.
result: pass

### 4. Configure Max Failed Attempts
expected: Find "Max failed attempts" field showing "5" (default). Change to 3. Field accepts input.
result: pass

### 5. Configure Challenge Expiry
expected: Find "Challenge expiry" field showing "2" (default). Change to 5. Field accepts input.
result: pass

### 6. Configure Max Pairing Resend Attempts
expected: In Pairing sub-section, find "Max pairing resend attempts" field showing "5" (default). Change to 3. Field accepts input.
result: pass

### 7. Configure Pairing Resend Cooldown
expected: Find "Pairing resend cooldown" field showing "1" (default). Change to 2. Field accepts input.
result: pass

### 8. Configure Auth Request Timeout
expected: Find "Auth request timeout" field showing "5" (default). Change to 10. Field accepts input.
result: pass

### 9. Invalid Value Error
expected: Enter a value outside the valid range for any field (e.g., enter 100 for "Max failed attempts" which has range 1-10). A snackbar error message appears indicating invalid value.
result: pass

### 10. Advanced Settings Persist
expected: Make changes to at least 2 advanced settings. Force close app completely. Reopen app, go to Settings, expand Advanced section. All changed values are still saved.
result: pass

## Summary

total: 10
passed: 10
issues: 0
pending: 0
skipped: 0

## Gaps

[none yet]
