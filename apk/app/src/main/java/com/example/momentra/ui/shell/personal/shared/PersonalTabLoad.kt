package com.example.momentra.ui.shell.personal.shared

import com.example.momentra.data.api.ActivityItemDto
import com.example.momentra.data.api.PersonalPulseDto
import com.example.momentra.data.repository.PersonalSliceRepository
import com.example.momentra.ui.shell.perf.ShellPerf
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/** Pulse-tab preview: 5 activity rows keeps Personal Pulse snappy (full list uses dedicated flow). */
const val PERSONAL_PULSE_ACTIVITY_LIMIT = 5

/** Moments first page — UI must not assume this is the full history. */
const val PERSONAL_MOMENTS_ACTIVITY_LIMIT = 30

data class PersonalMomentsPage(
    val pulse: PersonalPulseDto?,
    val activities: List<ActivityItemDto>,
    val mayHaveMore: Boolean,
)

/**
 * Parallel pulse + activity fetch with in-memory SWR cache.
 * Callers should paint [PersonalTabDataCache.peek] before awaiting this when possible.
 */
suspend fun loadPersonalPulseTab(
    repository: PersonalSliceRepository,
    momentId: String?,
    activityLimit: Int = PERSONAL_PULSE_ACTIVITY_LIMIT,
): Result<Pair<PersonalPulseDto, List<ActivityItemDto>>> = runCatching {
    val mark = ShellPerf.start("pulse_tab_ready")
    coroutineScope {
        val pulseDeferred = async { repository.getPulse(momentId = momentId) }
        val activityDeferred = async {
            repository.getActivity(momentId = momentId, limit = activityLimit)
        }
        val pulse = pulseDeferred.await().getOrThrow()
        val activities = activityDeferred.await().getOrThrow().items
        PersonalTabDataCache.put(momentId, pulse, activities)
        ShellPerf.end(mark, mapOf("context" to "PERSONAL", "parallel" to true, "cached" to false))
        pulse to activities
    }
}

suspend fun loadPersonalMomentsTab(
    repository: PersonalSliceRepository,
    momentId: String?,
    activityLimit: Int = PERSONAL_MOMENTS_ACTIVITY_LIMIT,
): Result<PersonalMomentsPage> = runCatching {
    val mark = ShellPerf.start("moments_tab_ready")
    coroutineScope {
        val pulseDeferred = async { repository.getPulse(momentId = momentId) }
        val activityDeferred = async {
            repository.getActivity(momentId = momentId, limit = activityLimit)
        }
        val pulse = pulseDeferred.await().getOrNull()
        val activities = activityDeferred.await().getOrNull()?.items.orEmpty()
        ShellPerf.end(mark, mapOf("context" to "PERSONAL", "moments" to true))
        PersonalMomentsPage(
            pulse = pulse,
            activities = activities,
            mayHaveMore = activities.size >= activityLimit,
        )
    }
}
