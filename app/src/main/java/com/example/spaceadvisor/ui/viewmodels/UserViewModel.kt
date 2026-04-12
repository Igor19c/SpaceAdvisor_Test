package com.example.spaceadvisor.ui.viewmodels

import android.content.Context
import android.content.res.AssetManager
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.spaceadvisor.SpaceAdvisorApplication
import com.example.spaceadvisor.utils.BadgeManager
import com.example.spaceadvisor.domain.models.Post
import com.example.spaceadvisor.domain.models.Review
import com.example.spaceadvisor.domain.models.Trip
import com.example.spaceadvisor.domain.models.User
import com.example.spaceadvisor.domain.repository.IPostRepository
import com.example.spaceadvisor.domain.repository.IReviewRepository
import com.example.spaceadvisor.domain.repository.IUserRepository
import com.firebase.ui.auth.AuthUI
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.InputStream

class UserViewModel(
    private val repository: IUserRepository,
    private val reviewRepository: IReviewRepository,
    private val postRepository: IPostRepository
) : ViewModel() {

    data class LoadingState(val isLoading: Boolean, val message: String? = null)

    private val _userData = MutableLiveData<User?>()
    val userData: LiveData<User?> = _userData

    private val _userReviews = MutableLiveData<List<Review>>()
    val userReviews: LiveData<List<Review>> = _userReviews

    private val _newBadgesEarned = MutableLiveData<List<String>?>()
    val newBadgesEarned: LiveData<List<String>?> = _newBadgesEarned

    private val _uploadProgress = MutableLiveData<Boolean>()
    val uploadProgress: LiveData<Boolean> = _uploadProgress

    private val _loadingState = MutableLiveData<LoadingState>(LoadingState(false))
    val loadingState: LiveData<LoadingState> = _loadingState


    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _passwordResetSent = MutableLiveData<Boolean>()
    val passwordResetSent: LiveData<Boolean> = _passwordResetSent

    private val _avatarPaths = MutableLiveData<List<String>>()
    val avatarPaths: LiveData<List<String>> = _avatarPaths

    private val _isLoggedOut = MutableLiveData<Boolean>(false)
    val isLoggedOut: LiveData<Boolean> = _isLoggedOut

    private val grantedBadgesIds = mutableSetOf<String>()
    private var isUpdatingBadges = false

    fun getCurrentUid(): String? = repository.getCurrentUid()

    fun getCurrentUserName(): String? = repository.getCurrentUserName()

    fun getFirebaseUserProperties(): Pair<String?, String?> = repository.getFirebaseUserProperties()

    fun startListening(uid: String) {
        viewModelScope.launch {
            repository.observeUser(uid).collectLatest { result ->
                result.onSuccess { user ->
                    // Only update if we are not in the middle of an upload/update to prevent flickering
                    if (_uploadProgress.value != true) {
                        _userData.value = user
                    }
                    user?.badges?.let { grantedBadgesIds.addAll(it) }
                }.onFailure { e ->
                    _error.value = e.message
                }
            }
        }
        fetchUserReviews(uid)
    }

    private fun fetchUserReviews(uid: String) {
        viewModelScope.launch {
            reviewRepository.fetchUserReviews(uid).collectLatest { result ->
                result.onSuccess { reviews ->
                    _userReviews.value = reviews
                }.onFailure { e ->
                    _error.value = e.message
                }
            }
        }
    }

    fun checkForBadges(trips: List<Trip>, posts: List<Post>, reviews: List<Review>) {
        if (isUpdatingBadges) return
        val currentUser = _userData.value ?: return
        val uid = getCurrentUid() ?: return

        grantedBadgesIds.addAll(currentUser.badges)
        val newBadgeIds = BadgeManager.checkNewBadges(currentUser, trips, posts, reviews)
            .filter { it !in grantedBadgesIds }

        if (newBadgeIds.isNotEmpty()) {
            isUpdatingBadges = true
            grantedBadgesIds.addAll(newBadgeIds)
            val updatedBadgeList = (currentUser.badges + newBadgeIds).distinct()

            viewModelScope.launch {
                val result = repository.updateUserBadges(uid, updatedBadgeList)
                isUpdatingBadges = false
                result.onSuccess {
                    _newBadgesEarned.value = newBadgeIds
                }.onFailure { e ->
                    _error.value = "Failed to update badges: ${e.message}"
                    grantedBadgesIds.removeAll(newBadgeIds.toSet())
                }
            }
        }
    }

    fun onBadgesDialogShown() {
        _newBadgesEarned.value = null
    }

    fun createNewUserProfile(onComplete: () -> Unit) {

        val uid = getCurrentUid() ?: return
        val (name, email) = getFirebaseUserProperties()
        val newUser = User(
            uid = uid,
            name = name ?: "Traveler",
            email = email ?: "",
            createdAt = System.currentTimeMillis()
        )
        viewModelScope.launch { repository.createUserProfile(newUser); onComplete() }
    }

    fun handleSignOut(context: Context) {
        _loadingState.value = LoadingState(true, "Signing out...")
        val settingsManager =
            (context.applicationContext as SpaceAdvisorApplication).appContainer.settingsManager

        AuthUI.getInstance()
            .signOut(context)
            .addOnCompleteListener {
                settingsManager.resetToDefaults()
                _userData.value = null
                grantedBadgesIds.clear()
                _isLoggedOut.value = true
                _loadingState.value = LoadingState(false)
            }
    }

    fun updateProfile(newName: String, newUsername: String, newBio: String) {
        val uid = getCurrentUid() ?: return

        val currentUser = _userData.value
        if (currentUser != null) {
            _userData.value = currentUser.copy(name = newName, username = newUsername, bio = newBio)
        }

        viewModelScope.launch {
            val result = repository.updateProfile(uid, newName, newUsername, newBio)
            result.onSuccess {
                val currentImage = _userData.value?.profileImageUrl ?: ""
                postRepository.syncUserPosts(uid, newName, currentImage)
            }.onFailure {
                _error.value = it.message
            }
        }
    }

    fun uploadProfileImage(imageUri: Uri, assetManager: AssetManager) {
        val uid = getCurrentUid() ?: return
        _uploadProgress.value = true

        // Optimistic Update: Show the local image immediately as a placeholder
        val currentUser = _userData.value
        if (currentUser != null) {
            _userData.value = currentUser.copy(profileImageUrl = imageUri.toString())
        }

        viewModelScope.launch {
            val uriString = imageUri.toString()
            val isAsset = uriString.contains("android_asset/") || !uriString.contains("://")

            val result = if (isAsset) {
                try {
                    val cleanPath = when {
                        uriString.contains("android_asset/") -> uriString.substringAfter("android_asset/")
                        uriString.startsWith("avatars/") -> uriString
                        else -> "avatars/$uriString"
                    }.trimStart('/')

                    val inputStream: InputStream = assetManager.open(cleanPath)
                    repository.uploadProfileImageStream(uid, inputStream)
                } catch (e: Exception) {
                    repository.uploadProfileImage(uid, imageUri)
                }
            } else {
                repository.uploadProfileImage(uid, imageUri)
            }

            result.onSuccess { imageUrl ->
                val freshName = _userData.value?.name ?: ""
                postRepository.syncUserPosts(uid, freshName, imageUrl)
                _uploadProgress.value = false
            }.onFailure {
                _error.value = it.message
                _uploadProgress.value = false
            }
        }
    }


    fun sendPasswordResetEmail() {
        val email = _userData.value?.email ?: return
        _loadingState.value = LoadingState(true, "Sending reset link...")
        viewModelScope.launch {
            val result = repository.sendPasswordResetEmail(email)
            _loadingState.value = LoadingState(false)
            result.onSuccess {
                _passwordResetSent.value = true
            }.onFailure {
                _error.value = it.message
            }
        }
    }

    fun resetPasswordStatus() {
        _passwordResetSent.value = false
    }

    fun loadAvatars(assetManager: AssetManager) {
        if (_avatarPaths.value != null) return
        viewModelScope.launch {
            val avatars = mutableListOf<String>()
            scanAssets(assetManager, "avatars", avatars)
            _avatarPaths.value = avatars
        }
    }

    private fun scanAssets(
        assetManager: AssetManager,
        dirPath: String,
        result: MutableList<String>
    ) {
        try {
            val list = assetManager.list(dirPath) ?: return
            if (list.isEmpty()) {
                if (dirPath.contains(".")) {
                    result.add(dirPath.substringAfter("avatars/"))
                }
            } else {
                for (name in list) {
                    scanAssets(assetManager, "$dirPath/$name", result)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

}
