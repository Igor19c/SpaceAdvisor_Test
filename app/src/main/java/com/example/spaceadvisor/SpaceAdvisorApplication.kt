package com.example.spaceadvisor

import android.app.Application
import com.example.spaceadvisor.data.repository.FirebaseDestinationRepository
import com.example.spaceadvisor.data.repository.FirebasePostRepository
import com.example.spaceadvisor.data.repository.FirebaseReviewRepository
import com.example.spaceadvisor.data.repository.FirebaseTripRepository
import com.example.spaceadvisor.data.repository.FirebaseUserRepository
import com.example.spaceadvisor.domain.repository.IDestinationRepository
import com.example.spaceadvisor.domain.repository.IPostRepository
import com.example.spaceadvisor.domain.repository.IReviewRepository
import com.example.spaceadvisor.domain.repository.ITripRepository
import com.example.spaceadvisor.domain.repository.IUserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class SpaceAdvisorApplication : Application() {

    lateinit var appContainer: AppContainer

    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer()
    }

    /**
     * Manual Dependency Injection container
     */
    class AppContainer {
        // Firebase Instances
        private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
        private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
        private val storage: FirebaseStorage by lazy { FirebaseStorage.getInstance() }

        // Repositories
        val tripRepository: ITripRepository by lazy {
            FirebaseTripRepository(db)
        }

        val postRepository: IPostRepository by lazy {
            FirebasePostRepository(db)
        }

        val userRepository: IUserRepository by lazy {
            FirebaseUserRepository(auth, db, storage)
        }

        val destinationRepository: IDestinationRepository by lazy {
            FirebaseDestinationRepository(db)
        }

        val reviewRepository: IReviewRepository by lazy {
            FirebaseReviewRepository(db)
        }
    }
}