package com.telebridge.sms.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.telebridge.sms.i18n.logStatusLabel
import com.telebridge.sms.i18n.stringsFor
import com.telebridge.sms.model.LogStatus
import com.telebridge.sms.ui.components.FlatSectionCard
import com.telebridge.sms.ui.components.KpiMetricCard
import com.telebridge.sms.ui.components.StatusInlineLabel
import com.telebridge.sms.ui.theme.TeleBridgeColors
import com.telebridge.sms.viewmodel.TeleBridgeUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    uiState: TeleBridgeUiState,
    isWideScreen: Boolean,
    onNavigateToChannels: () -> Unit,
    onNavigateToRules: () -> Unit,
    onNavigateToTester: () -> Unit,
    onClearLogs: () -> Unit,
) {
    val dict = stringsFor(uiState.language)
    var searchQuery by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf<LogStatus?>(null) }

    val forwardedCount = uiState.logs.count {
        it.status == LogStatus.FORWARDED || it.status == LogStatus.DRY_RUN
    }
    val activeRulesCount = uiState.rules.count { it.enabled }
    val channelsCount = uiState.channels.size
    val ignoredOrFailedCount = uiState.logs.count {
        it.status == LogStatus.IGNORED || it.status == LogStatus.FAILED
    }

    val filteredLogs = remember(uiState.logs, searchQuery, statusFilter) {
        val q = searchQuery.trim().lowercase()
        uiState.logs.filter { log ->
            val statusOk = statusFilter == null || log.status == statusFilter
            val searchOk = q.isEmpty() ||
                log.sender.lowercase().contains(q) ||
                log.message.lowercase().contains(q) ||
                (log.ruleName ?: "").lowercase().contains(q)
            statusOk && searchOk
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Responsive 2x2 or 4-column KPI strip
        item {
            if (isWideScreen) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    KpiMetricCard(dict.statForwarded, forwardedCount, Modifier.weight(1f))
                    KpiMetricCard(dict.statActiveRules, activeRulesCount, Modifier.weight(1f))
                    KpiMetricCard(dict.statChannels, channelsCount, Modifier.weight(1f))
                    KpiMetricCard(dict.statIgnoredFailed, ignoredOrFailedCount, Modifier.weight(1f))
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        KpiMetricCard(dict.statForwarded, forwardedCount, Modifier.weight(1f))
                        KpiMetricCard(dict.statActiveRules, activeRulesCount, Modifier.weight(1f))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        KpiMetricCard(dict.statChannels, channelsCount, Modifier.weight(1f))
                        KpiMetricCard(dict.statIgnoredFailed, ignoredOrFailedCount, Modifier.weight(1f))
                    }
                }
            }
        }

        // Setup Checklist Card
        if (channelsCount == 0 || activeRulesCount == 0 || forwardedCount == 0) {
            item {
                FlatSectionCard {
                    Text(
                        text = dict.setupTitle,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = dict.setupSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 14.dp),
                        color = TeleBridgeColors.Border,
                    )
                    ChecklistStepRow(
                        title = dict.step1Title,
                        description = dict.step1Desc,
                        completed = channelsCount > 0,
                        doneLabel = dict.doneLabel,
                        pendingLabel = dict.pendingLabel,
                        actionLabel = dict.navChannels,
                        onClick = onNavigateToChannels,
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = TeleBridgeColors.Border,
                    )
                    ChecklistStepRow(
                        title = dict.step2Title,
                        description = dict.step2Desc,
                        completed = activeRulesCount > 0,
                        doneLabel = dict.doneLabel,
                        pendingLabel = dict.pendingLabel,
                        actionLabel = dict.navRules,
                        onClick = onNavigateToRules,
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = TeleBridgeColors.Border,
                    )
                    ChecklistStepRow(
                        title = dict.step3Title,
                        description = dict.step3Desc,
                        completed = forwardedCount > 0,
                        doneLabel = dict.doneLabel,
                        pendingLabel = dict.pendingLabel,
                        actionLabel = dict.openTester,
                        onClick = onNavigateToTester,
                    )
                }
            }
        }

        // Sample Forwarding Logs Card
        item {
            FlatSectionCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = dict.logsHeading,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    if (uiState.logs.isNotEmpty()) {
                        OutlinedButton(
                            onClick = onClearLogs,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DeleteOutline,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = dict.clearLogs, fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(dict.searchHint, fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(10.dp))

                val filterOptions = listOf(
                    null to dict.filterAll,
                    LogStatus.FORWARDED to dict.filterForwarded,
                    LogStatus.IGNORED to dict.filterIgnored,
                    LogStatus.FAILED to dict.filterFailed,
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    filterOptions.forEachIndexed { idx, (statusVal, label) ->
                        SegmentedButton(
                            selected = statusFilter == statusVal,
                            onClick = { statusFilter = statusVal },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = idx,
                                count = filterOptions.size,
                            ),
                            label = {
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (uiState.logs.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = dict.emptyLogsTitle,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            color = TeleBridgeColors.TextPrimary,
                        )
                        Text(
                            text = dict.emptyLogsDesc,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp, bottom = 14.dp),
                        )
                        Button(
                            onClick = onNavigateToTester,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = TeleBridgeColors.Accent,
                            ),
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Text(text = dict.openTester, fontSize = 13.sp)
                        }
                    }
                } else {
                    filteredLogs.forEachIndexed { index, log ->
                        if (index > 0) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 10.dp),
                                color = TeleBridgeColors.SurfaceMuted,
                            )
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.Center,
                                ) {
                                    Text(
                                        text = log.sender,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        color = TeleBridgeColors.TextPrimary,
                                    )
                                    Text(
                                        text = "·",
                                        color = TeleBridgeColors.TextMuted,
                                        fontSize = 12.sp,
                                    )
                                    Text(
                                        text = log.timestamp,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        color = TeleBridgeColors.TextMuted,
                                    )
                                }
                                StatusInlineLabel(
                                    status = log.status,
                                    label = logStatusLabel(uiState.language, log.status),
                                )
                            }
                            Text(
                                text = log.message,
                                fontSize = 13.sp,
                                color = TeleBridgeColors.TextPrimary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = listOfNotNull(log.ruleName, log.reason).joinToString(" · "),
                                fontSize = 12.sp,
                                color = TeleBridgeColors.TextMuted,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChecklistStepRow(
    title: String,
    description: String,
    completed: Boolean,
    doneLabel: String,
    pendingLabel: String,
    actionLabel: String,
    onClick: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = TeleBridgeColors.TextPrimary,
                modifier = Modifier.weight(1f),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (completed) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (completed) TeleBridgeColors.Success else TeleBridgeColors.Warning,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (completed) doneLabel else pendingLabel,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (completed) TeleBridgeColors.Success else TeleBridgeColors.Warning,
                )
            }
        }
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
        )
        TextButton(
            onClick = onClick,
            contentPadding = PaddingValues(0.dp),
        ) {
            Text(
                text = actionLabel,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = TeleBridgeColors.AccentDark,
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                contentDescription = null,
                tint = TeleBridgeColors.AccentDark,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}
