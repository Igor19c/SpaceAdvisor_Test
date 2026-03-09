package com.example.spaceadvisor.data.repository

import android.net.Uri
import com.example.spaceadvisor.domain.models.Destination
import com.example.spaceadvisor.domain.models.Post
import com.example.spaceadvisor.domain.repository.IPostRepository
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebasePostRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
) : IPostRepository {

    override fun fetchUserPosts(uid: String): Flow<Result<List<Post>>> = callbackFlow {
        val subscription = db.collection("posts")
            .whereEqualTo("uid", uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val posts = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Post::class.java)?.copy(id = doc.id)
                    }
                    trySend(Result.success(posts))
                }
            }
        awaitClose { subscription.remove() }
    }

    override fun fetchPosts(): Flow<Result<List<Post>>> = callbackFlow {
        val subscription = db.collection("posts")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val posts = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Post::class.java)?.copy(id = doc.id)
                    }
                    trySend(Result.success(posts))
                }
            }
        awaitClose { subscription.remove() }
    }

    override suspend fun savePost(post: Post): Result<String> {
        return try {
            val documentReference = db.collection("posts").document()
            val postWithId = post.copy(id = documentReference.id)
            documentReference.set(postWithId).await()
            Result.success(documentReference.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updatePost(post: Post): Result<Unit> {
        return try {
            if (post.id.isEmpty()) throw Exception("Post ID is required for update")
            db.collection("posts").document(post.id).set(post).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun likePost(userId: String, post: Post): Result<Unit> {
        return try {
            db.collection("posts").document(post.id)
                .update(
                    "likedBy", FieldValue.arrayUnion(userId),
                    "likes", FieldValue.increment(1)
                ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun unlikePost(userId: String, postId: String): Result<Unit> {
        return try {
            db.collection("posts").document(postId)
                .update(
                    "likedBy",
                    FieldValue.arrayRemove(userId),
                    "likes",
                    FieldValue.increment(-1)
                ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deletePost(postId: String): Result<Unit> {
        return try {
            db.collection("posts").document(postId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun uploadPostImage(postId: String, imageUri: Uri): Result<String> {
        return try {
            val storageRef = storage.reference.child("post_images/$postId.jpg")
            storageRef.putFile(imageUri).await()
            val downloadUrl = storageRef.downloadUrl.await().toString()
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
