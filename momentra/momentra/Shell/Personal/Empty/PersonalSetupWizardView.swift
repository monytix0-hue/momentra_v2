import SwiftUI

enum PersonalSetupSystem: String, CaseIterable, Identifiable {
    case lifeOperations = "LIFE_OPERATIONS"
    case futureBuilding = "FUTURE_BUILDING"
    case lifestyle = "LIFESTYLE"
    case relationships = "RELATIONSHIPS"

    var id: String { rawValue }

    var momentTypeCode: String {
        switch self {
        case .lifeOperations: return "LIFE_RHYTHM"
        case .futureBuilding: return "FUTURE_GOAL"
        case .lifestyle: return "LIFESTYLE"
        case .relationships: return "RELATIONSHIP_CONNECTION"
        }
    }

    var defaultTitle: String {
        switch self {
        case .lifeOperations: return "My life operations rhythm"
        case .futureBuilding: return "My future building"
        case .lifestyle: return "My intentional lifestyle"
        case .relationships: return "My relationships"
        }
    }

    var label: String {
        switch self {
        case .lifeOperations: return "Life Operations"
        case .futureBuilding: return "Future Building"
        case .lifestyle: return "Lifestyle"
        case .relationships: return "Relationships"
        }
    }

    var setupTitle: String { "Set up \(label)" }

    var pulseFamily: PersonalPulseFamily {
        switch self {
        case .lifeOperations: return .lifeOperations
        case .futureBuilding: return .futureBuilding
        case .lifestyle: return .lifestyle
        case .relationships: return .relationships
        }
    }
}

/// Routes Create cards to Figma long-form setup sheets (not chip wizards).
struct PersonalSetupWizardView: View {
    let system: PersonalSetupSystem
    var editingMomentId: String? = nil
    var initialTitle: String? = nil
    var onBack: () -> Void
    var onCreated: (String, String, String?) -> Void

    var body: some View {
        switch system {
        case .lifeOperations:
            PersonalLifeOpsSetupView(
                editingMomentId: editingMomentId,
                initialTitle: initialTitle,
                onBack: onBack,
                onCreated: onCreated
            )
        case .futureBuilding:
            PersonalFutureSetupView(
                editingMomentId: editingMomentId,
                initialTitle: initialTitle,
                onBack: onBack,
                onCreated: onCreated
            )
        case .lifestyle:
            PersonalLifestyleSetupView(
                editingMomentId: editingMomentId,
                initialTitle: initialTitle,
                onBack: onBack,
                onCreated: onCreated
            )
        case .relationships:
            PersonalRelationshipsSetupView(
                editingMomentId: editingMomentId,
                initialTitle: initialTitle,
                onBack: onBack,
                onCreated: onCreated
            )
        }
    }
}
