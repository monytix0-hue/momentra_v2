package com.example.momentra.ui.shell.business.setup

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.data.api.CompanyInviteDto
import com.example.momentra.ui.shell.empty.group.generateInviteQrBitmap
import com.example.momentra.ui.theme.generated.BusinessTokens
import kotlinx.coroutines.launch

/** Company invite URL helpers — prefer `/c/` scheme for redeem compatibility. */
object CompanyInviteLink {
    private const val HTTPS_BASE = "https://momentra.tech/c"

    fun displayPath(code: String): String = "$HTTPS_BASE/${code.trim().lowercase()}"

    fun copyText(code: String): String = displayPath(code)

    fun qrPayload(code: String): String = displayPath(code)
}

/**
 * Business-themed invite block with Group-style code / QR / share mechanics.
 * Keep in lockstep with `BusinessSetupInviteSection.swift`.
 * When [embedded] is true (People & Roles), content renders without an outer SectionCard.
 */
@Composable
fun BusinessSetupInviteSection(
    theme: BusinessSetupTheme,
    companyId: String?,
    mintInvite: suspend (String) -> CompanyInviteDto?,
    modifier: Modifier = Modifier,
    embedded: Boolean = true,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var invite by remember { mutableStateOf<CompanyInviteDto?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var copied by remember { mutableStateOf(false) }
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val code = invite?.inviteCode
    val displayPath = code?.let { CompanyInviteLink.displayPath(it) }
    val copyText = displayPath
    val qrPayload = code?.let { CompanyInviteLink.qrPayload(it) }

    fun mint() {
        val id = companyId?.takeIf { it.isNotBlank() } ?: return
        scope.launch {
            loading = true
            error = null
            val minted = mintInvite(id)
            invite = minted
            if (minted == null) error = "Couldn’t create invite. Try again."
            loading = false
        }
    }

    LaunchedEffect(companyId) {
        if (!companyId.isNullOrBlank() && invite == null) mint()
    }

    LaunchedEffect(qrPayload) {
        qrBitmap = qrPayload?.let { generateInviteQrBitmap(it, 144) }
    }

    val body: @Composable () -> Unit = {
        error?.let {
            Text(it, color = BusinessTokens.Colors.SignalDanger, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }

        when {
            companyId.isNullOrBlank() -> {
                Text(
                    text = "Activate to mint an invite for your new company, or open setup after create.",
                    color = theme.hint,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
            invite != null && displayPath != null -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(theme.inset)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = displayPath,
                        color = BusinessTokens.Colors.TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = if (copied) "Copied" else "Copy",
                        color = theme.accent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable {
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cm.setPrimaryClip(ClipData.newPlainText("invite", copyText))
                            copied = true
                        },
                    )
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = "OR SCAN TO JOIN",
                        color = theme.accent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                    )
                    val bmp = qrBitmap
                    if (bmp != null) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Invite QR",
                            modifier = Modifier
                                .size(176.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(androidx.compose.ui.graphics.Color.White)
                                .padding(16.dp),
                        )
                    } else {
                        CircularProgressIndicator(color = theme.accent, modifier = Modifier.size(48.dp))
                    }
                    Text(
                        text = "Anyone who scans can join this company.",
                        color = theme.hint,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlineAction(theme, "Share QR", Modifier.weight(1f)) {
                        val payload = qrPayload ?: return@OutlineAction
                        val full = generateInviteQrBitmap(payload, 512)
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, copyText)
                        }
                        context.startActivity(Intent.createChooser(send, "Share invite"))
                        full.recycle()
                    }
                    OutlineAction(theme, "Share link", Modifier.weight(1f)) {
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, copyText)
                        }
                        context.startActivity(Intent.createChooser(send, "Share invite"))
                    }
                }
            }
            else -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(theme.accent)
                        .clickable(enabled = !loading) { mint() },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    if (loading) {
                        CircularProgressIndicator(
                            color = theme.onAccent,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Text(
                        text = if (loading) "Creating invite…" else "Create invite link",
                        color = theme.onAccent,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = if (loading) 8.dp else 0.dp),
                    )
                }
            }
        }
    }

    if (embedded) {
        Column(
            modifier = modifier.fillMaxWidth().padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Invite your team",
                color = BusinessTokens.Colors.TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Share a link or QR so teammates can join this company.",
                color = theme.hint,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            )
            body()
        }
    } else {
        BusinessSetupDocumentKit.SectionCard(
            theme = theme,
            index = 99,
            title = "Invite your team",
            subtitle = "Share a link or QR so teammates can join this company.",
            modifier = modifier,
        ) {
            body()
        }
    }
}

@Composable
private fun OutlineAction(
    theme: BusinessSetupTheme,
    title: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Text(
        text = title,
        color = theme.accent,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        modifier = modifier
            .height(44.dp)
            .border(1.dp, theme.accent, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
    )
}
