package com.wa2c.android.cifsdocumentsprovider.presentation.ui.send

import android.content.Context
import android.text.format.DateUtils
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.wa2c.android.cifsdocumentsprovider.R
import com.wa2c.android.cifsdocumentsprovider.databinding.LayoutSendItemBinding
import com.wa2c.android.cifsdocumentsprovider.domain.model.SendData
import com.wa2c.android.cifsdocumentsprovider.domain.model.SendDataState
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.isVisible

/**
 * Send list adapter.
 */
class SendListAdapter(
    private val viewModel: SendViewModel
): ListAdapter<SendData ,RecyclerView.ViewHolder>(
    object: DiffUtil.ItemCallback<SendData>() {
        override fun areItemsTheSame(oldItem: SendData, newItem: SendData): Boolean { return oldItem == newItem }
        override fun areContentsTheSame(oldItem: SendData, newItem: SendData): Boolean { return oldItem == newItem }
    }
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return (DataBindingUtil.inflate(LayoutInflater.from(parent.context), R.layout.layout_send_item, parent,false) as LayoutSendItemBinding).let {
            val rootView = it.root
            rootView.tag = it
            object : RecyclerView.ViewHolder(rootView) {}
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val binding = holder.itemView.tag as LayoutSendItemBinding
        val item = getItem(position)
        binding.sendListItemTitle.text = item.name
        binding.sendListItemSummary.text = item.getSummaryText(binding.root.context)
        binding.sendListItemProgress.progress = item.progress
        binding.sendListItemProgress.isVisible(item.inProgress)
        binding.sendListItemIcon.setOnClickListener { viewModel.onClickCancel(item) }
    }

    /**
     * Summary Text
     */
    private fun SendData.getSummaryText(context: Context): String {
        // 10% [10MB/100MB] (1MB/s)
        return when (state) {
            SendDataState.PROGRESS -> {
                val sendSize = " (${Formatter.formatShortFileSize(context, progressSize)}/${Formatter.formatShortFileSize(context, size)})"
                val sendSpeed = "${Formatter.formatShortFileSize(context, bps)}/s (${DateUtils.formatElapsedTime(elapsedTime / 1000)}"
                "$progress% $sendSize $sendSpeed"
            }
            SendDataState.FAILURE -> {
                "Failure"
            }
            SendDataState.SUCCESS -> {
                "Success"
            }
            SendDataState.CANCEL -> {
                "Cancel"
            }
            SendDataState.READY -> {
                "Ready"
            }
        }
    }

}
