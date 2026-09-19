package com.therian.oc.aaa.ui

import android.view.LayoutInflater
import com.therian.oc.aaa.R
import com.therian.oc.aaa.core.base.BaseActivity
import com.therian.oc.aaa.core.extensions.gone
import com.therian.oc.aaa.core.extensions.handleBackLeftToRight
import com.therian.oc.aaa.core.extensions.policy
import com.therian.oc.aaa.core.extensions.select
import com.therian.oc.aaa.core.extensions.setImageActionBar
import com.therian.oc.aaa.core.extensions.setTextActionBar
import com.therian.oc.aaa.core.extensions.shareApp
import com.therian.oc.aaa.core.extensions.startIntentRightToLeft
import com.therian.oc.aaa.core.extensions.visible
import com.therian.oc.aaa.core.utils.key.IntentKey
import com.therian.oc.aaa.core.utils.state.RateState
import com.therian.oc.aaa.databinding.ActivitySettingsBinding
import com.therian.oc.aaa.ui.language.LanguageActivity
import com.therian.oc.aaa.core.extensions.tap
import com.therian.oc.aaa.core.helper.MusicHelper
import com.therian.oc.aaa.core.helper.RateHelper
import kotlin.jvm.java

class SettingsActivity : BaseActivity<ActivitySettingsBinding>() {
    override fun setViewBinding(): ActivitySettingsBinding {
        return ActivitySettingsBinding.inflate(LayoutInflater.from(this))
    }

    override fun initView() {
       // binding.tvMusic.select()
        initRate()
        initMusic()
        binding.actionBar.tvCenter.select()
    }

    private fun initMusic() {
        updateMusicUI(sharePreference.isMusicEnabled())
    }

    private fun updateMusicUI(isEnabled: Boolean) {
      //  binding.btnMusic.setImageResource(
//            if (isEnabled) R.drawable.ic_sw_on else R.drawable.ic_sw_off_ms
//        )
    }

    private fun toggleMusic() {
        val isEnabled = !sharePreference.isMusicEnabled()
        sharePreference.setMusicEnabled(isEnabled)
        updateMusicUI(isEnabled)
        if (isEnabled) {
            MusicHelper.play()
        } else {
            MusicHelper.pause()
        }
    }

    override fun viewListener() {
        binding.apply {
            actionBar.btnActionBarLeft.tap { handleBackLeftToRight() }
        //    layoutMusic.tap { toggleMusic() }
            btnLang.tap { startIntentRightToLeft(LanguageActivity::class.java, IntentKey.INTENT_KEY) }
            btnShareApp.tap(1500) { shareApp() }
            btnRate.tap {
                RateHelper.showRateDialog(this@SettingsActivity, sharePreference){ state ->
                    if (state != RateState.CANCEL){
                        btnRate.gone()
                        showToast(R.string.have_rated)
                    }
                }
            }
            btnPolicy.tap(1500) { policy() }
        }
    }

    override fun initText() {
        binding.actionBar.tvCenter.select()
    }

    override fun initActionBar() {
        binding.actionBar.apply {
            setImageActionBar(btnActionBarLeft, R.drawable.ic_back)
            setTextActionBar(tvCenter, getString(R.string.settings))
            tvStart.gone()
        }
    }

    private fun initRate() {
        if (sharePreference.getIsRate(this)) {
            binding.btnRate.gone()
        } else {
            binding.btnRate.visible()
        }
    }
}
