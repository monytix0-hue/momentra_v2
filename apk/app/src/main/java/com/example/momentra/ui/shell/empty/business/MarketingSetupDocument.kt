package com.example.momentra.ui.shell.empty.business

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.R
import com.example.momentra.data.api.OperatingProjectionDto
import com.example.momentra.ui.shell.business.shared.IndiaStateCatalog
import com.example.momentra.ui.shell.business.shared.IndiaStateSheet
import com.example.momentra.ui.shell.business.shared.OperatingMasterContext
import com.example.momentra.ui.shell.business.shared.OperatingMasterTab
import com.example.momentra.ui.shell.business.shared.OperatingMastersSheet
import com.example.momentra.ui.shell.business.shared.OperatingPickerMode
import com.example.momentra.ui.shell.business.shared.OperatingPickerSheet
import com.example.momentra.ui.shell.business.shared.OperatingSheetScrim
import com.example.momentra.ui.shell.maestro.MaestroIds
import com.example.momentra.ui.theme.PlusJakartaSans
import java.time.LocalDate

/**
 * `MKT-SETUP-v6` / `MKT-OWN-SETUP-v6` — one scrolling document with Client / Own kind tiles,
 * indigo ↔ magenta accents, warm-orange emblem glow, and accent section shadows.
 */
