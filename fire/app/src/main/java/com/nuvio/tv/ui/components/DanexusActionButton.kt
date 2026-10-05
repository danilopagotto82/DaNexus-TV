@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)

package com.nuvio.tv.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import com.nuvio.tv.ui.theme.DanexusCinematic

/** Selection and DPAD focus have separate states; destructive actions use a quiet tint. */
@Composable
fun DanexusActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    selected: Boolean = false,
    destructive: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
    content: @Composable RowScope.() -> Unit
) {
    val shape = RoundedCornerShape(12.dp)
    Button(
        onClick = onClick,
        modifier = modifier.semantics { this.selected = selected },
        enabled = enabled,
        contentPadding = contentPadding,
        shape = ButtonDefaults.shape(shape),
        scale = ButtonDefaults.scale(focusedScale = 1.025f),
        colors = ButtonDefaults.colors(
            containerColor = if (selected) DanexusCinematic.selectedSurface else Color.White.copy(alpha = 0.055f),
            contentColor = if (destructive) DanexusCinematic.danger else DanexusCinematic.text,
            focusedContainerColor = if (destructive) DanexusCinematic.dangerSurface else DanexusCinematic.focusedSurface,
            focusedContentColor = DanexusCinematic.text,
            disabledContainerColor = Color.White.copy(alpha = 0.025f),
            disabledContentColor = DanexusCinematic.secondaryText.copy(alpha = 0.45f)
        ),
        border = ButtonDefaults.border(
            border = if (selected) Border(BorderStroke(1.dp, DanexusCinematic.focus.copy(alpha = 0.4f)), shape = shape) else Border.None,
            focusedBorder = Border(BorderStroke(2.dp, if (destructive) DanexusCinematic.danger else DanexusCinematic.focus), shape = shape)
        ),
        content = content
    )
}
