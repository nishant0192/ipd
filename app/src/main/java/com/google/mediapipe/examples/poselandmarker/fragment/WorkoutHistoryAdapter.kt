package com.google.mediapipe.examples.poselandmarker.fragment

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.mediapipe.examples.poselandmarker.ExerciseType
import com.google.mediapipe.examples.poselandmarker.R
import com.google.mediapipe.examples.poselandmarker.history.WorkoutResult
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Adapter for displaying workout history in a RecyclerView
 */
class WorkoutHistoryAdapter(
    private val onWorkoutSelected: (String) -> Unit
) : ListAdapter<WorkoutRecord, WorkoutHistoryAdapter.WorkoutViewHolder>(DIFF_CALLBACK) {

    class WorkoutViewHolder(
        itemView: View,
        private val onWorkoutSelected: (String) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        
        private val exerciseIcon: ImageView = itemView.findViewById(R.id.workout_exercise_icon)
        private val exerciseName: TextView = itemView.findViewById(R.id.workout_exercise_name)
        private val workoutDate: TextView = itemView.findViewById(R.id.workout_date)
        private val repCount: TextView = itemView.findViewById(R.id.workout_rep_count)
        private val duration: TextView = itemView.findViewById(R.id.workout_duration)
        private val scoreText: TextView = itemView.findViewById(R.id.workout_score)
        
        private val dateFormatter = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault())
        
        fun bind(workout: WorkoutRecord) {
            // Set exercise icon and name
            exerciseIcon.setImageResource(getExerciseIcon(workout.exerciseType))
            exerciseName.text = getExerciseName(workout.exerciseType)
            
            // Format and set date
            workoutDate.text = dateFormatter.format(Date(workout.timestamp))
            
            // Set rep count
            repCount.text = "${workout.totalReps} reps"
            
            // Format and set duration
            val minutes = workout.duration / 60000
            val seconds = (workout.duration % 60000) / 1000
            duration.text = String.format("%02d:%02d", minutes, seconds)
            
            // Set score with color based on value
            scoreText.text = "${workout.score}/100"
            scoreText.setTextColor(getScoreColor(workout.score))
            
            // Set click listener
            itemView.setOnClickListener {
                onWorkoutSelected(workout.id)
            }
        }
        
        private fun getExerciseName(exerciseType: ExerciseType): String {
            return when (exerciseType) {
                ExerciseType.BICEP -> "Bicep Curl"
                ExerciseType.SQUAT -> "Squat"
                ExerciseType.LATERAL_RAISE -> "Lateral Raise"
                ExerciseType.LUNGES -> "Lunges"
                ExerciseType.SHOULDER_PRESS -> "Shoulder Press"
            }
        }
        
        private fun getExerciseIcon(exerciseType: ExerciseType): Int {
            return when (exerciseType) {
                ExerciseType.BICEP -> R.drawable.ic_bicep
                ExerciseType.SQUAT -> R.drawable.ic_squat
                ExerciseType.LATERAL_RAISE -> R.drawable.ic_lateral_raise
                ExerciseType.LUNGES -> R.drawable.ic_lunges
                ExerciseType.SHOULDER_PRESS -> R.drawable.ic_shoulder_press
            }
        }
        
        private fun getScoreColor(score: Int): Int {
            val context = itemView.context
            return when {
                score >= 90 -> ContextCompat.getColor(context, R.color.green_grade)
                score >= 80 -> ContextCompat.getColor(context, R.color.blue_grade)
                score >= 70 -> ContextCompat.getColor(context, R.color.yellow_grade)
                score >= 60 -> ContextCompat.getColor(context, R.color.orange_grade)
                else -> ContextCompat.getColor(context, R.color.red_grade)
            }
        }
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WorkoutViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_workout_history, parent, false)
        return WorkoutViewHolder(view, onWorkoutSelected)
    }
    
    override fun onBindViewHolder(holder: WorkoutViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<WorkoutRecord>() {
            override fun areItemsTheSame(oldItem: WorkoutRecord, newItem: WorkoutRecord): Boolean {
                return oldItem.id == newItem.id
            }
            
            override fun areContentsTheSame(oldItem: WorkoutRecord, newItem: WorkoutRecord): Boolean {
                return oldItem == newItem
            }
        }
    }
}