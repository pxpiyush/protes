package com.example.core.repository

import com.example.core.database.dao.UserPreferencesDao
import com.example.core.model.UserPreferences
import kotlinx.coroutines.flow.Flow

class UserPreferencesRepository(private val userPreferencesDao: UserPreferencesDao) {
    val userPreferences: Flow<UserPreferences?> = userPreferencesDao.getUserPreferences()

    suspend fun saveUserPreferences(preferences: UserPreferences) {
        userPreferencesDao.insertUserPreferences(preferences)
    }
}
