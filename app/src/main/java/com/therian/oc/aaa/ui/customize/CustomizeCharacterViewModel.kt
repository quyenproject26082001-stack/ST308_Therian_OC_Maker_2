package com.therian.oc.aaa.ui.customize

import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.lifecycle.ViewModel
import com.therian.oc.aaa.R
import com.therian.oc.aaa.core.extensions.hideNavigation

import com.therian.oc.aaa.core.helper.BitmapHelper
import com.therian.oc.aaa.core.helper.InternetHelper
import com.therian.oc.aaa.core.helper.MediaHelper
import com.therian.oc.aaa.core.utils.key.AssetsKey
import com.therian.oc.aaa.core.utils.key.ValueKey
import com.therian.oc.aaa.core.utils.state.HandleState
import com.therian.oc.aaa.core.utils.state.SaveState
import com.therian.oc.aaa.data.model.custom.CustomizeModel
import com.therian.oc.aaa.data.model.custom.ItemColorImageModel
import com.therian.oc.aaa.data.model.custom.ItemColorModel
import com.therian.oc.aaa.data.model.custom.ItemNavCustomModel
import com.therian.oc.aaa.data.model.custom.LayerListModel
import com.therian.oc.aaa.data.model.custom.LayerTransformModel
import com.therian.oc.aaa.data.model.custom.NavigationModel
import com.therian.oc.aaa.data.model.custom.SuggestionModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlin.collections.get

class CustomizeCharacterViewModel : ViewModel() {
    // Đếm số lần random, chỉ số được chọn
    var countRandom = 0
    var positionSelected = 0

    // Data từ API hay không
    private val _isDataAPI = MutableStateFlow(false)

    // Trạng thái flip
    private val _isFlip = MutableStateFlow(false)
    val isFlip = _isFlip.asStateFlow()

    private val _isHideView = MutableStateFlow(false)
    val isHideView = _isHideView.asStateFlow()

    private val _isCreated = MutableStateFlow(false)
    val isCreated = _isCreated.asStateFlow()

    var statusFrom = ValueKey.CREATE

    var avatarPath = ""

    //----------------------------------------------------------------------------------------------------------------------
    var positionNavSelected = -1

    var positionCustom = -1

    // Gender filter: 1 = man, 2 = woman
    private val _selectedGender = MutableStateFlow(1)
    val selectedGender = _selectedGender.asStateFlow()

    // Nhớ nav đang chọn cho từng gender khi switch
    val genderNavPositionMap = mutableMapOf<Int, Int>() // gender -> layerIndex

    // Data gốc
    private val _dataCustomize = MutableStateFlow<CustomizeModel?>(null)
    val dataCustomize = _dataCustomize.asStateFlow()

    // Danh sách Navigation bottom
    private val _bottomNavigationList = MutableStateFlow(arrayListOf<NavigationModel>())
    val bottomNavigationList = _bottomNavigationList.asStateFlow()

    val itemNavList = ArrayList<ArrayList<ItemNavCustomModel>>()

    // Danh sách màu
    var colorItemNavList = ArrayList<ArrayList<ItemColorModel>>()

    // Trạng thái chọn item/màu
    var positionColorItemList = ArrayList<Int>()

    val isSelectedItemList = ArrayList<Boolean>()

    val isShowColorList = ArrayList<Boolean>()

    // Key + Path đã chọn
    var keySelectedItemList = ArrayList<String>()

    var pathSelectedList = ArrayList<String>()

    // Danh sách ImageView trên layout
    val imageViewList = ArrayList<ImageView>()

    val layerTransformList = ArrayList<LayerTransformModel>()

    fun initLayerTransformList(size: Int) {
        layerTransformList.clear()
        repeat(size) { layerTransformList.add(LayerTransformModel()) }
    }

    fun ensureLayerTransformList(size: Int) {
        if (layerTransformList.size == size) return
        val savedTransforms = layerTransformList.take(size).map { it.copy() }
        layerTransformList.clear()
        repeat(size) { index ->
            layerTransformList.add(savedTransforms.getOrNull(index) ?: LayerTransformModel())
        }
    }

    fun resetLayerTransform(position: Int) {
        if (position !in layerTransformList.indices) return
        layerTransformList[position] = LayerTransformModel()
    }


    val colorListMost = ArrayList<String>()

    var suggestionModel = SuggestionModel()

