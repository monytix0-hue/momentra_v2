import SwiftUI

/// Trading TRD-SHEET palette — lockstep APK `TradeSheetTheme`.
struct OperatingSheetTheme {
    var accent: Color = Color(hex: "#10B981")
    var accentSoft: Color = Color(hex: "#6EE7B7")
    var canvas: Color = Color(hex: "#090D16")
    var surface: Color = Color(hex: "#151D2E")
    var inset: Color = Color(hex: "#1E293B")
    var border: Color = Color(hex: "#1E293B")
    var ink: Color = .white
    var body: Color = Color(hex: "#8B97AD")
    var warning: Color = Color(hex: "#F59E0B")
    var negative: Color = Color(hex: "#F87171")

    static let trading = OperatingSheetTheme()
}

/// Shared View-all bottom sheet chrome for Trading (Figma TRD-SHEET).
struct OperatingSheetFrame<Content: View>: View {
    let title: String
    let eyebrow: String
    let model: OperatingProjectionPayload
    let theme: OperatingSheetTheme
    let sheetCode: String
    let totalCount: Int
    let visibleCount: (String) -> Int
    var onDismiss: () -> Void
    var onFooterAction: ((String) -> Void)?
    @ViewBuilder var content: (String) -> Content

    @State private var period = OperatingSheetChrome.periodFilters[0]
    @State private var status: String = "All"

    private var filters: [String] { OperatingSheetChrome.statusFilters(sheetCode) }
    private var footerCta: String { OperatingSheetChrome.footerCta(sheetCode) }

    private var contextLine: String {
        let name = (model.title?.trimmingCharacters(in: .whitespacesAndNewlines)).flatMap { $0.isEmpty ? nil : $0 } ?? "Business"
        let month: String = {
            guard let raw = model.startDate?.prefix(7), raw.count == 7 else { return "This month" }
            let parts = raw.split(separator: "-")
            guard parts.count == 2, let y = Int(parts[0]), let m = Int(parts[1]), (1...12).contains(m) else {
                return "This month"
            }
            let names = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"]
            return "\(names[m - 1]) \(y)"
        }()
        return "\(name) · \(month) · full activity, newest first"
    }

    var body: some View {
        ZStack(alignment: .bottom) {
            Color.black.opacity(0.55)
                .ignoresSafeArea()
                .onTapGesture(perform: onDismiss)

            VStack(spacing: 0) {
                Capsule()
                    .fill(Color.white.opacity(0.2))
                    .frame(width: 40, height: 5)
                    .padding(.top, 12)

                VStack(alignment: .leading, spacing: 10) {
                    HStack(alignment: .center, spacing: 8) {
                        Text(title)
                            .font(.system(size: 24, weight: .semibold))
                            .foregroundStyle(theme.ink)
                            .frame(maxWidth: .infinity, alignment: .leading)
                        Button(action: onDismiss) {
                            Text("✕")
                                .font(.system(size: 16))
                                .foregroundStyle(theme.body)
                                .padding(.horizontal, 10)
                                .padding(.vertical, 6)
                        }
                    }
                    Text(contextLine)
                        .font(.system(size: 12))
                        .foregroundStyle(theme.body)
                    if !eyebrow.isEmpty {
                        Text(eyebrow)
                            .font(.system(size: 12))
                            .foregroundStyle(theme.body)
                    }
                    chipRow(OperatingSheetChrome.periodFilters, selected: period) { period = $0 }
                    chipRow(filters, selected: status) { status = $0 }
                }
                .padding(.horizontal, 17)
                .padding(.top, 10)

                ScrollView {
                    VStack(alignment: .leading, spacing: 12) {
                        content(status)
                    }
                    .padding(.horizontal, 17)
                    .padding(.top, 12)
                    .padding(.bottom, 16)
                }

                VStack(alignment: .leading, spacing: 8) {
                    Text("Showing \(visibleCount(status)) of \(totalCount) · tap a row for detail")
                        .font(.system(size: 12))
                        .foregroundStyle(theme.body)
                        .frame(maxWidth: .infinity, alignment: .leading)
                    Button {
                        onFooterAction?(OperatingSheetChrome.footerAction(sheetCode))
                        onDismiss()
                    } label: {
                        Text(footerCta)
                            .font(.system(size: 15, weight: .semibold))
                            .foregroundStyle(theme.canvas)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 14)
                            .background(theme.accent)
                            .clipShape(RoundedRectangle(cornerRadius: 14))
                    }
                }
                .padding(.horizontal, 17)
                .padding(.vertical, 12)
            }
            .frame(maxHeight: UIScreen.main.bounds.height * 0.82)
            .background(theme.surface)
            .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
        }
        .onAppear {
            status = filters.first ?? "All"
        }
    }

    private func chipRow(_ options: [String], selected: String, onSelect: @escaping (String) -> Void) -> some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(options, id: \.self) { option in
                    let on = option == selected
                    Button {
                        onSelect(option)
                    } label: {
                        Text(option)
                            .font(.system(size: 12, weight: .semibold))
                            .foregroundStyle(on ? theme.ink : theme.body)
                            .padding(.horizontal, 13)
                            .padding(.vertical, 7)
                            .background(on ? theme.accent.opacity(0.22) : theme.inset)
                            .overlay(
                                RoundedRectangle(cornerRadius: 999)
                                    .stroke(on ? theme.accent.opacity(0.55) : theme.border, lineWidth: 1)
                            )
                            .clipShape(Capsule())
                    }
                }
            }
        }
    }
}

