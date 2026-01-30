package com.example.spaceadvisor.data

import java.io.Serializable

data class Trip(
    val id: String = "",             // (Document ID in Firestore)
    val uid: String = "",            // User ID
    val title: String = "",          // Trip Name
    val subtitle: String = "",       // Trip Description
    val imageUrl: String = "",       // Trip Image
    val tags: List<String> = emptyList(), // Trip Tags
    val destinationIds: List<String> = emptyList(), // List of Destination IDs
    val destinationNames: List<String> = emptyList(), // List of Destination Titles
    val destinationImages: List<String> = emptyList(), // List of Destination Images
    val status: String = "",         // (PENDING, IN_PROGRESS, COMPLETED)
    val startDate: Long? = null,     // Start Date
    val endDate: Long? = null,       // End Date
    val createdAt: Long? = null,

    ) : Serializable
