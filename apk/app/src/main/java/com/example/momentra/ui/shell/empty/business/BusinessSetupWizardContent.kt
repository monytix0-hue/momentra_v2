package com.example.momentra.ui.shell.empty.business

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.analytics.MomentraAnalytics
import com.example.momentra.domain.CreateMomentOutcome
import com.example.momentra.ui.create.MomentCreateViewModel
import com.example.momentra.ui.shell.business.shared.OperatingMomentCatalog
import com.example.momentra.ui.shell.business.shared.OperatingSubtype
import com.example.momentra.ui.shell.maestro.MaestroIds

private val Card = Color(0xFF161B26)
private val Border = Color(0xFF1E293B)
private val Muted = Color(0xFF94A3B8)
private val TextPrimary = Color(0xFFE5E0EE)

@Composable
fun BusinessSetupWizardContent(
    kind: BusinessSetupKind,
    companyId: String,
    createViewModel: MomentCreateViewModel,
    submitting: Boolean,
    error: String?,
    onClose: () -> Unit,
    onCreated: (CreateMomentOutcome) -> Unit,
    modifier: Modifier = Modifier,
    editingMomentId: String? = null,
    initialTitle: String? = null,
    editingMomentStatus: String? = null,
    ownerName: String = "",
) {
    val subtypes = remember(kind) { OperatingMomentCatalog.subtypes(kind) }
    var step by remember(kind) { mutableIntStateOf(0) }
    var subtype by remember(kind) { mutableStateOf(subtypes.first()) }
    var identity by remember(kind) { mutableStateOf(initialTitle.orEmpty()) }
    var party by remember(kind) { mutableStateOf("") }
    var description by remember(kind) { mutableStateOf("") }
    var channels by remember(kind) { mutableStateOf("") }
    var budget by remember(kind) { mutableStateOf("") }
    var target by remember(kind) { mutableStateOf("") }
    var metric by remember(kind) { mutableStateOf(subtypes.first().targetMetrics.first()) }
    var people by remember(kind) { mutableStateOf("") }
    var role by remember(kind) { mutableStateOf("OPERATIONS_LEAD") }
    var counterparties by remember(kind) { mutableStateOf("") }
    var localError by remember(kind) { mutableStateOf<String?>(null) }
    var companySeed by remember(kind) { mutableStateOf("") }
    var gstSeed by remember(kind) { mutableStateOf("") }

    LaunchedEffect(editingMomentId, companyId) {
        val momentId = editingMomentId ?: return@LaunchedEffect
        val prefill = createViewModel.getDomainSetupPrefill(momentId)
        val existingCompany = companyId.takeIf { it.isNotBlank() } ?: prefill?.companyId
        if (!existingCompany.isNullOrBlank()) {
            val detail = runCatching { com.example.momentra.data.api.ApiClient.apiService.getCompany(existingCompany).data }.getOrNull()
            companySeed = detail?.get("displayName")?.toString().orEmpty().ifBlank { prefill?.title ?: initialTitle.orEmpty() }
            gstSeed = detail?.get("taxIdentifier")?.toString().orEmpty()
        } else {
            companySeed = prefill?.title ?: initialTitle.orEmpty()
        }
    }

    DisposableEffect(kind) {
        MomentraAnalytics.get().onScreenEnter(kind.analyticsScreen)
        onDispose { MomentraAnalytics.get().onScreenExit(kind.analyticsScreen) }
    }

    val dedicatedSetup = (kind == BusinessSetupKind.RETAIL_CONSUMER && subtype.code in setOf("RETAIL_STORE", "RESTAURANT_FOOD", "CONSUMER_SERVICE")) ||
        (kind == BusinessSetupKind.TRADING_DISTRIBUTION && subtype.code in setOf("TRADING_WHOLESALE", "DISTRIBUTION_DEALERSHIP")) ||
        (kind == BusinessSetupKind.MARKETING_CAMPAIGNS && subtype.code in setOf("CLIENT_CAMPAIGN", "OWN_BUSINESS_CAMPAIGN")) ||
        (kind == BusinessSetupKind.MANUFACTURING_PRODUCTION && subtype.code in setOf("STANDARD_PRODUCTION", "MADE_TO_ORDER"))
    if (dedicatedSetup) {
        RetailStoreSetup(
            subtypes = subtypes,
            subtype = subtype,
            onSubtype = {
                subtype = it
                metric = it.targetMetrics.first()
            },
            ownerName = ownerName,
            submitting = submitting,
            error = error,
            onClose = onClose,
            initialName = initialTitle.orEmpty(),
            initialCompanyName = companySeed,
            initialGstin = gstSeed,
            companyId = companyId,
            onActivate = { preferences, title ->
                createViewModel.submitBusinessSetup(
                    kind = kind,
                    companyId = companyId.takeIf { it.isNotBlank() },
                    preferences = preferences,
                    title = title,
                    subtype = subtype.code,
                    editingMomentId = editingMomentId,
                    editingMomentStatus = editingMomentStatus,
                    status = "ACTIVE",
                    companyName = preferences["companyName"]?.toString(),
                    taxIdentifier = preferences["gstin"]?.toString(),
                    onSuccess = onCreated,
                )
            },
            modifier = modifier,
        )
        return
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text("Close", color = Muted, modifier = Modifier.clickable(onClick = onClose))
            Text("Step ${step + 1} of 4", color = Muted, fontSize = 12.sp)
        }
        Text(kind.title, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 22.sp)
        Text(kind.body, color = Muted, fontSize = 13.sp)
        when (step) {
            0 -> IdentityStep(
                subtypes = subtypes,
                subtype = subtype,
                onSubtype = {
                    subtype = it
                    metric = it.targetMetrics.first()
                },
                identity = identity,
                onIdentity = { identity = it },
                party = party,
                onParty = { party = it },
                description = description,
                onDescription = { description = it },
            )
            1 -> PeopleStep(people, { people = it }, role, { role = it })
            2 -> FieldBlock("Counterparties", "Optional. One name per line.", counterparties, { counterparties = it })
            else -> PlanStep(subtype, channels, { channels = it }, budget, { budget = formatIndianAmount(it) }, target, { target = formatIndianAmount(it) }, metric, { metric = it })
        }
        val shownError = localError ?: error
        if (!shownError.isNullOrBlank()) {
            Text(shownError, color = Color(0xFFF87171), fontSize = 13.sp)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (step > 0) {
                StepButton("Back", Color(0xFF1E293B)) { step -= 1 }
            }
            StepButton(
                if (step < 3) "Continue" else if (submitting) "Activating…" else "Activate",
                kind.activateColor,
                MaestroIds.BUSINESS_SETUP_SUBMIT,
            ) {
                if (submitting) return@StepButton
                val problem = validateStep(step, subtype, identity, party, channels, budget, target)
                if (problem != null) {
                    localError = problem
                    return@StepButton
                }
                localError = null
                if (step < 3) {
                    step += 1
                } else {
                    createViewModel.submitBusinessSetup(
                        kind = kind,
                        companyId = companyId?.takeIf { it.isNotBlank() },
                        preferences = buildPrefs(
                            subtype, identity, party, description, people, role, counterparties, channels, budget, target, metric,
                        ),
                        title = identity.trim(),
                        subtype = subtype.code,
                        editingMomentId = editingMomentId,
                        editingMomentStatus = editingMomentStatus,
                        status = "ACTIVE",
                        onSuccess = onCreated,
                    )
                }
            }
        }
    }
}

