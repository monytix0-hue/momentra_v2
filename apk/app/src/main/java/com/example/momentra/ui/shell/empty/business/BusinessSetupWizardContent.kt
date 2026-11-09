package com.example.momentra.ui.shell.empty.business

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.analytics.MomentraAnalytics
import com.example.momentra.data.api.ApiClient
import com.example.momentra.data.api.CompanyInviteDto
import com.example.momentra.data.api.MintCompanyInviteBody
import com.example.momentra.data.repository.BusinessSliceRepository
import com.example.momentra.domain.CreateMomentOutcome
import com.example.momentra.ui.create.MomentCreateViewModel
import com.example.momentra.ui.shell.business.shared.CompanyJoinLink
import com.example.momentra.ui.shell.business.shared.OperatingMasterTab
import com.example.momentra.ui.shell.business.shared.OperatingMomentCatalog
import com.example.momentra.ui.shell.business.shared.OperatingSubtype
import com.example.momentra.ui.shell.empty.group.generateInviteQrBitmap
import com.example.momentra.ui.shell.empty.group.inviteMessage
import com.example.momentra.ui.shell.empty.group.sendInviteWhatsApp
import com.example.momentra.ui.shell.empty.group.shareInviteQr
import com.example.momentra.ui.shell.maestro.MaestroIds
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

private val Card = Color(0xFF161B26)
private val Border = Color(0xFF1E293B)
private val Muted = Color(0xFF94A3B8)
private val TextPrimary = Color(0xFFE5E0EE)

/** How long `inviteFor` waits on a mint already in flight before giving up on it. */
private const val MINT_WAIT_MS = 15_000L

/**
 * Entry point for every business setup. `TRADING_DISTRIBUTION` renders the six-section
 * `TRD-SETUP-v6` / `DST-SETUP-v6` document; the remaining families keep the four-step form.
 */
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
    DisposableEffect(kind) {
        MomentraAnalytics.get().onScreenEnter(kind.analyticsScreen)
        onDispose { MomentraAnalytics.get().onScreenExit(kind.analyticsScreen) }
    }
    when (kind) {
        BusinessSetupKind.TRADING_DISTRIBUTION,
        BusinessSetupKind.MANUFACTURING_PRODUCTION,
        BusinessSetupKind.RETAIL_CONSUMER,
        -> TradingDistributionSetupContent(
            kind = kind,
            companyId = companyId,
            createViewModel = createViewModel,
            submitting = submitting,
            error = error,
            onClose = onClose,
            onCreated = onCreated,
            modifier = modifier,
            editingMomentId = editingMomentId,
            initialTitle = initialTitle,
            editingMomentStatus = editingMomentStatus,
            ownerName = ownerName,
        )
        BusinessSetupKind.MARKETING_CAMPAIGNS -> MarketingCampaignsSetupContent(
            kind = kind,
            companyId = companyId,
            createViewModel = createViewModel,
            submitting = submitting,
            error = error,
            onClose = onClose,
            onCreated = onCreated,
            modifier = modifier,
            editingMomentId = editingMomentId,
            initialTitle = initialTitle,
            editingMomentStatus = editingMomentStatus,
            ownerName = ownerName,
        )
        else -> LegacySetupWizardContent(
            kind = kind,
            companyId = companyId,
            createViewModel = createViewModel,
            submitting = submitting,
            error = error,
            onClose = onClose,
            onCreated = onCreated,
            modifier = modifier,
            editingMomentId = editingMomentId,
            initialTitle = initialTitle,
            editingMomentStatus = editingMomentStatus,
        )
    }
}

/**
 * The six-section document, plus the draft lifecycle it needs.
 *
 * A draft moment exists from the moment the document opens: §04's five rows open real
 * masters sheets scoped to it, and §06 promotes it. Edits are saved to the draft as the
 * operator works, so activation only has to flip the status.
 */
