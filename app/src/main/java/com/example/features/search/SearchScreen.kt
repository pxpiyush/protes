package com.example.features.search

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.Prompt
import com.example.core.widgets.PromptCard
import com.example.core.widgets.SearchBar
import com.example.core.widgets.getCategoryIcon
import com.example.features.PromptSort
import com.example.features.ProtesViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: ProtesViewModel,
    onEditPrompt: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val query by viewModel.searchQuery.collectAsState()
    val prompts by viewModel.filteredPrompts.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val collections by viewModel.collections.collectAsState()
    val allTags by viewModel.allTags.collectAsState()
    val filters by viewModel.searchFilters.collectAsState()
    val searchHistory by viewModel.searchHistory.collectAsState()
    val currentSort by viewModel.currentSort.collectAsState()

    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Synchronized local query for debouncing keyboard inputs
    var localQuery by remember { mutableStateOf(query) }

    // Distraction-free reader state overlay
    var activeDetailPrompt by remember { mutableStateOf<Prompt?>(null) }

    // Sync external query changes (e.g. from clicking search history/suggestions)
    LaunchedEffect(query) {
        if (query != localQuery) {
            localQuery = query
        }
    }

    // Debounce state flow: only propagate changes to ViewModel after 150ms of silence
    LaunchedEffect(localQuery) {
        if (localQuery == query) return@LaunchedEffect
        delay(150)
        viewModel.setSearchQuery(localQuery)
        if (localQuery.isNotBlank()) {
            viewModel.addSearchHistory(localQuery)
        }
    }

    // Scroll state preservation across tabs
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = viewModel.searchScrollIndex,
        initialFirstVisibleItemScrollOffset = viewModel.searchScrollOffset
    )

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .distinctUntilChanged()
            .collectLatest { (index, offset) ->
                viewModel.searchScrollIndex = index
                viewModel.searchScrollOffset = offset
            }
    }

    val anyFilterActive = filters.categoryIds.isNotEmpty() ||
            filters.collectionIds.isNotEmpty() ||
            filters.promptTypes.isNotEmpty() ||
            filters.tags.isNotEmpty() ||
            filters.isFavoriteOnly ||
            filters.isPinnedOnly ||
            filters.hasCoverOnly ||
            localQuery.isNotEmpty()

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Header Title Area
            Column(
                modifier = Modifier
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Search Vault",
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Scan all templates, variables, and labels offline in milliseconds.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                SearchBar(
                    query = localQuery,
                    onQueryChange = { localQuery = it },
                    placeholder = "Search title, body, label, notes...",
                    modifier = Modifier.testTag("search_bar_input")
                )
            }

            // Quick Filter Bar - horizontal scroll of elegant Material 3 chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("quick_filter_bar")
            ) {
                if (anyFilterActive) {
                    item {
                        FilterChip(
                            selected = false,
                            onClick = {
                                localQuery = ""
                                viewModel.clearAllFilters()
                            },
                            label = { Text("Reset Filters") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.RotateLeft,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }

                // Sorting Selector Dropdown
                item {
                    var showSortMenu by remember { mutableStateOf(false) }
                    Box {
                        FilterChip(
                            selected = true,
                            onClick = { showSortMenu = true },
                            label = {
                                val sortText = when (currentSort) {
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
                                Text("Sort: $sortText")
                            },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.ArrowDropDown,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                        MaterialTheme(
                            colorScheme = MaterialTheme.colorScheme.copy(surface = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
                            shapes = MaterialTheme.shapes.copy(extraSmall = RoundedCornerShape(16.dp))
                        ) {
                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false }
                            ) {
                                PromptSort.values().forEach { sortOption ->
                                    val label = when (sortOption) {
                                        PromptSort.NEWEST -> "🆕 Newest"
                                        PromptSort.OLDEST -> "🗓️ Oldest"
                                        PromptSort.RECENTLY_USED -> "⏱️ Recently Used"
                                        PromptSort.MOST_COPIED -> "📥 Most Used"
                                        PromptSort.ALPHABETICAL -> "🔤 Alphabetical (A-Z)"
                                        PromptSort.ALPHABETICAL_DESC -> "🔤 Alphabetical (Z-A)"
                                        PromptSort.UPDATED_RECENTLY -> "🔄 Recently Updated"
                                        PromptSort.PINNED_FIRST -> "📌 Pinned First"
                                        PromptSort.FAVORITES_FIRST -> "⭐ Favorites First"
                                        PromptSort.PROMPT_LENGTH -> "📏 Prompt Length"
                                    }
                                    DropdownMenuItem(
                                        text = { Text(label) },
                                        onClick = {
                                            viewModel.setSort(sortOption)
                                            showSortMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Core quick filters
                item {
                    FilterChip(
                        selected = filters.isFavoriteOnly,
                        onClick = { viewModel.toggleFavoriteFilter() },
                        label = { Text("Favorites") },
                        leadingIcon = {
                            Icon(
                                imageVector = if (filters.isFavoriteOnly) Icons.Outlined.Star else Icons.Outlined.StarBorder,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = if (filters.isFavoriteOnly) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            )
                        }
                    )
                }

                item {
                    FilterChip(
                        selected = filters.isPinnedOnly,
                        onClick = { viewModel.togglePinnedFilter() },
                        label = { Text("Pinned") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.PushPin,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = if (filters.isPinnedOnly) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            )
                        }
                    )
                }

                item {
                    FilterChip(
                        selected = filters.hasCoverOnly,
                        onClick = { viewModel.toggleHasCoverFilter() },
                        label = { Text("Has Cover Pattern") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Brush,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    )
                }

                // Prompt Type Chips
                val promptTypeOptions = listOf("Text", "Code", "Image")
                items(promptTypeOptions) { type ->
                    val isSelected = filters.promptTypes.contains(type)
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.togglePromptTypeFilter(type) },
                        label = { Text("$type Prompts") }
                    )
                }

                // Categories Multi-select Chips
                items(categories) { category ->
                    val isSelected = filters.categoryIds.contains(category.id)
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.toggleCategoryFilter(category.id) },
                        label = { Text(category.name) },
                        leadingIcon = {
                            Icon(
                                imageVector = getCategoryIcon(category.iconName),
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    )
                }

                // Collections Multi-select Chips
                items(collections) { collection ->
                    val isSelected = filters.collectionIds.contains(collection.id)
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.toggleCollectionFilter(collection.id) },
                        label = { Text("📚 ${collection.name}") }
                    )
                }

                // Dynamic Tags list
                items(allTags) { tag ->
                    val isSelected = filters.tags.contains(tag)
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.toggleTagFilter(tag) },
                        label = { Text("#$tag", maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Main View Content: Suggested Sections vs Search Results
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 24.dp)
            ) {
                Crossfade(
                    targetState = localQuery.isEmpty() && !anyFilterActive,
                    animationSpec = tween(durationMillis = 150)
                ) { isEmptyState ->
                    if (isEmptyState) {
                        // Curved, vertically scrollable Suggested Exploration Hub to maximize discoverability
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            // 1. Local Search History Section
                            if (searchHistory.isNotEmpty()) {
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "RECENT SEARCHES",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 1.sp
                                            ),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        TextButton(
                                            onClick = { viewModel.clearSearchHistory() },
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Text(
                                                text = "Clear All",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        items(searchHistory) { historyTerm ->
                                            Surface(
                                                modifier = Modifier
                                                    .border(
                                                        width = 1.dp,
                                                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                                        shape = RoundedCornerShape(16.dp)
                                                    ),
                                                color = MaterialTheme.colorScheme.surface,
                                                shape = RoundedCornerShape(16.dp)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.padding(start = 12.dp, end = 6.dp, top = 6.dp, bottom = 6.dp)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.clickable { localQuery = historyTerm },
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Outlined.History,
                                                            contentDescription = null,
                                                            modifier = Modifier.size(14.dp),
                                                            tint = MaterialTheme.colorScheme.outline
                                                        )
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text(
                                                            text = historyTerm,
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    IconButton(
                                                        onClick = { viewModel.removeSearchHistory(historyTerm) },
                                                        modifier = Modifier.size(20.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Outlined.Close,
                                                            contentDescription = "Delete",
                                                            modifier = Modifier.size(12.dp),
                                                            tint = MaterialTheme.colorScheme.outline
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // 2. Suggested Categories Explorations
                            Column {
                                Text(
                                    text = "SUGGESTED CATEGORIES",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    ),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(categories) { category ->
                                        Surface(
                                            modifier = Modifier
                                                .border(
                                                    width = 1.dp,
                                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                                    shape = RoundedCornerShape(12.dp)
                                                )
                                                .clickable { viewModel.toggleCategoryFilter(category.id) },
                                            color = MaterialTheme.colorScheme.surface,
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = getCategoryIcon(category.iconName),
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = category.name,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // 3. Pinned Prompts Highlights
                            val pinnedPrompts = prompts.filter { it.isPinned }
                            if (pinnedPrompts.isNotEmpty()) {
                                Column {
                                    Text(
                                        text = "📌 PINNED TEMPLATES",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.sp
                                        ),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        pinnedPrompts.take(3).forEachIndexed { idx, p ->
                                            val category = categories.find { it.id == p.categoryId }
                                            PromptCard(
                                                prompt = p,
                                                categoryName = category?.name,
                                                categoryIcon = category?.iconName,
                                                index = idx,
                                                onCardClick = { activeDetailPrompt = p },
                                                onFavoriteToggle = { viewModel.toggleFavorite(p) },
                                                onPinToggle = { viewModel.togglePin(p) },
                                                onCopyClick = {
                                                    clipboardManager.setText(AnnotatedString(p.content))
                                                    viewModel.recordPromptCopy(p)
                                                    Toast.makeText(context, "Copied content!", Toast.LENGTH_SHORT).show()
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            // 4. Recently Used Templates
                            val recentlyUsed = prompts.filter { it.lastUsedAt != null }
                                .sortedByDescending { it.lastUsedAt }
                            if (recentlyUsed.isNotEmpty()) {
                                Column {
                                    Text(
                                        text = "⏱️ RECENTLY COPIED & USED",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.sp
                                        ),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        recentlyUsed.take(3).forEachIndexed { idx, p ->
                                            val category = categories.find { it.id == p.categoryId }
                                            PromptCard(
                                                prompt = p,
                                                categoryName = category?.name,
                                                categoryIcon = category?.iconName,
                                                index = idx + 10,
                                                onCardClick = { activeDetailPrompt = p },
                                                onFavoriteToggle = { viewModel.toggleFavorite(p) },
                                                onPinToggle = { viewModel.togglePin(p) },
                                                onCopyClick = {
                                                    clipboardManager.setText(AnnotatedString(p.content))
                                                    viewModel.recordPromptCopy(p)
                                                    Toast.makeText(context, "Copied content!", Toast.LENGTH_SHORT).show()
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            // 5. Popular Templates
                            val popularPrompts = prompts.filter { it.usageCount > 0 }
                                .sortedByDescending { it.usageCount }
                            if (popularPrompts.isNotEmpty()) {
                                Column {
                                    Text(
                                        text = "🔥 MOST POPULAR VAULT",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.sp
                                        ),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        popularPrompts.take(3).forEachIndexed { idx, p ->
                                            val category = categories.find { it.id == p.categoryId }
                                            PromptCard(
                                                prompt = p,
                                                categoryName = category?.name,
                                                categoryIcon = category?.iconName,
                                                index = idx + 20,
                                                onCardClick = { activeDetailPrompt = p },
                                                onFavoriteToggle = { viewModel.toggleFavorite(p) },
                                                onPinToggle = { viewModel.togglePin(p) },
                                                onCopyClick = {
                                                    clipboardManager.setText(AnnotatedString(p.content))
                                                    viewModel.recordPromptCopy(p)
                                                    Toast.makeText(context, "Copied content!", Toast.LENGTH_SHORT).show()
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            // Decorative Quote
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "“Never Lose Your Next Great Prompt.”",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontFamily = FontFamily.SansSerif,
                                        color = MaterialTheme.colorScheme.outline,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "PROTES OFFLINE VAULT SYSTEM",
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.outline,
                                        letterSpacing = 1.sp
                                    )
                                }
                            }
                        }
                    } else {
                        // Show Dynamic Search Results Feed
                        if (prompts.isEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .wrapContentSize(Alignment.Center),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Search,
                                    contentDescription = "No match",
                                    modifier = Modifier.size(56.dp),
                                    tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "No prompt templates found",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Try adjusting labels, filters, or spelling criteria.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.outline,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 24.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = {
                                        localQuery = ""
                                        viewModel.clearAllFilters()
                                    },
                                    shape = RoundedCornerShape(24.dp)
                                ) {
                                    Icon(Icons.Outlined.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Clear Filters")
                                }
                            }
                        } else {
                            LazyColumn(
                                state = listState,
                                contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("search_results_feed")
                            ) {
                                itemsIndexed(prompts, key = { _, prompt -> prompt.id }) { index, prompt ->
                                    val category = categories.find { it.id == prompt.categoryId }

                                    PromptCard(
                                        prompt = prompt,
                                        categoryName = category?.name,
                                        categoryIcon = category?.iconName,
                                        index = index,
                                        highlightQuery = localQuery,
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
            }
        }

        // Beautiful distraction-free Detail overlay screen inside Search
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
