package com.example.core.settings

import android.content.Context
import android.content.SharedPreferences

object SettingsManager {
    private const val PREFS_NAME = "protes_app_settings"
    
    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // Appearance
    fun getTypographySize(context: Context): String = getPrefs(context).getString("typography_size", "Comfortable") ?: "Comfortable"
    fun setTypographySize(context: Context, value: String) = getPrefs(context).edit().putString("typography_size", value).apply()
    
    fun getCardDensity(context: Context): String = getPrefs(context).getString("card_density", "Normal") ?: "Normal"
    fun setCardDensity(context: Context, value: String) = getPrefs(context).edit().putString("card_density", value).apply()
    
    fun getAnimationSpeed(context: Context): String = getPrefs(context).getString("animation_speed", "Normal") ?: "Normal"
    fun setAnimationSpeed(context: Context, value: String) = getPrefs(context).edit().putString("animation_speed", value).apply()
    
    fun isReduceMotion(context: Context): Boolean = getPrefs(context).getBoolean("reduce_motion", false)
    fun setReduceMotion(context: Context, value: Boolean) = getPrefs(context).edit().putBoolean("reduce_motion", value).apply()

    // Workspace
    fun getDefaultHomeView(context: Context): String = getPrefs(context).getString("default_home_view", "Vision Board") ?: "Vision Board"
    fun setDefaultHomeView(context: Context, value: String) = getPrefs(context).edit().putString("default_home_view", value).apply()
    
    fun getDefaultWorkspaceView(context: Context): String = getPrefs(context).getString("default_workspace_view", "Grid") ?: "Grid"
    fun setDefaultWorkspaceView(context: Context, value: String) = getPrefs(context).edit().putString("default_workspace_view", value).apply()
    
    fun getRecentPromptsLimit(context: Context): Int = getPrefs(context).getInt("recent_prompts_limit", 20)
    fun setRecentPromptsLimit(context: Context, value: Int) = getPrefs(context).edit().putInt("recent_prompts_limit", value).apply()
    
    fun isShowPromptCovers(context: Context): Boolean = getPrefs(context).getBoolean("show_prompt_covers", true)
    fun setShowPromptCovers(context: Context, value: Boolean) = getPrefs(context).edit().putBoolean("show_prompt_covers", value).apply()
    
    fun isShowMetadata(context: Context): Boolean = getPrefs(context).getBoolean("show_metadata", true)
    fun setShowMetadata(context: Context, value: Boolean) = getPrefs(context).edit().putBoolean("show_metadata", value).apply()

    // Prompt Editor
    fun isAutoSave(context: Context): Boolean = getPrefs(context).getBoolean("auto_save", true)
    fun setAutoSave(context: Context, value: Boolean) = getPrefs(context).edit().putBoolean("auto_save", value).apply()
    
    fun isShowVariablesPanel(context: Context): Boolean = getPrefs(context).getBoolean("show_variables_panel", true)
    fun setShowVariablesPanel(context: Context, value: Boolean) = getPrefs(context).edit().putBoolean("show_variables_panel", value).apply()
    
    fun isMonospaceEditing(context: Context): Boolean = getPrefs(context).getBoolean("monospace_editing", false)
    fun setMonospaceEditing(context: Context, value: Boolean) = getPrefs(context).edit().putBoolean("monospace_editing", value).apply()
    
    fun getEditorFontSize(context: Context): String = getPrefs(context).getString("editor_font_size", "Medium") ?: "Medium"
    fun setEditorFontSize(context: Context, value: String) = getPrefs(context).edit().putString("editor_font_size", value).apply()

    // Search
    fun isRememberSearchHistory(context: Context): Boolean = getPrefs(context).getBoolean("remember_search_history", true)
    fun setRememberSearchHistory(context: Context, value: Boolean) = getPrefs(context).edit().putBoolean("remember_search_history", value).apply()
    
    fun getMaxRecentSearches(context: Context): Int = getPrefs(context).getInt("max_recent_searches", 10)
    fun setMaxRecentSearches(context: Context, value: Int) = getPrefs(context).edit().putInt("max_recent_searches", value).apply()
    
    fun isHighlightSearchMatches(context: Context): Boolean = getPrefs(context).getBoolean("highlight_search_matches", true)
    fun setHighlightSearchMatches(context: Context, value: Boolean) = getPrefs(context).edit().putBoolean("highlight_search_matches", value).apply()

    // Security (Session)
    fun getSessionTimeout(context: Context): String = getPrefs(context).getString("session_timeout", "Immediately") ?: "Immediately"
    fun setSessionTimeout(context: Context, value: String) = getPrefs(context).edit().putString("session_timeout", value).apply()
}
