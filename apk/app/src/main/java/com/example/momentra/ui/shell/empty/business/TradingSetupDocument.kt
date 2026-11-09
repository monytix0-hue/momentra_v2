package com.example.momentra.ui.shell.empty.business

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.data.api.OperatingProjectionDto
import com.example.momentra.ui.shell.business.shared.OperatingMasterContext
import com.example.momentra.ui.shell.business.shared.OperatingMasterTab
import com.example.momentra.ui.shell.business.shared.OperatingMastersSheet
import com.example.momentra.ui.shell.business.shared.OperatingPickerMode
import com.example.momentra.ui.shell.business.shared.OperatingPickerSheet
import com.example.momentra.ui.shell.business.shared.OperatingSheetScrim
import com.example.momentra.ui.shell.business.shared.OperatingSheetTheme
import com.example.momentra.ui.shell.business.shared.IndiaStateCatalog
import com.example.momentra.ui.shell.business.shared.IndiaStateSheet
import com.example.momentra.ui.shell.maestro.MaestroIds
import com.example.momentra.ui.theme.PlusJakartaSans
import java.time.LocalDate
import java.time.YearMonth

/** Which picker the document is currently showing, or `null` when none is open. */
internal enum class SetupPickerTarget { START_DATE, END_DATE, MONTH, GST_EFFECTIVE, CURRENCY, GST_STATE }

/**
 * Everything the six-section document edits. One holder, one `remember`, so a recomposition
 * from a sheet write cannot lose a half-typed field.
 */
