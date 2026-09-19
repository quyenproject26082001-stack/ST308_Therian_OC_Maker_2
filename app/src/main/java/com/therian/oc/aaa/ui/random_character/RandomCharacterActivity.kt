package com.therian.oc.aaa.ui.random_character

import android.app.ActivityOptions
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.lvt.ads.util.Admob
import com.therian.oc.aaa.R
import com.therian.oc.aaa.ui.customize.CustomizeCharacterActivity
import com.therian.oc.aaa.ui.customize.CustomizeCharacterViewModel
import com.therian.oc.aaa.ui.home.DataViewModel
import com.therian.oc.aaa.core.base.BaseActivity
import com.therian.oc.aaa.core.extensions.dLog
import com.therian.oc.aaa.core.extensions.eLog
import com.therian.oc.aaa.core.extensions.handleBackLeftToRight
import com.therian.oc.aaa.core.extensions.hideNavigation
import com.therian.oc.aaa.core.extensions.invisible
import com.therian.oc.aaa.core.extensions.loadNativeCollabAds
import com.therian.oc.aaa.core.extensions.setImageActionBar
import com.therian.oc.aaa.core.extensions.setTextActionBar
import com.therian.oc.aaa.core.extensions.showInterAll
import com.therian.oc.aaa.core.extensions.tap
import com.therian.oc.aaa.core.extensions.startIntentRightToLeft
import com.therian.oc.aaa.core.extensions.visible
import com.therian.oc.aaa.core.helper.InternetHelper
import com.therian.oc.aaa.core.helper.MediaHelper
import com.therian.oc.aaa.core.utils.key.IntentKey
import com.therian.oc.aaa.core.utils.key.ValueKey
import com.therian.oc.aaa.data.model.custom.SuggestionModel
import com.therian.oc.aaa.databinding.ActivityRandomCharacterBinding
import com.therian.oc.aaa.dialog.YesNoDialog
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.collections.get
import kotlin.getValue
import kotlin.text.compareTo

class RandomCharacterActivity : BaseActivity<ActivityRandomCharacterBinding>() {
    private val viewModel: RandomCharacterViewModel by viewModels()
    private val dataViewModel: DataViewModel by viewModels()
    private val customizeCharacterViewModel: CustomizeCharacterViewModel by viewModels()
    private val randomCharacterAdapter by lazy { RandomCharacterAdapter(this) }

    override fun setViewBinding(): ActivityRandomCharacterBinding {
        return ActivityRandomCharacterBinding.inflate(LayoutInflater.from(this))
    }

    override fun initView() {
        lifecycleScope.launch { showLoading() }
        dataViewModel.ensureData(this)
    }

    override fun dataObservable() {
        lifecycleScope.launch {
            lifecycleScope.launch {
                dataViewModel.allData.collect { data ->
                    if (data.isNotEmpty()) {
                        initData()
                    }
                }
            }
        }
    }

    override fun viewListener() {
        binding.apply {
            actionBar.btnActionBarLeft.tap { showInterAll{handleBackLeftToRight()} }

        }

        randomCharacterAdapter.onItemClick = { model -> handleItemClick(model)}

    }

    override fun initActionBar() {
        binding.actionBar.apply {
            btnActionBarLeft.visible()
            tvCenter.invisible()
        }
    }

