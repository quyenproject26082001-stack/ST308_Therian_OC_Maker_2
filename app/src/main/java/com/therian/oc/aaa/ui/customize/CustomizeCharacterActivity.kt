package com.therian.oc.aaa.ui.customize

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.lvt.ads.util.Admob
import com.therian.oc.aaa.R
import com.therian.oc.aaa.core.base.BaseActivity
import com.therian.oc.aaa.core.extensions.checkInternet
import com.therian.oc.aaa.core.extensions.dLog
import com.therian.oc.aaa.core.extensions.eLog
import com.therian.oc.aaa.core.extensions.hideNavigation
import com.therian.oc.aaa.core.extensions.invisible
import com.therian.oc.aaa.core.extensions.loadNativeCollabAds
import com.therian.oc.aaa.core.extensions.logEvent
import com.therian.oc.aaa.core.extensions.select
import com.therian.oc.aaa.core.extensions.setImageActionBar
import com.therian.oc.aaa.core.extensions.showInterAll
import com.therian.oc.aaa.core.extensions.startIntentLeftToRight
import com.therian.oc.aaa.core.extensions.startIntentRightToLeft
import com.therian.oc.aaa.core.extensions.visible
import com.therian.oc.aaa.core.helper.LanguageHelper
import com.therian.oc.aaa.core.utils.key.IntentKey
import com.therian.oc.aaa.core.utils.key.ValueKey
import com.therian.oc.aaa.core.utils.state.SaveState
import com.therian.oc.aaa.data.model.custom.ItemNavCustomModel
import com.therian.oc.aaa.databinding.ActivityCustomizeBinding
import com.therian.oc.aaa.dialog.YesNoDialog
import com.therian.oc.aaa.ui.home.DataViewModel
import com.therian.oc.aaa.core.extensions.tap
import com.therian.oc.aaa.core.extensions.tapAndHold
import com.therian.oc.aaa.core.helper.MediaHelper
import com.therian.oc.aaa.data.model.custom.SuggestionModel
import com.therian.oc.aaa.ui.add_character.AddCharacterActivity
import com.therian.oc.aaa.ui.my_creation.MyCreationActivity
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.collections.get
import kotlin.jvm.java
import kotlin.text.get

class CustomizeCharacterActivity : BaseActivity<ActivityCustomizeBinding>() {
    companion object {
        private const val MIN_SCALE = 0.5f
        private const val MAX_SCALE = 1.5f
        private const val SCALE_STEP = 0.05f
        private const val MOVE_STEP = 5f
        private const val ROTATE_STEP = 15f
    }

    private val viewModel: CustomizeCharacterViewModel by viewModels()
    private var characterLoaded = false
    private var lastClickedLayerPosition: Int =
        -1 // Track last clicked layer position for scrolling
    private val dataViewModel: DataViewModel by viewModels()
    val colorLayerCustomizeAdapter by lazy { ColorLayerCustomizeAdapter(this) }
    val layerCustomizeAdapter by lazy { LayerCustomizeAdapter(this) }
    val bottomNavigationCustomizeAdapter by lazy { BottomNavigationCustomizeAdapter(this) }
    val hideList: ArrayList<View> by lazy {
        arrayListOf(
            binding.btnRandom,
            binding.color,
            binding.rcvLayer,
            binding.flBottomNav
        )
    }

    override fun setViewBinding(): ActivityCustomizeBinding {
        return ActivityCustomizeBinding.inflate(LayoutInflater.from(this))
    }

    override fun initView() {
        initRcv()
        lifecycleScope.launch { showLoading() }
        dataViewModel.ensureData(this)


    }

