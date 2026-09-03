package com.example.momentra.ui.shell.business.teamops.create

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.momentra.data.api.CreateBusinessApprovalRequestBody
import com.example.momentra.data.api.CreateBusinessIssueBody
import com.example.momentra.data.api.CreateBusinessMemoryBody
import com.example.momentra.data.api.CreateBusinessUpdateBody
import com.example.momentra.data.api.CreateDecisionBody
import com.example.momentra.data.api.CreateMeetingRecordBody
import com.example.momentra.data.api.CreateMilestoneBody
import com.example.momentra.data.api.CreateRecognitionBody
import com.example.momentra.data.api.CreateRetrospectiveBody
import com.example.momentra.data.api.CreateRiskBody
import com.example.momentra.data.api.CreateActivityLogEntryBody
import com.example.momentra.data.repository.BusinessSliceRepository
import com.example.momentra.data.repository.GroupSliceRepository
import com.example.momentra.ui.setup.SetupDateTimeUtils
import com.example.momentra.ui.shell.business.shared.BusinessQuickAddKind
import com.example.momentra.ui.shell.business.shared.emoji
import com.example.momentra.ui.shell.business.shared.teamOpsHubIconRes
import com.example.momentra.ui.shell.business.teamops.components.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

private val UpdateTypes = listOf("Progress", "Announcement", "Blocker Resolved")
private val Visibilities = listOf("Team", "Company", "Private")
private val DecidedByOptions = listOf("You", "Lead", "Team")
private val ImpactAreas = listOf("Engineering", "Design", "Operations", "All")
private val Severities = listOf("Critical", "High", "Medium", "Low")
private val BlockedItems = listOf("Feature", "Release", "Dependency", "Sprint goal", "Other")
private val OwnerOptions = listOf("You", "Lead", "Unassigned")
private val RecognitionTypes = listOf("Kudos", "Shoutout", "Award")
private val Urgencies = listOf("Normal", "High", "Urgent")
private val MilestoneStatuses = listOf("Planned", "In Progress", "Done")
private val ActivityOwners = listOf("You", "Lead", "Team")
private val ActivityCategories = listOf("Delivery", "Decision", "Ops", "Other")

object TeamOpsQuickAddSheets {
    fun isTeamOpsKind(kind: BusinessQuickAddKind): Boolean = when (kind) {
        BusinessQuickAddKind.TEAM_UPDATE,
        BusinessQuickAddKind.DECISION,
        BusinessQuickAddKind.BLOCKER,
        BusinessQuickAddKind.MEETING,
        BusinessQuickAddKind.RECOGNITION,
        BusinessQuickAddKind.APPROVAL,
        BusinessQuickAddKind.MILESTONE,
        BusinessQuickAddKind.RETROSPECTIVE,
        BusinessQuickAddKind.RISK_FLAG,
        BusinessQuickAddKind.ACTIVITY_LOG,
        BusinessQuickAddKind.POLL,
        BusinessQuickAddKind.MEMORY,
        -> true
        else -> false
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamOpsGapQuickAddSheet(
    kind: BusinessQuickAddKind,
    visible: Boolean,
    momentId: String?,
    onDismiss: () -> Unit,
    onSaved: () -> Unit = {},
    repository: BusinessSliceRepository = remember { BusinessSliceRepository() },
    groupRepo: GroupSliceRepository = remember { GroupSliceRepository() },
) {
    if (!visible) return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = TeamOpsSheetTokens.SheetBg,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 12.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TeamOpsSheetHandle()
            when (kind) {
                BusinessQuickAddKind.TEAM_UPDATE ->
                    TeamUpdateForm(momentId, onDismiss, onSaved, repository)
                BusinessQuickAddKind.DECISION ->
                    DecisionForm(momentId, onDismiss, onSaved, repository)
                BusinessQuickAddKind.BLOCKER ->
                    BlockerForm(momentId, onDismiss, onSaved, repository)
                BusinessQuickAddKind.MEETING ->
                    MeetingForm(momentId, onDismiss, onSaved, repository)
                BusinessQuickAddKind.RECOGNITION ->
                    RecognitionForm(momentId, onDismiss, onSaved, repository)
                BusinessQuickAddKind.APPROVAL ->
                    ApprovalForm(momentId, onDismiss, onSaved, repository)
                BusinessQuickAddKind.MILESTONE ->
                    MilestoneForm(momentId, onDismiss, onSaved, repository)
                BusinessQuickAddKind.RETROSPECTIVE ->
                    RetroForm(momentId, onDismiss, onSaved, repository)
                BusinessQuickAddKind.RISK_FLAG ->
                    RiskForm(momentId, onDismiss, onSaved, repository)
                BusinessQuickAddKind.ACTIVITY_LOG ->
                    ActivityForm(momentId, onDismiss, onSaved, repository)
                BusinessQuickAddKind.POLL ->
                    PollForm(momentId, onDismiss, onSaved, groupRepo)
                BusinessQuickAddKind.MEMORY ->
                    MemoryForm(momentId, onDismiss, onSaved, repository)
                else -> Unit
            }
        }
    }
}

