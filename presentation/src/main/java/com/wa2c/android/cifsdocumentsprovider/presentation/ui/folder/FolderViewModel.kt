package com.wa2c.android.cifsdocumentsprovider.presentation.ui.folder

import androidx.lifecycle.ViewModel
import com.wa2c.android.cifsdocumentsprovider.domain.model.RemoteConnection
import com.wa2c.android.cifsdocumentsprovider.domain.model.RemoteFile
import com.wa2c.android.cifsdocumentsprovider.domain.model.StorageUri
import com.wa2c.android.cifsdocumentsprovider.domain.repository.EditRepository
import com.wa2c.android.cifsdocumentsprovider.presentation.ext.MainCoroutineScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

/**
 * Folder Screen ViewModel
 */
@HiltViewModel
class FolderViewModel @Inject constructor(
    private val editRepository: EditRepository
): ViewModel(), CoroutineScope by MainCoroutineScope() {

    private val temporaryConnection: RemoteConnection by lazy {
        editRepository.loadTemporaryConnection() ?: throw IllegalStateException()
    }

    private val rootConnection = temporaryConnection.copy(folder = null)

    private val _currentUri = MutableStateFlow<StorageUri>(temporaryConnection.uri)
    val currentUri: StateFlow<StorageUri> = _currentUri

    private val _fileList = MutableStateFlow<List<RemoteFile>>(emptyList())
    val fileList: StateFlow<List<RemoteFile>> = _fileList

    private val _result = MutableSharedFlow<Result<Unit>>()
    val result: SharedFlow<Result<Unit>> = _result

    private val _isLoading =  MutableStateFlow<Boolean>(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        launch {
            loadList(currentUri.value)
        }
    }

    /**
     * On select folder
     */
    fun onSelectFolder(file: RemoteFile) {
        if (isLoading.value) return
        launch {
            loadList(file.uri)
        }
    }

    /**
     * on up folder
     * @return true if not root
     */
    fun onUpFolder(): Boolean {
        val uri = currentUri.value.parentUri ?: return false
        launch {
            loadList(uri)
        }
        return true
    }


    /**
     * Reload current folder
     */
    fun onClickReload() {
        launch {
            loadList(currentUri.value)
        }
    }

    /**
     * Load list
     */
    private suspend fun loadList(uri: StorageUri) {
        _isLoading.emit(true)
        runCatching {
            editRepository.getFileChildren(rootConnection, uri)
        }.onSuccess { list ->
            _fileList.emit(list.filter { it.isDirectory }.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name }))
        }.onFailure {
            _fileList.emit(emptyList())
            _result.emit(Result.failure(it))
        }.also {
            _currentUri.emit(uri)
            _isLoading.emit(false)
        }
    }

    override fun onCleared() {
        runBlocking {
            _isLoading.emit(false)
        }
        super.onCleared()
    }

}
