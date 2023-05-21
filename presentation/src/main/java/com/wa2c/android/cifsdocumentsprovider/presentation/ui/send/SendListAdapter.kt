//package com.wa2c.android.cifsdocumentsprovider.presentation.ui.send
//
//import android.view.LayoutInflater
//import android.view.ViewGroup
//import androidx.appcompat.widget.PopupMenu
//import androidx.core.view.isInvisible
//import androidx.databinding.DataBindingUtil
//import androidx.recyclerview.widget.DiffUtil
//import androidx.recyclerview.widget.ListAdapter
//import androidx.recyclerview.widget.RecyclerView
//import com.wa2c.android.cifsdocumentsprovider.domain.model.SendData
//import com.wa2c.android.cifsdocumentsprovider.presentation.R
//import com.wa2c.android.cifsdocumentsprovider.presentation.databinding.LayoutSendItemBinding
//import com.wa2c.android.cifsdocumentsprovider.presentation.ext.getSummaryText
//
///**
// * Send list adapter.
// */
//class SendListAdapter(
//    private val viewModel: SendViewModel
//): ListAdapter<SendData ,RecyclerView.ViewHolder>(
//    object: DiffUtil.ItemCallback<SendData>() {
//        override fun areItemsTheSame(oldItem: SendData, newItem: SendData): Boolean { return oldItem == newItem }
//        override fun areContentsTheSame(oldItem: SendData, newItem: SendData): Boolean { return oldItem == newItem }
//    }
//) {
//
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
//        return (DataBindingUtil.inflate(LayoutInflater.from(parent.context), R.layout.layout_send_item, parent,false) as LayoutSendItemBinding).let {
//            val rootView = it.root
//            rootView.tag = it
//            object : RecyclerView.ViewHolder(rootView) {}
//        }
//    }
//
//    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
//        val binding = holder.itemView.tag as LayoutSendItemBinding
//        val item = getItem(position)
//        binding.sendListItemTitle.text = item.name
//        binding.sendListItemSummary.text = item.getSummaryText(binding.root.context)
//        binding.sendListItemProgress.progress = item.progress
//        binding.sendListItemProgress.isInvisible = !item.state.inProgress
//        binding.root.let { root ->
//            root.setOnClickListener {
//                PopupMenu(root.context, root).also {
//                    val cancel = if (item.state.isCancelable)  it.menu.add(R.string.send_action_cancel)  else null
//                    val retry = if (item.state.isRetryable) it.menu.add(R.string.send_action_retry) else null
//                    val remove = it.menu.add(R.string.send_action_remove)
//                    it.setOnMenuItemClickListener { menuItem ->
//                        when (menuItem) {
//                            cancel -> { viewModel.onClickCancel(position) }
//                            retry -> { viewModel.onClickRetry(position) }
//                            remove -> { viewModel.onClickRemove(position) }
//                        }
//                        true
//                    }
//                }.show()
//            }
//        }
//    }
//
//}
