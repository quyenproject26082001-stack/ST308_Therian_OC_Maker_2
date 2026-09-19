package com.therian.oc.aaa.ui.trending

import android.app.ActivityOptions
import android.content.Intent
import android.graphics.Canvas
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.viewModels
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.graphics.createBitmap
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.therian.oc.aaa.R
import com.therian.oc.aaa.core.base.BaseActivity
import com.therian.oc.aaa.core.extensions.checkInternet
import com.therian.oc.aaa.core.extensions.gone
import com.therian.oc.aaa.core.extensions.handleBackLeftToRight
import com.therian.oc.aaa.core.extensions.hideNavigation
import com.therian.oc.aaa.core.extensions.setImageActionBar
import com.therian.oc.aaa.core.extensions.setTextActionBar
import com.therian.oc.aaa.core.extensions.showInterAll
import com.therian.oc.aaa.core.extensions.tap
import com.therian.oc.aaa.core.extensions.visible
import com.therian.oc.aaa.core.helper.InternetHelper
import com.therian.oc.aaa.core.helper.MediaHelper
import com.therian.oc.aaa.core.utils.key.IntentKey
import com.therian.oc.aaa.core.utils.key.ValueKey
import com.therian.oc.aaa.core.utils.state.SaveState
import com.therian.oc.aaa.data.model.custom.SuggestionModel
import com.therian.oc.aaa.databinding.ActivityTrendingBinding
import com.therian.oc.aaa.dialog.YesNoDialog
import com.therian.oc.aaa.ui.customize.CustomizeCharacterActivity
import com.therian.oc.aaa.ui.customize.CustomizeCharacterViewModel
import com.therian.oc.aaa.ui.home.DataViewModel
import com.therian.oc.aaa.ui.random_character.RandomCharacterViewModel
import com.lvt.ads.util.Admob
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

class TrendingActivity : BaseActivity<ActivityTrendingBinding>() {

    private val viewModel: RandomCharacterViewModel by viewModels()
    private val dataViewModel: DataViewModel by viewModels()
    private val customizeCharacterViewModel: CustomizeCharacterViewModel by viewModels()

    private var currentSuggestion: SuggestionModel? = null
    private var isAnimating = false

    private var randomFrom2 = false


    override fun setViewBinding(): ActivityTrendingBinding {
        return ActivityTrendingBinding.inflate(LayoutInflater.from(this))
    }

    override fun initView() {
        lifecycleScope.launch { showLoading() }
        dataViewModel.ensureData(this)
        binding.cvImage.post {
        }
        binding.tvEdit.isSelected = true
        binding.tvGenerate.isSelected = true

    }

    override fun dataObservable() {
        lifecycleScope.launch {
            dataViewModel.allData.collect { data ->
                if (data.isNotEmpty()) {
                    initData()
                }
            }
        }
    }

    override fun viewListener() {
        binding.apply {
            actionBar.btnActionBarLeft.tap { showInterAll { handleBackLeftToRight() } }
            btnGenerate.tap(0) {
                if (randomFrom2 == true) {
                    checkInternet { showInterAll { handleGenerate() } }
                } else {
                    randomFrom2 = true
                    checkInternet { handleGenerate() }
                }
            }
            btnEdit.tap {
                checkInternet {
                    if (!binding.guidRandom.isVisible) {
                        handleEdit()}
                    else{
                        Toast.makeText(this@TrendingActivity,R.string.no_item_here, Toast.LENGTH_SHORT).show()
                    }
                    }
                }
            }
        }

        override fun initActionBar() {
            binding.actionBar.apply {
                tvCenter.visible()
                btnActionBarLeft.visible()
                tvCenter.setText(R.string.random)
                tvCenter.updateLayoutParams<ConstraintLayout.LayoutParams> {
                    width = 0
                    height = WRAP_CONTENT
                    marginStart = 0
                    marginEnd = 0
                }
                tvCenter.isSelected = true
            }
        }

