package com.google.mediapipe.examples.poselandmarker.fragment

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.mediapipe.examples.poselandmarker.ExerciseType
import com.google.mediapipe.examples.poselandmarker.R
import com.google.mediapipe.examples.poselandmarker.feedback.EnhancedFormFeedback
import com.google.mediapipe.examples.poselandmarker.history.WorkoutHistoryDatabase
import com.google.mediapipe.examples.poselandmarker.history.WorkoutHistoryRepository
import com.google.mediapipe.examples.poselandmarker.history.WorkoutResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Dialog fragment to display detailed workout analysis
 */
class WorkoutDetailsDialog : DialogFragment() {

    private lateinit var workoutId: String
    private lateinit var workoutHistoryRepository: WorkoutHistoryRepository
    
    // UI Elements
    private lateinit var exerciseTypeText: TextView
    private lateinit var workoutDateText: TextView
    private lateinit var gradeText: TextView
    private lateinit var durationText: TextView
    private lateinit var totalRepsText: TextView
    private lateinit var perfectFormText: TextView
    private lateinit var scoreText: TextView
    private lateinit var primaryRecommendationText: TextView
    private lateinit var secondaryRecommendationText: TextView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Set style for full screen dialog
        setStyle(STYLE_NORMAL, R.style.FullScreenDialogStyle)
        
        // Get workout ID from arguments
        workoutId = arguments?.getString(ARG_WORKOUT_ID) ?: return
        
        // Initialize repository
        val db = WorkoutHistoryDatabase.getDatabase(requireContext())
        workoutHistoryRepository = WorkoutHistoryRepository(db.workoutHistoryDao())
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_workout_details, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize UI elements
        exerciseTypeText = view.findViewById(R.id.text_exercise_type)
        workoutDateText = view.findViewById(R.id.text_workout_date)
        gradeText = view.findViewById(R.id.text_grade)
        durationText = view.findViewById(R.id.text_duration)
        totalRepsText = view.findViewById(R.id.text_total_reps)
        perfectFormText = view.findViewById(R.id.text_perfect_form)
        scoreText = view.findViewById(R.id.text_score)
        primaryRecommendationText = view.findViewById(R.id.text_primary_recommendation)
        secondaryRecommendationText = view.findViewById(R.id.text_secondary_recommendation)
        
        // Set up close button
        view.findViewById<Button>(R.id.btn_close).setOnClickListener {
            dismiss()
        }
        
        // Set up share button
        view.findViewById<Button>(R.id.btn_share).setOnClickListener {
            // Share workout results (would implement sharing functionality)
        }
        
