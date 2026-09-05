package com.example.momentra.ui.shell.empty.group

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.R
import com.example.momentra.data.api.CreateMomentParticipantBody
import com.example.momentra.data.api.GroupInviteDto
import com.example.momentra.data.api.GroupSetupBlockDto
import kotlinx.coroutines.launch
import com.example.momentra.domain.CreateMomentOutcome
import com.example.momentra.data.repository.AccountRepository
import com.example.momentra.ui.create.MomentCreateViewModel
import com.example.momentra.ui.setup.SetupDateTimeUtils
import com.example.momentra.ui.setup.SetupDateRangeField
import com.example.momentra.ui.setup.SetupTitleField
import com.example.momentra.ui.shell.maestro.MaestroIds
import com.example.momentra.ui.theme.PlusJakartaSans

/** Figma 575:9761 — Shared Experience long-form setup. */
@Composable
fun GroupExperienceSetupContent(
    onBack: () -> Unit,
    onCreated: (CreateMomentOutcome) -> Unit,
    createViewModel: MomentCreateViewModel,
    submitting: Boolean,
    error: String?,
    onSetupTypeChanged: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    editingMomentId: String? = null,
    initialTitle: String? = null,
    initialTypeCode: String? = null,
) {
    val types = remember { GroupSetupCatalog.experienceTypes }
    var selectedCode by remember {
        mutableStateOf(
            initialTypeCode?.takeIf { code -> types.any { it.code == code } } ?: "TRIP",
        )
    }
    val selected = types.first { it.code == selectedCode }
    var name by remember(selectedCode) {
        mutableStateOf(initialTitle?.takeIf { it.isNotBlank() } ?: selected.defaultName)
    }
    var startDateIso by remember(selectedCode) { mutableStateOf<String?>(null) }
    var endDateIso by remember(selectedCode) { mutableStateOf<String?>(null) }
    var destination by remember(selectedCode) { mutableStateOf("") }
    var primaryGoal by remember(selectedCode) {
        mutableStateOf("Enjoy time together")
    }
    var budget by remember(selectedCode) { mutableStateOf("₹80,000") }
    var budgetCustomAmount by remember(selectedCode) { mutableStateOf("") }
    var currency by remember(selectedCode) { mutableStateOf("INR") }
    var splitStyle by remember(selectedCode) { mutableStateOf("Equal split") }
    var multiCurrency by remember(selectedCode) { mutableStateOf("Enabled") }
    var paymentRhythm by remember(selectedCode) { mutableStateOf("After each expense") }
    var joinApproval by remember(selectedCode) { mutableStateOf("Admin approval") }
    var notifyChanges by remember(selectedCode) { mutableStateOf(true) }
    var expenseReminders by remember(selectedCode) { mutableStateOf("Enabled") }
    var photoReminders by remember(selectedCode) { mutableStateOf("Enabled") }
    var updateCadence by remember(selectedCode) { mutableStateOf("Every week") }
    var people by remember(selectedCode) { mutableStateOf(defaultGroupPeople(selectedCode)) }
    var peopleEdited by remember(selectedCode) { mutableStateOf(false) }
    var issuedInvite by remember { mutableStateOf<GroupInviteDto?>(null) }
    var mintingInvite by remember { mutableStateOf(false) }
    var inviteError by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val accountRepo = remember { AccountRepository() }

    LaunchedEffect(editingMomentId) {
        val mid = editingMomentId ?: return@LaunchedEffect
        accountRepo.getMomentNotificationPreferences(mid)
            .onSuccess { notifyChanges = it.notifyOnChanges }
    }

    val palette = selected.palette
    val accent = palette.accent

    suspend fun ensureInvite(): GroupInviteDto? {
        issuedInvite?.let { return it }
        mintingInvite = true
        inviteError = null
        val minted = createViewModel.mintGroupInvite(
            title = name.trim().ifBlank { selected.defaultName },
            momentTypeCode = selectedCode,
            section = "experience",
        )
        mintingInvite = false
        if (minted == null) {
            inviteError = "Couldn’t create invite link. Try again."
            return null
        }
        issuedInvite = minted
        return minted
    }

    fun shareQr() {
        scope.launch {
            val invite = ensureInvite() ?: return@launch
            val url = GroupInviteLink.qrPayload(invite.inviteCode)
            val bitmap = generateInviteQrBitmap(url, 512)
            shareInviteQr(context, bitmap, url)
        }
    }

    fun shareWhatsApp() {
        scope.launch {
            val invite = ensureInvite() ?: return@launch
            val url = GroupInviteLink.copyText(invite.inviteCode)
            sendInviteWhatsApp(
                context,
                phone = null,
                message = inviteMessage(name.trim().ifBlank { selected.defaultName }, url),
            )
        }
    }

    LaunchedEffect(selectedCode) {
        onSetupTypeChanged(selectedCode)
        issuedInvite = null
        inviteError = null
    }

    Box(modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(GroupSetupTheme.Bg)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 16.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    modifier = Modifier
                        .clickable(onClick = onBack)
                        .semantics {
                            role = Role.Button
                            contentDescription = "Close"
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text("×", color = GroupSetupTheme.TextSecondary, fontSize = 16.sp, fontFamily = PlusJakartaSans)
                    Text("Close", color = GroupSetupTheme.TextSecondary, fontSize = 14.sp, fontFamily = PlusJakartaSans)
                }
                Text(
                    "GROUP MODE",
                    color = GroupSetupTheme.TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = PlusJakartaSans,
                )
            }

            GroupSetupHero(
                title = "Set up Shared Experience",
                subtitle = "Plan a trip, celebration, or shared adventure together. Everything can be refined later.",
                accent = accent,
                iconRes = selected.iconRes,
            )

            GroupLongFormTypeChipStrip(
                title = "Experience setups",
                types = types,
                selectedCode = selectedCode,
                onSelect = { opt ->
                    selectedCode = opt.code
                    name = opt.defaultName
                    startDateIso = null
                    endDateIso = null
                    people = defaultGroupPeople(opt.code)
                    peopleEdited = false
                },
                shortLabel = ::experienceChipLabel,
            )

            GroupLongFormDiamondDivider()

            GroupLongFormSectionCard(step = "01", title = "Experience Basics", accent = accent) {
                SetupTitleField(
                    label = selected.nameLabel,
                    value = name,
                    onValueChange = { name = it },
                    placeholder = selected.defaultName,
                    testTag = MaestroIds.SETUP_TITLE,
                )
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    GroupLongFormSubsectionTitle("Your Experience")
                    GroupLongFormPrefRow(
                        label = "Experience type",
                        hint = "What are you planning?",
                        value = experienceChipLabel(selected),
                        options = types.map(::experienceChipLabel),
                        onValueChange = { label ->
                            val next = types.first { experienceChipLabel(it) == label }
                            selectedCode = next.code
                            name = next.defaultName
                            startDateIso = null
                            endDateIso = null
                            people = defaultGroupPeople(next.code)
                        },
                        testTag = MaestroIds.setupDropdown("experienceType"),
                    )
                    GroupLongFormPrefRow(
                        label = "Primary goal",
                        hint = "What brings everyone together?",
                        value = primaryGoal,
                        options = listOf("Enjoy time together", "Celebrate", "Explore", "Reconnect"),
                        onValueChange = { primaryGoal = it },
                        testTag = MaestroIds.setupDropdown("primaryGoal"),
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    GroupLongFormSubsectionTitle("Experience Details")
                    GroupLongFormDestinationField(
                        label = "Destination",
                        hint = "Where you're going",
                        value = destination,
                        onValueChange = { destination = it },
                        placeholder = "e.g. Goa, India",
                        testTag = MaestroIds.setupField("destination"),
                    )
                    SetupDateRangeField(
                        label = "Dates",
                        startIso = startDateIso,
                        endIso = endDateIso,
                        onStartChange = { startDateIso = it },
                        onEndChange = { endDateIso = it },
                        testTag = MaestroIds.setupDate("dates"),
                    )
                }
            }

            GroupLongFormDiamondDivider()

            GroupLongFormSectionCard(step = "02", title = "Dates, Budget & Split", accent = accent) {
                GroupLongFormGroupTitle("Plan the Practical Details")
                GroupLongFormDestinationField(
                    label = "Destination",
                    hint = "Where you're going",
                    value = destination,
                    onValueChange = { destination = it },
                    placeholder = "e.g. Goa, India",
                    testTag = MaestroIds.setupField("destinationPractical"),
                )
                SetupDateRangeField(
                    label = "Dates",
                    startIso = startDateIso,
                    endIso = endDateIso,
                    onStartChange = { startDateIso = it },
                    onEndChange = { endDateIso = it },
                    testTag = MaestroIds.setupDate("dates"),
                )
                HorizontalDivider(color = GroupSetupTheme.Border, thickness = 1.dp)
                GroupLongFormGroupTitle("Money")
                GroupLongFormPrefRow(
                    label = "Budget",
                    hint = "Expected total",
                    value = budget,
                    options = GroupBudgetUtils.PRESET_OPTIONS,
                    onValueChange = { budget = it },
                    editableGlyph = true,
                    testTag = MaestroIds.setupDropdown("budget"),
                )
                if (budget == GroupBudgetUtils.CUSTOM_OPTION) {
                    GroupBudgetCustomField(
                        value = budgetCustomAmount,
                        onValueChange = { budgetCustomAmount = it },
                        currencyCode = currency,
                    )
                }
                GroupLongFormPrefRow(
                    label = "Currency",
                    hint = "Default currency",
                    value = currency,
                    options = listOf("INR", "USD", "EUR"),
                    onValueChange = { currency = it },
                    testTag = MaestroIds.setupDropdown("currency"),
                )
                GroupLongFormPrefRow(
                    label = "Split style",
                    hint = "How costs are shared",
                    value = splitStyle,
                    options = listOf("Equal split", "By share", "Host pays", "Custom"),
                    onValueChange = { splitStyle = it },
                    testTag = MaestroIds.setupDropdown("splitStyle"),
                )
                HorizontalDivider(color = GroupSetupTheme.Border, thickness = 1.dp)
                GroupLongFormGroupTitle("Planning Preferences")
                GroupLongFormPrefRow(
                    label = "Multi-currency",
                    hint = "Track expenses in other currencies",
                    value = multiCurrency,
                    options = listOf("Enabled", "Disabled"),
                    onValueChange = { multiCurrency = it },
                    testTag = MaestroIds.setupDropdown("multiCurrency"),
                )
                GroupLongFormPrefRow(
                    label = "Payment rhythm",
                    hint = "How to settle",
                    value = paymentRhythm,
                    options = listOf("After each expense", "Weekly", "End of trip"),
                    onValueChange = { paymentRhythm = it },
                    testTag = MaestroIds.setupDropdown("paymentRhythm"),
                )
                GroupLongFormLocalOnlyNote()
            }

            GroupLongFormDiamondDivider()

            GroupLongFormSectionCard(step = "03", title = "People & Notifications", accent = accent) {
                GroupLongFormGroupTitle("Participants")
                GroupPeopleCard(
                    people = people,
                    palette = palette,
                    onShareQr = ::shareQr,
                    onWhatsApp = ::shareWhatsApp,
                    shareEnabled = true,
                    mintingInvite = mintingInvite,
                    inviteError = inviteError,
                    onRemove = { person ->
                        peopleEdited = true
                        people = people.filterNot {
                            it === person || (it.name == person.name && it.roleCode == person.roleCode)
                        }
                    },
                )
                HorizontalDivider(color = GroupSetupTheme.Border, thickness = 1.dp)
                GroupLongFormGroupTitle("Invitations")
                GroupLongFormPrefRow(
                    label = "Join approval",
                    hint = "Who can enter",
                    value = joinApproval,
                    options = listOf("Admin approval", "Anyone with link", "Invite only"),
                    onValueChange = { joinApproval = it },
                    testTag = MaestroIds.setupDropdown("joinApproval"),
                )
                GroupLongFormToggleRow(
                    title = "Notify me on changes",
                    subtitle = "Get alerts when people join or edit",
                    checked = notifyChanges,
                    onCheckedChange = { notifyChanges = it },
                    accent = accent,
                )
                HorizontalDivider(color = GroupSetupTheme.Border, thickness = 1.dp)
                GroupLongFormGroupTitle("Group Preferences")
                GroupLongFormPrefRow(
                    label = "Expense reminders",
                    hint = "Keep the group on track",
                    value = expenseReminders,
                    options = listOf("Enabled", "Disabled"),
                    onValueChange = { expenseReminders = it },
                    testTag = MaestroIds.setupDropdown("expenseReminders"),
                )
                GroupLongFormPrefRow(
                    label = "Photo reminders",
                    hint = "Capture shared memories",
                    value = photoReminders,
                    options = listOf("Enabled", "Disabled"),
                    onValueChange = { photoReminders = it },
                    testTag = MaestroIds.setupDropdown("photoReminders"),
                )
                GroupLongFormPrefRow(
                    label = "Update cadence",
                    hint = "How often to check in",
                    value = updateCadence,
                    options = listOf("Every week", "Daily", "Only on changes"),
                    onValueChange = { updateCadence = it },
                    editableGlyph = true,
                    testTag = MaestroIds.setupDropdown("updateCadence"),
                )
                GroupLongFormLocalOnlyNote()
            }

            GroupLongFormDiamondDivider()

            GroupLongFormSectionCard(step = "04", title = "Experience Summary", accent = accent) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(GroupSetupTheme.Card)
                        .border(1.dp, GroupSetupTheme.Border, RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    SummaryLine("Experience", name)
                    SummaryLine("Dates", SetupDateTimeUtils.formatDateRangeDisplay(startDateIso, endDateIso))
                    SummaryLine("Budget", GroupBudgetUtils.summaryLabel(budget, budgetCustomAmount))
                    SummaryLine("Members", buildMemberSummary(people))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            experienceChipLabel(selected).uppercase(),
                            color = GroupSetupTheme.TextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = PlusJakartaSans,
                        )
                        Text(
                            "SUMMARY",
                            color = GroupSetupTheme.TextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = PlusJakartaSans,
                        )
                    }
                }
                Text(
                    "${people.size} people · preferences on this device",
                    color = GroupSetupTheme.TextSecondary,
                    fontSize = 12.sp,
                    fontFamily = PlusJakartaSans,
                )
                GroupLongFormReadyBanner(
                    message = "Your shared experience is ready",
                    accent = accent,
                )
                if (error != null) {
                    Text(error, color = ColorError, fontSize = 12.sp, fontFamily = PlusJakartaSans)
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(palette.accentGradient)
                        .clickable(enabled = !submitting) {
                            if (name.isBlank()) return@clickable
                            val startAt = SetupDateTimeUtils.isoDateToStartInstant(startDateIso)
                            val endAt = SetupDateTimeUtils.isoDateToEndInstant(endDateIso ?: startDateIso)
                            val invitees = people
                                .filter { it.roleCode != "ORGANIZER" }
                                .map {
                                    CreateMomentParticipantBody(
                                        displayName = it.name,
                                        roleCode = it.roleCode,
                                        email = it.contactEmail,
                                        phone = it.contactPhone,
                                    )
                                }
                            val budgetAmount = GroupBudgetUtils.resolveBudgetAmount(budget, budgetCustomAmount)
                            val groupSetup = budgetAmount?.let {
                                GroupSetupBlockDto(
                                    budgetAmount = it,
                                    budgetCurrencyCode = currency,
                                    destinationText = destination.takeIf { d -> d.isNotBlank() },
                                )
                            }
                            createViewModel.submitGroupMoment(
                                section = "experience",
                                momentTypeCode = selectedCode,
                                title = name.trim(),
                                description = selected.defaultNotes.takeIf { it.isNotBlank() },
                                startAt = startAt,
                                endAt = endAt,
                                participants = invitees,
                                inviteCode = issuedInvite?.inviteCode,
                                groupSetup = groupSetup,
                                editingMomentId = editingMomentId,
                                onSuccess = { outcome ->
                                    scope.launch {
                                        accountRepo.patchMomentNotificationPreferences(
                                            outcome.momentId,
                                            notifyChanges,
                                            mapOf(
                                                "expenseReminders" to (expenseReminders.equals("Enabled", ignoreCase = true)),
                                                "photoReminders" to (photoReminders.equals("Enabled", ignoreCase = true)),
                                            ),
                                        )
                                        onCreated(outcome)
                                    }
                                },
                            )
                        }
                        .testTag(MaestroIds.GROUP_SETUP_SUBMIT)
                        .semantics {
                            role = Role.Button
                            contentDescription = "Activate Shared Experience"
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    if (submitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.height(22.dp),
                            color = GroupSetupTheme.CtaText,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text(
                            "Activate Shared Experience →",
                            color = GroupSetupTheme.CtaText,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = PlusJakartaSans,
                        )
                    }
                }
                Text(
                    "Modify, extend or change anytime.",
                    color = GroupSetupTheme.TextSecondary,
                    fontSize = 12.sp,
                    fontFamily = PlusJakartaSans,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun SummaryLine(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            label.uppercase(),
            color = GroupSetupTheme.TextSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = PlusJakartaSans,
        )
        Text(
            value,
            color = GroupSetupTheme.TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = PlusJakartaSans,
        )
    }
}

private val ColorError = androidx.compose.ui.graphics.Color(0xFFEF4444)
