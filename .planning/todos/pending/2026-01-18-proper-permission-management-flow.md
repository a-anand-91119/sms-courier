---
created: 2026-01-18T20:00
title: Implement proper permission management flow
area: feature-backlog
files: []
priority: backlog
jira: SC-36
---

## Problem

Currently, the app handles SMS permissions but lacks a comprehensive permission management flow. Users need clear guidance when permissions are missing or revoked, especially since SMS permissions are critical for core functionality.

## Solution

Future milestone to implement:
1. Permission status screen showing all required permissions
2. Clear explanations of why each permission is needed
3. Deep links to system settings for each permission
4. Re-check permissions when returning from settings
5. Graceful degradation when permissions are missing
6. First-run permission onboarding flow
7. Handle permission revocation mid-session

Note: v0.0.63 Settings includes basic permission status display (INFO-01), but a full permission management flow should be a dedicated milestone.
