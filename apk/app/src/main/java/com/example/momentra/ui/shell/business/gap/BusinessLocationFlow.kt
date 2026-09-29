package com.example.momentra.ui.shell.business.gap

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.data.api.ApiClient
import com.example.momentra.data.api.CreateLocationBody
import com.example.momentra.data.api.LocationItemDto
import com.example.momentra.ui.theme.PlusJakartaSans
import java.time.Instant
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.launch

@Composable
fun BusinessLocationFlow(
    companyId: String,
    page: BusinessGapPage,
    selected: LocationItemDto?,
    onSelect: (LocationItemDto) -> Unit,
    onPage: (BusinessGapPage) -> Unit,
    onBackToSettings: () -> Unit,
) {
    var locations by remember { mutableStateOf<List<LocationItemDto>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var adding by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<LocationItemDto?>(null) }
    var pendingRemove by remember { mutableStateOf<LocationItemDto?>(null) }
    var refresh by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(companyId, refresh) {
        loading = true
        runCatching { ApiClient.apiService.listLocations(companyId).data.items }
            .onSuccess {
                locations = it
                error = null
            }
            .onFailure { error = it.message }
        loading = false
    }

    // Dashboard / Config / Inheritance are no longer the primary path — keep list as home.
    if (page != BusinessGapPage.LocationPicker) {
        LaunchedEffect(page) { onPage(BusinessGapPage.LocationPicker) }
    }

    GapScreen(title = "Locations", onBack = onBackToSettings) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            error?.let { Text(it, color = GapMuted, fontSize = 12.sp, fontFamily = PlusJakartaSans) }
            if (loading) CircularProgressIndicator(color = GapAccent)
            if (!loading && locations.isEmpty()) {
                Text("No locations yet.", color = GapMuted, fontSize = 13.sp, fontFamily = PlusJakartaSans)
            }
            locations.forEach { location ->
                GapCard {
                    Text(
                        location.name,
                        color = GapText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = PlusJakartaSans,
                    )
                    Text(
                        location.addressText?.takeIf { it.isNotBlank() } ?: "—",
                        color = GapMuted,
                        fontSize = 12.sp,
                        fontFamily = PlusJakartaSans,
                    )
                    Text(location.status, color = GapMuted, fontSize = 12.sp, fontFamily = PlusJakartaSans)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(top = 8.dp),
                    ) {
                        Text(
                            "Edit",
                            color = GapAccent,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = PlusJakartaSans,
                            modifier = Modifier.clickable {
                                onSelect(location)
                                editing = location
                            },
                        )
                        Text(
                            "Remove",
                            color = ColorRemove,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = PlusJakartaSans,
                            modifier = Modifier.clickable { pendingRemove = location },
                        )
                    }
                }
            }
            Text(
                "Add Location",
                color = GapBg,
                fontWeight = FontWeight.Bold,
                fontFamily = PlusJakartaSans,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(GapAccent)
                    .clickable { adding = true }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            )
        }
    }

    if (adding) {
        AddLocationSheet(
            companyId = companyId,
            onDismiss = { adding = false },
            onCreated = { created ->
                onSelect(created)
                adding = false
                refresh += 1
            },
        )
    }
    editing?.let { loc ->
        EditLocationSheet(
            companyId = companyId,
            location = loc,
            onDismiss = { editing = null },
            onSaved = { updated ->
                onSelect(updated)
                editing = null
                refresh += 1
            },
        )
    }
    pendingRemove?.let { location ->
        AlertDialog(
            onDismissRequest = { pendingRemove = null },
            title = { Text("Remove ${location.name}?") },
            text = {
                Text("It will no longer appear in your locations.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val version = location.version
                        pendingRemove = null
                        if (version == null) {
                            error = "Missing location version — refresh and try again."
                            return@TextButton
                        }
                        scope.launch {
                            runCatching {
                                ApiClient.apiService.patchLocation(
                                    companyId = companyId,
                                    locationId = location.locationId,
                                    idempotencyKey = UUID.randomUUID().toString(),
                                    body = mapOf<String, Any>(
                                        "expectedVersion" to version,
                                        "status" to "INACTIVE",
                                    ),
                                )
                            }.fold(
                                onSuccess = {
                                    if (selected?.locationId == location.locationId) {
                                        onSelect(location.copy(status = "INACTIVE"))
                                    }
                                    refresh += 1
                                },
                                onFailure = { error = it.message },
                            )
                        }
                    },
                ) {
                    Text("Remove", color = ColorRemove)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingRemove = null }) {
                    Text("Cancel")
                }
            },
        )
    }
}

private val ColorRemove = androidx.compose.ui.graphics.Color(0xFFF87171)

