package com.therian.oc.aaa.data.local

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import com.therian.oc.aaa.core.utils.key.ValueKey
import com.therian.oc.aaa.data.local.entity.CharacterColorEntity
import com.therian.oc.aaa.data.local.entity.CharacterEntity
import com.therian.oc.aaa.data.local.entity.CharacterLayerEntity
import com.therian.oc.aaa.data.local.entity.CharacterLayerGroupEntity
import com.therian.oc.aaa.data.local.relation.CharacterWithLayers
import com.therian.oc.aaa.data.model.custom.ColorModel
import com.therian.oc.aaa.data.model.custom.CustomizeModel
import com.therian.oc.aaa.data.model.custom.LayerListModel
import com.therian.oc.aaa.data.model.custom.LayerModel
import com.google.gson.Gson
import com.google.gson.stream.JsonReader
import java.io.File

object CharacterRepository {
    const val SOURCE_LOCAL = "local"
    const val SOURCE_API = "api"
    private val gson = Gson()

    suspend fun getSummaries(context: Context): ArrayList<CustomizeModel> {
        return DatabaseProvider.getDatabase(context)
            .characterDao()
            .getCharacterSummaries()
            .map { it.toSummaryModel() }
            .toCollection(ArrayList())
    }

    suspend fun getCharacter(context: Context, summary: CustomizeModel): CustomizeModel? {
        if (summary.layerList.isNotEmpty()) return summary
        val source = if (summary.isFromAPI) SOURCE_API else SOURCE_LOCAL
        val character = DatabaseProvider.getDatabase(context)
            .characterDao()
            .getCharacter(source, summary.dataName, summary.avatar)
            ?.toCustomizeModel()
        if (character != null) {
            Log.d(
                "ROOM_CHARACTER",
                "Loaded one character: ${character.dataName} | groups=${character.layerList.size} | layers=${character.layerList.sumOf { it.layer.size }}"
            )
        }
        return character
    }

    suspend fun hasSource(context: Context, source: String): Boolean {
        return DatabaseProvider.getDatabase(context).characterDao().countBySource(source) > 0
    }

    suspend fun migrateLegacyCharacterFiles(context: Context) {
        val dao = DatabaseProvider.getDatabase(context).characterDao()
        if (dao.count() > 0) return

        val localCount = importLegacyFile(
            context,
            ValueKey.DATA_FILE_INTERNAL,
            SOURCE_LOCAL
        )
        val apiCount = importLegacyFile(
            context,
            ValueKey.DATA_FILE_API_INTERNAL,
            SOURCE_API
        )
        Log.d(
            "ROOM_CHARACTER",
            "Legacy character migration complete | local=$localCount | api=$apiCount"
        )
    }

    suspend fun replaceSource(
        context: Context,
        source: String,
        characters: List<CustomizeModel>
    ): Int = replaceSource(context, source, characters.asSequence())

    suspend fun replaceSource(
        context: Context,
        source: String,
        characters: Sequence<CustomizeModel>
    ): Int {
        val database = DatabaseProvider.getDatabase(context)
        return database.withTransaction {
            val dao = database.characterDao()
            dao.deleteBySource(source)

            var characterIndex = 0
            val iterator = characters.iterator()
            while (iterator.hasNext()) {
                val rows = iterator.next().toRows(source, characterIndex)
                dao.insertCharacters(rows.characters)
                if (rows.layerGroups.isNotEmpty()) dao.insertLayerGroups(rows.layerGroups)
                if (rows.layers.isNotEmpty()) dao.insertLayers(rows.layers)
                if (rows.colors.isNotEmpty()) dao.insertColors(rows.colors)
                characterIndex++
            }
            characterIndex
        }
    }

    fun deleteLegacyApiFile(context: Context) {
        File(context.filesDir, ValueKey.DATA_FILE_API_INTERNAL)
            .takeIf { it.exists() }
            ?.delete()
    }

