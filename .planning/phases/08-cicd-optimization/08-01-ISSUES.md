# UAT Issues: Phase 8 Plan 1

**Tested:** 2026-01-16
**Source:** .planning/phases/08-cicd-optimization/08-01-SUMMARY.md
**Tester:** User via /gsd:verify-work

## Open Issues

[None]

## Resolved Issues

### UAT-001: AAB path resolution fails in upload_internal lane

**Discovered:** 2026-01-16
**Phase/Plan:** 08-01
**Severity:** Blocker
**Feature:** upload_internal lane AAB validation
**Description:** The File.exist? check in upload_internal fails because fastlane runs from fastlane/ directory, but the path is relative to project root.
**Expected:** Path `app/build/outputs/bundle/release/app-release.aab` should resolve correctly
**Actual:** File.exist? looks for `fastlane/app/build/...` which doesn't exist

**Resolved:** 2026-01-16 - Fixed in commit `53631c7`
**Fix applied:** Used `File.expand_path("../#{aab_path}", __dir__)` to resolve path from project root.

---

*Phase: 08-cicd-optimization*
*Plan: 01*
*Tested: 2026-01-16*
