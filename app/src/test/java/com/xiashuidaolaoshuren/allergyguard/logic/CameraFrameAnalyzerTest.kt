package com.xiashuidaolaoshuren.allergyguard.logic

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CameraFrameAnalyzerTest {
    @Test
    fun shouldProcessFrame_processesFirstAndEveryThirdFrame() {
        assertTrue(CameraFrameAnalyzer.shouldProcessFrame(frameIndex = 0, processEveryNFrames = 3))
        assertFalse(CameraFrameAnalyzer.shouldProcessFrame(frameIndex = 1, processEveryNFrames = 3))
        assertFalse(CameraFrameAnalyzer.shouldProcessFrame(frameIndex = 2, processEveryNFrames = 3))
        assertTrue(CameraFrameAnalyzer.shouldProcessFrame(frameIndex = 3, processEveryNFrames = 3))
        assertFalse(CameraFrameAnalyzer.shouldProcessFrame(frameIndex = 4, processEveryNFrames = 3))
        assertFalse(CameraFrameAnalyzer.shouldProcessFrame(frameIndex = 5, processEveryNFrames = 3))
        assertTrue(CameraFrameAnalyzer.shouldProcessFrame(frameIndex = 6, processEveryNFrames = 3))
    }

    @Test
    fun shouldProcessFrame_invalidCadenceDefaultsToEveryFrame() {
        assertTrue(CameraFrameAnalyzer.shouldProcessFrame(frameIndex = 0, processEveryNFrames = 0))
        assertTrue(CameraFrameAnalyzer.shouldProcessFrame(frameIndex = 1, processEveryNFrames = 0))
        assertTrue(CameraFrameAnalyzer.shouldProcessFrame(frameIndex = 2, processEveryNFrames = -5))
    }
}