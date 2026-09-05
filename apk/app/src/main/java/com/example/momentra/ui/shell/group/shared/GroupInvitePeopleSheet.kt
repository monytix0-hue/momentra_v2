package com.example.momentra.ui.shell.group.shared

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.R
import com.example.momentra.data.api.GroupParticipantDto
import com.example.momentra.data.repository.GroupSliceRepository
import com.example.momentra.ui.shell.empty.group.GroupInviteLink
import com.example.momentra.ui.shell.empty.group.generateInviteQrBitmap
import com.example.momentra.ui.shell.empty.group.inviteMessage
import com.example.momentra.ui.shell.empty.group.sendInviteSms
import com.example.momentra.ui.shell.empty.group.sendInviteWhatsApp
import com.example.momentra.ui.shell.empty.group.shareInviteLink
import com.example.momentra.ui.shell.empty.group.shareInviteQr
import com.example.momentra.ui.theme.PlusJakartaSans
import kotlinx.coroutines.launch
import java.util.Locale

private val MANAGE_ROLE_OPTIONS = listOf(
    "PARTICIPANT" to "Member",
    "ORGANIZER" to "Organizer",
    "OBSERVER" to "Viewer",
)

private fun displayRoleLabel(roleCode: String?, isGuest: Boolean = false): String {
    if (isGuest) return "Guest"
    return when (roleCode?.uppercase(Locale.US)) {
        "ORGANIZER", "CO_ORGANIZER" -> "Organizer"
        "OBSERVER", "VIEWER" -> "Viewer"
        "RESIDENT" -> "Resident"
        "CONTRIBUTOR" -> "Contributor"
        else -> "Member"
    }
}

private fun uiRoleCode(roleCode: String?): String = when (roleCode?.uppercase(Locale.US)) {
    "ORGANIZER", "CO_ORGANIZER" -> "ORGANIZER"
    "OBSERVER", "VIEWER" -> "OBSERVER"
    else -> "PARTICIPANT"
}

