package id.jo.jomanager.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState

@Composable
fun BottomNavBar(navController: NavHostController, items: List<String>, isAmoledMode: Boolean) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val containerColor = if (isAmoledMode) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainer

    NavigationBar(
        modifier = Modifier.fillMaxWidth(),
        containerColor = containerColor
    ) {
        val routes = listOf("home", "tuning", "misc")
        items.forEachIndexed { index, screen ->
            val route = routes.getOrElse(index) { "" }
            val selected = currentRoute == route
            val (filledIcon, outlinedIcon) = getNavIcons(route)
            val iconToDisplay = if (selected) filledIcon else outlinedIcon

            NavigationBarItem(
                selected = selected,
                onClick = {
                    if (currentRoute != route) {
                        if (currentRoute != null && currentRoute !in routes) {
                            navController.popBackStack()
                        }
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = { Icon(imageVector = iconToDisplay, contentDescription = screen) },
                label = {
                    Text(
                        screen,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.onSurface, // Or onSecondaryContainer
                    indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

// Helper function to get filled and outlined icons for each screen.
private fun getNavIcons(route: String): Pair<ImageVector, ImageVector> { // Pair(Filled, Outlined)
    return when (route) {
        "home" -> Pair(Icons.Filled.Home, Icons.Outlined.Home)
        "tuning" -> Pair(Icons.Filled.Build, Icons.Outlined.Build) // Assuming Build has an Outlined version
        "misc" -> Pair(Icons.Filled.Tune, Icons.Outlined.Tune) // Changed icon
        // Fallback icons if a screen name doesn't match
        else -> Pair(Icons.AutoMirrored.Filled.Help, Icons.AutoMirrored.Outlined.HelpOutline) // Example fallback
    }
}