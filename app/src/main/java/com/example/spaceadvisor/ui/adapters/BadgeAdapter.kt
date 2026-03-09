package com.example.spaceadvisor.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.spaceadvisor.databinding.ItemBadgeBinding
import com.example.spaceadvisor.domain.models.Badge

class BadgeAdapter(
    private var badges: List<Badge> = listOf(),
) : RecyclerView.Adapter<BadgeAdapter.BadgeViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BadgeViewHolder {
        val binding = ItemBadgeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BadgeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BadgeViewHolder, position: Int) {
        val badge = badges[position]

        holder.binding.badgeName.text = badge.name

        holder.binding.badgeCard.setOnClickListener {
            holder.binding.badgeDescriptionText.text = badge.description
            holder.binding.badgeDescriptionLayout.visibility =
                if (holder.binding.badgeDescriptionLayout.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }

        val assetPath = "file:///android_asset/${badge.assetPath}"

        Glide.with(holder.itemView.context)
            .load(assetPath)
            .centerCrop()
            .into(holder.binding.badgeImage)
    }

    override fun getItemCount(): Int = badges.size

    fun updateBadges(newBadges: List<Badge>) {
        this.badges = newBadges
        notifyDataSetChanged()
    }

    class BadgeViewHolder(val binding: ItemBadgeBinding) : RecyclerView.ViewHolder(binding.root)
}