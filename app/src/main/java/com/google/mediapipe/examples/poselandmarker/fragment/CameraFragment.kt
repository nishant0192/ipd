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

    // Shared ViewModel from MainActivity.
    private val viewModel: MainViewModel by activityViewModels()

    private lateinit var poseLandmarkerHelper: PoseLandmarkerHelper

    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null

    // Use front camera by default.
    private var cameraFacing = CameraSelector.LENS_FACING_FRONT

    // Background executor for ML ops.
    private lateinit var backgroundExecutor: ExecutorService

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _fragmentCameraBinding = FragmentCameraBinding.inflate(inflater, container, false)
        return fragmentCameraBinding.root
    }

    @SuppressLint("UnsafeOptInUsageError")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        backgroundExecutor = Executors.newSingleThreadExecutor()

        // Set up camera when view is ready.
        fragmentCameraBinding.viewFinder.post {
            setUpCamera()
        }

        // Initialize PoseLandmarkerHelper on background thread.
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

        // Initialize bottom sheet controls.
        initBottomSheetControls()

        // Set up switch camera button.
        fragmentCameraBinding.switchCamera.setOnClickListener {
            toggleCamera()
        }
    }

    override fun onResume() {
        super.onResume()
        backgroundExecutor.execute {
            if (this::poseLandmarkerHelper.isInitialized && poseLandmarkerHelper.isClose()) {
                poseLandmarkerHelper.setupPoseLandmarker()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        if (this::poseLandmarkerHelper.isInitialized) {
            viewModel.setMinPoseDetectionConfidence(poseLandmarkerHelper.minPoseDetectionConfidence)
            viewModel.setMinPoseTrackingConfidence(poseLandmarkerHelper.minPoseTrackingConfidence)
            viewModel.setMinPosePresenceConfidence(poseLandmarkerHelper.minPosePresenceConfidence)
            viewModel.setDelegate(poseLandmarkerHelper.currentDelegate)

            backgroundExecutor.execute { poseLandmarkerHelper.clearPoseLandmarker() }
        }
    }

    override fun onDestroyView() {
        _fragmentCameraBinding = null
        super.onDestroyView()
        backgroundExecutor.shutdown()
        backgroundExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS)
    }

    private fun initBottomSheetControls() {
        val bottomSheet = fragmentCameraBinding.bottomSheetLayout

        bottomSheet.detectionThresholdValue.text = String.format(
            Locale.US, "%.2f", viewModel.currentMinPoseDetectionConfidence
        )
        bottomSheet.trackingThresholdValue.text = String.format(
            Locale.US, "%.2f", viewModel.currentMinPoseTrackingConfidence
        )
        bottomSheet.presenceThresholdValue.text = String.format(
            Locale.US, "%.2f", viewModel.currentMinPosePresenceConfidence
        )

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

        bottomSheet.spinnerModel.setSelection(viewModel.currentModel, false)
        bottomSheet.spinnerModel.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                poseLandmarkerHelper.currentModel = position
                updateControlsUi()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        bottomSheet.spinnerDelegate.setSelection(viewModel.currentDelegate, false)
        bottomSheet.spinnerDelegate.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                poseLandmarkerHelper.currentDelegate = position
                updateControlsUi()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        bottomSheet.spinnerExercise.setSelection(
            when (viewModel.currentExerciseType) {
                ExerciseType.BICEP -> 0
                ExerciseType.SQUAT -> 1
                ExerciseType.LATERAL_RAISE -> 2
                ExerciseType.LUNGES -> 3
                ExerciseType.SHOULDER_PRESS -> 4
            }, false
        )
        bottomSheet.spinnerExercise.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, v: View?, position: Int, id: Long) {
                val newExercise = when (position) {
                    0 -> ExerciseType.BICEP
                    1 -> ExerciseType.SQUAT
                    2 -> ExerciseType.LATERAL_RAISE
                    3 -> ExerciseType.LUNGES
                    4 -> ExerciseType.SHOULDER_PRESS
                    else -> ExerciseType.BICEP
                }
                viewModel.setExerciseType(newExercise)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun updateControlsUi() {
        fragmentCameraBinding.bottomSheetLayout.detectionThresholdValue.text =
            String.format(Locale.US, "%.2f", poseLandmarkerHelper.minPoseDetectionConfidence)
        fragmentCameraBinding.bottomSheetLayout.trackingThresholdValue.text =
            String.format(Locale.US, "%.2f", poseLandmarkerHelper.minPoseTrackingConfidence)
        fragmentCameraBinding.bottomSheetLayout.presenceThresholdValue.text =
            String.format(Locale.US, "%.2f", poseLandmarkerHelper.minPosePresenceConfidence)

        backgroundExecutor.execute {
            poseLandmarkerHelper.clearPoseLandmarker()
            poseLandmarkerHelper.setupPoseLandmarker()
        }
        fragmentCameraBinding.overlay.clear()
    }

    private fun toggleCamera() {
        cameraFacing = if (cameraFacing == CameraSelector.LENS_FACING_FRONT)
            CameraSelector.LENS_FACING_BACK
        else
            CameraSelector.LENS_FACING_FRONT
        setUpCamera()
    }

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

        preview = Preview.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setTargetRotation(fragmentCameraBinding.viewFinder.display.rotation)
            .build()

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

    override fun onResults(resultBundle: PoseLandmarkerHelper.ResultBundle) {
        requireActivity().runOnUiThread {
            fragmentCameraBinding.bottomSheetLayout.inferenceTimeVal.text =
                String.format("%d ms", resultBundle.inferenceTime)
            fragmentCameraBinding.overlay.setExerciseType(viewModel.currentExerciseType)
            fragmentCameraBinding.overlay.setResults(
                poseLandmarkerResults = resultBundle.results.first(),
                imageHeight = resultBundle.inputImageHeight,
                imageWidth = resultBundle.inputImageWidth,
                runningMode = RunningMode.LIVE_STREAM
            )
            fragmentCameraBinding.overlay.invalidate()
        }
    }

    override fun onError(error: String, errorCode: Int) {
        requireActivity().runOnUiThread {
            Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
            if (errorCode == PoseLandmarkerHelper.GPU_ERROR) {
                fragmentCameraBinding.bottomSheetLayout.spinnerDelegate.setSelection(
                    PoseLandmarkerHelper.DELEGATE_CPU, false
                )
            }
        }
    }
}
