package com.example.momentra.ui.shell.empty.group

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.data.api.GroupInviteDto
import com.example.momentra.data.repository.GroupSliceRepository
import com.example.momentra.ui.theme.PlusJakartaSans

private val SheetBg = Color(0xFF161B26)
private val FieldBg = Color(0xFF252230)
private val Accent = Color(0xFFE8621A)
private val Red = Color(0xFFF87171)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupJoinConfirmSheet(
    code: String,
    visible: Boolean,
    onDismiss: () -> Unit,
    onJoin: (onFinished: () -> Unit) -> Unit,
    repository: GroupSliceRepository = remember { GroupSliceRepository() },
) {
    if (!visible) return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var preview by remember(code) { mutableStateOf<GroupInviteDto?>(null) }
    var loading by remember(code) { mutableStateOf(true) }
    var joining by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(code) {
        loading = true
        joining = false
        error = null
        repository.previewGroupInvite(code).fold(
            onSuccess = {
                preview = it
                loading = false
            },
            onFailure = {
                preview = null
                loading = false
                error = it.message ?: "Invite not found or no longer valid."
            },
        )
    }

    ModalBottomSheet(
        onDismissRequest = { if (!joining) onDismiss() },
        sheetState = sheetState,
        containerColor = SheetBg,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Join group moment",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = PlusJakartaSans,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "Close",
                    color = Accent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = PlusJakartaSans,
                    modifier = Modifier.clickable(enabled = !joining, onClick = onDismiss),
                )
            }
            when {
                loading -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Accent)
                }
                preview != null -> {
                    val invite = preview!!
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(FieldBg)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(invite.title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
                        Text(invite.momentTypeCode, color = Color(0xFF9E9AA8), fontSize = 12.sp, fontFamily = PlusJakartaSans)
                        Text(
                            inviteStatusCopy(invite.status),
                            color = Color(0xFF9E9AA8),
                            fontSize = 12.sp,
                            fontFamily = PlusJakartaSans,
                        )
                    }
                }
            }
            error?.let {
                Text(it, color = Red, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
            }
            val joinEnabled = !loading && !joining && preview != null
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brush.horizontalGradient(listOf(Accent, Color(0xFFFFB598))))
                    .clickable(enabled = joinEnabled) {
                        joining = true
                        error = null
                        onJoin { joining = false }
                    }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (joining) {
                    CircularProgressIndicator(
                        color = Color(0xFF14121B),
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(
                        if (loading) "Loading…" else "Join moment",
                        color = Color(0xFF14121B).copy(alpha = if (joinEnabled) 1f else 0.5f),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = PlusJakartaSans,
                    )
                }
            }
        }
    }
}

private fun inviteStatusCopy(status: String): String = when (status.uppercase()) {
    "PENDING" -> "Ready to join — you’ll enter when the organizer activates the group."
    "ACTIVE", "OPEN" -> "Ready to join this group moment."
    else -> "Invite status: $status"
}
