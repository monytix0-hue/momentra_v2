import SwiftUI

/// Moment Story viewer — cream editorial + purple cover (Figma 5-chapter redesign).
struct MomentStoryViewerView: View {
    let momentId: String
    var onClose: () -> Void

    @State private var phase = "loading"
    @State private var message: String?
    @State private var story: APIClient.MomentStoryPayload?
    @State private var page = 0
    @State private var sharing = false
    @State private var shareNotice: String?

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
                            .frame(width: 108, height: 40)
                            .padding(.horizontal, chromeDark ? 0 : 8)
                            .padding(.vertical, chromeDark ? 0 : 4)
                            .background(chromeDark ? Color.clear : Color(hex: "#201E28"))
                            .clipShape(RoundedRectangle(cornerRadius: 10))
                        Text(story?.snapshot?.display?.displayLabel ?? "Moment Story")
                            .font(.system(size: 12))
                            .foregroundStyle(chromeDark ? Color(hex: "#C4BDEE") : Color(hex: "#746F67"))
                    }
                    Spacer()
                    Button("Close", action: onClose)
                        .foregroundStyle(chromeDark ? Color(hex: "#E8621A") : Color(hex: "#6C4EF2"))
                }
                .padding(16)

                if phase == "loading" || phase == "generating" {
                    Spacer()
                    ProgressView().tint(Color(hex: "#E8621A"))
                    if phase == "generating" {
                        Text("Generating your Moment Story…")
                            .foregroundStyle(chromeDark ? Color(hex: "#F5F0FF") : Color(hex: "#25231F"))
                            .padding(.top, 12)
                    }
                    Spacer()
                } else if phase == "failed" || phase == "error" {
                    Spacer()
                    Text(message ?? "Could not load Story")
                        .foregroundStyle(chromeDark ? Color(hex: "#F5F0FF") : Color(hex: "#25231F"))
                        .padding()
                    Button("Retry") { Task { await load() } }
                        .foregroundStyle(Color(hex: "#E8621A"))
                    Spacer()
                } else if phase == "not_started" {
                    Spacer()
                    Text(message ?? "No Story yet — complete the moment to unlock it.")
                        .foregroundStyle(chromeDark ? Color(hex: "#F5F0FF") : Color(hex: "#25231F"))
                        .padding()
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

                    HStack(spacing: 8) {
                        Button(action: { Task { await share() } }) {
                            Text("Share")
                                .font(.system(size: 15, weight: .semibold))
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 12)
                                .background(Color(hex: "#E8621A"))
                                .foregroundStyle(.white)
                                .clipShape(RoundedRectangle(cornerRadius: 12))
                        }
                        .disabled(sharing)
                        Button(action: { Task { await sharePdf() } }) {
                            Text("Share PDF")
                                .font(.system(size: 15, weight: .semibold))
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 12)
                                .background(Color(hex: "#2D1F5E"))
                                .foregroundStyle(.white)
                                .clipShape(RoundedRectangle(cornerRadius: 12))
                        }
                        .disabled(sharing)
                        Text("\(page + 1) / \(chapters.count)")
                            .foregroundStyle(chromeDark ? Color(hex: "#C4BDEE") : Color(hex: "#746F67"))
                            .font(.system(size: 13))
                    }
                    .padding(16)
                }
            }
        }
        .task { await load() }
        .alert("Moment Story", isPresented: Binding(
            get: { shareNotice != nil },
            set: { if !$0 { shareNotice = nil } }
        )) {
            Button("OK", role: .cancel) {}
        } message: {
            Text(shareNotice ?? "")
        }
    }

    static func moneyChapterEmpty(_ story: APIClient.MomentStoryPayload?) -> Bool {
        guard let money = story?.snapshot?.money else { return true }
        return (money.contributed ?? 0) == 0
            && (money.spent ?? 0) == 0
            && (money.remaining ?? 0) == 0
            && (money.expenses ?? []).isEmpty
    }

    static func resolveChapters(story: APIClient.MomentStoryPayload?) -> [String] {
        let raw = story?.chapters ?? story?.snapshot?.chapters
        let chapters: [String]
        if let raw, raw.contains(where: { $0 == "moment" || $0 == "together" }) {
            chapters = raw
        } else {
            chapters = ["cover", "moment", "together", "money", "close"]
        }
        return moneyChapterEmpty(story) ? chapters.filter { $0 != "money" } : chapters
    }

    private func storyCurrency(_ snap: APIClient.MomentStorySnapshot?) -> String {
        if let code = snap?.identity?.currencyCode, code.count == 3 { return code }
        return "INR"
    }

    private func celebrationFamily(_ family: String?) -> Bool {
        family == "HOUSE_PARTY" || family == "WEDDING" || family == "SHARED_EXPERIENCE"
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
                metricGrid(metrics: snap?.metrics, keys: snap?.display?.metricKeys, onDark: true, currencyCode: storyCurrency(snap))
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
                let mosaic = (snap?.photos ?? []).compactMap { photo -> (url: URL, title: String?)? in
                    guard let raw = photo.url, !raw.isEmpty, let url = URL(string: raw) else { return nil }
                    return (url, memoryPhotoTitle(photo.title))
                }
                if !mosaic.isEmpty {
                    Text("PHOTO STORY")
                        .font(.system(size: 10, weight: .semibold))
                        .tracking(1.2)
                        .foregroundStyle(Color(hex: "#B45F3D"))
                        .padding(.top, 8)
                    Text("Celebrations, held close.")
                        .font(.system(size: 22, design: .serif))
                        .foregroundStyle(Color(hex: "#25231F"))
                    photoMosaic(mosaic)
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
                let currency = storyCurrency(snap)
                LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 8) {
                    moneyFlowTile("Contributed", money?.contributed ?? 0, currency)
                    moneyFlowTile("Spent", spent, currency)
                    moneyFlowTile("Remaining", money?.remaining ?? 0, currency)
                    moneyFlowTile("Unsettled", money?.unsettled ?? 0, currency)
                }
                if (money?.unsettled ?? 0) <= 0, spent > 0 {
                    Text("✓  All recorded balances settled.")
                        .font(.system(size: 13))
                        .foregroundStyle(Color(hex: "#0A6640"))
                }
                let categories = (money?.categories ?? []).filter { ($0.amount ?? 0) > 0 }
                if !categories.isEmpty, spent > 0 {
                    Text("WHERE THE MONEY WENT")
                        .font(.system(size: 11, weight: .bold))
                        .foregroundStyle(Color(hex: "#6C4EF2"))
                        .padding(.top, 4)
                    categoryDonut(categories, spent: spent, currency: currency)
                }
                let expenses = money?.expenses ?? []
                if !expenses.isEmpty {
                    Text("EXPENSES BEHIND THE MOMENT")
                        .font(.system(size: 11, weight: .bold))
                        .foregroundStyle(Color(hex: "#6C4EF2"))
                        .padding(.top, 4)
                    ForEach(Array(Self.groupStoryExpenses(expenses).enumerated()), id: \.offset) { _, day in
                        HStack {
                            Text(day.label)
                                .font(.system(size: 13, weight: .bold))
                                .foregroundStyle(Color(hex: "#25231F"))
                            Spacer()
                            Text(Self.formatStoryMoney(day.total, currencyCode: currency))
                                .font(.system(size: 12))
                                .foregroundStyle(Color(hex: "#746F67"))
                        }
                        VStack(alignment: .leading, spacing: 8) {
                            ForEach(Array(day.items.enumerated()), id: \.offset) { _, e in
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
                                    Text(Self.formatStoryMoney(e.amount ?? 0, currencyCode: currency))
                                        .bold()
                                        .foregroundStyle(.white)
                                        .font(.system(size: 12))
                                }
                            }
                        }
                        .padding(14)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .background(Color(hex: "#201E28"))
                        .clipShape(RoundedRectangle(cornerRadius: 16))
                    }
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

    private func moneyFlowTile(_ label: String, _ value: Double, _ currencyCode: String) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(Self.formatStoryMoney(value, currencyCode: currencyCode))
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
                    .frame(width: 130, height: 48)
                Text("TOGETHER · FORWARD")
                    .font(.system(size: 12, weight: .bold))
                    .tracking(1)
                    .foregroundStyle(Color(hex: "#E8621A"))
                let celebration = celebrationFamily(snap?.identity?.familyProfile)
                Text(celebration ? "The celebration ended.\nThe Moment stayed." : (snap?.display?.closeLine ?? "Life happens in moments."))
                    .font(.system(size: 28, design: .serif))
                    .foregroundStyle(.white)
                if celebration {
                    Text(snap?.display?.closeLine ?? "Life happens in moments.")
                        .font(.system(size: 16))
                        .foregroundStyle(Color(hex: "#C4BDEE"))
                }
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
    private func photoMosaic(_ photos: [(url: URL, title: String?)]) -> some View {
        VStack(spacing: 8) {
            if let first = photos.first {
                storyPhoto(first.url, title: first.title, height: 180, radius: 16)
            }
            let rest = Array(photos.dropFirst())
            let rows = stride(from: 0, to: rest.count, by: 2).map { start in
                Array(rest[start..<min(start + 2, rest.count)])
            }
            ForEach(Array(rows.enumerated()), id: \.offset) { _, row in
                HStack(spacing: 8) {
                    ForEach(Array(row.enumerated()), id: \.offset) { _, photo in
                        storyPhoto(photo.url, title: photo.title, height: 110, radius: 14)
                    }
                    if row.count == 1 {
                        Color.clear.frame(maxWidth: .infinity).frame(height: 110)
                    }
                }
            }
        }
    }

    private func storyPhoto(_ url: URL, title: String?, height: CGFloat, radius: CGFloat) -> some View {
        ZStack(alignment: .bottomLeading) {
            AsyncImage(url: url) { phase in
                switch phase {
                case .success(let img):
                    img.resizable().scaledToFill()
                default:
                    Color(hex: "#E7E0D6")
                }
            }
            if let title {
                Text(title)
                    .font(.system(size: 11, weight: .semibold))
                    .foregroundStyle(.white)
                    .lineLimit(2)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 6)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(Color.black.opacity(0.55))
            }
        }
        .frame(maxWidth: .infinity)
        .frame(height: height)
        .clipped()
        .clipShape(RoundedRectangle(cornerRadius: radius))
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
                    let value = Self.formatMetricDisplay(raw, key: key, countKeys: countKeys, moneyKeys: moneyKeys, currencyCode: storyCurrency(snap))
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
        onDark: Bool,
        currencyCode: String
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
            return (key, label, Self.formatMetricDisplay(raw, key: key, countKeys: countKeys, moneyKeys: moneyKeys, currencyCode: currencyCode))
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
        phase = "loading"
        message = nil
        do {
            let status = try await APIClient.shared.getMomentStoryStatus(momentId: momentId)
            if status.status == "GENERATING" {
                phase = "generating"
                return
            }
            if status.status == "FAILED" {
                phase = "failed"
                message = status.errorMessage ?? "Story generation failed."
                return
            }
            if status.status == "NOT_STARTED" {
                phase = "not_started"
                message = "No Story yet — complete the moment to unlock it."
                return
            }
            story = try await APIClient.shared.getMomentStory(momentId: momentId)
            phase = "ready"
        } catch {
            phase = "error"
            message = (error as? LocalizedError)?.errorDescription ?? error.localizedDescription
        }
    }

    private func nonEmptyTrimmed(_ value: String?) -> String? {
        guard let trimmed = value?.trimmingCharacters(in: .whitespacesAndNewlines), !trimmed.isEmpty else {
            return nil
        }
        return trimmed
    }

    private func share() async {
        guard !sharing else { return }
        sharing = true
        defer { sharing = false }
        do {
            let pack = try await APIClient.shared.getMomentStorySharePack(momentId: momentId)
            guard let link = nonEmptyTrimmed(pack.webUrl), link.hasPrefix("https://") else {
                shareNotice = "Story link is not ready."
                return
            }
            let headline = nonEmptyTrimmed(pack.blurb)
                ?? story?.snapshot?.identity?.title
                ?? "Moment Story"
            InviteOutboundShare.presentSystemShare(items: ["\(headline)\n\(link)"])
        } catch {
            shareNotice = (error as? LocalizedError)?.errorDescription ?? "Could not share story."
        }
    }

    private func sharePdf() async {
        guard !sharing else { return }
        sharing = true
        defer { sharing = false }
        do {
            let data = try await APIClient.shared.getMomentStoryPdf(momentId: momentId)
            let url = FileManager.default.temporaryDirectory.appendingPathComponent("moment-story.pdf")
            try data.write(to: url, options: .atomic)
            InviteOutboundShare.presentSystemShare(items: [url])
        } catch {
            shareNotice = (error as? LocalizedError)?.errorDescription ?? "Could not share PDF."
        }
    }

    private struct StoryExpenseDay {
        let label: String
        let total: Double
        let items: [APIClient.MomentStoryMoneyExpense]
    }

    private func categoryDonut(
        _ categories: [APIClient.MomentStoryMoneyCategory],
        spent: Double,
        currency: String
    ) -> some View {
        let amounts = categories.map { $0.amount ?? 0 }
        let sliceTotal = amounts.reduce(0, +) > 0 ? amounts.reduce(0, +) : spent
        let percents = Self.categoryPercents(amounts, spent: sliceTotal)
        let colors = Self.storySliceColors
        return HStack(alignment: .center, spacing: 12) {
            Canvas { context, size in
                let center = CGPoint(x: size.width / 2, y: size.height / 2)
                let radius = min(size.width, size.height) / 2
                var start = -Double.pi / 2
                for (index, cat) in categories.enumerated() {
                    let sweep = ((cat.amount ?? 0) / sliceTotal) * 2 * Double.pi
                    guard sweep > 0 else { continue }
                    var path = Path()
                    path.move(to: center)
                    path.addArc(
                        center: center,
                        radius: radius,
                        startAngle: .radians(start),
                        endAngle: .radians(start + sweep),
                        clockwise: false
                    )
                    path.closeSubpath()
                    context.fill(path, with: .color(colors[index % colors.count]))
                    start += sweep
                }
                let hole = radius * 0.58
                let holeRect = CGRect(x: center.x - hole, y: center.y - hole, width: hole * 2, height: hole * 2)
                context.fill(Path(ellipseIn: holeRect), with: .color(Color(hex: "#F7F4EE")))
            }
            .frame(width: 132, height: 132)
            VStack(alignment: .leading, spacing: 6) {
                ForEach(Array(categories.enumerated()), id: \.offset) { index, cat in
                    HStack(spacing: 8) {
                        Circle()
                            .fill(colors[index % colors.count])
                            .frame(width: 8, height: 8)
                        VStack(alignment: .leading, spacing: 1) {
                            Text(cat.name ?? "Other")
                                .font(.system(size: 13))
                                .foregroundStyle(Color(hex: "#25231F"))
                            Text(Self.formatStoryMoney(cat.amount ?? 0, currencyCode: currency))
                                .font(.system(size: 11))
                                .foregroundStyle(Color(hex: "#746F67"))
                        }
                        Spacer(minLength: 4)
                        Text("\(percents.indices.contains(index) ? percents[index] : 0)%")
                            .font(.system(size: 13, weight: .bold))
                            .foregroundStyle(Color(hex: "#25231F"))
                    }
                }
            }
        }
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
        moneyKeys: Set<String>,
        currencyCode: String
    ) -> String {
        if case .string(let s) = raw, moneyKeys.contains(key) { return s }
        if countKeys.contains(key), let n = raw.numericValue { return String(Int(n.rounded())) }
        if moneyKeys.contains(key), let n = raw.numericValue { return formatStoryMoney(n, currencyCode: currencyCode) }
        if case .string(let s) = raw { return s }
        return raw.label
    }

    private static func formatStoryMoney(_ value: Double, currencyCode: String) -> String {
        let code = currencyCode.count == 3 ? currencyCode : "INR"
        let formatter = NumberFormatter()
        formatter.numberStyle = .currency
        formatter.currencyCode = code
        formatter.maximumFractionDigits = 0
        return formatter.string(from: NSNumber(value: value)) ?? "\(code) \(Int(value.rounded()))"
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

    private static let storySliceColors: [Color] = [
        Color(hex: "#4B3EA8"),
        Color(hex: "#E8621A"),
        Color(hex: "#0F7A6A"),
        Color(hex: "#C4893A"),
        Color(hex: "#6C4EF2"),
        Color(hex: "#B45F3D"),
        Color(hex: "#3D6B9A"),
        Color(hex: "#8A6A4A"),
    ]

    private static func groupStoryExpenses(_ expenses: [APIClient.MomentStoryMoneyExpense]) -> [StoryExpenseDay] {
        var buckets: [String: [APIClient.MomentStoryMoneyExpense]] = [:]
        var undated: [APIClient.MomentStoryMoneyExpense] = []
        let dayKey = DateFormatter()
        dayKey.locale = Locale(identifier: "en_US_POSIX")
        dayKey.dateFormat = "yyyy-MM-dd"
        let heading = DateFormatter()
        heading.locale = Locale(identifier: "en_US_POSIX")
        heading.dateFormat = "EEEE, d MMM"
        for expense in expenses {
            guard let iso = expense.at, let date = parseISO(iso) else {
                undated.append(expense)
                continue
            }
            let key = dayKey.string(from: date)
            buckets[key, default: []].append(expense)
        }
        var days = buckets.keys.sorted().map { key -> StoryExpenseDay in
            let items = buckets[key] ?? []
            let label = dayKey.date(from: key).map { heading.string(from: $0) } ?? key
            let total = items.reduce(0) { $0 + ($1.amount ?? 0) }
            return StoryExpenseDay(label: label, total: total, items: items)
        }
        if !undated.isEmpty {
            days.append(StoryExpenseDay(label: "Undated", total: undated.reduce(0) { $0 + ($1.amount ?? 0) }, items: undated))
        }
        return days
    }

    private static func categoryPercents(_ amounts: [Double], spent: Double) -> [Int] {
        guard spent > 0, !amounts.isEmpty else { return amounts.map { _ in 0 } }
        var raw = amounts.map { Int(($0 / spent * 100).rounded()) }
        let drift = 100 - raw.reduce(0, +)
        if drift != 0, let largest = amounts.indices.max(by: { amounts[$0] < amounts[$1] }) {
            raw[largest] = max(0, raw[largest] + drift)
        }
        return raw
    }

    private static func parseISO(_ iso: String) -> Date? {
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        if let d = formatter.date(from: iso) { return d }
        formatter.formatOptions = [.withInternetDateTime]
        return formatter.date(from: iso)
    }
}
