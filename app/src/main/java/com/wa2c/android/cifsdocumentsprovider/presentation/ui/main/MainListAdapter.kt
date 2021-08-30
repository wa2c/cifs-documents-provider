package com.wa2c.android.cifsdocumentsprovider.presentation.ui.main

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.wa2c.android.cifsdocumentsprovider.R
import com.wa2c.android.cifsdocumentsprovider.common.utils.renew
import com.wa2c.android.cifsdocumentsprovider.databinding.LayoutCifsItemBinding
import com.wa2c.android.cifsdocumentsprovider.domain.model.CifsConnection
import com.wa2c.android.cifsdocumentsprovider.domain.model.CifsFile

/**
 * Main List item.
 */
class MainListAdapter(
    private val viewModel: MainViewModel
): ListAdapter<CifsConnection, RecyclerView.ViewHolder>(
    object: DiffUtil.ItemCallback<CifsConnection>() {
        override fun areItemsTheSame(oldItem: CifsConnection, newItem: CifsConnection): Boolean { return oldItem == newItem }
        override fun areContentsTheSame(oldItem: CifsConnection, newItem: CifsConnection): Boolean { return oldItem == newItem }
    }
) {

    init {
        this.submitList(viewModel.cifsConnections.value?.toMutableList())
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return (DataBindingUtil.inflate(LayoutInflater.from(parent.context), R.layout.layout_cifs_item, parent,false) as LayoutCifsItemBinding).let {
            val rootView = it.root
            rootView.tag = it
            object : RecyclerView.ViewHolder(rootView) {}
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val binding = holder.itemView.tag as LayoutCifsItemBinding
        val item = getItem(position)
        if (item != null) {
            binding.cifsListItemTitle.visibility = View.VISIBLE
            binding.cifsListItemSummary.visibility = View.VISIBLE
            binding.cifsListItemTitle.text = item.name
            binding.cifsListItemSummary.text = item.connectionUri
        }
        binding.root.setOnClickListener { viewModel.onClickItem(item) }
    }

}
