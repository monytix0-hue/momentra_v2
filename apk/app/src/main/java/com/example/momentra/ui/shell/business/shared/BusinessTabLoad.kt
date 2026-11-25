package com.example.momentra.ui.shell.business.shared

import com.example.momentra.data.repository.BusinessSliceRepository
import com.example.momentra.ui.shell.perf.ShellPerf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

const val BUSINESS_PULSE_ACTIVITY_LIMIT = 5

private val pulseInflight =
    ConcurrentHashMap<String, CompletableDeferred<Result<BusinessTabDataCache.PulseTab>>>()

private val pulseGeneration = ConcurrentHashMap<String, AtomicLong>()

fun businessPulseLoadFamily(momentTypeCode: String?): BusinessMomentFamilyConfig.Family {
    val code = momentTypeCode?.uppercase().orEmpty()
    return when {
        code.contains("RUNWAY") -> BusinessMomentFamilyConfig.Family.MONEY
        code.contains("OPERATIONS") && !code.contains("TEAM") -> BusinessMomentFamilyConfig.Family.DAILY
        else -> BusinessMomentFamilyConfig.Family.TEAM
    }
}

/**
 * Drop cached Business facets and any in-flight pulse load for one moment.
 * Call this before bumping the refresh token or starting a new prefetch.
 */
fun invalidateBusinessPulseLoad(momentId: String) {
    BusinessTabDataCache.invalidateMoment(momentId)
    pulseGeneration.computeIfAbsent(momentId) { AtomicLong(0) }.incrementAndGet()
    pulseInflight.remove(momentId)?.complete(Result.failure(CancellationException("invalidated")))
}

private fun pulseGen(momentId: String): Long = pulseGeneration[momentId]?.get() ?: 0L

/**
 * Personal-parity pulse load: one bundled /pulse GET (finance + activity preview).
 * Callers should paint [BusinessTabDataCache.peekPulse] before awaiting.
 */
suspend fun loadBusinessPulseTab(
    repository: BusinessSliceRepository,
    momentId: String,
    activityLimit: Int = BUSINESS_PULSE_ACTIVITY_LIMIT,
    fetchTeamOpsMetrics: Boolean = false,
    family: BusinessMomentFamilyConfig.Family = BusinessMomentFamilyConfig.Family.TEAM,
): Result<BusinessTabDataCache.PulseTab> {
    val created = CompletableDeferred<Result<BusinessTabDataCache.PulseTab>>()
    val existing = pulseInflight.putIfAbsent(momentId, created)
    val shared = existing ?: created
    if (existing == null) {
        val generation = pulseGen(momentId)
        try {
            val fetched = fetchBundledPulse(repository, momentId, activityLimit, family)
            if (pulseGen(momentId) != generation) {
                created.complete(Result.failure(CancellationException("invalidated")))
            } else {
                fetched.onSuccess { BusinessTabDataCache.putPulse(momentId, it) }
                created.complete(fetched)
            }
        } catch (t: Throwable) {
            created.complete(Result.failure(t))
        } finally {
            pulseInflight.remove(momentId, created)
        }
    }
    val base = shared.await()
    if (!fetchTeamOpsMetrics) return base
    return base.mapCatching { tab -> enrichTeamOps(repository, momentId, tab) }
}

private suspend fun fetchBundledPulse(
    repository: BusinessSliceRepository,
    momentId: String,
    activityLimit: Int,
    family: BusinessMomentFamilyConfig.Family,
): Result<BusinessTabDataCache.PulseTab> = runCatching {
    val mark = ShellPerf.start("pulse_tab_ready")
    val data = when (family) {
        BusinessMomentFamilyConfig.Family.MONEY -> fetchMoneyPulse(repository, momentId, activityLimit)
        BusinessMomentFamilyConfig.Family.DAILY -> fetchDailyPulse(repository, momentId, activityLimit)
        BusinessMomentFamilyConfig.Family.TEAM -> fetchTeamPulse(repository, momentId, activityLimit)
    }
    ShellPerf.end(mark, mapOf("context" to "BUSINESS", "bundled" to true, "cached" to false))
    data
}

private suspend fun fetchMoneyPulse(
    repository: BusinessSliceRepository,
    momentId: String,
    activityLimit: Int,
): BusinessTabDataCache.PulseTab = coroutineScope {
    val pulseDeferred = async { repository.getPulse(momentId) }
    val lifeDeferred = async { repository.getLife(momentId) }
    val pulseFacet = pulseDeferred.await().getOrThrow()
    val lifeResult = lifeDeferred.await()
    val pulse = pulseFacet.payload
    val activities = pulse?.activity
        ?: repository.getActivity(momentId, limit = activityLimit).getOrThrow().items
    val approvals = repository.listPendingApprovals(momentId).getOrNull()?.items
    BusinessTabDataCache.PulseTab(
        pulse = pulse,
        finance = pulse?.finance,
        life = lifeResult.getOrNull()?.payload,
        activities = activities,
        businessFamily = pulseFacet.businessFamily,
        facetStatus = pulseFacet.status,
        approvals = approvals,
        lifeFailed = lifeResult.isFailure,
    )
}

