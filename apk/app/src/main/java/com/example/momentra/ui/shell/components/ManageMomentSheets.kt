package com.example.momentra.ui.shell.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.R
import com.example.momentra.data.api.ApiClient
import com.example.momentra.data.repository.MomentLifecycleRepository
import com.example.momentra.domain.AppContext
import kotlinx.coroutines.launch

private val SheetBg = Color(0xFF161B26)
private val RowBg = Color(0xFF14121B)
private val TextPrimary = Color(0xFFF5F2FC)
private val TextMuted = Color(0xFF9CA3AF)
private val Purple = Color(0xFF7C5CFC)
private val Blue = Color(0xFF3B82F6)
private val Green = Color(0xFF10B981)
private val Red = Color(0xFFEF4444)
private val RedDark = Color(0xFFDC2626)
private val RedText = Color(0xFFFF5961)
private val DestructiveBg = Color(0xFF2A1A1A)
private val BorderMuted = Color(0xFF1E293B)

enum class ManageMomentPane {
    MENU,
    RENAME,
    DELETE,
    CONFIRM_PAUSE,
    CONFIRM_COMPLETE,
    LEAVE_TRANSFER,
    LEAVE_CONFIRM,
}

data class LeaveCandidate(
    val userId: String,
    val displayName: String,
    val roleLabel: String,
)