@Composable
private fun FieldBlock(label: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        TeamOpsFieldLabel(label)
        content()
    }
}

@Composable
private fun TeamUpdateForm(
    momentId: String?,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    repository: BusinessSliceRepository,
) {
    val accent = TeamOpsIndigoAccent
    val kind = BusinessQuickAddKind.TEAM_UPDATE
    var updateType by remember { mutableStateOf(UpdateTypes.first()) }
    var title by remember { mutableStateOf("") }
    var details by remember { mutableStateOf("") }
    var visibility by remember { mutableStateOf(Visibilities.first()) }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    TeamOpsSheetHeader(
        iconRes = kind.teamOpsHubIconRes(),
        emojiFallback = kind.emoji(),
        title = "Team Update",
        explanation = "Share progress with the team",
        accent = accent,
        onClose = onDismiss,
    )
    FieldBlock("Update Type") {
        TeamOpsSegmentedControl(UpdateTypes, updateType, accent) { updateType = it }
    }
    FieldBlock("Title") {
        TeamOpsTextField(title, { title = it }, "Update title", accent)
    }
    FieldBlock("Details") {
        TeamOpsTextField(details, { details = it }, "What should the team know…", accent, singleLine = false, minHeight = 96)
    }
    FieldBlock("Visibility") {
        TeamOpsChipRow(Visibilities, visibility, accent) { visibility = it }
    }
    TeamOpsErrorText(error)
    TeamOpsPrimaryCta(
        label = if (submitting) "Saving…" else "Share Update",
        enabled = !momentId.isNullOrBlank() && title.isNotBlank() && !submitting,
        loading = submitting,
        footerHint = "Update will be shared",
        accent = accent,
        onClick = {
            val id = momentId ?: return@TeamOpsPrimaryCta
            submitting = true
            error = null
            scope.launch {
                val body = buildString {
                    appendLine("Type: $updateType")
                    appendLine("Visibility: $visibility")
                    if (details.isNotBlank()) append(details.trim())
                }
                repository.createBusinessUpdate(
                    momentId = id,
                    body = CreateBusinessUpdateBody(title = title.trim(), body = body),
                    idempotencyKey = UUID.randomUUID().toString(),
                ).fold(
                    onSuccess = { submitting = false; onSaved(); onDismiss() },
                    onFailure = { submitting = false; error = it.message ?: "Could not share update" },
                )
            }
        },
    )
}

