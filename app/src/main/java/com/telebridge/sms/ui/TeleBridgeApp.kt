package com.telebridge.sms.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.telebridge.sms.i18n.stringsFor
import com.telebridge.sms.model.SenderRule
import com.telebridge.sms.ui.components.NoticeBanner
import com.telebridge.sms.ui.screens.ChannelsScreen
import com.telebridge.sms.ui.screens.DashboardScreen
import com.telebridge.sms.ui.screens.DirectForwardScreen
import com.telebridge.sms.ui.screens.RuleEditorDialog
import com.telebridge.sms.ui.screens.RulesScreen
import com.telebridge.sms.ui.screens.SettingsStatusScreen
import com.telebridge.sms.ui.theme.TeleBridgeColors
import com.telebridge.sms.viewmodel.TeleBridgeViewModel
import kotlinx.coroutines.delay

enum class AppTab {
    DASHBOARD,
    RULES,
    CHANNELS,
    DIRECT,
    SETTINGS,
}

private data class NavDestination(
    val tab: AppTab,
    val label: String,
    val icon: ImageVector,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeleBridgeApp(
    viewModel: TeleBridgeViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val dict = stringsFor(uiState.language)

    var currentTab by remember { mutableStateOf(AppTab.DASHBOARD) }
    var showRuleDialog by remember { mutableStateOf(false) }
    var editingRule by remember { mutableStateOf<SenderRule?>(null) }

    LaunchedEffect(currentTab) {
        viewModel.refreshFromStore()
    }

    LaunchedEffect(uiState.noticeMessage) {
        if (uiState.noticeMessage != null) {
            delay(3500)
            viewModel.dismissNotice()
        }
    }

    val destinations = listOf(
        NavDestination(AppTab.DASHBOARD, dict.navDashboard, Icons.Outlined.Dashboard),
        NavDestination(AppTab.RULES, dict.navRules, Icons.Outlined.FilterList),
        NavDestination(AppTab.CHANNELS, dict.navChannels, Icons.Outlined.Send),
        NavDestination(AppTab.DIRECT, dict.navDirect, Icons.Outlined.Sms),
        NavDestination(AppTab.SETTINGS, dict.navSettings, Icons.Outlined.Settings),
    )

    val currentTitle = when (currentTab) {
        AppTab.DASHBOARD -> dict.navDashboard
        AppTab.RULES -> dict.rulesHeading
        AppTab.CHANNELS -> dict.channelsHeading
        AppTab.DIRECT -> dict.directTitle
        AppTab.SETTINGS -> dict.settingsTitle
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 600.dp

        Scaffold(
            containerColor = TeleBridgeColors.CanvasBg,
            topBar = {
                Column {
                    TopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = TeleBridgeColors.Surface,
                        ),
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .background(
                                            TeleBridgeColors.Accent,
                                            RoundedCornerShape(7.dp),
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Send,
                                        contentDescription = null,
                                        tint = TeleBridgeColors.Surface,
                                        modifier = Modifier.size(15.dp),
                                    )
                                }
                                Text(
                                    text = currentTitle,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TeleBridgeColors.TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        },
                        actions = {
                            Button(
                                onClick = {
                                    editingRule = null
                                    showRuleDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = TeleBridgeColors.Accent,
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.padding(end = 12.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = dict.newRule, fontSize = 12.sp, maxLines = 1)
                            }
                        },
                    )
                    HorizontalDivider(color = TeleBridgeColors.Border)
                }
            },
            bottomBar = {
                if (!isWideScreen) {
                    Column {
                        HorizontalDivider(color = TeleBridgeColors.Border)
                        NavigationBar(containerColor = TeleBridgeColors.Surface) {
                            destinations.forEach { dest ->
                                NavigationBarItem(
                                    selected = currentTab == dest.tab,
                                    onClick = { currentTab = dest.tab },
                                    icon = { Icon(dest.icon, contentDescription = dest.label) },
                                    label = {
                                        Text(
                                            text = dest.label,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    },
                                )
                            }
                        }
                    }
                }
            },
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                if (isWideScreen) {
                    NavigationRail(
                        containerColor = TeleBridgeColors.Surface,
                        modifier = Modifier.fillMaxHeight(),
                    ) {
                        Spacer(modifier = Modifier.padding(top = 8.dp))
                        destinations.forEach { dest ->
                            NavigationRailItem(
                                selected = currentTab == dest.tab,
                                onClick = { currentTab = dest.tab },
                                icon = { Icon(dest.icon, contentDescription = dest.label) },
                                label = {
                                    Text(
                                        text = dest.label,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                },
                            )
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    uiState.noticeMessage?.let { msg ->
                        NoticeBanner(
                            message = msg,
                            isError = uiState.noticeIsError,
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
                        )
                    }

                    when (currentTab) {
                        AppTab.DASHBOARD -> DashboardScreen(
                            uiState = uiState,
                            isWideScreen = isWideScreen,
                            onNavigateToChannels = { currentTab = AppTab.CHANNELS },
                            onNavigateToRules = { currentTab = AppTab.RULES },
                            onNavigateToTester = { currentTab = AppTab.DIRECT },
                            onClearLogs = viewModel::clearLogs,
                        )
                        AppTab.RULES -> RulesScreen(
                            uiState = uiState,
                            onOpenNewRule = {
                                editingRule = null
                                showRuleDialog = true
                            },
                            onEditRule = { rule ->
                                editingRule = rule
                                showRuleDialog = true
                            },
                            onToggleRule = viewModel::toggleRule,
                            onDeleteRule = viewModel::deleteRule,
                        )
                        AppTab.CHANNELS -> ChannelsScreen(
                            uiState = uiState,
                            onAddChannel = viewModel::verifyAndSaveChannel,
                            onTestChannel = viewModel::sendChannelTest,
                            onDeleteChannel = viewModel::deleteChannel,
                        )
                        AppTab.DIRECT -> DirectForwardScreen(
                            uiState = uiState,
                            onPermissionResult = viewModel::refreshFromStore,
                            onToggleAutoForward = viewModel::setAutoForwardEnabled,
                            onRunTestSms = viewModel::runDirectTestSms,
                        )
                        AppTab.SETTINGS -> SettingsStatusScreen(
                            uiState = uiState,
                            onRefreshConnection = viewModel::checkConnectionStatus,
                            onUpdateSettings = viewModel::updateSettings,
                            onChangeLanguage = viewModel::setLanguage,
                        )
                    }
                }
            }
        }

        if (showRuleDialog) {
            RuleEditorDialog(
                language = uiState.language,
                channels = uiState.channels,
                initialRule = editingRule,
                onDismiss = {
                    showRuleDialog = false
                    editingRule = null
                },
                onSave = { name, mode, pattern, inc, exc, chId, tmpl, enabled ->
                    viewModel.saveRule(
                        editingRuleId = editingRule?.id,
                        name = name,
                        matchMode = mode,
                        senderPattern = pattern,
                        includeKeywords = inc,
                        excludeKeywords = exc,
                        channelId = chId,
                        template = tmpl,
                        enabled = enabled,
                    )
                },
            )
        }
    }
}
