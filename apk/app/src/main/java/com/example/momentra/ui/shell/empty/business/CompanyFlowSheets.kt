package com.example.momentra.ui.shell.empty.business

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.data.api.ApiClient
import com.example.momentra.data.api.CompanyMemberDto
import com.example.momentra.data.api.CreateCompanyBody
import com.example.momentra.domain.CompanySummary
import com.example.momentra.ui.shell.business.shared.BusinessAudience
import com.example.momentra.ui.shell.business.shared.CompanyModules
import java.util.UUID
import kotlinx.coroutines.launch

private val SheetBg = Color(0xFF161B26)
private val FieldBg = Color(0xFF0C0F15)
private val Accent = Color(0xFF818CF8)
private val TextPrimary = Color(0xFFF1F5F9)
private val TextSecondary = Color(0xFFCBD5E1)
private val TextMuted = Color(0xFF64748B)
private val CardStroke = Color.White.copy(alpha = 0.10f)

private val IndustryOptions = listOf(
    "Technology & Software",
    "E-commerce",
    "Retail",
    "Services",
    "Manufacturing",
    "Other",
)

private val SizeOptions = listOf("1-10", "11-50", "51-200", "201-500", "500+")

private val EntityTypeOptions = listOf("Pvt Ltd", "LLP", "Partnership", "Sole Prop")

private val CurrencyOptions = listOf(
    "₹ INR — Indian Rupee",
    "$ USD — US Dollar",
    "€ EUR — Euro",
    "£ GBP — British Pound",
    "د.إ AED — UAE Dirham",
    "S$ SGD — Singapore Dollar",
)

private data class ModuleToggleDef(val label: String, val key: String, val smallShopHidden: Boolean = false)

private val ModuleToggleDefs = listOf(
    ModuleToggleDef("Team & Work", "teamOps"),
    ModuleToggleDef("Daily Business", "dailyBusiness"),
    ModuleToggleDef("Money & Cash Flow", "money"),
    ModuleToggleDef("Projects & Tasks", "projects", smallShopHidden = true),
    ModuleToggleDef("Events & Plans", "events", smallShopHidden = true),
    ModuleToggleDef("Suppliers & Vendors", "vendors"),
)

private val AlertToggleDefs = listOf(
    "Purchase & Expense Alerts" to "purchaseExpense",
    "Approval Requests" to "approvals",
    "Issue / Risk Alerts" to "issues",
    "Payment Reminders" to "paymentReminders",
    "Important Business Updates" to "importantUpdates",
)

private enum class CompanySheetPage { Switch, Settings, Create }

@Composable
fun CompanyFlowSheet(
    companies: List<CompanySummary>,
    selectedCompanyId: String?,
    startOnCreate: Boolean,
    startOnSettings: Boolean = false,
    onDismiss: () -> Unit,
    onSelect: (CompanySummary) -> Unit,
    onCreated: (CompanySummary) -> Unit,
    onOpenLocations: () -> Unit = {},
) {
    var page by remember {
        mutableStateOf(
            when {
                startOnCreate -> CompanySheetPage.Create
                startOnSettings -> CompanySheetPage.Settings
                else -> CompanySheetPage.Switch
            },
        )
    }
    var settingsCompany by remember {
        mutableStateOf(companies.firstOrNull { it.companyId == selectedCompanyId })
    }

    BusinessSetupBottomSheet(onDismiss = onDismiss) {
        if (startOnCreate || page == CompanySheetPage.Create) {
            CompanySetupContent(
                onClose = { if (startOnCreate) onDismiss() else page = CompanySheetPage.Switch },
                onActivated = {
                    onCreated(it)
                    onDismiss()
                },
            )
            return@BusinessSetupBottomSheet
        }
        when (page) {
            CompanySheetPage.Switch -> CompanySwitchPage(
                companies = companies,
                selectedCompanyId = selectedCompanyId,
                onClose = onDismiss,
                onSelect = {
                    onSelect(it)
                    onDismiss()
                },
                onSettings = {
                    settingsCompany = it
                    page = CompanySheetPage.Settings
                },
                onAdd = { page = CompanySheetPage.Create },
            )
            CompanySheetPage.Settings -> CompanySettingsPage(
                companyId = settingsCompany?.companyId.orEmpty(),
                companyName = settingsCompany?.displayName.orEmpty(),
                onClose = {
                    if (startOnCreate) onDismiss() else page = CompanySheetPage.Switch
                },
                onOpenLocations = onOpenLocations,
            )
            CompanySheetPage.Create -> Unit
        }
    }
}

