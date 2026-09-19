package com.therian.oc.aaa.dialog

import android.app.Activity
import android.graphics.Bitmap
import com.therian.oc.aaa.R
import com.therian.oc.aaa.core.base.BaseDialog
import com.therian.oc.aaa.core.extensions.hideNavigation
import com.therian.oc.aaa.core.extensions.loadImage
import com.therian.oc.aaa.core.extensions.tap
import com.therian.oc.aaa.databinding.DialogResultBinding

class CosplayResultDialog(
    private val context: Activity,
    private val playerBitmap: Bitmap,
    private val samplePath: String,
    private val progress: Int
) : BaseDialog<DialogResultBinding>(context, maxWidth = true, maxHeight = true) {

    override val layoutId: Int = R.layout.dialog_result
    override val isCancelOnTouchOutside: Boolean = false
    override val isCancelableByBack: Boolean = false

    var onNextClick: () -> Unit = {}

    override fun initView() {
        context.hideNavigation()
        binding.btnImage.setImageBitmap(playerBitmap)
        if (samplePath.isNotEmpty()) {
            loadImage(context, samplePath, binding.btnSamplePhoto)
        }
        binding.tvRef.text = context.getString(R.string._100_match)
            .replace("100%", "${progress.coerceIn(0, 100)}%")
        binding.tvTitle.isSelected = true
    }

    override fun initAction() {
        binding.btnNext.tap { onNextClick() }
    }

    override fun onDismissListener() = Unit
}
