package com.therian.oc.aaa.ui.add_character

import android.annotation.SuppressLint
import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.LinearGradient
import android.graphics.Shader
import android.net.Uri
import android.text.Editable
import android.text.SpannableString
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.ImageView
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.facebook.shimmer.ShimmerDrawable
import com.lvt.ads.util.Admob
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import com.therian.oc.aaa.R
import com.therian.oc.aaa.core.base.BaseActivity
import com.therian.oc.aaa.core.extensions.checkInternet
import com.therian.oc.aaa.core.extensions.checkPermissions
import com.therian.oc.aaa.core.extensions.goToSettings
import com.therian.oc.aaa.core.extensions.gone
import com.therian.oc.aaa.core.extensions.hideNavigation
import com.therian.oc.aaa.core.extensions.hideSoftKeyboard
import com.therian.oc.aaa.core.extensions.invisible
import com.therian.oc.aaa.core.extensions.loadImage
import com.therian.oc.aaa.core.extensions.loadNativeCollabAds
import com.therian.oc.aaa.core.extensions.requestPermission
import com.therian.oc.aaa.core.extensions.select
import com.therian.oc.aaa.core.extensions.setFont
import com.therian.oc.aaa.core.extensions.setImageActionBar
import com.therian.oc.aaa.core.extensions.showInterAll
import com.therian.oc.aaa.core.extensions.tap
import com.therian.oc.aaa.core.extensions.visible
import com.therian.oc.aaa.core.helper.BitmapHelper
import com.therian.oc.aaa.core.helper.LanguageHelper
import com.therian.oc.aaa.core.utils.DataLocal
import com.therian.oc.aaa.core.utils.key.AssetsKey
import com.therian.oc.aaa.core.utils.key.IntentKey
import com.therian.oc.aaa.core.utils.key.RequestKey
import com.therian.oc.aaa.core.utils.key.ValueKey
import com.therian.oc.aaa.core.utils.state.SaveState
import com.therian.oc.aaa.data.model.draw.Draw
import com.therian.oc.aaa.data.model.draw.DrawableDraw
import com.therian.oc.aaa.databinding.ActivityAddCharacterBinding
import com.therian.oc.aaa.dialog.ChooseColorDialog
import com.therian.oc.aaa.dialog.DialogSpeech
import com.therian.oc.aaa.dialog.YesNoDialog
import com.therian.oc.aaa.listener.listenerdraw.OnDrawListener
import com.therian.oc.aaa.ui.add_character.adapter.BackgroundColorAdapter
import com.therian.oc.aaa.ui.add_character.adapter.AddCharacterCategoryAdapter
import com.therian.oc.aaa.ui.add_character.adapter.BackgroundImageAdapter
import com.therian.oc.aaa.ui.add_character.adapter.SpeechAdapter
import com.therian.oc.aaa.ui.add_character.adapter.StickerAdapter
import com.therian.oc.aaa.ui.add_character.adapter.TextColorAdapter
import com.therian.oc.aaa.ui.add_character.adapter.TextFontAdapter
import com.therian.oc.aaa.ui.permission.PermissionViewModel
import com.therian.oc.aaa.ui.view.ViewActivity
import com.therian.oc.aaa.ui.success.SuccessActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.collections.get
import kotlin.getValue
import kotlin.toString

class AddCharacterActivity : BaseActivity<ActivityAddCharacterBinding>() {
    private val viewModel: AddCharacterViewModel by viewModels()
    private val permissionViewModel: PermissionViewModel by viewModels()
    private val backgroundImageAdapter by lazy { BackgroundImageAdapter() }
    private val backgroundCategoryAdapter by lazy { AddCharacterCategoryAdapter() }
    private val backgroundColorAdapter by lazy { BackgroundColorAdapter() }
    private val stickerCategoryAdapter by lazy { AddCharacterCategoryAdapter() }
    private val stickerAdapter by lazy { StickerAdapter() }
    private val speechCategoryAdapter by lazy { AddCharacterCategoryAdapter() }
    private val speechAdapter by lazy { SpeechAdapter() }
    private val textFontAdapter by lazy { TextFontAdapter(this) }
    private val textColorAdapter by lazy { TextColorAdapter() }

