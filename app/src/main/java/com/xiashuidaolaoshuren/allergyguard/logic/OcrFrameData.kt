package com.xiashuidaolaoshuren.allergyguard.logic

import android.graphics.Rect

data class OcrTextBlock(
    val text: String,
    val boundingBox: Rect
)

data class OcrFrameData(
    val fullText: String,
    val textBlocks: List<OcrTextBlock>,
    val sourceWidth: Int,
    val sourceHeight: Int,
    val isFrontCamera: Boolean
)