package com.example.spaceadvisor.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.spaceadvisor.R
import com.example.spaceadvisor.databinding.FragmentFaqBinding
import com.example.spaceadvisor.databinding.ItemDestHazardBinding
import com.example.spaceadvisor.databinding.ItemFaqBinding
import com.example.spaceadvisor.domain.models.FAQ
import com.example.spaceadvisor.domain.models.Safety
import com.example.spaceadvisor.domain.repository.FAQRepository
import com.example.spaceadvisor.ui.UIConfig
import com.example.spaceadvisor.ui.adapters.FAQAdapter

class FAQFragment : BaseFragment() {
    override fun getUIConfig(): UIConfig {
        return UIConfig(
            title = "FAQ's",
            isHeaderVisible = true,
            isBottomNavVisible = false,
            selectedTabId = null,
            isLeftBtnVisible = true,
            leftIconRes = R.drawable.ic_back,
            onLeftClick = { parentFragmentManager.popBackStack() },
            isRightBtnVisible = false
        )
    }

    private var _binding: FragmentFaqBinding? = null
    private val binding get() = _binding!!


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFaqBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.headerIconFaqFragment.applyGradientTint()
        setupRVs()
//        setupButtons()
    }


    private fun setupRVs() {
        // General Questions
        binding.rvGeneral.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = FAQAdapter(FAQRepository.generalFaq)
        }

        binding.rvDestinationExploration.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = FAQAdapter(FAQRepository.destinationExplorationFaq)


        }

        // Trip Planning
        binding.rvTripPlanning.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = FAQAdapter(FAQRepository.tripPlanningFaq)
        }

        // Technical
        binding.rvTechnical.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = FAQAdapter(FAQRepository.technicalFaq)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}