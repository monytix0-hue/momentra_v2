import SwiftUI

/**
 Figma Own Business Campaign hub (MKT-OWN-PULSE-v12 · Moments · Life · Memory · QA hub).
 Presentation only — binds to GET /operating + /widgets; no client finance math.
 Keep in lockstep with `OwnMarketingHub.kt` on Android.
 */
enum OwnMarketingHub {
    static let typeCode = "OWN_BUSINESS_CAMPAIGN"
    static let tagline = "Plan. Launch. Measure."

    /// Pulse Figma order: Needs Attention → Target → Payment → Finance → Activity.
    private static let pulseOrder = ["P04", "P01", "P03", "P02", "P05"]
    private static let momentsOrder = ["M01", "M02", "M03", "M04", "MKT-RESULTS", "MKT-ATTRIBUTION"]
    private static let lifeOrder = ["L01", "L02", "L03", "L04"]
    private static let memoryOrder = ["ME01", "ME02", "ME03", "ME04"]

    private static let titles: [String: String] = [
        "P04": "Needs Attention",
        "P01": "Target & Budget",
        "P03": "Payment Readiness",
        "P02": "Finance",
        "P05": "Activity",
        "M01": "Campaign / Business Moment",
        "M02": "People",
        "M03": "Channels & Network",
        "M04": "Open Commitments",
        "MKT-RESULTS": "Campaign Results",
        "MKT-ATTRIBUTION": "Attribution",
        "L01": "Current Campaign",
        "L02": "Channel Activity",
        "L03": "Money Moving",
        "L04": "People Today",
        "ME01": "Monthly Memory",
        "ME02": "Money Memory",
        "ME03": "People & Activity",
        "ME04": "Patterns & Decisions",
    ]

    private static let anCodes: [String: String] = [
        "P04": "AN-P01",
        "P01": "AN-P02",
        "P03": "AN-P03",
        "P02": "AN-P04",
        "P05": "AN-P05",
        "M01": "AN-M01",
        "M02": "AN-M02",
        "M03": "AN-M03",
        "M04": "AN-M04",
        "L01": "AN-L01",
        "L02": "AN-L02",
        "L03": "AN-L03",
        "L04": "AN-L04",
        "ME01": "AN-ME01",
        "ME02": "AN-ME02",
        "ME03": "AN-ME03",
        "ME04": "AN-ME04",
    ]

    private static let pulseGroupAn: [String: String] = [
        "ATTENTION": "AN-P01",
        "TARGET_BUDGET": "AN-P02",
        "PAYMENT_READINESS": "AN-P03",
        "FINANCE": "AN-P04",
        "ACTIVITY": "AN-P05",
    ]

    static func isOwn(_ typeCode: String?) -> Bool {
        typeCode?.trimmingCharacters(in: .whitespacesAndNewlines).uppercased() == Self.typeCode
    }

    static func order(for screen: String) -> [String]? {
        switch screen.uppercased() {
        case "PULSE": return pulseOrder
        case "MOMENTS": return momentsOrder
        case "LIFE": return lifeOrder
        case "MEMORY": return memoryOrder
        default: return nil
        }
    }

    static func displayTitle(widgetId: String, serverTitle: String) -> String {
        titles[widgetId] ?? (serverTitle.isEmpty ? widgetId : serverTitle)
    }

    static func displayCode(widgetId: String, serverDisplayCode: String?) -> String? {
        if let server = serverDisplayCode?.trimmingCharacters(in: .whitespacesAndNewlines), !server.isEmpty {
            return server
        }
        return anCodes[widgetId]
    }

    static func pulseGroupDisplayCode(code: String, server: String?) -> String? {
        if let server = server?.trimmingCharacters(in: .whitespacesAndNewlines), !server.isEmpty {
            return server
        }
        return pulseGroupAn[code.uppercased()]
    }

