package com.example.spaceadvisor.ui.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.spaceadvisor.R
import com.example.spaceadvisor.SpaceAdvisorApplication
import com.example.spaceadvisor.databinding.DialogChooseImageSourceBinding
import com.example.spaceadvisor.ui.activities.BaseActivity
import com.example.spaceadvisor.ui.activities.LoadingType
import com.example.spaceadvisor.ui.adapters.ColorAdapter
import com.example.spaceadvisor.utils.SettingsManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class SelectDialogAccentColor : BottomSheetDialogFragment() {

    private var _binding: DialogChooseImageSourceBinding? = null
    private val binding get() = _binding!!
    private lateinit var settingsManager: SettingsManager

    private var onColorSelected: ((Int) -> Unit)? = null

    fun setOnColorSelectedListener(listener: (Int) -> Unit) {
        this.onColorSelected = listener
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogChooseImageSourceBinding.inflate(inflater, container, false)
        settingsManager =
            (requireActivity().application as SpaceAdvisorApplication).appContainer.settingsManager
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.dialogSelectTitle.text = "Select Accent Color"

        val themeOptions = listOf(
            Triple(
                getString(R.string.ac_indigo),
                R.color.accent_space_purple,
                R.style.Theme_SpaceAdvisor
            ),
            Triple(
                getString(R.string.ac_blue),
                R.color.accent_space_blue,
                R.style.Theme_SpaceAdvisor_Blue
            ),
            Triple(
                getString(R.string.ac_crimson),
                R.color.accent_space_red,
                R.style.Theme_SpaceAdvisor_Red
            ),
            Triple(
                getString(R.string.ac_green),
                R.color.accent_space_green,
                R.style.Theme_SpaceAdvisor_Green
            )
        )

        binding.itemRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = ColorAdapter(themeOptions.map { Pair(it.first, it.second) }) { index ->
                val selectedTheme = themeOptions[index].third
                onColorSelected?.invoke(selectedTheme)
                val currentActivity = activity as? BaseActivity
                currentActivity?.showLoading("Updating accent color...", LoadingType.LOTTIE)
                dismiss()

            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "SelectDialogAccentColor"
        fun newInstance() = SelectDialogAccentColor()
    }
}