    // Photo Picker - NO PERMISSION REQUIRED
    private val pickMedia =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                handleSetBackgroundImage(uri.toString(), 0)
            }
        }

    override fun onResume() {
        super.onResume()
        hideNavigation()
        binding.drawView.autoSelectFirstDraw()  //

    }

    private val buttonNavigationList by lazy {
        arrayListOf(
            binding.btnBackground,
            binding.btnSticker,
            binding.btnSpeech,
            binding.btnText,
        )
    }

    private val layoutNavigationList by lazy {
        arrayListOf(
            binding.backgroundTab.lnlBackground,
            binding.stickerTab.root,
            binding.speechTab.root,
            binding.textTab.root,
        )
    }

    override fun setViewBinding(): ActivityAddCharacterBinding {
        return ActivityAddCharacterBinding.inflate(LayoutInflater.from(this))
    }

    override fun initView() {
        binding.actionBar.btnActionBarLeft.visible()
        viewModel.layoutParams = binding.flFunction.layoutParams as ViewGroup.MarginLayoutParams
        viewModel.originalMarginBottom =
            viewModel.layoutParams.topMargin  // Capture initial topMargin
        binding.actionBar.btnActionBarRightText.visible()
        initRcv()
        initDrawView()
        initData()
        setupKeyboardLogging()
        setupBackPressHandler()

        // 🔒 FIX CỨNG VIVO ANDROID 8 AUTO FOCUS
        binding.main.post {
            binding.main.requestFocus()     // Cướp focus khỏi EditText
            binding.textTab.edtText.clearFocus()    // Đảm bảo edtText không focus
            hideSoftKeyboard()              // Ép keyboard tắt
            viewModel.setIsFocusEditText(false) // Reset state
        }
    }

    private var lastImeVisible = false

    private fun setupKeyboardLogging() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
            val imeHeight = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            Log.d("EditTextFlow", "WindowInsets: imeVisible=$imeVisible, imeHeight=$imeHeight")
            Log.d(
                "EditTextFlow",
                "scvText.scrollY=${binding.textTab.root.scrollY}, scvText.canScrollVertically(1)=${
                    binding.textTab.root.canScrollVertically(1)
                }, scvText.canScrollVertically(-1)=${binding.textTab.root.canScrollVertically(-1)}"
            )

            // Handle keyboard visibility changes
            if (lastImeVisible && !imeVisible) {
                // Keyboard was visible, now it's hidden
                Log.d("EditTextFlow", "Keyboard dismissed by system (via window insets)")
                if (viewModel.isFocusEditText.value && binding.textTab.edtText.hasFocus()) {
                    Log.d(
                        "EditTextFlow",
                        "EditText still has focus - back button dismissed keyboard"
                    )
                    // Back button was pressed while EditText had focus
                    viewModel.setIsFocusEditText(false)
                }
            }
            lastImeVisible = imeVisible

            insets
        }
    }

    private fun setupBackPressHandler() {
        val callback = object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                Log.d("EditTextFlow", "OnBackPressedCallback.handleOnBackPressed called")
                Log.d("EditTextFlow", "isFocusEditText.value=${viewModel.isFocusEditText.value}")
                Log.d("EditTextFlow", "edtText.hasFocus()=${binding.textTab.edtText.hasFocus()}")

                if (viewModel.isFocusEditText.value || binding.textTab.edtText.hasFocus()) {
                    Log.d(
                        "EditTextFlow",
                        "Back pressed with EditText focused - hiding keyboard via callback"
                    )
                    viewModel.setIsFocusEditText(false)
                } else {
                    Log.d(
                        "EditTextFlow",
                        "Back pressed without EditText focused - showing confirmExit"
                    )
                    confirmExit()
                }
            }
        }
        onBackPressedDispatcher.addCallback(this, callback)
        Log.d("EditTextFlow", "OnBackPressedCallback registered, enabled=${callback.isEnabled}")
    }
