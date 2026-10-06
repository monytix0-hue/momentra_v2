import Foundation

enum OnboardingPrefs {
    private static let key = "momentra_onboarding_seen"
    private static let consentKey = "momentra_consent_gate_seen"
    private static let ageGateKey = "momentra_age_gate_accepted_13plus"

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

    /// Local confirmation that the user is 13+ (Privacy Policy under-13).
    static var isAgeGateAccepted: Bool {
        UserDefaults.standard.bool(forKey: ageGateKey)
    }

    static func markAgeGateAccepted() {
        UserDefaults.standard.set(true, forKey: ageGateKey)
    }
}
