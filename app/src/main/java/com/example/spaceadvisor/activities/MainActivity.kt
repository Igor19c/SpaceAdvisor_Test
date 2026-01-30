package com.example.spaceadvisor.activities

import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.spaceadvisor.R
import com.example.spaceadvisor.databinding.ActivityMainBinding
import com.example.spaceadvisor.fragments.HomeFragment
import com.example.spaceadvisor.fragments.TripEditorFragment
import com.example.spaceadvisor.fragments.MyTripsFragment
import com.example.spaceadvisor.fragments.ProfileFragment
import com.example.spaceadvisor.viewmodels.UserViewModel
import com.google.firebase.auth.FirebaseAuth

class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding
    private val userViewModel: UserViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        hideSystemBars(binding.root)

        FirebaseAuth.getInstance().currentUser?.uid?.let { uid ->
            userViewModel.startListening(uid)
        }

        val fabHome = binding.homeBtn
        val bottomNav = binding.btnNavigation
        val fabHomeText = binding.homeBtnLabel

        // Default fragment
        if (savedInstanceState == null) {
            loadHomeFragment()
        }

        fabHome.setOnClickListener {
            if (bottomNav.selectedItemId == R.id.place_holder) return@setOnClickListener
            loadHomeFragment()
        }

        bottomNav.setOnItemSelectedListener { item ->
            if (item.itemId == bottomNav.selectedItemId) return@setOnItemSelectedListener false

            bottomNav.itemIconTintList =
                ContextCompat.getColorStateList(this, R.color.nav_clicked)
            bottomNav.itemTextColor =
                ContextCompat.getColorStateList(this, R.color.nav_clicked)
            fabHomeText.setTextColor(ContextCompat.getColor(this, R.color.btn_nav_unselected))

            when (item.itemId) {
                R.id.nav_explore -> {
                    // loadFragment(ExploreFragment())
                    true
                }

                R.id.nav_trips -> {
                    loadFragment(MyTripsFragment())
                    binding.headerTitle.text = "My Trips"
                    true
                }

                R.id.nav_profile -> {
                    loadFragment(ProfileFragment())
                    binding.headerTitle.text = "My Profile"
                    true
                }

                R.id.nav_create_trip -> {
                    loadFragment(TripEditorFragment())
                    binding.headerTitle.text = "My Trip"
                    true
                }

                else -> false
            }
        }
    }

    private fun loadHomeFragment() {
        binding.btnNavigation.selectedItemId = R.id.place_holder
        binding.btnNavigation.itemIconTintList =
            ContextCompat.getColorStateList(this, R.color.btn_nav_unselected)
        binding.btnNavigation.itemTextColor =
            ContextCompat.getColorStateList(this, R.color.btn_nav_unselected)
        binding.homeBtnLabel.setTextColor(ContextCompat.getColor(this, R.color.btn_nav_selected))
        binding.headerTitle.setText(R.string.app_name)
        loadFragment(HomeFragment())
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.main_frame, fragment)
            .commit()
    }
}