    override fun dataObservable() {
        lifecycleScope.launch {
            launch {
                dataViewModel.allData.collect { list ->
                    if (list.isNotEmpty() && !characterLoaded) {
                        characterLoaded = true
                        viewModel.positionSelected = intent.getIntExtra(IntentKey.INTENT_KEY, 0)
                        viewModel.statusFrom =
                            intent.getIntExtra(IntentKey.STATUS_FROM_KEY, ValueKey.CREATE)
                        val safePosition = viewModel.positionSelected.coerceIn(0, list.size - 1)
                        val character = dataViewModel.getCharacter(
                            this@CustomizeCharacterActivity,
                            safePosition
                        )
                        if (character == null) {
                            dismissLoading()
                            showToast(R.string.an_error_occurred)
                            return@collect
                        }
                        viewModel.setDataCustomize(character)
                        viewModel.setIsDataAPI(character.isFromAPI)
                        // Detect default gender từ layer đầu tiên của data
                        val defaultGender =
                            if (character.layerList.firstOrNull()?.type == 2) 2 else 1
                        viewModel.setGender(defaultGender)
                        binding.btnMan.isSelected = defaultGender == 1
                        binding.btnWooman.isSelected = defaultGender == 2
                        initData()
                    }
                }
            }
            launch {
                viewModel.isFlip.collect { status ->
                    val rotation = if (status) -180f else 0f
                    viewModel.imageViewList.forEachIndexed { index, view ->
                        view.rotationY = rotation
                    }
                }
            }
            launch {
                viewModel.isHideView.collect { status ->
                    if (viewModel.isCreated.value) {
                        val res = if (status) {
                            hideList.forEach { it.invisible() }
                            R.drawable.ic_hide
                        } else {
                            hideList.forEach { it.visible() }
                            checkStatusColor()
                            R.drawable.ic_show
                        }
                        binding.btnHide.setImageResource(res)
                    }
                }
            }
            launch {
                viewModel.bottomNavigationList.collect { bottomNavigationList ->
                    if (bottomNavigationList.isNotEmpty()) {
                        val selectedNavIndex = bottomNavigationList.indexOfFirst { it.isSelected }
                        val navLayerIndex = if (selectedNavIndex >= 0) bottomNavigationList[selectedNavIndex].layerIndex
                        else viewModel.positionNavSelected
                        bottomNavigationCustomizeAdapter.submitList(bottomNavigationList)
                        if (selectedNavIndex >= 0) {
                            binding.rcvNavigation.post {
                                binding.rcvNavigation.scrollToPosition(selectedNavIndex)
                            }
                        }
                        layerCustomizeAdapter.submitList(viewModel.itemNavList[navLayerIndex])
                        colorLayerCustomizeAdapter.submitList(viewModel.colorItemNavList[navLayerIndex])
                        if (viewModel.colorItemNavList[navLayerIndex].size > 1) {
                            binding.rcvColor.smoothScrollToPosition(viewModel.colorItemNavList[navLayerIndex].indexOfFirst { it.isSelected })
                        }
                    }
                }
            }
        }
    }

    override fun viewListener() {
        binding.apply {
            actionBar.apply {
                btnActionBarLeft.tap { confirmExit() }
                btnActionBarCenterRight.tap {   handleReset() }
                //btnActionBarCenterRightRight.tap { viewModel.setIsFlip() }
                actionBar.btnActionBarRightText.tap(10) {
                   checkInternet {   handleSave()}
                }
                btnActionBarCenterLeft.tap { viewModel.setIsFlip() }
            }
            btnRandom.tap { viewModel.checkDataInternet(this@CustomizeCharacterActivity) {  showInterAll {  handleRandomAllLayer() } }}
            btnColor.tap { viewModel.checkDataInternet(this@CustomizeCharacterActivity) { handleStatusColor() } }
            btnHide.tap { viewModel.checkDataInternet(this@CustomizeCharacterActivity) { viewModel.setIsHideView() } }
            btnMan.tap { handleGenderSwitch(1) }
            btnWooman.tap { handleGenderSwitch(2) }

            btnMove.tap(1000) {
                if (layoutMove.isVisible) {
                    closeMovePanel()
                } else if (btnMove.isEnabled) {
                    layoutMove.visible()
                    rcvLayer.invisible()
                    flBottomNav.invisible()
                    btnMove.setImageResource(R.drawable.ic_move_slt)
                    refreshMoveControls()
                }
            }

            btnMoveLeft.tapAndHold { moveSelectedLayer(deltaX = -MOVE_STEP) }
            btnMoveRight.tapAndHold { moveSelectedLayer(deltaX = MOVE_STEP) }
            btnMoveUp.tapAndHold { moveSelectedLayer(deltaY = -MOVE_STEP) }
            btnMoveDown.tapAndHold { moveSelectedLayer(deltaY = MOVE_STEP) }
            rotateToLeft.tapAndHold { rotateSelectedLayer(-ROTATE_STEP) }
            rotateToRight.tapAndHold { rotateSelectedLayer(ROTATE_STEP) }
            scaleUp.tapAndHold { scaleSelectedLayer(SCALE_STEP) }
            scaleDown.tapAndHold { scaleSelectedLayer(-SCALE_STEP) }
            btnResetMove.tap { resetSelectedLayerTransform() }
        }
        handleRcv()
    }

    override fun initActionBar() {
        binding.actionBar.apply {
            btnActionBarLeft.visible()
            btnActionBarRightText.visible()
            tvRightText.visible()
            tvRightText.setText(R.string.next)
            btnActionBarCenterRight.visible()
            btnActionBarCenterLeft.visible()
        }

    }

    private fun isTransformChanged(): Boolean {
        val transform = viewModel.layerTransformList.getOrNull(viewModel.positionCustom)
            ?: return false
        return transform.translationX != 0f ||
                transform.translationY != 0f ||
                transform.rotation != 0f || transform.scaleX !=
                1f || transform.scaleY != 1f
    }


    private fun updateResetBtn() {
        val changed = isTransformChanged()
        binding.btnResetMove.alpha = if (changed) 1f else 0.4f
        binding.btnResetMove.isEnabled = changed
    }

    private fun closeMovePanel() {
        binding.layoutMove.invisible()
        binding.rcvLayer.visible()
        binding.flBottomNav.visible()
        updateMoveButtonState()
    }

    private fun selectedLayerView() =
        viewModel.imageViewList.getOrNull(viewModel.positionCustom)

