package com.example.features.favorites

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.Prompt
import com.example.core.widgets.PromptCard
import com.example.core.widgets.getCategoryIcon
import com.example.features.ProtesViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FavoritesScreen(
    viewModel: ProtesViewModel,
    onEditPrompt: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val prompts by viewModel.prompts.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val favoritePrompts = prompts.filter { it.isFavorite }

    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Distraction-free reader state overlay
    var activeDetailPrompt by remember { mutableStateOf<Prompt?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Text(
                    text = "Favorites Vault",
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Your most prized templates, pinned and highlighted for speed-dial retrieval.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 24.dp)
            ) {
                if (favoritePrompts.isEmpty()) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.StarOutline,
                            contentDescription = "No favorites",
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Star a great prompt.",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap the star icon on any card in Home or Search to pin it to this curated speed dial list.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        itemsIndexed(favoritePrompts, key = { _, prompt -> prompt.id }) { index, prompt ->
                            val category = categories.find { it.id == prompt.categoryId }

                            PromptCard(
                                prompt = prompt,
                                categoryName = category?.name,
                                categoryIcon = category?.iconName,
                                index = index,
                                onCardClick = {
                                    activeDetailPrompt = prompt
                                },
                                onFavoriteToggle = { viewModel.toggleFavorite(prompt) },
                                onPinToggle = { viewModel.togglePin(prompt) },
                                onCopyClick = {
                                    clipboardManager.setText(AnnotatedString(prompt.content))
                                    viewModel.recordPromptCopy(prompt)
                                    Toast.makeText(context, "Copied content!", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
            }
        }

        // Beautiful distraction-free Detail overlay screen inside Favorites
        com.example.features.home.PromptDetailOverlay(
            visible = activeDetailPrompt != null,
            prompt = activeDetailPrompt,
            onDismiss = { activeDetailPrompt = null },
            viewModel = viewModel,
            onEditPrompt = onEditPrompt
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(16.dp)
        )
    }
}
