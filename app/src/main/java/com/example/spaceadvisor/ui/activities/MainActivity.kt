package com.example.spaceadvisor.ui.activities

import android.os.Bundle
import android.util.TypedValue
import android.view.View
import androidx.activity.viewModels
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.example.spaceadvisor.R
import com.example.spaceadvisor.SpaceAdvisorApplication
import com.example.spaceadvisor.databinding.ActivityMainBinding
import com.example.spaceadvisor.ui.fragments.BaseFragment
import com.example.spaceadvisor.ui.fragments.ExploreFragment
import com.example.spaceadvisor.ui.fragments.ExploreSearchFragment
import com.example.spaceadvisor.ui.fragments.FeedFragment
import com.example.spaceadvisor.ui.fragments.HomeFragment
import com.example.spaceadvisor.ui.fragments.MyTripsFragment
import com.example.spaceadvisor.ui.fragments.ProfileFragment
import com.example.spaceadvisor.ui.fragments.SettingsFragment
import com.example.spaceadvisor.ui.viewmodels.UIViewModel
import com.example.spaceadvisor.ui.viewmodels.UserViewModel
import com.example.spaceadvisor.ui.viewmodels.ViewModelFactory
import com.example.spaceadvisor.utils.SettingsManager

class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding
    private val userViewModel: UserViewModel by viewModels { ViewModelFactory(application as SpaceAdvisorApplication) }
    private val uiViewModel: UIViewModel by viewModels()
    private lateinit var settingsManager: SettingsManager

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
        settingsManager = (application as SpaceAdvisorApplication).appContainer.settingsManager
        settingsManager.applyTheme(this)

        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        hideSystemBars(binding.root)

        if (savedInstanceState != null) {
            showLoading(type = LoadingType.LOTTIE)
            binding.root.postDelayed({ hideLoading() }, 400)
        }

        setupUserObserver()
        setupUIObserver()
        setupNavigation()

        if (savedInstanceState == null) {
            val isNewUser = intent.getBooleanExtra("IS_NEW_USER", false)
            if (isNewUser) {
                loadFragment(profileFragment)
                binding.btnNavigation.selectedItemId = R.id.nav_profile
            } else {
                loadHomeFragment()
            }
        }
    }

    private fun setupUserObserver() {
        userViewModel.getCurrentUid()?.let { uid ->
            userViewModel.startListening(uid)
        }
    }

    private fun setupUIObserver() {
        uiViewModel.uiConfig.observe(this) { config ->
            binding.headerTitle.text = config.title
            binding.mainHeader.visibility = if (config.isHeaderVisible) View.VISIBLE else View.GONE

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

            binding.mainHeaderLeftBtn.apply {
                visibility = if (config.isLeftBtnVisible) View.VISIBLE else View.GONE
                setIconResource(config.leftIconRes)
                setOnClickListener { config.onLeftClick?.invoke() ?: openSettings() }
            }

            binding.mainHeaderRightBtn.apply {
                visibility = if (config.isRightBtnVisible) View.VISIBLE else View.INVISIBLE
                setIconResource(config.rightIconRes)
                setOnClickListener { config.onRightClick?.invoke() ?: openSearch() }
            }

            binding.navContainer.visibility =
                if (config.isBottomNavVisible) View.VISIBLE else View.GONE

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
        loadFragment(SettingsFragment(), addToBackStack = true)
    }

    private fun openSearch() {
        loadFragment(ExploreSearchFragment(), addToBackStack = true)
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

    private fun loadFragment(fragment: Fragment, addToBackStack: Boolean = false) {
        if (fragment === activeFragment && !addToBackStack) return

        val tag = fragment.javaClass.simpleName
        val transaction = supportFragmentManager.beginTransaction()

        transaction.setCustomAnimations(
            R.anim.fade_in,
            R.anim.fade_out,
            R.anim.fade_in,
            R.anim.fade_out
        )

        supportFragmentManager.fragments.forEach {
            if (it != fragment && it.isAdded && !it.isHidden) {
                transaction.hide(it)
            }
        }

        if (!fragment.isAdded) {
            transaction.add(R.id.main_frame, fragment, tag)
        } else {
            transaction.show(fragment)
        }

        if (addToBackStack) {
            transaction.addToBackStack(tag)
        }

        transaction.commit()
        activeFragment = fragment
        if (fragment is BaseFragment) {
            uiViewModel.updateUI(fragment.getUIConfig())
        }
    }

    private fun updateBottomNavStyles(selectedItemId: Int) {
        val isHome = selectedItemId == R.id.place_holder
        val activeColor = ContextCompat.getColorStateList(
            this,
            if (isHome) R.color.btn_nav_unselected else R.color.nav_btn_tint
        )
        val fabLabelColor = if (isHome) {
            getColorFromAttr(R.attr.customColorPrimary)
        } else {
            ContextCompat.getColor(this, R.color.btn_nav_unselected)
        }

        binding.btnNavigation.itemIconTintList = activeColor
        binding.btnNavigation.itemTextColor = activeColor
        binding.homeBtnLabel.setTextColor(fabLabelColor)
    }

    @ColorInt
    private fun getColorFromAttr(@AttrRes attrColor: Int): Int {
        val typedValue = TypedValue()
        if (theme.resolveAttribute(attrColor, typedValue, true)) {
            return if (typedValue.resourceId != 0) {
                ContextCompat.getColor(this, typedValue.resourceId)
            } else {
                typedValue.data
            }
        }
        return ContextCompat.getColor(this, R.color.primary)
    }
}