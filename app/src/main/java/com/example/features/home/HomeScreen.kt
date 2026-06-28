package com.example.features.home

import android.widget.Toast
import androidx.compose.animation.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.draw.clip
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import com.example.core.widgets.BoardShape
import com.example.core.widgets.CategoryCard
import com.example.core.widgets.PromptCard
import com.example.core.widgets.SearchBar
import com.example.core.widgets.getCategoryIcon
import com.example.features.ProtesViewModel
import com.example.features.PromptSort
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalAnimationApi::class)
@Composable
fun HomeScreen(
    viewModel: ProtesViewModel,
    onEditPrompt: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val prompts by viewModel.homePrompts.collectAsState()
    val allPromptsRaw by viewModel.prompts.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val selectedCategory by viewModel.selectedCategoryId.collectAsState()
    val currentSort by viewModel.currentSort.collectAsState()

    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Distraction-free reader state overlay
    var activeDetailPrompt by remember { mutableStateOf<Prompt?>(null) }

    // Category Manager Dialog States
    var showCategoryManager by remember { mutableStateOf(false) }
    var showCreateCategoryDialog by remember { mutableStateOf(false) }
    var showRenameCategoryDialog by remember { mutableStateOf<String?>(null) } // categoryId to rename
    var newCategoryName by remember { mutableStateOf("") }
    var renameCategoryValue by remember { mutableStateOf("") }

    Box(modifier = modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = selectedCategory,
            transitionSpec = {
                fadeIn(animationSpec = tween(300)) with fadeOut(animationSpec = tween(250))
            },
            label = "home_screen_content_transition"
        ) { targetCategoryId ->
            if (targetCategoryId == null) {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    // 1. App Logo, Title and Tagline Header
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(top = 16.dp, bottom = 8.dp, start = 24.dp, end = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            com.example.core.widgets.ProtesLogo(
                                iconSize = 180.dp,
                                showText = false,
                                textColor = androidx.compose.ui.graphics.Color.Unspecified
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Never Lose a Great Prompt.",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontFamily = FontFamily.SansSerif,
                                    fontWeight = FontWeight.Normal,
                                    letterSpacing = 0.5.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // 2. Beautiful spaced search bar
                    item {
                        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                            val searchVal by viewModel.searchQuery.collectAsState()
                            SearchBar(
                                query = searchVal,
                                onQueryChange = { viewModel.setSearchQuery(it) },
                                placeholder = "Scan titles, variables, or prompts..."
                            )
                        }
                    }

                    // 3. Signature PROMPT BOARDS (Dynamic Vision Board Layout)
                    item {
                        com.example.core.widgets.CategoryBoard(
                            viewModel = viewModel,
                            categories = categories,
                            allPrompts = allPromptsRaw,
                            selectedCategoryId = null,
                            onCategorySelected = { viewModel.selectCategory(it) },
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }

                    // 4. Curated Magazine Layout Feed Header & Sorting Chips
                    item {
                        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "HOME FEED",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.5.sp
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Recent prompts dynamically styled with editorial covers.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Premium, Tactile Sorting Chips
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(PromptSort.values()) { sortOption ->
                                    val isSelected = currentSort == sortOption
                                    val label = when (sortOption) {
                                        PromptSort.NEWEST -> "🆕 Newest"
                                        PromptSort.OLDEST -> "🗓️ Oldest"
                                        PromptSort.RECENTLY_USED -> "⏱️ Recent"
                                        PromptSort.MOST_COPIED -> "📥 Copied"
                                        PromptSort.ALPHABETICAL -> "🔤 A-Z"
                                        PromptSort.ALPHABETICAL_DESC -> "🔤 Z-A"
                                        PromptSort.UPDATED_RECENTLY -> "🔄 Updated"
                                        PromptSort.PINNED_FIRST -> "📌 Pinned"
                                        PromptSort.FAVORITES_FIRST -> "⭐ Favs"
                                        PromptSort.PROMPT_LENGTH -> "📏 Length"
                                    }
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { viewModel.setSort(sortOption) },
                                        label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                                    )
                                }
                            }
                        }
                    }

                    // 5. Magazine layout feed of prompts
                    if (prompts.isEmpty()) {
                        item {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 48.dp, horizontal = 24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.FolderOpen,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.outline
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "No prompts in this vault view.",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Tap 'Add' in the navigation bar to deposit your very first prompt template.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.outline,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        itemsIndexed(prompts, key = { _, prompt -> prompt.id }) { index, prompt ->
                            val category = categories.find { it.id == prompt.categoryId }

                            Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                                PromptCard(
                                    prompt = prompt,
                                    categoryName = category?.name,
                                    categoryIcon = category?.iconName,
                                    index = index,
                                    onCardClick = { activeDetailPrompt = prompt },
                                    onFavoriteToggle = { viewModel.toggleFavorite(prompt) },
                                    onPinToggle = { viewModel.togglePin(prompt) },
                                    onCopyClick = {
                                        clipboardManager.setText(AnnotatedString(prompt.content))
                                        viewModel.recordPromptCopy(prompt)
                                        Toast.makeText(context, "Copied template!", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }
                    }
                }
            } else {
                val category = categories.find { it.id == targetCategoryId }
                if (category != null) {
                    val promptsInCategory = allPromptsRaw.filter { it.categoryId == category.id }
                    ImmersiveCategoryView(
                        category = category,
                        promptsInCategory = promptsInCategory,
                        viewModel = viewModel,
                        clipboardManager = clipboardManager,
                        context = context,
                        onBackClick = { viewModel.selectCategory(null) },
                        onPromptClick = { activeDetailPrompt = it },
                        onEditPrompt = onEditPrompt,
                        onAddPromptClick = {
                            // Can add additional prompt trigger or instruct to click bottom bar Add
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Category workspace not found")
                    }
                }
            }
        }

        // Beautiful distraction-free Detail overlay screen inside Home
        PromptDetailOverlay(
            visible = activeDetailPrompt != null,
            prompt = activeDetailPrompt,
            onDismiss = { activeDetailPrompt = null },
            viewModel = viewModel,
            onEditPrompt = onEditPrompt
        )

        // Floating Snackbar Host for Undoing Delete and Archiving
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(16.dp)
        )
    }

    // CATEGORIES MANAGER SHEET / DIALOG
    if (showCategoryManager) {
        AlertDialog(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            onDismissRequest = { showCategoryManager = false },
            title = { Text("Category Boards Manager") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 350.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "Customize your vision boards. System defaults are locked.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    categories.forEach { cat ->
                        val isDefault = cat.id in listOf("cat_dev", "cat_creative", "cat_marketing", "cat_productivity")
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    imageVector = getCategoryIcon(cat.iconName),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = cat.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isDefault) FontWeight.Bold else FontWeight.Normal
                                )
                            }

                            if (isDefault) {
                                Text(
                                    text = "Locked",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            } else {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(
                                        onClick = {
                                            showRenameCategoryDialog = cat.id
                                            renameCategoryValue = cat.name
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(imageVector = Icons.Outlined.Edit, contentDescription = "Rename", modifier = Modifier.size(16.dp))
                                    }
                                    IconButton(
                                        onClick = {
                                            viewModel.deleteCategory(
                                                categoryId = cat.id,
                                                onSuccess = {
                                                    Toast.makeText(context, "Deleted board successfully", Toast.LENGTH_SHORT).show()
                                                },
                                                onError = { err ->
                                                    Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                                }
                                            )
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.DeleteOutline,
                                            contentDescription = "Delete",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showCreateCategoryDialog = true }) {
                    Icon(imageVector = Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Custom Board", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCategoryManager = false }) {
                    Text("Done")
                }
            }
        )
    }

    // CREATE NEW CATEGORY BOARD DIALOG
    if (showCreateCategoryDialog) {
        AlertDialog(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            onDismissRequest = { showCreateCategoryDialog = false },
            title = { Text("New Custom Board") },
            text = {
                Column {
                    Text("Design a personalized board to keep your workflow isolated.")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newCategoryName,
                        onValueChange = { newCategoryName = it },
                        label = { Text("Board Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newCategoryName.isNotBlank()) {
                            viewModel.createCategory(newCategoryName)
                            Toast.makeText(context, "Created category board: $newCategoryName", Toast.LENGTH_SHORT).show()
                            newCategoryName = ""
                            showCreateCategoryDialog = false
                        }
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateCategoryDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // RENAME CATEGORY BOARD DIALOG
    val catToRename = showRenameCategoryDialog
    if (catToRename != null) {
        AlertDialog(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            onDismissRequest = { showRenameCategoryDialog = null },
            title = { Text("Rename Board") },
            text = {
                OutlinedTextField(
                    value = renameCategoryValue,
                    onValueChange = { renameCategoryValue = it },
                    label = { Text("New Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (renameCategoryValue.isNotBlank()) {
                            viewModel.renameCategory(catToRename, renameCategoryValue)
                            showRenameCategoryDialog = null
                            renameCategoryValue = ""
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameCategoryDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ImmersiveCategoryView(
    category: com.example.core.model.Category,
    promptsInCategory: List<Prompt>,
    viewModel: ProtesViewModel,
    clipboardManager: androidx.compose.ui.platform.ClipboardManager,
    context: android.content.Context,
    onBackClick: () -> Unit,
    onPromptClick: (Prompt) -> Unit,
    onEditPrompt: (String) -> Unit,
    onAddPromptClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalPrompts = promptsInCategory.size
    val favoritesCount = promptsInCategory.count { it.isFavorite }
    val pinnedCount = promptsInCategory.count { it.isPinned }
    val mostUsedPrompt = promptsInCategory.maxByOrNull { it.usageCount }?.title ?: "None"
    
    val maxTimestamp = promptsInCategory.maxOfOrNull { maxOf(it.updatedAt, it.createdAt) }
    val recentlyUpdated = if (maxTimestamp != null) {
        SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(maxTimestamp))
    } else "Never"

    LazyColumn(
        contentPadding = PaddingValues(bottom = 56.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        modifier = modifier.fillMaxSize()
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f))
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
                    )
            ) {
                val firstLetter = category.name.firstOrNull()?.uppercaseChar()?.toString() ?: ""
                Text(
                    text = firstLetter,
                    fontSize = 180.sp,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 10.dp, y = 30.dp)
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = onBackClick,
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(imageVector = Icons.Outlined.ArrowBack, contentDescription = "Back to Boards", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Boards", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        }

                        var showDetailCustomizer by remember { mutableStateOf(false) }
                        IconButton(onClick = { showDetailCustomizer = true }) {
                            Icon(imageVector = Icons.Outlined.Tune, contentDescription = "Board Settings")
                        }

                        if (showDetailCustomizer) {
                            AlertDialog(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                                onDismissRequest = { showDetailCustomizer = false },
                                title = { Text("Workspace Layout & Style") },
                                text = {
                                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Text("Change how this vision board is represented on the home grid:")
                                        
                                        Text("Card Shape size:", style = MaterialTheme.typography.labelMedium)
                                        val shapesList = listOf(
                                            "Small Square" to "small_square",
                                            "Large Square" to "large_square",
                                            "Wide Rectangle" to "wide_rectangle",
                                            "Tall Rectangle" to "tall_rectangle",
                                            "Circle" to "circle",
                                            "Rounded Capsule" to "rounded_capsule"
                                        )
                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            shapesList.forEach { (label, value) ->
                                                val isSelectedShape = category.boardSize.lowercase() == value
                                                FilterChip(
                                                    selected = isSelectedShape,
                                                    onClick = { viewModel.updateCategorySize(category.id, value) },
                                                    label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                                                )
                                            }
                                        }

                                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                                        Button(
                                            onClick = {
                                                viewModel.toggleArchiveCategory(category.id)
                                                showDetailCustomizer = false
                                                onBackClick()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Icon(Icons.Outlined.Archive, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Archive Category Board")
                                        }
                                    }
                                },
                                confirmButton = {
                                    TextButton(onClick = { showDetailCustomizer = false }) {
                                        Text("Done")
                                    }
                                }
                            )
                        }
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = getCategoryIcon(category.iconName),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = category.name,
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontFamily = FontFamily.SansSerif,
                                    fontWeight = FontWeight.ExtraBold
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "WORKSPACE   •   $totalPrompts TEMPLATES",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.5.sp
                        )
                    }
                }
            }
        }

        item {
            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                Text(
                    text = "SMART BOARD STATS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatCard(
                            label = "Favorites",
                            value = "$favoritesCount ⭐",
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            label = "Pinned Items",
                            value = "$pinnedCount 📌",
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            label = "Updated",
                            value = recentlyUpdated,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatCard(
                            label = "Most Popular Prompt",
                            value = mostUsedPrompt,
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }
            }
        }

        val pinnedPrompts = promptsInCategory.filter { it.isPinned }
        if (pinnedPrompts.isNotEmpty()) {
            item {
                Column {
                    Text(
                        text = "📌 PINNED IDEAS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 12.dp)
                    )

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(pinnedPrompts, key = { it.id }) { prompt ->
                            Box(modifier = Modifier.width(300.dp)) {
                                PromptCard(
                                    prompt = prompt,
                                    categoryName = category.name,
                                    categoryIcon = category.iconName,
                                    index = 0,
                                    onCardClick = { onPromptClick(prompt) },
                                    onFavoriteToggle = { viewModel.toggleFavorite(prompt) },
                                    onPinToggle = { viewModel.togglePin(prompt) },
                                    onCopyClick = {
                                        clipboardManager.setText(AnnotatedString(prompt.content))
                                        viewModel.recordPromptCopy(prompt)
                                        Toast.makeText(context, "Copied template!", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "📝 RECENT TEMPLATES",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                ),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 4.dp)
            )
        }

        if (promptsInCategory.isEmpty()) {
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp, horizontal = 24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FolderOpen,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No templates in this workspace.",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Start adding creative prompts to this custom board right now.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onAddPromptClick,
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Icon(imageVector = Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Create Prompt")
                    }
                }
            }
        } else {
            itemsIndexed(promptsInCategory, key = { _, prompt -> prompt.id }) { index, prompt ->
                Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                    PromptCard(
                        prompt = prompt,
                        categoryName = category.name,
                        categoryIcon = category.iconName,
                        index = index,
                        onCardClick = { onPromptClick(prompt) },
                        onFavoriteToggle = { viewModel.toggleFavorite(prompt) },
                        onPinToggle = { viewModel.togglePin(prompt) },
                        onCopyClick = {
                            clipboardManager.setText(AnnotatedString(prompt.content))
                            viewModel.recordPromptCopy(prompt)
                            Toast.makeText(context, "Copied template!", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = false
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
        ),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = label.uppercase(),
                fontSize = 8.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.outline,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = if (singleLine) 1 else 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun PromptDetailOverlay(
    visible: Boolean,
    prompt: Prompt?,
    onDismiss: () -> Unit,
    viewModel: ProtesViewModel,
    onEditPrompt: (String) -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically(initialOffsetY = { it / 3 }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 3 })
    ) {
        if (prompt != null) {
            val categories by viewModel.categories.collectAsState()
            val category = categories.find { it.id == prompt.categoryId }
            val context = LocalContext.current
            val clipboardManager = LocalClipboardManager.current
            val scope = rememberCoroutineScope()

            // Fetch historical versions
            val promptVersions by viewModel.getVersionsForPrompt(prompt.id).collectAsState(initial = emptyList())

            // State for interactive variables
            val allVariables = remember(prompt) {
                (prompt.getVariables() + prompt.placeholdersCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }).distinct()
            }
            var variableValues by remember(prompt) { mutableStateOf(mapOf<String, String>()) }

            // State for compare dialog
            var compareVersion by remember { mutableStateOf<com.example.core.model.PromptVersion?>(null) }

            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp)
                ) {
                    // Header Back action and tools
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Outlined.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconButton(onClick = { viewModel.togglePin(prompt) }) {
                                Icon(
                                    imageVector = Icons.Outlined.PushPin,
                                    contentDescription = "Pin",
                                    tint = if (prompt.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                )
                            }
                            IconButton(onClick = { viewModel.toggleFavorite(prompt) }) {
                                Icon(
                                    imageVector = if (prompt.isFavorite) Icons.Outlined.Star else Icons.Outlined.StarBorder,
                                    contentDescription = "Favorite",
                                    tint = if (prompt.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Category & Model Tags
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (category != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = getCategoryIcon(category.iconName),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.outline
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = category.name.uppercase(),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.outline,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            }
                            Text(
                                text = "•",
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Large Title
                    Text(
                        text = prompt.title,
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    if (prompt.description.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = prompt.description,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 24.sp
                        )
                    }

                    // Metadata Badges Row (Quality, Difficulty, Success Status, Source)
                    Spacer(modifier = Modifier.height(16.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SuggestionChip(
                            onClick = {},
                            label = { Text("Quality: ${prompt.quality}") }
                        )
                        SuggestionChip(
                            onClick = {},
                            label = { Text("Difficulty: ${prompt.difficulty}") }
                        )
                        SuggestionChip(
                            onClick = {},
                            label = {
                                Text(
                                    text = when (prompt.lastSuccess) {
                                        "Worked Great" -> "🟢 Great"
                                        "Worked Okay" -> "🟡 Okay"
                                        "Needs Improvement" -> "🔴 Needs Imp."
                                        else -> "⚪ None"
                                    }
                                )
                            }
                        )
                        SuggestionChip(
                            onClick = {},
                            label = { Text("Origin: ${prompt.source}") }
                        )
                    }

                    // Rating Stars
                    if (prompt.rating > 0) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Usefulness:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            (1..5).forEach { star ->
                                Icon(
                                    imageVector = if (star <= prompt.rating) Icons.Outlined.Star else Icons.Outlined.StarBorder,
                                    contentDescription = null,
                                    tint = if (star <= prompt.rating) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    // Statistics info (Usage count, timestamps)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "📈 Copied: ${prompt.usageCount} times",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        if (prompt.lastUsedAt != null) {
                            val dateStr = java.text.DateFormat.getDateInstance().format(java.util.Date(prompt.lastUsedAt))
                            Text(
                                text = "⏱️ Last Used: $dateStr",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }

                    // Private Rich Notes
                    if (prompt.notes.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "📓 PRIVATE RICH NOTES",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.5.sp
                                    ),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = prompt.notes,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Prompt content in a beautiful monospaced notebook style
                    Text(
                        text = "MASTER PROMPT CODE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                RoundedCornerShape(12.dp)
                            ),
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = prompt.content,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 14.sp,
                                    lineHeight = 22.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Master Action Row for Copying Original
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(prompt.content))
                            viewModel.recordPromptCopy(prompt)
                            Toast.makeText(context, "Master prompt template copied!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ContentCopy,
                            contentDescription = "Copy"
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Copy Master Template",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    // Interactive variable section if present
                    if (allVariables.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(32.dp))
                        Text(
                            text = "✍️ INTERACTIVE VARIABLES",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp
                            ),
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Fill variables live below to compile your custom prompt.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            allVariables.forEach { variable ->
                                OutlinedTextField(
                                    value = variableValues[variable] ?: "",
                                    onValueChange = { newValue ->
                                        variableValues = variableValues + (variable to newValue)
                                    },
                                    label = { Text("Value for: $variable") },
                                    placeholder = { Text("Enter custom value...") },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Live Filled Preview Box
                        Text(
                            text = "👁️ LIVE FILLED PREVIEW",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp
                            ),
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        val filledContent = remember(prompt, variableValues) {
                            var result = prompt.content
                            allVariables.forEach { v ->
                                val replacement = variableValues[v] ?: ""
                                if (replacement.isNotEmpty()) {
                                    result = result.replace("{{$v}}", replacement).replace("[[$v]]", replacement)
                                }
                            }
                            result
                        }

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                    RoundedCornerShape(12.dp)
                                ),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = filledContent,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 14.sp,
                                        lineHeight = 22.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(filledContent))
                                viewModel.recordPromptCopy(prompt)
                                Toast.makeText(context, "Filled prompt copied!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Check,
                                contentDescription = "Copy Filled"
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Copy Custom Filled Prompt",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }

                    // Export actions
                    Spacer(modifier = Modifier.height(32.dp))
                    Text(
                        text = "📥 EXPORT PROMPT",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = {
                                val exported = viewModel.exportPrompt(prompt, "JSON")
                                clipboardManager.setText(AnnotatedString(exported))
                                Toast.makeText(context, "Copied JSON export to clipboard!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("JSON", fontSize = 12.sp)
                        }
                        Button(
                            onClick = {
                                val exported = viewModel.exportPrompt(prompt, "Markdown")
                                clipboardManager.setText(AnnotatedString(exported))
                                Toast.makeText(context, "Copied Markdown export to clipboard!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Markdown", fontSize = 12.sp)
                        }
                        Button(
                            onClick = {
                                val exported = viewModel.exportPrompt(prompt, "Plain Text")
                                clipboardManager.setText(AnnotatedString(exported))
                                Toast.makeText(context, "Copied Plain Text export to clipboard!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Plain", fontSize = 12.sp)
                        }
                    }

                    // Version History Panel
                    Spacer(modifier = Modifier.height(32.dp))
                    Text(
                        text = "⏱️ VERSION HISTORY",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (promptVersions.isEmpty()) {
                        Text(
                            text = "No previous versions found. Any future edits you commit will archive here offline.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            promptVersions.forEachIndexed { vIdx, version ->
                                val verNum = promptVersions.size - vIdx
                                val dateStr = java.text.DateFormat.getDateTimeInstance().format(java.util.Date(version.updatedAt))
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Version $verNum",
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = dateStr,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = version.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        if (version.description.isNotEmpty()) {
                                            Text(
                                                text = version.description,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Button(
                                                onClick = { compareVersion = version },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                                ),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("🔍 Compare", fontSize = 11.sp)
                                            }
                                            Button(
                                                onClick = {
                                                    viewModel.restorePromptVersion(prompt.id, version)
                                                    Toast.makeText(context, "Restored successfully!", Toast.LENGTH_SHORT).show()
                                                },
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.weight(1.5f)
                                            ) {
                                                Text("Revert to v$verNum", fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // CRUD Actions Row: Edit, Delete
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                onDismiss()
                                onEditPrompt(prompt.id)
                            },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        ) {
                            Icon(imageVector = Icons.Outlined.Edit, contentDescription = "Edit")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Edit")
                        }

                        Button(
                            onClick = {
                                viewModel.archivePrompt(prompt)
                                Toast.makeText(context, "Archived to Vault", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Icon(imageVector = Icons.Outlined.Archive, contentDescription = "Archive")
                        }

                        Button(
                            onClick = {
                                viewModel.softDeletePrompt(prompt)
                                Toast.makeText(context, "Moved to Trash Bin", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            )
                        ) {
                            Icon(imageVector = Icons.Outlined.Delete, contentDescription = "Delete")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Delete")
                        }
                    }
                }
            }

            // Compare Dialog
            if (compareVersion != null) {
                val cv = compareVersion!!
                AlertDialog(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                    onDismissRequest = { compareVersion = null },
                    title = { Text("Compare version differences") },
                    text = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Current Title: ${prompt.title}",
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Historical Title: ${cv.title}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline
                            )
                            HorizontalDivider()

                            Text(
                                text = "Current Prompt:",
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = prompt.content,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }

                            Text(
                                text = "Historical Prompt:",
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = cv.content,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { compareVersion = null }) {
                            Text("Close")
                        }
                    }
                )
            }
        }
    }
}
