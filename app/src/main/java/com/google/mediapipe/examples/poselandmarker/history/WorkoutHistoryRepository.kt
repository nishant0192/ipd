package com.google.mediapipe.examples.poselandmarker.history

import com.google.gson.Gson
import com.google.mediapipe.examples.poselandmarker.ExerciseType
import com.google.mediapipe.examples.poselandmarker.feedback.EnhancedFormFeedback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Repository for workout history data
 */
class WorkoutHistoryRepository(private val workoutHistoryDao: WorkoutHistoryDao) {
    
    /**
     * Save a complete workout with rep details
     */
    suspend fun saveWorkout(
        exerciseType: ExerciseType,
        duration: Long,
        totalReps: Int,
        perfectFormReps: Int,
        averageAngle: Float,
        score: Int,
        difficultyLevel: Int,
        repFeedbacks: List<EnhancedFormFeedback.RepFeedback>
    ): String = withContext(Dispatchers.IO) {
        // Create and save workout entity
        val workoutId = UUID.randomUUID().toString()
        val workout = WorkoutRecord(
            id = workoutId,
            exerciseType = exerciseType,
            timestamp = System.currentTimeMillis(),
            duration = duration,
            totalReps = totalReps,
            perfectFormReps = perfectFormReps,
            averageAngle = averageAngle,
            score = score,
            difficultyLevel = difficultyLevel
        )
        workoutHistoryDao.insertWorkout(workout)
        
        // Save detailed rep data
        val gson = Gson()
        repFeedbacks.forEachIndexed { index, feedback ->
            val repDetail = RepDetailEntity(
                workoutId = workoutId,
                repNumber = index + 1,
                angle = feedback.angle,
                formRating = feedback.rating,
                speedRating = feedback.speedRating,
                issuesJson = gson.toJson(feedback.issues),
                timestamp = System.currentTimeMillis()
            )
            workoutHistoryDao.insertRepDetail(repDetail)
        }
        
        return@withContext workoutId
    }
    
    /**
     * Get all workout summaries
     */
    suspend fun getAllWorkouts(): List<WorkoutRecord> = withContext(Dispatchers.IO) {
        return@withContext workoutHistoryDao.getAllWorkouts()
    }
    
    /**
     * Get a specific workout with all rep details
     */
    suspend fun getWorkoutWithDetails(workoutId: String): Pair<WorkoutRecord?, List<RepDetailEntity>> = 
        withContext(Dispatchers.IO) {
            val workout = workoutHistoryDao.getWorkoutById(workoutId)
            val repDetails = workoutHistoryDao.getRepDetailsForWorkout(workoutId)
            return@withContext Pair(workout, repDetails)
        }
    
    /**
     * Get summary statistics for all workouts
     */
    suspend fun getWorkoutStats(): WorkoutStats = withContext(Dispatchers.IO) {
        val workoutCount = workoutHistoryDao.getWorkoutCount()
        val totalReps = workoutHistoryDao.getTotalRepsCount() ?: 0
        val perfectFormReps = workoutHistoryDao.getTotalPerfectFormReps() ?: 0
        val averageScore = workoutHistoryDao.getAverageWorkoutScore() ?: 0f
        
        return@withContext WorkoutStats(
            totalWorkouts = workoutCount,
            totalReps = totalReps,
            perfectFormReps = perfectFormReps,
            averageScore = averageScore
        )
    }
    
    /**
     * Delete a workout and all its rep details
     */
    suspend fun deleteWorkout(workoutId: String) = withContext(Dispatchers.IO) {
        workoutHistoryDao.deleteRepDetailsForWorkout(workoutId)
        workoutHistoryDao.deleteWorkout(workoutId)
    }
}