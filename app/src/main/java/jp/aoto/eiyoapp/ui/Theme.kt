package jp.aoto.eiyoapp.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Paper = Color(0xFF141311)
val Surface = Color(0xFF1E1D1A)
val Ink = Color(0xFFF2EFE9)
val Muted = Color(0xFF99958E)
val Rule = Color(0xFF3B3833)
val Accent = Color(0xFFE1AD66)

private val colors = darkColorScheme(
    primary = Accent, onPrimary = Ink, background = Paper, onBackground = Ink,
    surface = Paper, onSurface = Ink, outline = Rule, secondary = Muted,
)

@Composable
fun EiyoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = colors,
        typography = MaterialTheme.typography.copy(
            headlineLarge = TextStyle(fontFamily=FontFamily.Serif, fontSize=30.sp, fontWeight=FontWeight.Normal),
            headlineMedium = TextStyle(fontFamily=FontFamily.Serif, fontSize=24.sp, fontWeight=FontWeight.Normal),
            titleLarge = TextStyle(fontFamily=FontFamily.Serif, fontSize=20.sp),
            bodyLarge = TextStyle(fontFamily=FontFamily.Serif, fontSize=16.sp),
            bodyMedium = TextStyle(fontFamily=FontFamily.Serif, fontSize=14.sp),
            labelLarge = TextStyle(fontFamily=FontFamily.Serif, fontSize=14.sp),
        ),
        content = content,
    )
}
