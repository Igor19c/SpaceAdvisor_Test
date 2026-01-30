package com.example.spaceadvisor.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.bumptech.glide.Glide
import com.example.spaceadvisor.R
import com.example.spaceadvisor.data.Destination
import com.example.spaceadvisor.data.DestinationDetails
import com.example.spaceadvisor.databinding.FragmentDestinationBinding
import com.example.spaceadvisor.viewmodels.DestinationViewModel
import com.example.spaceadvisor.viewmodels.TripViewModel

class DestinationFragment : Fragment() {

    private var _binding: FragmentDestinationBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DestinationViewModel by viewModels()
    private val tripViewModel: TripViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDestinationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val destination = arguments?.getSerializable("destination_key") as? Destination

        destination?.let {
            setupInitialUI(it)
            observeViewModel()
            viewModel.fetchFullDetails(it.id)
            setupButtons(it)
        }

        binding.backBtn.setOnClickListener { parentFragmentManager.popBackStack() }
    }

    private fun setupInitialUI(destination: Destination) {
        binding.detailsTitle.text = destination.title
        binding.detailsSubtitle.text = destination.subtitle
        binding.detailsRating.text = "★ ${destination.ratingAvg}"

        if (destination.imageUrl.isNotEmpty()) {
            Glide.with(this)
                .load(destination.imageUrl)
                .placeholder(R.drawable.ic_home_earth)
                .into(binding.detailsImage)
        }

        binding.detailsDescription.text = destination.shortDescription
    }

    private fun observeViewModel() {
        viewModel.destinationDetails.observe(viewLifecycleOwner) { details ->
            details?.let { updateDetailsUI(it) }
        }
    }

    private fun setupButtons(destination: Destination) {
        binding.addBtn.text =
            if (destination.childCount > 0) "Explore ${destination.title}" else "Add to Trip"

        binding.addBtn.setOnClickListener {
            if (destination.childCount > 0) {
                // Navigate deeper
                val nextFragment = SelectDestinationFragment().apply {
                    arguments = Bundle().apply {
                        putSerializable("target_destination", destination)
                    }
                }
                parentFragmentManager.beginTransaction()
                    .replace(R.id.main_frame, nextFragment)
                    .addToBackStack(null)
                    .commit()
            } else {
                tripViewModel.addDestination(destination)
                Toast.makeText(requireContext(), "${destination.title} added!", Toast.LENGTH_SHORT)
                    .show()
            }
        }

        binding.myTripBtn.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.main_frame, TripEditorFragment())
                .addToBackStack(null)
                .commit()
        }
    }

    private fun updateDetailsUI(details: DestinationDetails) {
        binding.detailsDescription.text = details.longDescription

        if (details.highlights.isNotEmpty()) {
            binding.highlightsContainer.visibility = View.VISIBLE
            binding.detailsHighlights.text = details.highlights.joinToString("\n") { "• $it" }
        } else {
            binding.highlightsContainer.visibility = View.GONE
        }

        val env = details.environment
        if (env != null) {
            binding.environmentSection.visibility = View.VISIBLE
            val envText = StringBuilder()
            envText.append("Gravity: ${env.gravityG}G\n")
            envText.append("Atmosphere: ${env.atmosphere}\n")
            env.temperatureC?.let {
                envText.append("Temperature: ${it.min}°C to ${it.max}°C\n")
            }
            envText.append("Radiation: ${env.radiation}\n")
            envText.append("Day Length: ${env.dayLengthHours}h\n")
            envText.append("Visibility: ${env.visibility}")
            binding.detailsEnvironment.text = envText.toString()
        }

        val logistics = details.logistics
        if (logistics != null) {
            binding.logisticsSection.visibility = View.VISIBLE
            val logText = StringBuilder()
            logistics.travelTimeFromEarth?.let {
                logText.append("Travel Time: ${it.value} ${it.unit}\n")
            }
            if (logistics.typicalRoute.isNotEmpty()) {
                logText.append("Route: ${logistics.typicalRoute.joinToString(" → ")}\n")
            }
            binding.detailsLogistics.text = logText.toString()
        }

        val safety = details.safety
        if (safety != null) {
            binding.safetySection.visibility = View.VISIBLE
            val safetyText = StringBuilder()
            safetyText.append("Safety Level: ${safety.overallLevel}/5\n")
            if (safety.notes.isNotEmpty()) {
                safetyText.append(safety.notes.joinToString("\n") { "• $it" })
            }
            binding.detailsSafety.text = safetyText.toString()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
