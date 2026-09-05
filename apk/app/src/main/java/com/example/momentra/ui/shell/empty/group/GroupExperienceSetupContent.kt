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
import com.example.momentra.data.api.GroupSetupBudgetDto
import com.example.momentra.data.api.GroupSetupPlaceDto
import kotlinx.coroutines.launch
import com.example.momentra.domain.CreateMomentOutcome
import com.example.momentra.data.repository.AccountRepository
import com.example.momentra.data.repository.GroupSliceRepository
import com.example.momentra.ui.create.MomentCreateViewModel
import com.example.momentra.ui.setup.SetupDateTimeUtils
import com.example.momentra.ui.setup.SetupDateRangeField
import com.example.momentra.ui.setup.SetupTitleField
import com.example.momentra.ui.shell.group.shared.GroupTravelCurrencyCatalog
import com.example.momentra.ui.shell.maestro.MaestroIds
import com.example.momentra.ui.theme.PlusJakartaSans

private data class ExperiencePlaceDraft(
    val label: String = "",
    val startIso: String? = null,
    val endIso: String? = null,
)

private data class ExtraBudgetDraft(
    val currencyCode: String = "USD",
    val amount: String = "",
)

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
    var places by remember(selectedCode) {
        mutableStateOf(listOf(ExperiencePlaceDraft()))
    }
    var primaryGoal by remember(selectedCode) {
        mutableStateOf("Enjoy time together")
    }
    var budget by remember(selectedCode) { mutableStateOf("₹80,000") }
    var budgetCustomAmount by remember(selectedCode) { mutableStateOf("") }
    var currency by remember(selectedCode) { mutableStateOf("INR") }
    var extraBudgets by remember(selectedCode) { mutableStateOf<List<ExtraBudgetDraft>>(emptyList()) }
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
    var editingMomentStatus by remember { mutableStateOf<String?>(null) }
    var issuedInvite by remember { mutableStateOf<GroupInviteDto?>(null) }
    var mintingInvite by remember { mutableStateOf(false) }
    var inviteError by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val accountRepo = remember { AccountRepository() }
    val groupRepo = remember { GroupSliceRepository() }

    LaunchedEffect(editingMomentId) {
        val mid = editingMomentId ?: return@LaunchedEffect
        createViewModel.getGroupSetupPrefill(mid)?.let { prefill ->
            editingMomentStatus = prefill.status
            prefill.title?.takeIf { it.isNotBlank() }?.let { name = it }
            prefill.primaryGoal?.let { primaryGoal = it }
            prefill.multiCurrencyEnabled?.let { multiCurrency = if (it) "Enabled" else "Disabled" }
            prefill.splitStyle?.let { code ->
                splitStyle = when (code.uppercase()) {
                    "SHARES" -> "By share"
                    "POOLED" -> "Host pays"
                    "EXACT", "PERCENTAGE" -> "Custom"
                    else -> "Equal split"
                }
            }
            val placeRows = prefill.places.orEmpty()
                .map {
                    ExperiencePlaceDraft(
                        label = it.label.orEmpty(),
                        startIso = it.startAt?.take(10),
                        endIso = it.endAt?.take(10),
                    )
                }
                .ifEmpty {
                    listOf(
                        ExperiencePlaceDraft(
                            label = prefill.destinationText.orEmpty(),
                            startIso = prefill.startAt?.take(10),
                            endIso = prefill.endAt?.take(10),
                        ),
                    )
                }
            places = placeRows
            val budgetRows = prefill.budgets.orEmpty()
            val primary = budgetRows.firstOrNull { it.isPrimary == true } ?: budgetRows.firstOrNull()
            if (primary != null) {
                currency = primary.currencyCode
                val display = GroupBudgetUtils.formatApiAmountForDisplay(primary.amount, primary.currencyCode)
                if (display in GroupBudgetUtils.PRESET_OPTIONS) {
                    budget = display
                    budgetCustomAmount = ""
                } else {
                    budget = GroupBudgetUtils.CUSTOM_OPTION
                    budgetCustomAmount = GroupBudgetUtils.formatCustomAmountInput(primary.amount)
                }
                extraBudgets = budgetRows
                    .filter { it !== primary && it.currencyCode != primary.currencyCode }
                    .map {
                        ExtraBudgetDraft(
                            currencyCode = it.currencyCode,
                            amount = GroupBudgetUtils.formatCustomAmountInput(it.amount),
                        )
                    }
            }
        }
        accountRepo.getMomentNotificationPreferences(mid).onSuccess { prefs ->
            notifyChanges = prefs.notifyOnChanges
            val rem = prefs.reminderPreferences.orEmpty()
            rem["expenseReminders"]?.let { expenseReminders = if (it) "Enabled" else "Disabled" }
            rem["photoReminders"]?.let { photoReminders = if (it) "Enabled" else "Disabled" }
        }
        if (editingMomentId != null && places.none { it.label.isNotBlank() }) {
            groupRepo.getFinance(mid).onSuccess { facet ->
                val totals = facet.payload?.totals.orEmpty()
                val total = totals.firstOrNull {
                    it.budgetTotal.toDoubleOrNull()?.let { v -> v > 0 } == true
                } ?: totals.firstOrNull()
                val raw = total?.budgetTotal?.takeIf { (it.toDoubleOrNull() ?: 0.0) > 0 } ?: return@onSuccess
                val display = GroupBudgetUtils.formatApiAmountForDisplay(raw, total!!.currencyCode)
                currency = total.currencyCode
                if (display in GroupBudgetUtils.PRESET_OPTIONS) {
                    budget = display
                    budgetCustomAmount = ""
                } else {
                    budget = GroupBudgetUtils.CUSTOM_OPTION
                    budgetCustomAmount = GroupBudgetUtils.formatCustomAmountInput(raw)
                }
                extraBudgets = totals
                    .filter { it.currencyCode != currency }
                    .mapNotNull { row ->
                        val amt = row.budgetTotal.takeIf { (it.toDoubleOrNull() ?: 0.0) > 0 } ?: return@mapNotNull null
                        ExtraBudgetDraft(
                            currencyCode = row.currencyCode,
                            amount = GroupBudgetUtils.formatCustomAmountInput(amt),
                        )
                    }
            }
        }
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
                        .clickable {
                            if (
                                !editingMomentId.isNullOrBlank() &&
                                editingMomentStatus.equals("DRAFT", ignoreCase = true)
                            ) {
                                createViewModel.discardMomentDraft(editingMomentId) { onBack() }
                            } else {
                                onBack()
                            }
                        }
                        .semantics {
                            role = Role.Button
                            contentDescription = "Discard draft"
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text("×", color = GroupSetupTheme.TextSecondary, fontSize = 16.sp, fontFamily = PlusJakartaSans)
                    Text(
                        "Discard draft",
                        color = GroupSetupTheme.TextSecondary,
                        fontSize = 14.sp,
                        fontFamily = PlusJakartaSans,
                    )
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
                    Text(
                        "Destinations (${places.size} places)",
                        color = GroupSetupTheme.TextSecondary,
                        fontSize = 12.sp,
                        fontFamily = PlusJakartaSans,
                    )
                    places.forEachIndexed { index, place ->
                        GroupLongFormDestinationField(
                            label = "Place ${index + 1}",
                            hint = "City or region",
                            value = place.label,
                            onValueChange = { v ->
                                places = places.toMutableList().also {
                                    it[index] = it[index].copy(label = v)
                                }
                            },
                            placeholder = "e.g. Goa, India",
                            testTag = MaestroIds.setupField("place_$index"),
                        )
                        SetupDateRangeField(
                            label = "Dates",
                            startIso = place.startIso,
                            endIso = place.endIso,
                            onStartChange = { v ->
                                places = places.toMutableList().also {
                                    it[index] = it[index].copy(startIso = v)
                                }
                            },
                            onEndChange = { v ->
                                places = places.toMutableList().also {
                                    it[index] = it[index].copy(endIso = v)
                                }
                            },
                            testTag = MaestroIds.setupDate("placeDates_$index"),
                        )
                    }
                    Text(
                        "+ Add another place",
                        color = accent,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = PlusJakartaSans,
                        modifier = Modifier
                            .clickable { places = places + ExperiencePlaceDraft() }
                            .padding(vertical = 8.dp),
                    )
                    // Keep legacy single destination in sync with first place for summary
                    LaunchedEffect(places) {
                        destination = places.firstOrNull()?.label.orEmpty()
                        startDateIso = places.firstOrNull()?.startIso
                        endDateIso = places.lastOrNull { it.endIso != null }?.endIso
                            ?: places.firstOrNull()?.endIso
                    }
                }
            }

            GroupLongFormDiamondDivider()

            GroupLongFormSectionCard(step = "02", title = "Dates, Budget & Split", accent = accent) {
                GroupLongFormGroupTitle("Plan the Practical Details")
                Text(
                    places.firstOrNull()?.label?.takeIf { it.isNotBlank() } ?: "Add places in section 01",
                    color = GroupSetupTheme.TextSecondary,
                    fontSize = 13.sp,
                    fontFamily = PlusJakartaSans,
                )
                HorizontalDivider(color = GroupSetupTheme.Border, thickness = 1.dp)
                GroupLongFormGroupTitle("Money")
                GroupLongFormPrefRow(
                    label = "Primary budget",
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
                    label = "Primary currency",
                    hint = "Default currency",
                    value = currency,
                    options = GroupTravelCurrencyCatalog.codes,
                    onValueChange = { currency = it },
                    testTag = MaestroIds.setupDropdown("currency"),
                )
                if (multiCurrency.equals("Enabled", ignoreCase = true)) {
                    extraBudgets.forEachIndexed { index, row ->
                        GroupLongFormPrefRow(
                            label = "Currency ${index + 2}",
                            hint = GroupTravelCurrencyCatalog.display(row.currencyCode),
                            value = row.currencyCode,
                            options = GroupTravelCurrencyCatalog.codes.filter { it != currency },
                            onValueChange = { code ->
                                extraBudgets = extraBudgets.toMutableList().also {
                                    it[index] = it[index].copy(currencyCode = code)
                                }
                            },
                            testTag = MaestroIds.setupDropdown("extraCurrency_$index"),
                        )
                        GroupBudgetCustomField(
                            value = row.amount,
                            onValueChange = { amt ->
                                extraBudgets = extraBudgets.toMutableList().also {
                                    it[index] = it[index].copy(amount = amt)
                                }
                            },
                            currencyCode = row.currencyCode,
                        )
                    }
                    Text(
                        "+ Add currency",
                        color = accent,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = PlusJakartaSans,
                        modifier = Modifier
                            .clickable {
                                val next = GroupTravelCurrencyCatalog.codes.firstOrNull { c ->
                                    c != currency && extraBudgets.none { it.currencyCode == c }
                                } ?: "USD"
                                extraBudgets = extraBudgets + ExtraBudgetDraft(currencyCode = next)
                            }
                            .padding(vertical = 8.dp),
                    )
                }
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
                    options = listOf("After each expense", "Weekly", "End of trip", "Manual expense"),
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
                    SummaryLine(
                        "Destinations",
                        places.filter { it.label.isNotBlank() }.joinToString(" → ") { it.label }
                            .ifBlank { destination.ifBlank { "—" } },
                    )
                    SummaryLine("Dates", SetupDateTimeUtils.formatDateRangeDisplay(startDateIso, endDateIso))
                    SummaryLine(
                        "Budget",
                        buildString {
                            append(GroupBudgetUtils.summaryLabel(budget, budgetCustomAmount))
                            append(" ")
                            append(currency)
                            if (multiCurrency.equals("Enabled", ignoreCase = true) && extraBudgets.isNotEmpty()) {
                                append(" + ")
                                append(extraBudgets.joinToString(" + ") { it.currencyCode })
                            }
                        },
                    )
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
                fun buildGroupSetupBlock(): GroupSetupBlockDto? {
                    val primaryAmount = GroupBudgetUtils.resolveBudgetAmount(budget, budgetCustomAmount)
                        ?: return null
                    val placeDtos = places
                        .filter { it.label.isNotBlank() }
                        .map {
                            GroupSetupPlaceDto(
                                label = it.label.trim(),
                                startAt = SetupDateTimeUtils.isoDateToStartInstant(it.startIso),
                                endAt = SetupDateTimeUtils.isoDateToEndInstant(it.endIso ?: it.startIso),
                            )
                        }
                        .ifEmpty {
                            destination.takeIf { it.isNotBlank() }?.let {
                                listOf(
                                    GroupSetupPlaceDto(
                                        label = it.trim(),
                                        startAt = SetupDateTimeUtils.isoDateToStartInstant(startDateIso),
                                        endAt = SetupDateTimeUtils.isoDateToEndInstant(endDateIso ?: startDateIso),
                                    ),
                                )
                            }.orEmpty()
                        }
                    val budgetDtos = buildList {
                        add(
                            GroupSetupBudgetDto(
                                currencyCode = currency,
                                amount = primaryAmount,
                                isPrimary = true,
                            ),
                        )
                        if (multiCurrency.equals("Enabled", ignoreCase = true)) {
                            extraBudgets.forEach { row ->
                                val amt = row.amount.filter { it.isDigit() || it == '.' }
                                    .takeIf { it.isNotBlank() } ?: return@forEach
                                if (row.currencyCode.equals(currency, ignoreCase = true)) return@forEach
                                add(
                                    GroupSetupBudgetDto(
                                        currencyCode = row.currencyCode,
                                        amount = amt,
                                        isPrimary = false,
                                    ),
                                )
                            }
                        }
                    }
                    val apiSplit = when (splitStyle) {
                        "By share" -> "SHARES"
                        "Host pays" -> "POOLED"
                        "Custom" -> "EXACT"
                        else -> "EQUAL"
                    }
                    return GroupSetupBlockDto(
                        budgetAmount = primaryAmount,
                        budgetCurrencyCode = currency,
                        destinationText = placeDtos.firstOrNull()?.label
                            ?: destination.takeIf { it.isNotBlank() },
                        places = placeDtos.takeIf { it.isNotEmpty() },
                        budgets = budgetDtos,
                        multiCurrencyEnabled = multiCurrency.equals("Enabled", ignoreCase = true),
                        splitStyle = apiSplit,
                        primaryGoal = primaryGoal,
                        reminderPreferences = mapOf(
                            "expenseReminders" to (expenseReminders.equals("Enabled", ignoreCase = true)),
                            "photoReminders" to (photoReminders.equals("Enabled", ignoreCase = true)),
                        ),
                        setupPreferences = mapOf(
                            "paymentRhythm" to paymentRhythm,
                            "joinApproval" to joinApproval,
                            "updateCadence" to updateCadence,
                        ),
                    )
                }
                fun submitExperience(status: String) {
                    if (name.isBlank()) return
                    val startAt = SetupDateTimeUtils.isoDateToStartInstant(
                        places.firstOrNull()?.startIso ?: startDateIso,
                    )
                    val endAt = SetupDateTimeUtils.isoDateToEndInstant(
                        places.mapNotNull { it.endIso ?: it.startIso }.lastOrNull()
                            ?: endDateIso
                            ?: startDateIso,
                    )
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
                    createViewModel.submitGroupMoment(
                        section = "experience",
                        momentTypeCode = selectedCode,
                        title = name.trim(),
                        description = selected.defaultNotes.takeIf { it.isNotBlank() },
                        startAt = startAt,
                        endAt = endAt,
                        participants = invitees,
                        inviteCode = issuedInvite?.inviteCode,
                        groupSetup = buildGroupSetupBlock(),
                        editingMomentId = editingMomentId,
                        editingMomentStatus = editingMomentStatus,
                        status = status,
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
                                if (status == "DRAFT" && editingMomentId == null) {
                                    // Stay on setup for draft; still notify host shell
                                }
                                onCreated(outcome)
                            }
                        },
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(palette.accentGradient)
                        .clickable(enabled = !submitting) { submitExperience("ACTIVE") }
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .border(1.dp, GroupSetupTheme.Border, RoundedCornerShape(14.dp))
                            .clickable(enabled = !submitting) { submitExperience("DRAFT") }
                            .testTag(MaestroIds.setupField("saveDraft"))
                            .semantics {
                                role = Role.Button
                                contentDescription = "Save draft"
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "Save draft",
                            color = GroupSetupTheme.TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = PlusJakartaSans,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable(enabled = !submitting, onClick = onBack)
                            .semantics {
                                role = Role.Button
                                contentDescription = "Schedule later"
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "Schedule later",
                            color = GroupSetupTheme.TextSecondary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
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
