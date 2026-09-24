package com.therian.oc.aaa.ui.my_creation.fragment

import android.app.ActivityOptions
import android.content.Intent
import androidx.core.app.ActivityOptionsCompat
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import com.therian.oc.aaa.R
import com.therian.oc.aaa.core.base.BaseFragment
import com.therian.oc.aaa.core.extensions.hideNavigation
import com.therian.oc.aaa.core.extensions.select
import com.therian.oc.aaa.core.extensions.showInterAll
import com.therian.oc.aaa.core.extensions.tap
import com.therian.oc.aaa.core.extensions.visible
import com.therian.oc.aaa.core.helper.LanguageHelper
import com.therian.oc.aaa.core.utils.key.IntentKey
import com.therian.oc.aaa.core.utils.key.ValueKey
import com.therian.oc.aaa.core.utils.state.HandleState
import com.therian.oc.aaa.databinding.FragmentMyDesignBinding
import com.therian.oc.aaa.dialog.YesNoDialog
import com.therian.oc.aaa.ui.customize.CustomizeCharacterActivity
import com.therian.oc.aaa.ui.home.DataViewModel
import com.therian.oc.aaa.ui.my_creation.MyCreationActivity
import com.therian.oc.aaa.ui.my_creation.view_model.MyCreationViewModel
import com.therian.oc.aaa.ui.my_creation.adapter.MyAvatarAdapter
import com.therian.oc.aaa.ui.my_creation.adapter.MyDesignAdapter
import com.therian.oc.aaa.ui.my_creation.view_model.MyAvatarViewModel
import com.therian.oc.aaa.ui.my_creation.view_model.MyDesignViewModel
import com.therian.oc.aaa.ui.view.ViewActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.getValue

class MyDesignFragment : BaseFragment<FragmentMyDesignBinding>() {
    private val viewModel: MyDesignViewModel by viewModels()
    private val myCreationViewModel: MyCreationViewModel by activityViewModels()
    private val myDesignAdapter by lazy { MyDesignAdapter() }

    private val myAlbumActivity: MyCreationActivity
        get() = requireActivity() as MyCreationActivity

    override fun setViewBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentMyDesignBinding {
        return FragmentMyDesignBinding.inflate(inflater, container, false)
    }

    override fun initView() {
        binding.tvNoitem.select()
        initRcv()
        // ✅ FIX: Removed redundant load - onStart() will handle it
        android.util.Log.d("MyDesignFragment", "initView() - NOT loading data (onStart will do it)")
    }