internal class SetupDocState(
    subtypeCode: String,
    initialName: String = "",
    initialCompanyName: String = "",
    initialPartyName: String = "",
) {
    var subtype by mutableStateOf(subtypeCode)

    // §01 Business Basics
    var businessName by mutableStateOf(initialName)
    var companyName by mutableStateOf(initialCompanyName)
    var gstin by mutableStateOf("")
    var currency by mutableStateOf(SetupDocumentSpec.currencyLabels.first())
    var startDate by mutableStateOf(LocalDate.now().toString())
    var endDate by mutableStateOf("")
    var description by mutableStateOf("")
    var location by mutableStateOf(initialPartyName)

    // §02 People & Roles
    var inviteRole by mutableStateOf(SetupDocumentSpec.DEFAULT_INVITE_ROLE)

    /** Everyone §02 has invited. The owner is never in here; they are the row above. */
    var invitees by mutableStateOf<List<SetupInvitee>>(emptyList())

    // §03 Channels & Monthly Plan
    var channels by mutableStateOf<Set<String>>(emptySet())
    var month by mutableStateOf(YearMonth.now().toString())
    var budget by mutableStateOf("")
    var target by mutableStateOf("")
    var metric by mutableStateOf(SetupDocumentSpec.metricLabel("REVENUE"))

    // §05 GST & Tax Details
    var gstOpen by mutableStateOf(true)
    var gstTracking by mutableStateOf("Yes")
    var gstRegistration by mutableStateOf("Regular")
    var gstState by mutableStateOf("")
    var gstPricing by mutableStateOf("GST is added separately")
    var gstEffectiveFrom by mutableStateOf("")

    /** Draft created on the first edit; §04 sheets and §06 activation both key off it. */
    var draftMomentId by mutableStateOf<String?>(null)

    /** Company the draft belongs to. Needed before masters can be listed. */
    var companyId by mutableStateOf<String?>(null)

    fun switchSubtype(code: String) {
        if (code == subtype) return
        subtype = code
        // Chips are keyed to the subtype: `Retail dealers` is not a channel of a trading moment,
        // so switching drops it instead of sending a channel the vertical cannot sell through.
        val allowed = SetupDocumentSpec.channels(code).toSet()
        channels = channels.intersect(allowed)
        // The role chip set is identical in both verticals, so the selection survives; it is
        // re-validated anyway so an old saved code can never outlive the chip that showed it.
        if (SetupDocumentSpec.inviteRoles(code).none { it.code == inviteRole }) {
            inviteRole = SetupDocumentSpec.DEFAULT_INVITE_ROLE
        }
    }

    fun toggleChannel(channel: String) {
        channels = if (channel in channels) channels - channel else channels + channel
    }

    val hasChannels: Boolean get() = channels.isNotEmpty()

    /** Selected channels in frame order, not in the order the operator happened to tap them. */
    val orderedChannels: List<String>
        get() = SetupDocumentSpec.channels(subtype).filter { it in channels }

    /** §04 rows and §06 activation only need a name to exist; everything else may be blank. */
    val canStart: Boolean
        get() = businessName.isNotBlank() && hasChannels &&
            plainAmount(budget).toDoubleOrNull().let { it != null && it > 0 } &&
            plainAmount(target).toDoubleOrNull().let { it != null && it > 0 }

    /**
     * Prefill from a saved draft, so reopening a draft (or editing a moment) continues where the
     * operator stopped instead of starting blank over a live record.
     */
    fun applyPrefill(preferences: Map<String, Any?>) {
        fun string(key: String): String? =
            (preferences[key] as? String)?.trim()?.takeIf { it.isNotEmpty() }

        string("subtype")?.let { saved ->
            // A draft saved in lower case still resolves to the canonical code, so the wire keeps
            // one spelling of each subtype.
            when {
                saved.equals(SetupDocumentSpec.TRADING, ignoreCase = true) ->
                    subtype = SetupDocumentSpec.TRADING
                saved.equals(SetupDocumentSpec.DISTRIBUTION, ignoreCase = true) ->
                    subtype = SetupDocumentSpec.DISTRIBUTION
                saved.equals(SetupDocumentSpec.STANDARD_PRODUCTION, ignoreCase = true) ->
                    subtype = SetupDocumentSpec.STANDARD_PRODUCTION
                saved.equals(SetupDocumentSpec.MADE_TO_ORDER, ignoreCase = true) ->
                    subtype = SetupDocumentSpec.MADE_TO_ORDER
                saved.equals(SetupDocumentSpec.RETAIL_STORE, ignoreCase = true) ->
                    subtype = SetupDocumentSpec.RETAIL_STORE
                saved.equals(SetupDocumentSpec.RESTAURANT_FOOD, ignoreCase = true) ->
                    subtype = SetupDocumentSpec.RESTAURANT_FOOD
                saved.equals(SetupDocumentSpec.CONSUMER_SERVICE, ignoreCase = true) ->
                    subtype = SetupDocumentSpec.CONSUMER_SERVICE
            }
        }
        string("identityName")?.let { businessName = it }
        string("partyName")?.let { location = it }
        string("description")?.let { description = it }
        string("location")?.let { location = it }
        string("gstin")?.let { gstin = it }
        string("gstState")?.let { gstState = it }
        string("startDate")?.let { startDate = it }
        string("endDate")?.let { endDate = it }
        string("gstEffectiveFrom")?.let { gstEffectiveFrom = it }
        string("currency")?.let { code ->
            currency = SetupDocumentSpec.currencyLabels.firstOrNull { it.startsWith(code) } ?: currency
        }
        (preferences["channels"] as? List<*>)?.let { saved ->
            val choices = SetupDocumentSpec.channels(subtype).toSet()
            channels = saved.filterIsInstance<String>().filter { it in choices }.toSet()
        }
        string("monthlyBudget")?.let { budget = formatIndianAmount(it) }
        string("monthlyTarget")?.let { target = formatIndianAmount(it) }
        string("targetMetric")?.let { metric = SetupDocumentSpec.metricLabel(it) }
        string("gstTracking")?.let { gstTracking = if (it.uppercase() == "NOT_NOW") "Not now" else "Yes" }
        string("gstRegistration")?.let { saved ->
            gstRegistration = SetupDocumentSpec.gstRegistrationLabels.firstOrNull {
                SetupDocumentSpec.gstRegistrationCode(it) == saved.uppercase()
            } ?: gstRegistration
        }
        string("salePriceIncludesGst")?.let { saved ->
            gstPricing = SetupDocumentSpec.gstPricingLabels.firstOrNull {
                SetupDocumentSpec.gstPricingCode(it) == saved.uppercase()
            } ?: gstPricing
        }
        inviteRole = SetupDocumentSpec.inviteRoles(subtype)
            .firstOrNull { it.code == inviteRole }?.code
            ?: SetupDocumentSpec.DEFAULT_INVITE_ROLE
        invitees = SetupDocumentSpec.invitees(preferences)
    }

    fun preferences(ownerName: String): Map<String, Any> = SetupDocumentSpec.preferences(
        subtypeCode = subtype,
        identityName = businessName,
        partyName = location,
        description = description,
        currency = SetupDocumentSpec.currencyCode(currency),
        startDate = startDate,
        endDate = endDate,
        location = location,
        channels = channels.toList(),
        monthlyBudget = plainAmount(budget),
        monthlyTarget = plainAmount(target),
        targetMetric = SetupDocumentSpec.metricCode(metric),
        participants = SetupDocumentSpec.participants(ownerName, invitees),
        gstin = gstin,
        gstState = gstState,
        gstRegistration = SetupDocumentSpec.gstRegistrationCode(gstRegistration),
        gstTracking = SetupDocumentSpec.gstTrackingCode(gstTracking),
        salePriceIncludesGst = SetupDocumentSpec.gstPricingCode(gstPricing),
        gstEffectiveFrom = gstEffectiveFrom,
    )
}

