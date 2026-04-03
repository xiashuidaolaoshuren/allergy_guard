package com.xiashuidaolaoshuren.allergyguard.logic

import android.os.SystemClock
import android.util.Log
import kotlin.math.max

/**
 * Debug-only, low-noise profiling counters for OCR memory and latency behavior.
 */
object OcrDebugProfiler {
    private const val TAG = "OcrDebugProfiler"
    private const val SUMMARY_WINDOW_MS = 5_000L

    private var lastSummaryAtMs = 0L
    private var processedFrames = 0
    private var skippedFrames = 0
    private var busyDroppedFrames = 0
    private var ocrFailures = 0

    private var ocrLatencyTotalMs = 0L
    private var ocrLatencyMaxMs = 0L
    private var ocrLatencySamples = 0

    private var matcherLatencyTotalMs = 0L
    private var matcherLatencyMaxMs = 0L
    private var matcherSamples = 0

    private var translationLatencyTotalMs = 0L
    private var translationLatencyMaxMs = 0L
    private var translationSamples = 0

    private var ocrBlockCountTotal = 0
    private var ocrBlockCountMax = 0
    private var matcherTokenCountTotal = 0
    private var matcherTokenCountMax = 0

    private var sessionTextChunkCountMax = 0
    private var sessionAllergenCountMax = 0

    private val isDebugBuild: Boolean by lazy {
        runCatching {
            val clazz = Class.forName("com.xiashuidaolaoshuren.allergyguard.BuildConfig")
            clazz.getField("DEBUG").getBoolean(null)
        }.getOrDefault(false)
    }

    fun frameStartToken(): Long = SystemClock.elapsedRealtime()

    @Synchronized
    fun markFrameSkipped() {
        if (!isDebugBuild) return
        skippedFrames += 1
        maybeLogSummary()
    }

    @Synchronized
    fun markFrameBusyDropped() {
        if (!isDebugBuild) return
        busyDroppedFrames += 1
        maybeLogSummary()
    }

    @Synchronized
    fun markOcrSuccess(latencyMs: Long, blockCount: Int) {
        if (!isDebugBuild) return
        processedFrames += 1
        ocrLatencySamples += 1
        ocrLatencyTotalMs += latencyMs
        ocrLatencyMaxMs = max(ocrLatencyMaxMs, latencyMs)
        ocrBlockCountTotal += blockCount
        ocrBlockCountMax = max(ocrBlockCountMax, blockCount)
        maybeLogSummary()
    }

    @Synchronized
    fun markOcrFailure(latencyMs: Long) {
        if (!isDebugBuild) return
        ocrFailures += 1
        ocrLatencySamples += 1
        ocrLatencyTotalMs += latencyMs
        ocrLatencyMaxMs = max(ocrLatencyMaxMs, latencyMs)
        maybeLogSummary()
    }

    @Synchronized
    fun markMatcherLatency(latencyMs: Long, tokenCount: Int) {
        if (!isDebugBuild) return
        matcherSamples += 1
        matcherLatencyTotalMs += latencyMs
        matcherLatencyMaxMs = max(matcherLatencyMaxMs, latencyMs)
        matcherTokenCountTotal += tokenCount
        matcherTokenCountMax = max(matcherTokenCountMax, tokenCount)
        maybeLogSummary()
    }

    @Synchronized
    fun markTranslationLatency(latencyMs: Long) {
        if (!isDebugBuild) return
        translationSamples += 1
        translationLatencyTotalMs += latencyMs
        translationLatencyMaxMs = max(translationLatencyMaxMs, latencyMs)
        maybeLogSummary()
    }

    @Synchronized
    fun markSessionBufferSize(textChunkCount: Int, allergenCount: Int) {
        if (!isDebugBuild) return
        sessionTextChunkCountMax = max(sessionTextChunkCountMax, textChunkCount)
        sessionAllergenCountMax = max(sessionAllergenCountMax, allergenCount)
        maybeLogSummary()
    }

    @Synchronized
    private fun maybeLogSummary() {
        val now = SystemClock.elapsedRealtime()
        if (now - lastSummaryAtMs < SUMMARY_WINDOW_MS) {
            return
        }

        lastSummaryAtMs = now
        val runtime = Runtime.getRuntime()
        val usedHeapMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
        val maxHeapMb = runtime.maxMemory() / (1024 * 1024)

        val ocrAvg = avg(ocrLatencyTotalMs, ocrLatencySamples)
        val matcherAvg = avg(matcherLatencyTotalMs, matcherSamples)
        val translationAvg = avg(translationLatencyTotalMs, translationSamples)
        val avgBlockCount = avg(ocrBlockCountTotal.toLong(), ocrLatencySamples)
        val avgTokenCount = avg(matcherTokenCountTotal.toLong(), matcherSamples)

        Log.d(
            TAG,
            "window=${SUMMARY_WINDOW_MS}ms heap=${usedHeapMb}/${maxHeapMb}MB " +
                "frames(processed=$processedFrames, skipped=$skippedFrames, busyDrop=$busyDroppedFrames, failed=$ocrFailures) " +
                "ocrMs(avg=$ocrAvg,max=$ocrLatencyMaxMs,blocksAvg=$avgBlockCount,blocksMax=$ocrBlockCountMax) " +
                "matchMs(avg=$matcherAvg,max=$matcherLatencyMaxMs,tokensAvg=$avgTokenCount,tokensMax=$matcherTokenCountMax) " +
                "translationMs(avg=$translationAvg,max=$translationLatencyMaxMs,samples=$translationSamples) " +
                "session(maxTextChunks=$sessionTextChunkCountMax,maxAllergens=$sessionAllergenCountMax)"
        )
    }

    private fun avg(total: Long, samples: Int): Long {
        if (samples <= 0) return 0L
        return total / samples
    }
}