    //----------------------------------------------------------------------------------------------------------------------
    // Base setter
    suspend fun setPositionNavSelected(position: Int) {
        positionNavSelected = position
    }

    suspend fun setPositionCustom(position: Int) {
        positionCustom = position
    }

    fun setDataCustomize(data: CustomizeModel) {
        _dataCustomize.value = data
    }

    fun setGender(gender: Int) {
        _selectedGender.value = gender
    }

    fun setIsDataAPI(isAPI: Boolean) {
        _isDataAPI.value = isAPI
    }

    fun setIsFlip() {
        _isFlip.value = !_isFlip.value
    }

    fun setIsFlipValue(value: Boolean) {
        _isFlip.value = value
    }

    fun setIsHideView() {
        _isHideView.value = !_isHideView.value
    }

    fun setIsCreated(status: Boolean) {
        _isCreated.value = status
    }

    fun updatePositionColorItemList(positionList: ArrayList<Int>) {
        positionColorItemList.clear()
        positionColorItemList.addAll(positionList)
    }

    fun updateIsSelectedItemList(selectedList: ArrayList<Boolean>) {
        isSelectedItemList.clear()
        isSelectedItemList.addAll(selectedList)
    }

    fun updateIsShowColorList(position: Int, status: Boolean) {
        isShowColorList[position] = status
    }

    fun updateIsShowColorList(showList: ArrayList<Boolean>) {
        isShowColorList.clear()
        isShowColorList.addAll(showList)
    }

    fun updateKeySelectedItemList(keyList: ArrayList<String>) {
        keySelectedItemList.clear()
        keySelectedItemList = keyList
    }

    fun updatePathSelectedList(pathList: ArrayList<String>) {
        pathSelectedList.clear()
        pathSelectedList.addAll(pathList)
    }

    fun setColorListMost(colorList: ArrayList<String>) {
        colorListMost.clear()
        colorListMost.addAll(colorList)
    }

    fun updateSuggestionModel(model: SuggestionModel) {
        suggestionModel = model
    }

    fun updateAvatarPath(path: String) {
        avatarPath = path
    }

    //----------------------------------------------------------------------------------------------------------------------
    // Setter suspend
    suspend fun setPositionColorItem(position: Int, newPosition: Int) {
        positionColorItemList =
            positionColorItemList.mapIndexed { index, oldPosition -> if (index == position) newPosition else oldPosition }
                .toCollection(ArrayList())
    }

    suspend fun setIsSelectedItem(position: Int) {
        isSelectedItemList[position] = true
    }

    suspend fun setKeySelected(position: Int, newKey: String) {
        keySelectedItemList[position] = newKey
    }

    suspend fun setPathSelected(position: Int, newPath: String) {
        pathSelectedList[position] = newPath
    }

    //----------------------------------------------------------------------------------------------------------------------
    // Bottom Navigation
    suspend fun setBottomNavigationList(bottomNavList: ArrayList<NavigationModel>) {
        _bottomNavigationList.value = bottomNavList
    }

