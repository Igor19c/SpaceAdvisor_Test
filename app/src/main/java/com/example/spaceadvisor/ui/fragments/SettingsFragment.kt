package com.example.spaceadvisor.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.transition.TransitionManager
import com.example.spaceadvisor.R
import com.example.spaceadvisor.SpaceAdvisorApplication
import com.example.spaceadvisor.databinding.DialogChooseImageSourceBinding
import com.example.spaceadvisor.databinding.FragmentSettingsBinding
import com.example.spaceadvisor.ui.UIConfig
import com.example.spaceadvisor.ui.activities.LoadingType
import com.example.spaceadvisor.ui.adapters.ColorAdapter
import com.example.spaceadvisor.ui.viewmodels.UserViewModel
import com.example.spaceadvisor.ui.viewmodels.ViewModelFactory
import com.example.spaceadvisor.utils.SettingsManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class SettingsFragment : BaseFragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private lateinit var settingsManager: SettingsManager

    private val userViewModel: UserViewModel by activityViewModels {
        ViewModelFactory(requireActivity().application as SpaceAdvisorApplication)
    }

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
        settingsManager =
            (requireActivity().application as SpaceAdvisorApplication).appContainer.settingsManager
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val isDarkMode = settingsManager.isDarkMode
        binding.darkModeSwitchSettingsFragment.isChecked = isDarkMode
        updateDarkModeIcon(isDarkMode)

        binding.darkModeSwitchSettingsFragment.setOnCheckedChangeListener { _, isChecked ->
            // מציגים Loading כדי להסתיר את המעבר
            showLoading("Applying theme...", LoadingType.LOTTIE)
            
            // דיליי קטן כדי שהאנימציה תתחיל לפני ה-Recreate
            binding.root.postDelayed({
                TransitionManager.beginDelayedTransition(binding.darkModeContainerSettingsFragment)
                updateDarkModeIcon(isChecked)
                settingsManager.isDarkMode = isChecked
                // הערה: settingsManager.isDarkMode מפעיל Recreate אוטומטי דרך AppCompatDelegate
            }, 400)
        }

        binding.btnAccentColorSettingsFragment.setOnClickListener {
            showColorPickerDialog()
        }

        userViewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            if (loading) {
                showLoading("Signing out...", LoadingType.LOTTIE)
            } else {
                hideLoading()
            }
        }

        userViewModel.isLoggedOut.observe(viewLifecycleOwner) { loggedOut ->
            if (loggedOut) {
                navigateToAuth()
            }
        }

        binding.btnLogoutSettingsFragment.setOnClickListener {
            handleLogout()
        }

        binding.btnEditProfileSettingsFragment.setOnClickListener {
            navigateTo(
                EditProfileFragment.newInstance(
                    isFirstTime = false
                )
            )
        }

        binding.btnChangePassSettingsFragment.setOnClickListener { }
        binding.btnDeleteProfileSettingsFragment.setOnClickListener { }
        
        binding.btnFaqSettingsFragment.setOnClickListener { navigateTo(FAQFragment()) }

        binding.btnAboutSettingsFragment.setOnClickListener { navigateTo(AboutFragment()) }
    }

    private fun updateDarkModeIcon(isDark: Boolean) {
        val iconRes = if (isDark) R.drawable.ic_moon else R.drawable.ic_sun
        binding.btnDarkModeSetingsFragment.setIconResource(iconRes)
        binding.darkModeSwitchSettingsFragment.thumbIconDrawable =
            ContextCompat.getDrawable(requireContext(), iconRes)
    }

    private fun showColorPickerDialog() {
        val dialog = BottomSheetDialog(requireContext())
        val dialogBinding = DialogChooseImageSourceBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)

        dialogBinding.dialogSelectTitle.text = "Select Accent Color"

        val themeOptions = listOf(
            Triple("Space Blue", R.color.accent_space_purple, R.style.Theme_SpaceAdvisor),
            Triple("Muted Red", R.color.accent_red_muted, R.style.Theme_SpaceAdvisor_Red),
            Triple("Muted Green", R.color.accent_green_muted, R.style.Theme_SpaceAdvisor_Green)
        )

        dialogBinding.itemRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = ColorAdapter(themeOptions.map { Pair(it.first, it.second) }) { index ->
                val selectedTheme = themeOptions[index].third
                
                showLoading("Updating accent color...", LoadingType.LOTTIE)
                dialog.dismiss()

                binding.root.postDelayed({
                    settingsManager.selectedThemeResId = selectedTheme
                    requireActivity().recreate()
                }, 400)
            }
        }

        dialog.show()
    }

    fun handleLogout(
        onLogout: (() -> Unit)? = null,
        onCancel: (() -> Unit)? = null
    ) {
        MaterialAlertDialogBuilder(requireContext(), R.style.CustomAlertDialog)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setNegativeButton("Cancel") { _, _ ->
                onCancel?.invoke()
            }
            .setOnCancelListener {
                onCancel?.invoke()
            }
            .setPositiveButton("Logout") { _, _ ->
                userViewModel.handleSignOut(requireContext())
                onLogout?.invoke()
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
