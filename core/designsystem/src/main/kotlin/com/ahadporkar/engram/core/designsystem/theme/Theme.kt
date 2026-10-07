package com.ahadporkar.engram.core.designsystem.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ahadporkar.engram.core.model.ThemeMode

// "Petrol & amber" palette: calm, focused primary; warm accent for streaks and goals.
private val LightColors = lightColorScheme(
    primary = Color(0xFF00687A),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFADECFF),
    onPrimaryContainer = Color(0xFF001F26),
    secondary = Color(0xFF8B5000),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFDCBE),
    onSecondaryContainer = Color(0xFF2C1600),
    tertiary = Color(0xFF6A4FA3),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFEBDDFF),
    onTertiaryContainer = Color(0xFF250059),
    background = Color(0xFFF6FAFC),
    onBackground = Color(0xFF171C1F),
    surface = Color(0xFFF6FAFC),
    onSurface = Color(0xFF171C1F),
    surfaceVariant = Color(0xFFDBE4E8),
    onSurfaceVariant = Color(0xFF3F484C),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF0F4F6),
    surfaceContainer = Color(0xFFEAEEF0),
    surfaceContainerHigh = Color(0xFFE4E9EB),
    surfaceContainerHighest = Color(0xFFDFE3E5),
    outline = Color(0xFF6F797C),
    outlineVariant = Color(0xFFBFC8CC),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF7FD3E8),
    onPrimary = Color(0xFF003640),
    primaryContainer = Color(0xFF004E5C),
    onPrimaryContainer = Color(0xFFADECFF),
    secondary = Color(0xFFFFB870),
    onSecondary = Color(0xFF4A2800),
    secondaryContainer = Color(0xFF6A3B00),
    onSecondaryContainer = Color(0xFFFFDCBE),
    tertiary = Color(0xFFD3BBFF),
    onTertiary = Color(0xFF3A1A71),
    tertiaryContainer = Color(0xFF51368A),
    onTertiaryContainer = Color(0xFFEBDDFF),
    background = Color(0xFF0F1416),
    onBackground = Color(0xFFDEE3E6),
    surface = Color(0xFF0F1416),
    onSurface = Color(0xFFDEE3E6),
    surfaceVariant = Color(0xFF3F484C),
    onSurfaceVariant = Color(0xFFBFC8CC),
    surfaceContainerLowest = Color(0xFF0A0F11),
    surfaceContainerLow = Color(0xFF171C1F),
    surfaceContainer = Color(0xFF1B2023),
    surfaceContainerHigh = Color(0xFF252B2D),
    surfaceContainerHighest = Color(0xFF303638),
    outline = Color(0xFF899296),
    outlineVariant = Color(0xFF3F484C),
)

/** Semantic colours that Material 3 does not define. */
@Immutable
data class EngramColors(
    val again: Color,
    val hard: Color,
    val good: Color,
    val easy: Color,
    val newCards: Color,
    val learningCards: Color,
    val reviewCards: Color,
    val correctContainer: Color,
    val onCorrectContainer: Color,
    val incorrectContainer: Color,
    val onIncorrectContainer: Color,
    val streak: Color,
)

private val LightEngramColors = EngramColors(
    again = Color(0xFFC62828),
    hard = Color(0xFFB26A00),
    good = Color(0xFF2E7D32),
    easy = Color(0xFF1565C0),
    newCards = Color(0xFF1565C0),
    learningCards = Color(0xFFB26A00),
    reviewCards = Color(0xFF2E7D32),
    correctContainer = Color(0xFFD7F5DA),
    onCorrectContainer = Color(0xFF0B3D12),
    incorrectContainer = Color(0xFFFFDAD6),
    onIncorrectContainer = Color(0xFF410002),
    streak = Color(0xFFE65100),
)

private val DarkEngramColors = EngramColors(
    again = Color(0xFFFF8A80),
    hard = Color(0xFFFFB74D),
    good = Color(0xFF81C784),
    easy = Color(0xFF90CAF9),
    newCards = Color(0xFF90CAF9),
    learningCards = Color(0xFFFFB74D),
    reviewCards = Color(0xFF81C784),
    correctContainer = Color(0xFF1E3B22),
    onCorrectContainer = Color(0xFFC8F0CC),
    incorrectContainer = Color(0xFF4A1B17),
    onIncorrectContainer = Color(0xFFFFDAD6),
    streak = Color(0xFFFFAB40),
)

val LocalEngramColors = staticCompositionLocalOf { LightEngramColors }

private val EngramTypography = Typography().let { base ->
    base.copy(
        displayMedium = base.displayMedium.copy(fontWeight = FontWeight.SemiBold),
        displaySmall = base.displaySmall.copy(fontWeight = FontWeight.SemiBold),
        headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
        titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.2.sp),
    )
}

private val EngramShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun EngramTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colorScheme: ColorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        dark -> DarkColors
        else -> LightColors
    }
    CompositionLocalProvider(LocalEngramColors provides if (dark) DarkEngramColors else LightEngramColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = EngramTypography,
            shapes = EngramShapes,
            content = content,
        )
    }
}

/** Shortcut: `EngramTheme.colors.good`. */
object EngramTheme {
    val colors: EngramColors
        @Composable get() = LocalEngramColors.current
}