@Composable
internal fun MarketingSetupDocument(
    state: MarketingSetupDocState,
    ownerName: String,
    counts: SetupMasterCounts,
    submitting: Boolean,
    error: String?,
    onClose: () -> Unit,
    onOpenMasters: (OperatingMasterTab, OperatingMasterTab?) -> Unit,
    onStart: () -> Unit,
    onOpenInvite: () -> Unit = {},
    onShareInviteQr: () -> Unit = {},
    onWhatsAppInvite: () -> Unit = {},
    mintingInvite: Boolean = false,
    inviteError: String? = null,
    momentStatus: String = "DRAFT",
    modifier: Modifier = Modifier,
) {
    val theme = marketingSetupDocTheme(state.subtype)
    val soft = marketingAccentSoft(state.subtype)
    var picker by remember { mutableStateOf<SetupPickerTarget?>(null) }
    val sections = MarketingSetupDocumentSpec.sections(state.subtype)
    val own = MarketingSetupDocumentSpec.isOwn(state.subtype)

    Column(
        modifier
            .fillMaxSize()
            .background(theme.canvas)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 17.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(SetupSectionGap),
    ) {
        MarketingSetupHeader(
            name = state.campaignName.ifBlank { "New campaign" },
            subtypeCode = state.subtype,
            momentStatus = momentStatus,
            theme = theme,
            soft = soft,
            onClose = onClose,
        )
        MarketingKindCard(state, theme)
        SetupSection(sections[0], theme, accentShadow = true) {
            MarketingSection01(state, theme, own) { picker = it }
        }
        SetupSection(sections[1], theme, accentShadow = true) {
            MarketingSection02(
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
        SetupSection(sections[2], theme, accentShadow = true) {
            MarketingSection03(state, theme, counts, own, onOpenMasters, onOpenInvite)
        }
        SetupSection(sections[3], theme, accentShadow = true) {
            MarketingSection04(state, theme, own) { picker = it }
        }
        SetupSection(sections[4], theme, accentShadow = true) {
            MarketingSection05(state, theme) { picker = it }
        }
        SetupSection(sections[5], theme, accentShadow = true) {
            MarketingSection06(state, theme, ownerName, submitting, momentStatus, own, onStart)
        }
        if (!error.isNullOrBlank()) {
            Text(error, color = theme.negative, fontFamily = PlusJakartaSans, fontSize = 13.sp)
        }
        Spacer(Modifier.height(28.dp))
    }

    val target = picker
    if (target == SetupPickerTarget.CURRENCY) {
        MarketingCurrencySheet(
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
                mode = OperatingPickerMode.DAY,
                initial = when (target) {
                    SetupPickerTarget.GST_EFFECTIVE -> state.gstEffectiveFrom
                        .takeIf { it.isNotBlank() }?.marketingLocalDateOrNull() ?: LocalDate.now()
                    SetupPickerTarget.END_DATE -> state.endDate
                        .takeIf { it.isNotBlank() }?.marketingLocalDateOrNull() ?: LocalDate.now()
                    else -> state.startDate.marketingLocalDateOrNull() ?: LocalDate.now()
                },
                theme = theme.toSheetTheme(),
                title = when (target) {
                    SetupPickerTarget.START_DATE -> "Start date"
                    SetupPickerTarget.END_DATE -> "End date"
                    SetupPickerTarget.GST_EFFECTIVE -> "Effective from"
                    else -> "Date"
                },
                onDismiss = { picker = null },
                onDay = { day ->
                    when (target) {
                        SetupPickerTarget.START_DATE -> state.startDate = day.toString()
                        SetupPickerTarget.END_DATE -> state.endDate = day.toString()
                        SetupPickerTarget.GST_EFFECTIVE -> state.gstEffectiveFrom = day.toString()
                        else -> Unit
                    }
                    picker = null
                },
                onMonth = { picker = null },
            )
        }
    }
}

@Composable
internal fun MarketingSetupMastersHost(
    state: MarketingSetupDocState,
    open: Boolean,
    tab: OperatingMasterTab,
    initialForm: OperatingMasterTab?,
    onDismiss: () -> Unit,
    onChanged: () -> Unit,
) {
    if (!open) return
    val theme = marketingSetupDocTheme(state.subtype)
    val model = OperatingProjectionDto(
        title = state.campaignName,
        role = "OWNER_ADMIN",
        familyCode = "MARKETING_CAMPAIGNS",
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

@Composable
private fun MarketingSetupHeader(
    name: String,
    subtypeCode: String,
    momentStatus: String,
    theme: SetupDocTheme,
    soft: Color,
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
                MarketingSetupDocumentSpec.kindLabel(subtypeCode),
                color = theme.accent,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
            )
        }
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .size(120.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                MarketingEmblemGlow.copy(alpha = 0.40f),
                                MarketingEmblemGlow.copy(alpha = 0.12f),
                                Color.Transparent,
                            ),
                        ),
                    ),
            )
            Box(
                Modifier
                    .size(96.dp)
                    .shadow(
                        elevation = 20.dp,
                        shape = RoundedCornerShape(48.dp),
                        ambientColor = MarketingEmblemGlow.copy(alpha = 0.40f),
                        spotColor = MarketingEmblemGlow.copy(alpha = 0.40f),
                    )
                    .clip(RoundedCornerShape(48.dp))
                    .background(theme.card)
                    .border(1.dp, theme.border, RoundedCornerShape(48.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_mkt_mega),
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
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
            "Campaign identity, people, network, and monthly plan before you activate.",
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
                    MarketingSetupDocumentSpec.statusPillLabel(momentStatus),
                    color = soft,
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                )
            }
        }
    }
}

