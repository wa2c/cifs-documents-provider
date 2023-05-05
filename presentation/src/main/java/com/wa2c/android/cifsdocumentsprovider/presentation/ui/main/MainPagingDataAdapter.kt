package com.wa2c.android.cifsdocumentsprovider.presentation.ui.main

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.wa2c.android.cifsdocumentsprovider.domain.model.CifsConnection
import com.wa2c.android.cifsdocumentsprovider.presentation.R
import com.wa2c.android.cifsdocumentsprovider.presentation.databinding.LayoutCifsItemBinding
import com.wa2c.android.cifsdocumentsprovider.presentation.ext.labelRes

/**
 * Main list adapter.
 */
class MainPagingDataAdapter(
    private val viewModel: MainViewModel
): PagingDataAdapter<CifsConnection, RecyclerView.ViewHolder>(
    object: DiffUtil.ItemCallback<CifsConnection>() {
        override fun areItemsTheSame(oldItem: CifsConnection, newItem: CifsConnection): Boolean { return oldItem == newItem }
        override fun areContentsTheSame(oldItem: CifsConnection, newItem: CifsConnection): Boolean { return oldItem == newItem }
    }
) {
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
            binding.cifsListItemTitle.text = item.name
            binding.cifsListItemStorage.text = binding.root.context.getString(item.storage.labelRes)
            binding.cifsListItemSummary.text = item.folderSmbUri
        }
        binding.root.setOnClickListener { viewModel.onClickItem(item) }
    }

}
