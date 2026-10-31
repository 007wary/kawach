package dev.wary.kawach.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Safe / caution colours that Material's scheme doesn't cover. Danger uses `colorScheme.error`. */
@Immutable
data class StatusColors(
    val ok: Color,
    val okContainer: Color,
    val warn: Color,
    val warnContainer: Color,
)

private val LightStatus = StatusColors(
    ok = Color(0xFF1C8648), okContainer = Color(0xFFDFF2E6),
    warn = Color(0xFF8A5A00), warnContainer = Color(0xFFFBF0D6),
)
private val DarkStatus = StatusColors(
    ok = Color(0xFF5BD08B), okContainer = Color(0xFF11301E),
    warn = Color(0xFFF2B53C), warnContainer = Color(0xFF36290C),
)

val LocalStatusColors = staticCompositionLocalOf { LightStatus }

private val LightScheme = lightColorScheme(
    primary = Color(0xFF0E6B67), onPrimary = Color.White,
    primaryContainer = Color(0xFFDDF0EE), onPrimaryContainer = Color(0xFF00201E),
    secondary = Color(0xFF4A6361), onSecondary = Color.White,
    secondaryContainer = Color(0xFFDDF0EE), onSecondaryContainer = Color(0xFF00201E),
    surfaceContainer = Color(0xFFFFFFFF),
    error = Color(0xFFC23628), onError = Color.White,
    errorContainer = Color(0xFFFBE4E1), onErrorContainer = Color(0xFF410002),
    background = Color(0xFFF2F5F8), onBackground = Color(0xFF13202D),
    surface = Color(0xFFF2F5F8), onSurface = Color(0xFF13202D),
    surfaceVariant = Color(0xFFE3E9EF), onSurfaceVariant = Color(0xFF566576),
    outline = Color(0xFFB7C2CD), outlineVariant = Color(0xFFD8DFE7),
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFF45BDB4), onPrimary = Color(0xFF00201E),
    primaryContainer = Color(0xFF12312F), onPrimaryContainer = Color(0xFFBDEDE8),
    secondary = Color(0xFFB0CCC9), onSecondary = Color(0xFF1B3533),
    secondaryContainer = Color(0xFF12312F), onSecondaryContainer = Color(0xFFBDEDE8),
    surfaceContainer = Color(0xFF151F29),
    error = Color(0xFFFF7A6B), onError = Color(0xFF3B0904),
    errorContainer = Color(0xFF3B1915), onErrorContainer = Color(0xFFFFDAD5),
    background = Color(0xFF0D131A), onBackground = Color(0xFFE5EBF1),
    surface = Color(0xFF0D131A), onSurface = Color(0xFFE5EBF1),
    surfaceVariant = Color(0xFF1B2733), onSurfaceVariant = Color(0xFF93A2B2),
    outline = Color(0xFF3A4856), outlineVariant = Color(0xFF283543),
)

// Larger than Material defaults: many users are elders.
private val Base = Typography()
private val KawachTypography = Typography(
    headlineMedium = Base.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
    headlineSmall = Base.headlineSmall.copy(fontWeight = FontWeight.Bold),
    titleLarge = Base.titleLarge.copy(fontWeight = FontWeight.Bold, fontSize = 24.sp),
    titleMedium = Base.titleMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 18.sp),
    bodyLarge = Base.bodyLarge.copy(fontSize = 18.sp, lineHeight = 26.sp),
    bodyMedium = Base.bodyMedium.copy(fontSize = 16.sp, lineHeight = 23.sp),
    labelLarge = Base.labelLarge.copy(fontSize = 17.sp, fontWeight = FontWeight.SemiBold),
)

@Composable
fun KawachTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    CompositionLocalProvider(LocalStatusColors provides if (dark) DarkStatus else LightStatus) {
        MaterialTheme(
            colorScheme = if (dark) DarkScheme else LightScheme,
            typography = KawachTypography,
            content = content,
        )
    }
}