/**
 * `TRD-SETUP-v6` / `DST-SETUP-v6` — one scrolling document with six sections.
 *
 * The document owns its own sheet hosts so a §04 row can open the live masters sheet without
 * the host screen knowing which sheet belongs where. `onOpenMasters` is only used to report a
 * write back to the caller so its count subtitles can refresh.
 */
@Composable
internal fun TradingSetupDocument(
    state: SetupDocState,
    ownerName: String,
    counts: SetupMasterCounts,
    warehouseNames: List<String>,
    submitting: Boolean,
    error: String?,
    onClose: () -> Unit,
    onOpenMasters: (OperatingMasterTab, OperatingMasterTab?) -> Unit,
    onStart: () -> Unit,
    /** §02 mints its invite in-setup, so these are the card's own share actions. */
    onOpenInvite: () -> Unit = {},
    onShareInviteQr: () -> Unit = {},
    onWhatsAppInvite: () -> Unit = {},
    mintingInvite: Boolean = false,
    inviteError: String? = null,
    /** `DRAFT` for a setup that has not started, `ACTIVE` when editing a live moment. */
    momentStatus: String = "DRAFT",
    modifier: Modifier = Modifier,
) {
    val theme = setupDocTheme(state.subtype)
    var picker by remember { mutableStateOf<SetupPickerTarget?>(null) }

    Column(
        modifier
            .fillMaxSize()
            .background(theme.canvas)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 17.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(SetupSectionGap),
    ) {
        SetupHeader(
            name = state.businessName.ifBlank { "New business" },
            subtypeCode = state.subtype,
            momentStatus = momentStatus,
            theme = theme,
            onClose = onClose,
        )
        SetupKindCard(state, theme)
        // DST-SETUP-v6: soft orange accent drop under section cards (matches Pulse card glow).
        val distGlow = SetupDocumentSpec.isDistribution(state.subtype) ||
            SetupDocumentSpec.isManufacturing(state.subtype) ||
            SetupDocumentSpec.isRetailFamily(state.subtype)
        SetupSection(SetupDocumentSpec.sections(state.subtype)[0], theme, accentShadow = distGlow) {
            Section01(state, theme) { picker = it }
        }
        SetupSection(SetupDocumentSpec.sections(state.subtype)[1], theme, accentShadow = distGlow) {
            Section02(
                state = state,
                theme = theme,
                ownerName = ownerName,
                mintingInvite = mintingInvite,
                inviteError = inviteError,
                onInvite = onOpenInvite,
                onShareQr = onShareInviteQr,
                onWhatsApp = onWhatsAppInvite,
            )
        }
        SetupSection(SetupDocumentSpec.sections(state.subtype)[2], theme, accentShadow = distGlow) {
            Section03(state, theme) { picker = it }
        }
        SetupSection(SetupDocumentSpec.sections(state.subtype)[3], theme, accentShadow = distGlow) {
            Section04(state, theme, counts, warehouseNames, onOpenMasters, onOpenInvite)
        }
        SetupSection(SetupDocumentSpec.sections(state.subtype)[4], theme, accentShadow = distGlow) {
            Section05(state, theme) { picker = it }
        }
        SetupSection(SetupDocumentSpec.sections(state.subtype)[5], theme, accentShadow = distGlow) {
            Section06(state, theme, ownerName, submitting, momentStatus, onStart)
        }
        if (!error.isNullOrBlank()) {
            Text(error, color = theme.negative, fontFamily = PlusJakartaSans, fontSize = 13.sp)
        }
        Spacer(Modifier.height(28.dp))
    }

    val target = picker
    if (target == SetupPickerTarget.CURRENCY) {
        CurrencySheet(
            selected = state.currency,
            theme = theme,
            onDismiss = { picker = null },
            onPick = {
                state.currency = it
                picker = null
            },
        )
    }
    if (target == SetupPickerTarget.GST_STATE) {
        IndiaStateSheet(
            selected = state.gstState,
            theme = theme.toSheetTheme(),
            onDismiss = { picker = null },
            onPick = { picked ->
                // Store the canonical name; the code is derived on emit so jurisdiction compares
                // never depend on how the operator typed it.
                state.gstState = picked.name
                picker = null
            },
        )
    }
    if (target != null && target != SetupPickerTarget.CURRENCY && target != SetupPickerTarget.GST_STATE) {
        OperatingSheetScrim(
            onDismiss = { picker = null },
            accent = theme.accent,
            surface = theme.canvas,
            border = theme.border,
        ) {
            OperatingPickerSheet(
                mode = if (target == SetupPickerTarget.MONTH) OperatingPickerMode.MONTH else OperatingPickerMode.DAY,
                initial = when (target) {
                    SetupPickerTarget.MONTH -> YearMonth.parse(state.month).atDay(1)
                    SetupPickerTarget.GST_EFFECTIVE -> state.gstEffectiveFrom
                        .takeIf { it.isNotBlank() }?.toLocalDateOrNull() ?: LocalDate.now()
                    SetupPickerTarget.END_DATE -> state.endDate
                        .takeIf { it.isNotBlank() }?.toLocalDateOrNull() ?: LocalDate.now()
                    else -> state.startDate.toLocalDateOrNull() ?: LocalDate.now()
                },
                theme = theme.toSheetTheme(),
                title = when (target) {
                    SetupPickerTarget.START_DATE -> "Start date"
                    SetupPickerTarget.END_DATE -> "End date"
                    SetupPickerTarget.MONTH -> "Month"
                    SetupPickerTarget.GST_EFFECTIVE -> "Effective from"
                    SetupPickerTarget.CURRENCY -> "Currency"
                    SetupPickerTarget.GST_STATE -> "State"
                },
                onDismiss = { picker = null },
                onDay = { day ->
                    when (target) {
                        SetupPickerTarget.START_DATE -> state.startDate = day.toString()
                        SetupPickerTarget.END_DATE -> state.endDate = day.toString()
                        SetupPickerTarget.MONTH -> state.month = YearMonth.from(day).toString()
                        SetupPickerTarget.GST_EFFECTIVE -> state.gstEffectiveFrom = day.toString()
                        SetupPickerTarget.CURRENCY -> Unit
                        SetupPickerTarget.GST_STATE -> Unit
                    }
                    picker = null
                },
                onMonth = { ym ->
                    state.month = ym.toString()
                    picker = null
                },
            )
        }
    }
}

