package com.wa2c.android.cifsdocumentsprovider.presentation.ui.folder

import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import com.wa2c.android.cifsdocumentsprovider.common.utils.parentUri
import com.wa2c.android.cifsdocumentsprovider.domain.model.CifsConnection
import com.wa2c.android.cifsdocumentsprovider.domain.model.CifsFile
import com.wa2c.android.cifsdocumentsprovider.domain.repository.CifsRepository
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
    private val cifsRepository: CifsRepository
): ViewModel(), CoroutineScope by MainCoroutineScope() {

    private val temporaryConnection: CifsConnection
        get() = cifsRepository.loadTemporaryConnection() ?: throw IllegalStateException()


    private val _isLoading =  MutableStateFlow<Boolean>(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _currentUri = MutableStateFlow<Uri>(temporaryConnection.folderSmbUri.toUri())
    val currentUri: StateFlow<Uri> = _currentUri

    private val _fileList = MutableStateFlow<List<CifsFile>>(emptyList())
    val fileList: StateFlow<List<CifsFile>> = _fileList

    private val _result = MutableSharedFlow<Result<Unit>>()
    val result: SharedFlow<Result<Unit>> = _result

    init {
        launch {
            loadList(temporaryConnection.folderSmbUri.toUri())
        }
    }

    /**
     * On select folder
     */
    fun onSelectFolder(file: CifsFile) {
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
    private suspend fun loadList(uri: Uri) {
        _isLoading.emit(true)
        runCatching {
            cifsRepository.getFileChildren(uri.toString(), temporaryConnection)
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