    private fun initData() {
        val handleExceptionCoroutine = CoroutineExceptionHandler { _, throwable ->
            eLog("initData: ${throwable.message}")
            CoroutineScope(Dispatchers.Main).launch {
                dismissLoading()
                val dialogExit = YesNoDialog(this@RandomCharacterActivity, R.string.error, R.string.an_error_occurred)
                dialogExit.show()
                dialogExit.onNoClick = {
                    dialogExit.dismiss()
                    finish()
                }
                dialogExit.onYesClick = {
                    dialogExit.dismiss()
                    hideNavigation()
                    startIntentRightToLeft(
                        RandomCharacterActivity::class.java, customizeCharacterViewModel.positionSelected
                    )
                    finish()
                }
            }
        }

        CoroutineScope(SupervisorJob() + Dispatchers.IO + handleExceptionCoroutine).launch {
            // Get data from list
            val deferred1 = async {
                val timeStart1 = System.currentTimeMillis()
                val hasInternet = InternetHelper.isInternetAvailable(this@RandomCharacterActivity)

                if (!hasInternet) {
                    return@async false
                }

                val filteredData = dataViewModel.allData.value

                dLog("==========================================================")
                dLog("RandomCharacter: Starting to process ${filteredData.size} characters")
                dLog("Total data available: ${dataViewModel.allData.value.size}")
                dLog("Has Internet: $hasInternet")
                dLog("Filtered to local only: false")
                dLog("==========================================================")

                for (i in 0 until filteredData.size) {
                    try {
                        dLog("---------- Processing Character $i ----------")
                        val summary = filteredData[i]
                        val currentData = dataViewModel.getCharacter(
                            this@RandomCharacterActivity,
                            summary
                        ) ?: continue
                        customizeCharacterViewModel.positionSelected =
                            dataViewModel.allData.value.indexOf(summary)
                        dLog("Character name: ${currentData.dataName}")
                        dLog("Avatar path: ${currentData.avatar}")
                        dLog("Layer count: ${currentData.layerList.size}")
                        dLog("Is from API: ${currentData.isFromAPI}")

                        customizeCharacterViewModel.setDataCustomize(currentData)
                        customizeCharacterViewModel.updateAvatarPath(currentData.avatar)

                        customizeCharacterViewModel.resetDataList()
                        customizeCharacterViewModel.addValueToItemNavList()
                        customizeCharacterViewModel.setItemColorDefault()
                        customizeCharacterViewModel.setBottomNavigationListDefault()

                        for (j in 0 until ValueKey.RANDOM_QUANTITY) {
                            customizeCharacterViewModel.setClickRandomFullLayer()
                            val suggestion = customizeCharacterViewModel.getSuggestionList()
                            dLog("Generated random $j for character $i - Avatar: ${suggestion.avatarPath}")
                            viewModel.updateRandomList(suggestion)
                        }
                        dLog("✓ Character $i completed successfully")
                    } catch (e: Exception) {
                        eLog("✗ ERROR processing character $i: ${e.message}")
                        e.printStackTrace()
                    }
                }
                viewModel.upsideDownList()

                dLog("==========================================================")
                dLog("RandomCharacter: Finished processing")
                dLog("Total time: ${System.currentTimeMillis() - timeStart1}ms")
                dLog("Final random list size: ${viewModel.randomList.size}")
                dLog("==========================================================")
                return@async true
            }

            withContext(Dispatchers.Main) {
                if (deferred1.await()) {
                    dismissLoading()
                    initRcv()
                } else {
                    dismissLoading()
                    showNoInternetDialog()
                }
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

    private fun initRcv() {
        binding.rcvRandomCharacter.apply {
            adapter = randomCharacterAdapter
            itemAnimator = null

            // ✅ PERFORMANCE OPTIMIZATIONS
            // Cache more ViewHolders to avoid recreating them
            setItemViewCacheSize(20)

            // Use a shared RecycledViewPool for better performance
            setRecycledViewPool(androidx.recyclerview.widget.RecyclerView.RecycledViewPool().apply {
                setMaxRecycledViews(0, 30)
            })

            // Enable drawing cache (deprecated but can help on older devices)
            isDrawingCacheEnabled = true
            setHasFixedSize(true) // All items have the same size

        }
        dLog("==========================================================")
        dLog("initRcv: Submitting ${viewModel.randomList.size} items to adapter")
        viewModel.randomList.forEachIndexed { index, item ->
            dLog("Item $index: Avatar=${item.avatarPath}, Layers=${item.pathSelectedList.size}")
        }
        dLog("==========================================================")
        randomCharacterAdapter.submitList(viewModel.randomList)
    }

    private fun handleItemClick(model: SuggestionModel) {
        customizeCharacterViewModel.positionSelected = dataViewModel.allData.value.indexOfFirst { it.avatar == model.avatarPath }
        // ✅ FIX: Use isFromAPI flag from character data instead of position
        val selectedCharacter = dataViewModel.allData.value.getOrNull(customizeCharacterViewModel.positionSelected)
        viewModel.setIsDataAPI(selectedCharacter?.isFromAPI ?: false)
        viewModel.checkDataInternet(this@RandomCharacterActivity) {
            lifecycleScope.launch {
                showLoading()
                withContext(Dispatchers.IO) {
                    MediaHelper.writeModelToFile(this@RandomCharacterActivity, ValueKey.SUGGESTION_FILE_INTERNAL, model)
                }
                val intent = Intent(this@RandomCharacterActivity, CustomizeCharacterActivity::class.java)
                intent.putExtra(IntentKey.INTENT_KEY, customizeCharacterViewModel.positionSelected)
                intent.putExtra(IntentKey.STATUS_FROM_KEY, ValueKey.SUGGESTION)
                val option = ActivityOptions.makeCustomAnimation(
                    this@RandomCharacterActivity,
                    R.anim.slide_out_left,
                    R.anim.slide_in_right
                )
                dismissLoading()
                showInterAll { startActivity(intent, option.toBundle()) }
            }
        }
    }
//
//    fun initNativeCollab() {
//    Admob.getInstance().loadNativeCollapNotBanner(this,getString(R.string.native_cl_trending), binding.flNativeCollab)
//    }

//    override fun initAds() {
//        initNativeCollab()
//    }
//
//    override fun onRestart() {
//        super.onRestart()
//        initNativeCollab()
//    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            applyUiCustomize()
            hideNavigation()

            window.decorView.removeCallbacks(reHideRunnable)
            window.decorView.postDelayed(reHideRunnable, 2000)
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

    override fun onDestroy() {
        super.onDestroy()
        // ✅ Cancel all pending image processing jobs to prevent memory leaks
        randomCharacterAdapter.cancelAllJobs()
    }
}