//    private fun setupKeyboardDetection() {
//        binding.main.viewTreeObserver.addOnGlobalLayoutListener {
//            val rect = android.graphics.Rect()
//            binding.main.getWindowVisibleDisplayFrame(rect)
//
//            val screenHeight = binding.main.rootView.height
//            val keypadHeight = screenHeight - rect.bottom
//
//            // If keyboard height is more than 15% of screen, keyboard is showing
//            val isKeyboardNowShowing = keypadHeight > screenHeight * 0.15
//
//            if (isKeyboardNowShowing != isKeyboardShowing) {
//                isKeyboardShowing = isKeyboardNowShowing
//
//                if (isKeyboardShowing && binding.textTab.edtText.hasFocus()) {
//                    // Keyboard just appeared
//                    adjustLayoutForKeyboard(keypadHeight)
//                } else if (!isKeyboardShowing) {
//                    // Keyboard just disappeared
//                    resetLayout()
//                }
//            }
//        }
//    }
//    private fun adjustLayoutForKeyboard(keyboardHeight: Int) {
//        binding.textTab.root.postDelayed({
//            // Scroll the ScrollView to show EditText
//            val location = IntArray(2)
//            binding.textTab.edtText.getLocationOnScreen(location)
//            val edtY = location[1]
//            val edtBottom = edtY + binding.textTab.edtText.height
//
//            val screenHeight = resources.displayMetrics.heightPixels
//            val visibleHeight = screenHeight - keyboardHeight
//
//            if (edtBottom > visibleHeight) {
//                // Calculate how much to scroll
//                val scrollAmount = edtBottom - visibleHeight + UnitHelper.dpToPx(this, 50)
//                binding.textTab.root.smoothScrollBy(0, scrollAmount)
//            }
//        }, 100)
//    }
//
//    private fun resetLayout() {
//        binding.textTab.root.smoothScrollTo(0, 0)
//    }


    override fun dataObservable() {
        binding.apply {
            lifecycleScope.launch {
                repeatOnLifecycle(Lifecycle.State.STARTED) {
                    launch {
//                        typeNavigation
                        viewModel.typeNavigation.collect { type ->
                            if (type != -1) {
                                setupTypeNavigation(type)
                            }
                        }
                    }

                    launch {
//                        typeBackground
                        viewModel.typeBackground.collect { type ->
                            if (type != -1) {
                                setupTypeBackground(type)
                            }
                        }
                    }

                    launch {
//                        isFocusEditText
                        viewModel.isFocusEditText.collect { status ->
                            Log.d("EditTextFlow", "isFocusEditText.collect: status=$status")
                            if (status) {

                                // Clear FLAG_LAYOUT_NO_LIMITS to allow adjustResize to work
                                Log.d(
                                    "EditTextFlow",
                                    "Keyboard showing - clearing FLAG_LAYOUT_NO_LIMITS"
                                )
                                window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
                                viewModel.layoutParams.topMargin = resources.getDimensionPixelSize(
                                    R.dimen.dp_minus_160
                                )
                                flFunction.layoutParams = viewModel.layoutParams
                                Log.d(
                                    "EditTextFlow",
                                    "Layout adjusted - topMargin: ${viewModel.layoutParams.topMargin}"
                                )
                            } else {
                                // Scroll back to top
                                // 🔒 CHỈ reset layout KHI IME ĐÃ THẬT SỰ TẮT
                                textTab.root.smoothScrollTo(0, 0)
                                viewModel.layoutParams.topMargin = viewModel.originalMarginBottom
                                flFunction.layoutParams = viewModel.layoutParams

                                hideSoftKeyboard()

                                textTab.edtText.clearFocus()

                                hideSoftKeyboard()
                                hideNavigation(true)
                                delay(300)
                                hideSoftKeyboard()


                                // Keyboard vẫn mở → ÉP TẮT

                                Log.d(
                                    "EditTextFlow",
                                    "Layout reset complete - topMargin: ${viewModel.layoutParams.topMargin}"
                                )
                            }
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
                btnActionBarCenter.tap { confirmReset() }
                btnActionBarRightText.tap (3000){
                    handleSave()
                }
            }
            backgroundTab.btnBackgroundImage.tap { viewModel.setTypeBackground(ValueKey.IMAGE_BACKGROUND) }
            backgroundTab.btnBackgroundColor.tap { viewModel.setTypeBackground(ValueKey.COLOR_BACKGROUND) }
            btnBackground.tap { viewModel.setTypeNavigation(ValueKey.BACKGROUND_NAVIGATION) }
            btnSticker.tap { viewModel.setTypeNavigation(ValueKey.STICKER_NAVIGATION) }
            btnSpeech.tap { viewModel.setTypeNavigation(ValueKey.SPEECH_NAVIGATION) }
            btnText.tap { viewModel.setTypeNavigation(ValueKey.TEXT_NAVIGATION) }

            textTab.edtText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

                override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                    binding.tvGetText.text = p0.toString()
                }

                override fun afterTextChanged(p0: Editable?) {}
            })
            textTab.edtText.setOnEditorActionListener { textView, i, keyEvent ->
                if (i == EditorInfo.IME_ACTION_DONE) {
                    viewModel.setIsFocusEditText(false)
                    true
                } else {
                    false
                }
            }
            textTab.edtText.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
                Log.d("EditTextFlow", "edtText.onFocusChangeListener: hasFocus=$hasFocus")
                Log.d(
                    "EditTextFlow",
                    "scvText.scrollY=${binding.textTab.root.scrollY}, scvText.height=${binding.textTab.root.height}"
                )
                if (hasFocus) {
                    Log.d("EditTextFlow", "EditText gained focus - setting isFocusEditText=true")
                    viewModel.setIsFocusEditText(true)
                    // Let adjustResize handle layout - no programmatic scroll needed
                } else {
                    Log.d("EditTextFlow", "EditText lost focus - setting isFocusEditText=false")
                    // Only update if not already false (prevent recursive calls)
                    if (viewModel.isFocusEditText.value) {
                        viewModel.setIsFocusEditText(false)
                    }
                }
            }
            textTab.btnDoneText.tap {
                Log.d("EditTextFlow", "btnDoneText tapped")
                handleDoneText()
            }

            main.tap {
                Log.d("EditTextFlow", "main layout tapped - clearing focus")
                viewModel.setIsFocusEditText(false)
                clearFocus()
            }

            backgroundImageAdapter.apply {
                onAddImageClick = { checkStoragePermission() }
                onBackgroundImageClick =
                    { path, position ->
                        if(path.startsWith("http")){

                        checkInternet {  handleSetBackgroundImage(path, position) }}
                    else{
                        handleSetBackgroundImage(path, position)
                        }
                    }
            }

            backgroundCategoryAdapter.onCategoryClick = { position ->
                viewModel.updateBackgroundCategorySelected(position)
                backgroundCategoryAdapter.submitList(viewModel.backgroundCategoryList)
                backgroundImageAdapter.submitList(viewModel.backgroundImageList)
                backgroundTab.rcvBackgroundImage.scrollToPosition(0)
            }

            backgroundColorAdapter.apply {
                onChooseColorClick = { handleChooseColor() }
                onBackgroundColorClick =
                    { color, position -> handleSetBackgroundColor(color, position) }
            }

            stickerAdapter.onItemClick = { path ->
                if (path.startsWith("http")) {
                    checkInternet { addDrawable(path) }
                } else {
                    addDrawable(path)
                }
            }

            stickerCategoryAdapter.onCategoryClick = { position ->
                viewModel.updateStickerCategorySelected(position)
                stickerCategoryAdapter.submitList(viewModel.stickerCategoryList)
                stickerAdapter.submitList(viewModel.stickerList)
                stickerTab.rcvSticker.scrollToPosition(0)
            }

            speechCategoryAdapter.onCategoryClick = { position ->
                viewModel.updateSpeechCategorySelected(position)
                speechCategoryAdapter.submitList(viewModel.speechCategoryList)
                speechAdapter.submitList(viewModel.speechList)
                speechTab.rcvSpeech.scrollToPosition(0)
            }

            speechAdapter.onItemClick = { path ->
                if (path.startsWith("http")) {
                    checkInternet { handleSpeech(path) }
                } else {
                    handleSpeech(path)
                }
            }

            textFontAdapter.onTextFontClick = { font, position -> handleFontClick(font, position) }

            textColorAdapter.apply {
                onChooseColorClick = { handleChooseColor(true) }
                onTextColorClick = { color, position -> handleTextColorClick(color, position) }
            }
        }
    }

    override fun initActionBar() {
        binding.actionBar.apply {
            setImageActionBar(btnActionBarCenter, R.drawable.ic_reset)

            // Căn giữa nút reset vào guideline
            val params =
                btnActionBarCenter.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
            params.endToEnd = guideline.id
            params.startToStart = guideline.id
            params.horizontalBias = 0.5f
            params.marginEnd = 0
            btnActionBarCenter.layoutParams = params
            btnActionBarRightText.visible()
            tvRightText.visible()
            tvRightText.setText(R.string.save)
        }
    }

    override fun initText() {
        binding.apply {
            backgroundTab.tvBackgroundImage.select()
            backgroundTab.tvBackgroundColor.select()

            // Apply gradient to tvText, tvFont, tvColor

        }
    }

    private fun initRcv() {
        binding.apply {
            backgroundTab.rcvBackgroundImage.apply {
                adapter = backgroundImageAdapter
                itemAnimator = null
            }

            backgroundTab.rcvBackgroundCategory.apply {
                adapter = backgroundCategoryAdapter
                itemAnimator = null
            }

            backgroundTab.rcvBackgroundColor.apply {
                adapter = backgroundColorAdapter
                itemAnimator = null
            }

            stickerTab.rcvSticker.apply {
                adapter = stickerAdapter
                itemAnimator = null
                setItemViewCacheSize(200)
                setHasFixedSize(true)
            }

            stickerTab.rcvCategory.apply {
                adapter = stickerCategoryAdapter
                itemAnimator = null
            }

            speechTab.rcvSpeech.apply {
                adapter = speechAdapter
                itemAnimator = null
                setItemViewCacheSize(200)
                setHasFixedSize(true)
            }

            speechTab.rcvCategory.apply {
                adapter = speechCategoryAdapter
                itemAnimator = null
            }

            textTab.rcvFont.apply {
                adapter = textFontAdapter
                itemAnimator = null
            }

            textTab.rcvTextColor.apply {
                adapter = textColorAdapter
                itemAnimator = null
            }
        }
    }


    fun loadCharacter(
        context: Context,
        path: String,
        imageView: ImageView,
        isLoadShimmer: Boolean = true
    ) {
        val shimmerDrawable = ShimmerDrawable().apply {
            setShimmer(DataLocal.shimmer)
        }
        if (isLoadShimmer) {
            Glide.with(context).load(path).placeholder(shimmerDrawable).error(shimmerDrawable)
                .into(imageView)
        } else {
            Glide.with(context).load(path).placeholder(shimmerDrawable).error(shimmerDrawable)
                .into(imageView)
        }

    }

    private fun initData() {
        lifecycleScope.launch(Dispatchers.IO) {
            showLoading()
            viewModel.loadDataDefault(this@AddCharacterActivity)
            viewModel.updatePathDefault(intent.getStringExtra(IntentKey.INTENT_KEY) ?: "")
            addDrawable(viewModel.pathDefault, true)



            withContext(Dispatchers.Main) {

                focusNoneBackgroundItem()
                backgroundCategoryAdapter.submitList(viewModel.backgroundCategoryList)
                binding.backgroundTab.rcvBackgroundCategory.isVisible =
                    viewModel.backgroundCategoryList.isNotEmpty()
                backgroundColorAdapter.submitList(viewModel.backgroundColorList)
                stickerCategoryAdapter.submitList(viewModel.stickerCategoryList)
                binding.stickerTab.rcvCategory.isVisible = viewModel.stickerCategoryList.isNotEmpty()
                stickerAdapter.submitList(viewModel.stickerList)
                speechCategoryAdapter.submitList(viewModel.speechCategoryList)
                binding.speechTab.rcvCategory.isVisible = viewModel.speechCategoryList.isNotEmpty()
                speechAdapter.submitList(viewModel.speechList)
                textFontAdapter.submitListReset(viewModel.textFontList)
                textColorAdapter.submitListReset(viewModel.textColorList)
                // Set initial text color and font to match selected items
                binding.textTab.edtText.setFont(viewModel.textFontList.first().color)
                binding.textTab.edtText.setTextColor(viewModel.textColorList[1].color)
                binding.tvGetText.setFont(viewModel.textFontList.first().color)
                binding.tvGetText.setTextColor(viewModel.textColorList[1].color)


                //clearFocus()
                delay(1000)
//
//                binding.drawView.autoSelectFirstDraw()
                dismissLoading(true)
                delay(500)
                binding.drawView.autoSelectFirstDraw()
            }
        }
    }

    /**
     * Async bitmap loading using Glide without blocking threads
     */
    private suspend fun loadBitmapAsync(path: String): Bitmap =
        suspendCancellableCoroutine { continuation ->
            val target = object : CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    if (continuation.isActive) {
                        continuation.resume(resource)
                    }
                }

                override fun onLoadFailed(errorDrawable: android.graphics.drawable.Drawable?) {
                    if (continuation.isActive) {
                        continuation.resumeWithException(Exception("Failed to load bitmap from: $path"))
                    }
                }

                override fun onLoadCleared(placeholder: android.graphics.drawable.Drawable?) {
                    // Cleanup if needed
                }
            }

            Glide.with(this@AddCharacterActivity)
                .asBitmap()
                .load(path)
                .into(target)

            continuation.invokeOnCancellation {
                Glide.with(this@AddCharacterActivity).clear(target)
            }
        }

    private fun addDrawable(
        path: String,
        isCharacter: Boolean = false,
        bitmapText: Bitmap? = null
    ) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val bitmapDefault = if (bitmapText == null) {
                    Glide.with(this@AddCharacterActivity)
                        .asBitmap()
                        .load(path)
                        .override(512, 512)
                        .encodeQuality(90)
                        .submit()
                        .get()
                } else {
                    bitmapText
                }

                val drawableEmoji = viewModel.loadDrawableEmoji(
                    this@AddCharacterActivity,
                    bitmapDefault,
                    isCharacter
                )

                withContext(Dispatchers.Main) {
                    binding.drawView.addDraw(drawableEmoji)
                }
            } catch (e: Exception) {
                Log.e("AddCharacterActivity", "Failed to add drawable: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    showToast(getString(R.string.save_failed_please_try_again))
                }
            }
        }
    }

    private fun initDrawView() {
        binding.drawView.apply {
            setConstrained(true)
            setLocked(false)
            setOnDrawListener(object : OnDrawListener {
                override fun onAddedDraw(draw: Draw) {
                    Log.d("EditTextFlow", "DrawView: onAddedDraw")
                    viewModel.updateCurrentCurrentDraw(draw)
                    viewModel.addDrawView(draw)
                    viewModel.setIsFocusEditText(false)
                }

                override fun onClickedDraw(draw: Draw) {
                    Log.d("EditTextFlow", "DrawView: onClickedDraw")
                    viewModel.setIsFocusEditText(false)
                }

                override fun onDeletedDraw(draw: Draw) {
                    Log.d("EditTextFlow", "DrawView: onDeletedDraw")
                    viewModel.deleteDrawView(draw)
                    viewModel.setIsFocusEditText(false)
                }

                override fun onDragFinishedDraw(draw: Draw) {
                    Log.d("EditTextFlow", "DrawView: onDragFinishedDraw")
                    viewModel.setIsFocusEditText(false)
                }

                override fun onTouchedDownDraw(draw: Draw) {
                    Log.d("EditTextFlow", "DrawView: onTouchedDownDraw")
                    viewModel.updateCurrentCurrentDraw(draw)
                    viewModel.setIsFocusEditText(false)
                }

                override fun onZoomFinishedDraw(draw: Draw) {}

                override fun onFlippedDraw(draw: Draw) {
                    Log.d("EditTextFlow", "DrawView: onFlippedDraw")
                    viewModel.setIsFocusEditText(false)
                }

                override fun onDoubleTappedDraw(draw: Draw) {}

                override fun onHideOptionIconDraw() {}

                override fun onUndoDeleteDraw(draw: List<Draw?>) {}

                override fun onUndoUpdateDraw(draw: List<Draw?>) {}

                override fun onUndoDeleteAll() {}

                override fun onRedoAll() {}

                override fun onReplaceDraw(draw: Draw) {}

                override fun onEditText(draw: DrawableDraw) {}

                override fun onReplace(draw: Draw) {}
            })
        }
    }

    private fun setupTypeBackground(type: Int) {
        binding.apply {

            when (type) {
                ValueKey.IMAGE_BACKGROUND -> {
                    backgroundTab.rcvBackgroundImage.visible()
                    backgroundTab.rcvBackgroundColor.gone()
                    backgroundTab.rcvBackgroundCategory.isVisible =
                        viewModel.backgroundCategoryList.isNotEmpty()
                    backgroundTab.sectionTab.setBackgroundResource(R.drawable.tab_bg_slt)
                    backgroundImageAdapter.submitList(viewModel.backgroundImageList)
                }

                ValueKey.COLOR_BACKGROUND -> {
                    backgroundTab.rcvBackgroundImage.gone()
                    backgroundTab.rcvBackgroundColor.visible()
                    backgroundTab.rcvBackgroundCategory.gone()
                    backgroundTab.sectionTab.setBackgroundResource(R.drawable.tab_color_slt)
                    backgroundColorAdapter.submitList(viewModel.backgroundColorList)
                }

                else -> {}
            }
        }
    }

    private fun setupTypeNavigation(type: Int) {
        buttonNavigationList.forEachIndexed { index, button ->
            val (res, status) = if (index == type) {
                DataLocal.bottomNavigationSelected[index] to true
            } else {
                DataLocal.bottomNavigationNotSelect[index] to false
            }

            button.setImageResource(res)
            layoutNavigationList[index].isVisible = status
        }

        // Show sectionTab and spaceSectionTab only when Background navigation is selected
        binding.apply {
            val isBackground = (type == ValueKey.BACKGROUND_NAVIGATION)

            // Section tab
            if (isBackground) backgroundTab.sectionTab.visible() else backgroundTab.sectionTab.gone()

            if (isBackground) {
                bgBg.visible()
                bgOther.gone()
            } else {
                bgBg.gone()
                bgOther.visible()
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
            finish()
        }
        dialog.onNoClick = {
            dialog.dismiss()
            hideNavigation(true)
        }
    }

    private fun confirmReset() {
        val dialog =
            YesNoDialog(this, R.string.reset, R.string.change_your_whole_design_are_you_sure)
        dialog.show()

        fun dismissDialog() {
            dialog.dismiss()
            hideNavigation(true)
        }

        dialog.onNoClick = {
            dismissDialog()
        }

        dialog.onYesClick = {
            checkInternet {
            dismissDialog()
            lifecycleScope.launch {
                showLoading()
                withContext(Dispatchers.IO) {
                    viewModel.loadDataDefault(this@AddCharacterActivity)
                    viewModel.resetDraw()
                }
                binding.drawView.removeAllDraw()
                binding.imvBackground.setImageBitmap(null)
                binding.imvBackground.setBackgroundColor(getColor(R.color.transparent))
                binding.textTab.edtText.setText("")
                binding.textTab.edtText.setFont(viewModel.textFontList.first().color)
                binding.textTab.edtText.setTextColor(viewModel.textColorList[1].color)
                binding.tvGetText.setFont(viewModel.textFontList.first().color)
                binding.tvGetText.setTextColor(viewModel.textColorList[1].color)
                addDrawable(viewModel.pathDefault, true)
                focusNoneBackgroundItem()
                backgroundCategoryAdapter.submitList(viewModel.backgroundCategoryList)
                binding.backgroundTab.rcvBackgroundCategory.isVisible =
                    viewModel.backgroundCategoryList.isNotEmpty()
                backgroundColorAdapter.submitList(viewModel.backgroundColorList)
                stickerCategoryAdapter.submitList(viewModel.stickerCategoryList)
                binding.stickerTab.rcvCategory.isVisible = viewModel.stickerCategoryList.isNotEmpty()
                stickerAdapter.submitList(viewModel.stickerList)
                speechCategoryAdapter.submitList(viewModel.speechCategoryList)
                binding.speechTab.rcvCategory.isVisible = viewModel.speechCategoryList.isNotEmpty()
                speechAdapter.submitList(viewModel.speechList)
                textFontAdapter.submitListReset(viewModel.textFontList)
                textColorAdapter.submitListReset(viewModel.textColorList)
                dismissLoading(true)
                showInterAll()
            }
            }
            dialog.dismiss()
        }
    }

    private fun handleSetBackgroundImage(path: String, position: Int) {
        binding.imvBackground.setBackgroundColor(getColor(R.color.transparent))
        if (path == AssetsKey.NONE_LAYER) {
            Glide.with(this).clear(binding.imvBackground)
            binding.imvBackground.setImageDrawable(null)
        } else {
            loadImage(this, path, binding.imvBackground)
        }
        lifecycleScope.launch(Dispatchers.IO) {
            viewModel.updateBackgroundImageSelected(position)
            withContext(Dispatchers.Main) {
                backgroundImageAdapter.submitItem(position, viewModel.backgroundImageList)
            }
        }
    }

    private fun focusNoneBackgroundItem() {
        viewModel.setTypeNavigation(ValueKey.BACKGROUND_NAVIGATION)
        viewModel.setTypeBackground(ValueKey.IMAGE_BACKGROUND)
        backgroundImageAdapter.submitList(viewModel.backgroundImageList)
        binding.backgroundTab.rcvBackgroundImage.post {
            binding.backgroundTab.rcvBackgroundImage.scrollToPosition(
                BackgroundImageAdapter.NONE_ITEM_POSITION
            )
        }
    }

    private fun checkStoragePermission() {
        // Use Photo Picker - NO PERMISSION REQUIRED
        pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    private fun handleChooseColor(isTextColor: Boolean = false) {
        val dialog = ChooseColorDialog(this)

        dialog.show()

        fun dismissDialog() {
            dialog.dismiss()
            hideNavigation(true)
        }

        dialog.onCloseEvent = {
            dismissDialog()
        }

        dialog.onDoneEvent = { color ->
            dismissDialog()
            Log.d(
                "AddCharacterActivity",
                "Color picker selected color: ${
                    String.format(
                        "#%06X",
                        0xFFFFFF and color
                    )
                }, isTextColor=$isTextColor"
            )
            if (!isTextColor) {
                Log.d("AddCharacterActivity", "Calling handleSetBackgroundColor with position 0")
                handleSetBackgroundColor(color, 0)
            } else {
                Log.d("AddCharacterActivity", "Calling handleTextColorClick with position 0")
                handleTextColorClick(color, 0)
            }
        }
    }

    private fun handleSpeech(path: String) {
        val dialog = DialogSpeech(this, path)
        dialog.show()
        dialog.onDoneClick = { bitmap ->
            dialog.dismiss()
            hideNavigation(true)
            if (bitmap != null) {
                addDrawable("", false, bitmap)
            }
        }
    }

    private fun handleSetBackgroundColor(color: Int, position: Int) {
        Log.d(
            "AddCharacterActivity",
            "handleSetBackgroundColor called: color=${
                String.format(
                    "#%06X",
                    0xFFFFFF and color
                )
            }, position=$position"
        )
        binding.apply {
            imvBackground.setImageBitmap(null)
            imvBackground.setBackgroundColor(color)
            lifecycleScope.launch(Dispatchers.IO) {
                Log.d(
                    "AddCharacterActivity",
                    "Before updateBackgroundColorSelected: list[0].color=${
                        String.format(
                            "#%06X",
                            0xFFFFFF and viewModel.backgroundColorList[0].color
                        )
                    }"
                )
                viewModel.updateBackgroundColorSelected(position)
                Log.d(
                    "AddCharacterActivity",
                    "After updateBackgroundColorSelected: list[0].color=${
                        String.format(
                            "#%06X",
                            0xFFFFFF and viewModel.backgroundColorList[0].color
                        )
                    }, list[0].isSelected=${viewModel.backgroundColorList[0].isSelected}"
                )
                withContext(Dispatchers.Main) {
                    backgroundColorAdapter.submitItem(position, viewModel.backgroundColorList)
                }
            }
        }
    }

    private fun handleFontClick(font: Int, position: Int) {
        binding.apply {
            textTab.edtText.hint = SpannableString(getString(R.string.hello_world))
            textTab.edtText.setFont(font)
            tvGetText.setFont(font)
            viewModel.updateTextFontSelected(position)
            textFontAdapter.submitItem(position, viewModel.textFontList)
        }
    }

    private fun handleTextColorClick(color: Int, position: Int) {
        Log.d(
            "AddCharacterActivity",
            "handleTextColorClick called: color=${
                String.format(
                    "#%06X",
                    0xFFFFFF and color
                )
            }, position=$position"
        )
        binding.apply {
            textTab.edtText.setTextColor(color)
            tvGetText.setTextColor(color)
            Log.d(
                "AddCharacterActivity",
                "Before updateTextColorSelected: list[0].color=${
                    String.format(
                        "#%06X",
                        0xFFFFFF and viewModel.textColorList[0].color
                    )
                }"
            )
            viewModel.updateTextColorSelected(position)
            Log.d(
                "AddCharacterActivity",
                "After updateTextColorSelected: list[0].color=${
                    String.format(
                        "#%06X",
                        0xFFFFFF and viewModel.textColorList[0].color
                    )
                }, list[0].isSelected=${viewModel.textColorList[0].isSelected}"
            )
            textColorAdapter.submitItem(position, viewModel.textColorList)
        }
    }

    @SuppressLint("SimpleDateFormat")
    private fun handleDoneText() {
        viewModel.setIsFocusEditText(false)
        binding.apply {
            if (textTab.edtText.text.toString().trim() == "") {
                showToast(getString(R.string.null_edt))
            } else {
                tvGetText.text = textTab.edtText.text.toString().trim()
                val bitmap = BitmapHelper.getBitmapFromEditText(tvGetText)
                val drawableEmoji =
                    viewModel.loadDrawableEmoji(this@AddCharacterActivity, bitmap, isText = true)
                binding.drawView.addDraw(drawableEmoji)

                // Reset
                val font = viewModel.textFontList.first().color
                val color = viewModel.textColorList[1].color

                textTab.edtText.text = null
                textTab.edtText.setFont(font)
                textTab.edtText.setTextColor(color)

                viewModel.updateTextFontSelected(0)
                viewModel.updateTextColorSelected(1)

                textFontAdapter.submitListReset(viewModel.textFontList)
                textColorAdapter.submitListReset(viewModel.textColorList)

                tvGetText.text = ""
                tvGetText.setFont(font)
                tvGetText.setTextColor(color)
            }
        }
    }

    private fun clearFocus() {
        binding.drawView.hideSelect()
    }

    private fun handleSave() {
        binding.apply {
            clearFocus()
            lifecycleScope.launch(Dispatchers.IO) {
                showLoading()
                delay(200)
                viewModel.saveImageFromView(this@AddCharacterActivity, flSave).collect { result ->
                    when (result) {
                        is SaveState.Loading -> showLoading()

                        is SaveState.Error -> {
                            dismissLoading(true)
                            withContext(Dispatchers.Main) {
                                showToast(R.string.save_failed_please_try_again)
                            }
                        }

                        is SaveState.Success -> {
                            val intent =
                                Intent(this@AddCharacterActivity, SuccessActivity::class.java)
                            intent.putExtra(IntentKey.INTENT_KEY, result.path)
                            val options = ActivityOptions.makeCustomAnimation(
                                this@AddCharacterActivity,
                                R.anim.slide_in_right,
                                R.anim.slide_out_left
                            )
                            dismissLoading(true)
                            withContext(Dispatchers.Main) {
                                showInterAll { startActivity(intent, options.toBundle()) }
                            }
                        }
                    }
                }
            }
        }
    }


    @Deprecated("Use OnBackPressedCallback instead")
    @SuppressLint("GestureBackNavigation")
    override fun onBackPressed() {
        Log.d(
            "EditTextFlow",
            "DEPRECATED onBackPressed called - this should NOT happen if callback is working"
        )

        confirmExit()

        // Don't call super or handle anything - let the callback handle it
        // This method should not be called if the OnBackPressedCallback is working properly
    }

    fun initNativeCollab() {
//        Admob.getInstance().loadNativeCollapNotBanner(
//            this,
//            getString(R.string.native_cl_bg),
//            binding.flNativeCollab
//        )
    }

    override fun initAds() {
//        initNativeCollab()
    }

    override fun onRestart() {
        super.onRestart()
//        initNativeCollab()
    }

    // Custom Input View Functions
    private fun setupCustomInput() {
        // val customInputView = binding.customInputLayout.root

        // Set height to 1/4 of screen
//        customInputView.post {
//            val screenHeight = resources.displayMetrics.heightPixels
//            val params = customInputView.layoutParams
//            params.height = screenHeight / 4
//            customInputView.layoutParams = params
//        }

        // Disable system keyboard for edtText
        binding.textTab.edtText.showSoftInputOnFocus = true

//        // Setup edtText click listener
//        binding.textTab.edtText.setOnClickListener {
//           // hideSystemKeyboard()
//          //  showCustomInput()
//        }

//        binding.textTab.edtText.setOnFocusChangeListener { _, hasFocus ->
//            if (hasFocus) {
//               // hideSystemKeyboard()
//                //showCustomInput()
//            }
//        }

        // Setup all letter buttons
        setupLetterButtons()

//        // Setup control buttons
//        binding.customInputLayout.btnSpace.tap { appendText(" ") }
//        binding.customInputLayout.btnDelete.tap { deleteLastChar() }
//        binding.customInputLayout.btnDone.tap { hideCustomInput() }
    }

    private fun setupLetterButtons() {
        val letters = listOf(
            "A", "B", "C", "D", "E", "F", "G", "H", "I", "J",
            "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T",
            "U", "V", "W", "X", "Y", "Z"
        )

//        letters.forEach { letter ->
//            val buttonId = resources.getIdentifier("btn$letter", "id", packageName)
//            val button = binding.customInputLayout.root.findViewById<android.widget.Button>(buttonId)
//            button?.tap { appendText(letter) }
//        }
    }