@Composable
fun ManageMomentSheet(
    momentId: String,
    momentTitle: String,
    domain: AppContext,
    currentUserId: String,
    companyId: String? = null,
    onDismiss: () -> Unit,
    onEditSetup: () -> Unit,
    onLifecycleChanged: () -> Unit,
    onLeft: () -> Unit = onLifecycleChanged,
    repo: MomentLifecycleRepository = remember { MomentLifecycleRepository() },
) {
    var pane by remember { mutableStateOf(ManageMomentPane.MENU) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var viewerIsLeader by remember { mutableStateOf(domain == AppContext.PERSONAL) }
    var candidates by remember { mutableStateOf<List<LeaveCandidate>>(emptyList()) }
    var transferUserId by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val supportsLeave = domain == AppContext.GROUP || domain == AppContext.BUSINESS
    val leaveNoun = if (domain == AppContext.BUSINESS) "Company" else "Group"

    LaunchedEffect(momentId, domain, companyId, currentUserId) {
        when (domain) {
            AppContext.GROUP -> {
                runCatching { ApiClient.apiService.getGroupParticipants(momentId).data.participants }
                    .onSuccess { list ->
                        val active = list.filter { it.status == "ACTIVE" && !it.userId.isNullOrBlank() }
                        val me = active.find { it.userId == currentUserId }
                        viewerIsLeader =
                            me?.roleCode == "ORGANIZER" || me?.roleCode == "CO_ORGANIZER"
                        candidates = active
                            .filter { it.userId != currentUserId }
                            .map {
                                LeaveCandidate(
                                    userId = it.userId!!,
                                    displayName = it.displayName?.takeIf { n -> n.isNotBlank() } ?: "Member",
                                    roleLabel = it.roleCode,
                                )
                            }
                    }
            }
            AppContext.BUSINESS -> {
                val cid = companyId
                if (cid.isNullOrBlank()) {
                    viewerIsLeader = false
                    candidates = emptyList()
                } else {
                    runCatching { ApiClient.apiService.listCompanyMembers(cid).data.members }
                        .onSuccess { list ->
                            val active = list.filter { it.status == "ACTIVE" }
                            val me = active.find { it.userId == currentUserId }
                            viewerIsLeader =
                                me?.membershipType == "OWNER" || me?.membershipType == "ADMIN"
                            candidates = active
                                .filter { it.userId != currentUserId }
                                .map {
                                    LeaveCandidate(
                                        userId = it.userId,
                                        displayName = it.displayName?.takeIf { n -> n.isNotBlank() } ?: "Member",
                                        roleLabel = it.membershipType,
                                    )
                                }
                        }
                }
            }
            else -> {
                viewerIsLeader = true
                candidates = emptyList()
            }
        }
    }

    fun runLifecycle(block: suspend (Long) -> Result<*>) {
        scope.launch {
            busy = true
            error = null
            val version = repo.getVersion(momentId).getOrElse {
                busy = false
                error = it.message ?: "Could not load moment"
                return@launch
            }
            block(version).fold(
                onSuccess = {
                    busy = false
                    onLifecycleChanged()
                    onDismiss()
                },
                onFailure = {
                    busy = false
                    error = it.message ?: "Request failed"
                },
            )
        }
    }

    fun runLeave() {
        scope.launch {
            busy = true
            error = null
            val result = when (domain) {
                AppContext.GROUP -> repo.leaveGroup(momentId, transferUserId)
                AppContext.BUSINESS -> {
                    val cid = companyId
                    if (cid.isNullOrBlank()) {
                        Result.failure(IllegalStateException("Company not selected"))
                    } else {
                        repo.leaveCompany(cid, transferUserId)
                    }
                }
                else -> Result.failure(IllegalStateException("Leave not supported"))
            }
            result.fold(
                onSuccess = {
                    busy = false
                    onLeft()
                    onDismiss()
                },
                onFailure = {
                    busy = false
                    error = it.message ?: "Leave failed"
                },
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SheetBg)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .size(width = 36.dp, height = 4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(BorderMuted),
        )
        Spacer(Modifier.height(16.dp))

        when (pane) {
            ManageMomentPane.MENU -> {
                SheetHeader(title = "Manage Moment", subtitle = momentTitle, onClose = onDismiss)
                Spacer(Modifier.height(16.dp))
                ManageRow(Icons.Filled.Settings, Purple, "Edit setup", "Revisit priorities and configuration") {
                    onDismiss()
                    onEditSetup()
                }
                Spacer(Modifier.height(4.dp))
                ManageRow(Icons.Filled.Edit, Blue, "Edit moment name", "Rename how this moment appears") {
                    pane = ManageMomentPane.RENAME
                }
                if (!supportsLeave || viewerIsLeader) {
                    Spacer(Modifier.height(4.dp))
                    ManageRow(Icons.Outlined.Pause, Blue, "Pause rhythm", "Pause tracking without losing your data") {
                        pane = ManageMomentPane.CONFIRM_PAUSE
                    }
                    Spacer(Modifier.height(4.dp))
                    ManageRow(Icons.Filled.Check, Green, "Complete Chapter", "Mark this moment as finished") {
                        pane = ManageMomentPane.CONFIRM_COMPLETE
                    }
                    Spacer(Modifier.height(8.dp))
                    DestructiveRow { pane = ManageMomentPane.DELETE }
                }
                if (supportsLeave) {
                    Spacer(Modifier.height(8.dp))
                    LeaveRow(
                        title = "Leave $leaveNoun",
                        subtitle = if (viewerIsLeader && candidates.isEmpty()) {
                            "Invite someone else before you can leave"
                        } else {
                            "You will lose access to shared content"
                        },
                        enabled = !(viewerIsLeader && candidates.isEmpty()),
                    ) {
                        error = null
                        transferUserId = null
                        pane = if (viewerIsLeader) ManageMomentPane.LEAVE_TRANSFER else ManageMomentPane.LEAVE_CONFIRM
                    }
                }
            }
            ManageMomentPane.RENAME -> {
                RenamePane(
                    momentTitle = momentTitle,
                    busy = busy,
                    error = error,
                    onClose = { pane = ManageMomentPane.MENU },
                    onSave = { name ->
                        scope.launch {
                            busy = true
                            error = null
                            val version = repo.getVersion(momentId).getOrElse {
                                busy = false
                                error = it.message
                                return@launch
                            }
                            repo.rename(momentId, name, version).fold(
                                onSuccess = {
                                    busy = false
                                    onLifecycleChanged()
                                    onDismiss()
                                },
                                onFailure = {
                                    busy = false
                                    error = it.message
                                },
                            )
                        }
                    },
                )
            }
            ManageMomentPane.DELETE -> {
                DeletePane(
                    momentTitle = momentTitle,
                    busy = busy,
                    error = error,
                    onClose = { pane = ManageMomentPane.MENU },
                    onDelete = { runLifecycle { repo.delete(momentId, it) } },
                )
            }
            ManageMomentPane.CONFIRM_PAUSE -> {
                ConfirmPane(
                    title = "Pause rhythm?",
                    body = "Pause tracking without losing your data.",
                    confirmLabel = "Pause",
                    busy = busy,
                    error = error,
                    onClose = { pane = ManageMomentPane.MENU },
                    onConfirm = { runLifecycle { repo.archive(momentId, it) } },
                )
            }
            ManageMomentPane.CONFIRM_COMPLETE -> {
                ConfirmPane(
                    title = "Complete Chapter?",
                    body = "Mark this moment as finished.",
                    confirmLabel = "Complete",
                    busy = busy,
                    error = error,
                    onClose = { pane = ManageMomentPane.MENU },
                    onConfirm = { runLifecycle { repo.cancel(momentId, it) } },
                )
            }
            ManageMomentPane.LEAVE_TRANSFER -> {
                LeaveTransferPane(
                    leaveNoun = leaveNoun,
                    candidates = candidates,
                    selectedUserId = transferUserId,
                    busy = busy,
                    error = error,
                    onSelect = { transferUserId = it },
                    onClose = { pane = ManageMomentPane.MENU },
                    onContinue = {
                        if (transferUserId == null) {
                            error = "Select a member to become the new ${if (domain == AppContext.BUSINESS) "admin" else "organizer"}."
                        } else {
                            error = null
                            pane = ManageMomentPane.LEAVE_CONFIRM
                        }
                    },
                )
            }
            ManageMomentPane.LEAVE_CONFIRM -> {
                LeaveConfirmPane(
                    momentTitle = momentTitle,
                    leaveNoun = leaveNoun,
                    busy = busy,
                    error = error,
                    onClose = {
                        pane = if (viewerIsLeader) ManageMomentPane.LEAVE_TRANSFER else ManageMomentPane.MENU
                    },
                    onConfirm = { runLeave() },
                )
            }
        }

        error?.takeIf { pane == ManageMomentPane.MENU }?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = RedText, fontSize = 12.sp)
        }
        if (busy && pane == ManageMomentPane.MENU) {
            Spacer(Modifier.height(8.dp))
            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Purple, strokeWidth = 2.dp)
        }
    }
}

