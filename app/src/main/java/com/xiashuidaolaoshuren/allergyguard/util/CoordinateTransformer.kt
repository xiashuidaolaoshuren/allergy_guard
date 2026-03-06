package com.xiashuidaolaoshuren.allergyguard.util

import kotlin.math.max

object CoordinateTransformer {
    data class SourceRect(
        val left: Int,
        val top: Int,
        val right: Int,
        val bottom: Int
    )

    data class OverlayRect(
        val left: Float,
        val top: Float,
        val right: Float,
        val bottom: Float
    ) {
        val isEmpty: Boolean
            get() = left >= right || top >= bottom
    }

    data class TransformSpec(
        val sourceWidth: Int,
        val sourceHeight: Int,
        val overlayWidth: Int,
        val overlayHeight: Int,
        val isFrontCamera: Boolean
    )

    fun toOverlayRect(sourceRect: SourceRect, spec: TransformSpec): OverlayRect {
        if (spec.sourceWidth <= 0 || spec.sourceHeight <= 0 || spec.overlayWidth <= 0 || spec.overlayHeight <= 0) {
            return OverlayRect(0f, 0f, 0f, 0f)
        }

        // Match PreviewView FILL_CENTER behavior: scale uniformly, then center-crop overflow.
        val scale = max(
            spec.overlayWidth.toFloat() / spec.sourceWidth.toFloat(),
            spec.overlayHeight.toFloat() / spec.sourceHeight.toFloat()
        )
        val scaledWidth = spec.sourceWidth * scale
        val scaledHeight = spec.sourceHeight * scale
        val dx = (spec.overlayWidth - scaledWidth) / 2f
        val dy = (spec.overlayHeight - scaledHeight) / 2f

        val left = sourceRect.left * scale + dx
        val right = sourceRect.right * scale + dx
        val top = sourceRect.top * scale + dy
        val bottom = sourceRect.bottom * scale + dy

        return if (spec.isFrontCamera) {
            val mirroredLeft = spec.overlayWidth - right
            val mirroredRight = spec.overlayWidth - left
            clamp(
                OverlayRect(mirroredLeft, top, mirroredRight, bottom),
                spec.overlayWidth,
                spec.overlayHeight
            )
        } else {
            clamp(OverlayRect(left, top, right, bottom), spec.overlayWidth, spec.overlayHeight)
        }
    }

    private fun clamp(rect: OverlayRect, width: Int, height: Int): OverlayRect {
        val safeWidth = width.toFloat().coerceAtLeast(0f)
        val safeHeight = height.toFloat().coerceAtLeast(0f)
        return OverlayRect(
            rect.left.coerceIn(0f, safeWidth),
            rect.top.coerceIn(0f, safeHeight),
            rect.right.coerceIn(0f, safeWidth),
            rect.bottom.coerceIn(0f, safeHeight)
        )
    }
}