import CryptoKit
import LocalAuthentication
import SwiftUI

/// Local App Lock — PIN never leaves the device (Keychain).
enum AppLockStore {
    private static let service = "com.momentra.applock"
    private static let pinAccount = "pin_hash"
    private static let bioKey = "momentra_biometrics_enabled"
    private static let autoLockKey = "momentra_auto_lock_sec"

    static var isPinEnabled: Bool {
        loadPinHash() != nil
    }

    static var biometricsEnabled: Bool {
        get { UserDefaults.standard.bool(forKey: bioKey) }
        set { UserDefaults.standard.set(newValue, forKey: bioKey) }
    }

    static var autoLockSeconds: Int {
        get {
            let v = UserDefaults.standard.integer(forKey: autoLockKey)
            return v == 0 ? 60 : v
        }
        set { UserDefaults.standard.set(newValue, forKey: autoLockKey) }
    }

    static func setPin(_ pin: String) throws {
        guard pin.count >= 4, pin.count <= 8, pin.allSatisfy(\.isNumber) else {
            throw NSError(domain: "AppLock", code: 1, userInfo: [NSLocalizedDescriptionKey: "PIN must be 4–8 digits"])
        }
        let hash = sha256(pin)
        let data = Data(hash.utf8)
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: pinAccount,
        ]
        SecItemDelete(query as CFDictionary)
        var add = query
        add[kSecValueData as String] = data
        add[kSecAttrAccessible as String] = kSecAttrAccessibleWhenUnlockedThisDeviceOnly
        let status = SecItemAdd(add as CFDictionary, nil)
        guard status == errSecSuccess else {
            throw NSError(domain: "AppLock", code: Int(status), userInfo: [NSLocalizedDescriptionKey: "Could not store PIN"])
        }
    }

    static func verifyPin(_ pin: String) -> Bool {
        guard let stored = loadPinHash() else { return false }
        return stored == sha256(pin)
    }

    static func clearPin() {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: pinAccount,
        ]
        SecItemDelete(query as CFDictionary)
        biometricsEnabled = false
    }

    private static func loadPinHash() -> String? {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: pinAccount,
            kSecReturnData as String: true,
            kSecMatchLimit as String: kSecMatchLimitOne,
        ]
        var item: CFTypeRef?
        let status = SecItemCopyMatching(query as CFDictionary, &item)
        guard status == errSecSuccess, let data = item as? Data else { return nil }
        return String(data: data, encoding: .utf8)
    }

    /// SHA-256 of the PIN, stored only in the Keychain. Never networked.
    private static func sha256(_ value: String) -> String {
        let digest = SHA256.hash(data: Data(value.utf8))
        return digest.map { String(format: "%02x", $0) }.joined()
    }
}

/// Shoulder-surfing mask for money text. Not an authorization or encryption control.
enum BalanceMask {
    static var hideBalances: Bool {
        UserDefaults.standard.bool(forKey: "momentra_hide_balances")
    }

    static func mask(_ text: String) -> String {
        hideBalances ? "••••" : text
    }
}

enum AppLockSession {
    static var unlocked = false
    static var lastBackground: Date?
}
