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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.telebridge.sms.i18n.stringsFor
import com.telebridge.sms.ui.components.FlatSectionCard
import com.telebridge.sms.ui.theme.TeleBridgeColors
import com.telebridge.sms.viewmodel.TeleBridgeUiState

@Composable
fun ChannelsScreen(
    uiState: TeleBridgeUiState,
    onAddChannel: (name: String, botToken: String, chatId: String) -> Unit,
    onTestChannel: (String) -> Unit,
    onDeleteChannel: (String) -> Unit,
) {
    val dict = stringsFor(uiState.language)
    var channelName by remember { mutableStateOf("") }
    var botToken by remember { mutableStateOf("") }
    var chatId by remember { mutableStateOf("") }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            FlatSectionCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = null,
                        tint = TeleBridgeColors.Accent,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(text = dict.channelGuideTitle, style = MaterialTheme.typography.titleMedium)
                }
                Text(
                    text = dict.channelsSubheading,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }

        item {
            FlatSectionCard {
                Text(text = dict.addChannelBtn, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = channelName,
                    onValueChange = { channelName = it },
                    label = { Text(dict.channelNameLabel) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = botToken,
                    onValueChange = { botToken = it },
                    label = { Text(dict.botTokenLabel) },
                    placeholder = {
                        Text(
                            if (uiState.telegramConfig.hasBotToken) {
                                uiState.telegramConfig.botTokenMasked
                            } else {
                                "123456789:ABCdefGHI..."
                            },
                            fontFamily = FontFamily.Monospace,
                        )
                    },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = chatId,
                    onValueChange = { chatId = it },
                    label = { Text(dict.channelIdLabel) },
                    placeholder = { Text("@my_channel or -1001234567890", fontFamily = FontFamily.Monospace) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { onAddChannel(channelName, botToken, chatId) },
                    enabled = !uiState.isBusy,
                    colors = ButtonDefaults.buttonColors(containerColor = TeleBridgeColors.Accent),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    if (uiState.isBusy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = TeleBridgeColors.Surface,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(dict.addChannelBtn, fontSize = 13.sp)
                }
            }
        }

        item {
            Text(
                text = dict.channelsHeading,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }

        items(uiState.channels, key = { it.id }) { ch ->
            FlatSectionCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = ch.name,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                            )
                            Icon(
                                imageVector = Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                tint = TeleBridgeColors.Success,
                                modifier = Modifier.size(14.dp),
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${ch.chatTitle} · ${ch.chatId} · ${ch.botUsername}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = TeleBridgeColors.TextSecondary,
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = { onDeleteChannel(ch.id) }) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = dict.delete,
                            tint = TeleBridgeColors.Error,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { onTestChannel(ch.id) },
                    enabled = !uiState.isBusy,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Icon(Icons.Outlined.Send, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(dict.testConnectionBtn, fontSize = 12.sp)
                }
            }
        }
    }
}
