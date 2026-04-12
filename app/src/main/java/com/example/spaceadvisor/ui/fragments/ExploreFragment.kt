package com.example.spaceadvisor.ui.fragments

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.MarginPageTransformer
import androidx.viewpager2.widget.ViewPager2
import com.example.spaceadvisor.R
import com.example.spaceadvisor.SpaceAdvisorApplication
import com.example.spaceadvisor.databinding.FragmentExploreBinding
import com.example.spaceadvisor.domain.models.Destination
import com.example.spaceadvisor.domain.models.UIConfig
import com.example.spaceadvisor.ui.adapters.CarouselAdapter
import com.example.spaceadvisor.utils.TripSelectionHelper
import com.example.spaceadvisor.ui.viewmodels.DestinationViewModel
import com.example.spaceadvisor.ui.viewmodels.TripViewModel
import com.example.spaceadvisor.ui.viewmodels.ViewModelFactory
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayoutMediator

class ExploreFragment : BaseFragment() {

    private var _binding: FragmentExploreBinding? = null
    private val binding get() = _binding!!

    private val destinationViewModel: DestinationViewModel by activityViewModels {
        ViewModelFactory(requireActivity().application as SpaceAdvisorApplication)
    }

    private val tripViewModel: TripViewModel by activityViewModels {
        ViewModelFactory(requireActivity().application as SpaceAdvisorApplication)
    }

    private lateinit var carouselAdapter: CarouselAdapter
    private var mediator: TabLayoutMediator? = null
    private var isLevelTransition = false
    private lateinit var tripSelectionHelper: TripSelectionHelper
    private var pendingDestinationToAdd: Destination? = null

    // Header View References
    private var headerProgressBar: ProgressBar? = null
    private var headerDotPlanet: ImageView? = null
    private var headerDotLocation: ImageView? = null
    private var headerLabelPlanet: TextView? = null
    private var headerLabelLocation: TextView? = null

    override fun getUIConfig(): UIConfig {
        val context = context ?: return UIConfig(
            selectedTabId = R.id.nav_explore,
            isHeaderVisible = true
        )

        val headerView = LayoutInflater.from(context).inflate(R.layout.layout_explore_header, null)
        headerProgressBar = headerView.findViewById(R.id.trip_progress_bar_header)
        headerDotPlanet = headerView.findViewById(R.id.dot_planet_header)
        headerDotLocation = headerView.findViewById(R.id.dot_location_header)
        headerLabelPlanet = headerView.findViewById(R.id.dot_label_planet_header)
        headerLabelLocation = headerView.findViewById(R.id.dot_label_location_header)

        val navState = destinationViewModel.navigationUiState.value
        if (navState != null) {
            updateProgressUI(navState.planetActive, navState.locationActive)
        }

        val isRoot: Boolean = destinationViewModel.currentExploreParentId == "root"

        return UIConfig(
            selectedTabId = R.id.nav_explore,
            isHeaderVisible = true,
            isLeftBtnVisible = true,
            leftIconRes = if (isRoot) R.drawable.ic_settings else R.drawable.ic_back,
            onLeftClick = if (isRoot) null else {
                { handleBackNavigation() }
            },
            isRightBtnVisible = true,
            customHeaderView = headerView
        )
    }

