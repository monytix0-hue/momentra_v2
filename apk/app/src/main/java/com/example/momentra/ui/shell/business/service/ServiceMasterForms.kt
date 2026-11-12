package com.example.momentra.ui.shell.business.service

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.data.api.ApiClient
import com.example.momentra.data.api.OperatingMasterBody
import com.example.momentra.data.api.OperatingMasterDto
import com.example.momentra.data.api.OperatingMasterUpdateBody
import com.example.momentra.data.api.OperatingProjectionDto
import com.example.momentra.data.repository.BusinessSliceRepository
import com.example.momentra.ui.shell.business.shared.IndiaStateCatalog
import com.example.momentra.ui.theme.PlusJakartaSans
import com.example.momentra.ui.theme.ServicesTeal
import kotlinx.coroutines.launch

/**
 * SVC master create/edit forms — Figma:
 * - `2530:29613` New / Edit Client
 * - `2530:29704` Add / Edit Room or Staff
 * - `2530:29783` Create Service offering
 * - `2530:29908` Create Package
 *
 * Persists via [BusinessSliceRepository.createMaster] / [updateMaster] using fields the
 * operating-master contract already accepts. Extra Figma fields without a wire column stay
 * collectible in the UI and are noted in the Phase 2 gaps report (not invented as new APIs).
 */
enum class ServiceMasterFormKind {
    Client,
    RoomStaff,
    Service,
    Package,
}

private val ServiceCategories = listOf("Hair", "Skin", "Spa", "Wellness", "Nails")
private val ClientInterests = listOf("Facial", "Hair", "Spa", "Nails", "Wellness", "Massage")
private val RoomOfferCategories = listOf("Hair", "Skin", "Spa", "Wellness", "Nails")
private val PackageIncludes = listOf("Keratin", "Hair spa", "Trim", "Wash")

