package com.therian.oc.aaa.data.local

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseProvider {
    @Volatile
    private var INSTANCE: AppDatabase? = null

    fun getDatabase(context: Context): AppDatabase {
        return INSTANCE ?: synchronized(this) {
            Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "app_database"
            )
                .addMigrations(MIGRATION_1_3, MIGRATION_2_3)
                .build()
                .also { INSTANCE = it }
        }
    }

    private val MIGRATION_1_3 = object : Migration(1, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("DROP TABLE IF EXISTS `users`")
            createPersistenceTables(db)
            createCharacterTables(db)
        }
    }

    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            createCharacterTables(db)
        }
    }

    private fun createPersistenceTables(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `app_settings` (
                `key` TEXT NOT NULL,
                `value` TEXT NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                PRIMARY KEY(`key`)
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `json_cache` (
                `key` TEXT NOT NULL,
                `json` TEXT NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                PRIMARY KEY(`key`)
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `saved_avatars` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `avatarPath` TEXT NOT NULL,
                `pathInternalEdit` TEXT NOT NULL,
                `suggestionJson` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_saved_avatars_pathInternalEdit` " +
                    "ON `saved_avatars` (`pathInternalEdit`)"
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `media_items` (
                `path` TEXT NOT NULL,
                `album` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                PRIMARY KEY(`path`)
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_media_items_album` ON `media_items` (`album`)"
        )
    }

    private fun createCharacterTables(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `characters` (
                `id` TEXT NOT NULL,
                `source` TEXT NOT NULL,
                `dataName` TEXT NOT NULL,
                `avatar` TEXT NOT NULL,
                `level` INTEGER NOT NULL,
                `isFromApi` INTEGER NOT NULL,
                `sortOrder` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_characters_source` ON `characters` (`source`)")
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_characters_dataName_avatar` " +
                    "ON `characters` (`dataName`, `avatar`)"
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `character_layer_groups` (
                `id` TEXT NOT NULL,
                `characterId` TEXT NOT NULL,
                `positionCustom` INTEGER NOT NULL,
                `positionNavigation` INTEGER NOT NULL,
                `imageNavigation` TEXT NOT NULL,
                `type` INTEGER NOT NULL,
                `sortOrder` INTEGER NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`characterId`) REFERENCES `characters`(`id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_character_layer_groups_characterId` " +
                    "ON `character_layer_groups` (`characterId`)"
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `character_layers` (
                `id` TEXT NOT NULL,
                `layerGroupId` TEXT NOT NULL,
                `image` TEXT NOT NULL,
                `isMoreColors` INTEGER NOT NULL,
                `thumb` TEXT NOT NULL,
                `sortOrder` INTEGER NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`layerGroupId`) REFERENCES `character_layer_groups`(`id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_character_layers_layerGroupId` " +
                    "ON `character_layers` (`layerGroupId`)"
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `character_colors` (
                `id` TEXT NOT NULL,
                `layerId` TEXT NOT NULL,
                `color` TEXT NOT NULL,
                `path` TEXT NOT NULL,
                `sortOrder` INTEGER NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`layerId`) REFERENCES `character_layers`(`id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_character_colors_layerId` " +
                    "ON `character_colors` (`layerId`)"
        )
    }
}
