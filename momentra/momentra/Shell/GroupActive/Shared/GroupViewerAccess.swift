import Foundation

/// Viewer (`OBSERVER` / legacy `VIEWER`) is read-only for Quick Add creates and expense entry.
enum GroupViewerAccess {
    static func isReadOnlyRole(_ roleCode: String?) -> Bool {
        let upper = (roleCode ?? "").trimmingCharacters(in: .whitespacesAndNewlines).uppercased()
        return upper == "OBSERVER" || upper == "VIEWER"
    }

    static func isViewer(
        participants: [APIClient.GroupParticipantPayload],
        currentUserId: String?
    ) -> Bool {
        guard let currentUserId, !currentUserId.isEmpty else { return false }
        guard let me = participants.first(where: { $0.userId == currentUserId }) else { return false }
        return isReadOnlyRole(me.roleCode)
    }
}