@Composable
private fun SheetHeader(title: String, subtitle: String, onClose: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            if (subtitle.isNotBlank()) {
                Text(subtitle, color = TextMuted, fontSize = 12.sp)
            }
        }
        Icon(
            Icons.Filled.Close,
            contentDescription = "Close",
            tint = TextMuted,
            modifier = Modifier
                .size(22.dp)
                .clickable(onClick = onClose),
        )
    }
}

@Composable
private fun ManageRow(icon: ImageVector, well: Color, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(RowBg)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(well),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text(subtitle, color = TextMuted, fontSize = 10.sp)
        }
        Icon(Icons.Filled.KeyboardArrowRight, contentDescription = null, tint = TextMuted, modifier = Modifier.size(14.dp))
    }
}

@Composable
private fun DestructiveRow(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DestructiveBg)
            .border(1.dp, Red, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Red),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Delete, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text("Delete permanently", color = RedText, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text("Removes this moment; analytics kept", color = TextMuted, fontSize = 10.sp)
        }
        Icon(Icons.Filled.KeyboardArrowRight, contentDescription = null, tint = TextMuted, modifier = Modifier.size(14.dp))
    }
}

@Composable
private fun LeaveRow(title: String, subtitle: String, enabled: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (enabled) Color(0x1AEF4444) else Color(0xFF1A1A1A))
            .border(1.dp, if (enabled) Color(0x33EF4444) else BorderMuted, RoundedCornerShape(12.dp))
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0x1AEF4444))
                .border(1.dp, Color(0x33EF4444), RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_leave_log_out),
                contentDescription = null,
                tint = if (enabled) Red else TextMuted,
                modifier = Modifier.size(13.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = if (enabled) RedText else TextMuted, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text(subtitle, color = TextMuted, fontSize = 10.sp)
        }
        Icon(Icons.Filled.KeyboardArrowRight, contentDescription = null, tint = TextMuted, modifier = Modifier.size(14.dp))
    }
}

@Composable
private fun LeaveTransferPane(
    leaveNoun: String,
    candidates: List<LeaveCandidate>,
    selectedUserId: String?,
    busy: Boolean,
    error: String?,
    onSelect: (String) -> Unit,
    onClose: () -> Unit,
    onContinue: () -> Unit,
) {
    SheetHeader(
        title = if (leaveNoun == "Company") "Choose a new admin" else "Choose a new organizer",
        subtitle = "They will manage this $leaveNoun after you leave",
        onClose = onClose,
    )
    Spacer(Modifier.height(12.dp))
    candidates.forEach { c ->
        val selected = c.userId == selectedUserId
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (selected) Color(0x1AEF4444) else RowBg)
                .border(1.dp, if (selected) Red else BorderMuted, RoundedCornerShape(12.dp))
                .clickable { onSelect(c.userId) }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(c.displayName, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(c.roleLabel, color = TextMuted, fontSize = 11.sp)
            }
            if (selected) {
                Icon(Icons.Filled.Check, null, tint = Red, modifier = Modifier.size(18.dp))
            }
        }
    }
    error?.let {
        Spacer(Modifier.height(8.dp))
        Text(it, color = RedText, fontSize = 12.sp)
    }
    Spacer(Modifier.height(16.dp))
    Button(
        onClick = onContinue,
        enabled = !busy && selectedUserId != null,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = Red),
        shape = RoundedCornerShape(16.dp),
    ) {
        Text("Continue", fontWeight = FontWeight.ExtraBold)
    }
    TextButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
        Text("Cancel", color = TextMuted)
    }
}