        // Load workout details
        loadWorkoutDetails()
    }
    
    override fun onStart() {
        super.onStart()
        
        // Make dialog full screen
        dialog?.window?.let { window ->
            val width = ViewGroup.LayoutParams.MATCH_PARENT
            val height = ViewGroup.LayoutParams.MATCH_PARENT
            window.setLayout(width, height)
        }
    }
    
    /**
     * Load workout details from database and populate UI
     */
    private fun loadWorkoutDetails() {
        lifecycleScope.launch {
            // Show loading state
            showLoadingState(true)
            
            // Get workout details
            val (workout, repDetails) = withContext(Dispatchers.IO) {
                workoutHistoryRepository.getWorkoutWithDetails(workoutId)
            }
            
            if (workout != null) {
                // Process rep details to create a simple workout result
                val workoutResult = processWorkoutData(workout, repDetails)
                
                // Update UI with workout details
                updateUI(workoutResult)
            }
            
            // Hide loading state
            showLoadingState(false)
        }
    }
    
    /**
     * Process workout data to create a workout result with analysis
     */
    private fun processWorkoutData(
        workout: com.google.mediapipe.examples.poselandmarker.history.WorkoutEntity,
        repDetails: List<com.google.mediapipe.examples.poselandmarker.history.RepDetailEntity>
    ): WorkoutResult {
        // Create empty maps and lists for workout result
        val formIssueFrequency = mutableMapOf<EnhancedFormFeedback.FormIssue, Int>()
        val progressOverTime = mutableListOf<Pair<Int, Float>>()
        val formQualityOverTime = mutableListOf<Pair<Int, EnhancedFormFeedback.FormRating>>()
        
        // Process rep details
        val gson = Gson()
        val typeToken = object : TypeToken<List<EnhancedFormFeedback.FormIssue>>() {}.type
        
        repDetails.forEach { repDetail ->
            // Add angle to progress data
            progressOverTime.add(Pair(repDetail.repNumber, repDetail.angle))
            
            // Add form rating to quality data
            formQualityOverTime.add(Pair(repDetail.repNumber, repDetail.formRating))
            
            // Parse and count form issues
            try {
                val issues: List<EnhancedFormFeedback.FormIssue> = 
                    gson.fromJson(repDetail.issuesJson, typeToken)
                    
                issues.forEach { issue ->
                    formIssueFrequency[issue] = (formIssueFrequency[issue] ?: 0) + 1
                }
            } catch (e: Exception) {
                // Handle parsing errors
            }
        }
        
        // Create and return workout result
        return WorkoutResult(
            workoutEntity = workout,
            repDetails = repDetails,
            formIssueFrequency = formIssueFrequency,
            progressOverTime = progressOverTime,
            formQualityOverTime = formQualityOverTime
        )
    }
    
    /**
     * Update UI with workout details
     */
    private fun updateUI(workoutResult: WorkoutResult) {
        val workout = workoutResult.workoutEntity
        
        // Set exercise info
        exerciseTypeText.text = getExerciseTypeName(workout.exerciseType)
        
        // Set date
        val dateFormat = SimpleDateFormat("MMMM d, yyyy • h:mm a", Locale.getDefault())
        workoutDateText.text = dateFormat.format(Date(workout.timestamp))
        
        // Set grade
        gradeText.text = workoutResult.grade
        
        // Set grade background color
        val gradeBackgroundDrawable = gradeText.background
        val colorRes = when {
            workout.score >= 80 -> R.color.green_grade
            workout.score >= 70 -> R.color.blue_grade
            workout.score >= 60 -> R.color.yellow_grade
            workout.score >= 50 -> R.color.orange_grade
            else -> R.color.red_grade
        }
        gradeBackgroundDrawable.setTint(ContextCompat.getColor(requireContext(), colorRes))
        
        // Set summary data
        durationText.text = workoutResult.formattedDuration
        totalRepsText.text = workout.totalReps.toString()
        perfectFormText.text = "${workoutResult.perfectFormPercentage}%"
        scoreText.text = workout.score.toString()
        
        // Setup recommendations
        setupRecommendations(workoutResult)
    }
    
    /**
     * Set up recommendations based on form issues
     */
    private fun setupRecommendations(workoutResult: WorkoutResult) {
        // Find most common form issue
        val topIssue = workoutResult.formIssueFrequency.entries
            .maxByOrNull { it.value }?.key
            
        if (topIssue != null) {
            // Set primary recommendation based on top issue
            primaryRecommendationText.text = "Focus on: ${getDetailedTipForIssue(topIssue)}"
        } else {
            primaryRecommendationText.text = "Your form was excellent! Keep up the good work."
        }
        
        // Set secondary recommendation based on exercise type
        secondaryRecommendationText.text = when (workoutResult.workoutEntity.exerciseType) {
            ExerciseType.BICEP -> "Try adding more weight or increasing reps to challenge yourself."
            ExerciseType.SQUAT -> "Focus on depth and keeping your weight on your heels."
            ExerciseType.LATERAL_RAISE -> "Keep the movement controlled and avoid using momentum."
            ExerciseType.LUNGES -> "Focus on balance and knee alignment during the movement."
            ExerciseType.SHOULDER_PRESS -> "Maintain core engagement throughout the exercise."
        }
    }
    
    /**
     * Show or hide loading state
     */
    private fun showLoadingState(isLoading: Boolean) {
        // Would implement loading indicator
    }
    
    /**
     * Get exercise type display name
     */
    private fun getExerciseTypeName(exerciseType: ExerciseType): String {
        return when (exerciseType) {
            ExerciseType.BICEP -> "Bicep Curl"
            ExerciseType.SQUAT -> "Squat"
            ExerciseType.LATERAL_RAISE -> "Lateral Raise"
            ExerciseType.LUNGES -> "Lunges"
            ExerciseType.SHOULDER_PRESS -> "Shoulder Press"
        }
    }
    
    /**
     * Get detailed tip for a form issue
     */
    private fun getDetailedTipForIssue(issue: EnhancedFormFeedback.FormIssue): String {
        return issue.tip
    }
    
    companion object {
        private const val ARG_WORKOUT_ID = "workout_id"
        
        /**
         * Create a new instance of the dialog with workout ID
         */
        fun newInstance(workoutId: String): WorkoutDetailsDialog {
            return WorkoutDetailsDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_WORKOUT_ID, workoutId)
                }
            }
        }
    }
}