@Composable
private fun DecisionForm(
    momentId: String?,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    repository: BusinessSliceRepository,
) {
    val accent = TeamOpsLavenderAccent
    val kind = BusinessQuickAddKind.DECISION
    var decision by remember { mutableStateOf("") }
    var context by remember { mutableStateOf("") }
    var decidedBy by remember { mutableStateOf(DecidedByOptions.first()) }
    var isoDate by remember { mutableStateOf(SetupDateTimeUtils.localDateToIso(LocalDate.now())) }
    var impact by remember { mutableStateOf(ImpactAreas.first()) }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    TeamOpsSheetHeader(
        iconRes = kind.teamOpsHubIconRes(),
        emojiFallback = kind.emoji(),
        title = "Log Decision",
        explanation = "Record a choice and its impact",
        accent = accent,
        onClose = onDismiss,
    )
    FieldBlock("Decision") {
        TeamOpsTextField(decision, { decision = it }, "What was decided", accent)
    }
    FieldBlock("Context") {
        TeamOpsTextField(context, { context = it }, "Why this decision was made…", accent, singleLine = false, minHeight = 80)
    }
    FieldBlock("Decided By") {
        TeamOpsDropdownField(decidedBy, DecidedByOptions, { decidedBy = it }, "Select")
    }
    FieldBlock("Date") {
        TeamOpsDateField(isoDate, { isoDate = it })
    }
    FieldBlock("Impact") {
        TeamOpsChipRow(ImpactAreas, impact, accent) { impact = it }
    }
    TeamOpsErrorText(error)
    TeamOpsPrimaryCta(
        label = if (submitting) "Saving…" else "Log Decision",
        enabled = !momentId.isNullOrBlank() && decision.isNotBlank() && !submitting,
        loading = submitting,
        footerHint = "Decision will be logged",
        accent = accent,
        onClick = {
            val id = momentId ?: return@TeamOpsPrimaryCta
            submitting = true
            error = null
            scope.launch {
                val rationale = buildString {
                    appendLine("Decided by: $decidedBy")
                    appendLine("Date: $isoDate")
                    appendLine("Impact: $impact")
                    if (context.isNotBlank()) append(context.trim())
                }
                repository.createDecision(
                    momentId = id,
                    body = CreateDecisionBody(
                        title = decision.trim(),
                        decisionText = context.trim().ifBlank { decision.trim() },
                        rationale = rationale,
                    ),
                    idempotencyKey = UUID.randomUUID().toString(),
                ).fold(
                    onSuccess = { submitting = false; onSaved(); onDismiss() },
                    onFailure = { submitting = false; error = it.message ?: "Could not log decision" },
                )
            }
        },
    )
}

@Composable
private fun BlockerForm(
    momentId: String?,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    repository: BusinessSliceRepository,
) {
    IssueStyleForm(
        momentId = momentId,
        onDismiss = onDismiss,
        onSaved = onSaved,
        repository = repository,
        kind = BusinessQuickAddKind.BLOCKER,
        accent = TeamOpsRedAccent,
        sheetTitle = "Flag Blocker",
        explanation = "Surface a delivery blocker",
        titleLabel = "Blocker",
        titlePlaceholder = "Blocker title",
        ctaLabel = "Flag Blocker",
        footerHint = "Team will be notified",
        errorFallback = "Could not flag blocker",
    )
}

@Composable
private fun RiskForm(
    momentId: String?,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    repository: BusinessSliceRepository,
) {
    val accent = TeamOpsRedAccent
    val kind = BusinessQuickAddKind.RISK_FLAG
    var title by remember { mutableStateOf("") }
    var severity by remember { mutableStateOf("High") }
    var blockedItem by remember { mutableStateOf(BlockedItems.first()) }
    var owner by remember { mutableStateOf(OwnerOptions.first()) }
    var isoDate by remember { mutableStateOf(SetupDateTimeUtils.localDateToIso(LocalDate.now())) }
    var details by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    TeamOpsSheetHeader(
        iconRes = kind.teamOpsHubIconRes(),
        emojiFallback = kind.emoji(),
        title = "Risk Flag",
        explanation = "Raise a delivery risk early",
        accent = accent,
        onClose = onDismiss,
    )
    FieldBlock("Risk") {
        TeamOpsTextField(title, { title = it }, "Risk title", accent)
    }
    FieldBlock("Severity") {
        TeamOpsChipRow(Severities, severity, accent) { severity = it }
    }
    FieldBlock("Blocked Item") {
        TeamOpsDropdownField(blockedItem, BlockedItems, { blockedItem = it }, "Select item")
    }
    FieldBlock("Owner") {
        TeamOpsDropdownField(owner, OwnerOptions, { owner = it }, "Select owner")
    }
    FieldBlock("Due date") {
        TeamOpsDateField(isoDate, { isoDate = it })
    }
    FieldBlock("Details") {
        TeamOpsTextField(details, { details = it }, "Describe impact…", accent, singleLine = false, minHeight = 80)
    }
    TeamOpsErrorText(error)
    TeamOpsPrimaryCta(
        label = if (submitting) "Saving…" else "Flag Risk",
        enabled = !momentId.isNullOrBlank() && title.isNotBlank() && !submitting,
        loading = submitting,
        footerHint = "Risk will be flagged",
        accent = accent,
        onClick = {
            val id = momentId ?: return@TeamOpsPrimaryCta
            submitting = true
            error = null
            scope.launch {
                val impactApi = when (severity) {
                    "Critical" -> "CRITICAL"
                    "High" -> "HIGH"
                    "Medium" -> "MEDIUM"
                    else -> "LOW"
                }
                val description = buildList {
                    add("Blocked item: $blockedItem")
                    add("Owner: $owner")
                    add("Due: $isoDate")
                    if (details.isNotBlank()) add(details.trim())
                }.joinToString(" · ")
                repository.createRisk(
                    momentId = id,
                    body = CreateRiskBody(
                        title = title.trim(),
                        description = description,
                        likelihood = "MEDIUM",
                        impact = impactApi,
                    ),
                    idempotencyKey = UUID.randomUUID().toString(),
                ).fold(
                    onSuccess = { submitting = false; onSaved(); onDismiss() },
                    onFailure = { submitting = false; error = it.message ?: "Could not flag risk" },
                )
            }
        },
    )
}