    suspend fun setBottomNavigationListDefault(targetLayerIndex: Int = -1) {
        val outputBottomNavigationList = arrayListOf<NavigationModel>()
        val gender = _selectedGender.value
        _dataCustomize.value!!.layerList.forEachIndexed { index, layerList ->
            if (layerList.type == gender || layerList.type == 0) {
                outputBottomNavigationList.add(
                    NavigationModel(imageNavigation = layerList.imageNavigation, layerIndex = index)
                )
            }
        }
        if (outputBottomNavigationList.isEmpty()) {
            // fallback: show all if no match
            _dataCustomize.value!!.layerList.forEachIndexed { index, layerList ->
                outputBottomNavigationList.add(
                    NavigationModel(imageNavigation = layerList.imageNavigation, layerIndex = index)
                )
            }
        }
        val selectedPosition = if (targetLayerIndex >= 0) {
            outputBottomNavigationList.indexOfFirst { it.layerIndex == targetLayerIndex }.takeIf { it >= 0 } ?: 0
        } else {
            0
        }
        outputBottomNavigationList[selectedPosition].isSelected = true
        positionNavSelected   = outputBottomNavigationList[selectedPosition].layerIndex
        _bottomNavigationList.value = outputBottomNavigationList

        // Nav đầu tiên là hình thân → không được có None
        // Chỉ recreate nếu chưa có item nào isSelected (tức là lần đầu load), giữ nguyên nếu user đã chọn rồi
        val firstLayerIndex = outputBottomNavigationList.first().layerIndex
        if (itemNavList.size > firstLayerIndex) {
            val alreadySelected = itemNavList[firstLayerIndex].any { it.isSelected }
            val firstNavItems = itemNavList[firstLayerIndex]
            val hasNone = firstNavItems.firstOrNull()?.path == com.therian.oc.aaa.core.utils.key.AssetsKey.NONE_LAYER
            val hasRandom = firstNavItems.firstOrNull()?.path == com.therian.oc.aaa.core.utils.key.AssetsKey.RANDOM_LAYER
            android.util.Log.d("NaviCheck", "=== NAVI ĐẦU GENDER $gender ===")
            android.util.Log.d("NaviCheck", "  firstLayerIndex=$firstLayerIndex, alreadySelected=$alreadySelected")
            android.util.Log.d("NaviCheck", "  layerType=${_dataCustomize.value!!.layerList[firstLayerIndex].type}, positionNavigation=${_dataCustomize.value!!.layerList[firstLayerIndex].positionNavigation}")
            android.util.Log.d("NaviCheck", "  item[0].path=${firstNavItems.getOrNull(0)?.path}")
            android.util.Log.d("NaviCheck", "  item[1].path=${firstNavItems.getOrNull(1)?.path}")
            android.util.Log.d("NaviCheck", "  hasNone=$hasNone | hasRandom=$hasRandom")
            if (hasNone) {
                // Navi đầu không được có NONE → xóa item NONE, giữ nguyên phần còn lại
                itemNavList[firstLayerIndex] = ArrayList(firstNavItems.drop(1))
                android.util.Log.d("NaviCheck", "  → FIX: Đã xóa NONE khỏi navi đầu gender $gender")
            } else {
                android.util.Log.d("NaviCheck", "  → OK: Navi đầu gender $gender không có NONE ✅")
            }
        }
    }


    suspend fun setClickBottomNavigation(position: Int) {
        _bottomNavigationList.value =
            _bottomNavigationList.value.mapIndexed { index, model -> model.copy(isSelected = index == position) }
                .toCollection(ArrayList())
    }

    //----------------------------------------------------------------------------------------------------------------------
    //  Item Nav / Layer
    suspend fun addValueToItemNavList() {
        itemNavList.clear()
        val layers = _dataCustomize.value!!.layerList
        // Tìm body layer (first nav) của từng gender
        val manBodyIndex = layers.indexOfFirst { it.type == 1 || it.type == 0 }
        val womanBodyIndex = layers.indexOfFirst { it.type == 2 || it.type == 0 }
        val bodyIndices = setOf(manBodyIndex, womanBodyIndex).filter { it >= 0 }
        layers.forEachIndexed { index, layer ->
            itemNavList.add(createListItem(layer, isBody = index in bodyIndices))
        }
    }

    suspend fun setFocusItemNavDefault() {
        itemNavList.forEachIndexed { parentIndex, itemParent ->
            itemNavList[parentIndex] = itemParent.mapIndexed { index, item ->
                item.copy(isSelected = index == 0)
            }.toCollection(ArrayList())
        }
        // Set index 1 (first image) làm selected cho body layer của từng gender
        val layers = _dataCustomize.value!!.layerList
        val manBodyIndex = layers.indexOfFirst { it.type == 1 || it.type == 0 }
        val womanBodyIndex = layers.indexOfFirst { it.type == 2 || it.type == 0 }
        setOf(manBodyIndex, womanBodyIndex).filter { it >= 0 }.forEach { layerIndex ->
            Log.d("AssetDebug", "setFocusItemNavDefault: character=${_dataCustomize.value?.dataName}, itemNavList[$layerIndex].size=${itemNavList.getOrNull(layerIndex)?.size}")
            if ((itemNavList.getOrNull(layerIndex)?.size ?: 0) < 2) {
                Log.e("AssetDebug", "⚠️ CRASH HERE: character=${_dataCustomize.value?.dataName}, body layer[$layerIndex] chỉ có ${itemNavList.getOrNull(layerIndex)?.size} item (cần ≥2)")
            }
            if (itemNavList.size > layerIndex) {
                itemNavList[layerIndex] = itemNavList[layerIndex].mapIndexed { index, item ->
                    item.copy(isSelected = index == 1)
                }.toCollection(ArrayList())
            }
        }
    }

    fun updateItemNavList(list: ArrayList<ArrayList<ItemNavCustomModel>>) {
        itemNavList.clear()
        itemNavList.addAll(list)
    }

