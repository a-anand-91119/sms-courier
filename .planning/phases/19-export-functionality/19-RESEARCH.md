# Phase 19: Export Functionality - Research

**Researched:** 2026-02-06
**Domain:** Android Storage Access Framework, CSV/JSON/TXT export, File I/O, Jetpack Compose Activity Result APIs
**Confidence:** HIGH

## Summary

Phase 19 implements export functionality for session and message history in CSV, JSON, and plain text formats. Users can export from Archive Management screen (archived devices) and Session History screen (single session or bulk per-device export). The implementation must use Storage Access Framework (SAF) exclusively for API 29-35 compatibility without permission fragmentation.

The codebase already has the data layer (ForwardingSession and ForwardedMessage entities in Room database v7), repository layer with query methods, and UI screens (SessionHistoryScreen, ArchiveManagementScreen). The export feature requires adding file generation logic (manual CSV/JSON/TXT formatting with Kotlin stdlib), SAF integration using rememberLauncherForActivityResult with CreateDocument contract, and UI triggers (buttons, progress indicators, snackbar feedback).

Key technical decisions from CONTEXT.md: support single-session AND per-device bulk export, optional metadata header toggle, RFC 4180 CSV compliance, nested JSON structure, chat-log-style plain text, fixed/generated filenames (no user editing), progress spinner with success/failure snackbar showing file location. Storage Access Framework eliminates all permission requests (WRITE_EXTERNAL_STORAGE not needed on API 30+).

**Primary recommendation:** Use Kotlin stdlib for CSV/JSON/TXT generation (no external libraries needed), rememberLauncherForActivityResult with ActivityResultContracts.CreateDocument for SAF integration, ViewModel methods for export orchestration, progress StateFlow for UI feedback, and DateTimeFormatters.kt extension for timestamp formatting per export type. Test across API 29, 30-32, and 33+ to verify no permission issues.

## Standard Stack

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Kotlin stdlib | 1.9.0 | CSV/JSON/TXT generation | Built-in string manipulation sufficient for simple export formats, no external dependencies needed |
| androidx.activity:activity-compose | 1.9.1 | Activity Result APIs | Already in use, provides rememberLauncherForActivityResult for SAF integration |
| android.content.Intent | Platform API | Storage Access Framework | ACTION_CREATE_DOCUMENT for file creation, works API 19+ (app targets 29-35) |
| android.net.Uri | Platform API | Content URI handling | Returned by SAF picker, used with ContentResolver for file writing |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| java.io.OutputStream | Platform API | File writing | Use with ContentResolver.openOutputStream(uri) for SAF file writing |
| java.text.SimpleDateFormat | Platform API | Timestamp formatting | Use for export-specific timestamp formats (ISO 8601, local datetime) |
| java.net.URLEncoder | Platform API | Filename sanitization | Use to sanitize device phone numbers in bulk export filenames |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Kotlin stdlib CSV | kotlinx-serialization-csv / OpenCSV | External dependency overhead for trivial format (5 lines of code), increases APK size |
| Kotlin stdlib JSON | kotlinx-serialization-json / Gson / Moshi | Simple nested structure doesn't justify reflection overhead or dependency weight |
| SAF (ACTION_CREATE_DOCUMENT) | MediaStore APIs (API 29+) | MediaStore requires API-level branching, SAF works universally on API 19+ |
| SAF | MANAGE_EXTERNAL_STORAGE permission | Permission requires Play Store review, user-hostile UX, unnecessary for document export |
| Manual JSON building | JSON library | Our JSON structure is simple (array of objects), manual building is 10 lines, library adds 200KB+ to APK |

**Installation:**
```gradle
// NO NEW DEPENDENCIES REQUIRED
// All capabilities available with existing dependencies:
dependencies {
    implementation(libs.androidx.activity.compose)  // 1.9.1 (already present)
    implementation(libs.androidx.material3)         // via BOM 2024.08.00 (already present)
    implementation(libs.androidx.lifecycle.viewmodel.compose)  // 2.7.0 (already present)
}
```

## Architecture Patterns

### Recommended Project Structure
```
app/src/main/java/dev/notyouraverage/smscourier/
├── composables/
│   ├── screens/
│   │   ├── (SessionHistoryScreen.kt)        # Add export button, existing
│   │   └── (ArchiveManagementScreen.kt)     # Add export button, existing
│   └── components/
│       ├── (MessageDetailBottomSheet.kt)    # Add export button, existing
│       └── ExportFormatBottomSheet.kt       # NEW: Format picker (CSV/JSON/TXT)
├── viewmodels/
│   ├── (SessionHistoryViewModel.kt)         # Add export methods, existing
│   └── (ArchiveManagementViewModel.kt)      # Add export methods, existing
├── export/
│   ├── ExportFormatter.kt                   # NEW: Format session/message data to CSV/JSON/TXT
│   ├── ExportFileWriter.kt                  # NEW: Write to ContentResolver OutputStream
│   └── ExportTypes.kt                       # NEW: Data classes for export configuration
├── repository/
│   ├── (ForwardingSessionRepository.kt)     # Add bulk query methods, existing
│   └── (ForwardedMessageRepository.kt)      # Add bulk query methods, existing
└── utils/
    └── (DateTimeFormatters.kt)              # Add export timestamp formatters, existing
```

### Pattern 1: Storage Access Framework with Activity Result API

