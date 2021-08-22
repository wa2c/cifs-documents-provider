package com.wa2c.android.cifsdocumentsprovider.presentation.ui.host

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.wa2c.android.cifsdocumentsprovider.R
import com.wa2c.android.cifsdocumentsprovider.databinding.LayoutHostItemBinding
import com.wa2c.android.cifsdocumentsprovider.domain.model.HostData

/**
 * Host List item.
 */
class HostListAdapter(
    private val viewModel: HostViewModel
):  RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val itemList = mutableListOf<HostData>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return (DataBindingUtil.inflate(LayoutInflater.from(parent.context), R.layout.layout_host_item, parent,false) as LayoutHostItemBinding).let {
            val rootView = it.root
            rootView.tag = it
            object : RecyclerView.ViewHolder(rootView) {}
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val binding = holder.itemView.tag as LayoutHostItemBinding
        val item = itemList.getOrNull(position) ?: return
        binding.data = item
        binding.root.setOnClickListener { viewModel.onClickItem(item) }
    }

    override fun getItemCount(): Int {
        return itemList.count()
    }

    /**
     * Add data
     */
    fun addData(data: HostData) {
        itemList.add(data)
        notifyItemInserted(itemList.size - 1)
    }

    /**
     * Clear data
     */
    fun clearData() {
        itemList.clear()
        notifyItemRangeRemoved(0, itemList.size)
    }


}
