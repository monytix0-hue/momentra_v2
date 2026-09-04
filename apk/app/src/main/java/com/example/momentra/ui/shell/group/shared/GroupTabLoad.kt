package com.example.momentra.ui.shell.group.shared

import com.example.momentra.data.repository.GroupSliceRepository
import com.example.momentra.ui.shell.perf.ShellPerf
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

const val GROUP_PULSE_ACTIVITY_LIMIT = 5

/** Parallel pulse + finance + activity with cache write. */
suspend fun loadGroupPulseTab(
    repository: GroupSliceRepository,
    momentId: String,
    activityLimit: Int = GROUP_PULSE_ACTIVITY_LIMIT,
): Result<GroupPulseTabData> = runCatching {
    val mark = ShellPerf.start("pulse_tab_ready")
    coroutineScope {
        val pulseDeferred = async { repository.getPulse(momentId) }
        val financeDeferred = async { repository.getFinance(momentId) }
        val activityDeferred = async { repository.getActivity(momentId, limit = activityLimit) }
        val insightsDeferred = async { repository.listAnalyticsInsights(scopeId = momentId) }
        val metricsDeferred = async { repository.listAnalyticsMetrics(scopeId = momentId) }
        val refreshDeferred = async { repository.refreshAnalytics(momentId = momentId) }
        val pulseFacet = pulseDeferred.await().getOrThrow()
        val financeFacet = financeDeferred.await().getOrThrow()
        val activities = activityDeferred.await().getOrThrow().items
        val insights = insightsDeferred.await().getOrNull() ?: emptyList()
        metricsDeferred.await()
        refreshDeferred.await()
        val data = GroupPulseTabData(
            title = pulseFacet.title,
            pulse = pulseFacet.payload,
            finance = financeFacet.payload ?: pulseFacet.payload?.finance,
            activities = activities,
            insights = insights,
        )
        GroupTabDataCache.putPulse(momentId, data)
        ShellPerf.end(mark, mapOf("context" to "GROUP", "parallel" to true, "cached" to false))
        data
    }
}

/**
 * Memory tab load — reuses warm pulse/finance cache when present; only /memory is required.
 * Callers should paint [GroupTabDataCache.peekPulse] / [peekMemory] before awaiting.
 */
suspend fun loadGroupMemoryTab(
    repository: GroupSliceRepository,
    momentId: String,
): Result<GroupTabDataCache.MemoryTab> = runCatching {
    coroutineScope {
        val cached = GroupTabDataCache.peekPulse(momentId)
        val memoryDeferred = async { repository.getMemory(momentId) }
        val financeDeferred = if (cached?.finance != null) null else async { repository.getFinance(momentId) }
        val pulseDeferred = if (cached?.pulse != null) null else async { repository.getPulse(momentId) }
        val participantsDeferred = async { repository.getParticipants(momentId) }
        val memory = memoryDeferred.await().getOrThrow().payload
        val finance = cached?.finance ?: financeDeferred!!.await().getOrThrow().payload
        val pulse = cached?.pulse ?: pulseDeferred!!.await().getOrThrow().payload
        val participants = participantsDeferred.await().getOrNull()?.participants.orEmpty()
        val data = GroupTabDataCache.MemoryTab(
            memory = memory,
            finance = finance,
            pulse = pulse,
            participants = participants,
        )
        GroupTabDataCache.putMemory(momentId, data)
        data
    }
}

/** Prefetch shared pulse+finance+activity for Group tab SWR (Personal-style warm path). */
suspend fun prefetchGroupTabs(repository: GroupSliceRepository, momentId: String) {
    if (momentId.isBlank()) return
    loadGroupPulseTab(repository, momentId)
}
