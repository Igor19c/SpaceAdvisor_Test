package com.example.spaceadvisor.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.spaceadvisor.R
import com.example.spaceadvisor.data.Destination
import com.google.android.material.button.MaterialButton
import com.google.android.material.imageview.ShapeableImageView

class TripAdapter(
    private var destinations: MutableList<Destination>,
    private val onRemoveClick: (Destination) -> Unit
) : RecyclerView.Adapter<TripAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val image: ShapeableImageView = view.findViewById(R.id.destination_image_item)
        val title: TextView = view.findViewById(R.id.destination_title_item)
        val subtitle: TextView = view.findViewById(R.id.destination_subtitle_item)
        val removeBtn: MaterialButton = view.findViewById(R.id.remove_dest_btn)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_trip_destination, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = destinations[position]
        holder.title.text = item.title
        holder.subtitle.text = item.subtitle

        if (item.imageUrl.isNotEmpty()) {
            Glide.with(holder.itemView.context)
                .load(item.imageUrl)
                .placeholder(R.drawable.ic_home_earth)
                .into(holder.image)
        }

        holder.removeBtn.setOnClickListener { onRemoveClick(item) }
    }

    override fun getItemCount() = destinations.size

    fun updateData(newList: List<Destination>) {
        this.destinations = newList.toMutableList()
        notifyDataSetChanged()
    }
}
