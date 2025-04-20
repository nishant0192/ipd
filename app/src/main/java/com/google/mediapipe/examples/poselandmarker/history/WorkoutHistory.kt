package com.google.mediapipe.examples.poselandmarker.history

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.mediapipe.examples.poselandmarker.ExerciseType
import com.google.mediapipe.examples.poselandmarker.feedback.EnhancedFormFeedback
import java.util.Date
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

/**
 * Entity representing a detailed analysis of a single rep
 */
@Entity(tableName = "rep_details")
data class RepDetailEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val workoutId: String,
    val repNumber: Int,
    val angle: Float,
    val formRating: EnhancedFormFeedback.FormRating,
    val speedRating: EnhancedFormFeedback.SpeedRating,
    @ColumnInfo(name = "issues") val issuesJson: String, // JSON string of FormIssue list
    val timestamp: Long
)

/**
 * Type converters for Room database
 */
class Converters {
    @TypeConverter
    fun fromExerciseType(value: ExerciseType): String = value.name

    @TypeConverter
    fun toExerciseType(value: String): ExerciseType = ExerciseType.valueOf(value)
    
    @TypeConverter
    fun fromFormRating(value: EnhancedFormFeedback.FormRating): String = value.name
    
    @TypeConverter
    fun toFormRating(value: String): EnhancedFormFeedback.FormRating = 
        EnhancedFormFeedback.FormRating.valueOf(value)
        
    @TypeConverter
    fun fromSpeedRating(value: EnhancedFormFeedback.SpeedRating): String = value.name
    
    @TypeConverter
    fun toSpeedRating(value: String): EnhancedFormFeedback.SpeedRating = 
        EnhancedFormFeedback.SpeedRating.valueOf(value)
    
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? = value?.let { Date(it) }
    
    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? = date?.time
}

/**
 * DAO for workout history
 */
@Dao
interface WorkoutHistoryDao {
    @Insert
    suspend fun insertWorkout(workout: WorkoutEntity)
    
    @Insert
    suspend fun insertRepDetail(repDetail: RepDetailEntity)
    
    @Query("SELECT * FROM workouts ORDER BY timestamp DESC")
    suspend fun getAllWorkouts(): List<WorkoutEntity>
    
    @Query("SELECT * FROM workouts WHERE id = :workoutId")
    suspend fun getWorkoutById(workoutId: String): WorkoutEntity?
    
    @Query("SELECT * FROM rep_details WHERE workoutId = :workoutId ORDER BY repNumber ASC")
    suspend fun getRepDetailsForWorkout(workoutId: String): List<RepDetailEntity>
    
    @Query("SELECT COUNT(*) FROM workouts")
    suspend fun getWorkoutCount(): Int
    
    @Query("SELECT SUM(totalReps) FROM workouts")
    suspend fun getTotalRepsCount(): Int?
    
    @Query("SELECT SUM(perfectFormReps) FROM workouts")
    suspend fun getTotalPerfectFormReps(): Int?
    
    @Query("SELECT AVG(score) FROM workouts")
    suspend fun getAverageWorkoutScore(): Float?
    
    @Query("SELECT * FROM workouts ORDER BY timestamp DESC LIMIT 1")
    suspend fun getMostRecentWorkout(): WorkoutEntity?
    
    @Query("DELETE FROM workouts WHERE id = :workoutId")
    suspend fun deleteWorkout(workoutId: String)
    
    @Query("DELETE FROM rep_details WHERE workoutId = :workoutId")
    suspend fun deleteRepDetailsForWorkout(workoutId: String)
}

/**
 * Room database for workout history
 */
