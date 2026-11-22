package com.telebridge.sms.viewmodel

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.telebridge.sms.model.AppSettings
import com.telebridge.sms.model.ConnectionState
import com.telebridge.sms.model.DirectTelegramConfig
import com.telebridge.sms.model.ForwardLog
import com.telebridge.sms.model.Language
import com.telebridge.sms.model.LogStatus
import com.telebridge.sms.model.MatchMode
import com.telebridge.sms.model.SampleSmsData
import com.telebridge.sms.model.SenderRule
import com.telebridge.sms.model.TelegramChannel
import com.telebridge.sms.security.SecureConfigStore
import com.telebridge.sms.sms.DirectSmsReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class TeleBridgeUiState(
    val language: Language = Language.BN,
    val channels: List<TelegramChannel> = emptyList(),
    val rules: List<SenderRule> = emptyList(),
    val logs: List<ForwardLog> = emptyList(),
    val telegramConfig: DirectTelegramConfig = DirectTelegramConfig(),
    val settings: AppSettings = AppSettings(),
    val smsPermissionGranted: Boolean = false,
    val connectionState: ConnectionState = ConnectionState.IDLE,
    val connectionMessage: String = "",
    val networkAvailable: Boolean = false,
    val noticeMessage: String? = null,
    val noticeIsError: Boolean = false,
    val isBusy: Boolean = false,
)

class TeleBridgeViewModel(application: Application) : AndroidViewModel(application) {

    private val secureStore = SecureConfigStore(application)

    private val _uiState = MutableStateFlow(loadInitialState())
    val uiState: StateFlow<TeleBridgeUiState> = _uiState.asStateFlow()

    private fun loadInitialState(): TeleBridgeUiState {
        val settings = secureStore.getSettings()
        return TeleBridgeUiState(
            channels = secureStore.getChannels(),
            rules = secureStore.getRules(),
            logs = secureStore.getLogs(),
            telegramConfig = DirectTelegramConfig(
                botTokenMasked = secureStore.getMaskedBotToken(),
                hasBotToken = secureStore.getBotToken().isNotBlank(),
                defaultChatId = secureStore.getDefaultChatId(),
                autoForwardEnabled = settings.autoForwardEnabled,
            ),
            settings = settings,
            smsPermissionGranted = hasSmsPermission(),
            networkAvailable = checkDeviceNetworkAvailable(),
        )
    }

    fun refreshFromStore() {
        _uiState.update { cur ->
            val settings = secureStore.getSettings()
            cur.copy(
                channels = secureStore.getChannels(),
                rules = secureStore.getRules(),
                logs = secureStore.getLogs(),
                smsPermissionGranted = hasSmsPermission(),
                networkAvailable = checkDeviceNetworkAvailable(),
                settings = settings,
                telegramConfig = cur.telegramConfig.copy(
                    botTokenMasked = secureStore.getMaskedBotToken(),
                    hasBotToken = secureStore.getBotToken().isNotBlank(),
                    defaultChatId = secureStore.getDefaultChatId(),
                    autoForwardEnabled = settings.autoForwardEnabled,
                ),
            )
        }
    }

    fun setLanguage(lang: Language) {
        _uiState.update { it.copy(language = lang) }
    }

    fun dismissNotice() {
        _uiState.update { it.copy(noticeMessage = null) }
    }

