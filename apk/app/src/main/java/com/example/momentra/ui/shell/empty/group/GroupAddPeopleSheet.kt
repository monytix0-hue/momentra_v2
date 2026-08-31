package com.example.momentra.ui.shell.empty.group

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.view.ViewGroup
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.content.ContextCompat
import com.example.momentra.ui.shell.maestro.MaestroIds
import androidx.core.content.FileProvider
import com.example.momentra.R
import com.example.momentra.ui.theme.PlusJakartaSans
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Figma 579:12741 — Popup / Add People bottom sheet. */
@Composable
fun GroupAddPeopleSheet(
    palette: GroupTypePalette,
    experienceTitle: String,
    typeCode: String,
    existingNames: List<String>,
    issuedInviteCode: String? = null,
    onAddPerson: (GroupDraftPerson) -> Unit,
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
    ) { granted ->
        hasContactsPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasContactsPermission) {
            permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
        }
    }

    LaunchedEffect(hasContactsPermission) {
        if (hasContactsPermission) {
            contacts = withContext(Dispatchers.IO) { loadDeviceContacts(context) }
        }
    }

    val inviteCode = issuedInviteCode?.trim()?.lowercase()?.takeIf { it.isNotBlank() }
    val invitePath = inviteCode?.let { GroupInviteLink.displayPath(it) }
    val copyText = inviteCode?.let { GroupInviteLink.copyText(it) }
    val qrPayload = inviteCode?.let { GroupInviteLink.qrPayload(it) }
    val qrSizePx = with(LocalDensity.current) { 144.dp.roundToPx() }
    val qrBitmap = remember(qrPayload, qrSizePx) {
        qrPayload?.let { generateInviteQrBitmap(it, qrSizePx) }
    }

    val sourceContacts = remember(hasContactsPermission, contacts) {
        if (hasContactsPermission && contacts.isNotEmpty()) contacts else figmaDemoContacts()
    }
    val existingLower = remember(existingNames) { existingNames.map { it.lowercase() }.toSet() }
    val filteredAll = remember(sourceContacts, query) {
        val q = query.trim()
        if (q.isEmpty()) sourceContacts
        else sourceContacts.filter {
            it.name.contains(q, ignoreCase = true) || it.subtitle.contains(q, ignoreCase = true)
        }
    }
    val contactPreviewLimit = 5
    val filtered = remember(filteredAll, query) {
        if (query.isBlank() && filteredAll.size > contactPreviewLimit) {
            filteredAll.take(contactPreviewLimit)
        } else {
            filteredAll
        }
    }
    val hasMoreContacts = query.isBlank() && filteredAll.size > contactPreviewLimit
    val typedQuery = query.trim()
    val showTypedAdd = typedQuery.isNotEmpty() &&
        existingLower.none { it == typedQuery.lowercase() } &&
        filteredAll.none { it.name.equals(typedQuery, ignoreCase = true) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
        ),
    ) {
        val dialogView = LocalView.current
        androidx.compose.runtime.SideEffect {
            val window = (dialogView.parent as? DialogWindowProvider)?.window ?: return@SideEffect
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            window.setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
            window.setDimAmount(0f)
        }
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
                    .fillMaxWidth()
                    .fillMaxHeight(0.92f)
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(GroupSetupTheme.Card)
                    .border(
                        width = 1.dp,
                        color = GroupSetupTheme.Border,
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    )
                    .navigationBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 64.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 36.dp, height = 4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(GroupSetupTheme.Border),
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "Add People",
                                color = GroupSetupTheme.TextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = PlusJakartaSans,
                                modifier = Modifier.weight(1f),
                            )
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(GroupSetupTheme.IconSurface)
                                    .clickable(onClick = onDismiss)
                                    .semantics {
                                        role = Role.Button
                                        contentDescription = "Close"
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Image(
                                    painterResource(R.drawable.ges_icon_x_circle),
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(GroupSetupTheme.Bg)
                            .border(1.dp, GroupSetupTheme.Border, RoundedCornerShape(14.dp))
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Image(
                            painterResource(R.drawable.ges_icon_search),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            colorFilter = ColorFilter.tint(GroupSetupTheme.TextSecondary),
                        )
                        BasicTextField(
                            value = query,
                            onValueChange = { query = it },
                            textStyle = TextStyle(
                                color = GroupSetupTheme.TextPrimary,
                                fontSize = 13.sp,
                                fontFamily = PlusJakartaSans,
                            ),
                            cursorBrush = SolidColor(palette.accent),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            decorationBox = { inner ->
                                Box {
                                    if (query.isEmpty()) {
                                        Text(
                                            "Search contacts, email or phone",
                                            color = GroupSetupTheme.TextSecondary,
                                            fontSize = 13.sp,
                                            fontFamily = PlusJakartaSans,
                                        )
                                    }
                                    inner()
                                }
                            },
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = true)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                "From Your Contacts".uppercase(),
                                color = palette.accent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = PlusJakartaSans,
                            )
                            if (filtered.isEmpty() && !showTypedAdd) {
                                Text(
                                    "No matching contacts.",
                                    color = GroupSetupTheme.TextSecondary,
                                    fontSize = 12.sp,
                                    fontFamily = PlusJakartaSans,
                                    modifier = Modifier.padding(vertical = 8.dp),
                                )
                            }
                            filtered.forEachIndexed { index, contact ->
                                if (index > 0) {
                                    HorizontalDivider(color = GroupSetupTheme.Border, thickness = 1.dp)
                                }
                                val already = existingLower.contains(contact.name.lowercase())
                                ContactInviteRow(
                                    name = contact.name,
                                    subtitle = contact.subtitle.ifBlank { "Contact" },
                                    photoUri = contact.photoUri,
                                    avatarRes = contact.avatarRes,
                                    added = already,
                                    palette = palette,
                                    onAdd = {
                                        if (!already) {
                                            val avatar = contact.avatarRes ?: R.drawable.ges_avatar_6
                                            onAddPerson(
                                                GroupDraftPerson(
                                                    name = contact.name,
                                                    roleCode = "PARTICIPANT",
                                                    roleLabel = "Member",
                                                    avatarRes = avatar,
                                                    avatarUri = contact.photoUri,
                                                    useInitials = contact.photoUri.isNullOrBlank() &&
                                                        contact.avatarRes == null,
                                                    contactEmail = contact.email,
                                                    contactPhone = contact.phone,
                                                ),
                                            )
                                        }
                                    },
                                )
                            }
                            if (showTypedAdd) {
                                if (filtered.isNotEmpty()) {
                                    HorizontalDivider(color = GroupSetupTheme.Border, thickness = 1.dp)
                                }
                                ContactInviteRow(
                                    name = typedQuery,
                                    subtitle = "Add from search",
                                    photoUri = null,
                                    added = false,
                                    palette = palette,
                                    onAdd = {
                                        val looksEmail = typedQuery.contains("@")
                                        val looksPhone = typedQuery.any { it.isDigit() } &&
                                            typedQuery.filter { it.isDigit() }.length >= 7
                                        onAddPerson(
                                            GroupDraftPerson(
                                                name = typedQuery,
                                                roleCode = "PARTICIPANT",
                                                roleLabel = "Member",
                                                avatarRes = R.drawable.ges_avatar_6,
                                                useInitials = true,
                                                contactEmail = typedQuery.takeIf { looksEmail },
                                                contactPhone = typedQuery.takeIf { looksPhone && !looksEmail },
                                            ),
                                        )
                                        query = ""
                                    },
                                )
                            }
                            if (hasMoreContacts) {
                                Text(
                                    "Search to find more contacts",
                                    color = GroupSetupTheme.TextSecondary,
                                    fontSize = 12.sp,
                                    fontFamily = PlusJakartaSans,
                                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                                )
                            }
                        }

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            HorizontalDivider(color = GroupSetupTheme.Border, thickness = 1.dp)
                            Text(
                                "Or share invite link",
                                color = GroupSetupTheme.TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = PlusJakartaSans,
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(GroupSetupTheme.Bg)
                                    .border(1.dp, GroupSetupTheme.Border, RoundedCornerShape(12.dp))
                                    .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    invitePath ?: "Getting a short invite…",
                                    color = GroupSetupTheme.TextSecondary,
                                    fontSize = 12.sp,
                                    fontFamily = PlusJakartaSans,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag(MaestroIds.GROUP_INVITE_CODE),
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(palette.accent)
                                        .clickable(enabled = copyText != null) {
                                            val text = copyText ?: return@clickable
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
                                        color = GroupSetupTheme.CtaText,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = PlusJakartaSans,
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = GroupSetupTheme.Border, thickness = 1.dp)

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(
                                "Or scan to join".uppercase(),
                                color = palette.accent,
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
                                        modifier = Modifier.size(144.dp),
                                        contentScale = ContentScale.Fit,
                                    )
                                } else {
                                    Box(
                                        Modifier.size(144.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        CircularProgressIndicator(
                                            color = palette.accent,
                                            strokeWidth = 2.dp,
                                            modifier = Modifier.size(28.dp),
                                        )
                                    }
                                }
                            }
                            Text(
                                "Scan with Momentra app to join instantly",
                                color = GroupSetupTheme.TextSecondary,
                                fontSize = 11.sp,
                                fontFamily = PlusJakartaSans,
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            OutlineActionButton(
                                iconRes = R.drawable.ges_icon_share,
                                label = "Share QR",
                                palette = palette,
                                enabled = qrBitmap != null && copyText != null,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    val bitmap = qrBitmap ?: return@OutlineActionButton
                                    shareQr(context, bitmap, copyText ?: return@OutlineActionButton)
                                },
                            )
                            OutlineActionButton(
                                iconRes = R.drawable.ges_icon_download,
                                label = if (saved) "Saved" else "Save to Photos",
                                palette = palette,
                                enabled = qrBitmap != null,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    val bitmap = qrBitmap ?: return@OutlineActionButton
                                    if (saveQrToPhotos(context, bitmap)) {
                                        saved = true
                                        scope.launch {
                                            delay(1600)
                                            saved = false
                                        }
                                    }
                                },
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(palette.accentGradient)
                        .clickable(onClick = onDismiss)
                        .semantics {
                            role = Role.Button
                            contentDescription = "Done"
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "Done",
                        color = GroupSetupTheme.CtaText,
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
private fun ContactInviteRow(
    name: String,
    subtitle: String,
    photoUri: String?,
    added: Boolean,
    palette: GroupTypePalette,
    onAdd: () -> Unit,
    avatarRes: Int? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        GroupContactAvatar(
            name = name,
            photoUri = photoUri,
            avatarRes = avatarRes,
            size = 40.dp,
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                name,
                color = GroupSetupTheme.TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = PlusJakartaSans,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                subtitle,
                color = GroupSetupTheme.TextSecondary,
                fontSize = 12.sp,
                fontFamily = PlusJakartaSans,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(
                    if (added) GroupSetupTheme.Border.copy(alpha = 0.2f)
                    else palette.accent.copy(alpha = 0.13f),
                )
                .border(
                    1.dp,
                    if (added) GroupSetupTheme.Border.copy(alpha = 0.35f)
                    else palette.accent.copy(alpha = 0.2f),
                    RoundedCornerShape(20.dp),
                )
                .clickable(enabled = !added, onClick = onAdd)
                .padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            Text(
                if (added) "Added" else "+ Add",
                color = if (added) GroupSetupTheme.TextSecondary else palette.accent,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = PlusJakartaSans,
            )
        }
    }
}

@Composable
private fun OutlineActionButton(
    iconRes: Int,
    label: String,
    palette: GroupTypePalette,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val stroke = if (enabled) palette.accent else palette.accent.copy(alpha = 0.35f)
    Row(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, stroke, RoundedCornerShape(14.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Image(
            painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            colorFilter = ColorFilter.tint(stroke),
        )
        Spacer(Modifier.size(6.dp))
        Text(
            label,
            color = stroke,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = PlusJakartaSans,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun shareQr(context: android.content.Context, bitmap: Bitmap, inviteUrl: String) {
    val file = File(context.cacheDir, "momentra-invite-qr.png")
    file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_TEXT, inviteUrl)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share QR"))
}

private fun saveQrToPhotos(context: android.content.Context, bitmap: Bitmap): Boolean {
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, "momentra-invite-qr.png")
        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Momentra")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
    }
    val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        ?: return false
    context.contentResolver.openOutputStream(uri)?.use { out ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
    } ?: return false
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        values.clear()
        values.put(MediaStore.Images.Media.IS_PENDING, 0)
        context.contentResolver.update(uri, values, null, null)
    }
    return true
}
