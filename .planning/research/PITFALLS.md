# Pitfalls Research

**Domain:** Android Message History & Device Management
**Researched:** 2026-02-04
**Confidence:** HIGH

## Critical Pitfalls

### Pitfall 1: Room Migration Failure from Composite Primary Key Constraints

**What goes wrong:**
Adding a new `messages` table with a foreign key reference to `paired_devices` fails at runtime because the composite primary key (`phoneNumber`, `device_role`) complicates the relationship. Users upgrade from v5 to v6 and the app crashes with `IllegalStateException: A migration from 5 to 6 was required but not found` or worse, foreign key constraint violations.

**Why it happens:**
The existing `paired_devices` table uses a composite primary key to support bidirectional pairing (same phone number can be both SOURCE and TARGET). When adding `messages` table, developers typically reference only `phoneNumber`, breaking the foreign key relationship. Additionally, soft delete adds a `deleted_at` column to `paired_devices`, but queries throughout the codebase don't filter by `deleted_at IS NULL`, returning deleted devices in active lists.

**How to avoid:**
1. **Test migration thoroughly**: Use Room's `MigrationTestHelper` to verify migration from v5 to v6 with real data
2. **Foreign key strategy**: Either (a) reference both parts of composite key (`phoneNumber` + `device_role`), or (b) don't use foreign keys at all and handle referential integrity in code
3. **Schema export verification**: Keep `exportSchema = true` and review generated JSON schemas before/after migration
4. **Soft delete from day 1**: Add `deleted_at` column in the same migration that adds `messages` table, and update ALL existing queries to include `WHERE deleted_at IS NULL` filter

**Warning signs:**
- Manual SQL in migration using `ALTER TABLE` without testing edge cases
- Foreign key constraints defined in entity but not validated in tests
- No MigrationTestHelper tests in test suite
- `fallbackToDestructiveMigration` used anywhere (causes data loss)

**Phase to address:**
Phase 1: Schema Design & Migration Strategy (before any code)

---

### Pitfall 2: Foreground Service Blocking from Synchronous Database Writes

**What goes wrong:**
`MasterService` receives SMS commands and immediately writes to Room database on the main thread. During high message volume (20+ messages/minute), the service becomes unresponsive, SMS commands are delayed by 2-5 seconds, and the "SMS forwarding active" notification freezes. Users report "app is not responding" errors.

**Why it happens:**
Room DAOs with suspend functions still execute SQLite operations, and if awaited in the service's main coroutine scope without proper dispatchers, they block other operations. The problem is invisible during testing with 1-2 messages but becomes severe when storing individual message history (100s of inserts per session).

**How to avoid:**
1. **Use Dispatchers.IO explicitly**: All Room operations must use `withContext(Dispatchers.IO) { dao.insert() }`
2. **Background coroutines**: Launch database writes in separate coroutines that don't block command processing
3. **Batch inserts**: Accumulate messages in memory (max 10-20) and batch insert every 5 seconds or when batch full
4. **Monitor blocking**: Add StrictMode during development to detect disk reads/writes on main thread
5. **Transaction optimization**: Wrap batch inserts in `@Transaction` to reduce overhead

**Warning signs:**
- Service methods directly calling `runBlocking { dao.method() }`
- No explicit `withContext(Dispatchers.IO)` in service layer
- Database operations in the same coroutine that handles SMS broadcast
- ANR (Application Not Responding) logs in testing

**Phase to address:**
Phase 2: Message Storage Implementation (during DAO/Repository implementation)

---

### Pitfall 3: Unbounded Database Growth Without WAL Mode Performance Degradation

**What goes wrong:**
After 3-6 months of active use, the database grows to 50MB+ with 10,000+ message records. Query performance degrades from 50ms to 2000ms, UI scrolling becomes janky (LazyColumn drops frames), and exports timeout. Users with older devices experience crashes due to memory pressure.

