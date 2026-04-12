package com.example.spaceadvisor.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.transition.TransitionManager
import com.example.spaceadvisor.R
import com.example.spaceadvisor.SpaceAdvisorApplication
import com.example.spaceadvisor.databinding.FragmentSettingsBinding
import com.example.spaceadvisor.domain.models.UIConfig
import com.example.spaceadvisor.ui.activities.LoadingType
import com.example.spaceadvisor.ui.dialogs.SelectDialogAccentColor
import com.example.spaceadvisor.ui.viewmodels.UserViewModel
import com.example.spaceadvisor.ui.viewmodels.ViewModelFactory
import com.example.spaceadvisor.utils.SettingsManager
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

        updateAccentColorText()

        binding.darkModeSwitchSettingsFragment.setOnCheckedChangeListener { _, isChecked ->
            showLoading("Applying theme...", LoadingType.LOTTIE)

            binding.root.postDelayed({
                TransitionManager.beginDelayedTransition(binding.darkModeContainerSettingsFragment)
                updateDarkModeIcon(isChecked)
                settingsManager.isDarkMode = isChecked
            }, 400)
        }

        binding.btnAccentColorSettingsFragment.setOnClickListener {
            showColorPickerDialog()
        }

        userViewModel.loadingState.observe(viewLifecycleOwner) { state ->
            if (state.isLoading) {
                showLoading(state.message, LoadingType.LOTTIE)
            } else {
                hideLoading()
            }
        }

        userViewModel.passwordResetSent.observe(viewLifecycleOwner) { sent ->
            if (sent) {
                showCustomMessage(
                    "Email Sent!",
                    "A reset link has been sent to your email address.",
                    5000
                )
                userViewModel.resetPasswordStatus()
            }
        }

        userViewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let { showError(it) }
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
                ProfileEditFragment.newInstance(
                    isFirstTime = false
                )
            )
        }

        binding.btnChangePassSettingsFragment.setOnClickListener { showChangePasswordConfirmation() }

        binding.btnFaqSettingsFragment.setOnClickListener { navigateTo(FAQFragment()) }

        binding.btnAboutSettingsFragment.setOnClickListener { navigateTo(AboutFragment()) }
    }

    private fun showChangePasswordConfirmation() {
        val email = userViewModel.userData.value?.email ?: "your email"

        MaterialAlertDialogBuilder(requireContext(), R.style.CustomAlertDialog)
            .setTitle("Reset Password")
            .setMessage("We will send a password reset link to:\n$email\n\nYou will remain logged in to Astryx during this process.")
            .setPositiveButton("Send Email") { _, _ ->
                settingsManager.needsReAuthAfterPasswordReset = true
                userViewModel.sendPasswordResetEmail()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateDarkModeIcon(isDark: Boolean) {
        val iconRes = if (isDark) R.drawable.ic_moon else R.drawable.ic_sun
        binding.btnDarkModeSetingsFragment.setIconResource(iconRes)
        binding.darkModeSwitchSettingsFragment.thumbIconDrawable =
            ContextCompat.getDrawable(requireContext(), iconRes)
    }

    private fun updateAccentColorText() {
        val currentThemeId = settingsManager.selectedThemeResId
        val themeName = when (currentThemeId) {
            R.style.Theme_SpaceAdvisor -> getString(R.string.ac_indigo)
            R.style.Theme_SpaceAdvisor_Blue -> getString(R.string.ac_blue)
            R.style.Theme_SpaceAdvisor_Red -> getString(R.string.ac_crimson)
            R.style.Theme_SpaceAdvisor_Green -> getString(R.string.ac_green)
            else -> "Space Color"
        }
        binding.accentColorTextTintSettingsFragment.text = themeName
    }

    private fun showColorPickerDialog() {
        val dialog = SelectDialogAccentColor.newInstance()

        dialog.setOnColorSelectedListener { selectedTheme ->
            settingsManager.selectedThemeResId = selectedTheme
            binding.root.postDelayed({
                requireActivity().recreate()
            }, 600)
        }

        dialog.show(parentFragmentManager, SelectDialogAccentColor.TAG)
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
