package com.example.spaceadvisor.ui.activities

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.example.spaceadvisor.R
import com.example.spaceadvisor.SpaceAdvisorApplication
import com.example.spaceadvisor.databinding.ActivityMainBinding
import com.example.spaceadvisor.ui.fragments.BaseFragment
import com.example.spaceadvisor.ui.fragments.ExploreFragment
import com.example.spaceadvisor.ui.fragments.FeedFragment
import com.example.spaceadvisor.ui.fragments.HomeFragment
import com.example.spaceadvisor.ui.fragments.MyTripsFragment
import com.example.spaceadvisor.ui.fragments.ProfileFragment
import com.example.spaceadvisor.ui.viewmodels.UIViewModel
import com.example.spaceadvisor.ui.viewmodels.UserViewModel
import com.example.spaceadvisor.ui.viewmodels.ViewModelFactory

class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding
    private val userViewModel: UserViewModel by viewModels { ViewModelFactory(application as SpaceAdvisorApplication) }
    private val uiViewModel: UIViewModel by viewModels()

    private val homeFragment by lazy {
        supportFragmentManager.findFragmentByTag("HomeFragment") ?: HomeFragment()
    }
    private val exploreFragment by lazy {
        supportFragmentManager.findFragmentByTag("ExploreFragment")
            ?: ExploreFragment()
    }
    private val tripsFragment by lazy {
        supportFragmentManager.findFragmentByTag("MyTripsFragment") ?: MyTripsFragment()
    }
    private val profileFragment by lazy {
        supportFragmentManager.findFragmentByTag("ProfileFragment") ?: ProfileFragment()
    }
    private val feedFragment by lazy {
        supportFragmentManager.findFragmentByTag("FeedFragment") ?: FeedFragment()
    }
    private var activeFragment: Fragment? = null
    private var isProgrammaticChange = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        hideSystemBars(binding.root)

        setupUserObserver()
        setupUIObserver()
        setupNavigation()

        if (savedInstanceState == null) {
            loadHomeFragment()
        }
    }

    private fun setupUserObserver() {
        userViewModel.getCurrentUid()?.let { uid ->
            userViewModel.startListening(uid)
        }
    }

    private fun setupUIObserver() {
        uiViewModel.uiConfig.observe(this) { config ->
            // Update Title
            binding.headerTitle.text = config.title

            // Header Visibility
            binding.mainHeader.visibility = if (config.isHeaderVisible) View.VISIBLE else View.GONE

            // Handle Custom Header View
            binding.customHeaderContainer.removeAllViews()
            if (config.customHeaderView != null) {
                binding.defaultHeaderTitleContainer.visibility = View.GONE
                binding.customHeaderContainer.visibility = View.VISIBLE
                (config.customHeaderView.parent as? android.view.ViewGroup)?.removeView(config.customHeaderView)
                binding.customHeaderContainer.addView(config.customHeaderView)
            } else {
                binding.defaultHeaderTitleContainer.visibility = View.VISIBLE
                binding.customHeaderContainer.visibility = View.GONE
            }

            // Left Button Control
            binding.mainHeaderLeftBtn.apply {
                visibility = if (config.isLeftBtnVisible) View.VISIBLE else View.GONE
                setIconResource(config.leftIconRes)
                setOnClickListener {
                    config.onLeftClick?.invoke()
                        ?: openSettings()
                }
            }

            // Right Button Control
            binding.mainHeaderRightBtn.apply {
                visibility = if (config.isRightBtnVisible) View.VISIBLE else View.GONE
                setIconResource(config.rightIconRes)
                setOnClickListener {
                    config.onRightClick?.invoke()
                }
            }

            // Navigation Visibilities
            binding.navContainer.visibility =
                if (config.isBottomNavVisible) View.VISIBLE else View.GONE

            // Sync BottomNav Selection
            config.selectedTabId?.let { id ->
                val menuItem = binding.btnNavigation.menu.findItem(id)
                if (binding.btnNavigation.selectedItemId != id && menuItem?.isEnabled == true) {
                    isProgrammaticChange = true
                    binding.btnNavigation.selectedItemId = id
                }
                updateBottomNavStyles(id)
            }
        }
    }


    private fun openSettings() {
        // Implement settings navigation or dialog
    }

    private fun setupNavigation() {
        binding.homeBtn.setOnClickListener {
            if (activeFragment !== homeFragment) loadHomeFragment()
        }

        binding.btnNavigation.setOnItemSelectedListener { item ->
            if (isProgrammaticChange) {
                isProgrammaticChange = false
                return@setOnItemSelectedListener true
            }
            handleNavigation(item.itemId)
            true
        }

        binding.btnNavigation.setOnItemReselectedListener { item ->
            if (activeFragment === homeFragment) {
                handleNavigation(item.itemId)
            } else {
                if (supportFragmentManager.backStackEntryCount > 0) {
                    supportFragmentManager.popBackStack(
                        null,
                        FragmentManager.POP_BACK_STACK_INCLUSIVE
                    )
                }
            }
        }
    }

    private fun handleNavigation(itemId: Int) {
        if (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStackImmediate(
                null,
                FragmentManager.POP_BACK_STACK_INCLUSIVE
            )
        }

        val target = when (itemId) {
            R.id.nav_trips -> tripsFragment
            R.id.nav_profile -> profileFragment
            R.id.nav_explore -> exploreFragment
            R.id.nav_feed -> feedFragment
            else -> return
        }
        loadFragment(target)
    }

    private fun loadHomeFragment() {
        if (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStackImmediate(
                null,
                FragmentManager.POP_BACK_STACK_INCLUSIVE
            )
        }
        loadFragment(homeFragment)
    }

    private fun loadFragment(fragment: Fragment) {
        if (fragment === activeFragment) return

        supportFragmentManager.beginTransaction().apply {

            setCustomAnimations(
                R.anim.fade_in,
                R.anim.fade_out,
                R.anim.fade_in,
                R.anim.fade_out
            )
            activeFragment?.let { hide(it) }
            val tag = fragment.javaClass.simpleName
            if (!fragment.isAdded) add(R.id.main_frame, fragment, tag) else show(fragment)
            commit()
        }

        activeFragment = fragment
        if (fragment is BaseFragment) {
            uiViewModel.updateUI(fragment.getUIConfig())
        }
    }

    private fun updateBottomNavStyles(selectedItemId: Int) {
        val isHome = selectedItemId == R.id.place_holder
        val activeColor = ContextCompat.getColorStateList(
            this,
            if (isHome) R.color.btn_nav_unselected else R.color.nav_clicked
        )
        val fabLabelColor = ContextCompat.getColor(
            this,
            if (isHome) R.color.btn_nav_selected else R.color.btn_nav_unselected
        )

        binding.btnNavigation.itemIconTintList = activeColor
        binding.btnNavigation.itemTextColor = activeColor
        binding.homeBtnLabel.setTextColor(fabLabelColor)
    }
}
