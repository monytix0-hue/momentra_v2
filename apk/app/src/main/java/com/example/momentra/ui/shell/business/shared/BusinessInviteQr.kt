package com.example.momentra.ui.shell.business.shared

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.momentra.data.api.ApiClient
import com.example.momentra.data.api.CompanyInviteDto
import com.example.momentra.data.api.MintCompanyInviteBody
import com.example.momentra.ui.shell.empty.group.DeviceContact
import com.example.momentra.ui.shell.empty.group.generateInviteQrBitmap
import com.example.momentra.ui.shell.empty.group.inviteMessage
import com.example.momentra.ui.shell.empty.group.loadDeviceContacts
import com.example.momentra.ui.shell.empty.group.saveInviteQrToPhotos
import com.example.momentra.ui.shell.empty.group.sendInviteWhatsApp
import com.example.momentra.ui.shell.empty.group.shareInviteQr
import com.example.momentra.ui.theme.PlusJakartaSans
import com.example.momentra.ui.theme.RestaurantEmber
import com.example.momentra.ui.theme.RetailRose
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

val BusinessInviteRoles = listOf(
    "FINANCE" to "Finance",
    "APPROVER" to "Approver",
    "OPERATIONS_LEAD" to "Operations",
    "CONTRIBUTOR" to "Contributor",
)

fun businessInviteMembership(role: String): String =
    if (role == "FINANCE" || role == "APPROVER") "ADMIN" else "MEMBER"

fun businessInviteRoleLabel(role: String?): String =
    BusinessInviteRoles.firstOrNull { it.first == role }?.second
        ?: role?.lowercase()?.replace('_', ' ')?.replaceFirstChar { it.uppercase() }
        ?: "Member"

object CompanyJoinLink {
    fun displayPath(code: String): String =
        "https://momentra.tech/c/${code.trim().lowercase()}"

    fun qrPayload(code: String): String = displayPath(code)
}

/**
 * Invite & QR — Figma `RTL-MASTER · Invite & QR` `2506:29333` and
 * `RST-MASTER · Invite & QR` `2465:29655`.
 *
 * Every value here is real: the QR is drawn from a code minted by
 * `POST /v1/company/invites`, the link is whatever that endpoint returns, and the contact
 * list comes from the device. Until an invite exists the QR block says so instead of
 * drawing a placeholder.
 *
 * The frames show five role chips; the backend accepts only
 * `FINANCE | APPROVER | OPERATIONS_LEAD | CONTRIBUTOR`, so those four are the ones offered.
 */
internal data class InvitePalette(
    val accent: Color,
    val accentMuted: Color,
    val card: Color,
    val field: Color,
    val chip: Color,
    val chipStroke: Color,
    val divider: Color,
    val qrPlate: Color,
    val textPrimary: Color,
    val textBody: Color,
    val textMuted: Color,
    val textFaint: Color,
    val warning: Color,
    val negative: Color,
)

internal val RoseInvitePalette = InvitePalette(
    accent = RetailRose.rose,
    accentMuted = RetailRose.roseMuted,
    card = RetailRose.cardSlate,
    field = RetailRose.field,
    chip = RetailRose.chip,
    chipStroke = RetailRose.chipStroke,
    divider = RetailRose.divider,
    qrPlate = RetailRose.cardSlate,
    textPrimary = RetailRose.textPrimary,
    textBody = RetailRose.textBody,
    textMuted = RetailRose.textMuted,
    textFaint = RetailRose.textFaint,
    warning = RetailRose.warning,
    negative = RetailRose.negative,
)

internal val EmberInvitePalette = InvitePalette(
    accent = RestaurantEmber.ember,
    accentMuted = RestaurantEmber.gold,
    card = RestaurantEmber.card,
    field = RestaurantEmber.surfaceWell,
    chip = RestaurantEmber.chip,
    chipStroke = RestaurantEmber.chipStroke,
    divider = RestaurantEmber.divider,
    qrPlate = RestaurantEmber.surfaceWell,
    textPrimary = RestaurantEmber.textPrimary,
    textBody = RestaurantEmber.textBody,
    textMuted = RestaurantEmber.textMuted,
    textFaint = RestaurantEmber.textFaint,
    warning = RestaurantEmber.warning,
    negative = RestaurantEmber.negative,
)