@Composable
private fun IssueStyleForm(
    momentId: String?,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    repository: BusinessSliceRepository,
    kind: BusinessQuickAddKind,
    accent: TeamOpsSheetAccent,
    sheetTitle: String,
    explanation: String,
    titleLabel: String,
    titlePlaceholder: String,
    ctaLabel: String,
    footerHint: String,
    errorFallback: String,
) {
    var title by remember { mutableStateOf("") }
    var severity by remember { mutableStateOf("High") }
    var blockedItem by remember { mutableStateOf(BlockedItems.first()) }
    var owner by remember { mutableStateOf(OwnerOptions.first()) }
    var isoDate by remember { mutableStateOf(SetupDateTimeUtils.localDateToIso(LocalDate.now())) }
    var details by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    TeamOpsSheetHeader(
        iconRes = kind.teamOpsHubIconRes(),
        emojiFallback = kind.emoji(),
        title = sheetTitle,
        explanation = explanation,
        accent = accent,
        onClose = onDismiss,
    )
    FieldBlock(titleLabel) {
        TeamOpsTextField(title, { title = it }, titlePlaceholder, accent)
    }
    FieldBlock("Severity") {
        TeamOpsChipRow(Severities, severity, accent) { severity = it }
    }
    FieldBlock("Blocked Item") {
        TeamOpsDropdownField(blockedItem, BlockedItems, { blockedItem = it }, "Select item")
    }
    FieldBlock("Owner") {
        TeamOpsDropdownField(owner, OwnerOptions, { owner = it }, "Select owner")
    }
    FieldBlock("Due date") {
        TeamOpsDateField(isoDate, { isoDate = it })
    }
    FieldBlock("Details") {
        TeamOpsTextField(details, { details = it }, "Describe impact…", accent, singleLine = false, minHeight = 80)
    }
    TeamOpsErrorText(error)
    TeamOpsPrimaryCta(
        label = if (submitting) "Saving…" else ctaLabel,
        enabled = !momentId.isNullOrBlank() && title.isNotBlank() && !submitting,
        loading = submitting,
        footerHint = footerHint,
        accent = accent,
        onClick = {
            val id = momentId ?: return@TeamOpsPrimaryCta
            submitting = true
            error = null
            scope.launch {
                val description = buildList {
                    add("Blocked item: $blockedItem")
                    add("Owner: $owner")
                    add("Due: $isoDate")
                    if (details.isNotBlank()) add(details.trim())
                }.joinToString(" · ")
                repository.createIssue(
                    momentId = id,
                    body = CreateBusinessIssueBody(
                        title = title.trim(),
                        description = description,
                        severity = severity.uppercase(),
                    ),
                    idempotencyKey = UUID.randomUUID().toString(),
                ).fold(
                    onSuccess = { submitting = false; onSaved(); onDismiss() },
                    onFailure = { submitting = false; error = it.message ?: errorFallback },
                )
            }
        },
    )
}

