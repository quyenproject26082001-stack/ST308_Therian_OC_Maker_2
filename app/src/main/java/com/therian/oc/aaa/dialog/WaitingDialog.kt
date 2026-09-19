package com.therian.oc.aaa.dialog

import android.animation.ObjectAnimator
import android.app.Activity
import android.view.animation.LinearInterpolator
import com.therian.oc.aaa.R
import com.therian.oc.aaa.core.base.BaseDialog
import com.therian.oc.aaa.databinding.DialogLoadingBinding

class WaitingDialog(val context: Activity) :
    BaseDialog<DialogLoadingBinding>(context, maxWidth = true, maxHeight = true) {
    override val layoutId: Int = R.layout.dialog_loading
    override val isCancelOnTouchOutside: Boolean = false
    override val isCancelableByBack: Boolean = false




    private var spinnerAnim: ObjectAnimator? = null

    override fun initView() {
        binding.tvTitle.isSelected = true

    }

    override fun onStart() {
        super.onStart()
        spinnerAnim = ObjectAnimator.ofFloat(binding.ivSpinner, "rotation", 0f, 360f).apply {
            duration = 1000
            repeatCount = ObjectAnimator.INFINITE
            interpolator = LinearInterpolator()
            start()
        }
    }

    override fun initAction() {}

    override fun onDismissListener() {
        spinnerAnim?.cancel()
        spinnerAnim = null
    }

}
