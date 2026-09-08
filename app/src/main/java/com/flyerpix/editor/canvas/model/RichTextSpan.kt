package com.flyerpix.editor.canvas.model

import java.io.Serializable

data class RichTextSpan(
    val start: Int,
    val end: Int,
    val color: Int? = null,
    val textSize: Float? = null,
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val isUnderline: Boolean = false,
    val isStrikethrough: Boolean = false
) : Serializable