//    private fun showCustomInput() {
//        binding.customInputLayout.root.visible()
//    }
//
//    private fun hideCustomInput() {
//        binding.customInputLayout.root.gone()
//    }

    private fun hideSystemKeyboard() {
        val imm =
            getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.hideSoftInputFromWindow(binding.textTab.edtText.windowToken, 0)
    }

    private fun appendText(text: String) {
        val currentText = binding.textTab.edtText.text.toString()
        binding.textTab.edtText.setText(currentText + text)
        binding.textTab.edtText.setSelection(binding.textTab.edtText.text.length)
    }

    private fun deleteLastChar() {
        val currentText = binding.textTab.edtText.text.toString()
        if (currentText.isNotEmpty()) {
            binding.textTab.edtText.setText(currentText.dropLast(1))
            binding.textTab.edtText.setSelection(binding.textTab.edtText.text.length)
        }
    }
//    override fun onWindowFocusChanged(hasFocus: Boolean) {
//        super.onWindowFocusChanged(hasFocus)
//        if (hasFocus) {
//            applyUiCustomize()
//            hideNavigation(true)
//
//            window.decorView.removeCallbacks(reHideRunnable)
//            window.decorView.postDelayed(reHideRunnable, 1500)
//        } else {
//            window.decorView.removeCallbacks(reHideRunnable)
//        }
//    }

    private val reHideRunnable = Runnable {
        applyUiCustomize()
        hideNavigation(true)
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
