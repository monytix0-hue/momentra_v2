package com.example.momentra.ui.shell.business.shared

import com.example.momentra.data.api.ActivityItemDto
import com.example.momentra.data.api.BusinessFinancePayloadDto
import com.example.momentra.data.api.BusinessLifePayloadDto
import com.example.momentra.data.api.BusinessMemoryPayloadDto
import com.example.momentra.data.api.BusinessPulsePayloadDto
import com.example.momentra.data.api.CapacityDto
import com.example.momentra.data.api.WorkloadDto
import java.util.concurrent.ConcurrentHashMap

/** In-memory SWR cache for Business tab datasets keyed by momentId. */
object BusinessTabDataCache {
    data class PulseTab(
        val pulse: BusinessPulsePayloadDto?,
        val finance: BusinessFinancePayloadDto?,
        val life: BusinessLifePayloadDto?,
        val activities: List<ActivityItemDto>,
        val businessFamily: String?,
        val facetStatus: String?,
        val capacity: CapacityDto? = null,
        val workload: WorkloadDto? = null,
    )

    data class MemoryTab(
        val memory: BusinessMemoryPayloadDto?,
        val pulse: BusinessPulsePayloadDto?,
        val finance: BusinessFinancePayloadDto?,
        val life: BusinessLifePayloadDto?,
    )

    private val pulseByMoment = ConcurrentHashMap<String, PulseTab>()
    private val memoryByMoment = ConcurrentHashMap<String, MemoryTab>()

    fun peekPulse(momentId: String): PulseTab? = pulseByMoment[momentId]

    fun putPulse(momentId: String, data: PulseTab) {
        pulseByMoment[momentId] = data
    }

    fun peekMemory(momentId: String): MemoryTab? = memoryByMoment[momentId]

    fun putMemory(momentId: String, data: MemoryTab) {
        memoryByMoment[momentId] = data
    }

    /** Drop cached facets for one moment after a write so SWR reloads fresh data. */
    fun invalidateMoment(momentId: String) {
        pulseByMoment.remove(momentId)
        memoryByMoment.remove(momentId)
    }

    fun clear() {
        pulseByMoment.clear()
        memoryByMoment.clear()
    }
}
