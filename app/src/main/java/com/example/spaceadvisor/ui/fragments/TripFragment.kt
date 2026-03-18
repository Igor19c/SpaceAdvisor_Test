package com.example.spaceadvisor.ui.fragments

import android.graphics.Canvas
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.spaceadvisor.R
import com.example.spaceadvisor.SpaceAdvisorApplication
import com.example.spaceadvisor.ui.adapters.TripDestinationsAdapter
import com.example.spaceadvisor.databinding.FragmentTripBinding
import com.example.spaceadvisor.ui.viewmodels.TripViewModel
import com.example.spaceadvisor.ui.viewmodels.UserViewModel
import com.example.spaceadvisor.ui.viewmodels.ViewModelFactory
import com.example.spaceadvisor.ui.UIConfig
import com.example.spaceadvisor.ui.utils.TripSelectionHelper
import java.text.SimpleDateFormat
import java.util.*

class TripFragment : BaseFragment() {

    private var _binding: FragmentTripBinding? = null
    private val binding get() = _binding!!

    private val tripViewModel: TripViewModel by activityViewModels {
        ViewModelFactory(requireActivity().application as SpaceAdvisorApplication)
    }
    private val userViewModel: UserViewModel by activityViewModels {
        ViewModelFactory(requireActivity().application as SpaceAdvisorApplication)
    }

    private lateinit var tripAdapter: TripDestinationsAdapter
    private lateinit var tripSelectionHelper: TripSelectionHelper
    private var isSubmitting = false

    override fun getUIConfig() = UIConfig(
        title = tripViewModel.currentTrip.value?.title ?: "My Journey",
        isBottomNavVisible = false,
        isHeaderVisible = true,
        isLeftBtnVisible = true,
        leftIconRes = R.drawable.ic_back,
        onLeftClick = {
            tripViewModel.setEditMode(false)
            parentFragmentManager.popBackStack()
        },
        isRightBtnVisible = true,
        selectedTabId = R.id.nav_trips
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTripBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tripSelectionHelper = TripSelectionHelper(this, tripViewModel)

        val currentTrip = tripViewModel.currentTrip.value
        if ((currentTrip == null || currentTrip.status.isEmpty()) && !isSubmitting) {
            EditTripDialogFragment.newInstance().show(parentFragmentManager, "ADD_TRIP_DIALOG")
        }

        if (savedInstanceState == null && tripViewModel.isEditMode.value != true) {
            val startInEditMode = arguments?.getBoolean("start_edit") ?: false
            if (startInEditMode) tripViewModel.setEditMode(true)
        }

        setupRecyclerView()
        setupClickListeners()
        observeViewModel()
    }

    private fun setupClickListeners() {
        val navigateToExplore = {
            tripViewModel.setEditMode(true)
            parentFragmentManager.beginTransaction()
                .replace(R.id.main_frame, ExploreFragment())
                .addToBackStack("trip_main")
                .commit()
        }

        binding.editBtn.setOnClickListener {
            EditTripDialogFragment.newInstance(isEdit = true)
                .show(parentFragmentManager, "EDIT_TRIP_DIALOG")
            tripViewModel.setEditMode(true)
        }

        binding.confirmTripBtn.setOnClickListener {
            tripViewModel.currentTrip.value?.let { trip ->
                tripSelectionHelper.handleConfirmTrip(trip)
            }
        }

        binding.addDestinationBtn.setOnClickListener { navigateToExplore() }
        binding.emptyAddDestinationBtn.setOnClickListener { navigateToExplore() }
    }

