package com.example.core.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.core.theme.Motion
import com.example.features.ProtesViewModel
import com.example.features.home.HomeScreen
import com.example.features.search.SearchScreen
import com.example.features.add.AddScreen
import com.example.features.favorites.FavoritesScreen
import com.example.features.settings.SettingsScreen

/**
 * Main application shell managing the persistent bottom navigation tabs with a
 * high-performance crossfade and proper gesture insets.
 */
@Composable
fun ProtesShell(
    viewModel: ProtesViewModel,
    modifier: Modifier = Modifier
) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )
                    .windowInsetsPadding(WindowInsets.navigationBars),
                containerColor = MaterialTheme.colorScheme.background,
                tonalElevation = 0.dp
            ) {
                Screen.items.forEach { screen ->
                    val isSelected = currentScreen.route == screen.route
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentScreen = screen },
                        icon = {
                            Icon(
                                imageVector = screen.icon,
                                contentDescription = screen.title,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        label = {
                            Text(
                                text = screen.title,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                            unselectedIconColor = MaterialTheme.colorScheme.outline,
                            unselectedTextColor = MaterialTheme.colorScheme.outline
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        // Premium Screen Navigation Transition (subtle slide combined with a calm fade)
        AnimatedContent(
            targetState = currentScreen,
            transitionSpec = {
                val initialIndex = Screen.items.indexOf(initialState)
                val targetIndex = Screen.items.indexOf(targetState)
                val direction = if (targetIndex >= initialIndex) 1 else -1

                val slideIn = slideInHorizontally(
                    animationSpec = tween(durationMillis = Motion.Duration.Slow, easing = Motion.Easing.Standard),
                    initialOffsetX = { fullWidth -> (direction * 24.dp.value * 2).toInt() }
                ) + fadeIn(animationSpec = tween(durationMillis = Motion.Duration.Normal))

                val slideOut = slideOutHorizontally(
                    animationSpec = tween(durationMillis = Motion.Duration.Slow, easing = Motion.Easing.Standard),
                    targetOffsetX = { fullWidth -> (-direction * 24.dp.value * 2).toInt() }
                ) + fadeOut(animationSpec = tween(durationMillis = Motion.Duration.Normal))

                (slideIn togetherWith slideOut).using(
                    SizeTransform(clip = false)
                )
            },
            label = "screen_transition",
            modifier = Modifier.padding(innerPadding)
        ) { screen ->
            val onEditPrompt: (String) -> Unit = { promptId ->
                viewModel.startEditing(promptId)
                currentScreen = Screen.Add
            }

            when (screen) {
                is Screen.Home -> HomeScreen(
                    viewModel = viewModel,
                    onEditPrompt = onEditPrompt
                )
                is Screen.Search -> SearchScreen(
                    viewModel = viewModel,
                    onEditPrompt = onEditPrompt
                )
                is Screen.Add -> AddScreen(
                    viewModel = viewModel,
                    onSuccessNavigate = { currentScreen = Screen.Home }
                )
                is Screen.Favorites -> FavoritesScreen(
                    viewModel = viewModel,
                    onEditPrompt = onEditPrompt
                )
                is Screen.Settings -> SettingsScreen(viewModel = viewModel)
            }
        }
    }
}
