package com.example.spaceadvisor.ui.utils

import android.app.AlertDialog
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.example.spaceadvisor.R
import com.example.spaceadvisor.databinding.ContainerAddToTripBtnsDestFragmentBinding
import com.example.spaceadvisor.databinding.ContainerBtnMyTripsFragmentBinding
import com.example.spaceadvisor.databinding.DialogSelectTripBinding
import com.example.spaceadvisor.domain.models.Destination
import com.example.spaceadvisor.domain.models.Trip
import com.example.spaceadvisor.ui.adapters.TripPickerAdapter
import com.example.spaceadvisor.ui.fragments.EditTripDialogFragment
import com.example.spaceadvisor.ui.viewmodels.TripViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class TripSelectionHelper(
    private val fragment: Fragment,
    private val tripViewModel: TripViewModel,
    private val onDestinationPending: ((Destination) -> Unit)? = null
) {

    /**
     * Logic for adding a destination to a trip (used in Explore and Destination fragments)
     */
    fun handleAddToTrip(destination: Destination) {
        showTripSelectionActionDialog(destination)
    }

    /**
     * Common logic for publishing a trip with validation (used in Trip and MyTrips fragments)
     */
    fun handleConfirmTrip(trip: Trip, onSuccess: (() -> Unit)? = null) {
        if (trip.canBePublished) {
            tripViewModel.setCurrentTrip(trip)
            tripViewModel.finalizeTrip()
            Toast.makeText(
                fragment.requireContext(),
                "Journey '${trip.title}' Confirmed!",
                Toast.LENGTH_SHORT
            ).show()
            onSuccess?.invoke()
        } else {
            when {
                trip.destinationIds.isEmpty() -> {
                    Toast.makeText(
                        fragment.requireContext(),
                        "Your trip is empty!",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                trip.isPastTrip -> {
                    showInvalidDateDialog(trip)
                }
            }
        }
    }

    /**
     * Custom dialog for handling past trip dates with the custom layout
     */
    fun showInvalidDateDialog(trip: Trip) {
        val context = fragment.requireContext()
        val dialogBinding = ContainerBtnMyTripsFragmentBinding.inflate(LayoutInflater.from(context))
        val dialog = AlertDialog.Builder(context)
            .setView(dialogBinding.root)
            .create()

        dialog.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())

        dialogBinding.fixDatesBtnContainerTripFragment.setOnClickListener {
            tripViewModel.setCurrentTrip(trip)
            EditTripDialogFragment.newInstance(isEdit = true)
                .show(fragment.parentFragmentManager, "EDIT_TRIP_DIALOG")
            tripViewModel.setEditMode(true)
            dialog.dismiss()
        }

        dialogBinding.deleteTripBtnContainerTripFragment.setOnClickListener {
            handleDeleteTrip(trip) {
                // Special case: if we are inside the TripFragment, close it after deletion
                if (fragment.javaClass.simpleName == "TripFragment") {
                    fragment.parentFragmentManager.popBackStack()
                }
            }
            dialog.dismiss()
        }

        dialogBinding.cancelBtnContainerTripFragment.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    /**
     * Common confirmation dialog for deleting a trip
     */
    fun handleDeleteTrip(
        trip: Trip,
        onDeleted: (() -> Unit)? = null,
        onCancel: (() -> Unit)? = null
    ) {
        MaterialAlertDialogBuilder(fragment.requireContext(), R.style.CustomAlertDialog)
            .setTitle("Delete Trip")
            .setMessage("Are you sure you want to delete '${trip.title}'?")
            .setNegativeButton("Cancel") { _, _ ->
                onCancel?.invoke()
            }
            .setOnCancelListener {
                onCancel?.invoke()
            }
            .setPositiveButton("Delete") { _, _ ->
                tripViewModel.deleteTrip(trip.id)
                onDeleted?.invoke()
            }
            .show()
    }

    /**
     * Confirmation dialog for removing a destination from a trip
     */
    fun handleDeleteTripDestination(
        dest: Destination,
        onDeleted: (() -> Unit)? = null,
        onCancel: (() -> Unit)? = null
    ) {
        MaterialAlertDialogBuilder(fragment.requireContext(), R.style.CustomAlertDialog)
            .setTitle("Delete Destination")
            .setMessage("Are you sure you want to delete '${dest.title}'?")
            .setNegativeButton("Cancel") { _, _ ->
                onCancel?.invoke()
            }
            .setOnCancelListener {
                onCancel?.invoke()
            }
            .setPositiveButton("Delete") { _, _ ->
                tripViewModel.removeDestination(dest.id)
                onDeleted?.invoke()
            }
            .show()
    }

    private fun isDestinationInTrip(trip: Trip, destination: Destination): Boolean {
        return trip.destinationIds.contains(destination.id)
    }

    private fun showTripSelectionActionDialog(destination: Destination) {
        val draftTrips = (tripViewModel.userTrips.value ?: emptyList()).filter { it.isDraft }
        val activeTrip =
            tripViewModel.currentTrip.value?.takeIf { it.id.isNotEmpty() && it.isDraft }
                ?: draftTrips.firstOrNull()

        val context = fragment.requireContext()
        val dialogBinding =
            ContainerAddToTripBtnsDestFragmentBinding.inflate(LayoutInflater.from(context))
        val dialog = AlertDialog.Builder(context)
            .setView(dialogBinding.root)
            .create()

        dialog.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())

        // 1. Add to Active Trip
        if (activeTrip != null) {
            dialogBinding.addToCurrentTripBtnContainer.apply {
                visibility = View.VISIBLE
                text = "Add to Current: ${activeTrip.title}"
                setOnClickListener {
                    if (isDestinationInTrip(activeTrip, destination)) {
                        Toast.makeText(
                            context,
                            "${destination.title} is already in ${activeTrip.title}!",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        tripViewModel.setCurrentTrip(activeTrip)
                        tripViewModel.addDestination(destination)
                        Toast.makeText(
                            context,
                            "${destination.title} added to ${activeTrip.title}!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    dialog.dismiss()
                }
            }
        } else {
            dialogBinding.addToCurrentTripBtnContainer.visibility = View.GONE
        }

        // 2. Select from Existing
        if (draftTrips.size > 1) {
            dialogBinding.selectExistTripBtnContainer.apply {
                visibility = View.VISIBLE
                text = "Select from Other Trips"
                setOnClickListener {
                    showExistingTripsSelectionDialog(destination)
                    dialog.dismiss()
                }
            }
        } else {
            dialogBinding.selectExistTripBtnContainer.visibility = View.GONE
        }

        // 3. Create New
        dialogBinding.createNewTripBtnContainer.setOnClickListener {
            tripViewModel.createNewTrip()
            onDestinationPending?.invoke(destination)
            EditTripDialogFragment.newInstance()
                .show(fragment.parentFragmentManager, "CREATE_TRIP_DIALOG")
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showExistingTripsSelectionDialog(destination: Destination) {
        val draftTrips = (tripViewModel.userTrips.value ?: emptyList()).filter { it.isDraft }
        val context = fragment.requireContext()

        val dialog = BottomSheetDialog(context)
        val dialogBinding = DialogSelectTripBinding.inflate(LayoutInflater.from(context))
        dialog.setContentView(dialogBinding.root)

        var selectedTrip: Trip? = null
        val pickerAdapter = TripPickerAdapter(draftTrips) { trip ->
            selectedTrip = trip
            dialogBinding.continueBtn.isEnabled = true
        }

        dialogBinding.tripRecyclerView.layoutManager = GridLayoutManager(context, 3)
        dialogBinding.tripRecyclerView.adapter = pickerAdapter

        dialogBinding.continueBtn.setOnClickListener {
            selectedTrip?.let { trip ->
                if (isDestinationInTrip(trip, destination)) {
                    Toast.makeText(
                        context,
                        "${destination.title} is already in ${trip.title}!",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    tripViewModel.setCurrentTrip(trip)
                    tripViewModel.addDestination(destination)
                    Toast.makeText(
                        context,
                        "${destination.title} added to ${trip.title}!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                dialog.dismiss()
            }
        }

        dialogBinding.cancelBtn.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }
}
