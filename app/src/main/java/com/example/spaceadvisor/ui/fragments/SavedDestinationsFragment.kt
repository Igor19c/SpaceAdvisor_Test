package com.example.spaceadvisor.ui.fragments

import android.graphics.Canvas
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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
import com.example.spaceadvisor.ui.adapters.TripDestinationsAdapter
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
        isRightBtnVisible = true,
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
                            Toast.makeText(
                                requireContext(),
                                "${destination.title} unsaved",
                                Toast.LENGTH_SHORT
                            ).show()
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
                }
            }.show()

    }

    private fun showUnsaveAllConfirmation() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Unsave All")
            .setMessage("Are you sure you want to remove all saved destinations?")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Remove All") { _, _ ->
                userViewModel.getCurrentUid()?.let { uid ->
                    destinationViewModel.unsaveAllDestinations(uid)
                    Toast.makeText(
                        requireContext(),
                        "All destinations removed",
                        Toast.LENGTH_SHORT
                    )
                        .show()
                }
            }
            .show()
    }

    private fun observeViewModel() {
        destinationViewModel.savedDestinations.observe(viewLifecycleOwner) { destinations ->
            adapter.updateData(destinations)
            binding.emptyStateText.visibility =
                if (destinations.isEmpty()) View.VISIBLE else View.GONE
            binding.myDestinationsRecyclerView.visibility =
                if (destinations.isEmpty()) View.GONE else View.VISIBLE
        }

        destinationViewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