/**
 * The helper line under a GSTIN field.
 *
 * Once both a GSTIN and a state are present, says whether the GSTIN's first two characters name
 * that state — the mismatch the invoice would eventually expose. Returns `null` so the field
 * falls back to its neutral hint while either value is still incomplete.
 */
private fun gstinStateHint(state: SetupDocState): String? =
    IndiaStateCatalog.gstinStateHint(state.gstin, state.gstState)

@Composable
private fun CurrencySheet(
    selected: String,
    theme: SetupDocTheme,
    onDismiss: () -> Unit,
    onPick: (String) -> Unit,
) {
    OperatingSheetScrim(
        onDismiss = onDismiss,
        accent = theme.accent,
        surface = theme.canvas,
        border = theme.border,
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 17.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(Modifier.fillMaxWidth().height(22.dp), horizontalArrangement = Arrangement.Center) {
                Box(
                    Modifier
                        .width(40.dp)
                        .height(5.dp)
                        .clip(RoundedCornerShape(100.dp))
                        .background(Color.White.copy(alpha = 0.2f)),
                )
            }
            Text(
                "Currency",
                color = theme.ink,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 22.sp,
            )
            SetupDocumentSpec.currencyLabels.forEach { label ->
                val on = label == selected
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (on) theme.accent.copy(alpha = 0.14f) else theme.inset)
                        .border(
                            1.dp,
                            if (on) theme.accent else theme.panelStroke,
                            RoundedCornerShape(14.dp),
                        )
                        .clickable { onPick(label) }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        label,
                        color = theme.ink,
                        fontFamily = PlusJakartaSans,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f),
                    )
                    if (on) Text("●", color = theme.accent, fontSize = 10.sp)
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SetupHeader(
    name: String,
    subtypeCode: String,
    momentStatus: String,
    theme: SetupDocTheme,
    onClose: () -> Unit,
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "‹  Close",
                color = theme.muted,
                fontFamily = PlusJakartaSans,
                fontSize = 14.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .clickable(onClick = onClose)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
            )
            Spacer(Modifier.weight(1f))
            Text(
                SetupDocumentSpec.kindLabel(subtypeCode),
                color = theme.accent,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
            )
        }
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(48.dp))
                    .background(theme.card)
                    .border(1.dp, theme.border, RoundedCornerShape(48.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "◈",
                    color = theme.ink,
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 44.sp,
                )
            }
        }
        Text(
            "$name Setup",
            color = theme.ink,
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 32.sp,
        )
        Text(
            "Bring your business, people, and monthly plan together before you start.",
            color = theme.muted,
            fontFamily = PlusJakartaSans,
            fontSize = 14.sp,
        )
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Row(
                Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(theme.card)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    SetupDocumentSpec.statusPillLabel(momentStatus),
                    color = theme.accent,
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                )
            }
        }
    }
}

