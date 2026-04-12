package com.example.spaceadvisor.ui.fragments

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.example.spaceadvisor.R
import com.example.spaceadvisor.SpaceAdvisorApplication
import com.example.spaceadvisor.databinding.DialogChooseImageSourceBinding
import com.example.spaceadvisor.databinding.FragmentProfileEditBinding
import com.example.spaceadvisor.domain.models.UIConfig
import com.example.spaceadvisor.ui.adapters.ImagePickerAdapter
import com.example.spaceadvisor.ui.viewmodels.UserViewModel
import com.example.spaceadvisor.ui.viewmodels.ViewModelFactory
import com.google.android.material.bottomsheet.BottomSheetDialog

class ProfileEditFragment : BaseFragment() {

    private var _binding: FragmentProfileEditBinding? = null
    private val binding get() = _binding!!

    private val userViewModel: UserViewModel by activityViewModels {
        ViewModelFactory(requireActivity().application as SpaceAdvisorApplication)
    }

    private var selectedImageUri: Uri? = null

    private val pickMedia =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                updatePreviewImage(uri)
            }
        }

    override fun getUIConfig(): UIConfig {
        return UIConfig(
            title = "Edit Profile",
            isHeaderVisible = true,
            isLeftBtnVisible = true,
            leftIconRes = R.drawable.ic_back,
            onLeftClick = { parentFragmentManager.popBackStack() },
            isRightBtnVisible = false,
            isBottomNavVisible = false,
            selectedTabId = R.id.nav_profile
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileEditBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val currentUser = userViewModel.userData.value

        if (currentUser != null) {
            binding.editTextNameEditProfileFragment.setText(currentUser.name)
            binding.editTextUsernameEditProfileFragment.setText(currentUser.username)
            binding.editTextBioEditProfileFragment.setText(currentUser.bio)
            currentUser.profileImageUrl?.let {
                Glide.with(this).load(it).circleCrop().into(binding.profilePicEditProfileFragment)
            }
        } else {
            val (firebaseName, _) = userViewModel.getFirebaseUserProperties()
            binding.editTextNameEditProfileFragment.setText(firebaseName ?: "Traveler")
        }

        binding.profilePicEditProfileFragment.setOnClickListener { showAvatarPicker() }
        binding.btnUpdatePicEditProfileFragment.setOnClickListener { showAvatarPicker() }

        binding.saveBtnEditProfileFragment.setOnClickListener {
            val isFirstTime = arguments?.getBoolean(ARG_IS_FIRST_TIME, false) ?: false

            userViewModel.updateProfile(
                binding.editTextNameEditProfileFragment.text.toString(),
                binding.editTextUsernameEditProfileFragment.text.toString(),
                binding.editTextBioEditProfileFragment.text.toString()
            )
            if (!isFirstTime) {
                showCustomMessage("Profile Updated!", "Your profile has been updated.")
            }
            selectedImageUri?.let { userViewModel.uploadProfileImage(it, requireContext().assets) }
            parentFragmentManager.popBackStack()
        }

        binding.cancelBtnEditProfileFragment.setOnClickListener { parentFragmentManager.popBackStack() }

        if (arguments?.getBoolean(ARG_IS_FIRST_TIME, false) == true) {
            showCustomMessage(
                title = "Welcome, Space Traveler!",
                body = "Your account is ready. Let's start by personalizing your profile!",
                duration = 5000
            )
            arguments?.putBoolean(ARG_IS_FIRST_TIME, false)
        }
    }

    private fun showAvatarPicker() {
        val pickerDialog = BottomSheetDialog(requireContext())
        val pickerBinding = DialogChooseImageSourceBinding.inflate(layoutInflater)
        pickerDialog.setContentView(pickerBinding.root)
        userViewModel.loadAvatars(requireContext().assets)
        userViewModel.avatarPaths.observe(viewLifecycleOwner) { avatarFiles ->
            val fullPaths = avatarFiles.map { "file:///android_asset/avatars/$it" }
            val adapter = ImagePickerAdapter(
                images = fullPaths,
                onPlusClick = {
                    openGallery()
                    pickerDialog.dismiss()
                },
                onImageSelected = { fullPath ->
                    updatePreviewImage(Uri.parse(fullPath))
                    pickerDialog.dismiss()
                })

            pickerBinding.itemRecyclerView.layoutManager =
                GridLayoutManager(requireContext(), 3)
            pickerBinding.itemRecyclerView.adapter = adapter
        }
        pickerDialog.show()
    }

    private fun openGallery() {
        pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    private fun updatePreviewImage(uri: Uri) {
        selectedImageUri = uri
        Glide.with(this).load(uri).circleCrop().into(binding.profilePicEditProfileFragment)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_IS_FIRST_TIME = "is_first_time"

        fun newInstance(isFirstTime: Boolean = false): ProfileEditFragment {
            val fragment = ProfileEditFragment()
            val args = Bundle()
            args.putBoolean(ARG_IS_FIRST_TIME, isFirstTime)
            fragment.arguments = args
            return fragment
        }
    }
}