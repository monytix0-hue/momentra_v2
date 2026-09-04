package com.example.momentra.ui.shell.group.shared

import com.example.momentra.data.api.ActivityItemDto
import com.example.momentra.data.api.AnalyticsInsightItemDto
import com.example.momentra.data.api.GroupFinancePayloadDto
import com.example.momentra.data.api.GroupLifePayloadDto
import com.example.momentra.data.api.GroupMemoryPayloadDto
import com.example.momentra.data.api.GroupParticipantDto
import com.example.momentra.data.api.GroupPulsePayloadDto
import java.util.concurrent.ConcurrentHashMap

/** In-memory SWR cache for Group tab datasets keyed by momentId. */
object GroupTabDataCache {
    data class PulseTab(
        val title: String?,
        val pulse: GroupPulsePayloadDto?,
        val finance: GroupFinancePayloadDto?,
        val activities: List<ActivityItemDto>,
        val insights: List<AnalyticsInsightItemDto> = emptyList(),
    )

    data class MemoryTab(
        val memory: GroupMemoryPayloadDto?,
        val finance: GroupFinancePayloadDto?,
        val pulse: GroupPulsePayloadDto?,
        val participants: List<GroupParticipantDto> = emptyList(),
    )

    private val pulseByMoment = ConcurrentHashMap<String, PulseTab>()
    private val memoryByMoment = ConcurrentHashMap<String, MemoryTab>()
    private val lifeByMoment = ConcurrentHashMap<String, GroupLifePayloadDto>()

    fun peekPulse(momentId: String): PulseTab? = pulseByMoment[momentId]

    fun putPulse(momentId: String, data: GroupPulseTabData) {
        pulseByMoment[momentId] = PulseTab(
            title = data.title,
            pulse = data.pulse,
            finance = data.finance,
            activities = data.activities,
            insights = data.insights,
        )
    }

    fun peekMemory(momentId: String): MemoryTab? = memoryByMoment[momentId]

    fun putMemory(momentId: String, data: MemoryTab) {
        memoryByMoment[momentId] = data
    }

    fun peekLife(momentId: String): GroupLifePayloadDto? = lifeByMoment[momentId]

    fun putLife(momentId: String, payload: GroupLifePayloadDto?) {
        if (payload != null) {
            lifeByMoment[momentId] = payload
        }
    }

    /** Drop cached facets for one moment after a write so SWR reloads fresh data. */
    fun invalidateMoment(momentId: String) {
        pulseByMoment.remove(momentId)
        memoryByMoment.remove(momentId)
        lifeByMoment.remove(momentId)
    }

    fun clear() {
        pulseByMoment.clear()
        memoryByMoment.clear()
        lifeByMoment.clear()
    }
}

data class GroupPulseTabData(
    val title: String?,
    val pulse: GroupPulsePayloadDto?,
    val finance: GroupFinancePayloadDto?,
    val activities: List<ActivityItemDto>,
    val insights: List<AnalyticsInsightItemDto> = emptyList(),
)
