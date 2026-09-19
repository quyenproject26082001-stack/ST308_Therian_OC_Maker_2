package com.therian.oc.aaa.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.therian.oc.aaa.data.local.entity.CharacterColorEntity
import com.therian.oc.aaa.data.local.entity.CharacterEntity
import com.therian.oc.aaa.data.local.entity.CharacterLayerEntity
import com.therian.oc.aaa.data.local.entity.CharacterLayerGroupEntity
import com.therian.oc.aaa.data.local.relation.CharacterWithLayers

@Dao
interface CharacterDao {
    @Query(
        """
        SELECT * FROM characters
        ORDER BY level ASC,
            CASE source WHEN 'local' THEN 0 ELSE 1 END ASC,
            sortOrder ASC
        """
    )
    suspend fun getCharacterSummaries(): List<CharacterEntity>

    @Transaction
    @Query(
        """
        SELECT * FROM characters
        WHERE source = :source AND dataName = :dataName AND avatar = :avatar
        LIMIT 1
        """
    )
    suspend fun getCharacter(
        source: String,
        dataName: String,
        avatar: String
    ): CharacterWithLayers?

    @Query("SELECT COUNT(*) FROM characters")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM characters WHERE source = :source")
    suspend fun countBySource(source: String): Int

    @Query("DELETE FROM characters WHERE source = :source")
    suspend fun deleteBySource(source: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCharacters(items: List<CharacterEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLayerGroups(items: List<CharacterLayerGroupEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLayers(items: List<CharacterLayerEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertColors(items: List<CharacterColorEntity>)
}
