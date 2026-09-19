package com.therian.oc.aaa.data.local

import android.content.Context
import android.util.Log
import com.therian.oc.aaa.core.utils.key.PermissionKey
import com.therian.oc.aaa.core.utils.key.SharePreferenceKey
import com.therian.oc.aaa.core.utils.key.ValueKey
import com.therian.oc.aaa.data.local.entity.AppSettingEntity
import com.therian.oc.aaa.data.local.entity.JsonCacheEntity
import com.therian.oc.aaa.data.local.entity.MediaItemEntity
import com.therian.oc.aaa.data.local.entity.SavedAvatarEntity
import com.therian.oc.aaa.data.model.custom.SuggestionModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File

object PersistenceRepository {
    private const val TAG = "PersistenceRepository"
    private const val MIGRATED_LEGACY_V1_KEY = "_legacy_migrated_v1"
    private const val MIGRATED_LEGACY_V2_KEY = "_legacy_migrated_v2"
    private const val LEGACY_TEMP_CLEANED_KEY = "_legacy_random_temp_removed_v1"
    private const val LEGACY_RANDOM_TEMP_ALBUM = "RANDOM_TEMP_ALBUM"
    private const val WHATSAPP_BUNDLE_NAME = "APP_BUNDLE_NAME"
    private val gson = Gson()

    suspend fun ensureMigrated(context: Context) {
        migrateLegacyIfNeeded(context)
    }

    suspend fun getSavedAvatarPaths(context: Context): ArrayList<String> =
        withContext(Dispatchers.IO) {
            migrateLegacyIfNeeded(context)
            DatabaseProvider.getDatabase(context)
                .savedAvatarDao()
                .getAllPaths()
                .toCollection(ArrayList())
        }

    suspend fun getSavedAvatar(context: Context, path: String): SuggestionModel? =
        withContext(Dispatchers.IO) {
            migrateLegacyIfNeeded(context)
            DatabaseProvider.getDatabase(context)
                .savedAvatarDao()
                .getByPath(path)
                ?.toSuggestionModel()
        }

    suspend fun deleteSavedAvatarRows(context: Context, paths: List<String>): Boolean =
        withContext(Dispatchers.IO) {
            migrateLegacyIfNeeded(context)
            if (paths.isEmpty()) return@withContext false
            DatabaseProvider.getDatabase(context).savedAvatarDao().deleteByPaths(paths) > 0
        }

    suspend fun getAlbumPathsAsync(context: Context, album: String): ArrayList<String> =
        withContext(Dispatchers.IO) {
            migrateLegacyIfNeeded(context)
            syncAlbumFromFiles(context, album)
            val db = DatabaseProvider.getDatabase(context)
            val items = db.mediaItemDao().getByAlbum(album)
            val missing = items.filter { !File(it.path).exists() || !isImageFile(File(it.path)) }
            if (missing.isNotEmpty()) {
                db.mediaItemDao().deleteByPaths(missing.map { it.path })
            }
            items.filter { File(it.path).exists() && isImageFile(File(it.path)) }
                .map { it.path }
                .toCollection(ArrayList())
        }

    fun readListJson(context: Context, fileName: String): String? =
        runBlocking(Dispatchers.IO) {
            migrateLegacyIfNeeded(context)
            val db = DatabaseProvider.getDatabase(context)
            when (fileName) {
                ValueKey.EDIT_FILE_INTERNAL -> {
                    val suggestions =
                        db.savedAvatarDao().getAll().mapNotNull { it.toSuggestionModel() }
                    gson.toJson(suggestions)
                }

                else -> db.jsonCacheDao().getJson(fileName)
            }
        }

    fun writeListJson(context: Context, fileName: String, json: String) =
        runBlocking(Dispatchers.IO) {
            migrateLegacyIfNeeded(context)
            val db = DatabaseProvider.getDatabase(context)
            when (fileName) {
                ValueKey.EDIT_FILE_INTERNAL -> {
                    val type = object : TypeToken<List<SuggestionModel>>() {}.type
                    val suggestions = runCatching {
                        gson.fromJson<List<SuggestionModel>>(json, type)
                    }.getOrNull().orEmpty()
                    db.savedAvatarDao().replaceAll(suggestions.toSavedAvatarEntities())
                }

                else -> db.jsonCacheDao().upsert(JsonCacheEntity(fileName, json))
            }
        }

