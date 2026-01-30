package com.example.spaceadvisor.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.spaceadvisor.databinding.ItemBadgeBinding

class BadgeAdapter(
    private var badges: List<String>,
) : RecyclerView.Adapter<BadgeAdapter.BadgeViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BadgeViewHolder {
        val binding = ItemBadgeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BadgeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BadgeViewHolder, position: Int) {
        val badgeName = badges[position]
        val cleanName =
            badgeName.substringBeforeLast(".").replace("_", " ").replaceFirstChar { it.uppercase() }
        holder.binding.badgeName.text = cleanName

        val assetPath = "file:///android_asset/badges/$badgeName"

        Glide.with(holder.itemView.context)
            .load(assetPath)
            .centerCrop()
            .into(holder.binding.badgeImage)
    }

    override fun getItemCount(): Int = badges.size

    fun updateBadges(newBadges: List<String>) {
        this.badges = newBadges
        notifyDataSetChanged()
    }

    class BadgeViewHolder(val binding: ItemBadgeBinding) : RecyclerView.ViewHolder(binding.root)
}
