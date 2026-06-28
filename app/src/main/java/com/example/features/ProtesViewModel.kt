package com.example.features

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.ProtesApplication
import com.example.core.model.*
import com.example.core.repository.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

enum class PromptSort {
    NEWEST,
    OLDEST,
    RECENTLY_USED,
    MOST_COPIED,
    ALPHABETICAL,
    ALPHABETICAL_DESC,
    UPDATED_RECENTLY,
    PINNED_FIRST,
    FAVORITES_FIRST,
    PROMPT_LENGTH
}

class ProtesViewModel(
    application: Application,
    private val promptRepository: PromptRepository,
    private val categoryRepository: CategoryRepository,
    private val collectionRepository: CollectionRepository,
    private val promptHistoryRepository: PromptHistoryRepository,
    private val promptVersionRepository: PromptVersionRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : AndroidViewModel(application) {

    // UI state flows
    val prompts: StateFlow<List<Prompt>> = promptRepository.allPrompts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<Category>> = categoryRepository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val collections: StateFlow<List<PromptCollection>> = collectionRepository.allCollections
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val history: StateFlow<List<PromptHistory>> = promptHistoryRepository.allHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val archivedPrompts: StateFlow<List<Prompt>> = promptRepository.archivedPrompts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val trashPrompts: StateFlow<List<Prompt>> = promptRepository.trashPrompts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userPreferences: StateFlow<UserPreferences> = userPreferencesRepository.userPreferences
        .map { it ?: UserPreferences() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserPreferences())

    // Filtering & Searching States
    private val _selectedCategoryId = MutableStateFlow<String?>(null)
    val selectedCategoryId: StateFlow<String?> = _selectedCategoryId.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _currentSort = MutableStateFlow(PromptSort.NEWEST)
    val currentSort: StateFlow<PromptSort> = _currentSort.asStateFlow()

    // Advanced search multi-select filters
    private val _selectedCategoryIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedCategoryIds: StateFlow<Set<String>> = _selectedCategoryIds.asStateFlow()

    private val _selectedCollectionIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedCollectionIds: StateFlow<Set<String>> = _selectedCollectionIds.asStateFlow()

    private val _selectedPromptTypes = MutableStateFlow<Set<String>>(emptySet())
    val selectedPromptTypes: StateFlow<Set<String>> = _selectedPromptTypes.asStateFlow()

    private val _selectedTags = MutableStateFlow<Set<String>>(emptySet())
    val selectedTags: StateFlow<Set<String>> = _selectedTags.asStateFlow()

    private val _isFavoriteFilter = MutableStateFlow<Boolean?>(null)
    val isFavoriteFilter: StateFlow<Boolean?> = _isFavoriteFilter.asStateFlow()

    private val _isPinnedFilter = MutableStateFlow<Boolean?>(null)
    val isPinnedFilter: StateFlow<Boolean?> = _isPinnedFilter.asStateFlow()

    private val _hasCoverFilter = MutableStateFlow<Boolean?>(null)
    val hasCoverFilter: StateFlow<Boolean?> = _hasCoverFilter.asStateFlow()

    // Scroll state preservation
    var searchScrollIndex = 0
    var searchScrollOffset = 0

    // Unified Filters Flow
    val searchFilters: StateFlow<SearchFilters> = combine(
        _selectedCategoryIds,
        _selectedCollectionIds,
        _selectedPromptTypes,
        _selectedTags,
        _isFavoriteFilter,
        _isPinnedFilter,
        _hasCoverFilter
    ) { array ->
        SearchFilters(
            categoryIds = array[0] as Set<String>,
            collectionIds = array[1] as Set<String>,
            promptTypes = array[2] as Set<String>,
            tags = array[3] as Set<String>,
            isFavoriteOnly = (array[4] as? Boolean) ?: false,
            isPinnedOnly = (array[5] as? Boolean) ?: false,
            hasCoverOnly = (array[6] as? Boolean) ?: false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SearchFilters())

    // Local Search History Reactive Streams
    val searchHistory: StateFlow<List<String>> = userPreferences
        .map { prefs ->
            if (prefs.searchHistoryRaw.isBlank()) emptyList()
            else prefs.searchHistoryRaw.split("|").filter { it.isNotBlank() }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addSearchHistory(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val currentList = searchHistory.value.toMutableList()
            currentList.remove(trimmed)
            currentList.add(0, trimmed)
            val updatedList = currentList.take(20)
            val raw = updatedList.joinToString("|")
            val currentPrefs = userPreferences.value
            userPreferencesRepository.saveUserPreferences(currentPrefs.copy(searchHistoryRaw = raw))
        }
    }

    fun removeSearchHistory(query: String) {
        viewModelScope.launch {
            val currentList = searchHistory.value.toMutableList()
            currentList.remove(query)
            val raw = currentList.joinToString("|")
            val currentPrefs = userPreferences.value
            userPreferencesRepository.saveUserPreferences(currentPrefs.copy(searchHistoryRaw = raw))
        }
    }

    fun clearSearchHistory() {
        viewModelScope.launch {
            val currentPrefs = userPreferences.value
            userPreferencesRepository.saveUserPreferences(currentPrefs.copy(searchHistoryRaw = ""))
        }
    }

    // Active Editing State
    private val _editingPromptId = MutableStateFlow<String?>(null)
    val editingPromptId: StateFlow<String?> = _editingPromptId.asStateFlow()

    fun startEditing(id: String) {
        _editingPromptId.value = id
    }

    fun stopEditing() {
        _editingPromptId.value = null
    }

    // Dynamic Tag System Stream
    val allTags: StateFlow<List<String>> = prompts.map { allPrompts ->
        allPrompts.flatMap { it.getTags() }.distinct().sorted()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Prompts List for Home Screen (only sorted, NOT filtered by search/advanced search)
    val homePrompts: StateFlow<List<Prompt>> = combine(
        prompts,
        _currentSort
    ) { allPrompts, sort ->
        when (sort) {
            PromptSort.NEWEST -> allPrompts.sortedByDescending { it.createdAt }
            PromptSort.OLDEST -> allPrompts.sortedBy { it.createdAt }
            PromptSort.RECENTLY_USED -> allPrompts.sortedByDescending { it.updatedAt }
            PromptSort.MOST_COPIED -> allPrompts.sortedByDescending { it.usageCount }
            PromptSort.ALPHABETICAL -> allPrompts.sortedBy { it.title.lowercase() }
            PromptSort.ALPHABETICAL_DESC -> allPrompts.sortedByDescending { it.title.lowercase() }
            PromptSort.UPDATED_RECENTLY -> allPrompts.sortedByDescending { it.updatedAt }
            PromptSort.PINNED_FIRST -> allPrompts.sortedWith(compareByDescending<Prompt> { it.isPinned }.thenByDescending { it.createdAt })
            PromptSort.FAVORITES_FIRST -> allPrompts.sortedWith(compareByDescending<Prompt> { it.isFavorite }.thenByDescending { it.createdAt })
            PromptSort.PROMPT_LENGTH -> allPrompts.sortedByDescending { it.content.length }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered Prompts List combining multi-select advanced search and Spotlight criteria
    val filteredPrompts: StateFlow<List<Prompt>> = combine(
        prompts,
        searchFilters,
        _searchQuery,
        _currentSort
    ) { allPrompts, filters, query, sort ->
        val filtered = allPrompts.filter { prompt ->
            val matchesCategory = filters.categoryIds.isEmpty() || filters.categoryIds.contains(prompt.categoryId)
            
            val matchesCollection = filters.collectionIds.isEmpty() || 
                    filters.collectionIds.contains(prompt.collectionId) ||
                    prompt.getCollectionIds().any { filters.collectionIds.contains(it) }

            val matchesPromptType = filters.promptTypes.isEmpty() || filters.promptTypes.contains(prompt.promptType)

            val matchesTags = filters.tags.isEmpty() || prompt.getTags().any { filters.tags.contains(it) }

            val matchesFavorite = !filters.isFavoriteOnly || prompt.isFavorite
            val matchesPinned = !filters.isPinnedOnly || prompt.isPinned
            val matchesCover = !filters.hasCoverOnly || !prompt.coverImageUri.isNullOrEmpty()

            val matchesSearch = query.isEmpty() || run {
                val catName = categories.value.find { it.id == prompt.categoryId }?.name ?: ""
                val collectionNames = collections.value.filter { it.id == prompt.collectionId || prompt.getCollectionIds().contains(it.id) }.map { it.name }

                prompt.title.contains(query, ignoreCase = true) ||
                prompt.content.contains(query, ignoreCase = true) ||
                prompt.description.contains(query, ignoreCase = true) ||
                prompt.promptType.contains(query, ignoreCase = true) ||
                catName.contains(query, ignoreCase = true) ||
                prompt.tagsCsv.contains(query, ignoreCase = true) ||
                collectionNames.any { it.contains(query, ignoreCase = true) }
            }

            matchesCategory && matchesCollection && matchesPromptType && matchesTags && matchesFavorite && matchesPinned && matchesCover && matchesSearch
        }

        when (sort) {
            PromptSort.NEWEST -> filtered.sortedByDescending { it.createdAt }
            PromptSort.OLDEST -> filtered.sortedBy { it.createdAt }
            PromptSort.RECENTLY_USED -> filtered.sortedWith(compareByDescending<Prompt> { it.lastUsedAt ?: 0L }.thenByDescending { it.updatedAt })
            PromptSort.MOST_COPIED -> filtered.sortedByDescending { it.usageCount }
            PromptSort.ALPHABETICAL -> filtered.sortedBy { it.title.lowercase() }
            PromptSort.ALPHABETICAL_DESC -> filtered.sortedByDescending { it.title.lowercase() }
            PromptSort.UPDATED_RECENTLY -> filtered.sortedByDescending { it.updatedAt }
            PromptSort.PINNED_FIRST -> filtered.sortedWith(compareByDescending<Prompt> { it.isPinned }.thenByDescending { it.updatedAt })
            PromptSort.FAVORITES_FIRST -> filtered.sortedWith(compareByDescending<Prompt> { it.isFavorite }.thenByDescending { it.updatedAt })
            PromptSort.PROMPT_LENGTH -> filtered.sortedByDescending { it.content.length }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Database Mutators
    fun toggleFavorite(prompt: Prompt) {
        viewModelScope.launch {
            promptRepository.updatePrompt(prompt.copy(isFavorite = !prompt.isFavorite, updatedAt = System.currentTimeMillis()))
        }
    }

    fun togglePin(prompt: Prompt) {
        viewModelScope.launch {
            promptRepository.updatePrompt(prompt.copy(isPinned = !prompt.isPinned, updatedAt = System.currentTimeMillis()))
        }
    }

    fun setSort(sort: PromptSort) {
        _currentSort.value = sort
    }

    fun selectCategory(categoryId: String?) {
        _selectedCategoryId.value = categoryId
        _selectedCategoryIds.value = if (categoryId != null) setOf(categoryId) else emptySet()
    }

    fun toggleCategoryFilter(categoryId: String) {
        val current = _selectedCategoryIds.value
        val updated = if (current.contains(categoryId)) {
            current - categoryId
        } else {
            current + categoryId
        }
        _selectedCategoryIds.value = updated
        _selectedCategoryId.value = updated.firstOrNull()
    }

    fun toggleCollectionFilter(collectionId: String) {
        val current = _selectedCollectionIds.value
        _selectedCollectionIds.value = if (current.contains(collectionId)) {
            current - collectionId
        } else {
            current + collectionId
        }
    }

    fun togglePromptTypeFilter(promptType: String) {
        val current = _selectedPromptTypes.value
        _selectedPromptTypes.value = if (current.contains(promptType)) {
            current - promptType
        } else {
            current + promptType
        }
    }

    fun toggleTagFilter(tag: String) {
        val current = _selectedTags.value
        _selectedTags.value = if (current.contains(tag)) {
            current - tag
        } else {
            current + tag
        }
    }

    fun toggleFavoriteFilter() {
        _isFavoriteFilter.value = if (_isFavoriteFilter.value == true) null else true
    }

    fun togglePinnedFilter() {
        _isPinnedFilter.value = if (_isPinnedFilter.value == true) null else true
    }

    fun toggleHasCoverFilter() {
        _hasCoverFilter.value = if (_hasCoverFilter.value == true) null else true
    }

    fun clearAllFilters() {
        _selectedCategoryIds.value = emptySet()
        _selectedCollectionIds.value = emptySet()
        _selectedPromptTypes.value = emptySet()
        _selectedTags.value = emptySet()
        _isFavoriteFilter.value = null
        _isPinnedFilter.value = null
        _hasCoverFilter.value = null
        _searchQuery.value = ""
        _selectedCategoryId.value = null
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // CRUD - Save Prompt (Create or Update)
    fun savePrompt(
        id: String? = null,
        title: String,
        content: String,
        description: String = "",
        categoryId: String? = null,
        isFavorite: Boolean = false,
        isPinned: Boolean = false,
        tagsCsv: String = "",
        collectionIdsCsv: String = "",
        coverImageUri: String? = null,
        promptType: String = "Text",
        quality: String = "Draft",
        difficulty: String = "Beginner",
        notes: String = "",
        rating: Int = 0,
        lastSuccess: String = "None",
        source: String = "Created by Me",
        isTemplate: Boolean = false,
        templateCategory: String = ""
    ) {
        viewModelScope.launch {
            val placeholderList = mutableListOf<String>()
            val regex = "\\[\\[(.*?)\\]\\]".toRegex()
            regex.findAll(content).forEach { matchResult ->
                placeholderList.add(matchResult.groups[1]?.value ?: "")
            }
            val csv = placeholderList.distinct().filter { it.isNotEmpty() }.joinToString(",")

            // Phase 6 variables scanning {{name}}
            val varRegex = "\\{\\{(.*?)\\}\\}".toRegex()
            val variablesList = varRegex.findAll(content).map { it.groups[1]?.value?.trim() ?: "" }.filter { it.isNotEmpty() }.distinct().toList()
            val variablesCsv = variablesList.joinToString(",")

            // Live metrics
            val wordCount = content.split("\\s+".toRegex()).filter { it.isNotEmpty() }.size
            val lengthType = when {
                wordCount < 50 -> "Short"
                wordCount < 150 -> "Medium"
                wordCount < 400 -> "Long"
                else -> "Very Long"
            }
            val charEst = (content.length / 4.0)
            val wordEst = (wordCount / 0.75)
            val estimatedTokens = maxOf(1, ((charEst + wordEst) / 2.0).toInt())

            val finalId = id ?: UUID.randomUUID().toString()
            val existing = prompts.value.find { it.id == finalId } ?: archivedPrompts.value.find { it.id == finalId } ?: trashPrompts.value.find { it.id == finalId }

            if (existing != null && (existing.title != title || existing.content != content || existing.description != description)) {
                // Save previous version to history before we write the new update
                val versionId = UUID.randomUUID().toString()
                promptVersionRepository.saveVersion(
                    PromptVersion(
                        id = versionId,
                        promptId = existing.id,
                        title = existing.title,
                        content = existing.content,
                        description = existing.description,
                        updatedAt = existing.updatedAt
                    )
                )
            }

            val prompt = Prompt(
                id = finalId,
                title = title,
                content = content,
                description = description,
                categoryId = categoryId,
                isFavorite = isFavorite,
                isPinned = isPinned,
                placeholdersCsv = csv,
                createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                isArchived = existing?.isArchived ?: false,
                isDeleted = existing?.isDeleted ?: false,
                tagsCsv = tagsCsv,
                collectionIdsCsv = collectionIdsCsv,
                coverImageUri = coverImageUri,
                promptType = promptType,
                usageCount = existing?.usageCount ?: 0,
                lastUsedAt = existing?.lastUsedAt,
                // Phase 6 extensions
                quality = quality,
                difficulty = difficulty,
                promptLengthType = lengthType,
                estimatedTokens = estimatedTokens,
                notes = notes,
                rating = rating,
                lastSuccess = lastSuccess,
                source = source,
                isTemplate = isTemplate,
                templateCategory = templateCategory,
                variablesCsv = variablesCsv
            )
            promptRepository.savePrompt(prompt)
        }
    }

    // CRUD - Duplicate Prompt
    fun duplicatePrompt(prompt: Prompt) {
        viewModelScope.launch {
            val duplicated = prompt.copy(
                id = UUID.randomUUID().toString(),
                title = "${prompt.title} (Copy)",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                usageCount = 0,
                lastUsedAt = null,
                isPinned = false
            )
            promptRepository.savePrompt(duplicated)
        }
    }

    // CRUD - Soft Delete Prompt
    fun softDeletePrompt(prompt: Prompt) {
        viewModelScope.launch {
            promptRepository.updatePrompt(prompt.copy(isDeleted = true, updatedAt = System.currentTimeMillis()))
        }
    }

    // CRUD - Restore Prompt
    fun restorePrompt(prompt: Prompt) {
        viewModelScope.launch {
            promptRepository.updatePrompt(prompt.copy(isDeleted = false, isArchived = false, updatedAt = System.currentTimeMillis()))
        }
    }

    // CRUD - Permanent Delete
    fun permanentDeletePrompt(id: String) {
        viewModelScope.launch {
            promptRepository.deletePromptById(id)
        }
    }

    // Compatibility support: deletes/soft-deletes based on prompt status
    fun deletePrompt(id: String) {
        viewModelScope.launch {
            val p = prompts.value.find { it.id == id } ?: archivedPrompts.value.find { it.id == id }
            if (p != null) {
                softDeletePrompt(p)
            } else {
                permanentDeletePrompt(id)
            }
        }
    }

    // CRUD - Archive Prompt
    fun archivePrompt(prompt: Prompt) {
        viewModelScope.launch {
            promptRepository.updatePrompt(prompt.copy(isArchived = true, updatedAt = System.currentTimeMillis()))
        }
    }

    // CRUD - Unarchive Prompt
    fun unarchivePrompt(prompt: Prompt) {
        viewModelScope.launch {
            promptRepository.updatePrompt(prompt.copy(isArchived = false, updatedAt = System.currentTimeMillis()))
        }
    }

    // Record copy action to increment count and timestamp
    fun recordPromptCopy(prompt: Prompt) {
        viewModelScope.launch {
            promptRepository.updatePrompt(
                prompt.copy(
                    usageCount = prompt.usageCount + 1,
                    lastUsedAt = System.currentTimeMillis()
                )
            )
        }
    }

    // Category System Mutators
    fun createCategory(name: String, iconName: String = "Folder", colorHex: String = "#262626") {
        viewModelScope.launch {
            val category = Category(
                id = "cat_" + UUID.randomUUID().toString().take(6),
                name = name,
                iconName = iconName,
                colorHex = colorHex
            )
            categoryRepository.saveCategory(category)
        }
    }

    fun renameCategory(categoryId: String, newName: String) {
        viewModelScope.launch {
            val existing = categories.value.find { it.id == categoryId }
            if (existing != null) {
                categoryRepository.saveCategory(existing.copy(name = newName))
            }
        }
    }

    fun deleteCategory(categoryId: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val defaultCategoryIds = listOf("cat_dev", "cat_creative", "cat_marketing", "cat_productivity")
            if (categoryId in defaultCategoryIds) {
                onError("Default categories cannot be deleted.")
                return@launch
            }
            val hasPrompts = prompts.value.any { it.categoryId == categoryId } || archivedPrompts.value.any { it.categoryId == categoryId }
            if (hasPrompts) {
                onError("Cannot delete category. It contains active prompts.")
                return@launch
            }
            categoryRepository.deleteCategoryById(categoryId)
            onSuccess()
        }
    }

    // Collections System Mutators
    fun createCollection(name: String, description: String = "") {
        viewModelScope.launch {
            val collection = PromptCollection(
                id = "col_" + UUID.randomUUID().toString().take(6),
                name = name,
                description = description
            )
            collectionRepository.saveCollection(collection)
        }
    }

    fun renameCollection(collectionId: String, newName: String) {
        viewModelScope.launch {
            val existing = collections.value.find { it.id == collectionId }
            if (existing != null) {
                collectionRepository.saveCollection(existing.copy(name = newName))
            }
        }
    }

    fun deleteCollection(collectionId: String) {
        viewModelScope.launch {
            collectionRepository.deleteCollectionById(collectionId)
            prompts.value.forEach { prompt ->
                if (prompt.collectionId == collectionId) {
                    promptRepository.updatePrompt(prompt.copy(collectionId = null))
                }
                if (prompt.collectionIdsCsv.contains(collectionId)) {
                    val updatedIds = prompt.getCollectionIds().filter { it != collectionId }.joinToString(",")
                    promptRepository.updatePrompt(prompt.copy(collectionIdsCsv = updatedIds))
                }
            }
        }
    }

    fun addPrompt(title: String, content: String, description: String, categoryId: String?) {
        savePrompt(
            title = title,
            content = content,
            description = description,
            categoryId = categoryId
        )
    }

    fun updateThemeMode(mode: String) {
        viewModelScope.launch {
            val currentPrefs = userPreferences.value
            userPreferencesRepository.saveUserPreferences(currentPrefs.copy(themeMode = mode))
        }
    }

    fun updateBoardViewMode(mode: String) {
        viewModelScope.launch {
            val currentPrefs = userPreferences.value
            userPreferencesRepository.saveUserPreferences(currentPrefs.copy(boardViewMode = mode))
        }
    }

    fun updateCategorySize(categoryId: String, size: String) {
        viewModelScope.launch {
            val existing = categories.value.find { it.id == categoryId }
            if (existing != null) {
                categoryRepository.saveCategory(existing.copy(boardSize = size))
            }
        }
    }

    fun updateCategoryCover(categoryId: String, coverImageUri: String?) {
        viewModelScope.launch {
            val existing = categories.value.find { it.id == categoryId }
            if (existing != null) {
                categoryRepository.saveCategory(existing.copy(coverImageUri = coverImageUri))
            }
        }
    }

    fun toggleArchiveCategory(categoryId: String) {
        viewModelScope.launch {
            val existing = categories.value.find { it.id == categoryId }
            if (existing != null) {
                categoryRepository.saveCategory(existing.copy(isArchived = !existing.isArchived))
            }
        }
    }

    fun updateCategoryOrder(categoryId: String, direction: Int) {
        viewModelScope.launch {
            val list = categories.value.filter { !it.isArchived }.sortedBy { it.orderIndex }.toMutableList()
            val index = list.indexOfFirst { it.id == categoryId }
            if (index == -1) return@launch
            val targetIndex = index + direction
            if (targetIndex in list.indices) {
                val item = list[index]
                val other = list[targetIndex]
                val updatedItem = item.copy(orderIndex = other.orderIndex)
                val updatedOther = other.copy(orderIndex = item.orderIndex)
                categoryRepository.saveCategory(updatedItem)
                categoryRepository.saveCategory(updatedOther)
            }
        }
    }

    fun swapCategories(fromIndex: Int, toIndex: Int) {
        viewModelScope.launch {
            val list = categories.value.filter { !it.isArchived }.sortedBy { it.orderIndex }.toMutableList()
            if (fromIndex in list.indices && toIndex in list.indices) {
                val item = list.removeAt(fromIndex)
                list.add(toIndex, item)
                list.forEachIndexed { idx, cat ->
                    if (cat.orderIndex != idx) {
                        categoryRepository.saveCategory(cat.copy(orderIndex = idx))
                    }
                }
            }
        }
    }

    fun updateBiometric(enabled: Boolean) {
        viewModelScope.launch {
            val currentPrefs = userPreferences.value
            userPreferencesRepository.saveUserPreferences(currentPrefs.copy(isBiometricEnabled = enabled))
        }
    }

    fun getVersionsForPrompt(promptId: String): Flow<List<PromptVersion>> =
        promptVersionRepository.getVersionsForPrompt(promptId)

    fun restorePromptVersion(promptId: String, version: PromptVersion) {
        viewModelScope.launch {
            val existing = prompts.value.find { it.id == promptId }
                ?: archivedPrompts.value.find { it.id == promptId }
            if (existing != null) {
                // First save current to history so they don't lose it
                val versionId = UUID.randomUUID().toString()
                promptVersionRepository.saveVersion(
                    PromptVersion(
                        id = versionId,
                        promptId = existing.id,
                        title = existing.title,
                        content = existing.content,
                        description = existing.description,
                        updatedAt = System.currentTimeMillis()
                    )
                )

                // Restore
                val restored = existing.copy(
                    title = version.title,
                    content = version.content,
                    description = version.description,
                    updatedAt = System.currentTimeMillis()
                )
                promptRepository.updatePrompt(restored)
            }
        }
    }

    // Export formats helper
    fun exportPrompt(prompt: Prompt, format: String): String {
        return when (format.uppercase()) {
            "JSON" -> {
                """{
  "title": "${prompt.title.replace("\"", "\\\"").replace("\n", "\\n")}",
  "content": "${prompt.content.replace("\"", "\\\"").replace("\n", "\\n")}",
  "description": "${prompt.description.replace("\"", "\\\"").replace("\n", "\\n")}",
  "categoryId": "${prompt.categoryId ?: ""}",
  "collectionIdsCsv": "${prompt.collectionIdsCsv}",
  "tagsCsv": "${prompt.tagsCsv}",
  "quality": "${prompt.quality}",
  "difficulty": "${prompt.difficulty}",
  "notes": "${prompt.notes.replace("\"", "\\\"").replace("\n", "\\n")}",
  "rating": ${prompt.rating},
  "lastSuccess": "${prompt.lastSuccess}",
  "isTemplate": ${prompt.isTemplate},
  "templateCategory": "${prompt.templateCategory}"
}"""
            }
            "MARKDOWN" -> {
                """# Title: ${prompt.title}
**Description**: ${prompt.description}
**Quality**: ${prompt.quality}
**Difficulty**: ${prompt.difficulty}
**Rating**: ${"★".repeat(prompt.rating)}

---

${prompt.content}"""
            }
            else -> {
                """Title: ${prompt.title}
Description: ${prompt.description}

Content:
${prompt.content}"""
            }
        }
    }

    // Import parsers
    fun parsePromptFromJson(json: String): Prompt? {
        try {
            fun getVal(key: String): String {
                val pattern = "\"$key\"\\s*:\\s*\"([^\"]*)\"".toRegex()
                return pattern.find(json)?.groups?.get(1)?.value ?: ""
            }
            fun getBoolVal(key: String): Boolean {
                val pattern = "\"$key\"\\s*:\\s*(true|false)".toRegex()
                return pattern.find(json)?.groups?.get(1)?.value?.toBoolean() ?: false
            }
            fun getIntVal(key: String): Int {
                val pattern = "\"$key\"\\s*:\\s*(\\d+)".toRegex()
                return pattern.find(json)?.groups?.get(1)?.value?.toIntOrNull() ?: 0
            }

            val title = getVal("title")
            val content = getVal("content").replace("\\n", "\n").replace("\\\"", "\"")
            val description = getVal("description")
            if (title.isBlank() || content.isBlank()) return null

            return Prompt(
                id = UUID.randomUUID().toString(),
                title = title,
                content = content,
                description = description,
                categoryId = getVal("categoryId").takeIf { it.isNotEmpty() },
                collectionIdsCsv = getVal("collectionIdsCsv"),
                tagsCsv = getVal("tagsCsv"),
                quality = getVal("quality").ifBlank { "Draft" },
                difficulty = getVal("difficulty").ifBlank { "Beginner" },
                notes = getVal("notes"),
                rating = getIntVal("rating"),
                lastSuccess = getVal("lastSuccess").ifBlank { "None" },
                source = "Imported",
                isTemplate = getBoolVal("isTemplate"),
                templateCategory = getVal("templateCategory")
            )
        } catch (e: Exception) {
            return null
        }
    }

    fun parsePromptFromMarkdown(markdown: String): Prompt? {
        try {
            val lines = markdown.lines()
            var title = ""
            var description = ""
            val contentLines = mutableListOf<String>()
            var contentStarted = false

            for (line in lines) {
                if (line.startsWith("# Title:")) {
                    title = line.removePrefix("# Title:").trim()
                } else if (line.startsWith("**Description**:")) {
                    description = line.removePrefix("**Description**:").trim()
                } else if (line.trim() == "---") {
                    contentStarted = true
                } else if (contentStarted) {
                    contentLines.add(line)
                }
            }

            if (title.isBlank()) {
                val nonEmpty = lines.firstOrNull { it.isNotBlank() } ?: ""
                title = nonEmpty.removePrefix("#").trim()
            }
            val content = if (contentStarted) contentLines.joinToString("\n").trim() else markdown.trim()
            if (title.isBlank() || content.isBlank()) return null

            return Prompt(
                id = UUID.randomUUID().toString(),
                title = title,
                content = content,
                description = description,
                source = "Imported"
            )
        } catch (e: Exception) {
            return null
        }
    }

    fun parsePromptFromPlainText(text: String): Prompt? {
        try {
            val lines = text.lines()
            var title = ""
            var description = ""
            val contentLines = mutableListOf<String>()
            var contentStarted = false

            for (line in lines) {
                if (line.startsWith("Title:")) {
                    title = line.removePrefix("Title:").trim()
                } else if (line.startsWith("Description:")) {
                    description = line.removePrefix("Description:").trim()
                } else if (line.startsWith("Content:")) {
                    contentStarted = true
                } else if (contentStarted) {
                    contentLines.add(line)
                }
            }

            if (title.isBlank()) {
                title = lines.firstOrNull { it.isNotBlank() }?.trim() ?: "Imported Plain Text"
            }
            val content = if (contentStarted) contentLines.joinToString("\n").trim() else text.trim()
            if (content.isBlank()) return null

            return Prompt(
                id = UUID.randomUUID().toString(),
                title = title,
                content = content,
                description = description,
                source = "Imported"
            )
        } catch (e: Exception) {
            return null
        }
    }

    // Smart duplicate check
    fun checkDuplicatePrompt(title: String, content: String): Prompt? {
        return (prompts.value + archivedPrompts.value).find {
            it.title.equals(title, ignoreCase = true) ||
            it.content.trim() == content.trim()
        }
    }

    fun addHistory(promptId: String, filledContent: String) {
        viewModelScope.launch {
            val hist = PromptHistory(
                id = UUID.randomUUID().toString(),
                promptId = promptId,
                filledContent = filledContent,
                usedAt = System.currentTimeMillis()
            )
            promptHistoryRepository.saveHistory(hist)
        }
    }

    // Phase 9: Backup, Restore & Security integrations
    val databaseInstance: com.example.core.database.ProtesDatabase 
        get() = (getApplication<ProtesApplication>()).database

    suspend fun performBackupJson(context: Context): String {
        return com.example.core.backup.BackupManager.generateBackupJson(context, databaseInstance)
    }

    suspend fun performRestoreReplace(json: String) {
        com.example.core.backup.BackupManager.restoreDatabaseReplace(databaseInstance, json)
    }

    suspend fun performRestoreMerge(json: String) {
        com.example.core.backup.BackupManager.restoreDatabaseMerge(databaseInstance, json)
    }

    suspend fun calculateStorageUsage(context: Context): com.example.core.backup.BackupManager.StorageUsage {
        return com.example.core.backup.BackupManager.calculateStorageUsage(context, databaseInstance)
    }

    suspend fun runAutoBackup(context: Context, limit: Int) {
        com.example.core.backup.BackupManager.runAutoBackup(context, databaseInstance, limit)
    }

    fun saveImportedPrompts(candidates: List<com.example.core.backup.BackupManager.ImportPromptCandidate>, resolveStrategy: String) {
        viewModelScope.launch {
            for (cand in candidates) {
                val exists = checkDuplicatePrompt(cand.title, cand.content)
                val finalPrompt = if (exists != null) {
                    when (resolveStrategy) {
                        "skip" -> continue
                        "replace" -> {
                            Prompt(
                                id = exists.id,
                                title = cand.title,
                                content = cand.content,
                                description = cand.description,
                                categoryId = cand.categoryId ?: exists.categoryId,
                                tagsCsv = cand.tagsCsv,
                                promptType = cand.promptType,
                                isTemplate = cand.isTemplate,
                                templateCategory = cand.templateCategory,
                                updatedAt = System.currentTimeMillis()
                            )
                        }
                        "keep_both" -> {
                            Prompt(
                                id = UUID.randomUUID().toString(),
                                title = "${cand.title} (Copy)",
                                content = cand.content,
                                description = cand.description,
                                categoryId = cand.categoryId,
                                tagsCsv = cand.tagsCsv,
                                promptType = cand.promptType,
                                isTemplate = cand.isTemplate,
                                templateCategory = cand.templateCategory
                            )
                        }
                        "merge" -> {
                            Prompt(
                                id = exists.id,
                                title = exists.title,
                                content = exists.content + "\n\nMerged Content:\n" + cand.content,
                                description = exists.description.ifEmpty { cand.description },
                                categoryId = exists.categoryId ?: cand.categoryId,
                                tagsCsv = if (exists.tagsCsv.isEmpty()) cand.tagsCsv else (exists.tagsCsv + "," + cand.tagsCsv),
                                promptType = exists.promptType,
                                isTemplate = exists.isTemplate || cand.isTemplate,
                                templateCategory = exists.templateCategory.ifEmpty { cand.templateCategory },
                                updatedAt = System.currentTimeMillis()
                            )
                        }
                        else -> continue
                    }
                } else {
                    Prompt(
                        id = UUID.randomUUID().toString(),
                        title = cand.title,
                        content = cand.content,
                        description = cand.description,
                        categoryId = cand.categoryId,
                        tagsCsv = cand.tagsCsv,
                        promptType = cand.promptType,
                        isTemplate = cand.isTemplate,
                        templateCategory = cand.templateCategory
                    )
                }
                promptRepository.savePrompt(finalPrompt)
            }
        }
    }

    // ViewModel Factory Creator
    class Factory(private val app: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val protesApp = app as ProtesApplication
            return ProtesViewModel(
                application = app,
                promptRepository = protesApp.promptRepository,
                categoryRepository = protesApp.categoryRepository,
                collectionRepository = protesApp.collectionRepository,
                promptHistoryRepository = protesApp.promptHistoryRepository,
                promptVersionRepository = protesApp.promptVersionRepository,
                userPreferencesRepository = protesApp.userPreferencesRepository
            ) as T
        }
    }
}
