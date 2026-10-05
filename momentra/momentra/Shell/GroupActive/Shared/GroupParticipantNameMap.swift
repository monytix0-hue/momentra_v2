import Foundation

/// Case-insensitive participantId → displayName map for Group Pulse / Finance rows.
enum GroupParticipantNameMap {
    static func build(_ participants: [APIClient.GroupParticipantPayload]) -> [String: String] {
        var map: [String: String] = [:]
        for p in participants {
            let key = p.participantId.lowercased()
            if map[key] != nil { continue }
            map[key] = PersonLabel.personName(p.displayName)
        }
        return map
    }

    /// Prefer finance position displayName, then the participants map, then "Member".
    static func resolve(
        participantId: String,
        positionDisplayName: String?,
        nameById: [String: String]
    ) -> String {
        let named = PersonLabel.personName(positionDisplayName, fallback: "")
        if !named.isEmpty { return named }
        if let name = nameById[participantId.lowercased()], !name.isEmpty {
            return name
        }
        return PersonLabel.personName(nil)
    }
}
