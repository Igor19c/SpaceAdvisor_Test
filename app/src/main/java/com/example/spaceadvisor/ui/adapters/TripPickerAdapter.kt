package com.example.spaceadvisor.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.spaceadvisor.R
import com.example.spaceadvisor.domain.models.Trip

class TripPickerAdapter(
    private val trips: List<Trip>,
    private val onTripSelected: (Trip) -> Unit
) : RecyclerView.Adapter<TripPickerAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val image: ImageView = view.findViewById(R.id.image_trip_picker_item)
        val title: TextView = view.findViewById(R.id.title_trip_picker_item)
        val container: View = view.findViewById(R.id.trip_picker_item_card)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_trip_picker, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val trip = trips[position]
        holder.title.text = trip.title
        
        Glide.with(holder.itemView.context)
            .load(trip.imageUrl)
            .placeholder(R.drawable.ic_home_earth)
            .circleCrop()
            .into(holder.image)

        holder.container.setOnClickListener { onTripSelected(trip) }
        holder.itemView.setOnClickListener { onTripSelected(trip) }
    }

    override fun getItemCount() = trips.size
}