@Composable
private fun MarketingKindCard(state: MarketingSetupDocState, theme: SetupDocTheme) {
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
            "Kind of campaign",
            color = theme.muted,
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp,
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf(MarketingSetupDocumentSpec.CLIENT, MarketingSetupDocumentSpec.OWN).forEach { code ->
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
                        MarketingSetupDocumentSpec.kindLabel(code),
                        color = if (on) theme.ink else theme.muted,
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                    )
                }
            }
        }
        Text(
            "4 sections + GST & Review",
            color = theme.muted,
            fontFamily = PlusJakartaSans,
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun MarketingSection01(
    state: MarketingSetupDocState,
    theme: SetupDocTheme,
    own: Boolean,
    onPicker: (SetupPickerTarget) -> Unit,
) {
    SetupPanel("Identity", theme) {
        SetupField(
            label = "Campaign name",
            value = state.campaignName,
            theme = theme,
            placeholder = if (own) "Spring launch" else "Brightline Q2",
            onValueChange = { state.campaignName = it },
        )
        SetupField(
            label = if (own) "Brand / business" else "Client",
            value = state.partyName,
            theme = theme,
            placeholder = if (own) "Your brand" else "Client name",
            onValueChange = { state.partyName = it },
        )
        if (own) {
            SetupField(
                label = "What you promote",
                value = state.offering,
                theme = theme,
                placeholder = "Product, offer, or message",
                onValueChange = { state.offering = it },
            )
        } else {
            SetupField(
                label = "Client fee (monthly)",
                value = state.clientFee,
                theme = theme,
                hint = "Optional retainer / fee — not an invoice",
                placeholder = "₹1,50,000",
                keyboardType = KeyboardType.Decimal,
                onValueChange = { state.clientFee = formatIndianAmount(it) },
            )
            SetupField(
                label = "Client-approved media budget",
                value = state.clientApprovedMediaBudget,
                theme = theme,
                hint = "Separate from agency operating cost",
                placeholder = "₹5,00,000",
                keyboardType = KeyboardType.Decimal,
                onValueChange = { state.clientApprovedMediaBudget = formatIndianAmount(it) },
            )
            SetupField(
                label = "Agency operating budget",
                value = state.agencyOperatingBudget,
                theme = theme,
                hint = "Agency cost bucket",
                placeholder = "₹1,00,000",
                keyboardType = KeyboardType.Decimal,
                onValueChange = { state.agencyOperatingBudget = formatIndianAmount(it) },
            )
            SetupChipRow(
                label = "Reporting cadence",
                options = listOf("WEEKLY", "MONTHLY", "CUSTOM"),
                selected = setOfNotNull(state.reportingCadence.takeIf { it.isNotBlank() }),
                theme = theme,
                onSelect = { state.reportingCadence = if (state.reportingCadence == it) "" else it },
            )
        }
        // Creative approval toggle (both modes)
        SetupChipRow(
            label = "Creative approval",
            options = listOf("Required", "Not required"),
            selected = setOf(if (state.creativeApprovalRequired) "Required" else "Not required"),
            theme = theme,
            onSelect = { state.creativeApprovalRequired = it == "Required" },
        )
        SetupChipRow(
            label = "Purpose",
            options = MarketingSetupDocumentSpec.campaignPurposes(state.subtype),
            selected = setOfNotNull(state.purpose.takeIf { it.isNotBlank() }),
            theme = theme,
            onSelect = { state.purpose = it },
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
            leftValue = if (state.startDate.isBlank()) "" else MarketingSetupDocumentSpec.setupDateLabel(state.startDate),
            leftPlaceholder = "Required",
            rightLabel = "End date",
            rightValue = if (state.endDate.isBlank()) "" else MarketingSetupDocumentSpec.setupDateLabel(state.endDate),
            rightPlaceholder = "Open",
            onLeft = { onPicker(SetupPickerTarget.START_DATE) },
            onRight = { onPicker(SetupPickerTarget.END_DATE) },
        )
    }
}

@Composable
private fun MarketingSection02(
    state: MarketingSetupDocState,
    theme: SetupDocTheme,
    ownerName: String,
    mintingInvite: Boolean,
    inviteError: String?,
    onInvite: () -> Unit,
    onShareQr: () -> Unit,
    onWhatsApp: () -> Unit,
) {
    val owner = ownerName.ifBlank { "You" }
    val campaign = state.campaignName.ifBlank { "this campaign" }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, theme.border, RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SetupAvatar(MarketingSetupDocumentSpec.initials(owner), theme, size = 44, tone = theme.accent)
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
                    "Owner · $campaign",
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
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(theme.accent.copy(alpha = 0.13f))
                    .border(1.dp, theme.accent, RoundedCornerShape(999.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            )
        }
        state.invitees.forEach { invitee ->
            HorizontalDivider(color = theme.border, thickness = 1.dp)
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SetupAvatar(MarketingSetupDocumentSpec.initials(invitee.name), theme, size = 36)
                Column(Modifier.weight(1f)) {
                    Text(invitee.name, color = theme.ink, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Text(MarketingSetupDocumentSpec.roleLabel(invitee.roleCode), color = theme.muted, fontFamily = PlusJakartaSans, fontSize = 11.sp)
                }
                Text(
                    "Remove",
                    color = theme.negative,
                    fontFamily = PlusJakartaSans,
                    fontSize = 11.sp,
                    modifier = Modifier.clickable { state.removeInvitee(invitee.id) },
                )
            }
        }
    }
    SetupPanel("Roles on invite", theme) {
        SetupChipRow(
            options = MarketingSetupDocumentSpec.inviteRoles(state.subtype).map { it.label },
            selected = setOf(MarketingSetupDocumentSpec.roleLabel(state.inviteRole)),
            theme = theme,
            onSelect = { state.inviteRole = MarketingSetupDocumentSpec.roleCode(it) },
        )
        Text(
            MarketingSetupDocumentSpec.inviteAccessNote(state.inviteRole),
            color = theme.muted,
            fontFamily = PlusJakartaSans,
            fontSize = 11.sp,
        )
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        SetupOutlineAction(
            title = "Share QR",
            busy = mintingInvite,
            theme = theme,
            onClick = onShareQr,
            modifier = Modifier.weight(1f),
        )
        Box(
            Modifier
                .weight(1f)
                .shadow(
                    elevation = 10.dp,
                    shape = RoundedCornerShape(14.dp),
                    ambientColor = MarketingEmblemGlow.copy(alpha = 0.60f),
                    spotColor = MarketingEmblemGlow.copy(alpha = 0.60f),
                ),
        ) {
            SetupOutlineAction(
                title = "WhatsApp",
                busy = mintingInvite,
                theme = theme,
                onClick = onWhatsApp,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
    if (!inviteError.isNullOrBlank()) {
        Text(inviteError, color = theme.negative, fontFamily = PlusJakartaSans, fontSize = 12.sp)
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
            "+ Invite people · ${MarketingSetupDocumentSpec.roleLabel(state.inviteRole)}",
            color = theme.ink,
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
        )
    }
}

@Composable
private fun MarketingSection03(
    state: MarketingSetupDocState,
    theme: SetupDocTheme,
    counts: SetupMasterCounts,
    own: Boolean,
    onOpenMasters: (OperatingMasterTab, OperatingMasterTab?) -> Unit,
    onOpenInvite: () -> Unit,
) {
    // BS-06 — single Campaign Resources & Providers area (no warehouses / separate price lists).
    SetupMasterRow(
        glyph = "◇",
        title = "Campaign Resources & Providers",
        subtitle = if (counts.skus + counts.principals + counts.dealers == 0) {
            "Register resources, providers, prices and tax in one place"
        } else {
            "${counts.skus} resources · ${counts.principals + counts.dealers} providers"
        },
        theme = theme,
    ) { onOpenMasters(OperatingMasterTab.Catalogue, null) }
    SetupMasterRow(
        glyph = "◍",
        title = if (own) "Providers & creators" else "Clients & providers",
        subtitle = if (counts.principals + counts.dealers == 0) {
            "Add providers this campaign buys from"
        } else {
            "${counts.principals + counts.dealers} contacts"
        },
        theme = theme,
    ) { onOpenMasters(OperatingMasterTab.Principals, null) }
    SetupMasterRow(
        glyph = "❐",
        title = "Team invite & QR",
        subtitle = MarketingSetupDocumentSpec.inviteRoles(state.subtype).joinToString(" · ") { it.label },
        theme = theme,
    ) { onOpenInvite() }
    SetupFootnote(
        if (own) {
            "Own Business has no client invoices — resources and providers only. No warehouse or price-list setup."
        } else {
            "Marketing skips warehouses and separate price lists; resources carry provider, price and GST."
        },
        theme,
    )
}

@Composable
private fun MarketingSection04(
    state: MarketingSetupDocState,
    theme: SetupDocTheme,
    own: Boolean,
    onPicker: (SetupPickerTarget) -> Unit,
) {
    SetupPanel("Channels", theme) {
        SetupChipRow(
            options = MarketingSetupDocumentSpec.channels(state.subtype),
            selected = state.channels,
            theme = theme,
            onSelect = { state.toggleChannel(it) },
        )
    }
    SetupField(
        label = if (own) "Monthly marketing budget" else "Monthly budget",
        value = state.budget,
        theme = theme,
        hint = "Amount",
        placeholder = "₹2,00,000",
        keyboardType = KeyboardType.Decimal,
        onValueChange = { state.budget = formatIndianAmount(it) },
    )
    if (!own) {
        SetupField(
            label = "Media approved (optional)",
            value = state.mediaApproved,
            theme = theme,
            placeholder = "₹1,20,000",
            keyboardType = KeyboardType.Decimal,
            onValueChange = { state.mediaApproved = formatIndianAmount(it) },
        )
    }
    SetupField(
        label = "Monthly target",
        value = state.target,
        theme = theme,
        hint = "Optional",
        placeholder = "120",
        keyboardType = KeyboardType.Decimal,
        onValueChange = { state.target = formatIndianAmount(it) },
    )
    SetupChipRow(
        label = "How the target is measured",
        options = MarketingSetupDocumentSpec.subtype(state.subtype).targetMetrics
            .map { MarketingSetupDocumentSpec.metricLabel(it) },
        selected = setOf(state.metric),
        theme = theme,
        onSelect = { state.metric = it },
    )
    SetupFootnote(
        "Unit · ${MarketingSetupDocumentSpec.metricUnit(MarketingSetupDocumentSpec.metricCode(state.metric))}",
        theme,
    )
    val budgetOk = plainAmount(state.budget).toDoubleOrNull().let { it != null && it > 0 }
    SetupStatusPill(
        text = if (budgetOk && state.hasChannels) "Channels and budget look good" else "Add channels and a budget",
        theme = theme,
        tone = if (budgetOk && state.hasChannels) theme.positive else theme.muted,
    )
}

@Composable
private fun MarketingSection05(
    state: MarketingSetupDocState,
    theme: SetupDocTheme,
    onPicker: (SetupPickerTarget) -> Unit,
) {
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
            options = MarketingSetupDocumentSpec.gstTrackingLabels,
            selected = setOf(state.gstTracking),
            theme = theme,
            onSelect = { state.gstTracking = it },
        )
    }
    if (state.gstTracking.startsWith("Yes", ignoreCase = true)) {
        SetupPanel("Registration", theme) {
            SetupChipRow(
                label = "How is your business registered for GST?",
                options = MarketingSetupDocumentSpec.gstRegistrationLabels,
                selected = setOf(state.gstRegistration),
                theme = theme,
                onSelect = { state.gstRegistration = it },
            )
            SetupField(
                label = "GSTIN",
                value = state.gstin,
                theme = theme,
                hint = IndiaStateCatalog.gstinStateHint(state.gstin, state.gstState) ?: "15-digit GSTIN",
                placeholder = "29ABCDE4321T1Z5",
                onValueChange = { state.gstin = it.uppercase() },
            )
            SetupPickerField(
                label = "Registered state",
                value = state.gstState,
                theme = theme,
                placeholder = "Pick a state",
                onClick = { onPicker(SetupPickerTarget.GST_STATE) },
            )
        }
        SetupPanel("Pricing", theme) {
            SetupChipRow(
                label = "Do prices include GST?",
                options = MarketingSetupDocumentSpec.gstPricingLabels,
                selected = setOf(state.gstPricing),
                theme = theme,
                onSelect = { state.gstPricing = it },
            )
            SetupPickerField(
                label = "Effective from (optional)",
                value = if (state.gstEffectiveFrom.isBlank()) {
                    ""
                } else {
                    MarketingSetupDocumentSpec.setupDateLabel(state.gstEffectiveFrom)
                },
                theme = theme,
                placeholder = "YYYY-MM-DD",
                onClick = { onPicker(SetupPickerTarget.GST_EFFECTIVE) },
            )
        }
    }
}

