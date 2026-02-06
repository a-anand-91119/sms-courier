package dev.notyouraverage.smscourier.export

/**
 * Represents the state of an export operation for UI feedback.
 */
sealed class ExportState {
    /** No export in progress */
    data object Idle : ExportState()

    /** Export is being prepared (loading data) */
    data object Loading : ExportState()

    /** Export completed successfully */
    data class Success(val fileName: String) : ExportState()

    /** Export failed with error */
    data class Error(val message: String) : ExportState()
}