private suspend fun fetchDailyPulse(
    repository: BusinessSliceRepository,
    momentId: String,
    activityLimit: Int,
): BusinessTabDataCache.PulseTab {
    val pulseFacet = repository.getPulse(momentId).getOrThrow()
    val pulse = pulseFacet.payload
    val activities = pulse?.activity
        ?: repository.getActivity(momentId, limit = activityLimit).getOrThrow().items
    val issues = repository.listIssues(momentId).getOrNull()?.items
    return BusinessTabDataCache.PulseTab(
        pulse = pulse,
        finance = pulse?.finance,
        life = null,
        activities = activities,
        businessFamily = pulseFacet.businessFamily,
        facetStatus = pulseFacet.status,
        issues = issues,
    )
}

private suspend fun fetchTeamPulse(
    repository: BusinessSliceRepository,
    momentId: String,
    activityLimit: Int,
): BusinessTabDataCache.PulseTab = coroutineScope {
    val pulseDeferred = async { repository.getPulse(momentId) }
    val rosterDeferred = async { repository.getRoster(momentId) }
    val approvalsDeferred = async { repository.listPendingApprovals(momentId) }
    val issuesDeferred = async { repository.listIssues(momentId) }
    val pulseFacet = pulseDeferred.await().getOrThrow()
    val pulse = pulseFacet.payload
    val activities = pulse?.activity
        ?: repository.getActivity(momentId, limit = activityLimit).getOrThrow().items
    val roster = rosterDeferred.await().getOrNull()
    BusinessTabDataCache.PulseTab(
        pulse = pulse,
        finance = pulse?.finance,
        life = null,
        activities = activities,
        businessFamily = pulseFacet.businessFamily,
        facetStatus = pulseFacet.status,
        approvals = approvalsDeferred.await().getOrNull()?.items,
        issues = issuesDeferred.await().getOrNull()?.items,
        rosterCount = roster?.members?.size,
    )
}

private suspend fun enrichTeamOps(
    repository: BusinessSliceRepository,
    momentId: String,
    tab: BusinessTabDataCache.PulseTab,
): BusinessTabDataCache.PulseTab {
    if (tab.capacity != null && tab.workload != null) return tab
    return coroutineScope {
        val capacityDeferred = async { repository.getCapacity(momentId) }
        val workloadDeferred = async { repository.getWorkload(momentId) }
        val enriched = tab.copy(
            capacity = capacityDeferred.await().getOrNull() ?: tab.capacity,
            workload = workloadDeferred.await().getOrNull() ?: tab.workload,
        )
        BusinessTabDataCache.putPulse(momentId, enriched)
        enriched
    }
}

/**
 * Memory tab load — reuses warm pulse/finance cache when present; only /memory is required.
 */
suspend fun loadBusinessMemoryTab(
    repository: BusinessSliceRepository,
    momentId: String,
): Result<BusinessTabDataCache.MemoryTab> = runCatching {
    coroutineScope {
        val cached = BusinessTabDataCache.peekPulse(momentId)
        val memoryDeferred = async { repository.getMemory(momentId) }
        val pulseDeferred = if (cached?.pulse != null) null else async { repository.getPulse(momentId) }
        val memory = memoryDeferred.await().getOrThrow().payload
        val pulseFacet = pulseDeferred?.await()?.getOrThrow()
        val pulse = cached?.pulse ?: pulseFacet?.payload
        val finance = cached?.finance ?: pulse?.finance ?: pulseFacet?.payload?.finance
        val data = BusinessTabDataCache.MemoryTab(
            memory = memory,
            pulse = pulse,
            finance = finance,
            life = cached?.life,
        )
        BusinessTabDataCache.putMemory(momentId, data)
        data
    }
}

/**
 * A business save refreshes Pulse by bumping the visible-tab token.
 * That screen load is the one fetch. Prefetch only when the caller asked
 * (moment select). A save that also prefetches finishes first, then the
 * screen starts a second Pulse load.
 */
fun businessRefreshPrefetchesPulse(forcePrefetch: Boolean): Boolean = forcePrefetch

/** Prefetch bundled pulse for Business tab SWR. */
suspend fun prefetchBusinessTabs(
    repository: BusinessSliceRepository,
    momentId: String,
    momentTypeCode: String? = null,
) {
    if (momentId.isBlank()) return
    loadBusinessPulseTab(
        repository,
        momentId,
        family = businessPulseLoadFamily(momentTypeCode),
    )
}