**What:** Use rememberLauncherForActivityResult with CreateDocument contract to launch SAF picker, receive URI, write to ContentResolver.openOutputStream

**When to use:** For all file export operations (CSV, JSON, TXT) to avoid permission requests

**Example:**
```kotlin
// Source: https://developer.android.com/training/data-storage/shared/documents-files
@Composable
fun ExportButton(
    onExport: (format: ExportFormat) -> Unit,
    viewModel: SessionHistoryViewModel
) {
    val context = LocalContext.current

    // SAF launcher for CSV
    val csvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let { viewModel.exportToFile(it, ExportFormat.CSV) }
    }

    // SAF launcher for JSON
    val jsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { viewModel.exportToFile(it, ExportFormat.JSON) }
    }

    // SAF launcher for TXT
    val txtLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        uri?.let { viewModel.exportToFile(it, ExportFormat.TXT) }
    }

    Button(onClick = {
        // Show format picker, then launch appropriate launcher
        when (selectedFormat) {
            ExportFormat.CSV -> csvLauncher.launch("smscourier_export_${timestamp}.csv")
            ExportFormat.JSON -> jsonLauncher.launch("smscourier_export_${timestamp}.json")
            ExportFormat.TXT -> txtLauncher.launch("smscourier_export_${timestamp}.txt")
        }
    }) {
        Text("Export")
    }
}

// In ViewModel
fun exportToFile(uri: Uri, format: ExportFormat) {
    viewModelScope.launch(Dispatchers.IO) {
        _isExporting.value = true
        try {
            val sessions = sessionRepository.getSessionsForDevice(phoneNumber) // Flow
            val allMessages = sessions.flatMap { session ->
                messageRepository.getMessagesForSession(session.id) // Flow
            }

            val content = when (format) {
                ExportFormat.CSV -> ExportFormatter.toCsv(sessions, allMessages, includeMetadata)
                ExportFormat.JSON -> ExportFormatter.toJson(sessions, allMessages, includeMetadata)
                ExportFormat.TXT -> ExportFormatter.toText(sessions, allMessages, includeMetadata)
            }

            // Write to SAF URI
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(content.toByteArray(Charsets.UTF_8))
            }

            _exportResult.value = ExportResult.Success(uri)
        } catch (e: Exception) {
            _exportResult.value = ExportResult.Error(e.message ?: "Export failed")
        } finally {
            _isExporting.value = false
        }
    }
}
```

### Pattern 2: Manual CSV Generation (RFC 4180 Compliant)

**What:** Use Kotlin stdlib string manipulation to generate RFC 4180 CSV with proper quote escaping

**When to use:** For CSV export format (CONTEXT.md: "RFC 4180 compliant, works in Excel/Google Sheets")

**Example:**
```kotlin
// Source: RFC 4180 specification and Kotlin stdlib docs
object ExportFormatter {

    fun toCsv(
        sessions: List<ForwardingSession>,
        messages: List<ForwardedMessage>,
        includeMetadata: Boolean
    ): String = buildString {
        // Metadata header (optional)
        if (includeMetadata) {
            appendLine("# Session Export")
            appendLine("# Generated: ${DateTimeFormatters.formatExportTimestamp(System.currentTimeMillis())}")
            appendLine("# Total Sessions: ${sessions.size}")
            appendLine("# Total Messages: ${messages.size}")
            appendLine()
        }

        // Column headers
        appendLine("Session ID,Session Start,Session End,Duration (minutes),Message Count,Sender Number,Message Timestamp,Message Content")

        // Group messages by session
        val messagesBySession = messages.groupBy { it.sessionId }

        sessions.forEach { session ->
            val sessionMessages = messagesBySession[session.id] ?: emptyList()

            if (sessionMessages.isEmpty()) {
                // Session row without messages
                appendCsvRow(
                    session.id.toString(),
                    DateTimeFormatters.formatExportTimestamp(session.startedAt),
                    if (session.isActive) "Active" else DateTimeFormatters.formatExportTimestamp(session.startedAt + session.durationMinutes * 60000),
                    session.durationMinutes.toString(),
                    session.messageCount.toString(),
                    "",  // No sender
                    "",  // No timestamp
                    ""   // No message
                )
            } else {
                sessionMessages.forEach { message ->
                    appendCsvRow(
                        session.id.toString(),
                        DateTimeFormatters.formatExportTimestamp(session.startedAt),
                        if (session.isActive) "Active" else DateTimeFormatters.formatExportTimestamp(session.startedAt + session.durationMinutes * 60000),
                        session.durationMinutes.toString(),
                        session.messageCount.toString(),
                        message.senderNumber,
                        DateTimeFormatters.formatExportTimestamp(message.timestamp),
                        message.messageContent
                    )
                }
            }
        }
    }

    private fun StringBuilder.appendCsvRow(vararg values: String) {
        appendLine(values.joinToString(",") { value ->
            // RFC 4180: Escape quotes and wrap in quotes if contains comma, newline, or quote
            if (value.contains(',') || value.contains('\n') || value.contains('"')) {
                "\"${value.replace("\"", "\"\"")}\""
            } else {
                value
            }
        })
    }
}
```

### Pattern 3: Manual JSON Generation (Nested Structure)

**What:** Use Kotlin stdlib buildString with proper escaping for JSON structure

**When to use:** For JSON export format (CONTEXT.md: "Nested structure with sessions array containing messages")

