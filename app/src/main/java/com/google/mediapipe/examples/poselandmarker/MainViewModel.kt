package com.google.mediapipe.examples.poselandmarker

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.mediapipe.examples.poselandmarker.data.*
import com.google.mediapipe.examples.poselandmarker.rl.QLearningAgent
import com.google.mediapipe.examples.poselandmarker.recommend.ItemBasedRecommender
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

class MainViewModel(app: Application) : AndroidViewModel(app) {
  private val db         = AppDatabase.getInstance(app)
  private val rlAgent    = QLearningAgent(app)
  private val recommender= ItemBasedRecommender(db.ratingDao())
  private val gson       = Gson()

  private val _difficulty      = MutableStateFlow(1)
  val difficulty: StateFlow<Int> = _difficulty

  private val _recommendations = MutableStateFlow<List<String>>(emptyList())
  val recommendations: StateFlow<List<String>> = _recommendations

  companion object {
    private const val BATCH = 5
    private const val TARGET_ANGLE = 45
  }

  /** Call this on each detected rep */
  fun recordRep(angle: Float, errors: List<String>) {
    viewModelScope.launch {
      db.sampleDao().insert(
        SampleEntity(
          timestamp = System.currentTimeMillis(),
          reps = 1,
          avgAngle = angle,
          errorsJson = gson.toJson(errors)
        )
      )
      updateRL()
    }
  }

  private fun updateRL() = viewModelScope.launch {
    val recents = db.sampleDao().getRecent(BATCH)
    if (recents.size < BATCH) return@launch

    val avg = recents.map { it.avgAngle }.average().roundToInt()
    val errs= recents.sumOf { gson.fromJson(it.errorsJson, Array<String>::class.java).size }
    val state = com.google.mediapipe.examples.poselandmarker.rl.State(avg, errs)
    val action= rlAgent.selectAction(state)
    _difficulty.value = (_difficulty.value + action).coerceAtLeast(1)

    val reward = if (abs(avg - TARGET_ANGLE) < 5 && errs == 0) 1f else -1f
    rlAgent.update(state, action, reward, state)
  }

  /** Call when user rates a workout */
  fun submitRating(workoutId: String, rating: Int) {
    viewModelScope.launch {
      db.ratingDao().insert(RatingEntity(workoutId = workoutId, rating = rating))
      _recommendations.value = recommender.recommend()
    }
  }
}
