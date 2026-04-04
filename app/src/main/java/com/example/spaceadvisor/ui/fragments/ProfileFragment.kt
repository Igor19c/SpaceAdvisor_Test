package com.example.spaceadvisor.ui.fragments

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityOptionsCompat
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.signature.ObjectKey
import com.example.spaceadvisor.R
import com.example.spaceadvisor.SpaceAdvisorApplication
import com.example.spaceadvisor.ui.UIConfig
import com.example.spaceadvisor.ui.activities.AuthActivity
import com.example.spaceadvisor.ui.adapters.BadgeAdapter
import com.example.spaceadvisor.ui.adapters.NewestTripsAdapter
import com.example.spaceadvisor.ui.adapters.ReviewAdapter
import com.example.spaceadvisor.databinding.DialogBadgeUnlockedBinding
import com.example.spaceadvisor.databinding.FragmentProfileBinding
import com.example.spaceadvisor.domain.models.Badge
import com.example.spaceadvisor.domain.repository.BadgeRepository
import com.example.spaceadvisor.ui.activities.LoadingType
import com.example.spaceadvisor.ui.viewmodels.FeedViewModel
import com.example.spaceadvisor.ui.viewmodels.TripViewModel
import com.example.spaceadvisor.ui.viewmodels.UserViewModel
import com.example.spaceadvisor.ui.viewmodels.ViewModelFactory
import java.util.LinkedList
import com.firebase.ui.auth.AuthUI
import java.util.Queue

class ProfileFragment : BaseFragment() {

    private var _binding: FragmentProfileBinding? = null
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

    private lateinit var newestTripsAdapter: NewestTripsAdapter
    private lateinit var badgesAdapter: BadgeAdapter
    private lateinit var reviewAdapter: ReviewAdapter

    private val badgeDialogQueue: Queue<Badge> = LinkedList()
    private var isBadgeDialogShowing = false

    override fun getUIConfig() = UIConfig(
        title = "My Profile",
        selectedTabId = R.id.nav_profile
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupBadgesRecyclerView()
        setupTripsRecyclerView()
        setupReviewsRecyclerView()
        observeViewModel()

        userViewModel.getCurrentUid()?.let { uid ->
            tripViewModel.fetchUserTrips(uid)
            userViewModel.startListening(uid)
            feedViewModel.fetchUserPosts(uid)
        }

        binding.signOutBtn.setOnClickListener { handleSignOut() }
        binding.savedBtn.setOnClickListener { navigateTo(SavedDestinationsFragment()) }
        binding.myTripsSeeAllBtn.setOnClickListener { navigateTo(MyTripsFragment()) }
        binding.reviewsSeeAllBtn.setOnClickListener { navigateTo(MyReviewsFragment()) }
        binding.editProfBtn.setOnClickListener {
            navigateTo(
                EditProfileFragment.newInstance(
                    isFirstTime = false
                )
            )
        }

        val isNewUser = requireActivity().intent.getBooleanExtra("IS_NEW_USER", false)
        if (isNewUser) {
            requireActivity().intent.removeExtra("IS_NEW_USER")
            navigateTo(EditProfileFragment.newInstance(isFirstTime = true))
        }
    }

    private fun handleSignOut() {
        showLoading("See you again!", LoadingType.LOTTIE)
        userViewModel.handleSignOut(requireContext())
    }

