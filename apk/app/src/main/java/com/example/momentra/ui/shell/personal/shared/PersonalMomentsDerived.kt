package com.example.momentra.ui.shell.personal.shared

import com.example.momentra.data.api.ActivityItemDto
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

data class MomentsHighlightItem(
    val title: String,
    val detail: String,
)

object PersonalMomentsHighlight {
    fun select(family: PersonalPulseFamily, activities: List<ActivityItemDto>): MomentsHighlightItem? {
        val visible = activities.filter { PersonalActivityTimelineDerived.isVisible(it) }
        if (visible.isEmpty()) return null
        return when (family) {
            PersonalPulseFamily.LIFE_OPERATIONS -> {
                visible.firstOrNull { it.activityCode.uppercase().contains("RECOVERY") }?.let {
                    MomentsHighlightItem(PersonalActivityTimelineDerived.displayTitle(it), "A recovery log that stood out")
                } ?: visible.firstOrNull { PersonalActivityTimelineDerived.isExpense(it) }?.let {
                    MomentsHighlightItem(PersonalActivityTimelineDerived.displayTitle(it), "Notable spend this period")
                }
            }
            PersonalPulseFamily.FUTURE_BUILDING -> {
                visible.firstOrNull { it.activityCode.uppercase().contains("MILESTONE") }?.let {
                    MomentsHighlightItem(PersonalActivityTimelineDerived.displayTitle(it), "Milestone logged")
                } ?: visible.firstOrNull { it.activityCode.uppercase().contains("PROGRESS") }?.let {
                    MomentsHighlightItem(PersonalActivityTimelineDerived.displayTitle(it), "Progress that moved the needle")
                }
            }
            PersonalPulseFamily.LIFESTYLE -> {
                visible.firstOrNull {
                    val c = it.activityCode.uppercase()
                    c.contains("EXPERIENCE") || c.contains("WELLBEING") || c.contains("DISCOVERY")
                }?.let {
                    MomentsHighlightItem(PersonalActivityTimelineDerived.displayTitle(it), "A moment worth keeping")
                }
            }
            PersonalPulseFamily.RELATIONSHIPS -> {
                visible.firstOrNull {
                    val c = it.activityCode.uppercase()
                    c.contains("CONNECTION") || c.contains("SHARED") || c.contains("SUPPORT")
                }?.let {
                    MomentsHighlightItem(PersonalActivityTimelineDerived.displayTitle(it), "A meaningful connection")
                }
            }
        }
    }

    fun sectionTitle(family: PersonalPulseFamily): String = when (family) {
        PersonalPulseFamily.LIFE_OPERATIONS -> "Turning Points"
        PersonalPulseFamily.FUTURE_BUILDING -> "Breakthroughs"
        PersonalPulseFamily.LIFESTYLE -> "Best Moments"
        PersonalPulseFamily.RELATIONSHIPS -> "Connections"
    }
}

object PersonalMomentsMoneyLine {
    fun line(family: PersonalPulseFamily, spendPairs: List<Pair<String, String>>): String? {
        val nonzero = spendPairs.mapNotNull { (currency, amount) ->
            val n = amount.toDoubleOrNull() ?: return@mapNotNull null
            if (n <= 0) null else currency to n
        }
        if (nonzero.isEmpty()) return null
        val formatted = nonzero.joinToString(" · ") { (currency, n) ->
            val symbol = if (currency == "INR") "₹" else "$currency "
            val value = if (kotlin.math.abs(n - n.toInt()) < 0.001) "${n.toInt()}" else "%.2f".format(n)
            "$symbol$value"
        }
        return when (family) {
            PersonalPulseFamily.LIFE_OPERATIONS -> "$formatted was part of your everyday this month"
            PersonalPulseFamily.FUTURE_BUILDING -> "$formatted invested toward your future this month"
            PersonalPulseFamily.LIFESTYLE -> "$formatted was part of your experiences this month"
            PersonalPulseFamily.RELATIONSHIPS -> "$formatted was shared with people this month"
        }
    }
}

object PersonalMomentsHeaderCopy {
    fun periodLabel(): String =
        DateTimeFormatter.ofPattern("MMMM", Locale.getDefault()).format(LocalDate.now())

    fun subtitle(momentCount: Int): String = when {
        momentCount <= 0 -> "Nothing logged yet this period."
        momentCount == 1 -> "1 moment so far"
        else -> "$momentCount moments so far"
    }
}
