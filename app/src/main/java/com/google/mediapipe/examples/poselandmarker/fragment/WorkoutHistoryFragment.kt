package com.google.mediapipe.examples.poselandmarker.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.mediapipe.examples.poselandmarker.R
import com.google.mediapipe.examples.poselandmarker.history.WorkoutHistoryDatabase
import com.google.mediapipe.examples.poselandmarker.history.WorkoutHistoryRepository
import com.google.mediapipe.examples.poselandmarker.history.WorkoutRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Fragment to display workout history
 */
class WorkoutHistoryFragment : Fragment() {

    private lateinit var workoutHistoryRepository: WorkoutHistoryRepository
    private lateinit var adapter: WorkoutHistoryAdapter
    private lateinit var emptyStateText: TextView
    private lateinit var recyclerView: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_workout_history, container, false)
        
        // Initialize views
        recyclerView = view.findViewById(R.id.workout_history_recycler)
        emptyStateText = view.findViewById(R.id.empty_state_text)
        
        // Initialize repository
        val db = WorkoutHistoryDatabase.getDatabase(requireContext())
        workoutHistoryRepository = WorkoutHistoryRepository(db.workoutHistoryDao())
        
        // Setup RecyclerView
        adapter = WorkoutHistoryAdapter { workoutId ->
            // Handle workout selection - open details
            showWorkoutDetailsDialog(workoutId)
        }
        
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
        
        // Load workout history
        loadWorkoutHistory()
        
        return view
    }
    
    /**
     * Load workout history from database
     */
    private fun loadWorkoutHistory() {
        lifecycleScope.launch {
            // Show loading state
            emptyStateText.text = "Loading workouts..."
            emptyStateText.visibility = View.VISIBLE
            
            // Get workouts from database
            val workouts = withContext(Dispatchers.IO) {
                workoutHistoryRepository.getAllWorkouts()
            }
            
            // Update UI
            if (workouts.isEmpty()) {
                emptyStateText.text = "No workout history found. Complete a workout to see it here!"
                emptyStateText.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
            } else {
                emptyStateText.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
                adapter.submitList(workouts)
            }
        }
    }
    
    /**
     * Show detailed dialog for a specific workout
     */
    private fun showWorkoutDetailsDialog(workoutId: String) {
        lifecycleScope.launch {
            // Get workout details
            val (workout, repDetails) = withContext(Dispatchers.IO) {
                workoutHistoryRepository.getWorkoutWithDetails(workoutId)
            }
            
            if (workout != null) {
                // Create and show dialog with workout details
                val dialog = WorkoutDetailsDialog.newInstance(workoutId)
                dialog.show(parentFragmentManager, "workout_details")
            }
        }
    }
}

/**
 * Adapter for WorkoutHistoryFragment's RecyclerView
 */
class WorkoutHistoryAdapter(
    private val onWorkoutSelected: (String) -> Unit
) : RecyclerView.Adapter<WorkoutHistoryAdapter.WorkoutViewHolder>() {

    private var workoutList: List<WorkoutRecord> = emptyList()

    fun submitList(workouts: List<WorkoutRecord>) {
        this.workoutList = workouts
        notifyDataSetChanged()
    }

    class WorkoutViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val exerciseNameTextView: TextView = itemView.findViewById(R.id.workout_exercise_name)
        val dateTextView: TextView = itemView.findViewById(R.id.workout_date)
        val repCountTextView: TextView = itemView.findViewById(R.id.workout_rep_count)
        val durationTextView: TextView = itemView.findViewById(R.id.workout_duration)
        val scoreTextView: TextView = itemView.findViewById(R.id.workout_score)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WorkoutViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_workout_history, parent, false)
        return WorkoutViewHolder(view)
    }

    override fun onBindViewHolder(holder: WorkoutViewHolder, position: Int) {
        val workout = workoutList[position]
        
        // Set exercise name
        holder.exerciseNameTextView.text = when(workout.exerciseType) {
            com.google.mediapipe.examples.poselandmarker.ExerciseType.BICEP -> "Bicep Curl"
            com.google.mediapipe.examples.poselandmarker.ExerciseType.SQUAT -> "Squat"
            com.google.mediapipe.examples.poselandmarker.ExerciseType.LATERAL_RAISE -> "Lateral Raise"
            com.google.mediapipe.examples.poselandmarker.ExerciseType.LUNGES -> "Lunges"
            com.google.mediapipe.examples.poselandmarker.ExerciseType.SHOULDER_PRESS -> "Shoulder Press"
        }
        
        // Format and set date
        val dateFormat = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault())
        holder.dateTextView.text = dateFormat.format(Date(workout.timestamp))
        
        // Set rep count
        holder.repCountTextView.text = "${workout.totalReps} reps"
        
        // Format and set duration
        val minutes = workout.duration / 60000
        val seconds = (workout.duration % 60000) / 1000
        holder.durationTextView.text = String.format("%02d:%02d", minutes, seconds)
        
        // Set score
        holder.scoreTextView.text = "${workout.score}/100"
        
        // Set click listener
        holder.itemView.setOnClickListener {
            onWorkoutSelected(workout.id)
        }
    }

    override fun getItemCount() = workoutList.size
}