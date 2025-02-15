package com.google.mediapipe.examples.poselandmarker

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.media.MediaPlayer
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.min

// Wrapper class to extract x,y coordinates via reflection.
private class LandmarkWrapper(val landmark: Any) {
    val x: Float
        get() = landmark.javaClass.getMethod("x").invoke(landmark) as Float
    val y: Float
        get() = landmark.javaClass.getMethod("y").invoke(landmark) as Float
}

class OverlayView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {

    private var results: PoseLandmarkerResult? = null

    private var pointPaint = Paint()
    private var linePaint = Paint()
    private var textPaint = Paint()

    // Image dimensions, scaling, and offsets for centering.
    private var scaleFactor = 1f
    private var imageWidth = 1
    private var imageHeight = 1
    private var offsetX = 0f
    private var offsetY = 0f

    // Default exercise type: BICEP. (Ensure your ExerciseType enum includes LATERAL_RAISE.)
    private var exerciseType: ExerciseType = ExerciseType.BICEP
    fun setExerciseType(exerciseType: ExerciseType) {
        this.exerciseType = exerciseType
    }

    // MediaPlayer for audio feedback.
    private var mediaPlayer: MediaPlayer? = null

    // ===== BICEP Logic Fields =====
    private val BICEP_ANGLE_MIN = 70f
    private val BICEP_ANGLE_MAX = 160f
    private var repCountLeftBicep = 0
    private var repCountRightBicep = 0
    private var stageLeftBicep: String? = null
    private var stageRightBicep: String? = null
    private var repFlagLeftBicep = false
    private var repFlagRightBicep = false

    // ===== SQUAT Logic Fields =====
    // Research-backed values (e.g., Verywell Fit and peer-reviewed studies :contentReference[oaicite:1]{index=1})
    private val SQUAT_KNEE_MIN = 85f
    private val SQUAT_KNEE_MAX = 95f
    private var repCountSquatLeft = 0
    private var repCountSquatRight = 0
    private var stageSquatLeft: String? = null
    private var stageSquatRight: String? = null
    private var repFlagSquatLeft = false
    private var repFlagSquatRight = false

    // ===== LATERAL RAISE Logic Fields =====
    // Ideal lateral raise (shoulder abduction) angle range is assumed to be 80°-100°.
    private val LATERAL_RAISE_MIN = 80f
    private val LATERAL_RAISE_MAX = 100f

    init {
        initPaints()
    }

    private fun initPaints() {
        linePaint.color = ContextCompat.getColor(context!!, R.color.mp_color_primary)
        linePaint.strokeWidth = 12f
        linePaint.style = Paint.Style.STROKE

        pointPaint.color = Color.YELLOW
        pointPaint.strokeWidth = 12f
        pointPaint.style = Paint.Style.FILL

        textPaint.color = Color.WHITE
        textPaint.textSize = 40f
        textPaint.style = Paint.Style.FILL
    }

    fun clear() {
        results = null

        // Reset Bicep counters
        repCountLeftBicep = 0
        repCountRightBicep = 0
        stageLeftBicep = null
        stageRightBicep = null
        repFlagLeftBicep = false
        repFlagRightBicep = false

        // Reset Squat counters
        repCountSquatLeft = 0
        repCountSquatRight = 0
        stageSquatLeft = null
        stageSquatRight = null
        repFlagSquatLeft = false
        repFlagSquatRight = false

        pointPaint.reset()
        linePaint.reset()
        textPaint.reset()
        initPaints()

        invalidate()
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        val poseLandmarkerResult = results ?: return
        if (poseLandmarkerResult.landmarks().isEmpty()) return

        val personLandmarks = poseLandmarkerResult.landmarks()[0]

        // Draw each landmark as a point with proper offset and scaling.
        for (lm in personLandmarks) {
            val lw = LandmarkWrapper(lm)
            canvas.drawPoint(
                offsetX + lw.x * imageWidth * scaleFactor,
                offsetY + lw.y * imageHeight * scaleFactor,
                pointPaint
            )
        }

        // Dispatch based on exercise type.
        when (exerciseType) {
            ExerciseType.BICEP -> processBicep(canvas, personLandmarks)
            ExerciseType.SQUAT -> processSquat(canvas, personLandmarks)
            ExerciseType.LATERAL_RAISE -> processLateralRaise(canvas, personLandmarks)
        }
    }

