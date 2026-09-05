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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.data.api.CreateMomentParticipantBody
import com.example.momentra.data.api.GroupInviteDto
import com.example.momentra.data.api.GroupSetupBlockDto
import com.example.momentra.data.repository.AccountRepository
import com.example.momentra.data.repository.GroupSliceRepository
import com.example.momentra.domain.CreateMomentOutcome
import com.example.momentra.ui.create.MomentCreateViewModel
import com.example.momentra.ui.setup.SetupDateField
import com.example.momentra.ui.setup.SetupDateTimeUtils
import com.example.momentra.ui.setup.SetupTitleField
import com.example.momentra.ui.shell.maestro.MaestroIds
import com.example.momentra.ui.theme.PlusJakartaSans
import kotlinx.coroutines.launch

/** Figma 575:9919 — Shared Purchase setup (4 variants). */
@Composable
fun GroupPurchaseSetupContent(
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
    GroupSectionLongFormFlow(
        variant = GroupSetupCatalog.purchase,
        family = GroupLongFormFamily.PURCHASE,
        onBack = onBack,
        onCreated = onCreated,
        createViewModel = createViewModel,
        submitting = submitting,
        error = error,
        onSetupTypeChanged = onSetupTypeChanged,
        modifier = modifier,
        editingMomentId = editingMomentId,
        initialTitle = initialTitle,
        initialTypeCode = initialTypeCode,
    )
}

/** Figma 575:10567 — Shared Living setup (4 variants). */
@Composable
fun GroupLivingSetupContent(
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
    GroupSectionLongFormFlow(
        variant = GroupSetupCatalog.living,
        family = GroupLongFormFamily.LIVING,
        onBack = onBack,
        onCreated = onCreated,
        createViewModel = createViewModel,
        submitting = submitting,
        error = error,
        onSetupTypeChanged = onSetupTypeChanged,
        modifier = modifier,
        editingMomentId = editingMomentId,
        initialTitle = initialTitle,
        initialTypeCode = initialTypeCode,
    )
}

private enum class GroupLongFormFamily { PURCHASE, LIVING }