    suspend fun setItemNavList(positionNavigation: Int, position: Int) {
        android.util.Log.d("FocusDebug", "setItemNavList: positionNavigation=$positionNavigation, markSelected=$position, listSize=${itemNavList[positionNavigation].size}")
        itemNavList[positionNavigation] =
            itemNavList[positionNavigation].mapIndexed { index, models -> models.copy(isSelected = index == position) }
                .toCollection(ArrayList())
    }

    suspend fun setClickFillLayer(item: ItemNavCustomModel, position: Int): String {
        val path = item.path
        setKeySelected(positionNavSelected, path)
        val pathSelected = if (item.listImageColor.isEmpty()) {
            path
        } else {
// Fix: Ensure color index is within bounds to prevent IndexOutOfBoundsException
            val colorIndex = positionColorItemList[positionNavSelected].coerceIn(0, item.listImageColor.size - 1)
            item.listImageColor[colorIndex].path        }
        setIsSelectedItem(positionNavSelected)
        setPathSelected(positionCustom, pathSelected)
        android.util.Log.d("FocusDebug", "setClickFillLayer: position=$position, positionNavSelected=$positionNavSelected, positionCustom=$positionCustom, path=$path, pathSelected=$pathSelected")
        setItemNavList(positionNavSelected, position)
        val afterList = itemNavList[positionNavSelected]
        afterList.forEachIndexed { i, it ->
            android.util.Log.d("FocusDebug", "  after setItemNavList[$i] isSelected=${it.isSelected} path=${it.path}")
        }
        return pathSelected
    }

    suspend fun setClickRandomLayer(): Pair<String, Boolean> {
        val isBodyLayer = positionNavSelected == _bottomNavigationList.value.firstOrNull()?.layerIndex
        val positionStartLayer = if (isBodyLayer) 1 else 2
        val randomLayer = if (isBodyLayer) {
            if (itemNavList[positionNavSelected].size <= positionStartLayer) {
                positionStartLayer.coerceAtMost(itemNavList[positionNavSelected].size - 1)
            } else {
                (positionStartLayer..<itemNavList[positionNavSelected].size).random()
            }
        } else {
            if (itemNavList[positionNavSelected].size <= positionStartLayer) {
                positionStartLayer.coerceAtMost(itemNavList[positionNavSelected].size - 1)
            } else {
                (positionStartLayer..<itemNavList[positionNavSelected].size).random()
            }
        }

        var randomColor: Int? = null

        var isMoreColors = false

        if (itemNavList[positionNavSelected][randomLayer].listImageColor.isNotEmpty()) {
            isMoreColors = true
            randomColor = (0..<(itemNavList[positionNavSelected][randomLayer].listImageColor.size)).random()
        }
        var pathRandom = itemNavList[positionNavSelected][randomLayer].path
        setKeySelected(positionNavSelected, pathRandom)

        if (!isMoreColors) {
            setPositionColorItem(positionNavSelected, 0)
        } else {
            pathRandom = itemNavList[positionNavSelected][randomLayer].listImageColor[randomColor!!].path
            setPositionColorItem(positionNavSelected, randomColor)
        }
        setPathSelected(positionCustom, pathRandom)
        setItemNavList(positionNavSelected, randomLayer)
        if (isMoreColors) {
            setColorItemNav(positionNavSelected, randomColor!!)
            updateAllItemsColor(randomColor, positionNavSelected)
        }
        return pathRandom to isMoreColors
    }

    suspend fun setClickRandomFullLayer(): Boolean {
        countRandom++
        val isOutTurn = countRandom == 300

        val colorCode = if (colorListMost.isNotEmpty()) colorListMost[(0..<colorListMost.size).random()] else "#123456"
        _bottomNavigationList.value.forEach { navModel ->
            val i = navModel.layerIndex
            val minSize = if (itemNavList[i].firstOrNull()?.path == AssetsKey.NONE_LAYER) 2 else 1
            if (itemNavList[i].size <= minSize) {
                return@forEach
            }
            val randomLayer = (minSize..<itemNavList[i].size).random()

            var randomColor: Int = 0

            val isMoreColors = if (itemNavList[i][minSize].listImageColor.isNotEmpty()) {
                randomColor = itemNavList[i][randomLayer].listImageColor.indexOfFirst { it.color == colorCode }
                if (randomColor == -1) {
                    randomColor = (0..<itemNavList[i][minSize].listImageColor.size).random()
                }
                true
            } else {
                false
            }
            keySelectedItemList[i] = itemNavList[i][randomLayer].path

            val pathItem = if (!isMoreColors) {
                positionColorItemList[i] = 0
                itemNavList[i][randomLayer].path
            } else {
                positionColorItemList[i] = randomColor
                itemNavList[i][randomLayer].listImageColor[randomColor].path
            }
            pathSelectedList[_dataCustomize.value!!.layerList[i].positionCustom] = pathItem
            setItemNavList(i, randomLayer)
            if (isMoreColors) {
                setColorItemNav(i, randomColor)
                updateAllItemsColor(randomColor, i)
            }
        }
        return isOutTurn
    }

