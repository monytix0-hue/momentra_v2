import Foundation

enum OnboardingPrefs {
    private static let key = "momentra_onboarding_seen"
    private static let consentKey = "momentra_consent_gate_seen"

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
}
