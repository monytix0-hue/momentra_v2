package com.example.momentra.ui.shell

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.momentra.data.api.ApiResultException
import com.example.momentra.data.api.RedeemGroupInviteResultDto
import com.example.momentra.data.local.AppPreferences
import com.example.momentra.data.repository.BusinessSliceRepository
import com.example.momentra.data.repository.GroupSliceRepository
import com.example.momentra.data.repository.MeGateway
import com.example.momentra.domain.AppContext
import com.example.momentra.domain.BottomDestination
import com.example.momentra.domain.CompanySummary
import com.example.momentra.domain.MomentExperienceKind
import com.example.momentra.domain.MomentSummary
import com.example.momentra.domain.ShellBootstrap
import com.example.momentra.domain.ShellContentState
import com.example.momentra.domain.ShellIdentity
import com.example.momentra.domain.activeMomentCount
import com.example.momentra.domain.isActiveStatus
import com.example.momentra.domain.resolveMomentExperience
import com.example.momentra.ui.shell.policy.ShellInvariantInput
import com.example.momentra.ui.shell.policy.ShellStateInvariants
import com.example.momentra.ui.shell.policy.ShellVisibilityPolicy
import com.example.momentra.ui.shell.perf.ShellPerf
import com.example.momentra.ui.shell.personal.shared.PersonalTabDataCache
import com.example.momentra.ui.shell.personal.shared.loadPersonalPulseTab
import com.example.momentra.ui.shell.business.shared.BusinessTabDataCache
import com.example.momentra.ui.shell.business.shared.prefetchBusinessTabs
import com.example.momentra.ui.shell.group.shared.GroupTabDataCache
import com.example.momentra.ui.shell.group.shared.prefetchGroupTabs
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AppShellUiState(
    val identity: ShellIdentity? = null,
    val bootstrapStatus: BootstrapStatus = BootstrapStatus.IDLE,
    val supportedContexts: List<AppContext> = listOf(AppContext.PERSONAL),
    val selectedContext: AppContext = AppContext.PERSONAL,
    val selectedCompany: CompanySummary? = null,
    val companies: List<CompanySummary> = emptyList(),
    val selectedMomentId: String? = null,
    val selectedMomentTitle: String? = null,
    val selectedMomentTypeCode: String? = null,
    val selectedMomentByContext: Map<AppContext, String?> = emptyMap(),
    val showMomentSwitcher: Boolean = false,
    val showCompanySwitcher: Boolean = false,
    val bottomDestination: BottomDestination = BottomDestination.PULSE,
    val lastNonCreateDestination: BottomDestination = BottomDestination.MOMENTS,
    /** Per-context tab memory — no global selectedTab. */
    val tabByContext: Map<AppContext, BottomDestination> = emptyMap(),
    val contextContent: ShellContentState = ShellContentState.Idle,
    val momentExperience: MomentExperienceKind = MomentExperienceKind.LOADING,
    val moments: List<MomentSummary> = emptyList(),
    val capabilities: List<String> = emptyList(),
    val companyMenuOpen: Boolean = false,
    val life360Open: Boolean = false,
    val profileOpen: Boolean = false,
    val generation: Long = 0L,
    val personalTabRefreshToken: Long = 0L,
    val groupTabRefreshToken: Long = 0L,
    val businessTabRefreshToken: Long = 0L,
    /** Elapsed ms from bindIdentity to first cached shell paint; null if no cache. */
    val ttcsMs: Long? = null,
)

enum class BootstrapStatus { IDLE, CACHED, REFRESHING, READY, ERROR }

/**
 * Authenticated shell controller.
 * Inventory from GET /v1/me bootstrap (SWR); tab datasets load separately.
 */