    fun verifyAndSaveChannel(name: String, botTokenInput: String, chatIdInput: String) {
        val cleanName = name.trim()
        val cleanToken = botTokenInput.trim().ifBlank { secureStore.getBotToken() }
        val cleanChatId = chatIdInput.trim()
        if (cleanName.isEmpty() || cleanToken.isEmpty() || cleanChatId.isEmpty()) {
            showNotice(
                if (_uiState.value.language == Language.BN) {
                    "চ্যানেলের নাম, Bot Token এবং Channel ID সবগুলো লিখুন।"
                } else {
                    "Channel label, Bot Token, and Channel ID are required."
                },
                isError = true,
            )
            return
        }

        _uiState.update { it.copy(isBusy = true) }
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                verifyTelegramBotAndChat(cleanToken, cleanChatId)
            }
            if (!result.ok) {
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        noticeMessage = result.error,
                        noticeIsError = true,
                    )
                }
                return@launch
            }

            secureStore.saveBotToken(cleanToken)
            secureStore.saveDefaultChatId(cleanChatId)

            val newChannel = TelegramChannel(
                id = UUID.randomUUID().toString(),
                name = cleanName,
                chatId = cleanChatId,
                botUsername = result.botUsername,
                chatTitle = result.chatTitle,
                verified = true,
                lastTestedAt = null,
            )

            val updatedChannels = secureStore.getChannels() + newChannel
            secureStore.saveChannels(updatedChannels)

            // Auto-assign the first verified channel to any unassigned rule (such as bKash & Nagad)
            val updatedRules = secureStore.getRules().map { rule ->
                if (rule.channelId.isBlank() || rule.channelId == "default") {
                    rule.copy(channelId = newChannel.id)
                } else {
                    rule
                }
            }
            secureStore.saveRules(updatedRules)

            _uiState.update { cur ->
                cur.copy(
                    isBusy = false,
                    channels = updatedChannels,
                    rules = updatedRules,
                    telegramConfig = cur.telegramConfig.copy(
                        botTokenMasked = secureStore.getMaskedBotToken(),
                        hasBotToken = true,
                        defaultChatId = cleanChatId,
                    ),
                    noticeMessage = if (cur.language == Language.BN) {
                        "টেলিগ্রাম চ্যানেল যাচাই ও সংরক্ষণ সম্পন্ন হয়েছে।"
                    } else {
                        "Telegram channel verified and saved in Android Keystore."
                    },
                    noticeIsError = false,
                )
            }
        }
    }

    fun sendChannelTest(channelId: String) {
        val channel = _uiState.value.channels.find { it.id == channelId } ?: return
        val token = secureStore.getBotToken()
        if (token.isBlank()) {
            showNotice("Bot Token not configured.", isError = true)
            return
        }

        _uiState.update { it.copy(isBusy = true) }
        viewModelScope.launch {
            val html = "✅ <b>TeleBridge Direct Connected</b>\n\n" +
                "Target: <b>${DirectSmsReceiver.escapeHtml(channel.name)}</b>\n" +
                "Device: ${DirectSmsReceiver.escapeHtml(Build.MODEL ?: "Android")}"
            val (ok, reason) = withContext(Dispatchers.IO) {
                DirectSmsReceiver.sendTelegramMessage(token, channel.chatId, html)
            }
            val now = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date())
            val updatedChannels = if (ok) {
                _uiState.value.channels.map {
                    if (it.id == channelId) it.copy(lastTestedAt = now) else it
                }.also { secureStore.saveChannels(it) }
            } else {
                _uiState.value.channels
            }
            _uiState.update { cur ->
                cur.copy(
                    isBusy = false,
                    channels = updatedChannels,
                    noticeMessage = if (ok) {
                        if (cur.language == Language.BN) "টেস্ট মেসেজ টেলিগ্রাম চ্যানেলে পাঠানো হয়েছে!" else "Test message delivered!"
                    } else {
                        reason
                    },
                    noticeIsError = !ok,
                )
            }
        }
    }

    fun deleteChannel(channelId: String) {
        val updatedChannels = _uiState.value.channels.filterNot { it.id == channelId }
        val updatedRules = _uiState.value.rules.map { rule ->
            if (rule.channelId == channelId) rule.copy(channelId = "") else rule
        }
        secureStore.saveChannels(updatedChannels)
        secureStore.saveRules(updatedRules)
        _uiState.update { it.copy(channels = updatedChannels, rules = updatedRules) }
    }

    fun saveRule(
        editingRuleId: String?,
        name: String,
        matchMode: MatchMode,
        senderPattern: String,
        includeKeywords: String,
        excludeKeywords: String,
        channelId: String,
        template: String,
        enabled: Boolean,
    ): Boolean {
        val cleanName = name.trim()
        val cleanPattern = senderPattern.trim()
        if (cleanName.isEmpty() || cleanPattern.isEmpty()) {
            showNotice("Rule name and sender pattern are required.", isError = true)
            return false
        }
        if (matchMode == MatchMode.REGEX && !isSafeRegex(cleanPattern)) {
            showNotice("Invalid or unsafe regular expression.", isError = true)
            return false
        }

        val currentRules = _uiState.value.rules
        val updated = if (editingRuleId == null) {
            val created = SenderRule(
                id = UUID.randomUUID().toString(),
                name = cleanName,
                enabled = enabled,
                matchMode = matchMode,
                senderPattern = cleanPattern,
                includeKeywords = includeKeywords.trim(),
                excludeKeywords = excludeKeywords.trim(),
                channelId = channelId,
                template = template.ifBlank { SampleSmsData.DEFAULT_TEMPLATE },
            )
            listOf(created) + currentRules
        } else {
            currentRules.map { rule ->
                if (rule.id == editingRuleId) {
                    rule.copy(
                        name = cleanName,
                        enabled = enabled,
                        matchMode = matchMode,
                        senderPattern = cleanPattern,
                        includeKeywords = includeKeywords.trim(),
                        excludeKeywords = excludeKeywords.trim(),
                        channelId = channelId,
                        template = template.ifBlank { SampleSmsData.DEFAULT_TEMPLATE },
                    )
                } else {
                    rule
                }
            }
        }
        secureStore.saveRules(updated)
        _uiState.update { state ->
            state.copy(
                rules = updated,
                noticeMessage = if (state.language == Language.BN) "রুল সংরক্ষিত হয়েছে।" else "Rule saved.",
                noticeIsError = false,
            )
        }
        return true
    }

    fun toggleRule(ruleId: String) {
        val updated = _uiState.value.rules.map {
            if (it.id == ruleId) it.copy(enabled = !it.enabled) else it
        }
        secureStore.saveRules(updated)
        _uiState.update { it.copy(rules = updated) }
    }

    fun deleteRule(ruleId: String) {
        val updated = _uiState.value.rules.filterNot { it.id == ruleId }
        secureStore.saveRules(updated)
        _uiState.update { it.copy(rules = updated) }
    }

    fun setAutoForwardEnabled(enabled: Boolean) {
        secureStore.setAutoForwardEnabled(enabled)
        _uiState.update { cur ->
            cur.copy(
                settings = cur.settings.copy(autoForwardEnabled = enabled),
                telegramConfig = cur.telegramConfig.copy(autoForwardEnabled = enabled),
            )
        }
    }

    fun updateSettings(updated: AppSettings) {
        secureStore.saveSettings(updated)
        _uiState.update { cur ->
            cur.copy(
                settings = updated,
                telegramConfig = cur.telegramConfig.copy(autoForwardEnabled = updated.autoForwardEnabled),
            )
        }
    }

    fun checkConnectionStatus() {
        val networkOk = checkDeviceNetworkAvailable()
        _uiState.update {
            it.copy(
                networkAvailable = networkOk,
                smsPermissionGranted = hasSmsPermission(),
                connectionState = ConnectionState.CHECKING,
                connectionMessage = "",
            )
        }
        if (!networkOk) {
            _uiState.update {
                it.copy(
                    connectionState = ConnectionState.DISCONNECTED,
                    connectionMessage = if (it.language == Language.BN) {
                        "ডিভাইসে সক্রিয় ইন্টারনেট সংযোগ নেই।"
                    } else {
                        "No active internet connection on device."
                    },
                )
            }
            return
        }

        viewModelScope.launch {
            val (ok, msg) = withContext(Dispatchers.IO) { probeTelegramApi() }
            _uiState.update {
                it.copy(
                    connectionState = if (ok) ConnectionState.CONNECTED else ConnectionState.DISCONNECTED,
                    connectionMessage = msg,
                )
            }
        }
    }

    fun runDirectTestSms(sender: String, message: String, dryRun: Boolean) {
        val cleanSender = sender.trim()
        val cleanMessage = message.trim()
        if (cleanSender.isEmpty() || cleanMessage.isEmpty()) {
            showNotice("Sender and message body are required.", isError = true)
            return
        }

        val state = _uiState.value
        val activeRules = state.rules.filter { it.enabled }
        val matchedRules = activeRules.filter { rule ->
            matchesSender(cleanSender, rule.matchMode, rule.senderPattern) &&
                matchesKeywords(cleanMessage, rule.includeKeywords, rule.excludeKeywords)
        }

        val logTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val displayTime = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date())
        val deviceName = Build.MODEL ?: "Android"

        if (matchedRules.isEmpty()) {
            val ignoredLog = ForwardLog(
                id = UUID.randomUUID().toString(),
                sender = cleanSender,
                message = cleanMessage,
                device = deviceName,
                status = LogStatus.IGNORED,
                ruleName = null,
                channelName = null,
                reason = if (activeRules.isEmpty()) {
                    "No active sender rules enabled"
                } else {
                    "Sender/keywords did not match active rules"
                },
                timestamp = logTime,
            )
            secureStore.prependLogs(listOf(ignoredLog))
            _uiState.update { it.copy(logs = secureStore.getLogs()) }
            return
        }

        if (dryRun) {
            val dryLogs = matchedRules.map { rule ->
                val ch = state.channels.find { it.id == rule.channelId }
                ForwardLog(
                    id = UUID.randomUUID().toString(),
                    sender = cleanSender,
                    message = cleanMessage,
                    device = deviceName,
                    status = LogStatus.DRY_RUN,
                    ruleName = rule.name,
                    channelName = ch?.name ?: state.telegramConfig.defaultChatId.ifBlank { "Unassigned" },
                    reason = "Matched rule \"${rule.name}\" (dry-run)",
                    timestamp = logTime,
                )
            }
            secureStore.prependLogs(dryLogs)
            _uiState.update { it.copy(logs = secureStore.getLogs()) }
            return
        }

        val botToken = secureStore.getBotToken()
        val defaultChatId = secureStore.getDefaultChatId()

        _uiState.update { it.copy(isBusy = true) }
        viewModelScope.launch {
            val createdLogs = mutableListOf<ForwardLog>()
            val incrementedIds = mutableSetOf<String>()

            for (rule in matchedRules) {
                val ch = state.channels.find { it.id == rule.channelId } ?: state.channels.firstOrNull()
                val chatId = ch?.chatId?.takeIf { it.isNotBlank() } ?: defaultChatId

                if (botToken.isBlank() || chatId.isBlank()) {
                    createdLogs.add(
                        ForwardLog(
                            id = UUID.randomUUID().toString(),
                            sender = cleanSender,
                            message = cleanMessage,
                            device = deviceName,
                            status = LogStatus.FAILED,
                            ruleName = rule.name,
                            channelName = ch?.name,
                            reason = "Configure Bot Token & Channel ID in Telegram tab first",
                            timestamp = logTime,
                        ),
                    )
                    continue
                }

                val htmlText = DirectSmsReceiver.renderTemplate(
                    template = rule.template,
                    sender = cleanSender,
                    message = cleanMessage,
                    device = deviceName,
                    time = displayTime,
                )

                val (ok, reason) = withContext(Dispatchers.IO) {
                    DirectSmsReceiver.sendTelegramMessage(botToken, chatId, htmlText)
                }
                if (ok) incrementedIds.add(rule.id)
                createdLogs.add(
                    ForwardLog(
                        id = UUID.randomUUID().toString(),
                        sender = cleanSender,
                        message = cleanMessage,
                        device = deviceName,
                        status = if (ok) LogStatus.FORWARDED else LogStatus.FAILED,
                        ruleName = rule.name,
                        channelName = ch?.name ?: chatId,
                        reason = reason,
                        timestamp = logTime,
                    ),
                )
            }

            val updatedRules = state.rules.map { r ->
                if (r.id in incrementedIds) r.copy(forwardedCount = r.forwardedCount + 1) else r
            }
            secureStore.saveRules(updatedRules)
            secureStore.prependLogs(createdLogs)

            _uiState.update {
                it.copy(
                    isBusy = false,
                    rules = updatedRules,
                    logs = secureStore.getLogs(),
                )
            }
        }
    }

    fun clearLogs() {
        secureStore.saveLogs(emptyList())
        _uiState.update { it.copy(logs = emptyList()) }
    }

    private fun hasSmsPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            getApplication(),
            Manifest.permission.RECEIVE_SMS,
        ) == PackageManager.PERMISSION_GRANTED

    private fun checkDeviceNetworkAvailable(): Boolean {
        val cm = getApplication<Application>().getSystemService(ConnectivityManager::class.java)
            ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun probeTelegramApi(): Pair<Boolean, String> {
        var conn: HttpURLConnection? = null
        return try {
            val url = URL("https://api.telegram.org")
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "HEAD"
                connectTimeout = 5000
                readTimeout = 5000
            }
            val code = conn.responseCode
            if (code in 200..499) {
                true to "api.telegram.org reachable (HTTP $code)"
            } else {
                false to "Telegram returned HTTP $code"
            }
        } catch (e: Exception) {
            false to (e.message ?: "Telegram API unreachable")
        } finally {
            conn?.disconnect()
        }
    }

    private data class VerifyResult(
        val ok: Boolean,
        val botUsername: String = "",
        val chatTitle: String = "",
        val error: String = "",
    )

    private fun verifyTelegramBotAndChat(botToken: String, chatId: String): VerifyResult {
        return try {
            val meConn = (URL("https://api.telegram.org/bot$botToken/getMe").openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
            }
            val meBody = meConn.inputStream.bufferedReader().use { it.readText() }
            meConn.disconnect()
            val meJson = JSONObject(meBody)
            if (!meJson.optBoolean("ok", false)) {
                return VerifyResult(ok = false, error = meJson.optString("description", "Invalid Bot Token"))
            }
            val botUser = meJson.getJSONObject("result").optString("username", "Bot")

            val chatConn = (URL("https://api.telegram.org/bot$botToken/getChat").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 8000
                readTimeout = 8000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            }
            chatConn.outputStream.use {
                it.write(JSONObject().put("chat_id", chatId).toString().toByteArray())
            }
            val chatStream = if (chatConn.responseCode in 200..299) chatConn.inputStream else chatConn.errorStream
            val chatBody = chatStream.bufferedReader().use { it.readText() }
            chatConn.disconnect()
            val chatJson = JSONObject(chatBody)
            if (!chatJson.optBoolean("ok", false)) {
                return VerifyResult(
                    ok = false,
                    error = chatJson.optString("description", "Cannot access channel. Add bot as Admin."),
                )
            }
            val res = chatJson.getJSONObject("result")
            val title = res.optString("title").ifBlank { res.optString("username", chatId) }
            VerifyResult(ok = true, botUsername = "@$botUser", chatTitle = title)
        } catch (e: Exception) {
            VerifyResult(ok = false, error = e.message ?: "Verification failed")
        }
    }

    private fun showNotice(msg: String, isError: Boolean) {
        _uiState.update { it.copy(noticeMessage = msg, noticeIsError = isError) }
    }

    companion object {
        fun isSafeRegex(pattern: String): Boolean {
            if (pattern.isEmpty() || pattern.length > 120) return false
            if (Regex("(\\([^)]*[+*][^)]*\\)[+*])").containsMatchIn(pattern)) return false
            return try {
                Regex(pattern, RegexOption.IGNORE_CASE)
                true
            } catch (_: Exception) {
                false
            }
        }

        fun matchesSender(sender: String, mode: MatchMode, pattern: String): Boolean {
            val s = sender.trim()
            val p = pattern.trim()
            if (s.isEmpty() || p.isEmpty()) return false
            return when (mode) {
                MatchMode.EXACT -> s.equals(p, ignoreCase = true)
                MatchMode.LIST -> p.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    .any { s.equals(it, ignoreCase = true) }
                MatchMode.CONTAINS -> s.lowercase(Locale.ROOT).contains(p.lowercase(Locale.ROOT))
                MatchMode.STARTS_WITH -> s.lowercase(Locale.ROOT).startsWith(p.lowercase(Locale.ROOT))
                MatchMode.REGEX -> if (isSafeRegex(p)) {
                    Regex(p, RegexOption.IGNORE_CASE).containsMatchIn(s)
                } else {
                    false
                }
            }
        }

        fun matchesKeywords(body: String, includeCsv: String, excludeCsv: String): Boolean {
            val lower = body.lowercase(Locale.ROOT)
            val excludes = excludeCsv.split(",").map { it.trim().lowercase(Locale.ROOT) }.filter { it.isNotEmpty() }
            if (excludes.any { lower.contains(it) }) return false

            val includes = includeCsv.split(",").map { it.trim().lowercase(Locale.ROOT) }.filter { it.isNotEmpty() }
            if (includes.isNotEmpty() && includes.none { lower.contains(it) }) return false
            return true
        }
    }
}
