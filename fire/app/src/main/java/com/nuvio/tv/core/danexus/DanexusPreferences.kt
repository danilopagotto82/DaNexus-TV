package com.nuvio.tv.core.danexus

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Device preferences are independent of the operator's addon configuration. */
data class DanexusOptions(
    val smartEnabled: Boolean = true,
    val waitSeconds: Int = 12,
    val promptSeconds: Int = 6,
    val stallSeconds: Int = 20,
    val showPrompt: Boolean = true,
    val learning: Boolean = true,
    val allAddons: Boolean = false,
    val sourceLimit: Int = 0,
    val topNavigation: Boolean = true,
    val expandLabels: Boolean = false,
    val showClock: Boolean = true,
    val recommendations: Boolean = true,
    val remoteEnabled: Boolean = true,
    val seekPreviews: Boolean = true,
    val uiScalePercent: Int = 95,
    val nuvioV2: Boolean = false,
    val v2Dark: Boolean = false,
    val v2Transparency: Int = 60,
    val v2CinematicFocus: Boolean = true,
) {
    val sourceTimeoutMs: Long get() = (waitSeconds + if (showPrompt) promptSeconds else 0) * 1_000L
}

object DanexusPreferences {
    private const val FILE = "danexus-interface-v1"
    private val state = MutableStateFlow(DanexusOptions())
    private var loaded = false

    @Synchronized fun observe(context: Context): StateFlow<DanexusOptions> {
        if (!loaded) { state.value = read(context); loaded = true }
        return state
    }

    fun current(context: Context): DanexusOptions = observe(context).value

    private fun read(context: Context): DanexusOptions {
        val p = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        return DanexusOptions(
            smartEnabled = p.getBoolean("smart", true),
            waitSeconds = p.getInt("wait", 12).coerceIn(5, 60),
            promptSeconds = p.getInt("prompt", 6).coerceIn(5, 15),
            stallSeconds = p.getInt("stall", 20).coerceIn(10, 60),
            showPrompt = p.getBoolean("showPrompt", true),
            learning = p.getBoolean("learning", true),
            allAddons = p.getBoolean("allAddons", false),
            sourceLimit = p.getInt("limit", 0).coerceIn(0, 200),
            topNavigation = p.getBoolean("top", true),
            expandLabels = p.getBoolean("labels", false),
            showClock = p.getBoolean("clock", true),
            recommendations = p.getBoolean("recommendations", true),
            remoteEnabled = p.getBoolean("remote", true),
            seekPreviews = p.getBoolean("seekPreviews", true),
            uiScalePercent = p.getInt("uiScale",95).coerceIn(85,115),
            nuvioV2 = p.getBoolean("nuvioV2",false),
            v2Dark = p.getBoolean("v2Dark",false),
            v2Transparency = p.getInt("v2Transparency",60).coerceIn(0,100),
            v2CinematicFocus = p.getBoolean("v2Focus",true),
        )
    }

    @Synchronized fun update(context: Context, transform: (DanexusOptions) -> DanexusOptions) {
        val v = transform(current(context))
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit()
            .putBoolean("smart", v.smartEnabled).putInt("wait", v.waitSeconds)
            .putInt("prompt", v.promptSeconds).putInt("stall", v.stallSeconds)
            .putBoolean("showPrompt", v.showPrompt).putBoolean("learning", v.learning)
            .putBoolean("allAddons", v.allAddons).putInt("limit", v.sourceLimit)
            .putBoolean("top", v.topNavigation).putBoolean("labels", v.expandLabels)
            .putBoolean("clock", v.showClock).putBoolean("recommendations", v.recommendations)
            .putBoolean("remote", v.remoteEnabled).putBoolean("seekPreviews", v.seekPreviews)
            .putInt("uiScale",v.uiScalePercent.coerceIn(85,115)).putBoolean("nuvioV2",v.nuvioV2)
            .putBoolean("v2Dark",v.v2Dark).putInt("v2Transparency",v.v2Transparency.coerceIn(0,100))
            .putBoolean("v2Focus",v.v2CinematicFocus).apply()
        state.value = v
    }
}
