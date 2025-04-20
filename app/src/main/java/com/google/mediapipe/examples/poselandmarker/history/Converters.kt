package com.google.mediapipe.examples.poselandmarker.history

import androidx.room.TypeConverter
import com.google.mediapipe.examples.poselandmarker.ExerciseType
import com.google.mediapipe.examples.poselandmarker.feedback.EnhancedFormFeedback
import java.util.Date

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