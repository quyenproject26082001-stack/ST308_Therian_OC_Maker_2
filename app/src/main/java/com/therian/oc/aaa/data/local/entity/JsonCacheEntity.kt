package com.therian.oc.aaa.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "json_cache")
data class JsonCacheEntity(
    @PrimaryKey val key: String,
    val json: String,
    val updatedAt: Long = System.currentTimeMillis()
)
