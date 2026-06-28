package com.example.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.core.database.dao.*
import com.example.core.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

import androidx.room.migration.Migration
import com.example.core.database.dao.PromptVersionDao

@Database(
    entities = [
        Prompt::class,
        Category::class,
        PromptCollection::class,
        PromptHistory::class,
        UserPreferences::class,
        PromptVersion::class
    ],
    version = 7,
    exportSchema = false
)
abstract class ProtesDatabase : RoomDatabase() {
    abstract fun promptDao(): PromptDao
    abstract fun categoryDao(): CategoryDao
    abstract fun collectionDao(): CollectionDao
    abstract fun promptHistoryDao(): PromptHistoryDao
    abstract fun userPreferencesDao(): UserPreferencesDao
    abstract fun promptVersionDao(): PromptVersionDao

    companion object {
        @Volatile
        private var INSTANCE: ProtesDatabase? = null

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE prompts ADD COLUMN quality TEXT NOT NULL DEFAULT 'Draft'")
                db.execSQL("ALTER TABLE prompts ADD COLUMN difficulty TEXT NOT NULL DEFAULT 'Beginner'")
                db.execSQL("ALTER TABLE prompts ADD COLUMN promptLengthType TEXT NOT NULL DEFAULT 'Short'")
                db.execSQL("ALTER TABLE prompts ADD COLUMN estimatedTokens INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE prompts ADD COLUMN notes TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE prompts ADD COLUMN rating INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE prompts ADD COLUMN lastSuccess TEXT NOT NULL DEFAULT 'None'")
                db.execSQL("ALTER TABLE prompts ADD COLUMN source TEXT NOT NULL DEFAULT 'Created by Me'")
                db.execSQL("ALTER TABLE prompts ADD COLUMN isTemplate INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE prompts ADD COLUMN templateCategory TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE prompts ADD COLUMN variablesCsv TEXT NOT NULL DEFAULT ''")
                
                db.execSQL("CREATE TABLE IF NOT EXISTS `prompt_versions` (`id` TEXT NOT NULL, `promptId` TEXT NOT NULL, `title` TEXT NOT NULL, `content` TEXT NOT NULL, `description` TEXT NOT NULL DEFAULT '', `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))")
            }
        }

        fun getDatabase(context: Context, scope: CoroutineScope): ProtesDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ProtesDatabase::class.java,
                    "protes_database"
                )
                    .addMigrations(MIGRATION_3_4)
                    .fallbackToDestructiveMigration(true)
                    .addCallback(ProtesDatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class ProtesDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    seedDatabase(database)
                }
            }
        }

        private suspend fun seedDatabase(database: ProtesDatabase) {
            // Seed Categories
            val categories = listOf(
                Category("cat_dev", "Development", "Code", "#262626", boardSize = "large_square", orderIndex = 0),
                Category("cat_creative", "Creative", "Create", "#262626", boardSize = "wide_rectangle", orderIndex = 1),
                Category("cat_marketing", "Marketing", "TrendingUp", "#262626", boardSize = "tall_rectangle", orderIndex = 2),
                Category("cat_productivity", "Productivity", "CheckCircle", "#262626", boardSize = "small_square", orderIndex = 3)
            )
            for (category in categories) {
                database.categoryDao().insertCategory(category)
            }

            // Seed User Preferences
            database.userPreferencesDao().insertUserPreferences(
                UserPreferences(id = "default_preferences", themeMode = "light")
            )

            // Seed Sample Prompts for elegant empty state / starting view
            val samplePrompts = listOf(
                Prompt(
                    id = "p1",
                    title = "Senior Technical Architect Review",
                    content = "Act as a Senior Technical Architect. Review the following Kotlin code for Jetpack Compose. Check for performance issues, redundant recompositions, memory leaks, and adherence to Material Design 3 guidelines: \n\n[[code]]",
                    description = "Comprehensive review of Jetpack Compose files focusing on state management, side effects, and M3 standards.",
                    categoryId = "cat_dev",
                    isPinned = true,
                    placeholdersCsv = "code"
                ),
                Prompt(
                    id = "p2",
                    title = "Copywriting Slogan Generator",
                    content = "Create 10 high-impact, memorable taglines for a premium brand named [[brand_name]] whose core mission is: [[mission]]. The tone should be [[tone]], utilizing clean and elegant messaging. Avoid cliché marketing vocabulary.",
                    description = "Editorial slogan generator for minimalist and luxury brands.",
                    categoryId = "cat_creative",
                    isFavorite = true,
                    placeholdersCsv = "brand_name,mission,tone"
                )
            )
            for (prompt in samplePrompts) {
                database.promptDao().insertPrompt(prompt)
            }
        }
    }
}
