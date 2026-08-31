package com.example.momentra.ui.shell.group

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.data.api.GroupParticipantDto
import com.example.momentra.data.repository.GroupSliceRepository
import com.example.momentra.ui.shell.empty.group.GroupInviteLink
import com.example.momentra.ui.theme.PlusJakartaSans
import kotlinx.coroutines.launch
import java.util.Locale

/** Trip Quick Add — invite link + add participant (Figma 575:15497 chrome). */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GroupInvitePeopleSheet(
    momentId: String,
    momentTitle: String,
    momentTypeCode: String,
    visible: Boolean,
    onDismiss: () -> Unit,
    onSaved: () -> Unit = {},
    repository: GroupSliceRepository = remember { GroupSliceRepository() },
) {
    if (!visible) return
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var inviteCode by remember { mutableStateOf<String?>(null) }
    var minting by remember { mutableStateOf(true) }
    var mintError by remember { mutableStateOf<String?>(null) }
    var participants by remember { mutableStateOf<List<GroupParticipantDto>>(emptyList()) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("PARTICIPANT") }
    var submitting by remember { mutableStateOf(false) }
    var formError by remember { mutableStateOf<String?>(null) }
    var copied by remember { mutableStateOf(false) }

    fun refreshParticipants() {
        scope.launch {
            repository.getParticipants(momentId).fold(
                onSuccess = { participants = it.participants },
                onFailure = { /* list is best-effort */ },
            )
        }
    }

    LaunchedEffect(momentId, momentTitle, momentTypeCode) {
        minting = true
        mintError = null
        repository.mintInviteForMoment(
            title = momentTitle.ifBlank { "Trip" },
            momentTypeCode = momentTypeCode.ifBlank { "TRIP" },
            momentId = momentId,
        ).fold(
            onSuccess = {
                inviteCode = it.inviteCode
                minting = false
            },
            onFailure = {
                mintError = it.message ?: "Could not mint invite"
                minting = false
            },
        )
        refreshParticipants()
    }

    val displayPath = inviteCode?.let { GroupInviteLink.displayPath(it) }
    val copyText = inviteCode?.let { GroupInviteLink.copyText(it) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = TripSheet.Bg,
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
                .padding(horizontal = 24.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(TripSheet.Orange.copy(alpha = 0.18f))
                        .border(1.dp, TripSheet.Orange.copy(alpha = 0.35f), RoundedCornerShape(18.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("👥", fontSize = 16.sp)
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        "Invite people",
                        color = TripSheet.Text,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = PlusJakartaSans,
                    )
                    Text(
                        "Share a link or add someone to this trip",
                        color = TripSheet.Muted,
                        fontSize = 12.sp,
                        fontFamily = PlusJakartaSans,
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "INVITE LINK",
                    color = TripSheet.Muted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = PlusJakartaSans,
                )
                when {
                    minting -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(TripSheet.Field)
                                .border(1.dp, TripSheet.Border, RoundedCornerShape(8.dp))
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            CircularProgressIndicator(
                                color = TripSheet.Purple,
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                            )
                            Text(
                                "Minting invite…",
                                color = TripSheet.Muted,
                                fontSize = 13.sp,
                                fontFamily = PlusJakartaSans,
                            )
                        }
                    }
                    displayPath != null -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(TripSheet.Field)
                                .border(1.dp, TripSheet.Border, RoundedCornerShape(8.dp))
                                .clickable {
                                    val text = copyText ?: return@clickable
                                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    cm.setPrimaryClip(ClipData.newPlainText("invite", text))
                                    copied = true
                                }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                displayPath,
                                color = TripSheet.Text,
                                fontSize = 13.sp,
                                fontFamily = PlusJakartaSans,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                if (copied) "Copied" else "Copy",
                                color = TripSheet.Purple,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = PlusJakartaSans,
                            )
                        }
                    }
                    else -> {
                        Text(
                            mintError ?: "Invite unavailable",
                            color = Color(0xFFF87171),
                            fontSize = 12.sp,
                            fontFamily = PlusJakartaSans,
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "NAME",
                    color = TripSheet.Muted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = PlusJakartaSans,
                )
                InviteField(name, { name = it }, "Display name")
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "EMAIL (OPTIONAL)",
                    color = TripSheet.Muted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = PlusJakartaSans,
                )
                InviteField(email, { email = it }, "name@email.com")
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "ROLE",
                    color = TripSheet.Muted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = PlusJakartaSans,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(
                        "PARTICIPANT" to "Guest",
                        "ORGANIZER" to "Organizer",
                    ).forEach { (code, label) ->
                        val selected = role == code
                        Text(
                            label,
                            color = if (selected) Color.White else TripSheet.Muted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = PlusJakartaSans,
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(if (selected) TripSheet.Orange else TripSheet.Field)
                                .border(
                                    1.dp,
                                    if (selected) TripSheet.Orange else TripSheet.Border,
                                    RoundedCornerShape(999.dp),
                                )
                                .clickable { role = code }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                        )
                    }
                }
            }

            formError?.let {
                Text(it, color = Color(0xFFF87171), fontSize = 12.sp, fontFamily = PlusJakartaSans)
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.horizontalGradient(listOf(TripSheet.Orange, TripSheet.Peach)))
                    .then(
                        if (name.isNotBlank() && !submitting) {
                            Modifier.clickable {
                                scope.launch {
                                    submitting = true
                                    formError = null
                                    repository.addParticipant(
                                        momentId = momentId,
                                        displayName = name.trim(),
                                        roleCode = role,
                                        email = email.trim().ifBlank { null },
                                    ).fold(
                                        onSuccess = {
                                            submitting = false
                                            name = ""
                                            email = ""
                                            refreshParticipants()
                                            onSaved()
                                        },
                                        onFailure = {
                                            submitting = false
                                            formError = it.message
                                        },
                                    )
                                }
                            }
                        } else {
                            Modifier
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (submitting) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                } else {
                    Text(
                        "Add participant",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = PlusJakartaSans,
                    )
                }
            }

            if (participants.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "ON THIS TRIP",
                        color = TripSheet.Muted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = PlusJakartaSans,
                    )
                    participants.forEach { p ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(TripSheet.Field)
                                .border(1.dp, TripSheet.Border, RoundedCornerShape(8.dp))
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                p.displayName?.takeIf { it.isNotBlank() } ?: "Participant",
                                color = TripSheet.Text,
                                fontSize = 14.sp,
                                fontFamily = PlusJakartaSans,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                (p.roleCode.ifBlank { "PARTICIPANT" }).lowercase(Locale.US)
                                    .replaceFirstChar { it.titlecase(Locale.US) },
                                color = TripSheet.Muted,
                                fontSize = 11.sp,
                                fontFamily = PlusJakartaSans,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InviteField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(TripSheet.Field)
            .border(1.dp, TripSheet.Border, RoundedCornerShape(8.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        if (value.isEmpty()) {
            Text(
                placeholder,
                color = TripSheet.Muted.copy(alpha = 0.7f),
                fontSize = 14.sp,
                fontFamily = PlusJakartaSans,
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(color = TripSheet.Text, fontSize = 14.sp, fontFamily = PlusJakartaSans),
            cursorBrush = SolidColor(TripSheet.Purple),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
