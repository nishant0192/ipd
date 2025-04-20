package com.google.mediapipe.examples.poselandmarker

import android.app.AlertDialog
import android.os.Bundle
import android.widget.RadioButton
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.fragment.NavHostFragment
import com.google.mediapipe.examples.poselandmarker.databinding.ActivityMainBinding
import com.google.mediapipe.examples.poselandmarker.fragment.CameraFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()
    private lateinit var drawerLayout: DrawerLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup toolbar and navigation drawer
        setSupportActionBar(binding.toolbar)
        drawerLayout = binding.drawerLayout
        
        // Setup NavigationView click listeners
        binding.navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_bicep -> {
                    viewModel.setExerciseType(ExerciseType.BICEP)
                    binding.toolbar.title = "Bicep Curl"
                    drawerLayout.closeDrawers()
                    true
                }
                R.id.menu_squat -> {
                    viewModel.setExerciseType(ExerciseType.SQUAT)
                    binding.toolbar.title = "Squat"
                    drawerLayout.closeDrawers()
                    true
                }
                R.id.menu_lateral_raise -> {
                    viewModel.setExerciseType(ExerciseType.LATERAL_RAISE)
                    binding.toolbar.title = "Lateral Raise"
                    drawerLayout.closeDrawers()
                    true
                }
                R.id.menu_lunges -> {
                    viewModel.setExerciseType(ExerciseType.LUNGES)
                    binding.toolbar.title = "Lunges"
                    drawerLayout.closeDrawers()
                    true
                }
                R.id.menu_shoulder_press -> {
                    viewModel.setExerciseType(ExerciseType.SHOULDER_PRESS)
                    binding.toolbar.title = "Shoulder Press"
                    drawerLayout.closeDrawers()
                    true
                }
                R.id.menu_model_settings -> {
                    // Show a dialog with model settings
                    showModelSettingsDialog()
                    drawerLayout.closeDrawers()
                    true
                }
                R.id.menu_toggle_camera -> {
                    // Toggle front/back camera
                    val cameraFragment = supportFragmentManager
                        .findFragmentById(R.id.fragment_container)
                        ?.childFragmentManager
                        ?.fragments
                        ?.firstOrNull() as? CameraFragment
                    cameraFragment?.toggleCamera()
                    drawerLayout.closeDrawers()
                    true
                }
                else -> false
            }
        }
        
        // Setup hamburger icon click
        binding.toolbar.setNavigationOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }
        
        // Initialize toolbar title based on current exercise
        updateToolbarTitle()
    }
    
    private fun updateToolbarTitle() {
        binding.toolbar.title = when (viewModel.currentExerciseType) {
            ExerciseType.BICEP -> "Bicep Curl"
            ExerciseType.SQUAT -> "Squat"
            ExerciseType.LATERAL_RAISE -> "Lateral Raise"
            ExerciseType.LUNGES -> "Lunges"
            ExerciseType.SHOULDER_PRESS -> "Shoulder Press"
        }
    }
    
    private fun showModelSettingsDialog() {
        val builder = AlertDialog.Builder(this)
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.dialog_model_settings, null)
        
        // Setup model selection
        val radioGroupModel = dialogView.findViewById<RadioGroup>(R.id.radio_group_model)
        when (viewModel.currentModel) {
            PoseLandmarkerHelper.MODEL_POSE_LANDMARKER_FULL -> 
                dialogView.findViewById<RadioButton>(R.id.radio_model_full).isChecked = true
            PoseLandmarkerHelper.MODEL_POSE_LANDMARKER_LITE -> 
                dialogView.findViewById<RadioButton>(R.id.radio_model_lite).isChecked = true
            PoseLandmarkerHelper.MODEL_POSE_LANDMARKER_HEAVY -> 
                dialogView.findViewById<RadioButton>(R.id.radio_model_heavy).isChecked = true
        }
        
        // Setup delegate selection
        val radioGroupDelegate = dialogView.findViewById<RadioGroup>(R.id.radio_group_delegate)
        when (viewModel.currentDelegate) {
            PoseLandmarkerHelper.DELEGATE_CPU -> 
                dialogView.findViewById<RadioButton>(R.id.radio_delegate_cpu).isChecked = true
            PoseLandmarkerHelper.DELEGATE_GPU -> 
                dialogView.findViewById<RadioButton>(R.id.radio_delegate_gpu).isChecked = true
        }
        
        // Setup detection confidence slider
        val seekbarDetection = dialogView.findViewById<SeekBar>(R.id.seekbar_detection)
        val textDetectionValue = dialogView.findViewById<TextView>(R.id.text_detection_value)
        seekbarDetection.progress = (viewModel.currentMinPoseDetectionConfidence * 100).toInt()
        textDetectionValue.text = viewModel.currentMinPoseDetectionConfidence.toString()
        seekbarDetection.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                val value = progress / 100f
                textDetectionValue.text = value.toString()
            }
            
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
        
        // Setup tracking confidence slider
        val seekbarTracking = dialogView.findViewById<SeekBar>(R.id.seekbar_tracking)
        val textTrackingValue = dialogView.findViewById<TextView>(R.id.text_tracking_value)
        seekbarTracking.progress = (viewModel.currentMinPoseTrackingConfidence * 100).toInt()
        textTrackingValue.text = viewModel.currentMinPoseTrackingConfidence.toString()
        seekbarTracking.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                val value = progress / 100f
                textTrackingValue.text = value.toString()
            }
            
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
        
        // Setup presence confidence slider
        val seekbarPresence = dialogView.findViewById<SeekBar>(R.id.seekbar_presence)
        val textPresenceValue = dialogView.findViewById<TextView>(R.id.text_presence_value)
        seekbarPresence.progress = (viewModel.currentMinPosePresenceConfidence * 100).toInt()
        textPresenceValue.text = viewModel.currentMinPosePresenceConfidence.toString()
        seekbarPresence.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                val value = progress / 100f
                textPresenceValue.text = value.toString()
            }
            
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
        
        // Set dialog buttons
        builder.setView(dialogView)
            .setTitle("Model Settings")
            .setPositiveButton("Apply") { dialog, _ ->
                // Get model selection
                val modelId = when {
                    dialogView.findViewById<RadioButton>(R.id.radio_model_full).isChecked -> 
                        PoseLandmarkerHelper.MODEL_POSE_LANDMARKER_FULL
                    dialogView.findViewById<RadioButton>(R.id.radio_model_lite).isChecked -> 
                        PoseLandmarkerHelper.MODEL_POSE_LANDMARKER_LITE
                    dialogView.findViewById<RadioButton>(R.id.radio_model_heavy).isChecked -> 
                        PoseLandmarkerHelper.MODEL_POSE_LANDMARKER_HEAVY
                    else -> viewModel.currentModel
                }
                
                // Get delegate selection
                val delegateId = when {
                    dialogView.findViewById<RadioButton>(R.id.radio_delegate_cpu).isChecked -> 
                        PoseLandmarkerHelper.DELEGATE_CPU
                    dialogView.findViewById<RadioButton>(R.id.radio_delegate_gpu).isChecked -> 
                        PoseLandmarkerHelper.DELEGATE_GPU
                    else -> viewModel.currentDelegate
                }
                
                // Get threshold values
                val detectionConfidence = seekbarDetection.progress / 100f
                val trackingConfidence = seekbarTracking.progress / 100f
                val presenceConfidence = seekbarPresence.progress / 100f
                
                // Update ViewModel
                viewModel.setModel(modelId)
                viewModel.setDelegate(delegateId)
                viewModel.setMinPoseDetectionConfidence(detectionConfidence)
                viewModel.setMinPoseTrackingConfidence(trackingConfidence)
                viewModel.setMinPosePresenceConfidence(presenceConfidence)
                
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.cancel()
            }
        
        builder.create().show()
    }
}