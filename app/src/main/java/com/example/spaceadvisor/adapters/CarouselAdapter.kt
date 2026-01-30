package com.example.spaceadvisor.adapters

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.spaceadvisor.data.Destination
import com.example.spaceadvisor.R
import com.google.android.material.button.MaterialButton

class CarouselAdapter(private var items: List<Destination>) :
    RecyclerView.Adapter<CarouselAdapter.ViewHolder>() {

    private var onItemClickListener: ((Destination) -> Unit)? = null
    private var onDetailsClickListener: ((Destination) -> Unit)? = null

    fun setOnItemClickListener(listener: (Destination) -> Unit) {
        onItemClickListener = listener
    }

    fun setOnDetailsClickListener(listener: (Destination) -> Unit) {
        onDetailsClickListener = listener
    }

    fun updateData(newItems: List<Destination>) {
        this.items = newItems
        notifyDataSetChanged()
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val image: ImageView = view.findViewById(R.id.carouselImage)
        val title: TextView = view.findViewById(R.id.carouselTitle)
        val subtitle: TextView = view.findViewById(R.id.carouselSubtitle)
        val type: TextView = view.findViewById(R.id.carouselType)
        val ratingAvg: TextView = view.findViewById(R.id.carouselRating)
        val selectButton: MaterialButton = view.findViewById(R.id.selectBtn)
        val detailsButton: MaterialButton = view.findViewById(R.id.detailsBtn)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_carousel, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.title.text = item.title
        holder.subtitle.text = item.subtitle
        holder.type.text = if (item.type == "BODY") item.bodyType else item.type
        holder.ratingAvg.text = "★ ${item.ratingAvg}"

        if (item.imageUrl.isNotEmpty()) {
            Glide.with(holder.itemView.context)
                .load(item.imageUrl)
                .placeholder(R.drawable.ic_home_earth)
                .into(holder.image)
        }

        holder.selectButton.setOnClickListener {
            onItemClickListener?.invoke(item)
        }

        holder.detailsButton.setOnClickListener {
            onDetailsClickListener?.invoke(item)
        }
    }

    override fun getItemCount() = items.size
}
