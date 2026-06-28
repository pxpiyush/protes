package com.example.core.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_preferences")
data class UserPreferences(
    @PrimaryKey val id: String = "default_preferences",
    val themeMode: String = "light", // system, light, dark
    val isBiometricEnabled: Boolean = false,
    val isCloudBackupEnabled: Boolean = false,
    val lastBackupAt: Long? = null,
    val searchHistoryRaw: String = "",
    val boardViewMode: String = "board", // board, list
    val canvasZoom: Float = 1.0f,
    val canvasPanX: Float = 0.0f,
    val canvasPanY: Float = 0.0f
)
