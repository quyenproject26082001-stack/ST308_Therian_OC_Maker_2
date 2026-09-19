package com.therian.oc.aaa.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "character_layers",
    foreignKeys = [
        ForeignKey(
            entity = CharacterLayerGroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["layerGroupId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["layerGroupId"])]
)
data class CharacterLayerEntity(
    @PrimaryKey val id: String,
    val layerGroupId: String,
    val image: String,
    val isMoreColors: Boolean,
    val thumb: String,
    val sortOrder: Int
)
