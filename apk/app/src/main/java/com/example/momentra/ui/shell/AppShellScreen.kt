package com.example.momentra.ui.shell

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext
import com.example.momentra.data.local.AppPreferences
import com.example.momentra.data.local.PendingJoinInvite
import com.example.momentra.data.local.PendingDeepLink
import com.example.momentra.domain.AppContext
import com.example.momentra.domain.BottomDestination
import com.example.momentra.domain.CompanySummary
import com.example.momentra.domain.MomentExperienceKind
import com.example.momentra.domain.MomentSummary
import com.example.momentra.domain.ShellContentState
import com.example.momentra.domain.ShellIdentity
import com.example.momentra.domain.isActiveStatus
import com.example.momentra.ui.shell.components.ContextSwitcher
import com.example.momentra.ui.shell.components.EditMomentSetupHost
import com.example.momentra.ui.shell.components.ManageMomentSheet
import com.example.momentra.ui.shell.components.MomentSwitcher
import com.example.momentra.ui.shell.components.MomentraTopBar
import com.example.momentra.ui.shell.components.MomentraTopBarConfig
import com.example.momentra.ui.shell.components.ShellBottomNavigation
import com.example.momentra.ui.shell.components.label
import com.example.momentra.ui.shell.empty.ContextEmptyExperience
import com.example.momentra.ui.shell.empty.GroupCreateFlow
import com.example.momentra.ui.shell.empty.group.GroupCreatePhase
import com.example.momentra.ui.shell.empty.group.GroupJoinConfirmSheet
import com.example.momentra.ui.shell.empty.group.GroupJoinQrScanner
import com.example.momentra.ui.shell.empty.personal.PersonalCreateEmptyContent
import com.example.momentra.ui.shell.empty.BusinessCreateFlow
import com.example.momentra.ui.shell.empty.CompanySetupContent
import com.example.momentra.ui.shell.business.shared.BusinessActiveTheme
import com.example.momentra.ui.shell.business.shared.BusinessExpenseSheet
import com.example.momentra.ui.shell.business.shared.BusinessGapQuickAddSheet
import com.example.momentra.ui.shell.business.shared.BusinessInvoiceSheet
import com.example.momentra.ui.shell.business.life.BusinessLifeActiveContent
import com.example.momentra.ui.shell.business.shared.BusinessMembersSheet
import com.example.momentra.ui.shell.business.shared.BusinessMemoryActiveContent
import com.example.momentra.ui.shell.business.shared.BusinessMomentsActiveContent
import com.example.momentra.ui.shell.business.shared.BusinessPulseActiveContent
import com.example.momentra.ui.shell.business.shared.BusinessQuickAddHub
import com.example.momentra.ui.shell.business.shared.BusinessQuickAddKind
import com.example.momentra.ui.shell.business.shared.BusinessRevenueSheet
import com.example.momentra.ui.shell.business.ops.create.OpsGapQuickAddSheet
import com.example.momentra.ui.shell.business.ops.memory.OpsMemoryActiveContent
import com.example.momentra.ui.shell.business.ops.moments.OpsMomentsActiveContent
import com.example.momentra.ui.shell.business.ops.pulse.OpsPulseActiveContent
import com.example.momentra.ui.shell.business.ops.create.OpsQuickAddSheets
import com.example.momentra.ui.shell.business.runway.memory.RunwayMemoryActiveContent
import com.example.momentra.ui.shell.business.runway.moments.RunwayMomentsActiveContent
import com.example.momentra.ui.shell.business.runway.pulse.RunwayPulseActiveContent
import com.example.momentra.ui.shell.business.runway.create.RunwayQuickAddSheet
import com.example.momentra.ui.shell.business.teamops.create.TeamOpsGapQuickAddSheet
import com.example.momentra.ui.shell.business.teamops.memory.TeamOpsMemoryActiveContent
import com.example.momentra.ui.shell.business.teamops.moments.TeamOpsMomentsActiveContent
import com.example.momentra.ui.shell.business.teamops.pulse.TeamOpsPulseActiveContent
import com.example.momentra.ui.shell.business.teamops.create.TeamOpsQuickAddSheets
import com.example.momentra.ui.shell.group.shared.GroupBudgetSheet
import com.example.momentra.ui.shell.group.shared.GroupCollabKind
import com.example.momentra.ui.shell.group.shared.GroupCollabSheet
import com.example.momentra.ui.shell.group.shared.GroupContributionSheet
import com.example.momentra.ui.shell.group.shared.GroupInvitePeopleSheet
import com.example.momentra.ui.shell.group.shared.GroupSettlementSheet
import com.example.momentra.ui.shell.group.shared.GroupExpenseSheet
import com.example.momentra.ui.shell.group.life.GroupLifeActiveContent
import com.example.momentra.ui.shell.group.life.GroupLifeQuickAction
import com.example.momentra.ui.shell.group.shared.GroupMemoryActiveContent
import com.example.momentra.ui.shell.group.shared.GroupMomentsActiveContent
import com.example.momentra.ui.shell.group.shared.GroupParticipantsSheet
import com.example.momentra.ui.shell.group.shared.GroupPulseActiveContent
import com.example.momentra.ui.shell.group.shared.GroupQuickAddHub
import com.example.momentra.ui.shell.group.shared.GroupExpenseSplitsFlow
import com.example.momentra.ui.shell.group.shared.GroupFinanceDetailFlow
import com.example.momentra.ui.shell.group.shared.GroupExperienceFamily
import com.example.momentra.ui.shell.group.shared.groupExperienceFamilyFor
import com.example.momentra.ui.shell.group.shared.isThemedExperience
import com.example.momentra.ui.shell.group.shared.isThemedLiving
import com.example.momentra.ui.shell.group.shared.isThemedPurchase
import com.example.momentra.ui.shell.group.experience.create.ExperienceActiveTheme
import com.example.momentra.ui.shell.group.experience.create.ExperienceGapQuickAddSheet
import com.example.momentra.ui.shell.group.experience.memory.ExperienceMemoryActiveContent
import com.example.momentra.ui.shell.group.experience.moments.ExperienceMomentsActiveContent
import com.example.momentra.ui.shell.group.experience.pulse.ExperiencePulseActiveContent
import com.example.momentra.ui.shell.group.experience.create.ExperienceQuickAddHub
import com.example.momentra.ui.shell.group.experience.create.ExperienceQuickAddKind
import com.example.momentra.ui.shell.group.living.create.LivingActiveTheme
import com.example.momentra.ui.shell.group.living.create.LivingGapQuickAddSheet
import com.example.momentra.ui.shell.group.living.memory.LivingMemoryActiveContent
import com.example.momentra.ui.shell.group.living.moments.LivingMomentsActiveContent
import com.example.momentra.ui.shell.group.living.pulse.LivingPulseActiveContent
import com.example.momentra.ui.shell.group.living.create.LivingQuickAddHub
import com.example.momentra.ui.shell.group.living.create.LivingQuickAddKind
import com.example.momentra.ui.shell.group.purchase.create.PurchaseActiveTheme
import com.example.momentra.ui.shell.group.purchase.create.PurchaseGapQuickAddSheet
import com.example.momentra.ui.shell.group.purchase.memory.PurchaseMemoryActiveContent
import com.example.momentra.ui.shell.group.purchase.moments.PurchaseMomentsActiveContent
import com.example.momentra.ui.shell.group.purchase.pulse.PurchasePulseActiveContent
import com.example.momentra.ui.shell.group.purchase.create.PurchaseQuickAddHub
import com.example.momentra.ui.shell.group.purchase.create.PurchaseQuickAddKind
import com.example.momentra.ui.shell.group.wedding.create.WeddingGapQuickAddSheet
import com.example.momentra.ui.shell.group.wedding.memory.WeddingMemoryActiveContent
import com.example.momentra.ui.shell.group.wedding.moments.WeddingMomentsActiveContent
import com.example.momentra.ui.shell.group.wedding.pulse.WeddingPulseActiveContent
import com.example.momentra.ui.shell.group.wedding.create.WeddingQuickAddHub
import com.example.momentra.ui.shell.group.wedding.create.WeddingQuickAddKind
import com.example.momentra.ui.shell.perf.ShellPerf
import com.example.momentra.ui.shell.personal.future.create.FutureQuickAddKind
import com.example.momentra.ui.shell.personal.future.create.PersonalFutureQuickAddSheet
import com.example.momentra.ui.shell.personal.future.memory.PersonalFutureMemoryActiveContent
import com.example.momentra.ui.shell.personal.future.moments.PersonalFutureMomentsActiveContent
import com.example.momentra.ui.shell.personal.life.PersonalLifeActiveContent
import com.example.momentra.ui.shell.personal.lifeops.create.LifeOpsQuickAddKind
import com.example.momentra.ui.shell.personal.lifeops.create.MoneyQuickAddKind
import com.example.momentra.ui.shell.personal.lifeops.create.PersonalLifeOpsQuickAddSheet
import com.example.momentra.ui.shell.personal.lifeops.create.PersonalMoneyQuickAddSheet
import com.example.momentra.ui.shell.personal.lifeops.memory.PersonalLifeOpsMemoryActiveContent
import com.example.momentra.ui.shell.personal.lifeops.moments.PersonalLifeOpsMomentsActiveContent
import com.example.momentra.ui.shell.personal.lifeops.pulse.PersonalPulseActiveContent
import com.example.momentra.ui.shell.personal.lifestyle.create.PersonalLifestyleQuickAddSheet
import com.example.momentra.ui.shell.personal.lifestyle.memory.PersonalLifestyleMemoryActiveContent
import com.example.momentra.ui.shell.personal.lifestyle.moments.PersonalLifestyleMomentsActiveContent
import com.example.momentra.ui.shell.personal.relationships.create.PersonalRelationshipsActivityFlow
import com.example.momentra.ui.shell.personal.relationships.create.PersonalRelationshipsQuickAddSheet
import com.example.momentra.ui.shell.personal.relationships.memory.PersonalRelationshipsMemoryActiveContent
import com.example.momentra.ui.shell.personal.relationships.moments.PersonalRelationshipsMomentsActiveContent
import com.example.momentra.ui.shell.personal.relationships.pulse.PersonalRelationshipsPulseActiveContent
import com.example.momentra.ui.shell.personal.shared.LifestyleQuickAddKind
import com.example.momentra.ui.shell.personal.shared.PersonalExpenseFab
import com.example.momentra.ui.shell.personal.shared.PersonalMasterExpenseSheet
import com.example.momentra.ui.shell.personal.shared.PersonalPulseFamily
import com.example.momentra.ui.shell.personal.shared.PersonalQuickAddHub
import com.example.momentra.ui.shell.personal.shared.PersonalRecentActivityFlow
import com.example.momentra.ui.shell.personal.shared.RelationshipsQuickAddKind
import com.example.momentra.ui.shell.personal.shared.personalPulseFamilyFor
import com.example.momentra.ui.splash.MomentraWordmark
import com.example.momentra.ui.theme.MomentraBrandColors
import com.example.momentra.ui.theme.ShellTokens
import com.example.momentra.ui.theme.shell.GlobalSurfaceTheme
import android.widget.Toast

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppShellScreen(
    identity: ShellIdentity,
    onSignOut: () -> Unit,
    onSessionExpired: () -> Unit = {},
    shellViewModel: AppShellViewModel,
) {
    val context = LocalContext.current
    val prefs = remember { AppPreferences(context) }
    val state by shellViewModel.state.collectAsState()

    var pendingGroupJoinCode by remember { mutableStateOf<String?>(null) }

    // Cold + warm: hydrate prefs, then observe pending invite while shell is open.
    LaunchedEffect(state.identity?.userId) {
        if (state.identity?.userId == null) return@LaunchedEffect
        prefs.getPendingJoinCode()?.let { PendingJoinInvite.hydrate(it) }
        PendingJoinInvite.code.collect { offered ->
            if (offered.isNullOrBlank()) return@collect
            val code = PendingJoinInvite.consume(prefs) ?: return@collect
            pendingGroupJoinCode = code
        }
    }

    LaunchedEffect(state.identity?.userId) {
        if (state.identity?.userId == null) return@LaunchedEffect
        PendingDeepLink.link.collect { offered ->
            if (offered.isNullOrBlank()) return@collect
            val link = PendingDeepLink.consume() ?: return@collect
            val momentId = PendingDeepLink.parseMomentId(link) ?: return@collect
            shellViewModel.selectMoment(momentId)
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (state.selectedContext == AppContext.GROUP) {
            shellViewModel.refreshVisibleGroupTab()
        }
    }

    var moneyQa by remember { mutableStateOf<MoneyQuickAddKind?>(null) }
    var groupExpenseSheetOpen by remember { mutableStateOf(false) }
    var groupContributionSheetOpen by remember { mutableStateOf(false) }
    var groupSettlementSheetOpen by remember { mutableStateOf(false) }
    var groupBudgetSheetOpen by remember { mutableStateOf(false) }
    var groupParticipantsSheetOpen by remember { mutableStateOf(false) }
    var groupInviteSheetOpen by remember { mutableStateOf(false) }
    var groupCollabKind by remember { mutableStateOf<GroupCollabKind?>(null) }
    var groupFinanceOpen by remember { mutableStateOf(false) }
    var groupSplitsOpen by remember { mutableStateOf(false) }
    var weddingGapQa by remember { mutableStateOf<WeddingQuickAddKind?>(null) }
    var experienceGapQa by remember { mutableStateOf<ExperienceQuickAddKind?>(null) }
    var purchaseGapQa by remember { mutableStateOf<PurchaseQuickAddKind?>(null) }
    var livingGapQa by remember { mutableStateOf<LivingQuickAddKind?>(null) }
    var businessGapQa by remember { mutableStateOf<BusinessQuickAddKind?>(null) }
    var businessExpenseSheetOpen by remember { mutableStateOf(false) }
    var businessRevenueSheetOpen by remember { mutableStateOf(false) }
    var businessInvoiceSheetOpen by remember { mutableStateOf(false) }
    var businessMembersSheetOpen by remember { mutableStateOf(false) }
    var lifeOpsQa by remember { mutableStateOf<LifeOpsQuickAddKind?>(null) }
    LaunchedEffect(lifeOpsQa) {
        lifeOpsQa?.let { kind ->
            ShellPerf.instant("lifeops_sheet_open", mapOf("kind" to kind.name))
        }
    }
    var futureQa by remember { mutableStateOf<FutureQuickAddKind?>(null) }
    var lifestyleQa by remember { mutableStateOf<LifestyleQuickAddKind?>(null) }
    var relationshipsQa by remember { mutableStateOf<RelationshipsQuickAddKind?>(null) }
    var relationshipsActivityOpen by remember { mutableStateOf(false) }
    var recentActivityOpen by remember { mutableStateOf(false) }
    var newMomentOpen by remember { mutableStateOf(false) }
    var groupCreatePhase by remember { mutableStateOf(GroupCreatePhase.CHOOSER) }
    var preferGroupCreateFlow by remember { mutableStateOf(false) }
    var showManageMoment by remember { mutableStateOf(false) }
    var editSetupOpen by remember { mutableStateOf(false) }
    var topChromeExpanded by remember { mutableStateOf(true) }
    var showJoinQrScanner by remember { mutableStateOf(false) }
    val shellAccent = com.example.momentra.ui.theme.shell.ContextThemes.of(state.selectedContext).contextAccent
    val momentAccent = com.example.momentra.ui.theme.shell.MomentThemes.resolve(
        state.selectedContext,
        state.selectedMomentTypeCode,
    ).primary
    val lifeOpsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val futureSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val lifestyleSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val relationshipsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val openBusinessCreateMoment: () -> Unit = {
        newMomentOpen = true
    }
    val openNewMoment: () -> Unit = {
        when (state.selectedContext) {
            AppContext.PERSONAL -> {
                // Empty / no active Moment → Create chooser tab. Active Moment → overlay chooser for another Moment.
                if (state.selectedMomentId != null && state.contextContent is ShellContentState.Ready) {
                    newMomentOpen = true
                } else {
                    shellViewModel.selectBottomDestination(BottomDestination.CREATE)
                }
            }
            AppContext.GROUP -> {
                groupCreatePhase = GroupCreatePhase.CHOOSER
                preferGroupCreateFlow = true
                shellViewModel.selectBottomDestination(BottomDestination.CREATE)
            }
            AppContext.BUSINESS -> openBusinessCreateMoment()
            else -> shellViewModel.selectBottomDestination(BottomDestination.CREATE)
        }
    }
    LaunchedEffect(identity.userId) {
        shellViewModel.restorePreferredPersonalMomentId(
            prefs.getSelectedPersonalMomentId(identity.userId),
        )
        shellViewModel.bindIdentity(identity)
    }
    LaunchedEffect(identity.userId, state.selectedContext, state.selectedMomentId) {
        if (state.selectedContext == AppContext.PERSONAL) {
            prefs.setSelectedPersonalMomentId(identity.userId, state.selectedMomentId)
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ShellTokens.SurfaceContent),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(ShellTokens.TopBarBackground)
                .statusBarsPadding(),
        ) {
            AnimatedVisibility(
                visible = topChromeExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Column {
                    MomentraTopBar(
                        config = MomentraTopBarConfig(
                            context = state.selectedContext,
                            displayName = identity.displayName,
                            companies = state.companies,
                            selectedCompany = state.selectedCompany,
                            companyMenuOpen = state.companyMenuOpen,
                            life360Available = true,
                            globalCreateAvailable = true,
                            qrScanAvailable = true,
                            referAvailable = true,
                        ),
                        onCompanyMenuToggle = shellViewModel::toggleCompanyMenu,
                        onCompanySelected = shellViewModel::selectCompany,
                        onQrScan = { showJoinQrScanner = true },
                        onLife360 = { shellViewModel.openLife360(true) },
                        onNewMoment = openNewMoment,
                        onRefer = {
                            Toast.makeText(
                                context,
                                "Referrals coming soon",
                                Toast.LENGTH_SHORT,
                            ).show()
                        },
                        onAvatar = { shellViewModel.openProfile(true) },
                    )
                    ContextSwitcher(
                        selectedContext = state.selectedContext,
                        supportedContexts = state.supportedContexts,
                        onSelect = {
                            newMomentOpen = false
                            groupCreatePhase = GroupCreatePhase.CHOOSER
                            preferGroupCreateFlow = false
                            shellViewModel.selectContext(it)
                        },
                    )
                }
            }
            if (!topChromeExpanded) {
                CompactShellChrome(
                    onExpand = { topChromeExpanded = true },
                    onNewMoment = openNewMoment,
                    onAvatar = { shellViewModel.openProfile(true) },
                )
            }
            if (!newMomentOpen && state.showMomentSwitcher) {
                MomentSwitcher(
                    selectedTitle = state.selectedMomentTitle,
                    selectedMomentId = state.selectedMomentId,
                    activeMoments = state.moments.filter { it.isActiveStatus() }.map { it.momentId to it.title },
                    isEmpty = state.contextContent is ShellContentState.Empty,
                    isLoading = state.contextContent is ShellContentState.Loading,
                    accent = momentAccent,
                    onSelectMoment = shellViewModel::selectMoment,
                    onSettings = {
                        if (state.selectedMomentId != null) showManageMoment = true
                    },
                    onInvite = if (state.selectedContext == AppContext.GROUP) {
                        { groupInviteSheetOpen = true }
                    } else {
                        null
                    },
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { topChromeExpanded = !topChromeExpanded }
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = if (topChromeExpanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                    contentDescription = if (topChromeExpanded) "Collapse top bar" else "Expand top bar",
                    tint = Color.White.copy(alpha = 0.45f),
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            if (newMomentOpen && state.selectedContext == AppContext.PERSONAL) {
                PersonalCreateEmptyContent(
                    history = state.moments,
                    onMomentCreated = { momentId, title, momentTypeCode ->
                        newMomentOpen = false
                        shellViewModel.onMomentCreated(momentId, title, momentTypeCode)
                    },
                    onOpenExisting = { momentId ->
                        newMomentOpen = false
                        shellViewModel.selectMoment(momentId)
                    },
                )
            } else if (newMomentOpen && state.selectedContext == AppContext.BUSINESS) {
                if (state.selectedCompany == null) {
                    CompanySetupContent(
                        onClose = { newMomentOpen = false },
                        onActivated = shellViewModel::onCompanyCreated,
                    )
                } else {
                    BusinessCreateFlow(
                        companyId = state.selectedCompany!!.companyId,
                        onCreateBack = { newMomentOpen = false },
                        onMomentCreated = { momentId, title, momentTypeCode ->
                            newMomentOpen = false
                            shellViewModel.onMomentCreated(momentId, title, momentTypeCode)
                        },
                    )
                }
            } else {
                ShellDestinationContent(
                    context = state.selectedContext,
                    destination = state.bottomDestination,
                    content = state.contextContent,
                    experience = state.momentExperience,
                    moments = state.moments,
                    hasCompany = state.selectedCompany != null,
                    companyId = state.selectedCompany?.companyId,
                    companyName = state.selectedCompany?.displayName,
                    selectedMomentId = state.selectedMomentId,
                    selectedMomentTitle = state.selectedMomentTitle,
                    selectedMomentTypeCode = state.selectedMomentTypeCode,
                    personalTabRefreshToken = state.personalTabRefreshToken,
                    groupTabRefreshToken = state.groupTabRefreshToken,
                    businessTabRefreshToken = state.businessTabRefreshToken,
                    capabilities = state.capabilities,
                    onRetry = { shellViewModel.selectContext(state.selectedContext) },
                    onSessionExpired = onSessionExpired,
                    onCreateMoment = openNewMoment,
                    onCreateBack = {
                        if (state.selectedContext == AppContext.GROUP &&
                            groupCreatePhase != GroupCreatePhase.CHOOSER
                        ) {
                            groupCreatePhase = GroupCreatePhase.CHOOSER
                        } else {
                            groupCreatePhase = GroupCreatePhase.CHOOSER
                            preferGroupCreateFlow = false
                            shellViewModel.exitCreateDestination()
                        }
                    },
                    onCompanyActivated = shellViewModel::onCompanyCreated,
                    onMomentCreated = { id, title, momentTypeCode ->
                        groupCreatePhase = GroupCreatePhase.CHOOSER
                        preferGroupCreateFlow = false
                        shellViewModel.onMomentCreated(id, title, momentTypeCode)
                    },
                    onJoinGroupCode = { code -> pendingGroupJoinCode = code },
                    preferGroupCreateFlow = preferGroupCreateFlow,
                    onPreferGroupCreateFlow = { preferGroupCreateFlow = it },
                    groupCreatePhase = groupCreatePhase,
                    onGroupCreatePhase = { groupCreatePhase = it },
                    onOpenGroupCreateTab = {
                        preferGroupCreateFlow = true
                        shellViewModel.selectBottomDestination(BottomDestination.CREATE)
                    },
                    onAddExpense = {
                        when (state.selectedContext) {
                            AppContext.GROUP -> groupExpenseSheetOpen = true
                            AppContext.BUSINESS -> businessExpenseSheetOpen = true
                            else -> moneyQa = MoneyQuickAddKind.MASTER_EXPENSE
                        }
                    },
                    onAddContribution = { groupContributionSheetOpen = true },
                    onAddSettlement = { groupSettlementSheetOpen = true },
                    onAddBudget = { groupBudgetSheetOpen = true },
                    onAddParticipants = { groupParticipantsSheetOpen = true },
                    onAddInvite = { groupInviteSheetOpen = true },
                    onAddPlanning = { groupCollabKind = GroupCollabKind.PLANNING },
                    onAddBooking = { groupCollabKind = GroupCollabKind.BOOKING },
                    onAddPoll = { groupCollabKind = GroupCollabKind.POLL },
                    onAddUpdate = { groupCollabKind = GroupCollabKind.UPDATE },
                    onAddMemory = { groupCollabKind = GroupCollabKind.MEMORY },
                    onAddPurchaseItem = { groupCollabKind = GroupCollabKind.PURCHASE_ITEM },
                    onAddResident = { groupInviteSheetOpen = true },
                    onViewSplits = { groupSplitsOpen = true },
                    onOpenGroupFinance = { groupFinanceOpen = true },
                    onWeddingQuickAdd = { kind ->
                        weddingGapQa = kind
                    },
                    onExperienceQuickAdd = { kind ->
                        if (kind == ExperienceQuickAddKind.PARTICIPANT) {
                            groupInviteSheetOpen = true
                        } else {
                            experienceGapQa = kind
                        }
                    },
                    onPurchaseQuickAdd = { kind ->
                        if (kind == PurchaseQuickAddKind.CONTRIBUTOR) {
                            groupInviteSheetOpen = true
                        } else {
                            purchaseGapQa = kind
                        }
                    },
                    onLivingQuickAdd = { kind ->
                        if (kind == LivingQuickAddKind.RESIDENT) {
                            groupInviteSheetOpen = true
                        } else {
                            livingGapQa = kind
                        }
                    },
                    onBusinessQuickAdd = { kind ->
                        val typeCode = state.selectedMomentTypeCode
                            ?: state.moments.firstOrNull { it.momentId == state.selectedMomentId }?.momentTypeCode
                        val code = typeCode.orEmpty().uppercase()
                        val isRunway = code.contains("RUNWAY")
                        val isOps = code.contains("OPERATIONS") && !code.contains("TEAM")
                        when {
                            isRunway -> businessGapQa = kind
                            isOps -> businessGapQa = when (kind) {
                                BusinessQuickAddKind.EXPENSE -> BusinessQuickAddKind.SPEND_ENTRY
                                else -> kind
                            }
                            kind == BusinessQuickAddKind.EXPENSE ||
                                kind == BusinessQuickAddKind.REVENUE ||
                                kind == BusinessQuickAddKind.INVOICE -> Unit
                            else -> businessGapQa = kind
                        }
                    },
                    onAddRevenue = { businessRevenueSheetOpen = true },
                    onAddInvoice = { businessInvoiceSheetOpen = true },
                    onAddMembers = { businessMembersSheetOpen = true },
                    onOpenQuickAdd = { shellViewModel.selectBottomDestination(BottomDestination.CREATE) },
                    onViewBusinessReport = {
                        shellViewModel.selectBottomDestination(BottomDestination.PULSE)
                    },
                    onLifeOpsQuickAdd = { lifeOpsQa = it },
                    onMoneyQuickAdd = { moneyQa = it },
                    onFutureQuickAdd = { futureQa = it },
                    onLifestyleQuickAdd = { lifestyleQa = it },
                    onRelationshipsQuickAdd = { relationshipsQa = it },
                    onOpenRelationshipsActivity = { relationshipsActivityOpen = true },
                    onViewAllActivity = { recentActivityOpen = true },
                )
            }
            if (
                state.selectedContext == AppContext.PERSONAL &&
                state.selectedMomentId != null &&
                !newMomentOpen &&
                state.bottomDestination in setOf(
                    BottomDestination.PULSE,
                    BottomDestination.MOMENTS,
                    BottomDestination.LIFE,
                    BottomDestination.MEMORY,
                )
            ) {
                PersonalExpenseFab(
                    onClick = { moneyQa = MoneyQuickAddKind.MASTER_EXPENSE },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = ShellTokens.BottomBarHeight + 16.dp),
                )
            }
        }
        if (state.selectedMomentId != null && state.selectedContext == AppContext.PERSONAL) {
            moneyQa?.let { kind ->
                when (kind) {
                    MoneyQuickAddKind.MASTER_EXPENSE -> {
                        val pulseFamily = personalPulseFamilyFor(state.selectedMomentTypeCode)
                        PersonalMasterExpenseSheet(
                            momentId = state.selectedMomentId!!,
                            visible = true,
                            pulseFamily = pulseFamily,
                            onDismiss = { moneyQa = null },
                            onSaved = {
                                moneyQa = null
                                shellViewModel.refreshVisiblePersonalTab()
                            },
                        )
                    }
                    MoneyQuickAddKind.INCOME, MoneyQuickAddKind.TRANSFER, MoneyQuickAddKind.SAVINGS -> {
                        PersonalMoneyQuickAddSheet(
                            kind = kind,
                            momentId = state.selectedMomentId!!,
                            visible = true,
                            onDismiss = { moneyQa = null },
                            onSaved = {
                                moneyQa = null
                                shellViewModel.refreshVisiblePersonalTab()
                            },
                        )
                    }
                }
            }
        }
        if (state.selectedMomentId != null && state.selectedContext == AppContext.GROUP) {
            val groupFamily = groupExperienceFamilyFor(
                state.selectedMomentTypeCode
                    ?: state.moments.firstOrNull { it.momentId == state.selectedMomentId }?.momentTypeCode,
            )
            val isWeddingFinance = groupFamily == GroupExperienceFamily.WEDDING
            val groupExpenseTypeCode = state.selectedMomentTypeCode
                ?: state.moments.firstOrNull { it.momentId == state.selectedMomentId }?.momentTypeCode
            GroupExpenseSheet(
                momentId = state.selectedMomentId!!,
                visible = groupExpenseSheetOpen,
                onDismiss = { groupExpenseSheetOpen = false },
                onSaved = { shellViewModel.refreshVisibleGroupTab() },
                isWedding = isWeddingFinance,
                momentTypeCode = groupExpenseTypeCode,
            )
            GroupContributionSheet(
                momentId = state.selectedMomentId!!,
                visible = groupContributionSheetOpen,
                onDismiss = { groupContributionSheetOpen = false },
                onSaved = { shellViewModel.refreshVisibleGroupTab() },
                isWedding = isWeddingFinance,
            )
            GroupSettlementSheet(
                momentId = state.selectedMomentId!!,
                visible = groupSettlementSheetOpen,
                onDismiss = { groupSettlementSheetOpen = false },
                onSaved = { shellViewModel.refreshVisibleGroupTab() },
                momentTypeCode = state.selectedMomentTypeCode
                    ?: state.moments.firstOrNull { it.momentId == state.selectedMomentId }?.momentTypeCode,
            )
            GroupParticipantsSheet(
                momentId = state.selectedMomentId!!,
                visible = groupParticipantsSheetOpen,
                onDismiss = { groupParticipantsSheetOpen = false },
                isWedding = isWeddingFinance,
            )
            if (state.selectedMomentId != null) {
                val groupInviteTypeCode = state.selectedMomentTypeCode
                    ?: state.moments.firstOrNull { it.momentId == state.selectedMomentId }?.momentTypeCode
                    ?: "TRIP"
                GroupInvitePeopleSheet(
                    momentId = state.selectedMomentId!!,
                    momentTitle = state.selectedMomentTitle ?: "Trip",
                    momentTypeCode = groupInviteTypeCode,
                    visible = groupInviteSheetOpen,
                    onDismiss = { groupInviteSheetOpen = false },
                    onSaved = { shellViewModel.refreshVisibleGroupTab() },
                    currentUserId = identity.userId,
                )
            }
            GroupBudgetSheet(
                momentId = state.selectedMomentId!!,
                visible = groupBudgetSheetOpen,
                onDismiss = { groupBudgetSheetOpen = false },
                onSaved = { shellViewModel.refreshVisibleGroupTab() },
                isWedding = isWeddingFinance,
            )
            groupCollabKind?.let { kind ->
                GroupCollabSheet(
                    kind = kind,
                    momentId = state.selectedMomentId!!,
                    visible = true,
                    onDismiss = { groupCollabKind = null },
                    onSaved = { shellViewModel.refreshVisibleGroupTab() },
                    momentTypeCode = state.selectedMomentTypeCode,
                )
            }
            weddingGapQa?.let { kind ->
                WeddingGapQuickAddSheet(
                    kind = kind,
                    visible = true,
                    momentId = state.selectedMomentId,
                    onDismiss = { weddingGapQa = null },
                    onSaved = { shellViewModel.refreshVisibleGroupTab() },
                )
            }
            experienceGapQa?.let { kind ->
                ExperienceGapQuickAddSheet(
                    theme = ExperienceActiveTheme.forFamily(groupFamily),
                    kind = kind,
                    visible = true,
                    momentId = state.selectedMomentId,
                    momentTypeCode = state.selectedMomentTypeCode,
                    onDismiss = { experienceGapQa = null },
                    onSaved = { shellViewModel.refreshVisibleGroupTab() },
                    onBooking = { groupCollabKind = GroupCollabKind.BOOKING },
                )
            }
            purchaseGapQa?.let { kind ->
                PurchaseGapQuickAddSheet(
                    theme = PurchaseActiveTheme.forFamily(groupFamily),
                    kind = kind,
                    visible = true,
                    momentId = state.selectedMomentId,
                    momentTypeCode = state.selectedMomentTypeCode,
                    onDismiss = { purchaseGapQa = null },
                    onSaved = { shellViewModel.refreshVisibleGroupTab() },
                )
            }
            livingGapQa?.let { kind ->
                LivingGapQuickAddSheet(
                    theme = LivingActiveTheme.forFamily(groupFamily),
                    kind = kind,
                    visible = true,
                    momentId = state.selectedMomentId,
                    momentTypeCode = state.selectedMomentTypeCode,
                    onDismiss = { livingGapQa = null },
                    onSaved = { shellViewModel.refreshVisibleGroupTab() },
                )
            }
            GroupFinanceDetailFlow(
                visible = groupFinanceOpen,
                momentId = state.selectedMomentId,
                momentTitle = state.selectedMomentTitle,
                isWedding = isWeddingFinance,
                experienceFamily = groupFamily,
                onDismiss = { groupFinanceOpen = false },
                onOpenSplits = {
                    groupFinanceOpen = false
                    groupSplitsOpen = true
                },
                onSettle = {
                    groupFinanceOpen = false
                    groupSettlementSheetOpen = true
                },
            )
            GroupExpenseSplitsFlow(
                visible = groupSplitsOpen,
                momentId = state.selectedMomentId,
                momentTitle = state.selectedMomentTitle,
                isWedding = isWeddingFinance,
                experienceFamily = groupFamily,
                onDismiss = { groupSplitsOpen = false },
                onOpenFinance = {
                    groupSplitsOpen = false
                    groupFinanceOpen = true
                },
                onSettle = {
                    groupSplitsOpen = false
                    groupSettlementSheetOpen = true
                },
            )
        }
        if (showJoinQrScanner) {
            GroupJoinQrScanner(
                onCode = { code ->
                    showJoinQrScanner = false
                    pendingGroupJoinCode = code
                },
                onCompanyCode = { code ->
                    showJoinQrScanner = false
                    shellViewModel.redeemCompanyInvite(code) { result ->
                        result.fold(
                            onSuccess = {
                                Toast.makeText(
                                    context,
                                    if (it.alreadyMember) "Already a company member" else "Joined company",
                                    Toast.LENGTH_SHORT,
                                ).show()
                            },
                            onFailure = {
                                Toast.makeText(
                                    context,
                                    it.message ?: "Could not join company",
                                    Toast.LENGTH_SHORT,
                                ).show()
                            },
                        )
                    }
                },
                onDismiss = { showJoinQrScanner = false },
            )
        }
        pendingGroupJoinCode?.let { code ->
            GroupJoinConfirmSheet(
                code = code,
                visible = true,
                onDismiss = { pendingGroupJoinCode = null },
                onJoin = {
                    shellViewModel.redeemGroupInvite(code) { result ->
                        result.fold(
                            onSuccess = {
                                pendingGroupJoinCode = null
                                preferGroupCreateFlow = false
                                groupCreatePhase = GroupCreatePhase.CHOOSER
                                val message = when {
                                    it.alreadyMember -> "Already a member"
                                    it.momentId.isNullOrBlank() ->
                                        "Invite claimed — you’ll join when the organizer finishes creating the group."
                                    else -> "Joined group"
                                }
                                Toast.makeText(
                                    context,
                                    message,
                                    if (it.momentId.isNullOrBlank() && !it.alreadyMember) {
                                        Toast.LENGTH_LONG
                                    } else {
                                        Toast.LENGTH_SHORT
                                    },
                                ).show()
                            },
                            onFailure = {
                                Toast.makeText(
                                    context,
                                    it.message ?: "Could not join",
                                    Toast.LENGTH_SHORT,
                                ).show()
                            },
                        )
                    }
                },
            )
        }
        if (state.selectedMomentId != null && state.selectedContext == AppContext.BUSINESS) {
            businessGapQa?.let { kind ->
                val typeCode = state.selectedMomentTypeCode
                    ?: state.moments.firstOrNull { it.momentId == state.selectedMomentId }?.momentTypeCode
                val code = typeCode.orEmpty().uppercase()
                val isRunway = code.contains("RUNWAY")
                val isTeamOps = code.contains("TEAM_OPERATIONS") &&
                    TeamOpsQuickAddSheets.isTeamOpsKind(kind)
                val isOps = code.contains("OPERATIONS") &&
                    !code.contains("TEAM") &&
                    !code.contains("RUNWAY") &&
                    OpsQuickAddSheets.isOpsKind(kind)
                when {
                    isRunway -> RunwayQuickAddSheet(
                        kind = kind,
                        visible = true,
                        momentId = state.selectedMomentId,
                        onDismiss = { businessGapQa = null },
                        onSaved = { shellViewModel.refreshVisibleBusinessTab() },
                    )
                    isTeamOps -> TeamOpsGapQuickAddSheet(
                        kind = kind,
                        visible = true,
                        momentId = state.selectedMomentId,
                        onDismiss = { businessGapQa = null },
                        onSaved = { shellViewModel.refreshVisibleBusinessTab() },
                    )
                    isOps -> OpsGapQuickAddSheet(
                        kind = kind,
                        visible = true,
                        momentId = state.selectedMomentId,
                        companyId = state.selectedCompany?.companyId,
                        momentTitle = state.moments.firstOrNull { it.momentId == state.selectedMomentId }?.title,
                        onDismiss = { businessGapQa = null },
                        onSaved = { shellViewModel.refreshVisibleBusinessTab() },
                        onExpense = { businessExpenseSheetOpen = true },
                    )
                    else -> BusinessGapQuickAddSheet(
                        theme = BusinessActiveTheme.forTypeCode(typeCode),
                        kind = kind,
                        visible = true,
                        momentId = state.selectedMomentId,
                        onDismiss = { businessGapQa = null },
                        onSaved = { shellViewModel.refreshVisibleBusinessTab() },
                        onExpense = { businessExpenseSheetOpen = true },
                        onRevenue = { businessRevenueSheetOpen = true },
                        onInvoice = { businessInvoiceSheetOpen = true },
                    )
                }
            }
            BusinessExpenseSheet(
                momentId = state.selectedMomentId!!,
                visible = businessExpenseSheetOpen,
                onDismiss = { businessExpenseSheetOpen = false },
                onSaved = { shellViewModel.refreshVisibleBusinessTab() },
            )
            BusinessRevenueSheet(
                momentId = state.selectedMomentId!!,
                visible = businessRevenueSheetOpen,
                onDismiss = { businessRevenueSheetOpen = false },
                onSaved = { shellViewModel.refreshVisibleBusinessTab() },
            )
            BusinessInvoiceSheet(
                momentId = state.selectedMomentId!!,
                visible = businessInvoiceSheetOpen,
                onDismiss = { businessInvoiceSheetOpen = false },
                onSaved = { shellViewModel.refreshVisibleBusinessTab() },
            )
        }
        if (state.selectedCompany != null && state.selectedContext == AppContext.BUSINESS) {
            BusinessMembersSheet(
                companyId = state.selectedCompany!!.companyId,
                visible = businessMembersSheetOpen,
                onDismiss = { businessMembersSheetOpen = false },
            )
        }
        lifeOpsQa?.let { kind ->
            val momentId = state.selectedMomentId
            if (momentId != null) {
                ModalBottomSheet(
                    onDismissRequest = { lifeOpsQa = null },
                    sheetState = lifeOpsSheetState,
                    containerColor = Color(0xFF14121B),
                    dragHandle = null,
                ) {
                    PersonalLifeOpsQuickAddSheet(
                        kind = kind,
                        momentId = momentId,
                        onClose = { lifeOpsQa = null },
                        onSaved = {
                            lifeOpsQa = null
                            shellViewModel.refreshVisiblePersonalTab()
                        },
                    )
                }
            }
        }
        futureQa?.let { kind ->
            val momentId = state.selectedMomentId
            if (momentId != null) {
                ModalBottomSheet(
                    onDismissRequest = { futureQa = null },
                    sheetState = futureSheetState,
                    containerColor = Color(0xFF14121B),
                    dragHandle = null,
                ) {
                    PersonalFutureQuickAddSheet(
                        kind = kind,
                        momentId = momentId,
                        onClose = { futureQa = null },
                        onSaved = {
                            futureQa = null
                            shellViewModel.refreshVisiblePersonalTab()
                        },
                    )
                }
            }
        }
        lifestyleQa?.let { kind ->
            val momentId = state.selectedMomentId
            if (momentId != null) {
                ModalBottomSheet(
                    onDismissRequest = { lifestyleQa = null },
                    sheetState = lifestyleSheetState,
                    containerColor = Color(0xFF14121B),
                    dragHandle = null,
                ) {
                    PersonalLifestyleQuickAddSheet(
                        kind = kind,
                        momentId = momentId,
                        onClose = { lifestyleQa = null },
                        onSaved = {
                            lifestyleQa = null
                            shellViewModel.refreshVisiblePersonalTab()
                        },
                    )
                }
            }
        }
        relationshipsQa?.let { kind ->
            val momentId = state.selectedMomentId
            if (momentId != null) {
                ModalBottomSheet(
                    onDismissRequest = { relationshipsQa = null },
                    sheetState = relationshipsSheetState,
                    containerColor = Color(0xFF14121B),
                    dragHandle = null,
                ) {
                    PersonalRelationshipsQuickAddSheet(
                        kind = kind,
                        momentId = momentId,
                        onClose = { relationshipsQa = null },
                        onSaved = {
                            relationshipsQa = null
                            shellViewModel.refreshVisiblePersonalTab()
                        },
                    )
                }
            }
        }
        PersonalRelationshipsActivityFlow(
            momentId = state.selectedMomentId,
            visible = relationshipsActivityOpen,
            onDismiss = { relationshipsActivityOpen = false },
            onChanged = { shellViewModel.refreshVisiblePersonalTab() },
        )
        PersonalRecentActivityFlow(
            momentId = state.selectedMomentId,
            visible = recentActivityOpen,
            onDismiss = { recentActivityOpen = false },
            onChanged = { shellViewModel.refreshVisiblePersonalTab() },
        )
        if (showManageMoment) {
            val momentId = state.selectedMomentId
            if (momentId != null) {
                ModalBottomSheet(
                    onDismissRequest = { showManageMoment = false },
                    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                    containerColor = Color(0xFF1A1628),
                    dragHandle = null,
                ) {
                    ManageMomentSheet(
                        momentId = momentId,
                        momentTitle = state.selectedMomentTitle ?: "Moment",
                        domain = state.selectedContext,
                        currentUserId = identity.userId,
                        companyId = state.selectedCompany?.companyId
                            ?: state.moments.firstOrNull { it.momentId == momentId }?.companyId,
                        onDismiss = { showManageMoment = false },
                        onEditSetup = {
                            showManageMoment = false
                            editSetupOpen = true
                        },
                        onLifecycleChanged = {
                            shellViewModel.reloadCurrentContext()
                        },
                        onLeft = {
                            showManageMoment = false
                            shellViewModel.clearSelectedMomentAfterLeave()
                        },
                    )
                }
            }
        }
        if (editSetupOpen) {
            val momentId = state.selectedMomentId
            if (momentId != null) {
                ModalBottomSheet(
                    onDismissRequest = { editSetupOpen = false },
                    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                    containerColor = Color(0xFF0C0F15),
                    dragHandle = null,
                ) {
                    EditMomentSetupHost(
                        context = state.selectedContext,
                        momentId = momentId,
                        momentTitle = state.selectedMomentTitle.orEmpty(),
                        momentTypeCode = state.selectedMomentTypeCode,
                        companyId = state.moments.firstOrNull { it.momentId == momentId }?.companyId,
                        onClose = { editSetupOpen = false },
                        onSaved = {
                            editSetupOpen = false
                            shellViewModel.reloadCurrentContext()
                        },
                    )
                }
            }
        }
        ShellBottomNavigation(
            selected = state.bottomDestination,
            onSelect = {
                newMomentOpen = false
                if (it == BottomDestination.CREATE && state.selectedContext == AppContext.GROUP) {
                    groupCreatePhase = GroupCreatePhase.CHOOSER
                    preferGroupCreateFlow = false
                }
                shellViewModel.selectBottomDestination(it)
            },
            accent = momentAccent,
        )
    }

    if (state.life360Open) {
        ModalBottomSheet(
            onDismissRequest = { shellViewModel.openLife360(false) },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = GlobalSurfaceTheme.life360.comingSoonBackground,
        ) {
            com.example.momentra.ui.shell.components.Life360GlobalSurface(
                onClose = { shellViewModel.openLife360(false) },
            )
        }
    }
    if (state.profileOpen) {
        ModalBottomSheet(
            onDismissRequest = { shellViewModel.openProfile(false) },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            com.example.momentra.ui.account.AccountHubSheet(
                identity = state.identity ?: identity,
                onSignOut = {
                    shellViewModel.openProfile(false)
                    onSignOut()
                },
                onClose = { shellViewModel.openProfile(false) },
                onAccountDeleted = {
                    shellViewModel.openProfile(false)
                    onSignOut()
                },
            )
        }
    }
}

private fun shouldShowMomentSwitcher(
    context: AppContext,
    show: Boolean,
    content: ShellContentState,
    destination: BottomDestination,
): Boolean {
    if (context == AppContext.CIRCLE) return false
    if (destination == BottomDestination.CREATE) return false
    if (content is ShellContentState.Empty) return false
    if (content is ShellContentState.Loading || content is ShellContentState.Idle) return false
    return show
}

@Composable

private fun ShellDestinationContent(
    context: AppContext,
    destination: BottomDestination,
    content: ShellContentState,
    experience: MomentExperienceKind,
    moments: List<MomentSummary>,
    hasCompany: Boolean,
    companyId: String?,
    companyName: String?,
    selectedMomentId: String?,
    selectedMomentTitle: String?,
    selectedMomentTypeCode: String? = null,
    personalTabRefreshToken: Long,
    groupTabRefreshToken: Long = 0L,
    businessTabRefreshToken: Long = 0L,
    capabilities: List<String> = emptyList(),
    onRetry: () -> Unit,
    onSessionExpired: () -> Unit,
    onCreateMoment: () -> Unit,
    onCreateBack: () -> Unit = onCreateMoment,
    onCompanyActivated: (CompanySummary) -> Unit = {},
    onMomentCreated: (String, String, String?) -> Unit = { _, _, _ -> },
    onJoinGroupCode: (String) -> Unit = {},
    preferGroupCreateFlow: Boolean = false,
    onPreferGroupCreateFlow: (Boolean) -> Unit = {},
    groupCreatePhase: GroupCreatePhase = GroupCreatePhase.CHOOSER,
    onGroupCreatePhase: (GroupCreatePhase) -> Unit = {},
    onOpenGroupCreateTab: () -> Unit = {},
    onAddExpense: () -> Unit = {},
    onAddContribution: () -> Unit = {},
    onAddSettlement: () -> Unit = {},
    onAddBudget: () -> Unit = {},
    onAddParticipants: () -> Unit = {},
    onAddInvite: () -> Unit = {},
    onAddPlanning: () -> Unit = {},
    onAddBooking: () -> Unit = {},
    onAddPoll: () -> Unit = {},
    onAddUpdate: () -> Unit = {},
    onAddMemory: () -> Unit = {},
    onAddPurchaseItem: () -> Unit = {},
    onAddResident: () -> Unit = {},
    onViewSplits: () -> Unit = {},
    onOpenGroupFinance: () -> Unit = {},
    onWeddingQuickAdd: (WeddingQuickAddKind) -> Unit = {},
    onExperienceQuickAdd: (ExperienceQuickAddKind) -> Unit = {},
    onPurchaseQuickAdd: (PurchaseQuickAddKind) -> Unit = {},
    onLivingQuickAdd: (LivingQuickAddKind) -> Unit = {},
    onBusinessQuickAdd: (BusinessQuickAddKind) -> Unit = {},
    onAddRevenue: () -> Unit = {},
    onAddInvoice: () -> Unit = {},
    onAddMembers: () -> Unit = {},
    onOpenQuickAdd: () -> Unit = {},
    onViewBusinessReport: () -> Unit = {},
    onLifeOpsQuickAdd: (LifeOpsQuickAddKind) -> Unit = {},
    onMoneyQuickAdd: (MoneyQuickAddKind) -> Unit = {},
    onFutureQuickAdd: (FutureQuickAddKind) -> Unit = {},
    onLifestyleQuickAdd: (LifestyleQuickAddKind) -> Unit = {},
    onRelationshipsQuickAdd: (RelationshipsQuickAddKind) -> Unit = {},
    onOpenRelationshipsActivity: () -> Unit = {},
    onViewAllActivity: () -> Unit = {},
) {
    when (content) {
        ShellContentState.Loading, ShellContentState.Idle -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MomentraBrandColors.Cta)
            }
        }
        ShellContentState.Offline -> {
            EmptyPanel(
                title = "You're offline",
                body = "Check your connection and try again.",
                actionLabel = "Retry",
                onAction = onRetry,
            )
        }
        is ShellContentState.Error -> {
            if (content.code == "UNAUTHORIZED" || content.message.contains("UNAUTHENTICATED", ignoreCase = true)) {
                LaunchedEffect(Unit) { onSessionExpired() }
            }
            EmptyPanel(
                title = "We couldn't load your moments",
                body = content.message,
                actionLabel = "Retry",
                onAction = onRetry,
            )
        }
        ShellContentState.Forbidden -> {
            EmptyPanel(
                title = "No access",
                body = "You no longer have access to this ${context.label.lowercase()} resource. Your session stays signed in.",
            )
        }
        ShellContentState.Deferred -> {
            // Legacy slot — Circle now uses Empty + Coming Soon; keep panel for any residual Deferred.
            com.example.momentra.ui.shell.circle.CircleComingSoonContent()
        }
        ShellContentState.Empty -> {
            ContextEmptyExperience(
                context = context,
                destination = destination,
                experience = experience,
                moments = moments,
                hasCompany = hasCompany,
                companyId = companyId,
                onCreateMoment = onCreateMoment,
                onCreateBack = onCreateBack,
                onCompanyActivated = onCompanyActivated,
                onMomentCreated = onMomentCreated,
                onJoinGroupCode = onJoinGroupCode,
                groupCreatePhase = groupCreatePhase,
                onGroupCreatePhase = onGroupCreatePhase,
                onOpenGroupCreateTab = onOpenGroupCreateTab,
            )
        }
        is ShellContentState.Ready -> {
            when {
                context == AppContext.GROUP && destination == BottomDestination.CREATE -> {
                    if (preferGroupCreateFlow || selectedMomentId == null) {
                        GroupCreateFlow(
                            phase = groupCreatePhase,
                            onPhase = onGroupCreatePhase,
                            onCreateBack = onCreateBack,
                            onMomentCreated = onMomentCreated,
                            onJoinCode = onJoinGroupCode,
                        )
                    } else {
                        val groupFamily = groupExperienceFamilyFor(
                            moments.firstOrNull { it.momentId == selectedMomentId }?.momentTypeCode
                                ?: selectedMomentTypeCode,
                        )
                        val isWedding = groupFamily == GroupExperienceFamily.WEDDING
                        val isExperience = groupFamily.isThemedExperience()
                        val isPurchase = groupFamily.isThemedPurchase()
                        val isLiving = groupFamily.isThemedLiving()
                        val experienceTheme = ExperienceActiveTheme.forFamily(groupFamily)
                        val purchaseTheme = PurchaseActiveTheme.forFamily(groupFamily)
                        val livingTheme = LivingActiveTheme.forFamily(groupFamily)
                        if (isWedding) {
                            WeddingQuickAddHub(
                                momentTitle = selectedMomentTitle,
                                hasActiveMoment = true,
                                onClose = onCreateBack,
                                onTile = onWeddingQuickAdd,
                                onCreateMoment = {
                                    onPreferGroupCreateFlow(true)
                                    onGroupCreatePhase(GroupCreatePhase.CHOOSER)
                                },
                                onJoinCode = onJoinGroupCode,
                                capabilities = capabilities,
                            )
                        } else if (isExperience) {
                            ExperienceQuickAddHub(
                                theme = experienceTheme,
                                momentTitle = selectedMomentTitle,
                                hasActiveMoment = true,
                                onClose = onCreateBack,
                                onTile = onExperienceQuickAdd,
                                onCreateMoment = {
                                    onPreferGroupCreateFlow(true)
                                    onGroupCreatePhase(GroupCreatePhase.CHOOSER)
                                },
                                onJoinCode = onJoinGroupCode,
                                capabilities = capabilities,
                            )
                        } else if (isPurchase) {
                            PurchaseQuickAddHub(
                                theme = purchaseTheme,
                                momentTitle = selectedMomentTitle,
                                hasActiveMoment = true,
                                onClose = onCreateBack,
                                onTile = onPurchaseQuickAdd,
                                onCreateMoment = {
                                    onPreferGroupCreateFlow(true)
                                    onGroupCreatePhase(GroupCreatePhase.CHOOSER)
                                },
                            )
                        } else if (isLiving) {
                            LivingQuickAddHub(
                                theme = livingTheme,
                                momentTitle = selectedMomentTitle,
                                hasActiveMoment = true,
                                onClose = onCreateBack,
                                onTile = onLivingQuickAdd,
                                onCreateMoment = {
                                    onPreferGroupCreateFlow(true)
                                    onGroupCreatePhase(GroupCreatePhase.CHOOSER)
                                },
                            )
                        } else {
                            GroupQuickAddHub(
                                hasActiveMoment = true,
                                onClose = onCreateBack,
                                onExpense = onAddExpense,
                                onContribution = onAddContribution,
                                onSettle = onAddSettlement,
                                onParticipants = onAddParticipants,
                                onInvite = onAddInvite,
                                onBudget = onAddBudget,
                                onPlanning = onAddPlanning,
                                onBooking = onAddBooking,
                                onPoll = onAddPoll,
                                onUpdate = onAddUpdate,
                                onMemory = onAddMemory,
                                onPurchaseItem = onAddPurchaseItem,
                                onResident = onAddResident,
                                onCreateMoment = {
                                    onPreferGroupCreateFlow(true)
                                    onGroupCreatePhase(GroupCreatePhase.CHOOSER)
                                },
                                onJoinCode = onJoinGroupCode,
                                momentTitle = selectedMomentTitle,
                                momentTypeCode = moments.firstOrNull { it.momentId == selectedMomentId }?.momentTypeCode,
                                capabilities = capabilities,
                            )
                        }
                    }
                }
                context == AppContext.BUSINESS && destination == BottomDestination.CREATE -> {
                    if (selectedMomentId != null && hasCompany) {
                        BusinessQuickAddHub(
                            hasActiveMoment = true,
                            hasCompany = hasCompany,
                            onClose = onCreateBack,
                            onExpense = onAddExpense,
                            onRevenue = onAddRevenue,
                            onInvoice = onAddInvoice,
                            onMembers = onAddMembers,
                            onCreateMoment = onCreateMoment,
                            onTile = onBusinessQuickAdd,
                            momentTypeCode = moments.firstOrNull { it.momentId == selectedMomentId }?.momentTypeCode
                                ?: selectedMomentTypeCode,
                            capabilities = capabilities,
                        )
                    } else {
                        ContextEmptyExperience(
                            context = context,
                            destination = destination,
                            experience = experience,
                            moments = moments,
                            hasCompany = hasCompany,
                            companyId = companyId,
                            onCreateMoment = onCreateMoment,
                            onCreateBack = onCreateBack,
                            onCompanyActivated = onCompanyActivated,
                            onMomentCreated = onMomentCreated,
                        )
                    }
                }
                else -> {
                    val personalTypeCode = selectedMomentTypeCode
                        ?: moments.firstOrNull { it.momentId == selectedMomentId }?.momentTypeCode
                    val personalFamily = personalPulseFamilyFor(personalTypeCode)
                    val isLifeOps = personalFamily == PersonalPulseFamily.LIFE_OPERATIONS
                    val isFutureBuilding = personalFamily == PersonalPulseFamily.FUTURE_BUILDING
                    val isLifestyle = personalFamily == PersonalPulseFamily.LIFESTYLE
                    val isRelationships = personalFamily == PersonalPulseFamily.RELATIONSHIPS
                    val groupTypeCode = selectedMomentTypeCode
                        ?: moments.firstOrNull { it.momentId == selectedMomentId }?.momentTypeCode
                    val groupFamily = groupExperienceFamilyFor(groupTypeCode)
                    val isWedding = groupFamily == GroupExperienceFamily.WEDDING
                    val isExperience = groupFamily.isThemedExperience()
                    val isPurchase = groupFamily.isThemedPurchase()
                    val isLiving = groupFamily.isThemedLiving()
                    val experienceTheme = ExperienceActiveTheme.forFamily(groupFamily)
                    val purchaseTheme = PurchaseActiveTheme.forFamily(groupFamily)
                    val livingTheme = LivingActiveTheme.forFamily(groupFamily)
                    when {
                        context == AppContext.GROUP && destination == BottomDestination.PULSE -> {
                            if (isWedding) {
                                WeddingPulseActiveContent(
                                    momentId = selectedMomentId,
                                    momentTitle = selectedMomentTitle,
                                    refreshToken = groupTabRefreshToken,
                                    momentTypeCode = groupTypeCode,
                                    onAddExpense = onAddExpense,
                                    onOpenQuickAdd = onOpenQuickAdd,
                                    onViewSplits = onViewSplits,
                                    onOpenFinance = onOpenGroupFinance,
                                    onQuickAddKind = onWeddingQuickAdd,
                                )
                            } else if (isExperience) {
                                ExperiencePulseActiveContent(
                                    theme = experienceTheme,
                                    momentId = selectedMomentId,
                                    momentTitle = selectedMomentTitle,
                                    refreshToken = groupTabRefreshToken,
                                    momentTypeCode = groupTypeCode,
                                    onAddExpense = onAddExpense,
                                    onOpenQuickAdd = onOpenQuickAdd,
                                    onViewSplits = onViewSplits,
                                    onOpenFinance = onOpenGroupFinance,
                                    onQuickAddKind = onExperienceQuickAdd,
                                )
                            } else if (isPurchase) {
                                PurchasePulseActiveContent(
                                    theme = purchaseTheme,
                                    momentId = selectedMomentId,
                                    momentTitle = selectedMomentTitle,
                                    refreshToken = groupTabRefreshToken,
                                    momentTypeCode = groupTypeCode,
                                    onAddExpense = onAddExpense,
                                    onOpenQuickAdd = onOpenQuickAdd,
                                    onViewSplits = onViewSplits,
                                    onOpenFinance = onOpenGroupFinance,
                                    onQuickAddKind = onPurchaseQuickAdd,
                                )
                            } else if (isLiving) {
                                LivingPulseActiveContent(
                                    theme = livingTheme,
                                    momentId = selectedMomentId,
                                    momentTitle = selectedMomentTitle,
                                    refreshToken = groupTabRefreshToken,
                                    momentTypeCode = groupTypeCode,
                                    onAddExpense = onAddExpense,
                                    onOpenQuickAdd = onOpenQuickAdd,
                                    onViewSplits = onViewSplits,
                                    onOpenFinance = onOpenGroupFinance,
                                    onQuickAddKind = onLivingQuickAdd,
                                )
                            } else {
                                GroupPulseActiveContent(
                                    momentId = selectedMomentId,
                                    momentTitle = selectedMomentTitle,
                                    refreshToken = groupTabRefreshToken,
                                    onAddExpense = onAddExpense,
                                    onViewSplits = onViewSplits,
                                    onOpenFinance = onOpenGroupFinance,
                                    onOpenMemory = onAddMemory,
                                    onOpenChat = onAddUpdate,
                                    onOpenItinerary = onAddPlanning,
                                )
                            }
                        }
                        context == AppContext.GROUP && destination == BottomDestination.MOMENTS -> {
                            if (isWedding) {
                                WeddingMomentsActiveContent(
                                    momentId = selectedMomentId,
                                    momentTitle = selectedMomentTitle,
                                    refreshToken = groupTabRefreshToken,
                                    momentTypeCode = selectedMomentTypeCode,
                                    onOpenQuickAdd = onOpenQuickAdd,
                                )
                            } else if (isExperience) {
                                ExperienceMomentsActiveContent(
                                    theme = experienceTheme,
                                    momentId = selectedMomentId,
                                    momentTitle = selectedMomentTitle,
                                    refreshToken = groupTabRefreshToken,
                                    momentTypeCode = selectedMomentTypeCode,
                                    onOpenQuickAdd = onOpenQuickAdd,
                                )
                            } else if (isPurchase) {
                                PurchaseMomentsActiveContent(
                                    theme = purchaseTheme,
                                    momentId = selectedMomentId,
                                    momentTitle = selectedMomentTitle,
                                    refreshToken = groupTabRefreshToken,
                                    momentTypeCode = selectedMomentTypeCode,
                                    onOpenQuickAdd = onOpenQuickAdd,
                                )
                            } else if (isLiving) {
                                LivingMomentsActiveContent(
                                    theme = livingTheme,
                                    momentId = selectedMomentId,
                                    momentTitle = selectedMomentTitle,
                                    refreshToken = groupTabRefreshToken,
                                    momentTypeCode = selectedMomentTypeCode,
                                    onOpenQuickAdd = onOpenQuickAdd,
                                )
                            } else {
                                GroupMomentsActiveContent(
                                    momentId = selectedMomentId,
                                    momentTitle = selectedMomentTitle,
                                    refreshToken = groupTabRefreshToken,
                                    momentTypeCode = selectedMomentTypeCode,
                                    onCreateMoment = onOpenGroupCreateTab,
                                )
                            }
                        }
                        context == AppContext.GROUP && destination == BottomDestination.LIFE -> {
                            GroupLifeActiveContent(
                                momentId = selectedMomentId,
                                momentTitle = selectedMomentTitle,
                                refreshToken = groupTabRefreshToken,
                                onQuickAction = { action ->
                                    when (action) {
                                        GroupLifeQuickAction.EXPERIENCE,
                                        GroupLifeQuickAction.GOAL,
                                        -> {
                                            when {
                                                isWedding -> onWeddingQuickAdd(WeddingQuickAddKind.PLANNING)
                                                isExperience -> onExperienceQuickAdd(ExperienceQuickAddKind.PLANNING)
                                                isPurchase -> onPurchaseQuickAdd(PurchaseQuickAddKind.PURCHASE_ITEM)
                                                isLiving -> onLivingQuickAdd(LivingQuickAddKind.TASK)
                                                else -> onAddPlanning()
                                            }
                                        }
                                        GroupLifeQuickAction.PURCHASE -> {
                                            when {
                                                isWedding -> onWeddingQuickAdd(WeddingQuickAddKind.EXPENSE)
                                                isExperience -> onExperienceQuickAdd(ExperienceQuickAddKind.EXPENSE)
                                                isPurchase -> onPurchaseQuickAdd(PurchaseQuickAddKind.EXPENSE)
                                                isLiving -> onLivingQuickAdd(LivingQuickAddKind.EXPENSE)
                                                else -> onAddExpense()
                                            }
                                        }
                                        GroupLifeQuickAction.LIVING -> {
                                            when {
                                                isWedding -> onWeddingQuickAdd(WeddingQuickAddKind.VENDOR)
                                                isExperience && experienceTheme.includesVendor ->
                                                    onExperienceQuickAdd(ExperienceQuickAddKind.VENDOR)
                                                isExperience -> onExperienceQuickAdd(ExperienceQuickAddKind.BOOKING)
                                                isPurchase && purchaseTheme.includesVendor ->
                                                    onPurchaseQuickAdd(PurchaseQuickAddKind.VENDOR)
                                                isPurchase -> onPurchaseQuickAdd(PurchaseQuickAddKind.CONTRIBUTION)
                                                isLiving -> onAddInvite()
                                                else -> onAddBooking()
                                            }
                                        }
                                        GroupLifeQuickAction.COMMUNITY -> {
                                            when {
                                                isWedding -> onWeddingQuickAdd(WeddingQuickAddKind.UPDATE)
                                                isExperience -> onExperienceQuickAdd(ExperienceQuickAddKind.UPDATE)
                                                isPurchase -> onPurchaseQuickAdd(PurchaseQuickAddKind.UPDATE)
                                                isLiving -> onLivingQuickAdd(LivingQuickAddKind.UPDATE)
                                                else -> onAddUpdate()
                                            }
                                        }
                                    }
                                },
                            )
                        }
                        context == AppContext.GROUP && destination == BottomDestination.MEMORY -> {
                            if (isWedding) {
                                WeddingMemoryActiveContent(
                                    momentId = selectedMomentId,
                                    momentTitle = selectedMomentTitle,
                                    refreshToken = groupTabRefreshToken,
                                    onOpenQuickAdd = { onWeddingQuickAdd(WeddingQuickAddKind.MEMORY) },
                                )
                            } else if (isExperience) {
                                ExperienceMemoryActiveContent(
                                    theme = experienceTheme,
                                    momentId = selectedMomentId,
                                    momentTitle = selectedMomentTitle,
                                    refreshToken = groupTabRefreshToken,
                                    onOpenQuickAdd = { onExperienceQuickAdd(ExperienceQuickAddKind.MEMORY) },
                                )
                            } else if (isPurchase) {
                                PurchaseMemoryActiveContent(
                                    theme = purchaseTheme,
                                    momentId = selectedMomentId,
                                    momentTitle = selectedMomentTitle,
                                    refreshToken = groupTabRefreshToken,
                                    onOpenQuickAdd = { onPurchaseQuickAdd(PurchaseQuickAddKind.MEMORY) },
                                )
                            } else if (isLiving) {
                                LivingMemoryActiveContent(
                                    theme = livingTheme,
                                    momentId = selectedMomentId,
                                    momentTitle = selectedMomentTitle,
                                    refreshToken = groupTabRefreshToken,
                                    onOpenQuickAdd = { onLivingQuickAdd(LivingQuickAddKind.MEMORY) },
                                )
                            } else {
                                GroupMemoryActiveContent(
                                    momentId = selectedMomentId,
                                    momentTitle = selectedMomentTitle,
                                    refreshToken = groupTabRefreshToken,
                                    onOpenQuickAdd = onAddMemory,
                                )
                            }
                        }
                        context == AppContext.BUSINESS && destination == BottomDestination.PULSE -> {
                            val businessTypeCode = selectedMomentTypeCode
                                ?: moments.firstOrNull { it.momentId == selectedMomentId }?.momentTypeCode
                            val code = businessTypeCode.orEmpty().uppercase()
                            when {
                                code.contains("RUNWAY") -> RunwayPulseActiveContent(
                                    momentId = selectedMomentId,
                                    momentTitle = selectedMomentTitle,
                                    refreshToken = businessTabRefreshToken,
                                    onLogExpense = { onBusinessQuickAdd(BusinessQuickAddKind.EXPENSE) },
                                    onOpenQuickAdd = onOpenQuickAdd,
                                )
                                code.contains("TEAM_OPERATIONS") -> TeamOpsPulseActiveContent(
                                    momentId = selectedMomentId,
                                    momentTitle = selectedMomentTitle,
                                    refreshToken = businessTabRefreshToken,
                                    onLogDelivery = { onBusinessQuickAdd(BusinessQuickAddKind.TEAM_UPDATE) },
                                    onOpenQuickAdd = onOpenQuickAdd,
                                )
                                code.contains("OPERATIONS") && !code.contains("TEAM") -> OpsPulseActiveContent(
                                    momentId = selectedMomentId,
                                    momentTitle = selectedMomentTitle,
                                    refreshToken = businessTabRefreshToken,
                                    onLogSpend = { onBusinessQuickAdd(BusinessQuickAddKind.SPEND_ENTRY) },
                                    onOpenQuickAdd = onOpenQuickAdd,
                                )
                                else -> BusinessPulseActiveContent(
                                    momentId = selectedMomentId,
                                    momentTitle = selectedMomentTitle,
                                    refreshToken = businessTabRefreshToken,
                                    momentTypeCode = businessTypeCode,
                                    onAddExpense = onAddExpense,
                                    onOpenQuickAdd = onOpenQuickAdd,
                                )
                            }
                        }
                        context == AppContext.BUSINESS && destination == BottomDestination.MOMENTS -> {
                            val businessTypeCode = selectedMomentTypeCode
                                ?: moments.firstOrNull { it.momentId == selectedMomentId }?.momentTypeCode
                            val code = businessTypeCode.orEmpty().uppercase()
                            when {
                                code.contains("RUNWAY") -> RunwayMomentsActiveContent(
                                    momentId = selectedMomentId,
                                    momentTitle = selectedMomentTitle,
                                    refreshToken = businessTabRefreshToken,
                                    onLogExpense = { onBusinessQuickAdd(BusinessQuickAddKind.EXPENSE) },
                                    onOpenQuickAdd = onOpenQuickAdd,
                                )
                                code.contains("TEAM_OPERATIONS") -> TeamOpsMomentsActiveContent(
                                    momentId = selectedMomentId,
                                    momentTitle = selectedMomentTitle,
                                    refreshToken = businessTabRefreshToken,
                                    onLogWin = { onBusinessQuickAdd(BusinessQuickAddKind.TEAM_UPDATE) },
                                    onOpenQuickAdd = onOpenQuickAdd,
                                )
                                code.contains("OPERATIONS") && !code.contains("TEAM") -> OpsMomentsActiveContent(
                                    momentId = selectedMomentId,
                                    momentTitle = selectedMomentTitle,
                                    refreshToken = businessTabRefreshToken,
                                    onLogSpend = { onBusinessQuickAdd(BusinessQuickAddKind.SPEND_ENTRY) },
                                    onOpenQuickAdd = onOpenQuickAdd,
                                )
                                else -> BusinessMomentsActiveContent(
                                    momentId = selectedMomentId,
                                    momentTitle = selectedMomentTitle,
                                    momentTypeCode = businessTypeCode,
                                    refreshToken = businessTabRefreshToken,
                                    onOpenQuickAdd = onOpenQuickAdd,
                                )
                            }
                        }
                        context == AppContext.BUSINESS && destination == BottomDestination.LIFE -> {
                            val businessTypeCode = selectedMomentTypeCode
                                ?: moments.firstOrNull { it.momentId == selectedMomentId }?.momentTypeCode
                            BusinessLifeActiveContent(
                                momentId = selectedMomentId,
                                momentTitle = selectedMomentTitle,
                                refreshToken = businessTabRefreshToken,
                                momentTypeCode = businessTypeCode,
                                onViewReport = onViewBusinessReport,
                            )
                        }
                        context == AppContext.BUSINESS && destination == BottomDestination.MEMORY -> {
                            val businessTypeCode = selectedMomentTypeCode
                                ?: moments.firstOrNull { it.momentId == selectedMomentId }?.momentTypeCode
                            val code = businessTypeCode.orEmpty().uppercase()
                            when {
                                code.contains("RUNWAY") -> RunwayMemoryActiveContent(
                                    momentId = selectedMomentId,
                                    momentTitle = selectedMomentTitle,
                                    refreshToken = businessTabRefreshToken,
                                    onRecordLearning = { onBusinessQuickAdd(BusinessQuickAddKind.MEMORY) },
                                )
                                code.contains("TEAM_OPERATIONS") -> TeamOpsMemoryActiveContent(
                                    momentId = selectedMomentId,
                                    momentTitle = selectedMomentTitle,
                                    refreshToken = businessTabRefreshToken,
                                    onRecordLearning = { onBusinessQuickAdd(BusinessQuickAddKind.MEMORY) },
                                    onOpenQuickAdd = onOpenQuickAdd,
                                )
                                code.contains("OPERATIONS") && !code.contains("TEAM") -> OpsMemoryActiveContent(
                                    momentId = selectedMomentId,
                                    momentTitle = selectedMomentTitle,
                                    refreshToken = businessTabRefreshToken,
                                    onRecordMemory = { onBusinessQuickAdd(BusinessQuickAddKind.MEMORY) },
                                )
                                else -> BusinessMemoryActiveContent(
                                    momentId = selectedMomentId,
                                    momentTitle = selectedMomentTitle,
                                    refreshToken = businessTabRefreshToken,
                                    momentTypeCode = businessTypeCode,
                                    onOpenQuickAdd = onOpenQuickAdd,
                                )
                            }
                        }
                        context == AppContext.PERSONAL && destination == BottomDestination.LIFE -> {
                            PersonalLifeActiveContent(
                                refreshToken = personalTabRefreshToken,
                                onLogRecovery = { onLifeOpsQuickAdd(LifeOpsQuickAddKind.RECOVERY) },
                            )
                        }
                        context == AppContext.PERSONAL && destination == BottomDestination.PULSE && isRelationships -> {
                            PersonalRelationshipsPulseActiveContent(
                                refreshToken = personalTabRefreshToken,
                                momentTitle = selectedMomentTitle,
                                momentId = selectedMomentId,
                                onAddExpense = onAddExpense,
                                onRelationshipsQuickAdd = onRelationshipsQuickAdd,
                                onOpenRecentActivity = onOpenRelationshipsActivity,
                            )
                        }
                        context == AppContext.PERSONAL && destination == BottomDestination.PULSE -> {
                            PersonalPulseActiveContent(
                                refreshToken = personalTabRefreshToken,
                                momentTitle = selectedMomentTitle,
                                momentId = selectedMomentId,
                                momentTypeCode = personalTypeCode,
                                onAddExpense = onAddExpense,
                                onLifeOpsQuickAdd = onLifeOpsQuickAdd,
                                onFutureQuickAdd = onFutureQuickAdd,
                                onLifestyleQuickAdd = onLifestyleQuickAdd,
                                onViewAllActivity = onViewAllActivity,
                            )
                        }
                        context == AppContext.PERSONAL && destination == BottomDestination.MOMENTS && isFutureBuilding -> {
                            PersonalFutureMomentsActiveContent(
                                refreshToken = personalTabRefreshToken,
                                momentId = selectedMomentId,
                                momentTitle = selectedMomentTitle,
                                onOpenQuickAdd = onOpenQuickAdd,
                                onAddExpense = onAddExpense,
                            )
                        }
                        context == AppContext.PERSONAL && destination == BottomDestination.MOMENTS && isLifeOps -> {
                            PersonalLifeOpsMomentsActiveContent(
                                refreshToken = personalTabRefreshToken,
                                momentId = selectedMomentId,
                                momentTitle = selectedMomentTitle,
                                onOpenQuickAdd = onOpenQuickAdd,
                                onAddExpense = onAddExpense,
                            )
                        }
                        context == AppContext.PERSONAL && destination == BottomDestination.MOMENTS && isLifestyle -> {
                            PersonalLifestyleMomentsActiveContent(
                                refreshToken = personalTabRefreshToken,
                                momentId = selectedMomentId,
                                momentTitle = selectedMomentTitle,
                                onOpenQuickAdd = onOpenQuickAdd,
                                onAddExpense = onAddExpense,
                            )
                        }
                        context == AppContext.PERSONAL && destination == BottomDestination.MOMENTS && isRelationships -> {
                            PersonalRelationshipsMomentsActiveContent(
                                refreshToken = personalTabRefreshToken,
                                momentId = selectedMomentId,
                                momentTitle = selectedMomentTitle,
                                onOpenQuickAdd = onOpenQuickAdd,
                                onAddExpense = onAddExpense,
                            )
                        }
                        context == AppContext.PERSONAL && destination == BottomDestination.MEMORY && isFutureBuilding -> {
                            PersonalFutureMemoryActiveContent(
                                refreshToken = personalTabRefreshToken,
                                momentId = selectedMomentId,
                                onProtectMilestone = { onFutureQuickAdd(FutureQuickAddKind.MILESTONE) },
                            )
                        }
                        context == AppContext.PERSONAL && destination == BottomDestination.MEMORY && isLifeOps -> {
                            PersonalLifeOpsMemoryActiveContent(
                                refreshToken = personalTabRefreshToken,
                                momentId = selectedMomentId,
                                onProtectRecovery = { onLifeOpsQuickAdd(LifeOpsQuickAddKind.RECOVERY) },
                            )
                        }
                        context == AppContext.PERSONAL && destination == BottomDestination.MEMORY && isLifestyle -> {
                            PersonalLifestyleMemoryActiveContent(
                                refreshToken = personalTabRefreshToken,
                                momentId = selectedMomentId,
                                onLogExperience = { onLifestyleQuickAdd(LifestyleQuickAddKind.EXPERIENCE) },
                            )
                        }
                        context == AppContext.PERSONAL && destination == BottomDestination.MEMORY && isRelationships -> {
                            PersonalRelationshipsMemoryActiveContent(
                                refreshToken = personalTabRefreshToken,
                                momentId = selectedMomentId,
                                onLogConnection = { onRelationshipsQuickAdd(RelationshipsQuickAddKind.CONNECTION) },
                            )
                        }
                        context == AppContext.PERSONAL && destination == BottomDestination.CREATE -> {
                            PersonalQuickAddHub(
                                hasActiveMoment = selectedMomentId != null,
                                onClose = onCreateBack,
                                onIncome = { onMoneyQuickAdd(MoneyQuickAddKind.INCOME) },
                                onRecovery = { onLifeOpsQuickAdd(LifeOpsQuickAddKind.RECOVERY) },
                                onMood = { onLifeOpsQuickAdd(LifeOpsQuickAddKind.MOOD) },
                                onAttention = { onLifeOpsQuickAdd(LifeOpsQuickAddKind.ATTENTION) },
                                onAdjust = { onLifeOpsQuickAdd(LifeOpsQuickAddKind.ADJUST) },
                                onTransfer = { onMoneyQuickAdd(MoneyQuickAddKind.TRANSFER) },
                                onSavings = { onMoneyQuickAdd(MoneyQuickAddKind.SAVINGS) },
                                onFutureQuickAdd = onFutureQuickAdd,
                                onLifestyleQuickAdd = onLifestyleQuickAdd,
                                onRelationshipsQuickAdd = onRelationshipsQuickAdd,
                                momentTypeCode = personalTypeCode,
                                capabilities = capabilities,
                            )
                        }
                        else -> {
                            if (context == AppContext.CIRCLE) {
                                com.example.momentra.ui.shell.circle.CircleComingSoonContent()
                            } else {
                                EmptyPanel(
                                    title = "${context.label} · ${destination.label}",
                                    body = "Active Moment ready. Product features arrive in later phases.",
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable

private fun EmptyPanel(
    title: String,
    body: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title,
            color = MomentraBrandColors.TextOnDark,
            fontWeight = FontWeight.SemiBold,
            fontSize = 20.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = body,
            color = ShellTokens.EmptyBody,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(16.dp))
            Button(onClick = onAction) { Text(actionLabel) }
        }
    }
}

/** Slim chrome when top bar + context tabs are collapsed. */
@Composable
private fun CompactShellChrome(
    onExpand: () -> Unit,
    onNewMoment: () -> Unit,
    onAvatar: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ShellTokens.TopBarBackground)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        MomentraWordmark(
            showTagline = false,
            titleSizeSp = 16f,
            taglineSizeSp = 6f,
            alignStart = true,
            modifier = Modifier.clickable(onClick = onExpand),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(ShellTokens.IconTap)
                    .clip(CircleShape)
                    .background(MomentraBrandColors.Cta)
                    .clickable(onClick = onNewMoment),
                contentAlignment = Alignment.Center,
            ) {
                Text("+", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Box(
                modifier = Modifier
                    .size(ShellTokens.AvatarSize)
                    .clip(CircleShape)
                    .background(ShellTokens.ActionCircle)
                    .clickable(onClick = onAvatar),
                contentAlignment = Alignment.Center,
            ) {
                Text("··", color = Color.White, fontSize = 11.sp)
            }
            Icon(
                Icons.Outlined.KeyboardArrowDown,
                contentDescription = "Expand top bar",
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier
                    .size(20.dp)
                    .clickable(onClick = onExpand),
            )
        }
    }
}