struct TradingBoardSheet: View {
    let state: TradingSheetState
    let model: OperatingProjectionPayload
    var theme: OperatingSheetTheme = .trading
    var onDismiss: () -> Void
    var onFooterAction: ((String) -> Void)?

    private var spec: OperatingSheetSpec { state.spec }
    private var section: OperatingBoardSectionPayload? { model.sectionByCode(spec.code) }

    private var totalCount: Int {
        switch spec.code {
        case "ATTENTION": return model.attention?.count ?? 0
        case "PAYMENT_READINESS": return model.paymentReadiness?.count ?? 0
        case "ACTIVITY": return model.activity?.count ?? 0
        default:
            return section?.rows?.count
                ?? model.pulseMetrics?.first(where: { $0.code == spec.code })?.metrics?.count
                ?? 0
        }
    }

    var body: some View {
        OperatingSheetFrame(
            title: spec.title,
            eyebrow: spec.eyebrow,
            model: model,
            theme: theme,
            sheetCode: spec.code,
            totalCount: totalCount,
            visibleCount: { status in visibleCount(status: status) },
            onDismiss: onDismiss,
            onFooterAction: onFooterAction
        ) { status in
            if spec.pulseSurface {
                pulseBody(status: status)
            } else {
                boardBody(status: status)
            }
        }
    }

    @ViewBuilder
    private func pulseBody(status: String) -> some View {
        let group = model.pulseMetrics?.first(where: { $0.code == spec.code })
        metricTiles(group?.metrics ?? [])
        if let why = group?.why, !why.isEmpty {
            whyFootnote(why)
        }
        switch spec.code {
        case "ATTENTION":
            attentionRows(status: status)
        case "PAYMENT_READINESS":
            paymentRows(status: status)
        case "ACTIVITY":
            activityRows(status: status)
        default:
            EmptyView()
        }
    }

    @ViewBuilder
    private func boardBody(status: String) -> some View {
        if let section {
            metricTiles(section.metrics ?? [])
            if let why = section.why, !why.isEmpty {
                whyFootnote(why)
            }
            let rows = (section.rows ?? []).filter {
                OperatingSheetChrome.matchesStatus(status, badge: $0.badge)
            }
            if rows.isEmpty {
                emptyBlock(compact: true)
            } else {
                ForEach(rows) { row in
                    boardRow(
                        title: row.title.isEmpty ? row.eventType : row.title,
                        subtitle: row.subtitle ?? row.counterpartyName,
                        amount: row.amount,
                        badge: row.badge
                    )
                }
            }
        } else {
            emptyBlock(compact: false)
        }
    }

    @ViewBuilder
    private func attentionRows(status: String) -> some View {
        let rows = (model.attention ?? []).filter {
            OperatingSheetChrome.matchesStatus(status, badge: attentionBadge($0.severity))
        }
        if rows.isEmpty {
            emptyBlock(compact: true)
        } else {
            ForEach(rows) { item in
                boardRow(
                    title: item.title,
                    subtitle: item.counterpartyName,
                    amount: item.amount,
                    badge: attentionBadge(item.severity)
                )
            }
        }
    }

    @ViewBuilder
    private func paymentRows(status: String) -> some View {
        let rows = (model.paymentReadiness ?? []).filter {
            OperatingSheetChrome.matchesStatus(status, badge: paymentBadge($0.dueBucket))
        }
        if rows.isEmpty {
            emptyBlock(
                title: "Nothing awaiting settlement",
                body: "Open balances appear here as they are recorded.",
                compact: true
            )
        } else {
            ForEach(rows) { item in
                boardRow(
                    title: item.title.isEmpty ? (item.counterpartyName ?? "Open balance") : item.title,
                    subtitle: [
                        item.counterpartyName,
                        item.dueAt.map { "Due \($0.prefix(10))" },
                    ].compactMap { $0 }.filter { !$0.isEmpty }.joined(separator: " · "),
                    amount: item.amount,
                    badge: paymentBadge(item.dueBucket)
                )
            }
        }
    }

