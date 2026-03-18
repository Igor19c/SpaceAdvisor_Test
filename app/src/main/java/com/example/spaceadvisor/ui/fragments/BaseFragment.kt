package com.example.spaceadvisor.ui.fragments

import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.spaceadvisor.ui.UIConfig
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