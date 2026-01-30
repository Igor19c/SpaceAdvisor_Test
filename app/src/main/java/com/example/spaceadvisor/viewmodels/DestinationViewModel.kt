package com.example.spaceadvisor.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.spaceadvisor.data.DestinationDetails
import com.google.firebase.firestore.FirebaseFirestore

class DestinationViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    
    private val _destinationDetails = MutableLiveData<DestinationDetails?>()
    val destinationDetails: LiveData<DestinationDetails?> = _destinationDetails

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun fetchFullDetails(destinationId: String) {
        _isLoading.value = true
        db.collection("destinations")
            .document(destinationId)
            .collection("details")
            .document("main")
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    _destinationDetails.value = document.toObject(DestinationDetails::class.java)
                }
                _isLoading.value = false
            }
            .addOnFailureListener {
                _isLoading.value = false
            }
    }
}
