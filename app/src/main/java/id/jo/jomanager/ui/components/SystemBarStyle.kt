package id.jo.jomanager.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.activity.ComponentActivity

/**
 * Apply custom system bar styling for edge-to-edge display
 * @param darkMode Whether to use dark mode for system bars
 * @param statusBarScrim Color for status bar scrim
 * @param navigationBarScrim Color for navigation bar scrim
 */
@Composable
fun CustomSystemBarStyle(
    darkMode: Boolean,
    statusBarScrim: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Transparent,
    navigationBarScrim: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Transparent,
) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity ?: return

    SideEffect {
        WindowCompat.setDecorFitsSystemWindows(activity.window, false)

        val controller = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
        controller.isAppearanceLightStatusBars = !darkMode
        controller.isAppearanceLightNavigationBars = !darkMode
    }
}