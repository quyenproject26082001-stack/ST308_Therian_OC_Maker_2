package com.therian.oc.aaa.ui.random_character

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.util.Log
import com.bumptech.glide.Glide
import com.therian.oc.aaa.core.base.BaseAdapter
import com.therian.oc.aaa.core.utils.key.ValueKey
import com.therian.oc.aaa.data.model.custom.SuggestionModel
import com.therian.oc.aaa.databinding.ItemRandomCharacterBinding
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.core.graphics.createBitmap
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.therian.oc.aaa.core.extensions.gone
import com.therian.oc.aaa.core.extensions.invisible
import com.therian.oc.aaa.core.extensions.tap
import com.therian.oc.aaa.core.extensions.visible
import com.therian.oc.aaa.core.helper.MediaHelper
import com.therian.oc.aaa.core.utils.state.SaveState


class RandomCharacterAdapter(val context: Context) :
    BaseAdapter<SuggestionModel, ItemRandomCharacterBinding>(ItemRandomCharacterBinding::inflate) {
    var onItemClick: ((SuggestionModel) -> Unit) = {}

    // Track active jobs per ViewHolder (by root view) to cancel them when recycled
    private val activeJobs = mutableMapOf<android.view.View, kotlinx.coroutines.Job>()

    override fun onBind(binding: ItemRandomCharacterBinding, item: SuggestionModel, position: Int) {
        binding.apply {
            Log.d("RandomAdapter", "========================================")
            Log.d("RandomAdapter", "onBind position: $position")
            Log.d("RandomAdapter", "Avatar path: ${item.avatarPath}")
            Log.d("RandomAdapter", "Selected paths count: ${item.pathSelectedList.size}")
            Log.d("RandomAdapter", "Internal random path: ${item.pathInternalRandom}")

            // Cancel any existing job for this ViewHolder
            activeJobs[root]?.cancel()

            // ✅ OPTIMIZATION: If already processed, just load the cached image
            if (item.pathInternalRandom.isNotEmpty()) {
                Log.d("RandomAdapter", "⚡ CACHED - Loading from: ${item.pathInternalRandom}")
                sflShimmer.gone()
                sflShimmer.stopShimmer()
                imvImage.visible()
                Glide.with(root).load(item.pathInternalRandom).into(imvImage)
                root.tap { onItemClick.invoke(item) }
                return@apply
            }

            sflShimmer.visible()
            sflShimmer.startShimmer()
            imvImage.invisible()

            var width = ValueKey.WIDTH_BITMAP
            var height = ValueKey.HEIGHT_BITMAP

            val listBitmap: ArrayList<Bitmap> = arrayListOf()
            val handleExceptionCoroutine = CoroutineExceptionHandler { _, throwable ->
                Log.e("RandomAdapter", "✗ ERROR at position $position: ${throwable.message}")
                throwable.printStackTrace()
            }
            // ✅ Store the job so we can cancel it if the view is recycled
            val job = CoroutineScope(SupervisorJob() + Dispatchers.IO + handleExceptionCoroutine).launch {
                val job1 = async {
                    if (item.pathSelectedList.isEmpty()) {
                        Log.e("RandomAdapter", "✗ position $position: pathSelectedList is EMPTY → cannot render")
                        return@async false
                    }
                    Log.d("RandomAdapter", "Loading first layer: ${item.pathSelectedList.first()}")
                    val bitmapDefault = Glide.with(context).asBitmap().load(item.pathSelectedList.first()).submit().get()
                    width = bitmapDefault.width/2 ?: ValueKey.WIDTH_BITMAP
                    height = bitmapDefault.height/2 ?: ValueKey.HEIGHT_BITMAP
                    Log.d("RandomAdapter", "Bitmap size: ${width}x${height}")

                    if (item.pathInternalRandom == ""){
                        Log.d("RandomAdapter", "Loading ${item.pathSelectedList.size} layers...")
                        item.pathSelectedList.forEachIndexed { idx, path ->
                            Log.d("RandomAdapter", "Loading layer $idx: $path")
                            listBitmap.add(Glide.with(context).asBitmap().load(path).submit(width, height).get())
                        }
                        Log.d("RandomAdapter", "✓ All layers loaded successfully")
                    }
                    return@async true
                }

                withContext(Dispatchers.Main) {
                    if (job1.await()) {
                        Log.d("RandomAdapter", "job1 done at position $position, pathInternalRandom='${item.pathInternalRandom}'")
                        if (item.pathInternalRandom == ""){
                            Log.d("RandomAdapter", "Creating combined bitmap...")
                            val combinedBitmap = createBitmap(width, height)
                            val canvas = Canvas(combinedBitmap)

                            for (i in 0 until listBitmap.size) {
                                val bitmap = listBitmap[i]
                                val left = (width - bitmap.width) / 2f
                                val top = (height - bitmap.height) / 2f
                                canvas.drawBitmap(bitmap, left, top, null)
                            }

                            MediaHelper.saveBitmapToPreviewCache(context, combinedBitmap).collect { state ->
                                when(state){
                                    is SaveState.Loading -> {
                                        Log.d("RandomAdapter", "Saving bitmap to internal storage...")
                                    }
                                    is SaveState.Error -> {
                                        Log.e("RandomAdapter", "✗ Failed to save bitmap: ${state.exception.message}")
                                    }
                                    is SaveState.Success -> {
                                        item.pathInternalRandom = state.path
                                        Log.d("RandomAdapter", "✓ Bitmap saved: ${state.path}")
                                    }
                                }
                            }
                        }


                        Log.d("RandomAdapter", "Loading final image from: ${item.pathInternalRandom}")
                        Glide.with(root).load(item.pathInternalRandom).listener(object : RequestListener<Drawable> {
                            override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable?>, isFirstResource: Boolean): Boolean {
                                Log.e("RandomAdapter", "✗ Glide load FAILED at position $position: ${e?.message}")
                                e?.logRootCauses("RandomAdapter")
                                sflShimmer.stopShimmer()
                                sflShimmer.gone()
                                return false
                            }

                            override fun onResourceReady(resource: Drawable, model: Any, target: Target<Drawable?>?, dataSource: DataSource, isFirstResource: Boolean): Boolean {
                                Log.d("RandomAdapter", "✓ Image loaded successfully at position $position")
                                sflShimmer.stopShimmer()
                                sflShimmer.gone()
                                imvImage.visible()
                                return false
                            }
                        }).into(imvImage)
                    }
                }
            }

            activeJobs[root] = job

            root.tap { onItemClick.invoke(item) }
        }
    }

    override fun onViewRecycled(holder: BaseAdapter<SuggestionModel, ItemRandomCharacterBinding>.BaseViewHolder) {
        super.onViewRecycled(holder)
        activeJobs[holder.binding.root]?.cancel()
        activeJobs.remove(holder.binding.root)
    }

    fun cancelAllJobs() {
        activeJobs.values.forEach { it.cancel() }
        activeJobs.clear()
    }
}