    suspend fun setClickReset(): String {
        resetDataList()
        _dataCustomize.value!!.layerList.forEachIndexed { i, _ ->
            val positionSelected = if (itemNavList[i].firstOrNull()?.path == AssetsKey.NONE_LAYER) 0 else 1
            setItemNavList(i, positionSelected)
            setColorItemNav(i, 0)
            updateAllItemsColor(0, i)
        }
        val layers = _dataCustomize.value!!.layerList
        val manBodyIndex = layers.indexOfFirst { it.type == 1 || it.type == 0 }
        val womanBodyIndex = layers.indexOfFirst { it.type == 2 || it.type == 0 }
        setOf(manBodyIndex, womanBodyIndex).filter { it >= 0 }.distinct().forEach { bodyIdx ->
            val posCustom = layers[bodyIdx].positionCustom
            val posNav = layers[bodyIdx].positionNavigation
            val defaultPath = layers[bodyIdx].layer.first().image
            pathSelectedList[posCustom] = defaultPath
            keySelectedItemList[posNav] = defaultPath
            isSelectedItemList[posNav] = true
        }
        return layers[manBodyIndex.coerceAtLeast(0)].layer.first().image
    }


    //----------------------------------------------------------------------------------------------------------------------
// Color
    suspend fun setItemColorDefault() {
        Log.d("AssetDebug", "setItemColorDefault: character=${_dataCustomize.value?.dataName}")
        itemNavList.forEachIndexed { index, nav ->
            val isBodyStructure = nav.firstOrNull()?.path == AssetsKey.RANDOM_LAYER
            val needed = if (isBodyStructure) 2 else 3
            if (nav.size < needed) {
                Log.e("AssetDebug", "⚠️ CRASH HERE: character=${_dataCustomize.value?.dataName}, layer[$index] size=${nav.size} (cần ≥$needed) — layerFolder=${_dataCustomize.value?.layerList?.getOrNull(index)?.imageNavigation}")
            }
        }
        colorItemNavList.clear()
        for (i in 0 until _dataCustomize.value!!.layerList.size) {
            // Lấy đối tượng LayerModel đầu tiên trong danh sách con
            val currentLayer = _dataCustomize.value!!.layerList[i].layer.first()
            var firstIndex = true
            // Kiểm tra isMoreColors để thêm màu hoặc danh sách rỗng
            if (currentLayer.isMoreColors) {
                val colorList = ArrayList<ItemColorModel>()
                for (j in 0 until currentLayer.listColor.size) {
                    val color = currentLayer.listColor[j].color
                    if (firstIndex) {
                        colorList.add(ItemColorModel(color, true))
                    } else {
                        colorList.add(ItemColorModel(color))
                    }
                    firstIndex = false
                }
                colorItemNavList.add(colorList)
            } else {
                colorItemNavList.add(arrayListOf())
            }
        }
        val getAllColor = ArrayList<String>()
        itemNavList.forEachIndexed { index, nav ->
            val isBodyStructure = nav.firstOrNull()?.path == AssetsKey.RANDOM_LAYER
            val position = if (isBodyStructure) 1 else 2
            val itemNav = nav[position]
            itemNav.listImageColor.forEach { colorList ->
                getAllColor.add(colorList.color)
            }
        }
        setColorListMost(
            getAllColor.groupingBy { it }.eachCount()
                .filter { it.value > 1 }.keys.toCollection(ArrayList())
        )
    }

    fun updateColorNavList(list: ArrayList<ArrayList<ItemColorModel>>) {
        colorItemNavList.clear()
        colorItemNavList.addAll(list)
    }

