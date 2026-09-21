import SwiftUI

/// Moment Story viewer — cream editorial + purple cover (Figma 5-chapter redesign).
struct MomentStoryViewerView: View {
    let momentId: String
    var onClose: () -> Void

    @State private var loading = true
    @State private var errorText: String?
    @State private var story: APIClient.MomentStoryPayload?
    @State private var sharePack: APIClient.MomentStorySharePack?
    @State private var page = 0

    private var chapters: [String] {
        Self.resolveChapters(story: story)
    }

    private var currentChapter: String {
        chapters.indices.contains(page) ? chapters[page] : "cover"
    }

    private var chromeDark: Bool {
        currentChapter == "cover" || currentChapter == "close"
    }

    private var pageBackground: Color {
        switch currentChapter {
        case "cover": return Color(hex: "#2D1F5E")
        case "close": return Color(hex: "#201E28")
        default: return Color(hex: "#F7F4EE")
        }
    }

    var body: some View {
        ZStack {
            pageBackground.ignoresSafeArea()
            VStack(spacing: 0) {
                HStack {
                    HStack(spacing: 10) {
                        Image("MomentraOfficialLogo")
                            .resizable()
                            .scaledToFit()
                            .frame(width: 28, height: 28)
                        VStack(alignment: .leading, spacing: 2) {
                            Text("momentra")
                                .font(.system(size: 18, weight: .bold))
                                .foregroundStyle(chromeDark ? Color(hex: "#F5F0FF") : Color(hex: "#B45F3D"))
                            Text(story?.snapshot?.display?.displayLabel ?? "Moment Story")
                                .font(.system(size: 12))
                                .foregroundStyle(chromeDark ? Color(hex: "#C4BDEE") : Color(hex: "#746F67"))
                        }
                    }
                    Spacer()
                    Button("Close", action: onClose)
                        .foregroundStyle(chromeDark ? Color(hex: "#E8621A") : Color(hex: "#6C4EF2"))
                }
                .padding(16)

                if loading {
                    Spacer()
                    ProgressView().tint(Color(hex: "#E8621A"))
                    Spacer()
                } else if let errorText, story == nil {
                    Spacer()
                    Text(errorText)
                        .foregroundStyle(chromeDark ? Color(hex: "#F5F0FF") : Color(hex: "#25231F"))
                        .padding()
                    Button("Retry") { Task { await load() } }
                        .foregroundStyle(Color(hex: "#E8621A"))
                    Spacer()
                } else {
                    TabView(selection: $page) {
                        ForEach(Array(chapters.enumerated()), id: \.offset) { idx, chapter in
                            chapterPage(chapter)
                                .tag(idx)
                                .padding(.horizontal, 8)
                        }
                    }
                    .tabViewStyle(.page(indexDisplayMode: .automatic))

                    HStack {
                        Button(action: share) {
                            Text("Share")
                                .font(.system(size: 15, weight: .semibold))
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 12)
                                .background(Color(hex: "#E8621A"))
                                .foregroundStyle(.white)
                                .clipShape(RoundedRectangle(cornerRadius: 12))
                        }
                        Text("\(page + 1) / \(chapters.count)")
                            .foregroundStyle(chromeDark ? Color(hex: "#C4BDEE") : Color(hex: "#746F67"))
                            .font(.system(size: 13))
                    }
                    .padding(16)
                }
            }
        }
        .task { await load() }
    }

    static func resolveChapters(story: APIClient.MomentStoryPayload?) -> [String] {
        let raw = story?.chapters ?? story?.snapshot?.chapters
        if let raw, raw.contains(where: { $0 == "moment" || $0 == "together" }) {
            return raw
        }
        return ["cover", "moment", "together", "money", "close"]
    }

    @ViewBuilder
    private func chapterPage(_ chapter: String) -> some View {
        let snap = story?.snapshot
        switch chapter {
        case "cover":
            coverChapter(snap)
        case "moment", "memories":
            momentChapter(snap)
        case "together", "alive":
            togetherChapter(snap)
        case "money":
            moneyChapter(snap)
        default:
            closeChapter(snap)
        }
    }

    // MARK: - Cover

    @ViewBuilder
    private func coverChapter(_ snap: APIClient.MomentStorySnapshot?) -> some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 12) {
                Text((snap?.display?.coverEyebrow ?? "Moment Story").uppercased())
                    .font(.system(size: 11, weight: .semibold))
                    .tracking(1)
                    .foregroundStyle(Color(hex: "#E8621A"))
                Text(snap?.identity?.title ?? "Moment")
                    .font(.system(size: 28, weight: .bold, design: .serif))
                    .foregroundStyle(Color(hex: "#F5F0FF"))
                HStack(spacing: 8) {
                    if let place = snap?.places?.first?.label, !place.isEmpty {
                        coverPill(place, Color(hex: "#0A6640"))
                    }
                    if let range = Self.dateRangeLabel(start: snap?.identity?.startAt, end: snap?.identity?.endAt) {
                        coverPill(range, Color(hex: "#8C83D4"))
                    }
                    let people = Self.metricInt(snap?.metrics, "people")
                    let days = Self.metricInt(snap?.metrics, "days")
                    if people != nil || days != nil {
                        let bits = [people.map { "\($0) people" }, days.map { "\($0) days" }].compactMap { $0 }
                        coverPill(bits.joined(separator: " · "), Color(hex: "#4B3EA8"))
                    }
                }
                if let urlStr = snap?.photos?.first?.url, let url = URL(string: urlStr) {
                    AsyncImage(url: url) { phase in
                        switch phase {
                        case .success(let img):
                            img.resizable().scaledToFill()
                        default:
                            Color(hex: "#4B3EA8").opacity(0.4)
                        }
                    }
                    .frame(height: 180)
                    .clipShape(RoundedRectangle(cornerRadius: 16))
                }
                Text(snap?.narrative?.opening ?? "")
                    .foregroundStyle(Color(hex: "#C4BDEE"))
                metricGrid(metrics: snap?.metrics, keys: snap?.display?.metricKeys, onDark: true)
            }
            .padding(20)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(Color(hex: "#4B3EA8").opacity(0.35))
            .clipShape(RoundedRectangle(cornerRadius: 20))
        }
    }

    private func coverPill(_ text: String, _ bg: Color) -> some View {
        Text(text)
            .font(.system(size: 11, weight: .semibold))
            .foregroundStyle(Color(hex: "#F5F0FF"))
            .padding(.horizontal, 10)
            .padding(.vertical, 6)
            .background(bg)
            .clipShape(Capsule())
    }

    // MARK: - Moment

    @ViewBuilder
    private func momentChapter(_ snap: APIClient.MomentStorySnapshot?) -> some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 12) {
                Text("A MOMENT TO REMEMBER")
                    .font(.system(size: 10, weight: .semibold))
                    .tracking(1.2)
                    .foregroundStyle(Color(hex: "#B45F3D"))
                Text(snap?.identity?.title ?? "Moment")
                    .font(.system(size: 26, weight: .regular, design: .serif))
                    .foregroundStyle(Color(hex: "#25231F"))
                Text(snap?.narrative?.opening ?? "")
                    .font(.system(size: 16))
                    .foregroundStyle(Color(hex: "#25231F"))
                Text("The Shape of This Moment")
                    .font(.system(size: 20, design: .serif))
                    .foregroundStyle(Color(hex: "#25231F"))
                    .padding(.top, 8)
                Text("Facts that hold the whole story.")
                    .font(.system(size: 13))
                    .foregroundStyle(Color(hex: "#746F67"))
                factRows(snap)
                Text("How the Moment Came Together")
                    .font(.system(size: 18, design: .serif))
                    .foregroundStyle(Color(hex: "#25231F"))
                    .padding(.top, 8)
                timelineList(Array((snap?.timeline ?? []).prefix(6)), dark: false)
                let mosaicUrls = (snap?.photos ?? []).compactMap { $0.url }.filter { !$0.isEmpty }.prefix(4).compactMap { URL(string: $0) }
                if !mosaicUrls.isEmpty {
                    Text("PHOTO STORY")
                        .font(.system(size: 10, weight: .semibold))
                        .tracking(1.2)
                        .foregroundStyle(Color(hex: "#B45F3D"))
                        .padding(.top, 8)
                    Text("Celebrations, held close.")
                        .font(.system(size: 22, design: .serif))
                        .foregroundStyle(Color(hex: "#25231F"))
                    photoMosaic(Array(mosaicUrls))
                }
            }
            .padding(20)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(Color(hex: "#FFFEFB"))
            .clipShape(RoundedRectangle(cornerRadius: 20))
        }
    }

    // MARK: - Together

    @ViewBuilder
    private func togetherChapter(_ snap: APIClient.MomentStorySnapshot?) -> some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 12) {
                HStack(spacing: 8) {
                    Circle().fill(Color(hex: "#6C4EF2")).frame(width: 8, height: 8)
                    Text("HOW IT CAME TOGETHER")
                        .font(.system(size: 11, weight: .bold))
                        .tracking(1)
                        .foregroundStyle(Color(hex: "#6C4EF2"))
                }
                Text("How the Moment came together")
                    .font(.system(size: 26, design: .serif))
                    .foregroundStyle(Color(hex: "#25231F"))
                Text("From the first invite to the last shared update.")
                    .font(.system(size: 14))
                    .foregroundStyle(Color(hex: "#746F67"))
                let peopleCount = Self.metricInt(snap?.metrics, "people") ?? snap?.people?.count ?? 0
                VStack(alignment: .leading, spacing: 6) {
                    Text("\(peopleCount) PEOPLE · NO RANKINGS")
                        .font(.system(size: 11, weight: .bold))
                        .foregroundStyle(Color(hex: "#C4BDEE"))
                    Text("No rankings. Just many ways of showing up.")
                        .font(.system(size: 13))
                        .foregroundStyle(Color.white.opacity(0.85))
                }
                .padding(16)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(Color(hex: "#201E28"))
                .clipShape(RoundedRectangle(cornerRadius: 16))
                timelineList(snap?.timeline ?? [], dark: false)
                Text("INVISIBLE PREP")
                    .font(.system(size: 11, weight: .bold))
                    .tracking(1)
                    .foregroundStyle(Color(hex: "#6C4EF2"))
                HStack(spacing: 8) {
                    let plansN = Self.metricInt(snap?.metrics, "plans")
                    prepCard((plansN.map { $0 > 0 ? "\($0)" : "—" }) ?? "—", "Plans")
                    prepCard(
                        snap?.metrics?["contributed"]?.label
                            ?? snap?.metrics?["raised"]?.label
                            ?? "—",
                        "Contributed"
                    )
                    let decisionsN = Self.metricInt(snap?.metrics, "decisions")
                    prepCard((decisionsN.map { $0 > 0 ? "\($0)" : "—" }) ?? "—", "Decisions")
                }
                let decisions = snap?.decisions ?? []
                if !decisions.isEmpty {
                    Text("Shared decisions")
                        .font(.system(size: 13, weight: .semibold))
                        .foregroundStyle(Color(hex: "#B45F3D"))
                    ForEach(decisions.prefix(4)) { d in
                        Text(d.title ?? "Decision")
                            .foregroundStyle(Color(hex: "#25231F"))
                            .padding(12)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .background(Color(hex: "#F7F4EE"))
                            .clipShape(RoundedRectangle(cornerRadius: 12))
                    }
                }
            }
            .padding(20)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(Color(hex: "#FFFEFB"))
            .clipShape(RoundedRectangle(cornerRadius: 20))
        }
    }

    private func prepCard(_ value: String, _ label: String) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(value)
                .font(.system(size: 16, weight: .bold))
                .foregroundStyle(.white)
            Text(label)
                .font(.system(size: 11))
                .foregroundStyle(Color(hex: "#C4BDEE"))
        }
        .padding(12)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color(hex: "#201E28"))
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }

    // MARK: - Money

    @ViewBuilder
    private func moneyChapter(_ snap: APIClient.MomentStorySnapshot?) -> some View {
        let money = snap?.money
        let spent = money?.spent ?? 0
        let target = money?.target
        let pct: Double? = {
            guard let target, target > 0 else { return nil }
            return (spent / target * 1000).rounded() / 10
        }()
        ScrollView {
            VStack(alignment: .leading, spacing: 12) {
                HStack(spacing: 8) {
                    Circle().fill(Color(hex: "#6C4EF2")).frame(width: 8, height: 8)
                    Text("THE MONEY BEHIND THE MOMENT")
                        .font(.system(size: 11, weight: .bold))
                        .tracking(1)
                        .foregroundStyle(Color(hex: "#6C4EF2"))
                }
                Text("The Money Behind the Moment")
                    .font(.system(size: 26, design: .serif))
                    .foregroundStyle(Color(hex: "#25231F"))
                Text("How contributions became days together.")
                    .font(.system(size: 14))
                    .foregroundStyle(Color(hex: "#746F67"))
                HStack {
                    Text("BUDGET AT A GLANCE")
                        .font(.system(size: 11, weight: .bold))
                        .foregroundStyle(Color(hex: "#6C4EF2"))
                    Spacer()
                    if let pct {
                        Text("\(Self.formatPlain(pct))% budget used")
                            .font(.system(size: 14, design: .serif))
                            .foregroundStyle(Color(hex: "#25231F"))
                    }
                }
                LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 8) {
                    moneyFlowTile("Contributed", money?.contributed ?? 0)
                    moneyFlowTile("Spent", spent)
                    moneyFlowTile("Remaining", money?.remaining ?? 0)
                    moneyFlowTile("Unsettled", money?.unsettled ?? 0)
                }
                if (money?.unsettled ?? 0) <= 0, spent > 0 {
                    Text("✓  All recorded balances settled.")
                        .font(.system(size: 13))
                        .foregroundStyle(Color(hex: "#0A6640"))
                }
                let categories = money?.categories ?? []
                if !categories.isEmpty, spent > 0 {
                    Text("WHERE THE MONEY WENT")
                        .font(.system(size: 11, weight: .bold))
                        .foregroundStyle(Color(hex: "#6C4EF2"))
                        .padding(.top, 4)
                    ForEach(categories.prefix(5)) { cat in
                        let amount = cat.amount ?? 0
                        let pctCat = Int((amount / spent * 100).rounded())
                        HStack {
                            Text(cat.name ?? "Other").foregroundStyle(Color(hex: "#25231F"))
                            Spacer()
                            Text("\(pctCat)%").bold().foregroundStyle(Color(hex: "#25231F"))
                        }
                        .font(.system(size: 13))
                        GeometryReader { geo in
                            ZStack(alignment: .leading) {
                                Capsule().fill(Color(hex: "#C4BDEE"))
                                Capsule()
                                    .fill(Color(hex: "#6C4EF2"))
                                    .frame(width: max(8, geo.size.width * CGFloat(pctCat) / 100))
                            }
                        }
                        .frame(height: 7)
                    }
                }
                let expenses = money?.expenses ?? []
                if !expenses.isEmpty {
                    Text("EXPENSES BEHIND THE MOMENT")
                        .font(.system(size: 11, weight: .bold))
                        .foregroundStyle(Color(hex: "#6C4EF2"))
                        .padding(.top, 4)
                    VStack(alignment: .leading, spacing: 8) {
                        ForEach(expenses.prefix(6)) { e in
                            HStack {
                                VStack(alignment: .leading, spacing: 2) {
                                    Text(Self.cleanExpenseTitle(e.description))
                                        .foregroundStyle(.white)
                                        .font(.system(size: 12))
                                    Text([e.category, e.payer].compactMap { $0 }.joined(separator: " · "))
                                        .foregroundStyle(Color(hex: "#C4BDEE"))
                                        .font(.system(size: 10))
                                }
                                Spacer()
                                Text("₹\(Self.formatInrAmount(e.amount ?? 0))")
                                    .bold()
                                    .foregroundStyle(.white)
                                    .font(.system(size: 12))
                            }
                        }
                    }
                    .padding(14)
                    .background(Color(hex: "#201E28"))
                    .clipShape(RoundedRectangle(cornerRadius: 16))
                }
                if let insight = snap?.narrative?.insights?.first(where: {
                    $0.localizedCaseInsensitiveContains("accounted") || $0.localizedCaseInsensitiveContains("spend")
                }) {
                    VStack(alignment: .leading, spacing: 6) {
                        Text("HUMAN INSIGHT")
                            .font(.system(size: 12, weight: .bold))
                            .foregroundStyle(Color(hex: "#B45F3D"))
                        Text(insight)
                            .font(.system(size: 13))
                            .foregroundStyle(Color(hex: "#746F67"))
                    }
                    .padding(16)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(Color(hex: "#F4E5DA"))
                    .clipShape(RoundedRectangle(cornerRadius: 16))
                }
                if money == nil || (spent <= 0 && (money?.contributed ?? 0) <= 0) {
                    Text("No expenses recorded.")
                        .foregroundStyle(Color(hex: "#746F67"))
                }
            }
            .padding(20)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(Color(hex: "#F7F4EE"))
            .clipShape(RoundedRectangle(cornerRadius: 20))
        }
    }

    private func moneyFlowTile(_ label: String, _ value: Double) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text("₹\(Self.formatInrAmount(value))")
                .font(.system(size: 16, weight: .bold))
                .foregroundStyle(Color(hex: "#25231F"))
            Text(label)
                .font(.system(size: 11))
                .foregroundStyle(Color(hex: "#746F67"))
        }
        .padding(12)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color(hex: "#FFFEFB"))
        .clipShape(RoundedRectangle(cornerRadius: 14))
    }

    // MARK: - Close

    @ViewBuilder
    private func closeChapter(_ snap: APIClient.MomentStorySnapshot?) -> some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 12) {
                Image("MomentraOfficialLogo")
                    .resizable()
                    .scaledToFit()
                    .frame(width: 36, height: 36)
                Text("TOGETHER · FORWARD")
                    .font(.system(size: 12, weight: .bold))
                    .tracking(1)
                    .foregroundStyle(Color(hex: "#E8621A"))
                Text("The celebration ended.\nThe Moment stayed.")
                    .font(.system(size: 28, design: .serif))
                    .foregroundStyle(.white)
                Text(snap?.display?.closeLine ?? "Life happens in moments.")
                    .font(.system(size: 16))
                    .foregroundStyle(Color(hex: "#C4BDEE"))
                Text(snap?.identity?.title ?? "")
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundStyle(Color.white.opacity(0.45))
                    .padding(.top, 8)
                if let plans = Self.metricInt(snap?.metrics, "plans"), plans > 0 {
                    Text("✓  \(plans) plans on the checklist")
                        .foregroundStyle(Color(hex: "#C4BDEE"))
                }
                if let decisions = Self.metricInt(snap?.metrics, "decisions"), decisions > 0 {
                    Text("✓  \(decisions) decisions closed")
                        .foregroundStyle(Color(hex: "#C4BDEE"))
                }
                if (snap?.money?.unsettled ?? 0) <= 0, (snap?.money?.spent ?? 0) > 0 {
                    Text("✓  All balances settled")
                        .foregroundStyle(Color(hex: "#C4BDEE"))
                }
                ForEach(Array((snap?.narrative?.insights ?? []).prefix(2)), id: \.self) { insight in
                    Text(insight)
                        .font(.system(size: 13))
                        .foregroundStyle(Color(hex: "#C4BDEE"))
                        .padding(.top, 4)
                }
            }
            .padding(24)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(Color(hex: "#201E28"))
            .clipShape(RoundedRectangle(cornerRadius: 20))
        }
    }

    // MARK: - Shared pieces

    @ViewBuilder
    private func photoMosaic(_ urls: [URL]) -> some View {
        VStack(spacing: 8) {
            if let first = urls.first {
                AsyncImage(url: first) { phase in
                    switch phase {
                    case .success(let img):
                        img.resizable().scaledToFill()
                    default:
                        Color(hex: "#E7E0D6")
                    }
                }
                .frame(maxWidth: .infinity)
                .frame(height: 180)
                .clipShape(RoundedRectangle(cornerRadius: 16))
            }
            if urls.count > 1 {
                HStack(spacing: 8) {
                    ForEach(Array(urls.dropFirst().prefix(2).enumerated()), id: \.offset) { _, url in
                        AsyncImage(url: url) { phase in
                            switch phase {
                            case .success(let img):
                                img.resizable().scaledToFill()
                            default:
                                Color(hex: "#E7E0D6")
                            }
                        }
                        .frame(maxWidth: .infinity)
                        .frame(height: 110)
                        .clipShape(RoundedRectangle(cornerRadius: 14))
                    }
                    if urls.count == 2 {
                        Color.clear.frame(maxWidth: .infinity).frame(height: 110)
                    }
                }
            }
            if urls.count > 3, let fourth = urls.dropFirst(3).first {
                AsyncImage(url: fourth) { phase in
                    switch phase {
                    case .success(let img):
                        img.resizable().scaledToFill()
                    default:
                        Color(hex: "#E7E0D6")
                    }
                }
                .frame(maxWidth: .infinity)
                .frame(height: 140)
                .clipShape(RoundedRectangle(cornerRadius: 14))
            }
        }
    }

    @ViewBuilder
    private func factRows(_ snap: APIClient.MomentStorySnapshot?) -> some View {
        let metrics = snap?.metrics ?? [:]
        let defs: [(String, String)] = {
            if let keys = snap?.display?.metricKeys, !keys.isEmpty {
                return keys.compactMap { k in
                    guard let key = k.key, !key.isEmpty else { return nil }
                    return (key, k.label ?? key.capitalized)
                }
            }
            return [
                ("people", "People"), ("days", "Days"), ("plans", "Plans"),
                ("spent", "Spent"), ("decisions", "Decisions"), ("photos", "Photos"),
            ]
        }()
        let countKeys: Set<String> = ["people", "days", "hours", "months", "plans", "decisions", "photos", "bills"]
        let moneyKeys: Set<String> = ["spent", "raised", "target", "remaining", "contributed"]
        ForEach(Array(defs.prefix(6)), id: \.0) { key, label in
            if let raw = metrics[key] {
                if !(countKeys.contains(key) && (raw.numericValue ?? -1) == 0) {
                    let value = Self.formatMetricDisplay(raw, key: key, countKeys: countKeys, moneyKeys: moneyKeys)
                    HStack(alignment: .top, spacing: 12) {
                        Text("\(value) \(label)")
                            .font(.system(size: 12, weight: .bold))
                            .foregroundStyle(Color(hex: "#B45F3D"))
                            .frame(width: 130, alignment: .leading)
                        Text(Self.factBlurb(key: key, value: value, label: label))
                            .font(.system(size: 13))
                            .foregroundStyle(Color(hex: "#25231F"))
                    }
                }
            }
        }
    }

    @ViewBuilder
    private func timelineList(_ items: [APIClient.MomentStoryTimelineItem], dark: Bool) -> some View {
        if items.isEmpty {
            Text("The moment unfolded together.")
                .foregroundStyle(dark ? Color(hex: "#C4BDEE") : Color(hex: "#746F67"))
        } else {
            ForEach(items) { item in
                HStack(alignment: .top, spacing: 10) {
                    Circle()
                        .fill(Color(hex: "#C79545"))
                        .frame(width: 8, height: 8)
                        .padding(.top, 4)
                    VStack(alignment: .leading, spacing: 2) {
                        if let at = item.at, let formatted = Self.formatStoryDateShort(at) {
                            Text(formatted)
                                .font(.system(size: 10, weight: .semibold))
                                .foregroundStyle(Color(hex: "#B45F3D"))
                        }
                        Text(item.label ?? "Milestone")
                            .font(.system(size: 13, weight: .semibold))
                            .foregroundStyle(dark ? Color(hex: "#F5F0FF") : Color(hex: "#25231F"))
                        if let detail = item.detail, !detail.isEmpty {
                            Text(detail)
                                .font(.system(size: 12))
                                .foregroundStyle(dark ? Color(hex: "#C4BDEE") : Color(hex: "#746F67"))
                        }
                    }
                }
            }
        }
    }

    private func metricGrid(
        metrics: [String: APIClient.MomentStoryMetricValue]?,
        keys: [APIClient.MomentStoryMetricKey]?,
        onDark: Bool
    ) -> some View {
        let defs: [(String, String)] = {
            if let keys, !keys.isEmpty {
                return keys.compactMap { k in
                    guard let key = k.key, !key.isEmpty else { return nil }
                    return (key, k.label ?? key.capitalized)
                }
            }
            return [
                ("people", "People"), ("days", "Days"), ("plans", "Plans"),
                ("decisions", "Decisions"), ("photos", "Photos"), ("spent", "Spent"),
            ]
        }()
        let countKeys: Set<String> = ["people", "days", "hours", "months", "plans", "decisions", "photos", "bills"]
        let moneyKeys: Set<String> = ["spent", "raised", "target", "remaining", "contributed"]
        let shown = defs.compactMap { key, label -> (String, String, String)? in
            guard let raw = metrics?[key] else { return nil }
            if countKeys.contains(key), let n = raw.numericValue, n == 0 { return nil }
            return (key, label, Self.formatMetricDisplay(raw, key: key, countKeys: countKeys, moneyKeys: moneyKeys))
        }.prefix(6)
        return LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible()), GridItem(.flexible())], spacing: 8) {
            ForEach(Array(shown), id: \.0) { item in
                VStack(alignment: .leading, spacing: 4) {
                    Text(item.2)
                        .font(.system(size: 16, weight: .bold))
                        .foregroundStyle(onDark ? Color(hex: "#F5F0FF") : Color(hex: "#25231F"))
                    Text(item.1)
                        .font(.system(size: 11))
                        .foregroundStyle(onDark ? Color(hex: "#C4BDEE") : Color(hex: "#746F67"))
                }
                .padding(12)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(onDark ? Color(hex: "#4B3EA8").opacity(0.55) : Color(hex: "#F7F4EE"))
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
        if !link.isEmpty { parts.append(link) }
        if sharePack == nil {
            print("[MomentStory] sharing title fallback — share pack unavailable")
        }
        InviteOutboundShare.presentSystemShare(items: [parts.joined(separator: "\n")])
    }

    // MARK: - Formatters

    private static func metricInt(_ metrics: [String: APIClient.MomentStoryMetricValue]?, _ key: String) -> Int? {
        guard let n = metrics?[key]?.numericValue else { return nil }
        return Int(n.rounded())
    }

    private static func formatMetricDisplay(
        _ raw: APIClient.MomentStoryMetricValue,
        key: String,
        countKeys: Set<String>,
        moneyKeys: Set<String>
    ) -> String {
        if case .string(let s) = raw, moneyKeys.contains(key) { return s }
        if countKeys.contains(key), let n = raw.numericValue { return String(Int(n.rounded())) }
        if moneyKeys.contains(key), let n = raw.numericValue { return "₹\(formatInrAmount(n))" }
        if case .string(let s) = raw { return s }
        return raw.label
    }

    private static func formatInrAmount(_ value: Double) -> String {
        let formatter = NumberFormatter()
        formatter.numberStyle = .decimal
        formatter.locale = Locale(identifier: "en_IN")
        formatter.maximumFractionDigits = 0
        return formatter.string(from: NSNumber(value: value)) ?? String(Int(value.rounded()))
    }

    /// Strip trailing " | Category" so titles don't duplicate the category subline.
    private static func cleanExpenseTitle(_ description: String?) -> String {
        guard let raw = description?.trimmingCharacters(in: .whitespacesAndNewlines), !raw.isEmpty else {
            return "Expense"
        }
        if let range = raw.range(of: " | ", options: .backwards) {
            let note = String(raw[..<range.lowerBound]).trimmingCharacters(in: .whitespacesAndNewlines)
            return note.isEmpty ? "Expense" : note
        }
        return raw
    }

    private static func formatPlain(_ value: Double) -> String {
        if value.rounded() == value { return String(Int(value)) }
        return String(format: "%g", value)
    }

    private static func factBlurb(key: String, value: String, label: String) -> String {
        switch key {
        case "people": return "\(value) people shared this Moment."
        case "days", "hours", "months": return "Together across \(value) \(label.lowercased())."
        case "plans": return "Plans moved from checklist to reality."
        case "decisions": return "Decisions were made together."
        case "photos": return "\(value) photos and memories were captured."
        case "spent", "raised", "contributed": return "The money story landed with the group."
        default: return "\(label): \(value)"
        }
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
        guard let date = parseISO(iso) else { return nil }
        let out = DateFormatter()
        out.dateStyle = .medium
        out.timeStyle = .none
        return out.string(from: date)
    }

    private static func formatStoryDateShort(_ iso: String) -> String? {
        guard let date = parseISO(iso) else { return nil }
        let out = DateFormatter()
        out.locale = Locale(identifier: "en_US_POSIX")
        out.dateFormat = "d MMM"
        return out.string(from: date).uppercased()
    }

    private static func parseISO(_ iso: String) -> Date? {
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        if let d = formatter.date(from: iso) { return d }
        formatter.formatOptions = [.withInternetDateTime]
        return formatter.date(from: iso)
    }
}
