package com.therian.oc.aaa.ui.my_creation.view_model

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.therian.oc.aaa.core.helper.MediaHelper
import com.therian.oc.aaa.core.utils.key.ValueKey
import com.therian.oc.aaa.data.local.PersistenceRepository
import com.therian.oc.aaa.data.model.MyAlbumModel
import com.therian.oc.aaa.ui.my_creation.MyCreationPreviewConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MyDesignViewModel : ViewModel() {
    private val _myDesignList = MutableStateFlow<ArrayList<MyAlbumModel>>(arrayListOf())
    val myDesignList = _myDesignList.asStateFlow()
    private val _isLastItem = MutableStateFlow<Boolean>(false)
    val isLastItem: StateFlow<Boolean> = _isLastItem
    private var loadJob: Job? = null

    fun loadMyDesign(context: Context) {
        if (MyCreationPreviewConfig.SHOW_FAKE_ITEMS) {
            loadJob?.cancel()
            _myDesignList.value = MyCreationPreviewConfig.createFakeItems("design")
            checkLastItem()
            return
        }
        val appContext = context.applicationContext
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            try {
                val paths = PersistenceRepository.getAlbumPathsAsync(
                    appContext,
                    ValueKey.DOWNLOAD_ALBUM
                )
                _myDesignList.value =
                    paths.map { MyAlbumModel(it) }.toCollection(ArrayList())
                checkLastItem()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                android.util.Log.e("MyDesignViewModel", "Failed to load saved designs", error)
                _myDesignList.value = arrayListOf()
                checkLastItem()
            }
        }
    }

    fun showLongClick(positionSelect: Int) {
        _myDesignList.value = _myDesignList.value.mapIndexed { position, item ->
            item.copy(isSelected = position == positionSelect, isShowSelection = true)
        }.toCollection(ArrayList())
        checkLastItem()
    }

    private fun checkLastItem() {
        _isLastItem.value = _myDesignList.value.any { !it.isSelected }
    }

    suspend fun deleteItem(context: Context, pathList: ArrayList<String>) {
        loadJob?.cancel()
        MediaHelper.deleteFileByPathNotFlow(pathList, context)
        _myDesignList.value = _myDesignList.value
            .filterNot { it.path in pathList }
            .toCollection(ArrayList())
        checkLastItem()
    }

    fun toggleSelect(position: Int) {
        val list = _myDesignList.value.toMutableList()
        list[position] = list[position].copy(isSelected = !list[position].isSelected, isShowSelection = true)
        _myDesignList.value = list.toCollection(ArrayList())
        checkLastItem()
    }

    fun selectAll(shouldSelect: Boolean) {
        _myDesignList.value = _myDesignList.value.map {
            it.copy(isSelected = shouldSelect, isShowSelection = true)
        }.toCollection(ArrayList())
        checkLastItem()
    }

    fun getPathSelected() : ArrayList<String>{
        return _myDesignList.value.filter { it.isSelected && !it.isFake }.map { it.path }.toCollection(ArrayList())
    }

    fun clearSelection() {
        _myDesignList.value = _myDesignList.value.map {
            it.copy(isSelected = false, isShowSelection = false)
        }.toCollection(ArrayList())
        checkLastItem()
    }
}
