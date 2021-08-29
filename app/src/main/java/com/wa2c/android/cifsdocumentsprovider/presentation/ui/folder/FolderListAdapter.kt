package com.wa2c.android.cifsdocumentsprovider.presentation.ui.folder

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.wa2c.android.cifsdocumentsprovider.R
import com.wa2c.android.cifsdocumentsprovider.databinding.LayoutFolderItemBinding
import com.wa2c.android.cifsdocumentsprovider.domain.model.CifsFile

/**
 * Folder List item.
 */
class FolderListAdapter(
    private val viewModel: FolderViewModel
): ListAdapter<CifsFile, RecyclerView.ViewHolder>(
    object: DiffUtil.ItemCallback<CifsFile>() {
        override fun areItemsTheSame(oldItem: CifsFile, newItem: CifsFile): Boolean { return oldItem == newItem }
        override fun areContentsTheSame(oldItem: CifsFile, newItem: CifsFile): Boolean { return oldItem == newItem }
    }
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return (DataBindingUtil.inflate(LayoutInflater.from(parent.context), R.layout.layout_folder_item, parent,false) as LayoutFolderItemBinding).let {
            val rootView = it.root
            rootView.tag = it
            object : RecyclerView.ViewHolder(rootView) {}
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val binding = holder.itemView.tag as LayoutFolderItemBinding
        val item = getItem(position)
        binding.data = item
        binding.root.setOnClickListener { viewModel.onSelectFolder(item) }
    }

}
