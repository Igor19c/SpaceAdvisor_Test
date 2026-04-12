package com.example.spaceadvisor.ui.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.spaceadvisor.domain.models.UIConfig

class UIViewModel : ViewModel() {
    private val _uiConfig = MutableLiveData<UIConfig>()
    val uiConfig: LiveData<UIConfig> = _uiConfig

    fun updateUI(config: UIConfig) {
        _uiConfig.value = config
    }
}
