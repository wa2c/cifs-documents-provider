package com.wa2c.android.cifsdocumentsprovider.presentation.ui.main

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.wa2c.android.cifsdocumentsprovider.R
import com.wa2c.android.cifsdocumentsprovider.databinding.LayoutCifsItemBinding
import com.wa2c.android.cifsdocumentsprovider.domain.model.CifsConnection

/**
 * CIFS List item.
 */
class CifsListAdapter(
    private val viewModel: MainViewModel,
    private val itemList: List<CifsConnection>
):  RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return (DataBindingUtil.inflate(LayoutInflater.from(parent.context), R.layout.layout_cifs_item, parent,false) as LayoutCifsItemBinding).let {
            val rootView = it.root
            rootView.tag = it
            object : RecyclerView.ViewHolder(rootView) {}
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val binding = holder.itemView.tag as LayoutCifsItemBinding
        val item = itemList.getOrNull(position)
        if (item != null) {
            binding.cifsListItemAdd.visibility = View.GONE
            binding.cifsListItemTitle.visibility = View.VISIBLE
            binding.cifsListItemSummary.visibility = View.VISIBLE

            binding.cifsListItemTitle.text = item.name
            binding.cifsListItemSummary.text = item.cifsUri
        } else {
            binding.cifsListItemAdd.visibility = View.VISIBLE
            binding.cifsListItemTitle.visibility = View.GONE
            binding.cifsListItemSummary.visibility = View.GONE

            binding.cifsListItemTitle.text = null
            binding.cifsListItemSummary.text = null
        }
        binding.root.setOnClickListener { viewModel.onClickItem(item) }
    }

    override fun getItemCount(): Int {
        return itemList.count() + 1
    }

}