    private suspend fun importLegacyFile(
        context: Context,
        fileName: String,
        source: String
    ): Int {
        val file = File(context.filesDir, fileName)
        if (!file.exists() || file.length() == 0L) return 0

        val models: Sequence<CustomizeModel> = sequence {
            file.bufferedReader().use { reader ->
                JsonReader(reader).use { jsonReader ->
                    jsonReader.beginArray()
                    while (jsonReader.hasNext()) {
                        val model: CustomizeModel =
                            gson.fromJson(jsonReader, CustomizeModel::class.java)
                        yield(model)
                    }
                    jsonReader.endArray()
                }
            }
        }
        return runCatching { replaceSource(context, source, models) }
            .onFailure { Log.e("ROOM_CHARACTER", "Failed to migrate $fileName", it) }
            .getOrDefault(0)
    }

    private data class CharacterRows(
        val characters: List<CharacterEntity>,
        val layerGroups: ArrayList<CharacterLayerGroupEntity> = arrayListOf(),
        val layers: ArrayList<CharacterLayerEntity> = arrayListOf(),
        val colors: ArrayList<CharacterColorEntity> = arrayListOf()
    )

    private fun CustomizeModel.toRows(source: String, characterIndex: Int): CharacterRows {
        val characterId = "$source|$dataName"
        val rows = CharacterRows(
            characters = listOf(
                CharacterEntity(
                    id = characterId,
                    source = source,
                    dataName = dataName,
                    avatar = avatar,
                    level = level,
                    isFromApi = isFromAPI,
                    sortOrder = characterIndex
                )
            )
        )

        layerList.forEachIndexed { groupIndex, group ->
            val groupId = "$characterId|group|$groupIndex"
            rows.layerGroups += CharacterLayerGroupEntity(
                id = groupId,
                characterId = characterId,
                positionCustom = group.positionCustom,
                positionNavigation = group.positionNavigation,
                imageNavigation = group.imageNavigation,
                type = group.type,
                sortOrder = groupIndex
            )

            group.layer.forEachIndexed { layerIndex, layer ->
                val layerId = "$groupId|layer|$layerIndex"
                rows.layers += CharacterLayerEntity(
                    id = layerId,
                    layerGroupId = groupId,
                    image = layer.image,
                    isMoreColors = layer.isMoreColors,
                    thumb = layer.thumb,
                    sortOrder = layerIndex
                )
                layer.listColor.forEachIndexed { colorIndex, color ->
                    rows.colors += CharacterColorEntity(
                        id = "$layerId|color|$colorIndex",
                        layerId = layerId,
                        color = color.color,
                        path = color.path,
                        sortOrder = colorIndex
                    )
                }
            }
        }
        return rows
    }

    private fun CharacterEntity.toSummaryModel() = CustomizeModel(
        dataName = dataName,
        avatar = avatar,
        level = level,
        isFromAPI = isFromApi
    )

    private fun CharacterWithLayers.toCustomizeModel(): CustomizeModel {
        val groups = layerGroups
            .sortedBy { it.layerGroup.sortOrder }
            .map { group ->
                LayerListModel(
                    positionCustom = group.layerGroup.positionCustom,
                    positionNavigation = group.layerGroup.positionNavigation,
                    imageNavigation = group.layerGroup.imageNavigation,
                    layer = group.layers
                        .sortedBy { it.layer.sortOrder }
                        .map { layer ->
                            LayerModel(
                                image = layer.layer.image,
                                isMoreColors = layer.layer.isMoreColors,
                                listColor = layer.colors
                                    .sortedBy { it.sortOrder }
                                    .map { ColorModel(it.color, it.path) }
                                    .toCollection(ArrayList()),
                                thumb = layer.layer.thumb
                            )
                        }
                        .toCollection(ArrayList()),
                    type = group.layerGroup.type
                )
            }
            .toCollection(ArrayList())

        return CustomizeModel(
            dataName = character.dataName,
            avatar = character.avatar,
            layerList = groups,
            level = character.level,
            isFromAPI = character.isFromApi
        )
    }
}
