package com.example.momentra.ui.shell.business.shared

import com.example.momentra.data.repository.BusinessSliceRepository
import com.example.momentra.ui.shell.perf.ShellPerf
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.util.concurrent.ConcurrentHashMap

const val BUSINESS_PULSE_ACTIVITY_LIMIT = 5

private val pulseInflight =
    ConcurrentHashMap<String, CompletableDeferred<Result<BusinessTabDataCache.PulseTab>>>()

/**
 * Personal-parity pulse load: one bundled /pulse GET (finance + activity preview).
 * Callers should paint [BusinessTabDataCache.peekPulse] before awaiting.
 */
suspend fun loadBusinessPulseTab(
    repository: BusinessSliceRepository,
    momentId: String,
    activityLimit: Int = BUSINESS_PULSE_ACTIVITY_LIMIT,
    fetchTeamOpsMetrics: Boolean = false,
): Result<BusinessTabDataCache.PulseTab> {
    val created = CompletableDeferred<Result<BusinessTabDataCache.PulseTab>>()
    val existing = pulseInflight.putIfAbsent(momentId, created)
    val shared = existing ?: created
    if (existing == null) {
        try {
            created.complete(fetchBundledPulse(repository, momentId, activityLimit))
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
): Result<BusinessTabDataCache.PulseTab> = runCatching {
    val mark = ShellPerf.start("pulse_tab_ready")
    val pulseFacet = repository.getPulse(momentId).getOrThrow()
    val pulse = pulseFacet.payload
    val finance = pulse?.finance
    val activities = pulse?.activity
        ?: repository.getActivity(momentId, limit = activityLimit).getOrThrow().items
    val previous = BusinessTabDataCache.peekPulse(momentId)
    val data = BusinessTabDataCache.PulseTab(
        pulse = pulse,
        finance = finance ?: previous?.finance,
        life = previous?.life,
        activities = activities,
        businessFamily = pulseFacet.businessFamily,
        facetStatus = pulseFacet.status,
        capacity = previous?.capacity,
        workload = previous?.workload,
    )
    BusinessTabDataCache.putPulse(momentId, data)
    ShellPerf.end(mark, mapOf("context" to "BUSINESS", "bundled" to true, "cached" to false))
    data
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

/** Prefetch bundled pulse for Business tab SWR. */
suspend fun prefetchBusinessTabs(repository: BusinessSliceRepository, momentId: String) {
    if (momentId.isBlank()) return
    loadBusinessPulseTab(repository, momentId)
}