class AppShellViewModel(
    private val meRepository: MeGateway,
    private val prefs: AppPreferences? = null,
    private val groupRepository: GroupSliceRepository = GroupSliceRepository(),
    private val businessRepository: BusinessSliceRepository = BusinessSliceRepository(),
) : ViewModel() {

    private val _state = MutableStateFlow(AppShellUiState())
    val state: StateFlow<AppShellUiState> = _state.asStateFlow()

    private var loadJob: Job? = null
    private var bootstrapRefreshJob: Job? = null
    private var groupPrefetchJob: Job? = null
    private var businessPrefetchJob: Job? = null
    private var preferredPersonalMomentId: String? = null
    private var bootstrap: ShellBootstrap? = null
    private var bindStartedAtMs: Long = 0L

    fun bindIdentity(identity: ShellIdentity) {
        bindStartedAtMs = System.currentTimeMillis()
        _state.update { it.copy(identity = identity, bootstrapStatus = BootstrapStatus.REFRESHING) }
        meRepository.cachedBootstrap(identity.userId)?.let { cached ->
            bootstrap = cached
            applyBootstrapInventory(cached, networkRefresh = false)
            _state.update {
                it.copy(
                    bootstrapStatus = BootstrapStatus.CACHED,
                    ttcsMs = System.currentTimeMillis() - bindStartedAtMs,
                )
            }
            ShellPerf.instant(
                "ttcs_cache_paint",
                mapOf("ttcsMs" to (System.currentTimeMillis() - bindStartedAtMs)),
            )
        }
        restorePersistedSelections(identity.userId)
        if (bootstrap != null && meRepository.isBootstrapCacheFresh(identity.userId)) {
            ensureContextContent()
            _state.update { it.copy(bootstrapStatus = BootstrapStatus.READY) }
            scheduleDeferredBootstrapRefresh()
        } else {
            refreshBootstrap()
        }
    }

    /** SWR: avoid hammering /me right after AuthViewModel bootstrap wrote a fresh cache. */
    private fun scheduleDeferredBootstrapRefresh(delayMs: Long = 15_000L) {
        bootstrapRefreshJob?.cancel()
        bootstrapRefreshJob = viewModelScope.launch {
            delay(delayMs)
            refreshBootstrap()
        }
    }

    fun clearForLogout() {
        loadJob?.cancel()
        bootstrapRefreshJob?.cancel()
        groupPrefetchJob?.cancel()
        businessPrefetchJob?.cancel()
        preferredPersonalMomentId = null
        PersonalTabDataCache.clear()
        GroupTabDataCache.clear()
        BusinessTabDataCache.clear()
        _state.value.identity?.userId?.let { meRepository.clearBootstrapCache(it) }
        bootstrap = null
        _state.value = AppShellUiState()
    }

    fun restorePreferredPersonalMomentId(momentId: String?) {
        preferredPersonalMomentId = momentId
    }

    fun selectContext(context: AppContext) {
        val mark = ShellPerf.start("context_switch")
        val supported = _state.value.supportedContexts
        // Until bootstrap arrives, allow optimistic selection; heal enforces after merge.
        if (bootstrap != null && context !in supported) return
        val previous = _state.value.selectedContext
        if (previous == context) {
            when (context) {
                AppContext.PERSONAL -> refreshVisiblePersonalTab()
                AppContext.GROUP -> refreshVisibleGroupTab()
                AppContext.BUSINESS -> refreshVisibleBusinessTab()
                else -> Unit
            }
            ShellPerf.end(mark, mapOf("sameContext" to true, "context" to context.name))
            return
        }
        val preservedTab = _state.value.tabByContext[context] ?: BottomDestination.PULSE
        val rememberedMoment = _state.value.selectedMomentByContext[context]
        _state.update {
            it.copy(
                selectedContext = context,
                bottomDestination = preservedTab,
                tabByContext = it.tabByContext + (previous to it.bottomDestination),
                selectedMomentByContext = it.selectedMomentByContext + (previous to it.selectedMomentId),
                selectedMomentId = rememberedMoment,
                selectedMomentTitle = null,
                selectedMomentTypeCode = null,
                showMomentSwitcher = false,
                companyMenuOpen = false,
                selectedCompany = if (context == AppContext.BUSINESS) it.selectedCompany else null,
                moments = emptyList(),
                momentExperience = MomentExperienceKind.LOADING,
                generation = it.generation + 1,
                contextContent = ShellContentState.Loading,
            )
        }
        persistContext(context)
        ensureContextContent()
        ShellPerf.end(mark, mapOf("from" to previous.name, "to" to context.name))
    }

    fun reloadCurrentContext() {
        refreshBootstrap()
    }

    /** After leaving a Group/Business moment or company, drop selection and reload inventory. */
    fun clearSelectedMomentAfterLeave() {
        _state.update {
            it.copy(
                selectedMomentId = null,
                selectedMomentTitle = null,
                selectedMomentTypeCode = null,
                selectedMomentByContext = it.selectedMomentByContext + (it.selectedContext to null),
                momentExperience = MomentExperienceKind.FIRST_MOMENT,
                contextContent = ShellContentState.Loading,
            )
        }
        refreshBootstrap()
    }

    private fun refreshBootstrap() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val hadCache = bootstrap != null
            if (!hadCache) {
                _state.update {
                    it.copy(
                        contextContent = ShellContentState.Loading,
                        momentExperience = MomentExperienceKind.LOADING,
                        bootstrapStatus = BootstrapStatus.REFRESHING,
                    )
                }
            } else {
                _state.update { it.copy(bootstrapStatus = BootstrapStatus.REFRESHING) }
            }
            meRepository.getBootstrap().fold(
                onSuccess = { boot ->
                    bootstrap = boot
                    applyBootstrapInventory(boot, networkRefresh = true)
                    _state.update { it.copy(bootstrapStatus = BootstrapStatus.READY) }
                },
                onFailure = { e ->
                    if (bootstrap != null) {
                        ensureContextContent()
                        _state.update { it.copy(bootstrapStatus = BootstrapStatus.CACHED) }
                    } else {
                        applyError(_state.value.generation, e)
                        _state.update { it.copy(bootstrapStatus = BootstrapStatus.ERROR) }
                    }
                },
            )
        }
    }

    private fun applyBootstrapInventory(
        boot: ShellBootstrap,
        networkRefresh: Boolean,
        preserveMomentId: String? = null,
    ) {
        val current = _state.value
        val rawMoments = when (current.selectedContext) {
            AppContext.PERSONAL -> boot.personalMoments
            AppContext.GROUP -> boot.groupMoments
            AppContext.BUSINESS -> boot.businessMoments
            AppContext.CIRCLE -> emptyList()
        }
        val preferredMomentId = preserveMomentId
            ?: current.selectedMomentId
            ?: current.selectedMomentByContext[current.selectedContext]
            ?: if (current.selectedContext == AppContext.PERSONAL) preferredPersonalMomentId else null
        // Keep an optimistic join/create selection visible until inventory catches up.
        val momentsForHeal = if (
            !preserveMomentId.isNullOrBlank() &&
            rawMoments.none { it.momentId == preserveMomentId }
        ) {
            rawMoments + MomentSummary(
                momentId = preserveMomentId,
                title = current.selectedMomentTitle?.takeIf { it.isNotBlank() } ?: "Group",
                status = "ACTIVE",
                momentTypeCode = current.selectedMomentTypeCode,
            )
        } else {
            rawMoments
        }
        val healed = ShellStateInvariants.heal(
            ShellInvariantInput(
                supportedContexts = boot.supportedContexts.ifEmpty {
                    listOf(AppContext.PERSONAL, AppContext.GROUP, AppContext.BUSINESS, AppContext.CIRCLE)
                },
                selectedContext = current.selectedContext,
                selectedCompanyId = current.selectedCompany?.companyId ?: boot.selectedCompany?.companyId,
                companies = boot.companies,
                moments = momentsForHeal,
                selectedMomentId = preferredMomentId,
                selectedTabByContext = current.tabByContext,
                currentlySelectedContextDefault = boot.currentlySelectedContext,
            ),
        )
        val company = boot.companies.firstOrNull { it.companyId == healed.selectedCompanyId }
        val previousById = current.moments.associateBy { it.momentId }
        // Preserve known type codes when bootstrap omits them (legacy group inventory).
        val mergedMoments = healed.moments.map { m ->
            if (!m.momentTypeCode.isNullOrBlank()) m
            else previousById[m.momentId]?.momentTypeCode?.let { code -> m.copy(momentTypeCode = code) } ?: m
        }
        val selectedMoment = mergedMoments.firstOrNull { it.momentId == healed.selectedMomentId }
        val experience = when (healed.selectedContext) {
            AppContext.CIRCLE -> MomentExperienceKind.FIRST_MOMENT
            else -> resolveMomentExperience(mergedMoments)
        }
        // S6: Circle Coming Soon — Empty (not Deferred); no Circle API fetch.
        val content = when {
            healed.selectedContext == AppContext.CIRCLE -> ShellContentState.Empty
            experience == MomentExperienceKind.ACTIVE -> ShellContentState.Ready(null)
            experience == MomentExperienceKind.LOADING || experience == MomentExperienceKind.ERROR ->
                ShellContentState.Loading
            else -> ShellContentState.Empty
        }
        val tab = healed.selectedTabByContext[healed.selectedContext] ?: current.bottomDestination
        _state.update {
            it.copy(
                identity = boot.identity,
                companies = boot.companies,
                capabilities = boot.capabilities,
                supportedContexts = healed.selectedContext.let { ctx ->
                    // preserve full supported list
                    boot.supportedContexts.ifEmpty {
                        listOf(AppContext.PERSONAL, AppContext.GROUP, AppContext.BUSINESS, AppContext.CIRCLE)
                    }
                }.let { list -> list.ifEmpty { listOf(AppContext.PERSONAL) } },
                selectedContext = healed.selectedContext,
                selectedCompany = company,
                moments = mergedMoments,
                selectedMomentId = healed.selectedMomentId,
                selectedMomentTitle = selectedMoment?.title,
                selectedMomentTypeCode = selectedMoment?.momentTypeCode
                    ?: current.selectedMomentTypeCode?.takeIf { code ->
                        !code.isNullOrBlank() && current.selectedMomentId == healed.selectedMomentId
                    },
                selectedMomentByContext = it.selectedMomentByContext +
                    (healed.selectedContext to healed.selectedMomentId),
                tabByContext = healed.selectedTabByContext,
                bottomDestination = tab,
                momentExperience = experience,
                contextContent = content,
                showMomentSwitcher = ShellVisibilityPolicy.showMomentSwitcher(
                    context = healed.selectedContext,
                    content = content,
                    destination = tab,
                    activeMomentCount = activeMomentCount(mergedMoments),
                    authReady = true,
                ),
                showCompanySwitcher = ShellVisibilityPolicy.showCompanySwitcher(
                    healed.selectedContext,
                    boot.companies,
                ),
            )
        }
        persistCompany(company?.companyId)
        if (healed.selectedContext == AppContext.GROUP &&
            content is ShellContentState.Ready &&
            !healed.selectedMomentId.isNullOrBlank()
        ) {
            prefetchGroupTabsFor(healed.selectedMomentId)
        }
        if (healed.selectedContext == AppContext.BUSINESS &&
            content is ShellContentState.Ready &&
            !healed.selectedMomentId.isNullOrBlank()
        ) {
            prefetchBusinessTabsFor(healed.selectedMomentId)
        }
        if (networkRefresh &&
            _state.value.momentExperience == MomentExperienceKind.ACTIVE
        ) {
            val previous = current
            val inventoryChanged = previous.moments.map { it.momentId } != healed.moments.map { it.momentId }
            val selectionChanged = previous.selectedMomentId != selectedMoment?.momentId
            if (inventoryChanged || selectionChanged) {
                when (_state.value.selectedContext) {
                    AppContext.PERSONAL -> refreshVisiblePersonalTab()
                    AppContext.GROUP -> refreshVisibleGroupTab()
                    AppContext.BUSINESS -> refreshVisibleBusinessTab()
                    else -> Unit
                }
            }
        }
    }

    fun selectBottomDestination(destination: BottomDestination) {
        val mark = ShellPerf.start("tab_switch")
        _state.update {
            val remembered = when {
                destination != BottomDestination.CREATE -> destination
                it.bottomDestination != BottomDestination.CREATE -> it.bottomDestination
                else -> it.lastNonCreateDestination
            }.let { d -> if (d == BottomDestination.CREATE) BottomDestination.MOMENTS else d }
            val tabMap = it.tabByContext + (it.selectedContext to destination)
            val content = it.contextContent
            it.copy(
                bottomDestination = destination,
                lastNonCreateDestination = remembered,
                tabByContext = tabMap,
                showMomentSwitcher = ShellVisibilityPolicy.showMomentSwitcher(
                    context = it.selectedContext,
                    content = content,
                    destination = destination,
                    activeMomentCount = activeMomentCount(it.moments),
                    authReady = true,
                ),
            )
        }
        if (destination == BottomDestination.CREATE) {
            ShellPerf.instant("quick_add_presentation", mapOf("context" to _state.value.selectedContext.name))
        }
        ShellPerf.end(mark, mapOf("destination" to destination.name))
    }

    fun selectMoment(momentId: String) {
        val mark = ShellPerf.start("moment_switch")
        val moment = _state.value.moments.firstOrNull { it.momentId == momentId } ?: return
        _state.update {
            it.copy(
                selectedMomentId = moment.momentId,
                selectedMomentTitle = moment.title,
                selectedMomentTypeCode = moment.momentTypeCode,
                selectedMomentByContext = it.selectedMomentByContext + (it.selectedContext to moment.momentId),
            )
        }
        when (_state.value.selectedContext) {
            AppContext.PERSONAL -> {
                preferredPersonalMomentId = momentId
                refreshVisiblePersonalTab()
            }
            AppContext.GROUP -> refreshVisibleGroupTab()
            AppContext.BUSINESS -> refreshVisibleBusinessTab()
            else -> Unit
        }
        ShellPerf.end(mark, mapOf("momentId" to momentId.take(8)))
    }

    fun onMomentCreated(momentId: String, title: String, momentTypeCode: String? = null) {
        val ctx = _state.value.selectedContext
        if (ctx == AppContext.PERSONAL) {
            preferredPersonalMomentId = momentId
        }
        _state.update {
            val existingIdx = it.moments.indexOfFirst { m -> m.momentId == momentId }
            val updatedMoments = if (existingIdx >= 0) {
                it.moments.toMutableList().apply {
                    val current = this[existingIdx]
                    this[existingIdx] = current.copy(
                        title = title,
                        momentTypeCode = momentTypeCode ?: current.momentTypeCode,
                    )
                }
            } else {
                it.moments + MomentSummary(
                    momentId = momentId,
                    title = title,
                    status = "ACTIVE",
                    momentTypeCode = momentTypeCode,
                )
            }
            it.copy(
                selectedMomentId = momentId,
                selectedMomentTitle = title,
                selectedMomentTypeCode = momentTypeCode ?: it.selectedMomentTypeCode,
                bottomDestination = BottomDestination.PULSE,
                lastNonCreateDestination = BottomDestination.PULSE,
                tabByContext = it.tabByContext + (ctx to BottomDestination.PULSE),
                selectedMomentByContext = it.selectedMomentByContext + (ctx to momentId),
                moments = updatedMoments,
            )
        }
        reloadCurrentContext()
        when (ctx) {
            AppContext.GROUP -> refreshVisibleGroupTab()
            AppContext.BUSINESS -> refreshVisibleBusinessTab()
            else -> Unit
        }
    }

    fun redeemGroupInvite(
        code: String,
        onResult: (Result<RedeemGroupInviteResultDto>) -> Unit = {},
    ) {
        viewModelScope.launch {
            val result = groupRepository.redeemGroupInvite(code.trim())
            result.onSuccess { dto ->
                val momentId = dto.momentId
                if (!momentId.isNullOrBlank()) {
                    _state.update {
                        val hasMoment = it.moments.any { m -> m.momentId == momentId }
                        it.copy(
                            selectedContext = AppContext.GROUP,
                            selectedMomentId = momentId,
                            selectedMomentTitle = it.selectedMomentTitle?.takeIf { t -> hasMoment }
                                ?: it.moments.firstOrNull { m -> m.momentId == momentId }?.title
                                ?: "Group",
                            bottomDestination = BottomDestination.PULSE,
                            lastNonCreateDestination = BottomDestination.PULSE,
                            tabByContext = it.tabByContext + (AppContext.GROUP to BottomDestination.PULSE),
                            selectedMomentByContext = it.selectedMomentByContext + (AppContext.GROUP to momentId),
                            moments = if (hasMoment) {
                                it.moments
                            } else {
                                it.moments + MomentSummary(
                                    momentId = momentId,
                                    title = "Group",
                                    status = "ACTIVE",
                                )
                            },
                            momentExperience = MomentExperienceKind.ACTIVE,
                            contextContent = ShellContentState.Ready(null),
                        )
                    }
                    // Retry inventory until the joined moment appears (heal no longer clears it).
                    var appeared = false
                    repeat(4) { attempt ->
                        meRepository.getBootstrap().onSuccess { boot ->
                            bootstrap = boot
                            if (boot.groupMoments.any { it.momentId == momentId }) {
                                appeared = true
                            }
                            applyBootstrapInventory(
                                boot,
                                networkRefresh = true,
                                preserveMomentId = momentId,
                            )
                            _state.update { it.copy(bootstrapStatus = BootstrapStatus.READY) }
                        }
                        if (appeared) return@repeat
                        delay(350L * (attempt + 1))
                    }
                    refreshVisibleGroupTab()
                }
                // PENDING claim (null momentId): stay put; UI shows honest messaging.
            }
            onResult(result)
        }
    }

    fun redeemCompanyInvite(
        code: String,
        onResult: (Result<com.example.momentra.data.api.RedeemCompanyInviteResultDto>) -> Unit = {},
    ) {
        viewModelScope.launch {
            val result = groupRepository.redeemCompanyInvite(code.trim())
            result.onSuccess { dto ->
                val companies = meRepository.listCompanies().getOrElse { emptyList() }
                val matched = companies.firstOrNull { it.companyId == dto.companyId }
                    ?: CompanySummary(companyId = dto.companyId, displayName = "Company")
                _state.update {
                    it.copy(
                        selectedContext = AppContext.BUSINESS,
                        companies = if (companies.isEmpty()) listOf(matched) else companies,
                    )
                }
                onCompanyCreated(matched)
            }
            onResult(result)
        }
    }

    fun refreshVisiblePersonalTab() {
        _state.update { it.copy(personalTabRefreshToken = it.personalTabRefreshToken + 1) }
        ShellPerf.instant("scoped_refresh_personal", mapOf("token" to _state.value.personalTabRefreshToken))
    }

    fun refreshVisibleGroupTab() {
        val momentId = _state.value.selectedMomentId
        prefetchGroupTabsFor(momentId)
        _state.update { it.copy(groupTabRefreshToken = it.groupTabRefreshToken + 1) }
    }

    /** Warm pulse+finance+activity cache so Moments/Memory/Life paint without spinners. */
    private fun prefetchGroupTabsFor(momentId: String?) {
        if (momentId.isNullOrBlank()) return
        groupPrefetchJob?.cancel()
        groupPrefetchJob = viewModelScope.launch {
            prefetchGroupTabs(groupRepository, momentId)
        }
    }

    fun refreshVisibleBusinessTab() {
        val momentId = _state.value.selectedMomentId
        prefetchBusinessTabsFor(momentId)
        _state.update { it.copy(businessTabRefreshToken = it.businessTabRefreshToken + 1) }
    }

    /** Warm bundled pulse cache so Business tabs paint without spinners. */
    private fun prefetchBusinessTabsFor(momentId: String?) {
        if (momentId.isNullOrBlank()) return
        businessPrefetchJob?.cancel()
        businessPrefetchJob = viewModelScope.launch {
            prefetchBusinessTabs(businessRepository, momentId)
        }
    }

    fun exitCreateDestination() {
        selectBottomDestination(_state.value.lastNonCreateDestination)
    }

    fun toggleCompanyMenu(open: Boolean? = null) {
        _state.update { it.copy(companyMenuOpen = open ?: !it.companyMenuOpen) }
    }

    fun openLife360(open: Boolean = true) {
        _state.update { it.copy(life360Open = open, profileOpen = if (open) false else it.profileOpen) }
    }

    fun openProfile(open: Boolean = true) {
        _state.update { it.copy(profileOpen = open, life360Open = if (open) false else it.life360Open) }
    }

    fun selectCompany(company: CompanySummary?) {
        val mark = ShellPerf.start("company_switch")
        // Company before Moment — never infer company from moment; never keep prior company's momentId.
        val boot = bootstrap
        val scoped = if (company == null) {
            emptyList()
        } else {
            boot?.businessMoments.orEmpty().filter { m ->
                m.companyId == null || m.companyId == company.companyId
            }
        }
        val nextMoment = scoped.firstOrNull { it.isActiveStatus() } ?: scoped.firstOrNull()
        val experience = when {
            company == null -> MomentExperienceKind.FIRST_MOMENT
            else -> resolveMomentExperience(scoped)
        }
        val content = when {
            company == null -> ShellContentState.Empty
            experience == MomentExperienceKind.ACTIVE -> ShellContentState.Ready(null)
            experience == MomentExperienceKind.LOADING || experience == MomentExperienceKind.ERROR ->
                ShellContentState.Loading
            else -> ShellContentState.Empty
        }
        _state.update {
            val tab = it.tabByContext[AppContext.BUSINESS] ?: it.bottomDestination
            it.copy(
                selectedCompany = company,
                selectedMomentId = nextMoment?.momentId,
                selectedMomentTitle = nextMoment?.title,
                selectedMomentTypeCode = nextMoment?.momentTypeCode,
                selectedMomentByContext = it.selectedMomentByContext +
                    (AppContext.BUSINESS to nextMoment?.momentId),
                showMomentSwitcher = ShellVisibilityPolicy.showMomentSwitcher(
                    context = AppContext.BUSINESS,
                    content = content,
                    destination = tab,
                    activeMomentCount = activeMomentCount(scoped),
                    authReady = true,
                ),
                companyMenuOpen = false,
                moments = scoped,
                momentExperience = experience,
                generation = it.generation + 1,
                contextContent = content,
                businessTabRefreshToken = it.businessTabRefreshToken + 1,
            )
        }
        persistCompany(company?.companyId)
        ShellPerf.end(mark, mapOf("companyId" to (company?.companyId?.take(8) ?: "none")))
    }

    fun onCompanyCreated(company: CompanySummary) {
        _state.update {
            val merged = listOf(company) + it.companies.filter { c -> c.companyId != company.companyId }
            it.copy(
                companies = merged,
                selectedCompany = company,
                bottomDestination = BottomDestination.CREATE,
                contextContent = ShellContentState.Empty,
                momentExperience = MomentExperienceKind.FIRST_MOMENT,
                companyMenuOpen = false,
                showCompanySwitcher = true,
            )
        }
        persistCompany(company.companyId)
        viewModelScope.launch { meRepository.getBootstrap().onSuccess { bootstrap = it } }
    }

    private fun ensureContextContent() {
        val boot = bootstrap ?: run {
            _state.update {
                it.copy(contextContent = ShellContentState.Loading, momentExperience = MomentExperienceKind.LOADING)
            }
            return
        }
        applyBootstrapInventory(boot, networkRefresh = false)
    }

    private fun applyError(generation: Long, e: Throwable) {
        if (_state.value.generation != generation) return
        val content = when (e) {
            is ApiResultException.Network -> ShellContentState.Offline
            is ApiResultException.Forbidden -> ShellContentState.Forbidden
            is ApiResultException.Unauthenticated -> ShellContentState.Error("UNAUTHORIZED", e.message ?: "Unauthorized")
            is ApiResultException.Conflict -> ShellContentState.Error(e.code, e.message ?: "Conflict")
            is ApiResultException.Validation -> ShellContentState.Error(e.code, e.message ?: "Validation")
            is ApiResultException -> ShellContentState.Error(null, e.message ?: "Error")
            else -> ShellContentState.Error(null, e.message ?: "Error")
        }
        _state.update {
            it.copy(
                contextContent = content,
                momentExperience = MomentExperienceKind.ERROR,
                showMomentSwitcher = false,
                moments = emptyList(),
            )
        }
    }

    private fun restorePersistedSelections(userId: String) {
        val p = prefs ?: return
        p.getShellContext(userId)?.let { raw ->
            val ctx = runCatching { AppContext.valueOf(raw) }.getOrNull() ?: return@let
            if (ctx in _state.value.supportedContexts || bootstrap == null) {
                _state.update { it.copy(selectedContext = ctx) }
            }
        }
        p.getShellCompanyId(userId)?.let { cid ->
            _state.update { st ->
                st.copy(selectedCompany = st.companies.firstOrNull { it.companyId == cid } ?: st.selectedCompany)
            }
        }
    }

    private fun persistContext(context: AppContext) {
        val uid = _state.value.identity?.userId ?: return
        prefs?.setShellContext(uid, context.name)
    }

    private fun persistCompany(companyId: String?) {
        val uid = _state.value.identity?.userId ?: return
        prefs?.setShellCompanyId(uid, companyId)
    }
}
