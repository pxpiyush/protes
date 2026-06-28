package com.example.core.widgets

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.core.model.Prompt
import com.example.core.theme.Motion
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class PromptCardStyle {
    IMAGE_COVER,       // Large abstract line-art cover layout
    TEXT_EDITORIAL,    // Magazine typography editorial layout
    COLLECTION_STACK,  // Pinterest-like stacked previews
    PINNED_HIGHLIGHT   // High-contrast highlighted layout
}

/**
 * Premium Magazine-Layout Card displaying Prompt content.
 * Alternates between various styles to keep the grid dynamic and curated,
 * matching Pinterest, Milanote, and modern typography layouts.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PromptCard(
    prompt: Prompt,
    categoryName: String?,
    categoryIcon: String?,
    onCardClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onPinToggle: () -> Unit,
    onCopyClick: () -> Unit,
    modifier: Modifier = Modifier,
    index: Int = 0,
    highlightQuery: String = ""
) {
    val context = LocalContext.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    var isLongPressed by remember { mutableStateOf(false) }
 
    // Soft bouncy spring scaling on touch (tiny scale reduction on press, springs back on release)
    val scaleFactor by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1.0f,
        animationSpec = spring(
            dampingRatio = 0.65f,
            stiffness = 350f
        ),
        label = "prompt_scale"
    )
 
    // Determine card style deterministically to avoid repeated layouts in feeds
    val cardStyle = remember(prompt.id, prompt.isPinned, index) {
        when {
            prompt.isPinned -> PromptCardStyle.PINNED_HIGHLIGHT
            index % 3 == 0 -> PromptCardStyle.IMAGE_COVER
            index % 3 == 1 -> PromptCardStyle.TEXT_EDITORIAL
            else -> PromptCardStyle.COLLECTION_STACK
        }
    }
 
    // Wrap in standard combinedClickable to trigger ripples, accessibility, and register press states correctly
    Box(
        modifier = modifier
            .scale(scaleFactor)
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onLongClick = {
                    isLongPressed = true
                    Motion.performHaptic(context, Motion.HapticStyle.Medium)
                },
                onClick = { onCardClick() }
            )
    ) {
        when (cardStyle) {
            PromptCardStyle.IMAGE_COVER -> {
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
                    Column {
                        // 1. Beautiful Abstract Monochrome Geometric Cover
                        val outlineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                        ) {
                            if (!prompt.coverImageUri.isNullOrEmpty()) {
                                AsyncImage(
                                    model = prompt.coverImageUri,
                                    contentDescription = "Cover Image",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val strokeWidth = 1.dp.toPx()
                                    // Draw an elegant fine grid / concentric lines pattern
                                    val steps = 12
                                    val center = Offset(size.width * 0.8f, size.height * 0.5f)
                                    for (i in 1..steps) {
                                        drawCircle(
                                            color = outlineColor,
                                            radius = (i * 15).dp.toPx(),
                                            center = center,
                                            style = Stroke(width = strokeWidth)
                                        )
                                    }
                                    // Subtle diagonal branding lines
                                    drawLine(
                                        color = outlineColor,
                                        start = Offset(0f, 0f),
                                        end = Offset(size.width, size.height),
                                        strokeWidth = strokeWidth
                                    )
                                }
                                
                                // Quotes Monogram Overlay
                                Text(
                                    text = "“",
                                    fontSize = 84.sp,
                                    fontFamily = FontFamily.SansSerif,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                    modifier = Modifier
                                        .align(Alignment.CenterStart)
                                        .padding(start = 24.dp, top = 24.dp)
                                )
                            }
                        }

                        // 2. Card Content
                        Column(modifier = Modifier.padding(16.dp)) {
                            HighlightedText(
                                text = prompt.title,
                                query = highlightQuery,
                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (prompt.description.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                HighlightedText(
                                    text = prompt.description,
                                    query = highlightQuery,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.fillMaxWidth(),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            ActionsRow(
                                isPinned = prompt.isPinned,
                                isFavorite = prompt.isFavorite,
                                onPinToggle = onPinToggle,
                                onFavoriteToggle = onFavoriteToggle,
                                onCopyClick = onCopyClick
                            )
                        }
                    }
                }
            }

            PromptCardStyle.TEXT_EDITORIAL -> {
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (categoryName != null && categoryIcon != null) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = getCategoryIcon(categoryIcon),
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.outline
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = categoryName.uppercase(),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.outline,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        HighlightedText(
                            text = prompt.title,
                            query = highlightQuery,
                            style = MaterialTheme.typography.displaySmall.copy(
                                fontSize = 22.sp,
                                lineHeight = 28.sp,
                                fontWeight = FontWeight.Normal
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Render beautiful inline code formatting or prompt snippet
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            HighlightedText(
                                text = prompt.content,
                                query = highlightQuery,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        ActionsRow(
                            isPinned = prompt.isPinned,
                            isFavorite = prompt.isFavorite,
                            onPinToggle = onPinToggle,
                            onFavoriteToggle = onFavoriteToggle,
                            onCopyClick = onCopyClick
                        )
                    }
                }
            }

            PromptCardStyle.COLLECTION_STACK -> {
                // pinterest style overlapping stacks
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp)
                ) {
                    // Back Sheet (stacked 2)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .height(140.dp)
                            .offset(y = 12.dp)
                            .align(Alignment.BottomCenter)
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                                RoundedCornerShape(16.dp)
                            ),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(16.dp)
                    ) {}

                    // Middle Sheet (stacked 1)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth(0.95f)
                            .height(140.dp)
                            .offset(y = 6.dp)
                            .align(Alignment.BottomCenter)
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                RoundedCornerShape(16.dp)
                            ),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(16.dp)
                    ) {}

                    // Front Sheet
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
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Outlined.FolderCopy,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "COLLECTION",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            HighlightedText(
                                text = prompt.title,
                                query = highlightQuery,
                                style = MaterialTheme.typography.headlineLarge,
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            if (prompt.description.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                HighlightedText(
                                    text = prompt.description,
                                    query = highlightQuery,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.fillMaxWidth(),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            ActionsRow(
                                isPinned = prompt.isPinned,
                                isFavorite = prompt.isFavorite,
                                onPinToggle = onPinToggle,
                                onFavoriteToggle = onFavoriteToggle,
                                onCopyClick = onCopyClick
                            )
                        }
                    }
                }
            }

            PromptCardStyle.PINNED_HIGHLIGHT -> {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(16.dp)
                        ),
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.PushPin,
                                    contentDescription = "Pinned",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "PINNED VAULT",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        HighlightedText(
                            text = prompt.title,
                            query = highlightQuery,
                            style = MaterialTheme.typography.headlineLarge.copy(fontSize = 22.sp),
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (prompt.description.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            HighlightedText(
                                text = prompt.description,
                                query = highlightQuery,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        ActionsRow(
                            isPinned = prompt.isPinned,
                            isFavorite = prompt.isFavorite,
                            onPinToggle = onPinToggle,
                            onFavoriteToggle = onFavoriteToggle,
                            onCopyClick = onCopyClick
                        )
                    }
                }
            }
        }
    }

    // long-press menu overlay state reset
    if (isLongPressed) {
        AlertDialog(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            onDismissRequest = { isLongPressed = false },
            title = { Text("Prompt Management", fontFamily = FontFamily.SansSerif) },
            text = { Text("Select action for \"${prompt.title}\":") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onCopyClick()
                        isLongPressed = false
                    }
                ) {
                    Text("Copy Content", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { isLongPressed = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ActionsRow(
    isPinned: Boolean,
    isFavorite: Boolean,
    onPinToggle: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onCopyClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isCopiedTransient by remember { mutableStateOf(false) }

    // Favorite star spring scale animation
    val favScale by animateFloatAsState(
        targetValue = if (isFavorite) 1.25f else 1.0f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 400f),
        label = "fav_scale"
    )

    // Pin spring scale & rotate animation
    val pinScale by animateFloatAsState(
        targetValue = if (isPinned) 1.25f else 1.0f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 400f),
        label = "pin_scale"
    )
    val pinRotation by animateFloatAsState(
        targetValue = if (isPinned) -25f else 0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 300f),
        label = "pin_rotation"
    )

    // Copy spring scale animation
    val copyScale by animateFloatAsState(
        targetValue = if (isCopiedTransient) 1.25f else 1.0f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 400f),
        label = "copy_scale"
    )

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    Motion.performHaptic(context, Motion.HapticStyle.Light)
                    onPinToggle()
                },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.PushPin,
                    contentDescription = "Pin prompt",
                    modifier = Modifier
                        .size(18.dp)
                        .graphicsLayer {
                            scaleX = pinScale
                            scaleY = pinScale
                            rotationZ = pinRotation
                        },
                    tint = if (isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                )
            }
            IconButton(
                onClick = {
                    Motion.performHaptic(context, Motion.HapticStyle.Light)
                    onFavoriteToggle()
                },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Outlined.Star else Icons.Outlined.StarBorder,
                    contentDescription = "Favorite prompt",
                    modifier = Modifier
                        .size(18.dp)
                        .graphicsLayer {
                            scaleX = favScale
                            scaleY = favScale
                        },
                    tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                )
            }
        }

        IconButton(
            onClick = {
                Motion.performHaptic(context, Motion.HapticStyle.Light)
                isCopiedTransient = true
                scope.launch {
                    delay(1200)
                    isCopiedTransient = false
                }
                onCopyClick()
            },
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = if (isCopiedTransient) Icons.Outlined.Check else Icons.Outlined.ContentCopy,
                contentDescription = "Copy content",
                modifier = Modifier
                    .size(18.dp)
                    .graphicsLayer {
                        scaleX = copyScale
                        scaleY = copyScale
                    },
                tint = if (isCopiedTransient) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun HighlightedText(
    text: String,
    query: String,
    style: androidx.compose.ui.text.TextStyle,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip
) {
    if (query.isEmpty() || !text.contains(query, ignoreCase = true)) {
        Text(text = text, style = style, modifier = modifier, maxLines = maxLines, overflow = overflow, color = style.color)
        return
    }

    val annotatedString = androidx.compose.ui.text.buildAnnotatedString {
        var startIdx = 0
        val queryLen = query.length
        val lowerText = text.lowercase()
        val lowerQuery = query.lowercase()

        while (true) {
            val idx = lowerText.indexOf(lowerQuery, startIdx)
            if (idx == -1) {
                append(text.substring(startIdx))
                break
            }
            append(text.substring(startIdx, idx))
            withStyle(style = androidx.compose.ui.text.SpanStyle(
                fontWeight = FontWeight.Bold,
                background = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                color = MaterialTheme.colorScheme.primary
            )) {
                append(text.substring(idx, idx + queryLen))
            }
            startIdx = idx + queryLen
        }
    }
    Text(text = annotatedString, style = style, modifier = modifier, maxLines = maxLines, overflow = overflow)
}
