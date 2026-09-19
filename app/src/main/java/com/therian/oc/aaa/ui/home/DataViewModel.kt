package com.therian.oc.aaa.ui.home

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.therian.oc.aaa.core.helper.AssetHelper
import com.therian.oc.aaa.core.helper.InternetHelper
import com.therian.oc.aaa.core.service.RetrofitClient
import com.therian.oc.aaa.core.service.RetrofitPreventive
import com.therian.oc.aaa.core.utils.DataLocal.isFailBaseURL
import com.therian.oc.aaa.core.utils.key.DomainKey
import com.therian.oc.aaa.core.utils.state.HandleState
import com.therian.oc.aaa.data.local.CharacterRepository
import com.therian.oc.aaa.data.local.PersistenceRepository
import com.therian.oc.aaa.data.model.DataAPI
import com.therian.oc.aaa.data.model.PartAPI
import com.therian.oc.aaa.data.model.custom.ColorModel
import com.therian.oc.aaa.data.model.custom.CustomizeModel
import com.therian.oc.aaa.data.model.custom.LayerListModel
import com.therian.oc.aaa.data.model.custom.LayerModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import retrofit2.Response
import kotlin.coroutines.cancellation.CancellationException

class DataViewModel : ViewModel() {
    private val _allData = MutableStateFlow<ArrayList<CustomizeModel>>(arrayListOf())
    val allData: StateFlow<ArrayList<CustomizeModel>> = _allData.asStateFlow()

    private val _dataLoadFinished = MutableStateFlow(false)
    val dataLoadFinished: StateFlow<Boolean> = _dataLoadFinished.asStateFlow()

    private val _getDataAPI = MutableLiveData<List<PartAPI>>()
    val getDataAPI: LiveData<List<PartAPI>> get() = _getDataAPI