@Composable
private fun MeetingForm(
    momentId: String?,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    repository: BusinessSliceRepository,
) {
    val accent = TeamOpsIndigoAccent
    val kind = BusinessQuickAddKind.MEETING
    var title by remember { mutableStateOf("") }
    var isoDate by remember { mutableStateOf(SetupDateTimeUtils.localDateToIso(LocalDate.now())) }
    var timeHm by remember {
        mutableStateOf("%02d:%02d".format(LocalTime.now().hour, LocalTime.now().minute))
    }
    var notes by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    TeamOpsSheetHeader(
        iconRes = kind.teamOpsHubIconRes(),
        emojiFallback = kind.emoji(),
        title = "Log Meeting",
        explanation = "Capture notes and next steps",
        accent = accent,
        onClose = onDismiss,
    )
    FieldBlock("Title") {
        TeamOpsTextField(title, { title = it }, "Sprint review", accent)
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TeamOpsFieldLabel("Date")
            TeamOpsDateField(isoDate, { isoDate = it })
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TeamOpsFieldLabel("Time")
            TeamOpsTimeField(timeHm, { timeHm = it })
        }
    }
    FieldBlock("Notes") {
        TeamOpsTextField(notes, { notes = it }, "Decisions and next steps…", accent, singleLine = false, minHeight = 96)
    }
    TeamOpsErrorText(error)
    TeamOpsPrimaryCta(
        label = if (submitting) "Saving…" else "Log Meeting",
        enabled = !momentId.isNullOrBlank() && title.isNotBlank() && !submitting,
        loading = submitting,
        footerHint = "Meeting will be logged",
        accent = accent,
        onClick = {
            val id = momentId ?: return@TeamOpsPrimaryCta
            submitting = true
            error = null
            scope.launch {
                val meetingAt = "${isoDate}T${timeHm}:00.000Z"
                repository.createMeetingRecord(
                    momentId = id,
                    body = CreateMeetingRecordBody(
                        title = title.trim(),
                        meetingAt = meetingAt,
                        notes = notes.trim().ifBlank { null },
                    ),
                    idempotencyKey = UUID.randomUUID().toString(),
                ).fold(
                    onSuccess = { submitting = false; onSaved(); onDismiss() },
                    onFailure = { submitting = false; error = it.message ?: "Could not log meeting" },
                )
            }
        },
    )
}

@Composable
private fun RecognitionForm(
    momentId: String?,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    repository: BusinessSliceRepository,
) {
    val accent = TeamOpsIndigoAccent
    val kind = BusinessQuickAddKind.RECOGNITION
    var recipient by remember { mutableStateOf("") }
    var why by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(RecognitionTypes.first()) }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    TeamOpsSheetHeader(
        iconRes = kind.teamOpsHubIconRes(),
        emojiFallback = kind.emoji(),
        title = "Recognition",
        explanation = "Celebrate a teammate win",
        accent = accent,
        onClose = onDismiss,
    )
    FieldBlock("Recipient") {
        TeamOpsTextField(recipient, { recipient = it }, "Teammate name", accent)
    }
    FieldBlock("Why") {
        TeamOpsTextField(why, { why = it }, "What they did…", accent, singleLine = false, minHeight = 80)
    }
    FieldBlock("Type") {
        TeamOpsChipRow(RecognitionTypes, type, accent) { type = it }
    }
    TeamOpsErrorText(error)
    TeamOpsPrimaryCta(
        label = if (submitting) "Saving…" else "Give Recognition",
        enabled = !momentId.isNullOrBlank() && recipient.isNotBlank() && why.isNotBlank() && !submitting,
        loading = submitting,
        footerHint = "Recognition will be shared",
        accent = accent,
        onClick = {
            val id = momentId ?: return@TeamOpsPrimaryCta
            submitting = true
            error = null
            scope.launch {
                repository.createRecognition(
                    momentId = id,
                    body = CreateRecognitionBody(
                        recipientName = recipient.trim(),
                        recognitionType = type.uppercase(),
                        whyText = why.trim(),
                    ),
                    idempotencyKey = UUID.randomUUID().toString(),
                ).fold(
                    onSuccess = { submitting = false; onSaved(); onDismiss() },
                    onFailure = { submitting = false; error = it.message ?: "Could not give recognition" },
                )
            }
        },
    )
}