    suspend fun setColorItemNav(positionNavSelected: Int, position: Int) {
        colorItemNavList[positionNavSelected] =
            colorItemNavList[positionNavSelected].mapIndexed { index, models -> models.copy(isSelected = index == position) }
                .toCollection(ArrayList())
    }

    suspend fun setClickChangeColor(position: Int): String {
        var pathColor = ""
        positionColorItemList[positionNavSelected] = position

        android.util.Log.d("ColorClick", "--- setClickChangeColor --- position: $position, positionNavSelected: $positionNavSelected, positionCustom: $positionCustom")
        android.util.Log.d("ColorClick", "keySelectedItemList[$positionNavSelected]: ${keySelectedItemList[positionNavSelected]}")

        // Đã chọn hình ảnh chưa
        if (keySelectedItemList[positionNavSelected] != "") {
            // Lấy filename từ keySelected (vd: "1.png" từ "403347/1.png" hoặc "746C78/1.png")
            val keySelected = keySelectedItemList[positionNavSelected]
            val keyFileName = keySelected.substringAfterLast('/')

            android.util.Log.d("ColorClick", "keySelected: $keySelected, keyFileName: $keyFileName")

            // Duyệt qua từng item trong bộ phận
            for ((index, item) in _dataCustomize.value!!.layerList[positionNavSelected].layer.withIndex()) {
                // So sánh theo filename thay vì full path
                val itemFileName = item.image.substringAfterLast('/')

                if (itemFileName == keyFileName) {
                    // Tìm thấy item matching
                    android.util.Log.d("ColorClick", "MATCHED at layer index: $index, item.image: ${item.image}, listColor.size: ${item.listColor.size}")
                    pathColor = item.listColor[position].path
                    pathSelectedList[positionCustom] = pathColor

                    android.util.Log.d("ColorClick", "pathColor (listColor[$position].path): $pathColor")

                    // ⭐ Update keySelected để lần sau vẫn tìm được
                    keySelectedItemList[positionNavSelected] = item.listColor[position].path
                    break
                }
            }
        }
        setColorItemNav(positionNavSelected, position)
        return pathColor
    }

    /**
     * Update màu cho TẤT CẢ items trong layer hiện tại khi user chọn màu khác
     * @param colorPosition: Vị trí màu được chọn trong rcvColor (0, 1, 2...)
     *
     * Logic:
     * - Duyệt qua tất cả items trong itemNavList[positionNavSelected]
     * - Với mỗi item có listImageColor (layer có màu):
     *   + Lấy path mới từ listImageColor[colorPosition]
     *   + Update item.path sang màu mới
     * - Skip NONE và RANDOM buttons
     * - Skip items không có màu (listImageColor rỗng)
     */
    suspend fun updateAllItemsColor(colorPosition: Int, navIndex: Int = positionNavSelected) {
        val currentNavIndex = navIndex
        val currentList = itemNavList[currentNavIndex]

        // Validate: Check list không rỗng
        if (currentList.isEmpty()) return

        // Check xem layer có items màu không
        val hasColorItems = currentList.any {
            it.listImageColor.isNotEmpty() &&
            it.path != AssetsKey.NONE_LAYER &&
            it.path != AssetsKey.RANDOM_LAYER
        }
        if (!hasColorItems) return

        // Update từng item
        val updatedList = ArrayList<ItemNavCustomModel>()
        for (item in currentList) {
            // Skip NONE và RANDOM buttons
            if (item.path == AssetsKey.NONE_LAYER ||
                item.path == AssetsKey.RANDOM_LAYER) {
                updatedList.add(item)
                continue
            }

            // Nếu item có màu
            if (item.listImageColor.isNotEmpty()) {
                // Validate bounds để tránh IndexOutOfBoundsException
                if (colorPosition < item.listImageColor.size) {
                    // Lấy path mới từ màu được chọn
                    val newPath = item.listImageColor[colorPosition].path
                    // Tạo item mới với path mới
                    updatedList.add(item.copy(path = newPath))
                } else {
                    // ColorPosition invalid, giữ nguyên
                    updatedList.add(item)
                }
            } else {
                // Item không có màu, giữ nguyên
                updatedList.add(item)
            }
        }

        // Update lại list
        itemNavList[currentNavIndex] = updatedList
    }

//----------------------------------------------------------------------------------------------------------------------
// Extension other

    suspend fun setImageViewList(frameLayout: FrameLayout) {
        frameLayout.removeAllViews()
        imageViewList.clear()
        imageViewList.addAll(addImageViewToLayout(_dataCustomize.value!!.layerList.size, frameLayout))
    }