    private fun processBicep(canvas: Canvas, landmarks: List<Any>) {
        if (landmarks.size <= 16) return

        val leftShoulder = landmarks[11]
        val leftElbow = landmarks[13]
        val leftWrist = landmarks[15]
        val rightShoulder = landmarks[12]
        val rightElbow = landmarks[14]
        val rightWrist = landmarks[16]

        val leftAngle = calculateAngle(leftShoulder, leftElbow, leftWrist)
        val rightAngle = calculateAngle(rightShoulder, rightElbow, rightWrist)

        if (leftAngle > BICEP_ANGLE_MAX) {
            if (stageLeftBicep == "up" && repFlagLeftBicep) repFlagLeftBicep = false
            stageLeftBicep = "down"
        }
        if (leftAngle < BICEP_ANGLE_MIN) {
            if (stageLeftBicep == "down" && !repFlagLeftBicep) {
                stageLeftBicep = "up"
                repCountLeftBicep++
                repFlagLeftBicep = true
            }
        }
        if (rightAngle > BICEP_ANGLE_MAX) {
            if (stageRightBicep == "up" && repFlagRightBicep) repFlagRightBicep = false
            stageRightBicep = "down"
        }
        if (rightAngle < BICEP_ANGLE_MIN) {
            if (stageRightBicep == "down" && !repFlagRightBicep) {
                stageRightBicep = "up"
                repCountRightBicep++
                repFlagRightBicep = true
            }
        }

        val leftColor = if (leftAngle in BICEP_ANGLE_MIN..BICEP_ANGLE_MAX) Color.GREEN else Color.RED
        val rightColor = if (rightAngle in BICEP_ANGLE_MIN..BICEP_ANGLE_MAX) Color.GREEN else Color.RED

        drawLimb(canvas, leftShoulder, leftElbow, leftWrist, leftColor)
        drawLimb(canvas, rightShoulder, rightElbow, rightWrist, rightColor)

        val le = LandmarkWrapper(leftElbow)
        val re = LandmarkWrapper(rightElbow)
        canvas.drawText("Left Angle: ${leftAngle.toInt()}°",
            offsetX + le.x * imageWidth * scaleFactor,
            offsetY + le.y * imageHeight * scaleFactor - 20,
            textPaint)
        canvas.drawText("Right Angle: ${rightAngle.toInt()}°",
            offsetX + re.x * imageWidth * scaleFactor,
            offsetY + re.y * imageHeight * scaleFactor - 20,
            textPaint)

        canvas.drawText("Left Reps: $repCountLeftBicep", 50f, 50f, textPaint)
        canvas.drawText("Right Reps: $repCountRightBicep", 50f, 100f, textPaint)
    }

    private fun processSquat(canvas: Canvas, landmarks: List<Any>) {
        // Squat indices: Left leg: Hip (23), Knee (25), Ankle (27); Right leg: Hip (24), Knee (26), Ankle (28)
        if (landmarks.size <= 28) return

        // Left leg
        val leftHip = landmarks[23]
        val leftKnee = landmarks[25]
        val leftAnkle = landmarks[27]
        val leftKneeAngle = calculateAngle(leftHip, leftKnee, leftAnkle)
        val leftHipWrap = LandmarkWrapper(leftHip)
        // Also compute left hip angle using Shoulder (11), Hip (23), Knee (25)
        val leftShoulder = landmarks[11]
        val leftHipAngle = calculateAngle(leftShoulder, leftHip, leftKnee)

        // Right leg
        val rightHip = landmarks[24]
        val rightKnee = landmarks[26]
        val rightAnkle = landmarks[28]
        val rightKneeAngle = calculateAngle(rightHip, rightKnee, rightAnkle)
        val rightHipWrap = LandmarkWrapper(rightHip)
        // Compute right hip angle using Shoulder (12), Hip (24), Knee (26)
        val rightShoulder = landmarks[12]
        val rightHipAngle = calculateAngle(rightShoulder, rightHip, rightKnee)

        if (leftKneeAngle > SQUAT_KNEE_MAX) {
            if (stageSquatLeft == "up" && repFlagSquatLeft) repFlagSquatLeft = false
            stageSquatLeft = "down"
        }
        if (leftKneeAngle < SQUAT_KNEE_MIN) {
            if (stageSquatLeft == "down" && !repFlagSquatLeft) {
                stageSquatLeft = "up"
                repCountSquatLeft++
                repFlagSquatLeft = true
                playAudioFeedback(R.raw.squat_incorrect)
            }
        }
        if (rightKneeAngle > SQUAT_KNEE_MAX) {
            if (stageSquatRight == "up" && repFlagSquatRight) repFlagSquatRight = false
            stageSquatRight = "down"
        }
        if (rightKneeAngle < SQUAT_KNEE_MIN) {
            if (stageSquatRight == "down" && !repFlagSquatRight) {
                stageSquatRight = "up"
                repCountSquatRight++
                repFlagSquatRight = true
                playAudioFeedback(R.raw.squat_incorrect)
            }
        }

        val leftColor = if (leftKneeAngle in SQUAT_KNEE_MIN..SQUAT_KNEE_MAX) Color.GREEN else Color.RED
        val rightColor = if (rightKneeAngle in SQUAT_KNEE_MIN..SQUAT_KNEE_MAX) Color.GREEN else Color.RED

        drawLimb(canvas, leftHip, leftKnee, leftAnkle, leftColor)
        drawLimb(canvas, rightHip, rightKnee, rightAnkle, rightColor)

        val leftKneeWrap = LandmarkWrapper(leftKnee)
        val rightKneeWrap = LandmarkWrapper(rightKnee)
        canvas.drawText("L Knee: ${leftKneeAngle.toInt()}°",
            offsetX + leftKneeWrap.x * imageWidth * scaleFactor,
            offsetY + leftKneeWrap.y * imageHeight * scaleFactor - 20,
            textPaint)
        canvas.drawText("L Hip: ${leftHipAngle.toInt()}°",
            offsetX + leftHipWrap.x * imageWidth * scaleFactor,
            offsetY + leftHipWrap.y * imageHeight * scaleFactor - 40,
            textPaint)
        canvas.drawText("R Knee: ${rightKneeAngle.toInt()}°",
            offsetX + rightKneeWrap.x * imageWidth * scaleFactor,
            offsetY + rightKneeWrap.y * imageHeight * scaleFactor - 20,
            textPaint)
        canvas.drawText("R Hip: ${rightHipAngle.toInt()}°",
            offsetX + rightHipWrap.x * imageWidth * scaleFactor,
            offsetY + rightHipWrap.y * imageHeight * scaleFactor - 40,
            textPaint)

        canvas.drawText("Left Squat Reps: $repCountSquatLeft", 50f, 50f, textPaint)
        canvas.drawText("Right Squat Reps: $repCountSquatRight", 50f, 100f, textPaint)
    }

