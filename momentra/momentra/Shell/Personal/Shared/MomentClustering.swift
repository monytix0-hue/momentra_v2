import Foundation

/// M5A — group co-occurring activities into richer Moment cards when evidence supports it.
/// Presentation stays `MomentCardModel`; singles remain 1:1.
enum MomentClustering {
    /// Max gap between activities in one cluster.
    private static let windowSeconds: TimeInterval = 2 * 60 * 60

    static func cards(
        from activities: [APIClient.ActivityItemPayload],
        family: PersonalPulseFamily
    ) -> [MomentCardModel] {
        let visible = activities
            .filter { PersonalActivityTimelineDerived.isVisible($0) }
            .sorted { lhs, rhs in
                (MomentCardTime.parse(lhs.occurredAt) ?? .distantPast)
                    > (MomentCardTime.parse(rhs.occurredAt) ?? .distantPast)
            }
        guard !visible.isEmpty else { return [] }

        var clusters: [[APIClient.ActivityItemPayload]] = []
        var current: [APIClient.ActivityItemPayload] = []

        for activity in visible {
            if current.isEmpty {
                current = [activity]
                continue
            }
            if canJoin(activity, into: current) {
                current.append(activity)
            } else {
                clusters.append(current)
                current = [activity]
            }
        }
        if !current.isEmpty { clusters.append(current) }

        return clusters.map { group in
            if group.count == 1 {
                return MomentCardModel.from(activity: group[0], family: family)
            }
            return fromCluster(group, family: family)
        }
    }

    private static func canJoin(
        _ candidate: APIClient.ActivityItemPayload,
        into cluster: [APIClient.ActivityItemPayload]
    ) -> Bool {
        guard let anchor = cluster.first,
              let t0 = MomentCardTime.parse(anchor.occurredAt),
              let t1 = MomentCardTime.parse(candidate.occurredAt)
        else { return false }
        guard abs(t0.timeIntervalSince(t1)) <= windowSeconds else { return false }
        guard Calendar.current.isDate(t0, inSameDayAs: t1) else { return false }

        let codes = Set((cluster + [candidate]).map { $0.activityCode.uppercased() })
        let hasSpend = codes.contains { $0.contains("EXPENSE") }
        let hasSocial = codes.contains { $0.contains("CONNECTION") || $0.contains("SHARED") || $0.contains("SUPPORT") }
        let hasMood = codes.contains { $0.contains("MOOD") }
        let hasExperience = codes.contains { $0.contains("EXPERIENCE") || $0.contains("WELLBEING") }
        let hasRecovery = codes.contains { $0.contains("RECOVERY") }

        // Complementary personal evidence — never cluster unrelated same-type spam.
        if hasSpend && (hasSocial || hasMood || hasExperience) { return true }
        if hasSocial && (hasMood || hasExperience) { return true }
        if hasRecovery && hasMood { return true }

        // Same merchant spend within window.
        if hasSpend {
            let merchants = (cluster + [candidate]).compactMap {
                $0.activityPayload?.merchantName?.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
            }.filter { !$0.isEmpty }
            if Set(merchants).count == 1, merchants.count >= 2 { return true }
        }
        return false
    }

    private static func fromCluster(
        _ group: [APIClient.ActivityItemPayload],
        family: PersonalPulseFamily
    ) -> MomentCardModel {
        let primary = group.first(where: { PersonalActivityTimelineDerived.isExpense($0) })
            ?? group.first(where: { $0.activityCode.uppercased().contains("CONNECTION") })
            ?? group.first(where: { $0.activityCode.uppercased().contains("EXPERIENCE") })
            ?? group[0]

        let codes = group.map { $0.activityCode.uppercased() }
        let hasSocial = codes.contains { $0.contains("CONNECTION") || $0.contains("SHARED") }
        let hasMood = codes.contains { $0.contains("MOOD") }
        let hasExperience = codes.contains { $0.contains("EXPERIENCE") }

        let baseTitle = PersonalActivityTimelineDerived.displayTitle(primary)
        let title: String = {
            if hasSocial, PersonalActivityTimelineDerived.isExpense(primary) {
                return "\(baseTitle) with people"
            }
            if hasExperience, PersonalActivityTimelineDerived.isExpense(primary) {
                return baseTitle
            }
            if group.count >= 3 {
                return baseTitle
            }
            return baseTitle
        }()

        let subtitleParts: [String] = {
            var parts: [String] = []
            if hasSocial { parts.append("Connection") }
            if hasMood {
                if let mood = group.first(where: { $0.activityCode.uppercased().contains("MOOD") }) {
                    if let chip = PersonalActivityTimelineDerived.feelingsChip(mood) {
                        parts.append(chip)
                    } else {
                        parts.append("Mood")
                    }
                }
            }
            if hasExperience { parts.append("Experience") }
            if parts.isEmpty {
                parts.append("\(group.count) linked logs")
            }
            return parts
        }()

        let amount = group.compactMap { PersonalActivityTimelineDerived.amountLabel($0) }.first
        let sourceIds: [String] = group.map { activity in
            activity.activityPayload?.activityId
                ?? activity.activityPayload?.expenseId
                ?? activity.activityPayload?.incomeId
                ?? activity.id
        }

        let earliest = group.min { a, b in
            (MomentCardTime.parse(a.occurredAt) ?? .distantFuture)
                < (MomentCardTime.parse(b.occurredAt) ?? .distantFuture)
        } ?? primary

        return MomentCardModel(
            id: "cluster:\(sourceIds.sorted().joined(separator: "|"))",
            title: title,
            subtitle: subtitleParts.joined(separator: " · "),
            contextLabel: PersonalActivityTimelineDerived.categoryChip(primary)
                ?? (hasSocial ? "People" : nil),
            family: family,
            amountLabel: amount,
            signalLabel: hasMood ? "Mood" : (hasSocial ? "Presence" : nil),
            peopleCount: hasSocial ? max(2, group.filter { $0.activityCode.uppercased().contains("CONNECTION") }.count + 1) : nil,
            occurredAt: earliest.occurredAt,
            sourceIconKind: MomentSourceIconKind.from(activityCode: primary.activityCode),
            activityCode: primary.activityCode,
            sourceActivityIds: sourceIds,
            tapDestination: .activityDetail,
            mediaUrls: []
        )
    }
}
