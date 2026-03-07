package com.xiashuidaolaoshuren.allergyguard.ui.camera

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CameraScanViewModelAlertTest {
    @Test
    fun shouldEmitAlert_allowsFirstAlert() {
        val result = CameraScanViewModel.shouldEmitAlert(
            nowMs = 1_000L,
            lastAlertMs = Long.MIN_VALUE,
            cooldownMs = 4_000L
        )

        assertTrue(result)
    }

    @Test
    fun shouldEmitAlert_blocksAlertsInsideCooldown() {
        val result = CameraScanViewModel.shouldEmitAlert(
            nowMs = 2_500L,
            lastAlertMs = 1_000L,
            cooldownMs = 4_000L
        )

        assertFalse(result)
    }

    @Test
    fun shouldEmitAlert_allowsAlertsAfterCooldown() {
        val result = CameraScanViewModel.shouldEmitAlert(
            nowMs = 5_200L,
            lastAlertMs = 1_000L,
            cooldownMs = 4_000L
        )

        assertTrue(result)
    }
}