    static func sortWidgets(screen: String, widgets: [OperatingWidgetPayload]) -> [OperatingWidgetPayload] {
        guard let order = order(for: screen) else { return widgets }
        let rank = Dictionary(uniqueKeysWithValues: order.enumerated().map { ($0.element, $0.offset) })
        return widgets.sorted {
            let left = rank[$0.widgetId] ?? Int.max
            let right = rank[$1.widgetId] ?? Int.max
            if left != right { return left < right }
            return $0.widgetId < $1.widgetId
        }
    }

    static func tabHeading(_ tab: OperatingHubTab) -> String {
        switch tab {
        case .moments: return "Campaigns & commitments"
        case .life: return "Today · live campaign"
        case .memory: return "Memory & patterns"
        default: return "Pulse"
        }
    }

    static func buildHeroStats(_ model: OperatingProjectionPayload?) -> String {
        guard let model else { return "" }
        var parts: [String] = []
        if let target = model.target, target.configured == true {
            let metric = target.metric
                .replacingOccurrences(of: "_", with: " ")
                .lowercased()
            let titled = metric.prefix(1).uppercased() + metric.dropFirst()
            parts.append("\(titled) \(target.progress) / \(target.value)")
        }
        if let budget = model.budget, budget.configured == true, !budget.amount.isEmpty {
            if let amount = Double(budget.amount), amount > 0, let spent = Double(budget.spent) {
                let pct = ((spent / amount) * 1000).rounded() / 10.0
                parts.append("Budget use \(pct)%")
            } else {
                parts.append("Budget \(budget.amount)")
            }
        }
        if let pacing = model.marketing?.pacing, !pacing.isEmpty {
            parts.append(pacing)
        }
        return parts.joined(separator: " · ")
    }
}

// MARK: - Headers

struct OwnMarketingHeroHeader: View {
    let projection: OperatingProjectionPayload?
    let momentTitle: String?
    let palette: HubPalette

    private var title: String {
        projection?.title.flatMap { $0.isEmpty ? nil : $0 }
            ?? momentTitle
            ?? projection?.subtypeLabel
            ?? "Own Business Campaign"
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("\(title) Pulse")
                .font(.system(size: 24, weight: .bold))
                .foregroundStyle(palette.text)

            let owner = projection?.role?
                .replacingOccurrences(of: "_", with: " ")
                .trimmingCharacters(in: .whitespacesAndNewlines)
            let party = projection?.partyName?.trimmingCharacters(in: .whitespacesAndNewlines)
            let subtitle = [owner.map { "Owner \($0)" }, party]
                .compactMap { $0.flatMap { $0.isEmpty ? nil : $0 } }
                .joined(separator: " · ")
            if !subtitle.isEmpty {
                Text(subtitle)
                    .font(.system(size: 12, weight: .medium))
                    .foregroundStyle(BusinessTokens.Colors.marketingOwnAccentSoft)
            }

            FlowLayout(spacing: 6) {
                OwnChip(label: "Own Business Campaign", selected: true)
                if let campaign = projection?.title, !campaign.isEmpty, campaign != title {
                    OwnChip(label: campaign)
                }
                let period = [projection?.startDate.map { String($0.prefix(7)) },
                              projection?.endDate.map { String($0.prefix(7)) }]
                    .compactMap { $0 }
                    .filter { !$0.isEmpty }
                let periodText = Array(Set(period)).sorted().joined(separator: "–")
                let currency = projection?.currency?.trimmingCharacters(in: .whitespacesAndNewlines)
                let meta = [periodText.isEmpty ? nil : periodText, currency.flatMap { $0.isEmpty ? nil : $0 }]
                    .compactMap { $0 }
                    .joined(separator: " · ")
                if !meta.isEmpty {
                    OwnChip(label: meta)
                }
            }

            let tagline = projection?.tagline.flatMap { $0.isEmpty ? nil : $0 } ?? OwnMarketingHub.tagline
            let heroStats = OwnMarketingHub.buildHeroStats(projection)
            VStack(alignment: .leading, spacing: 6) {
                Text("\(title.uppercased()) · OWN BUSINESS")
                    .font(.system(size: 11, weight: .bold))
                    .foregroundStyle(Color(hex: "#F5D0FE"))
                Text(tagline)
                    .font(.system(size: 22, weight: .bold))
                    .foregroundStyle(.white)
                if !heroStats.isEmpty {
                    Text(heroStats)
                        .font(.system(size: 12))
                        .foregroundStyle(BusinessTokens.Colors.marketingOwnLabel)
                }
            }
            .padding(20)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(
                LinearGradient(
                    colors: [
                        BusinessTokens.Colors.marketingOwnHeroStart,
                        BusinessTokens.Colors.marketingOwnHeroEnd,
                    ],
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                )
            )
            .clipShape(RoundedRectangle(cornerRadius: 24))
        }
    }
}

