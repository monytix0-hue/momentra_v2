import Foundation

/// Unified Personal helpers — prefer Life Ops, family target pick, missing setup systems.
enum PersonalUnified {
    static func activeMoments(_ moments: [MomentSummary]) -> [MomentSummary] {
        moments.filter(\.isActiveStatus)
    }

    static func resolvePreferred(
        moments: [MomentSummary],
        currentSelectedId: String?
    ) -> MomentSummary? {
        let active = activeMoments(moments)
        if active.isEmpty { return nil }
        if let lifeOps = active.first(where: {
            PersonalPulseFamily.forTypeCode($0.momentTypeCode) == .lifeOperations
        }) {
            return lifeOps
        }
        if let current = active.first(where: { $0.momentId == currentSelectedId }) {
            return current
        }
        return active.first
    }

    static func resolveFamilyTarget(
        moments: [MomentSummary],
        family: PersonalPulseFamily,
        currentSelectedId: String?
    ) -> MomentSummary? {
        let inFamily = activeMoments(moments).filter {
            PersonalPulseFamily.forTypeCode($0.momentTypeCode) == family
        }
        if inFamily.isEmpty { return nil }
        if let current = inFamily.first(where: { $0.momentId == currentSelectedId }) {
            return current
        }
        return inFamily.first
    }

    static func presentFamilies(_ moments: [MomentSummary]) -> Set<PersonalPulseFamily> {
        Set(activeMoments(moments).map { PersonalPulseFamily.forTypeCode($0.momentTypeCode) })
    }

    static func missingSetupSystems(_ moments: [MomentSummary]) -> [PersonalSetupSystem] {
        let present = presentFamilies(moments)
        return PersonalSetupSystem.allCases.filter { !present.contains($0.pulseFamily) }
    }
}
