package com.example.spaceadvisor.ui.activities

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.animation.LinearInterpolator
import android.widget.ProgressBar
import androidx.activity.viewModels
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
        if (userViewModel.getCurrentUid() != null) {
            startMainActivity()
        } else {
            startAuthActivity()
        }
    }

    private fun startMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        applyTransition()
        finish()
    }

    private fun startAuthActivity() {
        val intent = Intent(this, AuthActivity::class.java)
        startActivity(intent)
        applyTransition()
        finish()
    }

    private fun applyTransition() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(
                OVERRIDE_TRANSITION_OPEN,
                R.anim.fade_in,
                R.anim.fade_out
            )
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        }
    }
}
