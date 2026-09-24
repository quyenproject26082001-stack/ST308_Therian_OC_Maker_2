package com.therian.oc.aaa.ui.my_creation.adapter

import android.content.Context
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.signature.ObjectKey
import androidx.core.content.ContextCompat
import com.therian.oc.aaa.R
import com.therian.oc.aaa.core.base.BaseAdapter
import com.therian.oc.aaa.core.extensions.gone
import com.therian.oc.aaa.core.extensions.tap
import com.therian.oc.aaa.core.extensions.visible
import com.therian.oc.aaa.data.model.MyAlbumModel
import com.therian.oc.aaa.databinding.ItemMyAlbumBinding
import com.google.protobuf.duration
import java.io.File

class MyAvatarAdapter(val context: Context) :
    BaseAdapter<MyAlbumModel, ItemMyAlbumBinding>(ItemMyAlbumBinding::inflate) {
    var onItemClick: ((String) -> Unit) = {}
    var onLongClick: ((Int) -> Unit) = {}
    var onItemTick: ((Int) -> Unit) = {}

    var onEditClick: ((String) -> Unit) = {}
    var onDeleteClick: ((String) -> Unit) = {}

    var isSelectMode: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                // Only notify item changes if needed, not full refresh
                notifyItemRangeChanged(0, itemCount)
            }
        }

    // DiffUtil optimization
    override fun areItemsTheSame(oldItem: MyAlbumModel, newItem: MyAlbumModel): Boolean {
        return oldItem.path == newItem.path
    }

    override fun areContentsTheSame(oldItem: MyAlbumModel, newItem: MyAlbumModel): Boolean {
        return oldItem == newItem
    }

    override fun onBind(binding: ItemMyAlbumBinding, item: MyAlbumModel, position: Int) {
        binding.apply {

            // Optimized Glide loading with thumbnail, size override, and caching
            val file = File(item.path)
            val imageSource: Any = item.previewResId ?: file
            Glide.with(context)
                .load(imageSource)
                .thumbnail(0.1f) // Load 10% quality thumbnail first
                .override(256, 256)
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .signature(ObjectKey(item.previewResId ?: file.lastModified())) // Cache invalidation
                .into(imvImage)

            if (item.isShowSelection) {
                btnSelect.visible()
                btnEdit.gone()
                btnDelete.gone()
            } else {
                btnSelect.gone()
                btnEdit.visible()
                btnDelete.visible()
            }

            if (item.isSelected) {
                btnSelect.setImageResource(R.drawable.ic_selected)
                frameCover.visible()
            } else {
                btnSelect.setImageResource(R.drawable.ic_not_select)
                frameCover.gone()
            }

            root.tap {
                if (!item.isFake) onItemClick.invoke(item.path)
            }

            root.setOnLongClickListener {
                if (items.any { album -> album.isShowSelection }) return@setOnLongClickListener false
                val (rv, rvChild) = findRecyclerView(root) ?: return@setOnLongClickListener false
                val actualPos = rv.getChildAdapterPosition(rvChild)
                if (actualPos == RecyclerView.NO_POSITION) return@setOnLongClickListener false
                onLongClick.invoke(actualPos)
                true
            }
            btnEdit.tap {
                if (!item.isFake) {
                    onEditClick.invoke(item.path)
                }
            }
            btnDelete.tap {
                if (!item.isFake) onDeleteClick.invoke(item.path)
            }
            btnSelect.tap {
                val (rv, rvChild) = findRecyclerView(root) ?: return@tap
                val actualPos = rv.getChildAdapterPosition(rvChild)
                if (actualPos != RecyclerView.NO_POSITION) onItemTick.invoke(actualPos)
            }
        }
    }

    override fun onViewRecycled(
        holder: BaseAdapter<MyAlbumModel, ItemMyAlbumBinding>.BaseViewHolder
    ) {
        Glide.with(holder.binding.root).clear(holder.binding.imvImage)
        super.onViewRecycled(holder)
    }

    private fun findRecyclerView(view: android.view.View): Pair<RecyclerView, android.view.View>? {
        var child: android.view.View = view
        var parent = view.parent
        while (parent != null) {
            if (parent is RecyclerView) return Pair(parent, child)
            child = parent as? android.view.View ?: return null
            parent = child.parent
        }
        return null
    }
}