/** Invite link/QR + active members manage (organizer role edit / remove). */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GroupInvitePeopleSheet(
    momentId: String,
    momentTitle: String,
    momentTypeCode: String,
    visible: Boolean,
    onDismiss: () -> Unit,
    onSaved: () -> Unit = {},
    currentUserId: String? = null,
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
    var actionError by remember { mutableStateOf<String?>(null) }
    var busyParticipantId by remember { mutableStateOf<String?>(null) }
    var removeTarget by remember { mutableStateOf<GroupParticipantDto?>(null) }
    var copied by remember { mutableStateOf(false) }
    var guestName by remember { mutableStateOf("") }
    var addingGuest by remember { mutableStateOf(false) }

    fun refreshParticipants() {
        scope.launch {
            repository.getParticipants(momentId).fold(
                onSuccess = { participants = it.participants },
                onFailure = { /* list is best-effort */ },
            )
        }
    }

    LaunchedEffect(momentId, momentTitle, momentTypeCode) {
        inviteCode = null
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
    val qrPayload = inviteCode?.let { GroupInviteLink.qrPayload(it) }
    val qrSizePx = with(LocalDensity.current) { 144.dp.roundToPx() }
    val qrBitmap = remember(qrPayload, qrSizePx) {
        qrPayload?.let { generateInviteQrBitmap(it, qrSizePx) }?.asImageBitmap()
    }
    val activeMembers = participants.filter { it.status.equals("ACTIVE", ignoreCase = true) }
    val viewerIsOrganizer = activeMembers.any { p ->
        p.userId != null &&
            p.userId == currentUserId &&
            p.roleCode.uppercase(Locale.US) in setOf("ORGANIZER", "CO_ORGANIZER")
    }

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
            TripSheetHeaderRow(
                title = "Invite People",
                subtitle = "Share a link or QR so people can join",
                iconRes = R.drawable.ic_group_qa_userplus,
                accent = TripSheetTokens.Accent,
            )

            TripInviteShareSection(
                minting = minting,
                displayPath = displayPath,
                mintError = mintError,
                copied = copied,
                copyText = copyText,
                qrBitmap = qrBitmap,
                onCopy = {
                    val text = copyText ?: return@TripInviteShareSection
                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    cm.setPrimaryClip(ClipData.newPlainText("invite", text))
                    copied = true
                },
                onShareLink = {
                    val text = copyText ?: return@TripInviteShareSection
                    shareInviteLink(context, text)
                },
                onShareQr = {
                    val text = copyText ?: return@TripInviteShareSection
                    val bitmap = qrPayload?.let { generateInviteQrBitmap(it, 512) } ?: return@TripInviteShareSection
                    shareInviteQr(context, bitmap, text)
                },
                onMessages = {
                    val text = copyText ?: return@TripInviteShareSection
                    sendInviteSms(context, null, inviteMessage(momentTitle, text))
                },
                onWhatsApp = {
                    val text = copyText ?: return@TripInviteShareSection
                    sendInviteWhatsApp(context, null, inviteMessage(momentTitle, text))
                },
            )

            if (viewerIsOrganizer) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TripFieldLabel("Add guest")
                    Text(
                        "Guests have no account — organizers can add them to expenses.",
                        color = TripSheetTokens.Muted,
                        fontSize = 11.sp,
                        fontFamily = PlusJakartaSans,
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        BasicTextField(
                            value = guestName,
                            onValueChange = { guestName = it },
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(
                                color = TripSheetTokens.Text,
                                fontSize = 13.sp,
                                fontFamily = PlusJakartaSans,
                            ),
                            cursorBrush = SolidColor(TripSheetTokens.Accent),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(TripSheetTokens.Field)
                                .border(1.dp, TripSheetTokens.Border, RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 12.dp)
                                .testTag("group.invite.guestName"),
                            decorationBox = { inner ->
                                if (guestName.isEmpty()) {
                                    Text(
                                        "Guest name",
                                        color = TripSheetTokens.Muted,
                                        fontSize = 13.sp,
                                        fontFamily = PlusJakartaSans,
                                    )
                                }
                                inner()
                            },
                        )
                        Text(
                            if (addingGuest) "…" else "Add",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = PlusJakartaSans,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(TripSheetTokens.Accent)
                                .clickable(
                                    enabled = !addingGuest && guestName.trim().isNotEmpty(),
                                ) {
                                    val name = guestName.trim()
                                    if (name.isEmpty()) return@clickable
                                    scope.launch {
                                        addingGuest = true
                                        actionError = null
                                        repository.addParticipant(momentId, name).fold(
                                            onSuccess = {
                                                guestName = ""
                                                addingGuest = false
                                                refreshParticipants()
                                                onSaved()
                                            },
                                            onFailure = {
                                                addingGuest = false
                                                actionError = it.message
                                            },
                                        )
                                    }
                                }
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                                .testTag("group.invite.addGuest"),
                        )
                    }
                }
            }

            if (activeMembers.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TripFieldLabel("Active members (${activeMembers.size})")
                    activeMembers.forEach { p ->
                        InviteActiveMemberRow(
                            participant = p,
                            viewerIsOrganizer = viewerIsOrganizer,
                            busy = busyParticipantId == p.participantId,
                            onRoleChange = { nextRole ->
                                scope.launch {
                                    busyParticipantId = p.participantId
                                    actionError = null
                                    repository.updateParticipantRole(
                                        momentId = momentId,
                                        participantId = p.participantId,
                                        roleCode = nextRole,
                                    ).fold(
                                        onSuccess = {
                                            busyParticipantId = null
                                            refreshParticipants()
                                            onSaved()
                                        },
                                        onFailure = {
                                            busyParticipantId = null
                                            actionError = it.message
                                        },
                                    )
                                }
                            },
                            onRemove = { removeTarget = p },
                        )
                    }
                }
            }

            actionError?.let {
                Text(it, color = Color(0xFFF87171), fontSize = 12.sp, fontFamily = PlusJakartaSans)
            }

            Text(
                "Share via Messages or WhatsApp so they can join",
                color = TripSheetTokens.Muted,
                fontSize = 12.sp,
                fontFamily = PlusJakartaSans,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    removeTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { removeTarget = null },
            title = { Text("Remove member?", fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Remove ${target.displayName?.takeIf { it.isNotBlank() } ?: "this member"} from the group?",
                    fontFamily = PlusJakartaSans,
                    fontSize = 14.sp,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val p = target
                        removeTarget = null
                        scope.launch {
                            busyParticipantId = p.participantId
                            actionError = null
                            repository.removeParticipant(momentId, p.participantId).fold(
                                onSuccess = {
                                    busyParticipantId = null
                                    refreshParticipants()
                                    onSaved()
                                },
                                onFailure = {
                                    busyParticipantId = null
                                    actionError = it.message
                                },
                            )
                        }
                    },
                ) {
                    Text("Remove", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { removeTarget = null }) {
                    Text("Cancel", color = TripSheetTokens.Muted)
                }
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InviteActiveMemberRow(
    participant: GroupParticipantDto,
    viewerIsOrganizer: Boolean,
    busy: Boolean,
    onRoleChange: (String) -> Unit,
    onRemove: () -> Unit,
) {
    val name = participant.displayName?.takeIf { it.isNotBlank() } ?: "Member"
    val selectedUiRole = uiRoleCode(participant.roleCode)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(TripSheetTokens.Field)
            .border(1.dp, TripSheetTokens.Border, RoundedCornerShape(8.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(TripSheetTokens.Accent.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                    color = TripSheetTokens.Accent,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = PlusJakartaSans,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(name, color = TripSheetTokens.Text, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, fontFamily = PlusJakartaSans)
                    if (participant.isGuest) {
                        Text(
                            "Guest",
                            color = TripSheetTokens.Accent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = PlusJakartaSans,
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(TripSheetTokens.Accent.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }
                Text(
                    participant.roleLabel ?: displayRoleLabel(participant.roleCode, participant.isGuest),
                    color = TripSheetTokens.Muted,
                    fontSize = 11.sp,
                    fontFamily = PlusJakartaSans,
                )
            }
            Text(
                "Active",
                color = TripFormTokens.Green,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = PlusJakartaSans,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(TripFormTokens.Green.copy(alpha = 0.12f))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
            if (viewerIsOrganizer) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Remove",
                    tint = if (busy) TripSheetTokens.Muted else Color(0xFFEF4444),
                    modifier = Modifier
                        .size(18.dp)
                        .then(if (busy) Modifier else Modifier.clickable(onClick = onRemove)),
                )
            }
        }
        if (viewerIsOrganizer && !participant.isGuest) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MANAGE_ROLE_OPTIONS.forEach { (code, label) ->
                    val selected = selectedUiRole == code
                    Text(
                        label,
                        color = if (selected) TripSheetTokens.Text else TripSheetTokens.Muted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = PlusJakartaSans,
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (selected) TripSheetTokens.Accent else TripSheetTokens.Bg)
                            .border(
                                1.dp,
                                if (selected) TripSheetTokens.Accent else TripSheetTokens.Border,
                                RoundedCornerShape(999.dp),
                            )
                            .then(
                                if (!busy && !selected) {
                                    Modifier.clickable { onRoleChange(code) }
                                } else {
                                    Modifier
                                },
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            }
        }
    }
}
