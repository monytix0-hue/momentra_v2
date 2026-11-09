package com.example.momentra.ui.shell.empty.business

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role as SemanticsRole
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.momentra.data.api.CompanyInviteDto
import com.example.momentra.ui.shell.business.shared.CompanyJoinLink
import com.example.momentra.ui.shell.business.shared.inviteRoleAccess
import com.example.momentra.ui.shell.components.MomentraFullscreenDialog
import com.example.momentra.ui.shell.empty.group.DeviceContact
import com.example.momentra.ui.shell.empty.group.generateInviteQrBitmap
import com.example.momentra.ui.shell.empty.group.inviteMessage
import com.example.momentra.ui.shell.empty.group.loadDeviceContacts
import com.example.momentra.ui.shell.empty.group.saveInviteQrToPhotos
import com.example.momentra.ui.shell.empty.group.sendInviteWhatsApp
import com.example.momentra.ui.shell.empty.group.shareInviteQr
import com.example.momentra.ui.theme.PlusJakartaSans
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * §02's invite sheet, modelled on the group setup `Add People` sheet (`579:12741`): the role the
 * invite grants, the device contacts it can be sent to, the link, and the QR.
 *
 * Every value is real. The code comes from `POST /v1/company/invites`, minted by the host the
 * first time §02 needs one, so the link and the QR are the same code the invitee will redeem.
 * Until it lands the QR block says so rather than drawing a placeholder.
 */
