package com.therian.oc.aaa.ui.customize

import android.content.Context
import android.graphics.drawable.GradientDrawable
import androidx.core.graphics.toColorInt
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.therian.oc.aaa.core.base.BaseAdapter
import com.therian.oc.aaa.core.extensions.tap
import com.therian.oc.aaa.data.model.custom.ItemColorModel
import com.therian.oc.aaa.databinding.ItemColorBinding

class ColorLayerCustomizeAdapter(val context: Context) :
    BaseAdapter<ItemColorModel, ItemColorBinding>(ItemColorBinding::inflate) {
    var onItemClick: ((Int) -> Unit) = {}
    override fun onBind(binding: ItemColorBinding, item: ItemColorModel, position: Int) {
        binding.apply {
            imvImage.background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(item.color.toColorInt())
            }
            imvFocus.isVisible = item.isSelected
            root.tap {
                val rv = root.parent as? RecyclerView ?: return@tap
                val currentPosition = rv.getChildAdapterPosition(root)
                if (currentPosition != RecyclerView.NO_POSITION) {
                    onItemClick.invoke(currentPosition)
                }
            }
        }
    }
}