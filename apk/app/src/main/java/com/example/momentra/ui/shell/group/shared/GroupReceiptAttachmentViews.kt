package com.example.momentra.ui.shell.group.shared

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.data.api.ContributionAttachmentDto
import com.example.momentra.data.api.GroupContributionItemDto
import com.example.momentra.data.repository.GroupSliceRepository
import com.example.momentra.ui.theme.PlusJakartaSans

@Composable
fun GroupReceiptPickControl(
    muted: Color,
    field: Color,
    border: Color,
    enabled: Boolean,
    uploading: Boolean,
    fileName: String?,
    onPick: () -> Unit,
    onClear: () -> Unit,
    label: String = "Receipt",
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            label.uppercase(),
            color = muted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = PlusJakartaSans,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(field)
                .border(1.dp, border, RoundedCornerShape(10.dp))
                .clickable(enabled = enabled && !uploading, onClick = onPick)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                when {
                    uploading -> "Uploading…"
                    !fileName.isNullOrBlank() -> fileName
                    else -> "📎 Attach PDF/Img"
                },
                color = muted,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = PlusJakartaSans,
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
            if (!fileName.isNullOrBlank() && !uploading) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Clear receipt",
                    tint = muted,
                    modifier = Modifier
                        .size(16.dp)
                        .clickable(onClick = onClear),
                )
            }
        }
    }
}

@Composable
fun GroupReceiptAttachmentRow(
    contentType: String?,
    downloadUrl: String?,
    fallbackLabel: String,
    accent: Color,
    text: Color,
    muted: Color,
) {
    val context = LocalContext.current
    val isImage = (contentType ?: "").lowercase().startsWith("image/")
    val openUrl: () -> Unit = openUrl@{
        val raw = downloadUrl?.takeIf { it.isNotBlank() } ?: return@openUrl
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(raw)))
        }
    }

    if (isImage && !downloadUrl.isNullOrBlank()) {
        RemoteMemoryImage(
            url = downloadUrl,
            contentDescription = fallbackLabel,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = openUrl),
        )
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.06f))
                .clickable(enabled = !downloadUrl.isNullOrBlank(), onClick = openUrl)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(Icons.Filled.Description, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
            Text(
                fallbackLabel,
                color = text,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = PlusJakartaSans,
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
            if (!downloadUrl.isNullOrBlank()) {
                Text(
                    "Open",
                    color = accent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = PlusJakartaSans,
                )
            }
        }
    }
}

@Composable
fun AttachmentPaperclipIcon(tint: Color, modifier: Modifier = Modifier) {
    Icon(
        imageVector = Icons.Filled.AttachFile,
        contentDescription = "Has receipt",
        tint = tint,
        modifier = modifier.size(14.dp),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContributionReceiptSheet(
    momentId: String,
    item: GroupContributionItemDto,
    chrome: MomentsChrome,
    visible: Boolean,
    onDismiss: () -> Unit,
    repository: GroupSliceRepository = remember { GroupSliceRepository() },
) {
    if (!visible) return
    var attachments by remember { mutableStateOf<List<ContributionAttachmentDto>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(momentId, item.contributionId) {
        loading = true
        error = null
        repository.listContributionAttachments(momentId, item.contributionId).fold(
            onSuccess = { attachments = it },
            onFailure = {
                error = it.message
                attachments = emptyList()
            },
        )
        loading = false
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = chrome.bg,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 4.dp)
                    .size(width = 40.dp, height = 5.dp)
                    .clip(RoundedCornerShape(100.dp))
                    .background(Color.White.copy(alpha = 0.2f)),
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                "Receipt",
                color = chrome.text,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = PlusJakartaSans,
            )
            Text(
                item.displayName?.takeIf { it.isNotBlank() } ?: "Member",
                color = chrome.text,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = PlusJakartaSans,
            )
            Text(
                GroupFinanceFormat.formatMoney(item.amount, item.currencyCode ?: "INR"),
                color = chrome.secondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = PlusJakartaSans,
            )
            when {
                loading -> {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = chrome.accent, modifier = Modifier.size(28.dp), strokeWidth = 2.dp)
                    }
                }
                error != null -> Text(error!!, color = Color(0xFFF87171), fontSize = 12.sp, fontFamily = PlusJakartaSans)
                attachments.isEmpty() -> Text(
                    "No receipt attached.",
                    color = chrome.secondary,
                    fontSize = 13.sp,
                    fontFamily = PlusJakartaSans,
                    modifier = Modifier.padding(vertical = 16.dp),
                )
                else -> attachments.forEachIndexed { idx, att ->
                    GroupReceiptAttachmentRow(
                        contentType = att.contentType,
                        downloadUrl = att.downloadUrl,
                        fallbackLabel = "Receipt ${idx + 1}",
                        accent = chrome.accent,
                        text = chrome.text,
                        muted = chrome.secondary,
                    )
                }
            }
        }
    }
}
