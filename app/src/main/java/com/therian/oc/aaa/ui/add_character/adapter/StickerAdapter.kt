package com.therian.oc.aaa.ui.add_character.adapter

import com.therian.oc.aaa.core.base.BaseAdapter
import com.therian.oc.aaa.core.extensions.loadImage
import com.therian.oc.aaa.core.extensions.loadImageSticker
import com.therian.oc.aaa.core.extensions.tap
import com.therian.oc.aaa.data.model.SelectedModel
import com.therian.oc.aaa.databinding.ItemStickerBinding

class StickerAdapter : BaseAdapter<SelectedModel, ItemStickerBinding>(ItemStickerBinding::inflate) {
    var onItemClick : ((String) -> Unit) = {}
    override fun onBind(binding: ItemStickerBinding, item: SelectedModel, position: Int) {
        binding.apply {
            loadImageSticker(root, item.path, imvSticker)
            root.tap { onItemClick.invoke(item.path) }
        }
    }
}