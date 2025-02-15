package com.google.mediapipe.examples.poselandmarker.fragment

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.Navigation
import com.google.mediapipe.examples.poselandmarker.*
import com.google.mediapipe.examples.poselandmarker.databinding.FragmentCameraBinding
import com.google.mediapipe.tasks.vision.core.RunningMode
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class CameraFragment : Fragment(), PoseLandmarkerHelper.LandmarkerListener {

    companion object {
        private const val TAG = "Pose Landmarker"
    }

    private var _fragmentCameraBinding: FragmentCameraBinding? = null
    private val fragmentCameraBinding get() = _fragmentCameraBinding!!

    // Shared ViewModel from MainActivity
    private val viewModel: MainViewModel by activityViewModels()

    private lateinit var poseLandmarkerHelper: PoseLandmarkerHelper

    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var cameraFacing = CameraSelector.LENS_FACING_BACK

    // Background executor for ML ops
    private lateinit var backgroundExecutor: ExecutorService

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _fragmentCameraBinding = FragmentCameraBinding.inflate(inflater, container, false)
        return fragmentCameraBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize executor
        backgroundExecutor = Executors.newSingleThreadExecutor()

        // Set up camera after the view is laid out
        fragmentCameraBinding.viewFinder.post {
            setUpCamera()
        }

        // Create (or re-create) the PoseLandmarkerHelper on a background thread
        backgroundExecutor.execute {
            poseLandmarkerHelper = PoseLandmarkerHelper(
                context = requireContext(),
                runningMode = RunningMode.LIVE_STREAM,
                minPoseDetectionConfidence = viewModel.currentMinPoseDetectionConfidence,
                minPoseTrackingConfidence = viewModel.currentMinPoseTrackingConfidence,
                minPosePresenceConfidence = viewModel.currentMinPosePresenceConfidence,
                currentDelegate = viewModel.currentDelegate,
                poseLandmarkerHelperListener = this
            )
        }

        // Initialize UI controls (threshold sliders, spinners, etc.)
        initBottomSheetControls()
    }

    override fun onResume() {
        super.onResume()
        // Check permissions
        // if (!PermissionsFragment.hasPermissions(requireContext())) {
        //     Navigation.findNavController(
        //         requireActivity(), R.id.fragment_container
        //     ).navigate(R.id.action_camera_to_permissions)
        // }

        // Re-init poseLandmarkerHelper if needed
        backgroundExecutor.execute {
            if (this::poseLandmarkerHelper.isInitialized && poseLandmarkerHelper.isClose()) {
                poseLandmarkerHelper.setupPoseLandmarker()
            }
        }
    }

    override fun onPause() {
        super.onPause()

        // Save current thresholds to ViewModel
        if (this::poseLandmarkerHelper.isInitialized) {
            viewModel.setMinPoseDetectionConfidence(poseLandmarkerHelper.minPoseDetectionConfidence)
            viewModel.setMinPoseTrackingConfidence(poseLandmarkerHelper.minPoseTrackingConfidence)
            viewModel.setMinPosePresenceConfidence(poseLandmarkerHelper.minPosePresenceConfidence)
            viewModel.setDelegate(poseLandmarkerHelper.currentDelegate)

            // Close resources
            backgroundExecutor.execute { poseLandmarkerHelper.clearPoseLandmarker() }
        }
    }

    override fun onDestroyView() {
        _fragmentCameraBinding = null
        super.onDestroyView()

        // Shutdown executor
        backgroundExecutor.shutdown()
        backgroundExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS)
    }

    // Set up bottom sheet controls (thresholds, model spinner, delegate spinner, exercise spinner)
    private fun initBottomSheetControls() {
        val bottomSheet = fragmentCameraBinding.bottomSheetLayout

        // Show current threshold values
        bottomSheet.detectionThresholdValue.text = String.format(
            Locale.US, "%.2f", viewModel.currentMinPoseDetectionConfidence
        )
        bottomSheet.trackingThresholdValue.text = String.format(
            Locale.US, "%.2f", viewModel.currentMinPoseTrackingConfidence
        )
        bottomSheet.presenceThresholdValue.text = String.format(
            Locale.US, "%.2f", viewModel.currentMinPosePresenceConfidence
        )

        // Adjust detection threshold
        bottomSheet.detectionThresholdMinus.setOnClickListener {
            if (poseLandmarkerHelper.minPoseDetectionConfidence >= 0.2f) {
                poseLandmarkerHelper.minPoseDetectionConfidence -= 0.1f
                updateControlsUi()
            }
        }
        bottomSheet.detectionThresholdPlus.setOnClickListener {
            if (poseLandmarkerHelper.minPoseDetectionConfidence <= 0.8f) {
                poseLandmarkerHelper.minPoseDetectionConfidence += 0.1f
                updateControlsUi()
            }
        }

        // Adjust tracking threshold
        bottomSheet.trackingThresholdMinus.setOnClickListener {
            if (poseLandmarkerHelper.minPoseTrackingConfidence >= 0.2f) {
                poseLandmarkerHelper.minPoseTrackingConfidence -= 0.1f
                updateControlsUi()
            }
        }
        bottomSheet.trackingThresholdPlus.setOnClickListener {
            if (poseLandmarkerHelper.minPoseTrackingConfidence <= 0.8f) {
                poseLandmarkerHelper.minPoseTrackingConfidence += 0.1f
                updateControlsUi()
            }
        }

        // Adjust presence threshold
        bottomSheet.presenceThresholdMinus.setOnClickListener {
            if (poseLandmarkerHelper.minPosePresenceConfidence >= 0.2f) {
                poseLandmarkerHelper.minPosePresenceConfidence -= 0.1f
                updateControlsUi()
            }
        }
        bottomSheet.presenceThresholdPlus.setOnClickListener {
            if (poseLandmarkerHelper.minPosePresenceConfidence <= 0.8f) {
                poseLandmarkerHelper.minPosePresenceConfidence += 0.1f
                updateControlsUi()
            }
        }

        // Model spinner
        bottomSheet.spinnerModel.setSelection(viewModel.currentModel, false)
        bottomSheet.spinnerModel.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                poseLandmarkerHelper.currentModel = position
                updateControlsUi()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Delegate spinner
        bottomSheet.spinnerDelegate.setSelection(viewModel.currentDelegate, false)
        bottomSheet.spinnerDelegate.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                poseLandmarkerHelper.currentDelegate = position
                updateControlsUi()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // **Exercise spinner** (Biceps vs. Squats)
        // We'll read the current exercise from the ViewModel and set the spinner initially
        // Suppose "0 -> Bicep", "1 -> Squat"
        bottomSheet.spinnerExercise.setSelection(
            when(viewModel.currentExerciseType) {
                ExerciseType.BICEP -> 0
                ExerciseType.SQUAT -> 1
                ExerciseType.LATERAL_RAISE -> 2
            }, 
            false
        )
        bottomSheet.spinnerExercise.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, v: View?, position: Int, id: Long) {
                val newExercise = when(position) {
                    0 -> ExerciseType.BICEP
                    1 -> ExerciseType.SQUAT
                    else -> ExerciseType.BICEP
                }
                viewModel.setExerciseType(newExercise)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    // Re-initialize the PoseLandmarker when any setting changes
    private fun updateControlsUi() {
        fragmentCameraBinding.bottomSheetLayout.detectionThresholdValue.text =
            String.format(Locale.US, "%.2f", poseLandmarkerHelper.minPoseDetectionConfidence)
        fragmentCameraBinding.bottomSheetLayout.trackingThresholdValue.text =
            String.format(Locale.US, "%.2f", poseLandmarkerHelper.minPoseTrackingConfidence)
        fragmentCameraBinding.bottomSheetLayout.presenceThresholdValue.text =
            String.format(Locale.US, "%.2f", poseLandmarkerHelper.minPosePresenceConfidence)

        // Clear existing landmarker, re-create it
        backgroundExecutor.execute {
            poseLandmarkerHelper.clearPoseLandmarker()
            poseLandmarkerHelper.setupPoseLandmarker()
        }
        // Clear the overlay
        fragmentCameraBinding.overlay.clear()
    }

    // Set up CameraX
    private fun setUpCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            bindCameraUseCases()
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun bindCameraUseCases() {
        val cameraProvider = cameraProvider ?: return
        val cameraSelector = CameraSelector.Builder().requireLensFacing(cameraFacing).build()

        // Preview
        preview = Preview.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setTargetRotation(fragmentCameraBinding.viewFinder.display.rotation)
            .build()

        // ImageAnalysis
        imageAnalyzer = ImageAnalysis.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setTargetRotation(fragmentCameraBinding.viewFinder.display.rotation)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build()
            .also { analysis ->
                analysis.setAnalyzer(backgroundExecutor) { imageProxy ->
                    detectPose(imageProxy)
                }
            }

        cameraProvider.unbindAll()
        try {
            camera = cameraProvider.bindToLifecycle(
                this, cameraSelector, preview, imageAnalyzer
            )
            preview?.setSurfaceProvider(fragmentCameraBinding.viewFinder.surfaceProvider)
        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }

    private fun detectPose(imageProxy: ImageProxy) {
        if (this::poseLandmarkerHelper.isInitialized) {
            poseLandmarkerHelper.detectLiveStream(
                imageProxy,
                cameraFacing == CameraSelector.LENS_FACING_FRONT
            )
        } else {
            imageProxy.close()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        imageAnalyzer?.targetRotation = fragmentCameraBinding.viewFinder.display.rotation
    }

    // Called when there's a new detection result
    override fun onResults(resultBundle: PoseLandmarkerHelper.ResultBundle) {
        requireActivity().runOnUiThread {
            // Show inference time
            fragmentCameraBinding.bottomSheetLayout.inferenceTimeVal.text =
                String.format("%d ms", resultBundle.inferenceTime)

            // FIRST, tell OverlayView which exercise type we want
            fragmentCameraBinding.overlay.setExerciseType(viewModel.currentExerciseType)

            // THEN, pass the new detection results
            fragmentCameraBinding.overlay.setResults(
                poseLandmarkerResults = resultBundle.results.first(),
                imageHeight = resultBundle.inputImageHeight,
                imageWidth = resultBundle.inputImageWidth,
                runningMode = RunningMode.LIVE_STREAM
            )
            // Force a redraw
            fragmentCameraBinding.overlay.invalidate()
        }
    }

    override fun onError(error: String, errorCode: Int) {
        requireActivity().runOnUiThread {
            Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
            if (errorCode == PoseLandmarkerHelper.GPU_ERROR) {
                // Fall back to CPU
                fragmentCameraBinding.bottomSheetLayout.spinnerDelegate.setSelection(
                    PoseLandmarkerHelper.DELEGATE_CPU, false
                )
            }
        }
    }
}
