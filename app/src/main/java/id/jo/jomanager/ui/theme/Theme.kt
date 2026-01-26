
/*
 * Copyright (c) 2025 Rve <rve27github @gmail.com>
 * All Rights Reserved.
 */
package id.jo.jomanager.ui.theme

import android.app.Activity
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.MotionScheme.Companion.expressive
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import id.jo.jomanager.util.ThemeManager

private val AMOLED_BLACK = Color(0xFF000000)

private fun Color.blend(other: Color, ratio: Float): Color {
    val inverse = 1f - ratio
    return Color(
        red = red * inverse + other.red * ratio,
        green = green * inverse + other.green * ratio,
        blue = blue * inverse + other.blue * ratio,
        alpha = alpha
    )
}

@RequiresApi(Build.VERSION_CODES.S)
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RvKernelManagerTheme(
    themeManager: ThemeManager,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val themeMode by themeManager.currentThemeMode.collectAsState(initial = ThemeMode.SYSTEM_DEFAULT)
    val isAmoledMode by themeManager.isAmoledMode.collectAsState(initial = false)

    val isDarkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM_DEFAULT -> isSystemInDarkTheme()
    }

    val colorScheme = when {
        isAmoledMode && isDarkTheme -> {
            val dynamicScheme = dynamicDarkColorScheme(context)
            dynamicScheme.copy(
                background = AMOLED_BLACK,
                surface = AMOLED_BLACK,
                surfaceVariant = dynamicScheme.surfaceVariant.blend(AMOLED_BLACK, 0.6f),
                surfaceContainer = dynamicScheme.surfaceContainer.blend(AMOLED_BLACK, 0.6f),
                surfaceContainerLow = dynamicScheme.surfaceContainerLow.blend(AMOLED_BLACK, 0.6f),
                surfaceContainerLowest = dynamicScheme.surfaceContainerLowest.blend(AMOLED_BLACK, 0.6f),
                surfaceContainerHigh = dynamicScheme.surfaceContainerHigh.blend(AMOLED_BLACK, 0.6f),
                surfaceContainerHighest = dynamicScheme.surfaceContainerHighest.blend(AMOLED_BLACK, 0.6f),
                primaryContainer = dynamicScheme.primaryContainer.blend(AMOLED_BLACK, 0.6f),
                secondaryContainer = dynamicScheme.secondaryContainer.blend(AMOLED_BLACK, 0.6f),
                tertiaryContainer = dynamicScheme.tertiaryContainer.blend(AMOLED_BLACK, 0.6f)
            )
        }
        isDarkTheme -> dynamicDarkColorScheme(context)
        else -> dynamicLightColorScheme(context)
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDarkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content,
        motionScheme = expressive()
    )
}
