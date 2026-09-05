import SwiftUI

/// Figma Group active tokens — 575:14165 family.
enum GroupActiveTheme {
    static let bg = Color(hex: "#131313")
    static let brand = Color(hex: "#FFB598")
    static let brandSoft = Color(hex: "#66FFB598")
    static let text = Color(hex: "#E5E2E1")
    static let secondary = Color(hex: "#DFC0B4")
    static let card = Color(hex: "#201F1F")
    static let border = Color(hex: "#2E2A28")
    static let accentOrange = Color(hex: "#FF7A3D")
}

/// Figma Trip Quick Add sheet chrome — #1C1A24 / #FF7A3D.
enum TripSheetTokens {
    static let bg = Color(hex: "#1C1A24")
    static let field = Color(hex: "#252332")
    static let border = Color(hex: "#323042")
    static let muted = Color(hex: "#9E9AA8")
    static let text = Color(hex: "#FFFFFF")
    static let accent = Color(hex: "#FF7A3D")
    static let accentEnd = Color(hex: "#FFB598")
}

enum GroupFinanceFormat {
    static func parseAmount(_ raw: String?) -> Decimal {
        guard let raw, !raw.isEmpty else { return 0 }
        let cleaned = raw.replacingOccurrences(of: ",", with: "")
        return Decimal(string: cleaned) ?? 0
    }

    static func utilizationPercent(expenseTotal: String?, budgetTotal: String?) -> Int {
        let budget = parseAmount(budgetTotal)
        guard budget > 0 else { return 0 }
        let expense = parseAmount(expenseTotal)
        let pct = (expense as NSDecimalNumber).doubleValue / (budget as NSDecimalNumber).doubleValue * 100
        return min(100, Int(pct.rounded()))
    }

    static func symbolFor(_ currencyCode: String) -> String {
        GroupTravelCurrencyCatalog.symbol(currencyCode)
    }

    static func formatMoney(_ raw: String?, currencyCode: String = "INR") -> String {
        let value = parseAmount(raw)
        if value <= 0 { return "—" }
        let prefix = symbolFor(currencyCode)
        let formatter = NumberFormatter()
        formatter.numberStyle = .decimal
        formatter.groupingSeparator = ","
        let nsValue = value as NSDecimalNumber
        formatter.maximumFractionDigits = nsValue.doubleValue.truncatingRemainder(dividingBy: 1) == 0 ? 0 : 2
        let body = formatter.string(from: value as NSDecimalNumber) ?? "0"
        return "\(prefix)\(body)"
    }

    static func compactMoney(_ raw: String?, currencyCode: String = "INR") -> String {
        let value = parseAmount(raw)
        let prefix = symbolFor(currencyCode)
        if value >= 100_000 {
            let k = (value as NSDecimalNumber).doubleValue / 1000
            return "\(prefix)\(Int(k.rounded()))K"
        }
        return formatMoney(raw, currencyCode: currencyCode)
    }

    /// Partitioned line e.g. "₹80,000 + $1,200 + €400" — no FX conversion.
    static func formatPartitionedAmounts(
        _ amounts: [(String, String)],
        compact: Bool = false,
        hide: Bool = false,
        separator: String = " + "
    ) -> String {
        if hide { return "••••" }
        let parts = amounts.compactMap { code, raw -> String? in
            let formatted = compact ? compactMoney(raw, currencyCode: code) : formatMoney(raw, currencyCode: code)
            return formatted == "—" ? nil : formatted
        }
        return parts.isEmpty ? "—" : parts.joined(separator: separator)
    }

    static func resolvePrimaryTotal(
        _ totals: [APIClient.GroupFinanceTotalsPayload],
        preferredCurrency: String? = nil
    ) -> APIClient.GroupFinanceTotalsPayload? {
        guard !totals.isEmpty else { return nil }
        if let pref = preferredCurrency,
           let match = totals.first(where: { $0.currencyCode.caseInsensitiveCompare(pref) == .orderedSame }) {
            return match
        }
        return totals.first(where: { parseAmount($0.budgetTotal) > 0 }) ?? totals.first
    }

    static func budgetPartitionLine(
        _ totals: [APIClient.GroupFinanceTotalsPayload],
        compact: Bool = false,
        hide: Bool = false
    ) -> String {
        formatPartitionedAmounts(
            totals.map { ($0.currencyCode, $0.budgetTotal ?? "0") },
            compact: compact,
            hide: hide
        )
    }

    static func expensePartitionLine(
        _ totals: [APIClient.GroupFinanceTotalsPayload],
        compact: Bool = false,
        hide: Bool = false
    ) -> String {
        formatPartitionedAmounts(
            totals.map { ($0.currencyCode, $0.expenseTotal ?? "0") },
            compact: compact,
            hide: hide
        )
    }

