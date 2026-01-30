package com.example.spaceadvisor.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.spaceadvisor.data.Destination
import com.example.spaceadvisor.data.Trip
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

class TripViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private var tripsListener: ListenerRegistration? = null

    private val _currentTrip = MutableLiveData(Trip())
    val currentTrip: LiveData<Trip> = _currentTrip

    private val _selectedDestinations = MutableLiveData<MutableList<Destination>>(mutableListOf())
    val selectedDestinations: LiveData<MutableList<Destination>> = _selectedDestinations

    private val _userTrips = MutableLiveData<List<Trip>>()
    val userTrips: LiveData<List<Trip>> = _userTrips

    fun setCurrentTrip(trip: Trip) {
        _currentTrip.value = trip
        fetchTripDestinations(trip)
    }

    fun createNewTrip() {
        _currentTrip.value = Trip()
        _selectedDestinations.value = mutableListOf()
    }

    fun addDestination(destination: Destination) {
        val currentList = _selectedDestinations.value ?: mutableListOf()

        if (currentList.none { it.id == destination.id }) {
            currentList.add(destination)
            _selectedDestinations.value = currentList
            updateTripObject(currentList)
        }
    }

    fun removeDestination(destinationId: String) {
        val currentList = _selectedDestinations.value ?: mutableListOf()
        currentList.removeIf { it.id == destinationId }
        _selectedDestinations.value = currentList
        updateTripObject(currentList)
    }

    fun updateTripObject(destinations: List<Destination>) {
        val trip = _currentTrip.value ?: Trip()
        val updatedTrip = trip.copy(
            destinationIds = destinations.map { it.id },
            destinationNames = destinations.map { it.title },
            destinationImages = destinations.map { it.imageUrl },
            imageUrl = if (destinations.isNotEmpty()) destinations[0].imageUrl else ""
        )
        _currentTrip.value = updatedTrip
    }

    fun updateTripStatus(status: String) {
        _currentTrip.value = _currentTrip.value?.copy(status = status)
    }

    fun saveTrip(title: String, subtitle: String) {
        val uid = auth.currentUser?.uid ?: return
        val trip = _currentTrip.value?.copy(
            uid = uid,
            title = title,
            subtitle = subtitle,
            status = "DRAFT",
            createdAt = System.currentTimeMillis()
        ) ?: return

        db.collection("trips").add(trip).addOnSuccessListener { documentReference ->
            val tripWithId = trip.copy(id = documentReference.id)
            _currentTrip.value = tripWithId
            Log.d("TripViewModel", "Trip created successfully with ID: ${documentReference.id}")
        }.addOnFailureListener { e ->
            Log.e("TripViewModel", "Error creating trip", e)
        }
    }

    fun syncTripWithFirebase() {
        val trip = _currentTrip.value ?: return
        if (trip.id.isEmpty()) {
            Log.e("TripViewModel", "Cannot sync: Trip ID is empty.")
            return
        }

        db.collection("trips").document(trip.id)
            .set(trip)
            .addOnSuccessListener {
                Log.d("TripViewModel", "Trip synced successfully: ${trip.id}")
            }.addOnFailureListener { e ->
                Log.e("TripViewModel", "Error syncing trip", e)
            }
    }

    fun fetchUserTrips(uid: String) {
        if (tripsListener != null) return

        tripsListener = db.collection("trips")
            .whereEqualTo("uid", uid)
            .orderBy(
                "createdAt",
                Query.Direction.DESCENDING
            )
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("TripViewModel", "Listen failed.", e)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val trips = snapshot.documents.mapNotNull { doc ->
                        val trip = doc.toObject(Trip::class.java)
                        trip?.copy(id = doc.id)
                    }
                    _userTrips.value = trips
                    Log.d("TripViewModel", "Real-time update: Received ${trips.size} trips")
                }
            }
    }

    fun fetchTripDestinations(trip: Trip) {
        val destList = trip.destinationIds.mapIndexed { index, destId ->
            Destination(
                id = destId,
                title = trip.destinationNames.getOrElse(index) { "" },
                imageUrl = trip.destinationImages.getOrElse(index) { "" }
            )
        }.toMutableList()
        _selectedDestinations.value = destList
    }

    override fun onCleared() {
        super.onCleared()
        tripsListener?.remove() // Important: Clean up the listener to prevent leaks
    }
}