@Composable
private fun ApprovalForm(
    momentId: String?,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    repository: BusinessSliceRepository,
) {
    val accent = TeamOpsIndigoAccent
    val kind = BusinessQuickAddKind.APPROVAL
    var title by remember { mutableStateOf("") }
    var amountDisplay by remember { mutableStateOf("") }
    var urgency by remember { mutableStateOf(Urgencies.first()) }
    var note by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    TeamOpsSheetHeader(
        iconRes = kind.teamOpsHubIconRes(),
        emojiFallback = kind.emoji(),
        title = "Request Approval",
        explanation = "Route a request for sign-off",
        accent = accent,
        onClose = onDismiss,
    )
    FieldBlock("Request title") {
        TeamOpsTextField(title, { title = it }, "What needs sign-off", accent)
    }
    FieldBlock("Amount") {
        TeamOpsAmountField(amountDisplay, { amountDisplay = it }, accent)
    }
    FieldBlock("Urgency") {
        TeamOpsChipRow(Urgencies, urgency, accent) { urgency = it }
    }
    FieldBlock("Note") {
        TeamOpsTextField(note, { note = it }, "Optional context", accent, singleLine = false, minHeight = 72)
    }
    TeamOpsErrorText(error)
    TeamOpsPrimaryCta(
        label = if (submitting) "Saving…" else "Request Approval",
        enabled = !momentId.isNullOrBlank() && title.isNotBlank() && !submitting,
        loading = submitting,
        footerHint = "Stakeholders will be notified",
        accent = accent,
        onClick = {
            val id = momentId ?: return@TeamOpsPrimaryCta
            val amount = teamOpsStripAmount(amountDisplay).takeIf { it.isNotBlank() }
            submitting = true
            error = null
            scope.launch {
                val noteBody = buildList {
                    add("Urgency: $urgency")
                    if (note.isNotBlank()) add(note.trim())
                }.joinToString(" · ")
                repository.createApprovalRequest(
                    momentId = id,
                    body = CreateBusinessApprovalRequestBody(
                        title = title.trim(),
                        amount = amount,
                        currencyCode = "INR",
                        note = noteBody,
                    ),
                    idempotencyKey = UUID.randomUUID().toString(),
                ).fold(
                    onSuccess = { submitting = false; onSaved(); onDismiss() },
                    onFailure = { submitting = false; error = it.message ?: "Could not request approval" },
                )
            }
        },
    )
}

@Composable
private fun MilestoneForm(
    momentId: String?,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    repository: BusinessSliceRepository,
) {
    val accent = TeamOpsIndigoAccent
    val kind = BusinessQuickAddKind.MILESTONE
    var name by remember { mutableStateOf("") }
    var isoDate by remember { mutableStateOf(SetupDateTimeUtils.localDateToIso(LocalDate.now())) }
    var status by remember { mutableStateOf(MilestoneStatuses.first()) }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    TeamOpsSheetHeader(
        iconRes = kind.teamOpsHubIconRes(),
        emojiFallback = kind.emoji(),
        title = "Add Milestone",
        explanation = "Mark a delivery checkpoint",
        accent = accent,
        onClose = onDismiss,
    )
    FieldBlock("Milestone name") {
        TeamOpsTextField(name, { name = it }, "Ship v1.2", accent)
    }
    FieldBlock("Due date") {
        TeamOpsDateField(isoDate, { isoDate = it })
    }
    FieldBlock("Status") {
        TeamOpsChipRow(MilestoneStatuses, status, accent) { status = it }
    }
    TeamOpsErrorText(error)
    TeamOpsPrimaryCta(
        label = if (submitting) "Saving…" else "Add Milestone",
        enabled = !momentId.isNullOrBlank() && name.isNotBlank() && !submitting,
        loading = submitting,
        footerHint = "Milestone will be tracked",
        accent = accent,
        onClick = {
            val id = momentId ?: return@TeamOpsPrimaryCta
            submitting = true
            error = null
            scope.launch {
                val apiStatus = when (status) {
                    "Planned" -> "PLANNED"
                    "In Progress" -> "ACTIVE"
                    "Done" -> "COMPLETED"
                    else -> "PLANNED"
                }
                val targetAt = "${isoDate}T12:00:00.000Z"
                repository.createMilestone(
                    momentId = id,
                    body = CreateMilestoneBody(
                        title = name.trim(),
                        targetAt = targetAt,
                        status = apiStatus,
                    ),
                    idempotencyKey = UUID.randomUUID().toString(),
                ).fold(
                    onSuccess = { submitting = false; onSaved(); onDismiss() },
                    onFailure = { submitting = false; error = it.message ?: "Could not add milestone" },
                )
            }
        },
    )
}

