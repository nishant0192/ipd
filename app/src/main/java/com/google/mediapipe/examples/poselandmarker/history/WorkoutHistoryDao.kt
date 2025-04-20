package com.google.mediapipe.examples.poselandmarker.history

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

/**
 * DAO for workout history
 */
@Dao
interface WorkoutHistoryDao {
    @Insert
    fun insertWorkout(workout: WorkoutEntity)
    
    @Insert
    fun insertRepDetail(repDetail: RepDetailEntity)
    
    @Query("SELECT * FROM workouts ORDER BY timestamp DESC")
    fun getAllWorkouts(): List<WorkoutEntity>
    
    @Query("SELECT * FROM workouts WHERE id = :workoutId")
    fun getWorkoutById(workoutId: String): WorkoutEntity?
    
    @Query("SELECT * FROM rep_details WHERE workoutId = :workoutId ORDER BY repNumber ASC")
    fun getRepDetailsForWorkout(workoutId: String): List<RepDetailEntity>
    
    @Query("SELECT COUNT(*) FROM workouts")
    fun getWorkoutCount(): Int
    
    @Query("SELECT SUM(totalReps) FROM workouts")
    fun getTotalRepsCount(): Int?
    
    @Query("SELECT SUM(perfectFormReps) FROM workouts")
    fun getTotalPerfectFormReps(): Int?
    
    @Query("SELECT AVG(score) FROM workouts")
    fun getAverageWorkoutScore(): Float?
    
    @Query("SELECT * FROM workouts ORDER BY timestamp DESC LIMIT 1")
    fun getMostRecentWorkout(): WorkoutEntity?
    
    @Query("DELETE FROM workouts WHERE id = :workoutId")
    fun deleteWorkout(workoutId: String)
    
    @Query("DELETE FROM rep_details WHERE workoutId = :workoutId")
    fun deleteRepDetailsForWorkout(workoutId: String)
}