/** §00 `Kind of business` — the subtype switcher, which also re-themes the document. */
@Composable
private fun SetupKindCard(state: SetupDocState, theme: SetupDocTheme) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(theme.card)
            .border(1.dp, theme.border, RoundedCornerShape(18.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            "Kind of business",
            color = theme.muted,
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp,
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val kindCodes = when {
                SetupDocumentSpec.isManufacturing(state.subtype) ->
                    listOf(SetupDocumentSpec.STANDARD_PRODUCTION, SetupDocumentSpec.MADE_TO_ORDER)
                SetupDocumentSpec.isRetailFamily(state.subtype) ->
                    listOf(
                        SetupDocumentSpec.RETAIL_STORE,
                        SetupDocumentSpec.RESTAURANT_FOOD,
                        SetupDocumentSpec.CONSUMER_SERVICE,
                    )
                else -> listOf(SetupDocumentSpec.DISTRIBUTION, SetupDocumentSpec.TRADING)
            }
            kindCodes.forEach { code ->
                val on = state.subtype.equals(code, ignoreCase = true)
                Column(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(theme.card)
                        .border(
                            1.dp,
                            if (on) theme.accent else theme.border,
                            RoundedCornerShape(10.dp),
                        )
                        .clickable { state.switchSubtype(code) }
                        .padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        "◈",
                        color = if (on) theme.ink else theme.muted,
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 20.sp,
                    )
                    Text(
                        SetupDocumentSpec.kindLabel(code),
                        color = if (on) theme.ink else theme.muted,
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                    )
                }
            }
        }
        Text(
            "6 steps · ${SetupDocumentSpec.kindLabel(state.subtype)} setup",
            color = theme.muted,
            fontFamily = PlusJakartaSans,
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun Section01(state: SetupDocState, theme: SetupDocTheme, onPicker: (SetupPickerTarget) -> Unit) {
    SetupPanel("Identity", theme) {
        SetupField(
            label = "Business name",
            value = state.businessName,
            theme = theme,
            placeholder = "Northstar Trading",
            onValueChange = { state.businessName = it },
        )
        SetupField(
            label = "Company name",
            value = state.companyName,
            theme = theme,
            hint = "Registered company name",
            placeholder = state.businessName,
            onValueChange = { state.companyName = it },
        )
        SetupField(
            label = "GSTIN",
            value = state.gstin,
            theme = theme,
            hint = gstinStateHint(state) ?: "15-digit GSTIN, if you have one",
            placeholder = "29ABCDE4321T1Z5",
            onValueChange = { state.gstin = it.uppercase() },
        )
        SetupPickerField(
            label = "Currency",
            value = state.currency,
            theme = theme,
            onClick = { onPicker(SetupPickerTarget.CURRENCY) },
        )
        SetupRowSplit(
            theme = theme,
            leftLabel = "Start date",
            leftValue = if (state.startDate.isBlank()) "" else SetupDocumentSpec.setupDateLabel(state.startDate),
            leftPlaceholder = "Today",
            rightLabel = "End date",
            rightValue = if (state.endDate.isBlank()) "" else SetupDocumentSpec.setupDateLabel(state.endDate),
            rightPlaceholder = "Still open",
            onLeft = { onPicker(SetupPickerTarget.START_DATE) },
            onRight = { onPicker(SetupPickerTarget.END_DATE) },
        )
    }
    SetupPanel("Dates & more", theme) {
        SetupField(
            label = "Description",
            value = state.description,
            theme = theme,
            placeholder = "Everyday notes about this business",
            maxLines = 2,
            onValueChange = { state.description = it },
        )
        SetupField(
            label = "Location",
            value = state.location,
            theme = theme,
            placeholder = "Add your DC location",
            onValueChange = { state.location = it },
        )
        SetupFootnote("You can fill in the extra details later.", theme)
    }
}

@Composable
private fun Section02(
    state: SetupDocState,
    theme: SetupDocTheme,
    ownerName: String,
    mintingInvite: Boolean,
    inviteError: String?,
    onInvite: () -> Unit,
    onShareQr: () -> Unit,
    onWhatsApp: () -> Unit,
) {
    val owner = ownerName.ifBlank { "You" }
    val business = state.businessName.ifBlank { "this business" }
    // One card with the owner first and every invitee under it — the group setup people card
    // keeps one container and separates its rows with a hairline, so the team reads as one list.
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, theme.border, RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Owner row — the only person who exists before an invite is minted.
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SetupAvatar(SetupDocumentSpec.initials(owner), theme, size = 44, tone = theme.accent)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "$owner · You",
                    color = theme.ink,
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                )
                Text(
                    "Owner · $business",
                    color = theme.muted,
                    fontFamily = PlusJakartaSans,
                    fontSize = 12.sp,
                    maxLines = 1,
                )
            }
            Text(
                "You are owner",
                color = theme.accent,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                maxLines = 1,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(theme.accent.copy(alpha = 0.13f))
                    .border(1.dp, theme.accent, RoundedCornerShape(999.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            )
        }

        // Everyone already invited, as live rows rather than a count — a name is what makes a
        // pending invite believable, and the group setup card lists its invitees the same way.
        state.invitees.forEach { invitee ->
            HorizontalDivider(color = theme.border, thickness = 1.dp)
            SetupInviteeRow(
                invitee = invitee,
                theme = theme,
                onRemove = { state.invitees = state.invitees.filterNot { it.id == invitee.id } },
            )
        }
    }

    SetupPanel("Roles on invite", theme) {
        SetupChipRow(
            options = SetupDocumentSpec.inviteRoles(state.subtype).map { it.label },
            selected = setOf(SetupDocumentSpec.roleLabel(state.inviteRole)),
            theme = theme,
            onSelect = { state.inviteRole = SetupDocumentSpec.roleCode(it) },
        )
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                SetupDocumentSpec.inviteAccessNote(state.inviteRole),
                color = theme.muted,
                fontFamily = PlusJakartaSans,
                fontSize = 11.sp,
                modifier = Modifier.weight(1f),
            )
            // The group card shows the count beside the roles rather than in a second panel —
            // a duplicate team panel would list the same people twice.
            Text(
                if (state.invitees.isEmpty()) "No invites yet"
                else "${state.invitees.size} pending",
                color = theme.muted,
                fontFamily = PlusJakartaSans,
                fontSize = 11.sp,
            )
        }
    }

    // The group card's two dotted actions: a QR for the room, WhatsApp for the person.
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        SetupOutlineAction(
            title = "Share QR",
            busy = mintingInvite,
            theme = theme,
            onClick = onShareQr,
            modifier = Modifier.weight(1f),
        )
        SetupOutlineAction(
            title = "WhatsApp",
            busy = mintingInvite,
            theme = theme,
            onClick = onWhatsApp,
            modifier = Modifier.weight(1f),
        )
    }

    if (!inviteError.isNullOrBlank()) {
        Text(
            inviteError,
            color = theme.negative,
            fontFamily = PlusJakartaSans,
            fontSize = 12.sp,
        )
    }

    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, theme.accent.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
            .clickable(onClick = onInvite)
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            "+ Invite people · ${SetupDocumentSpec.roleLabel(state.inviteRole)}",
            color = theme.ink,
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
        )
    }

    SetupWhy(
        title = SetupDocumentSpec.APPROVAL_WHY_TITLE,
        body = SetupDocumentSpec.approvalWhyBody(owner, state.businessName),
        theme = theme,
    )
}