        private fun initData() {
            val handleExceptionCoroutine = CoroutineExceptionHandler { _, throwable ->
                CoroutineScope(Dispatchers.Main).launch {
                    val dialogExit = YesNoDialog(
                        this@TrendingActivity,
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
                        finish()
                    }
                }
            }

            CoroutineScope(SupervisorJob() + Dispatchers.Main + handleExceptionCoroutine).launch {
                val t0 = System.currentTimeMillis()
                android.util.Log.d(
                    "TIMING_TRENDING",
                    "initData() START (allData.size=${dataViewModel.allData.value.size})"
                )

                val internetStart = System.currentTimeMillis()
                val hasInternet = withContext(Dispatchers.IO) {
                    InternetHelper.isInternetAvailable(this@TrendingActivity)
                }
                android.util.Log.d(
                    "TIMING_TRENDING",
                    "  [Step1] internetCheck: ${System.currentTimeMillis() - internetStart}ms | hasInternet=$hasInternet"
                )

                if (!hasInternet) {
                    dismissLoading()
                    showNoInternetDialog()
                    return@launch
                }

                val filteredData = dataViewModel.allData.value
                android.util.Log.d(
                    "TIMING_TRENDING",
                    "  filteredData.size=${filteredData.size} (allData.size=${dataViewModel.allData.value.size}, hasInternet=$hasInternet)"
                )
                if (filteredData.isEmpty()) {
                    android.util.Log.w("TIMING_TRENDING", "  filteredData rỗng → return")
                    return@launch
                }

                suspend fun processCharacter(
                    summary: com.therian.oc.aaa.data.model.custom.CustomizeModel
                ) {
                    val data = dataViewModel.getCharacter(
                        this@TrendingActivity,
                        summary
                    ) ?: return
                    customizeCharacterViewModel.positionSelected =
                        dataViewModel.allData.value.indexOf(summary)
                    customizeCharacterViewModel.setDataCustomize(data)
                    customizeCharacterViewModel.updateAvatarPath(data.avatar)
                    customizeCharacterViewModel.resetDataList()
                    customizeCharacterViewModel.addValueToItemNavList()
                    customizeCharacterViewModel.setItemColorDefault()
                    val allNavList = data.layerList.mapIndexed { index, layer ->
                        com.therian.oc.aaa.data.model.custom.NavigationModel(
                            imageNavigation = layer.imageNavigation,
                            layerIndex = index
                        )
                    }.toCollection(ArrayList())
                    allNavList.firstOrNull()?.isSelected = true
                    customizeCharacterViewModel.setBottomNavigationList(allNavList)
                    for (j in 0 until ValueKey.RANDOM_QUANTITY) {
                        customizeCharacterViewModel.setClickRandomFullLayer()
                        val suggestion = customizeCharacterViewModel.getSuggestionList()
                        viewModel.updateRandomList(suggestion)
                    }
                }

                // Xử lý character đầu tiên → show ngay
                val processFirstStart = System.currentTimeMillis()
                withContext(Dispatchers.IO) {
                    try {
                        processCharacter(filteredData[0])
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    viewModel.upsideDownList()
                }
                android.util.Log.d(
                    "TIMING_TRENDING",
                    "  [Step2] processCharacter[0] (${filteredData[0].dataName}, isFromAPI=${filteredData[0].isFromAPI}): ${System.currentTimeMillis() - processFirstStart}ms | randomList.size=${viewModel.randomList.size}"
                )

                android.util.Log.d(
                    "TIMING_TRENDING",
                    "  [Step3] processFirst DONE: ${System.currentTimeMillis() - t0}ms"
                )
                lifecycleScope.launch { dismissLoading() }

                // Xử lý phần còn lại ở background
                if (filteredData.size > 1) {
                    val bgStart = System.currentTimeMillis()
                    withContext(Dispatchers.IO) {
                        for (i in 1 until filteredData.size) {
                            val charStart = System.currentTimeMillis()
                            try {
                                processCharacter(filteredData[i])
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                            android.util.Log.d(
                                "TIMING_TRENDING",
                                "  [BG] processCharacter[$i] (${filteredData[i].dataName}): ${System.currentTimeMillis() - charStart}ms"
                            )
                        }
                        viewModel.upsideDownList()
                    }
                    android.util.Log.d(
                        "TIMING_TRENDING",
                        "  [BG] tất cả characters còn lại: ${System.currentTimeMillis() - bgStart}ms | randomList.size=${viewModel.randomList.size}"
                    )
                }
                android.util.Log.d(
                    "TIMING_TRENDING",
                    "initData() COMPLETE | tổng: ${System.currentTimeMillis() - t0}ms"
                )
            }
        }

        private fun showRandomSuggestion(onComplete: (() -> Unit)? = null) {
            if (viewModel.randomList.isEmpty()) {
                onComplete?.invoke()
                return
            }
            val model = viewModel.randomList.random()
            currentSuggestion = model
            renderSuggestion(model, onComplete)
        }

        private fun handleGenerate() {
            if (viewModel.randomList.isEmpty()) return
            if (isAnimating) return
            isAnimating = true
            binding.btnGenerate.alpha = 0.3f

            lifecycleScope.launch {
                showLoading()

                // Check internet, timeout 3s để tránh hang khi mất mạng
                val hasInternet = withContext(Dispatchers.IO) {
                    try {
                        withTimeout(3000) { InternetHelper.isInternetAvailable(this@TrendingActivity) }
                    } catch (e: TimeoutCancellationException) {
                        false
                    }
                }
                if (!hasInternet) {
                    dismissLoading()
                    isAnimating = false
                    binding.btnGenerate.visibility = View.VISIBLE
                    showNoInternetDialog()
                    return@launch
                }

                val availableList = viewModel.randomList
                val finalModel = availableList.randomOrNull() ?: run {
                    dismissLoading()
                    isAnimating = false
                    binding.btnGenerate.visibility = View.VISIBLE
                    return@launch
                }

                currentSuggestion = finalModel
                renderSuggestion(finalModel) {
                    lifecycleScope.launch { dismissLoading() }
                    isAnimating = false
                    binding.btnGenerate.alpha = 1f
                }
            }
        }

        private fun showNoInternetDialog() {
            val dialog = YesNoDialog(
                this,
                R.string.no_internet,
                R.string.please_check_your_internet,
                isError = true
            )
            dialog.show()
            dialog.onYesClick = {
                dialog.dismiss()
                hideNavigation()
            }
        }

        private fun renderSuggestion(model: SuggestionModel, onComplete: (() -> Unit)? = null) {
            android.util.Log.d(
                "TrendingDebug",
                "renderSuggestion() called | pathInternalRandom='${model.pathInternalRandom}' | pathSelectedList.size=${model.pathSelectedList.size} | avatarPath='${model.avatarPath}'"
            )

            if (model.pathInternalRandom.isNotEmpty()) {
                android.util.Log.d(
                    "TrendingDebug",
                    "  → pathInternalRandom không rỗng, load trực tiếp"
                )
                Glide.with(this)
                    .load(model.pathInternalRandom)
                    .listener(glideListener(onComplete))
                    .into(binding.imvImage)
                return
            }

            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val paths = model.pathSelectedList.filter { it.isNotEmpty() }
                    android.util.Log.d(
                        "TrendingDebug",
                        "  → pathInternalRandom rỗng, tính từ pathSelectedList"
                    )
                    android.util.Log.d(
                        "TrendingDebug",
                        "     pathSelectedList raw (${model.pathSelectedList.size} item): ${model.pathSelectedList}"
                    )
                    android.util.Log.d(
                        "TrendingDebug",
                        "     paths sau filter notEmpty (${paths.size} item): $paths"
                    )
                    if (paths.isEmpty()) {
                        android.util.Log.w(
                            "TrendingDebug",
                            "  !! paths.isEmpty() → onComplete gọi không có ảnh nào, GIF sẽ còn quay!"
                        )
                        withContext(Dispatchers.Main) { onComplete?.invoke() }
                        return@launch
                    }

                    val bitmapDefault = Glide.with(this@TrendingActivity)
                        .asBitmap().load(paths.first()).submit().get()
                    val width = bitmapDefault.width / 2
                    val height = bitmapDefault.height / 2

                    val listBitmap = coroutineScope {
                        paths.map { path ->
                            async {
                                Glide.with(this@TrendingActivity)
                                    .asBitmap().load(path).submit(width, height).get()
                            }
                        }.awaitAll()
                    }

                    val combinedBitmap = createBitmap(width, height)
                    val canvas = Canvas(combinedBitmap)
                    for (bitmap in listBitmap) {
                        val left = (width - bitmap.width) / 2f
                        val top = (height - bitmap.height) / 2f
                        canvas.drawBitmap(bitmap, left, top, null)
                    }

                    MediaHelper.saveBitmapToPreviewCache(
                        this@TrendingActivity,
                        combinedBitmap
                    ).collect { state ->
                        android.util.Log.d(
                            "TrendingDebug",
                            "  → saveBitmapToPreviewCache state: $state"
                        )
                        if (state is SaveState.Success) {
                            model.pathInternalRandom = state.path
                            android.util.Log.d(
                                "TrendingDebug",
                                "     Save thành công: path='${state.path}'"
                            )
                        }
                    }

                    android.util.Log.d(
                        "TrendingDebug",
                        "  → Sau save: pathInternalRandom='${model.pathInternalRandom}'"
                    )
                    withContext(Dispatchers.Main) {
                        if (model.pathInternalRandom.isEmpty()) {
                            android.util.Log.w(
                                "TrendingDebug",
                                "  !! pathInternalRandom vẫn rỗng sau save → Glide.load('') sẽ fail, GIF còn quay!"
                            )
                        }
                        Glide.with(this@TrendingActivity)
                            .load(model.pathInternalRandom)
                            .listener(glideListener(onComplete))
                            .into(binding.imvImage)
                    }
                } catch (e: Exception) {
                    android.util.Log.e(
                        "TrendingDebug",
                        "  !! Exception trong renderSuggestion: ${e::class.simpleName}: ${e.message}",
                        e
                    )
                    withContext(Dispatchers.Main) { onComplete?.invoke() }
                }
            }
        }

