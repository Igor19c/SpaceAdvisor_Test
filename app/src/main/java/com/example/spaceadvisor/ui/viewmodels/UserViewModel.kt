package com.example.spaceadvisor.ui.viewmodels

import android.content.res.AssetManager
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.spaceadvisor.domain.managers.BadgeManager
import com.example.spaceadvisor.domain.models.Post
import com.example.spaceadvisor.domain.models.Review
import com.example.spaceadvisor.domain.models.Trip
import com.example.spaceadvisor.domain.models.User
import com.example.spaceadvisor.domain.repository.IReviewRepository
import com.example.spaceadvisor.domain.repository.IUserRepository
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.InputStream

class UserViewModel(
    private val repository: IUserRepository,
    private val reviewRepository: IReviewRepository
) : ViewModel() {

    private val _userData = MutableLiveData<User?>()
    val userData: LiveData<User?> = _userData

    private val _userReviews = MutableLiveData<List<Review>>()
    val userReviews: LiveData<List<Review>> = _userReviews

    private val _newBadgesEarned = MutableLiveData<List<String>?>()
    val newBadgesEarned: LiveData<List<String>?> = _newBadgesEarned

    private val _uploadProgress = MutableLiveData<Boolean>()
    val uploadProgress: LiveData<Boolean> = _uploadProgress

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _avatarPaths = MutableLiveData<List<String>>()
    val avatarPaths: LiveData<List<String>> = _avatarPaths

    private val grantedBadgesIds = mutableSetOf<String>()
    private var isUpdatingBadges = false

    fun getCurrentUid(): String? = repository.getCurrentUid()

    fun getCurrentUserName(): String? = repository.getCurrentUserName()


    fun getFirebaseUserProperties(): Pair<String?, String?> = repository.getFirebaseUserProperties()

    fun startListening(uid: String) {
        viewModelScope.launch {
            repository.observeUser(uid).collectLatest { result ->
                result.onSuccess { user ->
                    _userData.value = user
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

    fun checkForBadges(trips: List<Trip>, posts: List<Post>) {
        if (isUpdatingBadges) return
        val currentUser = _userData.value ?: return
        val uid = getCurrentUid() ?: return

        grantedBadgesIds.addAll(currentUser.badges)
        val newBadgeIds = BadgeManager.checkNewBadges(currentUser, trips, posts)
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

    fun updateProfile(newName: String, newUsername: String, newBio: String) {
        val uid = getCurrentUid() ?: return
        val currentUser = _userData.value
        if (currentUser != null) {
            _userData.value = currentUser.copy(name = newName, username = newUsername, bio = newBio)
        }
        viewModelScope.launch {
            repository.updateProfile(uid, newName, newUsername, newBio)
                .onFailure { _error.value = it.message }
        }
    }

    fun uploadProfileImage(imageUri: Uri, assetManager: AssetManager) {
        val uid = getCurrentUid() ?: return
        _uploadProgress.value = true
        val currentUser = _userData.value
        if (currentUser != null) {
            _userData.value = currentUser.copy(profileImageUrl = imageUri.toString())
        }
        viewModelScope.launch {
            val uriString = imageUri.toString()
            val result =
                if (!uriString.contains("://") || uriString.startsWith("file:///android_asset/")) {
                    try {
                        val cleanPath =
                            if (uriString.startsWith("file:///android_asset/")) uriString.substringAfter(
                                "android_asset/"
                            ) else (if (uriString.startsWith("avatars/")) uriString else "avatars/$uriString")
                        val inputStream: InputStream = assetManager.open(cleanPath)
                        repository.uploadProfileImageStream(uid, inputStream)
                    } catch (e: Exception) {
                        repository.uploadProfileImage(uid, imageUri)
                    }
                } else {
                    repository.uploadProfileImage(uid, imageUri)
                }
            _uploadProgress.value = false
            result.onFailure { _error.value = it.message }
        }


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

    fun signOut() {
        repository.signOut(); _userData.value = null; grantedBadgesIds.clear()
    }
}