package id.jo.jomanager.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Blur surface colors for different blur levels and themes
 */
object BlurSurfaceColors {
    // Light theme blur colors
    val lightBlur = Color(0xFFF7F2FA)
    val mediumBlur = Color(0xFFEDE7F6)
    val heavyBlur = Color(0xFFE1D5E7)

    // Dark theme blur colors (optimized for OLED)
    val darkLightBlur = Color(0xFF1E1E1E)
    val darkMediumBlur = Color(0xFF2A2A2A)
    val darkHeavyBlur = Color(0xFF363636)
}
