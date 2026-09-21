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
                    Text(snap?.narrative?.opening ?? "")
                        .foregroundStyle(Color(hex: "#C4BDEE"))
                    metricGrid(snap?.metrics)
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
                    ForEach(snap?.narrative?.insights ?? [], id: \.self) { insight in
                        Text(insight).foregroundStyle(Color(hex: "#C4BDEE"))
                    }
                default:
                    Text("TOGETHER · FORWARD")
                        .font(.system(size: 14, weight: .bold))
                        .foregroundStyle(Color(hex: "#E8621A"))
                    Text(snap?.display?.closeLine ?? "Life happens in moments.")
                        .font(.system(size: 22, weight: .semibold))
                        .foregroundStyle(Color(hex: "#F5F0FF"))
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
    private func moneyContent(_ money: APIClient.MomentStoryMoney?) -> some View {
        let spent = money?.spent ?? 0
        let remaining = money?.remaining ?? 0
        let contributed = money?.contributed ?? 0
        let unsettled = money?.unsettled ?? 0
        let categories = money?.categories ?? []
        let hasMoney = spent > 0 || contributed > 0 || remaining != 0 || unsettled > 0 || !categories.isEmpty
        if !hasMoney {
            Text("No expenses recorded.")
                .foregroundStyle(Color(hex: "#C4BDEE"))
        } else {
            Text("Spent ₹\(Self.formatAmount(spent)) · Remaining ₹\(Self.formatAmount(remaining))")
                .foregroundStyle(Color(hex: "#C4BDEE"))
            if contributed > 0 {
                Text("Contributed ₹\(Self.formatAmount(contributed))")
                    .foregroundStyle(Color(hex: "#C4BDEE"))
            }
            if unsettled > 0 {
                Text("Unsettled ₹\(Self.formatAmount(unsettled))")
                    .foregroundStyle(Color(hex: "#C4BDEE"))
            }
            ForEach(categories.prefix(6)) { cat in
                HStack {
                    Text(cat.name ?? "Other")
                        .foregroundStyle(Color(hex: "#F5F0FF"))
                    Spacer()
                    Text("₹\(Self.formatAmount(cat.amount ?? 0))")
                        .foregroundStyle(Color(hex: "#C4BDEE"))
                }
                .padding(12)
                .background(Color(hex: "#4B3EA8").opacity(0.5))
                .clipShape(RoundedRectangle(cornerRadius: 12))
            }
        }
    }

    private func metricGrid(_ metrics: [String: APIClient.MomentStoryMetricValue]?) -> some View {
        let keys = ["people", "days", "hours", "months", "plans", "decisions", "photos", "spent", "raised"]
        let shown = keys.compactMap { k -> (String, String)? in
            guard let v = metrics?[k] else { return nil }
            return (k, v.label)
        }.prefix(6)
        return LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible()), GridItem(.flexible())], spacing: 8) {
            ForEach(Array(shown), id: \.0) { item in
                VStack(alignment: .leading, spacing: 4) {
                    Text(item.1)
                        .font(.system(size: 16, weight: .bold))
                        .foregroundStyle(Color(hex: "#F5F0FF"))
                    Text(item.0.capitalized)
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

    private static func formatAmount(_ value: Double) -> String {
        if value.rounded() == value { return String(Int(value)) }
        return String(format: "%.0f", value)
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

    private func share() {
        let blurb = sharePack?.blurb?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        let title = story?.snapshot?.identity?.title ?? "Moment Story"
        let link = sharePack?.webUrl?.trimmingCharacters(in: .whitespacesAndNewlines).flatMap { $0.isEmpty ? nil : $0 }
            ?? sharePack?.appDeepLink?.trimmingCharacters(in: .whitespacesAndNewlines).flatMap { $0.isEmpty ? nil : $0 }
            ?? ""
        let text = [blurb.isEmpty ? title : blurb, link]
            .filter { !$0.isEmpty }
            .joined(separator: "\n")
        if sharePack == nil {
            print("[MomentStory] sharing title fallback — share pack unavailable")
        }
        InviteOutboundShare.presentSystemShare(items: [text])
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
