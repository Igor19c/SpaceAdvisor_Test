package com.example.spaceadvisor.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.spaceadvisor.R
import com.example.spaceadvisor.domain.models.Destination

class TrendingDestinationsAdapter(
    private var destinations: List<Destination>,
    private val onCardClick: (Destination) -> Unit
) : RecyclerView.Adapter<TrendingDestinationsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val image: ImageView = view.findViewById(R.id.trend_dest_image_item)
        val title: TextView = view.findViewById(R.id.trend_dest_title_item)
        val subtitle: TextView = view.findViewById(R.id.trend_dest_subtitle_item)
        val ratings: TextView = view.findViewById(R.id.trend_dest_rating_item)
        val difficulty: TextView = view.findViewById(R.id.trend_dest_difficulty_item)
        val destCard: View = view.findViewById(R.id.trend_dest_card_item)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_trending_destination, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {
        val item = destinations[position]
        holder.title.text = item.title
        holder.subtitle.text = item.subtitle
        holder.difficulty.text = "" + item.difficulty
        holder.ratings.text = "" + item.ratingAvg

        if (item.imageUrl.isNotEmpty()) {
            Glide.with(holder.itemView.context)
                .load(item.imageUrl)
                .placeholder(R.drawable.ic_home_earth)
                .into(holder.image)
        }

        holder.destCard.setOnClickListener { onCardClick(item) }
    }


    override fun getItemCount() = destinations.size

    fun updateData(newList: List<Destination>) {
        this.destinations = newList.toMutableList()
        notifyDataSetChanged()
    }
}
