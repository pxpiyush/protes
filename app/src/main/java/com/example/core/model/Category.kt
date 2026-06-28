package com.example.core.model

import androidx.compose.runtime.Stable
import androidx.room.Entity
import androidx.room.PrimaryKey

@Stable
@Entity(tableName = "categories")
data class Category(
    @PrimaryKey val id: String,
    val name: String,
    val iconName: String,
    val colorHex: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val boardSize: String = "large_square", // small_square, large_square, wide_rectangle, tall_rectangle, circle, rounded_capsule
    val orderIndex: Int = 0,
    val coverImageUri: String? = null,
    val isArchived: Boolean = false
)
