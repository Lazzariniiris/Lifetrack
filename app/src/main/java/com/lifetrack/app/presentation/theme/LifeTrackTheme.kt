package com.lifetrack.app.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.lifetrack.app.domain.model.ThemeMode

private val LightColors = lightColorScheme(
    primary = Color(0xFF005E68),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFB9EBEF),
    onPrimaryContainer = Color(0xFF00363C),
    secondary = Color(0xFF006C62),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFA8F2E2),
    onSecondaryContainer = Color(0xFF00201C),
    tertiary = Color(0xFF287364),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFB7ECDD),
    onTertiaryContainer = Color(0xFF08372F),
    background = Color(0xFFF4F8F7),
    onBackground = Color(0xFF17211F),
    surface = Color(0xFFF9FCFB),
    onSurface = Color(0xFF17211F),
    surfaceVariant = Color(0xFFE2ECEA),
    onSurfaceVariant = Color(0xFF53615E),
    outline = Color(0xFF71807D),
    outlineVariant = Color(0xFFC1CDC9),
    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFFFDAD6),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF65D5DF),
    onPrimary = Color(0xFF00363C),
    primaryContainer = Color(0xFF003F46),
    onPrimaryContainer = Color(0xFFB9EBEF),
    secondary = Color(0xFF61D9C7),
    onSecondary = Color(0xFF003731),
    secondaryContainer = Color(0xFF005047),
    onSecondaryContainer = Color(0xFFA8F2E2),
    tertiary = Color(0xFF91D7C6),
    onTertiary = Color(0xFF00382F),
    tertiaryContainer = Color(0xFF164F43),
    onTertiaryContainer = Color(0xFFB7ECDD),
    background = Color(0xFF0C1514),
    onBackground = Color(0xFFDDE7E4),
    surface = Color(0xFF121E1C),
    onSurface = Color(0xFFDDE7E4),
    surfaceVariant = Color(0xFF23312E),
    onSurfaceVariant = Color(0xFFAAB8B4),
    outline = Color(0xFF899692),
    outlineVariant = Color(0xFF3D4A47),
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF93000A),
)

object LifeTrackColors {
    val Water = Color(0xFF00A6A6)
    val Sleep = Color(0xFF4F7FC7)
    val Habits = Color(0xFF2D907B)
    val Meals = Color(0xFF39785D)
}

private val LifeTrackShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
)

private val LifeTrackTypography = Typography(
    displaySmall = TextStyle(fontSize = 36.sp, lineHeight = 42.sp, fontWeight = FontWeight.SemiBold),
    headlineMedium = TextStyle(fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.SemiBold),
    headlineSmall = TextStyle(fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
)

@Composable
fun LifeTrackTheme(themeMode: ThemeMode, content: @Composable () -> Unit) {
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colors = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val controller = WindowCompat.getInsetsController((view.context as android.app.Activity).window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }
    MaterialTheme(colorScheme = colors, typography = LifeTrackTypography, shapes = LifeTrackShapes, content = content)
}
