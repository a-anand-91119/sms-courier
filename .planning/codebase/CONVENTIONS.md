# Coding Conventions

**Analysis Date:** 2026-01-17

## Naming Patterns

**Files:**
- PascalCase for all Kotlin files: `SecurityManager.kt`, `PairedDevice.kt`
- `*Test.kt` suffix for test files: `CommandParserTest.kt`, `SecurityManagerTest.kt`
- `index.kt` not used (Kotlin doesn't have barrel exports)

**Functions:**
- camelCase for all functions: `parse()`, `generateChallenge()`, `verifyPassword()`
- No special prefix for async/suspend functions
- Handler pattern: `handle*()` for command handlers
- Query methods: `get*()`, `is*()`, `has*()`
- Update methods: `update*()`, `record*()`

**Variables:**
- camelCase for variables: `phoneNumber`, `deviceRole`
- UPPER_SNAKE_CASE for constants: `BCRYPT_COST`, `MAX_FAILED_ATTEMPTS`
- No underscore prefix for private members

**Types:**
- PascalCase for interfaces, no I prefix: `PairedDeviceDao`, not `IPairedDeviceDao`
- PascalCase for type aliases and data classes
- PascalCase for enum names, UPPER_CASE for values: `DeviceRole.SOURCE`, `PairingStatus.APPROVED`

## Code Style

**Formatting:**
- Spotless with ktlint (`app/spotless.gradle`)
- 4 space indentation
- Single quotes not applicable (Kotlin uses double quotes)
- No explicit semicolons (Kotlin convention)

**Linting:**
- ktlint via Spotless plugin
- Ratcheting from `origin/main` (only checks changed files)
- Run: `./gradlew spotlessApply` before committing
- Check: `./gradlew spotlessCheck`

**ktlint Overrides:**
```kotlin
// Disabled rules (from spotless.gradle)
"ktlint_function_naming_ignore_when_annotated_with": "Composable"
"ktlint_standard_property-naming": "disabled"
"ktlint_standard_no-wildcard-imports": "disabled"
```

## Import Organization

**Order:**
1. Android framework imports (`android.*`)
2. AndroidX imports (`androidx.*`)
3. Third-party libraries
4. Project imports (`dev.notyouraverage.smscourier.*`)

**Grouping:**
- No explicit blank lines required between groups (ktlint handles)
- Wildcard imports allowed (disabled in ktlint)

**Path Aliases:**
- None used (standard package imports)

## Error Handling

**Patterns:**
- Try-catch at service/repository boundaries
- Log.e() for errors with TAG and context
- Early return for validation failures
- Null checks with `?.` and `?:` operators

**Error Types:**
- Use built-in Exception types
- Log with structured context: `Log.e(TAG, "Failed to process: $phoneNumber", e)`
- Silent failures logged but not propagated in some cases (see CONCERNS.md)

## Logging

**Framework:**
- Android Log class (Log.d, Log.i, Log.w, Log.e)

**TAG Format:**
- Pattern: `"SMSC:<ClassName>"`
- Examples: `"SMSC:SecurityManager"`, `"SMSC:MasterService"`, `"SMSC:DeviceRepository"`

**Patterns:**
- Debug: Generation events, state changes
- Info: Successful operations, user actions
- Warning: Validation failures, unexpected states
- Error: Exceptions, failures
- Format: `Log.d(TAG, "Generated challenge for $phoneNumber, expires in ${CHALLENGE_EXPIRY_MS / 1000}s")`

## Comments

**When to Comment:**
- Explain why, not what
- Document complex algorithms (encryption, challenge-response)
- Note security considerations
- Mark TODOs with context

**KDoc/TSDoc:**
- Minimal KDoc usage
- Inline comments preferred for explanation
- Example:
```kotlin
/**
 * Derive an authentication key from password using SHA-256.
 * This key is stored on TARGET and computed on SOURCE for HMAC verification.
 */
fun deriveAuthKey(password: String): String
```

**TODO Comments:**
- Format: `// TODO: description`
- Example: `// In production, use libphonenumber for proper E.164 normalization`

**Section Headers:**
- Pattern: `// ==================== sectionName ====================`
- Used to separate major sections in large files

## Function Design

**Size:**
- Keep under 50 lines when practical
- Extract helpers for complex logic

**Parameters:**
- Use data classes for multiple related parameters
- Default values for optional parameters
- Destructure in parameter list when appropriate

**Return Values:**
- Use nullable types for operations that can fail
- Return early for guard clauses
- Suspend functions for async operations

## Module Design

**Exports:**
- Public classes and functions are default (no explicit visibility)
- Internal/private for implementation details
- Companion objects for constants and factory methods

**ViewModel Pattern:**
```kotlin
class ExampleViewModel(
    private val repository: ExampleRepository
) : ViewModel() {
    companion object {
        fun Factory(repository: ExampleRepository) = viewModelFactory {
            initializer { ExampleViewModel(repository) }
        }
    }
}
```

**Repository Pattern:**
```kotlin
class ExampleRepository(private val dao: ExampleDao) {
    suspend fun getItems(): List<Item> = withContext(Dispatchers.IO) {
        dao.getAll()
    }
}
```

## Coroutine Patterns

**Scopes:**
- ViewModels: `viewModelScope`
- Services: `CoroutineScope(SupervisorJob() + Dispatchers.Main)`
- Repository: `withContext(Dispatchers.IO)` for database calls

**Flow Usage:**
- StateFlow for UI state
- Flow for database observations
- `SharingStarted.WhileSubscribed(5000)` for state sharing

## Compose Conventions

**Composable Functions:**
- PascalCase function names (ktlint exception)
- `Screen` suffix for full-screen composables
- Parameters: state first, callbacks last

**State Management:**
- ViewModel provides StateFlow
- `collectAsStateWithLifecycle()` in composables
- Minimal state hoisting

---

*Convention analysis: 2026-01-17*
*Update when patterns change*
