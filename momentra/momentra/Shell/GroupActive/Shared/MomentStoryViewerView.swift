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
                    Text("Plans, people, and decisions that shaped this moment.")
                        .foregroundStyle(Color(hex: "#C4BDEE"))
                case "money":
                    Text("Money & fairness")
                        .font(.system(size: 24, weight: .bold))
                        .foregroundStyle(Color(hex: "#F5F0FF"))
                    Text("See contributions, spend, and what’s left to settle.")
                        .foregroundStyle(Color(hex: "#C4BDEE"))
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

    private func metricGrid(_ metrics: [String: APIClient.MomentStoryMetricValue]?) -> some View {
        let keys = ["people", "days", "hours", "months", "plans", "photos", "spent", "raised"]
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
            sharePack = try? await APIClient.shared.getMomentStorySharePack(momentId: momentId)
        } catch {
            errorText = (error as? LocalizedError)?.errorDescription ?? error.localizedDescription
        }
    }

    private func share() {
        let text = [
            sharePack?.blurb ?? story?.snapshot?.identity?.title ?? "Moment Story",
            sharePack?.webUrl ?? sharePack?.webPath ?? sharePack?.appDeepLink ?? "",
        ].joined(separator: "\n")
        let av = UIActivityViewController(activityItems: [text], applicationActivities: nil)
        guard let scene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
              let root = scene.windows.first(where: { $0.isKeyWindow })?.rootViewController
                ?? scene.windows.first?.rootViewController else { return }
        root.present(av, animated: true)
    }
}
