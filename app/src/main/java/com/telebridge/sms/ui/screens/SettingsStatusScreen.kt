package com.telebridge.sms.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.telebridge.sms.i18n.stringsFor
import com.telebridge.sms.model.AppSettings
import com.telebridge.sms.model.ConnectionState
import com.telebridge.sms.model.Language
import com.telebridge.sms.ui.components.FlatSectionCard
import com.telebridge.sms.ui.theme.TeleBridgeColors
import com.telebridge.sms.viewmodel.TeleBridgeUiState

@Composable
fun SettingsStatusScreen(
    uiState: TeleBridgeUiState,
    onRefreshConnection: () -> Unit,
    onUpdateSettings: (AppSettings) -> Unit,
    onChangeLanguage: (Language) -> Unit,
) {
    val dict = stringsFor(uiState.language)

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            FlatSectionCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = dict.connectionStatusTitle,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedButton(
                        onClick = onRefreshConnection,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(dict.checkConnectionBtn, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val isOnline = uiState.networkAvailable
                    Icon(
                        imageVector = if (isOnline) Icons.Outlined.CloudDone else Icons.Outlined.CloudOff,
                        contentDescription = null,
                        tint = if (isOnline) TeleBridgeColors.Success else TeleBridgeColors.Error,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = if (isOnline) {
                            if (uiState.language == Language.BN) "ডিভাইস নেটওয়ার্ক: অনলাইন" else "Device Network: Online"
                        } else {
                            if (uiState.language == Language.BN) "ডিভাইস নেটওয়ার্ক: অফলাইন" else "Device Network: Offline"
                        },
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                    )
                }

                if (uiState.connectionMessage.isNotBlank()) {
                    Text(
                        text = uiState.connectionMessage,
                        fontSize = 12.sp,
                        color = when (uiState.connectionState) {
                            ConnectionState.CONNECTED -> TeleBridgeColors.Success
                            ConnectionState.DISCONNECTED -> TeleBridgeColors.Error
                            else -> TeleBridgeColors.TextSecondary
                        },
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
        }

        item {
            FlatSectionCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PrivacyTip,
                        contentDescription = null,
                        tint = TeleBridgeColors.Accent,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(text = dict.permissionsTitle, style = MaterialTheme.typography.titleMedium)
                }
                Text(
                    text = dict.permissionsBody,
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    color = TeleBridgeColors.TextSecondary,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }

        item {
            FlatSectionCard {
                Text(text = dict.safetyControlsTitle, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(12.dp))

                SettingToggleRow(
                    title = dict.autoForwardLabel,
                    subtitle = dict.autoForwardDesc,
                    checked = uiState.settings.autoForwardEnabled,
                    onCheckedChange = {
                        onUpdateSettings(uiState.settings.copy(autoForwardEnabled = it))
                    },
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = TeleBridgeColors.SurfaceMuted)

                SettingToggleRow(
                    title = dict.previewFormattedTitle,
                    subtitle = dict.previewFormattedDesc,
                    checked = uiState.settings.showFormattedPreview,
                    onCheckedChange = {
                        onUpdateSettings(uiState.settings.copy(showFormattedPreview = it))
                    },
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = TeleBridgeColors.SurfaceMuted)

                SettingToggleRow(
                    title = dict.ignoreUnmatchedTitle,
                    subtitle = dict.ignoreUnmatchedDesc,
                    checked = uiState.settings.ignoreUnmatchedLogs,
                    onCheckedChange = {
                        onUpdateSettings(uiState.settings.copy(ignoreUnmatchedLogs = it))
                    },
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = TeleBridgeColors.SurfaceMuted)

                Text(
                    text = dict.languageLabel,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = uiState.language == Language.BN,
                        onClick = { onChangeLanguage(Language.BN) },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        label = { Text("বাংলা", fontSize = 13.sp) },
                    )
                    SegmentedButton(
                        selected = uiState.language == Language.EN,
                        onClick = { onChangeLanguage(Language.EN) },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        label = { Text("English", fontSize = 13.sp) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(text = title, fontWeight = FontWeight.Medium, fontSize = 14.sp)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedTrackColor = TeleBridgeColors.Accent),
        )
    }
}
