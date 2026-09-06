package com.gymbuddy

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import kotlin.math.min

object ProgressPieRenderer {
    private val outlineColor = Color.parseColor("#BDBDBD")
    private val backgroundColor = Color.parseColor("#2A2A2A")
    private val completedFill = Color.parseColor("#80FF80")
    private val segmentDone = Color.parseColor("#00FF00")
    private val segmentTodo = Color.parseColor("#424242")
    private val checkColor = Color.parseColor("#2A2A2A")

    fun drawPie(
        canvas: Canvas,
        width: Float,
        height: Float,
        completedSets: Int,
        totalSets: Int,
        segmented: Boolean,
        outlineEnabled: Boolean,
        outlineStroke: Float = 8f
    ) {
        val completed = completedSets
        val total = totalSets.coerceAtLeast(1)
        val centerX = width / 2f
        val centerY = height / 2f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            strokeWidth = outlineStroke
        }
        val radius = min(centerX, centerY) - outlineStroke / 2f

        paint.color = backgroundColor
        paint.style = Paint.Style.FILL
        canvas.drawCircle(centerX, centerY, radius, paint)

        if (outlineEnabled) {
            paint.color = outlineColor
            paint.style = Paint.Style.STROKE
            canvas.drawCircle(centerX, centerY, radius, paint)
        }

        if (completed >= total) {
            paint.color = completedFill
            paint.style = Paint.Style.FILL
            canvas.drawCircle(centerX, centerY, radius - outlineStroke, paint)

            val checkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                color = checkColor
                strokeCap = Paint.Cap.ROUND
                strokeWidth = radius * 0.06f
            }
            val checkPath = Path()
            val checkSize = radius * 0.6f
            checkPath.moveTo(centerX - checkSize * 0.3f, centerY)
            checkPath.lineTo(centerX - checkSize * 0.1f, centerY + checkSize * 0.2f)
            checkPath.lineTo(centerX + checkSize * 0.3f, centerY - checkSize * 0.2f)
            canvas.drawPath(checkPath, checkPaint)
        } else if (segmented) {
            paint.style = Paint.Style.FILL
            val anglePerSegment = 360f / total
            for (i in 0 until total) {
                val startAngle = -90f + i * anglePerSegment
                paint.color = if (i < completed) segmentDone else segmentTodo
                canvas.drawArc(
                    centerX - radius + outlineStroke,
                    centerY - radius + outlineStroke,
                    centerX + radius - outlineStroke,
                    centerY + radius - outlineStroke,
                    startAngle,
                    anglePerSegment,
                    true,
                    paint
                )
            }
        } else {
            val progress = completed.toFloat() / total
            if (progress > 0f) {
                paint.color = completedFill
                paint.style = Paint.Style.FILL
                canvas.drawArc(
                    centerX - radius + outlineStroke,
                    centerY - radius + outlineStroke,
                    centerX + radius - outlineStroke,
                    centerY + radius - outlineStroke,
                    -90f,
                    progress * 360f,
                    true,
                    paint
                )
            }
        }
    }

    fun createWidgetBitmap(
        sizePx: Int,
        completedSets: Int,
        totalSets: Int
    ): Bitmap {
        val size = sizePx.coerceAtLeast(64)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawPie(
            canvas = canvas,
            width = size.toFloat(),
            height = size.toFloat(),
            completedSets = completedSets,
            totalSets = totalSets,
            segmented = true,
            outlineEnabled = true
        )
        return bitmap
    }
}