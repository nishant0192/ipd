/*
 * Copyright 2023 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.mediapipe.examples.poselandmarker

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.PI

/**
 * A simple helper class that “wraps” a landmark object and uses reflection
 * to call its x() and y() methods. (Your dependency’s landmark type does have these methods.)
 */
private class LandmarkWrapper(val landmark: Any) {
  val x: Float
    get() = landmark.javaClass.getMethod("x").invoke(landmark) as Float
  val y: Float
    get() = landmark.javaClass.getMethod("y").invoke(landmark) as Float
}

/**
 * OverlayView draws the detected pose landmarks, computes elbow angles,
 * counts “reps” for each arm, and displays the angles and rep counts.
 *
 * This version does not require an explicit landmark type. Instead, it treats each landmark
 * as an opaque object and uses [LandmarkWrapper] (via reflection) to access its coordinates.
 */
class OverlayView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {

    // Latest pose detection result.
    private var results: PoseLandmarkerResult? = null

    // Paint objects for drawing points, lines, and text.
    private var pointPaint = Paint()
    private var linePaint = Paint()
    private var textPaint = Paint()

    // Dimensions and scaling.
    private var scaleFactor: Float = 1f
    private var imageWidth: Int = 1
    private var imageHeight: Int = 1

    // Exercise thresholds.
    private val CORRECT_ANGLE_MIN = 70f
    private val CORRECT_ANGLE_MAX = 160f

    // Rep counter variables.
    private var repCountLeft = 0
    private var repCountRight = 0
    private var stageLeft: String? = null  // expected values: "up" or "down"
    private var stageRight: String? = null
    private var repFlagLeft = false
    private var repFlagRight = false
    private val angleHistoryLeft = mutableListOf<Float>()
    private val angleHistoryRight = mutableListOf<Float>()

    init {
        initPaints()
    }

    fun clear() {
        results = null
        repCountLeft = 0
        repCountRight = 0
        stageLeft = null
        stageRight = null
        repFlagLeft = false
        repFlagRight = false
        angleHistoryLeft.clear()
        angleHistoryRight.clear()
        pointPaint.reset()
        linePaint.reset()
        textPaint.reset()
        initPaints()
        invalidate()
    }

    private fun initPaints() {
        // Initialize line paint.
        linePaint.color = ContextCompat.getColor(context!!, R.color.mp_color_primary)
        linePaint.strokeWidth = LANDMARK_STROKE_WIDTH
        linePaint.style = Paint.Style.STROKE

        // Initialize point paint.
        pointPaint.color = Color.YELLOW
        pointPaint.strokeWidth = LANDMARK_STROKE_WIDTH
        pointPaint.style = Paint.Style.FILL

        // Initialize text paint.
        textPaint.color = Color.WHITE
        textPaint.textSize = 40f
        textPaint.style = Paint.Style.FILL
    }

