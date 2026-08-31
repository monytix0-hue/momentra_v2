package com.example.momentra.ui.shell.empty.personal

import com.example.momentra.ui.setup.SetupChipOption

data class PersonalSetupCatalogEntry(
    val defaultTitle: String,
    val subtitle: String,
    val momentTypeCode: String,
    val activateLabel: String,
    val footerTagline: String,
    val fields: List<PersonalSetupFieldSpec> = emptyList(),
    val defaultPreferences: Map<String, Any>,
)

data class PersonalSetupFieldSpec(
    val key: String,
    val label: String,
    val multiSelect: Boolean,
    val options: List<String>,
    val emojiByOption: Map<String, String> = emptyMap(),
    val kind: com.example.momentra.ui.setup.SetupFieldKind = com.example.momentra.ui.setup.SetupFieldKind.CHIPS,
) {
    fun chipOptions(): List<SetupChipOption> = options.map { option ->
        SetupChipOption(value = option, label = option, emoji = emojiByOption[option])
    }
}

/** Defaults + allowedKeys must match backend PERSONAL_SETUP_CATALOG (Figma field model). */
object PersonalSetupCatalog {
    private val lifeOps = PersonalSetupCatalogEntry(
        defaultTitle = "My life operations rhythm",
        subtitle = "Create a calmer operating system for everyday life. Everything can be refined later.",
        momentTypeCode = "LIFE_RHYTHM",
        activateLabel = "Activate Life Operations →",
        footerTagline = "Private by default · Change anytime",
        defaultPreferences = mapOf(
            "lifeFocus" to "Daily balance",
            "currentRhythm" to "Busy but manageable",
            "primaryNeed" to "More breathing room",
            "healthEnergy" to "Selected",
            "timeBalance" to "Selected",
            "shapesFocus" to "Daily balance",
            "shapesRhythm" to "Busy but manageable",
            "mainPressure" to "Too many commitments",
            "recoveryWindow" to "Weekends",
            "checkInRhythm" to "Weekly",
            "helpfulSupport" to "Clear routines",
            "recoveryStyle" to "Quiet time",
            "habit" to "Morning routine",
            "habit2" to "",
            "currentEnergy" to "Steady",
            "reflectWeekly" to true,
            "stressCheckIn" to "Enabled",
            "recoveryCheckIn" to "Enabled",
            "reviewCadence" to "Every week",
            "profile" to "STRUCTURE SEEKER",
        ),
    )

    private val future = PersonalSetupCatalogEntry(
        defaultTitle = "My future building",
        subtitle = "Set the direction you want your future to move. Everything can be refined later.",
        momentTypeCode = "FUTURE_GOAL",
        activateLabel = "Activate Future Building →",
        footerTagline = "Private by default · Change anytime",
        defaultPreferences = mapOf(
            "building" to "Career growth",
            "today" to "Making progress",
            "primaryValue" to "Freedom",
            "valueGrowth" to "Selected",
            "valueSecurity" to "Selected",
            "futureFeel" to "Hopeful",
            "focusHorizon" to "12 months",
            "progressRhythm" to "Weekly",
            "mainFriction" to "Lack of time",
            "supportStyle" to "Daily progress",
            "momentumDriver" to "Daily progress",
            "habit2" to "",
            "remindWeekly" to true,
            "learningCheckIn" to "Enabled",
            "focusTimeCheckIn" to "Enabled",
            "reviewCadence" to "Every week",
            "profile" to "Future Builder",
        ),
    )

    private val lifestyle = PersonalSetupCatalogEntry(
        defaultTitle = "My intentional lifestyle",
        subtitle = "Shape the way you want to live, feel, and spend your time. Everything can be refined later.",
        momentTypeCode = "LIFESTYLE",
        activateLabel = "Activate My Lifestyle →",
        footerTagline = "Private by default · Change anytime",
        defaultPreferences = mapOf(
            "vision" to "Balanced & energized",
            "current" to "Good, with room to grow",
            "primaryPriority" to "Health & energy",
            "workLifeBalance" to "Selected",
            "homeEnvironment" to "Selected",
            "healthEnergy" to "Strong and consistent",
            "socialRhythm" to "A few times a week",
            "homeRhythm" to "Calm & organized",
            "topPriority" to "Health & energy",
            "neglectedArea" to "Rest & recovery",
            "habit" to "Movement routine",
            "habit2" to "",
            "desiredFeeling" to "Balanced",
            "remindWeekly" to true,
            "energyCheckIn" to "Enabled",
            "balanceCheckIn" to "Enabled",
            "reviewCadence" to "Every week",
            "profile" to "Lifestyle Curator",
        ),
    )

    private val relationships = PersonalSetupCatalogEntry(
        defaultTitle = "My relationships",
        subtitle = "Be intentional about the people and connections that matter. Everything can be refined later.",
        momentTypeCode = "RELATIONSHIP_CONNECTION",
        activateLabel = "Activate My Relationships →",
        footerTagline = "Private by default · Change anytime",
        defaultPreferences = mapOf(
            "relationshipFocus" to "Deeper connection",
            "current" to "Connected, but busy",
            "primaryCircle" to "Family",
            "partnerFamily" to "Selected",
            "friendsCommunity" to "Selected",
            "timeTogether" to "Meaningful moments",
            "reachOutRhythm" to "Every week",
            "communicationStyle" to "Thoughtful check-ins",
            "strongestConnection" to "Family",
            "needsInvestment" to "Friends",
            "ritual" to "Weekly check-in",
            "habit2" to "",
            "desiredFeeling" to "Close & supported",
            "remindWeekly" to true,
            "connectionCheckIn" to "Enabled",
            "reachOutReminder" to "Enabled",
            "reviewCadence" to "Every week",
            "profile" to "Connection Builder",
        ),
    )

    fun forKind(kind: PersonalSetupKind): PersonalSetupCatalogEntry = when (kind) {
        PersonalSetupKind.LIFE_OPERATIONS -> lifeOps
        PersonalSetupKind.FUTURE_BUILDING -> future
        PersonalSetupKind.LIFESTYLE -> lifestyle
        PersonalSetupKind.RELATIONSHIPS -> relationships
    }

    fun allowedKeys(kind: PersonalSetupKind): Set<String> =
        forKind(kind).defaultPreferences.keys
}