**Example:**
```kotlin
// Source: JSON specification and Kotlin stdlib
object ExportFormatter {

    fun toJson(
        sessions: List<ForwardingSession>,
        messages: List<ForwardedMessage>,
        includeMetadata: Boolean
    ): String = buildString {
        appendLine("{")

        // Metadata (optional)
        if (includeMetadata) {
            appendLine("  \"metadata\": {")
            appendLine("    \"generated\": \"${DateTimeFormatters.formatExportTimestampISO(System.currentTimeMillis())}\",")
            appendLine("    \"totalSessions\": ${sessions.size},")
            appendLine("    \"totalMessages\": ${messages.size}")
            appendLine("  },")
        }

        appendLine("  \"sessions\": [")

        val messagesBySession = messages.groupBy { it.sessionId }

        sessions.forEachIndexed { sessionIndex, session ->
            val sessionMessages = messagesBySession[session.id] ?: emptyList()

            appendLine("    {")
            appendLine("      \"id\": ${session.id},")
            appendLine("      \"startedAt\": \"${DateTimeFormatters.formatExportTimestampISO(session.startedAt)}\",")
            appendLine("      \"durationMinutes\": ${session.durationMinutes},")
            appendLine("      \"messageCount\": ${session.messageCount},")
            appendLine("      \"isActive\": ${session.isActive},")
            appendLine("      \"messages\": [")

            sessionMessages.forEachIndexed { msgIndex, message ->
                appendLine("        {")
                appendLine("          \"id\": ${message.id},")
                appendLine("          \"senderNumber\": \"${message.senderNumber}\",")
                appendLine("          \"timestamp\": \"${DateTimeFormatters.formatExportTimestampISO(message.timestamp)}\",")
                appendLine("          \"content\": ${message.messageContent.toJsonString()}")
                append("        }")
                if (msgIndex < sessionMessages.size - 1) appendLine(",") else appendLine()
            }

            appendLine("      ]")
            append("    }")
            if (sessionIndex < sessions.size - 1) appendLine(",") else appendLine()
        }

        appendLine("  ]")
        appendLine("}")
    }

    // JSON string escaping
    private fun String.toJsonString(): String {
        return "\"" + this
            .replace("\\", "\\\\")  // Backslash
            .replace("\"", "\\\"")  // Quote
            .replace("\n", "\\n")   // Newline
            .replace("\r", "\\r")   // Carriage return
            .replace("\t", "\\t")   // Tab
            + "\""
    }
}
```

### Pattern 4: Plain Text Chat Log Format

**What:** Generate human-readable chat log with timestamps and sender numbers

**When to use:** For TXT export format (CONTEXT.md: "Chat log style, like reading a conversation transcript")

**Example:**
```kotlin
// Source: Common chat log formatting patterns
object ExportFormatter {

    fun toText(
        sessions: List<ForwardingSession>,
        messages: List<ForwardedMessage>,
        includeMetadata: Boolean
    ): String = buildString {
        // Metadata header (optional)
        if (includeMetadata) {
            appendLine("═══════════════════════════════════════════════════════")
            appendLine("SMS Courier Export")
            appendLine("Generated: ${DateTimeFormatters.formatExportTimestamp(System.currentTimeMillis())}")
            appendLine("Total Sessions: ${sessions.size}")
            appendLine("Total Messages: ${messages.size}")
            appendLine("═══════════════════════════════════════════════════════")
            appendLine()
        }

        val messagesBySession = messages.groupBy { it.sessionId }

        sessions.forEach { session ->
            // Session header
            appendLine("─────────────────────────────────────────────────────")
            appendLine("Session #${session.id}")
            appendLine("Started: ${DateTimeFormatters.formatExportTimestamp(session.startedAt)}")
            appendLine("Duration: ${session.durationMinutes} minutes")
            appendLine("Messages: ${session.messageCount}")
            if (session.isActive) appendLine("Status: ACTIVE")
            appendLine("─────────────────────────────────────────────────────")
            appendLine()

            val sessionMessages = messagesBySession[session.id] ?: emptyList()

            if (sessionMessages.isEmpty()) {
                appendLine("(No messages in this session)")
                appendLine()
            } else {
                sessionMessages.forEach { message ->
                    // Message format: [timestamp] +sender: content
                    appendLine("[${DateTimeFormatters.formatExportTime(message.timestamp)}] ${message.senderNumber}:")
                    appendLine(message.messageContent)
                    appendLine()
                }
            }
        }
    }
}
```

### Pattern 5: Export Progress and Feedback UI

**What:** Show progress spinner during export, success/failure snackbar with file location

**When to use:** For all export operations (per CONTEXT.md: "Progress spinner during export, then success/failure snackbar")

