import SwiftUI

enum GroupExperienceFamily {
    case sharedGeneric
    case wedding
    case houseParty
    case officeOuting
    case giftPool
    case groupPurchase
    case sharedAsset
    case customPurchase
    case flatmates
    case familyHousehold
    case coLiving
    case customLiving

    static func forTypeCode(_ momentTypeCode: String?) -> GroupExperienceFamily {
        let code = (momentTypeCode ?? "").uppercased()
        if code.contains("WEDDING") { return .wedding }
        if code.contains("HOUSE_PARTY") { return .houseParty }
        if code.contains("OFFICE_OUTING") { return .officeOuting }
        if code.contains("GIFT_POOL") { return .giftPool }
        if code.contains("GROUP_PURCHASE") { return .groupPurchase }
        if code.contains("SHARED_ASSET") { return .sharedAsset }
        if code.contains("COMMUNITY_PURCHASE") { return .customPurchase }
        if code.contains("FLATMATES") { return .flatmates }
        if code.contains("FAMILY_HOUSEHOLD") { return .familyHousehold }
        if code.contains("CO_LIVING") { return .coLiving }
        if code.contains("COMMUNITY_LIVING") { return .customLiving }
        if code.contains("SHARED_LIVING") { return .flatmates }
        if code.contains("SHARED_RENTAL") { return .flatmates }
        if code == "CUSTOM" { return .customLiving }
        return .sharedGeneric
    }

    var isWedding: Bool { self == .wedding }
    var isHouseParty: Bool { self == .houseParty }
    var isOfficeOuting: Bool { self == .officeOuting }
    var isThemedExperience: Bool { self == .houseParty || self == .officeOuting }
    var isThemedPurchase: Bool {
        switch self {
        case .giftPool, .groupPurchase, .sharedAsset, .customPurchase: return true
        default: return false
        }
    }
    var isThemedLiving: Bool {
        switch self {
        case .flatmates, .familyHousehold, .coLiving, .customLiving: return true
        default: return false
        }
    }

    var invitePeopleSubtitle: String {
        if isThemedLiving { return "Share a link or add someone to this household" }
        if isThemedPurchase { return "Share a link or add someone to this purchase" }
        return "Share a link or add someone to this trip"
    }
}
