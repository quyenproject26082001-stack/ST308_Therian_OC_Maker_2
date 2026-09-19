package com.therian.oc.aaa.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "media_items",
    indices = [Index(value = ["album"])]
)
data class MediaItemEntity(
    @PrimaryKey val path: String,
    val album: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
