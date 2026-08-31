import Foundation

/// Caches last Momentra identity keyed by Firebase UID.
/// Never stores Firebase UID as Momentra userId.
enum MomentraIdentityCache {
    private static let defaults = UserDefaults.standard

    private static func userIdKey(_ firebaseUid: String) -> String { "momentra_identity_user_id_\(firebaseUid)" }
    private static func displayNameKey(_ firebaseUid: String) -> String { "momentra_identity_display_\(firebaseUid)" }
    private static func emailKey(_ firebaseUid: String) -> String { "momentra_identity_email_\(firebaseUid)" }

    static func save(firebaseUid: String, userId: String, displayName: String?, email: String?) {
        guard !userId.isEmpty, userId != firebaseUid else { return }
        defaults.set(userId, forKey: userIdKey(firebaseUid))
        defaults.set(displayName, forKey: displayNameKey(firebaseUid))
        defaults.set(email, forKey: emailKey(firebaseUid))
    }

    static func load(firebaseUid: String) -> ShellIdentity? {
        guard let userId = defaults.string(forKey: userIdKey(firebaseUid)),
              !userId.isEmpty,
              userId != firebaseUid
        else { return nil }
        return ShellIdentity(
            userId: userId,
            displayName: defaults.string(forKey: displayNameKey(firebaseUid)),
            email: defaults.string(forKey: emailKey(firebaseUid)),
            firebaseUid: firebaseUid
        )
    }

    static func clear(firebaseUid: String?) {
        guard let firebaseUid, !firebaseUid.isEmpty else { return }
        defaults.removeObject(forKey: userIdKey(firebaseUid))
        defaults.removeObject(forKey: displayNameKey(firebaseUid))
        defaults.removeObject(forKey: emailKey(firebaseUid))
    }
}