@Database(entities = [WorkoutEntity::class, RepDetailEntity::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class WorkoutHistoryDatabase : RoomDatabase() {
    abstract fun workoutHistoryDao(): WorkoutHistoryDao
    
    companion object {
        @Volatile
        private var INSTANCE: WorkoutHistoryDatabase? = null
        
        fun getDatabase(context: Context): WorkoutHistoryDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WorkoutHistoryDatabase::class.java,
                    "workout_history_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

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
    ): String {
        // Create and save workout entity
        val workoutId = UUID.randomUUID().toString()
        val workout = WorkoutEntity(
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
        
        return workoutId
    }
    
    /**
     * Get all workout summaries
     */
    suspend fun getAllWorkouts() = workoutHistoryDao.getAllWorkouts()
    
    /**
     * Get a specific workout with all rep details
     */
    suspend fun getWorkoutWithDetails(workoutId: String): Pair<WorkoutEntity?, List<RepDetailEntity>> {
        val workout = workoutHistoryDao.getWorkoutById(workoutId)
        val repDetails = workoutHistoryDao.getRepDetailsForWorkout(workoutId)
        return Pair(workout, repDetails)
    }
    
    /**
     * Get summary statistics for all workouts
     */
    suspend fun getWorkoutStats(): WorkoutStats {
        val workoutCount = workoutHistoryDao.getWorkoutCount()
        val totalReps = workoutHistoryDao.getTotalRepsCount() ?: 0
        val perfectFormReps = workoutHistoryDao.getTotalPerfectFormReps() ?: 0
        val averageScore = workoutHistoryDao.getAverageWorkoutScore() ?: 0f
        
        return WorkoutStats(
            totalWorkouts = workoutCount,
            totalReps = totalReps,
            perfectFormReps = perfectFormReps,
            averageScore = averageScore
        )
    }
    
    /**
     * Delete a workout and all its rep details
     */
    suspend fun deleteWorkout(workoutId: String) {
        workoutHistoryDao.deleteRepDetailsForWorkout(workoutId)
        workoutHistoryDao.deleteWorkout(workoutId)
    }
}

/**
 * Summary statistics across all workouts
 */
data class WorkoutStats(
    val totalWorkouts: Int,
    val totalReps: Int,
    val perfectFormReps: Int,
    val averageScore: Float
)

/**
 * Detailed workout result to display in UI
 */
data class WorkoutResult(
    val workoutEntity: WorkoutEntity,
    val repDetails: List<RepDetailEntity>,
    val formIssueFrequency: Map<EnhancedFormFeedback.FormIssue, Int>,
    val progressOverTime: List<Pair<Int, Float>>, // Rep number to angle
    val formQualityOverTime: List<Pair<Int, EnhancedFormFeedback.FormRating>> // Rep number to rating
) {
    /**
     * Most common form issue
     */
    val mostCommonIssue: EnhancedFormFeedback.FormIssue? 
        get() = formIssueFrequency.entries.maxByOrNull { it.value }?.key
    
    /**
     * Percentage of perfect form reps
     */
    val perfectFormPercentage: Int
        get() = if (workoutEntity.totalReps > 0) {
            (workoutEntity.perfectFormReps * 100) / workoutEntity.totalReps
        } else 0
        
    /**
     * Format duration as MM:SS
     */
    val formattedDuration: String
        get() {
            val seconds = workoutEntity.duration / 1000
            val minutes = seconds / 60
            val remainingSeconds = seconds % 60
            return String.format("%02d:%02d", minutes, remainingSeconds)
        }
    
    /**
     * Letter grade based on score
     */
    val grade: String
        get() = when {
            workoutEntity.score >= 90 -> "A+"
            workoutEntity.score >= 85 -> "A"
            workoutEntity.score >= 80 -> "A-"
            workoutEntity.score >= 75 -> "B+"
            workoutEntity.score >= 70 -> "B"
            workoutEntity.score >= 65 -> "B-"
            workoutEntity.score >= 60 -> "C+"
            workoutEntity.score >= 55 -> "C"
            workoutEntity.score >= 50 -> "C-"
            workoutEntity.score >= 45 -> "D+"
            workoutEntity.score >= 40 -> "D"
            else -> "F"
        }
}