    private fun processLateralRaise(canvas: Canvas, landmarks: List<Any>) {
        // Lateral raise indices: Left: Shoulder (11), Elbow (13), Hip (23); Right: Shoulder (12), Elbow (14), Hip (24)
        if (landmarks.size <= 24) return

        val leftShoulder = landmarks[11]
        val leftElbow = landmarks[13]
        val leftHip = landmarks[23]
        val rightShoulder = landmarks[12]
        val rightElbow = landmarks[14]
        val rightHip = landmarks[24]

        val leftLateralAngle = calculateAngle(leftElbow, leftShoulder, leftHip)
        val rightLateralAngle = calculateAngle(rightElbow, rightShoulder, rightHip)

        val leftColor = if (leftLateralAngle in LATERAL_RAISE_MIN..LATERAL_RAISE_MAX) Color.GREEN else Color.RED
        val rightColor = if (rightLateralAngle in LATERAL_RAISE_MIN..LATERAL_RAISE_MAX) Color.GREEN else Color.RED

        if (leftLateralAngle !in LATERAL_RAISE_MIN..LATERAL_RAISE_MAX ||
            rightLateralAngle !in LATERAL_RAISE_MIN..LATERAL_RAISE_MAX) {
            playAudioFeedback(R.raw.lateral_raise_incorrect)
        }

        // Assume wrists are at indices 15 and 16.
        val leftWrist = landmarks[15]
        val rightWrist = landmarks[16]
        drawLimb(canvas, leftShoulder, leftElbow, leftWrist, leftColor)
        drawLimb(canvas, rightShoulder, rightElbow, rightWrist, rightColor)

        val ls = LandmarkWrapper(leftShoulder)
        val rs = LandmarkWrapper(rightShoulder)
        canvas.drawText("L Raise: ${leftLateralAngle.toInt()}°",
            offsetX + ls.x * imageWidth * scaleFactor,
            offsetY + ls.y * imageHeight * scaleFactor - 20,
            textPaint)
        canvas.drawText("R Raise: ${rightLateralAngle.toInt()}°",
            offsetX + rs.x * imageWidth * scaleFactor,
            offsetY + rs.y * imageHeight * scaleFactor - 20,
            textPaint)
    }

    private fun drawLimb(canvas: Canvas, first: Any, second: Any, third: Any, color: Int) {
        linePaint.color = color
        pointPaint.color = color

        val f = LandmarkWrapper(first)
        val s = LandmarkWrapper(second)
        val t = LandmarkWrapper(third)

        val fx = offsetX + f.x * imageWidth * scaleFactor
        val fy = offsetY + f.y * imageHeight * scaleFactor
        val sx = offsetX + s.x * imageWidth * scaleFactor
        val sy = offsetY + s.y * imageHeight * scaleFactor
        val tx = offsetX + t.x * imageWidth * scaleFactor
        val ty = offsetY + t.y * imageHeight * scaleFactor

        canvas.drawLine(fx, fy, sx, sy, linePaint)
        canvas.drawLine(sx, sy, tx, ty, linePaint)
        canvas.drawCircle(fx, fy, 8f, pointPaint)
        canvas.drawCircle(sx, sy, 8f, pointPaint)
        canvas.drawCircle(tx, ty, 8f, pointPaint)
    }

    // Calculates the angle at point b (between lines ab and bc)
    private fun calculateAngle(a: Any, b: Any, c: Any): Float {
        val A = LandmarkWrapper(a)
        val B = LandmarkWrapper(b)
        val C = LandmarkWrapper(c)
        val radians = atan2(C.y - B.y, C.x - B.x) - atan2(A.y - B.y, A.x - B.x)
        var angle = abs(radians * 180.0 / PI).toFloat()
        if (angle > 180f) angle = 360f - angle
        return angle
    }

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
        offsetX = (width - imageWidth * scaleFactor) / 2f
        offsetY = (height - imageHeight * scaleFactor) / 2f

        invalidate()
    }

    // Plays an audio feedback file given its resource ID.
    private fun playAudioFeedback(audioResId: Int) {
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer.create(context, audioResId)
        mediaPlayer?.start()
    }
}
