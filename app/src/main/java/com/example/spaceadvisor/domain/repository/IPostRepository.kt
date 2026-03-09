package com.example.spaceadvisor.domain.repository

import android.net.Uri
import com.example.spaceadvisor.domain.models.Destination
import com.example.spaceadvisor.domain.models.Post
import kotlinx.coroutines.flow.Flow

interface IPostRepository {
    fun fetchUserPosts(uid: String): Flow<Result<List<Post>>>

    fun fetchPosts(): Flow<Result<List<Post>>>

    /**
     * Saves a new post and returns its ID.
     */
    suspend fun savePost(post: Post): Result<String>

    /**
     * Updates an existing post.
     */
    suspend fun updatePost(post: Post): Result<Unit>

    suspend fun likePost(userId: String, post: Post): Result<Unit>

    suspend fun unlikePost(userId: String, postId: String): Result<Unit>

    /**
     * Deletes a post by ID.
     */
    suspend fun deletePost(postId: String): Result<Unit>

    /**
     * Uploads a post image and returns its URL.
     */
    suspend fun uploadPostImage(postId: String, imageUri: Uri): Result<String>
}
