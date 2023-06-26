package com.wa2c.android.cifsdocumentsprovider.presentation.ui.home

import androidx.lifecycle.ViewModel
import com.wa2c.android.cifsdocumentsprovider.domain.repository.CifsRepository
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
    private val cifsRepository: CifsRepository
): ViewModel(), CoroutineScope by MainCoroutineScope() {

    val connectionListFlow = cifsRepository.connectionListFlow

    /**
     * Move item.
     */
    fun onItemMove(fromPosition: Int, toPosition: Int) {
        runBlocking {
            // run blocking for drag animation
            cifsRepository.moveConnection(fromPosition, toPosition)
        }
    }

}
