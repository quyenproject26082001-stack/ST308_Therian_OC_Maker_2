package com.therian.oc.aaa.ui.add_character

import android.R.attr.bitmap
import android.R.attr.type
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.drawable.toDrawable
import androidx.lifecycle.ViewModel
import com.therian.oc.aaa.core.helper.AssetHelper
import com.therian.oc.aaa.core.helper.BitmapHelper
import com.therian.oc.aaa.core.helper.InternetHelper
import com.therian.oc.aaa.core.helper.MediaHelper
import com.therian.oc.aaa.core.helper.RemoteAssetHelper
import com.therian.oc.aaa.core.utils.DataLocal
import com.therian.oc.aaa.core.utils.key.AssetsKey
import com.therian.oc.aaa.core.utils.key.DomainKey
import com.therian.oc.aaa.core.utils.key.ValueKey
import com.therian.oc.aaa.core.utils.state.SaveState
import com.therian.oc.aaa.data.model.SelectedModel
import com.therian.oc.aaa.data.model.draw.Draw
import com.therian.oc.aaa.data.model.draw.DrawableDraw
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.text.SimpleDateFormat
import java.util.Date

class AddCharacterViewModel : ViewModel() {

    private data class RemoteAssetConfig(
        val localAssetFolder: String,
        val remoteBaseUrl: String,
        val extensions: List<String>,
        val includePickerItem: Boolean = false
    )

    private val backgroundConfig = RemoteAssetConfig(
        localAssetFolder = AssetsKey.BACKGROUND_ASSET,
        remoteBaseUrl = DomainKey.getAddCharacterAssetUrl(AssetsKey.BACKGROUND_ASSET),
        extensions = listOf("png"),
        includePickerItem = true
    )

    private val stickerConfig = RemoteAssetConfig(
        localAssetFolder = AssetsKey.STICKER_ASSET,
        remoteBaseUrl = DomainKey.getAddCharacterAssetUrl(AssetsKey.STICKER_ASSET),
        extensions = listOf("png", "webp")
    )

    private val speechConfig = RemoteAssetConfig(
        localAssetFolder = AssetsKey.SPEECH_ASSET,
        remoteBaseUrl = DomainKey.getAddCharacterAssetUrl(AssetsKey.SPEECH_ASSET),
        extensions = listOf("png", "webp", "jpg")
    )



    var backgroundImageList: ArrayList<SelectedModel> = arrayListOf()
    var backgroundColorList: ArrayList<SelectedModel> = arrayListOf()
    var stickerList: ArrayList<SelectedModel> = arrayListOf()
    var speechList: ArrayList<SelectedModel> = arrayListOf()
    var textFontList: ArrayList<SelectedModel> = arrayListOf()
    var textColorList: ArrayList<SelectedModel> = arrayListOf()

    private val _typeNavigation = MutableStateFlow<Int>(-1)
    val typeNavigation = _typeNavigation.asStateFlow()

    private val _typeBackground = MutableStateFlow<Int>(-1)
    val typeBackground = _typeBackground.asStateFlow()

    private val _isFocusEditText = MutableStateFlow<Boolean>(false)
    val isFocusEditText = _isFocusEditText.asStateFlow()

    var currentDraw: Draw? = null

    var drawViewList: ArrayList<Draw> = arrayListOf()

    lateinit var layoutParams: ViewGroup.MarginLayoutParams

    var originalMarginBottom: Int = 0

    var pathDefault = ""

    fun setTypeNavigation(type: Int) {
        _typeNavigation.value = type
    }

    fun setTypeBackground(type: Int) {
        _typeBackground.value = type
    }

    fun setIsFocusEditText(status: Boolean) {
        _isFocusEditText.value = status
    }

    suspend fun loadDataDefault(context: Context) {
        backgroundImageList.clear()
        backgroundImageList.addAll(loadAssetOptions(context, backgroundConfig))


        backgroundColorList.clear()
        backgroundColorList.addAll(DataLocal.getBackgroundColorDefault(context))


        stickerList.clear()
        stickerList.addAll(loadAssetOptions(context, stickerConfig))



        speechList.clear()
        speechList.addAll(
            AssetHelper.getSubfoldersAsset(context, AssetsKey.SPEECH_ASSET).map { SelectedModel(path = it) })

        textFontList.clear()
        textFontList.addAll(DataLocal.getTextFontDefault())
        textFontList.first().isSelected = true

        textColorList.clear()
        textColorList.addAll(DataLocal.getTextColorDefault(context))
        textColorList[1].isSelected = true
    }

