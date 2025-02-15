package com.google.mediapipe.examples.poselandmarker

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.mediapipe.examples.poselandmarker.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    // Shared ViewModel that can be accessed by fragments via `activityViewModels()`
    val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // If you're using a BottomNavigationView in activity_main.xml, set it up:
        // (This depends on your layout. If not used, remove these lines.)
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.fragment_container) as NavHostFragment
        val navController = navHostFragment.navController

        // If you have a <com.google.android.material.bottomnavigation.BottomNavigationView
        //    android:id="@+id/navigation" ...> in activity_main.xml, do:
        // binding.navigation.setupWithNavController(navController)

        // Optional: handle nav re-selections
        // binding.navigation.setOnNavigationItemReselectedListener { /* ignore */ }
    }
}