**Example:**
```kotlin
// Source: Material 3 Snackbar best practices
@Composable
fun SessionHistoryScreen(viewModel: SessionHistoryViewModel) {
    val isExporting by viewModel.isExporting.collectAsState()
    val exportResult by viewModel.exportResult.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(exportResult) {
        when (val result = exportResult) {
            is ExportResult.Success -> {
                snackbarHostState.showSnackbar(
                    message = "Export successful: ${result.filename}",
                    duration = SnackbarDuration.Short
                )
            }
            is ExportResult.Error -> {
                snackbarHostState.showSnackbar(
                    message = "Export failed: ${result.message}",
                    duration = SnackbarDuration.Long
                )
            }
            null -> { /* No result yet */ }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            LargeTopAppBar(
                title = { Text("Session History") },
                actions = {
                    // Export button
                    if (isExporting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        IconButton(onClick = { /* Show format picker */ }) {
                            Icon(Icons.Default.Download, "Export")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        // Session list content
    }
}

// ViewModel state
sealed class ExportResult {
    data class Success(val uri: Uri) : ExportResult() {
        val filename: String get() = uri.lastPathSegment ?: "export file"
    }
    data class Error(val message: String) : ExportResult()
}

private val _isExporting = MutableStateFlow(false)
val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

private val _exportResult = MutableStateFlow<ExportResult?>(null)
val exportResult: StateFlow<ExportResult?> = _exportResult.asStateFlow()
```

### Pattern 6: Bulk Export for Device History

**What:** Query all sessions and messages for a device, generate export with per-role filtering

**When to use:** For per-device bulk export from Device History or Session History "Export all" button

**Example:**
```kotlin
// Source: Existing Repository patterns
// Add to ForwardingSessionRepository
suspend fun getAllSessionsForDeviceWithMessages(
    phoneNumber: String,
    roleFilter: DeviceRole? = null
): List<SessionWithMessages> = withContext(Dispatchers.IO) {
    // Query all sessions for device
    val sessions = if (roleFilter != null) {
        // Filter by role (per CONTEXT.md: user picks which role's sessions to export)
        forwardingSessionDao.getSessionsForDeviceList(phoneNumber)
            .filter { session ->
                // Match session to paired device role
                val device = deviceDao.getByPhoneNumberAndRole(phoneNumber, roleFilter)
                device != null
            }
    } else {
        forwardingSessionDao.getSessionsForDeviceList(phoneNumber)
    }

    // Fetch messages for each session
    sessions.map { session ->
        val messages = forwardedMessageDao.getMessagesForSessionList(session.id)
        SessionWithMessages(session, messages)
    }
}

data class SessionWithMessages(
    val session: ForwardingSession,
    val messages: List<ForwardedMessage>
)

// In ViewModel
fun exportAllSessions(format: ExportFormat, roleFilter: DeviceRole? = null) {
    viewModelScope.launch(Dispatchers.IO) {
        _isExporting.value = true
        try {
            val data = sessionRepository.getAllSessionsForDeviceWithMessages(phoneNumber, roleFilter)
            val allSessions = data.map { it.session }
            val allMessages = data.flatMap { it.messages }

            // Generate export content
            val content = when (format) {
                ExportFormat.CSV -> ExportFormatter.toCsv(allSessions, allMessages, includeMetadata.value)
                ExportFormat.JSON -> ExportFormatter.toJson(allSessions, allMessages, includeMetadata.value)
                ExportFormat.TXT -> ExportFormatter.toText(allSessions, allMessages, includeMetadata.value)
            }

            // Trigger SAF launcher with generated filename
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val extension = format.extension
            _pendingExportContent.value = PendingExport(
                content = content,
                suggestedFilename = "smscourier_export_${timestamp}.${extension}"
            )
        } catch (e: Exception) {
            _exportResult.value = ExportResult.Error(e.message ?: "Export failed")
        } finally {
            _isExporting.value = false
        }
    }
}
```

### Anti-Patterns to Avoid

- **Don't use WRITE_EXTERNAL_STORAGE permission:** Doesn't work on API 30+, causes permission fragmentation. Use SAF exclusively.
- **Don't use Environment.getExternalStorageDirectory():** Deprecated on API 29+, returns null on API 30+. Use SAF.
- **Don't add external CSV/JSON libraries:** Manual generation is 20 lines, libraries add 200KB+ to APK for zero functional benefit.
- **Don't edit filename in SAF picker:** SAF doesn't support pre-filling editable filename reliably. Pass as suggestion, let user modify if needed.
- **Don't export on main thread:** File I/O and database queries must use Dispatchers.IO to avoid ANR.
- **Don't load all messages in memory for large exports:** For archived devices with 10k+ messages, use chunked queries (500 rows at a time) with streaming write.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| File picker UI | Custom dialog with directory browser | ActivityResultContracts.CreateDocument | Handles platform-specific file pickers (SAF on Android, native picker on ChromeOS), accessibility, scoped storage, permission-free operation |
| CSV escaping logic | Manual quote/comma handling | RFC 4180 pattern with proper escaping | CSV looks simple but has edge cases (quotes in quotes, newlines in fields, Unicode). RFC pattern handles all cases. |
| JSON generation | String concatenation | buildString with proper escaping | JSON escaping has 8 special characters (quote, backslash, newline, etc.), manual approach misses edge cases like control characters |
| Timestamp formatting | Manual date math | SimpleDateFormat with locale | Handles time zones, locales, DST transitions, leap seconds automatically. Custom math breaks on edge cases. |
| Permission handling | Manual runtime permission requests | SAF (no permissions needed) | SAF requires zero permissions for document creation, works across all API levels without branching logic |

**Key insight:** SAF looks like just a file picker, but it handles scoped storage enforcement, permission grants, cross-device file access (Google Drive, OneDrive), remote file systems, and API-level permission fragmentation automatically. Custom implementation would need 500+ lines of branching logic for API 29 vs 30+ vs 33+ permission models. Similarly, CSV escaping looks trivial ("just wrap in quotes") until you hit nested quotes, newlines in content, or Unicode characters.

