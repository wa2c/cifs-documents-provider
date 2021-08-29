package com.wa2c.android.cifsdocumentsprovider.presentation.ui.host

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.wa2c.android.cifsdocumentsprovider.R
import com.wa2c.android.cifsdocumentsprovider.common.values.HostSortType
import com.wa2c.android.cifsdocumentsprovider.databinding.LayoutHostItemBinding
import com.wa2c.android.cifsdocumentsprovider.domain.model.HostData
import java.util.*

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
        when (viewModel.sortType) {
            HostSortType.DetectionAscend -> {
                // Insert to last
                itemList.add(data)
                notifyItemInserted(itemList.size - 1)
            }
            HostSortType.DetectionDescend -> {
                // Insert to first
                itemList.add(0, data)
                notifyItemInserted(0)
            }
            else -> {
                // Insert and sort
                itemList.add(data)
                sort()
            }
        }
    }

    /**
     * Clear data
     */
    fun clearData() {
        notifyItemRangeRemoved(0, itemList.size)
        itemList.clear()
    }

    /**
     * Sort data
     */
    fun sort() {
        Collections.sort(itemList, hostComparator)
        notifyItemRangeChanged(0, itemList.size)
    }

    /**
     * Sort comparator
     */
    private val hostComparator = object : Comparator<HostData> {
        override fun compare(p0: HostData, p1: HostData): Int {
            return when (viewModel.sortType) {
                HostSortType.DetectionAscend -> p0.detectionTime.compareTo(p1.detectionTime)
                HostSortType.DetectionDescend -> p1.detectionTime.compareTo(p0.detectionTime)
                HostSortType.HostNameAscend -> compareHostName(p0, p1, true)
                HostSortType.HostNameDescend -> compareHostName(p0, p1, false)
                HostSortType.IpAddressAscend -> compareIpAddress(p0, p1)
                HostSortType.IpAddressDescend -> compareIpAddress(p1, p0)
            }
        }

        /**
         * Compare host name.
         */
        private fun compareHostName(p0: HostData, p1: HostData, isAscend: Boolean): Int {
            val ascend = if (isAscend) 1 else -1
            return if (p0.hasHostName && p1.hasHostName) {
                p0.hostName.compareTo(p1.hostName) * ascend
            } else if (p0.hasHostName) {
                -1
            } else if (p1.hasHostName) {
                1
            } else {
                compareIpAddress(p0, p1) * ascend
            }
        }

        /**
         * Compare IP address
         */
        private fun compareIpAddress(p0: HostData, p1: HostData): Int {
            val p0Address = p0.ipAddress.split(".").map { it.toIntOrNull() ?: Int.MAX_VALUE }
            val p1Address = p1.ipAddress.split(".").map { it.toIntOrNull() ?: Int.MAX_VALUE }
            return if (p0Address.size == 4 && p1Address.size == 4) {
                for(i in 0 until 4) {
                    (p0Address[i].compareTo(p1Address[i])).let {
                        if (it != 0) return it
                    }
                }
                return 0
            } else {
                p0.ipAddress.compareTo(p1.ipAddress)
            }
        }

    }
}
