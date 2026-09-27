package com.example.momentra.ui.shell.empty.business

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.data.api.CompanyInviteDto
import com.example.momentra.data.repository.GroupSliceRepository
import com.example.momentra.domain.CompanySummary
import com.example.momentra.ui.theme.PlusJakartaSans

private val SheetBg = Color(0xFF161B26)
private val FieldBg = Color(0xFF252230)
private val Accent = Color(0xFF818CF8)
private val TextMuted = Color(0xFF94A3B8)
private val Red = Color(0xFFF87171)

private enum class CompanyJoinPhase { Preview, Joining, Welcome }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompanyJoinConfirmSheet(
    code: String,
    visible: Boolean,
    onDismiss: () -> Unit,
    onGoToCompany: (CompanySummary) -> Unit,
    repository: GroupSliceRepository = remember { GroupSliceRepository() },
) {
    if (!visible) return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var preview by remember(code) { mutableStateOf<CompanyInviteDto?>(null) }
    var phase by remember(code) { mutableStateOf(CompanyJoinPhase.Preview) }
    var error by remember(code) { mutableStateOf<String?>(null) }
    var joined by remember(code) { mutableStateOf<CompanySummary?>(null) }
    var roleLabel by remember(code) { mutableStateOf<String?>(null) }

    LaunchedEffect(code) {
        phase = CompanyJoinPhase.Preview
        error = null
        repository.previewCompanyInvite(code).fold(
            onSuccess = { preview = it },
            onFailure = {
                preview = null
                error = it.message ?: "Invite not found or no longer valid."
            },
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SheetBg,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(width = 36.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF625E70)),
            )
            when (phase) {
                CompanyJoinPhase.Preview -> PreviewBody(
                    preview = preview,
                    error = error,
                    onDecline = onDismiss,
                    onJoin = {
                        if (preview != null) {
                            phase = CompanyJoinPhase.Joining
                            error = null
                        }
                    },
                )
                CompanyJoinPhase.Joining -> JoiningBody(name = preview?.title.orEmpty())
                CompanyJoinPhase.Welcome -> WelcomeBody(
                    company = joined,
                    role = roleLabel,
                    onStay = onDismiss,
                    onGo = { joined?.let(onGoToCompany) },
                )
            }
        }
    }

    LaunchedEffect(phase) {
        if (phase != CompanyJoinPhase.Joining) return@LaunchedEffect
        val invite = preview ?: run {
            phase = CompanyJoinPhase.Preview
            return@LaunchedEffect
        }
        repository.redeemCompanyInvite(code).fold(
            onSuccess = { result ->
                roleLabel = result.membershipType ?: invite.membershipType
                joined = CompanySummary(
                    companyId = result.companyId.ifBlank { invite.companyId },
                    displayName = invite.title.ifBlank { "Company" },
                )
                phase = CompanyJoinPhase.Welcome
            },
            onFailure = {
                error = it.message ?: "Could not join company"
                phase = CompanyJoinPhase.Preview
            },
        )
    }
}

@Composable
private fun PreviewBody(
    preview: CompanyInviteDto?,
    error: String?,
    onDecline: () -> Unit,
    onJoin: () -> Unit,
) {
    if (preview == null && error == null) {
        CircularProgressIndicator(color = Accent)
        return
    }
    val name = preview?.title.orEmpty()
    if (name.isNotBlank()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Initials(name)
            Text(
                text = name,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = PlusJakartaSans,
                modifier = Modifier.padding(start = 12.dp),
            )
        }
        preview?.membershipType?.takeIf { it.isNotBlank() }?.let { role ->
            Text(
                text = "You join as ${role.lowercase().replaceFirstChar { it.uppercase() }}",
                color = Accent,
                fontSize = 12.sp,
                fontFamily = PlusJakartaSans,
                modifier = Modifier
                    .clip(RoundedCornerShape(100.dp))
                    .background(Accent.copy(alpha = 0.15f))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
    }
    error?.let {
        Text(it, color = Red, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
    }
    ActionButton(label = "Join Company", enabled = preview != null, onClick = onJoin)
    TextButton(label = "Decline", onClick = onDecline)
    Text(
        text = "You can leave anytime from company settings",
        color = TextMuted,
        fontSize = 12.sp,
        textAlign = TextAlign.Center,
        fontFamily = PlusJakartaSans,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun JoiningBody(name: String) {
    CircularProgressIndicator(color = Accent)
    Text(
        text = if (name.isBlank()) "Joining…" else "Joining $name...",
        color = Color.White,
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        fontFamily = PlusJakartaSans,
    )
}

@Composable
private fun WelcomeBody(
    company: CompanySummary?,
    role: String?,
    onStay: () -> Unit,
    onGo: () -> Unit,
) {
    val name = company?.displayName.orEmpty()
    Text(
        text = if (name.isBlank()) "Welcome" else "Welcome to $name!",
        color = Color.White,
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = PlusJakartaSans,
        textAlign = TextAlign.Center,
    )
    if (!role.isNullOrBlank()) {
        Text(
            text = "You've successfully joined as a ${role.lowercase().replaceFirstChar { it.uppercase() }}",
            color = TextMuted,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            fontFamily = PlusJakartaSans,
        )
    }
    if (name.isNotBlank()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(FieldBg)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Initials(name)
            Text(
                text = name,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                fontFamily = PlusJakartaSans,
                modifier = Modifier.padding(start = 12.dp),
            )
        }
    }
    ActionButton(label = "Go to Company", enabled = company != null, onClick = onGo)
    TextButton(label = "Stay Here", onClick = onStay)
}

@Composable
private fun Initials(name: String) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Accent.copy(alpha = 0.2f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(companyInitials(name), color = Accent, fontWeight = FontWeight.Bold, fontSize = 13.sp, fontFamily = PlusJakartaSans)
    }
}

@Composable
private fun ActionButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Accent.copy(alpha = if (enabled) 0.9f else 0.35f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, fontFamily = PlusJakartaSans)
    }
}

@Composable
private fun TextButton(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        color = TextMuted,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        fontFamily = PlusJakartaSans,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        textAlign = TextAlign.Center,
    )
}
