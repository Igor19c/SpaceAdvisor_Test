package com.example.spaceadvisor.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.spaceadvisor.R
import com.example.spaceadvisor.databinding.FragmentAboutBinding
import com.example.spaceadvisor.domain.models.UIConfig
import com.example.spaceadvisor.utils.applyGradientText
import com.example.spaceadvisor.utils.applyGradientTint

class AboutFragment : BaseFragment() {
    private var _binding: FragmentAboutBinding? = null
    private val binding get() = _binding!!

    override fun getUIConfig(): UIConfig {
        return UIConfig(
            title = "About",
            selectedTabId = null,
            isHeaderVisible = true,
            leftIconRes = R.drawable.ic_back,
            isLeftBtnVisible = true,
            onLeftClick = { parentFragmentManager.popBackStack() },
            isRightBtnVisible = false,
            isBottomNavVisible = false
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAboutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.titleAboutFragment.applyGradientText()
        binding.tvSuccessfulMissionsCounter.applyGradientText()
        binding.tvHappyTravelersCounter.applyGradientText()
        binding.tvYearsInOperationCounter.applyGradientText()
        binding.tvPlanetsVisitedCounter.applyGradientText()
        binding.footerIconAboutFragment.applyGradientTint()
        binding.btnBookJourneyAboutFragment.setOnClickListener { navigateTo(ExploreFragment()) }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}