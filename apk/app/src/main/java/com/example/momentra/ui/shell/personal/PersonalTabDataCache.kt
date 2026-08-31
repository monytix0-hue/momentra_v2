package com.example.momentra.ui.shell.personal

import com.example.momentra.data.api.ActivityItemDto
import com.example.momentra.data.api.PersonalPulseDto
import java.util.concurrent.ConcurrentHashMap

/** In-memory SWR cache for Personal tab datasets (pulse + activity preview). */
object PersonalTabDataCache {
    data class Entry(
        val pulse: PersonalPulseDto,
        val activities: List<ActivityItemDto>,
        val savedAtMs: Long = System.currentTimeMillis(),
    )

    private val store = ConcurrentHashMap<String, Entry>()

    fun cacheKey(momentId: String?): String = momentId?.takeIf { it.isNotBlank() } ?: "__personal_default__"

    fun peek(momentId: String?): Entry? = store[cacheKey(momentId)]

    fun put(momentId: String?, pulse: PersonalPulseDto, activities: List<ActivityItemDto>) {
        store[cacheKey(momentId)] = Entry(pulse, activities)
    }

    fun clear() {
        store.clear()
    }
}
