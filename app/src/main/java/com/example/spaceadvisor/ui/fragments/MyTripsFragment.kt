package com.example.spaceadvisor.ui.fragments

import android.graphics.Canvas
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.spaceadvisor.R
import com.example.spaceadvisor.SpaceAdvisorApplication
import com.example.spaceadvisor.ui.adapters.MyTripsAdapter
import com.example.spaceadvisor.databinding.FragmentMyTripsBinding
import com.example.spaceadvisor.domain.models.Trip
import com.example.spaceadvisor.ui.UIConfig
import com.example.spaceadvisor.ui.utils.TripSelectionHelper
import com.example.spaceadvisor.ui.viewmodels.FeedViewModel
import com.example.spaceadvisor.ui.viewmodels.TripViewModel
import com.example.spaceadvisor.ui.viewmodels.UserViewModel
import com.example.spaceadvisor.ui.viewmodels.ViewModelFactory

class MyTripsFragment : BaseFragment() {

    private var _binding: FragmentMyTripsBinding? = null
    private val binding get() = _binding!!

    private val userViewModel: UserViewModel by activityViewModels {
        ViewModelFactory(requireActivity().application as SpaceAdvisorApplication)
    }
    private val tripViewModel: TripViewModel by activityViewModels {
        ViewModelFactory(requireActivity().application as SpaceAdvisorApplication)
    }
    private val feedViewModel: FeedViewModel by activityViewModels {
        ViewModelFactory(requireActivity().application as SpaceAdvisorApplication)
    }

    private lateinit var allTripsAdapter: MyTripsAdapter
    private lateinit var tripSelectionHelper: TripSelectionHelper

    override fun getUIConfig() = UIConfig(
        title = "My Trips",
        selectedTabId = R.id.nav_trips
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMyTripsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tripSelectionHelper = TripSelectionHelper(this, tripViewModel)

        setupRecyclerView()
        setupClickListeners()
        observeViewModel()

        userViewModel.getCurrentUid()?.let { uid ->
            tripViewModel.fetchUserTrips(uid)
        }
    }

    private fun setupClickListeners() {
        val createTripAction = {
            tripViewModel.createNewTrip()
            EditTripDialogFragment.newInstance(navigateOnSuccess = true)
                .show(parentFragmentManager, "CREATE_TRIP_DIALOG")
        }

        binding.createTripFab.setOnClickListener { createTripAction() }
        binding.emptyCreateTripBtn.setOnClickListener { createTripAction() }
    }

    private fun setupRecyclerView() {
        allTripsAdapter = MyTripsAdapter(
            trips = mutableListOf(),
            onOpenClick = { trip ->
                tripViewModel.setCurrentTrip(trip)
                navigateToTripEditor(trip)
            },
            onConfirmClick = { trip -> tripSelectionHelper.handleConfirmTrip(trip) },
            onShareClick = { trip -> navigateToCreatePost(trip) }
        )
        binding.draftTripsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.draftTripsRecyclerView.adapter = allTripsAdapter

        val itemTouchHelperCallback =
            object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
                override fun onMove(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder
                ): Boolean = false

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                    val position = viewHolder.adapterPosition
                    val trip = allTripsAdapter.getTripAt(position)

                    tripSelectionHelper.handleDeleteTrip(
                        trip,
                        onDeleted = {
                            showCustomMessage("Trip Removed!", "Trip has been removed")
                        },
                        onCancel = {
                            allTripsAdapter.notifyItemChanged(position)
                        }
                    )
                }

                override fun onChildDraw(
                    c: Canvas,
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    dX: Float,
                    dY: Float,
                    actionState: Int,
                    isCurrentlyActive: Boolean
                ) {
                    if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                        val holder = viewHolder as MyTripsAdapter.ViewHolder
                        holder.cardContainer.translationX = dX
                    } else {
                        super.onChildDraw(
                            c,
                            recyclerView,
                            viewHolder,
                            dX,
                            dY,
                            actionState,
                            isCurrentlyActive
                        )
                    }
                }
            }

        ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(binding.draftTripsRecyclerView)
    }

    private fun observeViewModel() {
        tripViewModel.userTrips.observe(viewLifecycleOwner) { trips ->
            val isEmpty = trips.isNullOrEmpty()

            binding.tripsListContainer.visibility = if (isEmpty) View.GONE else View.VISIBLE
            binding.emptyStateContainer.visibility = if (isEmpty) View.VISIBLE else View.GONE

            if (!isEmpty) {
                allTripsAdapter.updateData(trips)
            }
        }

        tripViewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let { showCustomMessage("Error", it) }
        }
    }

    private fun navigateToCreatePost(trip: Trip) {
        val fragment = CreatePostFragment().apply {
            arguments = Bundle().apply {
                putSerializable("selectedTrip", trip)
                putBoolean("isSharingTrip", true)
            }
        }
        parentFragmentManager.beginTransaction()
            .replace(R.id.main_frame, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun navigateToTripEditor(trip: Trip) {
        val fragment = TripFragment().apply {
            arguments = Bundle().apply {
                putBoolean("start_edit", false)
                putBoolean("is_completed", trip.isCompleted)
            }
        }

        parentFragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.slide_in_bottom_to_top,
                R.anim.fade_out,
                R.anim.fade_in,
                R.anim.slide_out_top_to_bottom
            )
            .replace(R.id.main_frame, fragment)
            .addToBackStack(null)
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
