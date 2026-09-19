package com.therian.oc.aaa.ui.choose_character

import androidx.core.content.ContextCompat
import com.therian.oc.aaa.R
import com.therian.oc.aaa.core.base.BaseAdapter
import com.therian.oc.aaa.core.extensions.gone
import com.therian.oc.aaa.core.extensions.loadImage
import com.therian.oc.aaa.core.extensions.tap
import com.therian.oc.aaa.data.model.custom.CustomizeModel
import com.therian.oc.aaa.databinding.ItemChooseAvatarBinding

class ChooseCharacterAdapter : BaseAdapter<CustomizeModel, ItemChooseAvatarBinding>(ItemChooseAvatarBinding::inflate) {
    var onItemClick: ((position: Int) -> Unit) = {}


    override fun onBind(binding: ItemChooseAvatarBinding, item: CustomizeModel, position: Int) {
        binding.apply {

            loadImage(item.avatar, imvImage, onDismissLoading = {
                sflShimmer.stopShimmer()
                sflShimmer.gone()
            })
            root.tap { onItemClick.invoke(position) }
        }
    }
}