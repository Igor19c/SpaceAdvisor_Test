package com.example.spaceadvisor.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.example.spaceadvisor.R
import com.example.spaceadvisor.databinding.FragmentAbout1Binding
import com.example.spaceadvisor.ui.UIConfig

class AboutFragment : BaseFragment() {
    private var _binding: FragmentAbout1Binding? = null
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
        _binding = FragmentAbout1Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.titleAboutFragment.applyGradientText()
        binding.tvSuccessfulMissionsCounter.applyGradientText()
        binding.tvHappyTravelersCounter.applyGradientText()
        binding.tvYearsInOperationCounter.applyGradientText()
        binding.tvPlanetsVisitedCounter.applyGradientText()
        binding.footerIconAboutFragment.applyGradientTint(
            ContextCompat.getColor(requireContext(), R.color.space_blue),
            ContextCompat.getColor(requireContext(), R.color.space_pink),
            GradientDirection.HORIZONTAL
        )

    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}