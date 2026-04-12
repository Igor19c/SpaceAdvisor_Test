package com.example.spaceadvisor.ui.fragments

import android.content.Intent
import com.example.spaceadvisor.R
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.spaceadvisor.domain.models.UIConfig
import com.example.spaceadvisor.ui.activities.AuthActivity
import com.example.spaceadvisor.ui.activities.BaseActivity
import com.example.spaceadvisor.ui.activities.LoadingType
import com.example.spaceadvisor.ui.viewmodels.UIViewModel

abstract class BaseFragment : Fragment() {

    protected val uiViewModel: UIViewModel by activityViewModels()

    abstract fun getUIConfig(): UIConfig

    override fun onStart() {
        super.onStart()
        if (!isHidden) {
            uiViewModel.updateUI(getUIConfig())
        }
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            uiViewModel.updateUI(getUIConfig())
        }
    }

    fun navigateTo(fragment: BaseFragment) {
        parentFragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.slide_in_bottom_to_top,
                R.anim.fade_out,
                R.anim.fade_in,
                R.anim.slide_out_top_to_bottom
            )
            .add(R.id.main_frame, fragment)
            .hide(this)
            .addToBackStack(null)
            .commit()
    }

    fun navigateToAuth() {
        val intent = Intent(requireActivity(), AuthActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }

    fun showLoading(
        message: String? = null,
        type: LoadingType = LoadingType.PROGRESS_BAR
    ) {
        (activity as? BaseActivity)?.showLoading(message, type)
    }

    fun hideLoading() {
        (activity as? BaseActivity)?.hideLoading()
    }

    fun showError(message: String) {
        showCustomMessage("Error", message)
    }

    fun showCustomMessage(title: String, body: String, duration: Long = 3000) {
        (activity as? BaseActivity)?.showCustomMessage(title, body, duration)
    }


}
