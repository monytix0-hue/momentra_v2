import SwiftUI

/// Shared rose / ember / teal hub chrome for retail-family Quick Adds (RTL/RST/SVC-HUB-v5).
/// Tile order and locked-role behaviour mirror Android retail / restaurant / service hubs.
enum RetailFamilyHubTile: Identifiable, Equatable {
    case capture(code: String, title: String, subtitle: String, glyph: String, requires: String?, wide: Bool = false)
    case masters(tab: OperatingMasterTab, title: String, subtitle: String, glyph: String, wide: Bool = false)

    var id: String {
        switch self {
        case let .capture(code, title, _, _, _, _): return "capture:\(code):\(title)"
        case let .masters(tab, title, _, _, _): return "masters:\(tab.rawValue):\(title)"
        }
    }

    var wide: Bool {
        switch self {
        case let .capture(_, _, _, _, _, wide), let .masters(_, _, _, _, wide): return wide
        }
    }

    var title: String {
        switch self {
        case let .capture(_, title, _, _, _, _), let .masters(_, title, _, _, _): return title
        }
    }

    var subtitle: String {
        switch self {
        case let .capture(_, _, subtitle, _, _, _), let .masters(_, _, subtitle, _, _): return subtitle
        }
    }

    var glyph: String {
        switch self {
        case let .capture(_, _, _, glyph, _, _), let .masters(_, _, _, glyph, _): return glyph
        }
    }

    var requires: String? {
        switch self {
        case let .capture(_, _, _, _, requires, _): return requires
        case .masters: return nil
        }
    }
}

struct RetailFamilyHubScreen: View {
    let theme: OperatingTheme
    let eyebrow: String
    let headline: String
    let heroBody: String
    let heroCta: String
    let heroCapture: String
    let whyNote: String
    let core: [RetailFamilyHubTile]
    let support: [RetailFamilyHubTile]
    let utilities: [RetailFamilyHubTile]
    let coreSection: String
    let supportSection: String
    let utilitySection: String
    /// Amber section trailers — Figma RTL-HUB uses `Retail journey` / `Network · masters`.
    var coreTrailing: String? = nil
    var supportTrailing: String? = nil
    @ObservedObject var model: BusinessOperatingModel
    var onCapture: (String) -> Void
    var onOpenMasters: (OperatingMasterTab) -> Void
    var onOpenStory: () -> Void = {}