struct OwnMarketingTabHeader: View {
    let tab: OperatingHubTab
    let projection: OperatingProjectionPayload?
    let momentTitle: String?

    private var heading: String {
        switch tab {
        case .moments: return "Moments"
        case .life: return "Life"
        case .memory: return "Memory"
        default: return "Pulse"
        }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(heading)
                .font(.system(size: 24, weight: .bold))
                .foregroundStyle(Color(hex: "#F8FAFC"))
            Text(OwnMarketingHub.tabHeading(tab))
                .font(.system(size: 12, weight: .medium))
                .foregroundStyle(BusinessTokens.Colors.marketingOwnAccentSoft)
            FlowLayout(spacing: 6) {
                OwnChip(label: "Own Business Campaign", selected: true)
                if let name = (projection?.title ?? momentTitle)?.trimmingCharacters(in: .whitespacesAndNewlines),
                   !name.isEmpty {
                    OwnChip(label: name)
                }
            }
        }
    }
}

private struct OwnChip: View {
    let label: String
    var selected: Bool = false

    var body: some View {
        Text(label)
            .font(.system(size: 12, weight: selected ? .bold : .medium))
            .foregroundStyle(selected ? Color.white : Color(hex: "#94A3B8"))
            .padding(.horizontal, 12)
            .padding(.vertical, 8)
            .background(selected ? BusinessTokens.Colors.marketingOwnAccent : Color(hex: "#334155"))
            .overlay(
                Capsule().stroke(
                    selected ? BusinessTokens.Colors.marketingOwnAccentSoft : Color(hex: "#475569"),
                    lineWidth: 1
                )
            )
            .clipShape(Capsule())
    }
}

// MARK: - Section cards

struct OwnMarketingWidgetCards: View {
    let widgets: [OperatingWidgetPayload]
    let financeVisible: Bool
    let palette: HubPalette
    var onOpenSources: (String) -> Void
    var onViewAll: (() -> Void)? = nil

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            ForEach(widgets) { w in
                let cov = WidgetCoveragePresentation.fromServer(w.coverage)
                let metricLine = (w.metrics ?? []).map { m -> String in
                    let mCov = WidgetCoveragePresentation.fromServer(m.coverage)
                    let valueText: String
                    if !mCov.showValue || m.valueString == nil {
                        valueText = mCov.label
                    } else {
                        valueText = "\(m.valueString!)\(m.unit.map { " \($0)" } ?? "")"
                    }
                    return "\(m.label ?? m.key ?? "metric"): \(valueText)"
                }.joined(separator: " · ")
                OwnSectionCard(
                    // Callers: OwnMarketing widgets ForEach (~L307). displayCode(widgetId:serverDisplayCode:).
                    // User: "Missing argument label 'widgetId:' in call"
                    displayCode: OwnMarketingHub.displayCode(widgetId: w.widgetId, serverDisplayCode: w.displayCode),
                    title: OwnMarketingHub.displayTitle(widgetId: w.widgetId, serverTitle: w.title ?? ""),
                    detail: (!financeVisible && w.financeOnly == true)
                        ? "Finance redacted for your role"
                        : (metricLine.isEmpty ? cov.label : metricLine),
                    why: w.why,
                    palette: palette,
                    onViewAll: w.widgetId == "P04" ? onViewAll : nil,
                    onClick: (cov.showSources || !(w.sourceEventIds ?? []).isEmpty)
                        ? { onOpenSources(w.widgetId) }
                        : nil
                )
            }
        }
    }
}

