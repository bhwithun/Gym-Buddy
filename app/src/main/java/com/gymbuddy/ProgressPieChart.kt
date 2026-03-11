package com.gymbuddy

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class ProgressPieChart @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var onClickListener: (() -> Unit)? = null

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val checkPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var completed = 0
    private var total = 1
    private var isSegmented = false
    private var isHighlighted = false
    private var isOutlineEnabled = true

    init {
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 8f
        paint.color = Color.parseColor("#BDBDBD") // lighter gray outline

        checkPaint.style = Paint.Style.STROKE
        checkPaint.strokeWidth = 6f
        checkPaint.color = Color.WHITE
        checkPaint.strokeCap = Paint.Cap.ROUND
    }

    fun setProgress(completedSets: Int, totalSets: Int) {
        completed = completedSets
        total = totalSets.coerceAtLeast(1)
        invalidate()
    }

    fun setSegmented(segmented: Boolean) {
        isSegmented = segmented
        invalidate()
    }

    fun setHighlighted(highlighted: Boolean) {
        isHighlighted = highlighted
        isOutlineEnabled = highlighted
        invalidate()
    }

    fun setOnClickListener(listener: () -> Unit) {
        onClickListener = listener
    }

    override fun onTouchEvent(event: android.view.MotionEvent): Boolean {
        if (event.action == android.view.MotionEvent.ACTION_UP) {
            onClickListener?.invoke()
        }
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val centerX = width / 2f
        val centerY = height / 2f
        val radius = min(centerX, centerY) - paint.strokeWidth / 2

        // Draw background circle
        paint.color = Color.parseColor("#2A2A2A") // dark gray
        paint.style = Paint.Style.FILL
        canvas.drawCircle(centerX, centerY, radius, paint)

        // Draw outline if enabled
        if (isOutlineEnabled) {
            paint.color = Color.parseColor("#BDBDBD") // lighter gray
            paint.style = Paint.Style.STROKE
            canvas.drawCircle(centerX, centerY, radius, paint)
        }

        if (completed >= total) {
            // Draw solid pale green
            paint.color = Color.parseColor("#80FF80") // pale green
            paint.style = Paint.Style.FILL
            canvas.drawCircle(centerX, centerY, radius - paint.strokeWidth, paint)

            // Draw checkmark
            val checkPath = Path()
            val checkSize = radius * 0.4f
            checkPath.moveTo(centerX - checkSize * 0.3f, centerY)
            checkPath.lineTo(centerX - checkSize * 0.1f, centerY + checkSize * 0.2f)
            checkPath.lineTo(centerX + checkSize * 0.3f, centerY - checkSize * 0.2f)
            canvas.drawPath(checkPath, checkPaint)
        } else if (isSegmented) {
            // Draw segmented pie
            paint.style = Paint.Style.FILL
            val anglePerSegment = 360f / total
            for (i in 0 until total) {
                val startAngle = -90f + i * anglePerSegment
                paint.color = if (i < completed) Color.parseColor("#00FF00") else Color.parseColor("#424242") // dark gray
                canvas.drawArc(
                    centerX - radius + paint.strokeWidth,
                    centerY - radius + paint.strokeWidth,
                    centerX + radius - paint.strokeWidth,
                    centerY + radius - paint.strokeWidth,
                    startAngle,
                    anglePerSegment,
                    true,
                    paint
                )
            }
        } else {
            // Draw progress arc
            val progress = completed.toFloat() / total
            if (progress > 0f) {
                paint.color = Color.parseColor("#80FF80") // pale green
                paint.style = Paint.Style.FILL
                val sweepAngle = progress * 360f
                canvas.drawArc(
                    centerX - radius + paint.strokeWidth,
                    centerY - radius + paint.strokeWidth,
                    centerX + radius - paint.strokeWidth,
                    centerY + radius - paint.strokeWidth,
                    -90f,
                    sweepAngle,
                    true,
                    paint
                )
            }
        }
    }
}
