package id.jo.jomanager.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import id.jo.jomanager.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnifiedTopAppBar(
    title: String,
    navController: NavController? = null,
    showSettingsIcon: Boolean = false,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    isAmoledMode: Boolean = false,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val surfaceColor = MaterialTheme.colorScheme.surface
    val elevation = if (isAmoledMode) 6.dp else 3.dp
    val surfaceColorAtElevation = MaterialTheme.colorScheme.surfaceColorAtElevation(elevation)
    val topBarContainerColor by remember(scrollBehavior, surfaceColor, surfaceColorAtElevation) {
        derivedStateOf {
            scrollBehavior?.state?.overlappedFraction?.let { fraction ->
                lerp(
                    surfaceColor,
                    surfaceColorAtElevation,
                    fraction
                )
            } ?: surfaceColor
        }
    }

    TopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        navigationIcon = {
            if (!showSettingsIcon && navController?.previousBackStackEntry != null) {
                FilledTonalIconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back)
                    )
                }
            }
        },
        actions = {
            actions()
            if (showSettingsIcon && navController != null) {
                IconButton(onClick = { 
                    val currentRoute = navController.currentBackStackEntry?.destination?.route
                    val routeWithArgs = if (currentRoute != null) {
                        "settings?fromScreen=$currentRoute"
                    } else {
                        "settings"
                    }
                    navController.navigate(routeWithArgs)
                }) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = stringResource(R.string.home_settings_button_desc)
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = topBarContainerColor
        ),
        scrollBehavior = scrollBehavior
    )
}