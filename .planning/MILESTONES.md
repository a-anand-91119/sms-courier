# Project Milestones: SMS Courier

## v1.1 Feature Improvements (Shipped: 2026-01-15)

**Delivered:** Bidirectional pairing architecture, improved pending state UX with rate limiting and contextual menus, and UI polish for production readiness.

**Phases completed:** 5-7 (6 main plans + 3 fix plans)

**Key accomplishments:**
- Bidirectional pairing: Same phone number can be SOURCE and TARGET simultaneously (composite PK architecture)
- Rate-limited resend: Max 5 attempts with 1-minute cooldown for pairing requests
- Contextual UX: Dropdown menus on long-press with status-aware actions
- Notification deep-linking: Approve button navigates to pairing screen
- Smart UNPAIR: Only sends SMS for approved devices (saves SMS charges)
- Service toggle loading state with polling mechanism

**Stats:**
- 30+ files created/modified
- Database migrations 3→4→5
- 228 unit tests passing
- 1 day from start to ship

**Git range:** Phases 5-7 commits

**What's next:** Continue with v1.0 Play Store Launch (Phases 1-4)

---

_See [milestones/v1.1-ROADMAP.md](milestones/v1.1-ROADMAP.md) for full milestone archive._
