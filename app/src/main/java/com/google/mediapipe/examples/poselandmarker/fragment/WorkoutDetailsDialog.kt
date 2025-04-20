package com.google.mediapipe.examples.poselandmarker.fragment

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.mediapipe.examples.poselandmarker.ExerciseType
import com.google.mediapipe.examples.poselandmarker.R
import com.google.mediapipe.examples.poselandmarker.feedback.EnhancedFormFeedback
import com.google.mediapipe.examples.poselandmarker.history.WorkoutEntity
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
    private lateinit var formQualityChart: PieChart
    private lateinit var progressChart: LineChart
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
        formQualityChart = view.findViewById(R.id.chart_form_quality)
        progressChart = view.findViewById(R.id.chart_progress)
        primaryRecommendationText = view.findViewById(R.id.text_primary_recommendation)
        secondaryRecommendationText = view.findViewById(R.id.text_secondary_recommendation)
        
        // Set up close button
        view.findViewById<View>(R.id.btn_close).setOnClickListener {
            dismiss()
        }
        
        // Set up share button
        view.findViewById<View>(R.id.btn_share).setOnClickListener {
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
                // Process rep details to create a WorkoutResult with analysis
                val workoutResult = processWorkoutData(workout, repDetails)
                
                // Update UI with workout details
                updateUI(workoutResult)
            }
            
            // Hide loading state
            showLoadingState(false)
        }
    }
    
    /**
     * Process workout data to create a comprehensive workout result with analysis
     */
    private fun processWorkoutData(
        workout: WorkoutEntity,
        repDetails: List<com.google.mediapipe.examples.poselandmarker.history.RepDetailEntity>
    ): WorkoutResult {
        // Convert rep details to form issues
        val gson = Gson()
        val typeToken = object : TypeToken<List<EnhancedFormFeedback.FormIssue>>() {}.type
        
        // Create form issue frequency map
        val formIssueFrequency = mutableMapOf<EnhancedFormFeedback.FormIssue, Int>()
        
        // Create progress over time data
        val progressOverTime = mutableListOf<Pair<Int, Float>>()
        
        // Create form quality over time data
        val formQualityOverTime = mutableListOf<Pair<Int, EnhancedFormFeedback.FormRating>>()
        
        // Process each rep detail
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
        
        // Setup form quality chart
        setupFormQualityChart(workoutResult)
        
        // Setup progress chart
        setupProgressChart(workoutResult)
        
        // Setup recommendations
        setupRecommendations(workoutResult)
    }
    
    /**
     * Set up form quality pie chart
     */
    private fun setupFormQualityChart(workoutResult: WorkoutResult) {
        // Process form quality data from form ratings
        val ratingsMap = workoutResult.formQualityOverTime.groupBy { it.second }
            .mapValues { it.value.size }
        
        // Create pie entries
        val entries = ArrayList<PieEntry>()
        EnhancedFormFeedback.FormRating.values().forEach { rating ->
            val count = ratingsMap[rating] ?: 0
            if (count > 0) {
                entries.add(PieEntry(count.toFloat(), rating.displayName))
            }
        }
        
        // If no entries, add a placeholder
        if (entries.isEmpty()) {
            entries.add(PieEntry(1f, "No Data"))
        }
        
        // Create dataset with colors
        val dataSet = PieDataSet(entries, "Form Quality")
        val colors = ArrayList<Int>()
        
        // Add colors based on available ratings
        EnhancedFormFeedback.FormRating.values().forEach { rating ->
            if (ratingsMap[rating] != null && ratingsMap[rating]!! > 0) {
                colors.add(getRatingColor(rating))
            }
        }
        
        // If no colors (only placeholder), add a default
        if (colors.isEmpty()) {
            colors.add(Color.GRAY)
        }
        
        dataSet.colors = colors
        
        // Create and configure pie data
        val data = PieData(dataSet)
        data.setValueTextSize(14f)
        data.setValueTextColor(Color.WHITE)
        
        // Configure chart appearance
        formQualityChart.apply {
            this.data = data
            description.isEnabled = false
            setDrawEntryLabels(false)
            legend.textSize = 14f
            setUsePercentValues(true)
            setCenterText("Form\nQuality")
            setCenterTextSize(16f)
            setHoleRadius(40f)
            setTransparentCircleRadius(45f)
            invalidate()
        }
    }
    
    /**
     * Set up progress line chart
     */
    private fun setupProgressChart(workoutResult: WorkoutResult) {
        // Create entries for the line chart
        val entries = ArrayList<Entry>()
        
        // Add data points from progress over time
        workoutResult.progressOverTime.forEach { (repNumber, angle) ->
            entries.add(Entry(repNumber.toFloat(), angle))
        }
        
        // If no entries, add placeholder
        if (entries.isEmpty()) {
            entries.add(Entry(0f, 0f))
        }
        
        // Create and configure dataset
        val dataSet = LineDataSet(entries, "Rep Angles")
        dataSet.apply {
            color = ContextCompat.getColor(requireContext(), R.color.mp_color_primary)
            lineWidth = 2f
            setCircleColor(ContextCompat.getColor(requireContext(), R.color.mp_color_primary))
            circleRadius = 4f
            setDrawValues(false)
        }
        
        // Create line data
        val lineData = LineData(dataSet)
        
        // Configure chart
        progressChart.apply {
            data = lineData
            description.isEnabled = false
            legend.textSize = 14f
            
            // Configure X axis to show rep numbers
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.granularity = 1f
            xAxis.labelCount = entries.size.coerceAtMost(10) // Limit labels for readability
            
            // Add some padding
            extraBottomOffset = 10f
            extraLeftOffset = 10f
            extraRightOffset = 10f
            
            invalidate()
        }
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
     * Get color for a form rating
     */
    private fun getRatingColor(rating: EnhancedFormFeedback.FormRating): Int {
        return when (rating) {
            EnhancedFormFeedback.FormRating.PERFECT -> 
                ContextCompat.getColor(requireContext(), R.color.perfect_form)
            EnhancedFormFeedback.FormRating.GOOD -> 
                ContextCompat.getColor(requireContext(), R.color.good_form)
            EnhancedFormFeedback.FormRating.FAIR -> 
                ContextCompat.getColor(requireContext(), R.color.fair_form)
            EnhancedFormFeedback.FormRating.NEEDS_WORK -> 
                ContextCompat.getColor(requireContext(), R.color.poor_form)
            EnhancedFormFeedback.FormRating.POOR -> 
                ContextCompat.getColor(requireContext(), R.color.bad_form)
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