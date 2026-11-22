package com.telebridge.sms.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import com.telebridge.sms.model.ForwardLog
import com.telebridge.sms.model.LogStatus
import com.telebridge.sms.security.SecureConfigStore
import com.telebridge.sms.viewmodel.TeleBridgeViewModel
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.Executors

/**
 * Listens directly for incoming SMS on-device and forwards matched senders
 * (such as bKash and Nagad) directly to api.telegram.org without any webhook.
 */
class DirectSmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        if (messages.isEmpty()) return

        val store = SecureConfigStore(context.applicationContext)
        if (!store.isAutoForwardEnabled()) return

        val botToken = store.getBotToken()
        val defaultChatId = store.getDefaultChatId()
        val channels = store.getChannels().associateBy { it.id }
        val activeRules = store.getRules().filter { it.enabled }
        val settings = store.getSettings()

        // Group multi-part SMS fragments by sender address
        val grouped = linkedMapOf<String, StringBuilder>()
        for (sms in messages) {
            val sender = sms.displayOriginatingAddress ?: sms.originatingAddress ?: "Unknown"
            val body = sms.displayMessageBody ?: sms.messageBody ?: ""
            grouped.getOrPut(sender) { StringBuilder() }.append(body)
        }

        val pendingResult = goAsync()
        val timeDisplay = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date())
        val logTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val deviceName = Build.MODEL ?: "Android"

        executor.execute {
            try {
                val createdLogs = mutableListOf<ForwardLog>()
                val updatedCounts = mutableMapOf<String, Int>()

                for ((sender, bodyBuilder) in grouped) {
                    val body = bodyBuilder.toString()
                    val matched = activeRules.filter { rule ->
                        TeleBridgeViewModel.matchesSender(sender, rule.matchMode, rule.senderPattern) &&
                            TeleBridgeViewModel.matchesKeywords(body, rule.includeKeywords, rule.excludeKeywords)
                    }

                    if (matched.isEmpty()) {
                        if (!settings.ignoreUnmatchedLogs) {
                            createdLogs.add(
                                ForwardLog(
                                    id = UUID.randomUUID().toString(),
                                    sender = sender,
                                    message = body,
                                    device = deviceName,
                                    status = LogStatus.IGNORED,
                                    ruleName = null,
                                    channelName = null,
                                    reason = "Sender did not match active rules",
                                    timestamp = logTime,
                                ),
                            )
                        }
                        continue
                    }

                    for (rule in matched) {
                        val channel = channels[rule.channelId] ?: channels.values.firstOrNull()
                        val targetChatId = channel?.chatId?.takeIf { it.isNotBlank() } ?: defaultChatId

                        if (botToken.isBlank() || targetChatId.isBlank()) {
                            createdLogs.add(
                                ForwardLog(
                                    id = UUID.randomUUID().toString(),
                                    sender = sender,
                                    message = body,
                                    device = deviceName,
                                    status = LogStatus.FAILED,
                                    ruleName = rule.name,
                                    channelName = channel?.name,
                                    reason = "Telegram Bot Token or Channel ID not configured in app",
                                    timestamp = logTime,
                                ),
                            )
                            continue
                        }

                        val htmlText = renderTemplate(
                            template = rule.template,
                            sender = sender,
                            message = body,
                            device = deviceName,
                            time = timeDisplay,
                        )

                        val (ok, reason) = sendTelegramMessage(botToken, targetChatId, htmlText)
                        if (ok) {
                            updatedCounts[rule.id] = (updatedCounts[rule.id] ?: 0) + 1
                        }
                        createdLogs.add(
                            ForwardLog(
                                id = UUID.randomUUID().toString(),
                                sender = sender,
                                message = body,
                                device = deviceName,
                                status = if (ok) LogStatus.FORWARDED else LogStatus.FAILED,
                                ruleName = rule.name,
                                channelName = channel?.name ?: targetChatId,
                                reason = reason,
                                timestamp = logTime,
                            ),
                        )
                    }
                }

                if (updatedCounts.isNotEmpty()) {
                    val refreshedRules = store.getRules().map { r ->
                        val inc = updatedCounts[r.id] ?: 0
                        if (inc > 0) r.copy(forwardedCount = r.forwardedCount + inc) else r
                    }
                    store.saveRules(refreshedRules)
                }
                if (createdLogs.isNotEmpty()) {
                    store.prependLogs(createdLogs)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private val executor = Executors.newSingleThreadExecutor()

        fun escapeHtml(input: String): String = input
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")

        fun renderTemplate(
            template: String,
            sender: String,
            message: String,
            device: String,
            time: String,
        ): String = template
            .replace("{{sender}}", escapeHtml(sender))
            .replace("{{message}}", escapeHtml(message))
            .replace("{{device}}", escapeHtml(device))
            .replace("{{time}}", escapeHtml(time))

        fun sendTelegramMessage(
            botToken: String,
            chatId: String,
            htmlText: String,
        ): Pair<Boolean, String> {
            var conn: HttpURLConnection? = null
            return try {
                val url = URL("https://api.telegram.org/bot$botToken/sendMessage")
                conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 10000
                    readTimeout = 10000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                }
                val payload = JSONObject().apply {
                    put("chat_id", chatId)
                    put("text", htmlText)
                    put("parse_mode", "HTML")
                    put("disable_web_page_preview", true)
                }
                OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use {
                    it.write(payload.toString())
                }
                val code = conn.responseCode
                if (code in 200..299) {
                    true to "Forwarded directly to Telegram ($chatId)"
                } else {
                    false to "Telegram API error HTTP $code"
                }
            } catch (e: Exception) {
                false to (e.message ?: "Network error")
            } finally {
                conn?.disconnect()
            }
        }
    }
}
