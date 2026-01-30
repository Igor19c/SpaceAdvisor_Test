package com.example.spaceadvisor.viewmodels

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.spaceadvisor.data.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.InputStream

class UserViewModel(application: Application) : AndroidViewModel(application) {
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val _userData = MutableLiveData<User?>()
    val userData: LiveData<User?> = _userData
    private val _uploadProgress = MutableLiveData<Boolean>()

    fun getCurrentUid(): String? = auth.currentUser?.uid

    fun startListening(uid: String) {
        db.collection("users").document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                _userData.value = snapshot?.toObject(User::class.java)
            }
    }

    fun updateProfile(newName: String, newUsername: String, newBio: String) {
        val uid = getCurrentUid() ?: return
        val updates = hashMapOf<String, Any>(
            "name" to newName,
            "username" to newUsername,
            "bio" to newBio
        )

        db.collection("users").document(uid).update(updates)
    }

    fun uploadProfileImage(imageUri: Uri) {
        val uid = getCurrentUid() ?: return
        _uploadProgress.value = true
        val storageRef = storage.reference.child("profile_images/$uid.jpg")

        val uploadTask = if (imageUri.toString().startsWith("file:///android_asset/")) {
            try {
                val assetPath = imageUri.path?.substringAfter("android_asset/") ?: ""
                val inputStream: InputStream = getApplication<Application>().assets.open(assetPath)
                storageRef.putStream(inputStream)
            } catch (e: Exception) {
                _uploadProgress.value = false
                return
            }
        } else {
            storageRef.putFile(imageUri)
        }

        uploadTask.addOnSuccessListener {
            storageRef.downloadUrl.addOnSuccessListener { uri ->
                updateProfileImageUrl(uid, uri.toString())
            }
        }.addOnFailureListener {
            _uploadProgress.value = false
        }
    }

    private fun updateProfileImageUrl(uid: String, url: String) {
        db.collection("users").document(uid)
            .update("profileImageUrl", url)
            .addOnCompleteListener {
                _uploadProgress.value = false
            }
    }

    fun signOut() {
        auth.signOut()
        _userData.value = null
    }
}
