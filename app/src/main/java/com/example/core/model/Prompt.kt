package com.example.core.model

import androidx.compose.runtime.Stable
import androidx.room.Entity
import androidx.room.PrimaryKey

@Stable
@Entity(tableName = "prompts")
data class Prompt(
    @PrimaryKey val id: String,
    val title: String,
    val content: String,
    val description: String = "",
    val categoryId: String? = null,
    val collectionId: String? = null,
    val isFavorite: Boolean = false,
    val isPinned: Boolean = false,
    val usageCount: Int = 0,
    val lastUsedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val placeholdersCsv: String = "",
    // Phase 3 additions
    val isArchived: Boolean = false,
    val isDeleted: Boolean = false,
    val tagsCsv: String = "",
    val collectionIdsCsv: String = "",
    val coverImageUri: String? = null,
    val promptType: String = "Text",
    // Phase 6 extensions
    val quality: String = "Draft", // Draft, Optimized, Production Ready, Favorite Workflow, Experimental
    val difficulty: String = "Beginner", // Beginner, Intermediate, Advanced, Expert
    val promptLengthType: String = "Short", // Short, Medium, Long, Very Long
    val estimatedTokens: Int = 0,
    val notes: String = "",
    val rating: Int = 0, // 0 to 5 stars
    val lastSuccess: String = "None", // None, Worked Great, Worked Okay, Needs Improvement
    val source: String = "Created by Me", // Created by Me, Imported, Copied, Template, Generated
    val isTemplate: Boolean = false,
    val templateCategory: String = "", // Writing, Image, Coding, Email, Business, Marketing, Education, Translation, Social, Custom
    val variablesCsv: String = ""
) {
    // Helper to extract place holders (e.g. text between double brackets [[topic]] or curly braces {topic})
    fun getPlaceholders(): List<String> {
        if (placeholdersCsv.isBlank()) return emptyList()
        return placeholdersCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    fun getVariables(): List<String> {
        if (variablesCsv.isBlank()) return emptyList()
        return variablesCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    fun getTags(): List<String> {
        if (tagsCsv.isBlank()) return emptyList()
        return tagsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    fun getCollectionIds(): List<String> {
        if (collectionIdsCsv.isBlank()) return emptyList()
        return collectionIdsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }
}
