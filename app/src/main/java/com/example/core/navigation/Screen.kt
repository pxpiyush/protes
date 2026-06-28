package com.example.core.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Clean navigation screen destinations representing persistent tabs in the bottom bar.
 */
sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home : Screen("home", "Home", Icons.Outlined.Home)
    object Search : Screen("search", "Search", Icons.Outlined.Search)
    object Add : Screen("add", "Add", Icons.Outlined.AddCircleOutline)
    object Favorites : Screen("favorites", "Favorites", Icons.Outlined.StarBorder)
    object Settings : Screen("settings", "Settings", Icons.Outlined.Settings)

    companion object {
        val items get() = listOf(Home, Search, Add, Favorites, Settings)
    }
}
