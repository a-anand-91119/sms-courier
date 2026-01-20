---
created: 2026-01-20T10:30
title: Scheduled and recurring forwarding sessions
area: feature-backlog
files: []
priority: backlog
jira: SC-54
---

## Problem

Currently users must manually start forwarding sessions each time they need them. Common use cases require predictable, automated forwarding:

- Forward work phone SMS during business hours (9am-5pm weekdays)
- Extended forwarding while traveling (vacation mode)
- Daily forwarding windows for specific purposes

Manual session management is tedious and error-prone for regular use.

## Solution

TBD - Options to explore:

**Schedule Types:**
- One-time: "Forward tomorrow 2pm-6pm"
- Recurring: "Forward Mon-Fri 9am-5pm"
- Duration-based: "Forward for next 7 days"

**Data Model:**
- New `ForwardingSchedule` entity with paired device, start time, end time, recurrence pattern
- WorkManager for reliable scheduled execution
- Handle timezone changes and DST

**UI:**
- Schedule creation wizard in ForwardingControlScreen
- Calendar view showing upcoming scheduled sessions
- Quick presets: "Work hours", "24 hours", "This week"
- Edit/delete existing schedules

**Edge Cases:**
- Overlapping schedules
- Device offline when schedule starts
- Session start requires password - handle securely
- Notification when scheduled session starts/ends

**Technical:**
- WorkManager for background scheduling (survives reboots)
- Consider AlarmManager for precise timing
- Battery optimization handling
