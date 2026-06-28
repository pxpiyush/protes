package com.example.core.widgets

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.Category
import com.example.core.model.Prompt
import com.example.features.ProtesViewModel
import kotlinx.coroutines.launch

/**
 * Creative Signature Vision Board of Protes.
 * Renders an asymmetric, Pinterest/Milanote style workspace on an invisible grid.
 * Can be toggled to a Compact List.
 * Fully supports Edit Mode with reordering, resizing, and customizing of cards.
 */
@OptIn(ExperimentalAnimationApi::class, ExperimentalLayoutApi::class)
@Composable
fun CategoryBoard(
    viewModel: ProtesViewModel,
    categories: List<Category>,
    allPrompts: List<Prompt>,
    selectedCategoryId: String?,
    onCategorySelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val userPrefs by viewModel.userPreferences.collectAsState()
    
    // Board view mode state (board vs. list) - persists to UserPreferences
    val viewMode = userPrefs.boardViewMode

    // Editing mode state (triggered via long press on any card or floating customize action)
    var isEditingMode by remember { mutableStateOf(false) }

    // Cover picker states
    var showCoverPickerDialog by remember { mutableStateOf<Category?>(null) }

    Column(modifier = modifier.fillMaxWidth()) {
        // 1. Board Header & Interactive Mode Controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "PROMPT BOARDS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = if (isEditingMode) "Rearrange, resize, or style your workspace" else "Your curated workspaces and ideas",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            // Quick Toggle Controls (Board vs. List, and Enter Edit Mode)
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Edit Mode Toggle
                IconButton(
                    onClick = { isEditingMode = !isEditingMode },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = if (isEditingMode) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else androidx.compose.ui.graphics.Color.Transparent
                    ),
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (isEditingMode) Icons.Outlined.Check else Icons.Outlined.Tune,
                        contentDescription = "Edit Board Layout",
                        modifier = Modifier.size(18.dp),
                        tint = if (isEditingMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }

                // View Mode Toggle (Grid/Board vs. List)
                IconButton(
                    onClick = {
                        val target = if (viewMode == "board") "list" else "board"
                        viewModel.updateBoardViewMode(target)
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (viewMode == "board") Icons.Outlined.List else Icons.Outlined.GridView,
                        contentDescription = "Toggle View Mode",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Empty Board State handling
        val activeCategories = categories.filter { !it.isArchived }
        if (activeCategories.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.02f),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(24.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = "Start your creative workspace.",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Create your first category and snap it onto the board.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            // Animated visibility switch between Curated Vision Board and Compact List
            AnimatedContent(
                targetState = viewMode,
                transitionSpec = {
                    fadeIn(animationSpec = tween(250)) with fadeOut(animationSpec = tween(200))
                },
                label = "board_view_mode_transition"
            ) { targetMode ->
                if (targetMode == "board") {
                    // 1. SIGNATURE CURATED VISION BOARD (Responsive Invisible Grid)
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 1200.dp) // Bound height for nested scroll compatibility
                    ) {
                        itemsIndexed(
                            items = activeCategories,
                            key = { _, cat -> cat.id },
                            span = { _, category ->
                                val span = when (category.boardSize.lowercase()) {
                                    "large_square", "wide_rectangle", "rounded_capsule" -> 2
                                    else -> 1
                                }
                                GridItemSpan(span)
                            }
                        ) { index, category ->
                            val promptCount = allPrompts.count { it.categoryId == category.id }
                            val boardShape = when (category.boardSize.lowercase()) {
                                "small_square" -> BoardShape.SMALL_SQUARE
                                "large_square" -> BoardShape.LARGE_SQUARE
                                "wide_rectangle" -> BoardShape.WIDE_RECTANGLE
                                "tall_rectangle" -> BoardShape.TALL_RECTANGLE
                                "circle" -> BoardShape.CIRCLE
                                "rounded_capsule" -> BoardShape.ROUNDED_CAPSULE
                                else -> BoardShape.SMALL_SQUARE
                            }

                            CategoryCard(
                                modifier = Modifier.fillMaxWidth(),
                                name = category.name,
                                iconName = category.iconName,
                                isSelected = selectedCategoryId == category.id,
                                onClick = {
                                    if (!isEditingMode) {
                                        onCategorySelected(category.id)
                                    }
                                },
                                onLongClick = {
                                    isEditingMode = true
                                },
                                shapeType = boardShape,
                                promptCount = promptCount,
                                coverImageUri = category.coverImageUri,
                                createdAt = category.createdAt,
                                isEditingMode = isEditingMode,
                                onResizeClick = {
                                    // Cycle size through all 6 beautiful shapes
                                    val nextSize = when (category.boardSize.lowercase()) {
                                        "small_square" -> "large_square"
                                        "large_square" -> "wide_rectangle"
                                        "wide_rectangle" -> "tall_rectangle"
                                        "tall_rectangle" -> "circle"
                                        "circle" -> "rounded_capsule"
                                        else -> "small_square"
                                    }
                                    viewModel.updateCategorySize(category.id, nextSize)
                                },
                                onMoveUp = {
                                    // Move backward in visual grid ordering index
                                    if (index > 0) {
                                        viewModel.swapCategories(index, index - 1)
                                    }
                                },
                                onMoveDown = {
                                    // Move forward in visual grid ordering index
                                    if (index < activeCategories.size - 1) {
                                        viewModel.swapCategories(index, index + 1)
                                    }
                                },
                                onArchiveClick = {
                                    viewModel.toggleArchiveCategory(category.id)
                                    Toast.makeText(context, "${category.name} archived", Toast.LENGTH_SHORT).show()
                                },
                                onDeleteClick = {
                                    viewModel.deleteCategory(category.id,
                                        onSuccess = {
                                            Toast.makeText(context, "${category.name} deleted", Toast.LENGTH_SHORT).show()
                                        },
                                        onError = { error ->
                                            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                                        }
                                    )
                                },
                                onChooseCoverClick = {
                                    showCoverPickerDialog = category
                                }
                            )
                        }
                    }
                } else {
                    // 2. COMPACT LIST VIEW MODE
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        activeCategories.forEachIndexed { index, category ->
                            val promptCount = allPrompts.count { it.categoryId == category.id }
                            val isSelected = selectedCategoryId == category.id
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                        else MaterialTheme.colorScheme.surface
                                    )
                                    .border(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        onCategorySelected(category.id)
                                    }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = getCategoryIcon(category.iconName),
                                        contentDescription = null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = category.name,
                                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = "$promptCount prompts",
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Outlined.ChevronRight,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Beautiful monochrome preset cover selector dialog
    if (showCoverPickerDialog != null) {
        val category = showCoverPickerDialog!!
        AlertDialog(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            onDismissRequest = { showCoverPickerDialog = null },
            title = {
                Text(
                    text = "Choose Cover Pattern",
                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Select a minimal monochrome theme cover pattern for your board:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    val covers = listOf(
                        "Design" to "res:design",
                        "Creative Workspace" to "res:creative",
                        "Magazine Editorial" to "res:editorial",
                        "Typographic Clean" to "res:clean",
                        "No Cover (Use Giant Initials)" to ""
                    )

                    covers.forEach { (label, uri) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    viewModel.updateCategoryCover(category.id, uri.ifEmpty { null })
                                    showCoverPickerDialog = null
                                }
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(label, style = MaterialTheme.typography.bodyLarge)
                            if (category.coverImageUri == uri || (category.coverImageUri.isNullOrEmpty() && uri.isEmpty())) {
                                Icon(
                                    imageVector = Icons.Outlined.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCoverPickerDialog = null }) {
                    Text("Close")
                }
            }
        )
    }
}
