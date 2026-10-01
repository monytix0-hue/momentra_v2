package com.example.momentra.ui.shell.personal.shared

import com.example.momentra.ui.shell.personal.lifeops.create.PersonalLifeOpsDerived

/** Human-language Pulse copy helpers — intelligence stays under the labels. */
object PersonalPulseCopy {
    data class SignalLine(
        val direction: Direction,
        val label: String,
    ) {
        enum class Direction { UP, DOWN, NEUTRAL }
    }

    fun statusBand(wellbeingScore: String?): String {
        val n = PersonalLifeOpsDerived.scoreNumber(wellbeingScore) ?: return "Getting started"
        return when {
            n >= 75 -> "Feeling strong"
            n >= 55 -> "Feeling steady"
            n >= 35 -> "Needs a little care"
            else -> "Feeling strained"
        }
    }

    fun heroSentence(
        family: PersonalPulseFamily,
        recoveryScore: String?,
        wellbeingScore: String?,
        moodState: String?,
        spendPairs: List<Pair<String, String>>,
        todayLogCount: Int,
    ): String {
        val recovery = PersonalLifeOpsDerived.scoreNumber(recoveryScore)
        val wellbeing = PersonalLifeOpsDerived.scoreNumber(wellbeingScore)
        val mood = moodState?.trim().orEmpty()

        return when (family) {
            PersonalPulseFamily.LIFE_OPERATIONS -> {
                val parts = mutableListOf<String>()
                if (recovery != null) {
                    parts += if (recovery >= 60) "Recovery is good." else "Recovery needs a little attention."
                }
                when {
                    spendPairs.isNotEmpty() -> parts += "Spending is on the board."
                    todayLogCount == 0 -> parts += "Nothing logged yet today."
                    mood.isEmpty() -> parts += "A quick mood check-in would help."
                }
                if (parts.isEmpty()) {
                    parts += when {
                        wellbeing != null && wellbeing >= 55 -> "Your day is taking shape."
                        wellbeing != null -> "Start with one small log."
                        else -> "Start with spend, mood, or recovery."
                    }
                }
                parts.take(2).joinToString(" ")
            }
            PersonalPulseFamily.FUTURE_BUILDING -> when {
                todayLogCount == 0 -> "Log a milestone, progress, or learning to move the needle."
                wellbeing != null && wellbeing >= 60 -> "Momentum is building. Keep the progress logs coming."
                else -> "A small progress log today compounds."
            }
            PersonalPulseFamily.LIFESTYLE -> when {
                todayLogCount == 0 -> "An experience or wellbeing check-in keeps lifestyle alive."
                recovery != null && recovery >= 60 -> "Vitality looks solid. Protect a ritual today."
                else -> "A quieter stretch — log one experience outside the usual."
            }
            PersonalPulseFamily.RELATIONSHIPS -> when {
                todayLogCount == 0 -> "A quick connection log keeps people close."
                else -> "Presence compounds. Log a connection or shared moment."
            }
        }
    }

    fun shapedTodaySignals(
        family: PersonalPulseFamily,
        recoveryScore: String?,
        wellbeingScore: String?,
        rhythmScore: String?,
        attentionCount: Int?,
        moodState: String?,
        spendPairs: List<Pair<String, String>>,
        helpingLabels: List<String>,
        hurtingLabels: List<String>,
    ): List<SignalLine> {
        val lines = mutableListOf<SignalLine>()
        val recovery = PersonalLifeOpsDerived.scoreNumber(recoveryScore)
        val wellbeing = PersonalLifeOpsDerived.scoreNumber(wellbeingScore)
        val rhythm = PersonalLifeOpsDerived.scoreNumber(rhythmScore)

        when (family) {
            PersonalPulseFamily.LIFE_OPERATIONS -> {
                if (recovery != null) {
                    lines += SignalLine(if (recovery >= 55) SignalLine.Direction.UP else SignalLine.Direction.DOWN, "Recovery")
                }
                if (rhythm != null) {
                    lines += SignalLine(if (rhythm >= 55) SignalLine.Direction.UP else SignalLine.Direction.DOWN, "Discipline")
                }
                if (spendPairs.isNotEmpty()) {
                    lines += SignalLine(SignalLine.Direction.DOWN, "Spending pressure")
                }
            }
            PersonalPulseFamily.FUTURE_BUILDING -> {
                if (wellbeing != null) {
                    lines += SignalLine(if (wellbeing >= 55) SignalLine.Direction.UP else SignalLine.Direction.DOWN, "Vision")
                }
                if (recovery != null) {
                    lines += SignalLine(if (recovery >= 55) SignalLine.Direction.UP else SignalLine.Direction.DOWN, "Growth")
                }
                if (rhythm != null) {
                    lines += SignalLine(if (rhythm >= 55) SignalLine.Direction.UP else SignalLine.Direction.NEUTRAL, "Momentum")
                }
            }
            PersonalPulseFamily.LIFESTYLE -> {
                if (recovery != null) {
                    lines += SignalLine(if (recovery >= 55) SignalLine.Direction.UP else SignalLine.Direction.DOWN, "Joy")
                }
                if (wellbeing != null) {
                    lines += SignalLine(if (wellbeing >= 55) SignalLine.Direction.UP else SignalLine.Direction.DOWN, "Fulfillment")
                }
                lines += SignalLine(
                    if ((attentionCount ?: 0) == 0) SignalLine.Direction.DOWN else SignalLine.Direction.UP,
                    "Exploration",
                )
            }
            PersonalPulseFamily.RELATIONSHIPS -> {
                if (recovery != null) {
                    lines += SignalLine(if (recovery >= 55) SignalLine.Direction.UP else SignalLine.Direction.DOWN, "Care")
                }
                if (wellbeing != null) {
                    lines += SignalLine(if (wellbeing >= 55) SignalLine.Direction.UP else SignalLine.Direction.DOWN, "Trust")
                }
                if (rhythm != null) {
                    lines += SignalLine(if (rhythm >= 55) SignalLine.Direction.UP else SignalLine.Direction.NEUTRAL, "Presence")
                }
            }
        }

        if (lines.isEmpty()) {
            if (helpingLabels.isNotEmpty()) lines += SignalLine(SignalLine.Direction.UP, helpingLabels[0])
            if (hurtingLabels.isNotEmpty()) lines += SignalLine(SignalLine.Direction.DOWN, hurtingLabels[0])
        }
        if (lines.isEmpty()) {
            val mood = moodState?.trim().orEmpty()
            lines += if (mood.isNotEmpty()) {
                SignalLine(SignalLine.Direction.NEUTRAL, "Mood · $mood")
            } else {
                SignalLine(SignalLine.Direction.NEUTRAL, "Waiting on today's first logs")
            }
        }
        return lines.take(4)
    }

    fun moneySpentLine(spendPairs: List<Pair<String, String>>, format: (String) -> String): String {
        if (spendPairs.isEmpty()) return "No spend logged"
        return spendPairs.joinToString(" · ") { "${format(it.second)} ${it.first}" } + " spent"
    }
}
