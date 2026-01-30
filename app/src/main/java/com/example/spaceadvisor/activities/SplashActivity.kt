package com.example.spaceadvisor.activities

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.animation.LinearInterpolator
import android.widget.ProgressBar
import android.widget.Toast
import com.example.spaceadvisor.R
import com.example.spaceadvisor.data.User
import com.example.spaceadvisor.databinding.ActivitySplashBinding
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@SuppressLint("CustomSplashScreen")
class SplashActivity : BaseActivity() {

    private lateinit var binding: ActivitySplashBinding

    private val signInLauncher = registerForActivityResult(
        FirebaseAuthUIActivityResultContract(),
    ) { res ->
        this.onSignInResult(res)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        hideSystemBars(binding.root)

        val progressBar = binding.loadingProgress
        startAnimation(progressBar)
    }

    private fun startAnimation(progressBar: ProgressBar) {
        val progressAnimator = ValueAnimator.ofInt(0, 100)
        progressAnimator.duration = 1000
        progressAnimator.interpolator = LinearInterpolator()

        progressAnimator.addUpdateListener { visualProgress ->
            val animatedValue = visualProgress.animatedValue as Int
            progressBar.progress = animatedValue

            if (animatedValue == 100) {
                checkUser()
            }
        }
        progressAnimator.start()
    }

    private fun checkUser() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            startMainActivity()
        } else {
            startLogin()
        }
    }

    private fun onSignInResult(res: FirebaseAuthUIAuthenticationResult) {
        val response = res.idpResponse
        if (res.resultCode == RESULT_OK) {
            if (response != null && response.isNewUser) {
                saveNewUserToFirestore()
            } else {
                checkUser()
            }
        } else {
            Toast.makeText(this, "Sign in cancelled", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveNewUserToFirestore() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val db = FirebaseFirestore.getInstance()

        currentUser?.let { firebaseUser ->
            val newUser = User(
                uid = firebaseUser.uid,
                name = firebaseUser.displayName ?: "Traveler",
                email = firebaseUser.email ?: "",
                phone = firebaseUser.phoneNumber ?: "",
                createdAt = System.currentTimeMillis()
            )

            db.collection("users").document(firebaseUser.uid)
                .set(newUser)
                .addOnSuccessListener {
                    startMainActivity()
                }
                .addOnFailureListener {
                    startMainActivity()
                }
        }
    }

    private fun startMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }

    private fun startLogin() {
        val providers = arrayListOf(
            AuthUI.IdpConfig.EmailBuilder().build(),
            AuthUI.IdpConfig.PhoneBuilder()
                .setDefaultCountryIso("il")
                .build(),
            AuthUI.IdpConfig.GoogleBuilder().build()
        )

        val signInIntent = AuthUI.getInstance()
            .createSignInIntentBuilder()
            .setAvailableProviders(providers)
            .setLogo(R.drawable.ic_home_earth)
            .setTheme(R.style.Theme_SpaceAdvisor)
            .setTosAndPrivacyPolicyUrls(
                "https://example.com/terms.html",
                "https://example.com/privacy.html",
            )
            .build()

        signInLauncher.launch(signInIntent)
    }
}
