package com.wa2c.android.cifsdocumentsprovider.presentation.ui.folder

import androidx.lifecycle.ViewModel
import com.wa2c.android.cifsdocumentsprovider.common.utils.logD
import com.wa2c.android.cifsdocumentsprovider.domain.model.CifsConnection
import com.wa2c.android.cifsdocumentsprovider.domain.model.CifsFile
import com.wa2c.android.cifsdocumentsprovider.domain.repository.CifsRepository
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.MainCoroutineScope
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.mapState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Folder Screen ViewModel
 */
@HiltViewModel
class FolderViewModel @Inject constructor(
    private val cifsRepository: CifsRepository
): ViewModel(), CoroutineScope by MainCoroutineScope() {

    private val _navigationEvent = MutableSharedFlow<FolderNav>()
    val navigationEvent: SharedFlow<FolderNav> = _navigationEvent

    private val _isLoading =  MutableStateFlow<Boolean>(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _fileList = MutableStateFlow<List<CifsFile>>(emptyList())
    val fileList: StateFlow<List<CifsFile>> = _fileList
    val isEmpty: StateFlow<Boolean> = fileList.mapState(this) { it.isEmpty() }

    private val _currentFile = MutableStateFlow<CifsFile?>(null)
    val currentFile: StateFlow<CifsFile?> = _currentFile

    private lateinit var cifsConnection: CifsConnection

    /**
     * Initialize
     */
    fun initialize(connection: CifsConnection) {
        cifsConnection = connection
        _isLoading.value = true
        launch {
            val file = cifsRepository.getFile(connection) ?: return@launch
            _currentFile.value = file
            loadList(file)
        }
    }

    /**
     * on up folder
     */
    fun onUpFolder(): Boolean {
        if (currentFile.value?.isRoot == true) return false

        if (isLoading.value) return true
        _isLoading.value = true
        launch {
            val uri = currentFile.value?.parentUri ?: return@launch
            val file = cifsRepository.getFile(cifsConnection, uri.toString()) ?: return@launch
            loadList(file)
        }
        return true
    }

    /**
     * On select folder
     */
    fun onSelectFolder(file: CifsFile) {
        if (isLoading.value) return
        _isLoading.value = true
        launch {
            loadList(file)
        }
    }

    /**
     * Load list
     */
    private suspend fun loadList(file: CifsFile) {
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

    /**
     * Reload current folder
     */
    fun reload() {
        currentFile.value?.let { onSelectFolder(it) }
    }

    override fun onCleared() {
        super.onCleared()
        _isLoading.value = false
    }

}
