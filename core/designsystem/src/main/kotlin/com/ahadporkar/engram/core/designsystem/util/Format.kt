package com.ahadporkar.engram.core.designsystem.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.ahadporkar.engram.core.designsystem.R
import java.text.DecimalFormat
import kotlin.math.roundToInt

/** Compact interval label such as "10m", "3d", "2.5mo" — shown on the rating buttons. */
@Composable
fun intervalLabel(seconds: Long): String {
    val minutes = seconds / 60.0
    val hours = minutes / 60.0
    val days = hours / 24.0
    val oneDecimal = DecimalFormat("0.#")
    return when {
        minutes < 60 -> stringResource(R.string.interval_minutes, minutes.roundToInt().coerceAtLeast(1))
        hours < 24 -> stringResource(R.string.interval_hours, hours.roundToInt())
        days < 31 -> stringResource(R.string.interval_days, days.roundToInt())
        days < 365 -> stringResource(R.string.interval_months, oneDecimal.format(days / 30.44))
        else -> stringResource(R.string.interval_years, oneDecimal.format(days / 365.25))
    }
}

/** "87 %" style percentage. */
fun percent(value: Double?): String = value?.let { "${(it * 100).roundToInt()} %" } ?: "–"

/** "12 min" / "45 s" style duration for study time. */
fun shortDuration(millis: Long): String {
    val totalSeconds = millis / 1000
    return when {
        totalSeconds < 60 -> "$totalSeconds s"
        totalSeconds < 3600 -> "${totalSeconds / 60} min"
        else -> "${totalSeconds / 3600} h ${totalSeconds % 3600 / 60} min"
    }
}
