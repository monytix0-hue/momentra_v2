import Foundation

/// Command result from POST /v1/moments — authoritative for post-create selection.
struct CreateMomentOutcome: Equatable {
    let momentId: String
    let title: String
    let domainCode: String
    let status: String
    let version: Int
    let momentTypeCode: String?
    let setupId: String?
    let projectionHints: [ProjectionHint]
}

struct ProjectionHint: Equatable {
    let projection: String
    let action: String
}

/// Alias used by create catalog / MomentCreateModel (same codes as PersonalSetupSystem).
typealias PersonalSetupKind = PersonalSetupSystem

struct CreateMomentParticipantInput: Encodable {
    let displayName: String
    let roleCode: String
    var email: String? = nil
    var phone: String? = nil
}

struct CreateMomentRequest: Encodable {
    let domainCode: String
    let momentTypeCode: String
    let title: String
    let description: String?
    let startAt: String?
    let endAt: String?
    let timezone: String
    var customTypeLabel: String? = nil
    let companyId: String?
    let participants: [CreateMomentParticipantInput]?
    var inviteCode: String? = nil
    let personalSetup: PersonalSetupBlock?
    let businessSetup: BusinessSetupBlock?
    var groupSetup: GroupSetupBlock? = nil

    struct PersonalSetupBlock: Encodable {
        let systemCode: String
        let preferences: [String: JSONEncodableValue]?
    }

    struct BusinessSetupBlock: Encodable {
        let familyCode: String
        let preferences: [String: JSONEncodableValue]?
    }

    struct GroupSetupBlock: Encodable {
        let budgetAmount: String
        let budgetCurrencyCode: String
        let destinationText: String?
        var reminderPreferences: [String: Bool]? = nil
    }
}

struct JSONEncodableValue: Encodable {
    private let encodeValue: (Encoder) throws -> Void

    init(_ value: Any) {
        encodeValue = { encoder in
            var container = encoder.singleValueContainer()
            switch value {
            case let string as String:
                try container.encode(string)
            case let bool as Bool:
                try container.encode(bool)
            case let numbers as [String]:
                try container.encode(numbers)
            case let numbers as [Any]:
                try container.encode(numbers.compactMap { $0 as? String })
            case let int as Int:
                try container.encode(int)
            case let double as Double:
                try container.encode(double)
            default:
                try container.encode(String(describing: value))
            }
        }
    }

    func encode(to encoder: Encoder) throws {
        try encodeValue(encoder)
    }

    static func map(_ preferences: [String: Any]) -> [String: JSONEncodableValue] {
        preferences.mapValues { JSONEncodableValue($0) }
    }
}

struct CreateMomentResult: Decodable {
    let momentId: String
    let title: String
    let domainCode: String
    let status: String
    let version: Int
    let momentTypeCode: String?
    let setupId: String?
}

struct ProjectionHintPayload: Decodable {
    let projection: String
    let action: String?
}

struct CreateMomentAPIResponse {
    let result: CreateMomentResult
    let projectionHints: [ProjectionHintPayload]
}

struct GroupInvite: Decodable {
    let inviteId: String
    let inviteCode: String
    let invitePath: String
    let inviteUrl: String
    let status: String
    let title: String
    let momentTypeCode: String
    let momentId: String?
}

struct RedeemGroupInviteResult: Decodable {
    let inviteCode: String
    let status: String
    let momentId: String?
    let participantId: String?
    let alreadyMember: Bool?
}

struct CompanyInvite: Decodable {
    let inviteId: String
    let inviteCode: String
    let invitePath: String
    let inviteUrl: String
    let status: String
    let title: String
    let companyId: String
    let membershipType: String
    let expiresAt: String?
}

struct RedeemCompanyInviteResult: Decodable {
    let inviteId: String?
    let inviteCode: String
    let status: String
    let companyId: String
    let membershipId: String?
    let membershipType: String?
    let alreadyMember: Bool?
}
