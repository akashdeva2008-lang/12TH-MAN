package com.example.twelveman

import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.twelveman.databinding.ActivityMainBinding
import com.example.twelveman.utils.ThemeManager

open class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private var lastNavInsetsBottom = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        // Ensure saved theme mode is applied before inflating views
        ThemeManager.applyTheme(ThemeManager.getTheme(this))

        super.onCreate(savedInstanceState)

        // Enable edge-to-edge system window inset dispatching
        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as NavHostFragment
        navController = navHostFragment.navController

        binding.bottomNavigation.setupWithNavController(navController)

        // Setup WindowInsets listeners for safe status bar & navigation bar handling
        setupWindowInsets()

        // Show/hide bottom navigation dynamically and style status bar per destination
        navController.addOnDestinationChangedListener { _, destination, _ ->
            updateUIForDestination(destination.id)
        }
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.mainRootLayout) { _, windowInsets ->
            val statusBars = windowInsets.getInsets(
                WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.displayCutout()
            )
            val navigationBars = windowInsets.getInsets(
                WindowInsetsCompat.Type.navigationBars()
            )

            // 1. Safe Status Bar Handling:
            // Adjust statusBarScrim height to perfectly match status bar + cutout height
            binding.statusBarScrim.updateLayoutParams {
                height = statusBars.top
            }
            binding.statusBarScrim.visibility = if (statusBars.top > 0) View.VISIBLE else View.GONE

            // 2. Horizontal cutout handling (for notches / landscape):
            binding.mainRootLayout.updatePadding(
                left = statusBars.left,
                right = statusBars.right
            )

            // 3. Navigation bar insets:
            lastNavInsetsBottom = navigationBars.bottom
            applyBottomInsets()

            windowInsets
        }
    }

    private fun applyBottomInsets() {
        if (binding.bottomNavigation.visibility == View.VISIBLE) {
            // BottomNavigation receives padding for the gesture pill / nav bar
            binding.bottomNavigation.updatePadding(bottom = lastNavInsetsBottom)
            binding.navHostFragment.updatePadding(bottom = 0)
        } else {
            // When bottomNavigation is hidden, navHostFragment receives the bottom insets
            // so bottom buttons/content never hide behind the navigation bar
            binding.bottomNavigation.updatePadding(bottom = 0)
            binding.navHostFragment.updatePadding(bottom = lastNavInsetsBottom)
        }
    }

    private fun updateUIForDestination(destinationId: Int) {
        val isDarkMode = ThemeManager.isDarkMode(this)
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)

        // Bottom Navigation visibility
        when (destinationId) {
            R.id.homeFragment,
            R.id.playersFragment,
            R.id.umpiresFragment,
            R.id.tournamentsFragment,
            R.id.bookingsFragment,
            R.id.profileFragment -> {
                binding.bottomNavigation.visibility = View.VISIBLE
            }
            else -> {
                binding.bottomNavigation.visibility = View.GONE
            }
        }
        applyBottomInsets()

        // Status bar scrim background and status icon contrast per destination
        when (destinationId) {
            R.id.homeFragment,
            R.id.umpireDashboardFragment -> {
                // Home screen and Umpire Dashboard have a light surface_white top bar
                binding.statusBarScrim.setBackgroundResource(R.color.surface_white)
                windowInsetsController.isAppearanceLightStatusBars = !isDarkMode
            }
            R.id.loginFragment,
            R.id.registerFragment,
            R.id.roleSelectionFragment,
            R.id.splashFragment -> {
                binding.statusBarScrim.setBackgroundResource(R.color.bg_light)
                windowInsetsController.isAppearanceLightStatusBars = !isDarkMode
            }
            else -> {
                // Dark top bar headers across scoring, tournament, match details, profile, etc.
                binding.statusBarScrim.setBackgroundResource(R.color.top_bar_dark_bg)
                windowInsetsController.isAppearanceLightStatusBars = false
            }
        }

        // Navigation bar icons contrast
        windowInsetsController.isAppearanceLightNavigationBars = !isDarkMode
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}
