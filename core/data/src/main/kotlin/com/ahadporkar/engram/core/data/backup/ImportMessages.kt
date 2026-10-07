package com.ahadporkar.engram.core.data.backup

import androidx.annotation.StringRes
import com.ahadporkar.engram.core.data.R

/** User-facing message for an import failure. */
@StringRes
fun Throwable.importErrorMessage(): Int = when ((this as? ImportException)?.reason) {
    ImportException.Reason.UNREADABLE -> R.string.import_error_unreadable
    ImportException.Reason.NOT_A_BACKUP -> R.string.import_error_not_a_backup
    ImportException.Reason.NEWER_VERSION -> R.string.import_error_newer_version
    ImportException.Reason.NOT_A_PROJECT_SHACO_DATABASE -> R.string.import_error_not_project_shaco
    ImportException.Reason.NO_WORDS -> R.string.import_error_no_words
    null -> R.string.import_error_unknown
}