    fun saveAndReadData(context: Context) {
        viewModelScope.launch {
            val startedAt = System.currentTimeMillis()
            _dataLoadFinished.value = false
            val summaries = try {
                withContext(Dispatchers.IO) {
                    PersistenceRepository.ensureMigrated(context)
                    CharacterRepository.migrateLegacyCharacterFiles(context)

                    if (!CharacterRepository.hasSource(
                            context,
                            CharacterRepository.SOURCE_LOCAL
                        )
                    ) {
                        val localCharacters = AssetHelper.getDataFromAsset(context)
                        CharacterRepository.replaceSource(
                            context,
                            CharacterRepository.SOURCE_LOCAL,
                            localCharacters
                        )
                    }

                    var current = CharacterRepository.getSummaries(context)
                    Log.d(
                        "ROOM_CHARACTER",
                        "Before API sync | total=${current.size} | api=${current.count { it.isFromAPI }}"
                    )

                    if (InternetHelper.checkInternet(context)) {
                        getAllParts(context).collect { state ->
                            if (state == HandleState.SUCCESS) {
                                current = CharacterRepository.getSummaries(context)
                            }
                        }
                    }
                    current
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("ROOM_CHARACTER", "Character loading failed", e)
                withContext(Dispatchers.IO) {
                    runCatching { CharacterRepository.getSummaries(context) }
                        .getOrDefault(arrayListOf())
                }
            }

            _allData.value = summaries
            _dataLoadFinished.value = true
            Log.d(
                "ROOM_CHARACTER",
                "Data ready in ${System.currentTimeMillis() - startedAt}ms | summaries=${summaries.size}"
            )
        }
    }

    fun ensureData(context: Context) {
        if (_allData.value.isEmpty()) {
            saveAndReadData(context)
        } else {
            _dataLoadFinished.value = true
        }
    }

    suspend fun getCharacter(
        context: Context,
        summary: CustomizeModel
    ): CustomizeModel? = withContext(Dispatchers.IO) {
        CharacterRepository.getCharacter(context, summary)
    }

    suspend fun getCharacter(context: Context, position: Int): CustomizeModel? {
        val summary = _allData.value.getOrNull(position) ?: return null
        return getCharacter(context, summary)
    }

    fun getAllParts(context: Context): Flow<HandleState> = flow {
        emit(HandleState.LOADING)

        val primary = fetchApi { RetrofitClient.api.getAllData() }
        val response: Response<Map<String, List<PartAPI>>>?
        val baseDomain: String
        if (primary?.isSuccessful == true && primary.body() != null) {
            response = primary
            baseDomain = DomainKey.BASE_URL
            isFailBaseURL = false
        } else {
            response = fetchApi { RetrofitPreventive.api.getAllData() }
            baseDomain = DomainKey.BASE_URL_PREVENTIVE
            isFailBaseURL = true
        }

        val dataMap = response?.takeIf { it.isSuccessful }?.body()
        if (dataMap != null) {
            withContext(Dispatchers.IO) {
                saveApiCharacters(context, baseDomain, dataMap)
            }
            emit(HandleState.SUCCESS)
        } else {
            Log.e("ROOM_CHARACTER", "Primary and preventive character APIs failed")
            emit(HandleState.FAIL)
        }
    }

    private suspend fun fetchApi(
        request: suspend () -> Response<Map<String, List<PartAPI>>>
    ): Response<Map<String, List<PartAPI>>>? {
        return withTimeoutOrNull(5_000) {
            runCatching { request() }
                .onFailure { Log.e("ROOM_CHARACTER", "API request failed", it) }
                .getOrNull()
        }
    }

    private suspend fun saveApiCharacters(
        context: Context,
        baseDomain: String,
        dataMap: Map<String, List<PartAPI>>
    ) {
        val characters = dataMap.asSequence().map { (name, parts) ->
            createApiCharacter(baseDomain, DataAPI(name, parts))
        }
        val count = CharacterRepository.replaceSource(
            context,
            CharacterRepository.SOURCE_API,
            characters
        )
        CharacterRepository.deleteLegacyApiFile(context)
        Log.d("ROOM_CHARACTER", "API characters saved to normalized Room tables: $count")
    }

    private fun createApiCharacter(
        baseDomain: String,
        data: DataAPI
    ): CustomizeModel {
        val avatar =
            "$baseDomain${DomainKey.SUB_DOMAIN}/${data.name}/${DomainKey.AVATAR_CHARACTER_API}"
        val sortedParts = data.parts.sortedBy { it.level }
        val layerGroups = ArrayList<LayerListModel>(sortedParts.size)

        sortedParts.forEach { part ->
            val positions = if (part.parts.contains("-")) {
                part.parts.split("-")
            } else {
                part.parts.split("_")
            }
            val positionCustom = positions.getOrNull(0)?.toIntOrNull()?.minus(1)
            val positionNavigation = positions.getOrNull(1)?.toIntOrNull()?.minus(1)
            if (positionCustom == null || positionNavigation == null) {
                Log.e(
                    "ROOM_CHARACTER",
                    "Invalid part '${part.parts}' for ${data.name}"
                )
                return@forEach
            }

            layerGroups += LayerListModel(
                positionCustom = positionCustom,
                positionNavigation = positionNavigation,
                imageNavigation =
                    "$baseDomain${DomainKey.SUB_DOMAIN}/${data.name}/${part.parts}/${DomainKey.IMAGE_NAVIGATION}",
                layer = getDataLayer(baseDomain, part, part.parts),
                type = positions.getOrNull(2)?.toIntOrNull() ?: 0
            )
        }
        layerGroups.sortBy { it.positionNavigation }

        return CustomizeModel(
            dataName = data.name,
            avatar = avatar,
            layerList = layerGroups,
            level = sortedParts.minOfOrNull { it.level } ?: 100,
            isFromAPI = true
        )
    }

    private fun getDataLayer(
        baseDomain: String,
        part: PartAPI,
        layer: String
    ): ArrayList<LayerModel> {
        return if (part.colorArray.isNotEmpty()) {
            getDataAPIColor(baseDomain, part, layer)
        } else {
            getDataAPINoColor(baseDomain, part, layer)
        }
    }

    private fun getDataAPINoColor(
        baseDomain: String,
        part: PartAPI,
        layer: String
    ): ArrayList<LayerModel> {
        val quantity = when (part.position) {
            "data300", "data100" -> part.quantity / 2
            else -> part.quantity
        }
        val prefix =
            "$baseDomain${DomainKey.SUB_DOMAIN}/${part.position}/$layer/"
        return ArrayList<LayerModel>(quantity).apply {
            for (index in 1..quantity) {
                add(
                    LayerModel(
                        image = "$prefix$index${DomainKey.LAYER_EXTENSION}",
                        isMoreColors = false,
                        listColor = arrayListOf(),
                        thumb = "${prefix}thumb_$index${DomainKey.LAYER_EXTENSION}"
                    )
                )
            }
        }
    }

    private fun getDataAPIColor(
        baseDomain: String,
        part: PartAPI,
        layer: String
    ): ArrayList<LayerModel> {
        val colors = part.colorArray.split(",").filter { it.isNotBlank() }
        val prefix =
            "$baseDomain${DomainKey.SUB_DOMAIN}/${part.position}/$layer/"
        return ArrayList<LayerModel>(part.quantity).apply {
            for (index in 1..part.quantity) {
                val variants = colors.map { color ->
                    ColorModel(
                        color = "#$color",
                        path = "$prefix$color/$index${DomainKey.LAYER_EXTENSION}"
                    )
                }.toCollection(ArrayList())
                add(
                    LayerModel(
                        image = variants.firstOrNull()?.path
                            ?: "$prefix$index${DomainKey.LAYER_EXTENSION}",
                        isMoreColors = variants.isNotEmpty(),
                        listColor = variants,
                        thumb = "${prefix}thumb_$index${DomainKey.LAYER_EXTENSION}"
                    )
                )
            }
        }
    }
}