    private suspend fun loadAssetOptions(context: Context, config:
    RemoteAssetConfig): ArrayList<SelectedModel> {
        val resolvedAssets = if (InternetHelper.checkInternet(context)) {
            val remoteAssets =
                RemoteAssetHelper.getSequentialRemoteAssets(config.remoteBaseUrl,
                    config.extensions)
            if (remoteAssets.isNotEmpty()) {

                remoteAssets
            } else {

                AssetHelper.getSubfoldersAsset(context,
                    config.localAssetFolder)
            }
        } else {

            AssetHelper.getSubfoldersAsset(context, config.localAssetFolder)
        }

        val shouldUseFallback = resolvedAssets.isEmpty() &&
                (config.localAssetFolder == AssetsKey.BACKGROUND_ASSET ||
                        config.localAssetFolder == AssetsKey.STICKER_ASSET)

        val finalAssets = if (shouldUseFallback) {
            val baseUrl = config.remoteBaseUrl.trimEnd('/')
            val ext = config.extensions.firstOrNull() ?: "png"


                (1..10).map { index ->
                "$baseUrl/$index.$ext"
            }
        } else {
            resolvedAssets
        }

        val items = finalAssets.map { SelectedModel(path =
            it) }.toCollection(ArrayList())

        if (config.includePickerItem) {
            items.add(0, SelectedModel(path = AssetsKey.NONE_LAYER, isSelected = true))
            items.add(0, SelectedModel())
        }

        return items
    }


    suspend fun updateBackgroundImageSelected(position: Int) {
        backgroundColorList = backgroundColorList.map { it.copy(isSelected = false) }.toCollection(ArrayList())
        backgroundImageList.forEachIndexed { index, model ->
            model.isSelected = index == position
        }
    }

    suspend fun updateBackgroundColorSelected(position: Int) {
        Log.d("AddCharacterViewModel", "updateBackgroundColorSelected called with position=$position")
        Log.d("AddCharacterViewModel", "Before update: backgroundColorList[0].color=${String.format("#%06X", 0xFFFFFF and backgroundColorList[0].color)}, isSelected=${backgroundColorList[0].isSelected}")

        backgroundImageList = backgroundImageList.map { it.copy(isSelected = false) }.toCollection(ArrayList())
        backgroundColorList.forEachIndexed { index, model ->
            Log.d("AddCharacterViewModel", "Setting position $index isSelected = ${index == position}")
            model.isSelected = index == position
        }

        Log.d("AddCharacterViewModel", "After update: backgroundColorList[0].color=${String.format("#%06X", 0xFFFFFF and backgroundColorList[0].color)}, isSelected=${backgroundColorList[0].isSelected}")
    }

    fun updateTextFontSelected(position: Int) {
        textFontList = textFontList.map { it.copy(isSelected = false) }.toCollection(ArrayList())
        textFontList.forEachIndexed { index, model ->
            model.isSelected = index == position
        }
    }

    fun updateTextColorSelected(position: Int) {
        Log.d("AddCharacterViewModel", "updateTextColorSelected called with position=$position")
        Log.d("AddCharacterViewModel", "Before update: textColorList[0].color=${String.format("#%06X", 0xFFFFFF and textColorList[0].color)}, isSelected=${textColorList[0].isSelected}")

        textColorList = textColorList.map { it.copy(isSelected = false) }.toCollection(ArrayList())
        textColorList.forEachIndexed { index, model ->
            Log.d("AddCharacterViewModel", "Setting position $index isSelected = ${index == position}")
            model.isSelected = index == position
        }

        Log.d("AddCharacterViewModel", "After update: textColorList[0].color=${String.format("#%06X", 0xFFFFFF and textColorList[0].color)}, isSelected=${textColorList[0].isSelected}")
    }

    fun updateCurrentCurrentDraw(draw: Draw) {
        currentDraw = draw
    }

    fun addDrawView(draw: Draw) {
        drawViewList.add(draw)
    }

    fun resetSelectionState() {
        backgroundImageList.forEach { it.isSelected = false }
        backgroundImageList.getOrNull(1)?.isSelected = true
        backgroundColorList.forEach { it.isSelected = false }
        stickerList.forEach { it.isSelected = false }
        speechList.forEach { it.isSelected = false }


        currentDraw = null
        drawViewList.removeAll { !it.isCharacter }
    }

    fun deleteDrawView(draw: Draw) {
        drawViewList.removeIf { it == draw }
    }

    fun updatePathDefault(path: String){
        pathDefault = path
    }
    fun loadDrawableEmoji(context: Context, bitmap: Bitmap, isCharacter: Boolean = false, isText: Boolean = false): DrawableDraw {
        val drawable = bitmap.toDrawable(context.resources)
        val drawableEmoji = DrawableDraw(drawable, "${SimpleDateFormat("dd_MM_yyyy_hh_mm_ss").format(Date())}.png")
        drawableEmoji.isCharacter = isCharacter
        drawableEmoji.isText = isText
        return drawableEmoji
    }

    fun resetDraw() {
        drawViewList.clear()

    }

    fun saveImageFromView(context: Context, view: View): Flow<SaveState> = flow {
        emit(SaveState.Loading)
        val bitmap = BitmapHelper.createBimapFromView(view)
        MediaHelper.saveBitmapToInternalStorage(context, ValueKey.DOWNLOAD_ALBUM, bitmap).collect { state ->
            emit(state)
        }
    }.flowOn(Dispatchers.IO)
}
