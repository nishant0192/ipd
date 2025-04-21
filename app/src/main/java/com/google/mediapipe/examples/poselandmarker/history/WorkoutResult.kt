package com.google.mediapipe.examples.poselandmarker.history

import com.google.mediapipe.examples.poselandmarker.feedback.EnhancedFormFeedback

/**
 * Detailed workout result to display in UI
 */
data class WorkoutResult(
    val WorkoutRecord: WorkoutRecord,
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
        get() = if (WorkoutRecord.totalReps > 0) {
            (WorkoutRecord.perfectFormReps * 100) / WorkoutRecord.totalReps
        } else 0
        
    /**
     * Format duration as MM:SS
     */
    val formattedDuration: String
        get() {
            val seconds = WorkoutRecord.duration / 1000
            val minutes = seconds / 60
            val remainingSeconds = seconds % 60
            return String.format("%02d:%02d", minutes, remainingSeconds)
        }
    
    /**
     * Letter grade based on score
     */
    val grade: String
        get() = when {
            WorkoutRecord.score >= 90 -> "A+"
            WorkoutRecord.score >= 85 -> "A"
            WorkoutRecord.score >= 80 -> "A-"
            WorkoutRecord.score >= 75 -> "B+"
            WorkoutRecord.score >= 70 -> "B"
            WorkoutRecord.score >= 65 -> "B-"
            WorkoutRecord.score >= 60 -> "C+"
            WorkoutRecord.score >= 55 -> "C"
            WorkoutRecord.score >= 50 -> "C-"
            WorkoutRecord.score >= 45 -> "D+"
            WorkoutRecord.score >= 40 -> "D"
            else -> "F"
        }
}