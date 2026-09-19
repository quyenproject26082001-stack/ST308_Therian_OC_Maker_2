package com.therian.oc.aaa.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.therian.oc.aaa.data.local.entity.SavedAvatarEntity

@Dao
interface SavedAvatarDao {
    @Query("SELECT * FROM saved_avatars ORDER BY createdAt DESC, id DESC")
    suspend fun getAll(): List<SavedAvatarEntity>

    @Query("SELECT pathInternalEdit FROM saved_avatars ORDER BY createdAt DESC, id DESC")
    suspend fun getAllPaths(): List<String>

    @Query("SELECT * FROM saved_avatars WHERE pathInternalEdit = :path LIMIT 1")
    suspend fun getByPath(path: String): SavedAvatarEntity?

    @Query("SELECT COUNT(*) FROM saved_avatars")
    suspend fun count(): Int

    @Query("DELETE FROM saved_avatars")
    suspend fun clear()

    @Query("DELETE FROM saved_avatars WHERE pathInternalEdit IN (:paths)")
    suspend fun deleteByPaths(paths: List<String>): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<SavedAvatarEntity>)

    @Upsert
    suspend fun upsert(item: SavedAvatarEntity)

    @Transaction
    suspend fun replaceAll(items: List<SavedAvatarEntity>) {
        clear()
        insertAll(items)
    }
}
