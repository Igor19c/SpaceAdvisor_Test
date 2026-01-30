package com.example.spaceadvisor.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.MarginPageTransformer
import com.example.spaceadvisor.R
import com.example.spaceadvisor.data.Destination
import com.example.spaceadvisor.adapters.CarouselAdapter
import com.example.spaceadvisor.databinding.FragmentSelectDestinationBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Stack

class SelectDestinationFragment : Fragment() {

    private var _binding: FragmentSelectDestinationBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()
    private val navigationStack = Stack<NavigationState>()
    private var currentParentId = "root"
    private var currentParentType: String? = null
    private var currentParentTitle: String? = null
    private var currentBodyType: String? = null
    private lateinit var carouselAdapter: CarouselAdapter

    data class NavigationState(
        val parentId: String, val type: String?, val title: String?, val bodyType: String?
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSelectDestinationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Check if we were navigated here with a target destination
        arguments?.getSerializable("target_destination")?.let {
            val dest = it as Destination
            currentParentId = dest.id
            currentParentType = dest.type
            currentParentTitle = dest.title
            currentBodyType = dest.bodyType
        }

        setupViewPager()
        if (currentParentId == "root") {
            showWelcomeDialog()
        }
        fetchDestinations(currentParentId, currentParentType, currentParentTitle, currentBodyType)
        binding.returnBtn.setOnClickListener { handleBackNavigation() }
    }

    private fun setupViewPager() {
        val viewPager = binding.carouselViewPager
        carouselAdapter = CarouselAdapter(emptyList())

        carouselAdapter.setOnItemClickListener { destination ->
            if (destination.childCount > 0) {
                navigateToChildren(destination)
            } else {
                openDestinationDetails(destination)
            }
        }

        carouselAdapter.setOnDetailsClickListener { destination ->
            openDestinationDetails(destination)
        }

        viewPager.adapter = carouselAdapter
        viewPager.offscreenPageLimit = 3
        viewPager.getChildAt(0).overScrollMode = RecyclerView.OVER_SCROLL_NEVER

        val marginTransformer = MarginPageTransformer(40)
        viewPager.setPageTransformer(marginTransformer)

        TabLayoutMediator(binding.carouselIndicator, viewPager) { _, _ -> }.attach()
    }

    private fun navigateToChildren(destination: Destination) {
        navigationStack.push(
            NavigationState(
                currentParentId, currentParentType, currentParentTitle, currentBodyType
            )
        )
        currentParentId = destination.id
        currentParentType = destination.type
        currentParentTitle = destination.title
        currentBodyType = destination.bodyType

        binding.carouselViewPager.setCurrentItem(0, false)
        fetchDestinations(currentParentId, currentParentType, currentParentTitle, currentBodyType)
    }

    private fun openDestinationDetails(destination: Destination) {
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

    private fun fetchDestinations(
        parentId: String,
        parentType: String? = null,
        title: String? = null,
        bodyType: String? = null
    ) {
        updateProgressUI(parentId, parentType)
        updateLabels(parentId, parentType, title, bodyType)
        db.collection("destinations").whereEqualTo("parentId", parentId)
            .addSnapshotListener { value, error ->
                if (error != null || _binding == null) return@addSnapshotListener
                val list = mutableListOf<Destination>()
                for (doc in value!!) {
                    val item = doc.toObject(Destination::class.java).copy(id = doc.id)
                    list.add(item)
                }
                carouselAdapter.updateData(list)
            }
    }

    private fun updateProgressUI(parentId: String, parentType: String?) {
        val progress: Int
        val galaxyActive: Boolean
        val bodyActive: Boolean
        val locationActive: Boolean

        when {
            parentId == "root" -> {
                progress = 15
                galaxyActive = true
                bodyActive = false
                locationActive = false
            }

            parentType == "GALAXY" -> {
                progress = 50
                galaxyActive = true
                bodyActive = true
                locationActive = false
            }

            parentType == "BODY" -> {
                progress = 100
                galaxyActive = true
                bodyActive = true
                locationActive = true
            }

            else -> {
                progress = 100
                galaxyActive = true
                bodyActive = true
                locationActive = true
            }
        }

        binding.tripProgressBar.progress = progress

        binding.dotGalaxy.setImageResource(if (galaxyActive) R.drawable.ic_dot_active else R.drawable.ic_dot_inactive)
        binding.dotPlanet.setImageResource(if (bodyActive) R.drawable.ic_dot_active else R.drawable.ic_dot_inactive)
        binding.dotLocation.setImageResource(if (locationActive) R.drawable.ic_dot_active else R.drawable.ic_dot_inactive)

        val activeColor = ContextCompat.getColor(requireContext(), R.color.white)
        val inactiveColor = ContextCompat.getColor(requireContext(), R.color.textQuaternary)

        binding.dotLabelGalaxy.setTextColor(if (galaxyActive) activeColor else inactiveColor)
        binding.dotLabelBody.setTextColor(if (bodyActive) activeColor else inactiveColor)
        binding.dotLabelLocation.setTextColor(if (locationActive) activeColor else inactiveColor)
    }

    private fun handleBackNavigation() {
        if (navigationStack.isNotEmpty()) {
            val previousState = navigationStack.pop()
            currentParentId = previousState.parentId
            currentParentType = previousState.type
            currentParentTitle = previousState.title
            currentBodyType = previousState.bodyType

            binding.carouselViewPager.setCurrentItem(0, false)
            fetchDestinations(
                currentParentId, currentParentType, currentParentTitle, currentBodyType
            )
        } else {
            // If stack empty but we are not at root, it means we came from "Explore" deeper
            if (currentParentId != "root") {
                currentParentId = "root"
                currentParentType = null
                currentParentTitle = null
                fetchDestinations(
                    currentParentId,
                    currentParentType,
                    currentParentTitle,
                    currentBodyType
                )
            }
        }
    }

    private fun showWelcomeDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_trip_explanation, null)
        MaterialAlertDialogBuilder(requireContext(), R.style.CustomAlertDialog).setView(
            dialogView
        ).setPositiveButton("Let's Go!") { d, _ -> d.dismiss() }.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun updateLabels(parentId: String, type: String?, title: String?, bodyType: String?) {
        when {
            parentId == "root" -> {
                binding.returnBtn.visibility = View.INVISIBLE
            }

            else -> {
                binding.returnBtn.visibility = View.VISIBLE
                when (type) {
                    "GALAXY" -> binding.returnBtn.text = "Galaxies"
                    "BODY" -> binding.returnBtn.text = "Planets & Stations"
                    else -> binding.returnBtn.text = "Back"
                }
            }
        }
    }
}