    private fun selectedLayerTransform() =
        viewModel.layerTransformList.getOrNull(viewModel.positionCustom)

    private fun moveSelectedLayer(deltaX: Float = 0f, deltaY: Float = 0f) {
        val imageView = selectedLayerView() ?: return
        val transform = selectedLayerTransform() ?: return
        val maxX = binding.layoutCustomLayer.width / 2f
        val maxY = binding.layoutCustomLayer.height / 2f
        imageView.translationX = (imageView.translationX + deltaX).coerceIn(-maxX, maxX)
        imageView.translationY = (imageView.translationY + deltaY).coerceIn(-maxY, maxY)
        transform.translationX = imageView.translationX
        transform.translationY = imageView.translationY
        refreshMoveControls()
    }

    private fun rotateSelectedLayer(delta: Float) {
        val imageView = selectedLayerView() ?: return
        val transform = selectedLayerTransform() ?: return
        imageView.rotation += delta
        transform.rotation = imageView.rotation
        updateResetBtn()
    }

    private fun scaleSelectedLayer(delta: Float) {
        val imageView = selectedLayerView() ?: return
        val transform = selectedLayerTransform() ?: return
        val scale = (imageView.scaleX + delta).coerceIn(MIN_SCALE, MAX_SCALE)
        imageView.scaleX = scale
        imageView.scaleY = scale
        transform.scaleX = scale
        transform.scaleY = scale
        refreshMoveControls()
    }

    private fun resetSelectedLayerTransform() {
        val imageView = selectedLayerView() ?: return
        imageView.translationX = 0f
        imageView.translationY = 0f
        imageView.rotation = 0f
        imageView.scaleX = 1f
        imageView.scaleY = 1f
        viewModel.resetLayerTransform(viewModel.positionCustom)
        refreshMoveControls()
    }

    private fun applySelectedLayerTransform() {
        val imageView = selectedLayerView() ?: return
        val transform = selectedLayerTransform() ?: return
        imageView.translationX = transform.translationX
        imageView.translationY = transform.translationY
        imageView.rotation = transform.rotation
        imageView.scaleX = transform.scaleX
        imageView.scaleY = transform.scaleY
    }

    private fun refreshMoveControls() {
        val imageView = selectedLayerView() ?: return
        val maxX = binding.layoutCustomLayer.width / 2f
        val maxY = binding.layoutCustomLayer.height / 2f
        setControlEnabled(binding.btnMoveLeft, imageView.translationX > -maxX)
        setControlEnabled(binding.btnMoveRight, imageView.translationX < maxX)
        setControlEnabled(binding.btnMoveUp, imageView.translationY > -maxY)
        setControlEnabled(binding.btnMoveDown, imageView.translationY < maxY)
        setControlEnabled(binding.scaleUp, imageView.scaleX < MAX_SCALE)
        setControlEnabled(binding.scaleDown, imageView.scaleX > MIN_SCALE)
        updateResetBtn()
    }

    private fun setControlEnabled(view: View, enabled: Boolean) {
        view.isEnabled = enabled
        view.alpha = if (enabled) 1f else 0.4f
    }

    private fun initRcv() {
        binding.apply {
            rcvLayer.apply {
                adapter = layerCustomizeAdapter
                itemAnimator = null
            }

            rcvColor.apply {
                adapter = colorLayerCustomizeAdapter
                itemAnimator = null
            }

            rcvNavigation.apply {
                adapter = bottomNavigationCustomizeAdapter
                itemAnimator = null
            }
        }
    }

    private fun handleRcv() {
        layerCustomizeAdapter.onItemClick =
            { item, position ->
                viewModel.checkDataInternet(this) {
                    handleFillLayer(
                        item,
                        position
                    )
                }
            }

        layerCustomizeAdapter.onNoneClick =
            { position -> viewModel.checkDataInternet(this) { handleNoneLayer(position) } }

        layerCustomizeAdapter.onRandomClick =
            { viewModel.checkDataInternet(this) { handleRandomLayer() } }

        colorLayerCustomizeAdapter.onItemClick =
            { position -> viewModel.checkDataInternet(this) { handleChangeColorLayer(position) } }

        bottomNavigationCustomizeAdapter.onItemClick =
            { positionBottomNavigation ->
                viewModel.checkDataInternet(this) {
                    handleClickBottomNavigation(
                        positionBottomNavigation
                    )
                }
            }
    }

