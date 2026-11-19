package com.example.twelveman.ui.tournaments

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.twelveman.databinding.ItemCommentaryBinding

class CommentaryAdapter(
    private var commentaryList: List<String> = emptyList()
) : RecyclerView.Adapter<CommentaryAdapter.CommentaryViewHolder>() {

    fun updateData(newList: List<String>) {
        commentaryList = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentaryViewHolder {
        val binding = ItemCommentaryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CommentaryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CommentaryViewHolder, position: Int) {
        holder.binding.tvCommentaryText.text = commentaryList[position]
    }

    override fun getItemCount(): Int = commentaryList.size

    class CommentaryViewHolder(val binding: ItemCommentaryBinding) : RecyclerView.ViewHolder(binding.root)
}
