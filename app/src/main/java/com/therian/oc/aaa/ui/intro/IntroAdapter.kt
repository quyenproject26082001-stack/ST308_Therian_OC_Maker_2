package com.therian.oc.aaa.ui.intro

import android.content.Context
import com.therian.oc.aaa.core.base.BaseAdapter
import com.therian.oc.aaa.core.extensions.loadImage
import com.therian.oc.aaa.core.extensions.select
import com.therian.oc.aaa.core.extensions.strings
import com.therian.oc.aaa.data.model.IntroModel
import com.therian.oc.aaa.databinding.ItemIntroBinding

class IntroAdapter(val context: Context) : BaseAdapter<IntroModel, ItemIntroBinding>(
    ItemIntroBinding::inflate
) {
    override fun onBind(binding: ItemIntroBinding, item: IntroModel, position: Int) {
        binding.apply {
            loadImage(root, item.image, imvImage, false)
            tvContent.text = context.strings(item.content)
            tvContent.select()
        }
    }
}