@Composable
internal fun ServiceMasterFormSheet(
    kind: ServiceMasterFormKind,
    model: OperatingProjectionDto,
    momentId: String?,
    existing: OperatingMasterDto?,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    onError: (String) -> Unit,
) {
    when (kind) {
        ServiceMasterFormKind.Client -> ClientMasterForm(
            model = model,
            momentId = momentId,
            existing = existing,
            onDismiss = onDismiss,
            onSaved = onSaved,
            onError = onError,
        )
        ServiceMasterFormKind.RoomStaff -> RoomStaffMasterForm(
            model = model,
            momentId = momentId,
            existing = existing,
            onDismiss = onDismiss,
            onSaved = onSaved,
            onError = onError,
        )
        ServiceMasterFormKind.Service -> ServiceOfferingMasterForm(
            model = model,
            momentId = momentId,
            existing = existing,
            onDismiss = onDismiss,
            onSaved = onSaved,
            onError = onError,
        )
        ServiceMasterFormKind.Package -> PackageMasterForm(
            model = model,
            momentId = momentId,
            existing = existing,
            onDismiss = onDismiss,
            onSaved = onSaved,
            onError = onError,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ClientMasterForm(
    model: OperatingProjectionDto,
    momentId: String?,
    existing: OperatingMasterDto?,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    onError: (String) -> Unit,
) {
    val companyId = rememberCompanyId(momentId)
    val repo = remember { BusinessSliceRepository() }
    val scope = rememberCoroutineScope()
    var createMode by remember(existing) { mutableStateOf(existing == null) }
    var working by remember(existing?.id) { mutableStateOf(existing) }

    var name by remember(working?.id) { mutableStateOf(working?.name.orEmpty()) }
    var legalName by remember(working?.id) { mutableStateOf(working?.code.orEmpty()) }
    var gstin by remember(working?.id) { mutableStateOf(working?.gstin.orEmpty()) }
    var gstRegistration by remember(working?.id) {
        mutableStateOf(working?.gstRegistration?.takeIf { it.isNotBlank() } ?: "Regular")
    }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var interests by remember(working?.id) {
        mutableStateOf(
            working?.dimension
                ?.split(',')
                ?.map { it.trim() }
                ?.filter { it.isNotEmpty() }
                ?.toSet()
                ?: setOf("Nails"),
        )
    }
    var preferredStaff by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var status by remember(working?.id) {
        mutableStateOf(
            when (working?.status?.uppercase()) {
                "INACTIVE", "ARCHIVED" -> "Inactive"
                else -> if (working?.code.equals("VIP", ignoreCase = true)) "VIP" else "Active"
            },
        )
    }
    var saving by remember { mutableStateOf(false) }
    var showToast by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }

    val peers = remember(model.masters) {
        model.masters.orEmpty().filter {
            it.kind.uppercase() in setOf("CUSTOMER", "CLIENT", "GUEST")
        }
    }

    FormScaffold(
        title = if (createMode) "New client" else "Edit client",
        subtitle = "Client master · GSTIN optional (B2B invoice only)",
        onClose = onDismiss,
        why = "Client is a master write. Place of supply decides CGST+SGST vs IGST on Finalize Charge. Booking save is commitment only.",
        cta = if (saving) "Saving…" else "Save client",
        caption = "Client saved · available in Booking",
        showToast = showToast,
        ctaEnabled = !saving && name.isNotBlank(),
        onCta = cta@ {
            val id = companyId
            if (id.isNullOrBlank()) {
                localError = "Company unavailable"
                onError("Company unavailable")
                return@cta
            }
            scope.launch {
                saving = true
                localError = null
                val bodyKind = working?.kind?.takeIf { it.isNotBlank() } ?: "CUSTOMER"
                val billing = IndiaStateCatalog.resolve(gstin.trim().take(2))?.name
                val result = if (createMode || working == null) {
                    repo.createMaster(
                        id,
                        OperatingMasterBody(
                            kind = bodyKind,
                            name = name.trim(),
                            code = legalName.trim().ifBlank { null },
                            momentId = momentId,
                            dimension = interests.joinToString(", ").ifBlank { null },
                            gstin = gstin.trim().uppercase().ifBlank { null },
                            gstRegistration = gstRegistration,
                            billingState = billing,
                        ),
                    )
                } else {
                    repo.updateMaster(
                        id,
                        working!!.id,
                        OperatingMasterUpdateBody(
                            name = name.trim(),
                            code = legalName.trim().ifBlank { null },
                            dimension = interests.joinToString(", ").ifBlank { null },
                            gstin = gstin.trim().uppercase().ifBlank { null },
                            billingState = billing,
                            status = when (status) {
                                "Inactive" -> "INACTIVE"
                                else -> "ACTIVE"
                            },
                            expectedVersion = working!!.version,
                        ),
                    )
                }
                result.fold(
                    onSuccess = {
                        showToast = true
                        onSaved()
                    },
                    onFailure = {
                        localError = it.message
                        onError(it.message ?: "Could not save client")
                        saving = false
                    },
                )
            }
        },
    ) {
        ModeChips(
            createMode = createMode,
            onCreate = {
                createMode = true
                working = null
                name = ""
                legalName = ""
                gstin = ""
                gstRegistration = "Regular"
                interests = setOf("Nails")
                status = "Active"
            },
            onEdit = {
                createMode = false
                if (working == null) {
                    peers.firstOrNull()?.let { peer ->
                        working = peer
                        name = peer.name
                        legalName = peer.code.orEmpty()
                        gstin = peer.gstin.orEmpty()
                        gstRegistration = peer.gstRegistration?.takeIf { it.isNotBlank() } ?: "Regular"
                        interests = peer.dimension
                            ?.split(',')
                            ?.map { it.trim() }
                            ?.filter { it.isNotEmpty() }
                            ?.toSet()
                            ?: emptySet()
                    }
                }
            },
        )
        if (!createMode && peers.isNotEmpty() && working == null) {
            FormLabel("Pick client")
            peers.take(6).forEach { peer ->
                TealModeChip(text = peer.name, selected = false, onClick = {
                    working = peer
                    name = peer.name
                    legalName = peer.code.orEmpty()
                    gstin = peer.gstin.orEmpty()
                })
            }
        }

        FormField("Client name", name, "e.g. Ananya Mehta") { name = it }
        FormField("Legal / trade name", legalName, "As on GST certificate") { legalName = it }
        FormField(
            label = "GSTIN (B2B, optional)",
            value = gstin,
            placeholder = "27AAACC1234D1Z7",
            keyboard = KeyboardType.Ascii,
            hint = gstinHint(gstin),
            onValueChange = { gstin = it.uppercase().take(15) },
        )
        FormLabel("GST registration")
        ChipRow(
            options = listOf("Regular", "Composition", "Unregistered"),
            selected = setOf(gstRegistration),
            single = true,
            onToggle = { gstRegistration = it },
        )
        FormField("Phone", phone, "+91 98200 12345", KeyboardType.Phone) { phone = it }
        FormUnavailableHint("Phone · not on master API yet")
        FormField("Email (optional)", email, "ananya@mail.com", KeyboardType.Email) { email = it }
        FormUnavailableHint("Email · not on master API yet")
        FormLabel("Interests")
        ChipRow(
            options = ClientInterests,
            selected = interests,
            single = false,
            onToggle = { chip ->
                interests = if (chip in interests) interests - chip else interests + chip
            },
        )
        FormField("Preferred staff", preferredStaff, "Priya · Chair 1") { preferredStaff = it }
        FormUnavailableHint("Preferred staff · not on master API yet")
        FormField("Address", address, "Pali Hill, Bandra West, Mumbai") { address = it }
        FormUnavailableHint("Address · not on master API yet")
        FormLabel("Status")
        ChipRow(
            options = listOf("Active", "VIP", "Inactive"),
            selected = setOf(status),
            single = true,
            onToggle = { status = it },
        )
        localError?.let {
            Text(it, style = ServicesTeal.rowCaption(ServicesTeal.negative), maxLines = 2)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RoomStaffMasterForm(
    model: OperatingProjectionDto,
    momentId: String?,
    existing: OperatingMasterDto?,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    onError: (String) -> Unit,
) {
    val companyId = rememberCompanyId(momentId)
    val repo = remember { BusinessSliceRepository() }
    val scope = rememberCoroutineScope()
    var createMode by remember(existing) { mutableStateOf(existing == null) }
    var working by remember(existing?.id) { mutableStateOf(existing) }

    var name by remember(working?.id) { mutableStateOf(working?.name.orEmpty()) }
    var type by remember(working?.id) {
        mutableStateOf(
            when (working?.kind?.uppercase()) {
                "STAFF" -> "Staff"
                "ROOM" -> "Room"
                else -> "Chair"
            },
        )
    }
    var colour by remember(working?.id) {
        mutableStateOf(working?.code?.takeIf { it in listOf("Lime", "Teal", "Cream") } ?: "Lime")
    }
    var displayOrder by remember(working?.id) {
        mutableStateOf(working?.conversionToBase?.takeIf { it.isNotBlank() } ?: "5")
    }
    var categories by remember(working?.id) {
        mutableStateOf(
            working?.dimension
                ?.split(',')
                ?.map { it.trim() }
                ?.filter { it.isNotEmpty() }
                ?.toSet()
                ?: setOf("Spa"),
        )
    }
    var assignedStaff by remember { mutableStateOf("") }
    var status by remember(working?.id) {
        mutableStateOf(
            if (working?.status.equals("INACTIVE", ignoreCase = true)) "Paused" else "Active",
        )
    }
    var saving by remember { mutableStateOf(false) }
    var showToast by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }

    FormScaffold(
        title = if (createMode) "Add room / staff" else "Edit room / staff",
        subtitle = "Room, chair or staff · bookable capacity",
        onClose = onDismiss,
        why = "Rooms and staff are capacity. Booking holds a slot; only Finalize Charge creates revenue.",
        cta = if (saving) "Saving…" else "Save room / staff",
        caption = "Saved · calendar slots ready",
        showToast = showToast,
        ctaEnabled = !saving && name.isNotBlank(),
        onCta = cta@ {
            val id = companyId
            if (id.isNullOrBlank()) {
                localError = "Company unavailable"
                onError("Company unavailable")
                return@cta
            }
            scope.launch {
                saving = true
                localError = null
                val kindWire = when (type) {
                    "Staff" -> "STAFF"
                    "Room" -> "ROOM"
                    else -> "CHAIR"
                }
                val result = if (createMode || working == null) {
                    repo.createMaster(
                        id,
                        OperatingMasterBody(
                            kind = kindWire,
                            name = name.trim(),
                            code = colour,
                            momentId = momentId,
                            unit = type,
                            dimension = categories.joinToString(", ").ifBlank { null },
                            conversionToBase = displayOrder.trim().ifBlank { null },
                        ),
                    )
                } else {
                    repo.updateMaster(
                        id,
                        working!!.id,
                        OperatingMasterUpdateBody(
                            kind = kindWire,
                            name = name.trim(),
                            code = colour,
                            unit = type,
                            dimension = categories.joinToString(", ").ifBlank { null },
                            conversionToBase = displayOrder.trim().ifBlank { null },
                            status = if (status == "Paused") "INACTIVE" else "ACTIVE",
                            expectedVersion = working!!.version,
                        ),
                    )
                }
                result.fold(
                    onSuccess = {
                        showToast = true
                        onSaved()
                    },
                    onFailure = {
                        localError = it.message
                        onError(it.message ?: "Could not save room / staff")
                        saving = false
                    },
                )
            }
        },
    ) {
        ModeChips(
            createMode = createMode,
            onCreate = {
                createMode = true
                working = null
                name = ""
                type = "Chair"
                colour = "Lime"
                displayOrder = "5"
                categories = setOf("Spa")
                status = "Active"
            },
            onEdit = { createMode = false },
        )
        FormField("Room / staff name", name, "e.g. Spa room C") { name = it }
        FormLabel("Type")
        ChipRow(
            options = listOf("Chair", "Room", "Staff"),
            selected = setOf(type),
            single = true,
            onToggle = { type = it },
        )
        FormLabel("Calendar colour")
        ChipRow(
            options = listOf("Lime", "Teal", "Cream"),
            selected = setOf(colour),
            single = true,
            onToggle = { colour = it },
        )
        FormUnavailableHint("Calendar colour stored as master code until a colour column exists")
        FormField("Display order", displayOrder, "5", KeyboardType.Number) { displayOrder = it }
        FormLabel("Offers categories")
        ChipRow(
            options = RoomOfferCategories,
            selected = categories,
            single = false,
            onToggle = { chip ->
                categories = if (chip in categories) categories - chip else categories + chip
            },
        )
        FormField("Assigned staff (optional)", assignedStaff, "Priya Nair") { assignedStaff = it }
        FormUnavailableHint("Assigned staff · not on master API yet")
        FormLabel("Status")
        ChipRow(
            options = listOf("Active", "Paused"),
            selected = setOf(status),
            single = true,
            onToggle = { status = it },
        )
        localError?.let {
            Text(it, style = ServicesTeal.rowCaption(ServicesTeal.negative), maxLines = 2)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ServiceOfferingMasterForm(
    model: OperatingProjectionDto,
    momentId: String?,
    existing: OperatingMasterDto?,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    onError: (String) -> Unit,
) {
    val companyId = rememberCompanyId(momentId)
    val repo = remember { BusinessSliceRepository() }
    val scope = rememberCoroutineScope()
    val brand = model.title?.takeIf { it.isNotBlank() } ?: "BrightCare"
    var createMode by remember(existing) { mutableStateOf(existing == null) }
    var working by remember(existing?.id) { mutableStateOf(existing) }

    var name by remember(working?.id) { mutableStateOf(working?.name.orEmpty()) }
    var category by remember(working?.id) {
        mutableStateOf(working?.dimension?.takeIf { it in ServiceCategories } ?: "Hair")
    }
    var duration by remember(working?.id) {
        mutableStateOf(
            working?.unit?.takeIf { it.contains("min", ignoreCase = true) } ?: "45 min",
        )
    }
    var price by remember(working?.id) {
        mutableStateOf(working?.sellPrice?.trimStart('₹')?.trim().orEmpty().ifBlank { "899" })
    }
    var seniorPrice by remember(working?.id) {
        mutableStateOf(working?.mrp?.trimStart('₹')?.trim().orEmpty().ifBlank { "1199" })
    }
    var gst by remember(working?.id) {
        mutableStateOf(
            when (working?.gstRate?.trim()) {
                "5", "5%" -> "5%"
                "0", "Exempt" -> "Exempt"
                else -> "18%"
            },
        )
    }
    var sac by remember(working?.id) {
        mutableStateOf(working?.hsn.orEmpty().ifBlank { "999721 · Hairdressing & beauty" })
    }
    var defaultRoom by remember { mutableStateOf("Chair 1") }
    var billedPer by remember(working?.id) {
        mutableStateOf(working?.code?.takeIf { it in listOf("session", "hour", "visit") } ?: "hour")
    }
    var buffer by remember { mutableStateOf("10 min · cleanup") }
    var supplies by remember { mutableStateOf("Shampoo, serum · optional") }
    var status by remember(working?.id) {
        mutableStateOf(
            if (working?.status.equals("INACTIVE", ignoreCase = true)) "Inactive" else "Active",
        )
    }
    var saving by remember { mutableStateOf(false) }
    var showToast by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }

    val rooms = remember(model.masters) {
        model.masters.orEmpty()
            .filter { it.kind.uppercase() in setOf("LOCATION", "ROOM", "CHAIR", "CHANNEL") }
            .map { it.name }
            .ifEmpty { listOf("Chair 1", "Chair 2", "Spa room A", "Room B") }
    }

    FormScaffold(
        title = if (createMode) "Create service offering" else "Edit service offering",
        subtitle = "Service master · SAC, GST, duration + price before it appears in Booking",
        onClose = onDismiss,
        why = "Create Service is a master write. Booking and Charge only pick saved services; GST posts on Finalize Charge, not on save.",
        cta = if (saving) "Saving…" else "Save service",
        caption = "Master saved · available in browser on next open",
        showToast = showToast,
        ctaEnabled = !saving && name.isNotBlank(),
        onCta = cta@ {
            val id = companyId
            if (id.isNullOrBlank()) {
                localError = "Company unavailable"
                onError("Company unavailable")
                return@cta
            }
            scope.launch {
                saving = true
                localError = null
                val gstRate = when (gst) {
                    "5%" -> "5"
                    "Exempt" -> "0"
                    else -> "18"
                }
                val sacCode = sac.substringBefore('·').trim().ifBlank { sac.trim() }
                val result = if (createMode || working == null) {
                    repo.createMaster(
                        id,
                        OperatingMasterBody(
                            kind = "SERVICE",
                            name = name.trim(),
                            code = billedPer,
                            momentId = momentId,
                            unit = duration,
                            dimension = category,
                            sellPrice = price.filter { it.isDigit() || it == '.' },
                            mrp = seniorPrice.filter { it.isDigit() || it == '.' }.ifBlank { null },
                            gstApplicable = if (gst == "Exempt") "NO" else "YES",
                            gstRate = gstRate,
                            hsn = sacCode.ifBlank { null },
                        ),
                    )
                } else {
                    repo.updateMaster(
                        id,
                        working!!.id,
                        OperatingMasterUpdateBody(
                            kind = "SERVICE",
                            name = name.trim(),
                            code = billedPer,
                            unit = duration,
                            dimension = category,
                            sellPrice = price.filter { it.isDigit() || it == '.' },
                            mrp = seniorPrice.filter { it.isDigit() || it == '.' }.ifBlank { null },
                            gstApplicable = if (gst == "Exempt") "NO" else "YES",
                            gstRate = gstRate,
                            hsn = sacCode.ifBlank { null },
                            status = if (status == "Inactive") "INACTIVE" else "ACTIVE",
                            expectedVersion = working!!.version,
                        ),
                    )
                }
                result.fold(
                    onSuccess = {
                        showToast = true
                        onSaved()
                    },
                    onFailure = {
                        localError = it.message
                        onError(it.message ?: "Could not save service")
                        saving = false
                    },
                )
            }
        },
    ) {
        ModeChips(
            createMode = createMode,
            onCreate = {
                createMode = true
                working = null
                name = ""
            },
            onEdit = { createMode = false },
        )
        ContextChipsRow(
            listOf("$brand Desk", "Services master", "GST-aware"),
            selected = setOf("$brand Desk"),
        )
        FormField("Service name", name, "e.g. Haircut & style") { name = it }
        FormLabel("Category")
        ChipRow(
            options = ServiceCategories,
            selected = setOf(category),
            single = true,
            onToggle = { category = it },
        )
        FormLabel("Duration")
        ChipRow(
            options = listOf("45 min", "30 min", "60 min"),
            selected = setOf(duration),
            single = true,
            onToggle = { duration = it },
        )
        PriceCard(
            label = "Price",
            currency = "INR",
            value = price,
            caption = "Base price · staff level can override",
            onValueChange = { price = it.filter { ch -> ch.isDigit() || ch == '.' } },
        )
        FormField("Senior stylist price", seniorPrice, "₹1,199", KeyboardType.Decimal) {
            seniorPrice = it.filter { ch -> ch.isDigit() || ch == '.' }
        }
        GstBlock(
            label = "GST on service",
            selected = gst,
            note = "Rate + SAC live on the master. Output GST posts on Finalize Charge only — never on booking save.",
            onSelect = { gst = it },
        )
        FormField("SAC", sac, "999721 · Hairdressing & beauty") { sac = it }
        FormLabel("Default room")
        ChipRow(
            options = rooms.take(4),
            selected = setOf(defaultRoom),
            single = true,
            onToggle = { defaultRoom = it },
        )
        FormUnavailableHint("Default room · UI only until parentMasterId wiring")
        FormLabel("Billed per")
        ChipRow(
            options = listOf("session", "hour", "visit"),
            selected = setOf(billedPer),
            single = true,
            onToggle = { billedPer = it },
        )
        FormField("Buffer after", buffer, "10 min · cleanup") { buffer = it }
        FormUnavailableHint("Buffer after · not on master API yet")
        PickerRow("Consumes supplies", supplies) { /* picker deferred to Phase 3 browser */ }
        FormUnavailableHint("Consumes supplies · not on master API yet")
        ChipRow(
            options = listOf("Active", "Inactive"),
            selected = setOf(status),
            single = true,
            onToggle = { status = it },
        )
        localError?.let {
            Text(it, style = ServicesTeal.rowCaption(ServicesTeal.negative), maxLines = 2)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PackageMasterForm(
    model: OperatingProjectionDto,
    momentId: String?,
    existing: OperatingMasterDto?,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    onError: (String) -> Unit,
) {
    val companyId = rememberCompanyId(momentId)
    val repo = remember { BusinessSliceRepository() }
    val scope = rememberCoroutineScope()
    val brand = model.title?.takeIf { it.isNotBlank() } ?: "BrightCare"
    var createMode by remember(existing) { mutableStateOf(existing == null) }
    var working by remember(existing?.id) { mutableStateOf(existing) }

    var name by remember(working?.id) { mutableStateOf(working?.name.orEmpty()) }
    var category by remember(working?.id) {
        mutableStateOf(working?.dimension?.substringBefore(',')?.trim()?.takeIf { it in ServiceCategories } ?: "Hair")
    }
    var sessions by remember(working?.id) {
        mutableStateOf(working?.unit?.takeIf { it.contains("session", ignoreCase = true) } ?: "3 sessions")
    }
    var price by remember(working?.id) {
        mutableStateOf(working?.sellPrice?.trimStart('₹')?.trim().orEmpty().ifBlank { "7999" })
    }
    var sumServices by remember(working?.id) {
        mutableStateOf(working?.mrp?.trimStart('₹')?.trim().orEmpty().ifBlank { "9000" })
    }
    var gst by remember(working?.id) {
        mutableStateOf(
            when (working?.gstRate?.trim()) {
                "5", "5%" -> "5%"
                "0", "Exempt" -> "Exempt"
                else -> "18%"
            },
        )
    }
    var sac by remember(working?.id) {
        mutableStateOf(working?.hsn.orEmpty().ifBlank { "999721 · Hairdressing & beauty" })
    }
    var includes by remember(working?.id) {
        mutableStateOf(
            working?.dimension
                ?.substringAfter(',', missingDelimiterValue = "")
                ?.split(',')
                ?.map { it.trim() }
                ?.filter { it.isNotEmpty() && it !in ServiceCategories }
                ?.toSet()
                ?.ifEmpty { setOf("Keratin") }
                ?: setOf("Keratin"),
        )
    }
    var validity by remember(working?.id) {
        mutableStateOf(working?.barcode?.takeIf { it.contains("day", ignoreCase = true) || it.contains("year", ignoreCase = true) } ?: "180 days")
    }
    var shareable by remember { mutableStateOf("Owner only · not transferable") }
    var reminder by remember { mutableStateOf("Before each session · optional") }
    var status by remember(working?.id) {
        mutableStateOf(
            if (working?.status.equals("INACTIVE", ignoreCase = true)) "Inactive" else "Active",
        )
    }
    var saving by remember { mutableStateOf(false) }
    var showToast by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }

    FormScaffold(
        title = if (createMode) "Create package" else "Edit package",
        subtitle = "Package master · bundled services, sessions + validity before it appears in Booking",
        onClose = onDismiss,
        why = "Create Package is a master write. Sessions are redeemed per booking; revenue and GST post per Finalize Charge.",
        cta = if (saving) "Saving…" else "Save package",
        caption = "Master saved · available in browser on next open",
        showToast = showToast,
        ctaEnabled = !saving && name.isNotBlank(),
        onCta = cta@ {
            val id = companyId
            if (id.isNullOrBlank()) {
                localError = "Company unavailable"
                onError("Company unavailable")
                return@cta
            }
            scope.launch {
                saving = true
                localError = null
                val gstRate = when (gst) {
                    "5%" -> "5"
                    "Exempt" -> "0"
                    else -> "18"
                }
                val sacCode = sac.substringBefore('·').trim().ifBlank { sac.trim() }
                val dimension = buildList {
                    add(category)
                    addAll(includes)
                }.joinToString(", ")
                val result = if (createMode || working == null) {
                    repo.createMaster(
                        id,
                        OperatingMasterBody(
                            kind = "PACKAGE",
                            name = name.trim(),
                            code = sessions,
                            momentId = momentId,
                            unit = sessions,
                            dimension = dimension,
                            sellPrice = price.filter { it.isDigit() || it == '.' },
                            mrp = sumServices.filter { it.isDigit() || it == '.' }.ifBlank { null },
                            gstApplicable = if (gst == "Exempt") "NO" else "YES",
                            gstRate = gstRate,
                            hsn = sacCode.ifBlank { null },
                            barcode = validity,
                        ),
                    )
                } else {
                    repo.updateMaster(
                        id,
                        working!!.id,
                        OperatingMasterUpdateBody(
                            kind = "PACKAGE",
                            name = name.trim(),
                            // UpdateBody has no barcode — keep validity in code alongside sessions.
                            code = "$sessions · $validity",
                            unit = sessions,
                            dimension = dimension,
                            sellPrice = price.filter { it.isDigit() || it == '.' },
                            mrp = sumServices.filter { it.isDigit() || it == '.' }.ifBlank { null },
                            gstApplicable = if (gst == "Exempt") "NO" else "YES",
                            gstRate = gstRate,
                            hsn = sacCode.ifBlank { null },
                            status = if (status == "Inactive") "INACTIVE" else "ACTIVE",
                            expectedVersion = working!!.version,
                        ),
                    )
                }
                result.fold(
                    onSuccess = {
                        showToast = true
                        onSaved()
                    },
                    onFailure = {
                        localError = it.message
                        onError(it.message ?: "Could not save package")
                        saving = false
                    },
                )
            }
        },
    ) {
        ModeChips(
            createMode = createMode,
            onCreate = {
                createMode = true
                working = null
                name = ""
            },
            onEdit = { createMode = false },
        )
        ContextChipsRow(
            listOf("$brand Desk", "Services master", "GST-aware"),
            selected = setOf("$brand Desk"),
        )
        FormField("Package name", name, "e.g. Keratin package · 3 sessions") { name = it }
        FormLabel("Category")
        ChipRow(
            options = ServiceCategories,
            selected = setOf(category),
            single = true,
            onToggle = { category = it },
        )
        FormLabel("Sessions")
        ChipRow(
            options = listOf("3 sessions", "1 session", "6 sessions"),
            selected = setOf(sessions),
            single = true,
            onToggle = { sessions = it },
        )
        PriceCard(
            label = "Package price",
            currency = "INR",
            value = price,
            caption = "Package price ≤ sum of services · deposit stays unallocated",
            onValueChange = { price = it.filter { ch -> ch.isDigit() || ch == '.' } },
        )
        FormField("Sum of services", sumServices, "₹9,000", KeyboardType.Decimal) {
            sumServices = it.filter { ch -> ch.isDigit() || ch == '.' }
        }
        GstBlock(
            label = "GST on package",
            selected = gst,
            note = "GST posts per session on Finalize Charge. Advance/deposit stays a liability until allocated.",
            onSelect = { gst = it },
        )
        FormField("SAC", sac, "999721 · Hairdressing & beauty") { sac = it }
        FormLabel("Includes")
        ChipRow(
            options = PackageIncludes,
            selected = includes,
            single = false,
            onToggle = { chip ->
                includes = if (chip in includes) includes - chip else includes + chip
            },
        )
        FormLabel("Validity")
        ChipRow(
            options = listOf("90 days", "180 days", "1 year"),
            selected = setOf(validity),
            single = true,
            onToggle = { validity = it },
        )
        FormUnavailableHint("Validity stored in barcode until a dedicated column exists")
        FormField("Shareable", shareable, "Owner only · not transferable") { shareable = it }
        FormUnavailableHint("Shareable · not on master API yet")
        PickerRow("Auto-reminder", reminder) { }
        FormUnavailableHint("Auto-reminder · not on master API yet")
        ChipRow(
            options = listOf("Active", "Inactive"),
            selected = setOf(status),
            single = true,
            onToggle = { status = it },
        )
        localError?.let {
            Text(it, style = ServicesTeal.rowCaption(ServicesTeal.negative), maxLines = 2)
        }
    }
}

/* ─── Shared chrome ──────────────────────────────────────────────────────── */

@Composable
private fun rememberCompanyId(momentId: String?): String? {
    var companyId by remember(momentId) { mutableStateOf<String?>(null) }
    LaunchedEffect(momentId) {
        if (momentId.isNullOrBlank()) return@LaunchedEffect
        runCatching {
            ApiClient.apiService.listBusinessMoments(limit = 50).data.items
                .firstOrNull { it.momentId == momentId }?.companyId
        }.onSuccess { companyId = it }.onFailure { companyId = null }
    }
    return companyId
}

@Composable
private fun FormScaffold(
    title: String,
    subtitle: String,
    onClose: () -> Unit,
    why: String,
    cta: String,
    caption: String,
    showToast: Boolean,
    ctaEnabled: Boolean,
    onCta: () -> Unit,
    content: @Composable () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        TealSheetGrabber()
        TealSheetHeader(title = title, subtitle = subtitle, onClose = onClose)
        content()
        WhyNote(why, tinted = true)
        TealGradientCta(
            label = cta,
            caption = caption,
            onClick = onCta,
            enabled = ctaEnabled,
        )
        if (showToast) TealSavedToast()
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun ModeChips(
    createMode: Boolean,
    onCreate: () -> Unit,
    onEdit: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        FormLabel("mode")
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            TealModeChip(text = "Create", selected = createMode, limeSelected = true, onClick = onCreate)
            TealModeChip(text = "Edit existing", selected = !createMode, limeSelected = true, onClick = onEdit)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ContextChipsRow(labels: List<String>, selected: Set<String>) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        labels.forEach { label ->
            TealModeChip(text = label, selected = label in selected)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipRow(
    options: List<String>,
    selected: Set<String>,
    single: Boolean,
    onToggle: (String) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        options.forEach { option ->
            TealModeChip(
                text = option,
                selected = option in selected,
                limeSelected = true,
                onClick = {
                    if (single) onToggle(option) else onToggle(option)
                },
            )
        }
    }
}

@Composable
private fun FormLabel(text: String) {
    Text(
        text,
        style = ServicesTeal.metricLabel().copy(color = ServicesTeal.grab, fontSize = 12.sp),
        maxLines = 1,
    )
}

@Composable
private fun FormUnavailableHint(text: String) {
    Text(
        text,
        style = ServicesTeal.rowCaption(ServicesTeal.grab).copy(fontSize = 10.sp),
        maxLines = 1,
    )
}

@Composable
private fun FormField(
    label: String,
    value: String,
    placeholder: String,
    keyboard: KeyboardType = KeyboardType.Text,
    hint: String? = null,
    onValueChange: (String) -> Unit,
) {
    Column(
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        FormLabel(label)
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(ServicesTeal.field)
                .border(1.dp, ServicesTeal.tealDeep, RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = keyboard),
                    textStyle = TextStyle(
                        color = ServicesTeal.textPrimary,
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                    ),
                    cursorBrush = SolidColor(ServicesTeal.lime),
                    decorationBox = { inner ->
                        if (value.isEmpty()) {
                            Text(
                                placeholder,
                                color = ServicesTeal.grab,
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        inner()
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                hint?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        it,
                        style = ServicesTeal.rowCaption(ServicesTeal.grab).copy(fontSize = 11.sp),
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun PriceCard(
    label: String,
    currency: String,
    value: String,
    caption: String,
    onValueChange: (String) -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 10.dp,
                shape = shape,
                clip = false,
                ambientColor = ServicesTeal.lime.copy(alpha = 0.4f),
                spotColor = ServicesTeal.lime.copy(alpha = 0.4f),
            )
            .clip(shape)
            .background(ServicesTeal.chip)
            .border(1.5.dp, ServicesTeal.lime, shape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = ServicesTeal.rowCaption(ServicesTeal.grab).copy(fontSize = 12.sp))
            Spacer(Modifier.weight(1f))
            Text(
                currency,
                style = ServicesTeal.eyebrow().copy(
                    color = ServicesTeal.whyBody,
                    fontSize = 11.sp,
                    letterSpacing = 0.sp,
                ),
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            textStyle = TextStyle(
                color = ServicesTeal.textPrimary,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Bold,
                fontSize = 40.sp,
            ),
            cursorBrush = SolidColor(ServicesTeal.lime),
            decorationBox = { inner ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "₹",
                        color = ServicesTeal.textPrimary,
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 40.sp,
                    )
                    if (value.isEmpty()) {
                        Text(
                            "0",
                            color = ServicesTeal.grab,
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 40.sp,
                        )
                    }
                    inner()
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
        Text(caption, style = ServicesTeal.rowCaption(ServicesTeal.grab).copy(fontSize = 11.sp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GstBlock(
    label: String,
    selected: String,
    note: String,
    onSelect: (String) -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(ServicesTeal.chip)
            .border(1.dp, ServicesTeal.whyBody.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        FormLabel(label)
        ChipRow(
            options = listOf("18%", "5%", "Exempt"),
            selected = setOf(selected),
            single = true,
            onToggle = onSelect,
        )
        Text(note, style = ServicesTeal.rowCaption(ServicesTeal.grab).copy(fontSize = 11.sp))
    }
}

@Composable
private fun PickerRow(label: String, value: String, onClick: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        FormLabel(label)
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(ServicesTeal.chip)
                .border(1.dp, ServicesTeal.teal, RoundedCornerShape(12.dp))
                .clickable(onClick = onClick)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                value,
                style = ServicesTeal.rowTitle().copy(fontWeight = FontWeight.Medium, fontSize = 14.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text("›", color = ServicesTeal.grab, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

private fun gstinHint(gstin: String): String? {
    val text = gstin.trim().uppercase()
    if (text.isEmpty()) return null
    val state = IndiaStateCatalog.resolve(text.take(2))
    val parts = buildList {
        add("${text.length.coerceAtMost(15)} chars")
        if (state != null) add("state ${state.code} ${state.name}")
        if (text.length == 15) add("verified")
    }
    return parts.joinToString(" · ")
}