@Composable
internal fun BusinessSetupInviteSheet(
    title: String,
    roleCode: String,
    invite: CompanyInviteDto?,
    minting: Boolean,
    error: String?,
    theme: SetupDocTheme,
    existingNames: List<String>,
    subtypeCode: String = SetupDocumentSpec.TRADING,
    onRoleChange: (String) -> Unit,
    onAddPerson: (SetupInvitee) -> Unit,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var contacts by remember { mutableStateOf<List<DeviceContact>>(emptyList()) }
    var copied by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(false) }
    var hasContactsPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> hasContactsPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasContactsPermission) permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
    }
    LaunchedEffect(hasContactsPermission) {
        if (hasContactsPermission) contacts = withContext(Dispatchers.IO) { loadDeviceContacts(context) }
    }

    val displayName = title.ifBlank { "this business" }
    // A code grants the role it was minted with, so a code minted for another chip is never
    // shown as this role's link — the host re-mints on a role change.
    val activeInvite = invite?.takeIf { it.operatingRole == roleCode }
    val inviteCode = activeInvite?.inviteCode?.trim()?.lowercase()?.takeIf { it.isNotBlank() }
    val invitePath = inviteCode?.let { code ->
        activeInvite?.invitePath?.takeIf { it.isNotBlank() }
            ?: activeInvite?.inviteUrl?.takeIf { it.isNotBlank() }
            ?: CompanyJoinLink.displayPath(code)
    }
    val inviteBody = remember(invitePath, displayName) {
        invitePath?.let { inviteMessage(displayName, it) }
    }
    val qrSizePx = with(LocalDensity.current) { 156.dp.roundToPx() }
    val qrBitmap = remember(inviteCode, qrSizePx) {
        inviteCode?.let { generateInviteQrBitmap(CompanyJoinLink.qrPayload(it), qrSizePx) }
    }

    val existingLower = remember(existingNames) { existingNames.map { it.lowercase() }.toSet() }
    val filtered = remember(contacts, query) {
        val q = query.trim()
        val matched = if (q.isEmpty()) {
            contacts
        } else {
            contacts.filter {
                it.name.contains(q, ignoreCase = true) ||
                    it.subtitle.contains(q, ignoreCase = true) ||
                    it.email?.contains(q, ignoreCase = true) == true ||
                    it.phone?.contains(q, ignoreCase = true) == true
            }
        }
        if (q.isEmpty()) matched.take(6) else matched.take(20)
    }

    MomentraFullscreenDialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .imePadding(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f))
                    .clickable(onClick = onDismiss),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxHeight(0.92f)
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(theme.card)
                    .border(
                        1.dp,
                        theme.border,
                        RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    )
                    .navigationBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 64.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 36.dp, height = 4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(theme.border),
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    "Invite team · $displayName",
                                    color = theme.ink,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = PlusJakartaSans,
                                    maxLines = 2,
                                )
                                Text(
                                    "Invite grants a role and its access. Joining ≠ approval ≠ payment.",
                                    color = theme.muted,
                                    fontSize = 11.sp,
                                    fontFamily = PlusJakartaSans,
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(theme.inset)
                                    .border(1.dp, theme.border, RoundedCornerShape(999.dp))
                                    .clickable(onClick = onDismiss)
                                    .semantics {
                                        role = SemanticsRole.Button
                                        contentDescription = "Close"
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    "Close",
                                    color = theme.muted,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = PlusJakartaSans,
                                )
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = true)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        // Role — the invite grants exactly this access.
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                "Role",
                                color = theme.accent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = PlusJakartaSans,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                SetupDocumentSpec.inviteRoles(subtypeCode).forEach { role ->
                                    val selected = role.code == roleCode
                                    Text(
                                        role.label,
                                        color = if (selected) theme.ink else theme.muted,
                                        fontSize = 12.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        fontFamily = PlusJakartaSans,
                                        maxLines = 1,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(999.dp))
                                            .background(if (selected) theme.accent else theme.inset)
                                            .border(
                                                1.dp,
                                                if (selected) theme.accent else theme.border,
                                                RoundedCornerShape(999.dp),
                                            )
                                            .clickable { onRoleChange(role.code) }
                                            .padding(horizontal = 11.dp, vertical = 7.dp),
                                    )
                                }
                            }
                            Text(
                                "Access: ${inviteRoleAccess(roleCode, SetupDocumentSpec.isDistribution(subtypeCode))}.",
                                color = theme.muted,
                                fontSize = 11.sp,
                                fontFamily = PlusJakartaSans,
                            )
                        }

                        // Contacts.
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(theme.inset)
                                    .border(1.dp, theme.border, RoundedCornerShape(14.dp))
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                BasicTextField(
                                    value = query,
                                    onValueChange = { query = it },
                                    textStyle = TextStyle(
                                        color = theme.ink,
                                        fontSize = 13.sp,
                                        fontFamily = PlusJakartaSans,
                                    ),
                                    cursorBrush = SolidColor(theme.accent),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    decorationBox = { inner ->
                                        Box {
                                            if (query.isEmpty()) {
                                                Text(
                                                    "Search contacts, email or phone",
                                                    color = theme.muted,
                                                    fontSize = 13.sp,
                                                    fontFamily = PlusJakartaSans,
                                                )
                                            }
                                            inner()
                                        }
                                    },
                                )
                            }
                            Text(
                                "From your contacts",
                                color = theme.accent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = PlusJakartaSans,
                            )
                            if (filtered.isEmpty()) {
                                Text(
                                    "No matching contacts.",
                                    color = theme.muted,
                                    fontSize = 12.sp,
                                    fontFamily = PlusJakartaSans,
                                    modifier = Modifier.padding(vertical = 8.dp),
                                )
                            }
                            filtered.forEachIndexed { index, contact ->
                                if (index > 0) {
                                    HorizontalDivider(color = theme.border, thickness = 1.dp)
                                }
                                val already = existingLower.contains(contact.name.lowercase())
                                ContactRow(
                                    name = contact.name,
                                    subtitle = contact.subtitle.ifBlank { "Contact" },
                                    added = already,
                                    theme = theme,
                                    onSend = {
                                        if (already) return@ContactRow
                                        onAddPerson(
                                            SetupInvitee(
                                                id = "contact:" + contact.id,
                                                name = contact.name,
                                                roleCode = roleCode,
                                                phone = contact.phone.orEmpty(),
                                            ),
                                        )
                                        query = ""
                                        val body = inviteBody
                                        if (body != null && contact.phone != null) {
                                            sendInviteWhatsApp(context, contact.phone, body)
                                        }
                                    },
                                )
                            }
                        }

                        HorizontalDivider(color = theme.border, thickness = 1.dp)

                        // Link.
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                "Or share invite link",
                                color = theme.muted,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = PlusJakartaSans,
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(theme.inset)
                                    .border(1.dp, theme.border, RoundedCornerShape(12.dp))
                                    .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    invitePath
                                        ?: if (minting) "Minting an invite…" else "Could not mint an invite",
                                    color = theme.muted,
                                    fontSize = 12.sp,
                                    fontFamily = PlusJakartaSans,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (invitePath != null) theme.accent else theme.border)
                                        .clickable(enabled = invitePath != null) {
                                            val text = invitePath ?: return@clickable
                                            val clipboard =
                                                context.getSystemService(ClipboardManager::class.java)
                                            clipboard.setPrimaryClip(
                                                ClipData.newPlainText("Invite link", text),
                                            )
                                            copied = true
                                            scope.launch {
                                                delay(1600)
                                                copied = false
                                            }
                                        }
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                ) {
                                    Text(
                                        if (copied) "Copied" else "Copy",
                                        color = if (invitePath != null) theme.ink else theme.muted,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = PlusJakartaSans,
                                    )
                                }
                            }
                            if (invitePath == null && !minting) {
                                Text(
                                    "Retry",
                                    color = theme.accent,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = PlusJakartaSans,
                                    modifier = Modifier.clickable(onClick = onRetry),
                                )
                            }
                        }

                        HorizontalDivider(color = theme.border, thickness = 1.dp)

                        // QR.
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(
                                "Or scan to join",
                                color = theme.accent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = PlusJakartaSans,
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.White)
                                    .padding(16.dp),
                            ) {
                                if (qrBitmap != null) {
                                    Image(
                                        bitmap = qrBitmap.asImageBitmap(),
                                        contentDescription = "Invite QR code",
                                        modifier = Modifier.size(156.dp),
                                        contentScale = ContentScale.Fit,
                                    )
                                } else {
                                    Box(
                                        Modifier.size(156.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        if (minting) {
                                            CircularProgressIndicator(
                                                color = theme.accent,
                                                strokeWidth = 2.dp,
                                                modifier = Modifier.size(28.dp),
                                            )
                                        } else {
                                            Text(
                                                "No invite yet",
                                                color = Color(0xFF64748B),
                                                fontSize = 12.sp,
                                                fontFamily = PlusJakartaSans,
                                            )
                                        }
                                    }
                                }
                            }
                            Text(
                                "Scan with the Momentra app to join. Joining is not approval and not payment.",
                                color = theme.muted,
                                fontSize = 11.sp,
                                fontFamily = PlusJakartaSans,
                            )
                        }

                        if (!error.isNullOrBlank()) {
                            Text(
                                error,
                                color = theme.negative,
                                fontSize = 12.sp,
                                fontFamily = PlusJakartaSans,
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            SetupOutlineAction(
                                title = "Share QR",
                                theme = theme,
                                busy = minting,
                                enabled = qrBitmap != null && invitePath != null,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    val bitmap = qrBitmap ?: return@SetupOutlineAction
                                    shareInviteQr(context, bitmap, invitePath ?: return@SetupOutlineAction)
                                },
                            )
                            SetupOutlineAction(
                                title = if (saved) "Saved" else "Save to Photos",
                                theme = theme,
                                enabled = qrBitmap != null,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    val bitmap = qrBitmap ?: return@SetupOutlineAction
                                    if (saveInviteQrToPhotos(context, bitmap)) {
                                        saved = true
                                        scope.launch {
                                            delay(1600)
                                            saved = false
                                        }
                                    }
                                },
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(theme.accent)
                        .clickable(onClick = onDismiss)
                        .semantics {
                            role = SemanticsRole.Button
                            contentDescription = "Done"
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "Done",
                        color = Color(0xFF0C0F15),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = PlusJakartaSans,
                    )
                }
            }
        }
    }
}

@Composable
private fun ContactRow(
    name: String,
    subtitle: String,
    added: Boolean,
    theme: SetupDocTheme,
    onSend: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SetupAvatar(SetupDocumentSpec.initials(name), theme, size = 40)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                name,
                color = theme.ink,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = PlusJakartaSans,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                subtitle,
                color = theme.muted,
                fontSize = 12.sp,
                fontFamily = PlusJakartaSans,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(if (added) theme.border else theme.accent.copy(alpha = 0.13f))
                .border(
                    1.dp,
                    if (added) theme.border else theme.accent.copy(alpha = 0.4f),
                    RoundedCornerShape(20.dp),
                )
                .clickable(enabled = !added, onClick = onSend)
                .padding(horizontal = 14.dp, vertical = 6.dp),
        ) {
            Text(
                if (added) "Invited" else "Send",
                color = if (added) theme.muted else theme.accent,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = PlusJakartaSans,
            )
        }
    }
}
