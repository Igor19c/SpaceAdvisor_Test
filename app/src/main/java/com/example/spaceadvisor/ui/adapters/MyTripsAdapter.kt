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
import java.text.SimpleDateFormat
import java.util.*

class MyTripsAdapter(
    private var trips: MutableList<Trip>,
    private val onOpenClick: (Trip) -> Unit,
    private val onConfirmClick: (Trip) -> Unit,
    private val onShareClick: (Trip) -> Unit
) : RecyclerView.Adapter<MyTripsAdapter.ViewHolder>() {

    private var isDeleteMode = false

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cardContainer: View = view.findViewById(R.id.trip_card_item)
        val image1: ImageView = view.findViewById(R.id.trip_dest_image_1)
        val image2: ImageView = view.findViewById(R.id.trip_dest_image_2)
        val image3: ImageView = view.findViewById(R.id.trip_dest_image_3)
        val additionalCounter: TextView = view.findViewById(R.id.additional_trip_dest_counter)

        val title: TextView = view.findViewById(R.id.title_trip_item)
        val tripDates: TextView = view.findViewById(R.id.trip_dates_trip_item)
        val createdAt: TextView = view.findViewById(R.id.created_at_trip_item)
        val stopCount: TextView = view.findViewById(R.id.stop_count_trip_item)

        val draftChip: TextView = view.findViewById(R.id.draft_chip_trip_item)
        val plannedChip: TextView = view.findViewById(R.id.planned_chip_trip_item)
        val completedChip: TextView = view.findViewById(R.id.completed_chip_trip_item)

        val openBtn: MaterialButton = view.findViewById(R.id.open_btn_trip_item)
        val confirmBtn: MaterialButton = view.findViewById(R.id.confirm_btn_trip_item)
        val shareBtn: MaterialButton = view.findViewById(R.id.share_btn_trip_item)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_trip, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val trip = trips[position]
        val context = holder.itemView.context

        holder.title.text = trip.title.ifEmpty { "Unnamed Journey" }
        holder.stopCount.text = "${trip.destinationIds.size} stops"

        if (trip.startDate != null && trip.endDate != null) {
            val sdf = SimpleDateFormat("dd/MM", Locale.getDefault())
            holder.tripDates.text =
                "${sdf.format(Date(trip.startDate))} - ${sdf.format(Date(trip.endDate))}"
            holder.tripDates.visibility = View.VISIBLE
        } else {
            holder.tripDates.visibility = View.GONE
        }

        trip.createdAt?.let {
            holder.createdAt.text = DateUtils.getRelativeTimeSpanString(it)
        }

        val currentTime = System.currentTimeMillis()
        val isCompleted =
            trip.status == "PLANNED" && trip.destinationIds.isNotEmpty() && (trip.startDate
                ?: 0) < currentTime

        // Chips
        holder.draftChip.visibility =
            if (trip.status == "DRAFT" || trip.destinationIds.isEmpty()) View.VISIBLE else View.GONE
        holder.plannedChip.visibility =
            if (trip.status == "PLANNED" && !isCompleted) View.VISIBLE else View.GONE
        holder.completedChip.visibility = if (isCompleted) View.VISIBLE else View.GONE

        // Buttons Visibility
        when {
            trip.status == "DRAFT" || trip.destinationIds.isEmpty() -> {
                holder.openBtn.visibility = View.VISIBLE
                holder.confirmBtn.visibility = View.VISIBLE
                holder.shareBtn.visibility = View.GONE
            }

            else -> {
                holder.openBtn.visibility = View.VISIBLE
                holder.confirmBtn.visibility = View.GONE
                holder.shareBtn.visibility = View.VISIBLE
            }
        }

        // Images
        val images = listOf(holder.image1, holder.image2, holder.image3)
        images.forEach { it.visibility = View.GONE }
        holder.additionalCounter.visibility = View.GONE

        trip.destinationImages.take(3).forEachIndexed { index, url ->
            images[index].visibility = View.VISIBLE
            Glide.with(context).load(url).placeholder(R.drawable.pic_profile).circleCrop()
                .into(images[index])
        }

        if (trip.destinationIds.size > 3) {
            holder.additionalCounter.visibility = View.VISIBLE
            holder.additionalCounter.text = "+${trip.destinationIds.size - 3}"
        }

        holder.openBtn.setOnClickListener { onOpenClick(trip) }
        holder.confirmBtn.setOnClickListener { onConfirmClick(trip) }
        holder.shareBtn.setOnClickListener { onShareClick(trip) }

        // Reset visual state
        holder.cardContainer.translationX = 0f
        holder.itemView.alpha = 1f
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.contains("RESET_SWIPE")) {
            holder.cardContainer.translationX = 0f
            holder.itemView.alpha = 1f
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    override fun getItemCount() = trips.size

    fun updateData(newList: List<Trip>) {
        this.trips = newList.toMutableList()
        notifyDataSetChanged()
    }

    fun setDeleteMode(enabled: Boolean) {
        this.isDeleteMode = enabled
        notifyDataSetChanged()
    }

    fun getTripAt(position: Int): Trip {
        return trips[position]
    }
}
