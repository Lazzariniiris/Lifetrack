package com.lifetrack.app.presentation.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.lifetrack.app.domain.model.ThemeMode

private val LightColors = lightColorScheme(
    primary = Color(0xFF2F6B4F),
    secondary = Color(0xFF53634F),
    tertiary = Color(0xFF38656F),
    surface = Color(0xFFF8FAF5),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF96D5AE),
    secondary = Color(0xFFB9CBB1),
    tertiary = Color(0xFFA3CFDA),
)

@Composable
fun LifeTrackTheme(themeMode: ThemeMode, content: @Composable () -> Unit) {
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val context = LocalContext.current
    val colors = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = colors, content = content)
}