@Composable
private fun RetroForm(
    momentId: String?,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    repository: BusinessSliceRepository,
) {
    val accent = TeamOpsIndigoAccent
    val kind = BusinessQuickAddKind.RETROSPECTIVE
    var wentWell by remember { mutableStateOf("") }
    var improve by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    TeamOpsSheetHeader(
        iconRes = kind.teamOpsHubIconRes(),
        emojiFallback = kind.emoji(),
        title = "Retrospective",
        explanation = "Review wins and improvements",
        accent = accent,
        onClose = onDismiss,
    )
    FieldBlock("What went well") {
        TeamOpsTextField(wentWell, { wentWell = it }, "Wins…", accent, singleLine = false, minHeight = 80)
    }
    FieldBlock("Improve next") {
        TeamOpsTextField(improve, { improve = it }, "What to improve…", accent, singleLine = false, minHeight = 80)
    }
    TeamOpsErrorText(error)
    TeamOpsPrimaryCta(
        label = if (submitting) "Saving…" else "Save Retrospective",
        enabled = !momentId.isNullOrBlank() && (wentWell.isNotBlank() || improve.isNotBlank()) && !submitting,
        loading = submitting,
        footerHint = "Retrospective will be saved",
        accent = accent,
        onClick = {
            val id = momentId ?: return@TeamOpsPrimaryCta
            submitting = true
            error = null
            scope.launch {
                repository.createRetrospective(
                    momentId = id,
                    body = CreateRetrospectiveBody(
                        wentWell = wentWell.trim().ifBlank { null },
                        improveNext = improve.trim().ifBlank { null },
                    ),
                    idempotencyKey = UUID.randomUUID().toString(),
                ).fold(
                    onSuccess = { submitting = false; onSaved(); onDismiss() },
                    onFailure = { submitting = false; error = it.message ?: "Could not save retrospective" },
                )
            }
        },
    )
}

@Composable
private fun ActivityForm(
    momentId: String?,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    repository: BusinessSliceRepository,
) {
    val accent = TeamOpsIndigoAccent
    val kind = BusinessQuickAddKind.ACTIVITY_LOG
    var title by remember { mutableStateOf("") }
    var owner by remember { mutableStateOf(ActivityOwners.first()) }
    var category by remember { mutableStateOf(ActivityCategories.first()) }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    TeamOpsSheetHeader(
        iconRes = kind.teamOpsHubIconRes(),
        emojiFallback = kind.emoji(),
        title = "Activity Log",
        explanation = "Track a team action",
        accent = accent,
        onClose = onDismiss,
    )
    FieldBlock("Activity title") {
        TeamOpsTextField(title, { title = it }, "What happened", accent)
    }
    FieldBlock("Owner") {
        TeamOpsDropdownField(owner, ActivityOwners, { owner = it }, "Select owner")
    }
    FieldBlock("Category") {
        TeamOpsChipRow(ActivityCategories, category, accent) { category = it }
    }
    TeamOpsErrorText(error)
    TeamOpsPrimaryCta(
        label = if (submitting) "Saving…" else "Log Activity",
        enabled = !momentId.isNullOrBlank() && title.isNotBlank() && !submitting,
        loading = submitting,
        footerHint = "Activity will be logged",
        accent = accent,
        onClick = {
            val id = momentId ?: return@TeamOpsPrimaryCta
            submitting = true
            error = null
            scope.launch {
                repository.createActivityLogEntry(
                    momentId = id,
                    body = CreateActivityLogEntryBody(
                        title = title.trim(),
                        ownerLabel = owner,
                        categoryCode = category.uppercase(),
                    ),
                    idempotencyKey = UUID.randomUUID().toString(),
                ).fold(
                    onSuccess = { submitting = false; onSaved(); onDismiss() },
                    onFailure = { submitting = false; error = it.message ?: "Could not log activity" },
                )
            }
        },
    )
}

