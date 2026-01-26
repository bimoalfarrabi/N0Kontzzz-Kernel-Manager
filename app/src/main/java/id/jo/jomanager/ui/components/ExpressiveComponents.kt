package id.jo.jomanager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp

// Define blur surface colors directly in this file to avoid import issues
private object BlurSurfaceColors {
    // Light theme blur colors
    val lightBlur = Color(0xFFF7F2FA)
    val mediumBlur = Color(0xFFEDE7F6)
    val heavyBlur = Color(0xFFE1D5E7)

    // Dark theme blur colors (optimized for OLED)
    val darkLightBlur = Color(0xFF1E1E1E)
    val darkMediumBlur = Color(0xFF2A2A2A)
    val darkHeavyBlur = Color(0xFF363636)
}

/**
 * Glassmorphism surface component for modern glass-like UI effects
 * Optimized for dark theme with proper blur and transparency
 */
@Composable
fun GlassmorphismSurface(
    modifier: Modifier = Modifier,
    blurRadius: Float = 12f,
    alpha: Float = 0.15f,
    borderAlpha: Float = 0.2f,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    // Base color for glassmorphism effect - always use dark colors since app is dark-only
    val baseColor = if (isDark) {
        when {
            blurRadius >= 10f -> BlurSurfaceColors.darkHeavyBlur
            blurRadius >= 5f -> BlurSurfaceColors.darkMediumBlur
            else -> BlurSurfaceColors.darkLightBlur
        }
    } else {
        // Fallback for light theme (won't be used in dark-only mode)
        when {
            blurRadius >= 10f -> BlurSurfaceColors.heavyBlur
            blurRadius >= 5f -> BlurSurfaceColors.mediumBlur
            else -> BlurSurfaceColors.lightBlur
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                color = baseColor.copy(alpha = alpha)
            )
            .border(
                width = 0.5.dp,
                color = if (isDark) {
                    Color.White.copy(alpha = borderAlpha * 0.3f)
                } else {
                    Color.Black.copy(alpha = borderAlpha * 0.2f)
                },
                shape = RoundedCornerShape(16.dp)
            ),
        content = content
    )
}

enum class BlurLevel {
    Light, Medium, Heavy
}

@Composable
fun BlurCard(
    modifier: Modifier = Modifier,
    blurLevel: BlurLevel = BlurLevel.Heavy, // Default ke Heavy untuk efek maksimal
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val backgroundColor = when (blurLevel) {
        BlurLevel.Light -> if (isDark) BlurSurfaceColors.darkLightBlur else BlurSurfaceColors.lightBlur
        BlurLevel.Medium -> if (isDark) BlurSurfaceColors.darkMediumBlur else BlurSurfaceColors.mediumBlur
        BlurLevel.Heavy -> if (isDark) BlurSurfaceColors.darkHeavyBlur else BlurSurfaceColors.heavyBlur
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.clickable { onClick() }
                } else {
                    Modifier
                }
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

@Composable
fun GradientCard(
    modifier: Modifier = Modifier,
    gradient: Brush = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
            MaterialTheme.colorScheme.secondary.copy(alpha = 0.05f)
        )
    ),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.clickable { onClick() }
                } else {
                    Modifier
                }
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {
        Box(
            modifier = Modifier
                .background(gradient)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                content = content
            )
        }
    }
}
