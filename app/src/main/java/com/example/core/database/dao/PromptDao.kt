package com.example.core.database.dao

import androidx.room.*
import com.example.core.model.Prompt
import kotlinx.coroutines.flow.Flow

@Dao
interface PromptDao {
    @Query("SELECT * FROM prompts WHERE isDeleted = 0 AND isArchived = 0 ORDER BY isPinned DESC, updatedAt DESC")
    fun getAllPrompts(): Flow<List<Prompt>>

    @Query("SELECT * FROM prompts WHERE isFavorite = 1 AND isDeleted = 0 AND isArchived = 0 ORDER BY isPinned DESC, updatedAt DESC")
    fun getFavoritePrompts(): Flow<List<Prompt>>

    @Query("SELECT * FROM prompts WHERE isPinned = 1 AND isDeleted = 0 AND isArchived = 0 ORDER BY updatedAt DESC")
    fun getPinnedPrompts(): Flow<List<Prompt>>

    @Query("SELECT * FROM prompts WHERE categoryId = :categoryId AND isDeleted = 0 AND isArchived = 0 ORDER BY isPinned DESC, updatedAt DESC")
    fun getPromptsByCategory(categoryId: String): Flow<List<Prompt>>

    @Query("SELECT * FROM prompts WHERE (collectionId = :collectionId OR collectionIdsCsv LIKE '%' || :collectionId || '%') AND isDeleted = 0 AND isArchived = 0 ORDER BY isPinned DESC, updatedAt DESC")
    fun getPromptsByCollection(collectionId: String): Flow<List<Prompt>>

    @Query("SELECT * FROM prompts WHERE isArchived = 1 AND isDeleted = 0 ORDER BY updatedAt DESC")
    fun getArchivedPrompts(): Flow<List<Prompt>>

    @Query("SELECT * FROM prompts WHERE isDeleted = 1 ORDER BY updatedAt DESC")
    fun getTrashPrompts(): Flow<List<Prompt>>

    @Query("SELECT * FROM prompts WHERE id = :id")
    fun getPromptById(id: String): Flow<Prompt?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrompt(prompt: Prompt)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrompts(prompts: List<Prompt>)

    @Query("SELECT * FROM prompts")
    suspend fun getRawAllPromptsList(): List<Prompt>

    @Update
    suspend fun updatePrompt(prompt: Prompt)

    @Delete
    suspend fun deletePrompt(prompt: Prompt)

    @Query("DELETE FROM prompts WHERE id = :id")
    suspend fun deletePromptById(id: String)

    @Query("SELECT * FROM prompts WHERE isDeleted = 0 AND isArchived = 0 AND (title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%') ORDER BY isPinned DESC, updatedAt DESC")
    fun searchPrompts(query: String): Flow<List<Prompt>>
}
