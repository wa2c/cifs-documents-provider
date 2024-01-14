package com.wa2c.android.cifsdocumentsprovider.presentation.ui.edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wa2c.android.cifsdocumentsprovider.common.utils.generateUUID
import com.wa2c.android.cifsdocumentsprovider.common.values.ConnectionResult
import com.wa2c.android.cifsdocumentsprovider.common.values.DEFAULT_ENCODING
import com.wa2c.android.cifsdocumentsprovider.common.values.StorageType
import com.wa2c.android.cifsdocumentsprovider.domain.exception.EditException
import com.wa2c.android.cifsdocumentsprovider.domain.model.RemoteConnection
import com.wa2c.android.cifsdocumentsprovider.domain.repository.EditRepository
import com.wa2c.android.cifsdocumentsprovider.presentation.ext.MainCoroutineScope
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.EditScreenParamHost
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.EditScreenParamId
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.channelFlow
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
    private var initConnection: RemoteConnection? = null

    /** True if adding new settings */
    val isNew: Boolean
        get() = paramId.isNullOrEmpty()

    /** True if data changed */
    val isChanged: Boolean
        get() = isNew || initConnection != try { createConnection() } catch (e: Exception) { null }

    init {
        launch {
            val connection = paramId?.let {
                editRepository.getConnection(paramId).also { initConnection = it }
            } ?: RemoteConnection.create(currentId, paramHost ?: "")
            deployConnection(connection)
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

    val id = MutableStateFlow<String?>(null)
    val name = MutableStateFlow<String?>(null)
    val storage = MutableStateFlow<StorageType>(StorageType.default)

    val domain = MutableStateFlow<String?>(null)
    val host = MutableStateFlow<String?>(null)
    val port = MutableStateFlow<String?>(null)
    val enableDfs = MutableStateFlow<Boolean>(false)
    val user = MutableStateFlow<String?>(null)
    val password = MutableStateFlow<String?>(null)
    val anonymous = MutableStateFlow<Boolean>(false)
    val folder = MutableStateFlow<String?>(null)

    val isFtpActiveMode = MutableStateFlow<Boolean>(false)
    val encoding = MutableStateFlow<String>(DEFAULT_ENCODING)

    val safeTransfer = MutableStateFlow<Boolean>(false)
    val optionReadOnly = MutableStateFlow<Boolean>(false)
    val extension = MutableStateFlow<Boolean>(false)

    private val _connectionResult = MutableSharedFlow<ConnectionResult?>()
    val connectionResult = channelFlow<ConnectionResult?> {
        launch { _connectionResult.collect { send(it) } }
        launch { storage.collect { send(null) } }
        launch { domain.collect { send(null) } }
        launch { host.collect { send(null) } }
        launch { port.collect { send(null) } }
        launch { enableDfs.collect { send(null) } }
        launch { user.collect { send(null) } }
        launch { password.collect { send(null) } }
        launch { anonymous.collect { send(null) } }
        launch { folder.collect { send(null) } }
        launch { isFtpActiveMode.collect { send(null) } }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    /**
     * Deploy connection data.
     */
    private fun deployConnection(connection: RemoteConnection) {
        id.value = connection.id
        name.value = connection.name
        storage.value = connection.storage
        domain.value = connection.domain
        host.value = connection.host
        port.value = connection.port
        enableDfs.value = connection.enableDfs
        folder.value = connection.folder
        user.value = connection.user
        password.value = connection.password
        anonymous.value = connection.anonymous
        isFtpActiveMode.value = connection.isFtpActiveMode
        encoding.value = connection.encoding
        safeTransfer.value = connection.optionSafeTransfer
        optionReadOnly.value = connection.optionReadOnly
        extension.value = connection.optionAddExtension
    }

    /**
     * Create connection data
     */
    private fun createConnection(): RemoteConnection {
        val isAnonymous = anonymous.value
        return RemoteConnection(
            id = id.value ?: throw EditException.InputRequiredException(),
            name = name.value?.ifEmpty { null } ?: host.value ?: throw EditException.InputRequiredException(),
            storage = storage.value,
            domain = domain.value?.ifEmpty { null },
            host = host.value?.ifEmpty { null } ?: throw EditException.InputRequiredException(),
            port = port.value?.ifEmpty { null },
            enableDfs = enableDfs.value,
            folder = folder.value?.ifEmpty { null },
            user = if (isAnonymous) null else user.value?.ifEmpty { null },
            password = if (isAnonymous) null else password.value?.ifEmpty { null },
            anonymous = isAnonymous,
            isFtpActiveMode = isFtpActiveMode.value,
            encoding = encoding.value,
            optionAddExtension = extension.value,
            optionReadOnly = optionReadOnly.value,
            optionSafeTransfer = safeTransfer.value,
        )
    }

    /**
     * Check connection
     */
    fun onClickCheckConnection() {
        launch {
            _isBusy.emit(true)
            runCatching {
                _connectionResult.emit(null)
                editRepository.checkConnection(createConnection())
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
                val folderConnection = createConnection()
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
                createConnection().let { con ->
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
