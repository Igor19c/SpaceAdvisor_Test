package com.example.spaceadvisor.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.spaceadvisor.R
import com.example.spaceadvisor.SpaceAdvisorApplication
import com.example.spaceadvisor.databinding.FragmentHomeBinding
import com.example.spaceadvisor.domain.models.Destination
import com.example.spaceadvisor.domain.models.UIConfig
import com.example.spaceadvisor.ui.adapters.ReviewAdapter
import com.example.spaceadvisor.ui.adapters.DestinationsAdapter
import com.example.spaceadvisor.ui.viewmodels.DestinationViewModel
import com.example.spaceadvisor.ui.viewmodels.ViewModelFactory

class HomeFragment : BaseFragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var reviewAdapter: ReviewAdapter

    private lateinit var trendingDestinationsAdapter: DestinationsAdapter

    private val destinationViewModel: DestinationViewModel by activityViewModels {
        ViewModelFactory(requireActivity().application as SpaceAdvisorApplication)
    }

    override fun getUIConfig() = UIConfig(
        title = "ASTRYX",
        selectedTabId = R.id.place_holder
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupTrendingDestinationsRecyclerView()
        setupRecyclerView()
        observeViewModel()

        destinationViewModel.fetchDestinationsByRating()
        destinationViewModel.fetchHomeReviews()

        val scrollView = binding.nestedScrollViewHomeFragment
        val title = binding.titleHomeFragment
        val subtitle = binding.subtitleHomeFragment

        scrollView.setOnScrollChangeListener { v, scrollX, scrollY, oldScrollX, oldScrollY ->
            val alpha = 1f - (scrollY.toFloat() / 800f)
            title.alpha = alpha.coerceIn(0f, 1f)
            subtitle.alpha = alpha.coerceIn(0f, 1f)

        }
//        binding.highlightsSaveIconHomeFragment.applyGradientTint()
//        binding.highlightsTripIconHomeFragment.applyGradientTint()
//        binding.highlightsExploreIconHomeFragment.applyGradientTint()

//        binding.createTripBtn.setOnClickListener {
//            tripViewModel.createNewTrip()
//            EditTripDialogFragment.newInstance()
//                .show(parentFragmentManager, "CREATE_TRIP_DIALOG")
//        }


    }

    private fun setupRecyclerView() {
        reviewAdapter = ReviewAdapter(listOf())
        binding.reviewsRecyclerViewHomeFragment.layoutManager =
            LinearLayoutManager(requireContext())
        binding.reviewsRecyclerViewHomeFragment.adapter = reviewAdapter
    }

    private fun observeViewModel() {
        destinationViewModel.trendingDestinations.observe(viewLifecycleOwner) { destinations ->
            trendingDestinationsAdapter.updateData(destinations)
        }

        destinationViewModel.allReviews.observe(viewLifecycleOwner) { reviews ->
            if (reviews != null) {
                val limitedReviews = reviews.take(5)
                reviewAdapter.updateData(limitedReviews)

                val count = reviews.size
                val avg = if (count > 0) reviews.sumOf { it.rating.toDouble() } / count else 0.0

                binding.reviewCountHomeFragment.text = "${count} reviews"
                binding.globalAvgRatingHomeFragment.text = String.format("%.1f", avg)
                updateGlobalStars(avg)
            }
        }
    }

    private fun updateGlobalStars(rating: Double) {
        val stars = listOf(
            binding.ratingStar01HomeFragment,
            binding.ratingStar02HomeFragment,
            binding.ratingStar03HomeFragment,
            binding.ratingStar04HomeFragment,
            binding.ratingStar05HomeFragment
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

    private fun setupTrendingDestinationsRecyclerView() {
        trendingDestinationsAdapter = DestinationsAdapter(mutableListOf()) { destination ->
            navigateToDestination(destination)
        }
        binding.trendingRecyclerViewHomeFragment.layoutManager = LinearLayoutManager(
            requireContext(),
            LinearLayoutManager.HORIZONTAL, false
        )
        binding.trendingRecyclerViewHomeFragment.adapter = trendingDestinationsAdapter
    }

    private fun navigateToDestination(destination: Destination) {
        val fragment = DestinationFragment().apply {
            arguments = Bundle().apply {
                putSerializable("destination_key", destination)
            }
        }

        parentFragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.slide_in_bottom_to_top,
                R.anim.fade_out,
                R.anim.fade_in,
                R.anim.slide_out_top_to_bottom
            )
            .add(R.id.main_frame, fragment)
            .hide(this)
            .addToBackStack(null)
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
