package com.google.mediapipe.examples.poselandmarker.history

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

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