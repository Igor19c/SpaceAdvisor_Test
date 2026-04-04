package com.example.spaceadvisor.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.spaceadvisor.R
import com.example.spaceadvisor.databinding.ItemExploreSearchBinding
import com.example.spaceadvisor.domain.models.Destination
import com.example.spaceadvisor.ui.fragments.DestinationTypes
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

class SearchDestinationsAdapter(
    private var destinations: List<Destination>,
    private val onDetailsClick: (Destination) -> Unit
) : RecyclerView.Adapter<SearchDestinationsAdapter.SearchDestinationsViewHolder>() {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): SearchDestinationsViewHolder {
        val binding = ItemExploreSearchBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return SearchDestinationsViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: SearchDestinationsViewHolder,
        position: Int
    ) {
        val item = destinations[position]
        holder.bind(item)
    }

    inner class SearchDestinationsViewHolder(private val binding: ItemExploreSearchBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(destination: Destination) {
            binding.titleItemExploreSearch.text = destination.title
            binding.subtitleItemExploreSearch.text = destination.subtitle
            binding.descriptionItemExploreSearch.text = destination.shortDescription
            binding.typeChipItemExploreSearch.text = destination.type

            if (destination.imageUrl.isNotEmpty()) {
                val image = binding.imageItemExploreSearch
                if (destination.type == "PLANET")
                    image.scaleType = ImageView.ScaleType.CENTER_INSIDE
                else
                    image.scaleType = ImageView.ScaleType.CENTER_CROP

                Glide.with(binding.root.context)
                    .load(destination.imageUrl)
                    .placeholder(R.drawable.ic_home_earth)
                    .into(image)
            }

            binding.typeChipItemExploreSearch.text =
                destination.type.lowercase().replaceFirstChar { it.uppercase() }

            binding.difficultyChipItemExploreSearch.text = when (destination.difficulty) {
                1 -> "Easy"
                2 -> "Medium"
                3 -> "Hard"
                else -> ""
            }

            val tagChipGroup = binding.destTagsChipGroupItemExploreSearch
            tagChipGroup.removeAllViews()

            for (tag in destination.tags) {
                val formattedName = tag.lowercase().replaceFirstChar { it.uppercase() }
                addChip(tagChipGroup, formattedName)
            }
            
            binding.btnViewDetailItemExploreSearch.setOnClickListener {
                onDetailsClick(destination)
            }
        }

        private fun addChip(tagChipGroup: ChipGroup, label: String) {
            val chip = LayoutInflater.from(tagChipGroup.context)
                .inflate(R.layout.layout_single_chip, tagChipGroup, false) as Chip
            chip.isClickable = false
            chip.apply {
                this.text = label
                this.id = View.generateViewId()
            }
            tagChipGroup.addView(chip)
        }
    }

    override fun getItemCount() = destinations.size

    fun updateData(newList: List<Destination>) {
        this.destinations = newList.toMutableList()
        notifyDataSetChanged()
    }

}
