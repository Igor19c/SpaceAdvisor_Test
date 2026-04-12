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
import com.example.spaceadvisor.domain.models.UIConfig
import com.example.spaceadvisor.ui.dialogs.EditDialogTrip
import com.example.spaceadvisor.utils.TripSelectionHelper
import com.example.spaceadvisor.ui.viewmodels.TripViewModel
import com.example.spaceadvisor.ui.viewmodels.UserViewModel
import com.example.spaceadvisor.ui.viewmodels.ViewModelFactory
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class MyTripsFragment : BaseFragment() {

    private var _binding: FragmentMyTripsBinding? = null
    private val binding get() = _binding!!

    private val userViewModel: UserViewModel by activityViewModels {
        ViewModelFactory(requireActivity().application as SpaceAdvisorApplication)
    }
    private val tripViewModel: TripViewModel by activityViewModels {
        ViewModelFactory(requireActivity().application as SpaceAdvisorApplication)
    }

    private lateinit var draftTripsAdapter: MyTripsAdapter

    private lateinit var plannedTripsAdapter: MyTripsAdapter

    private lateinit var completedTripsAdapter: MyTripsAdapter

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
            EditDialogTrip.newInstance(navigateOnSuccess = true)
                .show(parentFragmentManager, "CREATE_TRIP_DIALOG")
        }

        binding.myTripsHelpBtn.setOnClickListener {
            showWelcomeDialog()
        }
        binding.createTripBtn.setOnClickListener { createTripAction() }
        binding.emptyCreateTripBtn.setOnClickListener { createTripAction() }
    }

    private fun setupRecyclerView() {
        draftTripsAdapter = MyTripsAdapter(
            trips = mutableListOf(),
            onOpenClick = { trip ->
                tripViewModel.setCurrentTrip(trip)
                navigateToTripEditor(trip)
            },
            onConfirmClick = { trip -> tripSelectionHelper.handleFinalizeTrip(trip) },
            onShareClick = { trip -> navigateToCreatePost(trip) }
        )
        binding.draftTripsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.draftTripsRecyclerView.adapter = draftTripsAdapter

        plannedTripsAdapter = MyTripsAdapter(
            trips = mutableListOf(),
            onOpenClick = { trip ->
                tripViewModel.setCurrentTrip(trip)
                navigateToTripEditor(trip)
            },
            onConfirmClick = { trip -> tripSelectionHelper.handleFinalizeTrip(trip) },
            onShareClick = { trip -> navigateToCreatePost(trip) }
        )
        binding.plannedTripsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.plannedTripsRecyclerView.adapter = plannedTripsAdapter

        completedTripsAdapter = MyTripsAdapter(
            trips = mutableListOf(),
            onOpenClick = { trip ->
                tripViewModel.setCurrentTrip(trip)
                navigateToTripEditor(trip)
            },
            onConfirmClick = { trip -> tripSelectionHelper.handleFinalizeTrip(trip) },
            onShareClick = { trip -> navigateToCreatePost(trip) }
        )
        binding.completedTripsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.completedTripsRecyclerView.adapter = completedTripsAdapter

        attachSwipeToDelete(binding.draftTripsRecyclerView, draftTripsAdapter)
        attachSwipeToDelete(binding.plannedTripsRecyclerView, plannedTripsAdapter)
        attachSwipeToDelete(binding.completedTripsRecyclerView, completedTripsAdapter)
    }

    private fun showWelcomeDialog() {
        val currentContext = context ?: return
        val dialogView =
            LayoutInflater.from(currentContext).inflate(R.layout.dialog_my_trips_explanation, null)

        val dialog = MaterialAlertDialogBuilder(currentContext, R.style.CustomAlertDialog)
            .setView(dialogView)
            .show()

        dialogView.findViewById<View>(R.id.lets_go_btn_my_trips_dialog).setOnClickListener {
            dialog.dismiss()
        }
    }

    private fun attachSwipeToDelete(recyclerView: RecyclerView, adapter: MyTripsAdapter) {
        val itemTouchHelperCallback =
            object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
                override fun onMove(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder
                ): Boolean = false

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                    val position = viewHolder.adapterPosition
                    val trip = adapter.getTripAt(position)

                    tripSelectionHelper.handleDeleteTrip(
                        trip,
                        onDeleted = {
                            showCustomMessage("Trip Removed!", "Trip has been removed")
                        },
                        onCancel = {
                            adapter.notifyItemChanged(position)
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

        ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(recyclerView)
    }

    private fun observeViewModel() {
        tripViewModel.userTrips.observe(viewLifecycleOwner) { trips ->
            val isEmpty = trips.isNullOrEmpty()

            binding.tripsListContainer.visibility = if (isEmpty) View.GONE else View.VISIBLE
            binding.emptyStateContainer.visibility = if (isEmpty) View.VISIBLE else View.GONE
            binding.createTripBtn.visibility = if (isEmpty) View.GONE else View.VISIBLE

            if (!isEmpty) {
                val drafts = trips.filter { it.isDraft }
                val planned = trips.filter { it.status == "PLANNED" && !it.isCompleted }
                val completed = trips.filter { it.isCompleted }

                draftTripsAdapter.updateData(drafts)
                plannedTripsAdapter.updateData(planned)
                completedTripsAdapter.updateData(completed)

                binding.draftTripsCountMyTripsFragment.text = "${drafts.size} trips"
                binding.plannedTripsCountMyTripsFragment.text = "${planned.size} trips"
                binding.completedTripsCountMyTripsFragment.text = "${completed.size} trips"

                binding.draftTripsContainerMyTripsFragment.visibility =
                    if (drafts.isEmpty()) View.GONE else View.VISIBLE
                binding.draftTripsRecyclerView.visibility =
                    if (drafts.isEmpty()) View.GONE else View.VISIBLE

                binding.plannedTripsContainerMyTripsFragment.visibility =
                    if (planned.isEmpty()) View.GONE else View.VISIBLE
                binding.plannedTripsRecyclerView.visibility =
                    if (planned.isEmpty()) View.GONE else View.VISIBLE

                binding.completedTripsContainerMyTripsFragment.visibility =
                    if (completed.isEmpty()) View.GONE else View.VISIBLE
                binding.completedTripsRecyclerView.visibility =
                    if (completed.isEmpty()) View.GONE else View.VISIBLE

            }
        }

        tripViewModel.error.observe(viewLifecycleOwner)
        { error ->
            error?.let { showCustomMessage("Error", it) }
        }
    }

    private fun navigateToCreatePost(trip: Trip) {
        val fragment = PostCreateFragment().apply {
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