    fun addImageViewToLayout(quantityLayer: Int, frameLayout: FrameLayout): ArrayList<ImageView> {
        val imageViewList = ArrayList<ImageView>()
        for (i in 0 until quantityLayer) {
            val imageView = ImageView(frameLayout.context).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT
                )
            }
            frameLayout.addView(imageView)
            imageViewList.add(imageView)
        }
        return imageViewList
    }

    fun createListItem(layers: LayerListModel, isBody: Boolean = false): ArrayList<ItemNavCustomModel> {
        val listItem = arrayListOf<ItemNavCustomModel>()
        val positionCustom = layers.positionCustom
        val positionNavigation = layers.positionNavigation
        if (isBody) {
            listItem.add(
                ItemNavCustomModel(
                    path = AssetsKey.RANDOM_LAYER,
                    positionCustom = positionCustom,
                    positionNavigation = positionNavigation
                )
            )
        } else {
            listItem.add(
                ItemNavCustomModel(
                    path = AssetsKey.NONE_LAYER,
                    positionCustom = positionCustom,
                    positionNavigation = positionNavigation,
                    isSelected = true
                )
            )
            listItem.add(
                ItemNavCustomModel(
                    path = AssetsKey.RANDOM_LAYER,
                    positionCustom = positionCustom,
                    positionNavigation = positionNavigation,
                )
            )
        }
        for (layer in layers.layer) {
            if (!layer.isMoreColors) {
                listItem.add(
                    ItemNavCustomModel(
                        path = layer.image,
                        positionCustom = positionCustom,
                        positionNavigation = positionNavigation,
                        thumb = layer.thumb
                    )
                )
            } else {
                val listItemColor = ArrayList<ItemColorImageModel>()

                for (colorModel in layer.listColor) {
                    listItemColor.add(
                        ItemColorImageModel(
                            color = colorModel.color, path = colorModel.path
                        )
                    )
                }
                listItem.add(
                    ItemNavCustomModel(
                        path = layer.image,
                        positionCustom = positionCustom,
                        positionNavigation = positionNavigation,
                        isSelected = false,
                        listImageColor = listItemColor,
                        thumb = layer.thumb
                    )
                )
            }
        }
        return listItem
    }

    fun saveImageFromView(context: Context, view: View): Flow<SaveState> = flow {
        emit(SaveState.Loading)
        val bitmap = BitmapHelper.createBimapFromView(view)
        MediaHelper.saveBitmapToInternalStorageZip(context, ValueKey.DOWNLOAD_ALBUM_BACKGROUND, bitmap).collect { state ->
            emit(state)
        }
    }.flowOn(Dispatchers.IO)

    fun checkDataInternet(context: Activity, action: (() -> Unit)) {
        if (!_isDataAPI.value) {
            action.invoke()
            return
        }
        InternetHelper.checkInternet(context) { result ->
            if (result == HandleState.SUCCESS) {
                action.invoke()
            } else {
                // Show No Internet dialog
                val dialog = com.therian.oc.aaa.dialog.YesNoDialog(
                    context,
                    R.string.no_internet,
                    R.string.please_check_your_internet,
                    isError = true
                )
                dialog.show()
                dialog.onYesClick = {
                    dialog.dismiss()
                }

            }
        }
    }



    suspend fun resetDataList() {
        val quantityLayer = _dataCustomize.value!!.layerList.size
        val positionColorItemList = ArrayList<Int>(quantityLayer)
        val isSelectedItemList = ArrayList<Boolean>(quantityLayer)
        val keySelectedItemList = ArrayList<String>(quantityLayer)
        val isShowColorList = ArrayList<Boolean>(quantityLayer)
        val pathSelectedList = ArrayList<String>(quantityLayer)

        repeat(quantityLayer) {
            positionColorItemList.add(0)
            isSelectedItemList.add(false)
            keySelectedItemList.add("")
            isShowColorList.add(true)
            pathSelectedList.add("")
        }

        updatePositionColorItemList(positionColorItemList)
        updateIsSelectedItemList(isSelectedItemList)
        updateKeySelectedItemList(keySelectedItemList)
        updateIsShowColorList(isShowColorList)
        updatePathSelectedList(pathSelectedList)
    }

    fun getSuggestionList(): SuggestionModel {
        return SuggestionModel(
            avatarPath = avatarPath,
            positionColorItemList = ArrayList(positionColorItemList),
            itemNavList = ArrayList(itemNavList),
            colorItemNavList = ArrayList(colorItemNavList),
            isSelectedItemList = ArrayList(isSelectedItemList),
            keySelectedItemList = ArrayList(keySelectedItemList),
            isShowColorList = ArrayList(isShowColorList),
            pathSelectedList = ArrayList(pathSelectedList),
            isFlip = _isFlip.value,
            layerTransformList = ArrayList(layerTransformList.map { it.copy() }),
        )
    }

    fun fillSuggestionToCustomize() {
        updatePositionColorItemList(suggestionModel.positionColorItemList)
        updateItemNavList(suggestionModel.itemNavList)
        updateColorNavList(suggestionModel.colorItemNavList)
        updateIsSelectedItemList(suggestionModel.isSelectedItemList)
        updateKeySelectedItemList(suggestionModel.keySelectedItemList)
        updateIsShowColorList(suggestionModel.isShowColorList)
        updatePathSelectedList(suggestionModel.pathSelectedList)
        setIsFlipValue(suggestionModel.isFlip)
        layerTransformList.clear()
        layerTransformList.addAll(suggestionModel.layerTransformList.map { it.copy() })
    }

    suspend fun updateEditCharacter(context: Context, pathInternal: String) {
        val editList = loadEditList(context)
        val indexEdit = editList.indexOfFirst { it.pathInternalEdit == suggestionModel.pathInternalEdit }
        if (indexEdit != -1) {
            editList[indexEdit].apply {
                avatarPath = this@CustomizeCharacterViewModel.avatarPath
                positionColorItemList = ArrayList(this@CustomizeCharacterViewModel.positionColorItemList)
                itemNavList = ArrayList(this@CustomizeCharacterViewModel.itemNavList.map { ArrayList(it) })
                colorItemNavList = ArrayList(this@CustomizeCharacterViewModel.colorItemNavList.map { ArrayList(it) })
                isSelectedItemList = ArrayList(this@CustomizeCharacterViewModel.isSelectedItemList)
                keySelectedItemList = ArrayList(this@CustomizeCharacterViewModel.keySelectedItemList)
                isShowColorList = ArrayList(this@CustomizeCharacterViewModel.isShowColorList)
                pathSelectedList = ArrayList(this@CustomizeCharacterViewModel.pathSelectedList)
                pathInternalEdit = pathInternal
                isFlip = this@CustomizeCharacterViewModel.isFlip.value
                layerTransformList = ArrayList(
                    this@CustomizeCharacterViewModel.layerTransformList.map { it.copy() }
                )
            }
            MediaHelper.writeListToFile(context, ValueKey.EDIT_FILE_INTERNAL, editList)

            // ⭐ CRITICAL FIX: Update suggestionModel.pathInternalEdit so next save can find the entry
            suggestionModel.pathInternalEdit = pathInternal
        }
    }

    fun addCharacterToEditList(context: Context, pathInternal: String) {
        val editList = loadEditList(context)
        val newEditModel = SuggestionModel(
            avatarPath = avatarPath,
            positionColorItemList = this@CustomizeCharacterViewModel.positionColorItemList,
            itemNavList = this@CustomizeCharacterViewModel.itemNavList,
            colorItemNavList = this@CustomizeCharacterViewModel.colorItemNavList,
            isSelectedItemList = this@CustomizeCharacterViewModel.isSelectedItemList,
            keySelectedItemList = this@CustomizeCharacterViewModel.keySelectedItemList,
            isShowColorList = this@CustomizeCharacterViewModel.isShowColorList,
            pathSelectedList = this@CustomizeCharacterViewModel.pathSelectedList,
            pathInternalEdit = pathInternal,
            isFlip = isFlip.value,
            layerTransformList = ArrayList(layerTransformList.map { it.copy() }),
        )
        editList.add(0, newEditModel)
        MediaHelper.writeListToFile(context, ValueKey.EDIT_FILE_INTERNAL, editList)
    }

    private fun loadEditList(context: Context): ArrayList<SuggestionModel> {
        return try {
            MediaHelper.readListFromFile<SuggestionModel>(context, ValueKey.EDIT_FILE_INTERNAL)
                .toCollection(ArrayList())
        } catch (e: Exception) {
            Log.e("nbhieu", "updateEditCharacter: $e")
            arrayListOf()
        }
    }
//----------------------------------------------------------------------------------------------------------------------

}
