package com.example.core.backup

import android.content.Context
import android.net.Uri
import com.example.core.database.ProtesDatabase
import com.example.core.model.*
import com.example.core.security.EncryptionHelper
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*

object BackupManager {
    const val SCHEMA_VERSION = 1

    data class BackupInfo(
        val date: Long,
        val promptsCount: Int,
        val categoriesCount: Int,
        val collectionsCount: Int,
        val isEncrypted: Boolean
    )

    // Storage Usage Model
    data class StorageUsage(
        val databaseSize: Long,
        val backupsSize: Long,
        val promptsCount: Int,
        val collectionsCount: Int,
        val categoriesCount: Int,
        val templatesCount: Int,
        val lastBackupTime: Long?
    )

    // Backup History Model
    data class BackupFile(
        val name: String,
        val file: File,
        val size: Long,
        val lastModified: Long,
        val isEncrypted: Boolean
    )

    // Duplicate Analysis Results
    enum class DuplicateType {
        EXACT, NEAR, TITLE, BODY, NONE
    }

    data class ImportPromptCandidate(
        val tempId: String = UUID.randomUUID().toString(),
        val title: String,
        val content: String,
        val description: String = "",
        val categoryId: String? = null,
        val tagsCsv: String = "",
        val promptType: String = "Text",
        val isTemplate: Boolean = false,
        val templateCategory: String = "",
        val duplicateType: DuplicateType = DuplicateType.NONE,
        val duplicateId: String? = null
    )

    // 1. BACKUP DATABASE TO JSON STRING
    suspend fun generateBackupJson(context: Context, database: ProtesDatabase): String {
        val root = JSONObject()
        root.put("schemaVersion", SCHEMA_VERSION)
        root.put("backupDate", System.currentTimeMillis())
        root.put("appName", "Protes")

        // Prompts
        val promptsList = database.promptDao().getRawAllPromptsList()
        val promptsArray = JSONArray()
        for (p in promptsList) {
            val obj = JSONObject()
            obj.put("id", p.id)
            obj.put("title", p.title)
            obj.put("content", p.content)
            obj.put("description", p.description)
            obj.put("categoryId", p.categoryId)
            obj.put("collectionId", p.collectionId)
            obj.put("isFavorite", p.isFavorite)
            obj.put("isPinned", p.isPinned)
            obj.put("usageCount", p.usageCount)
            obj.put("lastUsedAt", p.lastUsedAt ?: JSONObject.NULL)
            obj.put("createdAt", p.createdAt)
            obj.put("updatedAt", p.updatedAt)
            obj.put("placeholdersCsv", p.placeholdersCsv)
            obj.put("isArchived", p.isArchived)
            obj.put("isDeleted", p.isDeleted)
            obj.put("tagsCsv", p.tagsCsv)
            obj.put("collectionIdsCsv", p.collectionIdsCsv)
            obj.put("coverImageUri", p.coverImageUri ?: JSONObject.NULL)
            obj.put("promptType", p.promptType)
            obj.put("quality", p.quality)
            obj.put("difficulty", p.difficulty)
            obj.put("promptLengthType", p.promptLengthType)
            obj.put("estimatedTokens", p.estimatedTokens)
            obj.put("notes", p.notes)
            obj.put("rating", p.rating)
            obj.put("lastSuccess", p.lastSuccess)
            obj.put("source", p.source)
            obj.put("isTemplate", p.isTemplate)
            obj.put("templateCategory", p.templateCategory)
            obj.put("variablesCsv", p.variablesCsv)
            promptsArray.put(obj)
        }
        root.put("prompts", promptsArray)

        // Categories
        val categoriesList = database.categoryDao().getRawAllCategoriesList()
        val categoriesArray = JSONArray()
        for (c in categoriesList) {
            val obj = JSONObject()
            obj.put("id", c.id)
            obj.put("name", c.name)
            obj.put("iconName", c.iconName)
            obj.put("colorHex", c.colorHex ?: JSONObject.NULL)
            obj.put("createdAt", c.createdAt)
            obj.put("boardSize", c.boardSize)
            obj.put("orderIndex", c.orderIndex)
            obj.put("coverImageUri", c.coverImageUri ?: JSONObject.NULL)
            obj.put("isArchived", c.isArchived)
            categoriesArray.put(obj)
        }
        root.put("categories", categoriesArray)

        // Collections
        val collectionsList = database.collectionDao().getRawAllCollectionsList()
        val collectionsArray = JSONArray()
        for (col in collectionsList) {
            val obj = JSONObject()
            obj.put("id", col.id)
            obj.put("name", col.name)
            obj.put("description", col.description)
            obj.put("createdAt", col.createdAt)
            collectionsArray.put(obj)
        }
        root.put("collections", collectionsArray)

        // History
        val historyList = database.promptHistoryDao().getRawAllHistoryList()
        val historyArray = JSONArray()
        for (h in historyList) {
            val obj = JSONObject()
            obj.put("id", h.id)
            obj.put("promptId", h.promptId)
            obj.put("usedAt", h.usedAt)
            obj.put("filledContent", h.filledContent)
            historyArray.put(obj)
        }
        root.put("history", historyArray)

        // Prompt Versions
        val versionsList = database.promptVersionDao().getRawAllVersionsList()
        val versionsArray = JSONArray()
        for (v in versionsList) {
            val obj = JSONObject()
            obj.put("id", v.id)
            obj.put("promptId", v.promptId)
            obj.put("title", v.title)
            obj.put("content", v.content)
            obj.put("description", v.description)
            obj.put("updatedAt", v.updatedAt)
            versionsArray.put(obj)
        }
        root.put("versions", versionsArray)

        // Settings (UserPreferences)
        val prefs = database.userPreferencesDao().getRawUserPreferences()
        if (prefs != null) {
            val obj = JSONObject()
            obj.put("themeMode", prefs.themeMode)
            obj.put("isBiometricEnabled", prefs.isBiometricEnabled)
            obj.put("boardViewMode", prefs.boardViewMode)
            obj.put("canvasZoom", prefs.canvasZoom.toDouble())
            obj.put("canvasPanX", prefs.canvasPanX.toDouble())
            obj.put("canvasPanY", prefs.canvasPanY.toDouble())
            root.put("settings", obj)
        }

        return root.toString(2)
    }

