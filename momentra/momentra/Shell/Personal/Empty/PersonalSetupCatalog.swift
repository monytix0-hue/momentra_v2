import Foundation

struct PersonalSetupFieldSpec {
    let key: String
    let label: String
    let multiSelect: Bool
    let options: [String]
    var kind: SetupFieldKind = .chips
}

struct PersonalSetupCatalogEntry {
    let defaultTitle: String
    let subtitle: String
    let momentTypeCode: String
    let activateLabel: String
    let footerTagline: String
    let missionTitle: String?
    let missionBody: String?
    let previewTitle: String
    let fields: [PersonalSetupFieldSpec]
    let defaultPreferences: [String: Any]
    let emojiByOption: [String: [String: String]]
}

/// Defaults + allowedKeys must match backend PERSONAL_SETUP_CATALOG (Figma field model).
enum PersonalSetupCatalog {
    private static let lifeOps = PersonalSetupCatalogEntry(
        defaultTitle: "My life operations rhythm",
        subtitle: "Create a calmer operating system for everyday life. Everything can be refined later.",
        momentTypeCode: "LIFE_RHYTHM",
        activateLabel: "Activate Life Operations →",
        footerTagline: "Private by default · Change anytime",
        missionTitle: nil,
        missionBody: nil,
        previewTitle: "Life summary",
        fields: [],
        defaultPreferences: [
            "lifeFocus": "Daily balance",
            "currentRhythm": "Busy but manageable",
            "primaryNeed": "More breathing room",
            "healthEnergy": "Selected",
            "timeBalance": "Selected",
            "shapesFocus": "Daily balance",
            "shapesRhythm": "Busy but manageable",
            "mainPressure": "Too many commitments",
            "recoveryWindow": "Weekends",
            "checkInRhythm": "Weekly",
            "helpfulSupport": "Clear routines",
            "recoveryStyle": "Quiet time",
            "habit": "Morning routine",
            "habit2": "",
            "currentEnergy": "Steady",
            "reflectWeekly": true,
            "stressCheckIn": "Enabled",
            "recoveryCheckIn": "Enabled",
            "reviewCadence": "Every week",
            "profile": "STRUCTURE SEEKER",
        ],
        emojiByOption: [:]
    )

    private static let future = PersonalSetupCatalogEntry(
        defaultTitle: "My future building",
        subtitle: "Set the direction you want your future to move. Everything can be refined later.",
        momentTypeCode: "FUTURE_GOAL",
        activateLabel: "Activate Future Building →",
        footerTagline: "Private by default · Change anytime",
        missionTitle: nil,
        missionBody: nil,
        previewTitle: "Future summary",
        fields: [],
        defaultPreferences: [
            "building": "Career growth",
            "today": "Making progress",
            "primaryValue": "Freedom",
            "valueGrowth": "Selected",
            "valueSecurity": "Selected",
            "futureFeel": "Hopeful",
            "focusHorizon": "12 months",
            "progressRhythm": "Weekly",
            "mainFriction": "Lack of time",
            "supportStyle": "Daily progress",
            "momentumDriver": "Daily progress",
            "habit2": "",
            "remindWeekly": true,
            "learningCheckIn": "Enabled",
            "focusTimeCheckIn": "Enabled",
            "reviewCadence": "Every week",
            "profile": "Future Builder",
        ],
        emojiByOption: [:]
    )

    private static let lifestyle = PersonalSetupCatalogEntry(
        defaultTitle: "My intentional lifestyle",
        subtitle: "Shape the way you want to live, feel, and spend your time. Everything can be refined later.",
        momentTypeCode: "LIFESTYLE",
        activateLabel: "Activate My Lifestyle →",
        footerTagline: "Private by default · Change anytime",
        missionTitle: nil,
        missionBody: nil,
        previewTitle: "Lifestyle summary",
        fields: [],
        defaultPreferences: [
            "vision": "Balanced & energized",
            "current": "Good, with room to grow",
            "primaryPriority": "Health & energy",
            "workLifeBalance": "Selected",
            "homeEnvironment": "Selected",
            "healthEnergy": "Strong and consistent",
            "socialRhythm": "A few times a week",
            "homeRhythm": "Calm & organized",
            "topPriority": "Health & energy",
            "neglectedArea": "Rest & recovery",
            "habit": "Movement routine",
            "habit2": "",
            "desiredFeeling": "Balanced",
            "remindWeekly": true,
            "energyCheckIn": "Enabled",
            "balanceCheckIn": "Enabled",
            "reviewCadence": "Every week",
            "profile": "Lifestyle Curator",
        ],
        emojiByOption: [:]
    )

    private static let relationships = PersonalSetupCatalogEntry(
        defaultTitle: "My relationships",
        subtitle: "Be intentional about the people and connections that matter. Everything can be refined later.",
        momentTypeCode: "RELATIONSHIP_CONNECTION",
        activateLabel: "Activate My Relationships →",
        footerTagline: "Private by default · Change anytime",
        missionTitle: nil,
        missionBody: nil,
        previewTitle: "Relationship summary",
        fields: [],
        defaultPreferences: [
            "relationshipFocus": "Deeper connection",
            "current": "Connected, but busy",
            "primaryCircle": "Family",
            "partnerFamily": "Selected",
            "friendsCommunity": "Selected",
            "timeTogether": "Meaningful moments",
            "reachOutRhythm": "Every week",
            "communicationStyle": "Thoughtful check-ins",
            "strongestConnection": "Family",
            "needsInvestment": "Friends",
            "ritual": "Weekly check-in",
            "habit2": "",
            "desiredFeeling": "Close & supported",
            "remindWeekly": true,
            "connectionCheckIn": "Enabled",
            "reachOutReminder": "Enabled",
            "reviewCadence": "Every week",
            "profile": "Connection Builder",
        ],
        emojiByOption: [:]
    )

    static func forKind(_ kind: PersonalSetupKind) -> PersonalSetupCatalogEntry {
        switch kind {
        case .lifeOperations: return lifeOps
        case .futureBuilding: return future
        case .lifestyle: return lifestyle
        case .relationships: return relationships
        }
    }

    static func chipOptions(for field: PersonalSetupFieldSpec, catalog: PersonalSetupCatalogEntry) -> [SetupChipOption] {
        field.options.map { option in
            SetupChipOption(
                value: option,
                label: option,
                emoji: catalog.emojiByOption[field.key]?[option]
            )
        }
    }

    static var allowedPreferenceKeys: (_ kind: PersonalSetupKind) -> Set<String> {
        { kind in Set(forKind(kind).defaultPreferences.keys) }
    }
}
