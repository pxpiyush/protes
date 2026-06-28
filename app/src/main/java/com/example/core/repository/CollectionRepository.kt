package com.example.core.repository

import com.example.core.database.dao.CollectionDao
import com.example.core.model.PromptCollection
import kotlinx.coroutines.flow.Flow

class CollectionRepository(private val collectionDao: CollectionDao) {
    val allCollections: Flow<List<PromptCollection>> = collectionDao.getAllCollections()

    suspend fun saveCollection(collection: PromptCollection) {
        collectionDao.insertCollection(collection)
    }

    suspend fun deleteCollection(collection: PromptCollection) {
        collectionDao.deleteCollection(collection)
    }

    suspend fun deleteCollectionById(id: String) {
        collectionDao.deleteCollectionById(id)
    }
}
