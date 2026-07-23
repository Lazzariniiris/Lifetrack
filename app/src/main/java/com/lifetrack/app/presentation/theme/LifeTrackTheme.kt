package com.lifetrack.app.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lifetrack.app.domain.model.ThemeMode

private val LightColors = lightColorScheme(
    primary = Color(0xFF008D7D),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFB6F6E6),
    onPrimaryContainer = Color(0xFF003D35),
    secondary = Color(0xFFB63E78),
    secondaryContainer = Color(0xFFFFD9E5),
    tertiary = Color(0xFF187CA7),
    tertiaryContainer = Color(0xFFC8EDFF),
    surface = Color(0xFFFFF9FC),
    surfaceVariant = Color(0xFFF1E5EC),
    outline = Color(0xFF85737C),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF70E5D1),
    onPrimary = Color(0xFF003F37),
    primaryContainer = Color(0xFF005B50),
    secondary = Color(0xFFFFB0C9),
    secondaryContainer = Color(0xFF8D1D5A),
    tertiary = Color(0xFF7DD3FF),
    tertiaryContainer = Color(0xFF005A7D),
    surface = Color(0xFF10111C),
    surfaceVariant = Color(0xFF282734),
)

private val LifeTrackShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(36.dp),
)

private val LifeTrackTypography = Typography(
    headlineMedium = TextStyle(fontSize = 30.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold),
    titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold),
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
    MaterialTheme(colorScheme = colors, typography = LifeTrackTypography, shapes = LifeTrackShapes, content = content)
}
