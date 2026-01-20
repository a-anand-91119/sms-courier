---
created: 2026-01-20T10:30
title: Message history and forwarding logs
area: feature-backlog
files: []
priority: backlog
---

## Problem

Currently SMS Courier has no message history or audit trail. Users cannot:
- View previously forwarded messages
- Search for specific forwarded content
- Verify delivery status of forwarded messages
- Export forwarding history for record-keeping

This limits visibility into what was forwarded and when, which is important for both personal use (finding an OTP that arrived earlier) and business use (audit compliance).

## Solution

TBD - Options to explore:

**Data Model:**
- New `ForwardedMessage` entity: sender, content (optionally stored), timestamp, session_id, delivery_status
- Consider privacy toggle: store metadata only vs full message content
- Retention policy: auto-delete after N days

**UI:**
- History screen showing forwarded messages grouped by date/session
- Search by sender number or content keywords
- Filter by date range or paired device
- Message detail view with delivery status

**Export:**
- CSV export with date range selection
- PDF report generation
- Optional: email export directly from app

**Privacy Considerations:**
- Option to disable message content storage (metadata only)
- Secure deletion of old messages
- Encrypted storage if content is saved