@Composable
private fun GroupSectionLongFormFlow(
    variant: GroupSetupVariant,
    family: GroupLongFormFamily,
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
    val types = variant.types
    var selectedCode by remember {
        mutableStateOf(
            initialTypeCode?.takeIf { code -> types.any { it.code == code } } ?: types.first().code,
        )
    }
    val selected = types.first { it.code == selectedCode }
    var name by remember(selectedCode) {
        mutableStateOf(initialTitle?.takeIf { it.isNotBlank() } ?: selected.defaultName)
    }
    var tagline by remember(selectedCode) {
        mutableStateOf(if (family == GroupLongFormFamily.PURCHASE) "Buying together" else "Living well together")
    }
    var profile by remember(selectedCode) {
        mutableStateOf(if (family == GroupLongFormFamily.PURCHASE) "Shared purchase" else "Shared home")
    }
    var itemOrGoal by remember(selectedCode) {
        mutableStateOf(if (family == GroupLongFormFamily.PURCHASE) "What everyone is funding" else "What matters most at home")
    }
    var ownership by remember(selectedCode) { mutableStateOf("Shared equally") }
    var targetDateIso by remember(selectedCode) { mutableStateOf<String?>(null) }
    var amount by remember(selectedCode) { mutableStateOf("₹25,000") }
    var amountCustom by remember(selectedCode) { mutableStateOf("") }
    var currency by remember(selectedCode) { mutableStateOf("INR") }
    var ownershipSplit by remember(selectedCode) { mutableStateOf("Equal") }
    var paymentPlan by remember(selectedCode) { mutableStateOf("Monthly") }
    var deadline by remember(selectedCode) { mutableStateOf("Before target date") }
    var multiCurrency by remember(selectedCode) { mutableStateOf("Enabled") }
    var approvalRule by remember(selectedCode) { mutableStateOf("Admin confirms") }
    var paymentReminders by remember(selectedCode) { mutableStateOf("Enabled") }
    var decisionCheckIn by remember(selectedCode) { mutableStateOf("On major changes") }
    var reviewCadence by remember(selectedCode) { mutableStateOf("Every week") }
    // Living-specific
    var residents by remember(selectedCode) { mutableStateOf("4 people") }
    var moveInDateIso by remember(selectedCode) { mutableStateOf<String?>(null) }
    var rentSplit by remember(selectedCode) { mutableStateOf("Equal") }
    var choreStyle by remember(selectedCode) { mutableStateOf("Rotate weekly") }
    var billRhythm by remember(selectedCode) { mutableStateOf("Monthly") }
    var houseRules by remember(selectedCode) { mutableStateOf("Consensus") }
    var quietHours by remember(selectedCode) { mutableStateOf("10pm–7am") }
    var guestPolicy by remember(selectedCode) { mutableStateOf("Ask first") }
    var joinApproval by remember(selectedCode) { mutableStateOf("Admin approval") }
    var billReminders by remember(selectedCode) { mutableStateOf("Enabled") }
    var choreReminders by remember(selectedCode) { mutableStateOf("Enabled") }
    var houseReview by remember(selectedCode) { mutableStateOf("Every month") }
    var people by remember(selectedCode) { mutableStateOf(defaultGroupPeople(selectedCode)) }
    var peopleEdited by remember(selectedCode) { mutableStateOf(false) }
    var issuedInvite by remember { mutableStateOf<GroupInviteDto?>(null) }
    var mintingInvite by remember { mutableStateOf(false) }
    var inviteError by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val accountRepo = remember { AccountRepository() }
    val groupRepo = remember { GroupSliceRepository() }

    LaunchedEffect(editingMomentId) {
        val mid = editingMomentId ?: return@LaunchedEffect
        accountRepo.getMomentNotificationPreferences(mid).onSuccess { prefs ->
            val rem = prefs.reminderPreferences.orEmpty()
            rem["billReminders"]?.let { billReminders = if (it) "Enabled" else "Disabled" }
            rem["choreReminders"]?.let { choreReminders = if (it) "Enabled" else "Disabled" }
            rem["paymentReminders"]?.let { paymentReminders = if (it) "Enabled" else "Disabled" }
        }
        groupRepo.getFinance(mid).onSuccess { facet ->
            val totals = facet.payload?.totals.orEmpty()
            val total = totals.firstOrNull {
                it.budgetTotal.toDoubleOrNull()?.let { v -> v > 0 } == true
            } ?: totals.firstOrNull()
            val raw = total?.budgetTotal?.takeIf { (it.toDoubleOrNull() ?: 0.0) > 0 } ?: return@onSuccess
            val display = GroupBudgetUtils.formatApiAmountForDisplay(raw, total!!.currencyCode)
            val options = if (family == GroupLongFormFamily.PURCHASE) {
                GroupBudgetUtils.PURCHASE_AMOUNT_OPTIONS
            } else {
                GroupBudgetUtils.LIVING_BUDGET_OPTIONS
            }
            currency = total.currencyCode
            if (display in options) {
                amount = display
                amountCustom = ""
            } else {
                amount = GroupBudgetUtils.CUSTOM_OPTION
                amountCustom = GroupBudgetUtils.formatCustomAmountInput(raw)
            }
        }
    }

    val palette = selected.palette
    val accent = palette.accent
    val chipLabel = if (family == GroupLongFormFamily.PURCHASE) ::purchaseChipLabel else ::livingChipLabel
    val setupsTitle = if (family == GroupLongFormFamily.PURCHASE) "Purchase setups" else "Living setups"
    val heroTitle = if (family == GroupLongFormFamily.PURCHASE) {
        "Set up Shared Purchase"
    } else {
        "Set up Shared Living"
    }
    val heroSubtitle = if (family == GroupLongFormFamily.PURCHASE) {
        "Pool funds, buy together, and stay aligned on contributions."
    } else {
        "Coordinate home life — bills, chores, and house rhythm — together."
    }
    val readyMsg = if (family == GroupLongFormFamily.PURCHASE) {
        "Your shared purchase is ready"
    } else {
        "Your shared living space is ready"
    }

    suspend fun ensureInvite(): GroupInviteDto? {
        issuedInvite?.let { return it }
        mintingInvite = true
        inviteError = null
        val minted = createViewModel.mintGroupInvite(
            title = name.trim().ifBlank { selected.defaultName },
            momentTypeCode = selectedCode,
            section = variant.section,
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
                    Text("×", color = GroupSetupTheme.TextSecondary, fontSize = 16.sp)
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
                title = heroTitle,
                subtitle = heroSubtitle,
                accent = accent,
                iconRes = selected.iconRes,
            )

            GroupLongFormTypeChipStrip(
                title = setupsTitle,
                types = types,
                selectedCode = selectedCode,
                onSelect = { opt ->
                    selectedCode = opt.code
                    name = opt.defaultName
                    people = defaultGroupPeople(opt.code)
                    peopleEdited = false
                },
                shortLabel = chipLabel,
            )

            GroupLongFormDiamondDivider()

            if (family == GroupLongFormFamily.PURCHASE) {
                GroupLongFormSectionCard(step = "01", title = "Purchase Basics", accent = accent) {
                    SetupTitleField(
                        label = selected.nameLabel,
                        value = name,
                        onValueChange = { name = it },
                        placeholder = selected.defaultName,
                        testTag = MaestroIds.SETUP_TITLE,
                    )
                    GroupLongFormSubsectionTitle("Your Purchase")
                    GroupLongFormPrefRow(
                        label = "Purchase profile",
                        hint = "What are you buying?",
                        value = profile,
                        options = listOf("Shared purchase", "Gift pool", "Shared asset", "Custom"),
                        onValueChange = { profile = it },
                        testTag = MaestroIds.setupDropdown("purchaseProfile"),
                    )
                    GroupLongFormPrefRow(
                        label = "Item or goal",
                        hint = "What is the purchase?",
                        value = itemOrGoal,
                        options = listOf("What everyone is funding", "Camera kit", "Sofa set", "Trip fund"),
                        onValueChange = { itemOrGoal = it },
                        testTag = MaestroIds.setupDropdown("itemOrGoal"),
                    )
                    GroupLongFormSubsectionTitle("Purchase Details")
                    GroupLongFormPrefRow(
                        label = "Ownership",
                        hint = "How ownership starts",
                        value = ownership,
                        options = listOf("Shared equally", "By contribution", "Named owner"),
                        onValueChange = { ownership = it },
                        testTag = MaestroIds.setupDropdown("ownership"),
                    )
                    SetupDateField(
                        label = "Target date",
                        isoValue = targetDateIso,
                        onIsoChange = { targetDateIso = it },
                        testTag = MaestroIds.setupDate("targetDate"),
                    )
                }

                GroupLongFormDiamondDivider()

                GroupLongFormSectionCard(step = "02", title = "Goal, Amount & Contributions", accent = accent) {
                    GroupLongFormGroupTitle("Funding")
                    GroupLongFormPrefRow(
                        label = "Item or goal",
                        hint = "What everyone is funding",
                        value = itemOrGoal,
                        options = listOf("What everyone is funding", "Camera kit", "Sofa set"),
                        onValueChange = { itemOrGoal = it },
                        editableGlyph = true,
                        testTag = MaestroIds.setupDropdown("itemOrGoal"),
                    )
                    GroupLongFormPrefRow(
                        label = "Expected amount",
                        hint = "Estimated total",
                        value = amount,
                        options = GroupBudgetUtils.PURCHASE_AMOUNT_OPTIONS,
                        onValueChange = { amount = it },
                        editableGlyph = true,
                        testTag = MaestroIds.setupDropdown("amount"),
                    )
                    if (amount == GroupBudgetUtils.CUSTOM_OPTION) {
                        GroupBudgetCustomField(
                            value = amountCustom,
                            onValueChange = { amountCustom = it },
                            currencyCode = currency,
                        )
                    }
                    GroupLongFormGroupTitle("Money")
                    GroupLongFormPrefRow(
                        label = "Currency",
                        hint = "Default currency",
                        value = currency,
                        options = listOf("INR", "USD", "EUR"),
                        onValueChange = { currency = it },
                        testTag = MaestroIds.setupDropdown("currency"),
                    )
                    GroupLongFormPrefRow(
                        label = "Ownership split",
                        hint = "How ownership is divided",
                        value = ownershipSplit,
                        options = listOf("Equal", "By %", "Custom"),
                        onValueChange = { ownershipSplit = it },
                        testTag = MaestroIds.setupDropdown("ownershipSplit"),
                    )
                    GroupLongFormPrefRow(
                        label = "Payment plan",
                        hint = "How people contribute",
                        value = paymentPlan,
                        options = listOf("Monthly", "One-time", "Flexible"),
                        onValueChange = { paymentPlan = it },
                        testTag = MaestroIds.setupDropdown("paymentPlan"),
                    )
                    GroupLongFormGroupTitle("Planning Preferences")
                    GroupLongFormPrefRow(
                        label = "Deadline",
                        hint = "When the goal should be met",
                        value = deadline,
                        options = listOf("Before target date", "Flexible", "Hard deadline"),
                        onValueChange = { deadline = it },
                        testTag = MaestroIds.setupDropdown("deadline"),
                    )
                    GroupLongFormPrefRow(
                        label = "Multi-currency",
                        hint = "Allow other currencies",
                        value = multiCurrency,
                        options = listOf("Enabled", "Disabled"),
                        onValueChange = { multiCurrency = it },
                        testTag = MaestroIds.setupDropdown("multiCurrency"),
                    )
                    GroupLongFormLocalOnlyNote()
                }

                GroupLongFormDiamondDivider()

                GroupLongFormSectionCard(step = "03", title = "Members & Ownership", accent = accent) {
                    GroupLongFormGroupTitle("Members")
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
                    GroupLongFormGroupTitle("Ownership rules")
                    GroupLongFormPrefRow(
                        label = "Approval rule",
                        hint = "Who confirms changes",
                        value = approvalRule,
                        options = listOf("Admin confirms", "Majority", "Anyone"),
                        onValueChange = { approvalRule = it },
                        testTag = MaestroIds.setupDropdown("approvalRule"),
                    )
                    GroupLongFormPrefRow(
                        label = "Payment reminders",
                        hint = "Keep contributions visible",
                        value = paymentReminders,
                        options = listOf("Enabled", "Disabled"),
                        onValueChange = { paymentReminders = it },
                        testTag = MaestroIds.setupDropdown("paymentReminders"),
                    )
                    GroupLongFormPrefRow(
                        label = "Decision check-in",
                        hint = "Confirm major changes",
                        value = decisionCheckIn,
                        options = listOf("On major changes", "Always", "Never"),
                        onValueChange = { decisionCheckIn = it },
                        testTag = MaestroIds.setupDropdown("decisionCheckIn"),
                    )
                    GroupLongFormPrefRow(
                        label = "Review cadence",
                        hint = "How often to review",
                        value = reviewCadence,
                        options = listOf("Every week", "Every month", "On demand"),
                        onValueChange = { reviewCadence = it },
                        editableGlyph = true,
                        testTag = MaestroIds.setupDropdown("reviewCadence"),
                    )
                    GroupLongFormLocalOnlyNote()
                }
            } else {
                GroupLongFormSectionCard(step = "01", title = "Home Basics", accent = accent) {
                    SetupTitleField(
                        label = selected.nameLabel,
                        value = name,
                        onValueChange = { name = it },
                        placeholder = selected.defaultName,
                        testTag = MaestroIds.SETUP_TITLE,
                    )
                    GroupLongFormSubsectionTitle("Your Home")
                    GroupLongFormPrefRow(
                        label = "Primary goal",
                        hint = "What matters most at home?",
                        value = itemOrGoal,
                        options = listOf(
                            "What matters most at home",
                            "Fair chores",
                            "Shared bills",
                            "Peaceful living",
                        ),
                        onValueChange = { itemOrGoal = it },
                        testTag = MaestroIds.setupDropdown("primaryGoal"),
                    )
                    GroupLongFormSubsectionTitle("Home Details")
                    GroupLongFormPrefRow(
                        label = "Residents",
                        hint = "Who lives here",
                        value = residents,
                        options = listOf("2 people", "3 people", "4 people", "5+"),
                        onValueChange = { residents = it },
                        testTag = MaestroIds.setupDropdown("residents"),
                    )
                    SetupDateField(
                        label = "Move-in date",
                        isoValue = moveInDateIso,
                        onIsoChange = { moveInDateIso = it },
                        testTag = MaestroIds.setupDate("moveIn"),
                    )
                }

                GroupLongFormDiamondDivider()

                GroupLongFormSectionCard(step = "02", title = "Budget, Responsibilities & Preferences", accent = accent) {
                    GroupLongFormGroupTitle("Money")
                    GroupLongFormPrefRow(
                        label = "Monthly budget",
                        hint = "Shared household spending",
                        value = amount,
                        options = GroupBudgetUtils.LIVING_BUDGET_OPTIONS,
                        onValueChange = { amount = it },
                        editableGlyph = true,
                        testTag = MaestroIds.setupDropdown("budget"),
                    )
                    if (amount == GroupBudgetUtils.CUSTOM_OPTION) {
                        GroupBudgetCustomField(
                            value = amountCustom,
                            onValueChange = { amountCustom = it },
                            currencyCode = currency,
                        )
                    }
                    GroupLongFormPrefRow(
                        label = "Rent split",
                        hint = "How rent is divided",
                        value = rentSplit,
                        options = listOf("Equal", "By room", "Custom"),
                        onValueChange = { rentSplit = it },
                        testTag = MaestroIds.setupDropdown("rentSplit"),
                    )
                    GroupLongFormPrefRow(
                        label = "Bill rhythm",
                        hint = "When shared bills settle",
                        value = billRhythm,
                        options = listOf("Monthly", "Weekly", "As due"),
                        onValueChange = { billRhythm = it },
                        testTag = MaestroIds.setupDropdown("billRhythm"),
                    )
                    GroupLongFormGroupTitle("Responsibilities")
                    GroupLongFormPrefRow(
                        label = "Chore style",
                        hint = "How tasks are shared",
                        value = choreStyle,
                        options = listOf("Rotate weekly", "Assigned", "Volunteer"),
                        onValueChange = { choreStyle = it },
                        testTag = MaestroIds.setupDropdown("choreStyle"),
                    )
                    GroupLongFormPrefRow(
                        label = "House rules",
                        hint = "How agreements are made",
                        value = houseRules,
                        options = listOf("Consensus", "Majority", "Host decides"),
                        onValueChange = { houseRules = it },
                        testTag = MaestroIds.setupDropdown("houseRules"),
                    )
                    GroupLongFormPrefRow(
                        label = "Quiet hours",
                        hint = "Protect rest and focus",
                        value = quietHours,
                        options = listOf("10pm–7am", "11pm–8am", "None"),
                        onValueChange = { quietHours = it },
                        testTag = MaestroIds.setupDropdown("quietHours"),
                    )
                    GroupLongFormPrefRow(
                        label = "Guest policy",
                        hint = "How visits are handled",
                        value = guestPolicy,
                        options = listOf("Ask first", "Anytime", "Weekends only"),
                        onValueChange = { guestPolicy = it },
                        testTag = MaestroIds.setupDropdown("guestPolicy"),
                    )
                    GroupLongFormLocalOnlyNote()
                }

                GroupLongFormDiamondDivider()

                GroupLongFormSectionCard(step = "03", title = "Residents & Invitations", accent = accent) {
                    GroupLongFormGroupTitle("Residents")
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
                        hint = "Who can join the home",
                        value = joinApproval,
                        options = listOf("Admin approval", "Anyone with link", "Invite only"),
                        onValueChange = { joinApproval = it },
                        testTag = MaestroIds.setupDropdown("joinApproval"),
                    )
                    GroupLongFormPrefRow(
                        label = "Bill reminders",
                        hint = "Keep shared costs visible",
                        value = billReminders,
                        options = listOf("Enabled", "Disabled"),
                        onValueChange = { billReminders = it },
                        testTag = MaestroIds.setupDropdown("billReminders"),
                    )
                    GroupLongFormPrefRow(
                        label = "Chore reminders",
                        hint = "Keep responsibilities fair",
                        value = choreReminders,
                        options = listOf("Enabled", "Disabled"),
                        onValueChange = { choreReminders = it },
                        testTag = MaestroIds.setupDropdown("choreReminders"),
                    )
                    GroupLongFormPrefRow(
                        label = "House review",
                        hint = "How often to check in",
                        value = houseReview,
                        options = listOf("Every month", "Every week", "On demand"),
                        onValueChange = { houseReview = it },
                        editableGlyph = true,
                        testTag = MaestroIds.setupDropdown("houseReview"),
                    )
                    GroupLongFormLocalOnlyNote()
                }
            }

            GroupLongFormDiamondDivider()

            val summaryTitle = if (family == GroupLongFormFamily.PURCHASE) "Purchase Summary" else "Living Summary"
            GroupLongFormSectionCard(step = "04", title = summaryTitle, accent = accent) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(GroupSetupTheme.Card)
                        .border(1.dp, GroupSetupTheme.Border, RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    SectionSummaryLine(
                        if (family == GroupLongFormFamily.PURCHASE) "Purchase" else "Home",
                        name,
                    )
                    SectionSummaryLine(
                        if (family == GroupLongFormFamily.PURCHASE) "Amount" else "Budget",
                        GroupBudgetUtils.summaryLabel(amount, amountCustom),
                    )
                    SectionSummaryLine("Members", buildMemberSummary(people))
                }
                GroupLongFormReadyBanner(message = readyMsg, accent = accent)
                if (error != null) {
                    Text(error, color = Color(0xFFEF4444), fontSize = 12.sp, fontFamily = PlusJakartaSans)
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(palette.accentGradient)
                        .clickable(enabled = !submitting) {
                            if (name.isBlank()) return@clickable
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
                            val budgetAmount = GroupBudgetUtils.resolveBudgetAmount(amount, amountCustom)
                            val reminderPreferences = mapOf(
                                "billReminders" to billReminders.equals("Enabled", ignoreCase = true),
                                "choreReminders" to choreReminders.equals("Enabled", ignoreCase = true),
                                "paymentReminders" to paymentReminders.equals("Enabled", ignoreCase = true),
                            )
                            val groupSetup = budgetAmount?.let {
                                GroupSetupBlockDto(
                                    budgetAmount = it,
                                    budgetCurrencyCode = currency,
                                    destinationText = null,
                                    reminderPreferences = reminderPreferences,
                                )
                            }
                            createViewModel.submitGroupMoment(
                                section = variant.section,
                                momentTypeCode = selectedCode,
                                title = name.trim(),
                                description = selected.defaultNotes.takeIf { it.isNotBlank() },
                                startAt = null,
                                endAt = null,
                                participants = invitees,
                                inviteCode = issuedInvite?.inviteCode,
                                groupSetup = groupSetup,
                                editingMomentId = editingMomentId,
                                onSuccess = { outcome ->
                                    scope.launch {
                                        runCatching {
                                            accountRepo.patchMomentNotificationPreferences(
                                                outcome.momentId,
                                                true,
                                                reminderPreferences,
                                            )
                                        }
                                        onCreated(outcome)
                                    }
                                },
                            )
                        }
                        .testTag(MaestroIds.GROUP_SETUP_SUBMIT)
                        .semantics {
                            role = Role.Button
                            contentDescription = variant.activateLabel.replace("→", "").trim()
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
                            "${variant.activateLabel} →",
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
                )
            }
        }
    }
}

@Composable
private fun SectionSummaryLine(label: String, value: String) {
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
