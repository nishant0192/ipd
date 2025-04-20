package com.google.mediapipe.examples.poselandmarker.history

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.mediapipe.examples.poselandmarker.feedback.EnhancedFormFeedback
import java.util.UUID

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