    /**
     * Calculates the angle (in degrees) at point [b] formed by the line segments ab and bc.
     *
     * The parameters [a], [b], and [c] are landmark objects (of unknown compile‑time type).
     * We wrap them with [LandmarkWrapper] to access their x and y coordinates.
     */
    private fun calculateAngle(a: Any, b: Any, c: Any): Float {
        val A = LandmarkWrapper(a)
        val B = LandmarkWrapper(b)
        val C = LandmarkWrapper(c)
        val radians = atan2(C.y - B.y, C.x - B.x) - atan2(A.y - B.y, A.x - B.x)
        var angle = abs(radians * 180.0 / PI).toFloat()
        if (angle > 180f) {
            angle = 360 - angle
        }
        return angle
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        results?.let { poseLandmarkerResult ->
            if (poseLandmarkerResult.landmarks().isNotEmpty()) {
                // Assume a single person is detected.
                val personLandmarks = poseLandmarkerResult.landmarks()[0]
                // Draw every landmark as a point.
                for (landmark in personLandmarks) {
                    val lw = LandmarkWrapper(landmark)
                    canvas.drawPoint(
                        lw.x * imageWidth * scaleFactor,
                        lw.y * imageHeight * scaleFactor,
                        pointPaint
                    )
                }

                // Ensure that there are enough landmarks to compute arm angles.
                if (personLandmarks.size > 16) {
                    // Landmark indices (based on MediaPipe Pose):
                    // Left: shoulder (11), elbow (13), wrist (15)
                    // Right: shoulder (12), elbow (14), wrist (16)
                    val leftShoulder = personLandmarks[11]
                    val leftElbow = personLandmarks[13]
                    val leftWrist = personLandmarks[15]
                    val rightShoulder = personLandmarks[12]
                    val rightElbow = personLandmarks[14]
                    val rightWrist = personLandmarks[16]

                    // Compute elbow angles.
                    val leftAngle = calculateAngle(leftShoulder, leftElbow, leftWrist)
                    val rightAngle = calculateAngle(rightShoulder, rightElbow, rightWrist)

                    // Record angle history (for potential feedback).
                    angleHistoryLeft.add(leftAngle)
                    angleHistoryRight.add(rightAngle)

                    // Determine color based on whether the angle is within the correct range.
                    val leftColor = if (leftAngle in CORRECT_ANGLE_MIN..CORRECT_ANGLE_MAX) Color.GREEN else Color.RED
                    val rightColor = if (rightAngle in CORRECT_ANGLE_MIN..CORRECT_ANGLE_MAX) Color.GREEN else Color.RED

                    // Rep counting logic for the left arm.
                    if (leftAngle > CORRECT_ANGLE_MAX) {
                        if (stageLeft == "up" && repFlagLeft) {
                            repFlagLeft = false
                        }
                        stageLeft = "down"
                    }
                    if (leftAngle < CORRECT_ANGLE_MIN) {
                        if (stageLeft == "down" && !repFlagLeft) {
                            stageLeft = "up"
                            repCountLeft++
                            repFlagLeft = true
                        }
                    }

                    // Rep counting logic for the right arm.
                    if (rightAngle > CORRECT_ANGLE_MAX) {
                        if (stageRight == "up" && repFlagRight) {
                            repFlagRight = false
                        }
                        stageRight = "down"
                    }
                    if (rightAngle < CORRECT_ANGLE_MIN) {
                        if (stageRight == "down" && !repFlagRight) {
                            stageRight = "up"
                            repCountRight++
                            repFlagRight = true
                        }
                    }

                    // Draw limbs for left and right arms.
                    drawLimb(canvas, leftShoulder, leftElbow, leftWrist, leftColor)
                    drawLimb(canvas, rightShoulder, rightElbow, rightWrist, rightColor)

                    // Draw the angle values near the elbows.
                    val leftElbowWrap = LandmarkWrapper(leftElbow)
                    val rightElbowWrap = LandmarkWrapper(rightElbow)
                    canvas.drawText(
                        "Left Angle: ${leftAngle.toInt()}°",
                        leftElbowWrap.x * imageWidth * scaleFactor,
                        leftElbowWrap.y * imageHeight * scaleFactor - 20,
                        textPaint
                    )
                    canvas.drawText(
                        "Right Angle: ${rightAngle.toInt()}°",
                        rightElbowWrap.x * imageWidth * scaleFactor,
                        rightElbowWrap.y * imageHeight * scaleFactor - 20,
                        textPaint
                    )

                    // Draw the rep counters.
                    canvas.drawText("Left Reps: $repCountLeft", 50f, 50f, textPaint)
                    canvas.drawText("Right Reps: $repCountRight", 50f, 100f, textPaint)
                }
            }
        }
    }

    /**
     * Draws a limb defined by [shoulder], [elbow], and [wrist] with the specified [color].
     * The landmark objects are wrapped to retrieve their x and y coordinates.
     */
    private fun drawLimb(canvas: Canvas, shoulder: Any, elbow: Any, wrist: Any, color: Int) {
        linePaint.color = color
        pointPaint.color = color

        val s = LandmarkWrapper(shoulder)
        val e = LandmarkWrapper(elbow)
        val w = LandmarkWrapper(wrist)

        val shoulderX = s.x * imageWidth * scaleFactor
        val shoulderY = s.y * imageHeight * scaleFactor
        val elbowX = e.x * imageWidth * scaleFactor
        val elbowY = e.y * imageHeight * scaleFactor
        val wristX = w.x * imageWidth * scaleFactor
        val wristY = w.y * imageHeight * scaleFactor

        canvas.drawLine(shoulderX, shoulderY, elbowX, elbowY, linePaint)
        canvas.drawLine(elbowX, elbowY, wristX, wristY, linePaint)
        canvas.drawCircle(shoulderX, shoulderY, 8f, pointPaint)
        canvas.drawCircle(elbowX, elbowY, 8f, pointPaint)
        canvas.drawCircle(wristX, wristY, 8f, pointPaint)
    }

    /**
     * Updates the current pose detection result and image dimensions.
     * The [runningMode] parameter determines how the overlay is scaled.
     */
    fun setResults(
        poseLandmarkerResults: PoseLandmarkerResult,
        imageHeight: Int,
        imageWidth: Int,
        runningMode: RunningMode = RunningMode.IMAGE
    ) {
        results = poseLandmarkerResults
        this.imageHeight = imageHeight
        this.imageWidth = imageWidth

        scaleFactor = when (runningMode) {
            RunningMode.IMAGE, RunningMode.VIDEO ->
                min(width * 1f / imageWidth, height * 1f / imageHeight)
            RunningMode.LIVE_STREAM ->
                max(width * 1f / imageWidth, height * 1f / imageHeight)
        }
        invalidate()
    }

    companion object {
        private const val LANDMARK_STROKE_WIDTH = 12F
    }
}
