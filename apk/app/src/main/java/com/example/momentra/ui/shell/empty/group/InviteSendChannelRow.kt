package com.example.momentra.ui.shell.empty.group

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.ui.theme.PlusJakartaSans

/** Messages / WhatsApp action row for invite surfaces. */
@Composable
fun InviteSendChannelRow(
    enabled: Boolean,
    accent: Color,
    onMessages: () -> Unit,
    onWhatsApp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        InviteChannelOutlineButton(
            label = "Messages",
            enabled = enabled,
            accent = accent,
            onClick = onMessages,
            modifier = Modifier.weight(1f),
        )
        InviteChannelOutlineButton(
            label = "WhatsApp",
            enabled = enabled,
            accent = accent,
            onClick = onWhatsApp,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun InviteChannelOutlineButton(
    label: String,
    enabled: Boolean,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val stroke = if (enabled) accent else accent.copy(alpha = 0.35f)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, stroke, RoundedCornerShape(12.dp))
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = stroke,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = PlusJakartaSans,
        )
    }
}

data class PendingInviteSend(
    val phone: String?,
    val message: String,
)

@Composable
fun InviteSendChooserDialog(
    pending: PendingInviteSend?,
    accent: Color,
    onMessages: (PendingInviteSend) -> Unit,
    onWhatsApp: (PendingInviteSend) -> Unit,
    onDismiss: () -> Unit,
) {
    if (pending == null) return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Send invite via…",
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Share the Momentra invite link through Messages or WhatsApp.",
                    fontSize = 13.sp,
                    fontFamily = PlusJakartaSans,
                    color = Color(0xFF9E9AA8),
                )
                InviteSendChannelRow(
                    enabled = true,
                    accent = accent,
                    onMessages = { onMessages(pending) },
                    onWhatsApp = { onWhatsApp(pending) },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Not now", fontFamily = PlusJakartaSans, color = accent)
            }
        },
    )
}