    private var allowed: Set<String> {
        Set((model.projection?.quickAdds ?? []).map { $0.code.uppercased() })
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                header
                contextChips
                hero
                section(
                    title: coreSection,
                    trailing: coreTrailing ?? (
                        theme.vertical == .consumerService ? "Services journey" : "Tap to record"
                    ),
                    tiles: core
                )
                section(
                    title: supportSection,
                    trailing: supportTrailing ?? (
                        theme.vertical == .consumerService ? "Clients · masters" : "Role-gated"
                    ),
                    tiles: support
                )
                // Figma hubs (RTL-HUB / SVC-HUB) omit Utilities · 3, but Hold/Transfer/Reprint
                // remain required screens — keep a muted discovery row (see RetailHubScreen /
                // ServiceHubScreen). Skip when the kit passes an empty list.
                if !utilities.isEmpty {
                    section(
                        title: utilitySection,
                        trailing: "Hold · move · reprint",
                        tiles: utilities,
                        muted: true
                    )
                }
                whyCard
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 8)
            .padding(.bottom, 24)
        }
        .background(theme.canvas)
    }

    private var header: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text("Quick Adds")
                .font(.plusJakarta(size: 28, weight: .bold))
                .foregroundStyle(OperatingV12.title)
            Text("\(model.title) · \(model.vertical.label)")
                .font(.plusJakarta(size: 13, weight: .medium))
                .foregroundStyle(OperatingV12.subtitle)
        }
    }

    private var contextChips: some View {
        HStack(spacing: 8) {
            chip(model.vertical.label, filled: true)
            chip(periodLine, filled: false)
            chip(gstOn ? "GST on" : "GST off", filled: false)
            Spacer(minLength: 0)
        }
    }

    private var hero: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text(eyebrow)
                .font(.plusJakarta(size: 11, weight: .bold))
                .tracking(1.1)
                .foregroundStyle(theme.onAccent.opacity(0.85))
            Text(headline)
                .font(.plusJakarta(size: 24, weight: .bold))
                .foregroundStyle(theme.onAccent)
            Text(heroBody)
                .font(.plusJakarta(size: 13, weight: .medium))
                .foregroundStyle(theme.onAccent.opacity(0.9))
                .fixedSize(horizontal: false, vertical: true)
            if heroAllowed {
                Button {
                    onCapture(heroCapture)
                } label: {
                    Text(heroCta)
                        .font(.plusJakarta(size: 15, weight: .bold))
                        .foregroundStyle(theme.accent)
                        .frame(maxWidth: .infinity)
                        .frame(height: 48)
                        // SVC-HUB cream CTA `#FEF3C7`; other retail-family kits keep white.
                        .background(theme.vertical == .consumerService ? Color(hex: "#FEF3C7") : Color.white)
                        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                        .shadow(
                            color: theme.vertical == .consumerService
                                ? Color(hex: "#84CC16").opacity(0.55)
                                : .clear,
                            radius: 12,
                            y: 6
                        )
                }
                .buttonStyle(.plain)
                .padding(.top, 4)
            }
        }
        .padding(20)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            LinearGradient(
                colors: [model.vertical.heroStart, model.vertical.heroEnd],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        )
        .clipShape(RoundedRectangle(cornerRadius: 22, style: .continuous))
        .shadow(color: theme.accent.opacity(0.35), radius: 18, y: 8)
    }

    private var heroAllowed: Bool {
        allowed.isEmpty || allowed.contains(heroCapture.uppercased())
            || (heroCapture == "POS" && allowed.contains("SALE"))
            || (heroCapture == "KOT" && allowed.contains("ORDER"))
            || (heroCapture == "STATUS" && allowed.contains("ORDER"))
            || (heroCapture == "SERVICE" && allowed.contains("SERVICE"))
    }

    private func section(
        title: String,
        trailing: String,
        tiles: [RetailFamilyHubTile],
        muted: Bool = false
    ) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                Text(title)
                    .font(.plusJakarta(size: muted ? 12 : 13, weight: .bold))
                    .foregroundStyle(muted ? OperatingV12.subtitle : OperatingV12.title)
                Spacer()
                Text(trailing)
                    .font(.plusJakarta(size: 11, weight: .medium))
                    .foregroundStyle(OperatingV12.subtitle.opacity(muted ? 0.85 : 1))
            }
            grid(tiles)
                .opacity(muted ? 0.92 : 1)
        }
    }

    private func grid(_ tiles: [RetailFamilyHubTile]) -> some View {
        let rows = Self.rows(from: tiles)
        return VStack(spacing: 8) {
            ForEach(Array(rows.enumerated()), id: \.offset) { _, row in
                HStack(spacing: 8) {
                    ForEach(row) { tile in
                        tileButton(tile)
                            .frame(maxWidth: .infinity)
                    }
                    if row.count == 1 && !row[0].wide {
                        Color.clear.frame(maxWidth: .infinity)
                        Color.clear.frame(maxWidth: .infinity)
                    } else if row.count == 2 && !row.allSatisfy(\.wide) {
                        Color.clear.frame(maxWidth: .infinity)
                    }
                }
            }
        }
    }

    private func tileButton(_ tile: RetailFamilyHubTile) -> some View {
        let locked = lockedReason(tile)
        return Button {
            guard locked == nil else { return }
            switch tile {
            case let .capture(code, _, _, _, _, _):
                onCapture(code)
            case let .masters(tab, _, _, _, _):
                onOpenMasters(tab)
            }
        } label: {
            VStack(alignment: .leading, spacing: 8) {
                HStack {
                    Image(systemName: tile.glyph)
                        .font(.system(size: 16, weight: .semibold))
                        .foregroundStyle(locked == nil ? theme.accent : OperatingV12.subtitle)
                    Spacer(minLength: 0)
                    if locked != nil {
                        Text("Locked")
                            .font(.plusJakarta(size: 9, weight: .bold))
                            .foregroundStyle(OperatingV12.subtitle)
                    }
                }
                Text(tile.title)
                    .font(.plusJakarta(size: 13, weight: .bold))
                    .foregroundStyle(OperatingV12.title)
                    .lineLimit(2)
                    .minimumScaleFactor(0.85)
                Text(tile.subtitle)
                    .font(.plusJakarta(size: 11, weight: .medium))
                    .foregroundStyle(OperatingV12.subtitle)
                    .lineLimit(2)
            }
            .padding(12)
            .frame(maxWidth: .infinity, minHeight: 96, alignment: .topLeading)
            .background(theme.inset)
            .overlay(
                RoundedRectangle(cornerRadius: 14, style: .continuous)
                    .strokeBorder(
                        locked == nil ? theme.accent.opacity(0.28) : theme.border.opacity(0.5),
                        lineWidth: 1
                    )
            )
            .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
            .opacity(locked == nil ? 1 : 0.55)
        }
        .buttonStyle(.plain)
        .disabled(locked != nil)
    }

    private func lockedReason(_ tile: RetailFamilyHubTile) -> String? {
        guard let requires = tile.requires else { return nil }
        if allowed.isEmpty { return nil }
        return allowed.contains(requires.uppercased()) ? nil : "Role locked"
    }

    private var whyCard: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Boundaries")
                .font(.plusJakarta(size: 13, weight: .bold))
                .foregroundStyle(theme.accent)
            Text(whyNote)
                .font(.plusJakarta(size: 12, weight: .medium))
                .foregroundStyle(Color(hex: "#CBD5E1"))
                .fixedSize(horizontal: false, vertical: true)
            Button(action: onOpenStory) {
                Text("Story config ›")
                    .font(.plusJakarta(size: 12, weight: .semibold))
                    .foregroundStyle(theme.accent)
            }
            .buttonStyle(.plain)
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(theme.accent.opacity(0.08))
        .overlay(
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .strokeBorder(theme.accent.opacity(0.35), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
    }

    private func chip(_ text: String, filled: Bool) -> some View {
        Text(text)
            .font(.plusJakarta(size: 11, weight: .semibold))
            .foregroundStyle(filled ? theme.onAccent : OperatingV12.subtitle)
            .padding(.horizontal, 10)
            .padding(.vertical, 6)
            .background(filled ? theme.accent : theme.inset)
            .overlay(
                Capsule().stroke(filled ? theme.accent : theme.border, lineWidth: 1)
            )
            .clipShape(Capsule())
    }

    private var gstOn: Bool {
        (model.projection?.gstTracking ?? "").uppercased() == "YES"
    }

    private var periodLine: String {
        guard let start = model.projection?.startDate, let year = Int(start.prefix(4)) else {
            return "\(model.currency) · FY"
        }
        return "\(model.currency) · FY \(year)–\(String(format: "%02d", (year + 1) % 100))"
    }

    static func rows(from tiles: [RetailFamilyHubTile]) -> [[RetailFamilyHubTile]] {
        var rows: [[RetailFamilyHubTile]] = []
        var row: [RetailFamilyHubTile] = []
        for tile in tiles {
            if tile.wide {
                if !row.isEmpty {
                    rows.append(row)
                    row = []
                }
                if let last = rows.last, last.first?.wide == true, last.count < 2 {
                    rows[rows.count - 1] = last + [tile]
                } else {
                    rows.append([tile])
                }
            } else {
                row.append(tile)
                if row.count == 3 {
                    rows.append(row)
                    row = []
                }
            }
        }
        if !row.isEmpty { rows.append(row) }
        return rows
    }
}