    // 2. PARSE BACKUP DETAILS
    fun readBackupInfo(jsonString: String): BackupInfo? {
        return try {
            val root = JSONObject(jsonString)
            val prompts = root.optJSONArray("prompts")?.length() ?: 0
            val categories = root.optJSONArray("categories")?.length() ?: 0
            val collections = root.optJSONArray("collections")?.length() ?: 0
            val date = root.optLong("backupDate", System.currentTimeMillis())
            BackupInfo(
                date = date,
                promptsCount = prompts,
                categoriesCount = categories,
                collectionsCount = collections,
                isEncrypted = false
            )
        } catch (e: Exception) {
            null
        }
    }

    // 3. RESTORE DATABASE FROM JSON (REPLACE Strategy)
    suspend fun restoreDatabaseReplace(database: ProtesDatabase, jsonString: String) {
        val root = JSONObject(jsonString)
        
        // Clear Existing Tables
        database.promptDao().getRawAllPromptsList().forEach { database.promptDao().deletePrompt(it) }
        database.categoryDao().getRawAllCategoriesList().forEach { database.categoryDao().deleteCategory(it) }
        database.collectionDao().getRawAllCollectionsList().forEach { database.collectionDao().deleteCollection(it) }
        database.promptHistoryDao().clearHistory()

        // 1. Categories
        val categoriesArray = root.optJSONArray("categories")
        if (categoriesArray != null) {
            val list = mutableListOf<Category>()
            for (i in 0 until categoriesArray.length()) {
                val obj = categoriesArray.getJSONObject(i)
                list.add(
                    Category(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        iconName = obj.getString("iconName"),
                        colorHex = if (obj.isNull("colorHex")) null else obj.getString("colorHex"),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        boardSize = obj.optString("boardSize", "large_square"),
                        orderIndex = obj.optInt("orderIndex", 0),
                        coverImageUri = if (obj.isNull("coverImageUri")) null else obj.getString("coverImageUri"),
                        isArchived = obj.optBoolean("isArchived", false)
                    )
                )
            }
            database.categoryDao().insertCategories(list)
        }

        // 2. Collections
        val collectionsArray = root.optJSONArray("collections")
        if (collectionsArray != null) {
            val list = mutableListOf<PromptCollection>()
            for (i in 0 until collectionsArray.length()) {
                val obj = collectionsArray.getJSONObject(i)
                list.add(
                    PromptCollection(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        description = obj.getString("description"),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
            database.collectionDao().insertCollections(list)
        }

        // 3. Prompts
        val promptsArray = root.optJSONArray("prompts")
        if (promptsArray != null) {
            val list = mutableListOf<Prompt>()
            for (i in 0 until promptsArray.length()) {
                val obj = promptsArray.getJSONObject(i)
                list.add(
                    Prompt(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        content = obj.getString("content"),
                        description = obj.optString("description", ""),
                        categoryId = if (obj.isNull("categoryId")) null else obj.getString("categoryId"),
                        collectionId = if (obj.isNull("collectionId")) null else obj.getString("collectionId"),
                        isFavorite = obj.optBoolean("isFavorite", false),
                        isPinned = obj.optBoolean("isPinned", false),
                        usageCount = obj.optInt("usageCount", 0),
                        lastUsedAt = if (obj.isNull("lastUsedAt")) null else obj.getLong("lastUsedAt"),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis()),
                        placeholdersCsv = obj.optString("placeholdersCsv", ""),
                        isArchived = obj.optBoolean("isArchived", false),
                        isDeleted = obj.optBoolean("isDeleted", false),
                        tagsCsv = obj.optString("tagsCsv", ""),
                        collectionIdsCsv = obj.optString("collectionIdsCsv", ""),
                        coverImageUri = if (obj.isNull("coverImageUri")) null else obj.getString("coverImageUri"),
                        promptType = obj.optString("promptType", "Text"),
                        quality = obj.optString("quality", "Draft"),
                        difficulty = obj.optString("difficulty", "Beginner"),
                        promptLengthType = obj.optString("promptLengthType", "Short"),
                        estimatedTokens = obj.optInt("estimatedTokens", 0),
                        notes = obj.optString("notes", ""),
                        rating = obj.optInt("rating", 0),
                        lastSuccess = obj.optString("lastSuccess", "None"),
                        source = obj.optString("source", "Created by Me"),
                        isTemplate = obj.optBoolean("isTemplate", false),
                        templateCategory = obj.optString("templateCategory", ""),
                        variablesCsv = obj.optString("variablesCsv", "")
                    )
                )
            }
            database.promptDao().insertPrompts(list)
        }

        // 5. History
        val historyArray = root.optJSONArray("history")
        if (historyArray != null) {
            val list = mutableListOf<PromptHistory>()
            for (i in 0 until historyArray.length()) {
                val obj = historyArray.getJSONObject(i)
                list.add(
                    PromptHistory(
                        id = obj.getString("id"),
                        promptId = obj.getString("promptId"),
                        usedAt = obj.getLong("usedAt"),
                        filledContent = obj.getString("filledContent")
                    )
                )
            }
            database.promptHistoryDao().insertHistories(list)
        }

        // 6. Versions
        val versionsArray = root.optJSONArray("versions")
        if (versionsArray != null) {
            val list = mutableListOf<PromptVersion>()
            for (i in 0 until versionsArray.length()) {
                val obj = versionsArray.getJSONObject(i)
                list.add(
                    PromptVersion(
                        id = obj.getString("id"),
                        promptId = obj.getString("promptId"),
                        title = obj.getString("title"),
                        content = obj.getString("content"),
                        description = obj.optString("description", ""),
                        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                    )
                )
            }
            database.promptVersionDao().insertVersions(list)
        }

        // 8. Settings
        val settingsObj = root.optJSONObject("settings")
        if (settingsObj != null) {
            val current = database.userPreferencesDao().getRawUserPreferences() ?: UserPreferences()
            database.userPreferencesDao().insertUserPreferences(
                current.copy(
                    themeMode = settingsObj.optString("themeMode", current.themeMode),
                    isBiometricEnabled = settingsObj.optBoolean("isBiometricEnabled", current.isBiometricEnabled),
                    boardViewMode = settingsObj.optString("boardViewMode", current.boardViewMode),
                    canvasZoom = settingsObj.optDouble("canvasZoom", current.canvasZoom.toDouble()).toFloat(),
                    canvasPanX = settingsObj.optDouble("canvasPanX", current.canvasPanX.toDouble()).toFloat(),
                    canvasPanY = settingsObj.optDouble("canvasPanY", current.canvasPanY.toDouble()).toFloat()
                )
            )
        }
    }

    // 4. RESTORE DATABASE FROM JSON (MERGE Strategy)
    suspend fun restoreDatabaseMerge(database: ProtesDatabase, jsonString: String) {
        val root = JSONObject(jsonString)

        // Merge Categories (keep existing, insert if missing)
        val categoriesArray = root.optJSONArray("categories")
        if (categoriesArray != null) {
            val existingIds = database.categoryDao().getRawAllCategoriesList().map { it.id }.toSet()
            val listToInsert = mutableListOf<Category>()
            for (i in 0 until categoriesArray.length()) {
                val obj = categoriesArray.getJSONObject(i)
                val id = obj.getString("id")
                if (!existingIds.contains(id)) {
                    listToInsert.add(
                        Category(
                            id = id,
                            name = obj.getString("name"),
                            iconName = obj.getString("iconName"),
                            colorHex = if (obj.isNull("colorHex")) null else obj.getString("colorHex"),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                            boardSize = obj.optString("boardSize", "large_square"),
                            orderIndex = obj.optInt("orderIndex", 0),
                            coverImageUri = if (obj.isNull("coverImageUri")) null else obj.getString("coverImageUri"),
                            isArchived = obj.optBoolean("isArchived", false)
                        )
                    )
                }
            }
            if (listToInsert.isNotEmpty()) database.categoryDao().insertCategories(listToInsert)
        }

        // Merge Collections
        val collectionsArray = root.optJSONArray("collections")
        if (collectionsArray != null) {
            val existingIds = database.collectionDao().getRawAllCollectionsList().map { it.id }.toSet()
            val listToInsert = mutableListOf<PromptCollection>()
            for (i in 0 until collectionsArray.length()) {
                val obj = collectionsArray.getJSONObject(i)
                val id = obj.getString("id")
                if (!existingIds.contains(id)) {
                    listToInsert.add(
                        PromptCollection(
                            id = id,
                            name = obj.getString("name"),
                            description = obj.getString("description"),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
            }
            if (listToInsert.isNotEmpty()) database.collectionDao().insertCollections(listToInsert)
        }

        // Merge Prompts (Replace if ID is same but incoming is newer, or skip)
        val promptsArray = root.optJSONArray("prompts")
        if (promptsArray != null) {
            val existingPrompts = database.promptDao().getRawAllPromptsList().associateBy { it.id }
            val listToInsert = mutableListOf<Prompt>()
            for (i in 0 until promptsArray.length()) {
                val obj = promptsArray.getJSONObject(i)
                val id = obj.getString("id")
                val incomingUpdatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                val existing = existingPrompts[id]

                if (existing == null || incomingUpdatedAt > existing.updatedAt) {
                    listToInsert.add(
                        Prompt(
                            id = id,
                            title = obj.getString("title"),
                            content = obj.getString("content"),
                            description = obj.optString("description", ""),
                            categoryId = if (obj.isNull("categoryId")) null else obj.getString("categoryId"),
                            collectionId = if (obj.isNull("collectionId")) null else obj.getString("collectionId"),
                            isFavorite = obj.optBoolean("isFavorite", false),
                            isPinned = obj.optBoolean("isPinned", false),
                            usageCount = obj.optInt("usageCount", 0),
                            lastUsedAt = if (obj.isNull("lastUsedAt")) null else obj.getLong("lastUsedAt"),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                            updatedAt = incomingUpdatedAt,
                            placeholdersCsv = obj.optString("placeholdersCsv", ""),
                            isArchived = obj.optBoolean("isArchived", false),
                            isDeleted = obj.optBoolean("isDeleted", false),
                            tagsCsv = obj.optString("tagsCsv", ""),
                            collectionIdsCsv = obj.optString("collectionIdsCsv", ""),
                            coverImageUri = if (obj.isNull("coverImageUri")) null else obj.getString("coverImageUri"),
                            promptType = obj.optString("promptType", "Text"),
                            quality = obj.optString("quality", "Draft"),
                            difficulty = obj.optString("difficulty", "Beginner"),
                            promptLengthType = obj.optString("promptLengthType", "Short"),
                            estimatedTokens = obj.optInt("estimatedTokens", 0),
                            notes = obj.optString("notes", ""),
                            rating = obj.optInt("rating", 0),
                            lastSuccess = obj.optString("lastSuccess", "None"),
                            source = obj.optString("source", "Created by Me"),
                            isTemplate = obj.optBoolean("isTemplate", false),
                            templateCategory = obj.optString("templateCategory", ""),
                            variablesCsv = obj.optString("variablesCsv", "")
                        )
                    )
                }
            }
            if (listToInsert.isNotEmpty()) database.promptDao().insertPrompts(listToInsert)
        }

        // Merge Versions
        val versionsArray = root.optJSONArray("versions")
        if (versionsArray != null) {
            val existingIds = database.promptVersionDao().getRawAllVersionsList().map { it.id }.toSet()
            val listToInsert = mutableListOf<PromptVersion>()
            for (i in 0 until versionsArray.length()) {
                val obj = versionsArray.getJSONObject(i)
                val id = obj.getString("id")
                if (!existingIds.contains(id)) {
                    listToInsert.add(
                        PromptVersion(
                            id = id,
                            promptId = obj.getString("promptId"),
                            title = obj.getString("title"),
                            content = obj.getString("content"),
                            description = obj.optString("description", ""),
                            updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                        )
                    )
                }
            }
            if (listToInsert.isNotEmpty()) database.promptVersionDao().insertVersions(listToInsert)
        }
    }

    // 5. IMPORT FILES (Markdown, TXT, CSV)
    fun parseMarkdownPrompts(markdownText: String): List<ImportPromptCandidate> {
        val list = mutableListOf<ImportPromptCandidate>()
        if (markdownText.isBlank()) return list

        // Split by markdown horizontal rule (---) for multi-prompts
        val sections = markdownText.split(Regex("\\n---\\s*\\n"))
        for (section in sections) {
            if (section.trim().isEmpty()) continue

            var title = "Imported Markdown"
            var description = ""
            var content = ""

            val lines = section.trim().split("\n")
            val contentLines = mutableListOf<String>()

            for (line in lines) {
                val trimmed = line.trim()
                if (trimmed.startsWith("# ")) {
                    title = trimmed.removePrefix("# ").trim()
                } else if (trimmed.startsWith("> ")) {
                    description = trimmed.removePrefix("> ").trim()
                } else if (!trimmed.startsWith("---")) {
                    contentLines.add(line)
                }
            }
            content = contentLines.joinToString("\n").trim()
            if (content.isNotEmpty()) {
                list.add(ImportPromptCandidate(title = title, content = content, description = description))
            }
        }

        // Fallback to single prompt if no sections were identified
        if (list.isEmpty() && markdownText.trim().isNotEmpty()) {
            list.add(ImportPromptCandidate(title = "Imported Text", content = markdownText.trim()))
        }
        return list
    }

    fun parseCsvPrompts(csvText: String): List<ImportPromptCandidate> {
        val list = mutableListOf<ImportPromptCandidate>()
        val lines = csvText.split("\n")
        if (lines.size <= 1) return list

        // Read headers
        val headers = parseCsvLine(lines[0])
        val titleIdx = headers.indexOfFirst { it.equals("title", true) || it.equals("name", true) }.let { if (it == -1) 0 else it }
        val contentIdx = headers.indexOfFirst { it.equals("content", true) || it.equals("prompt", true) || it.equals("body", true) }.let { if (it == -1) 1 else it }
        val descIdx = headers.indexOfFirst { it.equals("description", true) || it.equals("desc", true) }
        val tagsIdx = headers.indexOfFirst { it.equals("tags", true) || it.equals("tag", true) }

        for (i in 1 until lines.size) {
            val line = lines[i]
            if (line.trim().isEmpty()) continue
            val values = parseCsvLine(line)
            if (values.size <= maxOf(titleIdx, contentIdx)) continue

            val title = values.getOrNull(titleIdx)?.trim() ?: "Imported Row $i"
            val content = values.getOrNull(contentIdx)?.trim() ?: ""
            val description = if (descIdx != -1) values.getOrNull(descIdx)?.trim() ?: "" else ""
            val tags = if (tagsIdx != -1) values.getOrNull(tagsIdx)?.trim() ?: "" else ""

            if (content.isNotEmpty()) {
                list.add(ImportPromptCandidate(title = title, content = content, description = description, tagsCsv = tags))
            }
        }
        return list
    }

    private fun parseCsvLine(line: String): List<String> {
        val list = mutableListOf<String>()
        var inQuotes = false
        val sb = StringBuilder()
        var i = 0
        while (i < line.length) {
            val c = line[i]
            if (c == '\"') {
                if (inQuotes && i + 1 < line.length && line[i + 1] == '\"') {
                    // Escaped quote
                    sb.append('\"')
                    i++
                } else {
                    inQuotes = !inQuotes
                }
            } else if (c == ',' && !inQuotes) {
                list.add(sb.toString())
                sb.setLength(0)
            } else {
                sb.append(c)
            }
            i++
        }
        list.add(sb.toString())
        return list
    }

    // Similarity Heuristics
    fun analyzeDuplicates(
        incoming: List<ImportPromptCandidate>,
        existing: List<Prompt>
    ): List<ImportPromptCandidate> {
        return incoming.map { inc ->
            var type = DuplicateType.NONE
            var dupId: String? = null

            for (ext in existing) {
                val titleMatch = ext.title.trim().equals(inc.title.trim(), ignoreCase = true)
                val bodyMatch = ext.content.trim() == inc.content.trim()

                if (titleMatch && bodyMatch) {
                    type = DuplicateType.EXACT
                    dupId = ext.id
                    break
                } else if (bodyMatch) {
                    type = DuplicateType.BODY
                    dupId = ext.id
                    break
                } else if (titleMatch) {
                    type = DuplicateType.TITLE
                    dupId = ext.id
                } else {
                    // Near duplicate check
                    val sim = calculateSimilarity(ext.content, inc.content)
                    if (sim >= 0.8f) {
                        type = DuplicateType.NEAR
                        dupId = ext.id
                        break
                    }
                }
            }
            inc.copy(duplicateType = type, duplicateId = dupId)
        }
    }

    private fun calculateSimilarity(s1: String, s2: String): Float {
        if (s1 == s2) return 1.0f
        val w1 = s1.lowercase().split(Regex("\\W+")).filter { it.length > 2 }.toSet()
        val w2 = s2.lowercase().split(Regex("\\W+")).filter { it.length > 2 }.toSet()
        if (w1.isEmpty() || w2.isEmpty()) return 0.0f
        val intersection = w1.intersect(w2).size
        val union = w1.union(w2).size
        return intersection.toFloat() / union.toFloat()
    }

    // 6. STORAGE ACCESS DETAILS
    suspend fun calculateStorageUsage(context: Context, database: ProtesDatabase): StorageUsage {
        val dbSize = try {
            context.getDatabasePath("protes_database").length()
        } catch (e: Exception) {
            0L
        }

        val backupsSize = try {
            val backupsDir = context.filesDir.resolve("backups")
            if (backupsDir.exists()) {
                backupsDir.listFiles()?.sumOf { it.length() } ?: 0L
            } else {
                0L
            }
        } catch (e: Exception) {
            0L
        }

        val promptsList = database.promptDao().getRawAllPromptsList()
        val templatesCount = promptsList.count { it.isTemplate }
        val collectionsList = database.collectionDao().getRawAllCollectionsList()
        val categoriesList = database.categoryDao().getRawAllCategoriesList()

        val prefs = database.userPreferencesDao().getRawUserPreferences()

        return StorageUsage(
            databaseSize = dbSize,
            backupsSize = backupsSize,
            promptsCount = promptsList.size,
            collectionsCount = collectionsList.size,
            categoriesCount = categoriesList.size,
            templatesCount = templatesCount,
            lastBackupTime = prefs?.lastBackupAt
        )
    }

    // Backup History Helper
    fun getBackupHistory(context: Context): List<BackupFile> {
        val list = mutableListOf<BackupFile>()
        try {
            val dir = context.filesDir.resolve("backups")
            if (dir.exists()) {
                dir.listFiles()?.forEach { file ->
                    if (file.isFile && (file.name.endsWith(".json") || file.name.endsWith(".enc"))) {
                        list.add(
                            BackupFile(
                                name = file.name,
                                file = file,
                                size = file.length(),
                                lastModified = file.lastModified(),
                                isEncrypted = file.name.endsWith(".enc")
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list.sortedByDescending { it.lastModified }
    }

    // Auto backup runner
    suspend fun runAutoBackup(context: Context, database: ProtesDatabase, maxBackupsLimit: Int) {
        try {
            val backupJson = generateBackupJson(context, database)
            val dir = context.filesDir.resolve("backups")
            if (!dir.exists()) dir.mkdirs()

            val sdf = SimpleDateFormat("yyyy_MM_dd_HH_mm", Locale.US)
            val dateStr = sdf.format(Date())
            val filename = "protes_backup_$dateStr.json"
            val file = dir.resolve(filename)

            FileOutputStream(file).use { fos ->
                fos.write(backupJson.toByteArray(Charsets.UTF_8))
            }

            // Prune excess backups
            val backups = getBackupHistory(context)
            if (maxBackupsLimit > 0 && backups.size > maxBackupsLimit) {
                // Delete oldest
                for (i in maxBackupsLimit until backups.size) {
                    backups[i].file.delete()
                }
            }

            // Save last backup timestamp
            val current = database.userPreferencesDao().getRawUserPreferences() ?: UserPreferences()
            database.userPreferencesDao().insertUserPreferences(
                current.copy(lastBackupAt = System.currentTimeMillis())
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // 7. EXPORTS TO FORMATS
    fun exportToMarkdown(prompts: List<Prompt>): String {
        val sb = StringBuilder()
        for ((idx, p) in prompts.withIndex()) {
            sb.append("# ").append(p.title).append("\n")
            if (p.description.isNotEmpty()) {
                sb.append("> ").append(p.description).append("\n")
            }
            if (p.tagsCsv.isNotEmpty()) {
                sb.append("**Tags**: ").append(p.tagsCsv).append("\n")
            }
            sb.append("\n").append(p.content).append("\n")
            if (idx < prompts.size - 1) {
                sb.append("\n---\n\n")
            }
        }
        return sb.toString()
    }

    fun exportToTxt(prompts: List<Prompt>): String {
        val sb = StringBuilder()
        for ((idx, p) in prompts.withIndex()) {
            sb.append("TITLE: ").append(p.title).append("\n")
            if (p.description.isNotEmpty()) {
                sb.append("DESCRIPTION: ").append(p.description).append("\n")
            }
            sb.append("CONTENT:\n").append(p.content).append("\n")
            if (idx < prompts.size - 1) {
                sb.append("\n========================================\n\n")
            }
        }
        return sb.toString()
    }

    fun exportToCsv(prompts: List<Prompt>): String {
        val sb = StringBuilder()
        sb.append("Title,Description,Content,Tags\n")
        for (p in prompts) {
            sb.append(escapeCsv(p.title)).append(",")
            sb.append(escapeCsv(p.description)).append(",")
            sb.append(escapeCsv(p.content)).append(",")
            sb.append(escapeCsv(p.tagsCsv)).append("\n")
        }
        return sb.toString()
    }

    private fun escapeCsv(value: String): String {
        if (!value.contains(",") && !value.contains("\n") && !value.contains("\"")) {
            return value
        }
        return "\"" + value.replace("\"", "\"\"") + "\""
    }

    // 8. PROMPT PACK EXPORT
    fun generatePromptPackJson(
        title: String,
        description: String,
        author: String,
        version: String,
        prompts: List<Prompt>
    ): String {
        val root = JSONObject()
        root.put("isPromptPack", true)
        root.put("title", title)
        root.put("description", description)
        root.put("author", author)
        root.put("version", version)
        root.put("createdDate", System.currentTimeMillis())
        root.put("promptCount", prompts.size)

        val promptsArray = JSONArray()
        for (p in prompts) {
            val obj = JSONObject()
            obj.put("title", p.title)
            obj.put("content", p.content)
            obj.put("description", p.description)
            obj.put("tagsCsv", p.tagsCsv)
            obj.put("promptType", p.promptType)
            obj.put("quality", p.quality)
            obj.put("difficulty", p.difficulty)
            obj.put("notes", p.notes)
            promptsArray.put(obj)
        }
        root.put("prompts", promptsArray)

        return root.toString(2)
    }

    fun parsePromptPack(jsonString: String): List<ImportPromptCandidate>? {
        return try {
            val root = JSONObject(jsonString)
            if (!root.optBoolean("isPromptPack", false)) return null
            val promptsArray = root.getJSONArray("prompts")
            val list = mutableListOf<ImportPromptCandidate>()
            for (i in 0 until promptsArray.length()) {
                val obj = promptsArray.getJSONObject(i)
                list.add(
                    ImportPromptCandidate(
                        title = obj.getString("title"),
                        content = obj.getString("content"),
                        description = obj.optString("description", ""),
                        tagsCsv = obj.optString("tagsCsv", ""),
                        promptType = obj.optString("promptType", "Text")
                    )
                )
            }
            list
        } catch (e: Exception) {
            null
        }
    }
}