    fun readModelJson(context: Context, fileName: String): String? =
        runBlocking(Dispatchers.IO) {
            migrateLegacyIfNeeded(context)
            DatabaseProvider.getDatabase(context).jsonCacheDao().getJson(fileName)
        }

    fun writeModelJson(context: Context, fileName: String, json: String) =
        runBlocking(Dispatchers.IO) {
            migrateLegacyIfNeeded(context)
            DatabaseProvider.getDatabase(context)
                .jsonCacheDao()
                .upsert(JsonCacheEntity(fileName, json))
        }

    fun hasInternalData(context: Context, fileName: String): Boolean =
        runBlocking(Dispatchers.IO) {
            migrateLegacyIfNeeded(context)
            val db = DatabaseProvider.getDatabase(context)
            when (fileName) {
                ValueKey.EDIT_FILE_INTERNAL -> db.savedAvatarDao().count() > 0
                else -> db.jsonCacheDao().countKey(fileName) > 0
            }
        }

    fun clearInternalData(context: Context, fileName: String) =
        runBlocking(Dispatchers.IO) {
            migrateLegacyIfNeeded(context)
            val db = DatabaseProvider.getDatabase(context)
            when (fileName) {
                ValueKey.EDIT_FILE_INTERNAL -> db.savedAvatarDao().clear()
                else -> db.jsonCacheDao().deleteByKey(fileName)
            }
            File(context.filesDir, fileName).takeIf { it.exists() }?.delete()
        }

    fun getAlbumPaths(context: Context, album: String): ArrayList<String> =
        runBlocking(Dispatchers.IO) {
            getAlbumPathsAsync(context, album)
        }

    fun upsertMediaItem(context: Context, album: String, path: String) =
        runBlocking(Dispatchers.IO) {
            migrateLegacyIfNeeded(context)
            val file = File(path)
            val timestamp =
                if (file.exists()) file.lastModified() else System.currentTimeMillis()
            DatabaseProvider.getDatabase(context).mediaItemDao().upsert(
                MediaItemEntity(
                    path = path,
                    album = album,
                    createdAt = timestamp,
                    updatedAt = timestamp
                )
            )
        }

    fun deleteMediaItems(context: Context, paths: List<String>) =
        runBlocking(Dispatchers.IO) {
            migrateLegacyIfNeeded(context)
            if (paths.isNotEmpty()) {
                DatabaseProvider.getDatabase(context).mediaItemDao().deleteByPaths(paths)
            }
        }

    fun deleteSavedAvatars(context: Context, paths: List<String>): Boolean =
        runBlocking(Dispatchers.IO) {
            migrateLegacyIfNeeded(context)
            if (paths.isEmpty()) return@runBlocking false
            DatabaseProvider.getDatabase(context).savedAvatarDao().deleteByPaths(paths) > 0
        }

    fun getStringSetting(context: Context, key: String, defaultValue: String): String =
        runBlocking(Dispatchers.IO) {
            migrateLegacyIfNeeded(context)
            DatabaseProvider.getDatabase(context).appSettingDao().getValue(key) ?: defaultValue
        }

    fun setStringSetting(context: Context, key: String, value: String) =
        runBlocking(Dispatchers.IO) {
            migrateLegacyIfNeeded(context)
            DatabaseProvider.getDatabase(context)
                .appSettingDao()
                .upsert(AppSettingEntity(key, value))
        }

    fun getBooleanSetting(context: Context, key: String, defaultValue: Boolean): Boolean {
        return getStringSetting(context, key, defaultValue.toString())
            .toBooleanStrictOrNull() ?: defaultValue
    }

    fun setBooleanSetting(context: Context, key: String, value: Boolean) {
        setStringSetting(context, key, value.toString())
    }

