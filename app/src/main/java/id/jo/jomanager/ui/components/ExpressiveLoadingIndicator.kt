package id.jo.jomanager.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicatorDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.RoundedPolygon

/**
 * Expressive Loading Indicator menggunakan Experimental Material 3 Expressive API
 * Komponen ini menggunakan LoadingIndicator dari Material 3 Expressive
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpressiveLoadingIndicator(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    size: Dp = 48.dp,
    polygons: List<RoundedPolygon> = LoadingIndicatorDefaults.DeterminateIndicatorPolygons
) {
    // Untuk determinate loading indicator, kita bisa menggunakan progress provider
    val progress by rememberInfiniteTransition(label = "loadingProgress").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "progress"
    )
    
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        LoadingIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxSize(),
            color = color,
            polygons = polygons
        )
    }
}

/**
 * Versi sederhana dari Expressive Loading Indicator menggunakan determinate progress
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SimpleExpressiveLoadingIndicator(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    progressValue: Float = 0.5f,
    polygons: List<RoundedPolygon> = LoadingIndicatorDefaults.DeterminateIndicatorPolygons
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        LoadingIndicator(
            progress = { progressValue },
            modifier = Modifier.size(24.dp),
            color = color,
            polygons = polygons
        )
    }
}

/**
 * Versi indeterminate dari Expressive Loading Indicator
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun IndeterminateExpressiveLoadingIndicator(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    polygons: List<RoundedPolygon> = LoadingIndicatorDefaults.IndeterminateIndicatorPolygons
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        LoadingIndicator(
            modifier = Modifier.size(48.dp),
            color = color,
            polygons = polygons
        )
    }
}