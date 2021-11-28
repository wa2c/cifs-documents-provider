package com.wa2c.android.cifsdocumentsprovider.presentation.ui.send

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.wa2c.android.cifsdocumentsprovider.R
import com.wa2c.android.cifsdocumentsprovider.databinding.LayoutSendItemBinding
import com.wa2c.android.cifsdocumentsprovider.domain.model.SendData
import com.wa2c.android.cifsdocumentsprovider.domain.model.SendData.Companion.getSummaryText
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
        binding.sendListItemProgress.isVisible(item.state.inProgress)
        binding.sendListItemIcon.setOnClickListener { viewModel.onClickCancel(item) }
    }



}
