package com.example.spaceadvisor.ui.viewmodels

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.spaceadvisor.domain.models.Post
import com.example.spaceadvisor.domain.models.User
import com.example.spaceadvisor.domain.repository.IPostRepository
import com.example.spaceadvisor.domain.repository.IUserRepository
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class FeedViewModel(
    private val repository: IPostRepository,
    private val userRepository: IUserRepository
) : ViewModel() {

    private val _posts = MutableLiveData<List<Post>>()
    val posts: LiveData<List<Post>> = _posts

    private val _likedPosts = MutableLiveData<List<Post>>()
    val likedPosts: LiveData<List<Post>> = _likedPosts

    private val _userPosts = MutableLiveData<List<Post>>()
    val userPosts: LiveData<List<Post>> = _userPosts

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    private val _postSaved = MutableLiveData<Boolean>(false)
    val postSaved: LiveData<Boolean> = _postSaved

    private val _selectedImageUri = MutableLiveData<Uri?>(null)
    val selectedImageUri: LiveData<Uri?> = _selectedImageUri

    init {
        fetchPosts()
    }

    fun fetchPosts() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.fetchPosts().collect { result ->
                result.onSuccess { postsList ->
                    // Perform "Join" with fresh user data
                    val updatedPosts = enrichPostsWithUserData(postsList)
                    _posts.value = updatedPosts
                    _isLoading.value = false
                }
                result.onFailure { 
                    _error.value = it.message
                    _isLoading.value = false
                }
            }
        }
    }

    fun fetchUserPosts(uid: String) {
        viewModelScope.launch {
            repository.fetchUserPosts(uid).collectLatest { result ->
                result.onSuccess { postsList ->
                    val updatedPosts = enrichPostsWithUserData(postsList)
                    _userPosts.value = updatedPosts
                }.onFailure { e ->
                    _error.value = e.message
                }
            }
        }
    }

    /**
     * Fetches current user info for all posts in the list and updates them.
     * This ensures names and profile pictures are always up-to-date.
     */
    private suspend fun enrichPostsWithUserData(postsList: List<Post>): List<Post> {
        val uids = postsList.map { it.uid }.distinct()
        val usersResult = userRepository.getUsers(uids)
        
        return if (usersResult.isSuccess) {
            val usersMap = usersResult.getOrNull()?.associateBy { it.uid } ?: emptyMap()
            postsList.map { post ->
                val user = usersMap[post.uid]
                if (user != null) {
                    post.copy(
                        username = user.username.ifEmpty { user.name },
                        userProfileImage = user.profileImageUrl
                    )
                } else {
                    post
                }
            }
        } else {
            postsList // Fallback to original data if fetch fails
        }
    }

    fun setSelectedImageUri(uri: Uri?) {
        _selectedImageUri.value = uri
    }

    fun savePost(
        user: User,
        title: String,
        description: String,
        rating: Int,
        localImageUri: Uri? = null,
        tripImageUrl: String = ""
    ) {
        viewModelScope.launch {
            _error.value = null
            _postSaved.value = false
            _isLoading.value = true

            var finalImageUrl = tripImageUrl

            if (localImageUri != null) {
                val tempId = java.util.UUID.randomUUID().toString()
                val uploadResult = repository.uploadPostImage(tempId, localImageUri)
                uploadResult.onSuccess { url ->
                    finalImageUrl = url
                }.onFailure {
                    _error.value = "Failed to upload image: ${it.message}"
                    _isLoading.value = false
                    return@launch
                }
            }

            val newPost = Post(
                uid = user.uid,
                title = title,
                description = description,
                rating = rating,
                imageUrl = finalImageUrl,
                createdAt = System.currentTimeMillis()
            )

            val result = repository.savePost(newPost)
            _isLoading.value = false

            result.onSuccess {
                _postSaved.value = true
                _selectedImageUri.value = null
            }.onFailure {
                _error.value = it.message
            }
        }
    }

    fun updatePost(
        existingPost: Post,
        newTitle: String,
        newDescription: String,
        newRating: Int,
        localImageUri: Uri? = null
    ) {
        viewModelScope.launch {
            _error.value = null
            _postSaved.value = false
            _isLoading.value = true

            var finalImageUrl = existingPost.imageUrl

            if (localImageUri != null) {
                val uploadResult = repository.uploadPostImage(existingPost.id, localImageUri)
                uploadResult.onSuccess { url ->
                    finalImageUrl = url
                }.onFailure {
                    _error.value = "Failed to upload image: ${it.message}"
                    _isLoading.value = false
                    return@launch
                }
            }

            val updatedPost = existingPost.copy(
                title = newTitle,
                description = newDescription,
                rating = newRating,
                imageUrl = finalImageUrl
            )

            val result = repository.updatePost(updatedPost)
            _isLoading.value = false

            result.onSuccess {
                _postSaved.value = true
                _selectedImageUri.value = null
            }.onFailure {
                _error.value = it.message
            }
        }
    }

    fun toggleLikePost(userId: String, post: Post) {
        viewModelScope.launch {
            val isCurrentlyLiked = post.likedBy.contains(userId)
            if (isCurrentlyLiked) {
                repository.unlikePost(userId, post.id)
            } else {
                repository.likePost(userId, post)
            }
        }
    }

    fun resetPostSavedState() {
        _postSaved.value = false
    }

    fun deletePost(post: Post) {
        viewModelScope.launch {
            repository.deletePost(post.id).onFailure { _error.value = it.message }
        }
    }
}
