package com.example.momentra.ui.shell.business.shared

import com.example.momentra.domain.MomentSummary
import java.util.concurrent.ConcurrentHashMap

/**
 * Business shell routing helpers (pending Quick Add selection per moment).
 * Callers: AppShellScreen → OperatingMomentPage CREATE tab.
 */
object BusinessMomentFamilyConfig {
    private val pendingQuickAddByMoment = ConcurrentHashMap<String, String>()

    fun setPendingQuickAdd(momentId: String, code: String) {
        if (code.isNotBlank()) {
            pendingQuickAddByMoment[momentId] = code.trim().uppercase()
        }
    }

    fun peekPendingQuickAdd(momentId: String): String? = pendingQuickAddByMoment[momentId]

    fun consumePendingQuickAdd(momentId: String): String? = pendingQuickAddByMoment.remove(momentId)

    fun selectedQuickAddTypeCode(
        momentId: String?,
        moments: List<MomentSummary>,
        fallbackTypeCode: String?,
    ): String? {
        if (!momentId.isNullOrBlank()) {
            peekPendingQuickAdd(momentId)?.let { return fallbackTypeCode }
        }
        return fallbackTypeCode
            ?: moments.firstOrNull { it.momentId == momentId }?.momentTypeCode
    }
}