package com.therian.oc.aaa.ui.language

import android.annotation.SuppressLint
import android.view.LayoutInflater
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.lvt.ads.util.Admob
import com.therian.oc.aaa.R
import com.therian.oc.aaa.core.base.BaseActivity
import com.therian.oc.aaa.core.extensions.gone
import com.therian.oc.aaa.core.extensions.handleBackLeftToRight
import com.therian.oc.aaa.core.extensions.invisible
import com.therian.oc.aaa.core.extensions.select
import com.therian.oc.aaa.core.extensions.startIntentRightToLeft
import com.therian.oc.aaa.core.extensions.startIntentWithClearTop
import com.therian.oc.aaa.core.extensions.visible
import com.therian.oc.aaa.core.utils.key.IntentKey
import com.therian.oc.aaa.databinding.ActivityLanguageBinding
import com.therian.oc.aaa.ui.home.HomeActivity
import com.therian.oc.aaa.ui.intro.IntroActivity
import com.therian.oc.aaa.core.extensions.tap
import com.therian.oc.aaa.core.extensions.strings
import kotlinx.coroutines.launch
import kotlin.system.exitProcess

class LanguageActivity : BaseActivity<ActivityLanguageBinding>() {
    private val viewModel: LanguageViewModel by viewModels()

    private val languageAdapter by lazy { LanguageAdapter(this) }

    override fun setViewBinding(): ActivityLanguageBinding {
        return ActivityLanguageBinding.inflate(LayoutInflater.from(this))
    }

    override fun initView() {
        initRcv()
        val intentValue = intent.getStringExtra(IntentKey.INTENT_KEY)
        val currentLang = sharePreference.getPreLanguage()
        viewModel.setFirstLanguage(intentValue == null)
        viewModel.loadLanguages(currentLang)
        binding.actionBar.tvStart.select()
        binding.actionBar.tvCenter.select()
    }

    override fun dataObservable() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.isFirstLanguage.collect { isFirst ->
                        languageAdapter.isFirstLanguage = isFirst
                        updateActionBar(isFirst)
                    }
                }
                launch {
                    viewModel.languageList.collect { list ->
                        languageAdapter.submitList(list)
                    }
                }
                launch {
                    viewModel.codeLang.collect { code ->
                        if (code.isNotEmpty() && viewModel.isFirstLanguage.value) {
                            binding.actionBar.btnActionBarRightLang.visible()
                        }
                    }
                }
            }
        }
    }

    override fun viewListener() {
        binding.apply {
            actionBar.btnActionBarLeft.tap { handleBackLeftToRight() }
            actionBar.btnActionBarRightLang.tap { handleDone() }
        }
        handleRcv()
    }

    override fun initText() {
        binding.actionBar.tvCenter.select()
        binding.actionBar.tvStart.select()
    }

    override fun initActionBar() {
        binding.actionBar.apply {
            tvCenter.text = strings(R.string.language)
            tvStart.text = strings(R.string.language)
        }
        updateActionBar(viewModel.isFirstLanguage.value)
    }

    private fun updateActionBar(isFirstLanguage: Boolean) {
        binding.actionBar.apply {
            if (isFirstLanguage) {
                btnActionBarLeft.invisible()
                btnActionBarRightLang.setImageResource(R.drawable.ic_done)
                btnActionBarRightLang.invisible()
                tvStart.visible()
                tvCenter.gone()
            } else {
                btnActionBarLeft.setImageResource(R.drawable.ic_back)
                btnActionBarLeft.visible()
                btnActionBarRightLang.setImageResource(R.drawable.ic_done)
                btnActionBarRightLang.visible()
                tvStart.gone()
                tvCenter.visible()
            }
        }
    }

    private fun initRcv() {
        binding.rcv.apply {
            adapter = languageAdapter
            itemAnimator = null
        }
    }

    private fun handleRcv() {
        binding.apply {
            languageAdapter.onItemClick = { code ->
                //  binding.actionBar.btnDone.visible()
                viewModel.selectLanguage(code)
            }
        }
    }

    private fun handleDone() {
        val code = viewModel.codeLang.value
        if (code.isEmpty()) {
            showToast(R.string.not_select_lang)
            return
        }
        sharePreference.setPreLanguage(code)

        if (viewModel.isFirstLanguage.value) {
            sharePreference.setIsFirstLang(false)
            startIntentRightToLeft(IntroActivity::class.java)
            finishAffinity()
        } else {
            startIntentWithClearTop(HomeActivity::class.java)
        }
    }

    @SuppressLint("MissingSuperCall", "GestureBackNavigation")
    override fun onBackPressed() {
        if (!viewModel.isFirstLanguage.value) {
            handleBackLeftToRight()
        } else {
            exitProcess(0)
        }
    }

    // Chỉ tắt nhạc khi là màn language đầu tiên
    override fun shouldPlayBackgroundMusic(): Boolean = false

    override fun initAds() {
//        Admob.getInstance().loadNativeAd(this@LanguageActivity, getString(R.string.native_language), binding.nativeAds, R.layout.ads_native_big_btn_top)
    }

}