@Composable
internal fun BusinessInviteQr(
    momentId: String?,
    title: String,
    role: String?,
    palette: InvitePalette,
    iconRes: Int? = null,
    noun: String = "store",
    onError: (String) -> Unit,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    var companyId by remember(momentId) { mutableStateOf<String?>(null) }
    var roleCode by remember { mutableStateOf("OPERATIONS_LEAD") }
    var invite by remember { mutableStateOf<CompanyInviteDto?>(null) }
    var minting by remember { mutableStateOf(false) }
    var mintError by remember { mutableStateOf<String?>(null) }
    var copied by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var contacts by remember { mutableStateOf<List<DeviceContact>>(emptyList()) }
    var hasContacts by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }

    val canInvite = role == "OWNER_ADMIN"
    val displayName = title.takeIf { it.isNotBlank() } ?: "this $noun"

    LaunchedEffect(momentId) {
        if (momentId.isNullOrBlank()) return@LaunchedEffect
        runCatching {
            ApiClient.apiService.listBusinessMoments(limit = 50).data.items
                .firstOrNull { it.momentId == momentId }?.companyId
        }.onSuccess { companyId = it }.onFailure { companyId = null }
    }

    LaunchedEffect(companyId, roleCode) {
        val id = companyId ?: return@LaunchedEffect
        if (!canInvite) return@LaunchedEffect
        minting = true
        mintError = null
        runCatching {
            ApiClient.apiService.mintCompanyInvite(
                idempotencyKey = UUID.randomUUID().toString(),
                body = MintCompanyInviteBody(
                    companyId = id,
                    membershipType = businessInviteMembership(roleCode),
                    operatingRole = roleCode,
                ),
            ).data
        }.fold(
            onSuccess = {
                invite = it
                copied = false
                saved = false
            },
            onFailure = { if (invite == null) mintError = it.message ?: "Could not mint an invite" },
        )
        minting = false
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> hasContacts = granted }

    LaunchedEffect(Unit) {
        if (!hasContacts) permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
    }
    LaunchedEffect(hasContacts) {
        if (hasContacts) contacts = withContext(Dispatchers.IO) { loadDeviceContacts(context) }
    }

    val invitePath = invite?.let {
        it.invitePath.takeIf { path -> path.isNotBlank() }
            ?: it.inviteUrl.takeIf { url -> url.isNotBlank() }
            ?: CompanyJoinLink.displayPath(it.inviteCode)
    }
    val inviteCode = invite?.inviteCode?.takeIf { it.isNotBlank() }
    val qrSizePx = with(density) { 168.dp.roundToPx() }
    val qrBitmap = remember(inviteCode, qrSizePx) {
        inviteCode?.let { generateInviteQrBitmap(CompanyJoinLink.qrPayload(it), qrSizePx) }
    }
    val shareBody = invitePath?.let { inviteMessage(displayName, it) }
    val shown = contacts
        .filter {
            val q = query.trim()
            q.isEmpty() ||
                it.name.contains(q, ignoreCase = true) ||
                it.subtitle.contains(q, ignoreCase = true) ||
                it.email?.contains(q, ignoreCase = true) == true ||
                it.phone?.contains(q, ignoreCase = true) == true
        }
        .let { if (query.isBlank()) it.take(6) else it.take(20) }

    // Header card
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(palette.card)
            .border(1.dp, palette.accent.copy(alpha = 0.22f), RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            if (iconRes != null) {
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(palette.accent.copy(alpha = 0.16f))
                        .border(1.dp, palette.accent.copy(alpha = 0.45f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    androidx.compose.material3.Icon(
                        androidx.compose.ui.res.painterResource(iconRes),
                        contentDescription = null,
                        tint = palette.accent,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "Invite the team",
                    color = palette.textPrimary,
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1,
                )
                Text(
                    "$displayName · ${businessInviteRoleLabel(role)} · invite grants a role",
                    color = palette.accentMuted,
                    fontFamily = PlusJakartaSans,
                    fontSize = 11.sp,
                    maxLines = 2,
                )
            }
            if (!canInvite) {
                Text(
                    "Owner only",
                    color = palette.warning,
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    maxLines = 1,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(palette.warning.copy(alpha = 0.16f))
                        .padding(horizontal = 9.dp, vertical = 4.dp),
                )
            }
        }
    }

    // Roles
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "ROLE FOR THE INVITE",
            color = palette.textFaint,
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            letterSpacing = 0.6.sp,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            BusinessInviteRoles.forEach { (code, label) ->
                val on = code == roleCode
                Text(
                    label,
                    color = if (on) palette.textPrimary else palette.textFaint,
                    fontFamily = PlusJakartaSans,
                    fontWeight = if (on) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 12.sp,
                    maxLines = 1,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (on) palette.accent else palette.chip)
                        .border(1.dp, if (on) palette.accent else palette.chipStroke, CircleShape)
                        .clickable(enabled = canInvite) { roleCode = code }
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                )
            }
        }
        Text(
            "Access: ${inviteRoleAccess(roleCode)}.",
            color = palette.textFaint,
            fontFamily = PlusJakartaSans,
            fontSize = 11.sp,
        )
        mintError?.let {
            Text(it, color = palette.negative, fontFamily = PlusJakartaSans, fontSize = 11.sp)
        }
    }

    if (canInvite) {
        TextField(
            value = query,
            onValueChange = { query = it },
            placeholder = {
                Text(
                    "Search contacts, email or phone",
                    color = palette.textFaint,
                    fontFamily = PlusJakartaSans,
                    fontSize = 11.sp,
                )
            },
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = palette.field,
                unfocusedContainerColor = palette.field,
                focusedIndicatorColor = palette.accent.copy(alpha = 0.55f),
                unfocusedIndicatorColor = palette.chipStroke,
                cursorColor = palette.accent,
                focusedTextColor = palette.textPrimary,
                unfocusedTextColor = palette.textPrimary,
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
        )

        Text(
            "FROM YOUR CONTACTS",
            color = palette.textFaint,
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            letterSpacing = 0.6.sp,
        )
        when {
            !hasContacts -> Text(
                "Contacts access is off. Share the invite link instead — it works the same.",
                color = palette.textBody,
                fontFamily = PlusJakartaSans,
                fontSize = 11.sp,
            )
            shown.isEmpty() -> Text(
                if (contacts.isEmpty()) {
                    "No contacts on this device. Share the link or the QR below."
                } else {
                    "No contact matches that search."
                },
                color = palette.textBody,
                fontFamily = PlusJakartaSans,
                fontSize = 11.sp,
            )
            else -> Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(palette.field)
                    .padding(horizontal = 10.dp),
            ) {
                shown.forEachIndexed { index, person ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Box(
                            Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(palette.accent.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                person.name.split(" ")
                                    .take(2)
                                    .mapNotNull { it.firstOrNull()?.uppercase() }
                                    .joinToString("")
                                    .ifBlank { "?" },
                                color = palette.accentMuted,
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                            )
                        }
                        Column(Modifier.weight(1f)) {
                            Text(
                                person.name.ifBlank { "Unnamed contact" },
                                color = palette.textPrimary,
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp,
                                maxLines = 1,
                            )
                            Text(
                                person.subtitle,
                                color = palette.textMuted,
                                fontFamily = PlusJakartaSans,
                                fontSize = 11.sp,
                                maxLines = 1,
                            )
                        }
                        Text(
                            "Send",
                            color = if (shareBody == null) palette.textFaint else palette.accent,
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier
                                .clip(CircleShape)
                                .border(
                                    1.dp,
                                    if (shareBody == null) palette.chipStroke else palette.accent.copy(alpha = 0.55f),
                                    CircleShape,
                                )
                                .clickable(enabled = shareBody != null) {
                                    shareBody?.let { sendInviteWhatsApp(context, person.phone, it) }
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                        )
                    }
                    if (index < shown.lastIndex) {
                        Box(Modifier.fillMaxWidth().height(1.dp).background(palette.divider))
                    }
                }
            }
        }

        Text(
            "OR SHARE INVITE LINK",
            color = palette.textFaint,
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            letterSpacing = 0.6.sp,
        )
        when {
            minting && invite == null -> CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                color = palette.accent,
                strokeWidth = 2.dp,
            )
            invitePath != null -> Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(palette.field)
                    .padding(horizontal = 10.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    invitePath,
                    color = palette.accentMuted,
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    if (copied) "Copied" else "Copy",
                    color = palette.accent,
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    modifier = Modifier
                        .clip(CircleShape)
                        .border(1.dp, palette.accent.copy(alpha = 0.55f), CircleShape)
                        .clickable {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("invite", invitePath))
                            copied = true
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
            companyId == null -> Text(
                "This $noun is not linked to a company yet, so there is nothing to invite into.",
                color = palette.textBody,
                fontFamily = PlusJakartaSans,
                fontSize = 11.sp,
            )
        }

        Text(
            "OR SCAN TO JOIN",
            color = palette.textFaint,
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            letterSpacing = 0.6.sp,
        )
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(palette.qrPlate)
                .border(1.dp, palette.accent.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (qrBitmap != null) {
                Image(
                    bitmap = qrBitmap.asImageBitmap(),
                    contentDescription = "Invite QR",
                    modifier = Modifier
                        .size(168.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .padding(10.dp),
                )
            } else {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(palette.field)
                        .border(1.dp, palette.chipStroke, RoundedCornerShape(12.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        "QR code needs an invite token",
                        color = palette.textPrimary,
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                    )
                    Text(
                        "The QR is drawn from the minted invite code — never from a placeholder.",
                        color = palette.textBody,
                        fontFamily = PlusJakartaSans,
                        fontSize = 11.sp,
                    )
                }
            }
            Text(
                "$displayName · joins as ${businessInviteRoleLabel(invite?.operatingRole ?: roleCode)} by default",
                color = palette.textMuted,
                fontFamily = PlusJakartaSans,
                fontSize = 10.sp,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                InviteOutlineAction(
                    title = if (saved) "Saved" else "Save to Photos",
                    enabled = qrBitmap != null,
                    palette = palette,
                ) {
                    val bitmap = qrBitmap ?: return@InviteOutlineAction
                    if (saveInviteQrToPhotos(context, bitmap)) {
                        saved = true
                    } else {
                        onError("Could not save the QR to Photos")
                    }
                }
                InviteOutlineAction("Share QR", enabled = qrBitmap != null, palette = palette) {
                    val bitmap = qrBitmap ?: return@InviteOutlineAction
                    shareInviteQr(context, bitmap, invitePath ?: "")
                }
            }
        }
    } else {
        Text(
            "Invites are owner-only. Ask the owner or an admin for the link — no role is granted without them.",
            color = palette.textBody,
            fontFamily = PlusJakartaSans,
            fontSize = 11.sp,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(palette.field)
                .border(1.dp, palette.chipStroke, RoundedCornerShape(12.dp))
                .padding(14.dp),
        )
    }

    Text(
        "Invite grants a role and its access. Joining ≠ approval ≠ payment.",
        color = palette.textBody,
        fontFamily = PlusJakartaSans,
        fontSize = 11.sp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(palette.accent.copy(alpha = 0.10f))
            .border(1.dp, palette.accent.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
            .padding(10.dp),
    )
}

internal fun inviteRoleAccess(role: String, distribution: Boolean = false): String =
    if (distribution) {
        when (role) {
            "FINANCE" -> "money, settlements and finance boards"
            "APPROVER" -> "warehouse stock, GRNs and holds"
            "OPERATIONS_LEAD" -> "routes, dispatches, dealers and orders"
            else -> "records route activity; money waits for approval"
        }
    } else {
        when (role) {
            "FINANCE" -> "money, payments and finance boards"
            "APPROVER" -> "approves money and stock adjustments"
            "OPERATIONS_LEAD" -> "sales, stock, purchases and orders"
            else -> "records activity; money waits for approval"
        }
    }

@Composable
private fun InviteOutlineAction(
    title: String,
    enabled: Boolean,
    palette: InvitePalette,
    onClick: () -> Unit,
) {
    Text(
        title,
        color = if (enabled) palette.accent else palette.textFaint,
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .border(
                1.dp,
                if (enabled) palette.accent.copy(alpha = 0.55f) else palette.chipStroke,
                RoundedCornerShape(12.dp),
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    )
}
