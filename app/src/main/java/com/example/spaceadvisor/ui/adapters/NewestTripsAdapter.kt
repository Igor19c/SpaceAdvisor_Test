package com.example.spaceadvisor.ui.adapters

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.spaceadvisor.R
import com.example.spaceadvisor.domain.models.Trip
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

class NewestTripsAdapter(
    private var trips: List<Trip>,
    private val onCardClick: (Trip) -> Unit
) : RecyclerView.Adapter<NewestTripsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val image: ImageView = view.findViewById(R.id.item_image)
        val title: TextView = view.findViewById(R.id.trip_title_item)
        val date: TextView = view.findViewById(R.id.trip_created_at)
        val stops: TextView = view.findViewById(R.id.trip_stop_count)
        val tripCard: View = view.findViewById(R.id.newest_trip_card_item)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_newest_trip, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = trips[position]
        holder.title.text = item.title
        holder.stops.text = "${item.destinationIds.size} stops"

        item.createdAt?.let {
            holder.date.text = DateUtils.getRelativeTimeSpanString(it)
        } ?: run {
            holder.date.text = ""
        }

        if (item.imageUrl.isNotEmpty()) {
            Glide.with(holder.itemView.context)
                .load(item.imageUrl)
                .placeholder(R.drawable.ic_home_earth)
                .into(holder.image)
        }

        holder.tripCard.setOnClickListener { onCardClick(item) }
    }

    override fun getItemCount() = trips.size

    fun updateData(newList: List<Trip>) {
        this.trips = newList
        notifyDataSetChanged()
    }
}
