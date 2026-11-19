package com.example.twelveman.ui.ai

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.twelveman.R
import com.example.twelveman.data.model.AiChatMessage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AiChatAdapter : ListAdapter<AiChatMessage, RecyclerView.ViewHolder>(DiffCallback) {

    companion object {
        private const val VIEW_TYPE_USER = 1
        private const val VIEW_TYPE_BOT = 2

        private val DiffCallback = object : DiffUtil.ItemCallback<AiChatMessage>() {
            override fun areItemsTheSame(oldItem: AiChatMessage, newItem: AiChatMessage): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: AiChatMessage, newItem: AiChatMessage): Boolean =
                oldItem == newItem
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (getItem(position).isUser) VIEW_TYPE_USER else VIEW_TYPE_BOT
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_TYPE_USER) {
            val view = inflater.inflate(R.layout.item_ai_chat_user, parent, false)
            UserMessageViewHolder(view)
        } else {
            val view = inflater.inflate(R.layout.item_ai_chat_bot, parent, false)
            BotMessageViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = getItem(position)
        if (holder is UserMessageViewHolder) {
            holder.bind(message)
        } else if (holder is BotMessageViewHolder) {
            holder.bind(message)
        }
    }

    class UserMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvMessage: TextView = itemView.findViewById(R.id.tvUserMessage)
        private val tvTimestamp: TextView = itemView.findViewById(R.id.tvUserTimestamp)
        private val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

        fun bind(message: AiChatMessage) {
            tvMessage.text = message.text
            tvTimestamp.text = timeFormat.format(Date(message.timestamp))
        }
    }

    class BotMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvMessage: TextView = itemView.findViewById(R.id.tvBotMessage)
        private val tvTimestamp: TextView = itemView.findViewById(R.id.tvBotTimestamp)
        private val tvLatency: TextView = itemView.findViewById(R.id.tvLatency)
        private val tvModelBadge: TextView = itemView.findViewById(R.id.tvModelBadge)
        private val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

        fun bind(message: AiChatMessage) {
            tvMessage.text = message.text
            tvTimestamp.text = timeFormat.format(Date(message.timestamp))

            if (message.latencyMs != null && message.latencyMs > 0) {
                tvLatency.visibility = View.VISIBLE
                val tokensStr = message.tokensGenerated?.let { " • ${it}t" } ?: ""
                tvLatency.text = "${message.latencyMs.toInt()}ms$tokensStr"
            } else {
                tvLatency.visibility = View.GONE
            }

            if (message.isError) {
                tvModelBadge.text = "Connection Error"
                tvModelBadge.setBackgroundResource(R.drawable.bg_badge_red)
            } else {
                tvModelBadge.text = "PyTorch Micro-GPT"
                tvModelBadge.setBackgroundResource(R.drawable.bg_badge_pill_inactive)
            }
        }
    }
}
