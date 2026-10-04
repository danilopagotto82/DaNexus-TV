@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)
package com.nuvio.tv.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.Card
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.nuvio.tv.R
import com.nuvio.tv.core.danexus.*
import com.nuvio.tv.ui.components.NuvioDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.launch
import tv.seekr.previews.core.SeekrPreviews

@Composable
internal fun DanexusSeekrSettings(options: DanexusOptions) {
    val context = LocalContext.current
    val revision by DanexusSeekrKey.revision.collectAsState()
    val configured = remember(revision) { DanexusSeekrKey.read(context).isNotBlank() }
    var editing by remember { mutableStateOf(false) }
    SettingsSectionLabel(stringResource(R.string.danexus_seek_section))
    SettingsToggleRow(stringResource(R.string.danexus_seek_enabled), stringResource(R.string.danexus_seek_enabled_desc), options.seekPreviews,
        onToggle = { DanexusPreferences.update(context) { it.copy(seekPreviews = !it.seekPreviews) } })
    SettingsActionRow(stringResource(R.string.danexus_seek_key), stringResource(R.string.danexus_seek_key_desc),
        value = stringResource(if(configured) R.string.danexus_seek_configured else R.string.danexus_seek_missing), onClick = { editing = true })
    SettingsNote(stringResource(R.string.danexus_seek_instructions))
    if (editing) {
        var value by remember { mutableStateOf("") }
        var busy by remember { mutableStateOf(false) }
        var message by remember { mutableStateOf<String?>(null) }
        val scope = rememberCoroutineScope()
        val focus = remember { FocusRequester() }
        val keyboard = LocalSoftwareKeyboardController.current
        val invalid = stringResource(R.string.danexus_seek_invalid)
        val failed = stringResource(R.string.danexus_seek_save_failed)
        val save: () -> Unit = {
            if (!busy) {
                if (!DanexusSeekPreviewPolicy.validKeyFormat(value)) message = invalid else {
                    busy = true; message = null
                    scope.launch {
                        val valid = withTimeoutOrNull(12_000L) { withContext(Dispatchers.IO) {
                            SeekrPreviews.create(apiKey = value.trim(), httpClient = DanexusSeekPreviewImages.http).validateKey()
                        } } == true
                        busy = false
                        if (!valid) message = invalid
                        else if (DanexusSeekrKey.save(context, value)) editing = false else message = failed
                    }
                }
            }
        }
        NuvioDialog(onDismiss = { editing = false }, title = stringResource(R.string.danexus_seek_key),
            subtitle = stringResource(R.string.danexus_seek_key_desc), width = 640.dp) {
            Card(onClick = { focus.requestFocus(); keyboard?.show() }) {
                BasicTextField(value, onValueChange = { value = it.trim(); message = null },
                    Modifier.fillMaxWidth().padding(16.dp).focusRequester(focus), singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface), visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { keyboard?.hide(); save() }))
            }
            message?.let { Text(it) }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { editing = false }) { Text(stringResource(R.string.action_cancel)) }
                Button(onClick = { if (DanexusSeekrKey.save(context, "")) editing = false else message = failed }, enabled = !busy) { Text(stringResource(R.string.action_clear)) }
                Button(onClick = save, enabled = !busy) { Text(stringResource(if (busy) R.string.danexus_seek_validating else R.string.danexus_seek_save)) }
            }
        }
    }
}
