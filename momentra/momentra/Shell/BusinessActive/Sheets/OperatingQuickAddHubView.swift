import SwiftUI

/// The Quick adds hub: one tile per capture the vertical offers, opening the shared capture
/// sheet. Also deep-links into masters and the three QA-only modes.
struct OperatingQuickAddHubView: View {
    let theme: OperatingTheme
    @ObservedObject var model: BusinessOperatingModel
    @Environment(\.dismiss) private var dismiss

    /// One sheet at a time: a capture or the masters tab. Chaining two `.sheet` modifiers on the
    /// same view is unreliable, so this hub keeps a single route.
    @State private var sheet: OperatingQuickAddSheet?

    private var adds: [OperatingQuickAddPayload] { model.projection?.quickAdds ?? [] }

    /// The quick-add codes the backend sends, plus the three QA modes that have their own frames.
    private var modes: [String] {
        var list = adds.map { $0.code.uppercased() }
        for extra in [OperatingCaptureLayout.holdMode, OperatingCaptureLayout.transferMode, OperatingCaptureLayout.reprintMode] {
            if !list.contains(extra) { list.append(extra) }
        }
        return list
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 14) {
                Text("Quick adds")
                    .font(.plusJakarta(size: 24, weight: .semibold))
                    .foregroundStyle(theme.ink)
                Text("Record what happened. Each capture posts one event — nothing is estimated.")
                    .font(.plusJakarta(size: 12))
                    .foregroundStyle(theme.body)
                LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 10) {
                    ForEach(modes, id: \.self) { mode in
                        Button {
                            sheet = .capture(mode)
                        } label: {
                            VStack(alignment: .leading, spacing: 5) {
                                Text(label(for: mode))
                                    .font(.plusJakarta(size: 14, weight: .semibold))
                                    .foregroundStyle(theme.ink)
                                    .multilineTextAlignment(.leading)
                                Text(qaCode(for: mode))
                                    .font(.plusJakarta(size: 10))
                                    .foregroundStyle(theme.muted)
                            }
                            .padding(12)
                            .frame(maxWidth: .infinity, minHeight: 78, alignment: .leading)
                            .background(theme.inset)
                            .overlay(
                                RoundedRectangle(cornerRadius: 14)
                                    .stroke(theme.border, lineWidth: 1)
                            )
                            .clipShape(RoundedRectangle(cornerRadius: 14))
                        }
                        .buttonStyle(.plain)
                    }
                }
                Button {
                    sheet = .masters
                } label: {
                    Text("Masters & price lists ›")
                        .font(.plusJakarta(size: 13, weight: .semibold))
                        .foregroundStyle(theme.accent)
                }
                .buttonStyle(.plain)
            }
            .padding(18)
        }
        .background(theme.canvas.ignoresSafeArea())
        .sheet(item: $sheet) { route in
            Group {
                switch route {
                case let .capture(mode):
                    OperatingCaptureSheet(theme: theme, mode: mode, model: model)
                case .masters:
                    OperatingMastersView(theme: theme, model: model)
                }
            }
            .presentationDetents([.large])
            .presentationDragIndicator(.visible)
            .presentationCornerRadius(24)
            .preferredColorScheme(.dark)
        }
    }

    private func label(for mode: String) -> String {
        switch mode {
        case OperatingCaptureLayout.holdMode: return "Hold a record"
        case OperatingCaptureLayout.transferMode: return "Transfer stock"
        case OperatingCaptureLayout.reprintMode: return "Reprint document"
        default:
            if let add = adds.first(where: { $0.code.uppercased() == mode }) { return add.label }
            return OperatingCaptureLayout.forMode(mode)?.label ?? mode.capitalized
        }
    }

    private func qaCode(for mode: String) -> String {
        switch mode {
        case OperatingCaptureLayout.holdMode: return "QA-HOLD"
        case OperatingCaptureLayout.transferMode: return "QA-TRANSFER"
        case OperatingCaptureLayout.reprintMode: return "QA-REPRINT"
        default: return OperatingCaptureLayout.forMode(mode)?.qaCode ?? "QA"
        }
    }
}

/// One sheet at a time in the quick-adds hub.
private enum OperatingQuickAddSheet: Identifiable {
    case capture(String)
    case masters

    var id: String {
        switch self {
        case let .capture(mode): return "capture:\(mode)"
        case .masters: return "masters"
        }
    }
}

/// `TRD-MEMORY-v12` / `DST-MEMORY-v12` — compose the eight-page business story.
///
/// The endpoint regenerates the moment's recorded window and takes no body, so the period and
/// detail controls stay visible but read as fixed rather than silently doing nothing.
struct OperatingStoryConfigSheet: View {
    let theme: OperatingTheme
    let model: BusinessOperatingModel
    /// Called with the moment id once the story exists, so the shell can present the viewer.
    let onGenerated: (String) -> Void

    @Environment(\.dismiss) private var dismiss
    @State private var generating = false
    @State private var error: String?

    private var month: String {
        let raw = model.projection?.startDate?.prefix(10).description
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd"
        formatter.locale = Locale(identifier: "en_US_POSIX")
        guard let raw, let date = formatter.date(from: raw) else {
            return DateFormatter().monthSymbols.first ?? "This month"
        }
        let out = DateFormatter()
        out.dateFormat = "MMMM yyyy"
        out.locale = Locale(identifier: "en_US")
        return out.string(from: date)
    }

