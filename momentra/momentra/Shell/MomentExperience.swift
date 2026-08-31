import Foundation

/// Presentation-level Moment experience — derived from real list reads, not a backend state machine.
enum MomentExperienceKind: Equatable {
    case loading
    case error
    case firstMoment
    case active
    case betweenMoments
    case pausedOnly
}

struct MomentSummary: Equatable, Identifiable {
    let momentId: String
    let title: String
    let status: String
    let momentTypeCode: String?
    /// Present for BUSINESS bootstrap moments — Company→Moment scoping.
    let companyId: String?
    var id: String { momentId }

    init(
        momentId: String,
        title: String,
        status: String,
        momentTypeCode: String? = nil,
        companyId: String? = nil
    ) {
        self.momentId = momentId
        self.title = title
        self.status = status
        self.momentTypeCode = momentTypeCode
        self.companyId = companyId
    }

    var isActiveStatus: Bool {
        status.caseInsensitiveCompare("ACTIVE") == .orderedSame ||
            status.caseInsensitiveCompare("DRAFT") == .orderedSame
    }

    var isPausedStatus: Bool {
        status.caseInsensitiveCompare("PAUSED") == .orderedSame
    }

    var isHistoricalStatus: Bool {
        status.caseInsensitiveCompare("COMPLETED") == .orderedSame ||
            status.caseInsensitiveCompare("CANCELLED") == .orderedSame ||
            status.caseInsensitiveCompare("ARCHIVED") == .orderedSame ||
            status.caseInsensitiveCompare("DELETED") == .orderedSame
    }
}

func resolveMomentExperience(_ moments: [MomentSummary]) -> MomentExperienceKind {
    if moments.isEmpty { return .firstMoment }
    if moments.contains(where: \.isActiveStatus) { return .active }
    let hasPaused = moments.contains(where: \.isPausedStatus)
    let hasHistory = moments.contains { $0.isHistoricalStatus || !$0.isPausedStatus }
    if hasPaused && !hasHistory { return .pausedOnly }
    return .betweenMoments
}

func recentHistoryMoments(_ moments: [MomentSummary], limit: Int = 5) -> [MomentSummary] {
    Array(
        moments
            .filter {
                !$0.isActiveStatus &&
                    $0.status.caseInsensitiveCompare("ARCHIVED") != .orderedSame
            }
            .prefix(limit)
    )
}

func activeMomentCount(_ moments: [MomentSummary]) -> Int {
    moments.filter(\.isActiveStatus).count
}
