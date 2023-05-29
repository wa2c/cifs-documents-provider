package com.wa2c.android.cifsdocumentsprovider.presentation.ui.folder

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.wa2c.android.cifsdocumentsprovider.common.utils.logD
import com.wa2c.android.cifsdocumentsprovider.domain.model.CifsConnection
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

    private val _navigationEvent = MutableSharedFlow<FolderNav>()
    val navigationEvent: SharedFlow<FolderNav> = _navigationEvent

    private val _isLoading =  MutableStateFlow<Boolean>(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _fileList = MutableStateFlow<List<CifsFile>>(emptyList())
    val fileList: StateFlow<List<CifsFile>> = _fileList

    private val _currentFile = MutableStateFlow<CifsFile?>(null)
    val currentFile: StateFlow<CifsFile?> = _currentFile

    private lateinit var cifsConnection: CifsConnection

    init {
        launch {
            val file = cifsRepository.getFile(paramUri) ?: return@launch
            loadList(file)
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
     */
    fun onUpFolder(): Boolean {
        if (currentFile.value?.isRoot == true) return false
        if (isLoading.value) return true

        launch {
            val uri = currentFile.value?.parentUri ?: return@launch
            val file = cifsRepository.getFile(cifsConnection, uri.toString()) ?: return@launch
            loadList(file)
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
    private suspend fun loadList(file: CifsFile) {
        _isLoading.value = true
        runCatching {
            cifsRepository.getFileChildren(cifsConnection, file.uri.toString())
        }.onSuccess { list ->
            _fileList.value = list.filter { it.isDirectory }.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
            _currentFile.value = file
            _isLoading.value = false
        }.onFailure {
            _fileList.value = emptyList()
            _currentFile.value = file
            _isLoading.value = false
        }
    }

    /**
     * On click set
     */
    fun onClickSet() {
        logD("onClickSetManually")
        launch {
            _navigationEvent.emit(FolderNav.SetFolder(currentFile.value))
        }
    }

    override fun onCleared() {
        runBlocking { cifsRepository.closeAllSessions() }
        _isLoading.value = false
        super.onCleared()
    }

}