    fun getIntSetting(context: Context, key: String, defaultValue: Int): Int {
        return getStringSetting(context, key, defaultValue.toString()).toIntOrNull()
            ?: defaultValue
    }

    fun setIntSetting(context: Context, key: String, value: Int) {
        setStringSetting(context, key, value.toString())
    }

    private suspend fun migrateLegacyIfNeeded(context: Context) =
        withContext(Dispatchers.IO) {
            val db = DatabaseProvider.getDatabase(context)
            cleanupLegacyRandomPreviews(context)
            if (db.appSettingDao().getValue(MIGRATED_LEGACY_V2_KEY) == "true") {
                return@withContext
            }

            val migratedByEarlierRoomBuild =
                db.appSettingDao().getValue(MIGRATED_LEGACY_V1_KEY) == "true"
            if (!migratedByEarlierRoomBuild) {
                migrateSettings(context)
                migrateWhatsAppSettings(context)
            }

            migrateJsonCache(context, ValueKey.SUGGESTION_FILE_INTERNAL)
            migrateSavedAvatars(context)
            syncAlbumFromFiles(context, ValueKey.DOWNLOAD_ALBUM)
            syncAlbumFromFiles(context, ValueKey.DOWNLOAD_ALBUM_BACKGROUND)
            syncAlbumFromFiles(context, ValueKey.PRIDE_ALBUM)

            db.appSettingDao().upsert(AppSettingEntity(MIGRATED_LEGACY_V2_KEY, "true"))
        }

    private suspend fun cleanupLegacyRandomPreviews(context: Context) {
        val db = DatabaseProvider.getDatabase(context)
        if (db.appSettingDao().getValue(LEGACY_TEMP_CLEANED_KEY) == "true") return

        File(context.filesDir, LEGACY_RANDOM_TEMP_ALBUM).deleteRecursively()
        db.mediaItemDao().deleteByAlbum(LEGACY_RANDOM_TEMP_ALBUM)
        db.appSettingDao().upsert(AppSettingEntity(LEGACY_TEMP_CLEANED_KEY, "true"))
    }

    private suspend fun migrateSettings(context: Context) {
        val db = DatabaseProvider.getDatabase(context)
        val prefs =
            context.getSharedPreferences(SharePreferenceKey.SHARE_KEY, Context.MODE_PRIVATE)
        val settings = listOf(
            AppSettingEntity(
                SharePreferenceKey.KEY_LANGUAGE,
                prefs.getString(SharePreferenceKey.KEY_LANGUAGE, "") ?: ""
            ),
            AppSettingEntity(
                SharePreferenceKey.FIRST_LANG_KEY,
                prefs.getBoolean(SharePreferenceKey.FIRST_LANG_KEY, true).toString()
            ),
            AppSettingEntity(
                SharePreferenceKey.FIRST_PERMISSION_KEY,
                prefs.getBoolean(SharePreferenceKey.FIRST_PERMISSION_KEY, true).toString()
            ),
            AppSettingEntity(
                SharePreferenceKey.RATE_KEY,
                prefs.getBoolean(SharePreferenceKey.RATE_KEY, false).toString()
            ),
            AppSettingEntity(
                SharePreferenceKey.COUNT_BACK_KEY,
                prefs.getInt(SharePreferenceKey.COUNT_BACK_KEY, 0).toString()
            ),
            AppSettingEntity(
                SharePreferenceKey.MUSIC_KEY,
                prefs.getBoolean(SharePreferenceKey.MUSIC_KEY, true).toString()
            ),
            AppSettingEntity(
                PermissionKey.STORAGE_KEY,
                prefs.getInt(PermissionKey.STORAGE_KEY, 0).toString()
            ),
            AppSettingEntity(
                PermissionKey.NOTIFICATION_KEY,
                prefs.getInt(PermissionKey.NOTIFICATION_KEY, 0).toString()
            ),
            AppSettingEntity(
                PermissionKey.CAMERA_KEY,
                prefs.getInt(PermissionKey.CAMERA_KEY, 0).toString()
            ),
            AppSettingEntity(
                PermissionKey.QUANTITY_UNZIPPED,
                prefs.getString(PermissionKey.QUANTITY_UNZIPPED, "[]") ?: "[]"
            ),
            AppSettingEntity(
                SharePreferenceKey.PRIDE_CUSTOM_FLAGS,
                prefs.getString(SharePreferenceKey.PRIDE_CUSTOM_FLAGS, "[]") ?: "[]"
            )
        )
        settings.forEach { db.appSettingDao().upsert(it) }
    }

