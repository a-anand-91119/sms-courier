---
created: 2026-01-18T17:30
title: Sync sent messages back to original device
area: feature-backlog
files: []
priority: backlog
jira: SC-18
---

## Problem

Currently SMS Courier only forwards incoming messages from target to source device. Sent messages from the source device are not synced back to the target device, making it difficult to maintain a complete conversation history on the original device.

## Solution

TBD - Options to explore:
- Bidirectional message sync protocol
- Reply-to-forward feature (send SMS through target device)
- Conversation threading across devices
- Sync on-demand vs real-time
