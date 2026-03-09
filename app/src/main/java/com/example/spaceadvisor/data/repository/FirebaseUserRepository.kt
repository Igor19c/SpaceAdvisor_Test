package com.example.spaceadvisor.data.repository

import android.net.Uri
import com.example.spaceadvisor.domain.models.User
import com.example.spaceadvisor.domain.repository.IUserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap

class FirebaseUserRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
) : IUserRepository {

    // Simple in-memory cache to avoid redundant network calls
    private val userCache = ConcurrentHashMap<String, User>()

    override fun getCurrentUid(): String? = auth.currentUser?.uid
    override fun getCurrentUserName(): String? = auth.currentUser?.displayName

    override fun getFirebaseUserProperties(): Pair<String?, String?> {
        val user = auth.currentUser
        return Pair(user?.displayName, user?.email)
    }

    override fun observeUser(uid: String): Flow<Result<User?>> = callbackFlow {
        val subscription = db.collection("users").document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                val user = snapshot?.toObject(User::class.java)
                if (user != null) {
                    userCache[uid] = user
                }
                trySend(Result.success(user))
            }
        awaitClose { subscription.remove() }
    }

    override suspend fun getUser(uid: String): Result<User?> {
        // Check cache first
        userCache[uid]?.let { return Result.success(it) }

        return try {
            val snapshot = db.collection("users").document(uid).get().await()
            val user = snapshot.toObject(User::class.java)
            if (user != null) {
                userCache[uid] = user
            }
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getUsers(uids: List<String>): Result<List<User>> {
        if (uids.isEmpty()) return Result.success(emptyList())

        val uniqueUids = uids.distinct()
        val usersFromCache = mutableListOf<User>()
        val uidsToFetch = mutableListOf<String>()

        for (uid in uniqueUids) {
            val cachedUser = userCache[uid]
            if (cachedUser != null) {
                usersFromCache.add(cachedUser)
            } else {
                uidsToFetch.add(uid)
            }
        }

        if (uidsToFetch.isEmpty()) return Result.success(usersFromCache)

        return try {
            // Firestore whereIn limit is 30
            val chunks = uidsToFetch.chunked(30)
            val fetchedUsers = mutableListOf<User>()

            for (chunk in chunks) {
                val snapshot = db.collection("users")
                    .whereIn(FieldPath.documentId(), chunk)
                    .get()
                    .await()
                
                val users = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(User::class.java)
                }
                fetchedUsers.addAll(users)
                
                // Update cache
                users.forEach { user -> userCache[user.uid] = user }
            }

            Result.success(usersFromCache + fetchedUsers)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createUserProfile(user: User): Result<Unit> {
        return try {
            db.collection("users").document(user.uid).set(user).await()
            userCache[user.uid] = user
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateProfile(
        uid: String,
        name: String,
        username: String,
        bio: String
    ): Result<Unit> {
        return try {
            val updates = hashMapOf<String, Any>(
                "name" to name,
                "username" to username,
                "bio" to bio
            )
            db.collection("users").document(uid).update(updates).await()
            
            // Update cache if exists
            userCache[uid]?.let { 
                userCache[uid] = it.copy(name = name, username = username, bio = bio)
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateUserBadges(uid: String, badgeIds: List<String>): Result<Unit> {
        return try {
            db.collection("users").document(uid).update("badges", badgeIds).await()
            userCache[uid]?.let { userCache[uid] = it.copy(badges = badgeIds) }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun uploadProfileImage(uid: String, imageUri: Uri): Result<String> {
        return try {
            val storageRef = storage.reference.child("profile_images/$uid.jpg")
            storageRef.putFile(imageUri).await()
            val downloadUrl = storageRef.downloadUrl.await().toString()
            db.collection("users").document(uid).update("profileImageUrl", downloadUrl).await()
            userCache[uid]?.let { userCache[uid] = it.copy(profileImageUrl = downloadUrl) }
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun uploadProfileImageStream(
        uid: String,
        inputStream: InputStream
    ): Result<String> {
        return try {
            val storageRef = storage.reference.child("profile_images/$uid.jpg")
            val bytes = inputStream.use { it.readBytes() }
            val metadata = StorageMetadata.Builder().setContentType("image/webp").build()
            storageRef.putBytes(bytes, metadata).await()
            val downloadUrl = storageRef.downloadUrl.await().toString()
            db.collection("users").document(uid).update("profileImageUrl", downloadUrl).await()
            userCache[uid]?.let { userCache[uid] = it.copy(profileImageUrl = downloadUrl) }
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun signOut() {
        auth.signOut()
        userCache.clear()
    }
}