    data class NavigationState(
        val parentId: String, val type: String?, val title: String?, val selectedChildIndex: Int
    ) : java.io.Serializable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (destinationViewModel.currentExploreParentId == "root" && savedInstanceState == null) {
            arguments?.getSerializable("target_destination")?.let {
                val dest = it as Destination
                destinationViewModel.currentExploreParentId = dest.id
                destinationViewModel.currentExploreParentType = dest.type
                destinationViewModel.currentExploreParentTitle = dest.title
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExploreBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tripSelectionHelper = TripSelectionHelper(this, tripViewModel) { destination ->
            pendingDestinationToAdd = destination
        }

        setupViewPager()
        observeViewModel()

        binding.exploreHelpBtn.setOnClickListener {
            showWelcomeDialog()
        }

        loadDestinationsWithAnimation(animateOut = false)
    }

    private fun setupViewPager() {
        carouselAdapter = CarouselAdapter(emptyList())

        binding.carouselViewPager.apply {
            adapter = carouselAdapter
            offscreenPageLimit = 3
            getChildAt(0).overScrollMode = RecyclerView.OVER_SCROLL_NEVER
            setPageTransformer(MarginPageTransformer(40))

            registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    if (carouselAdapter.itemCount > 0) {
                        updateDestinationDetails(carouselAdapter.getItem(position))
                    }
                }
            })
        }
    }

    private fun updateDestinationDetails(destination: Destination) {
        if (isLevelTransition) {
            applyDestinationData(destination)
            return
        }

        val fadeOut = AlphaAnimation(1f, 0f).apply { duration = 200 }
        val fadeIn = AlphaAnimation(0f, 1f).apply { duration = 300 }

        binding.destTitleCarouselItem.startAnimation(fadeOut)
        binding.destSubtitleCarouselItem.startAnimation(fadeOut)
        binding.destTravelTimeContainerCarouselItem.startAnimation(fadeOut)
        binding.destRatingChipCarouselItem.startAnimation(fadeOut)

        fadeOut.setAnimationListener(object :
            android.view.animation.Animation.AnimationListener {
            override fun onAnimationStart(p0: android.view.animation.Animation?) {}
            override fun onAnimationRepeat(p0: android.view.animation.Animation?) {}
            override fun onAnimationEnd(p0: android.view.animation.Animation?) {
                applyDestinationData(destination)
                binding.destTitleCarouselItem.startAnimation(fadeIn)
                binding.destSubtitleCarouselItem.startAnimation(fadeIn)
                binding.destTravelTimeContainerCarouselItem.startAnimation(fadeIn)
                binding.destRatingChipCarouselItem.startAnimation(fadeIn)
            }
        })
    }

    private fun applyDestinationData(destination: Destination) {
        binding.destRatingTextCarouselItem.text = String.format("%.1f", destination.ratingAvg)
        binding.destTitleCarouselItem.text = destination.title
        binding.destSubtitleCarouselItem.text = destination.subtitle

        if (destination.travelTimeFromEarth != null) {
            binding.destTravelTimeTextCarouselItem.text =
                "${destination.travelTimeFromEarth.value} ${destination.travelTimeFromEarth.unit}"
        }

        setupPriceTierIcons(
            binding.destPriceTierContainerCarouselItem,
            destination.priceTier
        )
        setupSafetyIcons(
            binding.destSafetyLvlContainerCarouselItem,
            destination.safetyLevel
        )

        if (destination.childCount == 0) {
            binding.destDifficultyContainerCarouselItem.visibility = View.VISIBLE
            setupDifficultyIcons(
                binding.destDifficultyContainerCarouselItem,
                destination.difficulty
            )
        } else
            binding.destDifficultyContainerCarouselItem.visibility = View.GONE

        binding.detailsBtn.setOnClickListener { openDestinationDetails(destination) }
        binding.selectBtn.text = if (destination.childCount == 0) "Add to Trip" else "Explore"
        binding.selectBtn.setOnClickListener {
            if (destination.childCount > 0) {
                navigateToChildren(destination)
            } else {
                tripSelectionHelper.handleAddToTrip(destination)
            }
        }
    }

    private fun setupDifficultyIcons(container: LinearLayout, level: Int) {
        container.removeAllViews()

        val icons = listOf(
            R.drawable.ic_easy_emoji,
            R.drawable.ic_medium_emoji,
            R.drawable.ic_hard_emoji
        )

        for (i in 1..3) {
            val iconView = ImageView(requireContext())
            val size = (16 * resources.displayMetrics.density).toInt()
            val params = LinearLayout.LayoutParams(size, size)
            params.setMargins(0, 0, 10, 0)
            iconView.layoutParams = params

            val iconRes = icons[i - 1]
            iconView.setImageResource(iconRes)

            val tintColor = if (i == level) {
                when (level) {
                    1 -> ContextCompat.getColor(requireContext(), R.color.success)
                    2 -> ContextCompat.getColor(requireContext(), R.color.achievement)
                    3 -> ContextCompat.getColor(requireContext(), R.color.alert)
                    else -> ContextCompat.getColor(requireContext(), R.color.body)
                }
            } else {
                ContextCompat.getColor(requireContext(), R.color.body)
            }

            iconView.setColorFilter(tintColor, android.graphics.PorterDuff.Mode.SRC_IN)
            container.addView(iconView)
        }
    }

    private fun setupPriceTierIcons(container: LinearLayout, level: Int) {
        container.removeAllViews()
        for (i in 1..4) {
            val icon = ImageView(requireContext())
            val size = (18 * resources.displayMetrics.density).toInt()
            val params = LinearLayout.LayoutParams(size, size)
            if (i < 4)
                params.setMargins(0, 0, -6, 0)
            icon.layoutParams = params

            val iconRes = R.drawable.ic_dollar_banknote
            icon.setImageResource(iconRes)

            val tintColor = if (i <= level) {
                resources.getColor(R.color.space_green_light, null)
            } else {
                resources.getColor(R.color.space_green_light_faded, null)
            }
            icon.setColorFilter(tintColor, android.graphics.PorterDuff.Mode.SRC_IN)
            container.addView(icon)
        }
    }

    private fun setupSafetyIcons(container: LinearLayout, level: Int) {
        container.removeAllViews()
        for (i in 1..5) {
            val star = ImageView(requireContext())
            val size = (17 * resources.displayMetrics.density).toInt()
            star.layoutParams = LinearLayout.LayoutParams(size, size)

            val iconRes = R.drawable.ic_hazard
            star.setImageResource(iconRes)

            val tintColor = if (i <= level) {
                resources.getColor(R.color.space_red, null)
            } else {
                resources.getColor(R.color.space_red_faded, null)
            }
            star.setColorFilter(tintColor, android.graphics.PorterDuff.Mode.SRC_IN)
            container.addView(star)
        }
    }

    private fun observeViewModel() {
        destinationViewModel.destinations.observe(viewLifecycleOwner) { list ->
            if (list.isNotEmpty()) {
                carouselAdapter.updateData(list)

                mediator?.detach()
                mediator = TabLayoutMediator(
                    binding.carouselIndicator,
                    binding.carouselViewPager
                ) { tab, _ ->
                    tab.setIcon(R.drawable.tab_selector)
                }
                mediator?.attach()

                var indexToSelect = destinationViewModel.explorePendingSelectedIndex ?: 0

                destinationViewModel.exploreTargetIdToRestore?.let { targetId ->
                    val foundIndex = list.indexOfFirst { it.id == targetId }
                    if (foundIndex != -1) {
                        indexToSelect = foundIndex
                    }
                    destinationViewModel.exploreTargetIdToRestore = null
                }

                val safeIndex = if (indexToSelect < list.size) indexToSelect else 0

                binding.carouselViewPager.post {
                    if (_binding == null) return@post
                    binding.carouselViewPager.setCurrentItem(safeIndex, false)
                    binding.carouselIndicator.selectTab(binding.carouselIndicator.getTabAt(safeIndex))

                    applyDestinationData(list[safeIndex])

                    binding.exploreContentLayout.animate()
                        .alpha(1f)
                        .setDuration(400)
                        .withEndAction {
                            destinationViewModel.explorePendingSelectedIndex = null
                            isLevelTransition = false
                        }
                        .start()

                    binding.carouselViewPager.requestLayout()
                }
            }
        }

        destinationViewModel.navigationUiState.observe(viewLifecycleOwner) { state ->
            updateProgressUI(state.planetActive, state.locationActive)
        }

        destinationViewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                val currentContext = context ?: return@observe
                showError(it)
                binding.exploreContentLayout.alpha = 1f
                isLevelTransition = false
            }
        }

        tripViewModel.currentTrip.observe(viewLifecycleOwner) { trip ->
            if (trip != null && trip.id.isNotEmpty() && pendingDestinationToAdd != null) {
                tripViewModel.addDestination(pendingDestinationToAdd!!)
                pendingDestinationToAdd = null
            }
        }
    }

    private fun loadDestinationsWithAnimation(animateOut: Boolean = true) {
        isLevelTransition = true

        if (animateOut) {
            binding.exploreContentLayout.animate()
                .alpha(0f)
                .setDuration(300)
                .withEndAction {
                    performDataFetch()
                }
                .start()
        } else {
            binding.exploreContentLayout.alpha = 0f
            performDataFetch()
        }
    }

    private fun performDataFetch() {
        uiViewModel.updateUI(getUIConfig())
        destinationViewModel.fetchDestinations(
            destinationViewModel.currentExploreParentId,
            destinationViewModel.currentExploreParentType
        )
    }

    private fun navigateToChildren(destination: Destination) {
        val currentIndex = binding.carouselViewPager.currentItem

        destinationViewModel.exploreNavigationStack.push(
            NavigationState(
                destinationViewModel.currentExploreParentId,
                destinationViewModel.currentExploreParentType,
                destinationViewModel.currentExploreParentTitle,
                currentIndex
            )
        )
        destinationViewModel.currentExploreParentId = destination.id
        destinationViewModel.currentExploreParentType = destination.type
        destinationViewModel.currentExploreParentTitle = destination.title

        loadDestinationsWithAnimation()
    }

    private fun openDestinationDetails(destination: Destination) {
        destinationViewModel.explorePendingSelectedIndex = binding.carouselViewPager.currentItem

        val fragment = DestinationFragment().apply {
            arguments = Bundle().apply {
                putSerializable("destination_key", destination)
            }
        }

        parentFragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.slide_in_bottom_to_top,
                R.anim.fade_out,
                R.anim.fade_in,
                R.anim.slide_out_top_to_bottom
            )
            .add(R.id.main_frame, fragment)
            .hide(this)
            .addToBackStack(null)
            .commit()
    }

    private fun updateProgressUI(bodyActive: Boolean, locationActive: Boolean) {
        val currentParentId = destinationViewModel.currentExploreParentId
        val progress = if (currentParentId == "root") 3 else 100

        headerProgressBar?.progress = progress
        headerDotPlanet?.setImageResource(if (bodyActive || currentParentId == "root") R.drawable.ic_dot_active else R.drawable.ic_dot_inactive)
        headerDotLocation?.setImageResource(if (locationActive) R.drawable.ic_dot_active else R.drawable.ic_dot_inactive)
        headerDotLocation?.backgroundTintList =
            (if (locationActive) null else ColorStateList.valueOf(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.explore_header_dot_inactive
                )
            ))

        val currentContext = context ?: return
        val activeColor = ContextCompat.getColor(currentContext, R.color.subtitle)
        val inactiveColor = ContextCompat.getColor(currentContext, R.color.body)

        headerLabelPlanet?.setTextColor(if (bodyActive || currentParentId == "root") activeColor else inactiveColor)
        headerLabelLocation?.setTextColor(if (locationActive) activeColor else inactiveColor)
    }

    private fun handleBackNavigation() {
        if (destinationViewModel.exploreNavigationStack.isNotEmpty()) {
            val previousState = destinationViewModel.exploreNavigationStack.pop()
            destinationViewModel.currentExploreParentId = previousState.parentId
            destinationViewModel.currentExploreParentType = previousState.type
            destinationViewModel.currentExploreParentTitle = previousState.title
            destinationViewModel.explorePendingSelectedIndex = previousState.selectedChildIndex
            loadDestinationsWithAnimation()
        } else if (destinationViewModel.currentExploreParentId != "root") {
            destinationViewModel.exploreTargetIdToRestore =
                destinationViewModel.currentExploreParentId
            destinationViewModel.currentExploreParentId = "root"
            destinationViewModel.currentExploreParentType = null
            destinationViewModel.currentExploreParentTitle = null
            loadDestinationsWithAnimation()
        }
    }

    private fun showWelcomeDialog() {
        val currentContext = context ?: return
        val dialogView =
            LayoutInflater.from(currentContext).inflate(R.layout.dialog_explore_explanation, null)
        val dialog = MaterialAlertDialogBuilder(currentContext, R.style.CustomAlertDialog)
            .setView(dialogView)
            .show()

        dialogView.findViewById<View>(R.id.lets_go_btn_explore_dialog).setOnClickListener {
            dialog.dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mediator?.detach()
        mediator = null
        _binding = null

        headerProgressBar = null
        headerDotPlanet = null
        headerDotLocation = null
        headerLabelPlanet = null
        headerLabelLocation = null
    }
}