    private fun initData() {
        val handleExceptionCoroutine = CoroutineExceptionHandler { _, throwable ->
            eLog("initData: ${throwable.message}")
            CoroutineScope(Dispatchers.Main).launch {
                dismissLoading()
                hideNavigation()

                val dialogExit =
                    YesNoDialog(
                        this@CustomizeCharacterActivity,
                        R.string.error,
                        R.string.an_error_occurred
                    )
                dialogExit.show()
                dialogExit.onNoClick = {
                    dialogExit.dismiss()
                    finish()
                }
                dialogExit.onYesClick = {
                    dialogExit.dismiss()
                    hideNavigation()

                    startIntentRightToLeft(
                        CustomizeCharacterActivity::class.java,
                        viewModel.positionSelected
                    )
                    finish()
                }
            }
        }

        CoroutineScope(SupervisorJob() + Dispatchers.IO + handleExceptionCoroutine).launch {
            var pathImageDefault = ""
            // Get data from list
            val deferred1 = async {
                viewModel.updateAvatarPath(viewModel.dataCustomize.value!!.avatar)
                when (viewModel.statusFrom) {
                    ValueKey.CREATE -> {
                        viewModel.resetDataList()
                        viewModel.addValueToItemNavList()
                        viewModel.setItemColorDefault()
                        viewModel.setFocusItemNavDefault()
                    }

                    // Edit
                    else -> {
                        viewModel.updateSuggestionModel(
                            MediaHelper.readModelFromFile<SuggestionModel>(
                                this@CustomizeCharacterActivity,
                                ValueKey.SUGGESTION_FILE_INTERNAL
                            )!!
                        )
                        viewModel.fillSuggestionToCustomize()
                    }
                }

                viewModel.setPositionCustom(viewModel.dataCustomize.value!!.layerList.first().positionCustom)
                viewModel.setPositionNavSelected(viewModel.dataCustomize.value!!.layerList.first().positionNavigation)
                viewModel.setBottomNavigationListDefault()
                dLog("deferred1")
                return@async true
            }
            // Add custom view in FrameLayout
            val deferred2 = async(Dispatchers.Main) {
                if (deferred1.await()) {
                    viewModel.setImageViewList(binding.layoutCustomLayer)
                    viewModel.ensureLayerTransformList(viewModel.imageViewList.size)
                    dLog("deferred2")
                }
                return@async true
            }

            // Fill data default
            val deferred3 = async {
                if (deferred1.await() && deferred2.await()) {
                    if (viewModel.statusFrom == ValueKey.CREATE) {
                        pathImageDefault =
                            viewModel.dataCustomize.value!!.layerList.first().layer.first().image
                        viewModel.setIsSelectedItem(viewModel.positionCustom)
                        viewModel.setPathSelected(viewModel.positionCustom, pathImageDefault)
                        viewModel.setKeySelected(viewModel.positionNavSelected, pathImageDefault)

                        // Load body default cho tất cả gender body layers
                        val layers = viewModel.dataCustomize.value!!.layerList
                        val manBodyIndex = layers.indexOfFirst { it.type == 1 || it.type == 0 }
                        val womanBodyIndex = layers.indexOfFirst { it.type == 2 || it.type == 0 }
                        setOf(manBodyIndex, womanBodyIndex)
                            .filter { it >= 0 }
                            .distinct()
                            .forEach { bodyIdx ->
                                val posCustom = layers[bodyIdx].positionCustom
                                val posNav = layers[bodyIdx].positionNavigation
                                if (viewModel.pathSelectedList.getOrNull(posCustom).isNullOrEmpty()) {
                                    val defaultPath = layers[bodyIdx].layer.first().image
                                    viewModel.setIsSelectedItem(posCustom)
                                    viewModel.setPathSelected(posCustom, defaultPath)
                                    viewModel.setKeySelected(posNav, defaultPath)
                                }
                            }
                    }
                    dLog("deferred3")
                }
                return@async true
            }

            withContext(Dispatchers.Main) {
                if (deferred1.await() && deferred2.await() && deferred3.await()) {
                    when (viewModel.statusFrom) {
                        ValueKey.CREATE -> {
                            // Load tất cả body layers đã được set path
                            val layers = viewModel.dataCustomize.value!!.layerList
                            val manBodyIndex = layers.indexOfFirst { it.type == 1 || it.type == 0 }
                            val womanBodyIndex = layers.indexOfFirst { it.type == 2 || it.type == 0 }
                            setOf(manBodyIndex, womanBodyIndex).filter { it >= 0 }.distinct().forEach { bodyIdx ->
                                val posCustom = layers[bodyIdx].positionCustom
                                val path = viewModel.pathSelectedList.getOrNull(posCustom) ?: ""
                                if (path.isNotEmpty()) {
                                    Glide.with(this@CustomizeCharacterActivity).load(path)
                                        .into(viewModel.imageViewList[posCustom])
                                }
                            }
                        }

                        // Edit
                        else -> {
                            viewModel.pathSelectedList.forEachIndexed { index, path ->
                                if (path != "") {
                                    Glide.with(this@CustomizeCharacterActivity).load(path)
                                        .into(viewModel.imageViewList[index])
                                }
                            }
                            viewModel.layerTransformList.forEachIndexed { index, transform ->
                                viewModel.imageViewList.getOrNull(index)?.apply {
                                    translationX = transform.translationX
                                    translationY = transform.translationY
                                    rotation = transform.rotation
                                    scaleX = transform.scaleX
                                    scaleY = transform.scaleY
                                }
                            }
                        }
                    }

                    layerCustomizeAdapter.submitList(viewModel.itemNavList[viewModel.positionNavSelected])
                    colorLayerCustomizeAdapter.submitList(viewModel.colorItemNavList[viewModel.positionNavSelected])
                    checkStatusColor()
                    // Apply flip state after imageViewList is populated (collector fires too early when list is still empty)
                    val flipRotation = if (viewModel.isFlip.value) -180f else 0f
                    viewModel.imageViewList.forEach { it.rotationY = flipRotation }
                    viewModel.setIsCreated(true)
                    refreshMoveControls()
                    updateMoveButtonState()
                    dismissLoading()
                    delay(300)
                    dismissLoading()
                    hideNavigation()
                    dLog("main")
                }
            }
        }
    }

