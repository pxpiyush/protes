package com.example

import android.app.Application
import com.example.core.database.ProtesDatabase
import com.example.core.repository.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class ProtesApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob())

    val database by lazy { ProtesDatabase.getDatabase(this, applicationScope) }

    val promptRepository by lazy { PromptRepository(database.promptDao()) }
    val categoryRepository by lazy { CategoryRepository(database.categoryDao()) }
    val collectionRepository by lazy { CollectionRepository(database.collectionDao()) }
    val promptHistoryRepository by lazy { PromptHistoryRepository(database.promptHistoryDao()) }
    val promptVersionRepository by lazy { PromptVersionRepository(database.promptVersionDao()) }
    val userPreferencesRepository by lazy { UserPreferencesRepository(database.userPreferencesDao()) }
}
