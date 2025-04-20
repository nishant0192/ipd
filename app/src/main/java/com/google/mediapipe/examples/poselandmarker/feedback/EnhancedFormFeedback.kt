package com.google.mediapipe.examples.poselandmarker.feedback

import android.graphics.Color
import com.google.mediapipe.examples.poselandmarker.ExerciseType

/**
 * Enhanced feedback system that provides detailed analysis of exercise form
 */
class EnhancedFormFeedback {

    /**
     * Form error types categorized by severity
     */
    enum class ErrorSeverity { 
        MINOR,   // Minor form issue, can continue but form not optimal
        MODERATE, // Moderate issue that should be corrected, but not dangerous
        SEVERE    // Severe issue that could lead to injury - requires immediate correction
    }
    
    /**
     * Different form issues categorized by body part and movement
     */
    enum class FormIssue(val description: String, val severity: ErrorSeverity, val tip: String) {
        // Bicep curl issues
        ELBOW_SWINGING("Elbow swinging during curl", ErrorSeverity.MODERATE, 
            "Keep your elbows fixed to your sides"),
        WRIST_ROTATION("Excessive wrist rotation", ErrorSeverity.MINOR, 
            "Keep wrists neutral throughout movement"),
        SHOULDER_RAISING("Raising shoulders during curl", ErrorSeverity.MODERATE, 
            "Keep shoulders down and back"),
        BACK_ARCHING("Arching back during curl", ErrorSeverity.SEVERE, 
            "Maintain neutral spine, avoid leaning back"),
            
        // Squat issues
        KNEE_INWARD("Knees caving inward", ErrorSeverity.SEVERE, 
            "Push knees outward in line with toes"),
        KNEES_OVER_TOES("Knees extending past toes", ErrorSeverity.MODERATE, 
            "Shift weight to heels, knees behind toes"),
        SHALLOW_DEPTH("Not reaching proper squat depth", ErrorSeverity.MINOR, 
            "Lower until thighs are parallel to ground"),
        LEANING_FORWARD("Excessive forward lean", ErrorSeverity.MODERATE, 
            "Keep chest up, maintain upright torso"),
        HEELS_RISING("Heels coming off the ground", ErrorSeverity.MODERATE, 
            "Keep weight on heels, whole foot should stay grounded"),
            
        // Lateral raise issues
        SHRUGGING("Shrugging shoulders during raise", ErrorSeverity.MODERATE, 
            "Keep shoulders relaxed and down"),
        ELBOW_BENDING("Excessive elbow bending", ErrorSeverity.MINOR, 
            "Maintain slight elbow bend throughout"),
        RAISING_TOO_HIGH("Raising arms too high", ErrorSeverity.MINOR, 
            "Raise arms to shoulder level, not higher"),
        ASYMMETRIC_MOVEMENT("Uneven arm movement", ErrorSeverity.MODERATE, 
            "Keep both arms moving at the same height"),
            
        // Lunges issues
        FRONT_KNEE_ALIGNMENT("Front knee not aligned with ankle", ErrorSeverity.SEVERE, 
            "Keep front knee directly above ankle"),
        BACK_KNEE_DROP("Back knee dropping too low", ErrorSeverity.MODERATE, 
            "Back knee should hover just above ground"),
        TORSO_LEANING("Leaning torso too far forward", ErrorSeverity.MODERATE, 
            "Keep torso upright, shoulders back"),
        UNEVEN_WEIGHT("Uneven weight distribution", ErrorSeverity.MODERATE, 
            "Weight should be evenly distributed"),
            
        // Shoulder press issues
        BACK_OVERARCHING("Overarching back", ErrorSeverity.SEVERE, 
            "Engage core to maintain neutral spine"),
        ELBOWS_SPLAYING("Elbows splaying outward", ErrorSeverity.MODERATE, 
            "Keep elbows pointing forward"),
        INCOMPLETE_EXTENSION("Incomplete arm extension", ErrorSeverity.MINOR, 
            "Fully extend arms at top of movement"),
        FORWARD_HEAD("Head pushing forward", ErrorSeverity.MODERATE, 
            "Keep head in neutral position, chin tucked");
    }
    
