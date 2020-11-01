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

    private val cifsConnections = mutableListOf<CifsConnection>()
    //val cifsConnections = _cifsConnections as LiveData<List<CifsConnection>>

    var name = MutableLiveData<String>()
    var domain = MutableLiveData<String>()
    var host = MutableLiveData<String>()
    var folder = MutableLiveData<String>()
    var user = MutableLiveData<String>()
    var password = MutableLiveData<String>()
    var anonymous = MutableLiveData<Boolean>()

    val uri = MediatorLiveData<String>().apply {
        fun post() { postValue(cifsUseCase.getConnectionUri(name.value, folder.value)) }
        addSource(name) { post() }
        addSource(folder) { post() }
    }

    fun initialize() {
        load()
    }

    private fun save() {
        createCifsServer()?.let {
            cifsUseCase.saveConnections(listOf(it))
        }
    }

    private fun load() {
        cifsUseCase.loadConnections().let {
            logD(it)
        }
    }

    private fun getNewName(): String {
        return "New Data"
    }

    private fun createCifsServer(): CifsConnection? {
        return CifsConnection(
            name = name.value?.ifEmpty { null } ?: getNewName(),
            domain = domain.value?.ifEmpty { null },
            host = host.value?.ifEmpty { null } ?: return null,
            folder = folder.value?.ifEmpty { null },
            user = user.value?.ifEmpty { null },
            password = password.value?.ifEmpty { null },
            anonymous = anonymous.value ?: false
        )
    }


    fun onClickSearchHost() {
//        val a= cifsUseCase.cifsClient
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
                    val isConnected = createCifsServer()?.let { cifsUseCase.checkConnection(it) } ?: false
                    if (!isConnected) throw IOException()
                }
            }.onSuccess {
                _navigationEvent.value = Nav.Warning("よい")
            }.onFailure {
                _navigationEvent.value = Nav.Warning("だめ")
            }
        }
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

        if (cifsConnections.any { it.name == inputName}) {
            Nav.Warning("Exists")
            return
        }

        save()
    }

    sealed class Nav {
        object Back : Nav()
        data class Warning(val message: String): Nav()
    }

}