**Why it happens:**
Room enables WAL (Write-Ahead Logging) mode by default, which helps with concurrent reads/writes but doesn't solve the core problem: missing indexes on frequently-queried columns, and no pagination for large result sets. The `messages` table has no index on `session_id` or `timestamp`, causing full table scans. Additionally, LazyColumn loads all messages for a session into memory at once instead of using Paging 3.

**How to avoid:**
1. **Mandatory indexes**: Add `@Index` annotations in MIGRATION_5_6 for:
   - `messages(session_id)` - for filtering by session
   - `messages(timestamp)` - for ordering
   - `messages(device_phone_number, timestamp)` - for device history queries
2. **Paging 3 integration**: Use `PagingSource` in DAO and `Pager` in repository for message lists (page size: 50)
3. **Auto-cleanup WorkManager**: Schedule periodic cleanup (default: messages older than 90 days) with `OneTimeWorkRequest` constraints:
   - `setRequiresDeviceIdle(true)` - run during idle
   - `setRequiresBatteryNotLow(true)` - avoid draining battery
4. **Export streaming**: Use `Flow<List<Message>>` with chunked queries instead of loading all messages into memory
5. **Room query validation**: Run queries on 10,000 record test database to verify performance before shipping

**Warning signs:**
- DAO methods return `List<Message>` instead of `PagingSource<Int, Message>` or `Flow<List<Message>>`
- No `@Index` annotations on `messages` entity
- Export code using `dao.getAllMessages()` without pagination
- No mention of WorkManager in cleanup implementation

**Phase to address:**
Phase 2: Message Storage Implementation (schema design with indexes) + Phase 4: Auto-Cleanup with WorkManager

---

### Pitfall 4: Soft Delete UX Confusion and Query Fragmentation

**What goes wrong:**
Users unpair a device expecting it to disappear from the UI, but it remains visible in the paired devices list with "removed" badge. The "History" screen shows both active and removed devices with no clear filtering, causing confusion. Worse, queries across the codebase are inconsistent - some filter `deleted_at IS NULL`, others don't, leading to bugs where removed devices appear in pairing flows or active session counts.

**Why it happens:**
Soft delete was added without updating the mental model throughout the app. The database allows it, but the UI doesn't clearly distinguish between:
- Active paired devices (can start sessions)
- Removed paired devices with history (can view history, cannot start sessions)
- Hard deleted devices (gone forever)

Additionally, developers forget to add `deleted_at IS NULL` to every query, especially in ViewModels and repositories.

**How to avoid:**
1. **Separate query methods**: Create distinct DAO methods:
   - `getActivePairedDevices()` - WHERE deleted_at IS NULL
   - `getRemovedPairedDevices()` - WHERE deleted_at IS NOT NULL
   - `getAllPairedDevicesIncludingRemoved()` - no filter (admin/debug only)
2. **UI separation**: Don't mix active and removed devices in same list - use tabs or separate screens
3. **Archived indicator**: Use clear visual language: "Archived", not "Removed" or "Deleted"
4. **Domain rules enforcement**: Repository layer ensures:
   - Can't start session with archived device
   - Can't send commands to archived device
   - Can view history of archived device
5. **Database constraints**: Add CHECK constraint `(deleted_at IS NULL AND status != 'ARCHIVED') OR (deleted_at IS NOT NULL)` to enforce consistency

**Warning signs:**
- Single `getPairedDevices()` method used everywhere
- Optional `includeDeleted: Boolean` parameter added to queries (prone to bugs)
- No separate UI for archived devices
- Comments like "TODO: filter deleted devices" in code

**Phase to address:**
Phase 3: Device History UI & Soft Delete (during repository and UI design)

---

### Pitfall 5: Scoped Storage Permission Fragmentation Across API Levels

**What goes wrong:**
Export to CSV/JSON fails on Android 11-13 with "Permission denied" errors, works on Android 10 and 14+. Users on different API levels have completely different experiences. The app requests `WRITE_EXTERNAL_STORAGE` which is ignored on API 30+, leading to confusing permission dialogs that don't actually grant storage access.