## Common Pitfalls

### Pitfall 1: Not Testing Export on API 30-32 (Permission Fragmentation)

**What goes wrong:** Export works on Android 14 (API 34) during development, fails with "Permission denied" on Android 11-12 (API 30-32) in production

**Why it happens:** Android 11 (API 30) changed scoped storage rules dramatically. Code using WRITE_EXTERNAL_STORAGE or getExternalFilesDir() works on API 29, fails on 30+. Developers test on latest Android only.

**How to avoid:**
```kotlin
// WRONG - Works on API 29, 33+, fails on 30-32
val file = File(Environment.getExternalStorageDirectory(), "export.csv")
file.writeText(content)

// CORRECT - SAF works on all API levels without permissions
val launcher = rememberLauncherForActivityResult(
    ActivityResultContracts.CreateDocument("text/csv")
) { uri ->
    uri?.let {
        context.contentResolver.openOutputStream(it)?.use { stream ->
            stream.write(content.toByteArray())
        }
    }
}
launcher.launch("export.csv")
```

**Warning signs:** Code checks `Build.VERSION.SDK_INT >= 30`, WRITE_EXTERNAL_STORAGE in AndroidManifest.xml, calls to Environment.getExternalStorageDirectory()

### Pitfall 2: CSV Export Not Escaping Quotes or Newlines

**What goes wrong:** Exported CSV opens in Excel with broken rows - message content containing commas splits into multiple columns, newlines break row alignment

**Why it happens:** Developer thinks "CSV is just comma-separated values", doesn't implement RFC 4180 quote escaping

**How to avoid:**
```kotlin
// WRONG - Breaks on messages with commas or quotes
fun toCsvRow(message: ForwardedMessage): String {
    return "${message.id},${message.senderNumber},${message.messageContent}"
}

// CORRECT - RFC 4180 escaping
fun toCsvField(value: String): String {
    return if (value.contains(',') || value.contains('\n') || value.contains('"')) {
        "\"${value.replace("\"", "\"\"")}\""  // Wrap in quotes, escape quotes
    } else {
        value
    }
}
```

**Warning signs:** CSV fields not wrapped in quotes, no handling of quote characters in content, newlines not escaped

### Pitfall 3: JSON Generation with Unescaped Special Characters

**What goes wrong:** Exported JSON fails to parse in JSON viewers, throws syntax errors on messages containing quotes, backslashes, or newlines

**Why it happens:** Developer uses string interpolation without escaping JSON special characters

**How to avoid:**
```kotlin
// WRONG - Breaks on messages with quotes or backslashes
val json = """{"message": "${message.messageContent}"}"""

// CORRECT - Escape all JSON special characters
fun String.toJsonString(): String {
    return "\"" + this
        .replace("\\", "\\\\")  // Backslash FIRST (order matters!)
        .replace("\"", "\\\"")  // Quote
        .replace("\n", "\\n")   // Newline
        .replace("\r", "\\r")   // Carriage return
        .replace("\t", "\\t")   // Tab
        .replace("\b", "\\b")   // Backspace
        .replace("\u000C", "\\f") // Form feed
        + "\""
}
```

**Warning signs:** JSON built with string interpolation, no handling of backslashes, quotes, or newlines

### Pitfall 4: Exporting on Main Thread Causes ANR

**What goes wrong:** App becomes unresponsive during export of large session history (1000+ messages), "Application Not Responding" dialog shown

**Why it happens:** Export logic queries database and writes file on main thread. Room queries are suspend functions but still do disk I/O.

**How to avoid:**
```kotlin
// WRONG - Blocks UI thread during export
fun exportToFile(uri: Uri, format: ExportFormat) {
    val sessions = runBlocking { sessionRepository.getAllSessions() }  // BAD!
    val content = ExportFormatter.toCsv(sessions, ...)
    context.contentResolver.openOutputStream(uri)?.use {
        it.write(content.toByteArray())
    }
}

// CORRECT - Use Dispatchers.IO for all I/O operations
fun exportToFile(uri: Uri, format: ExportFormat) {
    viewModelScope.launch(Dispatchers.IO) {
        _isExporting.value = true
        try {
            val sessions = sessionRepository.getAllSessions()  // Suspend, runs on IO
            val content = ExportFormatter.toCsv(sessions, ...)
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                stream.write(content.toByteArray())
            }
            _exportResult.value = ExportResult.Success(uri)
        } finally {
            _isExporting.value = false
        }
    }
}
```

**Warning signs:** No Dispatchers.IO in export code, runBlocking usage, direct suspend function calls without launch

### Pitfall 5: Loading All Messages in Memory for Large Exports (OOM)

**What goes wrong:** Export crashes with OutOfMemoryError on archived devices with 10,000+ messages

**Why it happens:** Code queries all sessions and messages at once, loads entire dataset into memory before writing