/** One invited person: initials, name, and the role the pending invite grants. */
@Composable
private fun SetupInviteeRow(
    invitee: SetupInvitee,
    theme: SetupDocTheme,
    onRemove: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SetupAvatar(SetupDocumentSpec.initials(invitee.name), theme, size = 40)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                invitee.name,
                color = theme.ink,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                maxLines = 1,
            )
            Text(
                SetupDocumentSpec.teamMeta("PENDING", invitee.roleLabel),
                color = theme.accent,
                fontFamily = PlusJakartaSans,
                fontSize = 12.sp,
                maxLines = 1,
            )
        }
        Text(
            "Remove",
            color = theme.muted,
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .clickable(onClick = onRemove)
                .padding(horizontal = 10.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun Section03(state: SetupDocState, theme: SetupDocTheme, onPicker: (SetupPickerTarget) -> Unit) {
    SetupPanel("Channels", theme) {
        SetupChipRow(
            options = SetupDocumentSpec.channels(state.subtype),
            selected = state.channels,
            theme = theme,
            onSelect = { state.toggleChannel(it) },
        )
        Text(
            SetupDocumentSpec.channelHint(state.channels.size),
            color = theme.muted,
            fontFamily = PlusJakartaSans,
            fontSize = 11.sp,
        )
    }
    SetupPickerField(
        label = "Month",
        value = SetupDocumentSpec.monthLabel(state.month),
        theme = theme,
        onClick = { onPicker(SetupPickerTarget.MONTH) },
    )
    SetupField(
        label = "Monthly budget",
        value = state.budget,
        theme = theme,
        hint = "Amount",
        placeholder = "₹60,00,000",
        keyboardType = KeyboardType.Decimal,
        onValueChange = { state.budget = formatIndianAmount(it) },
    )
    SetupField(
        label = "Monthly sales target",
        value = state.target,
        theme = theme,
        hint = "Amount",
        placeholder = "₹90,00,000",
        keyboardType = KeyboardType.Decimal,
        onValueChange = { state.target = formatIndianAmount(it) },
    )
    SetupChipRow(
        label = "How the target is measured",
        options = setupMetricLabels(),
        selected = setOf(state.metric),
        theme = theme,
        onSelect = { state.metric = it },    )
    SetupWhy(
        title = SetupDocumentSpec.TARGET_WHY_TITLE,
        body = SetupDocumentSpec.TARGET_WHY_BODY,
        theme = theme,
    )
    val budgetOk = plainAmount(state.budget).toDoubleOrNull().let { it != null && it > 0 }
    val targetOk = plainAmount(state.target).toDoubleOrNull().let { it != null && it > 0 }
    SetupStatusPill(
        text = if (budgetOk && targetOk) {
            "Budget and sales target look good"
        } else {
            "Add a budget and a sales target"
        },
        theme = theme,
        tone = if (budgetOk && targetOk) theme.positive else theme.muted,
    )
}

@Composable
private fun Section04(
    state: SetupDocState,
    theme: SetupDocTheme,
    counts: SetupMasterCounts,
    warehouseNames: List<String>,
    onOpenMasters: (OperatingMasterTab, OperatingMasterTab?) -> Unit,
    onOpenInvite: () -> Unit,
) {
    SetupMasterRow(
        glyph = "◇",
        title = "Catalogue & SKU",
        subtitle = SetupDocumentSpec.catalogueSubtitle(counts),
        theme = theme,
    ) { onOpenMasters(OperatingMasterTab.Catalogue, null) }
    SetupMasterRow(
        glyph = "⊞",
        title = "+ Create SKU",
        subtitle = SetupDocumentSpec.CATALOGUE_INVITE,
        theme = theme,
    ) { onOpenMasters(OperatingMasterTab.Catalogue, OperatingMasterTab.Catalogue) }
    SetupMasterRow(
        glyph = "▤",
        title = "Warehouses & routes",
        subtitle = SetupDocumentSpec.warehouseSubtitle(warehouseNames, counts),
        theme = theme,
    ) { onOpenMasters(OperatingMasterTab.Locations, null) }
    SetupMasterRow(
        glyph = "◍",
        title = SetupDocumentSpec.networkLabel(state.subtype),
        subtitle = SetupDocumentSpec.networkSubtitle(state.subtype, counts),
        theme = theme,
    ) { onOpenMasters(OperatingMasterTab.Principals, null) }
    SetupMasterRow(
        glyph = "❐",
        title = "Team invite & QR",
        subtitle = SetupDocumentSpec.inviteSubtitle(state.subtype),
        theme = theme,
    ) { onOpenInvite() }
    SetupFootnote(SetupDocumentSpec.networkFootnote(state.subtype), theme)
}

@Composable
private fun Section05(state: SetupDocState, theme: SetupDocTheme, onPicker: (SetupPickerTarget) -> Unit) {
    Text(
        if (state.gstOpen) "Hide GST setup" else "Show GST setup",
        color = theme.accent,
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .clickable { state.gstOpen = !state.gstOpen }
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
    if (!state.gstOpen) return

    SetupPanel("Tracking", theme) {
        SetupChipRow(
            label = "Do you want to track GST in this Moment?",
            options = SetupDocumentSpec.gstTrackingLabels,
            selected = setOf(state.gstTracking),
            theme = theme,
            onSelect = { state.gstTracking = it },
        )
        if (state.gstTracking != "Yes") {
            Text(
                SetupDocumentSpec.GST_SKIP_CAVEAT,
                color = theme.muted,
                fontFamily = PlusJakartaSans,
                fontSize = 11.sp,
            )
        }
    }
    if (state.gstTracking == "Yes") {
        SetupPanel("Registration", theme) {
            SetupChipRow(
                label = "How is your business registered for GST?",
                options = SetupDocumentSpec.gstRegistrationLabels,
                selected = setOf(state.gstRegistration),
                theme = theme,
                onSelect = { state.gstRegistration = it },
            )
            SetupPickerField(
                label = "Which state is your business registered in?",
                value = state.gstState,
                theme = theme,
                hint = gstinStateHint(state) ?: "Used to decide CGST + SGST or IGST on every sale.",
                placeholder = "Pick a state",
                onClick = { onPicker(SetupPickerTarget.GST_STATE) },
            )
        }
        SetupPanel("Pricing", theme) {
            SetupChipRow(
                label = "Do prices include GST?",
                options = SetupDocumentSpec.gstPricingLabels,
                selected = setOf(state.gstPricing),
                theme = theme,
                onSelect = { state.gstPricing = it },
            )
            Text(
                SetupDocumentSpec.GST_NOTE,
                color = theme.muted,
                fontFamily = PlusJakartaSans,
                fontSize = 11.sp,
            )
            SetupPickerField(
                label = "Effective from (optional)",
                value = if (state.gstEffectiveFrom.isBlank()) "" else {
                    SetupDocumentSpec.setupDateLabel(state.gstEffectiveFrom)
                },
                theme = theme,
                placeholder = "YYYY-MM-DD",
                onClick = { onPicker(SetupPickerTarget.GST_EFFECTIVE) },
            )
        }
    }
}

@Composable
private fun Section06(
    state: SetupDocState,
    theme: SetupDocTheme,
    ownerName: String,
    submitting: Boolean,
    momentStatus: String,
    onStart: () -> Unit,
) {
    val owner = ownerName.ifBlank { "Owner" }
    SetupPanel("Moment", theme) {
        Text(
            state.businessName.ifBlank { "New business" },
            color = theme.ink,
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
        )
        Text(
            SetupDocumentSpec.momentLine(
                subtypeCode = state.subtype,
                currency = SetupDocumentSpec.currencyCode(state.currency),
                startDate = state.startDate,
                endDate = state.endDate,
            ),
            color = theme.muted,
            fontFamily = PlusJakartaSans,
            fontSize = 12.sp,
        )
    }
    SetupPanel("Summary", theme) {
        SetupKeyValue("Company", state.companyName.ifBlank { state.businessName }.ifBlank { "—" }, theme)
        SetupKeyValue("GSTIN", state.gstin.ifBlank { "Not added" }, theme)
        SetupKeyValue("People", "$owner · Owner", theme)
        val month = SetupDocumentSpec.monthLabel(state.month)
        SetupKeyValue("$month budget", state.budget.ifBlank { "Not set" }, theme)
        SetupKeyValue("$month sales target", state.target.ifBlank { "Not set" }, theme)
        SetupKeyValue("Target measure", state.metric, theme)
    }
    SetupPanel("Before you start", theme) {
        Text(
            if (state.hasChannels) {
                "Sales channels · ${state.orderedChannels.joinToString(", ")}"
            } else {
                "Choose at least one sales channel in section 03."
            },
            color = theme.muted,
            fontFamily = PlusJakartaSans,
            fontSize = 11.sp,
        )
        Text(
            "Name, currency, owner, and sales channels are set. Budget and sales target are filled in.",
            color = theme.muted,
            fontFamily = PlusJakartaSans,
            fontSize = 11.sp,
        )
        Text(
            "This stays private to your business. Only the owner can change who has access. " +
                "Nothing is counted until you start.",
            color = theme.muted,
            fontFamily = PlusJakartaSans,
            fontSize = 11.sp,
        )
    }
    SetupStatusPill(
        text = if (state.canStart) "Ready to start" else "A few fields still need you",
        theme = theme,
        tone = if (state.canStart) theme.positive else theme.muted,
    )
    Box(Modifier.testTag(MaestroIds.BUSINESS_SETUP_SUBMIT)) {
        SetupCta(
            label = if (submitting) {
                SetupDocumentSpec.activatingLabel(momentStatus)
            } else {
                SetupDocumentSpec.activateLabel(momentStatus)
            },
            theme = theme,
            enabled = state.canStart && !submitting,
            onClick = onStart,
        )
    }
}

/** The document palette expressed as the shared sheet theme, for the picker and masters sheets. */
internal fun SetupDocTheme.toSheetTheme(): OperatingSheetTheme = OperatingSheetTheme(
    accent = accent,
    accentSoft = positive,
    canvas = canvas,
    surface = card,
    inset = inset,
    border = panelStroke,
    ink = ink,
    body = muted,
    warning = Color(0xFFF59E0B),
    negative = negative,
)

/**
 * Hosts the §04 masters sheet. Rendered by the setup host *outside* the scrolling column so a
 * sheet scrim covers the document instead of scrolling with it.
 */
@Composable
internal fun SetupMastersHost(
    state: SetupDocState,
    open: Boolean,
    tab: OperatingMasterTab,
    initialForm: OperatingMasterTab?,
    onDismiss: () -> Unit,
    onChanged: () -> Unit,
) {
    if (!open) return
    val theme = setupDocTheme(state.subtype)
    val model = OperatingProjectionDto(
        title = state.businessName,
        role = "OWNER_ADMIN",
        familyCode = if (SetupDocumentSpec.isManufacturing(state.subtype)) {
            "MANUFACTURING_PRODUCTION"
        } else {
            "TRADING_DISTRIBUTION"
        },
        subtype = state.subtype,
        masters = emptyList(),
    )
    OperatingSheetScrim(
        onDismiss = onDismiss,
        accent = theme.accent,
        surface = theme.canvas,
        border = theme.border,
    ) {
        OperatingMastersSheet(
            tab = tab,
            model = model,
            context = OperatingMasterContext(
                companyId = state.companyId,
                momentId = state.draftMomentId,
                onChanged = onChanged,
            ),
            theme = theme.toSheetTheme(),
            onClose = onDismiss,
            initialForm = initialForm,
        )
    }
}

private fun String.toLocalDateOrNull(): LocalDate? = runCatching { LocalDate.parse(take(10)) }.getOrNull()

/** §03's chip row is single-select for the metric; this keeps the label/code mapping honest. */
internal fun setupMetricLabels(): List<String> = listOf("Revenue", "Orders", "Count")