struct OwnMarketingPulseGroups: View {
    let groups: [OperatingPulseMetricGroupPayload]
    let palette: HubPalette
    var onViewAllAttention: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            ForEach(groups) { group in
                OwnSectionCard(
                    displayCode: OwnMarketingHub.pulseGroupDisplayCode(code: group.code, server: group.displayCode),
                    title: ownPulseGroupTitle(group.code),
                    metrics: (group.metrics ?? []).map { ($0.label.isEmpty ? $0.key : $0.label, $0.value) },
                    why: group.why,
                    palette: palette,
                    onViewAll: group.code.uppercased() == "ATTENTION" ? onViewAllAttention : nil
                )
            }
        }
    }
}

struct OwnMarketingBoardSections: View {
    let sections: [OperatingBoardSectionPayload]
    let heading: String
    let palette: HubPalette

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(heading)
                .font(.system(size: 15, weight: .medium))
                .foregroundStyle(palette.text)
            if sections.isEmpty {
                OwnSectionCard(
                    displayCode: nil,
                    title: "This campaign",
                    detail: "Nothing recorded yet.",
                    palette: palette
                )
            } else {
                ForEach(sections) { section in
                    OwnSectionCard(
                        displayCode: section.displayCode,
                        title: section.title,
                        detail: section.detail.isEmpty ? "Nothing recorded yet." : section.detail,
                        metrics: (section.metrics ?? []).map { ($0.label.isEmpty ? $0.key : $0.label, $0.value) },
                        rows: (section.rows ?? []).map { row in
                            let amount = row.amount.map { " · \($0)" } ?? ""
                            let badge = row.badge
                                ?? row.eventType.replacingOccurrences(of: "_", with: " ")
                            return (row.title + amount, badge)
                        },
                        why: section.why,
                        palette: palette
                    )
                }
            }
        }
    }
}

private func ownPulseGroupTitle(_ code: String) -> String {
    switch code.uppercased() {
    case "TARGET_BUDGET": return "Target & budget"
    case "FINANCE": return "Finance"
    case "PAYMENT_READINESS": return "Payment readiness"
    case "ATTENTION": return "Needs Attention"
    case "ACTIVITY": return "Activity"
    default: return code.replacingOccurrences(of: "_", with: " ")
    }
}

private struct OwnSectionCard: View {
    let displayCode: String?
    let title: String
    var detail: String? = nil
    var metrics: [(String, String)] = []
    var rows: [(String, String)] = []
    var why: String? = nil
    let palette: HubPalette
    var onViewAll: (() -> Void)? = nil
    var onClick: (() -> Void)? = nil

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(alignment: .center, spacing: 8) {
                HStack(spacing: 8) {
                    if let code = displayCode?.trimmingCharacters(in: .whitespacesAndNewlines), !code.isEmpty {
                        Text(code)
                            .font(.system(size: 10, weight: .bold))
                            .foregroundStyle(BusinessTokens.Colors.marketingOwnAccentSoft)
                    }
                    Text(title)
                        .font(.system(size: 16, weight: .bold))
                        .foregroundStyle(palette.text)
                }
                Spacer(minLength: 8)
                if let onViewAll {
                    Button("View all ›", action: onViewAll)
                        .font(.system(size: 12, weight: .bold))
                        .foregroundStyle(BusinessTokens.Colors.marketingOwnAccentSoftLink)
                }
            }
            if !metrics.isEmpty {
                FlowLayout(spacing: 8) {
                    ForEach(Array(metrics.enumerated()), id: \.offset) { _, pair in
                        VStack(alignment: .leading, spacing: 2) {
                            Text(pair.0)
                                .font(.system(size: 11))
                                .foregroundStyle(palette.muted)
                            Text(pair.1)
                                .font(.system(size: 14, weight: .semibold))
                                .foregroundStyle(palette.text)
                        }
                        .padding(.horizontal, 12)
                        .padding(.vertical, 10)
                        .background(BusinessTokens.Colors.marketingOwnInset)
                        .clipShape(RoundedRectangle(cornerRadius: 12))
                    }
                }
            }
            if let detail, !detail.isEmpty {
                Text(detail)
                    .font(.system(size: 12))
                    .foregroundStyle(palette.muted)
            }
            ForEach(Array(rows.enumerated()), id: \.offset) { _, row in
                HStack {
                    Text(row.0)
                        .font(.system(size: 13))
                        .foregroundStyle(palette.text)
                        .frame(maxWidth: .infinity, alignment: .leading)
                    Text(row.1)
                        .font(.system(size: 11))
                        .foregroundStyle(palette.accent)
                }
                .padding(.horizontal, 12)
                .padding(.vertical, 10)
                .background(BusinessTokens.Colors.marketingOwnInset)
                .clipShape(RoundedRectangle(cornerRadius: 12))
            }
            if let why, !why.isEmpty {
                Text(why)
                    .font(.system(size: 12))
                    .foregroundStyle(palette.muted)
            }
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(palette.card)
        .overlay(RoundedRectangle(cornerRadius: 20).stroke(palette.border, lineWidth: 1))
        .clipShape(RoundedRectangle(cornerRadius: 20))
        .contentShape(RoundedRectangle(cornerRadius: 20))
        .onTapGesture {
            onClick?()
        }
    }
}

