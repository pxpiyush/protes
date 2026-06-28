package com.example.core.widgets

import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class BoardShape {
    SMALL_SQUARE,
    LARGE_SQUARE,
    WIDE_RECTANGLE,
    TALL_RECTANGLE,
    CIRCLE,
    ROUNDED_CAPSULE
}

/**
 * Premium, customizable, responsive Category Card for the Curated Vision Board.
 * Designed like Pinterest + Milanote + Apple Freeform.
 * Supports tactile touch scaling, edit mode animations, typographic placeholder covers,
 * and direct actions for resizing, reordering, and deleting.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CategoryCard(
    name: String,
    iconName: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    shapeType: BoardShape = BoardShape.SMALL_SQUARE,
    promptCount: Int = 0,
    coverImageUri: String? = null,
    createdAt: Long = System.currentTimeMillis(),
    isEditingMode: Boolean = false,
    onResizeClick: (() -> Unit)? = null,
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null,
    onArchiveClick: (() -> Unit)? = null,
    onDeleteClick: (() -> Unit)? = null,
    onChooseCoverClick: (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Gentle floating wiggle animation when in Edit Mode (similar to iOS app arranging)
    val infiniteTransition = rememberInfiniteTransition(label = "wiggle_transition")
    val wiggleRotation by infiniteTransition.animateFloat(
        initialValue = -1.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(120, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wiggle_rotation"
    )

    // Lift and scale on press, or slightly scale and wiggle if in editing mode
    val scaleFactor by animateFloatAsState(
        targetValue = when {
            isPressed -> 1.06f
            isEditingMode -> 1.03f
            isSelected -> 1.02f
            else -> 1.0f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "lift_scale"
    )

    val cardShadowElevation by animateDpAsState(
        targetValue = when {
            isPressed -> 8.dp
            isEditingMode -> 6.dp
            isSelected -> 3.dp
            else -> 1.dp
        },
        animationSpec = tween(150),
        label = "shadow_elevation"
    )

    val shape = when (shapeType) {
        BoardShape.CIRCLE -> CircleShape
        BoardShape.ROUNDED_CAPSULE -> RoundedCornerShape(55.dp)
        BoardShape.LARGE_SQUARE -> RoundedCornerShape(24.dp)
        BoardShape.WIDE_RECTANGLE, BoardShape.TALL_RECTANGLE -> RoundedCornerShape(24.dp)
        BoardShape.SMALL_SQUARE -> RoundedCornerShape(16.dp)
    }

    val height = when (shapeType) {
        BoardShape.SMALL_SQUARE -> 120.dp
        BoardShape.LARGE_SQUARE -> 256.dp
        BoardShape.WIDE_RECTANGLE -> 120.dp
        BoardShape.TALL_RECTANGLE -> 256.dp
        BoardShape.CIRCLE -> 120.dp
        BoardShape.ROUNDED_CAPSULE -> 120.dp
    }

    val widthModifier = when (shapeType) {
        BoardShape.CIRCLE -> Modifier.size(120.dp)
        BoardShape.SMALL_SQUARE -> Modifier.height(120.dp)
        else -> Modifier.height(height)
    }

    val outlineColor = when {
        isEditingMode -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        isSelected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
    }

    val backgroundColor = when {
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surface
    }

    // Dynamic Editorial Typography Placeholder Background
    val firstLetter = name.firstOrNull()?.uppercaseChar()?.toString() ?: ""
    val formattedDate = remember(createdAt) {
        val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        "Active: " + sdf.format(Date(createdAt))
    }

    Surface(
        modifier = modifier
            .then(widthModifier)
            .scale(scaleFactor)
            .then(
                if (isEditingMode) {
                    Modifier.graphicsLayer {
                        rotationZ = wiggleRotation
                    }
                } else Modifier
            )
            .shadow(
                elevation = cardShadowElevation,
                shape = shape,
                clip = true
            )
            .border(
                width = if (isEditingMode) 2.dp else if (isSelected) 1.5.dp else 1.dp,
                color = outlineColor,
                shape = shape
            )
            .combinedClickable(
                interactionSource = interactionSource,
                indication = androidx.compose.foundation.LocalIndication.current,
                onClick = onClick,
                onLongClick = onLongClick
            )
            .testTag("category_card_${name.lowercase().replace(" ", "_")}"),
        color = backgroundColor,
        shape = shape
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            
            // 1. Dynamic background cover or Typographic editorial placeholder
            if (!coverImageUri.isNullOrEmpty()) {
                // Background cover styling - minimal editorial monochrome style
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))
                ) {
                    // Show a stylized background design/pattern depending on the cover string
                    Text(
                        text = coverImageUri.uppercase(),
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                        ),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp)
                    )
                }
            } else {
                // TYPOGRAPHIC EMBOSSED PLACEHOLDER COVER (Pinterest/Milanote style)
                // Render a gorgeous oversized single serif character in the background
                if (shapeType == BoardShape.LARGE_SQUARE || shapeType == BoardShape.TALL_RECTANGLE) {
                    Text(
                        text = firstLetter,
                        fontSize = 140.sp,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = 10.dp, y = 20.dp)
                    )
                } else if (shapeType == BoardShape.ROUNDED_CAPSULE || shapeType == BoardShape.WIDE_RECTANGLE) {
                    Text(
                        text = name.uppercase(),
                        fontSize = 32.sp,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f),
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .offset(x = (-10).dp)
                    )
                }
            }

            // 2. Card Content Layout based on Shape Size to ensure maximum visual beauty
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        horizontal = if (shapeType == BoardShape.CIRCLE) 12.dp else 16.dp,
                        vertical = if (shapeType == BoardShape.CIRCLE) 12.dp else 14.dp
                    ),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Row: Minimal monochrome icon and prompt count
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val icon = getCategoryIcon(iconName)
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(if (shapeType == BoardShape.CIRCLE) 18.dp else 20.dp),
                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )

                    Surface(
                        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = promptCount.toString(),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                // Middle/Bottom Area: Name, date, action buttons
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = name,
                        style = when (shapeType) {
                            BoardShape.LARGE_SQUARE -> MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                            BoardShape.TALL_RECTANGLE -> MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            BoardShape.CIRCLE -> MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            else -> MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                        },
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = if (shapeType == BoardShape.LARGE_SQUARE || shapeType == BoardShape.TALL_RECTANGLE) 3 else 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = if (shapeType == BoardShape.CIRCLE) Modifier.fillMaxWidth() else Modifier
                    )

                    // Optional details (last updated, metadata) only shown in larger shapes
                    if (shapeType == BoardShape.LARGE_SQUARE || shapeType == BoardShape.TALL_RECTANGLE) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = formattedDate,
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            // 3. EDIT MODE CONTROL OVERLAY (Accessible resize, reorder, delete actions)
            if (isEditingMode) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.08f))
                ) {
                    // Small floating button row at the top or center of the card
                    Row(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .background(
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                                shape = RoundedCornerShape(24.dp)
                            )
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Cycle Resize Button
                        if (onResizeClick != null) {
                            IconButton(
                                onClick = onResizeClick,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.AspectRatio,
                                    contentDescription = "Resize Board Card",
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        // 2. Choose Cover Button
                        if (onChooseCoverClick != null) {
                            IconButton(
                                onClick = onChooseCoverClick,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Brush,
                                    contentDescription = "Choose Cover Pattern",
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        // 3. Move Up (Left) Accessible Button
                        if (onMoveUp != null) {
                            IconButton(
                                onClick = onMoveUp,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ArrowBack,
                                    contentDescription = "Move Category Backward",
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        // 4. Move Down (Right) Accessible Button
                        if (onMoveDown != null) {
                            IconButton(
                                onClick = onMoveDown,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ArrowForward,
                                    contentDescription = "Move Category Forward",
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        // 5. Archive Button
                        if (onArchiveClick != null) {
                            IconButton(
                                onClick = onArchiveClick,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Archive,
                                    contentDescription = "Archive Category",
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.outline
                                )
                            }
                        }

                        // 6. Delete Button (only if empty)
                        if (onDeleteClick != null && promptCount == 0) {
                            IconButton(
                                onClick = onDeleteClick,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.DeleteOutline,
                                    contentDescription = "Delete Empty Category",
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

fun getCategoryIcon(iconName: String): ImageVector {
    return when (iconName) {
        "Code", "code" -> Icons.Outlined.Code
        "Create", "create" -> Icons.Outlined.Create
        "Folder", "folder" -> Icons.Outlined.Folder
        "TrendingUp", "trendingup" -> Icons.Outlined.TrendingUp
        "CheckCircle", "checkcircle" -> Icons.Outlined.CheckCircle
        "Work", "work" -> Icons.Outlined.WorkOutline
        "School", "school" -> Icons.Outlined.School
        "Home", "home" -> Icons.Outlined.Home
        "Lightbulb", "lightbulb" -> Icons.Outlined.Lightbulb
        "Settings", "settings" -> Icons.Outlined.Settings
        else -> Icons.Outlined.Folder
    }
}
