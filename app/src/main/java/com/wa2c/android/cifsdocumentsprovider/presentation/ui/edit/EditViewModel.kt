package com.wa2c.android.cifsdocumentsprovider.presentation.ui.edit

import androidx.lifecycle.*
import com.wa2c.android.cifsdocumentsprovider.R
import com.wa2c.android.cifsdocumentsprovider.common.utils.MainCoroutineScope
import com.wa2c.android.cifsdocumentsprovider.common.values.ConnectionResult
import com.wa2c.android.cifsdocumentsprovider.domain.model.CifsConnection
import com.wa2c.android.cifsdocumentsprovider.domain.repository.CifsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.*
import javax.inject.Inject

/**
 * Edit Screen ViewModel
 */
@HiltViewModel
class EditViewModel @Inject constructor(
    private val cifsRepository: CifsRepository
) : ViewModel(), CoroutineScope by MainCoroutineScope() {

    private val _navigationEvent = MutableSharedFlow<EditNav>()
    val navigationEvent: SharedFlow<EditNav> = _navigationEvent

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy

    var name = MutableStateFlow<String?>(null)
    var domain = MutableStateFlow<String?>(null)
    var host = MutableStateFlow<String?>(null)
    var port = MutableStateFlow<String?>(null)
    var enableDfs = MutableStateFlow<Boolean>(false)
    var folder = MutableStateFlow<String?>(null)
    var user = MutableStateFlow<String?>(null)
    var password = MutableStateFlow<String?>(null)
    var anonymous = MutableStateFlow<Boolean>(false)
    var extension = MutableStateFlow<Boolean>(false)
    var safeTransfer = MutableStateFlow<Boolean>(false)

    val connectionUri: StateFlow<String> = combine(host, port, folder) { host, port, folder ->
        CifsConnection.getSmbUri(host, port, folder)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, "")

    val providerUri: StateFlow<String> = combine(host, port, folder) { host, port, folder ->
        CifsConnection.getContentUri(host, port, folder)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, "")

    private val _connectionResult = MutableStateFlow<ConnectionResult?>(null)
    val connectionResult = channelFlow<ConnectionResult?> {
        launch { _connectionResult.collect { send(it) } }
        launch { domain.collect { send(null) } }
        launch { host.collect { send(null) } }
        launch { port.collect { send(null) } }
        launch { enableDfs.collect { send(null) } }
        launch { folder.collect { send(null) } }
        launch { user.collect { send(null) } }
        launch { password.collect { send(null) } }
        launch { anonymous.collect { send(null) } }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    /** Current ID */
    private var currentId: String = CifsConnection.NEW_ID

    /** True if adding new settings */
    val isNew: Boolean
        get() = currentId == CifsConnection.NEW_ID

    /** Init connection */
    private var initConnection: CifsConnection? = null

    /** True if initialized */
    private var initialized: Boolean = false

    /**
     * Initialize
     */
    fun initialize(connection: CifsConnection?) {
        if (initialized) return
        initConnection = if (connection == null || connection.isNew) null else connection
        deployCifsConnection(connection)
        initialized = true
    }

    /**
     * Save connection
     */
    private fun save() {
        createCifsConnection(isNew)?.let { con ->
            if (cifsRepository.loadConnection().filter { it.id != con.id }.any { it.folderSmbUri == con.folderSmbUri }) {
                // Duplicate URI
                throw IllegalArgumentException()
            }
            cifsRepository.saveConnection(con)
            currentId = con.id
            initConnection = con
        } ?: run {
            throw IOException()
        }
    }

    /**
     * Delete connection
     */
    private fun delete() {
        cifsRepository.deleteConnection(currentId)
    }

    /**
     * Deploy connection data.
     */
    private fun deployCifsConnection(connection: CifsConnection?) {
        currentId = connection?.id ?: CifsConnection.NEW_ID
        name.value = connection?.name
        domain.value = connection?.domain
        host.value = connection?.host
        port.value = connection?.port
        enableDfs.value = connection?.enableDfs ?: false
        folder.value = connection?.folder
        user.value = connection?.user
        password.value = connection?.password
        anonymous.value = connection?.anonymous ?: false
        extension.value = connection?.extension ?: false
        safeTransfer.value = connection?.safeTransfer ?: false
    }

    /**
     * Create connection data
     */
    private fun createCifsConnection(generateId: Boolean): CifsConnection? {
        val isAnonymous = anonymous.value
        return CifsConnection(
            id = if (generateId) UUID.randomUUID().toString() else currentId,
            name = name.value?.ifEmpty { null } ?: host.value ?: return null,
            domain = domain.value?.ifEmpty { null },
            host = host.value?.ifEmpty { null } ?: return null,
            port = port.value?.ifEmpty { null },
            enableDfs = enableDfs.value,
            folder = folder.value?.ifEmpty { null },
            user = if (isAnonymous) null else user.value?.ifEmpty { null },
            password = if (isAnonymous) null else password.value?.ifEmpty { null },
            anonymous = isAnonymous,
            extension = extension.value,
            safeTransfer = safeTransfer.value,
        )
    }

    /**
     * Check connection
     */
    fun onClickCheckConnection() {
        _isBusy.value = true
        launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    createCifsConnection(false)?.let { cifsRepository.checkConnection(it) }
                }
            }.getOrNull().let {
                _connectionResult.value = it ?: ConnectionResult.Failure()
                _isBusy.value = false
            }
        }
    }

    fun onClickSearchHost() {
        launch {
            _navigationEvent.emit(EditNav.SelectHost(createCifsConnection(false)))
        }
    }

    /**
     * Select Folder Click
     */
    fun onClickSelectFolder() {
        _isBusy.value = true
        launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val folderConnection = createCifsConnection(false) ?: throw IOException()

                    // use target folder
                    val folderResult = cifsRepository.checkConnection(folderConnection)
                    if (folderResult is ConnectionResult.Success) {
                        _navigationEvent.emit(EditNav.SelectFolder(folderConnection))
                        return@withContext folderResult
                    } else if (folderResult is ConnectionResult.Failure) {
                        return@withContext folderResult
                    }

                    // use root folder
                    val rootConnection = folderConnection.copy(folder = null)
                    val rootResult = cifsRepository.checkConnection(rootConnection)
                    if (rootResult == ConnectionResult.Success) {
                        _navigationEvent.emit(EditNav.SelectFolder(rootConnection))
                        return@withContext folderResult // Show target folder warning
                    }
                    return@withContext rootResult
                }
            }.getOrNull().let {
                _connectionResult.value = it ?: ConnectionResult.Failure()
                _isBusy.value = false
            }
        }
    }

    /**
     * Set host result.
     */
    fun setHostResult(hostText: String?) {
        host.value = hostText
    }

    /**
     * Set folder result.
     */
    fun setFolderResult(path: String?) {
        folder.value = path
        _connectionResult.value = if (path != null) ConnectionResult.Success else ConnectionResult.Failure()
    }

    /**
     * Delete Click
     */
    fun onClickDelete() {
        launch {
            delete()
            _navigationEvent.emit(EditNav.Back())
        }
    }

    /**
     * Save Click
     */
    fun onClickAccept() {
        _isBusy.value = true
        launch {
            runCatching {
                save()
            }.onSuccess {
                _navigationEvent.emit(EditNav.SaveResult(null))
                _isBusy.value = false
            }.onFailure {
                if (it is IllegalArgumentException) {
                    // URI duplicated
                    _navigationEvent.emit(EditNav.SaveResult(R.string.edit_save_duplicate_message))
                } else {
                    // Host empty
                    _navigationEvent.emit(EditNav.SaveResult(R.string.edit_save_ng_message))
                }
                _isBusy.value = false
            }
        }
    }

    /**
     * Back Click
     */
    fun onClickBack() {
        launch {
            _navigationEvent.emit(EditNav.Back(initConnection == null || initConnection != createCifsConnection(false)))
        }
    }

}
