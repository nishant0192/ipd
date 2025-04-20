package com.google.mediapipe.examples.poselandmarker.stats

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.mediapipe.examples.poselandmarker.ExerciseType
import com.google.mediapipe.examples.poselandmarker.MainViewModel
import com.google.mediapipe.examples.poselandmarker.R
import com.google.mediapipe.examples.poselandmarker.feedback.EnhancedFormFeedback
import com.google.mediapipe.examples.poselandmarker.history.WorkoutHistoryDatabase
import com.google.mediapipe.examples.poselandmarker.history.WorkoutHistoryRepository
import com.google.mediapipe.examples.poselandmarker.recommend.RecommendationAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Enhanced workout statistics manager that integrates RL/RS models into the UI
 * and controls workout start/stop functionality with detailed form feedback
 */
class WorkoutStatsManager(
    private val context: Context,
    private val viewModel: MainViewModel,
    private val lifecycleOwner: LifecycleOwner,
    private val onExerciseSelected: (ExerciseType) -> Unit
) {
    // Generate a unique ID for this workout session
    private val workoutId = UUID.randomUUID().toString()

    // Workout state
    private var isWorkoutActive = false
    private var targetReps = 0
    private var collectingJob: Job? = null
    private var workoutStartTime = 0L

    // Feedback collection
    private val repFeedbacks = mutableListOf<EnhancedFormFeedback.RepFeedback>()
    private var lastRepTimestamp = 0L

    // Dialog for displaying workout statistics
    private var statsDialog: Dialog? = null

    // CoroutineScope for async operations
    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())

    // Database access
    private val workoutHistoryRepository: WorkoutHistoryRepository

    init {
        // Initialize repository
        val db = WorkoutHistoryDatabase.getDatabase(context)
        workoutHistoryRepository = WorkoutHistoryRepository(db.workoutHistoryDao())
    }

    /**
     * Record a completed rep with its form quality
     * Only counts if workout is active
     */
    fun recordRep(angle: Float, hasErrors: Boolean): Boolean {
        if (!isWorkoutActive) {
            return false // Workout not started, don't count rep
        }

        // Record rep in ViewModel
        val errors = if (hasErrors) listOf("form_error") else emptyList()
        viewModel.recordRep(angle, errors)

        // Update any currently open dialog
        updateStatsDisplay()

        // Check if target reached
        if (targetReps > 0 && (viewModel.totalReps.value ?: 0) >= targetReps) {
            stopWorkout()
            Toast.makeText(context, "Target reps reached! Great job!", Toast.LENGTH_SHORT).show()
        }

        return true
    }

    /**
     * Record a rep with detailed form feedback
     */
    fun recordRepWithFeedback(
        angle: Float,
        hasErrors: Boolean,
        repFeedback: EnhancedFormFeedback.RepFeedback
    ): Boolean {
        if (!isWorkoutActive) {
            return false // Workout not started, don't count rep
        }

        // Record basic rep data
        val recorded = recordRep(angle, hasErrors)

        // Store the feedback for this rep
        if (recorded) {
            repFeedbacks.add(repFeedback)
            lastRepTimestamp = System.currentTimeMillis()
        }

        return recorded
    }

    /**
     * Start a new workout
     */
    fun startWorkout() {
        if (isWorkoutActive) return

        isWorkoutActive = true
        workoutStartTime = System.currentTimeMillis()
        viewModel.resetWorkoutStats()
        repFeedbacks.clear()

        // Update UI if dialog is showing
        statsDialog?.findViewById<TextView>(R.id.workout_status)?.text = "Workout in progress"
        statsDialog?.findViewById<Button>(R.id.btn_start_workout)?.isEnabled = false
        statsDialog?.findViewById<Button>(R.id.btn_stop_workout)?.isEnabled = true
        statsDialog?.findViewById<View>(R.id.target_reps_layout)?.visibility = View.GONE
    }

    /**
     * Stop the current workout
     */
    fun stopWorkout() {
        if (!isWorkoutActive) return

        isWorkoutActive = false

        // Update UI if dialog is showing
        statsDialog?.findViewById<TextView>(R.id.workout_status)?.text = "Workout completed"
        statsDialog?.findViewById<Button>(R.id.btn_start_workout)?.isEnabled = true
        statsDialog?.findViewById<Button>(R.id.btn_stop_workout)?.isEnabled = false
        statsDialog?.findViewById<View>(R.id.target_reps_layout)?.visibility = View.VISIBLE

        // Show workout analysis dialog with slight delay for better UX
        Handler(Looper.getMainLooper()).postDelayed({
            showWorkoutAnalysis()
        }, 500)
    }

    /**
     * Show workout analysis dialog
     */
    private fun showWorkoutAnalysis() {
        if (repFeedbacks.isEmpty()) {
            Toast.makeText(context, "No workout data to analyze", Toast.LENGTH_SHORT).show()
            return
        }

        val dialogBuilder = AlertDialog.Builder(context)
        val dialogView = LayoutInflater.from(context).inflate(R.layout.workout_analysis_dialog, null)

        // Setup analysis dialog
        setupWorkoutAnalysisDialog(dialogView)

        val dialog = dialogBuilder
            .setView(dialogView)
            .setCancelable(true)
            .create()

        // Set button click listeners
        dialogView.findViewById<Button>(R.id.btn_save).setOnClickListener {
            saveWorkoutToHistory()
            dialog.dismiss()
            Toast.makeText(context, "Workout saved to history", Toast.LENGTH_SHORT).show()
        }

        dialogView.findViewById<Button>(R.id.btn_share).setOnClickListener {
            // Share workout results logic would go here
            Toast.makeText(context, "Sharing functionality coming soon", Toast.LENGTH_SHORT).show()
        }

        dialog.show()
    }

    /**
     * Setup the workout analysis dialog with data
     */
    private fun setupWorkoutAnalysisDialog(dialogView: View) {
        // Get workout data
        val exerciseType = viewModel.currentExerciseType
        val totalReps = viewModel.totalReps.value ?: 0
        val perfectReps = viewModel.perfectReps.value ?: 0
        val avgAngle = viewModel.avgAngle.value ?: 0f
        val workoutDuration = System.currentTimeMillis() - workoutStartTime

        // Calculate overall score
        val score = EnhancedFormFeedback().calculateWorkoutScore(repFeedbacks)

        // Set exercise info
        dialogView.findViewById<TextView>(R.id.text_exercise_type).text = getExerciseTypeName(exerciseType)

        // Set date
        val dateFormat = SimpleDateFormat("MMMM d, yyyy • h:mm a", Locale.getDefault())
        dialogView.findViewById<TextView>(R.id.text_workout_date).text = dateFormat.format(Date())

        // Set grade
        val gradeText = dialogView.findViewById<TextView>(R.id.text_grade)
        val grade = when {
            score >= 90 -> "A+"
            score >= 85 -> "A"
            score >= 80 -> "A-"
            score >= 75 -> "B+"
            score >= 70 -> "B"
            score >= 65 -> "B-"
            score >= 60 -> "C+"
            score >= 55 -> "C"
            score >= 50 -> "C-"
            score >= 45 -> "D+"
            score >= 40 -> "D"
            else -> "F"
        }
        gradeText.text = grade

        // Set background color based on grade
        val backgroundDrawable = gradeText.background
        val colorRes = when {
            score >= 80 -> R.color.green_grade
            score >= 70 -> R.color.blue_grade
            score >= 60 -> R.color.yellow_grade
            score >= 50 -> R.color.orange_grade
            else -> R.color.red_grade
        }
        backgroundDrawable.setTint(ContextCompat.getColor(context, colorRes))

        // Set summary data
        val durationMinutes = workoutDuration / 60000
        val durationSeconds = (workoutDuration % 60000) / 1000
        dialogView.findViewById<TextView>(R.id.text_duration).text =
            String.format("%02d:%02d", durationMinutes, durationSeconds)
        dialogView.findViewById<TextView>(R.id.text_total_reps).text = totalReps.toString()

        val perfectPercentage = if (totalReps > 0) (perfectReps * 100) / totalReps else 0
        dialogView.findViewById<TextView>(R.id.text_perfect_form).text = "$perfectPercentage%"

        dialogView.findViewById<TextView>(R.id.text_score).text = score.toString()

        // Setup form quality chart
        setupFormQualityChart(dialogView)

        // Setup rep-by-rep progress chart
        setupProgressChart(dialogView)

        // Setup form issues if any
        setupFormIssues(dialogView)

        // Setup recommendations
        setupRecommendations(dialogView)
    }

    /**
     * Setup the form quality pie chart
     */
    private fun setupFormQualityChart(dialogView: View) {
        // This would be implemented with actual charting library
    }

    /**
     * Setup the rep-by-rep progress chart
     */
    private fun setupProgressChart(dialogView: View) {
        // This would be implemented with actual charting library
    }

    /**
     * Setup the form issues section
     */
    private fun setupFormIssues(dialogView: View) {
        // Analyze most common issues
        val issueFrequency = mutableMapOf<EnhancedFormFeedback.FormIssue, Int>()

        for (feedback in repFeedbacks) {
            for (issue in feedback.issues) {
                issueFrequency[issue] = (issueFrequency[issue] ?: 0) + 1
            }
        }

        // If no issues, show "no issues" message
        if (issueFrequency.isEmpty()) {
            dialogView.findViewById<TextView>(R.id.text_no_issues)?.visibility = View.VISIBLE
            return
        }

        // For simplicity, show only the most common issue
        val topIssue = issueFrequency.entries.maxByOrNull { it.value }?.key
        topIssue?.let {
            dialogView.findViewById<TextView>(R.id.text_primary_recommendation).text =
                "Focus on: ${it.tip}"
        }
    }

    /**
     * Setup recommendations based on workout data
     */
    private fun setupRecommendations(dialogView: View) {
        val secondaryRecommendation = dialogView.findViewById<TextView>(R.id.text_secondary_recommendation)

        val speedIssues = repFeedbacks.groupBy { it.speedRating }
            .mapValues { it.value.size }

        secondaryRecommendation.text = when {
            (speedIssues[EnhancedFormFeedback.SpeedRating.TOO_FAST] ?: 0) > repFeedbacks.size / 3 ->
                "Try slowing down your movements for better muscle engagement and control."
            (speedIssues[EnhancedFormFeedback.SpeedRating.TOO_SLOW] ?: 0) > repFeedbacks.size / 3 ->
                "Consider a slightly faster pace to maintain tension throughout the movement."
            else ->
                "Your exercise tempo is good. Keep up the consistent pace!"
        }
    }

    /**
     * Save current workout to history database
     */
    private fun saveWorkoutToHistory() {
        if (repFeedbacks.isEmpty()) return

        val exerciseType = viewModel.currentExerciseType
        val totalReps    = viewModel.totalReps.value ?: 0
        val perfectReps  = viewModel.perfectReps.value ?: 0
        val avgAngle     = viewModel.avgAngle.value ?: 0f
        val duration     = System.currentTimeMillis() - workoutStartTime
        val score        = EnhancedFormFeedback().calculateWorkoutScore(repFeedbacks)
        val difficulty   = viewModel.difficulty.value ?: 1

        coroutineScope.launch {
            val savedWorkoutId = withContext(Dispatchers.IO) {
                workoutHistoryRepository.saveWorkout(
                    exerciseType     = exerciseType,
                    duration         = duration,
                    totalReps        = totalReps,
                    perfectFormReps  = perfectReps,
                    averageAngle     = avgAngle,
                    score            = score,
                    difficultyLevel  = difficulty,
                    repFeedbacks     = repFeedbacks
                )
            }

            val rating = when {
                score >= 85 -> 5
                score >= 70 -> 4
                score >= 55 -> 3
                score >= 40 -> 2
                else         -> 1
            }
            viewModel.submitRating(savedWorkoutId, rating)
        }
    }

    /**
     * Set target number of reps
     */
    private fun setTargetReps(target: Int) {
        targetReps = target
        updateProgressBar()
    }

    /**
     * Update the progress bar based on current/target reps
     */
    private fun updateProgressBar() {
        val dialog = statsDialog ?: return
        if (!dialog.isShowing) return

        val progressBar = dialog.findViewById<ProgressBar>(R.id.rep_progress) ?: return
        val totalReps   = viewModel.totalReps.value ?: 0

        if (targetReps > 0) {
            val progress = (totalReps * 100) / targetReps
            progressBar.max      = 100
            progressBar.progress = progress.coerceAtMost(100)
            dialog.findViewById<TextView>(R.id.value_target_reps)?.text = targetReps.toString()
        } else {
            progressBar.max      = 100
            progressBar.progress = 0
            dialog.findViewById<TextView>(R.id.value_target_reps)?.text = "--"
        }
    }

    /**
     * Show workout statistics dialog
     */
    fun showWorkoutStats() {
        if (statsDialog?.isShowing == true) {
            updateStatsDisplay()
            return
        }

        val builder    = AlertDialog.Builder(context)
        val dialogView = LayoutInflater.from(context).inflate(R.layout.workout_stats_dialog, null)
        setupDialogControls(dialogView)

        builder.setView(dialogView)
            .setTitle("Workout Dashboard")

        val dialog = builder.create()
        dialog.setOnDismissListener {
            collectingJob?.cancel()
            collectingJob = null
        }
        statsDialog = dialog
        dialog.show()
    }

    /**
     * Setup all the controls in the dashboard dialog
     */
    private fun setupDialogControls(dialogView: View) {
        val exerciseTypeText  = dialogView.findViewById<TextView>(R.id.value_exercise_type)
        val totalRepsText     = dialogView.findViewById<TextView>(R.id.value_total_reps)
        val perfectRepsText   = dialogView.findViewById<TextView>(R.id.value_perfect_reps)
        val avgAngleText      = dialogView.findViewById<TextView>(R.id.value_avg_angle)
        val difficultyText    = dialogView.findViewById<TextView>(R.id.value_difficulty)
        val targetRepsInput   = dialogView.findViewById<EditText>(R.id.edit_target_reps)
        val setTargetButton   = dialogView.findViewById<Button>(R.id.btn_set_target)
        val startButton       = dialogView.findViewById<Button>(R.id.btn_start_workout)
        val stopButton        = dialogView.findViewById<Button>(R.id.btn_stop_workout)
        val statusText        = dialogView.findViewById<TextView>(R.id.workout_status)
        val recommendationsRv = dialogView.findViewById<RecyclerView>(R.id.recommendation_list)
        val ratingBar         = dialogView.findViewById<RatingBar>(R.id.workout_rating)
        val submitButton      = dialogView.findViewById<Button>(R.id.btn_submit_rating)

        recommendationsRv.layoutManager = LinearLayoutManager(context)
        exerciseTypeText.text  = getExerciseTypeName(viewModel.currentExerciseType)
        totalRepsText.text     = viewModel.totalReps.value?.toString() ?: "0"
        perfectRepsText.text   = viewModel.perfectReps.value?.toString() ?: "0"
        avgAngleText.text      = viewModel.avgAngle.value?.let { String.format("%.1f°", it) } ?: "0°"
        difficultyText.text    = viewModel.difficulty.value?.toString() ?: "1"
        statusText.text        = if (isWorkoutActive) "Workout in progress" else "Ready to start"

        startButton.isEnabled = !isWorkoutActive
        stopButton.isEnabled  = isWorkoutActive

        viewModel.totalReps.observe(lifecycleOwner) { reps ->
            totalRepsText.text = reps.toString()
            updateProgressBar()
        }
        viewModel.perfectReps.observe(lifecycleOwner) { reps ->
            perfectRepsText.text = reps.toString()
        }
        viewModel.avgAngle.observe(lifecycleOwner) { angle ->
            avgAngleText.text = String.format("%.1f°", angle)
        }
        viewModel.difficulty.observe(lifecycleOwner) { diff ->
            difficultyText.text = diff.toString()
        }

        updateRecommendations(recommendationsRv)

        setTargetButton.setOnClickListener {
            val t = targetRepsInput.text.toString().toIntOrNull() ?: 0
            if (t > 0) {
                setTargetReps(t)
                Toast.makeText(context, "Target set to $t reps", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Please enter a valid number", Toast.LENGTH_SHORT).show()
            }
        }

        startButton.setOnClickListener { startWorkout() }
        stopButton.setOnClickListener  { stopWorkout() }

        submitButton.setOnClickListener {
            val rating = ratingBar.rating.toInt()
            if (rating > 0) {
                viewModel.submitRating(workoutId, rating)
                ratingBar.rating    = 0f
                submitButton.text   = "Thank you!"
                submitButton.isEnabled = false
                updateRecommendations(recommendationsRv)
            } else {
                Toast.makeText(context, "Please select a rating", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Refresh progress bar if dialog is visible
     */
    private fun updateStatsDisplay() {
        statsDialog?.let { dlg ->
            if (dlg.isShowing) updateProgressBar()
        }
    }

    /**
     * Populate the recommendations list
     */
    private fun updateRecommendations(recyclerView: RecyclerView) {
        val recs = viewModel.recommendations.value
        val adapter = RecommendationAdapter(recs) { recId ->
            val a    = recyclerView.adapter as? RecommendationAdapter ?: return@RecommendationAdapter
            val type = a.getExerciseType(recId)
            onExerciseSelected(type)
            statsDialog?.findViewById<TextView>(R.id.value_exercise_type)
                ?.text = getExerciseTypeName(type)
        }
        recyclerView.adapter = adapter
    }

    private fun getExerciseTypeName(exerciseType: ExerciseType): String =
        when (exerciseType) {
            ExerciseType.BICEP          -> "Bicep Curl"
            ExerciseType.SQUAT          -> "Squat"
            ExerciseType.LATERAL_RAISE  -> "Lateral Raise"
            ExerciseType.LUNGES         -> "Lunges"
            ExerciseType.SHOULDER_PRESS -> "Shoulder Press"
        }

    /**
     * Clean up resources
     */
    fun cleanup() {
        statsDialog?.dismiss()
        collectingJob?.cancel()
    }

    /**
     * Check if workout is active
     */
    fun isWorkoutActive(): Boolean = isWorkoutActive
}
