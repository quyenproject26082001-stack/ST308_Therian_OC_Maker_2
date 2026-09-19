package com.therian.oc.aaa.ui.success

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.therian.oc.aaa.core.extensions.shareImagesPaths
import com.therian.oc.aaa.core.helper.MediaHelper
import com.therian.oc.aaa.core.utils.key.ValueKey
import com.therian.oc.aaa.core.utils.state.HandleState
import com.therian.oc.aaa.data.model.custom.SuggestionModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SuccessViewModel : ViewModel() {
    private val _pathInternal = MutableStateFlow<String>("")
    val pathInternal: StateFlow<String> = _pathInternal.asStateFlow()

    fun setPath(path: String) {
        _pathInternal.value = path
    }

    fun downloadFiles(context: Activity): Flow<HandleState> = flow {
        emitAll(
            MediaHelper.downloadPartsToExternal(
                context, arrayListOf(_pathInternal.value)
            )
        )
    }

    fun shareFiles(context: Activity) {
        viewModelScope.launch {
            context.shareImagesPaths(arrayListOf(_pathInternal.value))
        }
    }

    fun saveToAvatar(context: android.content.Context, onDone: () -> Unit) {
        viewModelScope.launch {
            val path = _pathInternal.value
            if (path.isEmpty()) return@launch
            withContext(Dispatchers.IO) {
                android.util.Log.d("saveToAvatar", "pathInternal=$path")
                android.util.Log.d("saveToAvatar", "file exists=${java.io.File(path).exists()} size=${java.io.File(path).length()}")
                val suggestion = MediaHelper.readModelFromFile<SuggestionModel>(
                    context, ValueKey.SUGGESTION_FILE_INTERNAL
                ) ?: SuggestionModel()
                android.util.Log.d("saveToAvatar", "suggestion.pathInternalRandom=${suggestion.pathInternalRandom}")
                android.util.Log.d("saveToAvatar", "suggestion.itemNavList.size=${suggestion.itemNavList.size}")
                suggestion.pathInternalEdit = path
                val editList = MediaHelper
                    .readListFromFile<SuggestionModel>(context, ValueKey.EDIT_FILE_INTERNAL)
                    .toCollection(ArrayList())
                editList.add(0, suggestion)
                MediaHelper.writeListToFile(context, ValueKey.EDIT_FILE_INTERNAL, editList)
                android.util.Log.d("saveToAvatar", "saved OK, editList.size=${editList.size}")
            }
            onDone()
        }
    }
}
