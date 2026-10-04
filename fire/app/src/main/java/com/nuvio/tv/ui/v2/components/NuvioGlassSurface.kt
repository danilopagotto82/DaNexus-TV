package com.nuvio.tv.ui.v2.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.nuvio.tv.domain.model.FocusStyle
import com.nuvio.tv.domain.model.VisualStyle
import com.nuvio.tv.ui.theme.NuvioTheme
import com.nuvio.tv.ui.v2.appearance.LocalV2Appearance

enum class GlassRole { NAVIGATION, CONTROL, PANEL, MODAL, HUD, CARD_FOCUS, TOOLTIP }

/** Derived from NuvioTV-Fork nt4: gradient, rim and role-specific opacity.
 * Static material keeps the same geometry and costs no video capture or blur pass.
 */
@Composable
fun Modifier.nuvioGlass(role: GlassRole, focused: Boolean = false,
    shape: Shape = RoundedCornerShape(20.dp), playback: Boolean = false): Modifier {
    val appearance = LocalV2Appearance.current ?: return this
    val dark = appearance.visualStyle == VisualStyle.PURE_LIQUID_DARK
    val alpha = if (dark) .94f else glassBodyAlpha(role, playback,
        if (role == GlassRole.CONTROL) .18f else .70f, appearance.glassTransparencyPercent)
    val accent = NuvioTheme.colors.Secondary
    val background = Brush.verticalGradient(
        if (dark) listOf(Color(0xFF102332).copy(alpha = alpha), Color(0xFF040D16).copy(alpha = alpha))
        else if (playback) listOf(Color(0xFF353B40).copy(alpha = alpha), Color(0xFF101418).copy(alpha = alpha))
        else listOf(Color(0xFF202225).copy(alpha = alpha), Color(0xFF0C0E11).copy(alpha = alpha)))
    val edge = Brush.verticalGradient(listOf(Color.White.copy(alpha = if (playback) .50f else .18f),
        Color.White.copy(alpha = .03f), Color.White.copy(alpha = if (playback) .23f else .08f)))
    return clip(shape).background(background,shape).drawWithCache {
        val outline = shape.createOutline(size, layoutDirection, this)
        val stroke = Stroke(1.dp.toPx())
        val reflection = Brush.linearGradient(listOf(Color.White.copy(alpha=.12f), Color.Transparent,
            Color.White.copy(alpha=.025f)))
        onDrawWithContent {
            if (!playback && !dark) drawOutline(outline, accent.copy(alpha = if (focused) .16f else .04f))
            if (playback) drawOutline(outline, reflection)
            drawContent()
            drawOutline(outline, edge, style=stroke)
            if (focused) drawOutline(outline, accent, alpha=.8f, style=stroke)
        }
    }
}

@Composable
fun NuvioGlassSurface(role: GlassRole, modifier: Modifier = Modifier, focused: Boolean = false,
    shape: Shape = RoundedCornerShape(20.dp), content: @Composable BoxScope.() -> Unit) {
    Box(modifier.nuvioGlass(role,focused,shape),content=content)
}

/** Decorates the existing focus target; does not create another focus node. */
@Composable
fun Modifier.nuvioV2Focus(focused: Boolean, shape: Shape, stationary: Boolean = false): Modifier {
    val appearance = LocalV2Appearance.current ?: return this
    if (stationary && !focused) return this
    val cinematic = appearance.focusStyle == FocusStyle.CINEMATIC_FOCUS
    val progress = animateFloatAsState(if (focused) 1f else 0f,
        tween(if (focused) 160 else 200), label="v2Focus")
    val accent = NuvioTheme.colors.Secondary
    return (if (stationary) this else graphicsLayer {
        scaleX = 1f + (if (cinematic) .025f else .018f) * progress.value
        scaleY = scaleX
    }).drawWithCache {
        val outline = shape.createOutline(size, layoutDirection, this)
        val edge = Brush.linearGradient(listOf(accent, Color.White.copy(alpha=.85f), accent))
        onDrawWithContent {
            drawContent()
            val p=progress.value
            if (p>0f) {
                if (cinematic) drawOutline(outline,accent,alpha=p*.10f,style=Stroke(6.dp.toPx()))
                drawOutline(outline,edge,alpha=p,style=Stroke(2.dp.toPx()))
            }
        }
    }
}
