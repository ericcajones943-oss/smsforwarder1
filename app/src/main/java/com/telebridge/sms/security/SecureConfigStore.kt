package com.telebridge.sms.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.telebridge.sms.model.AppSettings
import com.telebridge.sms.model.ForwardLog
import com.telebridge.sms.model.LogStatus
import com.telebridge.sms.model.MatchMode
import com.telebridge.sms.model.SampleSmsData
import com.telebridge.sms.model.SenderRule
import com.telebridge.sms.model.TelegramChannel
import org.json.JSONArray
import org.json.JSONObject

/**
 * Stores runtime user-entered Telegram Bot Token, Channel ID, rules, and logs inside
 * Android Keystore-backed EncryptedSharedPreferences.
 * No API keys or bot tokens are ever hardcoded in the APK.
 */
class SecureConfigStore(context: Context) {

    private val prefs: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(context.applicationContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context.applicationContext,
                PREFS_FILE,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        } catch (_: Exception) {
            context.applicationContext.getSharedPreferences(PREFS_FILE_FALLBACK, Context.MODE_PRIVATE)
        }
    }

    fun getBotToken(): String = prefs.getString(KEY_BOT_TOKEN, "") ?: ""

    fun saveBotToken(token: String) {
        prefs.edit().putString(KEY_BOT_TOKEN, token.trim()).apply()
    }

    fun getMaskedBotToken(): String {
        val raw = getBotToken()
        if (raw.isEmpty()) return ""
        return if (raw.length <= 12) "••••••••" else "${raw.take(6)}••••••${raw.takeLast(4)}"
    }

    fun getDefaultChatId(): String = prefs.getString(KEY_CHAT_ID, "") ?: ""

    fun saveDefaultChatId(chatId: String) {
        prefs.edit().putString(KEY_CHAT_ID, chatId.trim()).apply()
    }

    fun isAutoForwardEnabled(): Boolean = prefs.getBoolean(KEY_AUTO_FORWARD, true)

    fun setAutoForwardEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_FORWARD, enabled).apply()
    }

    fun getSettings(): AppSettings = AppSettings(
        autoForwardEnabled = prefs.getBoolean(KEY_AUTO_FORWARD, true),
        showFormattedPreview = prefs.getBoolean(KEY_SHOW_PREVIEW, true),
        ignoreUnmatchedLogs = prefs.getBoolean(KEY_IGNORE_UNMATCHED, false),
    )

    fun saveSettings(settings: AppSettings) {
        prefs.edit()
            .putBoolean(KEY_AUTO_FORWARD, settings.autoForwardEnabled)
            .putBoolean(KEY_SHOW_PREVIEW, settings.showFormattedPreview)
            .putBoolean(KEY_IGNORE_UNMATCHED, settings.ignoreUnmatchedLogs)
            .apply()
    }

    fun getChannels(): List<TelegramChannel> {
        val raw = prefs.getString(KEY_CHANNELS_JSON, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { idx ->
                val obj = arr.getJSONObject(idx)
                TelegramChannel(
                    id = obj.optString("id"),
                    name = obj.optString("name"),
                    chatId = obj.optString("chatId"),
                    botUsername = obj.optString("botUsername", "Bot"),
                    chatTitle = obj.optString("chatTitle", obj.optString("chatId")),
                    verified = obj.optBoolean("verified", true),
                    lastTestedAt = obj.optString("lastTestedAt").takeIf { it.isNotBlank() },
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveChannels(channels: List<TelegramChannel>) {
        val arr = JSONArray()
        channels.forEach { ch ->
            arr.put(
                JSONObject().apply {
                    put("id", ch.id)
                    put("name", ch.name)
                    put("chatId", ch.chatId)
                    put("botUsername", ch.botUsername)
                    put("chatTitle", ch.chatTitle)
                    put("verified", ch.verified)
                    put("lastTestedAt", ch.lastTestedAt ?: "")
                },
            )
        }
        prefs.edit().putString(KEY_CHANNELS_JSON, arr.toString()).apply()
    }

    fun getRules(): List<SenderRule> {
        val raw = prefs.getString(KEY_RULES_JSON, null) ?: return defaultBkashNagadRules()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { idx ->
                val obj = arr.getJSONObject(idx)
                val modeStr = obj.optString("matchMode", MatchMode.LIST.name)
                val mode = MatchMode.entries.find { it.name == modeStr } ?: MatchMode.LIST
                SenderRule(
                    id = obj.optString("id"),
                    name = obj.optString("name"),
                    enabled = obj.optBoolean("enabled", true),
                    matchMode = mode,
                    senderPattern = obj.optString("senderPattern"),
                    includeKeywords = obj.optString("includeKeywords"),
                    excludeKeywords = obj.optString("excludeKeywords"),
                    channelId = obj.optString("channelId"),
                    template = obj.optString("template", SampleSmsData.DEFAULT_TEMPLATE),
                    forwardedCount = obj.optInt("forwardedCount", 0),
                )
            }
        } catch (_: Exception) {
            defaultBkashNagadRules()
        }
    }

    fun saveRules(rules: List<SenderRule>) {
        val arr = JSONArray()
        rules.forEach { rule ->
            arr.put(
                JSONObject().apply {
                    put("id", rule.id)
                    put("name", rule.name)
                    put("enabled", rule.enabled)
                    put("matchMode", rule.matchMode.name)
                    put("senderPattern", rule.senderPattern)
                    put("includeKeywords", rule.includeKeywords)
                    put("excludeKeywords", rule.excludeKeywords)
                    put("channelId", rule.channelId)
                    put("template", rule.template)
                    put("forwardedCount", rule.forwardedCount)
                },
            )
        }
        prefs.edit().putString(KEY_RULES_JSON, arr.toString()).apply()
    }

    fun getLogs(): List<ForwardLog> {
        val raw = prefs.getString(KEY_LOGS_JSON, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { idx ->
                val obj = arr.getJSONObject(idx)
                val statusStr = obj.optString("status", LogStatus.IGNORED.name)
                val status = LogStatus.entries.find { it.name == statusStr } ?: LogStatus.IGNORED
                ForwardLog(
                    id = obj.optString("id"),
                    sender = obj.optString("sender"),
                    message = obj.optString("message"),
                    device = obj.optString("device"),
                    status = status,
                    ruleName = obj.optString("ruleName").takeIf { it.isNotBlank() },
                    channelName = obj.optString("channelName").takeIf { it.isNotBlank() },
                    reason = obj.optString("reason"),
                    timestamp = obj.optString("timestamp"),
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun prependLogs(newLogs: List<ForwardLog>) {
        val merged = (newLogs + getLogs()).take(150)
        saveLogs(merged)
    }

    fun saveLogs(logs: List<ForwardLog>) {
        val arr = JSONArray()
        logs.take(150).forEach { log ->
            arr.put(
                JSONObject().apply {
                    put("id", log.id)
                    put("sender", log.sender)
                    put("message", log.message)
                    put("device", log.device)
                    put("status", log.status.name)
                    put("ruleName", log.ruleName ?: "")
                    put("channelName", log.channelName ?: "")
                    put("reason", log.reason)
                    put("timestamp", log.timestamp)
                },
            )
        }
        prefs.edit().putString(KEY_LOGS_JSON, arr.toString()).apply()
    }

    companion object {
        private const val PREFS_FILE = "telebridge_encrypted_prefs"
        private const val PREFS_FILE_FALLBACK = "telebridge_local_prefs"
        private const val KEY_BOT_TOKEN = "telegram_bot_token"
        private const val KEY_CHAT_ID = "telegram_default_chat_id"
        private const val KEY_AUTO_FORWARD = "auto_forward_enabled"
        private const val KEY_SHOW_PREVIEW = "show_formatted_preview"
        private const val KEY_IGNORE_UNMATCHED = "ignore_unmatched_logs"
        private const val KEY_CHANNELS_JSON = "channels_json"
        private const val KEY_RULES_JSON = "rules_json"
        private const val KEY_LOGS_JSON = "logs_json"

        fun defaultBkashNagadRules(): List<SenderRule> = listOf(
            SenderRule(
                id = "rule-bkash-nagad",
                name = "bKash & Nagad",
                enabled = true,
                matchMode = MatchMode.LIST,
                senderPattern = "bKash, Nagad, 16247, 16167",
                includeKeywords = "",
                excludeKeywords = "",
                channelId = "default",
                template = SampleSmsData.DEFAULT_TEMPLATE,
            ),
        )
    }
}
