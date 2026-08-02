package com.claudettewest.offlinebookshelf.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Platinum = Color(0xFFE9E4DF)
val Silver = Color(0xFFBDA9A4)
val TaupeGray = Color(0xFF958893)
val Charcoal = Color(0xFF494C60)
val RussianViolet = Color(0xFF1B1745)

private val LightColors = lightColorScheme(primary = RussianViolet, onPrimary = Platinum, secondary = Charcoal, tertiary = TaupeGray, background = Platinum, surface = Color(0xFFF8F5F2), surfaceVariant = Silver.copy(alpha = .35f), onBackground = RussianViolet, onSurface = RussianViolet)
private val DarkColors = darkColorScheme(primary = Silver, onPrimary = RussianViolet, secondary = TaupeGray, background = RussianViolet, surface = Charcoal, onBackground = Platinum, onSurface = Platinum)

@Composable fun BookshelfTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors, typography = Typography(), content = content)
}
