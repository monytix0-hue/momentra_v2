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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.delay
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext
import com.example.momentra.data.local.AppPreferences
import com.example.momentra.data.local.PendingCompanyJoinInvite
import com.example.momentra.data.local.PendingJoinInvite
import com.example.momentra.data.local.PendingDeepLink
import com.example.momentra.data.repository.AccountRepository
import com.example.momentra.ui.notifications.NotificationInboxSheet
import com.example.momentra.domain.AppContext
import com.example.momentra.data.repository.GroupSliceRepository
import com.example.momentra.ui.shell.group.shared.GroupViewerAccess
import com.example.momentra.domain.BottomDestination
import com.example.momentra.domain.CompanySummary
import com.example.momentra.domain.MomentExperienceKind
import com.example.momentra.domain.MomentSummary
import com.example.momentra.domain.ShellContentState
import com.example.momentra.domain.ShellIdentity
import com.example.momentra.domain.isActiveStatus
import com.example.momentra.domain.isCompletedStatus
import com.example.momentra.ui.shell.tour.TourHost
import com.example.momentra.ui.shell.tour.TourSignal
import com.example.momentra.ui.shell.tour.TourWhereToLook
import com.example.momentra.ui.shell.tour.rememberTourController
import com.example.momentra.ui.shell.tour.rememberTourTargetRegistry
import com.example.momentra.ui.shell.components.ContextSwitcher
import com.example.momentra.ui.shell.components.EditMomentSetupHost
import com.example.momentra.ui.shell.components.ManageMomentSheet
import com.example.momentra.ui.shell.business.appstate.BusinessGlassSheetChrome
import com.example.momentra.ui.shell.business.appstate.BusinessGlassModalBottomSheet
import com.example.momentra.ui.shell.business.appstate.BusinessAppStateViewAllSheet
import com.example.momentra.ui.shell.business.appstate.BusinessAppStateAccent
import com.example.momentra.ui.shell.components.MomentSwitcher
import com.example.momentra.ui.shell.components.MomentraFullscreenDialog
import com.example.momentra.ui.shell.components.MomentraTopBar
import com.example.momentra.ui.shell.components.momentraMaxWidth
import com.example.momentra.ui.shell.components.rememberMomentraWindowSize
import com.example.momentra.ui.shell.components.MomentraTopBarConfig
import com.example.momentra.ui.shell.components.ShellBottomNavigation
import com.example.momentra.ui.shell.components.label
import com.example.momentra.ui.shell.empty.ContextEmptyExperience
import com.example.momentra.data.local.OfflineOutbox
import com.example.momentra.ui.shell.business.appstate.BusinessAppStateHost
import com.example.momentra.ui.shell.business.appstate.BusinessAppStateToast
import com.example.momentra.ui.shell.business.appstate.BusinessAppStateToastHost
import com.example.momentra.ui.shell.empty.GroupCreateFlow
import com.example.momentra.ui.shell.empty.group.GroupCreatePhase
import com.example.momentra.ui.shell.empty.group.GroupJoinConfirmSheet
import com.example.momentra.ui.shell.empty.group.GroupJoinQrScanner
import com.example.momentra.ui.shell.momentswitcher.MomentSwitcherDirectory
import com.example.momentra.ui.shell.momentswitcher.businessMomentSwitcherSpec
import com.example.momentra.ui.shell.momentswitcher.groupMomentSwitcherSpec
import com.example.momentra.ui.shell.group.shared.GroupBudgetSheet
import com.example.momentra.ui.shell.group.shared.GroupCollabKind
import com.example.momentra.ui.shell.group.shared.GroupCollabSheet
import com.example.momentra.ui.shell.group.shared.GroupContributionSheet
import com.example.momentra.ui.shell.group.shared.GroupInvitePeopleSheet
import com.example.momentra.ui.shell.group.shared.GroupSettlementSheet
import com.example.momentra.ui.shell.group.shared.GroupExpenseSheet
import com.example.momentra.ui.shell.group.shared.GroupTabDataCache
import com.example.momentra.ui.shell.group.shared.MomentStoryViewerScreen
import com.example.momentra.ui.shell.group.life.GroupLifeActiveContent
import com.example.momentra.ui.shell.group.life.GroupLifeQuickAction
import com.example.momentra.ui.shell.group.trip.create.GroupQuickAddHub
import com.example.momentra.ui.shell.group.trip.memory.GroupMemoryActiveContent
import com.example.momentra.ui.shell.group.trip.moments.GroupMomentsActiveContent
import com.example.momentra.ui.shell.group.trip.pulse.GroupPulseActiveContent
import com.example.momentra.ui.shell.group.shared.GroupParticipantsSheet
import com.example.momentra.ui.shell.group.shared.GroupRecentActivityFlow
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
import com.example.momentra.ui.shell.personal.lifeops.create.LifeOpsQuickAddKind
import com.example.momentra.ui.shell.personal.lifeops.create.MoneyQuickAddKind
import com.example.momentra.ui.shell.personal.lifeops.create.PersonalLifeOpsQuickAddSheet
import com.example.momentra.ui.shell.personal.lifeops.create.PersonalMoneyQuickAddSheet
import com.example.momentra.ui.shell.personal.shared.PersonalEveningNudgeScheduler
import com.example.momentra.ui.shell.personal.shared.PersonalMasterExpenseSheet
import com.example.momentra.ui.shell.personal.shared.PersonalPulseFamily
import com.example.momentra.ui.shell.personal.shared.PersonalRecentActivityFlow
import com.example.momentra.ui.shell.personal.shared.missingPersonalSetupSystems
import com.example.momentra.ui.shell.personal.shared.personalPulseFamilyFor
import com.example.momentra.ui.shell.personal.shared.presentPersonalFamilies
import com.example.momentra.ui.shell.personal.shared.resolveFamilyTargetMoment
import com.example.momentra.ui.shell.personal.shared.resolvePreferredPersonalMoment
import com.example.momentra.ui.shell.empty.personal.PersonalSetupSystem
import com.example.momentra.ui.shell.empty.personal.PersonalSetupWizardContent
import com.example.momentra.ui.splash.MomentraWordmark
import com.example.momentra.ui.theme.MomentraBrandColors
import com.example.momentra.ui.theme.ShellTokens
import com.example.momentra.ui.theme.shell.GlobalSurfaceTheme
import android.widget.Toast
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppShellScreen(
    identity: ShellIdentity,
    onSignOut: () -> Unit,
    onSessionExpired: () -> Unit = {},
    onDisplayNameSaved: (String) -> Unit = {},
    shellViewModel: AppShellViewModel,
) {
    val context = LocalContext.current
    val prefs = remember { AppPreferences(context) }
    val state by shellViewModel.state.collectAsState()
    val tourController = rememberTourController(prefs)
    val tourRegistry = rememberTourTargetRegistry()

    var pendingGroupJoinCode by remember { mutableStateOf<String?>(null) }
    var pendingCompanyJoinCode by remember { mutableStateOf<String?>(null) }
    var inboxOpen by remember { mutableStateOf(false) }
    var storyMomentId by remember { mutableStateOf<String?>(null) }
    var unreadNotificationCount by remember { mutableIntStateOf(0) }
    val accountRepository = remember { AccountRepository() }
    val shellScope = rememberCoroutineScope()

    fun handlePendingPushDeepLink() {
        val pending = PendingDeepLink.peek(context) ?: return
        if (PendingDeepLink.isInboxLink(pending.link)) {
            PendingDeepLink.consume(context)
            inboxOpen = true
            pending.userNotificationId?.let { id ->
                shellScope.launch {
                    accountRepository.markNotificationsRead(notificationIds = listOf(id))
                }
            }
            return
        }
        val momentId = PendingDeepLink.parseMomentId(pending.link) ?: return
        val openStory = PendingDeepLink.isStoryLink(pending.link)
        if (openStory) {
            shellViewModel.openMomentFromDeepLink(momentId)
            storyMomentId = momentId
            PendingDeepLink.consume(context)
            pending.userNotificationId?.let { id ->
                shellScope.launch {
                    accountRepository.markNotificationsRead(notificationIds = listOf(id))
                }
            }
            return
        }
        if (!shellViewModel.openMomentFromDeepLink(momentId)) return
        PendingDeepLink.consume(context)
        pending.userNotificationId?.let { id ->
            shellScope.launch {
                accountRepository.markNotificationsRead(notificationIds = listOf(id))
            }
        }
    }

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
        prefs.getPendingCompanyJoinCode()?.let { PendingCompanyJoinInvite.hydrate(it) }
        PendingCompanyJoinInvite.code.collect { offered ->
            if (offered.isNullOrBlank()) return@collect
            val code = PendingCompanyJoinInvite.consume(prefs) ?: return@collect
            pendingCompanyJoinCode = code
        }
    }

    LaunchedEffect(state.identity?.userId) {
        if (state.identity?.userId == null) return@LaunchedEffect
        PendingDeepLink.hydrateFromDisk(context)
        PendingDeepLink.link.collect { offered ->
            if (offered.isNullOrBlank()) return@collect
            handlePendingPushDeepLink()
        }
    }

    LaunchedEffect(state.moments.map { it.momentId }, state.identity?.userId) {
        if (state.identity?.userId == null) return@LaunchedEffect
        handlePendingPushDeepLink()
    }

    LaunchedEffect(state.identity?.userId) {
        if (state.identity?.userId == null) return@LaunchedEffect
        accountRepository.listNotifications(limit = 1, unreadOnly = true)
            .onSuccess { unreadNotificationCount = it.unreadCount }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (state.identity?.userId != null) {
            shellScope.launch {
                accountRepository.listNotifications(limit = 1, unreadOnly = true)
                    .onSuccess { unreadNotificationCount = it.unreadCount }
            }
        }
        if (state.selectedContext == AppContext.GROUP) {
            shellViewModel.refreshVisibleGroupTab()
        }
    }

    var moneyQa by remember { mutableStateOf<MoneyQuickAddKind?>(null) }
    /** Required target for Personal quick-add sheets (never fall back blindly). */
    var personalQaMomentId by remember { mutableStateOf<String?>(null) }
    var personalQaTypeCode by remember { mutableStateOf<String?>(null) }
    var personalSetupSystem by remember { mutableStateOf<PersonalSetupSystem?>(null) }
    var personalSetupChooserOpen by remember { mutableStateOf(false) }
    var groupExpenseSheetOpen by remember { mutableStateOf(false) }
    var groupContributionSheetOpen by remember { mutableStateOf(false) }
    var groupSettlementSheetOpen by remember { mutableStateOf(false) }
    var groupBudgetSheetOpen by remember { mutableStateOf(false) }
    var groupParticipantsSheetOpen by remember { mutableStateOf(false) }
    var groupInviteSheetOpen by remember { mutableStateOf(false) }
    var groupMomentDirectoryOpen by remember { mutableStateOf(false) }
    var businessMomentDirectoryOpen by remember { mutableStateOf(false) }
    var groupDirectoryPreferCompleted by remember { mutableStateOf(false) }
    var groupViewerReadOnly by remember { mutableStateOf(false) }
    var groupCollabKind by remember { mutableStateOf<GroupCollabKind?>(null) }
    var groupFinanceOpen by remember { mutableStateOf(false) }
    var groupSplitsOpen by remember { mutableStateOf(false) }
    var weddingGapQa by remember { mutableStateOf<WeddingQuickAddKind?>(null) }
    var experienceGapQa by remember { mutableStateOf<ExperienceQuickAddKind?>(null) }
    var purchaseGapQa by remember { mutableStateOf<PurchaseQuickAddKind?>(null) }
    var livingGapQa by remember { mutableStateOf<LivingQuickAddKind?>(null) }
    var lifeOpsQa by remember { mutableStateOf<LifeOpsQuickAddKind?>(null) }
    LaunchedEffect(lifeOpsQa) {
        lifeOpsQa?.let { kind ->
            ShellPerf.instant("lifeops_sheet_open", mapOf("kind" to kind.name))
        }
    }
    var recentActivityOpen by remember { mutableStateOf(false) }
    var groupRecentActivityOpen by remember { mutableStateOf(false) }
    var newMomentOpen by remember { mutableStateOf(false) }
    var groupCreatePhase by remember { mutableStateOf(GroupCreatePhase.CHOOSER) }
    var preferGroupCreateFlow by remember { mutableStateOf(false) }
    // Callers: openNewMoment (Business +), ShellDestinationContent useOperatingHub gate.
    // Separates Create New Moment from Add Operating Event (CREATE tab Quick Add).
    // User: "Separate Create New Business Moment from Add Operating Event."
    var preferBusinessCreateFlow by remember { mutableStateOf(false) }
    var showManageMoment by remember { mutableStateOf(false) }
    var businessAttentionViewAllOpen by remember { mutableStateOf(false) }
    var businessAttentionFilter by remember { mutableStateOf("Open") }
    var editSetupOpen by remember { mutableStateOf(false) }
    var topChromeExpanded by remember { mutableStateOf(true) }
    var showJoinQrScanner by remember { mutableStateOf(false) }
    var habitRewardMessage by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(habitRewardMessage) {
        if (habitRewardMessage == null) return@LaunchedEffect
        delay(2500)
        habitRewardMessage = null
    }
    val shellAccent = com.example.momentra.ui.theme.shell.ContextThemes.of(state.selectedContext).contextAccent
    val momentAccent = com.example.momentra.ui.theme.shell.MomentThemes.resolve(
        state.selectedContext,
        state.selectedMomentTypeCode,
    ).primary
    val lifeOpsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val openNewMoment: () -> Unit = {
        when (state.selectedContext) {
            AppContext.PERSONAL -> {
                shellViewModel.selectBottomDestination(BottomDestination.CREATE)
            }
            AppContext.GROUP -> {
                groupCreatePhase = GroupCreatePhase.CHOOSER
                preferGroupCreateFlow = true
                shellViewModel.selectBottomDestination(BottomDestination.CREATE)
            }
            // Create New Moment — never Quick Add, even when a Moment is already selected.
            AppContext.BUSINESS -> {
                preferBusinessCreateFlow = true
                shellViewModel.selectBottomDestination(BottomDestination.CREATE)
            }
            else -> shellViewModel.selectBottomDestination(BottomDestination.CREATE)
        }
    }
    LaunchedEffect(identity.userId, state.selectedContext, state.selectedMomentId) {
        if (state.selectedContext != AppContext.GROUP || state.selectedMomentId.isNullOrBlank()) {
            groupViewerReadOnly = false
            return@LaunchedEffect
        }
        GroupSliceRepository().getParticipants(state.selectedMomentId!!).onSuccess { payload ->
            groupViewerReadOnly = GroupViewerAccess.isViewer(payload.participants, identity.userId)
        }
    }

    LaunchedEffect(Unit) {
        val activity = context as? android.app.Activity ?: return@LaunchedEffect
        if (activity.intent?.getBooleanExtra(
                PersonalEveningNudgeScheduler.EXTRA_OPEN_PERSONAL_PULSE,
                false,
            ) == true
        ) {
            activity.intent?.removeExtra(PersonalEveningNudgeScheduler.EXTRA_OPEN_PERSONAL_PULSE)
            shellViewModel.selectContext(AppContext.PERSONAL)
            shellViewModel.selectBottomDestination(BottomDestination.PULSE)
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
    fun openPersonalSheet(momentId: String?, typeCode: String? = null) {
        if (momentId.isNullOrBlank()) return
        personalQaMomentId = momentId
        personalQaTypeCode = typeCode
            ?: state.moments.firstOrNull { it.momentId == momentId }?.momentTypeCode
    }

    fun clearPersonalSheetTarget() {
        personalQaMomentId = null
        personalQaTypeCode = null
    }

    fun targetForFamily(family: PersonalPulseFamily): MomentSummary? =
        resolveFamilyTargetMoment(state.moments, family, state.selectedMomentId)

    fun openLifeOpsQa(kind: LifeOpsQuickAddKind) {
        val m = targetForFamily(PersonalPulseFamily.LIFE_OPERATIONS)
        if (m == null) {
            personalSetupSystem = PersonalSetupSystem.LIFE_OPERATIONS
            return
        }
        openPersonalSheet(m.momentId, m.momentTypeCode)
        lifeOpsQa = kind
    }

    fun openMoneyQa(kind: MoneyQuickAddKind) {
        val m = targetForFamily(PersonalPulseFamily.LIFE_OPERATIONS)
        if (m == null) {
            personalSetupSystem = PersonalSetupSystem.LIFE_OPERATIONS
            return
        }
        openPersonalSheet(m.momentId, m.momentTypeCode)
        moneyQa = kind
    }

    LaunchedEffect(
        identity.userId,
        state.selectedContext,
        state.moments.map { it.momentId to it.momentTypeCode },
        state.selectedMomentId,
    ) {
        if (state.selectedContext != AppContext.PERSONAL) return@LaunchedEffect
        shellViewModel.applyPreferredPersonalLock()
    }
    LaunchedEffect(identity.userId, state.selectedContext, state.contextContent, state.selectedMomentId) {
        if (state.identity?.userId == null) return@LaunchedEffect
        when (state.selectedContext) {
            AppContext.PERSONAL -> {
                if (state.contextContent is ShellContentState.Loading ||
                    state.contextContent is ShellContentState.Idle
                ) {
                    return@LaunchedEffect
                }
                tourController.startPersonal()
            }
            AppContext.GROUP -> tourController.startGroupMini()
            AppContext.BUSINESS -> tourController.startBusinessMini()
            AppContext.CIRCLE -> Unit
        }
    }

    LaunchedEffect(state.bottomDestination) {
        if (state.bottomDestination == BottomDestination.CREATE) {
            tourController.onSignal(TourSignal.OPENED_CREATE_TAB)
        }
    }

    TourHost(
        controller = tourController,
        registry = tourRegistry,
        onNavigate = { dest ->
            newMomentOpen = false
            groupMomentDirectoryOpen = false
            businessMomentDirectoryOpen = false
            shellViewModel.selectBottomDestination(dest)
        },
    ) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ShellTokens.SurfaceContent),
    ) {
        state.offlineNotice?.let { notice ->
            val failed = OfflineOutbox.failedMessage()
            val pending = OfflineOutbox.pendingCount()
            val businessToast = if (state.selectedContext == AppContext.BUSINESS) {
                when {
                    failed != null -> BusinessAppStateToast.Error(code = "SYNC_TIMEOUT_504")
                    pending > 0 -> BusinessAppStateToast.OfflineQueue(queuedCount = pending)
                    else -> null
                }
            } else {
                null
            }
            if (businessToast != null) {
                BusinessAppStateToastHost(
                    toast = businessToast,
                    onRetry = { shellViewModel.selectContext(state.selectedContext) },
                )
            } else {
                Text(
                    text = notice,
                    color = Color.White,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF3A2A12))
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }
        val groupDirectoryHome = state.groupMomentDirectoryHome &&
            state.selectedContext == AppContext.GROUP
        val groupDirectoryVisible = state.selectedContext == AppContext.GROUP &&
            (groupMomentDirectoryOpen || groupDirectoryHome)
        val businessDirectoryVisible = state.selectedContext == AppContext.BUSINESS &&
            businessMomentDirectoryOpen
        if (!groupDirectoryVisible && !businessDirectoryVisible) {
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
                            life360Available = true,
                            globalCreateAvailable = state.selectedContext != AppContext.PERSONAL,
                            qrScanAvailable = true,
                            referAvailable = true,
                            unreadNotificationCount = unreadNotificationCount,
                        ),
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
                        onInbox = { inboxOpen = true },
                        onAvatar = { shellViewModel.openProfile(true) },
                    )
                    ContextSwitcher(
                        selectedContext = state.selectedContext,
                        supportedContexts = state.supportedContexts,
                        onSelect = {
                            newMomentOpen = false
                            groupMomentDirectoryOpen = false
                            businessMomentDirectoryOpen = false
                            groupCreatePhase = GroupCreatePhase.CHOOSER
                            preferGroupCreateFlow = false
                            shellViewModel.selectContext(it)
                        },
                    )
                }
            }
            if (!topChromeExpanded) {
                CompactShellChrome(
                    showNewMoment = state.selectedContext != AppContext.PERSONAL,
                    onExpand = { topChromeExpanded = true },
                    onNewMoment = openNewMoment,
                    onAvatar = { shellViewModel.openProfile(true) },
                )
            }
            if (!newMomentOpen && state.showMomentSwitcher && !groupDirectoryVisible && !businessDirectoryVisible) {
                if (state.selectedContext == AppContext.PERSONAL) {
                    val needsEveryday = state.moments.none {
                        it.isActiveStatus() &&
                            personalPulseFamilyFor(it.momentTypeCode) == PersonalPulseFamily.LIFE_OPERATIONS
                    } && state.moments.any { it.isActiveStatus() }
                    PersonalUnifiedChrome(
                        accent = momentAccent,
                        showManage = state.selectedMomentId != null,
                        showSetUpEveryday = needsEveryday,
                        onManage = { showManageMoment = true },
                        onSetUpEveryday = {
                            personalSetupSystem = PersonalSetupSystem.LIFE_OPERATIONS
                        },
                    )
                } else {
                    MomentSwitcher(
                        selectedTitle = state.selectedMomentTitle,
                        selectedMomentId = state.selectedMomentId,
                        activeMoments = state.moments
                            .filter { it.isActiveStatus() || it.momentId == state.selectedMomentId }
                            .map { it.momentId to it.title },
                        isEmpty = state.contextContent is ShellContentState.Empty &&
                            state.moments.none { it.isActiveStatus() || it.isCompletedStatus() },
                        isLoading = state.contextContent is ShellContentState.Loading,
                        accent = momentAccent,
                        selectedIsCompleted = state.moments
                            .firstOrNull { it.momentId == state.selectedMomentId }
                            ?.isCompletedStatus() == true,
                        onSelectMoment = shellViewModel::selectMoment,
                        onSettings = {
                            if (state.selectedMomentId != null) showManageMoment = true
                        },
                        onInvite = when (state.selectedContext) {
                            AppContext.GROUP -> {
                                { groupInviteSheetOpen = true }
                            }
                            // Business: same invite affordance as Group; V4 no-op until rebuild.
                            AppContext.BUSINESS -> {
                                { }
                            }
                            else -> null
                        },
                        useDirectorySelector = state.selectedContext == AppContext.GROUP ||
                            state.selectedContext == AppContext.BUSINESS,
                        startExpanded = false,
                        participantNames = emptyList(),
                        onOpenDirectory = {
                            if (state.selectedContext == AppContext.BUSINESS) {
                                businessMomentDirectoryOpen = true
                            } else {
                                groupDirectoryPreferCompleted = false
                                groupMomentDirectoryOpen = true
                            }
                        },
                    )
                }
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
        }
        val shellWindow = rememberMomentraWindowSize()
        Box(
            modifier = Modifier
                .weight(1f)
                .align(Alignment.CenterHorizontally)
                .momentraMaxWidth(shellWindow.contentMaxWidth),
        ) {
            val operatingCompanyId = state.selectedCompany?.companyId
                ?: state.moments.firstOrNull { it.momentId == state.selectedMomentId }?.companyId
            ShellDestinationContent(
                    context = state.selectedContext,
                    destination = state.bottomDestination,
                    content = state.contextContent,
                    experience = state.momentExperience,
                    moments = state.moments,
                    hasCompany = state.selectedCompany != null,
                    companyId = operatingCompanyId,
                    companyName = state.selectedCompany?.displayName,
                    selectedMomentId = state.selectedMomentId,
                    selectedMomentTitle = state.selectedMomentTitle,
                    selectedMomentTypeCode = state.selectedMomentTypeCode,
                    personalTabRefreshToken = state.personalTabRefreshToken,
                    groupTabRefreshToken = state.groupTabRefreshToken,
                    businessTabRefreshToken = state.businessTabRefreshToken,
                    capabilities = state.capabilities,
                    forcePersonalSimple = true,
                    personalPresentFamilies = presentPersonalFamilies(state.moments),
                    viewerReadOnly = groupViewerReadOnly,
                    onRetry = { shellViewModel.selectContext(state.selectedContext) },
                    onSelectDestination = { shellViewModel.selectBottomDestination(it) },
                    onSessionExpired = onSessionExpired,
                    onCreateMoment = openNewMoment,
                    onOpenPersonalLife = { shellViewModel.selectBottomDestination(BottomDestination.LIFE) },
                    onOpenPersonalPulse = { shellViewModel.selectBottomDestination(BottomDestination.PULSE) },
                    onOpenPersonalMoments = { shellViewModel.selectBottomDestination(BottomDestination.MOMENTS) },
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
                    onMomentCreated = { id, title, momentTypeCode, status ->
                        groupCreatePhase = GroupCreatePhase.CHOOSER
                        preferGroupCreateFlow = false
                        preferBusinessCreateFlow = false
                        shellViewModel.onMomentCreated(id, title, momentTypeCode, status)
                        tourController.onSignal(TourSignal.MOMENT_CREATED)
                    },
                    preferBusinessCreateFlow = preferBusinessCreateFlow,
                    onPreferBusinessCreateFlow = { preferBusinessCreateFlow = it },
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
                            AppContext.GROUP -> if (!groupViewerReadOnly) groupExpenseSheetOpen = true
                            AppContext.BUSINESS -> Unit
                            else -> openMoneyQa(MoneyQuickAddKind.MASTER_EXPENSE)
                        }
                    },
                    onAddContribution = { if (!groupViewerReadOnly) groupContributionSheetOpen = true },
                    onAddSettlement = { groupSettlementSheetOpen = true },
                    onAddBudget = { groupBudgetSheetOpen = true },
                    onAddParticipants = { groupParticipantsSheetOpen = true },
                    onAddInvite = { groupInviteSheetOpen = true },
                    onAddPlanning = { groupCollabKind = GroupCollabKind.PLANNING },
                    onAddChecklist = { groupCollabKind = GroupCollabKind.CHECKLIST },
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
                    onBusinessQuickAdd = { },
                    onAddRevenue = { },
                    onAddInvoice = { },
                    onAddMembers = { },
                    onOpenQuickAdd = { shellViewModel.selectBottomDestination(BottomDestination.CREATE) },
                    onOpenMoments = { shellViewModel.selectBottomDestination(BottomDestination.MOMENTS) },
                    onViewBusinessReport = { },
                    onOpenBusinessFinance = { },
                    onOpenVendor = { },
                    onLifeOpsQuickAdd = { openLifeOpsQa(it) },
                    onMoneyQuickAdd = { openMoneyQa(it) },
                    onViewAllActivity = { recentActivityOpen = true },
                    onPersonalActivityChanged = { shellViewModel.refreshVisiblePersonalTab() },
                    onViewAllGroupActivity = { groupRecentActivityOpen = true },
                    onViewAllTeamActivity = { },
                    onViewAllBusinessActivity = { },
                    onViewAllBusinessAttention = { businessAttentionViewAllOpen = true },
                    onSelectMoment = shellViewModel::selectMoment,
                    onOpenCompletedMoments = {
                        groupDirectoryPreferCompleted = true
                        groupMomentDirectoryOpen = true
                    },
                )
            // One spec-driven switcher serves both contexts (Figma 2678:37791).
            val businessSwitcher = state.selectedContext == AppContext.BUSINESS
            MomentSwitcherDirectory(
                spec = if (businessSwitcher) {
                    businessMomentSwitcherSpec(
                        companyName = state.selectedCompany?.displayName.orEmpty()
                            .ifBlank { "Your business" },
                    )
                } else {
                    groupMomentSwitcherSpec(allowDismiss = !groupDirectoryHome)
                },
                moments = state.moments,
                selectedMomentId = state.selectedMomentId,
                visible = groupDirectoryVisible || businessDirectoryVisible,
                onDismiss = {
                    if (businessSwitcher) {
                        businessMomentDirectoryOpen = false
                    } else if (!groupDirectoryHome) {
                        groupMomentDirectoryOpen = false
                        groupDirectoryPreferCompleted = false
                    }
                },
                onSelectMoment = { momentId ->
                    shellViewModel.selectMoment(momentId)
                    groupMomentDirectoryOpen = false
                    groupDirectoryPreferCompleted = false
                    businessMomentDirectoryOpen = false
                },
                onSelectCompletedMoment = { moment ->
                    if (businessSwitcher) {
                        shellViewModel.selectCompletedBusinessMoment(moment)
                    } else {
                        shellViewModel.selectCompletedGroupMoment(moment)
                    }
                    groupMomentDirectoryOpen = false
                    groupDirectoryPreferCompleted = false
                    businessMomentDirectoryOpen = false
                },
                onOpenStory = { momentId ->
                    storyMomentId = momentId
                    groupMomentDirectoryOpen = false
                    groupDirectoryPreferCompleted = false
                    businessMomentDirectoryOpen = false
                },
                onCreateMoment = {
                    groupMomentDirectoryOpen = false
                    groupDirectoryPreferCompleted = false
                    businessMomentDirectoryOpen = false
                    openNewMoment()
                },
                initialCompletedTab = !businessSwitcher && (
                    groupDirectoryPreferCompleted ||
                        (groupDirectoryHome && state.moments.none { it.isActiveStatus() })
                    ),
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding(),
            )
        }
        if (personalQaMomentId != null && state.selectedContext == AppContext.PERSONAL) {
            moneyQa?.let { kind ->
                val sheetMomentId = personalQaMomentId!!
                when (kind) {
                    MoneyQuickAddKind.MASTER_EXPENSE -> {
                        val pulseFamily = personalPulseFamilyFor(
                            personalQaTypeCode ?: state.selectedMomentTypeCode,
                        )
                        PersonalMasterExpenseSheet(
                            momentId = sheetMomentId,
                            visible = true,
                            pulseFamily = pulseFamily,
                            onDismiss = {
                                moneyQa = null
                                clearPersonalSheetTarget()
                            },
                            onSaved = {
                                moneyQa = null
                                clearPersonalSheetTarget()
                                shellViewModel.refreshVisiblePersonalTab()
                                habitRewardMessage = "Saved · counts toward your day"
                                PersonalEveningNudgeScheduler.onTodaySave(context)
                                tourController.showWhereToLook(TourWhereToLook.forMoney(kind))
                            },
                        )
                    }
                    MoneyQuickAddKind.INCOME, MoneyQuickAddKind.TRANSFER, MoneyQuickAddKind.SAVINGS -> {
                        PersonalMoneyQuickAddSheet(
                            kind = kind,
                            momentId = sheetMomentId,
                            visible = true,
                            onDismiss = {
                                moneyQa = null
                                clearPersonalSheetTarget()
                            },
                            onSaved = {
                                moneyQa = null
                                clearPersonalSheetTarget()
                                shellViewModel.refreshVisiblePersonalTab()
                                habitRewardMessage = "Saved · counts toward your day"
                                tourController.showWhereToLook(TourWhereToLook.forMoney(kind))
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
                onSaved = {
                    shellViewModel.refreshVisibleGroupTab()
                    tourController.showWhereToLook(TourWhereToLook.forGroupGeneric("Expense saved"))
                },
                isWedding = isWeddingFinance,
                momentTypeCode = groupExpenseTypeCode,
            )
            GroupContributionSheet(
                momentId = state.selectedMomentId!!,
                visible = groupContributionSheetOpen,
                onDismiss = { groupContributionSheetOpen = false },
                onSaved = {
                    shellViewModel.refreshVisibleGroupTab()
                    tourController.showWhereToLook(TourWhereToLook.forGroupGeneric("Contribution saved"))
                },
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
                    pendingCompanyJoinCode = code
                },
                onDismiss = { showJoinQrScanner = false },
            )
        }
        pendingGroupJoinCode?.let { code ->
            GroupJoinConfirmSheet(
                code = code,
                visible = true,
                onDismiss = { pendingGroupJoinCode = null },
                onJoin = { onFinished ->
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
                                onFinished()
                            },
                            onFailure = {
                                Toast.makeText(
                                    context,
                                    it.message ?: "Could not join",
                                    Toast.LENGTH_SHORT,
                                ).show()
                                onFinished()
                            },
                        )
                    }
                },
            )
        }
        lifeOpsQa?.let { kind ->
            val momentId = personalQaMomentId
            if (momentId != null) {
                ModalBottomSheet(
                    onDismissRequest = {
                        lifeOpsQa = null
                        clearPersonalSheetTarget()
                    },
                    sheetState = lifeOpsSheetState,
                    containerColor = Color(0xFF14121B),
                    dragHandle = null,
                ) {
                    PersonalLifeOpsQuickAddSheet(
                        kind = kind,
                        momentId = momentId,
                        onClose = {
                            lifeOpsQa = null
                            clearPersonalSheetTarget()
                        },
                        onSaved = {
                            lifeOpsQa = null
                            clearPersonalSheetTarget()
                            shellViewModel.refreshVisiblePersonalTab()
                            habitRewardMessage = "Logged · your day is updating"
                            if (kind == LifeOpsQuickAddKind.MOOD || kind == LifeOpsQuickAddKind.RECOVERY) {
                                PersonalEveningNudgeScheduler.onTodaySave(context)
                            }
                            tourController.showWhereToLook(TourWhereToLook.forLifeOps(kind))
                        },
                    )
                }
            }
        }
        PersonalRecentActivityFlow(
            momentId = state.selectedMomentId,
            visible = recentActivityOpen,
            onDismiss = { recentActivityOpen = false },
            onChanged = { shellViewModel.refreshVisiblePersonalTab() },
        )
        GroupRecentActivityFlow(
            momentId = state.selectedMomentId,
            visible = groupRecentActivityOpen,
            onDismiss = { groupRecentActivityOpen = false },
            onChanged = { shellViewModel.refreshVisibleGroupTab(forcePrefetch = true) },
            momentTypeCode = state.selectedMomentTypeCode
                ?: state.moments.firstOrNull { it.momentId == state.selectedMomentId }?.momentTypeCode,
        )

        if (businessAttentionViewAllOpen) {
            BusinessAppStateViewAllSheet(
                accent = BusinessAppStateAccent.Cyan,
                selectedFilter = businessAttentionFilter,
                onSelectFilter = { businessAttentionFilter = it },
                onClose = { businessAttentionViewAllOpen = false },
                onShowAll = { businessAttentionViewAllOpen = false },
            )
        }

        if (showManageMoment) {
            val momentId = state.selectedMomentId
            if (momentId != null) {
                val manageIsBusiness = state.selectedContext == AppContext.BUSINESS
                if (manageIsBusiness) {
                    BusinessGlassModalBottomSheet(
                        onDismissRequest = { showManageMoment = false },
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
                        onCompleted = {
                            val completedId = state.selectedMomentId
                            showManageMoment = false
                            storyMomentId = completedId
                            shellViewModel.reloadCurrentContext()
                        },
                        onLeft = {
                            showManageMoment = false
                            shellViewModel.clearSelectedMomentAfterLeave()
                        },
                        onDuplicated = { newId, title ->
                            showManageMoment = false
                            shellViewModel.onMomentCreated(momentId = newId, title = title)
                        },
                    )
                    }
                } else {
                    ModalBottomSheet(
                        onDismissRequest = { showManageMoment = false },
                        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                        containerColor = Color(0xFF1A1628),
                        scrimColor = Color.Black.copy(alpha = 0.32f),
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
                        onCompleted = {
                            val completedId = state.selectedMomentId
                            showManageMoment = false
                            storyMomentId = completedId
                            shellViewModel.reloadCurrentContext()
                        },
                        onLeft = {
                            showManageMoment = false
                            shellViewModel.clearSelectedMomentAfterLeave()
                        },
                        onDuplicated = { newId, title ->
                            showManageMoment = false
                            shellViewModel.onMomentCreated(momentId = newId, title = title)
                        },
                    )
                    }
                }
            }
        }
        if (personalSetupChooserOpen && state.selectedContext == AppContext.PERSONAL) {
            val missing = missingPersonalSetupSystems(state.moments)
            MomentraFullscreenDialog(onDismissRequest = { personalSetupChooserOpen = false }) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF14121B))
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        "Set up another life area",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                    )
                    Text(
                        "Add Future, Lifestyle, or People without leaving Personal.",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 14.sp,
                    )
                    missing.forEach { system ->
                        Text(
                            text = system.setupTitle,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF201E28))
                                .clickable {
                                    personalSetupChooserOpen = false
                                    personalSetupSystem = system
                                }
                                .padding(16.dp),
                        )
                    }
                    Text(
                        "Cancel",
                        color = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier
                            .clickable { personalSetupChooserOpen = false }
                            .padding(8.dp),
                    )
                }
            }
        }
        personalSetupSystem?.let { system ->
            MomentraFullscreenDialog(onDismissRequest = { personalSetupSystem = null }) {
                PersonalSetupWizardContent(
                    system = system,
                    onBack = { personalSetupSystem = null },
                    onCreated = { id, title, typeCode, status ->
                        personalSetupSystem = null
                        shellViewModel.onMomentCreated(id, title, typeCode, status)
                        tourController.onSignal(TourSignal.MOMENT_CREATED)
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        if (storyMomentId != null) {
            val momentId = storyMomentId
            if (momentId != null) {
                MomentraFullscreenDialog(onDismissRequest = { storyMomentId = null }) {
                    MomentStoryViewerScreen(
                        momentId = momentId,
                        onClose = { storyMomentId = null },
                    )
                }
            }
        }
        if (editSetupOpen) {
            val momentId = state.selectedMomentId
            if (momentId != null) {
                MomentraFullscreenDialog(onDismissRequest = { editSetupOpen = false }) {
                    EditMomentSetupHost(
                        context = state.selectedContext,
                        momentId = momentId,
                        momentTitle = state.selectedMomentTitle.orEmpty(),
                        momentTypeCode = state.selectedMomentTypeCode,
                        companyId = state.moments.firstOrNull { it.momentId == momentId }?.companyId,
                        editingMomentStatus = state.moments.firstOrNull { it.momentId == momentId }?.status,
                        onClose = { editSetupOpen = false },
                        onSaved = {
                            editSetupOpen = false
                            state.selectedMomentId?.let { GroupTabDataCache.invalidateMoment(it) }
                            shellViewModel.refreshVisibleGroupTab(forcePrefetch = true)
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
                groupMomentDirectoryOpen = false
                businessMomentDirectoryOpen = false
                if (it == BottomDestination.CREATE && state.selectedContext == AppContext.GROUP) {
                    groupCreatePhase = GroupCreatePhase.CHOOSER
                    preferGroupCreateFlow = false
                }
                // Bottom CREATE with a selected Moment = Quick Add, not guided create.
                if (it == BottomDestination.CREATE && state.selectedContext == AppContext.BUSINESS) {
                    preferBusinessCreateFlow = false
                }
                if (it != BottomDestination.CREATE) {
                    preferBusinessCreateFlow = false
                }
                shellViewModel.selectBottomDestination(it)
            },
            accent = momentAccent,
            createLabel = if (state.selectedContext == AppContext.PERSONAL) "Add" else BottomDestination.CREATE.label,
            // Personal / Group / Business share default bottom-nav tokens.
            unselected = ShellTokens.BottomUnselected,
            barBackground = ShellTokens.BottomBarBackground,
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
    if (inboxOpen) {
        ModalBottomSheet(
            onDismissRequest = { inboxOpen = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            NotificationInboxSheet(
                onOpenMoment = { momentId ->
                    if (!shellViewModel.openMomentFromDeepLink(momentId)) {
                        PendingDeepLink.offer("momentra://moment/$momentId")
                    }
                },
                onClose = { inboxOpen = false },
                onUnreadCountChanged = { unreadNotificationCount = it },
                repository = accountRepository,
            )
        }
    }
    if (state.profileOpen) {
        ModalBottomSheet(
            onDismissRequest = {
                shellViewModel.openProfile(false)
            },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            com.example.momentra.ui.account.AccountHubSheet(
                identity = state.identity ?: identity,
                onDisplayNameSaved = { name ->
                    shellViewModel.applyDisplayName(name)
                    onDisplayNameSaved(name)
                },
                onSignOut = {
                    shellViewModel.openProfile(false)
                    onSignOut()
                },
                onClose = {
                    shellViewModel.openProfile(false)
                },
                onAccountDeleted = {
                    shellViewModel.openProfile(false)
                    onSignOut()
                },
                onReplayTour = {
                    shellViewModel.openProfile(false)
                    tourController.startPersonal(force = true)
                },
            )
        }
    }
    habitRewardMessage?.let { message ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 88.dp),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Text(
                text = message,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = com.example.momentra.ui.theme.PlusJakartaSans,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF2D1F5E))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            )
        }
    }
    } // TourHost
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
    forcePersonalSimple: Boolean = false,
    personalPresentFamilies: Set<PersonalPulseFamily> = emptySet(),
    viewerReadOnly: Boolean = false,
    onRetry: () -> Unit,
    onSessionExpired: () -> Unit,
    onCreateMoment: () -> Unit,
    onOpenPersonalLife: () -> Unit = {},
    onOpenPersonalPulse: () -> Unit = {},
    onOpenPersonalMoments: () -> Unit = {},
    onCreateBack: () -> Unit = onCreateMoment,
    onCompanyActivated: (CompanySummary) -> Unit = {},
    onMomentCreated: (String, String, String?, String) -> Unit = { _, _, _, _ -> },
    onJoinGroupCode: (String) -> Unit = {},
    preferGroupCreateFlow: Boolean = false,
    onPreferGroupCreateFlow: (Boolean) -> Unit = {},
    preferBusinessCreateFlow: Boolean = false,
    onPreferBusinessCreateFlow: (Boolean) -> Unit = {},
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
    onAddChecklist: () -> Unit = {},
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
    onBusinessQuickAdd: () -> Unit = {},
    onOpenCompanySettings: () -> Unit = {},
    /** Moves the shell to another bottom destination (kit "Back to Pulse" CTAs). */
    onSelectDestination: (BottomDestination) -> Unit = {},
    onAddRevenue: () -> Unit = {},
    onAddInvoice: () -> Unit = {},
    onAddMembers: () -> Unit = {},
    onOpenQuickAdd: () -> Unit = {},
    onOpenMoments: () -> Unit = {},
    onViewBusinessReport: () -> Unit = {},
    onOpenBusinessFinance: () -> Unit = {},
    onOpenVendor: () -> Unit = {},
    onLifeOpsQuickAdd: (LifeOpsQuickAddKind) -> Unit = {},
    onMoneyQuickAdd: (MoneyQuickAddKind) -> Unit = {},
    onViewAllActivity: () -> Unit = {},
    onPersonalActivityChanged: () -> Unit = {},
    onViewAllGroupActivity: () -> Unit = {},
    onViewAllTeamActivity: () -> Unit = {},
    onViewAllBusinessActivity: () -> Unit = {},
    onViewAllBusinessAttention: () -> Unit = {},
    onSelectMoment: (String) -> Unit = {},
    onOpenCompletedMoments: (() -> Unit)? = null,
) {
    if (context == AppContext.BUSINESS) {
        when (content) {
            is ShellContentState.Error -> {
                if (content.code == "UNAUTHORIZED" || content.message.contains("UNAUTHENTICATED", ignoreCase = true)) {
                    LaunchedEffect(Unit) { onSessionExpired() }
                }
            }
            else -> Unit
        }
        // P4 completion: Create New Moment (preferBusinessCreateFlow / no moment) → AppState.
        // Ready + selected Moment + Pulse/Moments/Life/Memory/CREATE(Quick Add) → operating hub.
        val creatingNewMoment = preferBusinessCreateFlow && destination == BottomDestination.CREATE
        val useOperatingHub = content is ShellContentState.Ready &&
            hasCompany &&
            !selectedMomentId.isNullOrBlank() &&
            !creatingNewMoment
        if (!useOperatingHub) {
            BusinessAppStateHost(
                destination = destination,
                content = content,
                hasCompany = hasCompany,
                momentTypeCode = selectedMomentTypeCode,
                onRetry = onRetry,
                onPrimary = onCreateMoment,
                onSecondary = {
                    onPreferBusinessCreateFlow(false)
                    onSelectDestination(BottomDestination.CREATE)
                },
                onCreateBusiness = onCreateMoment,
                onJoinCompany = onOpenCompanySettings,
                onOpenQuickAdds = {
                    onPreferBusinessCreateFlow(false)
                    onSelectDestination(BottomDestination.CREATE)
                },
                onBackToPulse = {
                    onPreferBusinessCreateFlow(false)
                    onSelectDestination(BottomDestination.PULSE)
                },
                onCreateBack = {
                    onPreferBusinessCreateFlow(false)
                    onCreateBack()
                },
                companyId = companyId,
                onMomentCreated = { id, title, type, status ->
                    onPreferBusinessCreateFlow(false)
                    onMomentCreated(id, title, type, status)
                },
            )
            return
        }
        com.example.momentra.ui.shell.business.shared.OperatingMomentPage(
            momentId = selectedMomentId,
            momentTypeCode = selectedMomentTypeCode,
            momentTitle = selectedMomentTitle,
            tab = com.example.momentra.ui.shell.business.shared.operatingTabFor(destination),
            refreshToken = businessTabRefreshToken,
            onCreateMoment = onCreateMoment,
            onOpenQuickAdd = {
                onPreferBusinessCreateFlow(false)
                onSelectDestination(BottomDestination.CREATE)
            },
            onViewAllAttention = onViewAllBusinessAttention,
        )
        return
    }

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
                momentTypeCode = selectedMomentTypeCode,
                onCreateMoment = onCreateMoment,
                onCreateBack = onCreateBack,
                onCompanyActivated = onCompanyActivated,
                onMomentCreated = onMomentCreated,
                onJoinGroupCode = onJoinGroupCode,
                onCreateBusiness = { onSelectDestination(BottomDestination.CREATE) },
                onJoinCompany = { onOpenCompanySettings() },
                onSelectDestination = onSelectDestination,
                groupCreatePhase = groupCreatePhase,
                onGroupCreatePhase = onGroupCreatePhase,
                onOpenGroupCreateTab = onOpenGroupCreateTab,
                onOpenCompletedMoments = onOpenCompletedMoments,
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
                                onTile = { if (!viewerReadOnly) onWeddingQuickAdd(it) },
                                onCreateMoment = {
                                    onPreferGroupCreateFlow(true)
                                    onGroupCreatePhase(GroupCreatePhase.CHOOSER)
                                },
                                onJoinCode = onJoinGroupCode,
                                capabilities = capabilities,
                                viewerReadOnly = viewerReadOnly,
                            )
                        } else if (isExperience) {
                            ExperienceQuickAddHub(
                                theme = experienceTheme,
                                momentTitle = selectedMomentTitle,
                                hasActiveMoment = true,
                                onClose = onCreateBack,
                                onTile = { if (!viewerReadOnly) onExperienceQuickAdd(it) },
                                onCreateMoment = {
                                    onPreferGroupCreateFlow(true)
                                    onGroupCreatePhase(GroupCreatePhase.CHOOSER)
                                },
                                onJoinCode = onJoinGroupCode,
                                capabilities = capabilities,
                                viewerReadOnly = viewerReadOnly,
                            )
                        } else if (isPurchase) {
                            PurchaseQuickAddHub(
                                theme = purchaseTheme,
                                momentTitle = selectedMomentTitle,
                                hasActiveMoment = true,
                                onClose = onCreateBack,
                                onTile = { if (!viewerReadOnly) onPurchaseQuickAdd(it) },
                                onCreateMoment = {
                                    onPreferGroupCreateFlow(true)
                                    onGroupCreatePhase(GroupCreatePhase.CHOOSER)
                                },
                                viewerReadOnly = viewerReadOnly,
                            )
                        } else if (isLiving) {
                            LivingQuickAddHub(
                                theme = livingTheme,
                                momentTitle = selectedMomentTitle,
                                hasActiveMoment = true,
                                onClose = onCreateBack,
                                onTile = { if (!viewerReadOnly) onLivingQuickAdd(it) },
                                onCreateMoment = {
                                    onPreferGroupCreateFlow(true)
                                    onGroupCreatePhase(GroupCreatePhase.CHOOSER)
                                },
                                viewerReadOnly = viewerReadOnly,
                            )
                        } else {
                            GroupQuickAddHub(
                                hasActiveMoment = true,
                                onClose = onCreateBack,
                                onExpense = { if (!viewerReadOnly) onAddExpense() },
                                onContribution = { if (!viewerReadOnly) onAddContribution() },
                                onSettle = { if (!viewerReadOnly) onAddSettlement() },
                                onParticipants = onAddParticipants,
                                onInvite = { if (!viewerReadOnly) onAddInvite() },
                                onBudget = { if (!viewerReadOnly) onAddBudget() },
                                onPlanning = { if (!viewerReadOnly) onAddPlanning() },
                                onChecklist = { if (!viewerReadOnly) onAddChecklist() },
                                onBooking = { if (!viewerReadOnly) onAddBooking() },
                                onPoll = { if (!viewerReadOnly) onAddPoll() },
                                onUpdate = { if (!viewerReadOnly) onAddUpdate() },
                                onMemory = { if (!viewerReadOnly) onAddMemory() },
                                onPurchaseItem = { if (!viewerReadOnly) onAddPurchaseItem() },
                                onResident = { if (!viewerReadOnly) onAddResident() },
                                onCreateMoment = {
                                    onPreferGroupCreateFlow(true)
                                    onGroupCreatePhase(GroupCreatePhase.CHOOSER)
                                },
                                onJoinCode = onJoinGroupCode,
                                momentTitle = selectedMomentTitle,
                                momentTypeCode = moments.firstOrNull { it.momentId == selectedMomentId }?.momentTypeCode,
                                capabilities = capabilities,
                                viewerReadOnly = viewerReadOnly,
                            )
                        }
                    }
                }
                else -> {
                    val personalTypeCode = selectedMomentTypeCode
                        ?: moments.firstOrNull { it.momentId == selectedMomentId }?.momentTypeCode
                    val personalFamily = personalPulseFamilyFor(personalTypeCode)
                    val isLifeOps = personalFamily == PersonalPulseFamily.LIFE_OPERATIONS
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
                                    onViewAllActivity = onViewAllGroupActivity,
                                )
                            } else if (isExperience) {
                                ExperiencePulseActiveContent(
                                    theme = experienceTheme,
                                    momentId = selectedMomentId,
                                    momentTitle = selectedMomentTitle,
                                    refreshToken = groupTabRefreshToken,
                                    momentTypeCode = groupTypeCode,
                                    viewerReadOnly = viewerReadOnly,
                                    onAddExpense = { if (!viewerReadOnly) onAddExpense() },
                                    onOpenQuickAdd = onOpenQuickAdd,
                                    onViewSplits = onViewSplits,
                                    onOpenFinance = onOpenGroupFinance,
                                    onQuickAddKind = { if (!viewerReadOnly) onExperienceQuickAdd(it) },
                                    onViewAllActivity = onViewAllGroupActivity,
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
                                    onViewAllActivity = onViewAllGroupActivity,
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
                                    onViewAllActivity = onViewAllGroupActivity,
                                )
                            } else {
                                GroupPulseActiveContent(
                                    momentId = selectedMomentId,
                                    momentTitle = selectedMomentTitle,
                                    refreshToken = groupTabRefreshToken,
                                    momentTypeCode = groupTypeCode,
                                    onAddExpense = onAddExpense,
                                    onViewSplits = onViewSplits,
                                    onOpenFinance = onOpenGroupFinance,
                                    onOpenMemory = onAddMemory,
                                    onOpenChat = onAddUpdate,
                                    onOpenItinerary = onAddPlanning,
                                    onViewAllActivity = onViewAllGroupActivity,
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
                                        GroupLifeQuickAction.EXPERIENCE -> {
                                            when {
                                                isWedding -> onWeddingQuickAdd(WeddingQuickAddKind.PLANNING)
                                                isExperience -> onExperienceQuickAdd(ExperienceQuickAddKind.PLANNING)
                                                isPurchase -> onPurchaseQuickAdd(PurchaseQuickAddKind.PURCHASE_ITEM)
                                                isLiving -> onLivingQuickAdd(LivingQuickAddKind.TASK)
                                                else -> onAddPlanning()
                                            }
                                        }
                                        GroupLifeQuickAction.GOAL,
                                        GroupLifeQuickAction.COMMUNITY,
                                        -> {
                                            // Goal / Community moments are Coming Soon — do not open unrelated sheets.
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
                        context == AppContext.PERSONAL && (
                            destination == BottomDestination.LIFE ||
                                destination == BottomDestination.PULSE ||
                                destination == BottomDestination.MOMENTS ||
                                destination == BottomDestination.MEMORY ||
                                destination == BottomDestination.CREATE
                            ) -> {
                            com.example.momentra.ui.shell.personal.experience.PersonalExperienceHost(
                                destination = destination,
                                hasActiveMoment = selectedMomentId != null,
                                momentTypeCode = personalTypeCode,
                                capabilityCodes = capabilities,
                                onOpenLife = onOpenPersonalLife,
                                onOpenPulse = onOpenPersonalPulse,
                                onOpenMoments = onOpenPersonalMoments,
                                onLifeOpsQuickAdd = onLifeOpsQuickAdd,
                                onMoneyQuickAdd = onMoneyQuickAdd,
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

/** Slim Personal chrome — Manage preferred + optional Set up Everyday (no family chips). */
@Composable
private fun PersonalUnifiedChrome(
    accent: Color,
    showManage: Boolean,
    showSetUpEveryday: Boolean,
    onManage: () -> Unit,
    onSetUpEveryday: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ShellTokens.TopBarBackground)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Personal",
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
            )
            if (showSetUpEveryday) {
                Text(
                    "Set up Everyday",
                    color = accent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable(onClick = onSetUpEveryday),
                )
            }
        }
        if (showManage) {
            Icon(
                imageVector = Icons.Outlined.Settings,
                contentDescription = "Manage Personal",
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier
                    .size(22.dp)
                    .clickable(onClick = onManage),
            )
        }
    }
}

/** Slim chrome when top bar + context tabs are collapsed. */
@Composable
private fun CompactShellChrome(
    showNewMoment: Boolean,
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
            if (showNewMoment) {
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
