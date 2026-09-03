package com.example.momentra.ui.shell.personal.relationships.create

import com.example.momentra.data.api.PersonalPulseDto
import kotlin.math.roundToInt
import com.example.momentra.ui.shell.personal.lifeops.create.PersonalLifeOpsDerived

/** Relationships Bond Index from pulse widgetPayload (PX-3 precision refresh — no fixed heuristics). */
object PersonalRelationshipsDerived {
    data class BondAxes(
        val trust: String,
        val care: String,
        val support: String,
        val presence: String,
    )

    fun payloadNumber(payload: Map<String, Any>?, key: String): Double? {
        val raw = payload?.get(key) ?: return null
        return when (raw) {
            is Number -> raw.toDouble()
            is String -> PersonalLifeOpsDerived.scoreNumber(raw)
            else -> null
        }
    }

    fun displayFromPayloadOrFallback(
        payload: Map<String, Any>?,
        payloadKey: String,
        fallback: String?,
    ): String {
        val fromPayload = payloadNumber(payload, payloadKey)
        if (fromPayload != null) return fromPayload.roundToInt().toString()
        return PersonalLifeOpsDerived.displayScore(fallback)
    }

    fun bondIndex(pulse: PersonalPulseDto?): Int? {
        PersonalLifeOpsDerived.scoreNumber(pulse?.wellbeingScore)?.roundToInt()?.let { return it }
        PersonalLifeOpsDerived.scoreNumber(pulse?.rhythmScore)?.roundToInt()?.let { return it }
        val payload = pulse?.widgetPayload
        val axes = listOf("bondIndex", "trustScore", "careScore", "supportScore", "presenceScore")
            .mapNotNull { payloadNumber(payload, it) }
        if (axes.isEmpty()) return null
        return axes.average().roundToInt()
    }

    fun bondIndexDisplay(pulse: PersonalPulseDto?): String =
        bondIndex(pulse)?.toString() ?: "—"

    fun bondAxes(pulse: PersonalPulseDto?): BondAxes {
        val payload = pulse?.widgetPayload
        return BondAxes(
            trust = displayFromPayloadOrFallback(payload, "trustScore", null),
            care = displayFromPayloadOrFallback(payload, "careScore", null),
            support = displayFromPayloadOrFallback(payload, "supportScore", null),
            presence = displayFromPayloadOrFallback(payload, "presenceScore", null),
        )
    }

    fun bondSubtitle(pulse: PersonalPulseDto?): String {
        val score = bondIndex(pulse)
        return if (score != null) "Bond signal present" else "Awaiting bond signals"
    }

    fun spendPairs(pulse: PersonalPulseDto?): List<Pair<String, String>> =
        (pulse?.widgetPayload?.get("spendByCurrency") as? Map<*, *>)
            ?.entries
            ?.mapNotNull { (k, v) ->
                val key = k?.toString()?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                val value = v?.toString()?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                key to value
            }
            .orEmpty()
}