/// Shared active-tab shell for retail family — Pulse keeps Operating v12 (vertical-aware
/// action row); Moments/Life/Memory use Android retail-family card IA. Create mounts the hub.
struct RetailFamilyActiveView<Hub: View>: View {
    @ObservedObject var model: BusinessOperatingModel
    let destination: BottomDestination
    let refreshToken: UInt64
    var onOpenQuickAdd: () -> Void = {}
    var onOpenStory: () -> Void = {}
    var onOpenMasters: (OperatingMasterTab) -> Void = { _ in }
    var onCapture: ((String) -> Void)? = nil
    var remapCapture: (String) -> String = { $0 }
    private let hub: () -> Hub

    @State private var activeSheet: RetailFamilyActiveSheet?

    private var theme: OperatingTheme { OperatingTheme(vertical: model.vertical) }

    init(
        model: BusinessOperatingModel,
        destination: BottomDestination,
        refreshToken: UInt64,
        onOpenQuickAdd: @escaping () -> Void = {},
        onOpenStory: @escaping () -> Void = {},
        onOpenMasters: @escaping (OperatingMasterTab) -> Void = { _ in },
        onCapture: ((String) -> Void)? = nil,
        remapCapture: @escaping (String) -> String = { $0 },
        @ViewBuilder hub: @escaping () -> Hub
    ) {
        self.model = model
        self.destination = destination
        self.refreshToken = refreshToken
        self.onOpenQuickAdd = onOpenQuickAdd
        self.onOpenStory = onOpenStory
        self.onOpenMasters = onOpenMasters
        self.onCapture = onCapture
        self.remapCapture = remapCapture
        self.hub = hub
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                phaseNotice
                switch destination {
                case .pulse:
                    // Obsidian takeover only when setup has no budget/target/GST and feeds are quiet.
                    if model.projection?.hasUsefulPulseChrome != true {
                        BusinessAppStateBody(
                            spec: BusinessAppStateSpecs.spec(.pulse, .empty),
                            accent: .emerald,
                            onPrimary: { capture("SALE") },
                            onSecondary: onOpenQuickAdd
                        )
                        .frame(minHeight: 420)
                    } else {
                        OperatingPulseScreen(
                            model: model,
                            theme: theme,
                            onCapture: capture,
                            onViewAll: openSheet,
                            onOpenRow: openRow
                        )
                    }
                case .moments:
                    RetailFamilyMomentsScreen(
                        model: model,
                        theme: theme,
                        onCapture: capture,
                        onViewAll: openSheet,
                        onOpenRow: openRow
                    )
                case .life:
                    RetailFamilyLifeScreen(
                        model: model,
                        theme: theme,
                        onCapture: capture,
                        onViewAll: openSheet,
                        onOpenRow: openRow
                    )
                case .memory:
                    RetailFamilyMemoryScreen(
                        model: model,
                        theme: theme,
                        onCapture: capture,
                        onStory: onOpenStory,
                        onViewAll: openSheet,
                        onOpenRow: openRow
                    )
                case .create:
                    hub()
                }
            }
            .padding(.bottom, 32)
        }
        .background(theme.canvas)
        .task { await model.load() }
        .onChange(of: refreshToken) { _, _ in
            Task { await model.load() }
        }
        .sheet(item: $activeSheet) { sheet in
            Group {
                switch sheet {
                case let .viewAll(spec):
                    OperatingViewAllSheet(
                        spec: spec,
                        theme: theme,
                        model: model,
                        onOpenRow: { activeSheet = .row($0) },
                        onFooterAction: { action in
                            activeSheet = nil
                            model.periodFilter = nil
                            model.statusFilter = nil
                            model.periodFrom = nil
                            model.periodTo = nil
                            switch action {
                            case "MEMORY":
                                onOpenStory()
                            case "ATTENTION", "QUICK_ADD":
                                onOpenQuickAdd()
                            default:
                                capture(action)
                            }
                        }
                    )
                case let .row(row):
                    OperatingRecordSheet(theme: theme, row: row, model: model)
                }
            }
            .presentationDetents([.large])
            .presentationDragIndicator(.visible)
            .presentationCornerRadius(24)
            .preferredColorScheme(.dark)
        }
    }

    private func capture(_ code: String) {
        let mapped = remapCapture(code)
        if let onCapture {
            onCapture(mapped)
        } else {
            onOpenQuickAdd()
        }
    }

    private func openRow(_ row: OperatingActivityPayload) {
        activeSheet = .row(row)
    }

    private func openSheet(_ code: String) {
        guard let spec = OperatingSheetCatalog.spec(forCode: code, vertical: model.vertical) else { return }
        activeSheet = .viewAll(spec)
    }

    @ViewBuilder
    private var phaseNotice: some View {
        switch model.phase {
        case .loading:
            HStack(spacing: 8) {
                ProgressView().tint(theme.accent)
                Text("Reading what was recorded…")
                    .font(.plusJakarta(size: 12))
                    .foregroundStyle(OperatingV12.subtitle)
            }
            .padding(.horizontal, 16)
        case let .error(message):
            OperatingV12Card(theme: theme) {
                Text("Couldn't load this moment")
                    .font(.plusJakarta(size: 14, weight: .bold))
                    .foregroundStyle(OperatingV12.title)
                Text(message)
                    .font(.plusJakarta(size: 12))
                    .foregroundStyle(OperatingV12.subtitle)
                Button {
                    Task { await model.load() }
                } label: {
                    Text("Retry")
                        .font(.plusJakarta(size: 14, weight: .bold))
                        .foregroundStyle(OperatingV12.title)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 12)
                        .background(theme.inset)
                        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                }
                .buttonStyle(.plain)
            }
            .padding(.horizontal, 16)
        case .empty:
            OperatingV12Card(theme: theme) {
                Text("This moment is closed")
                    .font(.plusJakarta(size: 14, weight: .bold))
                    .foregroundStyle(OperatingV12.title)
                Text("A closed moment is read-only. Start a new one to record again.")
                    .font(.plusJakarta(size: 12))
                    .foregroundStyle(OperatingV12.subtitle)
            }
            .padding(.horizontal, 16)
        case .ready:
            EmptyView()
        }
    }
}

private enum RetailFamilyActiveSheet: Identifiable {
    case viewAll(OperatingSheetSpec)
    case row(OperatingActivityPayload)

    var id: String {
        switch self {
        case let .viewAll(spec): return "viewAll:\(spec.code)"
        case let .row(row): return "row:\(row.id)"
        }
    }
}
