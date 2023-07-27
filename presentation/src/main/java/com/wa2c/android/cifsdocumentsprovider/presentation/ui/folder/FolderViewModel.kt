package com.wa2c.android.cifsdocumentsprovider.presentation.ui.folder

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.wa2c.android.cifsdocumentsprovider.domain.model.CifsFile
import com.wa2c.android.cifsdocumentsprovider.domain.repository.CifsRepository
import com.wa2c.android.cifsdocumentsprovider.presentation.ext.MainCoroutineScope
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.FolderScreenParamUri
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
    private val savedStateHandle: SavedStateHandle,
    private val cifsRepository: CifsRepository
): ViewModel(), CoroutineScope by MainCoroutineScope() {

    private val paramUri: String = checkNotNull(savedStateHandle[FolderScreenParamUri])

    private val _isLoading =  MutableStateFlow<Boolean>(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _fileList = MutableStateFlow<List<CifsFile>>(emptyList())
    val fileList: StateFlow<List<CifsFile>> = _fileList

    private val _currentFile = MutableStateFlow<CifsFile?>(null)
    val currentFile: StateFlow<CifsFile?> = _currentFile

    private val _result = MutableSharedFlow<Result<Unit>>()
    val result: SharedFlow<Result<Unit>> = _result

    init {
        launch {
            loadList(paramUri)
        }
    }

    /**
     * On select folder
     */
    fun onSelectFolder(file: CifsFile) {
        if (isLoading.value) return
        launch {
            loadList(file)
        }
    }

    /**
     * on up folder
     * @return true if not root
     */
    fun onUpFolder(): Boolean {
        if (currentFile.value?.isRoot == true) return false
        if (isLoading.value) return true

        launch {
            val uri = currentFile.value?.parentUri ?: return@launch
            loadList(uri.toString())
        }
        return true
    }


    /**
     * Reload current folder
     */
    fun onClickReload() {
        val file = currentFile.value ?: return
        if (isLoading.value) return

        launch {
            loadList(file)
        }
    }

    /**
     * Load list
     */
    private suspend fun loadList(uri: String) {
        _isLoading.emit(true)
        runCatching {
            cifsRepository.getFile(uri) ?: throw IllegalArgumentException()
        }.onSuccess { file ->
            loadList(file)
        }.onFailure {
            _result.emit(Result.failure(it))
            _fileList.emit(emptyList())
            _currentFile.emit(null)
            _isLoading.emit(false)
        }
    }

    /**
     * Load list
     */
    private suspend fun loadList(file: CifsFile) {
        _isLoading.emit(true)
        runCatching {
            cifsRepository.getFileChildren(file.uri.toString())
        }.onSuccess { list ->
            _fileList.emit(list.filter { it.isDirectory }.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name }))
            _currentFile.emit(file)
            _isLoading.emit(false)
        }.onFailure {
            _result.emit(Result.failure(it))
            _fileList.emit(emptyList())
            _currentFile.emit(file)
            _isLoading.emit(false)
        }
    }

    override fun onCleared() {
        runBlocking {
            cifsRepository.closeAllSessions()
            _isLoading.emit(false)
        }
        super.onCleared()
    }

}