    static func positionsForParticipant(
        _ positions: [APIClient.GroupFinancePositionPayload],
        participantId: String?
    ) -> [APIClient.GroupFinancePositionPayload] {
        guard let participantId else { return [] }
        return positions.filter { $0.participantId == participantId }
    }

    static func viewerBalanceHeadline(
        viewer: APIClient.GroupFinancePositionPayload?,
        allPositions: [APIClient.GroupFinancePositionPayload],
        hide: Bool
    ) -> (headline: String, incl: String?) {
        guard let viewer else { return ("No balance yet", nil) }
        let primaryCode = viewer.currencyCode
        let primaryNet = parseAmount(viewer.netPosition)
        let headline: String = {
            let v = (primaryNet as NSDecimalNumber).doubleValue
            if v < 0 {
                let owed = formatMoney(String(format: "%.2f", abs(v)), currencyCode: primaryCode)
                return "You owe \(hide ? "••••" : owed)"
            }
            if v > 0 {
                let owed = formatMoney(viewer.netPosition, currencyCode: primaryCode)
                return "You are owed \(hide ? "••••" : owed)"
            }
            return "You're settled up"
        }()
        let others = positionsForParticipant(allPositions, participantId: viewer.participantId)
            .filter { $0.currencyCode.caseInsensitiveCompare(primaryCode) != .orderedSame }
            .compactMap { pos -> String? in
                let net = parseAmount(pos.netPosition)
                guard net != 0 else { return nil }
                let v = (net as NSDecimalNumber).doubleValue
                return formatMoney(String(format: "%.2f", abs(v)), currencyCode: pos.currencyCode)
            }
        let incl: String? = (others.isEmpty || hide) ? nil : "incl. \(others.joined(separator: " + "))"
        return (headline, incl)
    }

    static func groupPositionsByParticipant(
        _ positions: [APIClient.GroupFinancePositionPayload]
    ) -> [(String, [APIClient.GroupFinancePositionPayload])] {
        Dictionary(grouping: positions, by: \.participantId).map { ($0.key, $0.value) }
    }
}

/// Trip pulse destination labels from pulse widget or setup prefill.
enum TripPulseDestinations {
    static func fromWidget(_ widget: [String: AnyDecodable]?) -> [String] {
        guard let widget else { return [] }
        if let places = widget["places"]?.value as? [[String: Any]] {
            let labels = places.compactMap { $0["label"] as? String }.filter { !$0.isEmpty }
            if !labels.isEmpty { return labels }
        }
        for key in ["destinationText", "destination_text", "destination"] {
            if let raw = widget[key]?.value as? String, !raw.isEmpty { return [raw] }
        }
        return []
    }

    static func fromPrefill(_ prefill: GroupSetupPrefill?) -> [String] {
        guard let prefill else { return [] }
        let fromPlaces = (prefill.places ?? []).compactMap(\.label).filter { !$0.isEmpty }
        if !fromPlaces.isEmpty { return fromPlaces }
        if let legacy = prefill.destinationText, !legacy.isEmpty { return [legacy] }
        return []
    }

    static func heroSubtitle(places: [String], peopleCount: Int) -> String {
        if places.count > 1 { return "\(places.count) destinations · \(peopleCount) people" }
        if places.count == 1 { return "\(places.first!) · \(peopleCount) people" }
        return "Trip · \(peopleCount) people"
    }
}

struct GroupHeroHeader: View {
    let title: String
    let subtitle: String
    var meta: String? = nil

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(title)
                .font(.plusJakarta(size: 22, weight: .heavy))
                .foregroundStyle(GroupActiveTheme.text)
            Text(subtitle)
                .font(.plusJakarta(size: 13))
                .foregroundStyle(GroupActiveTheme.secondary)
            if let meta {
                Text(meta)
                    .font(.plusJakarta(size: 12, weight: .semibold))
                    .foregroundStyle(GroupActiveTheme.brand)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(20)
        .background(
            LinearGradient(
                colors: [Color(hex: "#3D2A24"), GroupActiveTheme.bg, Color(hex: "#1A1512")],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        )
        .clipShape(RoundedRectangle(cornerRadius: 24))
    }
}

struct GroupSectionCard<Content: View, Badge: View>: View {
    let title: String
    @ViewBuilder var badge: () -> Badge
    @ViewBuilder var content: () -> Content

    init(title: String, @ViewBuilder badge: @escaping () -> Badge = { EmptyView() }, @ViewBuilder content: @escaping () -> Content) {
        self.title = title
        self.badge = badge
        self.content = content
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text(title)
                    .font(.plusJakarta(size: 15, weight: .bold))
                    .foregroundStyle(GroupActiveTheme.text)
                Spacer()
                badge()
            }
            content()
        }
        .padding(16)
        .background(GroupActiveTheme.card)
        .clipShape(RoundedRectangle(cornerRadius: 18))
        .overlay(RoundedRectangle(cornerRadius: 18).stroke(GroupActiveTheme.border))
    }
}

