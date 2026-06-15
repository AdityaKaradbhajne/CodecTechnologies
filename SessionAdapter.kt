package com.example.aichat.ui.history

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.aichat.R
import com.example.aichat.data.ChatSession
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SessionAdapter(
    private val onSessionClick: (ChatSession) -> Unit,
    private val onDeleteClick: (ChatSession) -> Unit
) : ListAdapter<ChatSession, SessionAdapter.SessionViewHolder>(SessionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SessionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_session, parent, false)
        return SessionViewHolder(view)
    }

    override fun onBindViewHolder(holder: SessionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class SessionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvTitle: TextView = view.findViewById(R.id.tvSessionTitle)
        private val tvMeta: TextView = view.findViewById(R.id.tvSessionMeta)
        private val btnDelete: ImageButton = view.findViewById(R.id.btnDeleteSession)
        private val root: View = view.findViewById(R.id.sessionRoot)

        fun bind(session: ChatSession) {
            tvTitle.text = session.title
            val date = SimpleDateFormat("MMM dd, yyyy · hh:mm a", Locale.getDefault())
                .format(Date(session.updatedAt))
            tvMeta.text = itemView.context.getString(
                R.string.messages_count, session.messageCount
            ) + "  ·  $date"

            root.setOnClickListener { onSessionClick(session) }
            btnDelete.setOnClickListener { onDeleteClick(session) }
        }
    }
}

class SessionDiffCallback : DiffUtil.ItemCallback<ChatSession>() {
    override fun areItemsTheSame(old: ChatSession, new: ChatSession) =
        old.sessionId == new.sessionId
    override fun areContentsTheSame(old: ChatSession, new: ChatSession) = old == new
}
