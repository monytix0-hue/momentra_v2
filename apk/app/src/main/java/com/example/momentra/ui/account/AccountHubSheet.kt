package com.example.momentra.ui.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.data.api.ConsentPurposeDto
import com.example.momentra.data.api.DeviceItemDto
import com.example.momentra.data.device.DeviceRegistrar
import com.example.momentra.ui.shell.maestro.MaestroIds
import com.example.momentra.data.repository.AccountRepository
import com.example.momentra.data.security.AppLockStore
import com.example.momentra.data.security.SecurityPreferences
import com.example.momentra.domain.ShellIdentity
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/** S7 Account hub — FIGMA_GAP shell-consistent UI. PIN never leaves the device. */
@Composable
fun AccountHubSheet(
    identity: ShellIdentity,
    onSignOut: () -> Unit,
    onClose: () -> Unit,
    onAccountDeleted: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val accountRepo = remember { AccountRepository() }
    val lockStore = remember { AppLockStore(context) }
    val securityPrefs = remember { SecurityPreferences(context) }

    var displayName by remember { mutableStateOf(identity.displayName.orEmpty()) }
    var statusMsg by remember { mutableStateOf<String?>(null) }
    var hideBalances by remember { mutableStateOf(securityPrefs.hideBalances()) }
    var biometrics by remember { mutableStateOf(lockStore.biometricsEnabled()) }
    var pinEnabled by remember { mutableStateOf(lockStore.isPinEnabled()) }
    var pinInput by remember { mutableStateOf("") }
    var devices by remember { mutableStateOf<List<DeviceItemDto>>(emptyList()) }
    var consents by remember { mutableStateOf<List<ConsentPurposeDto>>(emptyList()) }
    var confirmDelete by remember { mutableStateOf(false) }
    var hubSection by remember { mutableStateOf("home") }

    LaunchedEffect(Unit) {
        DeviceRegistrar.register(context)
        devices = accountRepo.listDevices().getOrDefault(emptyList())
        consents = accountRepo.listConsents().getOrDefault(emptyList())
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
            .semantics { contentDescription = "Account hub" },
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = "Account", fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Text(text = identity.email ?: "No email", fontSize = 14.sp)
        statusMsg?.let { Text(text = it, fontSize = 13.sp) }

        when (hubSection) {
            "home" -> {
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Display name") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(
                    onClick = {
                        scope.launch {
                            accountRepo.patchMe(displayName = displayName.trim().ifBlank { null })
                                .onSuccess { statusMsg = "Profile saved" }
                                .onFailure { statusMsg = it.message ?: "Save failed" }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Save profile") }

                HorizontalDivider()
                TextButton(onClick = { hubSection = "security" }) { Text("App Security") }
                TextButton(onClick = { hubSection = "privacy" }) { Text("Privacy & Consent") }
                TextButton(onClick = { hubSection = "devices" }) { Text("Devices") }
                TextButton(onClick = { hubSection = "prefs" }) { Text("Preferences") }
                TextButton(onClick = { hubSection = "legal" }) { Text("Help & Legal") }

                HorizontalDivider()
                Button(
                    onClick = onSignOut,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(MaestroIds.ACCOUNT_SIGN_OUT)
                        .semantics { contentDescription = "Sign out" },
                ) { Text("Sign out") }

                if (!confirmDelete) {
                    TextButton(onClick = { confirmDelete = true }) { Text("Delete account…") }
                } else {
                    Text(
                        text = "Soft-deletes your Momentra profile (DELETED). Domain history may be retained.",
                        fontSize = 12.sp,
                    )
                    Button(
                        onClick = {
                            scope.launch {
                                accountRepo.softDeleteMe()
                                    .onSuccess {
                                        runCatching {
                                            FirebaseAuth.getInstance().currentUser?.delete()?.await()
                                        }
                                        onAccountDeleted()
                                    }
                                    .onFailure { statusMsg = it.message ?: "Delete failed" }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Confirm delete account") }
                    TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
                }
            }

            "security" -> {
                Text(text = "App Security", fontWeight = FontWeight.SemiBold)
                Text(
                    text = "PIN and biometrics lock this device only. Never sent to Momentra servers.",
                    fontSize = 12.sp,
                )
                OutlinedTextField(
                    value = pinInput,
                    onValueChange = { if (it.length <= 8 && it.all(Char::isDigit)) pinInput = it },
                    label = { Text(if (pinEnabled) "New PIN" else "Set PIN (4–8 digits)") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(onClick = {
                    runCatching {
                        lockStore.setPin(pinInput)
                        pinEnabled = true
                        pinInput = ""
                        statusMsg = "PIN saved locally"
                    }.onFailure { statusMsg = it.message }
                }) { Text(if (pinEnabled) "Change PIN" else "Enable PIN") }
                if (pinEnabled) {
                    TextButton(onClick = {
                        lockStore.clearPin()
                        pinEnabled = false
                        biometrics = false
                        statusMsg = "PIN removed"
                    }) { Text("Remove PIN") }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Unlock with biometrics")
                    Switch(
                        checked = biometrics && pinEnabled,
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
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(0, 30, 60, 300).forEach { sec ->
                            TextButton(onClick = { lockStore.setAutoLockSeconds(sec) }) {
                                Text("${sec}s")
                            }
                        }
                    }
                }
                TextButton(onClick = { hubSection = "home" }) { Text("Back") }
            }

            "prefs" -> {
                Text(text = "Preferences", fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Hide balances")
                    Switch(
                        checked = hideBalances,
                        onCheckedChange = {
                            hideBalances = it
                            securityPrefs.setHideBalances(it)
                        },
                    )
                }
                Text(
                    text = "Currency / language / appearance: deferred (FIGMA_GAP).",
                    fontSize = 12.sp,
                )
                TextButton(onClick = { hubSection = "home" }) { Text("Back") }
            }

            "privacy" -> {
                Text(text = "Privacy & Consent", fontWeight = FontWeight.SemiBold)
                consents.forEach { c ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(text = c.displayName ?: c.code, modifier = Modifier.weight(1f))
                        Switch(
                            checked = c.granted,
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
                TextButton(onClick = { hubSection = "home" }) { Text("Back") }
            }

            "devices" -> {
                Text(text = "Devices", fontWeight = FontWeight.SemiBold)
                val currentId = DeviceRegistrar.deviceId(context)
                devices.filter { !it.revoked }.forEach { d ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = buildString {
                                append(d.platform ?: "Device")
                                append(" · ")
                                append(d.deviceId.take(12))
                                if (d.deviceId == currentId) append(" (this)")
                            },
                            modifier = Modifier.weight(1f),
                            fontSize = 12.sp,
                        )
                        if (d.deviceId != currentId) {
                            TextButton(onClick = {
                                scope.launch {
                                    accountRepo.revokeDevice(d.deviceId)
                                    devices = accountRepo.listDevices().getOrDefault(devices)
                                }
                            }) { Text("Revoke") }
                        }
                    }
                }
                Text(text = "Logout-all sessions: deferred.", fontSize = 12.sp)
                TextButton(onClick = { hubSection = "home" }) { Text("Back") }
            }

            else -> {
                Text(text = "Help & Legal", fontWeight = FontWeight.SemiBold)
                Text(text = "About Momentra", fontWeight = FontWeight.Medium)
                Text(
                    text = "Momentra helps you run Personal, Group, and Business moments in one shell.",
                    fontSize = 13.sp,
                )
                Text(
                    text = "Privacy Policy / Terms: placeholder (FIGMA_GAP).",
                    fontSize = 12.sp,
                )
                TextButton(onClick = { hubSection = "home" }) { Text("Back") }
            }
        }

        TextButton(onClick = onClose) { Text("Close") }
    }
}
