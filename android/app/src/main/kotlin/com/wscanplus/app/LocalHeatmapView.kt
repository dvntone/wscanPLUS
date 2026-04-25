package com.wscanplus.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class LocalHeatmapView : View {
    constructor(context: Context) : super(context)

    constructor(
        context: Context,
        attrs: AttributeSet?,
    ) : super(context, attrs)

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
    ) : super(context, attrs, defStyleAttr)

    data class Point(
        val latitude: Double,
        val longitude: Double,
        val weight: Double,
    )

    private val points = mutableListOf<Point>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val gridPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(80, 64, 196, 255)
            strokeWidth = 1f
        }
    private val framePaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(86, 11, 18, 32)
            style = Paint.Style.FILL
        }

    init {
        setWillNotDraw(false)
        isClickable = false
        isFocusable = false
    }

    fun setPoints(newPoints: List<Point>) {
        points.clear()
        points.addAll(newPoints)
        updateVisibility()
        invalidate()
    }

    fun clearPoints() {
        points.clear()
        updateVisibility()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (points.isEmpty() || width <= 0 || height <= 0) {
            return
        }

        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), framePaint)
        drawGrid(canvas)

        val bounds = Bounds.from(points)
        val radius = max(36f, min(width, height) * 0.095f)
        points.forEach { point ->
            val x = bounds.projectX(point.longitude, width.toFloat())
            val y = bounds.projectY(point.latitude, height.toFloat())
            val normalized = point.weight.coerceIn(0.1, 1.0).toFloat()
            val alpha = (70 + normalized * 155).toInt().coerceIn(70, 225)
            paint.shader =
                RadialGradient(
                    x,
                    y,
                    radius * (0.8f + normalized),
                    intArrayOf(
                        Color.argb(alpha, 255, 70, 70),
                        Color.argb(alpha / 2, 255, 193, 7),
                        Color.argb(0, 64, 196, 255),
                    ),
                    floatArrayOf(0f, 0.42f, 1f),
                    Shader.TileMode.CLAMP,
                )
            canvas.drawCircle(x, y, radius * (0.8f + normalized), paint)
            paint.shader = null
        }
    }

    private fun updateVisibility() {
        visibility = if (points.isEmpty()) GONE else VISIBLE
    }

    private fun drawGrid(canvas: Canvas) {
        val columns = 4
        val rows = 6
        for (column in 1 until columns) {
            val x = width * column / columns.toFloat()
            canvas.drawLine(x, 0f, x, height.toFloat(), gridPaint)
        }
        for (row in 1 until rows) {
            val y = height * row / rows.toFloat()
            canvas.drawLine(0f, y, width.toFloat(), y, gridPaint)
        }
    }

    private data class Bounds(
        val minLat: Double,
        val maxLat: Double,
        val minLng: Double,
        val maxLng: Double,
    ) {
        fun projectX(
            lng: Double,
            width: Float,
        ): Float {
            val span = max(abs(maxLng - minLng), MIN_COORDINATE_SPAN)
            return (((lng - minLng) / span).coerceIn(0.0, 1.0) * width).toFloat()
        }

        fun projectY(
            lat: Double,
            height: Float,
        ): Float {
            val span = max(abs(maxLat - minLat), MIN_COORDINATE_SPAN)
            return ((1.0 - ((lat - minLat) / span).coerceIn(0.0, 1.0)) * height).toFloat()
        }

        companion object {
            fun from(points: List<Point>): Bounds {
                val minLat = points.minOf { it.latitude }
                val maxLat = points.maxOf { it.latitude }
                val minLng = points.minOf { it.longitude }
                val maxLng = points.maxOf { it.longitude }
                val latPadding = max(abs(maxLat - minLat) * 0.12, MIN_COORDINATE_SPAN)
                val lngPadding = max(abs(maxLng - minLng) * 0.12, MIN_COORDINATE_SPAN)
                return Bounds(
                    minLat = minLat - latPadding,
                    maxLat = maxLat + latPadding,
                    minLng = minLng - lngPadding,
                    maxLng = maxLng + lngPadding,
                )
            }
        }
    }

    companion object {
        private const val MIN_COORDINATE_SPAN = 0.0002
    }
}
