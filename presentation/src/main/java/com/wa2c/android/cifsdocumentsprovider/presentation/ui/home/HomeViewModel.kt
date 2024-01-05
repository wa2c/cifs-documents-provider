package com.wa2c.android.cifsdocumentsprovider.presentation.ui.home

import androidx.lifecycle.ViewModel
import com.wa2c.android.cifsdocumentsprovider.domain.repository.EditRepository
import com.wa2c.android.cifsdocumentsprovider.presentation.ext.MainCoroutineScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

/**
 * Home Screen ViewModel
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val editRepository: EditRepository
): ViewModel(), CoroutineScope by MainCoroutineScope() {

    val connectionListFlow = editRepository.connectionListFlow

    /**
     * Move item.
     */
    fun onItemMove(fromPosition: Int, toPosition: Int) {
        runBlocking {
            // run blocking for drag animation
            editRepository.moveConnection(fromPosition, toPosition)
        }
    }

}
