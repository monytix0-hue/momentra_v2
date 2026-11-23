import Foundation

enum OnboardingPrefs {
    private static let key = "momentra_onboarding_seen"
    private static let consentKey = "momentra_consent_gate_seen"
    private static let ageGateKeyLegacy13 = "momentra_age_gate_accepted_13plus"
    private static let ageGateKey18 = "momentra_age_gate_accepted_18plus"

    static var isSeen: Bool {
        UserDefaults.standard.bool(forKey: key)
    }

    static func markSeen() {
        UserDefaults.standard.set(true, forKey: key)
    }

    static var isConsentGateSeen: Bool {
        UserDefaults.standard.bool(forKey: consentKey)
    }

    static func markConsentGateSeen() {
        UserDefaults.standard.set(true, forKey: consentKey)
    }

    /// DPDP s.9: product gate is 18+. Legacy 13+ flag alone is insufficient.
    static var isAgeGateAccepted: Bool {
        UserDefaults.standard.bool(forKey: ageGateKey18)
    }

    static func markAgeGateAccepted() {
        UserDefaults.standard.set(true, forKey: ageGateKey18)
        UserDefaults.standard.removeObject(forKey: ageGateKeyLegacy13)
    }

    static func clearAgeGateAccepted() {
        UserDefaults.standard.removeObject(forKey: ageGateKey18)
        UserDefaults.standard.removeObject(forKey: ageGateKeyLegacy13)
    }
}
