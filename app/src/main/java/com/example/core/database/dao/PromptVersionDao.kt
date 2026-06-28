package com.example.core.database.dao

import androidx.room.*
import com.example.core.model.PromptVersion
import kotlinx.coroutines.flow.Flow

@Dao
interface PromptVersionDao {
    @Query("SELECT * FROM prompt_versions WHERE promptId = :promptId ORDER BY updatedAt DESC")
    fun getVersionsForPrompt(promptId: String): Flow<List<PromptVersion>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVersion(version: PromptVersion)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVersions(versions: List<PromptVersion>)

    @Query("SELECT * FROM prompt_versions")
    suspend fun getRawAllVersionsList(): List<PromptVersion>

    @Delete
    suspend fun deleteVersion(version: PromptVersion)

    @Query("DELETE FROM prompt_versions WHERE promptId = :promptId")
    suspend fun deleteVersionsForPrompt(promptId: String)
}