@Composable
private fun LeaveConfirmPane(
    momentTitle: String,
    leaveNoun: String,
    busy: Boolean,
    error: String?,
    onClose: () -> Unit,
    onConfirm: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(Color(0x1AEF4444))
                .border(1.dp, Color(0x33EF4444), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_leave_log_out),
                contentDescription = null,
                tint = Red,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "Leave $momentTitle?",
            color = Color.White,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 20.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "You will lose access to shared moments, schedules, and household data. This action cannot be undone.",
            color = TextMuted,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            lineHeight = 21.sp,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0x0DEF4444))
                .border(1.dp, Color(0x33EF4444), RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_alert_triangle),
                contentDescription = null,
                tint = Red,
                modifier = Modifier.size(14.dp),
            )
            Text(
                "This will permanently remove you from all shared content.",
                color = Red,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                modifier = Modifier.weight(1f),
            )
        }
        error?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = RedText, fontSize = 12.sp)
        }
        Spacer(Modifier.height(20.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TextButton(
                onClick = onClose,
                enabled = !busy,
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, BorderMuted, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text("Cancel", color = TextMuted, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
            Button(
                onClick = onConfirm,
                enabled = !busy,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(listOf(RedDark, Red)),
                            RoundedCornerShape(16.dp),
                        )
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (busy) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text("Leave $leaveNoun", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun RenamePane(
    momentTitle: String,
    busy: Boolean,
    error: String?,
    onClose: () -> Unit,
    onSave: (String) -> Unit,
) {
    var name by remember { mutableStateOf(momentTitle) }
    SheetHeader(title = "Edit Moment Name", subtitle = momentTitle, onClose = onClose)
    Spacer(Modifier.height(16.dp))
    Text("MOMENT NAME", color = TextMuted, fontWeight = FontWeight.Bold, fontSize = 11.sp)
    Spacer(Modifier.height(8.dp))
    OutlinedTextField(
        value = name,
        onValueChange = { name = it },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        leadingIcon = { Icon(Icons.Filled.Edit, null, tint = Blue) },
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            focusedContainerColor = RowBg,
            unfocusedContainerColor = RowBg,
            focusedBorderColor = Purple,
            unfocusedBorderColor = Color.Transparent,
        ),
        shape = RoundedCornerShape(12.dp),
    )
    error?.let {
        Spacer(Modifier.height(8.dp))
        Text(it, color = RedText, fontSize = 12.sp)
    }
    Spacer(Modifier.height(16.dp))
    Button(
        onClick = { onSave(name.trim()) },
        enabled = !busy && name.trim().isNotEmpty(),
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = Purple),
        shape = RoundedCornerShape(12.dp),
    ) {
        if (busy) CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
        else Text("Save", fontWeight = FontWeight.Bold)
    }
    TextButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
        Text("Cancel", color = TextMuted)
    }
}

@Composable
private fun DeletePane(
    momentTitle: String,
    busy: Boolean,
    error: String?,
    onClose: () -> Unit,
    onDelete: () -> Unit,
) {
    SheetHeader(title = "Delete Permanently?", subtitle = momentTitle, onClose = onClose)
    Spacer(Modifier.height(16.dp))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Red),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Delete, null, tint = Color.White)
        }
    }
    Spacer(Modifier.height(12.dp))
    Text(
        "This cannot be undone. The moment and operational data will be removed; analytics are kept.",
        color = TextMuted,
        fontSize = 13.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
    error?.let {
        Spacer(Modifier.height(8.dp))
        Text(it, color = RedText, fontSize = 12.sp)
    }
    Spacer(Modifier.height(16.dp))
    Button(
        onClick = onDelete,
        enabled = !busy,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = Red),
        shape = RoundedCornerShape(12.dp),
    ) {
        if (busy) CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
        else Text("Delete", fontWeight = FontWeight.Bold)
    }
    TextButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
        Text("Cancel", color = TextMuted)
    }
}

@Composable
private fun ConfirmPane(
    title: String,
    body: String,
    confirmLabel: String,
    busy: Boolean,
    error: String?,
    onClose: () -> Unit,
    onConfirm: () -> Unit,
) {
    SheetHeader(title = title, subtitle = "", onClose = onClose)
    Spacer(Modifier.height(12.dp))
    Text(body, color = TextMuted, fontSize = 13.sp)
    error?.let {
        Spacer(Modifier.height(8.dp))
        Text(it, color = RedText, fontSize = 12.sp)
    }
    Spacer(Modifier.height(16.dp))
    Button(
        onClick = onConfirm,
        enabled = !busy,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = Purple),
        shape = RoundedCornerShape(12.dp),
    ) {
        if (busy) CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
        else Text(confirmLabel, fontWeight = FontWeight.Bold)
    }
    TextButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
        Text("Cancel", color = TextMuted)
    }
}
