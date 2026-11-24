package com.example.momentra.ui.account

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.data.api.ConsentPurposeDto
import com.example.momentra.data.api.DeviceItemDto
import com.example.momentra.data.api.ErasurePreflightDto
import com.example.momentra.data.api.NotificationCategoriesDto
import com.example.momentra.data.api.ApiClient
import com.example.momentra.data.device.DeviceRegistrar
import com.example.momentra.ui.shell.maestro.MaestroIds
import com.example.momentra.data.local.AppPreferences
import com.example.momentra.data.repository.AccountRepository
import com.example.momentra.ui.shell.shared.PersonLabel
import com.example.momentra.data.security.AppLockStore
import com.example.momentra.data.security.SecurityPreferences
import com.example.momentra.domain.ShellIdentity
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private val AccountBg = Color(0xFF14121B)
private val CardSurface = Color(0xFF1B1824)
private val CardBorder = Color(0xFF2A2D3D)
private val TextPrimary = Color(0xFFF5F2FC)
private val TextSecondary = Color(0xFFC9C4D8)
private val BrandPurple = Color(0xFF7C5CFC)
private val DestructiveRed = Color(0xFFFF453A)

/** S7 Account hub — Redesigned with global color palette tokens for pristine readability & UX. */
@Composable
fun AccountHubSheet(
    identity: ShellIdentity,
    onDisplayNameSaved: (String) -> Unit = {},
    onSignOut: () -> Unit,
    onClose: () -> Unit,
    onAccountDeleted: () -> Unit,
    onReplayTour: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val accountRepo = remember { AccountRepository() }
    val lockStore = remember { AppLockStore(context) }
    val securityPrefs = remember { SecurityPreferences(context) }

    var displayName by remember { mutableStateOf(identity.displayName.orEmpty()) }
    var saving by remember { mutableStateOf(false) }
    var statusMsg by remember { mutableStateOf<String?>(null) }
    var hideBalances by remember { mutableStateOf(securityPrefs.hideBalances()) }
    var pushEnabled by remember { mutableStateOf(securityPrefs.notificationsEnabledLocal()) }
    var digestEnabled by remember { mutableStateOf(false) }
    var quietStart by remember { mutableStateOf("22:00") }
    var quietEnd by remember { mutableStateOf("07:00") }
    var categoryMap by remember {
        mutableStateOf(
            mapOf(
                "finance" to true,
                "tasks" to true,
                "social" to true,
                "invites" to true,
                "approvals" to true,
                "reminders" to true,
            ),
        )
    }
    var biometrics by remember { mutableStateOf(lockStore.biometricsEnabled()) }
    var pinEnabled by remember { mutableStateOf(lockStore.isPinEnabled()) }
    var pinInput by remember { mutableStateOf("") }
    var devices by remember { mutableStateOf<List<DeviceItemDto>>(emptyList()) }
    var consents by remember { mutableStateOf<List<ConsentPurposeDto>>(emptyList()) }
    var confirmDelete by remember { mutableStateOf(false) }
    var erasurePreflight by remember { mutableStateOf<ErasurePreflightDto?>(null) }
    var deletingAccount by remember { mutableStateOf(false) }
    var hubSection by remember { mutableStateOf("home") }
    val appPrefs = remember { AppPreferences(context) }

    LaunchedEffect(Unit) {
        DeviceRegistrar.register(context)
        devices = accountRepo.listDevices().getOrDefault(emptyList())
        consents = accountRepo.listConsents().getOrDefault(emptyList())
        accountRepo.getNotificationPreferences().onSuccess {
            pushEnabled = it.pushNotificationsEnabled
            securityPrefs.setNotificationsEnabledLocal(it.pushNotificationsEnabled)
            digestEnabled = it.digestEnabled == true
            quietStart = it.quietHoursStart?.take(5) ?: "22:00"
            quietEnd = it.quietHoursEnd?.take(5) ?: "07:00"
            val c = it.categories
            if (c != null) {
                categoryMap = mapOf(
                    "finance" to (c.finance != false),
                    "tasks" to (c.tasks != false),
                    "social" to (c.social != false),
                    "invites" to (c.invites != false),
                    "approvals" to (c.approvals != false),
                    "reminders" to (c.reminders != false),
                )
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AccountBg)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
            .semantics { contentDescription = "Account hub" },
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Account Settings",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = TextPrimary,
                modifier = Modifier.semantics { heading() },
            )
            TextButton(onClick = onClose) {
                Text("Close", color = TextSecondary)
            }
        }

        statusMsg?.let {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(BrandPurple.copy(alpha = 0.15f))
                    .padding(12.dp),
            ) {
                Text(
                    text = it,
                    fontSize = 13.sp,
                    color = TextPrimary,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
        }

        when (hubSection) {
            "home" -> {
                val trimmedName = displayName.trim()
                val visibleName = PersonLabel.accountName(trimmedName.ifBlank { identity.displayName })

                // Profile Card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CardSurface)
                        .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(BrandPurple),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = profileInitials(visibleName),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = visibleName.ifBlank { "Your name" },
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = TextPrimary,
                            )
                            PersonLabel.contactEmail(identity.email)?.let { email ->
                                Text(text = email, fontSize = 13.sp, color = TextSecondary)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        label = { Text("Display name", color = TextSecondary) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BrandPurple,
                            unfocusedBorderColor = CardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedLabelColor = BrandPurple,
                            unfocusedLabelColor = TextSecondary,
                        ),
                    )

                    Button(
                        onClick = {
                            scope.launch {
                                saving = true
                                accountRepo.patchMe(displayName = trimmedName)
                                    .onSuccess { saved ->
                                        val applied = saved.displayName?.trim().orEmpty().ifBlank { trimmedName }
                                        displayName = applied
                                        onDisplayNameSaved(applied)
                                        statusMsg = "Profile saved successfully"
                                    }
                                    .onFailure { statusMsg = it.message ?: "Save failed" }
                                saving = false
                            }
                        },
                        enabled = trimmedName.isNotEmpty() && !saving,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandPurple),
                        shape = RoundedCornerShape(12.dp),
                    ) { Text(if (saving) "Saving…" else "Save Profile", color = Color.White, fontWeight = FontWeight.SemiBold) }
                }

                // Settings Navigation Menu Card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CardSurface)
                        .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
                        .padding(vertical = 4.dp),
                ) {
                    SettingMenuItem("Security", "PIN, biometrics & screen protection") { hubSection = "security" }
                    HorizontalDivider(color = CardBorder, thickness = 1.dp)
                    SettingMenuItem("Notifications", "Action-needed alerts, digest & quiet hours") { hubSection = "prefs" }
                    HorizontalDivider(color = CardBorder, thickness = 1.dp)
                    SettingMenuItem("Privacy and consent", "Data consents, policies & export") { hubSection = "privacy" }
                    HorizontalDivider(color = CardBorder, thickness = 1.dp)
                    SettingMenuItem("Devices", "Active sessions & security revocation") { hubSection = "devices" }
                    HorizontalDivider(color = CardBorder, thickness = 1.dp)
                    SettingMenuItem("Help and legal", "Support & grievance redressal") { hubSection = "legal" }
                    if (onReplayTour != null) {
                        HorizontalDivider(color = CardBorder, thickness = 1.dp)
                        SettingMenuItem("Replay Personal tour", "Interactive walkthrough") { onReplayTour() }
                    }
                }

                // Sign Out & Account Deletion Card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CardSurface)
                        .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Button(
                        onClick = onSignOut,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(MaestroIds.ACCOUNT_SIGN_OUT)
                            .semantics { contentDescription = "Sign out" },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2D3D)),
                        shape = RoundedCornerShape(12.dp),
                    ) { Text("Sign out", color = TextPrimary, fontWeight = FontWeight.SemiBold) }

                    if (!confirmDelete) {
                        TextButton(
                            onClick = {
                                scope.launch {
                                    accountRepo.erasurePreflight()
                                        .onSuccess {
                                            erasurePreflight = it
                                            confirmDelete = true
                                        }
                                        .onFailure { statusMsg = it.message ?: "Could not check erasure readiness" }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Delete account…", color = DestructiveRed, fontWeight = FontWeight.Medium)
                        }
                    } else {
                        val blockers = erasurePreflight?.blockers.orEmpty()
                        if (blockers.isNotEmpty()) {
                            Text(
                                text = "Resolve these before deleting your account:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = DestructiveRed,
                            )
                            blockers.forEach { b ->
                                Text(
                                    text = "• ${b.title ?: b.id}: ${b.message ?: b.kind}",
                                    fontSize = 12.sp,
                                    color = TextSecondary,
                                )
                            }
                        } else {
                            Text(
                                text = "This erases your principal-owned data: profile identifiers anonymized, devices revoked, personal-only moments and USER media removed, consents withdrawn. Shared Group/Business content stays for other members after you leave.",
                                fontSize = 12.sp,
                                color = TextSecondary,
                            )
                        }
                        Button(
                            onClick = {
                                if (erasurePreflight?.canErase != true) return@Button
                                scope.launch {
                                    deletingAccount = true
                                    accountRepo.softDeleteMe()
                                        .onSuccess {
                                            runCatching {
                                                FirebaseAuth.getInstance().currentUser?.delete()?.await()
                                            }
                                            appPrefs.clearAgeGateAccepted()
                                            appPrefs.setConsentGateSeen(false)
                                            runCatching { lockStore.clearPin() }
                                            context.getSharedPreferences("retail_hold_session", 0)
                                                .edit().clear().apply()
                                            ApiClient.clearAuthToken()
                                            onAccountDeleted()
                                        }
                                        .onFailure { statusMsg = it.message ?: "Delete failed" }
                                    deletingAccount = false
                                }
                            },
                            enabled = erasurePreflight?.canErase == true && !deletingAccount,
                            colors = ButtonDefaults.buttonColors(containerColor = DestructiveRed),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(if (deletingAccount) "Deleting…" else "Confirm delete account", color = Color.White) }
                        TextButton(onClick = { confirmDelete = false; erasurePreflight = null }) {
                            Text("Cancel", color = TextSecondary)
                        }
                    }
                }
            }

            "security" -> {
                SubSectionScaffold(title = "App Security", onBack = { hubSection = "home" }) {
                    Text(
                        text = "PIN and biometrics lock this device only. Never sent to Momentra servers.",
                        fontSize = 13.sp,
                        color = TextSecondary,
                    )
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { if (it.length <= 8 && it.all(Char::isDigit)) pinInput = it },
                        label = { Text(if (pinEnabled) "New PIN" else "Set PIN (4–8 digits)", color = TextSecondary) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BrandPurple,
                            unfocusedBorderColor = CardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                        ),
                    )
                    Button(
                        onClick = {
                            runCatching {
                                lockStore.setPin(pinInput)
                                pinEnabled = true
                                pinInput = ""
                                statusMsg = "PIN saved locally"
                            }.onFailure { statusMsg = it.message }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandPurple),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(if (pinEnabled) "Change PIN" else "Enable PIN", color = Color.White) }

                    if (pinEnabled) {
                        TextButton(onClick = {
                            lockStore.clearPin()
                            pinEnabled = false
                            biometrics = false
                            statusMsg = "PIN removed"
                        }) { Text("Remove PIN", color = DestructiveRed) }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Unlock with biometrics", color = TextPrimary)
                        Switch(
                            checked = biometrics && pinEnabled,
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = BrandPurple),
                            onCheckedChange = {
                                if (!pinEnabled) {
                                    statusMsg = "Set a PIN first"
                                } else {
                                    biometrics = it
                                    lockStore.setBiometricsEnabled(it)
                                }
                            },
                            enabled = pinEnabled,
                        )
                    }

                    if (pinEnabled) {
                        Text(
                            text = "Auto-lock after ${lockStore.autoLockSeconds()}s in background (0 = immediate).",
                            fontSize = 12.sp,
                            color = TextSecondary,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(0, 30, 60, 300).forEach { sec ->
                                TextButton(
                                    onClick = { lockStore.setAutoLockSeconds(sec) },
                                    colors = ButtonDefaults.textButtonColors(contentColor = BrandPurple),
                                ) {
                                    Text("${sec}s")
                                }
                            }
                        }
                    }

                    var secureScreen by remember { mutableStateOf(lockStore.secureScreenEnabled()) }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Secure Screen", color = TextPrimary)
                            Text("Prevent screenshots & app recents preview", fontSize = 11.sp, color = TextSecondary)
                        }
                        Switch(
                            checked = secureScreen,
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = BrandPurple),
                            onCheckedChange = {
                                secureScreen = it
                                lockStore.setSecureScreenEnabled(it)
                            },
                        )
                    }
                }
            }

            "prefs" -> {
                SubSectionScaffold(title = "Notifications", onBack = { hubSection = "home" }) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Push Notifications", color = TextPrimary, fontWeight = FontWeight.Medium)
                        Switch(
                            checked = pushEnabled,
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = BrandPurple),
                            onCheckedChange = { enabled ->
                                pushEnabled = enabled
                                securityPrefs.setNotificationsEnabledLocal(enabled)
                                scope.launch {
                                    accountRepo.patchNotificationPreferences(enabled = enabled)
                                        .onSuccess { statusMsg = if (enabled) "Push enabled" else "Push muted" }
                                        .onFailure { statusMsg = it.message ?: "Save failed" }
                                }
                            },
                        )
                    }
                    Text(text = "What you hear about", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = TextSecondary)
                    Text(
                        text = "Pushes fire on create/update/delete and when open work needs you — not empty daily digests.",
                        fontSize = 12.sp,
                        color = TextSecondary,
                    )
                    listOf(
                        "finance" to "Money & expenses",
                        "tasks" to "Tasks & planning",
                        "social" to "Social & memories",
                        "invites" to "Invitations",
                        "approvals" to "Approvals",
                        "reminders" to "Reminders",
                    ).forEach { (key, label) ->
                        val checked = categoryMap[key] != false
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(label, color = TextPrimary)
                            Switch(
                                checked = checked,
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = BrandPurple),
                                onCheckedChange = { on ->
                                    categoryMap = categoryMap + (key to on)
                                    scope.launch {
                                        accountRepo.patchNotificationPreferences(
                                            categories = NotificationCategoriesDto(
                                                finance = categoryMap["finance"],
                                                tasks = categoryMap["tasks"],
                                                social = categoryMap["social"],
                                                invites = categoryMap["invites"],
                                                approvals = categoryMap["approvals"],
                                                reminders = categoryMap["reminders"],
                                            ),
                                        )
                                    }
                                },
                            )
                        }
                    }
                    Text(text = "Delivery", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = TextSecondary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Smart digest", color = TextPrimary)
                        Switch(
                            checked = digestEnabled,
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = BrandPurple),
                            onCheckedChange = { on ->
                                digestEnabled = on
                                scope.launch {
                                    accountRepo.patchNotificationPreferences(digestEnabled = on)
                                }
                            },
                        )
                    }
                    Text("Quiet hours: $quietStart – $quietEnd", fontSize = 12.sp, color = TextSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = {
                            quietStart = "22:00"; quietEnd = "07:00"
                            scope.launch {
                                accountRepo.patchNotificationPreferences(
                                    quietHoursStart = "22:00",
                                    quietHoursEnd = "07:00",
                                )
                            }
                        }) { Text("10pm–7am", color = BrandPurple) }
                        TextButton(onClick = {
                            quietStart = ""; quietEnd = ""
                            scope.launch {
                                accountRepo.patchNotificationPreferences(
                                    quietHoursStart = null,
                                    quietHoursEnd = null,
                                )
                            }
                        }) { Text("Off", color = TextSecondary) }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Hide balances", color = TextPrimary)
                        Switch(
                            checked = hideBalances,
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = BrandPurple),
                            onCheckedChange = {
                                hideBalances = it
                                securityPrefs.setHideBalances(it)
                            },
                        )
                    }
                }
            }

            "privacy" -> {
                SubSectionScaffold(title = "Privacy & Consent", onBack = { hubSection = "home" }) {
                    consents.forEach { c ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(text = c.displayName ?: c.code, color = TextPrimary, modifier = Modifier.weight(1f))
                            Switch(
                                checked = c.granted,
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = BrandPurple),
                                onCheckedChange = { enabled ->
                                    scope.launch {
                                        val r = if (enabled) {
                                            accountRepo.grantConsent(c.code)
                                        } else {
                                            accountRepo.withdrawConsent(c.code)
                                        }
                                        r.onSuccess {
                                            consents = accountRepo.listConsents().getOrDefault(consents)
                                        }.onFailure { statusMsg = it.message }
                                    }
                                },
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Full policies & data management:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
                    TextButton(onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://momentra.tech/privacy")))
                    }) { Text("Privacy Policy", color = BrandPurple) }
                    TextButton(onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://momentra.tech/terms")))
                    }) { Text("Terms of Service", color = BrandPurple) }
                    TextButton(onClick = {
                        scope.launch {
                            accountRepo.exportMe().onSuccess { export ->
                                val json = com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(export)
                                context.startActivity(
                                    Intent(Intent.ACTION_SEND).apply {
                                        type = "application/json"
                                        putExtra(Intent.EXTRA_SUBJECT, "Momentra data export")
                                        putExtra(Intent.EXTRA_TEXT, json)
                                    }.let { Intent.createChooser(it, "Download my data") },
                                )
                            }
                        }
                    }) { Text("Download my data (JSON)", color = BrandPurple) }
                }
            }

            "devices" -> {
                SubSectionScaffold(title = "Authorized Devices", onBack = { hubSection = "home" }) {
                    val currentId = DeviceRegistrar.deviceId(context)
                    devices.filter { !it.revoked }.forEach { d ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(CardSurface)
                                .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = d.platform ?: "Device",
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                )
                                Text(
                                    text = "${d.deviceId.take(12)}${if (d.deviceId == currentId) " (This device)" else ""}",
                                    fontSize = 12.sp,
                                    color = TextSecondary,
                                )
                            }
                            if (d.deviceId != currentId) {
                                TextButton(
                                    onClick = {
                                        scope.launch {
                                            accountRepo.revokeDevice(d.deviceId)
                                            devices = accountRepo.listDevices().getOrDefault(devices)
                                        }
                                    },
                                ) { Text("Revoke", color = DestructiveRed) }
                            }
                        }
                    }
                }
            }

            else -> {
                SubSectionScaffold(title = "Help & Legal", onBack = { hubSection = "home" }) {
                    Text(text = "About Momentra", fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text(
                        text = "Momentra helps you run Personal, Group, and Business moments in one unified shell.",
                        fontSize = 13.sp,
                        color = TextSecondary,
                    )
                    Text(
                        text = "Grievance Contact: hello@momentra.app",
                        fontSize = 13.sp,
                        color = TextSecondary,
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingMenuItem(title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
            Text(subtitle, fontSize = 12.sp, color = TextSecondary)
        }
        Text("›", fontSize = 20.sp, color = TextSecondary)
    }
}

@Composable
private fun SubSectionScaffold(title: String, onBack: () -> Unit, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardSurface)
            .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)
            TextButton(onClick = onBack) {
                Text("Back", color = BrandPurple)
            }
        }
        content()
    }
}

private fun profileInitials(name: String): String {
    val parts = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.take(2)
    val letters = parts.mapNotNull { it.firstOrNull() }.joinToString("")
    return letters.uppercase().ifBlank { "?" }
}
