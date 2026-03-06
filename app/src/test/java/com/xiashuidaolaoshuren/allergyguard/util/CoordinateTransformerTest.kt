package com.xiashuidaolaoshuren.allergyguard.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CoordinateTransformerTest {
    @Test
    fun toOverlayRect_mapsCenterCropWithoutMirror() {
        val sourceRect = CoordinateTransformer.SourceRect(200, 400, 400, 700)
        val spec = CoordinateTransformer.TransformSpec(
            sourceWidth = 1000,
            sourceHeight = 2000,
            overlayWidth = 1000,
            overlayHeight = 1000,
            isFrontCamera = false
        )

        val transformed = CoordinateTransformer.toOverlayRect(sourceRect, spec)

        // Scale is 1.0 in X and 0.5 in Y equivalent via center crop; Y is shifted by -500.
        assertEquals(200f, transformed.left, 0.001f)
        assertEquals(0f, transformed.top, 0.001f)
        assertEquals(400f, transformed.right, 0.001f)
        assertEquals(200f, transformed.bottom, 0.001f)
    }

    @Test
    fun toOverlayRect_mirrorsForFrontCamera() {
        val sourceRect = CoordinateTransformer.SourceRect(100, 100, 300, 300)
        val spec = CoordinateTransformer.TransformSpec(
            sourceWidth = 1000,
            sourceHeight = 1000,
            overlayWidth = 1000,
            overlayHeight = 1000,
            isFrontCamera = true
        )

        val transformed = CoordinateTransformer.toOverlayRect(sourceRect, spec)

        assertEquals(700f, transformed.left, 0.001f)
        assertEquals(100f, transformed.top, 0.001f)
        assertEquals(900f, transformed.right, 0.001f)
        assertEquals(300f, transformed.bottom, 0.001f)
    }

    @Test
    fun toOverlayRect_returnsEmptyWhenDimensionsInvalid() {
        val transformed = CoordinateTransformer.toOverlayRect(
            CoordinateTransformer.SourceRect(0, 0, 10, 10),
            CoordinateTransformer.TransformSpec(
                sourceWidth = 0,
                sourceHeight = 100,
                overlayWidth = 200,
                overlayHeight = 200,
                isFrontCamera = false
            )
        )

        assertTrue(transformed.isEmpty)
    }
}