**Why it happens:**
Android's storage permissions changed dramatically at API 30 (Android 11). `WRITE_EXTERNAL_STORAGE` became useless. The correct approach depends on API level:
- API 29 and below: `WRITE_EXTERNAL_STORAGE` + `getExternalStoragePublicDirectory()`
- API 30-32: MediaStore APIs or Storage Access Framework (SAF)
- API 33+: No permissions needed for app-specific directory, but SAF required for user-chosen location

Developers target API 35 but test only on Android 14, missing the API 30-32 edge cases.

**How to avoid:**
1. **Use Storage Access Framework (SAF) exclusively**:
   - `Intent(Intent.ACTION_CREATE_DOCUMENT)` for export
   - `contentResolver.openOutputStream(uri)` to write
   - Works across all API levels, no permissions needed
2. **Remove storage permissions**: Don't request `WRITE_EXTERNAL_STORAGE` or `READ_EXTERNAL_STORAGE` - they're misleading on API 30+
3. **API-level testing matrix**: Test export on:
   - API 29 (Android 10)
   - API 30-32 (Android 11-12L) - critical edge case
   - API 33+ (Android 13+)
4. **User-visible errors**: If export fails, show specific error message with API level context (not generic "Permission denied")

**Warning signs:**
- Code checking `Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q` and branching logic
- `WRITE_EXTERNAL_STORAGE` permission in `AndroidManifest.xml`
- Calls to `Environment.getExternalStoragePublicDirectory()` or `getExternalFilesDir()`
- No SAF usage (`ACTION_CREATE_DOCUMENT`, `ACTION_OPEN_DOCUMENT`)

**Phase to address:**
Phase 5: Export Functionality (during implementation, before testing)

---

### Pitfall 6: WorkManager Battery Optimization Restrictions on Android 12+

**What goes wrong:**
Auto-cleanup WorkManager job scheduled to run daily never executes on Android 12+ devices with aggressive battery optimization. Users accumulate 6 months of message history despite setting retention to 30 days. The WorkManager job shows as "scheduled" in debugging but never runs, leading to database bloat and performance issues.

**Why it happens:**
Android 12 (API 31) introduced stricter background execution limits. WorkManager jobs can be indefinitely deferred if constraints are too restrictive or the app is in a restrictive battery bucket. Common mistakes:
- Setting `setRequiresDeviceIdle(true)` - device may never be "idle" according to Android's definition
- Not handling battery optimization exemption requests
- Using `PeriodicWorkRequest` with interval < 15 minutes
- Not checking job execution in WorkManager logs

**How to avoid:**
1. **Lenient constraints**: Use only essential constraints for cleanup:
   - `setRequiresBatteryNotLow(true)` - reasonable
   - `setRequiresCharging(false)` - don't require charging
   - `setRequiresDeviceIdle(false)` - avoid idle requirement
2. **Periodic interval**: Use minimum 15 minutes for `PeriodicWorkRequest`, but for daily cleanup, `24 hours` is appropriate
3. **Exponential backoff**: Set reasonable backoff policy for retries: `setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)`
4. **User-visible setting**: Add "Last cleanup: X hours ago" in settings to expose if cleanup isn't running
5. **Manual cleanup option**: Provide "Clean up now" button in settings as escape hatch
6. **Testing on real devices**: WorkManager behavior differs significantly between emulators and real devices with manufacturer battery optimization

**Warning signs:**
- No logging in Worker's `doWork()` method
- No user-visible indication of last cleanup time
- Constraints include `RequiresDeviceIdle` for non-critical work
- No manual cleanup option in UI
- Only tested on emulator, not real devices

**Phase to address:**
Phase 4: Auto-Cleanup with WorkManager (during implementation and testing)

---

### Pitfall 7: Jetpack Compose LazyColumn Recomposition Storm from Flow Collection