**How to avoid:**
```kotlin
// WRONG - Loads all messages in memory
fun exportAllSessions(uri: Uri) {
    val sessions = sessionRepository.getAllSessionsList()  // 1000 sessions
    val allMessages = sessions.flatMap {
        messageRepository.getMessagesForSessionList(it.id)  // 10k+ messages in memory!
    }
    val csv = ExportFormatter.toCsv(sessions, allMessages)
    writeToFile(uri, csv)
}

// CORRECT - Stream sessions in chunks, write incrementally
fun exportAllSessions(uri: Uri) {
    context.contentResolver.openOutputStream(uri)?.bufferedWriter().use { writer ->
        // Write CSV header
        writer.write("Session ID,Sender,Timestamp,Message\n")

        // Process sessions in chunks
        val sessions = sessionRepository.getAllSessionsList()
        sessions.chunked(100).forEach { sessionChunk ->
            sessionChunk.forEach { session ->
                val messages = messageRepository.getMessagesForSessionList(session.id)
                messages.forEach { message ->
                    writer.write(toCsvRow(session, message))
                    writer.write("\n")
                }
            }
            writer.flush()  // Flush every 100 sessions
        }
    }
}
```

**Warning signs:** FlatMap with all sessions/messages, no chunking, single buildString for entire export

### Pitfall 6: Not Showing Export Progress or Error Feedback

**What goes wrong:** User taps export, nothing happens for 5 seconds, no indication of success or failure

**Why it happens:** Developer forgets to show loading state or handle error cases

**How to avoid:**
```kotlin
// WRONG - No feedback
Button(onClick = { viewModel.exportToFile(uri, format) }) {
    Text("Export")
}

// CORRECT - Show progress, handle success/error
val isExporting by viewModel.isExporting.collectAsState()
val exportResult by viewModel.exportResult.collectAsState()

Button(
    onClick = { viewModel.exportToFile(uri, format) },
    enabled = !isExporting
) {
    if (isExporting) {
        CircularProgressIndicator(modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
    }
    Text(if (isExporting) "Exporting..." else "Export")
}

// Show snackbar on result
LaunchedEffect(exportResult) {
    when (exportResult) {
        is ExportResult.Success -> snackbarHostState.showSnackbar("Export successful")
        is ExportResult.Error -> snackbarHostState.showSnackbar("Export failed: ${error.message}")
    }
}
```

**Warning signs:** No progress StateFlow in ViewModel, no loading indicator in UI, no error handling

### Pitfall 7: Incorrect Timestamp Formatting for Export

**What goes wrong:** Exported timestamps show in wrong timezone or inconsistent format (CSV uses UTC, JSON uses local, TXT uses 12-hour)

**Why it happens:** Different SimpleDateFormat patterns applied inconsistently across formats

**How to avoid:**
```kotlin
// WRONG - Inconsistent formatting
fun formatCsv(timestamp: Long) = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date(timestamp))  // Locale missing
fun formatJson(timestamp: Long) = timestamp.toString()  // Unix timestamp
fun formatTxt(timestamp: Long) = SimpleDateFormat("MMM d, h:mm a").format(Date(timestamp))  // 12-hour

// CORRECT - Consistent ISO 8601 for CSV/JSON, readable for TXT
object DateTimeFormatters {
    // For CSV/JSON: ISO 8601 with timezone
    fun formatExportTimestampISO(timestampMillis: Long): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US)
        return formatter.format(Date(timestampMillis))
    }

    // For TXT: Local readable format
    fun formatExportTimestamp(timestampMillis: Long): String {
        val formatter = SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault())
        return formatter.format(Date(timestampMillis))
    }

    // For TXT message timestamps: time only
    fun formatExportTime(timestampMillis: Long): String {
        val formatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return formatter.format(Date(timestampMillis))
    }
}
```

**Warning signs:** Multiple inconsistent SimpleDateFormat patterns, no timezone handling, locale not specified

## Code Examples

Verified patterns from official sources:

### Storage Access Framework Integration

```kotlin
// Source: https://developer.android.com/training/data-storage/shared/documents-files
@Composable
fun ExportButton(viewModel: SessionHistoryViewModel) {
    val context = LocalContext.current
    var selectedFormat by remember { mutableStateOf<ExportFormat?>(null) }
    var showFormatPicker by remember { mutableStateOf(false) }

    // Separate launcher for each MIME type (required by SAF)
    val csvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri -> uri?.let { viewModel.writeExportToUri(it) } }

    val jsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let { viewModel.writeExportToUri(it) } }

    val txtLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri -> uri?.let { viewModel.writeExportToUri(it) } }

    // Export button
    IconButton(onClick = { showFormatPicker = true }) {
        Icon(Icons.Default.Download, "Export")
    }

    // Format picker bottom sheet
    if (showFormatPicker) {
        ExportFormatBottomSheet(
            onDismiss = { showFormatPicker = false },
            onFormatSelected = { format ->
                showFormatPicker = false
                viewModel.prepareExport(format)

                // Launch appropriate SAF picker
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                when (format) {
                    ExportFormat.CSV -> csvLauncher.launch("smscourier_export_$timestamp.csv")
                    ExportFormat.JSON -> jsonLauncher.launch("smscourier_export_$timestamp.json")
                    ExportFormat.TXT -> txtLauncher.launch("smscourier_export_$timestamp.txt")
                }
            }
        )
    }
}

// In ViewModel
private var pendingExportContent: String? = null

fun prepareExport(format: ExportFormat) {
    viewModelScope.launch(Dispatchers.IO) {
        _isExporting.value = true
        try {
            val sessions = sessionRepository.getSessionsForDeviceList(phoneNumber)
            val messages = sessions.flatMap { messageRepository.getMessagesForSessionList(it.id) }

            pendingExportContent = when (format) {
                ExportFormat.CSV -> ExportFormatter.toCsv(sessions, messages, includeMetadata.value)
                ExportFormat.JSON -> ExportFormatter.toJson(sessions, messages, includeMetadata.value)
                ExportFormat.TXT -> ExportFormatter.toText(sessions, messages, includeMetadata.value)
            }
        } catch (e: Exception) {
            _exportResult.value = ExportResult.Error(e.message ?: "Failed to prepare export")
            _isExporting.value = false
        }
    }
}

fun writeExportToUri(uri: Uri) {
    viewModelScope.launch(Dispatchers.IO) {
        try {
            val content = pendingExportContent ?: throw IllegalStateException("No export content prepared")

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(content.toByteArray(Charsets.UTF_8))
            }

            _exportResult.value = ExportResult.Success(uri)
        } catch (e: Exception) {
            _exportResult.value = ExportResult.Error(e.message ?: "Export failed")
        } finally {
            pendingExportContent = null
            _isExporting.value = false
        }
    }
}
```

