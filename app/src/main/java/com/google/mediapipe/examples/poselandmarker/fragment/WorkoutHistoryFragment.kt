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
import com.google.android.material.card.MaterialCardView
import com.google.mediapipe.examples.poselandmarker.ExerciseType
import com.google.mediapipe.examples.poselandmarker.R
import com.google.mediapipe.examples.poselandmarker.history.WorkoutHistoryDatabase
import com.google.mediapipe.examples.poselandmarker.history.WorkoutHistoryRepository
import com.google.mediapipe.examples.poselandmarker.history.WorkoutEntity
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