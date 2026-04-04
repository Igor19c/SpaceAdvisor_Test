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
import com.example.spaceadvisor.ui.adapters.SavedAdapter
import com.example.spaceadvisor.databinding.FragmentSavedDestinationsBinding
import com.example.spaceadvisor.domain.models.Destination
import com.example.spaceadvisor.ui.UIConfig
import com.example.spaceadvisor.ui.viewmodels.DestinationViewModel
import com.example.spaceadvisor.ui.viewmodels.UserViewModel
import com.example.spaceadvisor.ui.viewmodels.ViewModelFactory
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class SavedDestinationsFragment : BaseFragment() {

    private var _binding: FragmentSavedDestinationsBinding? = null
    private val binding get() = _binding!!

    private val userViewModel: UserViewModel by activityViewModels {
        ViewModelFactory(requireActivity().application as SpaceAdvisorApplication)
    }
    private val destinationViewModel: DestinationViewModel by activityViewModels {
        ViewModelFactory(requireActivity().application as SpaceAdvisorApplication)
    }

    private lateinit var adapter: SavedAdapter

    override fun getUIConfig() = UIConfig(
        title = "Saved",
        isBottomNavVisible = false,
        isHeaderVisible = true,
        isLeftBtnVisible = true,
        leftIconRes = R.drawable.ic_back,
        onLeftClick = { parentFragmentManager.popBackStack() },
        isRightBtnVisible = false,
        selectedTabId = R.id.nav_profile
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSavedDestinationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeViewModel()

        userViewModel.getCurrentUid()?.let { uid ->
            destinationViewModel.fetchSavedDestinations(uid)
        }

        binding.unsaveAllBtn.setOnClickListener {
            showUnsaveAllConfirmation()
        }
        binding.emptyCreateTripBtn.setOnClickListener { navigateTo(ExploreFragment()) }

    }

    private fun setupRecyclerView() {
        adapter = SavedAdapter(
            destinations = mutableListOf()
        )
        binding.myDestinationsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.myDestinationsRecyclerView.adapter = adapter

        val itemTouchHelperCallback =
            object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
                override fun onMove(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder
                ): Boolean = false

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                    val position = viewHolder.adapterPosition
                    val destination = adapter.getDestinationAt(position)

                    handleUnsaveDestination(
                        destination,
                        onDeleted = {
                            showCustomMessage(
                                title = "Unsaved",
                                body = "${destination.title} removed from favorites.",
                                duration = 2000
                            )
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
                        val holder = viewHolder as SavedAdapter.ViewHolder
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

        ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(binding.myDestinationsRecyclerView)
    }

    private fun handleUnsaveDestination(
        destination: Destination, onDeleted: (() -> Unit)? = null,
        onCancel: (() -> Unit)? = null
    ) {
        MaterialAlertDialogBuilder(requireContext(), R.style.CustomAlertDialog)
            .setTitle("Delete Destination")
            .setMessage("Are you sure you want to delete '${destination.title}'?")
            .setNegativeButton("Cancel") { _, _ ->
                onCancel?.invoke()
            }
            .setOnCancelListener {
                onCancel?.invoke()
            }
            .setPositiveButton("Delete") { _, _ ->
                userViewModel.getCurrentUid()?.let { uid ->
                    destinationViewModel.unsaveDestination(uid, destination.id)
                    onDeleted?.invoke()
                }
            }.show()

    }

    private fun showUnsaveAllConfirmation() {
        MaterialAlertDialogBuilder(requireContext(), R.style.CustomAlertDialog)
            .setTitle("Unsave All")
            .setMessage("Are you sure you want to remove all saved destinations?")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Remove All") { _, _ ->
                userViewModel.getCurrentUid()?.let { uid ->
                    destinationViewModel.unsaveAllDestinations(uid)
                    showCustomMessage(
                        title = "Cleared",
                        body = "All destinations removed.",
                        duration = 3000
                    )
                }
            }
            .show()
    }

    private fun observeViewModel() {
        destinationViewModel.savedDestinations.observe(viewLifecycleOwner) { destinations ->
            val isEmpty = destinations.isNullOrEmpty()

            binding.myDestinationsRecyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
            binding.emptyStateContainer.visibility = if (isEmpty) View.VISIBLE else View.GONE
            binding.unsaveAllBtn.visibility = if (isEmpty) View.INVISIBLE else View.VISIBLE

            if (!isEmpty) {
                adapter.updateData(destinations)
            }
        }

        destinationViewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                showError(it)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