@Composable
private fun MarketingSection06(
    state: MarketingSetupDocState,
    theme: SetupDocTheme,
    ownerName: String,
    submitting: Boolean,
    momentStatus: String,
    own: Boolean,
    onStart: () -> Unit,
) {
    val owner = ownerName.ifBlank { "Owner" }
    SetupPanel("Moment", theme) {
        Text(
            state.campaignName.ifBlank { "New campaign" },
            color = theme.ink,
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
        )
        Text(
            "${MarketingSetupDocumentSpec.kindLabel(state.subtype)} · ${MarketingSetupDocumentSpec.currencyCode(state.currency)}",
            color = theme.muted,
            fontFamily = PlusJakartaSans,
            fontSize = 12.sp,
        )
    }
    SetupPanel("Summary", theme) {
        SetupKeyValue(
            if (own) "Brand" else "Client",
            state.partyName.ifBlank { "Not set" },
            theme,
        )
        if (!own) SetupKeyValue("Client fee", state.clientFee.ifBlank { "—" }, theme)
        if (!own) SetupKeyValue("Client media budget", state.clientApprovedMediaBudget.ifBlank { "—" }, theme)
        if (!own) SetupKeyValue("Agency budget", state.agencyOperatingBudget.ifBlank { "—" }, theme)
        if (!own) SetupKeyValue("Reporting", state.reportingCadence.ifBlank { "—" }, theme)
        SetupKeyValue("Creative approval", if (state.creativeApprovalRequired) "Required" else "Not required", theme)
        if (own) SetupKeyValue("Offering", state.offering.ifBlank { "—" }, theme)
        SetupKeyValue("People", "$owner · Owner", theme)
        SetupKeyValue("Budget", state.budget.ifBlank { "Not set" }, theme)
        SetupKeyValue("Target measure", state.metric, theme)
        SetupKeyValue(
            "Channels",
            if (state.hasChannels) state.orderedChannels.joinToString(", ") else "None yet",
            theme,
        )
    }
    SetupStatusPill(
        text = if (state.canStart) "Ready to activate" else "A few fields still need you",
        theme = theme,
        tone = if (state.canStart) theme.positive else theme.muted,
    )
    Box(Modifier.testTag(MaestroIds.BUSINESS_SETUP_SUBMIT)) {
        SetupCta(
            label = if (submitting) {
                MarketingSetupDocumentSpec.activatingLabel(momentStatus)
            } else {
                MarketingSetupDocumentSpec.activateLabel(momentStatus)
            },
            theme = theme,
            enabled = state.canStart && !submitting,
            accentShadow = true,
            onClick = onStart,
        )
    }
}

@Composable
private fun MarketingCurrencySheet(
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
            MarketingSetupDocumentSpec.currencyLabels().forEach { label ->
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
                    Text(label, color = theme.ink, fontFamily = PlusJakartaSans, fontSize = 14.sp, modifier = Modifier.weight(1f))
                    if (on) Text("●", color = theme.accent, fontSize = 10.sp)
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

private fun String.marketingLocalDateOrNull(): LocalDate? =
    runCatching { LocalDate.parse(take(10)) }.getOrNull()
