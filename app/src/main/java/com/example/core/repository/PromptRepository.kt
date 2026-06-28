package com.example.core.repository

import com.example.core.database.dao.PromptDao
import com.example.core.model.Prompt
import kotlinx.coroutines.flow.Flow

class PromptRepository(private val promptDao: PromptDao) {
    val allPrompts: Flow<List<Prompt>> = promptDao.getAllPrompts()
    val favoritePrompts: Flow<List<Prompt>> = promptDao.getFavoritePrompts()
    val pinnedPrompts: Flow<List<Prompt>> = promptDao.getPinnedPrompts()
    val archivedPrompts: Flow<List<Prompt>> = promptDao.getArchivedPrompts()
    val trashPrompts: Flow<List<Prompt>> = promptDao.getTrashPrompts()

    fun getPromptsByCategory(categoryId: String): Flow<List<Prompt>> =
        promptDao.getPromptsByCategory(categoryId)

    fun getPromptsByCollection(collectionId: String): Flow<List<Prompt>> =
        promptDao.getPromptsByCollection(collectionId)

    fun getPromptById(id: String): Flow<Prompt?> =
        promptDao.getPromptById(id)

    suspend fun savePrompt(prompt: Prompt) {
        promptDao.insertPrompt(prompt)
    }

    suspend fun updatePrompt(prompt: Prompt) {
        promptDao.updatePrompt(prompt)
    }

    suspend fun deletePrompt(prompt: Prompt) {
        promptDao.deletePrompt(prompt)
    }

    suspend fun deletePromptById(id: String) {
        promptDao.deletePromptById(id)
    }

    fun searchPrompts(query: String): Flow<List<Prompt>> =
        promptDao.searchPrompts(query)
}
