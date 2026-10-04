package com.nuvio.tv.ui.v2.appearance

import androidx.compose.runtime.staticCompositionLocalOf
import com.nuvio.tv.domain.model.V2AppearancePreferences

/** Fire TV uses the fork's static performance material; video is never blurred. */
val LocalV2Appearance = staticCompositionLocalOf<V2AppearancePreferences?> { null }
