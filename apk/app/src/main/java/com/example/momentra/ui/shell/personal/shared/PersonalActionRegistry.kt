package com.example.momentra.ui.shell.personal.shared

/**
 * Single mapper from V019 capability / action codes to Quick Add destinations.
 * Empty capabilities fail closed until bootstrap fills V019 codes (parity with Group).
 */
object PersonalActionRegistry {

    const val EXPENSE_CREATE = "EXPENSE_CREATE"
    const val LIFE_OBSERVATION_RECORD = "LIFE_OBSERVATION_RECORD"
    const val GOAL_CREATE = "GOAL_CREATE"
    const val MILESTONE_CREATE = "MILESTONE_CREATE"
    const val LIFESTYLE_ACTIVITY_CREATE = "LIFESTYLE_ACTIVITY_CREATE"
    const val RELATIONSHIP_ACTIVITY_RECORD = "RELATIONSHIP_ACTIVITY_RECORD"
    const val MOVEMENT_RECORD = "MOVEMENT_RECORD"

    enum class Destination {
        EXPENSE,
        LIFE_OPS,
        FUTURE,
        LIFESTYLE,
        RELATIONSHIPS,
        MOVEMENT,
    }

    fun destinationFor(capabilityCode: String): Destination? = when (capabilityCode.uppercase()) {
        EXPENSE_CREATE -> Destination.EXPENSE
        LIFE_OBSERVATION_RECORD -> Destination.LIFE_OPS
        GOAL_CREATE, MILESTONE_CREATE,
        "PROGRESS_RECORD", "OPPORTUNITY_CREATE", "PIVOT_RECORD", "LEARNING_ACTIVITY_CREATE",
        "PIVOT_CREATE", "LEARNING_RECORD", // legacy aliases
        -> Destination.FUTURE
        LIFESTYLE_ACTIVITY_CREATE -> Destination.LIFESTYLE
        RELATIONSHIP_ACTIVITY_RECORD -> Destination.RELATIONSHIPS
        MOVEMENT_RECORD -> Destination.MOVEMENT
        else -> null
    }

    /**
     * Empty capabilities must NOT enable everything — fail closed until bootstrap fills V019 codes
     * (same rule as GroupActionRegistry).
     */
    fun isDestinationEnabled(capabilities: List<String>, destination: Destination): Boolean {
        if (capabilities.isEmpty()) return false
        return capabilities.any { destinationFor(it) == destination }
    }

    fun enabledDestinations(capabilities: List<String>): Set<Destination> {
        if (capabilities.isEmpty()) return emptySet()
        return capabilities.mapNotNull { destinationFor(it) }.toSet()
    }
}
