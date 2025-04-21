package com.google.mediapipe.examples.poselandmarker.history

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.mediapipe.examples.poselandmarker.ExerciseType
import java.util.UUID

/**
 * Entity representing a completed workout
 */
@Entity(tableName = "workouts")
data class WorkoutEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val exerciseType: ExerciseType,
    val timestamp: Long,
    val duration: Long, // duration in milliseconds
    val totalReps: Int,
    val perfectFormReps: Int,
    val averageAngle: Float,
    val score: Int, // 0-100 score based on form
    val difficultyLevel: Int
)