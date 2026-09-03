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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import com.example.momentra.ui.shell.empty.group.generateInviteQrBitmap
import com.example.momentra.ui.shell.empty.group.shareInviteLink
import com.example.momentra.ui.shell.empty.group.shareInviteQr
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.R
import com.example.momentra.data.api.GroupParticipantDto
import com.example.momentra.data.repository.GroupSliceRepository
import com.example.momentra.ui.shell.empty.group.GroupInviteLink
import com.example.momentra.ui.theme.PlusJakartaSans
import kotlinx.coroutines.launch
import java.util.Locale

/** Trip Quick Add — invite link + add participant (Figma 581:13699 / TripSheetFormComponents chrome). */
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
    val inviteSubtitle = groupExperienceFamilyFor(momentTypeCode).invitePeopleSubtitle()

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
                subtitle = inviteSubtitle,
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
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    TripFieldLabel("Name")
                    TripSheetField(name, { name = it }, "Aarav Mehta")
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    TripFieldLabel("Email/Phone")
                    TripSheetField(
                        value = email,
                        onValueChange = { email = it },
                        placeholder = "aarav@email.com",
                        keyboardType = KeyboardType.Email,
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TripFieldLabel("Assigned Role")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(
                        "PARTICIPANT" to "Member",
                        "ORGANIZER" to "Organizer",
                        "VIEWER" to "Viewer",
                    ).forEach { (code, label) ->
                        val selected = role == code
                        Text(
                            label,
                            color = if (selected) TripSheetTokens.Text else TripSheetTokens.Muted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = PlusJakartaSans,
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(if (selected) TripSheetTokens.Accent else TripSheetTokens.Field)
                                .border(
                                    1.dp,
                                    if (selected) TripSheetTokens.Accent else TripSheetTokens.Border,
                                    RoundedCornerShape(999.dp),
                                )
                                .clickable { role = code }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                        )
                    }
                }
            }

            if (participants.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TripFieldLabel("Already Invited (${participants.size})")
                    participants.forEach { p ->
                        TripParticipantListRow(
                            displayName = p.displayName?.takeIf { it.isNotBlank() } ?: "Participant",
                            status = p.status?.takeIf { it.isNotBlank() } ?: "Member",
                            roleLabel = (p.status ?: "Pending").lowercase(Locale.US)
                                .replaceFirstChar { it.titlecase(Locale.US) },
                        )
                    }
                }
            }

            formError?.let {
                Text(it, color = Color(0xFFF87171), fontSize = 12.sp, fontFamily = PlusJakartaSans)
            }

            TripPrimaryCta(
                label = "Send Invite",
                enabled = name.isNotBlank(),
                loading = submitting,
                footer = "Share the invite link so they can join",
                onClick = {
                    scope.launch {
                        submitting = true
                        formError = null
                        val contact = email.trim().ifBlank { null }
                        val isPhone = contact != null &&
                            contact.any { it.isDigit() } &&
                            !contact.contains('@')
                        repository.addParticipant(
                            momentId = momentId,
                            displayName = name.trim(),
                            roleCode = role,
                            email = if (isPhone) null else contact,
                            phone = if (isPhone) contact else null,
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
                },
            )
        }
    }
}
