package com.xiashuidaolaoshuren.allergyguard.ui.camera

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import android.animation.ValueAnimator
import android.view.animation.LinearInterpolator
import com.xiashuidaolaoshuren.allergyguard.util.CoordinateTransformer

class OverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var pulseAlpha = 1.0f
    private val pulseAnimator = ValueAnimator.ofFloat(0.4f, 1.0f).apply {
        duration = 800
        repeatMode = ValueAnimator.REVERSE
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener { animator ->
            pulseAlpha = animator.animatedValue as Float
            invalidate()
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        pulseAnimator.start()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        pulseAnimator.cancel()
    }

    data class OverlayBlock(
        val text: String,
        val sourceBoundingBox: Rect,
        val isAllergen: Boolean
    )

    data class OverlayRenderModel(
        val blocks: List<OverlayBlock>,
        val sourceWidth: Int,
        val sourceHeight: Int,
        val isFrontCamera: Boolean
    )

    private val allergenPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFE53935.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 6f
    }

    private val safePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF2E7D32.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 5f
    }

    private var renderModel: OverlayRenderModel? = null

    fun render(model: OverlayRenderModel?) {
        if (renderModel == model) {
            return
        }
        renderModel = model
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val model = renderModel ?: return
        if (width == 0 || height == 0) {
            return
        }

        val spec = CoordinateTransformer.TransformSpec(
            sourceWidth = model.sourceWidth,
            sourceHeight = model.sourceHeight,
            overlayWidth = width,
            overlayHeight = height,
            isFrontCamera = model.isFrontCamera
        )

        model.blocks.forEach { block ->
            val transformed = CoordinateTransformer.toOverlayRect(
                CoordinateTransformer.SourceRect(
                    left = block.sourceBoundingBox.left,
                    top = block.sourceBoundingBox.top,
                    right = block.sourceBoundingBox.right,
                    bottom = block.sourceBoundingBox.bottom
                ),
                spec
            )
            if (transformed.isEmpty) {
                return@forEach
            }

            val paint = if (block.isAllergen) {
                allergenPaint.alpha = (pulseAlpha * 255).toInt()
                allergenPaint
            } else {
                safePaint.alpha = 255
                safePaint
            }

            canvas.drawRect(
                transformed.left,
                transformed.top,
                transformed.right,
                transformed.bottom,
                paint
            )
        }
    }
}