package com.nuvio.tv.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/** Classic DaNexus surfaces. Gradients are static and need no blur or V2 renderer. */
object DanexusCinematic {
    val black = Color(0xFF07080A)
    val graphite = Color(0xFF191C21)
    val navy = Color(0xFF07101B)
    val text = Color(0xFFF3F3F4)
    val secondaryText = Color(0xFFB8BEC7)
    val focus = Color(0xFFF0F2F5)
    val focusedSurface = Color(0xF02D323A)
    val selectedSurface = Color(0xD9272D35)
    val danger = Color(0xFFE6ADAD)
    val dangerSurface = Color(0xF0322529)
    val edge = Color(0x20FFFFFF)

    val screenBrush = Brush.linearGradient(listOf(black, Color(0xFF101217), navy))
    val panelBrush = Brush.linearGradient(listOf(Color(0xED090A0D), Color(0xEC14171C), Color(0xEE080F19)))
    val cardBrush = Brush.horizontalGradient(listOf(Color(0xB808090C), Color(0x9514181E), Color(0x7009101B)))
    val backdropBrush = Brush.horizontalGradient(
        0f to Color(0xF508090B), 0.5f to Color(0xCE080A0E), 1f to Color(0x8208101B)
    )
    val bottomScrim = Brush.verticalGradient(
        0f to Color(0x14000000), 0.6f to Color(0x28000000), 1f to Color(0xEE07101B)
    )
    val artworkScrim = Brush.horizontalGradient(
        0f to Color(0xF508090B), 0.48f to Color(0xD3090C11), 1f to Color(0x4808101B)
    )
    val progressBrush = Brush.horizontalGradient(listOf(Color(0xFFE3E8EE), Color(0xFFAAB8CA)))
}
