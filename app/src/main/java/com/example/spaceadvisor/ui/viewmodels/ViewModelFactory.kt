package com.example.spaceadvisor.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.spaceadvisor.SpaceAdvisorApplication

class ViewModelFactory(private val application: SpaceAdvisorApplication) :
    ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val container = application.appContainer

        return when {
            modelClass.isAssignableFrom(TripViewModel::class.java) -> {
                TripViewModel(container.tripRepository, container.destinationRepository) as T
            }

            modelClass.isAssignableFrom(FeedViewModel::class.java) -> {
                FeedViewModel(container.postRepository) as T
            }

            modelClass.isAssignableFrom(UserViewModel::class.java) -> {
                UserViewModel(
                    container.userRepository,
                    container.reviewRepository,
                    container.postRepository
                ) as T
            }

            modelClass.isAssignableFrom(DestinationViewModel::class.java) -> {
                DestinationViewModel(
                    container.destinationRepository,
                    container.reviewRepository
                ) as T
            }

            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}