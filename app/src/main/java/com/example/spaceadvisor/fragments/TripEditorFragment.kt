package com.example.spaceadvisor.fragments

import android.animation.ObjectAnimator
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.spaceadvisor.R
import com.example.spaceadvisor.adapters.TripAdapter
import com.example.spaceadvisor.databinding.DialogNewTripNameBinding
import com.example.spaceadvisor.databinding.FragmentTripEditorBinding
import com.example.spaceadvisor.viewmodels.TripViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog

class TripEditorFragment : Fragment() {

    private var _binding: FragmentTripEditorBinding? = null
    private val binding get() = _binding!!
    private val tripViewModel: TripViewModel by activityViewModels()
    private lateinit var tripAdapter: TripAdapter
    private var destTitle: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTripEditorBinding.inflate(inflater, container, false)
        binding.root.visibility = View.INVISIBLE
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeViewModel()

        val currentTrip = tripViewModel.currentTrip.value
        if (currentTrip == null || currentTrip.title.isEmpty()) {
            binding.root.visibility = View.INVISIBLE
            showCreateTripDialog()
        } else {
            binding.root.visibility = View.VISIBLE
            updateActivityHeader(currentTrip.title)
        }

        binding.finalizeTripBtn.setOnClickListener {
            val destinations = tripViewModel.selectedDestinations.value
            if (destinations.isNullOrEmpty()) {
                Toast.makeText(requireContext(), "Your trip is empty!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            Toast.makeText(requireContext(), "Trip saved!", Toast.LENGTH_LONG).show()
            tripViewModel.updateTripObject(destinations)
            tripViewModel.syncTripWithFirebase()
        }
        binding.addDestinationBtn.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.main_frame, SelectDestinationFragment())
                .addToBackStack(null)
                .commit()
        }
    }

    private fun showCreateTripDialog() {
        val dialog = BottomSheetDialog(requireContext())
        val dialogBinding = DialogNewTripNameBinding.inflate(layoutInflater)
        dialog.setCancelable(false)
        dialog.setContentView(dialogBinding.root)

        dialogBinding.saveBtn.setOnClickListener {
            destTitle = dialogBinding.editNameEt.text.toString()
            if (destTitle.isBlank()) {
                dialogBinding.editNameEt.error = "Please enter a trip name"
                return@setOnClickListener
            }

            tripViewModel.saveTrip(destTitle, "0 destinations planned")

            binding.root.visibility = View.VISIBLE
            updateActivityHeader(destTitle)

            dialog.dismiss()
        }

        dialogBinding.cancelBtn.setOnClickListener {
            parentFragmentManager.popBackStack()
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun updateActivityHeader(title: String) {
        binding.myTripTitle.text = title
    }

    private fun setupRecyclerView() {
        tripAdapter = TripAdapter(mutableListOf()) { destination ->
            tripViewModel.removeDestination(destination.id)
        }
        binding.itineraryRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = tripAdapter
        }
    }

    private fun observeViewModel() {
        tripViewModel.selectedDestinations.observe(viewLifecycleOwner) { destinations ->
            tripAdapter.updateData(destinations)
            updateTimelineUI(destinations.size)
            binding.finalizeTripBtn.isEnabled = destinations.isNotEmpty()
        }
    }

    private fun updateTimelineUI(count: Int) {
        binding.dotsContainer.removeAllViews()
        if (count == 0) {
            binding.tripProgressBar.progress = 0
            return
        }

        val dotSizePx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 20f, resources.displayMetrics
        ).toInt()

        for (i in 0 until count) {
            val dot = ImageView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(dotSizePx, dotSizePx).apply {
                    weight = 1f
                }
                setImageResource(R.drawable.ic_dot_active)
            }
            binding.dotsContainer.addView(dot)
        }

        val targetProgress = if (count <= 1) 0 else 100
        ObjectAnimator.ofInt(binding.tripProgressBar, "progress", targetProgress).apply {
            duration = 1000
            interpolator = DecelerateInterpolator()
            start()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