@Composable
private fun SheetHeader(
    title: String,
    subtitle: String? = null,
    onClose: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
            if (!subtitle.isNullOrBlank()) {
                Text(subtitle, color = TextSecondary, fontSize = 13.sp)
            }
        }
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .clickable(role = Role.Button, onClick = onClose)
                .semantics { contentDescription = "Close" },
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.Close, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun CompanySwitchPage(
    companies: List<CompanySummary>,
    selectedCompanyId: String?,
    onClose: () -> Unit,
    onSelect: (CompanySummary) -> Unit,
    onSettings: (CompanySummary) -> Unit,
    onAdd: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        SheetHeader(title = "Switch Company", onClose = onClose)
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            companies.forEach { company ->
                val selected = company.companyId == selectedCompanyId
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (selected) Accent.copy(alpha = 0.12f) else Color.Transparent)
                        .clickable { onSelect(company) }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    InitialsAvatar(company.displayName)
                    Text(
                        text = company.displayName,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 12.dp),
                    )
                    if (selected) {
                        Icon(Icons.Outlined.Check, contentDescription = "Selected", tint = Accent, modifier = Modifier.size(18.dp))
                    }
                    Box(
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .size(32.dp)
                            .clip(CircleShape)
                            .clickable(role = Role.Button) { onSettings(company) }
                            .semantics { contentDescription = "Company settings" },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Outlined.Settings, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
        Box(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth()
                .height(1.dp)
                .background(CardStroke),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onAdd)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .padding(bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .border(1.dp, Accent, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.Add, contentDescription = null, tint = Accent, modifier = Modifier.size(16.dp))
            }
            Text(
                text = "Add New Company",
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                modifier = Modifier.padding(start = 12.dp),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CompanySettingsPage(
    companyId: String,
    companyName: String,
    onClose: () -> Unit,
    onOpenLocations: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val headerName = companyName.ifBlank { "Company" }

    var loading by remember(companyId) { mutableStateOf(true) }
    var loadError by remember(companyId) { mutableStateOf<String?>(null) }
    var version by remember(companyId) { mutableStateOf<Int?>(null) }

    var displayName by remember(companyId) { mutableStateOf(companyName) }
    var legalName by remember(companyId) { mutableStateOf("") }
    var taxIdentifier by remember(companyId) { mutableStateOf("") }
    var companyType by remember(companyId) { mutableStateOf(EntityTypeOptions.first()) }
    var industry by remember(companyId) { mutableStateOf("") }
    var companySize by remember(companyId) { mutableStateOf(SizeOptions.first()) }
    var currency by remember(companyId) { mutableStateOf(CurrencyOptions.first()) }
    var financialYear by remember(companyId) { mutableStateOf("—") }
    var smallShop by remember(companyId) { mutableStateOf(false) }

    var moduleToggles by remember(companyId) {
        mutableStateOf(ModuleToggleDefs.associate { it.key to true })
    }
    var alertToggles by remember(companyId) {
        mutableStateOf(AlertToggleDefs.associate { it.second to true })
    }
    var memoryOn by remember(companyId) { mutableStateOf(true) }

    var members by remember(companyId) { mutableStateOf<List<CompanyMemberDto>>(emptyList()) }
    var defaultLocationName by remember(companyId) { mutableStateOf<String?>(null) }
    var showInviteSheet by remember { mutableStateOf(false) }

    var savingProfile by remember { mutableStateOf(false) }
    var profileSaveError by remember { mutableStateOf<String?>(null) }
    var profileSaved by remember { mutableStateOf(false) }
    var settingsPatchBusy by remember { mutableStateOf(false) }

    var entityTypeOpen by remember { mutableStateOf(false) }
    var industryOpen by remember { mutableStateOf(false) }
    var currencyOpen by remember { mutableStateOf(false) }

    fun applyCompanyPayload(data: Map<String, Any?>) {
        version = companyVersionFrom(data) ?: version
        displayName = data["displayName"]?.toString().orEmpty().ifBlank { displayName }
        legalName = data["legalName"]?.toString().orEmpty()
        taxIdentifier = data["taxIdentifier"]?.toString().orEmpty()
        data["companyType"]?.toString()?.takeIf { it.isNotBlank() }?.let { companyType = it }
        val profile = companyAnyMap(data["profileJson"])
        industry = profile["industry"]?.toString().orEmpty()
        profile["companySize"]?.toString()?.takeIf { it.isNotBlank() }?.let { companySize = it }
        profile["currency"]?.toString()?.takeIf { it.isNotBlank() }?.let { currency = it }
        financialYear = profile["financialYear"]?.toString()
            ?: profile["fyCycle"]?.toString()
            ?: "—"
        smallShop = BusinessAudience.isSmallShop(profile[BusinessAudience.PREF_KEY]?.toString())
        val settings = companyAnyMap(profile["settings"])
        val modules = companyAnyMap(settings["modules"])
        moduleToggles = ModuleToggleDefs.associate { def ->
            def.key to companyAnyBool(modules[def.key], default = true)
        }
        CompanyModules.saveModules(context, companyId, moduleToggles)
        val alertsMap = companyAnyMap(settings["alerts"])
        alertToggles = AlertToggleDefs.associate { (_, key) ->
            key to companyAnyBool(alertsMap[key], default = true)
        }
        memoryOn = companyAnyBool(settings["businessMemoryEnabled"], default = true)
    }

    LaunchedEffect(companyId) {
        if (companyId.isBlank()) {
            loading = false
            loadError = "No company selected"
            return@LaunchedEffect
        }
        loading = true
        loadError = null
        val companyResult = runCatching { ApiClient.apiService.getCompany(companyId).data }
        val membersResult = runCatching { ApiClient.apiService.listCompanyMembers(companyId).data.members }
        val locationsResult = runCatching { ApiClient.apiService.listLocations(companyId).data.items }
        companyResult.onSuccess { applyCompanyPayload(it) }
            .onFailure { loadError = it.message ?: "Could not load company" }
        members = membersResult.getOrElse { emptyList() }
        defaultLocationName = locationsResult.getOrNull()?.firstOrNull()?.name
        loading = false
    }

    fun patchSettings(profilePatch: Map<String, Any>, rollback: () -> Unit) {
        val v = version ?: return
        if (settingsPatchBusy) return
        settingsPatchBusy = true
        scope.launch {
            val result = runCatching {
                ApiClient.apiService.patchCompany(
                    companyId = companyId,
                    idempotencyKey = UUID.randomUUID().toString(),
                    body = mapOf(
                        "expectedVersion" to v,
                        "profileJson" to profilePatch,
                    ),
                ).data
            }
            settingsPatchBusy = false
            result.onSuccess { applyCompanyPayload(it) }
                .onFailure { rollback() }
        }
    }

    Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        SheetHeader(
            title = "Company Settings",
            subtitle = displayName.ifBlank { headerName },
            onClose = onClose,
        )
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (loading) {
                Text("Loading…", color = TextMuted, fontSize = 13.sp)
            }
            loadError?.let {
                Text(it, color = Color(0xFFF87171), fontSize = 12.sp)
            }
            SettingsCard(title = "Business Profile") {
                SettingsEditableField("Business Name", displayName) { displayName = it }
            }
            SettingsCard(title = "Business Details", note = "Optional formal business information.") {
                SettingsEditableField("Legal Business Name", legalName) { legalName = it }
                SettingsDropdownRow(
                    label = "Business Registration Type",
                    value = companyType,
                    expanded = entityTypeOpen,
                    onExpanded = { entityTypeOpen = it },
                    options = EntityTypeOptions,
                    onSelect = { companyType = it },
                )
                SettingsEditableField("GSTIN / Tax ID", taxIdentifier) { taxIdentifier = it }
                SettingsDropdownRow(
                    label = "Industry",
                    value = industry.ifBlank { "Select industry…" },
                    expanded = industryOpen,
                    onExpanded = { industryOpen = it },
                    options = IndustryOptions,
                    onSelect = { industry = it },
                )
                Text("COMPANY SIZE", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SizeOptions.forEach { option ->
                        val selected = option == companySize
                        Text(
                            text = option,
                            color = if (selected) TextPrimary else TextSecondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .clip(RoundedCornerShape(100.dp))
                                .background(if (selected) Accent.copy(alpha = 0.25f) else Color.Transparent)
                                .border(1.dp, if (selected) Accent else CardStroke, RoundedCornerShape(100.dp))
                                .clickable { companySize = option }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                        )
                    }
                }
                SettingsDropdownRow(
                    label = "Currency",
                    value = currency,
                    expanded = currencyOpen,
                    onExpanded = { currencyOpen = it },
                    options = CurrencyOptions,
                    onSelect = { currency = it },
                )
                profileSaveError?.let {
                    Text(it, color = Color(0xFFF87171), fontSize = 12.sp)
                }
                if (profileSaved) {
                    Text("Saved", color = CoGreen, fontSize = 12.sp)
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Accent.copy(alpha = if (savingProfile || companyId.isBlank()) 0.35f else 0.9f))
                        .clickable(enabled = !savingProfile && companyId.isNotBlank() && version != null) {
                            val v = version ?: return@clickable
                            savingProfile = true
                            profileSaveError = null
                            profileSaved = false
                            scope.launch {
                                val profileJson = buildMap<String, Any> {
                                    if (industry.isNotBlank()) put("industry", industry)
                                    put("companySize", companySize)
                                    put("currency", currency)
                                }
                                val body = buildMap<String, Any> {
                                    put("expectedVersion", v)
                                    put("displayName", displayName.trim())
                                    put("legalName", legalName.trim().ifBlank { displayName.trim() })
                                    put("taxIdentifier", taxIdentifier.trim())
                                    put("companyType", companyType)
                                    put("profileJson", profileJson)
                                }
                                runCatching {
                                    ApiClient.apiService.patchCompany(
                                        companyId = companyId,
                                        idempotencyKey = UUID.randomUUID().toString(),
                                        body = body,
                                    ).data
                                }.onSuccess {
                                    applyCompanyPayload(it)
                                    profileSaved = true
                                }.onFailure {
                                    profileSaveError = it.message ?: "Could not save"
                                }
                                savingProfile = false
                            }
                        }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (savingProfile) "Saving…" else "Save profile",
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                    )
                }
            }
            SettingsCard(title = "Business Accounts") {
                Text("Accounts · Coming soon", color = TextMuted.copy(alpha = 0.7f), fontSize = 12.sp)
            }
            SettingsCard(title = "Money Settings") {
                SettingsValueRow("Currency", currency.ifBlank { "—" })
                SettingsValueRow("Financial Year", financialYear)
                SettingsValueRow("Default Expense Account", "—", mutedValue = true)
                SettingsValueRow(
                    "Default Location",
                    defaultLocationName ?: "—",
                    mutedValue = defaultLocationName == null,
                )
            }
            SettingsCard(title = "Locations") {
                Text(
                    "Manage locations",
                    color = Accent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable(onClick = onOpenLocations),
                )
            }
            SettingsCard(title = "Team & Roles") {
                if (members.isEmpty()) {
                    Text("No team members yet", color = TextMuted, fontSize = 13.sp)
                } else {
                    members.forEach { member ->
                        SettingsValueRow(
                            member.displayName?.takeIf { it.isNotBlank() } ?: member.userId,
                            member.membershipType,
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Accent.copy(alpha = 0.9f))
                        .clickable(enabled = companyId.isNotBlank()) { showInviteSheet = true }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "Invite",
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                    )
                }
            }
            SettingsCard(title = "What do you use Momentra for?", note = "Turn on only the business moment types you want to use.") {
                ModuleToggleDefs.filter { !smallShop || !it.smallShopHidden }.forEach { def ->
                    val checked = moduleToggles[def.key] == true
                    SettingsToggleRow(def.label, checked) { on ->
                        val previous = moduleToggles
                        moduleToggles = previous + (def.key to on)
                        patchSettings(mapOf("settings" to mapOf("modules" to mapOf(def.key to on)))) {
                            moduleToggles = previous
                        }
                    }
                }
            }
            SettingsCard(title = "Notifications & Approvals") {
                AlertToggleDefs.forEach { (label, key) ->
                    val checked = alertToggles[key] == true
                    SettingsToggleRow(label, checked) { on ->
                        val previous = alertToggles
                        alertToggles = previous + (key to on)
                        patchSettings(mapOf("settings" to mapOf("alerts" to mapOf(key to on)))) {
                            alertToggles = previous
                        }
                    }
                }
            }
            SettingsCard(title = "Business Memory & Data") {
                SettingsToggleRow("Business Memory", memoryOn) { on ->
                    val previous = memoryOn
                    memoryOn = on
                    patchSettings(mapOf("settings" to mapOf("businessMemoryEnabled" to on))) {
                        memoryOn = previous
                    }
                }
                Text(
                    "Archive, export, and retention options coming later",
                    color = TextMuted.copy(alpha = 0.65f),
                    fontSize = 12.sp,
                )
            }
            SettingsCard(title = "Plan & Usage") {
                Text(
                    "Plan details · Coming soon",
                    color = TextMuted.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                )
            }
            SettingsCard(title = "Business Management") {
                listOf("Transfer Ownership", "Archive Business", "Deactivate Business").forEach { label ->
                    Text(
                        "$label · Coming soon",
                        color = TextMuted.copy(alpha = 0.65f),
                        fontWeight = FontWeight.Normal,
                        fontSize = 12.sp,
                    )
                }
            }
        }
    }

    CompanyInviteShareSheet(
        companyId = companyId,
        visible = showInviteSheet,
        onDismiss = { showInviteSheet = false },
        companyName = displayName.ifBlank { headerName },
    )
}

