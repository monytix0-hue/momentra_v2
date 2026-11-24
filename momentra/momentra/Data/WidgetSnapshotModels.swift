import Foundation

/// Codable payloads shared by `WidgetSnapshotManager` (app) and the widget-extension providers.
///
/// Field-for-field identical to what each provider decodes; the App Group file each one maps to is
/// documented on the matching `WidgetSnapshotManager.write*`/`read*` pair.

// MARK: - Snapshot Models

struct OutboxWidgetSnapshot: Codable {
    var pendingCount: Int
    var failedCount: Int
    var failedMessage: String?
    var totalCount: Int
    var lastUpdated: TimeInterval
}

struct PersonalPulseWidgetSnapshot: Codable {
    let recoveryScore: String?
    let activeMomentCount: Int
    let attentionCount: Int?
    let moodState: String?
    let updatedAt: TimeInterval
}

struct PersonalLifeWidgetSnapshot: Codable {
    let score: Int?
    let scoreMax: Int?
    let statusLabel: String?
    let trendLabel: String?
    let areaScores: [LifeAreaScore]?
    let updatedAt: TimeInterval

    struct LifeAreaScore: Codable {
        let code: String
        let label: String
        let score: Int?
        let color: String
    }
}

struct GroupPulseWidgetSnapshot: Codable {
    let participantCount: Int?
    let attentionCount: Int?
    let openTaskCount: Int?
    let title: String?
    let updatedAt: TimeInterval
}

struct GroupFinanceWidgetSnapshot: Codable {
    let expenseTotal: String?
    let budgetTotal: String?
    let contributionTotal: String?
    let settledTotal: String?
    let outstandingTotal: String?
    let currencyCode: String
    let viewerNetPosition: String?
    let updatedAt: TimeInterval
}

struct GroupMemoryWidgetSnapshot: Codable {
    let memoryCount: Int?
    let recentTitles: [String]
    let updatedAt: TimeInterval
}

struct GroupAttendanceWidgetSnapshot: Codable {
    let confirmed: Int
    let pending: Int
    let declined: Int
    let updatedAt: TimeInterval
}

struct GroupRosterWidgetSnapshot: Codable {
    let members: [RosterMember]
    let updatedAt: TimeInterval

    struct RosterMember: Codable {
        let displayName: String?
        let membershipType: String?
    }
}

struct ActivityFeedWidgetSnapshot: Codable {
    let items: [ActivityRow]
    let updatedAt: TimeInterval

    struct ActivityRow: Codable {
        let title: String
        let subtitle: String?
        let occurredAt: String
        let activityCode: String
    }
}

struct BusinessPulseWidgetSnapshot: Codable {
    let runwayMonths: String?
    let financialHealthScore: String?
    let activeMomentCount: Int?
    let attentionCount: Int?
    let updatedAt: TimeInterval
}

struct BusinessFinanceWidgetSnapshot: Codable {
    let expenseTotal: String?
    let revenueTotal: String?
    let invoiceOutstandingTotal: String?
    let currencyCode: String
    let updatedAt: TimeInterval
}

struct BusinessLifeWidgetSnapshot: Codable {
    let activeModuleCount: Int?
    let activeMomentCount: Int?
    let runwayMonths: String?
    let financialHealthScore: String?
    let attentionCount: Int?
    let updatedAt: TimeInterval
}

struct BusinessTimelineWidgetSnapshot: Codable {
    let items: [TimelineRow]
    let kpis: TimelineKpis
    let updatedAt: TimeInterval

    struct TimelineRow: Codable {
        let eventType: String
        let title: String
        let occurredAt: String
    }

    struct TimelineKpis: Codable {
        let spendEvents: Int?
        let issueCount: Int?
        let openIssueCount: Int?
        let updateCount: Int?
    }
}

struct BusinessWeeklyWidgetSnapshot: Codable {
    let title: String?
    let period: String?
    let generatedAt: String?
    let sections: [Section]
    let updatedAt: TimeInterval

    struct Section: Codable {
        let heading: String?
        let items: [String]?
    }
}

struct BusinessRosterWidgetSnapshot: Codable {
    let members: [RosterMember]
    let updatedAt: TimeInterval

    struct RosterMember: Codable {
        let displayName: String?
        let membershipType: String?
    }
}

struct KhataWidgetSnapshot: Codable {
    let parties: [Party]
    let updatedAt: TimeInterval

    struct Party: Codable {
        let name: String
        let balanceDue: String
        let currencyCode: String?
        let overdue: Bool
    }
}
