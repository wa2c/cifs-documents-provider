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

    val uri = MediatorLiveData<String>().apply {
        fun post() { postValue(cifsUseCase.getConnectionUri(name.value, folder.value)) }
        addSource(name) { post() }
        addSource(folder) { post() }
    }

    private var currentId: Long = NEW_ID

    val isNew: Boolean
        get() = currentId < 0

    private fun getNewId(): Long {
        return System.currentTimeMillis()
    }


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
            id = if (isNew) getNewId() else currentId ,
            name = name.value?.ifEmpty { null } ?: host.value ?: return null,
            domain = domain.value?.ifEmpty { null },
            host = host.value?.ifEmpty { null } ?: return null,
            folder = folder.value?.ifEmpty { null },
            user = if (isAnonymous) null else user.value?.ifEmpty { null },
            password = if (isAnonymous) null else password.value?.ifEmpty { null },
            anonymous = isAnonymous
        )
    }



    fun onClickSearchHost() {
        logD("")
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
                _navigationEvent.value = Nav.CheckResult(true)
            }.onFailure {
                _navigationEvent.value = Nav.CheckResult(false)
            }
        }
    }

    fun onClickDelete() {
        cifsUseCase.deleteConnection(currentId)
        _navigationEvent.value = Nav.Back
    }

    fun onClickCancel() {
        _navigationEvent.value = Nav.Back
    }

    fun onClickAccept() {
        val inputName = name.value.let { text ->
            if (text.isNullOrBlank()) {
                (host.value ?: "").also { name.value = it }
            } else {
                text
            }
        }

        if (cifsUseCase.provideConnections().any { it.name == inputName}) {
            _navigationEvent.value = Nav.Warning("Exists")
            return
        }

        save()
        _navigationEvent.value = Nav.Back
    }

    sealed class Nav {
        object Back : Nav()
        data class CheckResult(val result: Boolean): Nav()
        data class Warning(val message: String): Nav()
    }

    companion object {
        private const val NEW_ID: Long = -1
    }

}
