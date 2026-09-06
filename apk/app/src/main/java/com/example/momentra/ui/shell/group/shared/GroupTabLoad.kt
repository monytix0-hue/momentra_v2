package com.example.momentra.ui.shell.group.shared

import com.example.momentra.data.repository.GroupSliceRepository
import com.example.momentra.ui.shell.perf.ShellPerf
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.util.concurrent.ConcurrentHashMap

const val GROUP_PULSE_ACTIVITY_LIMIT = 5

private val pulseInflight =
    ConcurrentHashMap<String, CompletableDeferred<Result<GroupPulseTabData>>>()

/**
 * Paint-first Group pulse load: pulse + finance + activity only.
 * Call [enrichGroupPulseTab] after paint for insights / analytics (non-blocking for spinner).
 * Inflight-deduped so VM prefetch + UI cannot double-fetch.
 */
suspend fun loadGroupPulseTab(
    repository: GroupSliceRepository,
    momentId: String,
    activityLimit: Int = GROUP_PULSE_ACTIVITY_LIMIT,
): Result<GroupPulseTabData> {
    val created = CompletableDeferred<Result<GroupPulseTabData>>()
    val existing = pulseInflight.putIfAbsent(momentId, created)
    val shared = existing ?: created
    if (existing == null) {
        try {
            created.complete(fetchGroupPulseCritical(repository, momentId, activityLimit))
        } catch (t: Throwable) {
            created.complete(Result.failure(t))
        } finally {
            pulseInflight.remove(momentId, created)
        }
    }
    return shared.await()
}

private suspend fun fetchGroupPulseCritical(
    repository: GroupSliceRepository,
    momentId: String,
    activityLimit: Int,
): Result<GroupPulseTabData> = runCatching {
    val mark = ShellPerf.start("pulse_tab_ready")
    coroutineScope {
        val pulseDeferred = async { repository.getPulse(momentId) }
        val financeDeferred = async { repository.getFinance(momentId) }
        val activityDeferred = async { repository.getActivity(momentId, limit = activityLimit) }
        val pulseFacet = pulseDeferred.await().getOrThrow()
        val financeFacet = financeDeferred.await().getOrThrow()
        val activities = activityDeferred.await().getOrThrow().items
        val previous = GroupTabDataCache.peekPulse(momentId)
        val data = GroupPulseTabData(
            title = pulseFacet.title,
            pulse = pulseFacet.payload,
            finance = financeFacet.payload ?: pulseFacet.payload?.finance,
            activities = activities,
            insights = previous?.insights.orEmpty(),
        )
        GroupTabDataCache.putPulse(momentId, data)
        ShellPerf.end(
            mark,
            mapOf("context" to "GROUP", "parallel" to true, "paint" to true, "cached" to false),
        )
        data
    }
}

/**
 * Deferred analytics enrich — does not block Pulse first paint.
 * Merges insights into [GroupTabDataCache] and returns updated tab data.
 */
suspend fun enrichGroupPulseTab(
    repository: GroupSliceRepository,
    momentId: String,
): Result<GroupPulseTabData> = runCatching {
    coroutineScope {
        val insightsDeferred = async { repository.listAnalyticsInsights(scopeId = momentId) }
        val metricsDeferred = async { repository.listAnalyticsMetrics(scopeId = momentId) }
        val refreshDeferred = async { repository.refreshAnalytics(momentId = momentId) }
        val insights = insightsDeferred.await().getOrNull() ?: emptyList()
        metricsDeferred.await()
        refreshDeferred.await()
        val previous = GroupTabDataCache.peekPulse(momentId)
            ?: return@coroutineScope GroupPulseTabData(
                title = null,
                pulse = null,
                finance = null,
                activities = emptyList(),
                insights = insights,
            )
        val data = GroupPulseTabData(
            title = previous.title,
            pulse = previous.pulse,
            finance = previous.finance,
            activities = previous.activities,
            insights = insights,
        )
        GroupTabDataCache.putPulse(momentId, data)
        ShellPerf.instant("group_pulse_enrich", mapOf("momentId" to momentId.take(8)))
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

/** Prefetch critical-path pulse+finance+activity for Group tab SWR. */
suspend fun prefetchGroupTabs(repository: GroupSliceRepository, momentId: String) {
    if (momentId.isBlank()) return
    loadGroupPulseTab(repository, momentId)
}