    private var hero: String {
        OperatingMoneyFormat.format(model.currency, model.projection?.finance?.sales, visible: model.financeVisible)
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 14) {
                Text("Your \(month) story is ready to compose")
                    .font(.plusJakarta(size: 15))
                    .foregroundStyle(theme.body)
                storyThumb
                OperatingCard(theme: theme) {
                    Text("Create Business Story")
                        .font(.plusJakarta(size: 20, weight: .bold))
                        .foregroundStyle(theme.ink)
                    Text("\(model.title) · \(model.vertical.storyPages.count) pages")
                        .font(.plusJakarta(size: 11))
                        .foregroundStyle(theme.body)
                    Text(model.vertical.storyPages.joined(separator: " · "))
                        .font(.plusJakarta(size: 11))
                        .foregroundStyle(theme.body)
                    HStack(spacing: 8) {
                        OperatingPill(theme: theme, label: "Summary", accent: true)
                        OperatingPill(theme: theme, label: "Detailed")
                    }
                    Text("Period follows the moment's recorded window — regenerating covers that range only.")
                        .font(.plusJakarta(size: 10))
                        .foregroundStyle(theme.body)
                    let hasActivity = !(model.projection?.activity ?? []).isEmpty
                    if !hasActivity {
                        VStack(alignment: .leading, spacing: 8) {
                            Text("⚠")
                                .font(.plusJakarta(size: 18))
                                .foregroundStyle(theme.warning)
                            Text("No verified activity in this period")
                                .font(.plusJakarta(size: 15, weight: .bold))
                                .foregroundStyle(theme.ink)
                            Text("We found no reconciled records for \(month). We won’t show zeros for data we don’t have.")
                                .font(.plusJakarta(size: 12))
                                .foregroundStyle(theme.body)
                            ForEach(["Activity", "Money", "Network"], id: \.self) { label in
                                HStack {
                                    Text(label).foregroundStyle(theme.body)
                                    Spacer()
                                    Text("Not recorded").foregroundStyle(theme.warning)
                                }
                                .font(.plusJakarta(size: 12))
                            }
                        }
                        .padding(16)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .background(theme.inset)
                        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                    }
                    Button {
                        Task { await generate() }
                    } label: {
                        Text(generating ? "Composing…" : (hasActivity ? "Generate story" : "Generate · needs activity"))
                            .font(.plusJakarta(size: 15, weight: .semibold))
                            .foregroundStyle(hasActivity ? theme.onAccent : theme.body)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 14)
                            .background(hasActivity ? theme.accent : theme.inset)
                            .clipShape(RoundedRectangle(cornerRadius: 13))
                    }
                    .buttonStyle(.plain)
                    .disabled(generating || !hasActivity)
                    if let error {
                        Text(error)
                            .font(.plusJakarta(size: 11))
                            .foregroundStyle(theme.negative)
                    }
                    OperatingWhyFootnote(
                        theme: theme,
                        text: "Every page is built from recorded activity. An empty page says so rather than estimating."
                    )
                }
            }
            .padding(18)
        }
        .background(theme.canvas.ignoresSafeArea())
    }

    private var storyThumb: some View {
        HStack {
            Spacer()
            VStack(alignment: .leading, spacing: 0) {
                HStack(spacing: 3) {
                    ForEach(0 ..< 8, id: \.self) { index in
                        Capsule()
                            .fill(index == 0 ? theme.accent : Color.white.opacity(0.22))
                            .frame(height: 2)
                    }
                }
                Spacer()
                Text("THE BUSINESS MOMENT")
                    .font(.plusJakarta(size: 6, weight: .semibold))
                    .foregroundStyle(theme.accent)
                Text(hero)
                    .font(.plusJakarta(size: 30, weight: .bold))
                    .foregroundStyle(theme.ink)
                    .lineLimit(1)
                    .minimumScaleFactor(0.5)
                Text("recorded sales · \(month)")
                    .font(.plusJakarta(size: 7))
                    .foregroundStyle(theme.body)
                    .lineLimit(1)
                Spacer()
                Capsule()
                    .fill(Color.white.opacity(0.6))
                    .frame(width: 46, height: 6)
            }
            .padding(10)
            .frame(width: 140, height: 300)
            .background(
                LinearGradient(
                    colors: [theme.surface, theme.canvas],
                    startPoint: .top,
                    endPoint: .bottom
                )
            )
            .overlay(
                RoundedRectangle(cornerRadius: 16)
                    .stroke(theme.accent.opacity(0.35), lineWidth: 1)
            )
            .clipShape(RoundedRectangle(cornerRadius: 16))
            Spacer()
        }
    }

    private func generate() async {
        generating = true
        error = nil
        defer { generating = false }
        do {
            _ = try await APIClient.shared.createMomentStory(
                momentId: model.momentId,
                idempotencyKey: UUID().uuidString
            )
            onGenerated(model.momentId)
            dismiss()
        } catch {
            self.error = error.localizedDescription
        }
    }
}
