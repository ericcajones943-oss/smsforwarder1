package com.telebridge.sms.model

enum class Language {
    BN,
    EN,
}

enum class MatchMode {
    EXACT,
    LIST,
    CONTAINS,
    STARTS_WITH,
    REGEX,
}

enum class LogStatus {
    FORWARDED,
    IGNORED,
    FAILED,
    DRY_RUN,
}

enum class ConnectionState {
    IDLE,
    CHECKING,
    CONNECTED,
    DISCONNECTED,
}

data class TelegramChannel(
    val id: String,
    val name: String,
    val chatId: String,
    val botUsername: String,
    val chatTitle: String,
    val verified: Boolean,
    val lastTestedAt: String?,
)

data class SenderRule(
    val id: String,
    val name: String,
    val enabled: Boolean,
    val matchMode: MatchMode,
    val senderPattern: String,
    val includeKeywords: String,
    val excludeKeywords: String,
    val channelId: String,
    val template: String,
    val forwardedCount: Int = 0,
)

data class ForwardLog(
    val id: String,
    val sender: String,
    val message: String,
    val device: String,
    val status: LogStatus,
    val ruleName: String?,
    val channelName: String?,
    val reason: String,
    val timestamp: String,
)

data class DirectTelegramConfig(
    val botTokenMasked: String = "",
    val hasBotToken: Boolean = false,
    val defaultChatId: String = "",
    val autoForwardEnabled: Boolean = true,
)

data class AppSettings(
    val autoForwardEnabled: Boolean = true,
    val showFormattedPreview: Boolean = true,
    val ignoreUnmatchedLogs: Boolean = false,
)

data class SampleSmsPreset(
    val id: String,
    val labelBn: String,
    val labelEn: String,
    val sender: String,
    val message: String,
)

object SampleSmsData {
    const val DEFAULT_TEMPLATE =
        "📩 <b>New SMS from {{sender}}</b>\n\n{{message}}\n\n📱 {{device}} · 🕒 {{time}}"

    val presets: List<SampleSmsPreset> = listOf(
        SampleSmsPreset(
            id = "bkash",
            labelBn = "বিকাশ (bKash) টেস্ট",
            labelEn = "bKash Test SMS",
            sender = "bKash",
            message = "You have received Tk 1,500.00 from 01711000000. Fee Tk 0.00. Balance Tk 12,450.00. TrxID TEST1234.",
        ),
        SampleSmsPreset(
            id = "nagad",
            labelBn = "নগদ (Nagad) টেস্ট",
            labelEn = "Nagad Test SMS",
            sender = "Nagad",
            message = "Cash In Tk 2,000.00 from 01811000000 successful. Balance: Tk 8,300.00. TxnID: TEST5678.",
        ),
    )
}
