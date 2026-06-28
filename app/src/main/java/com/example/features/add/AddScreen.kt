package com.example.features.add

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.Prompt
import com.example.core.widgets.CategoryCard
import com.example.core.widgets.PromptCard
import com.example.core.widgets.ProtesButton
import com.example.core.widgets.ProtesInput
import com.example.features.ProtesViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddScreen(
    viewModel: ProtesViewModel,
    onSuccessNavigate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val editingPromptId by viewModel.editingPromptId.collectAsState()
    val prompts by viewModel.prompts.collectAsState()
    val archivedPrompts by viewModel.archivedPrompts.collectAsState()
    
    val editingPrompt = remember(editingPromptId, prompts, archivedPrompts) {
        if (editingPromptId != null) {
            prompts.find { it.id == editingPromptId } ?: archivedPrompts.find { it.id == editingPromptId }
        } else null
    }

    // Editable fields
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf<String?>(null) }
    var selectedModelIdsCsv by remember { mutableStateOf("") }
    var isFavorite by remember { mutableStateOf(false) }
    var isPinned by remember { mutableStateOf(false) }
    var tagsCsv by remember { mutableStateOf("") }
    var collectionIdsCsv by remember { mutableStateOf("") }
    var coverImageUri by remember { mutableStateOf<String?>(null) }
    var promptType by remember { mutableStateOf("Text") }

    // Phase 6 extensions
    var quality by remember { mutableStateOf("Draft") }
    var difficulty by remember { mutableStateOf("Beginner") }
    var notes by remember { mutableStateOf("") }
    var rating by remember { mutableStateOf(0) }
    var lastSuccess by remember { mutableStateOf("None") }
    var source by remember { mutableStateOf("Created by Me") }
    var isTemplate by remember { mutableStateOf(false) }
    var templateCategory by remember { mutableStateOf("Writing") }

    // Dropdowns and dialogs
    var showDiscardDialog by remember { mutableStateOf(false) }
    var showNewCollectionDialog by remember { mutableStateOf(false) }
    var newCollectionName by remember { mutableStateOf("") }

    val categories by viewModel.categories.collectAsState()
    val collections by viewModel.collections.collectAsState()
    val availableTags by viewModel.allTags.collectAsState()
    val context = LocalContext.current

    // Initialize fields when in Edit mode
    LaunchedEffect(editingPrompt) {
        if (editingPrompt != null) {
            title = editingPrompt.title
            description = editingPrompt.description
            content = editingPrompt.content
            selectedCategoryId = editingPrompt.categoryId
            isFavorite = editingPrompt.isFavorite
            isPinned = editingPrompt.isPinned
            tagsCsv = editingPrompt.tagsCsv
            collectionIdsCsv = editingPrompt.collectionIdsCsv.ifEmpty { editingPrompt.collectionId ?: "" }
            coverImageUri = editingPrompt.coverImageUri
            promptType = editingPrompt.promptType
            quality = editingPrompt.quality
            difficulty = editingPrompt.difficulty
            notes = editingPrompt.notes
            rating = editingPrompt.rating
            lastSuccess = editingPrompt.lastSuccess
            source = editingPrompt.source
            isTemplate = editingPrompt.isTemplate
            templateCategory = editingPrompt.templateCategory.ifEmpty { "Writing" }
        } else {
            // Reset to blank when not editing
            title = ""
            description = ""
            content = ""
            selectedCategoryId = viewModel.selectedCategoryId.value
            isFavorite = false
            isPinned = false
            tagsCsv = ""
            collectionIdsCsv = ""
            coverImageUri = null
            promptType = "Text"
            quality = "Draft"
            difficulty = "Beginner"
            notes = ""
            rating = 0
            lastSuccess = "None"
            source = "Created by Me"
            isTemplate = false
            templateCategory = "Writing"
        }
    }

    val hasUnsavedChanges = remember(
        title, description, content, selectedCategoryId, 
        isFavorite, isPinned, tagsCsv, collectionIdsCsv, coverImageUri, promptType,
        quality, difficulty, notes, rating, lastSuccess, source, isTemplate, templateCategory, editingPrompt
    ) {
        if (editingPrompt == null) {
            title.isNotEmpty() || description.isNotEmpty() || content.isNotEmpty()
        } else {
            title != editingPrompt.title ||
            description != editingPrompt.description ||
            content != editingPrompt.content ||
            selectedCategoryId != editingPrompt.categoryId ||
            isFavorite != editingPrompt.isFavorite ||
            isPinned != editingPrompt.isPinned ||
            tagsCsv != editingPrompt.tagsCsv ||
            collectionIdsCsv != (editingPrompt.collectionIdsCsv.ifEmpty { editingPrompt.collectionId ?: "" }) ||
            coverImageUri != editingPrompt.coverImageUri ||
            promptType != editingPrompt.promptType ||
            quality != editingPrompt.quality ||
            difficulty != editingPrompt.difficulty ||
            notes != editingPrompt.notes ||
            rating != editingPrompt.rating ||
            lastSuccess != editingPrompt.lastSuccess ||
            source != editingPrompt.source ||
            isTemplate != editingPrompt.isTemplate ||
            templateCategory != editingPrompt.templateCategory
        }
    }

    val resetAndNavigate: () -> Unit = {
        title = ""
        description = ""
        content = ""
        selectedCategoryId = null
        isFavorite = false
        isPinned = false
        tagsCsv = ""
        collectionIdsCsv = ""
        coverImageUri = null
        promptType = "Text"
        quality = "Draft"
        difficulty = "Beginner"
        notes = ""
        rating = 0
        lastSuccess = "None"
        source = "Created by Me"
        isTemplate = false
        templateCategory = "Writing"
        viewModel.stopEditing()
        onSuccessNavigate()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        // Edit Mode Header / Title
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (editingPrompt != null) "Edit Prompt" else "Draft Prompt",
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = if (editingPrompt != null) "Refining \"${editingPrompt.title}\" dynamically" else "Craft prompts inside your premium notebook vault.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            if (editingPrompt != null) {
                IconButton(onClick = {
                    if (hasUnsavedChanges) {
                        showDiscardDialog = true
                    } else {
                        resetAndNavigate()
                    }
                }) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Cancel edit",
                        tint = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Notebook Container
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                    shape = RoundedCornerShape(16.dp)
                ),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Toggles row (Pin, Favorite, and Prompt Type)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Type selector
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "TYPE:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        listOf("Text", "Code", "Image").forEach { type ->
                            val isTypeSelected = promptType == type
                            Surface(
                                modifier = Modifier
                                    .padding(end = 4.dp)
                                    .clickable { promptType = type },
                                color = if (isTypeSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = type,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    color = if (isTypeSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Row {
                        IconButton(onClick = { isPinned = !isPinned }) {
                            Icon(
                                imageVector = Icons.Outlined.PushPin,
                                contentDescription = "Pin draft",
                                tint = if (isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            )
                        }
                        IconButton(onClick = { isFavorite = !isFavorite }) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Outlined.Star else Icons.Outlined.StarBorder,
                                contentDescription = "Favorite draft",
                                tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Title Input
                ProtesInput(
                    value = title,
                    onValueChange = { title = it },
                    label = "Title",
                    placeholder = "e.g., Code Refactoring Review",
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Description Input
                ProtesInput(
                    value = description,
                    onValueChange = { description = it },
                    label = "Brief Description",
                    placeholder = "e.g., Technical design review checking for recompositions...",
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Prompt Content Input
                ProtesInput(
                    value = content,
                    onValueChange = { content = it },
                    label = "Prompt Body",
                    placeholder = "e.g., Review the following [[code]]...",
                    singleLine = false,
                    minLines = 6,
                    supportingText = "Use {{variable}} or [[placeholder]] for smart variable replacement.",
                    visualTransformation = com.example.core.widgets.VariableHighlightTransformation(MaterialTheme.colorScheme.primary)
                )

                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Tag System
        Text(
            text = "Labels",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Type comma-separated labels for your prompt.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )
        Spacer(modifier = Modifier.height(12.dp))
        ProtesInput(
            value = tagsCsv,
            onValueChange = { tagsCsv = it },
            label = "Prompt Labels",
            placeholder = "e.g., system, performance, creative",
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val premadeLabels = listOf("Gemini", "ChatGPT", "Claude")
            val allAvailable = (premadeLabels + availableTags).distinct()
            allAvailable.take(8).forEach { tag ->
                val isPresent = tagsCsv.split(",").map { it.trim() }.contains(tag)
                InputChip(
                    selected = isPresent,
                    onClick = {
                        val currentList = tagsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toMutableList()
                        if (isPresent) {
                            currentList.remove(tag)
                        } else {
                            currentList.add(tag)
                        }
                        tagsCsv = currentList.distinct().joinToString(", ")
                    },
                    label = { Text(tag) }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Collection System
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Add to Collections",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            TextButton(onClick = { showNewCollectionDialog = true }) {
                Icon(imageVector = Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("New Collection")
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        if (collections.isEmpty()) {
            Text(
                text = "No collections created yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline
            )
        } else {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                collections.forEach { col ->
                    val isSelected = collectionIdsCsv.split(",").map { it.trim() }.contains(col.id)
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            val currentCols = collectionIdsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toMutableList()
                            if (isSelected) {
                                currentCols.remove(col.id)
                            } else {
                                currentCols.add(col.id)
                            }
                            collectionIdsCsv = currentCols.distinct().joinToString(",")
                        },
                        label = { Text(col.name) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        var showCreateCategoryDialog by remember { mutableStateOf(false) }
        var newCategoryName by remember { mutableStateOf("") }
        if (showCreateCategoryDialog) {
            AlertDialog(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                onDismissRequest = { showCreateCategoryDialog = false },
                title = { Text("New Category") },
                text = {
                    OutlinedTextField(
                        value = newCategoryName,
                        onValueChange = { newCategoryName = it },
                        label = { Text("Category Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (newCategoryName.isNotBlank()) {
                                viewModel.createCategory(name = newCategoryName.trim())
                                newCategoryName = ""
                                showCreateCategoryDialog = false
                            }
                        }
                    ) { Text("Create") }
                },
                dismissButton = {
                    TextButton(onClick = { showCreateCategoryDialog = false }) { Text("Cancel") }
                }
            )
        }

        // Category Selection Row
        Text(
            text = "Select Category",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(12.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                Surface(
                    modifier = Modifier
                        .height(80.dp)
                        .width(100.dp)
                        .clickable { showCreateCategoryDialog = true }
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha=0.5f), RoundedCornerShape(12.dp)),
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = "New Category",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "New",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            items(categories) { category ->
                CategoryCard(
                    name = category.name,
                    iconName = category.iconName,
                    isSelected = selectedCategoryId == category.id,
                    onClick = {
                        selectedCategoryId = if (selectedCategoryId == category.id) null else category.id
                    },
                    modifier = Modifier.width(100.dp).height(80.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Optional Cover Image
        Text(
            text = "Optional Cover Image",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Upload a cover image for this prompt.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )
        Spacer(modifier = Modifier.height(12.dp))
        val imagePicker = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            if (uri != null) {
                coverImageUri = uri.toString()
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(onClick = { imagePicker.launch("image/*") }) {
                Text("Select File")
            }
            Spacer(modifier = Modifier.width(12.dp))
            if (coverImageUri != null) {
                Text("Image selected", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = { coverImageUri = null }) {
                    Icon(imageVector = Icons.Outlined.Close, contentDescription = "Remove Image")
                }
            } else {
                Text("No file selected", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Real-Time Live Preview Card
        Text(
            text = "LIVE PREVIEW",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            ),
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "This is exactly how it will render in your curated vaults.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )
        Spacer(modifier = Modifier.height(16.dp))

        val livePrompt = remember(title, content, description, selectedCategoryId, isFavorite, isPinned, tagsCsv, collectionIdsCsv, coverImageUri, promptType) {
            Prompt(
                id = editingPromptId ?: "live_draft",
                title = if (title.isBlank()) "Draft Prompt" else title,
                content = if (content.isBlank()) "Start drafting above to see your real-time layout card update instantly..." else content,
                description = description,
                categoryId = selectedCategoryId,
                isFavorite = isFavorite,
                isPinned = isPinned,
                placeholdersCsv = "",
                tagsCsv = tagsCsv,
                collectionIdsCsv = collectionIdsCsv,
                coverImageUri = coverImageUri,
                promptType = promptType
            )
        }
        val targetCat = categories.find { it.id == selectedCategoryId }

        PromptCard(
            prompt = livePrompt,
            categoryName = targetCat?.name,
            categoryIcon = targetCat?.iconName,
            index = 0,
            onCardClick = {},
            onFavoriteToggle = { isFavorite = !isFavorite },
            onPinToggle = { isPinned = !isPinned },
            onCopyClick = {}
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Action Deposit Button
        ProtesButton(
            onClick = {
                if (title.isBlank() || content.isBlank()) {
                    Toast.makeText(context, "Title and content cannot be empty.", Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.savePrompt(
                        id = editingPrompt?.id,
                        title = title,
                        content = content,
                        description = description,
                        categoryId = selectedCategoryId,
                        isFavorite = isFavorite,
                        isPinned = isPinned,
                        tagsCsv = tagsCsv,
                        collectionIdsCsv = collectionIdsCsv,
                        coverImageUri = coverImageUri,
                        promptType = promptType,
                        quality = quality,
                        difficulty = difficulty,
                        notes = notes,
                        rating = rating,
                        lastSuccess = lastSuccess,
                        source = source,
                        isTemplate = isTemplate,
                        templateCategory = templateCategory
                    )
                    
                    val toastMessage = if (editingPrompt != null) "Updated successfully!" else "Saved prompt!"
                    Toast.makeText(context, toastMessage, Toast.LENGTH_SHORT).show()

                    resetAndNavigate()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(if (editingPrompt != null) "Save Changes" else "Save Prompt")
        }
    }

    // Unsaved Changes Discard Dialog
    if (showDiscardDialog) {
        AlertDialog(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Discard Unsaved Changes?") },
            text = { Text("You have refined this prompt draft. Exiting now will lose all your modifications.") },
            confirmButton = {
                TextButton(onClick = {
                    showDiscardDialog = false
                    resetAndNavigate()
                }) {
                    Text("Discard", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text("Continue Editing")
                }
            }
        )
    }

    // New Collection Dialog
    if (showNewCollectionDialog) {
        AlertDialog(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            onDismissRequest = { showNewCollectionDialog = false },
            title = { Text("New Collection") },
            text = {
                Column {
                    Text("Group your master templates into modern Pinterest-style collections.")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newCollectionName,
                        onValueChange = { newCollectionName = it },
                        label = { Text("Collection Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newCollectionName.isNotBlank()) {
                            viewModel.createCollection(newCollectionName)
                            Toast.makeText(context, "Created collection: $newCollectionName", Toast.LENGTH_SHORT).show()
                            newCollectionName = ""
                            showNewCollectionDialog = false
                        }
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewCollectionDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