        private fun glideListener(onComplete: (() -> Unit)?): RequestListener<android.graphics.drawable.Drawable> {
            return object : RequestListener<android.graphics.drawable.Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<android.graphics.drawable.Drawable>,
                    isFirstResource: Boolean
                ): Boolean {
                    android.util.Log.e(
                        "TrendingDebug",
                        "  !! Glide.onLoadFailed: model='$model' | cause=${e?.causes?.joinToString { it.message ?: it::class.simpleName ?: "?" }}"
                    )
                    onComplete?.invoke()
                    return false
                }

                override fun onResourceReady(
                    resource: android.graphics.drawable.Drawable,
                    model: Any,
                    target: Target<android.graphics.drawable.Drawable>?,
                    dataSource: DataSource,
                    isFirstResource: Boolean
                ): Boolean {
                    android.util.Log.d(
                        "TrendingDebug",
                        "  ✓ Glide.onResourceReady: model='$model' | source=$dataSource"
                    )
                    binding.guidRandom.gone()
                    onComplete?.invoke()
                    return false
                }
            }
        }

        private fun handleEdit() {
            val suggestion = currentSuggestion ?: return
            customizeCharacterViewModel.positionSelected =
                dataViewModel.allData.value.indexOfFirst { it.avatar == suggestion.avatarPath }
            val selectedCharacter =
                dataViewModel.allData.value.getOrNull(customizeCharacterViewModel.positionSelected)
            viewModel.setIsDataAPI(selectedCharacter?.isFromAPI ?: false)
            viewModel.checkDataInternet(this) {
                lifecycleScope.launch {
                    showLoading()
                    withContext(Dispatchers.IO) {
                        MediaHelper.writeModelToFile(
                            this@TrendingActivity,
                            ValueKey.SUGGESTION_FILE_INTERNAL,
                            suggestion
                        )
                    }
                    val intent =
                        Intent(this@TrendingActivity, CustomizeCharacterActivity::class.java)
                    intent.putExtra(
                        IntentKey.INTENT_KEY,
                        customizeCharacterViewModel.positionSelected
                    )
                    intent.putExtra(IntentKey.STATUS_FROM_KEY, ValueKey.SUGGESTION)
                    val option = ActivityOptions.makeCustomAnimation(
                        this@TrendingActivity,
                        R.anim.slide_out_left,
                        R.anim.slide_in_right
                    )
                    dismissLoading()
                    showInterAll { startActivity(intent, option.toBundle()) }
                }
            }
        }

        override fun onWindowFocusChanged(hasFocus: Boolean) {
            super.onWindowFocusChanged(hasFocus)
            if (hasFocus) {
                applyUiCustomize()
                hideNavigation(true)
                window.decorView.removeCallbacks(reHideRunnable)
                window.decorView.postDelayed(reHideRunnable, 2000)
            } else {
                window.decorView.removeCallbacks(reHideRunnable)
            }
        }

        private val reHideRunnable = Runnable {
            applyUiCustomize()
            hideNavigation(true)
        }

        @Suppress("DEPRECATION")
        private fun applyUiCustomize() {
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        }


        fun initNativeCollab() {
//        Admob.getInstance().loadNativeCollapNotBanner(this,getString(R.string.native_cl_random), binding.flNativeCollab)
        }

        override fun initAds() {
//        initNativeCollab()
        }

        override fun onRestart() {
            super.onRestart()
//        initNativeCollab()
        }


    }
