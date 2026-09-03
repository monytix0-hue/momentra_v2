package com.example.momentra.ui.shell.personal.lifestyle.create

import com.example.momentra.data.api.PersonalPulseDto
import kotlin.math.roundToInt
import com.example.momentra.ui.shell.personal.lifeops.create.PersonalLifeOpsDerived

/** Lifestyle Vitality Index + axis scores from pulse widgetPayload (PX-2 precision refresh writes CANONICAL_COUNTS). */
object PersonalLifestyleDerived {
    data class AxisScores(
        val joy: String,
        val fulfillment: String,
        val vitality: String,
        val exploration: String,
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

    fun axisScores(pulse: PersonalPulseDto?): AxisScores {
        val payload = pulse?.widgetPayload
        return AxisScores(
            joy = displayFromPayloadOrFallback(payload, "joyScore", pulse?.recoveryScore),
            fulfillment = displayFromPayloadOrFallback(payload, "fulfillmentScore", pulse?.wellbeingScore),
            vitality = displayFromPayloadOrFallback(payload, "vitalityScore", pulse?.rhythmScore),
            exploration = displayFromPayloadOrFallback(
                payload,
                "explorationScore",
                PersonalLifeOpsDerived.attentionDisplay(pulse?.attentionCount),
            ),
        )
    }

    fun vitalityIndex(pulse: PersonalPulseDto?): Int? {
        val axes = axisScores(pulse)
        val nums = listOf(axes.joy, axes.fulfillment, axes.vitality, axes.exploration)
            .mapNotNull { PersonalLifeOpsDerived.scoreNumber(it) }
        if (nums.isEmpty()) {
            return PersonalLifeOpsDerived.scoreNumber(pulse?.wellbeingScore)?.roundToInt()
        }
        return (nums.average()).roundToInt()
    }

    fun vitalityIndexDisplay(pulse: PersonalPulseDto?): String {
        return vitalityIndex(pulse)?.toString() ?: "—"
    }

    fun networkStability(pulse: PersonalPulseDto?): String {
        val index = vitalityIndex(pulse) ?: return "Awaiting first signals"
        return when {
            index >= 75 -> "Flourishing"
            index >= 50 -> "Growing"
            else -> "Building"
        }
    }

    fun experienceCount(pulse: PersonalPulseDto?): Int {
        val raw = payloadNumber(pulse?.widgetPayload, "experienceCount") ?: return 0
        return raw.toInt()
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

    fun momentumLabels(pulse: PersonalPulseDto?, activities: List<Pair<String, String>>): List<String> {
        val axes = axisScores(pulse)
        return listOf(
            if (axes.joy == "—") "Joy pending" else "Joy Rising",
            if (experienceCount(pulse) <= 0) "Ritual quiet" else "Experiences logged",
            if (pulse?.moodState.isNullOrBlank()) "Mood pending" else pulse?.moodState ?: "Mood pending",
            if (spendPairs(pulse).isEmpty()) "Budget quiet" else "Budget active",
        )
    }

    fun statusBadge(score: String, axis: String): String = PersonalLifeOpsDerived.statusBadge(score, axis)
}
