package com.example.spaceadvisor.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.spaceadvisor.R
import com.example.spaceadvisor.domain.models.Destination

class TripDestinationsAdapter(
    private var destinations: MutableList<Destination>,
    private val onImageClick: (ImageView, String) -> Unit
) : RecyclerView.Adapter<TripDestinationsAdapter.ViewHolder>() {

    private var isEditMode: Boolean = false

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cardContainer: View = view.findViewById(R.id.card_container)
        val image: ImageView = view.findViewById(R.id.trip_destination_image)
        val title: TextView = view.findViewById(R.id.trip_destination_title)
        val subtitle: TextView = view.findViewById(R.id.trip_destination_subtitle)
        val ratings: TextView = view.findViewById(R.id.trip_destination_rating)
        val difficultyIconContainer: LinearLayout =
            view.findViewById(R.id.trip_dest_difficulty_container)
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
        setupDifficultyIcons(holder.difficultyIconContainer, item.difficulty)

        holder.ratings.text = "${item.ratingAvg}"

        if (item.imageUrl.isNotEmpty()) {
            Glide.with(holder.itemView.context)
                .load(item.imageUrl)
                .placeholder(R.drawable.ic_home_earth)
                .into(holder.image)
        }

        holder.image.setOnClickListener {
            if (item.imageUrl.isNotEmpty()) {
                onImageClick(holder.image, item.imageUrl)
            }
        }

        holder.cardContainer.translationX = 0f
    }

    private fun setupDifficultyIcons(container: LinearLayout, difficulty: Int) {
        container.removeAllViews()

        val icons = listOf(
            R.drawable.ic_easy_emoji,
            R.drawable.ic_medium_emoji,
            R.drawable.ic_hard_emoji
        )

        for (i in 1..3) {
            val iconView = ImageView(container.context)
            val size = (14 * container.context.resources.displayMetrics.density).toInt()
            val params = LinearLayout.LayoutParams(size, size)
            params.setMargins(0, 0, 14, 0)
            iconView.layoutParams = params

            iconView.setImageResource(icons[i - 1])

            val tintColorRes = if (i == difficulty) {
                when (difficulty) {
                    1 -> R.color.green_light
                    2 -> R.color.yellow_accent
                    3 -> R.color.red_muted
                    else -> R.color.body
                }
            } else {
                R.color.body
            }

            iconView.setColorFilter(
                ContextCompat.getColor(container.context, tintColorRes),
                android.graphics.PorterDuff.Mode.SRC_IN
            )
            container.addView(iconView)
        }
    }

    override fun getItemCount() = destinations.size

    fun updateData(newList: List<Destination>) {
        this.destinations = newList.toMutableList()
        notifyDataSetChanged()
    }

    fun setEditMode(enabled: Boolean) {
        this.isEditMode = enabled
        notifyDataSetChanged()
    }

    fun getDestinationAt(position: Int): Destination {
        return destinations[position]
    }
}
