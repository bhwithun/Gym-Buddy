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

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val checkPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var progress = 0f // 0.0 to 1.0

    init {
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 8f
        paint.color = Color.parseColor("#BDBDBD") // lighter gray outline

        checkPaint.style = Paint.Style.STROKE
        checkPaint.strokeWidth = 6f
        checkPaint.color = Color.WHITE
        checkPaint.strokeCap = Paint.Cap.ROUND
    }

    fun setProgress(value: Float) {
        progress = value.coerceIn(0f, 1f)
        invalidate()
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

        // Draw outline
        paint.color = Color.parseColor("#BDBDBD") // lighter gray
        paint.style = Paint.Style.STROKE
        canvas.drawCircle(centerX, centerY, radius, paint)

        if (progress >= 1f) {
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
        } else if (progress > 0f) {
            // Draw progress arc
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