    private fun checkStatusColor() {
        if (viewModel.colorItemNavList[viewModel.positionNavSelected].size > 1) {
            binding.color.visible()
             binding.btnColor.visible()
            val (res, status) = if (viewModel.isShowColorList[viewModel.positionNavSelected]) {
                R.drawable.ic_color to true
            } else {
                R.drawable.ic_color to false
            }
            binding.btnColor.setImageResource(res)
            if (status) binding.flColor.visible() else binding.flColor.invisible()
        } else {
            binding.color.invisible()
            binding.btnColor.invisible()
            binding.flColor.invisible()
        }
    }

    private fun handleStatusColor(isClose: Boolean = false) {
        if (isClose) {
            binding.flColor.invisible()
            viewModel.updateIsShowColorList(viewModel.positionNavSelected, false)
        } else {
            if (viewModel.isShowColorList[viewModel.positionNavSelected]) {
                binding.flColor.invisible()
            } else {
                binding.flColor.visible()
            }
            viewModel.updateIsShowColorList(
                viewModel.positionNavSelected,
                !viewModel.isShowColorList[viewModel.positionNavSelected]
            )
        }
        checkStatusColor()
    }

    private fun handleFillLayer(item: ItemNavCustomModel, position: Int) {
        lastClickedLayerPosition = position // Save clicked position for scrolling
        android.util.Log.d("CustomizeScroll", "Layer clicked at position: $position")

        lifecycleScope.launch(Dispatchers.IO) {
            val pathSelected = viewModel.setClickFillLayer(item, position)
            withContext(Dispatchers.Main) {
                Glide.with(this@CustomizeCharacterActivity).load(pathSelected)
                    .into(viewModel.imageViewList[viewModel.positionCustom])
                updateMoveButtonState()
                val listToSubmit = viewModel.itemNavList[viewModel.positionNavSelected]
                android.util.Log.d("FocusDebug", "handleFillLayer submitList: positionNavSelected=${viewModel.positionNavSelected}, listSize=${listToSubmit.size}")
                listToSubmit.forEachIndexed { i, it ->
                    android.util.Log.d("FocusDebug", "  submit[$i] isSelected=${it.isSelected} path=${it.path}")
                }
                layerCustomizeAdapter.submitList(listToSubmit)
            }
        }
    }

    private fun handleNoneLayer(position: Int) {
        lastClickedLayerPosition = position // Save clicked position for scrolling
        android.util.Log.d("CustomizeScroll", "None layer clicked at position: $position")

        lifecycleScope.launch(Dispatchers.IO) {
            viewModel.setIsSelectedItem(viewModel.positionCustom)
            viewModel.setPathSelected(viewModel.positionCustom, "")
            viewModel.setKeySelected(viewModel.positionNavSelected, "")
            viewModel.setItemNavList(viewModel.positionNavSelected, position)
            withContext(Dispatchers.Main) {
                Glide.with(this@CustomizeCharacterActivity)
                    .clear(viewModel.imageViewList[viewModel.positionCustom])
                layerCustomizeAdapter.submitList(viewModel.itemNavList[viewModel.positionNavSelected])
                if (binding.layoutMove.isVisible) closeMovePanel()
                updateMoveButtonState()
            }
        }
    }

    private fun updateMoveButtonState() {
        val isNone = viewModel.keySelectedItemList
            .getOrNull(viewModel.positionNavSelected)
            .isNullOrEmpty()
        binding.btnMove.apply {
            alpha = if (isNone) 0.4f else 1f
            if (binding.layoutMove.isVisible) {
                isEnabled = true

                setImageResource(R.drawable.ic_move_slt)
            } else {
                isEnabled = !isNone
                setImageResource(if (isNone)
                    R.drawable.ic_move_uslt else
                    R.drawable.ic_move)
            }
        }
    }

    private fun handleRandomLayer() {
        lifecycleScope.launch(Dispatchers.IO) {
            val (pathRandom, isMoreColors) = viewModel.setClickRandomLayer()
            withContext(Dispatchers.Main) {
                Glide.with(this@CustomizeCharacterActivity).load(pathRandom)
                    .into(viewModel.imageViewList[viewModel.positionCustom])
                layerCustomizeAdapter.submitList(viewModel.itemNavList[viewModel.positionNavSelected])
                updateMoveButtonState()
                if (isMoreColors) {
                    colorLayerCustomizeAdapter.submitList(viewModel.colorItemNavList[viewModel.positionNavSelected])
                    binding.rcvColor.smoothScrollToPosition(viewModel.colorItemNavList[viewModel.positionNavSelected].indexOfFirst { it.isSelected })
                }
            }
        }
    }

