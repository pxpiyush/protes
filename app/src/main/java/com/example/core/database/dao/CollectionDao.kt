package com.example.core.database.dao

import androidx.room.*
import com.example.core.model.PromptCollection
import kotlinx.coroutines.flow.Flow

@Dao
interface CollectionDao {
    @Query("SELECT * FROM prompt_collections ORDER BY name ASC")
    fun getAllCollections(): Flow<List<PromptCollection>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCollection(collection: PromptCollection)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCollections(collections: List<PromptCollection>)

    @Query("SELECT * FROM prompt_collections")
    suspend fun getRawAllCollectionsList(): List<PromptCollection>

    @Delete
    suspend fun deleteCollection(collection: PromptCollection)

    @Query("DELETE FROM prompt_collections WHERE id = :id")
    suspend fun deleteCollectionById(id: String)
}
