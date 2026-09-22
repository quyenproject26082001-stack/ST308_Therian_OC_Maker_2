package com.therian.oc.aaa.ui.add_character.adapter

import android.annotation.SuppressLint
import androidx.core.content.ContextCompat
import com.therian.oc.aaa.R
import com.therian.oc.aaa.core.base.BaseAdapter
import com.therian.oc.aaa.core.extensions.tap
import com.therian.oc.aaa.data.model.AddCharacterCategoryModel
import com.therian.oc.aaa.databinding.ItemCategoryBinding

class AddCharacterCategoryAdapter :
    BaseAdapter<AddCharacterCategoryModel, ItemCategoryBinding>(ItemCategoryBinding::inflate) {

    var onCategoryClick: ((Int) -> Unit) = {}

    @SuppressLint("NotifyDataSetChanged")
    override fun submitList(list: List<AddCharacterCategoryModel>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onBind(
        binding: ItemCategoryBinding,
        item: AddCharacterCategoryModel,
        position: Int
    ) {
        binding.apply {
            tvCategory.text = item.name
            tvCategory.isSelected = true
            tvCategory.setBackgroundResource(
                if (item.isSelected) R.drawable.slt_tab_category else R.drawable.uslt_tab_cate
            )
            tvCategory.setTextColor(
                ContextCompat.getColor(
                    root.context,
                    if (item.isSelected) R.color.figma_primary else R.color.white
                )
            )
            root.tap { onCategoryClick(position) }
        }
    }
}