    @ViewBuilder
    private func activityRows(status: String) -> some View {
        let rows = (model.activity ?? []).filter {
            OperatingSheetChrome.matchesStatus(status, badge: $0.badge)
        }
        if rows.isEmpty {
            emptyBlock(compact: true)
        } else {
            ForEach(rows) { row in
                boardRow(
                    title: row.title.isEmpty ? row.eventType : row.title,
                    subtitle: row.subtitle ?? row.counterpartyName,
                    amount: row.amount,
                    badge: row.badge
                )
            }
        }
    }

    private func visibleCount(status: String) -> Int {
        switch spec.code {
        case "ATTENTION":
            return (model.attention ?? []).filter {
                OperatingSheetChrome.matchesStatus(status, badge: attentionBadge($0.severity))
            }.count
        case "PAYMENT_READINESS":
            return (model.paymentReadiness ?? []).filter {
                OperatingSheetChrome.matchesStatus(status, badge: paymentBadge($0.dueBucket))
            }.count
        case "ACTIVITY":
            return (model.activity ?? []).filter {
                OperatingSheetChrome.matchesStatus(status, badge: $0.badge)
            }.count
        default:
            return (section?.rows ?? []).filter {
                OperatingSheetChrome.matchesStatus(status, badge: $0.badge)
            }.count
        }
    }

    private func attentionBadge(_ severity: String?) -> String {
        switch severity?.uppercased() {
        case "CRITICAL": return "Overdue"
        case "HIGH": return "Alert"
        case "MEDIUM": return "Unavailable"
        default: return "Open"
        }
    }

    private func paymentBadge(_ dueBucket: String?) -> String {
        switch dueBucket?.uppercased() {
        case "OVERDUE": return "Overdue"
        case "DUE_SOON": return "Due soon"
        case "OPEN": return "Open"
        default: return dueBucket ?? "Open"
        }
    }

    private func metricTiles(_ metrics: [OperatingBoardMetricPayload]) -> some View {
        let cols = Array(metrics.prefix(6))
        return LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 8) {
            ForEach(cols) { tile in
                VStack(alignment: .leading, spacing: 4) {
                    Text(tile.label)
                        .font(.system(size: 11, weight: .medium))
                        .foregroundStyle(theme.body)
                    Text(tile.value)
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundStyle(toneColor(tile.tone))
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(12)
                .background(theme.inset)
                .clipShape(RoundedRectangle(cornerRadius: 12))
            }
        }
    }

    private func toneColor(_ tone: String?) -> Color {
        switch tone?.uppercased() {
        case "WARNING": return theme.warning
        case "POSITIVE": return theme.accentSoft
        default: return theme.ink
        }
    }

    private func whyFootnote(_ why: String) -> some View {
        Text("Why: \(why)")
            .font(.system(size: 12))
            .foregroundStyle(Color(hex: "#CFEFF6"))
            .padding(12)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(Color(hex: "#06B6D4").opacity(0.12))
            .clipShape(RoundedRectangle(cornerRadius: 12))
    }

    private func emptyBlock(
        title: String? = nil,
        body: String? = nil,
        compact: Bool
    ) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(title ?? spec.emptyTitle)
                .font(.system(size: compact ? 14 : 16, weight: .semibold))
                .foregroundStyle(theme.ink)
            Text(body ?? spec.emptyBody)
                .font(.system(size: 12))
                .foregroundStyle(theme.body)
        }
        .padding(compact ? 12 : 16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(theme.inset)
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }

    private func boardRow(title: String, subtitle: String?, amount: String?, badge: String?) -> some View {
        HStack(alignment: .center, spacing: 10) {
            RoundedRectangle(cornerRadius: 2)
                .fill(theme.accent)
                .frame(width: 3, height: 32)
            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(.system(size: 14, weight: .medium))
                    .foregroundStyle(theme.ink)
                if let subtitle, !subtitle.isEmpty {
                    Text(subtitle)
                        .font(.system(size: 12))
                        .foregroundStyle(theme.body)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            if let amount, model.financeVisible == true, !amount.isEmpty {
                Text(amount)
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundStyle(theme.accent)
            } else if let badge, !badge.isEmpty {
                Text(badge)
                    .font(.system(size: 11, weight: .semibold))
                    .foregroundStyle(theme.ink)
                    .padding(.horizontal, 10)
                    .padding(.vertical, 5)
                    .background(theme.inset)
                    .clipShape(Capsule())
            }
        }
        .padding(.vertical, 8)
    }
}