**What goes wrong:**
Device history screen with message list becomes unusable - scrolling drops to 10 FPS, UI freezes for 1-2 seconds when new message arrives, and battery drains rapidly. The `LazyColumn` showing 100+ messages recomposes all visible items every time a single message is added, even though only one item changed.

**Why it happens:**
ViewModel exposes `Flow<List<Message>>` collected with `collectAsState()` in composable. Every database update emits a new `List<Message>`, triggering full recomposition. Compose can't determine that only one item changed because:
1. No stable keys in `LazyColumn` items
2. Passing unstable parameters (ViewModel, mutable state) down composable hierarchy
3. Missing `@Stable` or `@Immutable` annotations on data classes
4. Using `remember` incorrectly, causing unnecessary recompositions

Compose strong skipping mode (available in 1.5.4+) helps but doesn't solve root cause.

**How to avoid:**
1. **Use Paging 3 with LazyPagingItems**: Replace `Flow<List<Message>>` with `Flow<PagingData<Message>>` and `collectAsLazyPagingItems()`:
   ```kotlin
   val messages = viewModel.messagesFlow.collectAsLazyPagingItems()
   LazyColumn {
       items(
           count = messages.itemCount,
           key = messages.itemKey { it.id }
       ) { index ->
           val message = messages[index]
           if (message != null) {
               MessageItem(message)
           }
       }
   }
   ```
2. **Stable keys**: Always provide `key` parameter to `LazyColumn.items()` using unique identifier (message ID)
3. **Defer state reads**: Use `derivedStateOf` for computed properties based on Flow state
4. **Immutable data classes**: Add `@Immutable` to `Message` entity
5. **Enable strong skipping mode**: In `build.gradle`:
   ```kotlin
   kotlinOptions {
       freeCompilerArgs += ["-P", "plugin:androidx.compose.compiler.plugins.kotlin:experimentalStrongSkipping=true"]
   }
   ```

**Warning signs:**
- `LazyColumn { items(list) }` without `key` parameter
- ViewModel passed directly to composables instead of specific state
- No use of Paging 3 despite large lists
- Slowness only appears in release build (debug builds have known Compose performance issues)
- All items flash/animate when one item changes

**Phase to address:**
Phase 3: Device History UI (during LazyColumn implementation)

---

### Pitfall 8: Bidirectional Visibility Logic Errors from Session Role Confusion

