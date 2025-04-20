package com.google.mediapipe.examples.poselandmarker.history

/**
 * Summary statistics across all workouts
 */
data class WorkoutStats(
    val totalWorkouts: Int,
    val totalReps: Int,
    val perfectFormReps: Int,
    val averageScore: Float
)