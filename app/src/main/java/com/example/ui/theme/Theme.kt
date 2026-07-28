package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape

private val DarkColorScheme = darkColorScheme(
    primary = BluePrimaryContainer,
    onPrimary = BlueOnPrimaryContainer,
    primaryContainer = BluePrimary,
    onPrimaryContainer = Color.White,
    secondary = SlateSecondary,
    onSecondary = DarkOnSurface,
    secondaryContainer = DarkSurfaceVariant,
    background = AIInsightDarkNavy,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = SlateSecondaryContainer,
    outline = OutlineDarkGrey,
    outlineVariant = OutlineVariantLight
)

private val LightColorScheme = lightColorScheme(
    primary = BluePrimary,
    onPrimary = Color.White,
    primaryContainer = BluePrimaryContainer,
    onPrimaryContainer = BlueOnPrimaryContainer,
    secondary = SlateSecondary,
    onSecondary = Color.White,
    secondaryContainer = SlateSecondaryContainer,
    background = MinimalBackground,
    onBackground = MinimalOnSurface,
    surface = MinimalSurface,
    onSurface = MinimalOnSurface,
    surfaceVariant = MinimalSurfaceVariant,
    onSurfaceVariant = MinimalOnSurfaceVariant,
    outline = OutlineDarkGrey,
    outlineVariant = OutlineVariantLight
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Allow dynamic color support by default to support wallpaper extraction
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

/**
 * Shared high-end Glassmorphism modifier that applies a premium frosted glass appearance.
 * Safe to use with child text components as it does not blur inner layout content,
 * instead using premium vertical and linear translucent gradients.
 */
fun Modifier.glassmorphism(
    shape: Shape = RoundedCornerShape(16.dp),
    containerColor: Color? = null,
    borderColor: Color? = null
): Modifier = composed {
    val isDark = isSystemInDarkTheme()
    val finalContainer = containerColor ?: if (isDark) GlassContainerDark else GlassContainerLight
    val finalBorder = borderColor ?: if (isDark) GlassBorderDark else GlassBorderLight

    this.then(
        Modifier
            .clip(shape)
            .background(finalContainer)
            .border(
                width = 1.dp,
                color = finalBorder,
                shape = shape
            )
    )
}