    /**
     * Color codes for different feedback states
     */
    companion object {
        val COLOR_PERFECT = Color.parseColor("#4CAF50") // Green
        val COLOR_GOOD = Color.parseColor("#8BC34A") // Light Green
        val COLOR_FAIR = Color.parseColor("#FFEB3B") // Yellow
        val COLOR_NEEDS_WORK = Color.parseColor("#FF9800") // Orange
        val COLOR_POOR = Color.parseColor("#F44336") // Red
    }
    
    /**
     * Feedback data for a specific exercise repetition
     */
    data class RepFeedback(
        val angle: Float,
        val issues: List<FormIssue>,
        val rating: FormRating,
        val speedRating: SpeedRating,
        val tipMessage: String
    )
    
    /**
     * Overall form quality rating
     */
    enum class FormRating(val displayName: String, val color: Int) {
        PERFECT("Perfect Form", COLOR_PERFECT),
        GOOD("Good Form", COLOR_GOOD),
        FAIR("Fair Form", COLOR_FAIR),
        NEEDS_WORK("Needs Work", COLOR_NEEDS_WORK),
        POOR("Poor Form", COLOR_POOR)
    }
    
    /**
     * Speed rating for exercise tempo
     */
    enum class SpeedRating(val displayName: String) {
        TOO_FAST("Too Fast"),
        GOOD_PACE("Good Pace"),
        TOO_SLOW("Too Slow")
    }
    
    /**
     * Evaluate exercise form and generate detailed feedback
     */
    fun evaluateForm(
        exerciseType: ExerciseType,
        angle: Float,
        errors: List<String>,
        repDuration: Long
    ): RepFeedback {
        // Identify specific form issues based on general errors
        val formIssues = identifyFormIssues(exerciseType, errors)
        
        // Determine overall form rating
        val formRating = when {
            formIssues.isEmpty() -> FormRating.PERFECT
            formIssues.any { it.severity == ErrorSeverity.SEVERE } -> FormRating.POOR
            formIssues.count { it.severity == ErrorSeverity.MODERATE } > 1 -> FormRating.NEEDS_WORK
            formIssues.any { it.severity == ErrorSeverity.MODERATE } -> FormRating.FAIR
            else -> FormRating.GOOD
        }
        
        // Determine movement speed rating
        val speedRating = evaluateSpeed(exerciseType, repDuration)
        
        // Generate tip message
        val tipMessage = if (formIssues.isNotEmpty()) {
            // Prioritize severe issues, then moderate, then minor
            formIssues.sortedByDescending { it.severity }.first().tip
        } else {
            // Default encouraging message
            "Great form! Keep it up!"
        }
        
        return RepFeedback(
            angle = angle,
            issues = formIssues,
            rating = formRating,
            speedRating = speedRating,
            tipMessage = tipMessage
        )
    }
    
