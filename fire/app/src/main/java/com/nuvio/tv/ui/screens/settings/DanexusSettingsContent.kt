@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)
package com.nuvio.tv.ui.screens.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.Text
import com.nuvio.tv.R
import com.nuvio.tv.core.danexus.*
import com.nuvio.tv.core.qr.QrCodeGenerator
import com.nuvio.tv.ui.components.BrandWordmark

@Composable
internal fun DanexusSettingsContent(initialFocusRequester: FocusRequester? = null) {
    val context = LocalContext.current
    val options by remember { DanexusPreferences.observe(context) }.collectAsState()
    val remoteUrl by DanexusRemoteControl.url.collectAsState()
    val upstream: DanexusUpstreamViewModel = hiltViewModel()
    val upstreamStatus by upstream.status.collectAsState()
    var picker by remember { mutableStateOf<String?>(null) }
    var controlOpen by remember { mutableStateOf(false) }
    fun update(change: (DanexusOptions) -> DanexusOptions) = DanexusPreferences.update(context, change)
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingsDetailHeader(stringResource(R.string.danexus_settings_title), stringResource(R.string.danexus_settings_subtitle))
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            BrandWordmark(Modifier.width(205.dp).height(64.dp))
            SettingsSectionLabel(stringResource(R.string.danexus_source_section))
            SettingsToggleRow(stringResource(R.string.danexus_source_enabled), stringResource(R.string.danexus_source_enabled_desc), options.smartEnabled,
                onToggle = { update { it.copy(smartEnabled = !it.smartEnabled) } },
                modifier = if (initialFocusRequester != null) Modifier.focusRequester(initialFocusRequester) else Modifier)
            SettingsActionRow(stringResource(R.string.danexus_wait_title), stringResource(R.string.danexus_wait_desc),
                value = stringResource(R.string.danexus_seconds, options.waitSeconds), onClick = { picker = "wait" })
            SettingsToggleRow(stringResource(R.string.danexus_prompt_enabled), stringResource(R.string.danexus_prompt_enabled_desc), options.showPrompt,
                onToggle = { update { it.copy(showPrompt = !it.showPrompt) } })
            SettingsActionRow(stringResource(R.string.danexus_prompt_duration), stringResource(R.string.danexus_prompt_duration_desc),
                value = stringResource(R.string.danexus_seconds, options.promptSeconds), onClick = { picker = "prompt" })
            SettingsActionRow(stringResource(R.string.danexus_stall_title), stringResource(R.string.danexus_stall_desc),
                value = stringResource(R.string.danexus_seconds, options.stallSeconds), onClick = { picker = "stall" })
            SettingsToggleRow(stringResource(R.string.danexus_learning_title), stringResource(R.string.danexus_learning_desc), options.learning,
                onToggle = { update { it.copy(learning = !it.learning) } })
            SettingsToggleRow(stringResource(R.string.danexus_all_addons), stringResource(R.string.danexus_all_addons_desc), options.allAddons,
                onToggle = { update { it.copy(allAddons = !it.allAddons) } })
            SettingsActionRow(stringResource(R.string.danexus_source_limit), stringResource(R.string.danexus_source_limit_desc),
                value = if (options.sourceLimit == 0) stringResource(R.string.danexus_all_sources) else options.sourceLimit.toString(), onClick = { picker = "limit" })
            SettingsNote(stringResource(R.string.danexus_source_session_note))
            SettingsSectionLabel(stringResource(R.string.danexus_interface_section))
            SettingsToggleRow(stringResource(R.string.danexus_v2_title),stringResource(R.string.danexus_v2_desc),options.nuvioV2,
                onToggle={ update { it.copy(nuvioV2=!it.nuvioV2) } })
            if(options.nuvioV2) {
                SettingsActionRow(stringResource(R.string.danexus_v2_style),stringResource(R.string.danexus_v2_style_desc),
                    value=stringResource(if(options.v2Dark) R.string.danexus_v2_dark else R.string.danexus_v2_glass),onClick={ picker="v2Style" })
                SettingsActionRow(stringResource(R.string.danexus_v2_transparency),stringResource(R.string.danexus_v2_transparency_desc),
                    value=stringResource(R.string.danexus_percent,options.v2Transparency),onClick={ picker="transparency" },enabled=!options.v2Dark)
                SettingsToggleRow(stringResource(R.string.danexus_v2_focus),stringResource(R.string.danexus_v2_focus_desc),options.v2CinematicFocus,
                    onToggle={ update { it.copy(v2CinematicFocus=!it.v2CinematicFocus) } })
            }
            SettingsActionRow(stringResource(R.string.danexus_ui_scale),stringResource(R.string.danexus_ui_scale_desc),
                value=stringResource(R.string.danexus_percent,options.uiScalePercent),onClick={ picker="scale" })
            SettingsToggleRow(stringResource(R.string.danexus_top_navigation), stringResource(R.string.danexus_top_navigation_desc), options.topNavigation,
                onToggle = { update { it.copy(topNavigation = !it.topNavigation) } })
            SettingsToggleRow(stringResource(R.string.danexus_expand_labels), stringResource(R.string.danexus_expand_labels_desc), options.expandLabels,
                onToggle = { update { it.copy(expandLabels = !it.expandLabels) } })
            SettingsToggleRow(stringResource(R.string.danexus_clock), stringResource(R.string.danexus_clock_desc), options.showClock,
                onToggle = { update { it.copy(showClock = !it.showClock) } })
            SettingsToggleRow(stringResource(R.string.danexus_recommendations), stringResource(R.string.danexus_recommendations_desc), options.recommendations,
                onToggle = { update { it.copy(recommendations = !it.recommendations) } })
            SettingsSectionLabel(stringResource(R.string.danexus_remote_title))
            SettingsToggleRow(stringResource(R.string.danexus_local_control), stringResource(R.string.danexus_local_control_desc), options.remoteEnabled,
                onToggle = { update { it.copy(remoteEnabled = !it.remoteEnabled) } })
            SettingsActionRow(stringResource(R.string.danexus_control_open), stringResource(R.string.danexus_control_open_desc),
                onClick = { DanexusRemoteControl.start(context); controlOpen = true }, enabled = options.remoteEnabled)
            DanexusSeekrSettings(options)
            SettingsSectionLabel(stringResource(R.string.danexus_upstream_title))
            SettingsActionRow(stringResource(R.string.danexus_upstream_check), upstreamStatus ?: stringResource(R.string.danexus_upstream_desc),
                onClick = { upstream.check() })
            SettingsNote(stringResource(R.string.danexus_upstream_preservation))
        }
    }
    picker?.let { key ->
        val values = when (key) {
            "wait" -> listOf(5, 8, 10, 12, 15, 20, 30, 45, 60)
            "prompt" -> listOf(5, 6, 8, 10, 12, 15)
            "stall" -> listOf(10, 15, 20, 30, 45, 60)
            "scale" -> listOf(85,90,95,100,105,110,115)
            "transparency" -> listOf(0,20,40,60,75,85,100)
            "v2Style" -> listOf(0,1)
            else -> listOf(0, 10, 20, 50, 100, 200)
        }
        val titleId = when(key) { "wait" -> R.string.danexus_wait_title; "prompt" -> R.string.danexus_prompt_duration; "stall" -> R.string.danexus_stall_title; "scale" -> R.string.danexus_ui_scale; "transparency" -> R.string.danexus_v2_transparency; "v2Style" -> R.string.danexus_v2_style; else -> R.string.danexus_source_limit }
        val selected = when(key) { "wait" -> options.waitSeconds; "prompt" -> options.promptSeconds; "stall" -> options.stallSeconds; "scale" -> options.uiScalePercent; "transparency" -> options.v2Transparency; "v2Style" -> if(options.v2Dark) 1 else 0; else -> options.sourceLimit }
        SettingsSingleChoiceDialog(title = stringResource(titleId), selectedValue = selected,
            options = values.map { SettingsPickerOption(it, when(key) {
                "limit" -> if(it==0) stringResource(R.string.danexus_all_sources) else it.toString()
                "scale","transparency" -> stringResource(R.string.danexus_percent,it)
                "v2Style" -> stringResource(if(it==0) R.string.danexus_v2_glass else R.string.danexus_v2_dark)
                else -> stringResource(R.string.danexus_seconds,it)
            }) },
            onOptionSelected = { value -> update { when(key) { "wait" -> it.copy(waitSeconds = value); "prompt" -> it.copy(promptSeconds = value); "stall" -> it.copy(stallSeconds = value); "scale" -> it.copy(uiScalePercent=value); "transparency" -> it.copy(v2Transparency=value); "v2Style" -> it.copy(v2Dark=value==1); else -> it.copy(sourceLimit = value) } }; picker = null },
            onDismiss = { picker = null })
    }
    if (controlOpen) com.nuvio.tv.ui.components.NuvioDialog(onDismiss = { controlOpen = false }, title = stringResource(R.string.danexus_remote_title), width = 540.dp) {
        val url = remoteUrl
        if (url == null) Text(stringResource(R.string.danexus_control_no_network)) else {
            val qr = remember(url) { runCatching { QrCodeGenerator.generate(url, 440, margin = 4).asImageBitmap() }.getOrNull() }
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
                qr?.let { Image(it, stringResource(R.string.danexus_remote_qr_description), Modifier.size(208.dp)) }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(url.substringBefore('?'))
                    Text(stringResource(R.string.danexus_control_pairing_code, url.substringAfter("pin=")))
                    Text(stringResource(R.string.danexus_control_instructions))
                }
            }
        }
        SettingsDialogActionButton(text = stringResource(R.string.danexus_close), onClick = { controlOpen = false })
    }
}
