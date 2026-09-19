package com.therian.oc.aaa.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "character_layer_groups",
    foreignKeys = [
        ForeignKey(
            entity = CharacterEntity::class,
            parentColumns = ["id"],
            childColumns = ["characterId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["characterId"])]
)
data class CharacterLayerGroupEntity(
    @PrimaryKey val id: String,
    val characterId: String,
    val positionCustom: Int,
    val positionNavigation: Int,
    val imageNavigation: String,
    val type: Int,
    val sortOrder: Int
)
