// File: app/src/main/java/com/google/mediapipe/examples/poselandmarker/MainActivity.kt
package com.google.mediapipe.examples.poselandmarker

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import com.google.mediapipe.examples.poselandmarker.databinding.ActivityMainBinding
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

  private lateinit var binding: ActivityMainBinding
  private val viewModel: MainViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = ActivityMainBinding.inflate(layoutInflater)
    setContentView(binding.root)

    // If you ever need the NavController:
    val navHost = supportFragmentManager
      .findFragmentById(R.id.fragment_container) as NavHostFragment
    // val navController = navHost.navController
    // (Since app:defaultNavHost="true" is set, navController handles back for you.)

    // ─── Collect difficulty StateFlow ─────────────────────────────────────
    lifecycleScope.launch {
      viewModel.difficulty.collect { diff ->
        binding.tvDifficulty.text = "Difficulty: $diff"
      }
    }

    // ─── Collect recommendations StateFlow ────────────────────────────────
    lifecycleScope.launch {
      viewModel.recommendations.collect { recs ->
        binding.tvRecs.text =
          if (recs.isEmpty()) "No recs yet" else recs.joinToString(", ")
      }
    }

    // ─── Simulate a rep (hook into your real detector instead) ─────────────
    binding.btnSimulateRep.setOnClickListener {
      val simulatedAngle = (30..60).random().toFloat()
      val errors = if (simulatedAngle < 40) listOf("lowArm") else emptyList()
      viewModel.recordRep(simulatedAngle, errors)
    }

    // ─── Submit a rating ──────────────────────────────────────────────────
    binding.btnRate.setOnClickListener {
      val workoutId = "workout-1"  // replace with real workout ID
      val rating    = binding.ratingBar.rating.toInt()
      viewModel.submitRating(workoutId, rating)
    }
  }
}