### Format Picker Bottom Sheet

```kotlin
// Source: Material 3 ModalBottomSheet patterns
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportFormatBottomSheet(
    onDismiss: () -> Unit,
    onFormatSelected: (ExportFormat) -> Unit,
    modifier: Modifier = Modifier
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Export Format",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(8.dp))

            // CSV option
            FormatOption(
                format = ExportFormat.CSV,
                title = "CSV (Spreadsheet)",
                description = "Opens in Excel, Google Sheets, and other spreadsheet apps",
                onClick = { onFormatSelected(ExportFormat.CSV) }
            )

            // JSON option
            FormatOption(
                format = ExportFormat.JSON,
                title = "JSON (Structured)",
                description = "Machine-readable format for data analysis and processing",
                onClick = { onFormatSelected(ExportFormat.JSON) }
            )

            // TXT option
            FormatOption(
                format = ExportFormat.TXT,
                title = "Plain Text (Chat Log)",
                description = "Human-readable conversation transcript",
                onClick = { onFormatSelected(ExportFormat.TXT) }
            )

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun FormatOption(
    format: ExportFormat,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

enum class ExportFormat(val extension: String, val mimeType: String) {
    CSV("csv", "text/csv"),
    JSON("json", "application/json"),
    TXT("txt", "text/plain")
}
```

### Complete CSV Export with RFC 4180 Compliance