@Composable
private fun AddLocationSheet(
    companyId: String,
    onDismiss: () -> Unit,
    onCreated: (LocationItemDto) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GapBg)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Add Location", color = GapText, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
        GapField("Name", name) { name = it }
        GapField("Address", address) { address = it }
        error?.let { Text(it, color = GapMuted, fontSize = 12.sp) }
        Text(
            "Save",
            color = if (name.isNotBlank() && !busy) GapBg else GapMuted,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(if (name.isNotBlank() && !busy) GapAccent else GapCard)
                .clickable(enabled = name.isNotBlank() && !busy) {
                    busy = true
                    scope.launch {
                        runCatching {
                            ApiClient.apiService.createLocation(
                                companyId = companyId,
                                idempotencyKey = UUID.randomUUID().toString(),
                                body = CreateLocationBody(
                                    name = name.trim(),
                                    addressText = address.trim().ifBlank { null },
                                    timezone = ZoneId.systemDefault().id,
                                ),
                            ).data
                        }.fold(
                            onSuccess = {
                                onCreated(
                                    LocationItemDto(
                                        locationId = it.locationId,
                                        name = it.name,
                                        addressText = address.trim().ifBlank { null },
                                        timezone = ZoneId.systemDefault().id,
                                        status = "ACTIVE",
                                        version = 1,
                                    ),
                                )
                            },
                            onFailure = {
                                error = it.message
                                busy = false
                            },
                        )
                    }
                }
                .padding(horizontal = 16.dp, vertical = 12.dp),
        )
        Text("Cancel", color = GapMuted, modifier = Modifier.clickable(onClick = onDismiss))
    }
}

@Composable
private fun EditLocationSheet(
    companyId: String,
    location: LocationItemDto,
    onDismiss: () -> Unit,
    onSaved: (LocationItemDto) -> Unit,
) {
    var name by remember { mutableStateOf(location.name) }
    var address by remember { mutableStateOf(location.addressText.orEmpty()) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GapBg)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Edit Location", color = GapText, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
        GapField("Name", name) { name = it }
        GapField("Address", address) { address = it }
        error?.let { Text(it, color = ColorRemove, fontSize = 12.sp) }
        Text(
            "Save",
            color = if (name.isNotBlank() && !busy) GapBg else GapMuted,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(if (name.isNotBlank() && !busy) GapAccent else GapCard)
                .clickable(enabled = name.isNotBlank() && !busy) {
                    val version = location.version
                    if (version == null) {
                        error = "Missing location version — refresh and try again."
                        return@clickable
                    }
                    busy = true
                    scope.launch {
                        runCatching {
                            ApiClient.apiService.patchLocation(
                                companyId = companyId,
                                locationId = location.locationId,
                                idempotencyKey = UUID.randomUUID().toString(),
                                body = buildMap<String, Any> {
                                    put("expectedVersion", version)
                                    put("name", name.trim())
                                    address.trim().takeIf { it.isNotEmpty() }?.let { put("addressText", it) }
                                    put("timezone", location.timezone ?: ZoneId.systemDefault().id)
                                },
                            )
                        }.fold(
                            onSuccess = {
                                onSaved(
                                    location.copy(
                                        name = name.trim(),
                                        addressText = address.trim().ifBlank { null },
                                        version = version + 1,
                                    ),
                                )
                            },
                            onFailure = {
                                error = it.message
                                busy = false
                            },
                        )
                    }
                }
                .padding(horizontal = 16.dp, vertical = 12.dp),
        )
        Text("Cancel", color = GapMuted, modifier = Modifier.clickable(onClick = onDismiss))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessDateSheet(initial: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    val initialMillis = remember(initial) {
        runCatching {
            java.time.LocalDate.parse(initial).atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
        }.getOrNull()
    }
    val state = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val millis = state.selectedDateMillis
                if (millis == null) onDismiss()
                else {
                    val date = Instant.ofEpochMilli(millis).atZone(ZoneId.of("UTC")).toLocalDate().toString()
                    onConfirm(date)
                }
            }) { Text("Use date") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    ) { DatePicker(state = state) }
}

@Composable
fun BusinessNotesSheet(initial: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf(initial) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GapBg)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Notes", color = GapText, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
        BasicTextField(
            value = text,
            onValueChange = { text = it },
            textStyle = TextStyle(color = GapText, fontSize = 15.sp, fontFamily = PlusJakartaSans),
            cursorBrush = SolidColor(GapAccent),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(GapCard)
                .padding(12.dp),
        )
        Text(
            "Save",
            color = GapBg,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(GapAccent)
                .clickable { onConfirm(text.trim()) }
                .padding(horizontal = 16.dp, vertical = 12.dp),
        )
        Text("Cancel", color = GapMuted, modifier = Modifier.clickable(onClick = onDismiss))
    }
}

@Composable
fun BusinessAddPeopleSheet(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GapBg)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Add people", color = GapText, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
        GapField("Name or email", name) { name = it }
        Text(
            "Add",
            color = if (name.isNotBlank()) GapBg else GapMuted,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(if (name.isNotBlank()) GapAccent else GapCard)
                .clickable(enabled = name.isNotBlank()) { onConfirm(name.trim()) }
                .padding(horizontal = 16.dp, vertical = 12.dp),
        )
        Text("Cancel", color = GapMuted, modifier = Modifier.clickable(onClick = onDismiss))
    }
}

@Composable
fun BusinessActivationSuccess(title: String, onContinue: () -> Unit) {
    Box(Modifier.fillMaxSize().background(GapBg), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(24.dp)) {
            Text("Moment ready", color = GapText, fontSize = 22.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
            Text(title.ifBlank { "Your business moment is active." }, color = GapMuted, fontFamily = PlusJakartaSans)
            Text(
                "Continue",
                color = GapBg,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(GapAccent)
                    .clickable(onClick = onContinue)
                    .padding(horizontal = 18.dp, vertical = 12.dp),
            )
        }
    }
}
