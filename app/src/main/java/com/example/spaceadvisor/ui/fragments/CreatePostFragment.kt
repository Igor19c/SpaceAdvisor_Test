package com.example.spaceadvisor.ui.fragments

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.example.spaceadvisor.R
import com.example.spaceadvisor.SpaceAdvisorApplication
import com.example.spaceadvisor.databinding.DialogChooseImageSourceBinding
import com.example.spaceadvisor.databinding.FragmentCreatePostBinding
import com.example.spaceadvisor.domain.models.Post
import com.example.spaceadvisor.domain.models.Trip
import com.example.spaceadvisor.ui.UIConfig
import com.example.spaceadvisor.ui.adapters.ImagePickerAdapter
import com.example.spaceadvisor.ui.viewmodels.FeedViewModel
import com.example.spaceadvisor.ui.viewmodels.UserViewModel
import com.example.spaceadvisor.ui.viewmodels.ViewModelFactory
import com.google.android.material.bottomsheet.BottomSheetDialog

class CreatePostFragment : BaseFragment() {

    private var _binding: FragmentCreatePostBinding? = null
    private val binding get() = _binding!!

    private val feedViewModel: FeedViewModel by activityViewModels {
        ViewModelFactory(requireActivity().application as SpaceAdvisorApplication)
    }
    private val userViewModel: UserViewModel by activityViewModels {
        ViewModelFactory(requireActivity().application as SpaceAdvisorApplication)
    }

    private var selectedTrip: Trip? = null
    private var editingPost: Post? = null

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { updatePreviewImage(it) }
    }

    override fun getUIConfig() = UIConfig(
        title = if (editingPost != null) "Edit Post" else "Create Post",
        isBottomNavVisible = false,
        selectedTabId = R.id.nav_feed
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreatePostBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        selectedTrip = arguments?.getSerializable("selectedTrip") as? Trip
        editingPost = arguments?.getSerializable("editingPost") as? Post

        setupInitialUI()

        binding.updatePicBtn.setOnClickListener {
            showImagePicker()
        }

        binding.saveBtn.setOnClickListener {
            val title = binding.editPostTitle.text.toString()
            val description = binding.editPostDescription.text.toString()
            val rating = binding.postRatingBar.rating.toInt()

            if (title.isBlank()) {
                binding.editPostTitle.error = "Title is required"
                return@setOnClickListener
            }

            if (rating == 0) {
                Toast.makeText(requireContext(), "Please provide a rating", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val user = userViewModel.userData.value
            if (user != null) {
                if (editingPost != null) {
                    feedViewModel.updatePost(
                        existingPost = editingPost!!,
                        newTitle = title,
                        newDescription = description,
                        newRating = rating,
                        localImageUri = feedViewModel.selectedImageUri.value
                    )
                } else {
                    feedViewModel.savePost(
                        user = user,
                        title = title,
                        description = description,
                        rating = rating,
                        localImageUri = feedViewModel.selectedImageUri.value,
                        tripImageUrl = selectedTrip?.imageUrl ?: ""
                    )
                }
                parentFragmentManager.popBackStack()
            }
        }

        binding.cancelBtn.setOnClickListener { parentFragmentManager.popBackStack() }
    }

    private fun setupInitialUI() {
        if (editingPost != null) {
            binding.editPostTitle.setText(editingPost!!.title)
            binding.editPostDescription.setText(editingPost!!.description)
            binding.postRatingBar.rating = editingPost!!.rating.toFloat()
            if (editingPost!!.imageUrl.isNotEmpty()) {
                Glide.with(this).load(editingPost!!.imageUrl).into(binding.editUserPic)
            }
        } else if (selectedTrip != null) {
            binding.editPostTitle.setText(selectedTrip!!.title)
            
            val isSharingTrip = arguments?.getBoolean("isSharingTrip", false) ?: false
            if (isSharingTrip) {
                binding.editPostDescription.setText("Check out my journey with ${selectedTrip!!.destinationIds.size} stops!")
            }
            
            if (selectedTrip!!.imageUrl.isNotEmpty()) {
                Glide.with(this).load(selectedTrip!!.imageUrl).into(binding.editUserPic)
            }
        }
    }

    private fun showImagePicker() {
        val pickerDialog = BottomSheetDialog(requireContext())
        val pickerBinding = DialogChooseImageSourceBinding.inflate(layoutInflater)
        pickerDialog.setContentView(pickerBinding.root)

        val tripImages = mutableListOf<String>()
        selectedTrip?.let { trip ->
            if (trip.imageUrl.isNotEmpty()) tripImages.add(trip.imageUrl)
            tripImages.addAll(trip.destinationImages.filter { it.isNotEmpty() })
        }
        
        editingPost?.let { post ->
            if (post.imageUrl.isNotEmpty() && !tripImages.contains(post.imageUrl)) {
                tripImages.add(post.imageUrl)
            }
        }

        val adapter = ImagePickerAdapter(images = tripImages, onPlusClick = {
            pickImageLauncher.launch("image/*")
            pickerDialog.dismiss()
        }, onImageSelected = { imageUrl ->
            updatePreviewImage(Uri.parse(imageUrl))
            pickerDialog.dismiss()
        })

        pickerBinding.itemRecyclerView.layoutManager = GridLayoutManager(requireContext(), 3)
        pickerBinding.itemRecyclerView.adapter = adapter
        pickerDialog.show()
    }

    private fun updatePreviewImage(uri: Uri) {
        if (uri.toString().startsWith("http")) {
            feedViewModel.setSelectedImageUri(null)
        } else {
            feedViewModel.setSelectedImageUri(uri)
        }
        Glide.with(this).load(uri).into(binding.editUserPic)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}