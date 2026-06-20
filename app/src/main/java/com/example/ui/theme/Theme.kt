package com.example.ui.theme

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

val PolishPrimary = Color(0xFF0061A4)
val PolishPrimaryContainer = Color(0xFFD3E4FF)
val PolishOnPrimaryContainer = Color(0xFF001D36)
val PolishBackground = Color(0xFFF7F9FC)
val PolishSurface = Color(0xFFFFFFFF)
val PolishOnBackground = Color(0xFF1A1C1E)
val PolishOnSurface = Color(0xFF1A1C1E)
val PolishSecondaryContainer = Color(0xFFE1E2EC)
val PolishOnSecondaryContainer = Color(0xFF44474E)

private val DarkColorScheme =
  darkColorScheme(
    primary = Color(0xFF9ECAFF),
    primaryContainer = Color(0xFF00497E),
    onPrimaryContainer = Color(0xFFD3E4FF),
    background = Color(0xFF1A1C1E),
    surface = Color(0xFF1F2025),
    onBackground = Color(0xFFE1E2EC),
    onSurface = Color(0xFFE1E2EC),
    secondaryContainer = Color(0xFF43475E),
    onSecondaryContainer = Color(0xFFE1E2EC)
  )

private val LightColorScheme =
  lightColorScheme(
    primary = PolishPrimary,
    primaryContainer = PolishPrimaryContainer,
    onPrimaryContainer = PolishOnPrimaryContainer,
    background = PolishBackground,
    surface = PolishSurface,
    onBackground = PolishOnBackground,
    onSurface = PolishOnSurface,
    secondaryContainer = PolishSecondaryContainer,
    onSecondaryContainer = PolishOnSecondaryContainer,
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
