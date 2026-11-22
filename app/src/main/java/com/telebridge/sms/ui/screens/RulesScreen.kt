package com.telebridge.sms.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.telebridge.sms.i18n.matchModeLabel
import com.telebridge.sms.i18n.stringsFor
import com.telebridge.sms.model.Language
import com.telebridge.sms.model.MatchMode
import com.telebridge.sms.model.SampleSmsData
import com.telebridge.sms.model.SenderRule
import com.telebridge.sms.model.TelegramChannel
import com.telebridge.sms.ui.components.FlatSectionCard
import com.telebridge.sms.ui.theme.TeleBridgeColors
import com.telebridge.sms.viewmodel.TeleBridgeUiState
import com.telebridge.sms.viewmodel.TeleBridgeViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RulesScreen(
    uiState: TeleBridgeUiState,
    onOpenNewRule: () -> Unit,
    onEditRule: (SenderRule) -> Unit,
    onToggleRule: (String) -> Unit,
    onDeleteRule: (String) -> Unit,
) {
    val dict = stringsFor(uiState.language)
    val channelMap = remember(uiState.channels) { uiState.channels.associateBy { it.id } }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            FlatSectionCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = dict.rulesHeading, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = dict.rulesSubheading,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = onOpenNewRule,
                        colors = ButtonDefaults.buttonColors(containerColor = TeleBridgeColors.Accent),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(dict.newRule, fontSize = 12.sp)
                    }
                }
            }
        }

        items(uiState.rules, key = { it.id }) { rule ->
            val channel = channelMap[rule.channelId]
            FlatSectionCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = rule.name,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                color = TeleBridgeColors.TextPrimary,
                            )
                            Text(
                                text = if (rule.enabled) dict.activeLabel else dict.pausedLabel,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (rule.enabled) TeleBridgeColors.Success else TeleBridgeColors.TextMuted,
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(
                                text = matchModeLabel(uiState.language, rule.matchMode),
                                fontSize = 12.sp,
                                color = TeleBridgeColors.TextSecondary,
                            )
                            Text("·", fontSize = 12.sp, color = TeleBridgeColors.TextMuted)
                            Text(
                                text = rule.senderPattern,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = TeleBridgeColors.TextPrimary,
                            )
                            Text("·", fontSize = 12.sp, color = TeleBridgeColors.TextMuted)
                            Text(
                                text = channel?.name ?: dict.unassignedChannel,
                                fontSize = 12.sp,
                                color = TeleBridgeColors.TextSecondary,
                            )
                        }
                    }

                    Switch(
                        checked = rule.enabled,
                        onCheckedChange = { onToggleRule(rule.id) },
                        colors = SwitchDefaults.colors(checkedTrackColor = TeleBridgeColors.Accent),
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 10.dp),
                    color = TeleBridgeColors.SurfaceMuted,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedButton(
                        onClick = { onEditRule(rule) },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(dict.edit, fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = { onDeleteRule(rule.id) }) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = dict.delete,
                            tint = TeleBridgeColors.Error,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RuleEditorDialog(
    language: Language,
    channels: List<TelegramChannel>,
    initialRule: SenderRule?,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        matchMode: MatchMode,
        senderPattern: String,
        includeKeywords: String,
        excludeKeywords: String,
        channelId: String,
        template: String,
        enabled: Boolean,
    ) -> Boolean,
) {
    val dict = stringsFor(language)
    var name by remember { mutableStateOf(initialRule?.name ?: "") }
    var matchMode by remember { mutableStateOf(initialRule?.matchMode ?: MatchMode.LIST) }
    var senderPattern by remember { mutableStateOf(initialRule?.senderPattern ?: "") }
    var includeKeywords by remember { mutableStateOf(initialRule?.includeKeywords ?: "") }
    var excludeKeywords by remember { mutableStateOf(initialRule?.excludeKeywords ?: "") }
    var channelId by remember { mutableStateOf(initialRule?.channelId ?: channels.firstOrNull()?.id ?: "") }
    var template by remember { mutableStateOf(initialRule?.template ?: SampleSmsData.DEFAULT_TEMPLATE) }
    var enabled by remember { mutableStateOf(initialRule?.enabled ?: channels.isNotEmpty()) }
    var sampleCheckSender by remember { mutableStateOf("ParcelTrack") }

    var modeMenuOpen by remember { mutableStateOf(false) }
    var channelMenuOpen by remember { mutableStateOf(false) }

    val patternMatches = remember(sampleCheckSender, matchMode, senderPattern) {
        TeleBridgeViewModel.matchesSender(sampleCheckSender, matchMode, senderPattern)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = TeleBridgeColors.Surface,
        title = {
            Text(
                text = if (initialRule == null) dict.newRule else dict.edit,
                style = MaterialTheme.typography.titleMedium,
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(if (language == Language.BN) "রুলের নাম" else "Rule Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                ExposedDropdownMenuBox(
                    expanded = channelMenuOpen,
                    onExpandedChange = { channelMenuOpen = !channelMenuOpen },
                ) {
                    val selectedChannel = channels.find { it.id == channelId }
                    OutlinedTextField(
                        value = selectedChannel?.name ?: dict.unassignedChannel,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(if (language == Language.BN) "টার্গেট চ্যানেল" else "Target Channel") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = channelMenuOpen) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                    )
                    ExposedDropdownMenu(
                        expanded = channelMenuOpen,
                        onDismissRequest = { channelMenuOpen = false },
                    ) {
                        channels.forEach { ch ->
                            DropdownMenuItem(
                                text = { Text("${ch.name} (${ch.chatId})") },
                                onClick = {
                                    channelId = ch.id
                                    channelMenuOpen = false
                                },
                            )
                        }
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = modeMenuOpen,
                    onExpandedChange = { modeMenuOpen = !modeMenuOpen },
                ) {
                    OutlinedTextField(
                        value = matchModeLabel(language, matchMode),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(if (language == Language.BN) "ম্যাচিং ধরন" else "Match Mode") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modeMenuOpen) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                    )
                    ExposedDropdownMenu(
                        expanded = modeMenuOpen,
                        onDismissRequest = { modeMenuOpen = false },
                    ) {
                        MatchMode.entries.forEach { mode ->
                            DropdownMenuItem(
                                text = { Text(matchModeLabel(language, mode)) },
                                onClick = {
                                    matchMode = mode
                                    modeMenuOpen = false
                                },
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = senderPattern,
                    onValueChange = { senderPattern = it },
                    label = { Text(if (language == Language.BN) "সেন্ডার প্যাটার্ন" else "Sender Pattern") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = TeleBridgeColors.CanvasBg,
                    border = BorderStroke(1.dp, TeleBridgeColors.Border),
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = sampleCheckSender,
                            onValueChange = { sampleCheckSender = it },
                            label = { Text(if (language == Language.BN) "প্যাটার্ন টেস্ট সেন্ডার" else "Test Sample Sender") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(
                            text = if (patternMatches) {
                                if (language == Language.BN) "ম্যাচ করেছে ✓" else "Matches ✓"
                            } else {
                                if (language == Language.BN) "ম্যাচ করেনি" else "No match"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (patternMatches) TeleBridgeColors.Success else TeleBridgeColors.TextMuted,
                        )
                    }
                }

                OutlinedTextField(
                    value = includeKeywords,
                    onValueChange = { includeKeywords = it },
                    label = { Text(if (language == Language.BN) "আবশ্যক কীওয়ার্ড (ঐচ্ছিক)" else "Include Keywords") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = excludeKeywords,
                    onValueChange = { excludeKeywords = it },
                    label = { Text(if (language == Language.BN) "বাদ দেওয়ার কীওয়ার্ড (ঐচ্ছিক)" else "Exclude Keywords") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = template,
                    onValueChange = { template = it },
                    label = { Text(if (language == Language.BN) "মেসেজ টেমপ্লেট" else "Message Template") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )

                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("{{sender}}", "{{message}}", "{{device}}", "{{time}}").forEach { token ->
                        OutlinedButton(
                            onClick = { template = "$template $token" },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        ) {
                            Text(token, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = enabled && channelId.isNotBlank(),
                        enabled = channelId.isNotBlank(),
                        onCheckedChange = { enabled = it },
                    )
                    Text(
                        text = if (language == Language.BN) "সাথে সাথে সক্রিয় করুন" else "Enable immediately",
                        fontSize = 13.sp,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val saved = onSave(
                        name,
                        matchMode,
                        senderPattern,
                        includeKeywords,
                        excludeKeywords,
                        channelId,
                        template,
                        enabled,
                    )
                    if (saved) onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = TeleBridgeColors.Accent),
            ) {
                Text(dict.save)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(dict.cancel)
            }
        },
    )
}
