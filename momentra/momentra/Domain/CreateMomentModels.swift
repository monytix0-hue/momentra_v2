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
    var status: String? = nil
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
        var budgetAmount: String? = nil
        var budgetCurrencyCode: String? = nil
        var destinationText: String? = nil
        var places: [PlaceBlock]? = nil
        var budgets: [BudgetBlock]? = nil
        var multiCurrencyEnabled: Bool? = nil
        var splitStyle: String? = nil
        var primaryGoal: String? = nil
        var reminderPreferences: [String: Bool]? = nil
        var setupPreferences: [String: JSONEncodableValue]? = nil

        struct PlaceBlock: Encodable {
            let label: String
            var startAt: String? = nil
            var endAt: String? = nil
        }

        struct BudgetBlock: Encodable {
            let currencyCode: String
            let amount: String
            var isPrimary: Bool? = nil
        }
    }
}

struct GroupSetupPrefill: Decodable {
    let momentId: String?
    let title: String?
    let status: String?
    let momentTypeCode: String?
    let startAt: String?
    let endAt: String?
    let places: [GroupSetupPlacePrefill]?
    let budgets: [GroupSetupBudgetPrefill]?
    let multiCurrencyEnabled: Bool?
    let splitStyle: String?
    let primaryGoal: String?
    let destinationText: String?
    let version: Int?
}

struct GroupSetupPlacePrefill: Decodable {
    let placeId: String?
    let label: String?
    let startAt: String?
    let endAt: String?
    let sortOrder: Int?
}

struct GroupSetupBudgetPrefill: Decodable {
    let currencyCode: String
    let amount: String
    let isPrimary: Bool?
}

struct DomainSetupPrefill: Decodable {
    let momentId: String
    let title: String
    let status: String
    let domainCode: String
    let momentTypeCode: String
    let systemCode: String?
    let familyCode: String?
    let preferences: [String: AnyDecodable]?
    let companyId: String?
    let version: Int
}

struct DiscardMomentDraftResult: Decodable {
    let momentId: String
    let discarded: Bool
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
            case let dict as [String: String]:
                try container.encode(dict)
            case let dict as [String: Bool]:
                try container.encode(dict)
            case let dict as [String: Any]:
                try container.encode(JSONEncodableValue.map(dict))
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
    let operatingRole: String?
    let expiresAt: String?

    init(
        inviteId: String,
        inviteCode: String,
        invitePath: String,
        inviteUrl: String,
        status: String,
        title: String,
        companyId: String,
        membershipType: String,
        operatingRole: String? = nil,
        expiresAt: String?
    ) {
        self.inviteId = inviteId
        self.inviteCode = inviteCode
        self.invitePath = invitePath
        self.inviteUrl = inviteUrl
        self.status = status
        self.title = title
        self.companyId = companyId
        self.membershipType = membershipType
        self.operatingRole = operatingRole
        self.expiresAt = expiresAt
    }

    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        inviteId = try c.decode(String.self, forKey: .inviteId)
        inviteCode = try c.decode(String.self, forKey: .inviteCode)
        invitePath = try c.decodeIfPresent(String.self, forKey: .invitePath) ?? ""
        inviteUrl = try c.decodeIfPresent(String.self, forKey: .inviteUrl) ?? ""
        status = try c.decodeIfPresent(String.self, forKey: .status) ?? "ACTIVE"
        title = try c.decodeIfPresent(String.self, forKey: .title) ?? ""
        companyId = try c.decodeIfPresent(String.self, forKey: .companyId) ?? ""
        membershipType = try c.decodeIfPresent(String.self, forKey: .membershipType) ?? "MEMBER"
        operatingRole = try c.decodeIfPresent(String.self, forKey: .operatingRole)
        expiresAt = try c.decodeIfPresent(String.self, forKey: .expiresAt)
    }

    private enum CodingKeys: String, CodingKey {
        case inviteId, inviteCode, invitePath, inviteUrl, status, title, companyId, membershipType, operatingRole, expiresAt
    }
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
