package com.google.mediapipe.examples.poselandmarker.history

import com.google.mediapipe.examples.poselandmarker.feedback.EnhancedFormFeedback

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