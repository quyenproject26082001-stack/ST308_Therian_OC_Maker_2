package com.therian.oc.aaa.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "characters",
    indices = [
        Index(value = ["source"]),
        Index(value = ["dataName", "avatar"])
    ]
)
data class CharacterEntity(
    @PrimaryKey val id: String,
    val source: String,
    val dataName: String,
    val avatar: String,
    val level: Int,
    val isFromApi: Boolean,
    val sortOrder: Int
)
