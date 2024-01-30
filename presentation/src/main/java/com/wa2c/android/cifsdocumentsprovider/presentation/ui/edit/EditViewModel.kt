package com.wa2c.android.cifsdocumentsprovider.presentation.ui.edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wa2c.android.cifsdocumentsprovider.common.utils.generateUUID
import com.wa2c.android.cifsdocumentsprovider.common.values.ConnectionResult
import com.wa2c.android.cifsdocumentsprovider.common.values.DEFAULT_ENCODING
import com.wa2c.android.cifsdocumentsprovider.common.values.StorageType
import com.wa2c.android.cifsdocumentsprovider.common.exception.EditException
import com.wa2c.android.cifsdocumentsprovider.domain.model.RemoteConnection
import com.wa2c.android.cifsdocumentsprovider.domain.repository.EditRepository
import com.wa2c.android.cifsdocumentsprovider.presentation.ext.MainCoroutineScope
import com.wa2c.android.cifsdocumentsprovider.presentation.ext.collectIn
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.EditScreenParamHost
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.EditScreenParamId
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

/**
 * Edit Screen ViewModel
 */
@HiltViewModel
class EditViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val editRepository: EditRepository,
) : ViewModel(), CoroutineScope by MainCoroutineScope() {

    private val paramId: String? = savedStateHandle[EditScreenParamId]
    private val paramHost: String? = savedStateHandle[EditScreenParamHost]

    /** Current ID */
    private var currentId: String = paramId ?: generateUUID()

    /** Init connection */
    private var initConnection: RemoteConnection = RemoteConnection.INVALID_CONNECTION

    /** Current RemoteConnection */
    val remoteConnection = MutableStateFlow<RemoteConnection>(initConnection)

    /** True if adding new settings */
    val isNew: Boolean
        get() = paramId.isNullOrEmpty()

    /** True if data changed */
    val isChanged: Boolean
        get() = isNew || initConnection != remoteConnection.value

    init {
        launch {
            val connection = paramId?.let {
                editRepository.getConnection(paramId)?.also { initConnection = it }
            } ?: RemoteConnection.create(currentId, paramHost ?: "")
            remoteConnection.emit(connection)
        }
    }

    private val _navigateSearchHost = MutableSharedFlow<String>()
    val navigateSearchHost = _navigateSearchHost.asSharedFlow()

    private val _navigateSelectFolder = MutableSharedFlow<Result<RemoteConnection>>()
    val navigateSelectFolder = _navigateSelectFolder.asSharedFlow()

    private val _result = MutableSharedFlow<Result<Unit>>()
    val result = _result.asSharedFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy = _isBusy.asStateFlow()

    private val _connectionResult = MutableSharedFlow<ConnectionResult?>()
    val connectionResult = channelFlow<ConnectionResult?> {
        var prevConnection = initConnection
        launch { _connectionResult.collect { send(it) } }
        launch {
            remoteConnection.collect {
                if (it.isChangedConnection(prevConnection)) {
                    send(null)
                }
                prevConnection = it
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    /**
     * Check connection
     */
    fun onClickCheckConnection() {
        launch {
            _isBusy.emit(true)
            runCatching {
                _connectionResult.emit(null)
                editRepository.checkConnection(remoteConnection.value)
            }.fold(
                onSuccess = { _connectionResult.emit(it) },
                onFailure = { _connectionResult.emit(ConnectionResult.Failure(it)) }
            ).also {
                _isBusy.emit(false)
            }
        }
    }

    fun onClickSearchHost() {
        launch {
            _navigateSearchHost.emit(currentId)
        }
    }

    /**
     * Select Folder Click
     */
    fun onClickSelectFolder() {
        launch {
            _isBusy.emit(true)
            runCatching {
                val folderConnection = remoteConnection.value
                val result = editRepository.checkConnection(folderConnection)
                if (result !is ConnectionResult.Failure) {
                    editRepository.saveTemporaryConnection(folderConnection)
                    _navigateSelectFolder.emit(Result.success(folderConnection))
                } else {
                    _connectionResult.emit(result)
                }
            }.onFailure {
                _connectionResult.emit(ConnectionResult.Failure(cause = it))
            }.also {
                _isBusy.emit(false)
            }
        }
    }

    /**
     * Delete Click
     */
    fun onClickDelete() {
        launch {
            _isBusy.emit(true)
            runCatching {
                editRepository.deleteConnection(currentId)
            }.onSuccess {
                _result.emit(Result.success(Unit))
                _isBusy.emit(false)
            }.onFailure {
                _result.emit(Result.failure(it))
                _isBusy.emit(false)
            }
        }
    }

    /**
     * Save Click
     */
    fun onClickSave() {
        launch {
            _isBusy.emit(true)
            runCatching {
                remoteConnection.value.let { con ->
                    if (RemoteConnection.isInvalidConnectionId(con.id)) {
                        throw EditException.InvalidIdException()
                    }
                    if (isNew && editRepository.getConnection(con.id) != null) {
                        throw EditException.DuplicatedIdException()
                    }
                    editRepository.saveConnection(con)
                    currentId = con.id
                    initConnection = con
                }
            }.onSuccess {
                _result.emit(Result.success(Unit))
                _isBusy.emit(false)
            }.onFailure {
                _result.emit(Result.failure(it))
                _isBusy.emit(false)
            }
        }
    }

    override fun onCleared() {
        runBlocking {
            editRepository.saveTemporaryConnection(null)
        }
        super.onCleared()
    }
}