    private fun handleChangeColorLayer(position: Int) {
        android.util.Log.d("ColorClick", "=== handleChangeColorLayer === position from adapter: $position")
        lifecycleScope.launch(Dispatchers.IO) {
            // 1. Lấy path màu mới cho item đang được chọn
            val pathColor = viewModel.setClickChangeColor(position)
            android.util.Log.d("ColorClick", "pathColor result: $pathColor")

            // 2. ⭐ Update màu cho TẤT CẢ items trong rcvLayer
            viewModel.updateAllItemsColor(position)

            withContext(Dispatchers.Main) {
                // 3. Update ảnh trong canvas chính (layoutCustomLayer)
                if (pathColor != "") {
                    Glide.with(this@CustomizeCharacterActivity)
                        .load(pathColor)
                        .into(viewModel.imageViewList[viewModel.positionCustom])
                }

                // 4. Update highlight trong rcvColor
                colorLayerCustomizeAdapter.submitList(viewModel.colorItemNavList[viewModel.positionNavSelected])

                // 5. ⭐ Refresh rcvLayer với data mới (tất cả items đã đổi màu)
                // Sử dụng .toList() để tạo list mới, giúp DiffUtil detect changes
                val newList = viewModel.itemNavList[viewModel.positionNavSelected].toList()
                android.util.Log.d(
                    "CustomizeScroll",
                    "submitList called - list size: ${newList.size}"
                )

                layerCustomizeAdapter.submitList(newList) {
                    android.util.Log.d("CustomizeScroll", "submitList callback - list committed")

                    // 6. ⭐ Scroll rcvLayer to center the last clicked layer position IMMEDIATELY
                    if (lastClickedLayerPosition >= 0) {
                        val layoutManager = binding.rcvLayer.layoutManager

                        if (layoutManager is androidx.recyclerview.widget.GridLayoutManager) {
                            val spanCount = layoutManager.spanCount

                            // Calculate row position
                            val rowPosition = (lastClickedLayerPosition / spanCount) * spanCount

                            // Calculate offset to center the row on screen
                            val recyclerHeight = binding.rcvLayer.height

                            // Use estimated item height if view not yet laid out
                            val itemView =
                                layoutManager.findViewByPosition(lastClickedLayerPosition)
                            val itemHeight =
                                itemView?.height ?: (recyclerHeight / 5) // Estimate ~1/5 of screen

                            // Center the item vertically: (recyclerHeight / 2) - (itemHeight / 2)
                            val centerOffset = (recyclerHeight / 2) - (itemHeight / 2)

                            android.util.Log.d(
                                "CustomizeScroll",
                                "INSTANT scroll to position $lastClickedLayerPosition - row: $rowPosition, offset: $centerOffset"
                            )

                            // Immediate scroll without animation
                            layoutManager.scrollToPositionWithOffset(rowPosition, centerOffset)
                        } else {
                            // Fallback - but shouldn't happen
                            binding.rcvLayer.scrollToPosition(lastClickedLayerPosition)
                        }
                    }
                }
            }
        }
    }

    private fun handleClickBottomNavigation(positionBottomNavigation: Int) {
        val layerIndex = viewModel.bottomNavigationList.value.getOrNull(positionBottomNavigation)?.layerIndex
            ?: positionBottomNavigation
        if (layerIndex == viewModel.positionNavSelected) return
        if (binding.layoutMove.isVisible) closeMovePanel()
        lifecycleScope.launch(Dispatchers.IO) {
            viewModel.setPositionNavSelected(layerIndex)
            viewModel.setPositionCustom(viewModel.dataCustomize.value!!.layerList[layerIndex].positionCustom)
            viewModel.setClickBottomNavigation(positionBottomNavigation)
            withContext(Dispatchers.Main) {
                applySelectedLayerTransform()
                // Scroll color list to selected item when tab changes
                if (viewModel.colorItemNavList[viewModel.positionNavSelected].size > 1) {
                    binding.rcvColor.smoothScrollToPosition(
                        viewModel.colorItemNavList[viewModel.positionNavSelected].indexOfFirst { it.isSelected }
                    )
                }
                checkStatusColor()
                refreshMoveControls()
                updateMoveButtonState()
            }
        }
    }

    private fun confirmExit() {
        val dialog =
            YesNoDialog(this, R.string.exit, R.string.do_you_want_to_exit)
        LanguageHelper.setLocale(this)
        dialog.show()
        dialog.onYesClick = {
            dialog.dismiss()
            showInterAll { finish() }
        }
        dialog.onNoClick = {
            dialog.dismiss()
            hideNavigation()
        }
    }

