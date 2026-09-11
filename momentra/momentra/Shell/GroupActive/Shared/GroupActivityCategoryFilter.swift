import Foundation

/// Category chips for Group “All activity”, sourced from that moment’s Quick Add hub names.
enum GroupActivityCategoryFilter {
    struct FilterChip: Identifiable, Equatable {
        let id: String
        let label: String
        let emoji: String
    }

    static let allId = "All"

    static func chips(for momentTypeCode: String?) -> [FilterChip] {
        let all = FilterChip(id: allId, label: "All", emoji: "📋")
        let family = GroupExperienceFamily.forTypeCode(momentTypeCode)
        let hub: [FilterChip]
        if family.isWedding {
            hub = WeddingQuickAddKind.allCases.map { kind in
                FilterChip(id: kind.rawValue, label: weddingHubLabel(kind), emoji: kind.emoji)
            }
        } else if family.isThemedExperience {
            hub = ExperienceQuickAddKind.allCases.map { kind in
                FilterChip(id: kind.rawValue, label: kind.hubLabel, emoji: kind.emoji)
            }
        } else if family.isThemedPurchase {
            hub = PurchaseQuickAddKind.allCases.map { kind in
                FilterChip(id: kind.rawValue, label: kind.label, emoji: kind.emoji)
            }
        } else if family.isThemedLiving {
            hub = LivingQuickAddKind.allCases.map { kind in
                FilterChip(id: kind.rawValue, label: kind.label, emoji: kind.emoji)
            }
        } else {
            hub = tripHubChips()
        }
        return [all] + hub
    }

    static func matches(_ item: APIClient.ActivityItemPayload, chipId: String) -> Bool {
        matches(activityCode: item.activityCode, chipId: chipId)
    }

    static func matches(activityCode: String, chipId: String) -> Bool {
        if chipId == allId || chipId.isEmpty { return true }
        return activityBelongs(to: chipId, activityCode: activityCode)
    }

    // MARK: - Private

    private static func tripHubChips() -> [FilterChip] {
        let emojiById: [String: String] = [
            "expense": "💳",
            "planning": "📋",
            "checklist": "✅",
            "budget": "💰",
            "booking": "🧳",
            "poll": "📊",
            "memory": "📷",
            "update": "📢",
            "contribution": "🎁",
            "invite": "👤",
        ]
        let labels = Dictionary(
            uniqueKeysWithValues: GroupActionRegistry.figmaHubTiles(hasActiveMoment: true)
                .map { ($0.tileId, $0.label) }
        )
        return GroupActionRegistry.tripHubTileIds.compactMap { id in
            guard let label = labels[id] else { return nil }
            return FilterChip(id: id, label: label, emoji: emojiById[id] ?? "📌")
        }
    }

    private static func weddingHubLabel(_ kind: WeddingQuickAddKind) -> String {
        switch kind {
        case .participant: return "Invite"
        case .planning: return "Planning"
        case .checklist: return "Checklist"
        case .expense: return "Expense"
        case .budget: return "Budget"
        case .contribution: return "Contribution"
        case .settle: return "Settle"
        case .vendor: return "Vendor"
        case .attendance: return "Attendance"
        case .update: return "Update"
        case .poll: return "Poll"
        case .memory: return "Memory"
        }
    }

    /// Substring match on `activityCode`, ordered so more-specific tokens win when callers iterate chips.
    private static func activityBelongs(to chipId: String, activityCode: String) -> Bool {
        let upper = activityCode.uppercased()
        switch chipId {
        case "expense":
            return upper.contains("EXPENSE")
        case "contribution":
            return upper.contains("CONTRIBUTION")
                || (upper.contains("CONTRIB") && !upper.contains("CONTRIBUTOR"))
        case "settle":
            return upper.contains("SETTLE")
        case "planning":
            return upper.contains("PLANNING")
        case "checklist":
            return upper.contains("PLANNING")
        case "task":
            return upper.contains("TASK") && !upper.contains("PLANNING")
        case "booking":
            return upper.contains("BOOKING")
        case "poll":
            return upper.contains("POLL")
        case "memory":
            return upper.contains("MEMORY")
        case "update":
            return upper.contains("UPDATE")
        case "invite", "participant", "resident", "contributor":
            return upper.contains("MEMBER")
                || upper.contains("RESIDENT")
                || upper.contains("PARTICIPANT")
                || upper.contains("INVITE")
                || upper.contains("CONTRIBUTOR")
        case "vendor":
            return upper.contains("VENDOR")
        case "attendance":
            return upper.contains("ATTENDANCE")
        case "purchaseItem", "purchase":
            return upper.contains("PURCHASE")
        case "delivery":
            return upper.contains("DELIVERY")
        case "ownership":
            return upper.contains("OWNERSHIP") || upper.contains("TRANSFER")
        case "asset":
            return upper.contains("ASSET")
        case "maintenance":
            return upper.contains("MAINTENANCE")
        case "rule":
            return upper.contains("RULE")
        case "budget":
            return upper.contains("BUDGET")
        default:
            return false
        }
    }
}
