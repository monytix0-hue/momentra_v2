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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.data.api.ApiClient
import com.example.momentra.data.api.CreateCompanyBody
import com.example.momentra.domain.CompanySummary
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

@Composable
private fun CompanySettingsPage(
    companyName: String,
    onClose: () -> Unit,
    onOpenLocations: () -> Unit,
) {
    val name = companyName.ifBlank { "Company" }
    var momentUses by remember {
        mutableStateOf(
            listOf(
                "Team & Work" to true,
                "Daily Business" to true,
                "Money & Cash Flow" to true,
                "Projects & Tasks" to false,
                "Events & Plans" to false,
                "Suppliers & Vendors" to true,
            ),
        )
    }
    var alerts by remember {
        mutableStateOf(
            listOf(
                "Purchase & Expense Alerts" to true,
                "Approval Requests" to true,
                "Issue / Risk Alerts" to true,
                "Payment Reminders" to true,
                "Important Business Updates" to true,
            ),
        )
    }
    var memoryOn by remember { mutableStateOf(true) }

    Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        SheetHeader(title = "Company Settings", subtitle = name, onClose = onClose)
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SettingsCard(title = "Business Identity") {
                SettingsValueRow("Business", name)
            }
            SettingsCard(title = "Business Profile") {
                SettingsValueRow("Business Name", name)
            }
            SettingsCard(title = "Business Details", note = "Optional formal business information.") {
                SettingsValueRow("Legal Business Name", "—")
                SettingsValueRow("Business Registration Type", "—")
                SettingsValueRow("GSTIN / Tax ID", "—")
                SettingsValueRow("Registration Number", "Optional")
            }
            SettingsCard(title = "Business Accounts") {
                Text("No accounts yet", color = TextMuted, fontSize = 13.sp)
            }
            SettingsCard(title = "Money Settings") {
                SettingsValueRow("Currency", "—")
                SettingsValueRow("Financial Year", "—")
                SettingsValueRow("Default Expense Account", "—")
                SettingsValueRow("Default Location", "—")
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
                Text("No team members yet", color = TextMuted, fontSize = 13.sp)
            }
            SettingsCard(title = "What do you use Momentra for?", note = "Turn on only the business moment types you want to use.") {
                momentUses.forEachIndexed { index, row ->
                    SettingsToggleRow(row.first, row.second) { on ->
                        momentUses = momentUses.mapIndexed { i, item -> if (i == index) item.first to on else item }
                    }
                }
            }
            SettingsCard(title = "Notifications & Approvals") {
                alerts.forEachIndexed { index, row ->
                    SettingsToggleRow(row.first, row.second) { on ->
                        alerts = alerts.mapIndexed { i, item -> if (i == index) item.first to on else item }
                    }
                }
            }
            SettingsCard(title = "Business Memory & Data") {
                SettingsToggleRow("Business Memory", memoryOn) { memoryOn = it }
                SettingsValueRow("Archived Moments", "—")
                SettingsValueRow("Export Business Data", "—")
                SettingsValueRow("Data Retention", "—")
            }
            SettingsCard(title = "Plan & Usage") {
                Text("Plan details aren't available yet", color = TextMuted, fontSize = 13.sp)
            }
            SettingsCard(title = "Business Management", note = "These actions affect your business setup and access.") {
                listOf("Transfer Ownership", "Archive Business", "Deactivate Business").forEach { label ->
                    Text(label, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
            }
        }
    }
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
private fun SettingsValueRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = TextSecondary, fontSize = 13.sp)
        Text(value, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
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
                                    },
                                ),
                            ).data
                        }.onSuccess { created ->
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
