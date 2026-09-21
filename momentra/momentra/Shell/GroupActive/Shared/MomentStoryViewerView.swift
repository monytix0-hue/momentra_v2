import SwiftUI

/// Interactive Moment Story viewer — swipeable chapters + share pack (Phase 1).
struct MomentStoryViewerView: View {
    let momentId: String
    var onClose: () -> Void

    @State private var loading = true
    @State private var errorText: String?
    @State private var story: APIClient.MomentStoryPayload?
    @State private var sharePack: APIClient.MomentStorySharePack?
    @State private var page = 0

    private var chapters: [String] {
        story?.chapters ?? story?.snapshot?.chapters ?? ["cover", "alive", "money", "close"]
    }

    var body: some View {
        ZStack {
            Color(hex: "#2D1F5E").ignoresSafeArea()
            VStack(spacing: 0) {
                HStack {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("momentra")
                            .font(.system(size: 18, weight: .bold))
                            .foregroundStyle(Color(hex: "#F5F0FF"))
                        Text(story?.snapshot?.display?.displayLabel ?? "Moment Story")
                            .font(.system(size: 12))
                            .foregroundStyle(Color(hex: "#C4BDEE"))
                    }
                    Spacer()
                    Button("Close", action: onClose)
                        .foregroundStyle(Color(hex: "#E8621A"))
                }
                .padding(16)

                if loading {
                    Spacer()
                    ProgressView().tint(Color(hex: "#E8621A"))
                    Spacer()
                } else if let errorText, story == nil {
                    Spacer()
                    Text(errorText)
                        .foregroundStyle(Color(hex: "#F5F0FF"))
                        .padding()
                    Button("Retry") { Task { await load() } }
                        .foregroundStyle(Color(hex: "#E8621A"))
                    Spacer()
                } else {
                    TabView(selection: $page) {
                        ForEach(Array(chapters.enumerated()), id: \.offset) { idx, chapter in
                            chapterPage(chapter)
                                .tag(idx)
                                .padding(.horizontal, 12)
                        }
                    }
                    .tabViewStyle(.page(indexDisplayMode: .automatic))

                    HStack {
                        Button {
                            share()
                        } label: {
                            Text("Share")
                                .font(.system(size: 15, weight: .semibold))
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 12)
                                .background(Color(hex: "#E8621A"))
                                .foregroundStyle(.white)
                                .clipShape(RoundedRectangle(cornerRadius: 12))
                        }
                        Text("\(page + 1) / \(chapters.count)")
                            .foregroundStyle(Color(hex: "#C4BDEE"))
                            .font(.system(size: 13))
                    }
                    .padding(16)
                }
            }
        }
        .task { await load() }
    }

    @ViewBuilder
    private func chapterPage(_ chapter: String) -> some View {
        let snap = story?.snapshot
        ScrollView {
            VStack(alignment: .leading, spacing: 12) {
                Text((snap?.display?.coverEyebrow ?? chapter).uppercased())
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundStyle(Color(hex: "#E8621A"))
                switch chapter {
                case "cover":
                    Text(snap?.identity?.title ?? "Moment")
                        .font(.system(size: 28, weight: .bold))
                        .foregroundStyle(Color(hex: "#F5F0FF"))
                    if let range = Self.dateRangeLabel(start: snap?.identity?.startAt, end: snap?.identity?.endAt) {
                        Text(range)
                            .font(.system(size: 13))
                            .foregroundStyle(Color(hex: "#C4BDEE").opacity(0.85))
                    }
                    Text(snap?.narrative?.opening ?? "")
                        .foregroundStyle(Color(hex: "#C4BDEE"))
                    metricGrid(metrics: snap?.metrics, keys: snap?.display?.metricKeys)
                case "alive":
                    Text("How it came alive")
                        .font(.system(size: 24, weight: .bold))
                        .foregroundStyle(Color(hex: "#F5F0FF"))
                    aliveContent(snap)
                case "money":
                    Text("Money & fairness")
                        .font(.system(size: 24, weight: .bold))
                        .foregroundStyle(Color(hex: "#F5F0FF"))
                    moneyContent(snap?.money)
                case "memories":
                    Text("Memories that stayed")
                        .font(.system(size: 24, weight: .bold))
                        .foregroundStyle(Color(hex: "#F5F0FF"))
                    memoriesContent(snap)
                default:
                    Text("TOGETHER · FORWARD")
                        .font(.system(size: 14, weight: .bold))
                        .foregroundStyle(Color(hex: "#E8621A"))
                    Text(snap?.display?.closeLine ?? "Life happens in moments.")
                        .font(.system(size: 22, weight: .semibold))
                        .foregroundStyle(Color(hex: "#F5F0FF"))
                    Text(snap?.identity?.title ?? "Moment")
                        .font(.system(size: 18, weight: .semibold))
                        .foregroundStyle(Color(hex: "#C4BDEE").opacity(0.5))
                    ForEach((snap?.narrative?.insights ?? []).prefix(2), id: \.self) { insight in
                        Text(insight)
                            .font(.system(size: 14))
                            .foregroundStyle(Color(hex: "#C4BDEE"))
                            .padding(.top, 4)
                    }
                }
            }
            .padding(20)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(Color(hex: "#4B3EA8").opacity(0.35))
            .clipShape(RoundedRectangle(cornerRadius: 20))
        }
    }

    @ViewBuilder
    private func aliveContent(_ snap: APIClient.MomentStorySnapshot?) -> some View {
        let timeline = snap?.timeline ?? []
        let decisions = snap?.decisions ?? []
        if timeline.isEmpty {
            Text("The moment unfolded together.")
                .foregroundStyle(Color(hex: "#C4BDEE"))
        } else {
            ForEach(timeline) { item in
                VStack(alignment: .leading, spacing: 4) {
                    if let at = item.at, let formatted = Self.formatStoryDate(at) {
                        Text(formatted)
                            .font(.system(size: 11, weight: .semibold))
                            .foregroundStyle(Color(hex: "#E8621A"))
                    }
                    Text(item.label ?? "Milestone")
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundStyle(Color(hex: "#F5F0FF"))
                    if let detail = item.detail, !detail.isEmpty {
                        Text(detail)
                            .font(.system(size: 13))
                            .foregroundStyle(Color(hex: "#C4BDEE"))
                    }
                }
                .padding(12)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(Color(hex: "#4B3EA8").opacity(0.5))
                .clipShape(RoundedRectangle(cornerRadius: 12))
            }
        }
        if !decisions.isEmpty {
            Text("Decisions")
                .font(.system(size: 13, weight: .semibold))
                .foregroundStyle(Color(hex: "#E8621A"))
                .padding(.top, 4)
            FlowDecisionChips(decisions: decisions)
        }
    }

    @ViewBuilder
    private func memoriesContent(_ snap: APIClient.MomentStorySnapshot?) -> some View {
        let quotes = (snap?.memories ?? []).compactMap { m -> String? in
            guard let text = m.text?.trimmingCharacters(in: .whitespacesAndNewlines), !text.isEmpty else { return nil }
            return text
        }
        if quotes.isEmpty {
            Text("Photos and memories will glow here next time.")
                .foregroundStyle(Color(hex: "#C4BDEE"))
        } else {
            ForEach(Array(quotes.prefix(3)), id: \.self) { quote in
                Text(quote)
                    .font(.system(size: 15, weight: .medium))
                    .foregroundStyle(Color(hex: "#F5F0FF"))
                    .padding(12)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(Color(hex: "#4B3EA8").opacity(0.5))
                    .clipShape(RoundedRectangle(cornerRadius: 12))
            }
        }
        ForEach(snap?.narrative?.insights ?? [], id: \.self) { insight in
            Text(insight)
                .foregroundStyle(Color(hex: "#C4BDEE"))
                .padding(.top, 4)
        }
    }

    @ViewBuilder
    private func moneyContent(_ money: APIClient.MomentStoryMoney?) -> some View {
        let spent = money?.spent ?? 0
        let remaining = money?.remaining ?? 0
        let contributed = money?.contributed ?? 0
        let unsettled = money?.unsettled ?? 0
        let categories = money?.categories ?? []
        let payers = money?.payers ?? []
        let hasMoney = spent > 0 || contributed > 0 || remaining != 0 || unsettled > 0 || !categories.isEmpty || !payers.isEmpty
        if !hasMoney {
            Text("No expenses recorded.")
                .foregroundStyle(Color(hex: "#C4BDEE"))
        } else {
            LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 8) {
                moneyTile(label: "Contributed", value: contributed)
                moneyTile(label: "Spent", value: spent)
                moneyTile(label: "Remaining", value: remaining)
                moneyTile(label: "Unsettled", value: unsettled)
            }
            let onlyOther = categories.count <= 1 && (categories.first?.name ?? "Other").caseInsensitiveCompare("Other") == .orderedSame
            if onlyOther, !payers.isEmpty {
                Text("Who paid")
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundStyle(Color(hex: "#E8621A"))
                    .padding(.top, 4)
                ForEach(payers.prefix(5)) { payer in
                    HStack {
                        Text(payer.name ?? "Someone")
                            .foregroundStyle(Color(hex: "#F5F0FF"))
                        Spacer()
                        Text("₹\(Self.formatInrAmount(payer.amount ?? 0))")
                            .foregroundStyle(Color(hex: "#C4BDEE"))
                    }
                    .padding(12)
                    .background(Color(hex: "#4B3EA8").opacity(0.5))
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                }
            }
            ForEach(categories.prefix(5)) { cat in
                HStack {
                    Text(cat.name ?? "Other")
                        .foregroundStyle(Color(hex: "#F5F0FF"))
                    Spacer()
                    Text("₹\(Self.formatInrAmount(cat.amount ?? 0))")
                        .foregroundStyle(Color(hex: "#C4BDEE"))
                }
                .padding(12)
                .background(Color(hex: "#4B3EA8").opacity(0.5))
                .clipShape(RoundedRectangle(cornerRadius: 12))
            }
        }
    }

    private func moneyTile(label: String, value: Double) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(label)
                .font(.system(size: 11))
                .foregroundStyle(Color(hex: "#C4BDEE"))
            Text("₹\(Self.formatInrAmount(value))")
                .font(.system(size: 16, weight: .bold))
                .foregroundStyle(Color(hex: "#F5F0FF"))
        }
        .padding(12)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color(hex: "#4B3EA8").opacity(0.5))
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }

    private func metricGrid(
        metrics: [String: APIClient.MomentStoryMetricValue]?,
        keys: [APIClient.MomentStoryMetricKey]?
    ) -> some View {
        let defs: [(String, String)] = {
            if let keys, !keys.isEmpty {
                return keys.compactMap { k in
                    guard let key = k.key, !key.isEmpty else { return nil }
                    return (key, k.label ?? key.capitalized)
                }
            }
            return [
                ("people", "People"),
                ("days", "Days"),
                ("plans", "Plans"),
                ("decisions", "Decisions"),
                ("photos", "Photos"),
                ("spent", "Spent"),
            ]
        }()
        let countKeys: Set<String> = ["people", "days", "hours", "months", "plans", "decisions", "photos", "bills"]
        let moneyKeys: Set<String> = ["spent", "raised", "target", "remaining", "contributed"]
        let shown = defs.compactMap { key, label -> (String, String, String)? in
            guard let raw = metrics?[key] else { return nil }
            if countKeys.contains(key), let n = raw.numericValue, n == 0 { return nil }
            let display: String
            if case .string(let s) = raw, moneyKeys.contains(key) {
                display = s
            } else if countKeys.contains(key), let n = raw.numericValue {
                display = String(Int(n.rounded()))
            } else if moneyKeys.contains(key), let n = raw.numericValue {
                display = "₹\(Self.formatInrAmount(n))"
            } else {
                display = Self.formatMetricDisplay(raw, key: key, countKeys: countKeys, moneyKeys: moneyKeys)
            }
            return (key, label, display)
        }.prefix(6)
        return LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible()), GridItem(.flexible())], spacing: 8) {
            ForEach(Array(shown), id: \.0) { item in
                VStack(alignment: .leading, spacing: 4) {
                    Text(item.2)
                        .font(.system(size: 16, weight: .bold))
                        .foregroundStyle(Color(hex: "#F5F0FF"))
                    Text(item.1)
                        .font(.system(size: 11))
                        .foregroundStyle(Color(hex: "#C4BDEE"))
                }
                .padding(12)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(Color(hex: "#4B3EA8").opacity(0.5))
                .clipShape(RoundedRectangle(cornerRadius: 12))
            }
        }
        .padding(.top, 8)
    }

    private static func formatMetricDisplay(
        _ raw: APIClient.MomentStoryMetricValue,
        key: String,
        countKeys: Set<String>,
        moneyKeys: Set<String>
    ) -> String {
        if case .string(let s) = raw { return s }
        if countKeys.contains(key), let n = raw.numericValue { return String(Int(n.rounded())) }
        if moneyKeys.contains(key), let n = raw.numericValue { return "₹\(formatInrAmount(n))" }
        return raw.label
    }

    private static func formatInrAmount(_ value: Double) -> String {
        let formatter = NumberFormatter()
        formatter.numberStyle = .decimal
        formatter.locale = Locale(identifier: "en_IN")
        formatter.maximumFractionDigits = 0
        return formatter.string(from: NSNumber(value: value)) ?? String(Int(value.rounded()))
    }

    private static func dateRangeLabel(start: String?, end: String?) -> String? {
        let s = start.flatMap { formatStoryDate($0) }
        let e = end.flatMap { formatStoryDate($0) }
        switch (s, e) {
        case let (a?, b?) where a != b: return "\(a) · \(b)"
        case let (a?, _): return a
        case let (_, b?): return b
        default: return nil
        }
    }

    private static func formatStoryDate(_ iso: String) -> String? {
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        var date = formatter.date(from: iso)
        if date == nil {
            formatter.formatOptions = [.withInternetDateTime]
            date = formatter.date(from: iso)
        }
        guard let date else { return nil }
        let out = DateFormatter()
        out.dateStyle = .medium
        out.timeStyle = .none
        return out.string(from: date)
    }

    private func load() async {
        loading = true
        errorText = nil
        defer { loading = false }
        do {
            let status = try await APIClient.shared.getMomentStoryStatus(momentId: momentId)
            if status.status == "GENERATING" {
                errorText = "Generating your Moment Story…"
                return
            }
            if status.status == "FAILED" {
                errorText = status.errorMessage ?? "Story generation failed."
                return
            }
            if status.status == "NOT_STARTED" {
                errorText = "No Story yet — complete the moment to unlock it."
                return
            }
            story = try await APIClient.shared.getMomentStory(momentId: momentId)
            do {
                sharePack = try await APIClient.shared.getMomentStorySharePack(momentId: momentId)
            } catch {
                print("[MomentStory] share-pack failed: \(error.localizedDescription)")
                sharePack = nil
            }
        } catch {
            errorText = (error as? LocalizedError)?.errorDescription ?? error.localizedDescription
        }
    }

    private func nonEmptyTrimmed(_ value: String?) -> String? {
        guard let trimmed = value?.trimmingCharacters(in: .whitespacesAndNewlines), !trimmed.isEmpty else {
            return nil
        }
        return trimmed
    }

    private func share() {
        let headline = nonEmptyTrimmed(sharePack?.blurb)
            ?? story?.snapshot?.identity?.title
            ?? "Moment Story"
        let link = nonEmptyTrimmed(sharePack?.webUrl)
            ?? nonEmptyTrimmed(sharePack?.appDeepLink)
            ?? ""
        var parts = [headline]
        if !link.isEmpty {
            parts.append(link)
        }
        if sharePack == nil {
            print("[MomentStory] sharing title fallback — share pack unavailable")
        }
        InviteOutboundShare.presentSystemShare(items: [parts.joined(separator: "\n")])
    }
}

private struct FlowDecisionChips: View {
    let decisions: [APIClient.MomentStoryDecision]

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            ForEach(decisions) { d in
                HStack(spacing: 8) {
                    Text(d.title ?? "Decision")
                        .font(.system(size: 13, weight: .semibold))
                        .foregroundStyle(Color(hex: "#F5F0FF"))
                    if let status = d.status, !status.isEmpty {
                        Text(status)
                            .font(.system(size: 11, weight: .medium))
                            .foregroundStyle(Color(hex: "#E8621A"))
                    }
                }
                .padding(.horizontal, 12)
                .padding(.vertical, 8)
                .background(Color(hex: "#4B3EA8").opacity(0.5))
                .clipShape(RoundedRectangle(cornerRadius: 12))
            }
        }
    }
}
