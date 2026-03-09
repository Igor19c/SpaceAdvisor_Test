package com.example.spaceadvisor.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.spaceadvisor.R
import com.example.spaceadvisor.SpaceAdvisorApplication
import com.example.spaceadvisor.databinding.FragmentMyReviewsBinding
import com.example.spaceadvisor.ui.UIConfig
import com.example.spaceadvisor.ui.adapters.ReviewAdapter
import com.example.spaceadvisor.ui.viewmodels.DestinationViewModel
import com.example.spaceadvisor.ui.viewmodels.UserViewModel
import com.example.spaceadvisor.ui.viewmodels.ViewModelFactory

class MyReviewsFragment : BaseFragment() {

    private var _binding: FragmentMyReviewsBinding? = null
    private val binding get() = _binding!!

    private val userViewModel: UserViewModel by activityViewModels {
        ViewModelFactory(requireActivity().application as SpaceAdvisorApplication)
    }
    private val destinationViewModel: DestinationViewModel by activityViewModels {
        ViewModelFactory(requireActivity().application as SpaceAdvisorApplication)
    }

    private lateinit var adapter: ReviewAdapter

    override fun getUIConfig() = UIConfig(
        title = "My Reviews",
        isHeaderVisible = false,
        selectedTabId = R.id.nav_profile
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMyReviewsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeViewModel()

        userViewModel.getCurrentUid()?.let { uid ->
            userViewModel.startListening(uid)
        }

        binding.myReviewsBackBtn.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun setupRecyclerView() {
        adapter = ReviewAdapter(listOf())
        binding.myReviewsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.myReviewsRecyclerView.adapter = adapter
    }

    private fun observeViewModel() {
        userViewModel.userReviews.observe(viewLifecycleOwner) { reviews ->
            if (reviews != null) {
                adapter.updateData(reviews)
//                binding.noReviewsTv.visibility = if (reviews.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}