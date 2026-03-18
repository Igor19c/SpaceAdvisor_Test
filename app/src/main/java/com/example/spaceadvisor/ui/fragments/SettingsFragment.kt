package com.example.spaceadvisor.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import com.example.spaceadvisor.R
import com.example.spaceadvisor.databinding.FragmentSettingsBinding
import com.example.spaceadvisor.ui.UIConfig

class SettingsFragment : BaseFragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun getUIConfig(): UIConfig {
        return UIConfig(
            title = "Settings",
            selectedTabId = null,
            isHeaderVisible = true,
            leftIconRes = R.drawable.ic_back,
            isLeftBtnVisible = true,
            onLeftClick = { parentFragmentManager.popBackStack() },
            isRightBtnVisible = false
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnEditProfileSettingsFragment.setOnClickListener {

        }

        binding.btnChangePassSettingsFragment.setOnClickListener {

        }

        binding.btnDeleteProfileSettingsFragment.setOnClickListener {

        }

        binding.btnLogoutSettingsFragment.setOnClickListener {

        }

        binding.darkModeSwitchSettingsFragment.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
        }

        binding.accentColorContainerSettingsFragment.setOnClickListener {

        }

        binding.btnFaqSettingsFragment.setOnClickListener {

        }

        binding.btnAboutSettingsFragment.setOnClickListener {

        }
    }
}

