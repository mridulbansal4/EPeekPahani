package io.sc.eppCordova.ui

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import dagger.hilt.android.AndroidEntryPoint
import io.sc.eppCordova.R
import io.sc.eppCordova.databinding.ActivityMainBinding

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration

    // Screens where Toolbar should be hidden (auth/splash flow/camera)
    private val fullScreenDestinations = setOf(
        R.id.splashFragment,
        R.id.languageFragment,
        R.id.loginFragment,
        R.id.otpFragment,
        R.id.cameraSurveyFragment,
        R.id.processingFragment,
        R.id.claimResultFragment
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // Top-level destinations (no back button shown)
        appBarConfiguration = AppBarConfiguration(
            setOf(R.id.lossClaimHomeFragment)
        )

        setupActionBarWithNavController(navController, appBarConfiguration)

        // Toggle visibility based on destination
        navController.addOnDestinationChangedListener { _, destination, _ ->
            val isFullScreen = destination.id in fullScreenDestinations
            binding.appBarLayout.visibility = if (isFullScreen) View.GONE else View.VISIBLE
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}