    private fun observeUserViewModel() {
        userViewModel.userData.observe(viewLifecycleOwner) { user ->
            user?.let {
                binding.userName.text = it.name
                binding.userNickname.text = it.username
                binding.userBio.text = it.bio
                binding.savedCounter.text = it.favoriteDestinations.size.toString()

                val fullBadges = BadgeRepository.getBadgesByIds(it.badges)
                badgesAdapter.updateBadges(fullBadges)

                if (!it.profileImageUrl.isNullOrEmpty()) {
                    Glide.with(this).load(it.profileImageUrl)
                        .placeholder(binding.userPic.drawable)
                        .signature(ObjectKey(it.profileImageUrl + (System.currentTimeMillis() / (1000 * 60 * 10))))
                        .diskCacheStrategy(DiskCacheStrategy.ALL).dontAnimate().circleCrop()
                        .into(binding.userPic)
                }
            }
        }

        userViewModel.newBadgesEarned.observe(viewLifecycleOwner) { badgeIds ->
            if (!badgeIds.isNullOrEmpty()) {
                badgeIds.forEach { id ->
                    BadgeRepository.getBadgeById(id)?.let { badgeDialogQueue.add(it) }
                }
                userViewModel.onBadgesDialogShown()
                showNextBadgeInQueue()
            }
        }

        userViewModel.userReviews.observe(viewLifecycleOwner) { reviews ->
            val recentReviews = reviews.take(5)
            reviewAdapter.updateData(recentReviews)

            binding.recentReviewsRecyclerView.visibility =
                if (recentReviews.isEmpty()) View.GONE else View.VISIBLE
        }

        userViewModel.isLoggedOut.observe(viewLifecycleOwner) { loggedOut ->
            if (loggedOut) {
                navigateToAuth()
            }
        }
    }

    private fun showNextBadgeInQueue() {
        if (isBadgeDialogShowing || badgeDialogQueue.isEmpty()) return
        val badge = badgeDialogQueue.poll() ?: return
        showBadgeUnlockedDialog(badge)
    }

    private fun showBadgeUnlockedDialog(badge: Badge) {
        isBadgeDialogShowing = true
        val dialog = Dialog(requireContext())
        val dialogBinding = DialogBadgeUnlockedBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialogBinding.dialogBadgeName.text = badge.name
        dialogBinding.dialogBadgeDescription.text = badge.description
        val assetPath = "file:///android_asset/${badge.assetPath}"
        Glide.with(this).load(assetPath).into(dialogBinding.dialogBadgeImage)
        dialogBinding.dialogCloseBtn.setOnClickListener { dialog.dismiss() }
        dialog.setOnDismissListener {
            isBadgeDialogShowing = false
            showNextBadgeInQueue()
        }
        dialog.show()
    }

    private fun observeTripViewModel() {
        tripViewModel.userTrips.observe(viewLifecycleOwner) { trips ->
            val recentTrips = trips.take(5)
            newestTripsAdapter.updateData(recentTrips)
            binding.tripsCounter.text = trips.size.toString()
            binding.myNewestTripsRecyclerView.visibility =
                if (recentTrips.isEmpty()) View.GONE else View.VISIBLE
            val posts = feedViewModel.userPosts.value ?: emptyList()
            userViewModel.checkForBadges(trips, posts)
        }
    }

    private fun observeFeedViewModel() {
        feedViewModel.userPosts.observe(viewLifecycleOwner) { posts ->
            binding.postsCounter.text = posts.size.toString()
            val trips = tripViewModel.userTrips.value ?: emptyList()
            userViewModel.checkForBadges(trips, posts)
        }
    }

    private fun observeViewModel() {
        observeUserViewModel()
        observeTripViewModel()
        observeFeedViewModel()
        userViewModel.error.observe(viewLifecycleOwner) { errorMessage ->
            errorMessage?.let { showError(it) }
        }
    }

    private fun setupReviewsRecyclerView() {
        reviewAdapter = ReviewAdapter()
        binding.recentReviewsRecyclerView.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.recentReviewsRecyclerView.adapter = reviewAdapter
    }


    private fun setupTripsRecyclerView() {
        newestTripsAdapter = NewestTripsAdapter(mutableListOf()) { trip ->
            tripViewModel.setCurrentTrip(trip)
            navigateTo(TripFragment())
        }
        binding.myNewestTripsRecyclerView.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.myNewestTripsRecyclerView.adapter = newestTripsAdapter
    }

    private fun setupBadgesRecyclerView() {
        badgesAdapter = BadgeAdapter(emptyList())
        binding.badgeRecyclerView.apply {
            layoutManager = GridLayoutManager(requireContext(), 3); isNestedScrollingEnabled =
            false; adapter = badgesAdapter
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