private fun validateStep(
    step: Int,
    subtype: OperatingSubtype,
    identity: String,
    party: String,
    channels: String,
    budget: String,
    target: String,
): String? = when (step) {
    0 -> when {
        identity.isBlank() -> "Moment name is required."
        party.isBlank() -> "${subtype.partyLabel} name is required."
        else -> null
    }
    3 -> when {
        subtype.requiresChannel && channels.isBlank() -> "Select at least one channel."
        plainAmount(budget).toDoubleOrNull()?.let { it > 0 } != true -> "Monthly budget must be a positive amount."
        plainAmount(target).toDoubleOrNull()?.let { it > 0 } != true -> "Monthly target must be a positive amount."
        else -> null
    }
    else -> null
}

private fun buildPrefs(
    subtype: OperatingSubtype,
    identity: String,
    party: String,
    description: String,
    people: String,
    role: String,
    counterparties: String,
    channels: String,
    budget: String,
    target: String,
    metric: String,
): Map<String, Any> {
    val participants = people.lines().map { it.trim() }.filter { it.isNotEmpty() }.map {
        mapOf("displayName" to it, "role" to role)
    }
    val parties = counterparties.lines().map { it.trim() }.filter { it.isNotEmpty() }.map {
        mapOf("name" to it, "kind" to "OTHER")
    }
    val channelList = channels.split(",", "\n").map { it.trim() }.filter { it.isNotEmpty() }
    return mapOf(
        "subtype" to subtype.code,
        "identityName" to identity.trim(),
        "partyName" to party.trim(),
        "description" to description.trim(),
        "currency" to "INR",
        "startDate" to "",
        "participants" to participants,
        "counterparties" to parties,
        "channels" to channelList,
        "monthlyBudget" to plainAmount(budget),
        "monthlyTarget" to plainAmount(target),
        "targetMetric" to metric,
    )
}

