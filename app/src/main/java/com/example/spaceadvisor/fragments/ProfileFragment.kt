package com.example.spaceadvisor.fragments

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.spaceadvisor.R
import com.example.spaceadvisor.adapters.AvatarAdapter
import com.example.spaceadvisor.adapters.BadgeAdapter
import com.example.spaceadvisor.adapters.MyTripsAdapter
import com.example.spaceadvisor.databinding.DialogEditProfileBinding
import com.example.spaceadvisor.databinding.DialogChooseImageSourceBinding
import com.example.spaceadvisor.databinding.FragmentProfileBinding
import com.example.spaceadvisor.viewmodels.TripViewModel
import com.example.spaceadvisor.viewmodels.UserViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var userViewModel: UserViewModel
    private lateinit var tripViewModel: TripViewModel
    private var selectedImageUri: Uri? = null
    private var currentDialogBinding: DialogEditProfileBinding? = null
    private lateinit var myTripsAdapter: MyTripsAdapter
    private lateinit var badgesAdapter: BadgeAdapter

    private val pickMedia =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                updatePreviewImage(uri)
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        userViewModel = ViewModelProvider(requireActivity())[UserViewModel::class.java]
        tripViewModel = ViewModelProvider(requireActivity())[TripViewModel::class.java]

        setupBadgesRecyclerView()
        setupTripsRecyclerView()
        observeViewModel()

        userViewModel.getCurrentUid()?.let { uid ->
            tripViewModel.fetchUserTrips(uid)
        }

        binding.signoutBtn.setOnClickListener {
            userViewModel.signOut()
        }

        binding.editProfBtn.setOnClickListener { showEditProfileDialog() }
    }

    private fun observeViewModel() {
        userViewModel.userData.observe(viewLifecycleOwner) { user ->
            user?.let {
                binding.userName.text = it.name
                binding.userNickname.text = it.username
                binding.userBio.text = it.bio

                badgesAdapter.updateBadges(it.badges)

                if (!it.profileImageUrl.isNullOrEmpty()) {
                    Glide.with(this).load(it.profileImageUrl).circleCrop().into(binding.userPic)
                }
            }
        }

        // Observe trips and show only the most recent ones (e.g., last 2)
        tripViewModel.userTrips.observe(viewLifecycleOwner) { trips ->
            val recentTrips = trips.take(2)
            myTripsAdapter.updateData(recentTrips)

            // Show/Hide section based on data availability
            binding.myTripsRecyclerView.visibility =
                if (recentTrips.isEmpty()) View.GONE else View.VISIBLE
        }
    }

    private fun setupTripsRecyclerView() {
        myTripsAdapter = MyTripsAdapter(mutableListOf()) { trip ->
            tripViewModel.setCurrentTrip(trip)
            navigateToMyTrip()
        }
        binding.myTripsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.myTripsRecyclerView.adapter = myTripsAdapter
    }

    private fun navigateToMyTrip() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.main_frame, TripEditorFragment())
            .addToBackStack(null)
            .commit()

        activity?.findViewById<TextView>(R.id.header_title)?.text = "My Trip"
    }

    private fun setupBadgesRecyclerView() {
        badgesAdapter = BadgeAdapter(emptyList())
        binding.badgeRecyclerView.apply {
            layoutManager = GridLayoutManager(requireContext(), 3)
            isNestedScrollingEnabled = false
            adapter = badgesAdapter
        }
    }

    private fun showEditProfileDialog() {
        val dialog = BottomSheetDialog(requireContext())
        val dialogBinding = DialogEditProfileBinding.inflate(layoutInflater)
        currentDialogBinding = dialogBinding
        dialog.setContentView(dialogBinding.root)

        val currentUser = userViewModel.userData.value
        dialogBinding.editNameEt.setText(currentUser?.name)
        dialogBinding.editUsernameEt.setText(currentUser?.username)
        dialogBinding.editBioEt.setText(currentUser?.bio)

        currentUser?.profileImageUrl?.let {
            Glide.with(this).load(it).circleCrop().into(dialogBinding.editUserPic)
        }

        dialogBinding.updatePicBtn.setOnClickListener {
            showAvatarPicker()
        }

        dialogBinding.saveBtn.setOnClickListener {
            userViewModel.updateProfile(
                dialogBinding.editNameEt.text.toString(),
                dialogBinding.editUsernameEt.text.toString(),
                dialogBinding.editBioEt.text.toString()
            )

            selectedImageUri?.let { uri -> userViewModel.uploadProfileImage(uri) }
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun getAllAvatarPaths(dirPath: String): List<String> {
        val result = mutableListOf<String>()
        val assets = context?.assets ?: return result

        try {
            val list = assets.list(dirPath) ?: return result
            if (list.isEmpty()) {
                if (dirPath.contains(".")) {
                    result.add(dirPath.substringAfter("avatars/"))
                }
            } else {
                for (name in list) {
                    result.addAll(getAllAvatarPaths("$dirPath/$name"))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result
    }

    private fun showAvatarPicker() {
        val pickerDialog = BottomSheetDialog(requireContext())
        val pickerBinding = DialogChooseImageSourceBinding.inflate(layoutInflater)
        pickerDialog.setContentView(pickerBinding.root)

        val avatarFiles = getAllAvatarPaths("avatars")

        val adapter = AvatarAdapter(avatars = avatarFiles, onPlusClick = {
            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            pickerDialog.dismiss()
        }, onAvatarClick = { assetPath ->
            updatePreviewImage(Uri.parse(assetPath))
            pickerDialog.dismiss()
        })

        pickerBinding.avatarRecyclerView.layoutManager = GridLayoutManager(requireContext(), 3)
        pickerBinding.avatarRecyclerView.adapter = adapter

        pickerDialog.show()
    }

    private fun updatePreviewImage(uri: Uri) {
        selectedImageUri = uri
        currentDialogBinding?.let {
            Glide.with(this).load(uri).circleCrop().into(it.editUserPic)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
