package com.example.shifthero.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = ShiftYellow,
    onPrimary = Carbon950,
    primaryContainer = Color(0xFF4A3900),
    onPrimaryContainer = ShiftYellowSoft,
    secondary = Steel300,
    onSecondary = Carbon950,
    secondaryContainer = Carbon700,
    onSecondaryContainer = Steel200,
    tertiary = IndustrialOrange,
    onTertiary = Carbon950,
    tertiaryContainer = Color(0xFF5A2B00),
    onTertiaryContainer = Color(0xFFFFD0A3),
    background = Carbon950,
    onBackground = Paper50,
    surface = Carbon900,
    onSurface = Paper50,
    surfaceVariant = Carbon800,
    onSurfaceVariant = Steel300,
    surfaceTint = ShiftYellow,
    inverseSurface = Paper50,
    inverseOnSurface = Carbon950,
    error = DangerRedDark,
    onError = Carbon950,
    errorContainer = Color(0xFF5E171B),
    onErrorContainer = Color(0xFFFFBFC2),
    outline = Carbon600,
    outlineVariant = Carbon700,
    scrim = Color.Black,
)

private val LightColorScheme = lightColorScheme(
    primary = ShiftYellowDeep,
    onPrimary = Carbon950,
    primaryContainer = ShiftYellowSoft,
    onPrimaryContainer = Carbon950,
    secondary = Carbon700,
    onSecondary = Color.White,
    secondaryContainer = Steel200,
    onSecondaryContainer = Carbon950,
    tertiary = IndustrialOrange,
    onTertiary = Carbon950,
    tertiaryContainer = Color(0xFFFFD8B2),
    onTertiaryContainer = Color(0xFF341700),
    background = Paper50,
    onBackground = Carbon950,
    surface = Color.White,
    onSurface = Carbon950,
    surfaceVariant = Paper100,
    onSurfaceVariant = Carbon700,
    surfaceTint = ShiftYellowDeep,
    inverseSurface = Carbon900,
    inverseOnSurface = Paper50,
    error = DangerRed,
    onError = Color.White,
    errorContainer = DangerRedSoft,
    onErrorContainer = Color(0xFF8F1D23),
    outline = Steel300,
    outlineVariant = Paper200,
    scrim = Color.Black,
)

val LocalShiftHeroColors = staticCompositionLocalOf { LightShiftHeroExtendedColors }

object ShiftHeroThemeTokens {
    val colors: ShiftHeroExtendedColors
        @Composable
        @ReadOnlyComposable
        get() = LocalShiftHeroColors.current
}

@Composable
fun ShiftHeroTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val extendedColors = if (darkTheme) {
        DarkShiftHeroExtendedColors
    } else {
        LightShiftHeroExtendedColors
    }
    val view = LocalView.current

    if (!view.isInEditMode) {
        val window = (view.context as Activity).window
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
    }

    CompositionLocalProvider(LocalShiftHeroColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
