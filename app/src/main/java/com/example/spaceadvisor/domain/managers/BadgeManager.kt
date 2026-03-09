package com.example.spaceadvisor.domain.managers

import com.example.spaceadvisor.domain.models.Post
import com.example.spaceadvisor.domain.models.Trip
import com.example.spaceadvisor.domain.models.User

object BadgeManager {

    fun checkNewBadges(user: User, trips: List<Trip>, posts: List<Post>): List<String> {
        val currentBadgeIds = user.badges.toSet()
        val newBadges = mutableListOf<String>()

        // 1. First Trip Badge
        if (!currentBadgeIds.contains("first_trip") && trips.isNotEmpty()) {
            newBadges.add("first_trip")
        }

        // 2. Explorer Badge (Visited 3 or more unique destinations)
        val allDestinationIds = trips.flatMap { it.destinationIds }.toSet()
        if (!currentBadgeIds.contains("explorer") && allDestinationIds.size >= 3) {
            newBadges.add("explorer")
        }

        // 3. Top Reviewer Badge (Created 5 or more posts)
        if (!currentBadgeIds.contains("top_reviewer") && posts.size >= 5) {
            newBadges.add("top_reviewer")
        }

        // Note: moon_walker logic can be added here if we have planet information in Trip or Destination

        return newBadges
    }
}