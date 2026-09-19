package com.therian.oc.aaa.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.therian.oc.aaa.data.local.entity.JsonCacheEntity

@Dao
interface JsonCacheDao {
    @Query("SELECT json FROM json_cache WHERE `key` = :key LIMIT 1")
    suspend fun getJson(key: String): String?

    @Query("SELECT COUNT(*) FROM json_cache WHERE `key` = :key")
    suspend fun countKey(key: String): Int

    @Upsert
    suspend fun upsert(cache: JsonCacheEntity)

    @Query("DELETE FROM json_cache WHERE `key` = :key")
    suspend fun deleteByKey(key: String)
}
