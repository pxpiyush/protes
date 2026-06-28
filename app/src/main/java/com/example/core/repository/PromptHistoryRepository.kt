package com.example.core.repository

import com.example.core.database.dao.PromptHistoryDao
import com.example.core.model.PromptHistory
import kotlinx.coroutines.flow.Flow

class PromptHistoryRepository(private val promptHistoryDao: PromptHistoryDao) {
    val allHistory: Flow<List<PromptHistory>> = promptHistoryDao.getAllHistory()

    fun getHistoryForPrompt(promptId: String): Flow<List<PromptHistory>> =
        promptHistoryDao.getHistoryForPrompt(promptId)

    suspend fun saveHistory(history: PromptHistory) {
        promptHistoryDao.insertHistory(history)
    }

    suspend fun clearHistory() {
        promptHistoryDao.clearHistory()
    }
}