**What goes wrong:**
Home screen shows incorrect directional indicators: Device A (SOURCE) displays "↑ Forwarding TO B" when it should show "↓ Receiving FROM B". When the same device is paired bidirectionally (both SOURCE and TARGET roles), the UI shows conflicting states or crashes with null pointer exceptions. Session counts are wrong (counting both directions as separate sessions when they're the same session).

**Why it happens:**
The mental model is inverted: SOURCE device receives forwarded messages, TARGET device sends them. Developers think "SOURCE = sending" but actually "SOURCE = source of the request for forwarding". When calculating directional indicators, the logic checks `session.devicePhoneNumber == myDevice.role` without considering the role defines the relationship, not the direction of data flow. Bidirectional case (same phone paired twice with different roles) isn't tested.

**How to avoid:**
1. **Centralized direction calculation**: Create a `SessionDirectionCalculator` class with clear semantics:
   ```kotlin
   fun getDirection(session: ForwardingSession, myPhoneNumber: String): Direction {
       // Find the paired device for this session
       val device = getPairedDevice(session.devicePhoneNumber, session.role)
       return when {
           device.role == DeviceRole.TARGET && device.phoneNumber == myPhoneNumber ->
               Direction.SENDING_TO(session.partnerPhone)
           device.role == DeviceRole.SOURCE && device.phoneNumber == myPhoneNumber ->
               Direction.RECEIVING_FROM(session.partnerPhone)
           else -> throw IllegalStateException("Session doesn't match current device")
       }
   }
   ```
2. **Domain terminology alignment**: Document clearly:
   - SOURCE role = requests forwarding = receives messages
   - TARGET role = provides forwarding = sends messages
3. **Bidirectional test cases**: Create test scenario where same phone number is paired twice (different roles)
4. **Session deduplication**: When showing active sessions, deduplicate bidirectional sessions or show as single entry with "⇅" indicator
5. **Role-based UI copy**: Always check role before showing "Forwarding to" vs "Receiving from"

**Warning signs:**
- UI copy says "SOURCE forwards to TARGET" (backwards)
- No enum for direction calculation (inline logic in UI)
- Active sessions query doesn't join with paired_devices to check role
- No test case for bidirectional pairing scenario
- Confusion in code comments about what SOURCE/TARGET mean

**Phase to address:**
Phase 6: Bidirectional Visibility Indicators (during implementation, before integration)

---

## Technical Debt Patterns

Shortcuts that seem reasonable but create long-term problems.

| Shortcut | Immediate Benefit | Long-term Cost | When Acceptable |
|----------|-------------------|----------------|-----------------|
| Using `fallbackToDestructiveMigration()` for v5→v6 | Skip writing migration code | All existing user data lost on upgrade | Never in production - testing only |
| No indexes on message table | Faster inserts (minimal difference) | Queries degrade to O(n), unusable at 10k+ records | Never - indexes are essential |
| Single DAO method `getPairedDevices(includeDeleted: Boolean)` | Less code to maintain | Every call site must remember to pass correct boolean, prone to bugs | Never - separate methods enforce correctness |
| Direct Flow collection without Paging | Simpler initial implementation | UI becomes unusable with 500+ messages | Only for lists guaranteed < 50 items |
| MediaStore instead of SAF for export | Fewer lines of code for API 29+ | Permission issues on API 30-32, requires branching logic | Never - SAF works universally |
| Synchronous DAO calls in service | No coroutine complexity | Blocks service, causes ANRs | Never - all DB calls must be async |

## Integration Gotchas

Common mistakes when connecting to external services.

| Integration | Common Mistake | Correct Approach |
|-------------|----------------|------------------|
| Room Database from MasterService | Calling DAO methods directly on service scope | Use `withContext(Dispatchers.IO)` wrapper, launch separate coroutines for DB writes |
| WorkManager constraints | Setting `RequiresDeviceIdle(true)` for cleanup | Use `RequiresBatteryNotLow(true)` only, avoid idle requirement |
| DataStore settings | Reading settings synchronously on service startup | Collect settings as Flow and react to changes, use default values until loaded |
| SAF (Storage Access Framework) | Expecting synchronous file path | Use `contentResolver.openOutputStream(uri)` with returned URI from picker |
| Foreground notification | Updating notification on every message | Batch updates (max 1/second) to avoid rate limiting |

## Performance Traps

Patterns that work at small scale but fail as usage grows.

| Trap | Symptoms | Prevention | When It Breaks |
|------|----------|------------|----------------|
| Loading all messages into memory for export | Export works instantly | Use streaming with chunked queries (500 rows at a time) | > 5,000 messages (OutOfMemoryError) |
| No pagination in message list | Initial load fast | Use Paging 3 with page size 50 | > 500 messages (UI janky) |
| Flow<List<Message>> for real-time updates | Simple code | Use Paging 3 Flow<PagingData<Message>> | > 100 messages (recomposition storm) |
| Single database transaction per message | Each insert fast | Use batched inserts with @Transaction | > 20 messages/minute (blocking) |
| No keys in LazyColumn items | Compose code simpler | Always provide unique key to items() | > 50 items (full recomposition) |
| Inline session direction calculation in UI | No extra class needed | Create SessionDirectionCalculator singleton | > 2 bidirectional pairs (logic errors) |

## Security Mistakes

Domain-specific security issues beyond general web security.

| Mistake | Risk | Prevention |
|---------|------|------------|
| Storing passwords in message history | Password leaked in CSV export | Never store session password in messages table, only session_id reference |
| Exporting archived device passwords | Password hash in exported JSON | Filter password_hash column from all exports |
| No device ownership validation before history access | User A can view User B's messages if phone numbers overlap | Check current device's phone number matches session owner |
| Soft delete allows re-pairing without clearing history | Unpair, re-pair = old history visible to new partner | Hard delete history on unpair, or require explicit "Keep history" opt-in |

## UX Pitfalls

Common user experience mistakes in this domain.

| Pitfall | User Impact | Better Approach |
|---------|-------------|-----------------|
| Archived devices shown in main paired list | Confusion - device shown but can't start session | Separate "Archived devices" section or screen with clear visual distinction |
| Export fails silently with no error message | User thinks export succeeded, can't find file | Show toast on success with "Saved to Downloads" + Show error dialog with specific reason on failure |
| "↑" and "↓" arrows without labels | Unclear what direction means | Add text labels: "↑ Sending to X" or "↓ Receiving from Y" |
| No indication of last auto-cleanup time | User doesn't know if auto-cleanup is working | Show "Last cleanup: 3 hours ago" in settings |
| Can't distinguish between "no messages" and "loading messages" | Blank screen with no context | Show loading spinner vs. empty state message |
| History retention in days only | User wants "keep 100 messages" not "keep 30 days" | Offer both options: days OR message count limit |

## "Looks Done But Isn't" Checklist

Things that appear complete but are missing critical pieces.

- [ ] **Message Storage:** Often missing indexes on `session_id` and `timestamp` — verify with EXPLAIN QUERY PLAN on 10k record database
- [ ] **Database Migration:** Often missing MigrationTestHelper tests — verify migration from v5 to v6 with real devices table data
- [ ] **Soft Delete:** Often missing `deleted_at IS NULL` filter in all queries — grep codebase for DAO methods without filter
- [ ] **Export Feature:** Often missing API 30-32 testing — manually test on Android 11-12 devices (not just 14+)
- [ ] **WorkManager Cleanup:** Often missing on real device testing — verify job executes on physical device with battery saver enabled
- [ ] **Bidirectional Indicators:** Often missing bidirectional test case — verify UI with same device paired as both SOURCE and TARGET
- [ ] **LazyColumn Performance:** Often missing unique keys — check all `items()` calls have `key` parameter
- [ ] **Paging Integration:** Often missing proper error handling — verify offline state, empty state, and error state in UI

## Recovery Strategies

When pitfalls occur despite prevention, how to recover.

| Pitfall | Recovery Cost | Recovery Steps |
|---------|---------------|----------------|
| Migration failure causes crashes | HIGH | 1. Release emergency patch with `fallbackToDestructiveMigration` + user warning, 2. Fix migration in next version with proper testing, 3. Consider data export/import tool for power users |
| Database growth > 100MB | MEDIUM | 1. Release hotfix with aggressive cleanup (10 day retention), 2. Add manual "Delete all history" option, 3. Prompt user to reduce retention on app start if DB > 50MB |
| WorkManager never runs | LOW | 1. Add manual cleanup button in settings, 2. Show "Last cleanup" timestamp to expose issue, 3. Debug constraints on affected device |
| LazyColumn performance death spiral | MEDIUM | 1. Add emergency pagination in patch (even if not Paging 3), 2. Limit visible items to 100 with "Load more" button, 3. Properly implement Paging 3 in next version |
| Export permission failure API 30-32 | LOW | 1. Fallback to share intent with temp file, 2. Update to SAF in next release |
| Soft delete query bugs | MEDIUM | 1. Add database view with `deleted_at IS NULL` filter, 2. Change DAOs to query view instead of table, 3. Fix individual query bugs as reported |

## Pitfall-to-Phase Mapping

How roadmap phases should address these pitfalls.

| Pitfall | Prevention Phase | Verification |
|---------|------------------|--------------|
| Room Migration Failure | Phase 1: Schema Design | MigrationTestHelper tests pass with v5 data |
| Foreground Service Blocking | Phase 2: Message Storage Implementation | StrictMode detects no main thread disk I/O |
| Unbounded Database Growth | Phase 2: Message Storage + Phase 4: Auto-Cleanup | Query 10k records completes in < 100ms |
| Soft Delete UX Confusion | Phase 3: Device History UI | User testing shows clear understanding of archived devices |
| Scoped Storage Permissions | Phase 5: Export Functionality | Export succeeds on API 29, 30-32, 33+ devices |
| WorkManager Restrictions | Phase 4: Auto-Cleanup | Cleanup job executes within 48 hours on real device |
| LazyColumn Recomposition Storm | Phase 3: Device History UI | Scrolling maintains 60 FPS with 200+ messages |
| Bidirectional Visibility Logic | Phase 6: Bidirectional Visibility | Test case with bidirectional pairing shows correct indicators |

## Sources

**Room Database Migrations:**
- [Migrate your Room database | Android Developers](https://developer.android.com/training/data-storage/room/migrating-db-versions)
- [Understanding migrations with Room | Medium](https://medium.com/androiddevelopers/understanding-migrations-with-room-f01e04b07929)
- [Common Room migration pitfalls | Infinum Handbook](https://infinum.com/handbook/android/common-android/room-migrations)

**Database Performance:**
- [The Hidden Dangers of Room Database Performance | ProAndroidDev](https://proandroiddev.com/the-hidden-dangers-of-room-database-performance-and-how-to-fix-them-ac93830885bd)
- [Android Room Hidden Costs](https://krossovochkin.com/posts/2020_12_18_android_room_hidden_costs/)
- [Squeezing Performance from SQLite with Room | HackerNoon](https://hackernoon.com/squeezing-performance-from-sqlite-insertions-with-room-d769512f8330)

**WorkManager Constraints:**
- [Optimize battery use for task scheduling APIs | Android Developers](https://developer.android.com/develop/background-work/background-tasks/optimize-battery)
- [Android WorkManager: A Complete Technical Deep Dive | ProAndroidDev](https://proandroiddev.com/android-workmanager-a-complete-technical-deep-dive-f037c768d87b)

**Foreground Services:**
- [Services overview | Android Developers](https://developer.android.com/develop/background-work/services)
- [The "misbehaving" foreground service in Android | ProAndroidDev](https://proandroiddev.com/when-your-app-makes-android-foreground-services-misbehave-8dbcc57dd99c)

**Scoped Storage:**
- [Scoped storage | Android Open Source Project](https://source.android.com/docs/core/storage/scoped)
- [Storage updates in Android 11 | Android Developers](https://developer.android.com/about/versions/11/privacy/storage)
- [Android storage use cases and best practices | Android Developers](https://developer.android.com/training/data-storage/use-cases)

**Jetpack Compose Performance:**
- [Practical performance problem solving in Jetpack Compose | Android Developers](https://developer.android.com/codelabs/jetpack-compose-performance)
- [Follow best practices | Jetpack Compose | Android Developers](https://developer.android.com/develop/ui/compose/performance/bestpractices)
- [Optimizing Performance in Jetpack Compose | Teknasyon Engineering](https://engineering.teknasyon.com/optimizing-performance-in-jetpack-compose-91e39cb0bf41)

**Soft Delete Patterns:**
- [Challenges of Implementing Soft Delete Pattern | Medium](https://medium.com/@sdtapusd20/challenges-of-implementing-soft-delete-pattern-a25a297753bf)
- [Avoiding the soft delete anti-pattern | Cultured Systems](https://www.cultured.systems/2024/04/24/Soft-delete/)

**Room WAL Mode:**
- [Compatibility write-ahead logging for apps | Android Open Source Project](https://source.android.com/docs/core/perf/compatibility-wal)
- [Parallelism with Android SQLite](https://blog.p-y.wtf/parallelism-with-android-sqlite)

---
*Pitfalls research for: SMS Courier v0.0.64 Device History & Bidirectional Visibility*
*Researched: 2026-02-04*
