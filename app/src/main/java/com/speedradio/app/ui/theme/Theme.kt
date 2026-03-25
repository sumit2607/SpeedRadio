package com.speedradio.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFE040FB), // Vibrant Purple
    onPrimary = Color(0xFF1A001F),
    primaryContainer = Color(0xFF5A0070),
    onPrimaryContainer = Color(0xFFF1B4FF),
    secondary = Color(0xFF00E5FF), // Cyber Cyan
    onSecondary = Color(0xFF001F24),
    secondaryContainer = Color(0xFF004E5A),
    onSecondaryContainer = Color(0xFFABEEFF),
    tertiary = Color(0xFFFF6D00), // Action Orange
    background = Color(0xFF0E0E12), // Deep Charcoal
    onBackground = Color(0xFFEFEFEF),
    surface = Color(0xFF1A1A22), 
    onSurface = Color(0xFFF5F5F7),
    surfaceVariant = Color(0xFF26262F),
    onSurfaceVariant = Color(0xFFCAC4D0),
    error = Color(0xFFCF6679),
    outline = Color(0xFF3A3A45)
)

val PremiumShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(32.dp),
    extraLarge = RoundedCornerShape(48.dp)
)

@Composable
fun SpeedRadioTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        shapes = PremiumShapes,
        content = content
    )
}
