package com.example.spaceadvisor.ui.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.example.spaceadvisor.R
import com.example.spaceadvisor.SpaceAdvisorApplication
import com.example.spaceadvisor.databinding.FragmentTripSelectBinding
import com.example.spaceadvisor.domain.models.Destination
import com.example.spaceadvisor.domain.models.Trip
import com.example.spaceadvisor.ui.adapters.TripPickerAdapter
import com.example.spaceadvisor.ui.fragments.PostCreateFragment
import com.example.spaceadvisor.ui.viewmodels.TripViewModel
import com.example.spaceadvisor.ui.viewmodels.ViewModelFactory
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class SelectDialogTrip : BottomSheetDialogFragment() {

    private var _binding: FragmentTripSelectBinding? = null
    private val binding get() = _binding!!

    private val tripViewModel: TripViewModel by activityViewModels {
        ViewModelFactory(requireActivity().application as SpaceAdvisorApplication)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTripSelectBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val destinationToAdd =
            arguments?.getSerializable("destination_to_add") as? Destination

        var trips: List<Trip>
        if (destinationToAdd != null) {
            trips = (tripViewModel.userTrips.value ?: emptyList()).filter { it.isDraft }
        } else
            trips = tripViewModel.userTrips.value ?: emptyList()

        var selectedTrip: Trip? = null

        val pickerAdapter = TripPickerAdapter(trips) { trip ->
            selectedTrip = trip
            binding.continueBtn.isEnabled = true
            binding.continueBtn.setBackgroundColor(resources.getColor(R.color.space_purple))
        }

        binding.tripRecyclerView.layoutManager = GridLayoutManager(requireContext(), 3)
        binding.tripRecyclerView.adapter = pickerAdapter

        binding.cancelBtnEditProfileFragment.setOnClickListener { dismiss() }

        binding.continueBtn.setOnClickListener {
            selectedTrip?.let { trip ->
                if (destinationToAdd != null) {
                    handleAddToTrip(trip, destinationToAdd)
                } else {
                    navigateToCreatePost(trip)
                }
                dismiss()
            }
        }
    }

    private fun handleAddToTrip(trip: Trip, destination: Destination) {
        if (trip.destinationIds.contains(destination.id)) {
            return
        }

        tripViewModel.setCurrentTrip(trip)
        tripViewModel.addDestination(destination)
    }

    override fun onStart() {
        super.onStart()

        (dialog as? BottomSheetDialog)?.behavior?.apply {
            state = BottomSheetBehavior.STATE_EXPANDED
            skipCollapsed = true
        }

    }

    private fun navigateToCreatePost(trip: Trip) {
        val createFragment = PostCreateFragment()
        val bundle = Bundle()
        bundle.putSerializable("selectedTrip", trip)
        createFragment.arguments = bundle

        parentFragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.slide_in_bottom_to_top,
                R.anim.fade_out,
                R.anim.fade_in,
                R.anim.slide_out_top_to_bottom
            )
            .replace(R.id.main_frame, createFragment)
            .addToBackStack(null)
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "SelectTripDialogFragment"
        fun newInstance(destination: Destination? = null) = SelectDialogTrip().apply {
            arguments = Bundle().apply {
                putSerializable("destination_to_add", destination)
            }
        }
    }
}