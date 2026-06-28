package com.example.core.repository

import com.example.core.database.dao.PromptVersionDao
import com.example.core.model.PromptVersion
import kotlinx.coroutines.flow.Flow

class PromptVersionRepository(private val promptVersionDao: PromptVersionDao) {
    fun getVersionsForPrompt(promptId: String): Flow<List<PromptVersion>> =
        promptVersionDao.getVersionsForPrompt(promptId)

    suspend fun saveVersion(version: PromptVersion) {
        promptVersionDao.insertVersion(version)
    }

    suspend fun deleteVersionsForPrompt(promptId: String) {
        promptVersionDao.deleteVersionsForPrompt(promptId)
    }
}
