package com.example.spaceadvisor.ui.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.spaceadvisor.domain.models.Destination
import com.example.spaceadvisor.domain.models.Trip
import com.example.spaceadvisor.domain.repository.IDestinationRepository
import com.example.spaceadvisor.domain.repository.ITripRepository
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class TripViewModel(
    private val repository: ITripRepository,
    private val destinationRepository: IDestinationRepository
) : ViewModel() {

    private val _currentTrip = MutableLiveData(Trip())
    val currentTrip: LiveData<Trip> = _currentTrip

    private val _selectedDestinations = MutableLiveData<MutableList<Destination>>(mutableListOf())
    val selectedDestinations: LiveData<MutableList<Destination>> = _selectedDestinations

    private val _userTrips = MutableLiveData<List<Trip>>()
    val userTrips: LiveData<List<Trip>> = _userTrips

    private val _isEditMode = MutableLiveData<Boolean>(false)
    val isEditMode: LiveData<Boolean> = _isEditMode

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun setEditMode(isEditing: Boolean) {
        _isEditMode.value = isEditing

        if (isEditing) {
            val trip = _currentTrip.value
            if (trip?.status == "PLANNED") {
                _currentTrip.value = trip.copy(status = "DRAFT")
                syncTripWithFirebase()
            }
        }
    }

    fun setCurrentTrip(trip: Trip) {
        if (trip.status != "COMPLETED")
            _currentTrip.value = trip
        fetchTripDestinations(trip)
    }

    fun createNewTrip() {
        _currentTrip.value = Trip()
        _selectedDestinations.value = mutableListOf()
        setEditMode(true)
    }

    fun addDestination(destination: Destination) {
        val currentList = _selectedDestinations.value ?: mutableListOf()

        if (currentList.none { it.id == destination.id }) {
            currentList.add(destination)
            _selectedDestinations.value = currentList
            updateTripObject(currentList)
            syncTripWithFirebase()
        }
    }

    fun removeDestination(destinationId: String) {
        val currentList = _selectedDestinations.value ?: mutableListOf()
        currentList.removeIf { it.id == destinationId }
        _selectedDestinations.value = currentList
        updateTripObject(currentList)
        syncTripWithFirebase()
    }

    fun updateTripDetails(title: String, imageUrl: String? = null) {
        val trip = _currentTrip.value ?: return

        val newStatus = if (trip.status == "PLANNED") "DRAFT" else trip.status

        _currentTrip.value = trip.copy(
            title = title,
            status = newStatus,
            imageUrl = imageUrl ?: trip.imageUrl
        )
    }

    private fun updateTripObject(destinations: List<Destination>) {
        val trip = _currentTrip.value ?: Trip()

        val oldFirstId = if (trip.destinationIds.isNotEmpty()) trip.destinationIds[0] else null
        val newFirstId = if (destinations.isNotEmpty()) destinations[0].id else null

        val updatedTrip = trip.copy(
            destinationIds = destinations.map { it.id },
            destinationImages = destinations.map { it.imageUrl }.filter { it.isNotEmpty() }
        )
        when {
            newFirstId == null -> {
                _currentTrip.value = updatedTrip.copy(imageUrl = "")
            }

            newFirstId != oldFirstId || updatedTrip.imageUrl.startsWith("avatars/") || updatedTrip.imageUrl.isEmpty() -> {
                val firstDest = destinations[0]

                val tripWithInitialImage = updatedTrip.copy(imageUrl = firstDest.imageUrl)
                _currentTrip.value = tripWithInitialImage

                if (firstDest.parentId != "root") {
                    viewModelScope.launch {
                        try {
                            val parentResult =
                                destinationRepository.fetchDestinationsByIds(listOf(firstDest.parentId))
                                    .first()
                            parentResult.onSuccess { parents ->
                                if (parents.isNotEmpty()) {
                                    val current = _currentTrip.value ?: return@onSuccess
                                    if (current.destinationIds.isNotEmpty() && current.destinationIds[0] == newFirstId) {
                                        _currentTrip.value =
                                            current.copy(imageUrl = parents[0].imageUrl)
                                        syncTripWithFirebase()
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("TripViewModel", "Error refining trip image", e)
                        }
                    }
                }
            }

            else -> {
                _currentTrip.value = updatedTrip
            }
        }
    }

    fun finalizeTrip() {
        _currentTrip.value = _currentTrip.value?.copy(status = "PLANNED")
        setEditMode(false)
        syncTripWithFirebase()
    }

    fun saveTrip(
        uid: String,
        title: String,
        subtitle: String,
        startDate: Long?,
        endDate: Long?
    ) {
        val trip = _currentTrip.value?.copy(
            uid = uid,
            title = title,
            subtitle = subtitle,
            startDate = startDate,
            endDate = endDate,
            status = "DRAFT",
            createdAt = System.currentTimeMillis()
        ) ?: return

        _currentTrip.value = trip

        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.saveTrip(trip)
            _isLoading.value = false

            result.onSuccess { newId ->
                val tripWithId = trip.copy(id = newId)
                _currentTrip.value = tripWithId
                Log.d("TripViewModel", "Trip created successfully with ID: $newId")
            }.onFailure { e ->
                _error.value = e.message
                _currentTrip.value = trip.copy(status = "")
                Log.e("TripViewModel", "Error creating trip", e)
            }
        }
    }

    fun syncTripWithFirebase() {
        val trip = _currentTrip.value ?: return
        if (trip.id.isEmpty()) return

        viewModelScope.launch {
            val result = repository.updateTrip(trip)
            result.onFailure { e ->
                _error.value = e.message
            }
        }
    }

    fun deleteTrip(tripId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.deleteTrip(tripId)
            _isLoading.value = false

            result.onSuccess {

                if (_currentTrip.value?.id == tripId) {
                    createNewTrip()
                    _isEditMode.value = false
                }
            }.onFailure { e ->
                _error.value = e.message
            }
        }
    }

    fun fetchUserTrips(uid: String) {
        viewModelScope.launch {
            repository.fetchUserTrips(uid).collectLatest { result ->
                result.onSuccess { trips ->
                    _userTrips.value = trips
                }.onFailure { e ->
                    _error.value = e.message
                }
            }
        }
    }

    fun fetchTripDestinations(trip: Trip) {
        if (trip.destinationIds.isEmpty()) {
            _selectedDestinations.value = mutableListOf()
            return
        }

        viewModelScope.launch {
            destinationRepository.fetchDestinationsByIds(trip.destinationIds)
                .collectLatest { result ->
                    result.onSuccess { destinations ->
                        _selectedDestinations.value = destinations.toMutableList()
                    }.onFailure { e ->
                        _error.value = e.message
                        val basicList = trip.destinationIds.mapIndexed { index, destId ->
                            Destination(
                                id = destId
                            )
                        }.toMutableList()
                        _selectedDestinations.value = basicList
                    }
                }
        }
    }
}