// MARK: - Quick Adds

struct OwnMarketingQuickAddHub: View {
    let projection: OperatingProjectionPayload?
    let palette: HubPalette
    @Binding var selectedCode: String
    var onSubmit: (String, String, String?) async -> Void

    @State private var titleText = ""
    @State private var amountText = ""
    @State private var showSavedToast = false

    private var writeAdds: [OperatingQuickAddPayload] {
        projection?.quickAdds ?? []
    }

    private var hub: OperatingQuickAddHubPayload? {
        projection?.quickAddHub
    }

    private var selectedWrite: OperatingQuickAddPayload? {
        if let match = writeAdds.first(where: { $0.code.caseInsensitiveCompare(selectedCode) == .orderedSame }) {
            return match
        }
        let tiles = hub?.groups?.flatMap { $0.tiles ?? [] } ?? []
        if let tile = tiles.first(where: {
            $0.code.caseInsensitiveCompare(selectedCode) == .orderedSame && ($0.kind ?? "WRITE").uppercased() == "WRITE"
        }) {
            return writeAdds.first(where: { $0.code == tile.code })
        }
        return nil
    }

    private var needsAmount: Bool {
        if selectedWrite?.needsAmount == true { return true }
        let tiles = hub?.groups?.flatMap { $0.tiles ?? [] } ?? []
        return tiles.first(where: { $0.code.caseInsensitiveCompare(selectedCode) == .orderedSame })?.needsAmount == true
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            if showSavedToast {
                OwnQuickAddSavedToast { showSavedToast = false }
            }

            VStack(alignment: .leading, spacing: 10) {
                Text("QUICK ADDS · v5")
                    .font(.system(size: 11, weight: .bold))
                    .foregroundStyle(BusinessTokens.Colors.marketingOwnAccentSoft)
                Text("Quick Adds")
                    .font(.system(size: 22, weight: .bold))
                    .foregroundStyle(palette.text)
                Text(projection?.tagline ?? OwnMarketingHub.tagline)
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundStyle(palette.accent)
                let meta = [
                    projection?.partyName.flatMap { $0.isEmpty ? nil : $0 },
                    projection?.title.flatMap { $0.isEmpty ? nil : $0 },
                    "Own",
                    projection?.currency.flatMap { $0.isEmpty ? nil : $0 },
                ].compactMap { $0 }.joined(separator: " · ")
                if !meta.isEmpty {
                    Text(meta)
                        .font(.system(size: 12))
                        .foregroundStyle(palette.muted)
                }
                Button {
                    selectedCode = "ACTIVITY"
                } label: {
                    Text("Add Activity")
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundStyle(palette.onAccent)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(.horizontal, 16)
                        .padding(.vertical, 14)
                        .background(palette.accent)
                        .clipShape(RoundedRectangle(cornerRadius: 14))
                }
            }
            .padding(16)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(palette.card)
            .overlay(RoundedRectangle(cornerRadius: 20).stroke(palette.border, lineWidth: 1))
            .clipShape(RoundedRectangle(cornerRadius: 20))

            if let hub, let groups = hub.groups, !groups.isEmpty {
                ForEach(groups) { group in
                    Text(group.title)
                        .font(.system(size: 12, weight: .semibold))
                        .foregroundStyle(palette.muted)
                    FlowLayout(spacing: 10) {
                        ForEach(group.tiles ?? []) { tile in
                            OwnHubTile(
                                tile: tile,
                                selected: selectedCode.caseInsensitiveCompare(tile.code) == .orderedSame
                                    || (tile.mapsTo.map { selectedCode.caseInsensitiveCompare($0) == .orderedSame } ?? false),
                                palette: palette
                            ) {
                                if tile.chromeOnly == true, let mapsTo = tile.mapsTo, !mapsTo.isEmpty {
                                    selectedCode = mapsTo
                                } else if (tile.kind ?? "WRITE").uppercased() == "WRITE" {
                                    selectedCode = tile.code
                                } else if let mapsTo = tile.mapsTo {
                                    selectedCode = mapsTo
                                }
                            }
                        }
                    }
                }
            } else {
                FlowLayout(spacing: 10) {
                    ForEach(writeAdds) { add in
                        let on = add.code.caseInsensitiveCompare(selectedCode) == .orderedSame
                        Button {
                            selectedCode = add.code
                        } label: {
                            Text(add.label)
                                .font(.system(size: 12, weight: .bold))
                                .foregroundStyle(on ? palette.accent : palette.text)
                                .padding(.horizontal, 14)
                                .padding(.vertical, 14)
                                .background(on ? palette.accent.opacity(0.18) : palette.card)
                                .overlay(
                                    RoundedRectangle(cornerRadius: 16)
                                        .stroke(on ? palette.accent : palette.border, lineWidth: 1)
                                )
                                .clipShape(RoundedRectangle(cornerRadius: 16))
                        }
                    }
                }
            }

            VStack(alignment: .leading, spacing: 6) {
                Text("Why these tiles save differently")
                    .font(.system(size: 12, weight: .bold))
                    .foregroundStyle(BusinessTokens.Colors.marketingOwnAccentSoft)
                Text("• Attributed sales ≠ ledger revenue\n• Results are measured, never booked as income\n• Invoice & Collection stay Client-only")
                    .font(.system(size: 12))
                    .foregroundStyle(Color(hex: "#FCD34D"))
            }
            .padding(14)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(BusinessTokens.Colors.marketingOwnInset.opacity(0.55))
            .overlay(
                RoundedRectangle(cornerRadius: 16)
                    .stroke(BusinessTokens.Colors.marketingOwnAccentSoft.opacity(0.35), lineWidth: 1)
            )
            .clipShape(RoundedRectangle(cornerRadius: 16))

            if selectedWrite != nil || writeAdds.contains(where: { $0.code.caseInsensitiveCompare(selectedCode) == .orderedSame }) {
                let writeCode = selectedWrite?.code ?? selectedCode
                TextField("Title", text: $titleText)
                    .textFieldStyle(.plain)
                    .padding(12)
                    .background(palette.card)
                    .overlay(RoundedRectangle(cornerRadius: 12).stroke(palette.border, lineWidth: 1))
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                    .foregroundStyle(palette.text)
                if needsAmount {
                    TextField("Amount", text: $amountText)
                        .textFieldStyle(.plain)
                        .padding(12)
                        .background(palette.card)
                        .overlay(RoundedRectangle(cornerRadius: 12).stroke(palette.border, lineWidth: 1))
                        .clipShape(RoundedRectangle(cornerRadius: 12))
                        .foregroundStyle(palette.text)
                }
                let canSubmit = !writeCode.isEmpty && !titleText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
                Button {
                    let title = titleText.trimmingCharacters(in: .whitespacesAndNewlines)
                    let amount = amountText.trimmingCharacters(in: .whitespacesAndNewlines)
                    Task {
                        await onSubmit(writeCode, title, amount.isEmpty ? nil : amount)
                        titleText = ""
                        amountText = ""
                        showSavedToast = true
                    }
                } label: {
                    Text("Save")
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundStyle(palette.onAccent)
                        .padding(.horizontal, 16)
                        .padding(.vertical, 12)
                        .background(canSubmit ? palette.accent : palette.muted.opacity(0.35))
                        .clipShape(RoundedRectangle(cornerRadius: 12))
                }
                .disabled(!canSubmit)
            }
        }
    }
}