    /**
     * Identify specific form issues based on exercise type and generic errors
     */
    private fun identifyFormIssues(
        exerciseType: ExerciseType,
        errors: List<String>
    ): List<FormIssue> {
        val issues = mutableListOf<FormIssue>()
        
        // Map generic error strings to specific form issues
        when (exerciseType) {
            ExerciseType.BICEP -> {
                if (errors.contains("elbow_away_from_body")) {
                    issues.add(FormIssue.ELBOW_SWINGING)
                }
                if (errors.contains("back_arching")) {
                    issues.add(FormIssue.BACK_ARCHING)
                }
                if (errors.contains("shoulder_raised")) {
                    issues.add(FormIssue.SHOULDER_RAISING)
                }
                if (errors.contains("wrist_rotation")) {
                    issues.add(FormIssue.WRIST_ROTATION)
                }
            }
            ExerciseType.SQUAT -> {
                if (errors.contains("knees_over_toes")) {
                    issues.add(FormIssue.KNEES_OVER_TOES)
                }
                if (errors.contains("knees_inward")) {
                    issues.add(FormIssue.KNEE_INWARD)
                }
                if (errors.contains("shallow_depth")) {
                    issues.add(FormIssue.SHALLOW_DEPTH)
                }
                if (errors.contains("leaning_forward")) {
                    issues.add(FormIssue.LEANING_FORWARD)
                }
                if (errors.contains("heels_rising")) {
                    issues.add(FormIssue.HEELS_RISING)
                }
            }
            ExerciseType.LATERAL_RAISE -> {
                if (errors.contains("shoulder_shrugging")) {
                    issues.add(FormIssue.SHRUGGING)
                }
                if (errors.contains("elbows_too_bent")) {
                    issues.add(FormIssue.ELBOW_BENDING)
                }
                if (errors.contains("asymmetric_movement")) {
                    issues.add(FormIssue.ASYMMETRIC_MOVEMENT)
                }
                if (errors.contains("arms_too_high")) {
                    issues.add(FormIssue.RAISING_TOO_HIGH)
                }
            }
            ExerciseType.LUNGES -> {
                if (errors.contains("knees_over_toes")) {
                    issues.add(FormIssue.FRONT_KNEE_ALIGNMENT)
                }
                if (errors.contains("back_knee_drop")) {
                    issues.add(FormIssue.BACK_KNEE_DROP)
                }
                if (errors.contains("torso_leaning")) {
                    issues.add(FormIssue.TORSO_LEANING)
                }
                if (errors.contains("uneven_weight")) {
                    issues.add(FormIssue.UNEVEN_WEIGHT)
                }
            }
            ExerciseType.SHOULDER_PRESS -> {
                if (errors.contains("back_arching")) {
                    issues.add(FormIssue.BACK_OVERARCHING)
                }
                if (errors.contains("elbows_splayed")) {
                    issues.add(FormIssue.ELBOWS_SPLAYING)
                }
                if (errors.contains("incomplete_extension")) {
                    issues.add(FormIssue.INCOMPLETE_EXTENSION)
                }
                if (errors.contains("forward_head")) {
                    issues.add(FormIssue.FORWARD_HEAD)
                }
            }
        }
        
        return issues
    }
    
    /**
     * Evaluate movement speed based on exercise type and duration
     */
    private fun evaluateSpeed(exerciseType: ExerciseType, repDuration: Long): SpeedRating {
        // Ideal rep duration ranges in milliseconds
        val idealDuration = when (exerciseType) {
            ExerciseType.BICEP -> 4000L..6000L // 4-6 seconds is ideal for bicep curl
            ExerciseType.SQUAT -> 3000L..5000L // 3-5 seconds for squat
            ExerciseType.LATERAL_RAISE -> 4000L..6000L // 4-6 seconds for lateral raise
            ExerciseType.LUNGES -> 3000L..5000L // 3-5 seconds for lunges
            ExerciseType.SHOULDER_PRESS -> 4000L..6000L // 4-6 seconds for shoulder press
        }
        
        return when {
            repDuration < idealDuration.first -> SpeedRating.TOO_FAST
            repDuration > idealDuration.last -> SpeedRating.TOO_SLOW
            else -> SpeedRating.GOOD_PACE
        }
    }
    
    /**
     * Get an overall score (0-100) for a workout based on form quality
     */
    fun calculateWorkoutScore(repFeedbacks: List<RepFeedback>): Int {
        if (repFeedbacks.isEmpty()) return 0
        
        // Points per rating
        val ratingPoints = mapOf(
            FormRating.PERFECT to 100,
            FormRating.GOOD to 80,
            FormRating.FAIR to 60,
            FormRating.NEEDS_WORK to 40,
            FormRating.POOR to 20
        )
        
        // Speed modifiers
        val speedModifiers = mapOf(
            SpeedRating.GOOD_PACE to 1.0,
            SpeedRating.TOO_FAST to 0.8,
            SpeedRating.TOO_SLOW to 0.9
        )
        
        // Calculate total score
        var totalScore = 0.0
        
        for (feedback in repFeedbacks) {
            val basePoints = ratingPoints[feedback.rating] ?: 0
            val speedModifier = speedModifiers[feedback.speedRating] ?: 1.0
            
            totalScore += basePoints * speedModifier
        }
        
        // Return average score rounded to nearest integer
        return (totalScore / repFeedbacks.size).toInt()
    }
}