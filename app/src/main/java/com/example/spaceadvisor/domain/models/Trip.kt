package com.example.spaceadvisor.domain.models

import java.io.Serializable

data class Trip(
    val id: String = "",
    val uid: String = "",
    val title: String = "",
    val subtitle: String = "",
    val imageUrl: String = "",
    val tags: List<String> = emptyList(),
    val destinationIds: List<String> = emptyList(),
    val destinationImages: List<String> = emptyList(),
    val status: String = "",
    val startDate: Long? = null,
    val endDate: Long? = null,
    val createdAt: Long? = null,
) : Serializable {

    val isDraft: Boolean get() = status == "DRAFT" || destinationIds.isEmpty()
    val isPlanned: Boolean get() = status == "PLANNED"
    
    val isPastTrip: Boolean get() {
        val start = startDate ?: return false
        return start < System.currentTimeMillis()
    }

    val isCompleted: Boolean get() {
        return isPlanned && destinationIds.isNotEmpty() && isPastTrip
    }

    val canBePublished: Boolean get() {
        return destinationIds.isNotEmpty() && !isPastTrip
    }
}
