package com.example.spaceadvisor.ui.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.spaceadvisor.domain.models.Destination
import com.example.spaceadvisor.domain.models.DestinationDetails
import com.example.spaceadvisor.domain.models.Review
import com.example.spaceadvisor.domain.repository.IDestinationRepository
import com.example.spaceadvisor.domain.repository.IReviewRepository
import com.example.spaceadvisor.ui.fragments.ExploreFragment
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Stack

data class NavigationUiState(
    val progress: Int = 0,
    val planetActive: Boolean = false,
    val locationActive: Boolean = false
)

class DestinationViewModel(
    private val repository: IDestinationRepository,
    private val reviewRepository: IReviewRepository
) : ViewModel() {

    private val _destinations = MutableLiveData<List<Destination>>()
    val destinations: LiveData<List<Destination>> = _destinations

    private val _filteredDestinations = MutableLiveData<List<Destination>>()
    val filteredDestinations: LiveData<List<Destination>> = _filteredDestinations

    private var allDestinationsList: List<Destination> = emptyList()

    private val _subDestinations = MutableLiveData<List<Destination>>()
    val subDestinations: LiveData<List<Destination>> = _subDestinations

    private val _trendingDestinations = MutableLiveData<List<Destination>>()
    val trendingDestinations: LiveData<List<Destination>> = _trendingDestinations

    private val _allReviews = MutableLiveData<List<Review>>()
    val allReviews: LiveData<List<Review>> = _allReviews

    private val _destReviews = MutableLiveData<List<Review>>()
    val destReviews: LiveData<List<Review>> = _destReviews

    private val _savedDestinations = MutableLiveData<List<Destination>>()
    val savedDestinations: LiveData<List<Destination>> = _savedDestinations

    private val _destinationDetails = MutableLiveData<DestinationDetails?>()
    val destinationDetails: LiveData<DestinationDetails?> = _destinationDetails

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _reviewSuccess = MutableLiveData<Boolean>()
    val reviewSuccess: LiveData<Boolean> = _reviewSuccess

    private val _saveStatus = MutableLiveData<Pair<Boolean, String>>()
    val saveStatus: LiveData<Pair<Boolean, String>> = _saveStatus

    private val _navigationUiState = MutableLiveData<NavigationUiState>()
    val navigationUiState: LiveData<NavigationUiState> = _navigationUiState

    // Explore Navigation State preserved in ViewModel
    var exploreNavigationStack = Stack<ExploreFragment.NavigationState>()
    var currentExploreParentId: String = "root"
    var currentExploreParentType: String? = null
    var currentExploreParentTitle: String? = null
    var explorePendingSelectedIndex: Int? = null
    var exploreTargetIdToRestore: String? = null


    fun fetchDestinations(parentId: String, parentType: String? = null) {
        updateNavigationState(parentId, parentType)

        viewModelScope.launch {
            _isLoading.value = true
            repository.fetchDestinationsByParent(parentId).collectLatest { result ->
                _isLoading.value = false
                result.onSuccess { list ->
                    _destinations.value = list
                }.onFailure { e ->
                    _error.value = e.message
                }
            }
        }
    }

    fun filterDestinations(query: String, type: String, difficulties: List<Int>) {
        val filtered = allDestinationsList.filter { dest ->
            val matchesQuery = query.isEmpty() || dest.title.contains(query, ignoreCase = true)
            val matchesType =
                type == "All" || dest.type.trim().equals(type.trim(), ignoreCase = true)
            val matchesDifficulty =
                difficulties.isEmpty() || difficulties.contains(dest.difficulty)

            matchesQuery && matchesType && matchesDifficulty
        }

        _filteredDestinations.value = filtered
    }

    fun fetchSubDestinations(parentId: String) {
        viewModelScope.launch {
            repository.fetchDestinationsByParent(parentId).collectLatest { result ->
                result.onSuccess { list ->
                    _subDestinations.value = list
                }.onFailure { e ->
                    _error.value = e.message
                }
            }
        }
    }

    fun fetchHomeReviews() {
        viewModelScope.launch {
            reviewRepository.fetchAllReviewsOrderedByCreatedAt().collectLatest { result ->
                result.onSuccess { reviews ->
                    _allReviews.value = reviews
                }.onFailure { error -> _error.value = error.message }
            }

        }
    }

    fun fetchDestinationReviews(destId: String) {
        viewModelScope.launch {
            reviewRepository.fetchDestinationReviews(destId).collectLatest { result ->
                result.onSuccess { reviews ->
                    _destReviews.value = reviews
                }.onFailure { error -> _error.value = error.message }
            }
        }
    }

    fun submitReview(review: Review) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = reviewRepository.addReviewAndUpdateRating(review)
            _isLoading.value = false

            result.onSuccess {
                _reviewSuccess.value = true
            }.onFailure { e ->
                _error.value = e.message
            }
        }
    }

    fun resetReviewStatus() {
        _reviewSuccess.value = false
    }

    fun resetSaveStatus() {
        _saveStatus.value = null
    }

    fun fetchDestinationsByRating() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.fetchDestinationsByRating().collectLatest { result ->
                _isLoading.value = false
                result.onSuccess { list ->
                    _trendingDestinations.value = list
                }.onFailure { e ->
                    _error.value = e.message
                }
            }
        }
    }

    fun fetchAllDestinationsForSearch() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.fetchAllDestinations().collectLatest { result ->
                _isLoading.value = false
                result.onSuccess { list ->
                    allDestinationsList = list
                    _filteredDestinations.value = list
                }.onFailure { e ->
                    _error.value = e.message
                }
            }
        }
    }

    private fun updateNavigationState(parentId: String, parentType: String?) {
        val state = when {
            parentId == "root" -> NavigationUiState(15, planetActive = true)
            parentType == "PLANET" -> NavigationUiState(
                50,
                planetActive = true,
                locationActive = true
            )

            else -> NavigationUiState(
                100,
                planetActive = true,
                locationActive = true
            )
        }
        _navigationUiState.value = state
    }

    fun fetchFullDetails(destinationId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.getDestinationDetails(destinationId)
            _isLoading.value = false

            result.onSuccess { details ->
                _destinationDetails.value = details
            }.onFailure { e ->
                _error.value = e.message
            }
        }
    }

    fun fetchSavedDestinations(userId: String) {
        viewModelScope.launch {
            repository.fetchSavedDestinations(userId).collectLatest { result ->
                result.onSuccess { _savedDestinations.value = it }
                result.onFailure { _error.value = it.message }
            }
        }
    }

    fun toggleSaveDestination(userId: String, destination: Destination) {
        viewModelScope.launch {
            val isCurrentlySaved = _savedDestinations.value?.any { it.id == destination.id } == true
            if (isCurrentlySaved) {
                repository.unsaveDestination(userId, destination.id).onSuccess {
                    _saveStatus.postValue(false to "${destination.title} removed from favorites")
                }
            } else {
                repository.saveDestination(userId, destination).onSuccess {
                    _saveStatus.postValue(true to "${destination.title} saved to favorites")
                }
            }
        }
    }

    fun unsaveDestination(userId: String, destinationId: String) {
        viewModelScope.launch {
            repository.unsaveDestination(userId, destinationId)
        }
    }

    fun unsaveAllDestinations(userId: String) {
        viewModelScope.launch {
            _savedDestinations.value?.forEach { destination ->
                repository.unsaveDestination(userId, destination.id)
            }
        }
    }

}