```kotlin
// Source: RFC 4180 specification
object ExportFormatter {

    fun toCsv(
        sessions: List<ForwardingSession>,
        messages: List<ForwardedMessage>,
        includeMetadata: Boolean
    ): String = buildString {
        // Metadata header (commented lines)
        if (includeMetadata) {
            appendLine("# SMS Courier Export")
            appendLine("# Generated: ${DateTimeFormatters.formatExportTimestampISO(System.currentTimeMillis())}")
            appendLine("# Total Sessions: ${sessions.size}")
            appendLine("# Total Messages: ${messages.size}")
            appendLine()
        }

        // CSV header row
        appendLine("Session ID,Session Start,Session Duration,Is Active,Message ID,Sender Number,Message Timestamp,Message Content")

        // Group messages by session
        val messagesBySession = messages.groupBy { it.sessionId }

        sessions.forEach { session ->
            val sessionMessages = messagesBySession[session.id] ?: emptyList()

            if (sessionMessages.isEmpty()) {
                // Session with no messages
                appendCsvRow(
                    session.id.toString(),
                    DateTimeFormatters.formatExportTimestampISO(session.startedAt),
                    session.durationMinutes.toString(),
                    session.isActive.toString(),
                    "",  // No message ID
                    "",  // No sender
                    "",  // No message timestamp
                    ""   // No content
                )
            } else {
                // Session with messages (one row per message)
                sessionMessages.forEach { message ->
                    appendCsvRow(
                        session.id.toString(),
                        DateTimeFormatters.formatExportTimestampISO(session.startedAt),
                        session.durationMinutes.toString(),
                        session.isActive.toString(),
                        message.id.toString(),
                        message.senderNumber,
                        DateTimeFormatters.formatExportTimestampISO(message.timestamp),
                        message.messageContent
                    )
                }
            }
        }
    }

    private fun StringBuilder.appendCsvRow(vararg values: String) {
        appendLine(values.joinToString(",") { it.toCsvField() })
    }

    private fun String.toCsvField(): String {
        // RFC 4180: Field must be wrapped in quotes if it contains:
        // - Comma
        // - Newline (CRLF or LF)
        // - Quote character
        // Quotes inside field are escaped by doubling them
        return if (this.contains(',') || this.contains('\n') || this.contains('\r') || this.contains('"')) {
            "\"${this.replace("\"", "\"\"")}\""
        } else {
            this
        }
    }
}
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| WRITE_EXTERNAL_STORAGE permission | Storage Access Framework (SAF) | Android 11 (API 30, 2020) | SAF eliminates permission requests, works across API levels without branching |
| File picker with manual directory navigation | ACTION_CREATE_DOCUMENT intent | Android 4.4 (API 19, 2013) | Platform handles picker UI, supports cloud storage (Drive, OneDrive) automatically |
| External CSV libraries (OpenCSV, SuperCSV) | Manual RFC 4180 implementation | N/A | External libraries add 200KB+ to APK for 10 lines of code, no functional benefit for simple use case |
| External JSON libraries (Gson, Moshi) | Manual JSON building with escaping | N/A | Simple nested structure doesn't justify reflection overhead or dependency weight |
| ActivityResultContracts | startActivityForResult (deprecated API 30) | Activity Result APIs (2020) | Contracts provide type-safe result handling, work with Compose and Fragment KTX |

**Deprecated/outdated:**
- **WRITE_EXTERNAL_STORAGE permission:** Doesn't work on API 30+, causes permission fragmentation. SAF requires zero permissions.
- **Environment.getExternalStorageDirectory():** Returns null on API 30+, deprecated. Use SAF for file creation.
- **MediaStore for document export:** More complex than SAF, requires API-level branching. SAF works universally.
- **startActivityForResult:** Deprecated in API 30. Use ActivityResultContracts with rememberLauncherForActivityResult.

## Open Questions

Things that couldn't be fully resolved:

1. **Chunked export for very large datasets (10k+ messages)**
   - What we know: Archive Management screen can have years of history (10,000+ messages), loading all in memory risks OutOfMemoryError
   - What's unclear: Should export use streaming (write incrementally as data is fetched), or chunked loading (fetch 500 rows, format, fetch next 500)?
   - Recommendation: Implement streaming write for CSV/TXT (line-by-line), chunked loading for JSON (need complete structure before writing). Add warning dialog if export > 5000 messages.

2. **Metadata header toggle persistence**
   - What we know: CONTEXT.md says "User toggle for including metadata header"
   - What's unclear: Should toggle state persist across app restarts (DataStore), or reset to default (remember state)?
   - Recommendation: Don't persist - keep as ephemeral UI state with remember. Metadata header is a per-export decision, not a global preference. Avoids adding DataStore key.

3. **Export filename sanitization for special characters**
   - What we know: Filename format is `smscourier_export_YYYYMMDD_HHMMSS.{ext}`, phone numbers in bulk export need sanitization
   - What's unclear: Should phone numbers in filename use URLEncoder (converts + to %2B) or manual replacement (+ to _, remove parentheses)?
   - Recommendation: Use manual sanitization for readability: replace `+` with `plus`, remove spaces/parentheses. Example: `smscourier_export_plus1234567890_20260206_143022.csv`. URLEncoder creates ugly filenames.

4. **Export from message detail bottom sheet**
   - What we know: CONTEXT.md says "Single-session export: Message detail bottom sheet button AND session card long-press menu"
   - What's unclear: Should message detail bottom sheet show export button (adds clutter), or only session card long-press?
   - Recommendation: Both. Bottom sheet gets export IconButton in top bar (next to close), session cards get long-press context menu with "Export session" option. Users expect export in detail view.

5. **Archived device role ambiguity**
   - What we know: CONTEXT.md says "Per-device export separates by role - user picks which role's sessions to export"
   - What's unclear: If device was paired bidirectionally (both SOURCE and TARGET), should export show two separate options or combined view?
   - Recommendation: Show separate export options with role labels: "Export as Source device" / "Export as Target device" / "Export all sessions". Most users won't have bidirectional pairing, this handles edge case clearly.

## Sources

### Primary (HIGH confidence)
- [Storage Access Framework](https://developer.android.com/training/data-storage/shared/documents-files) - Official Android guide, ACTION_CREATE_DOCUMENT patterns
- [Activity Result APIs](https://developer.android.com/training/basics/intents/result) - Official guide for ActivityResultContracts
- [Scoped Storage Overview](https://source.android.com/docs/core/storage/scoped) - Android Open Source Project, API 30+ storage changes
- [RFC 4180 CSV Specification](https://www.ietf.org/rfc/rfc4180.txt) - Official CSV format specification
- [JSON Specification (ECMA-404)](https://www.json.org/json-en.html) - Official JSON format specification
- [SimpleDateFormat API](https://developer.android.com/reference/java/text/SimpleDateFormat) - Official Android reference for date formatting
- Existing codebase: SessionHistoryScreen.kt, ArchiveManagementScreen.kt, DateTimeFormatters.kt, SessionHistoryViewModel.kt, ForwardingSessionRepository.kt

### Secondary (MEDIUM confidence)
- [Storage Use Cases Guide](https://developer.android.com/training/data-storage/use-cases) - Official guide for when to use SAF vs other storage APIs
- [Content URI Best Practices](https://developer.android.com/training/secure-file-sharing/setup-sharing) - Official guide for working with content:// URIs
- [CSV Best Practices (Medium)](https://medium.com/@kasperfred/5-ways-to-safely-handle-csv-files-that-have-escaped-quotes-or-commas-in-their-values-dffe2e2f2b5a) - Community article on CSV edge cases
- [JSON Encoding in Kotlin (ProAndroidDev)](https://proandroiddev.com/json-encoding-in-kotlin-4e048d98df82) - Community guide on manual JSON generation

### Tertiary (LOW confidence)
- [OpenCSV Library](http://opencsv.sourceforge.net/) - Third-party CSV library (not recommended, adds dependency)
- [kotlinx-serialization-csv](https://github.com/doyaaaaaken/kotlin-csv) - Third-party CSV library (not recommended, external dependency)

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - All patterns verified with official Android docs, no external dependencies needed
- Architecture: HIGH - SAF patterns confirmed in official guides, manual CSV/JSON generation well-documented
- Pitfalls: HIGH - Based on official Android scoped storage migration guide and verified community issues

**Research date:** 2026-02-06
**Valid until:** 2026-03-06 (30 days for stable APIs - SAF, Activity Result APIs, File I/O are mature)