@Composable
private fun PollForm(
    momentId: String?,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    groupRepo: GroupSliceRepository,
) {
    val accent = TeamOpsIndigoAccent
    val kind = BusinessQuickAddKind.POLL
    var question by remember { mutableStateOf("") }
    var optionA by remember { mutableStateOf("") }
    var optionB by remember { mutableStateOf("") }
    var optionC by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    TeamOpsSheetHeader(
        iconRes = kind.teamOpsHubIconRes(),
        emojiFallback = kind.emoji(),
        title = "Create Poll",
        explanation = "Gather quick team input",
        accent = accent,
        onClose = onDismiss,
    )
    FieldBlock("Question") {
        TeamOpsTextField(question, { question = it }, "What should we decide?", accent)
    }
    FieldBlock("Option A") {
        TeamOpsTextField(optionA, { optionA = it }, "First option", accent)
    }
    FieldBlock("Option B") {
        TeamOpsTextField(optionB, { optionB = it }, "Second option", accent)
    }
    FieldBlock("Option C") {
        TeamOpsTextField(optionC, { optionC = it }, "Optional", accent)
    }
    TeamOpsErrorText(error)
    TeamOpsPrimaryCta(
        label = if (submitting) "Saving…" else "Create Poll",
        enabled = !momentId.isNullOrBlank() &&
            question.isNotBlank() &&
            optionA.isNotBlank() &&
            optionB.isNotBlank() &&
            !submitting,
        loading = submitting,
        footerHint = "Poll will go live",
        accent = accent,
        onClick = {
            val id = momentId ?: return@TeamOpsPrimaryCta
            val options = listOf(optionA, optionB, optionC).map { it.trim() }.filter { it.isNotBlank() }
            submitting = true
            error = null
            scope.launch {
                groupRepo.createPoll(
                    momentId = id,
                    question = question.trim(),
                    options = options,
                    idempotencyKey = UUID.randomUUID().toString(),
                ).fold(
                    onSuccess = { submitting = false; onSaved(); onDismiss() },
                    onFailure = { submitting = false; error = it.message ?: "Could not create poll" },
                )
            }
        },
    )
}

@Composable
private fun MemoryForm(
    momentId: String?,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    repository: BusinessSliceRepository,
) {
    val accent = TeamOpsIndigoAccent
    val kind = BusinessQuickAddKind.MEMORY
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    TeamOpsSheetHeader(
        iconRes = kind.teamOpsHubIconRes(),
        emojiFallback = kind.emoji(),
        title = "Save to Memory",
        explanation = "Capture a learning for the playbook",
        accent = accent,
        onClose = onDismiss,
    )
    FieldBlock("Title") {
        TeamOpsTextField(title, { title = it }, "Memory title", accent)
    }
    FieldBlock("Body") {
        TeamOpsTextField(body, { body = it }, "What should we remember?", accent, singleLine = false, minHeight = 96)
    }
    TeamOpsErrorText(error)
    TeamOpsPrimaryCta(
        label = if (submitting) "Saving…" else "Save to Memory",
        enabled = !momentId.isNullOrBlank() && title.isNotBlank() && body.isNotBlank() && !submitting,
        loading = submitting,
        footerHint = "Logged under team memory",
        accent = accent,
        onClick = {
            val id = momentId ?: return@TeamOpsPrimaryCta
            submitting = true
            error = null
            scope.launch {
                repository.createMemory(
                    momentId = id,
                    body = CreateBusinessMemoryBody(
                        title = title.trim(),
                        body = body.trim(),
                    ),
                    idempotencyKey = UUID.randomUUID().toString(),
                ).fold(
                    onSuccess = { submitting = false; onSaved(); onDismiss() },
                    onFailure = { submitting = false; error = it.message ?: "Could not save memory" },
                )
            }
        },
    )
}
