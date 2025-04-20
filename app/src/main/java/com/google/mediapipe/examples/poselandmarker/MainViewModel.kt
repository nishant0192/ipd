// File: app/src/main/java/com/google/mediapipe/examples/poselandmarker/MainViewModel.kt
package com.google.mediapipe.examples.poselandmarker

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.mediapipe.examples.poselandmarker.data.*
import com.google.mediapipe.examples.poselandmarker.rl.QLearningAgent
import com.google.mediapipe.examples.poselandmarker.recommend.ItemBasedRecommender
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.roundToInt

class MainViewModel(app: Application) : AndroidViewModel(app) {

  // ─── Original pose‑landmarker settings ─────────────────────────────────
  private var _model = 0
  private var _delegate = 0
  private var _minPoseDetectionConfidence = 0.5f
  private var _minPoseTrackingConfidence   = 0.5f
  private var _minPosePresenceConfidence   = 0.5f
  private var _exerciseType = ExerciseType.BICEP

  val currentModel: Int get() = _model
  val currentDelegate: Int get() = _delegate
  val currentMinPoseDetectionConfidence: Float get() = _minPoseDetectionConfidence
  val currentMinPoseTrackingConfidence: Float get() = _minPoseTrackingConfidence
  val currentMinPosePresenceConfidence: Float get() = _minPosePresenceConfidence
  val currentExerciseType: ExerciseType        get() = _exerciseType

  fun setModel(m: Int)                         { _model = m }
  fun setDelegate(d: Int)                      { _delegate = d }
  fun setMinPoseDetectionConfidence(c: Float)  { _minPoseDetectionConfidence = c }
  fun setMinPoseTrackingConfidence(c: Float)   { _minPoseTrackingConfidence = c }
  fun setMinPosePresenceConfidence(c: Float)   { _minPosePresenceConfidence = c }
  fun setExerciseType(t: ExerciseType)         { _exerciseType = t }

  // ─── Client‑side RL/RS fields ────────────────────────────────────────────
  private val db          = AppDatabase.getInstance(app)
  private val rlAgent     = QLearningAgent(app)
  private val recommender = ItemBasedRecommender(db.ratingDao())
  private val gson        = Gson()

  private val _difficulty      = MutableStateFlow(1)
  val difficulty: StateFlow<Int> = _difficulty

  private val _recommendations      = MutableStateFlow<List<String>>(emptyList())
  val recommendations: StateFlow<List<String>> = _recommendations

  companion object {
    private const val BATCH = 5
    private const val TARGET_ANGLE = 45
  }

  /** On each rep: store sample then tune difficulty */
  fun recordRep(angle: Float, errors: List<String>) {
    viewModelScope.launch {
      withContext(Dispatchers.IO) {
        db.sampleDao().insert(
          SampleEntity(
            timestamp = System.currentTimeMillis(),
            reps = 1,
            avgAngle = angle,
            errorsJson = gson.toJson(errors)
          )
        )
      }
      updateRL()
    }
  }

  private fun updateRL() = viewModelScope.launch {
    val recents = withContext(Dispatchers.IO) {
      db.sampleDao().getRecent(BATCH)
    }
    if (recents.size < BATCH) return@launch

    val avg = recents.map { it.avgAngle }.average().roundToInt()
    val errs = recents.sumOf {
      gson.fromJson(it.errorsJson, Array<String>::class.java).size
    }

    val state  = com.google.mediapipe.examples.poselandmarker.rl.State(avg, errs)
    val action = rlAgent.selectAction(state)
    _difficulty.value = (_difficulty.value + action).coerceAtLeast(1)

    val reward = if (abs(avg - TARGET_ANGLE) < 5 && errs == 0) 1f else -1f
    rlAgent.update(state, action, reward, state)
  }

  /** When user rates a workout */
  fun submitRating(workoutId: String, rating: Int) {
    viewModelScope.launch {
      withContext(Dispatchers.IO) {
        db.ratingDao().insert(RatingEntity(workoutId = workoutId, rating = rating))
      }
      updateRecommendations()
    }
  }

  private fun updateRecommendations() = viewModelScope.launch {
    val recs = withContext(Dispatchers.Default) {
      recommender.recommend()
    }
    _recommendations.value = recs
  }
}