private val CoGreen = Color(0xFF10B981)

@Suppress("UNCHECKED_CAST")
private fun companyAnyMap(value: Any?): Map<String, Any?> = when (value) {
    is Map<*, *> -> value.entries.associate { (k, v) -> k.toString() to v }
    else -> emptyMap()
}

private fun companyAnyBool(value: Any?, default: Boolean): Boolean = when (value) {
    is Boolean -> value
    is Number -> value.toInt() != 0
    is String -> value.equals("true", ignoreCase = true)
    else -> default
}

private fun companyVersionFrom(data: Map<String, Any?>): Int? = when (val v = data["version"]) {
    is Number -> v.toInt()
    is String -> v.toIntOrNull()
    else -> null
}

@Composable
private fun SettingsCard(
    title: String,
    note: String? = null,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(title, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        if (!note.isNullOrBlank()) {
            Text(note, color = TextMuted, fontSize = 12.sp)
        }
        content()
    }
}

@Composable
private fun SettingsValueRow(label: String, value: String, mutedValue: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = TextSecondary, fontSize = 13.sp)
        Text(
            value,
            color = if (mutedValue) TextMuted else TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun SettingsEditableField(label: String, value: String, onChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, color = TextSecondary, fontSize = 13.sp)
        BasicTextField(
            value = value,
            onValueChange = onChange,
            textStyle = TextStyle(color = TextPrimary, fontSize = 14.sp),
            cursorBrush = SolidColor(Accent),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(FieldBg)
                .border(1.dp, CardStroke, RoundedCornerShape(10.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
        )
    }
}

@Composable
private fun SettingsDropdownRow(
    label: String,
    value: String,
    expanded: Boolean,
    onExpanded: (Boolean) -> Unit,
    options: List<String>,
    onSelect: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, color = TextSecondary, fontSize = 13.sp)
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(FieldBg)
                    .border(1.dp, CardStroke, RoundedCornerShape(10.dp))
                    .clickable { onExpanded(true) }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = value,
                    color = if (value.startsWith("Select")) TextMuted else TextPrimary,
                    fontSize = 14.sp,
                    modifier = Modifier.weight(1f),
                )
                Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = null, tint = TextMuted)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { onExpanded(false) }) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onSelect(option)
                            onExpanded(false)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = TextPrimary, fontSize = 14.sp, modifier = Modifier.weight(1f))
        androidx.compose.material3.Switch(checked = checked, onCheckedChange = onChange)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CompanyCreatePage(
    onCancel: () -> Unit,
    onCreated: (CompanySummary) -> Unit,
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var industry by remember { mutableStateOf("") }
    var industryOpen by remember { mutableStateOf(false) }
    var size by remember { mutableStateOf("1-10") }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SheetHeader(title = "Create Company", onClose = onCancel)
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .border(1.dp, Accent, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.Add, contentDescription = null, tint = Accent)
            }
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text("Company Logo", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text("Optional", color = TextMuted, fontSize = 12.sp)
            }
        }
        Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("COMPANY NAME", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            BasicTextField(
                value = name,
                onValueChange = { name = it },
                textStyle = TextStyle(color = TextPrimary, fontSize = 15.sp),
                cursorBrush = SolidColor(Accent),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(FieldBg)
                    .border(1.dp, CardStroke, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 14.dp),
                decorationBox = { inner ->
                    if (name.isEmpty()) Text("e.g. Acme Corp", color = TextMuted, fontSize = 15.sp)
                    inner()
                },
            )
        }
        Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("INDUSTRY", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Box {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(FieldBg)
                        .border(1.dp, CardStroke, RoundedCornerShape(12.dp))
                        .clickable { industryOpen = true }
                        .padding(horizontal = 12.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = industry.ifBlank { "Select industry..." },
                        color = if (industry.isBlank()) TextMuted else TextPrimary,
                        fontSize = 15.sp,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = null, tint = TextMuted)
                }
                DropdownMenu(expanded = industryOpen, onDismissRequest = { industryOpen = false }) {
                    IndustryOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                industry = option
                                industryOpen = false
                            },
                        )
                    }
                }
            }
        }
        Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("COMPANY SIZE", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SizeOptions.forEach { option ->
                    val selected = option == size
                    Text(
                        text = option,
                        color = if (selected) TextPrimary else TextSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .clip(RoundedCornerShape(100.dp))
                            .background(if (selected) Accent.copy(alpha = 0.25f) else Color.Transparent)
                            .border(1.dp, if (selected) Accent else CardStroke, RoundedCornerShape(100.dp))
                            .clickable { size = option }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                    )
                }
            }
        }
        error?.let {
            Text(it, color = Color(0xFFF87171), fontSize = 12.sp, modifier = Modifier.padding(horizontal = 16.dp))
        }
        Box(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Accent.copy(alpha = if (name.isBlank() || saving) 0.35f else 0.9f))
                .clickable(enabled = name.isNotBlank() && !saving) {
                    saving = true
                    error = null
                    scope.launch {
                        val trimmed = name.trim()
                        runCatching {
                            ApiClient.apiService.createCompany(
                                idempotencyKey = UUID.randomUUID().toString(),
                                body = CreateCompanyBody(
                                    displayName = trimmed,
                                    legalName = trimmed,
                                    timezone = java.util.TimeZone.getDefault().id,
                                    profileJson = buildMap {
                                        if (industry.isNotBlank()) put("industry", industry)
                                        put("companySize", size)
                                        put(BusinessAudience.PREF_KEY, BusinessAudience.SMALL_SHOP)
                                    },
                                ),
                            ).data
                        }.onSuccess { created ->
                            BusinessAudience.saveForCompany(
                                context,
                                created.companyId,
                                BusinessAudience.SMALL_SHOP,
                            )
                            onCreated(CompanySummary(created.companyId, created.displayName))
                        }.onFailure {
                            saving = false
                            error = it.message ?: "Could not create company"
                        }
                    }
                }
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (saving) "Creating…" else "Create Company",
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
            )
        }
        Box(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .clickable(onClick = onCancel)
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("Cancel", color = TextSecondary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        }
    }
}

@Composable
private fun InitialsAvatar(name: String) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Accent.copy(alpha = 0.2f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(companyInitials(name), color = Accent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

internal fun companyInitials(name: String): String {
    val parts = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    val letters = when {
        parts.isEmpty() -> "?"
        parts.size == 1 -> parts[0].take(2)
        else -> parts[0].take(1) + parts[1].take(1)
    }
    return letters.uppercase()
}
