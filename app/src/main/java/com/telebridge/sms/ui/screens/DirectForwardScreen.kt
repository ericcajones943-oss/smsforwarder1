package com.telebridge.sms.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import com.telebridge.sms.i18n.logStatusLabel
import com.telebridge.sms.i18n.stringsFor
import com.telebridge.sms.model.Language
import com.telebridge.sms.model.SampleSmsData
import com.telebridge.sms.ui.components.FlatSectionCard
import com.telebridge.sms.ui.components.StatusInlineLabel
import com.telebridge.sms.ui.theme.TeleBridgeColors
import com.telebridge.sms.viewmodel.TeleBridgeUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DirectForwardScreen(
    uiState: TeleBridgeUiState,
    onPermissionResult: () -> Unit,
    onToggleAutoForward: (Boolean) -> Unit,
    onRunTestSms: (sender: String, message: String, dryRun: Boolean) -> Unit,
) {
    val dict = stringsFor(uiState.language)

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) {
        onPermissionResult()
    }

    var testSender by remember { mutableStateOf(SampleSmsData.presets.first().sender) }
    var testBody by remember { mutableStateOf(SampleSmsData.presets.first().message) }
    var dryRun by remember { mutableStateOf(false) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Direct SMS Permission & Auto-Forward Switch Card
        item {
            FlatSectionCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Sms,
                        contentDescription = null,
                        tint = TeleBridgeColors.Accent,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(text = dict.directTitle, style = MaterialTheme.typography.titleMedium)
                }
                Text(
                    text = dict.directSubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp),
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 14.dp),
                    color = TeleBridgeColors.Border,
                )

                Text(
                    text = dict.smsPermissionTitle,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                )
                Row(
                    modifier = Modifier.padding(top = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = if (uiState.smsPermissionGranted) {
                            Icons.Outlined.CheckCircle
                        } else {
                            Icons.Outlined.WarningAmber
                        },
                        contentDescription = null,
                        tint = if (uiState.smsPermissionGranted) {
                            TeleBridgeColors.Success
                        } else {
                            TeleBridgeColors.Warning
                        },
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = if (uiState.smsPermissionGranted) {
                            dict.smsPermissionGranted
                        } else {
                            dict.smsPermissionMissing
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (uiState.smsPermissionGranted) {
                            TeleBridgeColors.Success
                        } else {
                            TeleBridgeColors.Warning
                        },
                    )
                }

                if (!uiState.smsPermissionGranted) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.RECEIVE_SMS) },
                        colors = ButtonDefaults.buttonColors(containerColor = TeleBridgeColors.Accent),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text(dict.grantPermissionBtn, fontSize = 12.sp)
                    }
                    Text(
                        text = dict.restrictedHint,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 14.dp),
                    color = TeleBridgeColors.Border,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(dict.autoForwardLabel, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        Text(dict.autoForwardDesc, style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(
                        checked = uiState.telegramConfig.autoForwardEnabled,
                        onCheckedChange = onToggleAutoForward,
                        colors = SwitchDefaults.colors(checkedTrackColor = TeleBridgeColors.Accent),
                    )
                }
            }
        }

        // bKash & Nagad Quick Tester Card
        item {
            FlatSectionCard {
                Text(text = dict.sampleTesterTitle, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = dict.sampleTesterSub,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp),
                )

                Spacer(modifier = Modifier.height(12.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SampleSmsData.presets.forEach { preset ->
                        OutlinedButton(
                            onClick = {
                                testSender = preset.sender
                                testBody = preset.message
                            },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        ) {
                            Text(
                                text = if (uiState.language == Language.BN) preset.labelBn else preset.labelEn,
                                fontSize = 12.sp,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = testSender,
                    onValueChange = { testSender = it },
                    label = { Text(dict.sampleSenderLabel) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = testBody,
                    onValueChange = { testBody = it },
                    label = { Text(dict.sampleBodyLabel) },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = dryRun,
                        onCheckedChange = { dryRun = it },
                    )
                    Text(
                        text = dict.dryRunLabel,
                        fontSize = 12.sp,
                        color = TeleBridgeColors.TextSecondary,
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = { onRunTestSms(testSender, testBody, dryRun) },
                    enabled = !uiState.isBusy,
                    colors = ButtonDefaults.buttonColors(containerColor = TeleBridgeColors.Accent),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (uiState.isBusy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = TeleBridgeColors.Surface,
                        )
                    } else {
                        Icon(Icons.Outlined.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(dict.runSampleTestBtn, fontSize = 13.sp)
                }

                uiState.logs.firstOrNull()?.let { latest ->
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 14.dp),
                        color = TeleBridgeColors.Border,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = latest.sender,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                        )
                        StatusInlineLabel(
                            status = latest.status,
                            label = logStatusLabel(uiState.language, latest.status),
                        )
                    }
                    Text(
                        text = latest.reason,
                        fontSize = 12.sp,
                        color = TeleBridgeColors.TextSecondary,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
}
