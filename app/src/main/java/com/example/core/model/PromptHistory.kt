package com.example.core.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "prompt_history")
data class PromptHistory(
    @PrimaryKey val id: String,
    val promptId: String,
    val usedAt: Long = System.currentTimeMillis(),
    val filledContent: String
)
