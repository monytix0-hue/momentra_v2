package com.example.momentra.ui.shell.personal.shared

import androidx.compose.ui.graphics.Color
import com.example.momentra.R

/** V019 Personal Quick Add capability codes — mirrors iOS `PersonalActionRegistry`. */
enum class PersonalActionCode(val raw: String) {
    EXPENSE_CREATE("EXPENSE_CREATE"),
    LIFE_OBSERVATION_RECORD("LIFE_OBSERVATION_RECORD"),
    GOAL_CREATE("GOAL_CREATE"),
    MILESTONE_CREATE("MILESTONE_CREATE"),
    PROGRESS_RECORD("PROGRESS_RECORD"),
    OPPORTUNITY_CREATE("OPPORTUNITY_CREATE"),
    PIVOT_RECORD("PIVOT_RECORD"),
    LEARNING_ACTIVITY_CREATE("LEARNING_ACTIVITY_CREATE"),
    LIFESTYLE_ACTIVITY_CREATE("LIFESTYLE_ACTIVITY_CREATE"),
    RELATIONSHIP_ACTIVITY_RECORD("RELATIONSHIP_ACTIVITY_RECORD"),
    MOVEMENT_RECORD("MOVEMENT_RECORD"),
    ;

    companion object {
        fun fromRaw(raw: String): PersonalActionCode? =
            entries.firstOrNull { it.raw.equals(raw, ignoreCase = true) }
    }
}

data class PersonalActionTile(
    val code: PersonalActionCode,
    val label: String,
    val iconRes: Int,
    val gradientStart: Color,
    val gradientEnd: Color,
    val enabledWhenMomentActive: Boolean = true,
    val tappable: Boolean = true,
)

object PersonalActionRegistry {

    enum class Destination {
        EXPENSE,
        LIFE_OPS,
        FUTURE,
        LIFESTYLE,
        RELATIONSHIPS,
        MOVEMENT,
    }

    fun destinationFor(code: PersonalActionCode): Destination = when (code) {
        PersonalActionCode.EXPENSE_CREATE -> Destination.EXPENSE
        PersonalActionCode.LIFE_OBSERVATION_RECORD -> Destination.LIFE_OPS
        PersonalActionCode.GOAL_CREATE,
        PersonalActionCode.MILESTONE_CREATE,
        PersonalActionCode.PROGRESS_RECORD,
        PersonalActionCode.OPPORTUNITY_CREATE,
        PersonalActionCode.PIVOT_RECORD,
        PersonalActionCode.LEARNING_ACTIVITY_CREATE,
        -> Destination.FUTURE
        PersonalActionCode.LIFESTYLE_ACTIVITY_CREATE -> Destination.LIFESTYLE
        PersonalActionCode.RELATIONSHIP_ACTIVITY_RECORD -> Destination.RELATIONSHIPS
        PersonalActionCode.MOVEMENT_RECORD -> Destination.MOVEMENT
    }

    fun defaultCodes(family: PersonalPulseFamily): List<PersonalActionCode> = when (family) {
        PersonalPulseFamily.LIFE_OPERATIONS -> listOf(
            PersonalActionCode.EXPENSE_CREATE,
            PersonalActionCode.LIFE_OBSERVATION_RECORD,
            PersonalActionCode.MOVEMENT_RECORD,
        )
        PersonalPulseFamily.FUTURE_BUILDING -> listOf(
            PersonalActionCode.MILESTONE_CREATE,
            PersonalActionCode.OPPORTUNITY_CREATE,
            PersonalActionCode.PIVOT_RECORD,
            PersonalActionCode.PROGRESS_RECORD,
            PersonalActionCode.LEARNING_ACTIVITY_CREATE,
        )
        PersonalPulseFamily.LIFESTYLE -> listOf(PersonalActionCode.LIFESTYLE_ACTIVITY_CREATE)
        PersonalPulseFamily.RELATIONSHIPS -> listOf(PersonalActionCode.RELATIONSHIP_ACTIVITY_RECORD)
    }

    /** Static Life Ops catalog — Future/Lifestyle/Relationships omitted (YAGNI on Android hub). */
    fun catalogTiles(family: PersonalPulseFamily): List<PersonalActionTile> = when (family) {
        PersonalPulseFamily.LIFE_OPERATIONS -> listOf(
            tile(PersonalActionCode.EXPENSE_CREATE, "Spend", R.drawable.ic_pers_wallet, 0xFF8B5CF6, 0xFF6C4EF2),
            tile(PersonalActionCode.LIFE_OBSERVATION_RECORD, "Mood", R.drawable.ic_pers_smile, 0xFF06B6D4, 0xFF0891B2),
            tile(PersonalActionCode.LIFE_OBSERVATION_RECORD, "Recovery", R.drawable.ic_pers_heart_pulse, 0xFF3B82F6, 0xFF1D4ED8),
            tile(PersonalActionCode.EXPENSE_CREATE, "Income", R.drawable.ic_pers_trending_up, 0xFF10B981, 0xFF047857),
            tile(PersonalActionCode.LIFE_OBSERVATION_RECORD, "Attention", R.drawable.ic_pers_award, 0xFFA78BFA, 0xFF7C3AED),
            tile(PersonalActionCode.MOVEMENT_RECORD, "Transfer", R.drawable.ic_pers_repeat, 0xFF1E40AF, 0xFF0B2A8A),
            tile(PersonalActionCode.MOVEMENT_RECORD, "Savings", R.drawable.ic_pers_piggy_bank, 0xFF10B981, 0xFF047857),
            tile(PersonalActionCode.LIFE_OBSERVATION_RECORD, "Adjust", R.drawable.ic_pers_layers, 0xFFD946EF, 0xFF86198F),
            tile(
                PersonalActionCode.LIFE_OBSERVATION_RECORD,
                "Reflect",
                R.drawable.ic_pers_book_open,
                0xFF6366F1,
                0xFF4338CA,
                tappable = false,
            ),
        )
        else -> emptyList()
    }

    fun tiles(
        family: PersonalPulseFamily,
        hasActiveMoment: Boolean,
        capabilityCodes: List<String>? = null,
        simpleMode: Boolean = false,
    ): List<PersonalActionTile> {
        val effectiveCaps = capabilityCodes?.map { it.uppercase() }
            ?: defaultCodes(family).map { it.raw }
        var catalog = catalogTiles(family)
        if (simpleMode && family == PersonalPulseFamily.LIFE_OPERATIONS) {
            val keep = setOf("Spend", "Mood", "Recovery")
            catalog = catalog.filter { it.label in keep }
        }
        return catalog.map { tile ->
            val enabled = isTileEnabled(tile, hasActiveMoment, effectiveCaps)
            tile.copy(enabledWhenMomentActive = enabled)
        }
    }

    private fun isTileEnabled(
        tile: PersonalActionTile,
        hasActiveMoment: Boolean,
        capabilityCodes: List<String>,
    ): Boolean {
        if (!hasActiveMoment || !tile.tappable) return false
        if (capabilityCodes.isEmpty()) return false
        val granted = capabilityCodes.map { it.uppercase() }.toSet()
        return granted.contains(tile.code.raw.uppercase())
    }

    private fun tile(
        code: PersonalActionCode,
        label: String,
        iconRes: Int,
        startArgb: Long,
        endArgb: Long,
        tappable: Boolean = true,
    ): PersonalActionTile = PersonalActionTile(
        code = code,
        label = label,
        iconRes = iconRes,
        gradientStart = Color(startArgb),
        gradientEnd = Color(endArgb),
        enabledWhenMomentActive = tappable,
        tappable = tappable,
    )
}