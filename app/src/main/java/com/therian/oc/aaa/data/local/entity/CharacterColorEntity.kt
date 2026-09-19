package com.therian.oc.aaa.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "character_colors",
    foreignKeys = [
        ForeignKey(
            entity = CharacterLayerEntity::class,
            parentColumns = ["id"],
            childColumns = ["layerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["layerId"])]
)
data class CharacterColorEntity(
    @PrimaryKey val id: String,
    val layerId: String,
    val color: String,
    val path: String,
    val sortOrder: Int
)
