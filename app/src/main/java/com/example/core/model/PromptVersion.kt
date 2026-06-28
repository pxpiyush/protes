package com.example.core.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "prompt_versions")
data class PromptVersion(
    @PrimaryKey val id: String,
    val promptId: String,
    val title: String,
    val content: String,
    val description: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)
