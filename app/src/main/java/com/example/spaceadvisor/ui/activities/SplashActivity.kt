package com.example.spaceadvisor.ui.activities

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.animation.LinearInterpolator
import android.widget.ProgressBar
import androidx.activity.viewModels
import androidx.core.app.ActivityOptionsCompat
import com.example.spaceadvisor.R
import com.example.spaceadvisor.SpaceAdvisorApplication
import com.example.spaceadvisor.databinding.ActivitySplashBinding
import com.example.spaceadvisor.ui.viewmodels.UserViewModel
import com.example.spaceadvisor.ui.viewmodels.ViewModelFactory

@SuppressLint("CustomSplashScreen")
class SplashActivity : BaseActivity() {

    private lateinit var binding: ActivitySplashBinding
    private val userViewModel: UserViewModel by viewModels {
        ViewModelFactory(application as SpaceAdvisorApplication)
    }

    private var isTransitionStarted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        val settingsManager = (application as SpaceAdvisorApplication).appContainer.settingsManager
        settingsManager.applyTheme(this)
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

            if (animatedValue == 100 && !isTransitionStarted) {
                isTransitionStarted = true
                checkUser()
            }
        }
        progressAnimator.start()
    }

    private fun checkUser() {
        val settingsManager = (application as SpaceAdvisorApplication).appContainer.settingsManager
        val uid = userViewModel.getCurrentUid()

        if (settingsManager.needsReAuthAfterPasswordReset) {
            settingsManager.needsReAuthAfterPasswordReset = false
            startAuthActivity(forceRelogin = true)
        } else if (uid != null) {
            startMainActivity()
        } else {
            startAuthActivity()
        }
    }

    private fun startMainActivity() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val options =
            ActivityOptionsCompat.makeCustomAnimation(this, R.anim.fade_in, R.anim.fade_out)
        startActivity(intent, options.toBundle())
        finish()
    }

    private fun startAuthActivity(forceRelogin: Boolean = false) {
        val intent = Intent(this, AuthActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("force_relogin", forceRelogin)
        }
        val options =
            ActivityOptionsCompat.makeCustomAnimation(this, R.anim.fade_in, R.anim.fade_out)
        startActivity(intent, options.toBundle())
        finish()
    }
}
