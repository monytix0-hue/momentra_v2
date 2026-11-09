import SwiftUI

/// Resolves and hosts the existing family setup wizard for Manage Moment → Edit setup.
enum EditMomentSetupTarget: Identifiable, Equatable {
    case personal(PersonalSetupSystem)
    case business(BusinessSetupKind)
    case groupExperience
    case groupPurchase
    case groupLiving

    var id: String {
        switch self {
        case .personal(let s): return "personal:\(s.rawValue)"
        case .business(let k): return "business:\(k.rawValue)"
        case .groupExperience: return "group:experience"
        case .groupPurchase: return "group:purchase"
        case .groupLiving: return "group:living"
        }
    }

    static func resolve(
        context: AppContextKind,
        momentTypeCode: String?
    ) -> EditMomentSetupTarget? {
        switch context {
        case .personal:
            switch PersonalPulseFamily.forTypeCode(momentTypeCode) {
            case .lifeOperations: return .personal(.lifeOperations)
            case .futureBuilding: return .personal(.futureBuilding)
            case .lifestyle: return .personal(.lifestyle)
            case .relationships: return .personal(.relationships)
            }
        case .business:
            let code = (momentTypeCode ?? "").uppercased()
            if code == "CLIENT_CAMPAIGN" || code == "OWN_BUSINESS_CAMPAIGN" || code.contains("MARKETING") {
                return .business(.marketing)
            }
            if code == "TRADING_WHOLESALE" || code.contains("TRADING") {
                return .business(.trading)
            }
            if code == "DISTRIBUTION_DEALERSHIP" || code.contains("DISTRIBUTION") {
                return .business(.distribution)
            }
            if code == "RETAIL_STORE" || code.contains("RETAIL") {
                return .business(.retail)
            }
            if code == "RESTAURANT_FOOD" || code.contains("RESTAURANT") {
                return .business(.restaurant)
            }
            if code == "CONSUMER_SERVICE"
                || (code.contains("SERVICE") && !code.contains("CAMPAIGN")) {
                return .business(.service)
            }
            return nil
        case .group:
            let family = GroupExperienceFamily.forTypeCode(momentTypeCode)
            if family.isThemedPurchase { return .groupPurchase }
            if family.isThemedLiving { return .groupLiving }
            return .groupExperience
        case .circle:
            return nil
        }
    }
}

struct EditMomentSetupHost: View {
    let target: EditMomentSetupTarget
    let momentId: String
    let momentTitle: String
    let momentTypeCode: String?
    let companyId: String?
    @ObservedObject var createModel: MomentCreateModel
    var onClose: () -> Void
    var onSaved: () -> Void

    var body: some View {
        switch target {
        case .personal(let system):
            PersonalSetupWizardView(
                system: system,
                editingMomentId: momentId,
                initialTitle: momentTitle,
                onBack: onClose,
                onCreated: { _, _, _, _ in onSaved() }
            )
        case .business(let kind):
            BusinessSetupWizardView(
                kind: kind,
                createModel: createModel,
                companyId: companyId,
                editingMomentId: momentId,
                initialTitle: momentTitle,
                onClose: onClose,
                onCreated: { _ in onSaved() }
            )
        case .groupExperience:
            GroupExperienceSetupView(
                createModel: createModel,
                onBack: onClose,
                onCreated: { _ in onSaved() },
                editingMomentId: momentId,
                initialTitle: momentTitle,
                initialTypeCode: momentTypeCode
            )
        case .groupPurchase:
            GroupPurchaseSetupView(
                createModel: createModel,
                onBack: onClose,
                onCreated: { _ in onSaved() },
                editingMomentId: momentId,
                initialTitle: momentTitle,
                initialTypeCode: momentTypeCode
            )
        case .groupLiving:
            GroupLivingSetupView(
                createModel: createModel,
                onBack: onClose,
                onCreated: { _ in onSaved() },
                editingMomentId: momentId,
                initialTitle: momentTitle,
                initialTypeCode: momentTypeCode
            )
        }
    }

}