    private fun handleSave() {
        lifecycleScope.launch(Dispatchers.IO) {
            viewModel.saveImageFromView(this@CustomizeCharacterActivity, binding.layoutCustomLayer)
                .collect { result ->
                    when (result) {
                        is SaveState.Loading -> showLoading()

                        is SaveState.Error -> {
                            dismissLoading()
                            withContext(Dispatchers.Main) {
                                showToast(R.string.save_failed_please_try_again)
                            }
                        }

                        is SaveState.Success -> {
                            dismissLoading()
                            when (viewModel.statusFrom) {
                                ValueKey.EDIT -> {
                                    // Chạy ngầm trong background, không blocking UI
                                    lifecycleScope.launch(Dispatchers.IO) {
                                        viewModel.updateEditCharacter(
                                            this@CustomizeCharacterActivity,
                                            result.path
                                        )
                                    }
                                    withContext(Dispatchers.Main) {
                                        logEvent("click_item_${viewModel.positionSelected}_edit")
                                        // ✅ 1) GỬI RESULT VỀ ViewActivity NGAY (nhưng chưa finish)
                                        val data = android.content.Intent().apply {
                                            putExtra("NEW_PATH", result.path)
                                        }
                                        setResult(RESULT_OK, data)

                                        // ✅ 2) VẪN SANG AddCharacterActivity như bạn muốn
                                        showInterAll {
                                            startIntentRightToLeft(AddCharacterActivity::class.java, result.path)
                                        }
                                    }
                                }

                                else -> {
                                    // Chạy ngầm trong background, không blocking UI
                                    lifecycleScope.launch(Dispatchers.IO) {
                                        viewModel.addCharacterToEditList(
                                            this@CustomizeCharacterActivity,
                                            result.path
                                        )
                                    }
                                    withContext(Dispatchers.Main) {
                                        logEvent("click_item_${viewModel.positionSelected}_done")
                                        showInterAll {
                                            startIntentRightToLeft(
                                                AddCharacterActivity::class.java,
                                                result.path
                                            )
                                        }
                                    }
                                }
                            }

                        }
                    }
                }
        }
    }

    private fun handleGenderSwitch(gender: Int) {
        if (viewModel.selectedGender.value == gender) return
        android.util.Log.d("GenderSwitch", "=== handleGenderSwitch: ${viewModel.selectedGender.value} -> $gender ===")
        // Lưu nav đang chọn của gender hiện tại trước khi switch
        viewModel.genderNavPositionMap[viewModel.selectedGender.value] = viewModel.positionNavSelected
        binding.btnMan.isSelected = gender == 1
        binding.btnWooman.isSelected = gender == 2
        lifecycleScope.launch(Dispatchers.IO) {
            viewModel.setGender(gender)
            // Khôi phục nav đã chọn trước đó cho gender này (nếu có)
            val savedNavIndex = viewModel.genderNavPositionMap[gender] ?: -1
            android.util.Log.d("GenderSwitch", "savedNavIndex=$savedNavIndex")
            viewModel.setBottomNavigationListDefault(savedNavIndex)
            val targetNav = viewModel.bottomNavigationList.value.first { it.isSelected }
            android.util.Log.d("GenderSwitch", "targetNav: layerIndex=${targetNav.layerIndex}")
            viewModel.setPositionNavSelected(targetNav.layerIndex)
            viewModel.setPositionCustom(viewModel.dataCustomize.value!!.layerList[targetNav.layerIndex].positionCustom)
            android.util.Log.d("GenderSwitch", "positionNavSelected=${viewModel.positionNavSelected}, positionCustom=${viewModel.positionCustom}")

            val listToSubmit = viewModel.itemNavList[viewModel.positionNavSelected]

            // Nếu body layer của gender này chưa được load (pathSelectedList rỗng), load default
            val currentPath = viewModel.pathSelectedList.getOrNull(viewModel.positionCustom)
            if (currentPath.isNullOrEmpty()) {
                val selectedItem = listToSubmit.firstOrNull {
                    it.isSelected &&
                    it.path != com.therian.oc.aaa.core.utils.key.AssetsKey.NONE_LAYER &&
                    it.path != com.therian.oc.aaa.core.utils.key.AssetsKey.RANDOM_LAYER
                }
                if (selectedItem != null) {
                    viewModel.setPathSelected(viewModel.positionCustom, selectedItem.path)
                    viewModel.setKeySelected(viewModel.positionNavSelected, selectedItem.path)
                    viewModel.setIsSelectedItem(viewModel.positionCustom)
                    withContext(Dispatchers.Main) {
                        Glide.with(this@CustomizeCharacterActivity).load(selectedItem.path)
                            .into(viewModel.imageViewList[viewModel.positionCustom])
                    }
                }
            }

            withContext(Dispatchers.Main) {
                if (binding.layoutMove.isVisible) closeMovePanel()
                applySelectedLayerTransform()
                layerCustomizeAdapter.submitList(listToSubmit)
                colorLayerCustomizeAdapter.submitList(viewModel.colorItemNavList[viewModel.positionNavSelected])
                if (viewModel.colorItemNavList[viewModel.positionNavSelected].size > 1) {
                    binding.rcvColor.smoothScrollToPosition(
                        viewModel.colorItemNavList[viewModel.positionNavSelected].indexOfFirst { it.isSelected }
                    )
                }
                checkStatusColor()
                refreshMoveControls()
                updateMoveButtonState()
            }
        }
    }

