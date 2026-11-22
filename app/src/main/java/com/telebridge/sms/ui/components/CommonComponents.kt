package com.telebridge.sms.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.telebridge.sms.model.LogStatus
import com.telebridge.sms.ui.theme.TeleBridgeColors

@Composable
fun FlatSectionCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = TeleBridgeColors.Surface,
        border = BorderStroke(1.dp, TeleBridgeColors.Border),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content,
        )
    }
}

@Composable
fun KpiMetricCard(
    label: String,
    value: Int,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = TeleBridgeColors.Surface,
        border = BorderStroke(1.dp, TeleBridgeColors.Border),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = TeleBridgeColors.TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = value.toString(),
                modifier = Modifier.padding(top = 6.dp),
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                fontSize = 24.sp,
                color = TeleBridgeColors.TextPrimary,
            )
        }
    }
}

@Composable
fun StatusInlineLabel(
    status: LogStatus,
    label: String,
) {
    val (color, icon) = when (status) {
        LogStatus.FORWARDED -> TeleBridgeColors.Success to Icons.Outlined.CheckCircle
        LogStatus.DRY_RUN -> TeleBridgeColors.Accent to Icons.Outlined.CheckCircle
        LogStatus.FAILED -> TeleBridgeColors.Error to Icons.Outlined.ErrorOutline
        LogStatus.IGNORED -> TeleBridgeColors.Warning to Icons.Outlined.Info
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(14.dp),
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = color,
            maxLines = 1,
        )
    }
}

@Composable
fun NoticeBanner(
    message: String,
    isError: Boolean,
    modifier: Modifier = Modifier,
) {
    val bg = if (isError) TeleBridgeColors.ErrorSoft else TeleBridgeColors.SuccessSoft
    val border = if (isError) TeleBridgeColors.Error.copy(alpha = 0.25f) else TeleBridgeColors.Success.copy(alpha = 0.25f)
    val ink = if (isError) TeleBridgeColors.Error else TeleBridgeColors.Success

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = bg,
        border = BorderStroke(1.dp, border),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = if (isError) Icons.Outlined.ErrorOutline else Icons.Outlined.CheckCircle,
                contentDescription = null,
                tint = ink,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = message,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = ink,
            )
        }
    }
}