    private fun observeViewModel() {
        tripViewModel.selectedDestinations.observe(viewLifecycleOwner) { destinations ->
            val isEmpty = destinations.isNullOrEmpty()

            binding.itineraryRecyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
            binding.tripEmptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE

            tripAdapter.updateData(destinations ?: emptyList())

            refreshUiComponents()
        }

        tripViewModel.currentTrip.observe(viewLifecycleOwner) { trip ->
            if (trip == null) return@observe
            if (trip.status.isNotEmpty()) isSubmitting = false

            uiViewModel.updateUI(getUIConfig().copy(title = trip.title.ifEmpty { "New Journey" }))

            refreshUiComponents()
        }

        tripViewModel.isEditMode.observe(viewLifecycleOwner) { refreshUiComponents() }

        tripViewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                isSubmitting = false
                showError(it)
            }
        }
    }

    private fun refreshUiComponents() {
        val trip = tripViewModel.currentTrip.value ?: return
        val isEditing = tripViewModel.isEditMode.value ?: false
        val hasDestinations = !tripViewModel.selectedDestinations.value.isNullOrEmpty()

        if (trip.status.isEmpty() || isSubmitting) {
            binding.root.visibility = View.INVISIBLE
            return
        }

        binding.root.visibility = View.VISIBLE

        val isForcedCompleted = arguments?.getBoolean("is_completed") ?: false
        val isCompleted = isForcedCompleted || trip.isCompleted

        if (trip.startDate != null && trip.endDate != null) {
            val sdf = SimpleDateFormat("dd/MM", Locale.getDefault())
            binding.tripDepartureDateText.text =
                "${sdf.format(Date(trip.startDate))}"
            binding.tripReturnDateText.text =
                "${sdf.format(Date(trip.endDate))}"
            binding.departureDateContainer.visibility = View.VISIBLE
            binding.returnDateContainer.visibility = View.VISIBLE
        } else {
            binding.departureDateContainer.visibility = View.GONE
            binding.returnDateContainer.visibility = View.GONE
        }

        binding.editBtn.visibility = View.GONE
        binding.addDestinationBtn.visibility = View.GONE
        binding.confirmTripBtn.visibility = View.GONE
        tripAdapter.setEditMode(false)

        if (isCompleted) {
            return
        }

        if (hasDestinations) {
            binding.editBtn.visibility = View.VISIBLE
            binding.addDestinationBtn.visibility = View.VISIBLE

            if (trip.status == "DRAFT" || isEditing) {
                binding.confirmTripBtn.visibility = View.VISIBLE
                tripAdapter.setEditMode(true)
            }
        }
    }


    private fun setupRecyclerView() {
        tripAdapter = TripDestinationsAdapter(mutableListOf()) { sourceImageView, imageUrl ->
            zoomImage(sourceImageView, imageUrl)
        }
        binding.itineraryRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.itineraryRecyclerView.adapter = tripAdapter


        val itemTouchHelperCallback =
            object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
                override fun onMove(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder
                ): Boolean = false

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                    val position = viewHolder.adapterPosition
                    val destination = tripAdapter.getDestinationAt(position)

                    tripSelectionHelper.handleDeleteTripDestination(
                        destination,
                        onDeleted = {
                            showCustomMessage("Success", "Destination removed")
                        },
                        onCancel = {
                            tripAdapter.notifyItemChanged(position)
                        }
                    )
                }

//                override fun onChildDraw(
//                    c: Canvas,
//                    recyclerView: RecyclerView,
//                    viewHolder: RecyclerView.ViewHolder,
//                    dX: Float,
//                    dY: Float,
//                    actionState: Int,
//                    isCurrentlyActive: Boolean
//                ) {
//                    if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
//                        val holder = viewHolder as TripDestinationsAdapter.ViewHolder
//                        holder.cardContainer.translationX = dX
//                    } else {
//                        super.getSwipeDirs(recyclerView, viewHolder)
//                    }
//                }

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
                        val holder = viewHolder as TripDestinationsAdapter.ViewHolder
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


                override fun getSwipeDirs(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder
                ): Int {
                    val trip = tripViewModel.currentTrip.value
                    val isEditing = tripViewModel.isEditMode.value ?: false
                    if (trip?.status == "DRAFT" || (trip?.status == "PLANNED" && isEditing)) {
                        return super.getSwipeDirs(recyclerView, viewHolder)
                    }
                    return 0
                }
            }

        ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(binding.itineraryRecyclerView)
    }

    private fun zoomImage(sourceImageView: View, imageUrl: String) {
        val dialog =
            android.app.Dialog(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        dialog.setContentView(R.layout.dialog_full_image)

        val container = dialog.findViewById<View>(R.id.dialog_full_image)
        val fullImageView = dialog.findViewById<ImageView>(R.id.full_image_view)

        if (fullImageView == null || container == null) return

        Glide.with(this).load(imageUrl).into(fullImageView)

        val screenLocation = IntArray(2)
        sourceImageView.getLocationOnScreen(screenLocation)

        fullImageView.pivotX = 0f
        fullImageView.pivotY = 0f
        fullImageView.scaleX =
            sourceImageView.width.toFloat() / resources.displayMetrics.widthPixels
        fullImageView.scaleY =
            sourceImageView.height.toFloat() / resources.displayMetrics.heightPixels
        fullImageView.translationX = screenLocation[0].toFloat()
        fullImageView.translationY = screenLocation[1].toFloat()
        container.alpha = 0f

        dialog.show()

        fullImageView.animate()
            .scaleX(1f)
            .scaleY(1f)
            .translationX(0f)
            .translationY(0f)
            .setDuration(300)
            .start()

        container.animate().alpha(1f).setDuration(300).start()

        fullImageView.setOnClickListener {
            fullImageView.animate()
                .scaleX(sourceImageView.width.toFloat() / resources.displayMetrics.widthPixels)
                .scaleY(sourceImageView.height.toFloat() / resources.displayMetrics.heightPixels)
                .translationX(screenLocation[0].toFloat())
                .translationY(screenLocation[1].toFloat())
                .setDuration(300)
                .withEndAction { dialog.dismiss() }
                .start()
            container.animate().alpha(0f).setDuration(300).start()
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
