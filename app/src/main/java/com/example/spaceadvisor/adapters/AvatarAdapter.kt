package com.example.spaceadvisor.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.spaceadvisor.databinding.ItemAvatarBinding

class AvatarAdapter(
    private val avatars: List<String>,
    private val onPlusClick: () -> Unit,
    private val onAvatarClick: (String) -> Unit
) : RecyclerView.Adapter<AvatarAdapter.AvatarViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AvatarViewHolder {
        val binding = ItemAvatarBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AvatarViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AvatarViewHolder, position: Int) {
        if (position == 0) {
            // הכפתור הראשון הוא כפתור הפלוס
            holder.binding.avatarImage.setImageDrawable(null)
            holder.binding.plusIcon.visibility = View.VISIBLE
            holder.itemView.setOnClickListener { onPlusClick() }
        } else {
            // שאר הפריטים הם אווטארים (מיקום פחות 1 בגלל הפלוס)
            holder.binding.plusIcon.visibility = View.GONE
            val avatarName = avatars[position - 1]
            val assetPath = "file:///android_asset/avatars/$avatarName"
            
            Glide.with(holder.itemView.context)
                .load(assetPath)
                .circleCrop()
                .into(holder.binding.avatarImage)

            holder.itemView.setOnClickListener { onAvatarClick(assetPath) }
        }
    }

    override fun getItemCount(): Int = avatars.size + 1

    class AvatarViewHolder(val binding: ItemAvatarBinding) : RecyclerView.ViewHolder(binding.root)
}
