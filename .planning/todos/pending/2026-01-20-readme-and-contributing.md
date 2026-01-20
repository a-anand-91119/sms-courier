---
created: 2026-01-20T10:45
title: Create README and CONTRIBUTING documentation
area: docs
files: []
priority: backlog
jira: SC-53
---

## Problem

The repository lacks standard open-source documentation files:
- No README.md explaining what the app does, how to build it, or how to use it
- No CONTRIBUTING.md guiding potential contributors

This makes it difficult for:
- New developers to understand the project
- Users to learn about the app's capabilities
- Contributors to know how to submit changes
- Play Store reviewers to understand the project context

## Solution

**README.md should include:**
- App description and purpose (SMS forwarding between devices)
- Screenshots or demo GIF
- Features list (pairing, forwarding, encryption, etc.)
- Requirements (Android version, permissions needed)
- Build instructions (reference existing CLAUDE.md commands)
- Installation options (Play Store link when available, APK releases)
- Basic usage guide
- Security overview (bcrypt, AES-256, challenge-response)
- License information
- Links to documentation

**CONTRIBUTING.md should include:**
- Code of conduct reference
- How to report bugs
- How to suggest features
- Development setup instructions
- Code style guide (Spotless/ktlint)
- Pull request process
- Testing requirements
- Commit message conventions
- Branch naming conventions

**Notes:**
- Can reference CLAUDE.md for build commands (avoid duplication)
- Keep README user-focused, CONTRIBUTING developer-focused