@Composable
private fun TradingDistributionSetupContent(
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
    val state = remember(kind, editingMomentId) {
        SetupDocState(
            subtypeCode = subtypes.first().code,
            initialName = initialTitle.orEmpty(),
        )
    }

    var draftId by remember(kind, editingMomentId) { mutableStateOf(editingMomentId) }
    var draftCompanyId by remember(kind, editingMomentId) {
        mutableStateOf(companyId.takeIf { it.isNotBlank() })
    }
    // A document that was opened for a new setup is a draft; one reopened over an existing moment
    // edits that moment in place. Only a draft may be discarded or promoted.
    var momentStatus by remember(kind, editingMomentId) {
        mutableStateOf(
            if (editingMomentId == null) {
                "DRAFT"
            } else {
                editingMomentStatus?.trim()?.uppercase()?.takeIf { it.isNotEmpty() } ?: "ACTIVE"
            },
        )
    }
    var starting by remember(kind) { mutableStateOf(false) }
    var started by remember(kind) { mutableStateOf(false) }
    // Close can beat the draft POST. The moment still gets created server-side, so the document
    // remembers it was abandoned and discards the late arrival instead of orphaning a draft.
    var abandoned by remember(kind) { mutableStateOf(false) }
    var counts by remember(kind) { mutableStateOf(SetupMasterCounts()) }
    var warehouseNames by remember(kind) { mutableStateOf<List<String>>(emptyList()) }
    var reloadKey by remember(kind) { mutableIntStateOf(0) }
    var sheetOpen by remember(kind) { mutableStateOf(false) }
    var sheetTab by remember(kind) { mutableStateOf(OperatingMasterTab.Catalogue) }
    var sheetForm by remember(kind) { mutableStateOf<OperatingMasterTab?>(null) }

    // §02 mints a real invite in-setup. It is company-scoped, so the lazily created setup company
    // is the first thing an invite touch needs; the code then rides the sheet and the card.
    var setupInvite by remember(kind, editingMomentId) { mutableStateOf<CompanyInviteDto?>(null) }
    var mintingInvite by remember(kind) { mutableStateOf(false) }
    var inviteError by remember(kind) { mutableStateOf<String?>(null) }
    var inviteSheetOpen by remember(kind) { mutableStateOf(false) }

    val repo = remember { BusinessSliceRepository() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Opening the document creates its draft so §04 has somewhere to write.
    LaunchedEffect(kind, editingMomentId) {
        if (draftId != null) return@LaunchedEffect
        createViewModel.submitBusinessSetup(
            kind = kind,
            companyId = companyId.takeIf { it.isNotBlank() },
            preferences = state.preferences(ownerName),
            title = state.businessName.ifBlank { kind.title },
            subtype = state.subtype,
            status = "DRAFT",
            onSuccess = { outcome ->
                if (abandoned) {
                    // Nobody is looking at this draft any more; leaving it behind would show an
                    // empty setup in the moment directory.
                    createViewModel.discardMomentDraft(outcome.momentId) {}
                } else {
                    draftId = outcome.momentId
                }
            },
        )
    }

    // The draft carries the company, and that is what the masters endpoints need. Reopening an
    // existing draft or editing an active moment is prefilled from what was saved, so the
    // document never starts blank over a live record.
    LaunchedEffect(draftId) {
        state.draftMomentId = draftId
        val id = draftId ?: return@LaunchedEffect
        // A resumed draft or an edited moment is prefilled from what was saved; a draft this
        // screen just created only has to contribute its company.
        val wantsPrefill = editingMomentId != null || state.businessName.isBlank()
        val prefill = if (wantsPrefill || draftCompanyId == null) {
            createViewModel.getDomainSetupPrefill(id)
        } else {
            null
        }
        if (wantsPrefill) {
            prefill?.preferences?.let { state.applyPrefill(it) }
            val saved = prefill?.title?.trim().orEmpty()
            if (saved.isNotEmpty() && state.businessName.isBlank()) {
                state.businessName = saved
            }
        }
        // The wire's status is authoritative: `Close` must not discard a live moment, and §06
        // must not report `Start` over one that is already running.
        prefill?.status?.trim()?.uppercase()?.takeIf { it.isNotEmpty() }?.let { momentStatus = it }
        prefill?.companyId?.takeIf { it.isNotBlank() }?.let { draftCompanyId = it }
        state.companyId = draftCompanyId
    }

    LaunchedEffect(draftCompanyId, draftId, reloadKey) {
        val cid = draftCompanyId ?: return@LaunchedEffect
        repo.listMasters(cid, momentId = draftId).onSuccess { masters ->
            val byKind = masters.groupBy { it.kind.uppercase() }
            fun count(vararg kinds: String) = kinds.sumOf { byKind[it].orEmpty().size }
            counts = counts.copy(
                skus = count("SKU", "PRODUCT", "BATCH"),
                warehouses = count("WAREHOUSE", "LOCATION"),
                routes = count("ROUTE"),
                principals = count("PRINCIPAL", "SUPPLIER"),
                dealers = count("DEALER", "BUYER", "CUSTOMER"),
            )
            warehouseNames = masters
                .filter { it.kind.uppercase() in setOf("WAREHOUSE", "LOCATION") }
                .map { it.name }
            repo.listPriceLists(cid, momentId = draftId).onSuccess { lists ->
                counts = counts.copy(priceLists = lists.size)
            }
        }
    }

    // Written from an effect, not during composition: `state.companyId` is snapshot-backed, and
    // writing it while composing would schedule a recomposition on every pass.
    LaunchedEffect(draftCompanyId) {
        state.companyId = draftCompanyId
    }

    // A debounced save keeps the draft in step with the document, so a close mid-way does not
    // lose §01-§05 and §06 only has to flip the status. A live moment has no draft row to patch,
    // so its edits ride the §06 submit instead.
    val revision = with(state) {
        listOf(
            subtype, businessName, companyName, gstin, currency, startDate, endDate,
            description, location, channels.joinToString("|"), month, budget, target, metric,
            inviteRole, gstTracking, gstRegistration, gstState, gstPricing, gstEffectiveFrom,
            invitees.joinToString("|") { it.name + ":" + it.roleLabel },
        ).joinToString("\u0001")
    }
    LaunchedEffect(draftId, revision, momentStatus) {
        val id = draftId ?: return@LaunchedEffect
        if (!SetupDocumentSpec.isPromotable(momentStatus)) return@LaunchedEffect
        delay(700)
        createViewModel.saveBusinessSetupDraft(
            momentId = id,
            preferences = state.preferences(ownerName),
            title = state.businessName.ifBlank { kind.title },
        )
    }

    // The invite is company-scoped, and a fresh draft may not have a company yet. Only mint when
    // §02 actually asks for a share, so an untouched document leaves no company or invite behind.
    suspend fun mintInvite(roleCode: String): CompanyInviteDto? {
        val draft = draftId
        val cid = draftCompanyId ?: if (draft != null) {
            createViewModel.ensureSetupCompany(
                momentId = draft,
                companyName = state.businessName.ifBlank { kind.title },
                taxIdentifier = state.gstin,
            )?.also {
                draftCompanyId = it
                state.companyId = it
            }
        } else {
            null
        } ?: return null
        return runCatching {
            ApiClient.apiService.mintCompanyInvite(
                idempotencyKey = UUID.randomUUID().toString(),
                body = MintCompanyInviteBody(
                    companyId = cid,
                    membershipType = SetupDocumentSpec.membershipType(roleCode),
                    operatingRole = roleCode,
                ),
            ).data
        }.getOrElse { failure ->
            inviteError = failure.message ?: "Could not mint an invite"
            null
        }
    }

    /**
     * The code for this role, minting one if §02 does not hold it yet. Suspending, so a share can
     * await the mint instead of no-op'ing on the first tap — the same shape the group setup's
     * `ensureInvite()` gives `shareQr()` and `shareWhatsApp()`.
     */
    suspend fun inviteFor(roleCode: String = state.inviteRole): CompanyInviteDto? {
        val current = setupInvite
        // A code already minted for this role is reusable; only a role change needs a new one.
        if (current != null && current.operatingRole == roleCode) return current
        // Another mint is in flight. Wait it out rather than firing a second request; the wait is
        // bounded so a mint that never settles cannot spin this caller forever.
        var waitedMs = 0
        while (mintingInvite && waitedMs < MINT_WAIT_MS) {
            delay(120)
            waitedMs += 120
            val settled = setupInvite
            if (settled != null && settled.operatingRole == roleCode) return settled
        }
        mintingInvite = true
        inviteError = null
        val minted = mintInvite(roleCode)
        if (minted != null) setupInvite = minted
        mintingInvite = false
        return minted
    }

    /** Fire-and-forget open path: the sheet reports the mint's progress through `mintingInvite`. */
    fun ensureInvite(roleCode: String = state.inviteRole) {
        scope.launch { inviteFor(roleCode) }
    }

    // Opening §02's sheet is the first touch that needs a code, so the mint starts there rather
    // than on open of the document — the same on-demand shape the group setup uses.

    Box(modifier.fillMaxSize()) {
        TradingSetupDocument(
            state = state,
            ownerName = ownerName,
            counts = counts,
            warehouseNames = warehouseNames,
            submitting = submitting && starting,
            error = error,
            onClose = {
                // A draft nobody starts is not left behind as an orphan; a live moment is simply
                // closed, because discarding it would delete a record still in use.
                val draft = draftId
                if (draft != null && !started && SetupDocumentSpec.isDiscardable(momentStatus)) {
                    createViewModel.discardMomentDraft(draft) { onClose() }
                } else {
                    // No draft yet means the create is still in flight; its result is discarded
                    // when it lands.
                    if (draft == null) abandoned = true
                    onClose()
                }
            },
            onOpenMasters = { tab, form ->
                // §04 writes are company-scoped, and a freshly created draft may not have one
                // yet. Create it before the sheet opens — the sheet is built once and would sit
                // on "needs a company" otherwise — but only when a row is actually used, so an
                // untouched document leaves no company behind.
                val draft = draftId
                if (draftCompanyId == null && draft != null) {
                    scope.launch {
                        val cid = createViewModel.ensureSetupCompany(
                            momentId = draft,
                            companyName = state.businessName.ifBlank { kind.title },
                            taxIdentifier = state.gstin,
                        )
                        if (cid != null) {
                            draftCompanyId = cid
                            state.companyId = cid
                        }
                        sheetTab = tab
                        sheetForm = form
                        sheetOpen = true
                    }
                } else {
                    sheetTab = tab
                    sheetForm = form
                    sheetOpen = true
                }
            },
            momentStatus = momentStatus,
            onStart = {
                val draft = draftId
                if (starting || draft == null) return@TradingSetupDocument
                starting = true
                createViewModel.submitBusinessSetup(
                    kind = kind,
                    companyId = draftCompanyId,
                    preferences = state.preferences(ownerName),
                    title = state.businessName.trim().ifBlank { kind.title },
                    subtype = state.subtype,
                    editingMomentId = draft,
                    editingMomentStatus = momentStatus,
                    status = "ACTIVE",
                    companyName = state.companyName,
                    taxIdentifier = state.gstin,
                    onSuccess = { outcome ->
                        starting = false
                        started = true
                        onCreated(outcome)
                    },
                )
            },
            onOpenInvite = {
                inviteSheetOpen = true
                // Opening §02's sheet is the first touch that needs a code, so the mint starts
                // here — the same on-demand shape the group setup uses.
                ensureInvite()
            },
            onShareInviteQr = {
                scope.launch {
                    // A share never depends on the sheet having been opened: mint, then share the
                    // QR the code draws, exactly like the group setup card.
                    val code = inviteFor()?.inviteCode?.takeIf { it.isNotBlank() } ?: return@launch
                    val px = (156 * context.resources.displayMetrics.density).toInt()
                    val bitmap = generateInviteQrBitmap(CompanyJoinLink.qrPayload(code), px)
                    shareInviteQr(context, bitmap, CompanyJoinLink.displayPath(code))
                }
            },
            onWhatsAppInvite = {
                scope.launch {
                    val code = inviteFor()?.inviteCode?.takeIf { it.isNotBlank() } ?: return@launch
                    sendInviteWhatsApp(
                        context,
                        null,
                        inviteMessage(
                            state.businessName.ifBlank { kind.title },
                            CompanyJoinLink.displayPath(code),
                        ),
                    )
                }
            },
            mintingInvite = mintingInvite,
            inviteError = inviteError,
        )
        SetupMastersHost(
            state = state,
            open = sheetOpen,
            tab = sheetTab,
            initialForm = sheetForm,
            onDismiss = { sheetOpen = false },
            onChanged = { reloadKey += 1 },
        )
        if (inviteSheetOpen) {
            BusinessSetupInviteSheet(
                title = state.businessName.ifBlank { kind.title },
                roleCode = state.inviteRole,
                invite = setupInvite,
                minting = mintingInvite,
                error = inviteError,
                theme = setupDocTheme(state.subtype),
                existingNames = state.invitees.map { it.name },
                subtypeCode = state.subtype,
                onRoleChange = { code ->
                    state.inviteRole = code
                    // A code grants the role it was minted with, so a role change must re-mint.
                    ensureInvite(code)
                },
                onAddPerson = { invitee ->
                    if (state.invitees.none { it.name.equals(invitee.name, ignoreCase = true) }) {
                        state.invitees = state.invitees + invitee
                    }
                },
                onRetry = { ensureInvite(state.inviteRole) },
                onDismiss = { inviteSheetOpen = false },
            )
        }
    }
}

@Composable
private fun MarketingCampaignsSetupContent(
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
    val state = remember(kind, editingMomentId) {
        MarketingSetupDocState(
            subtypeCode = subtypes.first().code,
            initialName = initialTitle.orEmpty(),
        )
    }

    var draftId by remember(kind, editingMomentId) { mutableStateOf(editingMomentId) }
    var draftCompanyId by remember(kind, editingMomentId) {
        mutableStateOf(companyId.takeIf { it.isNotBlank() })
    }
    var momentStatus by remember(kind, editingMomentId) {
        mutableStateOf(
            if (editingMomentId == null) {
                "DRAFT"
            } else {
                editingMomentStatus?.trim()?.uppercase()?.takeIf { it.isNotEmpty() } ?: "ACTIVE"
            },
        )
    }
    var starting by remember(kind) { mutableStateOf(false) }
    var started by remember(kind) { mutableStateOf(false) }
    var abandoned by remember(kind) { mutableStateOf(false) }
    var counts by remember(kind) { mutableStateOf(SetupMasterCounts()) }
    var reloadKey by remember(kind) { mutableIntStateOf(0) }
    var sheetOpen by remember(kind) { mutableStateOf(false) }
    var sheetTab by remember(kind) { mutableStateOf(OperatingMasterTab.Catalogue) }
    var sheetForm by remember(kind) { mutableStateOf<OperatingMasterTab?>(null) }

    var setupInvite by remember(kind, editingMomentId) { mutableStateOf<CompanyInviteDto?>(null) }
    var mintingInvite by remember(kind) { mutableStateOf(false) }
    var inviteError by remember(kind) { mutableStateOf<String?>(null) }
    var inviteSheetOpen by remember(kind) { mutableStateOf(false) }

    val repo = remember { BusinessSliceRepository() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(kind, editingMomentId) {
        if (draftId != null) return@LaunchedEffect
        createViewModel.submitBusinessSetup(
            kind = kind,
            companyId = companyId.takeIf { it.isNotBlank() },
            preferences = state.preferences(ownerName),
            title = state.campaignName.ifBlank { kind.title },
            subtype = state.subtype,
            status = "DRAFT",
            onSuccess = { outcome ->
                if (abandoned) {
                    createViewModel.discardMomentDraft(outcome.momentId) {}
                } else {
                    draftId = outcome.momentId
                }
            },
        )
    }

    LaunchedEffect(draftId) {
        state.draftMomentId = draftId
        val id = draftId ?: return@LaunchedEffect
        val wantsPrefill = editingMomentId != null || state.campaignName.isBlank()
        val prefill = if (wantsPrefill || draftCompanyId == null) {
            createViewModel.getDomainSetupPrefill(id)
        } else {
            null
        }
        if (wantsPrefill) {
            prefill?.preferences?.let { state.applyPrefill(it) }
            val saved = prefill?.title?.trim().orEmpty()
            if (saved.isNotEmpty() && state.campaignName.isBlank()) {
                state.campaignName = saved
            }
        }
        prefill?.status?.trim()?.uppercase()?.takeIf { it.isNotEmpty() }?.let { momentStatus = it }
        prefill?.companyId?.takeIf { it.isNotBlank() }?.let { draftCompanyId = it }
        state.companyId = draftCompanyId
    }

    LaunchedEffect(draftCompanyId, draftId, reloadKey) {
        val cid = draftCompanyId ?: return@LaunchedEffect
        repo.listMasters(cid, momentId = draftId).onSuccess { masters ->
            val byKind = masters.groupBy { it.kind.uppercase() }
            fun count(vararg kinds: String) = kinds.sumOf { byKind[it].orEmpty().size }
            counts = counts.copy(
                skus = count("SKU", "PRODUCT", "BATCH"),
                warehouses = count("WAREHOUSE", "LOCATION"),
                routes = count("ROUTE"),
                principals = count("PRINCIPAL", "SUPPLIER", "VENDOR", "CLIENT"),
                dealers = count("DEALER", "BUYER", "CUSTOMER", "CREATOR"),
            )
        }
    }

    LaunchedEffect(draftCompanyId) {
        state.companyId = draftCompanyId
    }

    val revision = with(state) {
        listOf(
            subtype, campaignName, partyName, offering, purpose, description, currency,
            startDate, endDate, gstin, channels.joinToString("|"), budget, clientFee,
            mediaApproved, target, metric, inviteRole, gstTracking, gstRegistration,
            gstState, gstPricing, gstEffectiveFrom,
            invitees.joinToString("|") { it.name + ":" + it.roleCode },
        ).joinToString("\u0001")
    }
    LaunchedEffect(draftId, revision, momentStatus) {
        val id = draftId ?: return@LaunchedEffect
        if (!MarketingSetupDocumentSpec.isPromotable(momentStatus)) return@LaunchedEffect
        delay(700)
        createViewModel.saveBusinessSetupDraft(
            momentId = id,
            preferences = state.preferences(ownerName),
            title = state.campaignName.ifBlank { kind.title },
        )
    }

    suspend fun mintInvite(roleCode: String): CompanyInviteDto? {
        val draft = draftId
        val cid = draftCompanyId ?: if (draft != null) {
            createViewModel.ensureSetupCompany(
                momentId = draft,
                companyName = state.campaignName.ifBlank { kind.title },
                taxIdentifier = state.gstin,
            )?.also {
                draftCompanyId = it
                state.companyId = it
            }
        } else {
            null
        } ?: return null
        return runCatching {
            ApiClient.apiService.mintCompanyInvite(
                idempotencyKey = UUID.randomUUID().toString(),
                body = MintCompanyInviteBody(
                    companyId = cid,
                    membershipType = MarketingSetupDocumentSpec.membershipType(roleCode),
                    operatingRole = roleCode,
                ),
            ).data
        }.getOrElse { failure ->
            inviteError = failure.message ?: "Could not mint an invite"
            null
        }
    }

    suspend fun inviteFor(roleCode: String = state.inviteRole): CompanyInviteDto? {
        val current = setupInvite
        if (current != null && current.operatingRole == roleCode) return current
        var waitedMs = 0
        while (mintingInvite && waitedMs < MINT_WAIT_MS) {
            delay(120)
            waitedMs += 120
            val settled = setupInvite
            if (settled != null && settled.operatingRole == roleCode) return settled
        }
        mintingInvite = true
        inviteError = null
        val minted = mintInvite(roleCode)
        if (minted != null) setupInvite = minted
        mintingInvite = false
        return minted
    }

    fun ensureInvite(roleCode: String = state.inviteRole) {
        scope.launch { inviteFor(roleCode) }
    }

    Box(modifier.fillMaxSize()) {
        MarketingSetupDocument(
            state = state,
            ownerName = ownerName,
            counts = counts,
            submitting = submitting && starting,
            error = error,
            onClose = {
                val draft = draftId
                if (draft != null && !started && MarketingSetupDocumentSpec.isDiscardable(momentStatus)) {
                    createViewModel.discardMomentDraft(draft) { onClose() }
                } else {
                    if (draft == null) abandoned = true
                    onClose()
                }
            },
            onOpenMasters = { tab, form ->
                val draft = draftId
                if (draftCompanyId == null && draft != null) {
                    scope.launch {
                        val cid = createViewModel.ensureSetupCompany(
                            momentId = draft,
                            companyName = state.campaignName.ifBlank { kind.title },
                            taxIdentifier = state.gstin,
                        )
                        if (cid != null) {
                            draftCompanyId = cid
                            state.companyId = cid
                        }
                        sheetTab = tab
                        sheetForm = form
                        sheetOpen = true
                    }
                } else {
                    sheetTab = tab
                    sheetForm = form
                    sheetOpen = true
                }
            },
            momentStatus = momentStatus,
            onStart = {
                val draft = draftId
                if (starting || draft == null) return@MarketingSetupDocument
                starting = true
                createViewModel.submitBusinessSetup(
                    kind = kind,
                    companyId = draftCompanyId,
                    preferences = state.preferences(ownerName),
                    title = state.campaignName.trim().ifBlank { kind.title },
                    subtype = state.subtype,
                    editingMomentId = draft,
                    editingMomentStatus = momentStatus,
                    status = "ACTIVE",
                    companyName = state.companyName.ifBlank { state.campaignName },
                    taxIdentifier = state.gstin,
                    onSuccess = { outcome ->
                        starting = false
                        started = true
                        onCreated(outcome)
                    },
                )
            },
            onOpenInvite = {
                inviteSheetOpen = true
                ensureInvite()
            },
            onShareInviteQr = {
                scope.launch {
                    val code = inviteFor()?.inviteCode?.takeIf { it.isNotBlank() } ?: return@launch
                    val px = (156 * context.resources.displayMetrics.density).toInt()
                    val bitmap = generateInviteQrBitmap(CompanyJoinLink.qrPayload(code), px)
                    shareInviteQr(context, bitmap, CompanyJoinLink.displayPath(code))
                }
            },
            onWhatsAppInvite = {
                scope.launch {
                    val code = inviteFor()?.inviteCode?.takeIf { it.isNotBlank() } ?: return@launch
                    sendInviteWhatsApp(
                        context,
                        null,
                        inviteMessage(
                            state.campaignName.ifBlank { kind.title },
                            CompanyJoinLink.displayPath(code),
                        ),
                    )
                }
            },
            mintingInvite = mintingInvite,
            inviteError = inviteError,
        )
        MarketingSetupMastersHost(
            state = state,
            open = sheetOpen,
            tab = sheetTab,
            initialForm = sheetForm,
            onDismiss = { sheetOpen = false },
            onChanged = { reloadKey += 1 },
        )
        if (inviteSheetOpen) {
            BusinessSetupInviteSheet(
                title = state.campaignName.ifBlank { kind.title },
                roleCode = state.inviteRole,
                invite = setupInvite,
                minting = mintingInvite,
                error = inviteError,
                theme = marketingSetupDocTheme(state.subtype),
                existingNames = state.invitees.map { it.name },
                subtypeCode = state.subtype,
                onRoleChange = { code ->
                    state.inviteRole = code
                    ensureInvite(code)
                },
                onAddPerson = { invitee ->
                    if (state.invitees.none { it.name.equals(invitee.name, ignoreCase = true) }) {
                        state.invitees = state.invitees + invitee
                    }
                },
                onRetry = { ensureInvite(state.inviteRole) },
                onDismiss = { inviteSheetOpen = false },
            )
        }
    }
}

@Composable
private fun LegacySetupWizardContent(
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
                        companyId = companyId.takeIf { it.isNotBlank() },
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

internal fun plainAmount(raw: String): String = buildString {
    var dot = false
    for (c in raw) {
        if (c.isDigit()) append(c)
        else if (c == '.' && !dot) {
            append('.')
            dot = true
        }
    }
}

internal fun formatIndianAmount(raw: String): String {
    val cleaned = plainAmount(raw)
    if (cleaned.isEmpty()) return ""
    val dot = cleaned.indexOf('.')
    val intRaw = if (dot >= 0) cleaned.substring(0, dot) else cleaned
    val frac = if (dot >= 0) cleaned.substring(dot).take(3) else ""
    val digits = intRaw.trimStart('0').ifEmpty { if (intRaw.isNotEmpty() || frac.isNotEmpty()) "0" else "" }
    if (digits.isEmpty()) return ""
    return indianGroup(digits) + frac
}

private fun indianGroup(digits: String): String {
    if (digits.length <= 3) return digits
    val head = digits.dropLast(3)
    val tail = digits.takeLast(3)
    return head.reversed().chunked(2).joinToString(",").reversed() + "," + tail
}
