package com.example.spaceadvisor.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.spaceadvisor.R
import com.example.spaceadvisor.SpaceAdvisorApplication
import com.example.spaceadvisor.databinding.FragmentFeedBinding
import com.example.spaceadvisor.domain.models.UIConfig
import com.example.spaceadvisor.ui.adapters.PostsAdapter
import com.example.spaceadvisor.ui.dialogs.SelectDialogTrip
import com.example.spaceadvisor.ui.viewmodels.FeedViewModel
import com.example.spaceadvisor.ui.viewmodels.TripViewModel
import com.example.spaceadvisor.ui.viewmodels.UserViewModel
import com.example.spaceadvisor.ui.viewmodels.ViewModelFactory
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class FeedFragment : BaseFragment() {

    private var _binding: FragmentFeedBinding? = null
    private val binding get() = _binding!!

    private val feedViewModel: FeedViewModel by activityViewModels {
        ViewModelFactory(requireActivity().application as SpaceAdvisorApplication)
    }
    private val userViewModel: UserViewModel by activityViewModels {
        ViewModelFactory(requireActivity().application as SpaceAdvisorApplication)
    }
    private val tripViewModel: TripViewModel by activityViewModels {
        ViewModelFactory(requireActivity().application as SpaceAdvisorApplication)
    }

    override fun getUIConfig() = UIConfig(
        title = "Feed",
        selectedTabId = R.id.nav_feed,
        isRightBtnVisible = false
    )

    private lateinit var adapter: PostsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFeedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupSwipeRefresh()
        observeViewModel()

        userViewModel.getCurrentUid()?.let { tripViewModel.fetchUserTrips(it) }

        binding.addPostFab.setOnClickListener {
            showSelectTripDialog()
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            feedViewModel.fetchPosts()
        }

        binding.swipeRefreshLayout.setColorSchemeResources(
            R.color.space_purple,
            R.color.space_purple,
            R.color.success
        )
    }

    private fun showSelectTripDialog() {
        val trips = tripViewModel.userTrips.value ?: emptyList()

        if (trips.isEmpty()) {
            showCustomMessage(
                title = "No trips found!",
                body = "You need at least one trip to create a post.",
                duration = 3000
            )
            return
        }

        SelectDialogTrip.newInstance().show(
            parentFragmentManager,
            SelectDialogTrip.TAG
        )
    }

    private fun setupRecyclerView() {
        adapter = PostsAdapter(
            posts = mutableListOf(),
            currentUserId = userViewModel.getCurrentUid(),
            onEditClick = { post ->
                val editFragment = PostCreateFragment()
                val bundle = Bundle()
                bundle.putSerializable("editingPost", post)
                editFragment.arguments = bundle

                parentFragmentManager.beginTransaction()
                    .replace(R.id.main_frame, editFragment)
                    .addToBackStack(null)
                    .commit()
            },
            onDeleteClick = { post ->
                MaterialAlertDialogBuilder(
                    requireContext(),
                    R.style.CustomAlertDialog
                ).setTitle("Delete Post")
                    .setMessage("Are you sure you want to remove this post from the feed?")
                    .setPositiveButton("Delete") { _, _ ->
                        feedViewModel.deletePost(post)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            },
            onLikeClick = { post ->
                val uid = userViewModel.getCurrentUid()
                if (uid != null) {
                    feedViewModel.toggleLikePost(uid, post)
                }
            }
        )
        binding.postsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.postsRecyclerView.adapter = adapter
    }

    private fun observeViewModel() {
        feedViewModel.posts.observe(viewLifecycleOwner) { posts ->
            adapter.updateData(posts)
            binding.postsRecyclerView.visibility = if (posts.isEmpty()) View.GONE else View.VISIBLE
            binding.swipeRefreshLayout.isRefreshing = false
        }

        feedViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            if (binding.swipeRefreshLayout.isRefreshing && !isLoading) {
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }

        feedViewModel.postSaved.observe(viewLifecycleOwner) { saved ->
            if (saved) {
                feedViewModel.resetPostSavedState()
                showCustomMessage(
                    title = "Post Shared!",
                    body = "Your space adventure is now live.",
                    duration = 3000
                )
            }
        }

        feedViewModel.postDeleted.observe(viewLifecycleOwner) { deleted ->
            if (deleted) {
                showCustomMessage("Post Removed", "Your post has been deleted successfully.")
                feedViewModel.resetPostDeletedState() // חשוב: אי
            }
        }

        feedViewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                showCustomMessage(
                    title = "Error",
                    body = it,
                    duration = 3000
                )
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}