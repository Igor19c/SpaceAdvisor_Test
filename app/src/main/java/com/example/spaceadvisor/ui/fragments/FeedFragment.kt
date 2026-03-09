package com.example.spaceadvisor.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.spaceadvisor.R
import com.example.spaceadvisor.SpaceAdvisorApplication
import com.example.spaceadvisor.databinding.DialogSelectTripBinding
import com.example.spaceadvisor.databinding.FragmentFeedBinding
import com.example.spaceadvisor.domain.models.Trip
import com.example.spaceadvisor.ui.UIConfig
import com.example.spaceadvisor.ui.adapters.PostsAdapter
import com.example.spaceadvisor.ui.adapters.TripPickerAdapter
import com.example.spaceadvisor.ui.viewmodels.FeedViewModel
import com.example.spaceadvisor.ui.viewmodels.TripViewModel
import com.example.spaceadvisor.ui.viewmodels.UserViewModel
import com.example.spaceadvisor.ui.viewmodels.ViewModelFactory
import com.google.android.material.bottomsheet.BottomSheetDialog

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
        selectedTabId = R.id.nav_feed
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
        observeViewModel()

        userViewModel.getCurrentUid()?.let { tripViewModel.fetchUserTrips(it) }

        binding.addPostFab.setOnClickListener {
            showSelectTripDialog()
        }
    }

    private fun showSelectTripDialog() {
        val dialog = BottomSheetDialog(requireContext())
        val dialogBinding = DialogSelectTripBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)

        var selectedTrip: Trip? = null

        tripViewModel.userTrips.observe(viewLifecycleOwner) { trips ->
            if (trips.isEmpty()) {
                Toast.makeText(requireContext(), "No trips found!", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                return@observe
            }

            val pickerAdapter = TripPickerAdapter(trips) { trip ->
                selectedTrip = trip
                dialogBinding.continueBtn.isEnabled = true
            }

            dialogBinding.tripRecyclerView.layoutManager = GridLayoutManager(requireContext(), 3)
            dialogBinding.tripRecyclerView.adapter = pickerAdapter
        }

        dialogBinding.continueBtn.setOnClickListener {
            selectedTrip?.let { trip ->
                val createFragment = CreatePostFragment()
                val bundle = Bundle()
                bundle.putSerializable("selectedTrip", trip)
                createFragment.arguments = bundle

                parentFragmentManager.beginTransaction()
                    .replace(R.id.main_frame, createFragment)
                    .addToBackStack(null)
                    .commit()

                dialog.dismiss()
            }
        }

        dialogBinding.cancelBtn.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun setupRecyclerView() {
        adapter = PostsAdapter(
            posts = mutableListOf(),
            currentUserId = userViewModel.getCurrentUid(),
            onEditClick = { post ->
                val editFragment = CreatePostFragment()
                val bundle = Bundle()
                bundle.putSerializable("editingPost", post)
                editFragment.arguments = bundle

                parentFragmentManager.beginTransaction()
                    .replace(R.id.main_frame, editFragment)
                    .addToBackStack(null)
                    .commit()
            },
            onDeleteClick = { post ->
                feedViewModel.deletePost(post)
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
        }

        feedViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.loadingLayout.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        feedViewModel.postSaved.observe(viewLifecycleOwner) { saved ->
            if (saved) {
                feedViewModel.resetPostSavedState()
                Toast.makeText(requireContext(), "Post Shared!", Toast.LENGTH_SHORT).show()
            }
        }

        feedViewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let { Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show() }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
