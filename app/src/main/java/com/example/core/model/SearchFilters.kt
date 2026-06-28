package com.example.core.model

data class SearchFilters(
    val categoryIds: Set<String> = emptySet(),
    val collectionIds: Set<String> = emptySet(),
    val aiModelIds: Set<String> = emptySet(),
    val promptTypes: Set<String> = emptySet(),
    val tags: Set<String> = emptySet(),
    val isFavoriteOnly: Boolean = false,
    val isPinnedOnly: Boolean = false,
    val hasCoverOnly: Boolean = false
)
