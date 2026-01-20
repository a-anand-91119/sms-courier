---
created: 2026-01-20T10:30
title: Multi-destination forwarding (one-to-many)
area: feature-backlog
files: []
priority: backlog
---

## Problem

Currently a TARGET device can only forward to one SOURCE device at a time. Common use cases require forwarding to multiple destinations:

- Business phone forwarding to multiple team members
- Personal phone forwarding to both backup phone and email
- Family sharing: parent's work phone forwarding to both parents

## Solution

TBD - Options to explore:

**Architecture Options:**

1. **Multiple Active Sessions:**
   - Allow multiple SOURCE devices to have active sessions simultaneously
   - Each SOURCE authenticates independently
   - TARGET forwards to all active SOURCEs

2. **Forwarding Groups:**
   - Define groups of SOURCE devices
   - Start forwarding to entire group with single command
   - Group management UI

**Data Model Changes:**
- Remove single-session constraint in ForwardingSession
- Add optional group_id for grouped sessions
- Update MasterService to iterate all active sessions when forwarding

**Command Protocol:**
- Current: `SMSC START_FORWARD` starts single session
- Option A: Each SOURCE starts own session (parallel sessions)
- Option B: New command `SMSC START_FORWARD_GROUP` with group identifier

**UI Changes:**
- ForwardingControlScreen shows multiple active sessions
- Option to start/stop individual destinations or all at once
- Group management screen (create, edit, delete groups)

**Considerations:**
- SMS costs multiply with each destination
- Rate limiting to avoid carrier restrictions
- Staggered sending to avoid spam detection