    override fun dataObservable() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.myDesignList.collect { list ->
                        myDesignAdapter.submitList(list)
                        binding.layoutNoItem.isVisible = list.isEmpty()
                        if (myCreationViewModel.typeStatus.value == ValueKey.MY_DESIGN_TYPE) {
                            myAlbumActivity.refreshBottomVisibility()
                        }
                    }
                }
                // Removed - action bar buttons are disabled
                // launch {
                //     viewModel.isLastItem.collect { selectStatus ->
                //         myAlbumActivity.changeImageActionBarRight(selectStatus)
                //     }
                // }
            }
        }
    }

    override fun viewListener() {
        binding.apply {
            rcvMyDesign.addOnItemTouchListener(object : RecyclerView.OnItemTouchListener {
                override fun onInterceptTouchEvent(
                    recyclerView: RecyclerView, motionEvent: MotionEvent
                ): Boolean {
                    return when {
                        motionEvent.action != MotionEvent.ACTION_UP || recyclerView.findChildViewUnder(
                            motionEvent.x, motionEvent.y
                        ) != null -> false

                        else -> {
                            resetSelectionMode()
                            true
                        }
                    }
                }

                override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {}
                override fun onTouchEvent(recyclerView: RecyclerView, motionEvent: MotionEvent) {}
            })
            // Action bar buttons disabled - no delete button for MyDesign tab yet
            // myAlbumActivity.binding.actionBar.btnActionBarRight.tap { handleSelectAll() }
            // myAlbumActivity.binding.actionBar.btnActionBarNextToRight.tap { handleDelete(viewModel.getPathSelected()) }

            // Share and Download buttons are handled in MyCreationActivity

            myDesignAdapter.onItemClick = { pathInternal -> handleItemClick(pathInternal) }
            myDesignAdapter.onItemTick = { position ->
                viewModel.toggleSelect(position)
                // Check if all items are now selected and update the icon
                val allSelected = viewModel.myDesignList.value.all { it.isSelected }
                myAlbumActivity.updateSelectAllIcon(allSelected)
            }
            myDesignAdapter.onDeleteClick = { pathInternal -> handleDelete(arrayListOf(pathInternal)) }
            myDesignAdapter.onLongClick = { position -> handleLongClick(position) }
        }
    }

    private fun initRcv() {
        binding.apply {
            rcvMyDesign.apply {
                adapter = myDesignAdapter
                itemAnimator = null
                setHasFixedSize(true)
                setItemViewCacheSize(4)
                isNestedScrollingEnabled = true
            }
        }
    }

    private fun handleDelete(pathInternalList: ArrayList<String>) {
        if (pathInternalList.isEmpty()) {
            myAlbumActivity.showToast(R.string.please_select_an_image)
            return
        }
        val dialog = YesNoDialog(myAlbumActivity, R.string.delete, R.string.are_you_sure_want_to_delete_this_item)
        LanguageHelper.setLocale(myAlbumActivity)
        dialog.show()
        dialog.onDismissClick = {
            dialog.dismiss()
            myAlbumActivity.hideNavigation()
        }
        dialog.onNoClick = {
            dialog.dismiss()
            myAlbumActivity.hideNavigation()
        }
        dialog.onYesClick = {
            lifecycleScope.launch(Dispatchers.IO) {
                viewModel.deleteItem(myAlbumActivity, pathInternalList)
                withContext(Dispatchers.Main) {
                    dialog.dismiss()
                    myAlbumActivity.hideNavigation()
                    resetSelectionMode()
                }
            }
        }
    }

    private fun handleItemClick(pathInternal: String) {
        if (myDesignAdapter.items.any { it.isShowSelection }) {
            // In selection mode - reset before navigating
            resetSelectionMode()
        }
        val intent = Intent(myAlbumActivity, ViewActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(IntentKey.INTENT_KEY, pathInternal)
            putExtra(IntentKey.TYPE_KEY, ValueKey.TYPE_VIEW)
            putExtra(IntentKey.STATUS_KEY, ValueKey.MY_DESIGN_TYPE)
        }
        val options = ActivityOptionsCompat.makeCustomAnimation(myAlbumActivity, R.anim.slide_in_right, R.anim.slide_out_left)
        myAlbumActivity.showInterAll { myAlbumActivity.launchViewActivity(intent, options) }
    }

    private fun handleLongClick(position: Int) {
        if (position >= viewModel.myDesignList.value.size) return
        viewModel.showLongClick(position)
        binding.rcvMyDesign.clipToPadding = true
        myAlbumActivity.enterSelectionMode()

        // Check if all items are now selected (e.g., if there's only 1 item)
        val allSelected = viewModel.myDesignList.value.all { it.isSelected }
        myAlbumActivity.updateSelectAllIcon(allSelected)

        val list = viewModel.myDesignList.value
        android.util.Log.d("SelectState", "=== rcvDesign (MyDesign) longClick pos=$position | total=${list.size} ===")
        list.forEachIndexed { i, item ->
            android.util.Log.d("SelectState", "  [$i] isSelected=${item.isSelected} | isShowSelection=${item.isShowSelection} | path=${item.path}")
        }
    }

    private fun resetData() {
        viewModel.loadMyDesign(myAlbumActivity)
        myAlbumActivity.exitSelectionMode()
    }

    fun getAllPaths(): ArrayList<String> {
        return viewModel.myDesignList.value
            .filterNot { it.isFake }
            .map { it.path }
            .toCollection(ArrayList())
    }

    fun getSelectedPaths(): ArrayList<String> {
        return viewModel.getPathSelected()
    }

    fun deleteSelectedItems() {
        handleDelete(viewModel.getPathSelected())
    }

    fun selectAllItems() {
        viewModel.selectAll(true)
        // Use notifyItemRangeChanged instead of notifyDataSetChanged for better performance
        myDesignAdapter.notifyItemRangeChanged(0, myDesignAdapter.itemCount)
    }

    fun deselectAllItems() {
        viewModel.selectAll(false)
        // Use notifyItemRangeChanged instead of notifyDataSetChanged for better performance
        myDesignAdapter.notifyItemRangeChanged(0, myDesignAdapter.itemCount)
    }

    fun resetSelectionMode() {
        viewModel.clearSelection()
        binding.rcvMyDesign.clipToPadding = false
        myAlbumActivity.exitSelectionMode()
        // notifyItemRangeChanged not needed - data will refresh automatically
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            myAlbumActivity.refreshBottomVisibility()
        }
    }

    override fun onStart() {
        super.onStart()
        if (!isHidden) {
            resetData()
        }
    }
}