@Composable
private fun IdentityStep(
    subtypes: List<OperatingSubtype>,
    subtype: OperatingSubtype,
    onSubtype: (OperatingSubtype) -> Unit,
    identity: String,
    onIdentity: (String) -> Unit,
    party: String,
    onParty: (String) -> Unit,
    description: String,
    onDescription: (String) -> Unit,
) {
    Text("Operating model", color = Muted, fontSize = 12.sp)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        subtypes.forEach { item ->
            val selected = item.code == subtype.code
            Text(
                item.label,
                color = if (selected) Color(0xFF0C0F15) else TextPrimary,
                fontSize = 12.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (selected) Color(0xFF818CF8) else Card)
                    .border(1.dp, Border, RoundedCornerShape(12.dp))
                    .clickable { onSubtype(item) }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
            )
        }
    }
    FieldBlock("Moment name", "What you are running", identity, onIdentity)
    FieldBlock(subtype.partyLabel, "Who this moment is for", party, onParty)
    FieldBlock("Description", "Optional", description, onDescription)
}

@Composable
private fun PeopleStep(people: String, onPeople: (String) -> Unit, role: String, onRole: (String) -> Unit) {
    Text("People and roles", color = TextPrimary, fontWeight = FontWeight.Medium)
    Text("You are the Owner. Add other people, one name per line. They share the role you pick.", color = Muted, fontSize = 12.sp)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OperatingMomentCatalog.roles.take(4).forEach { (code, label) ->
            val selected = code == role
            Text(
                label,
                color = if (selected) Color(0xFF0C0F15) else TextPrimary,
                fontSize = 11.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selected) Color(0xFF34D399) else Card)
                    .clickable { onRole(code) }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
            )
        }
    }
    FieldBlock("Names", "Optional", people, onPeople)
}

@Composable
private fun PlanStep(
    subtype: OperatingSubtype,
    channels: String,
    onChannels: (String) -> Unit,
    budget: String,
    onBudget: (String) -> Unit,
    target: String,
    onTarget: (String) -> Unit,
    metric: String,
    onMetric: (String) -> Unit,
) {
    if (subtype.requiresChannel) {
        FieldBlock("Channels", "At least one, separated by commas", channels, onChannels)
    }
    FieldBlock("Monthly budget", "Positive amount", budget, onBudget)
    FieldBlock("Monthly target", "Positive amount or count", target, onTarget)
    Text("Target measure", color = Muted, fontSize = 12.sp)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        subtype.targetMetrics.forEach { item ->
            val selected = item == metric
            Text(
                item,
                color = if (selected) Color(0xFF0C0F15) else TextPrimary,
                fontSize = 11.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selected) Color(0xFFFBBF24) else Card)
                    .clickable { onMetric(item) }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
            )
        }
    }
}

@Composable
private fun FieldBlock(label: String, hint: String, value: String, onValue: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Text(hint, color = Muted, fontSize = 11.sp)
        BasicTextField(
            value = value,
            onValueChange = onValue,
            textStyle = TextStyle(color = TextPrimary, fontSize = 15.sp),
            cursorBrush = SolidColor(Color(0xFF818CF8)),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Card)
                .border(1.dp, Border, RoundedCornerShape(12.dp))
                .padding(12.dp),
        )
    }
}

@Composable
private fun StepButton(label: String, color: Color, tag: String? = null, onClick: () -> Unit) {
    Text(
        label,
        color = Color(0xFF0C0F15),
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .then(if (tag != null) Modifier.testTag(tag) else Modifier)
            .clip(RoundedCornerShape(12.dp))
            .background(color)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    )
}
