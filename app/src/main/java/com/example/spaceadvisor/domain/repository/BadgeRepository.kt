package com.example.spaceadvisor.domain.repository

import com.example.spaceadvisor.domain.models.Badge

object BadgeRepository {
    private val allBadges = listOf(
        Badge(
            id = "first_trip",
            name = "First Trip",
            description = "Completed your first space journey!",
            assetPath = "badges/first_trip.webp"
        ),
        Badge(
            id = "explorer",
            name = "Explorer",
            description = "Visited 5 different planets",
            assetPath = "badges/explorer.webp"
        ),
        Badge(
            id = "moon_walker",
            name = "Moon Walker",
            description = "Set foot on the Moon",
            assetPath = "badges/moon_walker.webp"
        ),
        Badge(
            id = "first_review",
            name = "First Review",
            description = "Wrote 1 helpful review",
            assetPath = "badges/first_review.webp"
        ),
        Badge(
            id = "top_reviewer",
            name = "Top Reviewer",
            description = "Wrote 10 helpful reviews",
            assetPath = "badges/top_reviewer.webp"
        ),
        Badge(
            id = "first_post",
            name = "First Post",
            description = "Posted 1 trip",
            assetPath = "badges/first_post.webp"
        ),
        Badge(
            id = "top_content_creator",
            name = "Top Content Creator",
            description = "Posted 5 trip with 5 stars",
            assetPath = "badges/top_content_creator.webp"
        ),
        Badge(
            id = "first_post_get_liked",
            name = "First Like",
            description = "Get first like on any post",
            assetPath = "badges/first_post_get_liked.webp"
        )

    )

    fun getAllBadges(): List<Badge> = allBadges

    fun getBadgeById(id: String): Badge? = allBadges.find { it.id == id }

    fun getBadgesByIds(ids: List<String>): List<Badge> {
        return allBadges.filter { it.id in ids }
    }
}