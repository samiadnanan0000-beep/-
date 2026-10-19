package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = OneUIBluePrimary,
    onPrimary = Color.White,
    primaryContainer = OneUIDarkSurfaceVariant,
    onPrimaryContainer = Color.White,
    secondary = GoldAmber,
    onSecondary = Color.Black,
    secondaryContainer = OneUIDarkSurfaceVariant,
    background = OneUIDarkBg,
    onBackground = OneUIDarkTextPrimary,
    surface = OneUIDarkSurface,
    onSurface = OneUIDarkTextPrimary,
    surfaceVariant = OneUIDarkSurfaceVariant,
    onSurfaceVariant = OneUIDarkTextSecondary,
    outline = Color(0xFF374151)
)

private val LightColorScheme = lightColorScheme(
    primary = OneUIBluePrimary,
    onPrimary = Color.White,
    primaryContainer = OneUIBlueLight,
    onPrimaryContainer = OneUIBlueDark,
    secondary = GoldAmber,
    onSecondary = Color.White,
    secondaryContainer = GoldContainer,
    background = OneUIGrayBg,
    onBackground = OneUITextPrimary,
    surface = OneUISurfaceWhite,
    onSurface = OneUITextPrimary,
    surfaceVariant = OneUISurfaceVariant,
    onSurfaceVariant = OneUITextSecondary,
    outline = Color(0xFFDCE2EC)
)

val OneUIShapes = Shapes(
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = OneUIShapes,
        content = content
    )
}
