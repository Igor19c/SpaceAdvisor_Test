package com.example.spaceadvisor.domain.repository

import com.example.spaceadvisor.domain.models.Review
import kotlinx.coroutines.flow.Flow

interface IReviewRepository {
    suspend fun addReviewAndUpdateRating(review: Review): Result<Unit>
    suspend fun syncDestinationRating(destinationId: String): Result<Unit>
    suspend fun syncAllDestinations(): Result<Unit>
    fun fetchUserReviews(uid: String): Flow<Result<List<Review>>>
    fun fetchDestinationReviews(destinationId: String): Flow<Result<List<Review>>>
    fun fetchAllReviewsOrderedByCreatedAt(): Flow<Result<List<Review>>>

}