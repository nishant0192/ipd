package com.google.mediapipe.examples.poselandmarker

import androidx.lifecycle.ViewModel

enum class ExerciseType {
    BICEP,
    SQUAT,
    LATERAL_RAISE,
    LUNGES,
    SHOULDER_PRESS
}


/**
 * ViewModel stores pose landmarker settings & chosen exercise
 */
class MainViewModel : ViewModel() {

    private var _model = PoseLandmarkerHelper.MODEL_POSE_LANDMARKER_FULL
    private var _delegate: Int = PoseLandmarkerHelper.DELEGATE_CPU
    private var _minPoseDetectionConfidence: Float =
        PoseLandmarkerHelper.DEFAULT_POSE_DETECTION_CONFIDENCE
    private var _minPoseTrackingConfidence: Float =
        PoseLandmarkerHelper.DEFAULT_POSE_TRACKING_CONFIDENCE
    private var _minPosePresenceConfidence: Float =
        PoseLandmarkerHelper.DEFAULT_POSE_PRESENCE_CONFIDENCE

    // Default to BICEP
    private var _exerciseType: ExerciseType = ExerciseType.BICEP

    val currentModel: Int get() = _model
    val currentDelegate: Int get() = _delegate
    val currentMinPoseDetectionConfidence: Float get() = _minPoseDetectionConfidence
    val currentMinPoseTrackingConfidence: Float get() = _minPoseTrackingConfidence
    val currentMinPosePresenceConfidence: Float get() = _minPosePresenceConfidence
    val currentExerciseType: ExerciseType get() = _exerciseType

    fun setModel(model: Int) { _model = model }
    fun setDelegate(delegate: Int) { _delegate = delegate }
    fun setMinPoseDetectionConfidence(confidence: Float) { _minPoseDetectionConfidence = confidence }
    fun setMinPoseTrackingConfidence(confidence: Float) { _minPoseTrackingConfidence = confidence }
    fun setMinPosePresenceConfidence(confidence: Float) { _minPosePresenceConfidence = confidence }

    fun setExerciseType(exerciseType: ExerciseType) {
        _exerciseType = exerciseType
    }
}
