package com.wa2c.android.cifsdocumentsprovider.presentation.ui.edit

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.hadilq.liveevent.LiveEvent
import com.wa2c.android.cifsdocumentsprovider.common.utils.MainCoroutineScope
import com.wa2c.android.cifsdocumentsprovider.domain.model.CifsConnection
import com.wa2c.android.cifsdocumentsprovider.domain.repository.CifsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject

/**
 * Edit Screen ViewModel
 */
@HiltViewModel
class EditViewModel @Inject constructor(
    private val cifsRepository: CifsRepository
) : ViewModel(), CoroutineScope by MainCoroutineScope() {

    private val _navigationEvent = LiveEvent<EditNav>()
    val navigationEvent: LiveData<EditNav> = _navigationEvent

    var name = MutableLiveData<String?>()
    var domain = MutableLiveData<String?>()
    var host = MutableLiveData<String?>()
    var port = MutableLiveData<String?>()
    var folder = MutableLiveData<String?>()
    var user = MutableLiveData<String?>()
    var password = MutableLiveData<String?>()
    var anonymous = MutableLiveData<Boolean?>()
    var extension = MutableLiveData<Boolean?>()

    val connectionUri = MediatorLiveData<String>().apply {
        fun post() { postValue(CifsConnection.getConnectionUri(host.value, port.value, folder.value)) }
        addSource(host) { post() }
        addSource(port) { post() }
        addSource(folder) { post() }
    }

    val providerUri = MediatorLiveData<String>().apply {
        fun post() { postValue(CifsConnection.getProviderUri(host.value, folder.value)) }
        addSource(host) { post() }
        addSource(folder) { post() }
    }

    private var currentId: String = NEW_ID

    val isNew: Boolean
        get() = currentId == NEW_ID


    private var initConnection: CifsConnection? = null

    fun initialize(connection: CifsConnection?) {
        initConnection = connection
        deployCifsConnection(connection)
    }

    /**
     * Save connection
     */
    private fun save() {
        createCifsConnection()?.let {
            cifsRepository.saveConnection(it)
            currentId = it.id
            initConnection = it
        } ?: run {
            throw IOException()
        }
    }

    /**
     * Save temporally connection
     */
    private fun saveTemporal() {
        createCifsConnection()?.let {
            cifsRepository.saveConnectionTemporal(it)
        }
    }

    /**
     * Clear temporally connection
     */
    fun clearTemporal() {
        cifsRepository.clearConnectionTemporal()
    }

    private fun delete() {
        cifsRepository.deleteConnection(currentId)
    }

    /**
     * Deploy connection data.
     */
    private fun deployCifsConnection(connection: CifsConnection?) {
        currentId = connection?.id ?: NEW_ID
        name.value = connection?.name
        domain.value = connection?.domain
        host.value = connection?.host
        port.value = connection?.port
        folder.value = connection?.folder
        user.value = connection?.user
        password.value = connection?.password
        anonymous.value = connection?.anonymous ?: false
        extension.value = connection?.extension ?: false
    }

    /**
     * Create connection data
     */
    private fun createCifsConnection(): CifsConnection? {
        val isAnonymous = anonymous.value ?: false
        return CifsConnection(
            id = if (isNew) CifsConnection.newId() else currentId,
            name = name.value?.ifEmpty { null } ?: host.value ?: return null,
            domain = domain.value?.ifEmpty { null },
            host = host.value?.ifEmpty { null } ?: return null,
            port = port.value?.ifEmpty { null },
            folder = folder.value?.ifEmpty { null },
            user = if (isAnonymous) null else user.value?.ifEmpty { null },
            password = if (isAnonymous) null else password.value?.ifEmpty { null },
            anonymous = isAnonymous,
            extension = extension.value ?: false
        )
    }

    /**
     * Check connection
     */
    fun onClickCheckConnection() {
        launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val isConnected = createCifsConnection()?.let { cifsRepository.checkConnection(it) } ?: false
                    if (!isConnected) throw IOException()
                }
            }.onSuccess {
                _navigationEvent.value = EditNav.CheckConnectionResult(true)
            }.onFailure {
                _navigationEvent.value = EditNav.CheckConnectionResult(false)
            }
        }
    }

    /**
     * Select Directory Click
     */
    fun onClickSelectDirectory() {
        launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val isConnected = createCifsConnection()?.let { cifsRepository.checkConnection(it, false) } ?: false
                    if (!isConnected) throw IOException()
                    saveTemporal()
                    CifsConnection.getProviderUri(host.value, folder.value)
                }
            }.onSuccess {
                _navigationEvent.value = EditNav.SelectDirectory(it)
            }.onFailure {
                _navigationEvent.value = EditNav.CheckConnectionResult(false)
            }
        }
    }


    /**
     * Delete Click
     */
    fun onClickDelete() {
        launch {
            delete()
            _navigationEvent.value = EditNav.Back()
        }
    }

    /**
     * Save Click
     */
    fun onClickAccept() {
        launch {
            runCatching {
                save()
            }.onSuccess {
                _navigationEvent.value = EditNav.SaveResult(true)
            }.onFailure {
                _navigationEvent.value = EditNav.SaveResult(false)
            }
        }
    }

    /**
     * Back Click
     */
    fun onClickBack() {
        _navigationEvent.value = EditNav.Back(initConnection == null || initConnection != createCifsConnection())
    }

    companion object {
        private const val NEW_ID: String = ""
    }

}
