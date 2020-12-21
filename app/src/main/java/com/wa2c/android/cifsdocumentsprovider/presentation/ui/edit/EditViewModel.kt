package com.wa2c.android.cifsdocumentsprovider.presentation.ui.edit

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.hadilq.liveevent.LiveEvent
import com.wa2c.android.cifsdocumentsprovider.common.utils.MainCoroutineScope
import com.wa2c.android.cifsdocumentsprovider.common.utils.logD
import com.wa2c.android.cifsdocumentsprovider.domain.model.CifsConnection
import com.wa2c.android.cifsdocumentsprovider.domain.usecase.CifsUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * Edit Screen ViewModel
 */
class EditViewModel @ViewModelInject constructor(
    private val cifsUseCase: CifsUseCase
) : ViewModel(), CoroutineScope by MainCoroutineScope() {

    private val _navigationEvent = LiveEvent<Nav>()
    val navigationEvent: LiveData<Nav> = _navigationEvent

    var name = MutableLiveData<String?>()
    var domain = MutableLiveData<String?>()
    var host = MutableLiveData<String?>()
    var folder = MutableLiveData<String?>()
    var user = MutableLiveData<String?>()
    var password = MutableLiveData<String?>()
    var anonymous = MutableLiveData<Boolean?>()

    val connectionUri = MediatorLiveData<String>().apply {
        fun post() { postValue(CifsConnection.getConnectionUri(host.value, folder.value)) }
        addSource(host) { post() }
        addSource(folder) { post() }
    }

    val providerUri = MediatorLiveData<String>().apply {
        fun post() { postValue(CifsConnection.getProviderUri(host.value, folder.value)) }
        addSource(name) { post() }
        addSource(folder) { post() }
    }

    private var currentId: String = NEW_ID

    val isNew: Boolean
        get() = currentId == NEW_ID



    fun initialize(connection: CifsConnection?) {
        deployCifsConnection(connection)
    }

    private fun save() {
        createCifsConnection()?.let {
            cifsUseCase.saveConnection(it)
            currentId = it.id
        }
    }

    private fun delete() {
        cifsUseCase.deleteConnection(currentId)
    }

    /**
     * Deploy connection data.
     */
    private fun deployCifsConnection(connection: CifsConnection?) {
        currentId = connection?.id ?: NEW_ID
        name.value = connection?.name
        domain.value = connection?.domain
        host.value = connection?.host
        folder.value = connection?.folder
        user.value = connection?.user
        password.value = connection?.password
        anonymous.value = connection?.anonymous ?: false
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
            folder = folder.value?.ifEmpty { null },
            user = if (isAnonymous) null else user.value?.ifEmpty { null },
            password = if (isAnonymous) null else password.value?.ifEmpty { null },
            anonymous = isAnonymous
        )
    }

    fun onClickSearchDirectory() {
        logD("")
    }

    /**
     * Check connection
     */
    fun onClickCheckConnection() {
        launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val isConnected = createCifsConnection()?.let { cifsUseCase.checkConnection(it) } ?: false
                    if (!isConnected) throw IOException()
                }
            }.onSuccess {
                _navigationEvent.value = Nav.CheckConnectionResult(true)
                _navigationEvent.value = Nav.CheckConnectionResult(false)
            }
        }
    }

    /**
     * Check picker
     */
    fun onClickCheckPicker() {
        _navigationEvent.value = Nav.CheckPicker
    }

    fun onClickDelete() {
        launch {
            delete()
            _navigationEvent.value = Nav.Back
        }
    }

    fun onClickAccept() {
        launch {
            save()
            _navigationEvent.value = Nav.Back
        }
    }

    sealed class Nav {
        object Back : Nav()
        data class CheckConnectionResult(val result: Boolean): Nav()
        object CheckPicker: Nav()
    }

    companion object {
        private const val NEW_ID: String = ""
    }

}