private struct OwnHubTile: View {
    let tile: OperatingQuickAddHubTilePayload
    let selected: Bool
    let palette: HubPalette
    let onClick: () -> Void

    var body: some View {
        Button(action: onClick) {
            VStack(alignment: .leading, spacing: 4) {
                Text(tile.label)
                    .font(.system(size: 12, weight: .bold))
                    .foregroundStyle(selected ? palette.accent : palette.text)
                if let subtitle = tile.subtitle, !subtitle.isEmpty {
                    Text(subtitle)
                        .font(.system(size: 10))
                        .foregroundStyle(palette.muted)
                }
                if tile.chromeOnly == true {
                    Text("Open")
                        .font(.system(size: 10))
                        .foregroundStyle(palette.muted)
                }
            }
            .frame(width: 108, alignment: .leading)
            .padding(.horizontal, 12)
            .padding(.vertical, 14)
            .background(selected ? palette.accent.opacity(0.18) : palette.card)
            .overlay(
                RoundedRectangle(cornerRadius: 16)
                    .stroke(selected ? palette.accent : palette.border, lineWidth: 1)
            )
            .clipShape(RoundedRectangle(cornerRadius: 16))
        }
    }
}

struct OwnGenerateBusinessStoryButton: View {
    let palette: HubPalette
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            VStack(alignment: .leading, spacing: 4) {
                Text("Generate Business Story")
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundStyle(.white)
                Text("Eight pages from recorded activity")
                    .font(.system(size: 12))
                    .foregroundStyle(.white.opacity(0.85))
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(16)
            .background(
                LinearGradient(
                    colors: [Color(hex: "#4F46E5"), Color(hex: "#7C3AED")],
                    startPoint: .leading,
                    endPoint: .trailing
                )
            )
            .clipShape(RoundedRectangle(cornerRadius: 16))
        }
    }
}

