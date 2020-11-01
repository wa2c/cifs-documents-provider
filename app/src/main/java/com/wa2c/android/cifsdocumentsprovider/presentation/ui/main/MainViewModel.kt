package com.wa2c.android.cifsdocumentsprovider.presentation.ui.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.hadilq.liveevent.LiveEvent

/**
 * Main Screen ViewModel
 */
class MainViewModel: ViewModel() {

    private val _navigationEvent = LiveEvent<Nav>()
    val navigationEvent: LiveData<Nav> = _navigationEvent

    fun onClickButton() {
        // do nothing
        _navigationEvent.value = Nav.Edit
    }

    sealed class Nav {
        object Edit: Nav()
    }

}