    private suspend fun migrateWhatsAppSettings(context: Context) {
        val db = DatabaseProvider.getDatabase(context)
        val prefs = context.getSharedPreferences(WHATSAPP_BUNDLE_NAME, Context.MODE_PRIVATE)
        prefs.all.forEach { (key, value) ->
            db.appSettingDao().upsert(AppSettingEntity(key, value.toString()))
        }
    }

    private suspend fun migrateJsonCache(context: Context, fileName: String) {
        val db = DatabaseProvider.getDatabase(context)
        if (db.jsonCacheDao().countKey(fileName) > 0) return
        val file = File(context.filesDir, fileName)
        if (file.exists() && file.length() > 0) {
            runCatching {
                db.jsonCacheDao().upsert(JsonCacheEntity(fileName, file.readText()))
            }.onFailure {
                Log.e(TAG, "Failed to migrate $fileName", it)
            }
        }
    }

    private suspend fun migrateSavedAvatars(context: Context) {
        val db = DatabaseProvider.getDatabase(context)
        if (db.savedAvatarDao().count() > 0) return
        val file = File(context.filesDir, ValueKey.EDIT_FILE_INTERNAL)
        if (!file.exists() || file.length() == 0L) return

        runCatching {
            val type = object : TypeToken<List<SuggestionModel>>() {}.type
            val suggestions =
                gson.fromJson<List<SuggestionModel>>(file.readText(), type).orEmpty()
            db.savedAvatarDao().replaceAll(suggestions.toSavedAvatarEntities())
        }.onFailure {
            Log.e(TAG, "Failed to migrate saved avatars", it)
        }
    }

    private suspend fun syncAlbumFromFiles(context: Context, album: String) {
        val directory = File(context.filesDir, album)
        if (!directory.exists() || !directory.isDirectory) return
        val items = directory.listFiles()
            ?.filter { isImageFile(it) }
            ?.map {
                MediaItemEntity(
                    path = it.absolutePath,
                    album = album,
                    createdAt = it.lastModified(),
                    updatedAt = it.lastModified()
                )
            }
            .orEmpty()
        if (items.isNotEmpty()) {
            DatabaseProvider.getDatabase(context).mediaItemDao().upsertAll(items)
        }
    }

    private fun List<SuggestionModel>.toSavedAvatarEntities(): List<SavedAvatarEntity> {
        val now = System.currentTimeMillis()
        return mapIndexedNotNull { index, suggestion ->
            if (suggestion.pathInternalEdit.isBlank()) return@mapIndexedNotNull null
            val timestamp = now - index
            SavedAvatarEntity(
                avatarPath = suggestion.avatarPath,
                pathInternalEdit = suggestion.pathInternalEdit,
                suggestionJson = gson.toJson(suggestion),
                createdAt = timestamp,
                updatedAt = timestamp
            )
        }
    }

    private fun SavedAvatarEntity.toSuggestionModel(): SuggestionModel? {
        return runCatching {
            gson.fromJson(suggestionJson, SuggestionModel::class.java).apply {
                avatarPath = this@toSuggestionModel.avatarPath
                pathInternalEdit = this@toSuggestionModel.pathInternalEdit
            }
        }.getOrNull()
    }

    private fun isImageFile(file: File): Boolean {
        val imageExtensions = listOf("jpg", "jpeg", "png", "bmp", "webp")
        return file.isFile && imageExtensions.contains(file.extension.lowercase())
    }
}
