package com.example.spaceadvisor.ui.activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.viewModels
import androidx.core.app.ActivityOptionsCompat
import com.example.spaceadvisor.R
import com.example.spaceadvisor.SpaceAdvisorApplication
import com.example.spaceadvisor.databinding.ActivityAuthBinding
import com.example.spaceadvisor.ui.viewmodels.UserViewModel
import com.example.spaceadvisor.ui.viewmodels.ViewModelFactory
import com.firebase.ui.auth.AuthMethodPickerLayout
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult
import com.google.firebase.auth.FirebaseAuth

class AuthActivity : BaseActivity() {

    private lateinit var binding: ActivityAuthBinding
    private val userViewModel: UserViewModel by viewModels {
        ViewModelFactory(application as SpaceAdvisorApplication)
    }
    private val signInLauncher = registerForActivityResult(
        FirebaseAuthUIActivityResultContract(),
    ) { res ->
        this.onSignInResult(res)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val forceRelogin = intent.getBooleanExtra("force_relogin", false)
        val currentUser = FirebaseAuth.getInstance().currentUser

        if (forceRelogin) {
            AuthUI.getInstance().signOut(this).addOnCompleteListener {
                startLogin()
            }
        } else if (currentUser == null) {
            startLogin()
        } else {
            startMainActivity(isNewUser = false)
        }
    }

    private fun initBinding() {
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)
        hideSystemBars(binding.root)
    }

    private fun startLogin() {
        val providers = arrayListOf(
            AuthUI.IdpConfig.EmailBuilder().setRequireName(true).setAllowNewAccounts(true).build(),
            AuthUI.IdpConfig.GoogleBuilder().build()
        )

        val customLayout = AuthMethodPickerLayout.Builder(R.layout.activity_auth)
            .setEmailButtonId(R.id.email_button_auth_activity)
            .setGoogleButtonId(R.id.google_button_auth_activity)
            .build()

        val signInIntent = AuthUI.getInstance()
            .createSignInIntentBuilder()
            .setAvailableProviders(providers)
            .setAuthMethodPickerLayout(customLayout)
            .setCredentialManagerEnabled(false)
            .setTosAndPrivacyPolicyUrls(
                "https://spaceadvisor.page.link/terms",
                "https://spaceadvisor.page.link/privacy"
            )
            .setTheme(R.style.Theme_Auth_FullScreen)
            .build()

        signInLauncher.launch(signInIntent)
    }

    private fun onSignInResult(res: FirebaseAuthUIAuthenticationResult) {
        val response = res.idpResponse

        if (res.resultCode == RESULT_OK) {
            val isNewUser = response?.isNewUser ?: false

            if (isNewUser) {
                initBinding()

                showLoading("Preparing your space...", LoadingType.LOTTIE)
                userViewModel.createNewUserProfile {
                    Handler(Looper.getMainLooper()).postDelayed({
                        hideLoading()
                        startMainActivity(isNewUser = true)
                    }, 2200)
                }
            } else {
                startMainActivity(isNewUser = false)
            }
        } else {
            if (response == null) {
                finish()
            } else {
                initBinding()
                showCustomMessage(
                    title = "Connection Error",
                    body = "We couldn't sign you in. Please try again.",
                    duration = 4000
                )
                startLogin()
            }
        }
    }

    private fun startMainActivity(isNewUser: Boolean) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("IS_NEW_USER", isNewUser)
        }

        val options =
            ActivityOptionsCompat.makeCustomAnimation(this, R.anim.fade_in, R.anim.fade_out)
        startActivity(intent, options.toBundle())
        finish()
    }
}
