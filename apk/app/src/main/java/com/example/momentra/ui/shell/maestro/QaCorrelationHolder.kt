package com.example.momentra.ui.shell.maestro

import java.util.UUID
import java.util.concurrent.atomic.AtomicReference

/**
 * Debug/QA correlation override for S9 Master Certification.
 *
 * Maestro (or adb) can set the next write's X-Correlation-Id via broadcast:
 *   adb shell am broadcast -a com.example.momentra.QA_SET_CORRELATION \
 *     --es correlation_id qa-20260827-personal-lifeops-expense-001 \
 *     --es run_id QA-20260827-0042
 *
 * Production builds ignore unset overrides and use random UUIDs.
 */
object QaCorrelationHolder {
    private val nextCorrelationId = AtomicReference<String?>(null)
    private val runId = AtomicReference<String?>(null)

    fun setNextCorrelationId(id: String?) {
        nextCorrelationId.set(id?.trim()?.takeIf { it.isNotEmpty() })
    }

    fun setRunId(id: String?) {
        runId.set(id?.trim()?.takeIf { it.isNotEmpty() })
    }

    fun peekRunId(): String? = runId.get()

    /** Consumes one-shot correlation id, or returns a fresh UUID. */
    fun takeCorrelationId(): String {
        val override = nextCorrelationId.getAndSet(null)
        return override ?: UUID.randomUUID().toString()
    }
}