struct GroupComingSoonBadge: View {
    var body: some View {
        Text("Coming Soon")
            .font(.plusJakarta(size: 10, weight: .bold))
            .foregroundStyle(GroupActiveTheme.brand)
            .padding(.horizontal, 10)
            .padding(.vertical, 4)
            .background(GroupActiveTheme.brandSoft)
            .clipShape(Capsule())
    }
}

struct GroupApiGapBadge: View {
    var body: some View {
        Text("API_GAP")
            .font(.plusJakarta(size: 9, weight: .bold))
            .foregroundStyle(Color(hex: "#F87171"))
            .padding(.horizontal, 8)
            .padding(.vertical, 4)
            .background(Color(hex: "#33F87171"))
            .clipShape(Capsule())
    }
}

struct GroupEmptySection: View {
    let message: String
    let detail: String

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(message)
                .font(.plusJakarta(size: 13, weight: .semibold))
                .foregroundStyle(GroupActiveTheme.text)
            Text(detail)
                .font(.plusJakarta(size: 12))
                .foregroundStyle(GroupActiveTheme.secondary)
        }
    }
}

struct GroupMetricTile: View {
    let label: String
    let value: String

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(label)
                .font(.plusJakarta(size: 11))
                .foregroundStyle(GroupActiveTheme.secondary)
            Text(value)
                .font(.plusJakarta(size: 16, weight: .bold))
                .foregroundStyle(GroupActiveTheme.text)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(12)
        .background(Color(hex: "#181716"))
        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
    }
}

struct GroupQuickChip: View {
    let label: String
    let enabled: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(label)
                .font(.plusJakarta(size: 12, weight: .semibold))
                .foregroundStyle(GroupActiveTheme.text)
                .padding(.horizontal, 14)
                .padding(.vertical, 8)
                .background(Color(hex: "#33FFB598"))
                .clipShape(Capsule())
                .overlay(Capsule().stroke(GroupActiveTheme.border))
        }
        .buttonStyle(.plain)
        .disabled(!enabled)
        .opacity(enabled ? 1 : 0.45)
    }
}

struct GroupProgressRing: View {
    let percent: Int
    let centerLabel: String
    let centerSub: String
    @State private var pulse = false

    var body: some View {
        ZStack {
            Circle()
                .stroke(Color(hex: "#2A2624"), lineWidth: 10)
            Circle()
                .trim(from: 0, to: CGFloat(min(max(percent, 0), 100)) / 100)
                .stroke(GroupActiveTheme.brand, style: StrokeStyle(lineWidth: 10, lineCap: .round))
                .rotationEffect(.degrees(-90))
            VStack(spacing: 2) {
                Text(centerLabel)
                    .font(.plusJakarta(size: 22, weight: .heavy))
                    .foregroundStyle(GroupActiveTheme.text)
                Text(centerSub)
                    .font(.plusJakarta(size: 11))
                    .foregroundStyle(GroupActiveTheme.secondary)
            }
        }
        .frame(width: 120, height: 120)
        .scaleEffect(pulse ? 1 : 0.94)
        .animation(.easeInOut(duration: 1.4).repeatForever(autoreverses: true), value: pulse)
        .onAppear { pulse = true }
    }
}

struct GroupProgressBar: View {
    let percent: Int

    var body: some View {
        GeometryReader { geo in
            ZStack(alignment: .leading) {
                Capsule().fill(Color(hex: "#2A2624"))
                Capsule()
                    .fill(GroupActiveTheme.brand)
                    .frame(width: geo.size.width * CGFloat(min(max(percent, 0), 100)) / 100)
            }
        }
        .frame(height: 8)
    }
}

struct GroupCtaButton: View {
    let label: String
    let enabled: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(label)
                .font(.plusJakarta(size: 14, weight: .bold))
                .foregroundStyle(Color(hex: "#131313"))
                .frame(maxWidth: .infinity)
                .padding(.vertical, 14)
                .background(
                    LinearGradient(
                        colors: enabled ? [GroupActiveTheme.brand, Color(hex: "#E89574")] : [Color(hex: "#3A3533"), Color(hex: "#3A3533")],
                        startPoint: .leading,
                        endPoint: .trailing
                    )
                )
                .clipShape(RoundedRectangle(cornerRadius: 14))
        }
        .buttonStyle(.plain)
        .disabled(!enabled)
        .opacity(enabled ? 1 : 0.45)
    }
}
