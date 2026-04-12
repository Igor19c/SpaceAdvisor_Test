package com.example.spaceadvisor.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.spaceadvisor.R
import com.example.spaceadvisor.SpaceAdvisorApplication
import com.example.spaceadvisor.domain.models.Destination
import com.example.spaceadvisor.domain.models.DestinationDetails
import com.example.spaceadvisor.domain.models.Review
import com.example.spaceadvisor.domain.models.Safety
import com.example.spaceadvisor.databinding.FragmentDestinationBinding
import com.example.spaceadvisor.databinding.ItemDestHazardBinding
import com.example.spaceadvisor.domain.models.UIConfig
import com.example.spaceadvisor.ui.adapters.ReviewAdapter
import com.example.spaceadvisor.ui.adapters.DestinationsAdapter
import com.example.spaceadvisor.utils.TripSelectionHelper
import com.example.spaceadvisor.ui.viewmodels.DestinationViewModel
import com.example.spaceadvisor.ui.viewmodels.TripViewModel
import com.example.spaceadvisor.ui.viewmodels.UserViewModel
import com.example.spaceadvisor.ui.viewmodels.ViewModelFactory
import com.google.android.material.chip.Chip
import com.google.android.material.tabs.TabLayout

class DestinationFragment : BaseFragment() {

    private var _binding: FragmentDestinationBinding? = null
    private val binding get() = _binding!!

    private val destinationViewModel: DestinationViewModel by activityViewModels {
        ViewModelFactory(requireActivity().application as SpaceAdvisorApplication)
    }
    private val userViewModel: UserViewModel by activityViewModels {
        ViewModelFactory(requireActivity().application as SpaceAdvisorApplication)
    }
    private val tripViewModel: TripViewModel by activityViewModels {
        ViewModelFactory(requireActivity().application as SpaceAdvisorApplication)
    }

    private lateinit var childrenAdapter: DestinationsAdapter
    private lateinit var reviewAdapter: ReviewAdapter
    private lateinit var tripSelectionHelper: TripSelectionHelper
    private var pendingDestinationToAdd: Destination? = null

    override fun getUIConfig() = UIConfig(
        isHeaderVisible = false,
        isBottomNavVisible = false,
        selectedTabId = R.id.nav_explore
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDestinationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tripSelectionHelper = TripSelectionHelper(this, tripViewModel) { destination ->
            pendingDestinationToAdd = destination
        }

        val destination = arguments?.getSerializable("destination_key") as? Destination

        setupAdapters()
        setupTabs()

        destination?.let { dest ->
            setupInitialUI(dest)
            observeViewModel(dest.id)
            destinationViewModel.fetchFullDetails(dest.id)
            setupButtons(dest)
            setupReviewSection(dest)
            destinationViewModel.fetchDestinationReviews(destination.id)

            userViewModel.getCurrentUid()?.let { uid ->
                destinationViewModel.fetchSavedDestinations(uid)
                tripViewModel.fetchUserTrips(uid)
            }

            binding.destSaveDestinationBtn.setOnClickListener {
                userViewModel.getCurrentUid()?.let { uid ->
                    destinationViewModel.toggleSaveDestination(uid, dest)
                } ?: showError("Please login to save")
            }
        }


        binding.destBackBtn.setOnClickListener { parentFragmentManager.popBackStack() }
    }

    private fun setupPriceTierIcons(container: LinearLayout, level: Int) {
        container.removeAllViews()
        for (i in 1..4) {
            val icon = ImageView(requireContext())
            val size = (18 * resources.displayMetrics.density).toInt()
            val params = LinearLayout.LayoutParams(size, size)
            if (i < 4)
                params.setMargins(0, 0, -6, 0)
            icon.layoutParams = params

            val iconRes = R.drawable.ic_dollar_banknote
            icon.setImageResource(iconRes)

            val tintColor = if (i <= level) {
                resources.getColor(R.color.space_green_light, null)
            } else {
                resources.getColor(R.color.space_green_light_faded, null)
            }
            icon.setColorFilter(tintColor, android.graphics.PorterDuff.Mode.SRC_IN)
            container.addView(icon)
        }
    }

