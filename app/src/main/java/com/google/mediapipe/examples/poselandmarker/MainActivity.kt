package com.google.mediapipe.examples.poselandmarker

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.mediapipe.examples.poselandmarker.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
  private lateinit var binding: ActivityMainBinding
  private val vm: MainViewModel by viewModels()

  override fun onCreate(saved: Bundle?) {
    super.onCreate(saved)
    binding = ActivityMainBinding.inflate(layoutInflater)
    setContentView(binding.root)

    // Observe difficulty
    vm.difficulty.observe(this) { binding.tvDifficulty.text = "Difficulty: $it" }

    // Observe recommendations
    vm.recommendations.observe(this) {
      binding.tvRecs.text = if (it.isEmpty()) "No recs yet" else it.joinToString()
    }

    // Hook into your pose‑detector’s rep callback:
    binding.btnSimulateRep.setOnClickListener {
      val angle = (30..60).random().toFloat()
      val errors = if (angle<40) listOf("lowArm") else emptyList()
      vm.recordRep(angle, errors)
    }

    // Simulate rating
    binding.btnRate.setOnClickListener {
      vm.submitRating("workout-1", binding.ratingBar.rating.toInt())
    }
  }
}
