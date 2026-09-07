import Foundation

/// Case-insensitive participantId → displayName map for Group Pulse / Finance rows.
enum GroupParticipantNameMap {
    static func build(_ participants: [APIClient.GroupParticipantPayload]) -> [String: String] {
        var map: [String: String] = [:]
        for p in participants {
            let key = p.participantId.lowercased()
            if map[key] != nil { continue }
            if let name = p.displayName?.trimmingCharacters(in: .whitespacesAndNewlines), !name.isEmpty {
                map[key] = name
            } else {
                map[key] = String(p.participantId.prefix(8))
            }
        }
        return map
    }

    /// Prefer finance position displayName, then participants map, then UUID prefix.
    static func resolve(
        participantId: String,
        positionDisplayName: String?,
        nameById: [String: String]
    ) -> String {
        if let name = positionDisplayName?.trimmingCharacters(in: .whitespacesAndNewlines), !name.isEmpty {
            return name
        }
        if let name = nameById[participantId.lowercased()], !name.isEmpty {
            return name
        }
        return String(participantId.prefix(8))
    }
}