    private fun setupTabs() {
        binding.destHighlightsContent.visibility = View.VISIBLE
        binding.destEnvironmentContent.visibility = View.GONE
        binding.destSafetyContent.visibility = View.GONE

        binding.destTabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                binding.destHighlightsContent.visibility = View.GONE
                binding.destEnvironmentContent.visibility = View.GONE
                binding.destSafetyContent.visibility = View.GONE

                when (tab?.position) {
                    0 -> binding.destHighlightsContent.visibility = View.VISIBLE
                    1 -> binding.destEnvironmentContent.visibility = View.VISIBLE
                    2 -> binding.destSafetyContent.visibility = View.VISIBLE
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupAdapters() {
        childrenAdapter = DestinationsAdapter(mutableListOf()) { destination ->
            navigateToDestination(destination)
        }
        binding.destChildrenRecycleView.apply {
            layoutManager =
                LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = childrenAdapter
        }

        reviewAdapter = ReviewAdapter(listOf())
        binding.reviewsRecyclerViewDestFragment.layoutManager =
            LinearLayoutManager(requireContext())
        binding.reviewsRecyclerViewDestFragment.adapter = reviewAdapter
    }

    private fun setupInitialUI(destination: Destination) {
        binding.destTitleText.text = destination.title
        binding.destSubtitleText.text = destination.subtitle
        updateRatingUI(destination.ratingAvg, destination.ratingCount)

        if (destination.imageUrl.isNotEmpty()) {
            Glide.with(this)
                .load(destination.imageUrl)
                .placeholder(R.drawable.ic_home_earth)
                .into(binding.destImage)
        }

        binding.destTagsChipGroup.removeAllViews()
        destination.tags.forEach { tagName ->
            val chip = LayoutInflater.from(requireContext())
                .inflate(R.layout.layout_single_chip, binding.destTagsChipGroup, false) as Chip
            chip.text = tagName
            binding.destTagsChipGroup.addView(chip)
        }

        setupPriceTierIcons(binding.destPriceTierContainer, destination.priceTier)

        binding.destShortDescriptionText.text = destination.shortDescription


        if (destination.childCount == 0) {
            binding.destChildrenSection.visibility = View.GONE
        } else {
            binding.destChildrenSection.visibility = View.VISIBLE
            binding.destChildrenTitleText.text = "${destination.title} destinations"
            destinationViewModel.fetchSubDestinations(destination.id)
        }
    }

    private fun navigateToDestination(destination: Destination) {
        val fragment = DestinationFragment().apply {
            arguments = Bundle().apply {
                putSerializable("destination_key", destination)
            }
        }
        parentFragmentManager.beginTransaction()
            .replace(R.id.main_frame, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun updateRatingUI(avg: Double, count: Int) {
        binding.destRatingText.text = String.format("%.1f", avg)
    }

    private fun observeViewModel(destinationId: String) {
        destinationViewModel.destinationDetails.observe(viewLifecycleOwner) { details ->
            details?.let { updateDetailsUI(it) }
        }

        destinationViewModel.subDestinations.observe(viewLifecycleOwner) { list ->
            childrenAdapter.updateData(list)
        }

        destinationViewModel.savedDestinations.observe(viewLifecycleOwner) { savedList ->
            val isSaved = savedList.any { it.id == destinationId }
            updateSaveButtonIcon(isSaved)
        }

        destinationViewModel.saveStatus.observe(viewLifecycleOwner) { statusPair ->
            val (isSaved, message) = statusPair
            val title = if (isSaved) "Added to Favorites" else "Removed from Favorites"

            showCustomMessage(title, message)
        }

        destinationViewModel.reviewSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                showCustomMessage("Review shared!", "Thank you for your feedback.")
                clearReviewFields()
                destinationViewModel.resetReviewStatus()
                destinationViewModel.fetchFullDetails(destinationId)
            }
        }

        tripViewModel.currentTrip.observe(viewLifecycleOwner) { trip ->
            if (trip != null && trip.id.isNotEmpty() && pendingDestinationToAdd != null) {
                tripViewModel.addDestination(pendingDestinationToAdd!!)
                pendingDestinationToAdd = null
            }
        }

        destinationViewModel.destReviews.observe(viewLifecycleOwner) { reviews ->
            if (reviews != null) {
                val count = reviews.size
                val avg = if (count > 0) reviews.sumOf { it.rating.toDouble() } / count else 0.0

                binding.destReviewCountDestFragment.text = "${count} reviews"
                binding.destAvgRatingDestFragment.text = String.format("%.1f", avg)
                updateGlobalStars(avg)

                val limitedReviews = reviews.take(5)
                reviewAdapter.updateData(limitedReviews)
            }
        }
    }

    private fun updateGlobalStars(rating: Double) {
        val stars = listOf(
            binding.ratingStar01DestFragment,
            binding.ratingStar02DestFragment,
            binding.ratingStar03DestFragment,
            binding.ratingStar04DestFragment,
            binding.ratingStar05DestFragment
        )

        for (i in stars.indices) {
            val starLevel = i + 1
            when {
                rating >= starLevel -> {
                    stars[i].setImageResource(R.drawable.ic_rating_star_filled)
                }

                rating >= starLevel - 0.5 -> {
                    stars[i].setImageResource(R.drawable.ic_rating_star_half_filled)
                }

                else -> {
                    stars[i].setImageResource(R.drawable.ic_rating_star_outlined)
                }
            }
        }
    }

    private fun setupReviewSection(destination: Destination) {
        binding.destSubmitReviewBtn.setOnClickListener {
            val user = userViewModel.userData.value
            if (user == null) {
                showError("Please login to leave a review")
                return@setOnClickListener
            }

            val rating = binding.destReviewRatingBar.rating.toInt()
            if (rating == 0) {
                showError("Please select a rating")
                return@setOnClickListener
            }

            val review = Review(
                destinationId = destination.id,
                destinationTitle = destination.title,
                uid = user.uid,
                username = user.username.ifEmpty { user.name },
                userProfileImage = user.profileImageUrl,
                rating = rating,
                comment = binding.destEditReviewContent.text.toString()
            )
            destinationViewModel.submitReview(review)
        }

        binding.destClearReviewBtn.setOnClickListener {
            clearReviewFields()
        }
    }

    private fun clearReviewFields() {
        binding.destReviewRatingBar.rating = 0f
        binding.destEditReviewContent.setText("")
    }

    private fun updateSaveButtonIcon(isSaved: Boolean) {
        val iconRes = if (isSaved) R.drawable.ic_star_filled else R.drawable.ic_star_outlined
        binding.destSaveDestinationBtn.icon = resources.getDrawable(iconRes, null)
    }

    private fun setupButtons(destination: Destination) {
        val canExplore = destination.childCount > 0
        binding.destAddBtn.text = if (canExplore) "Explore ${destination.title}" else "Add to Trip"

        binding.destAddBtn.setOnClickListener {
            if (canExplore) {
                val nextFragment = ExploreFragment().apply {
                    arguments = Bundle().apply {
                        putSerializable("target_destination", destination)
                    }
                }
                parentFragmentManager.beginTransaction()
                    .add(R.id.main_frame, nextFragment)
                    .hide(this)
                    .addToBackStack(null)
                    .commit()
            } else {
                tripSelectionHelper.handleAddToTrip(destination)
            }
        }
    }

    private fun updateDetailsUI(details: DestinationDetails) {
        if (details.longDescription.isNotEmpty()) {
            binding.destLongDescriptionText.text = details.longDescription
        }

        if (details.bestTimeToVisit.isNotEmpty()) {
            binding.destBestTimeToVisitText.text = details.bestTimeToVisit
        }

        binding.destHighlightsContainer.removeAllViews()
        if (details.highlights.isNotEmpty()) {
            details.highlights.forEach { highlight ->
                val itemView = layoutInflater.inflate(
                    R.layout.item_bullet,
                    binding.destHighlightsContainer,
                    false
                )
                itemView.findViewById<TextView>(R.id.bullet_text).text = highlight
                binding.destHighlightsContainer.addView(itemView)
            }
        }

        val env = details.environment ?: return
        binding.envAtmosphereValue.text = env.atmosphere
        binding.envGravityValue.text = "${env.gravityG} G"
        binding.envTempValue.text = if (env.temperatureC != null)
            "${env.temperatureC.min}°C to ${env.temperatureC.max}°C" else "N/A"
        binding.envRadiationValue.text = env.radiation
        binding.envDayLengthValue.text = "${env.dayLengthHours}h"
        binding.envVisibilityValue.text = env.visibility

        details.safety?.let { updateSafetyUI(it) }

        binding.destSafetyNotesContainer.removeAllViews()
        if (details.safety!!.notes.isNotEmpty()) {
            details.safety.notes.forEach { highlight ->
                val itemView = layoutInflater.inflate(
                    R.layout.item_bullet,
                    binding.destSafetyNotesContainer,
                    false
                )
                itemView.findViewById<TextView>(R.id.bullet_text).text = highlight
                binding.destSafetyNotesContainer.addView(itemView)
            }
        }
    }

    private fun updateSafetyUI(safety: Safety) {
        binding.destHazardsContainer.removeAllViews()
        safety.hazards.forEach { hazard ->
            val itemBinding =
                ItemDestHazardBinding.inflate(layoutInflater, binding.destHazardsContainer, false)
            itemBinding.hazardName.text = hazard.name
            itemBinding.hazardTipText.text = hazard.tip

            itemBinding.hazardTipHeader.setOnClickListener {
                val isVisible = itemBinding.hazardTipText.visibility == View.VISIBLE
                if (isVisible) {
                    itemBinding.hazardTipText.visibility = View.GONE
                    itemBinding.hazardTipArrow.animate().rotation(0f).setDuration(200).start()
                } else {
                    itemBinding.hazardTipText.visibility = View.VISIBLE
                    itemBinding.hazardTipArrow.animate().rotation(90f).setDuration(200).start()
                }
            }

            setupHazardStars(itemBinding.hazardStarsContainer, hazard.level)
            binding.destHazardsContainer.addView(itemBinding.root)
        }
    }

    private fun setupHazardStars(container: LinearLayout, level: Int) {
        container.removeAllViews()
        for (i in 1..5) {
            val star = ImageView(requireContext())
            val size = (20 * resources.displayMetrics.density).toInt()
            star.layoutParams = LinearLayout.LayoutParams(size, size)

            val iconRes = R.drawable.ic_hazard
            star.setImageResource(iconRes)

            val tintColor = if (i <= level) {
                resources.getColor(R.color.space_red, null)
            } else {
                resources.getColor(R.color.space_red_faded, null)
            }
            star.setColorFilter(tintColor, android.graphics.PorterDuff.Mode.SRC_IN)
            container.addView(star)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}