    private fun handleReset() {
        val dialog = YesNoDialog(
            this@CustomizeCharacterActivity,
            R.string.reset,
            R.string.change_your_whole_design_are_you_sure
        )
        LanguageHelper.setLocale(this)
        dialog.show()
        dialog.onYesClick = {
            checkInternet {
            dialog.dismiss()
            lifecycleScope.launch(Dispatchers.IO) {
                viewModel.setClickReset()
                withContext(Dispatchers.Main) {
                    viewModel.imageViewList.forEach { imageView ->
                        Glide.with(this@CustomizeCharacterActivity).clear(imageView)
                    }
                    val layers = viewModel.dataCustomize.value!!.layerList
                    val manBodyIndex = layers.indexOfFirst { it.type == 1 || it.type == 0 }
                    val womanBodyIndex = layers.indexOfFirst { it.type == 2 || it.type == 0 }
                    setOf(manBodyIndex, womanBodyIndex).filter { it >= 0 }.distinct().forEach { bodyIdx ->
                        val posCustom = layers[bodyIdx].positionCustom
                        val path = viewModel.pathSelectedList.getOrNull(posCustom) ?: ""
                        if (path.isNotEmpty()) {
                            Glide.with(this@CustomizeCharacterActivity).load(path)
                                .into(viewModel.imageViewList[posCustom])
                        }
                    }
                    viewModel.imageViewList.forEach { imageView ->
                        imageView.translationX = 0f
                        imageView.translationY = 0f
                        imageView.rotation = 0f
                        imageView.scaleX = 1f
                        imageView.scaleY = 1f
                    }
                    viewModel.initLayerTransformList(viewModel.imageViewList.size)
                    if (binding.layoutMove.isVisible) closeMovePanel()
                    layerCustomizeAdapter.submitList(viewModel.itemNavList[viewModel.positionNavSelected])
                    colorLayerCustomizeAdapter.submitList(viewModel.colorItemNavList[viewModel.positionNavSelected])
                    refreshMoveControls()
                    updateMoveButtonState()
                    showInterAll { hideNavigation() }
                }
            }
            }
        }
        dialog.onNoClick = {
            dialog.dismiss()
            hideNavigation()
        }
    }

    private fun handleRandomAllLayer() {
        lifecycleScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                binding.actionBar.btnActionBarRightText.isEnabled = false
                if (loadingDialog.isShowing.not()) loadingDialog.show()
            }
            val timeStart = System.currentTimeMillis()
            val isOutTurn = viewModel.setClickRandomFullLayer()

            withContext(Dispatchers.Main) {
                viewModel.pathSelectedList.forEachIndexed { index, path ->
                    Glide.with(this@CustomizeCharacterActivity)
                        .load(path)
                        .into(viewModel.imageViewList[index])
                }
                layerCustomizeAdapter.submitList(viewModel.itemNavList[viewModel.positionNavSelected])
                colorLayerCustomizeAdapter.submitList(viewModel.colorItemNavList[viewModel.positionNavSelected])
                if (isOutTurn) binding.btnRandom.invisible()
                val timeEnd = System.currentTimeMillis()
                delay(800)
                if (loadingDialog.isShowing) loadingDialog.dismiss()
                binding.actionBar.btnActionBarRightText.isEnabled = true
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.setIsCreated(false)
    }

    @SuppressLint("GestureBackNavigation", "MissingSuperCall")
    override fun onBackPressed() {
        confirmExit()
    }

    fun initNativeCollab() {
//        Admob.getInstance().loadNativeCollapNotBanner(this,getString(R.string.native_cl_custom),
//            binding.flNativeCollab
//        )
    }

    override fun initAds() {
//        initNativeCollab()
    }

    override fun onRestart() {
        super.onRestart()
//       initNativeCollab()

    }

    override fun onStart() {
        super.onStart()
        android.util.Log.d("CustomizeLifecycle", "onStart() called")
    }

    override fun onResume() {
        super.onResume()
        android.util.Log.d("CustomizeLifecycle", "onResume() called")
    }

    override fun onPause() {
        super.onPause()
        android.util.Log.d("CustomizeLifecycle", "onPause() called")
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            applyUiCustomize()
            hideNavigation()

            window.decorView.removeCallbacks(reHideRunnable)
            window.decorView.postDelayed(reHideRunnable, 1500)
        } else {
            window.decorView.removeCallbacks(reHideRunnable)
        }
    }

    private val reHideRunnable = Runnable {
        applyUiCustomize()
        hideNavigation()
    }
    @Suppress("DEPRECATION")
    private fun applyUiCustomize() {
        // Cho phép app tự vẽ màu system bar
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)

        // Transparent status bar
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT

        // Flags
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                     View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        // nếu muốn icon status bar đen thì thêm:
        // or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
    }
}
