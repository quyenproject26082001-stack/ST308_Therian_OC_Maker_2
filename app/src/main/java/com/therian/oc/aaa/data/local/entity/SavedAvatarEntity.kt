package com.therian.oc.aaa.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "saved_avatars",
    indices = [Index(value = ["pathInternalEdit"], unique = true)]
)
data class SavedAvatarEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val avatarPath: String,
    val pathInternalEdit: String,
    val suggestionJson: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
