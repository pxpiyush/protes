package com.example.core.database.dao

import androidx.room.*
import com.example.core.model.PromptHistory
import kotlinx.coroutines.flow.Flow

@Dao
interface PromptHistoryDao {
    @Query("SELECT * FROM prompt_history WHERE promptId = :promptId ORDER BY usedAt DESC")
    fun getHistoryForPrompt(promptId: String): Flow<List<PromptHistory>>

    @Query("SELECT * FROM prompt_history ORDER BY usedAt DESC")
    fun getAllHistory(): Flow<List<PromptHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: PromptHistory)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistories(histories: List<PromptHistory>)

    @Query("SELECT * FROM prompt_history")
    suspend fun getRawAllHistoryList(): List<PromptHistory>

    @Query("DELETE FROM prompt_history")
    suspend fun clearHistory()
}
