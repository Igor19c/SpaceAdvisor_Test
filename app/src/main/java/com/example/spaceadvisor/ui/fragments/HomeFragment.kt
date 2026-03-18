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
import com.example.spaceadvisor.ui.UIConfig
import com.example.spaceadvisor.ui.adapters.TrendingDestinationsAdapter
import com.example.spaceadvisor.ui.viewmodels.DestinationViewModel
import com.example.spaceadvisor.ui.viewmodels.TripViewModel
import com.example.spaceadvisor.ui.viewmodels.ViewModelFactory

class HomeFragment : BaseFragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var trendingDestinationsAdapter: TrendingDestinationsAdapter

    private val tripViewModel: TripViewModel by activityViewModels {
        ViewModelFactory(requireActivity().application as SpaceAdvisorApplication)
    }

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
        observeViewModel()

        destinationViewModel.fetchDestinationsByRating()

        binding.createTripBtn.setOnClickListener {
            tripViewModel.createNewTrip()
            EditTripDialogFragment.newInstance()
                .show(parentFragmentManager, "CREATE_TRIP_DIALOG")
        }
    }

    private fun observeViewModel() {
        destinationViewModel.trendingDestinations.observe(viewLifecycleOwner) { destinations ->
            trendingDestinationsAdapter.updateData(destinations)
        }
    }

    private fun setupTrendingDestinationsRecyclerView() {
        trendingDestinationsAdapter = TrendingDestinationsAdapter(mutableListOf()) { destination ->
            navigateToDestination(destination)
        }
        binding.trendingRecyclerView.layoutManager = LinearLayoutManager(
            requireContext(),
            LinearLayoutManager.HORIZONTAL, false
        )
        binding.trendingRecyclerView.adapter = trendingDestinationsAdapter
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