struct OwnQuickAddSavedToast: View {
    var onDismiss: () -> Void

    @State private var scale: CGFloat = 0.92

    var body: some View {
        HStack(spacing: 12) {
            ZStack {
                Circle()
                    .fill(Color(hex: "#10B981").opacity(0.22))
                    .frame(width: 28, height: 28)
                Circle()
                    .stroke(Color(hex: "#10B981"), lineWidth: 1.5)
                    .frame(width: 28, height: 28)
                Text("✓")
                    .font(.system(size: 14, weight: .bold))
                    .foregroundStyle(Color(hex: "#10B981"))
            }
            .scaleEffect(scale)

            VStack(alignment: .leading, spacing: 2) {
                Text("Saved · posted once")
                    .font(.system(size: 14, weight: .bold))
                    .foregroundStyle(.white)
                Text("Pulse · Moments · Life · Memory updated")
                    .font(.system(size: 12))
                    .foregroundStyle(Color(hex: "#94A3B8"))
            }
            .frame(maxWidth: .infinity, alignment: .leading)

            Button("View", action: onDismiss)
                .font(.system(size: 13, weight: .semibold))
                .foregroundStyle(Color(hex: "#10B981"))
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 12)
        .background(Color(hex: "#16120E").opacity(0.95))
        .overlay(
            RoundedRectangle(cornerRadius: 16)
                .stroke(Color(hex: "#10B981").opacity(0.45), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .onAppear {
            withAnimation(.easeInOut(duration: 0.7).repeatForever(autoreverses: true)) {
                scale = 1.06
            }
            Task {
                try? await Task.sleep(nanoseconds: 2_750_000_000)
                onDismiss()
            }
        }
    }
}
