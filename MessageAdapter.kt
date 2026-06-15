package com.example.aichat.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.aichat.R
import com.example.aichat.data.Message
import com.example.aichat.data.MessageRole
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MessageAdapter : ListAdapter<Message, RecyclerView.ViewHolder>(MessageDiffCallback()) {

    companion object {
        private const val VIEW_TYPE_USER = 0
        private const val VIEW_TYPE_ASSISTANT = 1
        private const val VIEW_TYPE_LOADING = 2
    }

    override fun getItemViewType(position: Int): Int = when (getItem(position).role) {
        MessageRole.USER -> VIEW_TYPE_USER
        MessageRole.ASSISTANT -> VIEW_TYPE_ASSISTANT
        MessageRole.LOADING -> VIEW_TYPE_LOADING
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_USER -> UserViewHolder(
                inflater.inflate(R.layout.item_message_user, parent, false)
            )
            VIEW_TYPE_LOADING -> LoadingViewHolder(
                inflater.inflate(R.layout.item_message_loading, parent, false)
            )
            else -> AssistantViewHolder(
                inflater.inflate(R.layout.item_message_assistant, parent, false)
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val msg = getItem(position)
        when (holder) {
            is UserViewHolder -> holder.bind(msg)
            is AssistantViewHolder -> holder.bind(msg)
            is LoadingViewHolder -> { /* animated dots via XML, no binding needed */ }
        }
    }

    // ── ViewHolders ────────────────────────────────────────────────────────────

    class UserViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvContent: TextView = view.findViewById(R.id.tvMessageContent)
        private val tvTime: TextView = view.findViewById(R.id.tvMessageTime)

        fun bind(message: Message) {
            tvContent.text = message.content
            tvTime.text = formatTime(message.timestamp)
        }
    }

    class AssistantViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvContent: TextView = view.findViewById(R.id.tvMessageContent)
        private val tvTime: TextView = view.findViewById(R.id.tvMessageTime)

        fun bind(message: Message) {
            tvContent.text = message.content
            tvTime.text = formatTime(message.timestamp)
        }
    }

    class LoadingViewHolder(view: View) : RecyclerView.ViewHolder(view)

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun formatTime(timestamp: Long): String =
        SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(timestamp))
}

class MessageDiffCallback : DiffUtil.ItemCallback<Message>() {
    override fun areItemsTheSame(old: Message, new: Message) = old.id == new.id
    override fun areContentsTheSame(old: Message, new: Message) = old == new
}
