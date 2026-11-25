import SwiftUI
import UIKit

/// Trading / Distribution Story v3 — Figma TRD-STORY-* IG chrome (config → compose → P1–P8 → saved → share → states).
enum TradingStoryPhase: Equatable {
    case config
    case generating
    case viewer
    case saved
    case share
    case failed
    case unauthorized
    case conflict
    case loading
}

enum TradingStoryColors {
    static let night = Color(red: 0.035, green: 0.051, blue: 0.086) // #090D16
    static let ink = Color(red: 0.973, green: 0.976, blue: 0.992)
    static let muted = Color.white.opacity(0.70)
    static let glass = Color.white.opacity(0.08)
    static let glassBorder = Color.white.opacity(0.14)
    static let ember = Color(red: 0.910, green: 0.384, blue: 0.102) // Trading accent
}

struct TradingStoryProgressBar: View {
    let current: Int
    let total: Int
    var accent: Color = TradingStoryColors.ember

    var body: some View {
        HStack(spacing: 4) {
            ForEach(0..<total, id: \.self) { index in
                Capsule()
                    .fill(index < current ? accent : Color.white.opacity(0.22))
                    .frame(height: index == current - 1 ? 4 : 3)
            }
        }
    }
}

struct TradingStoryChromeHeader: View {
    let businessName: String
    let subtitle: String
    var onClose: () -> Void
    var accent: Color = TradingStoryColors.ember

    var body: some View {
        HStack(spacing: 10) {
            Circle()
                .fill(accent.opacity(0.85))
                .frame(width: 34, height: 34)
                .overlay(
                    Text(String(businessName.prefix(1)).uppercased())
                        .font(.system(size: 14, weight: .bold))
                        .foregroundStyle(.white)
                )
            VStack(alignment: .leading, spacing: 2) {
                Text(businessName)
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundStyle(TradingStoryColors.ink)
                Text(subtitle)
                    .font(.system(size: 12))
                    .foregroundStyle(TradingStoryColors.muted)
            }
            Spacer()
            Button(action: onClose) {
                Image(systemName: "xmark")
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundStyle(TradingStoryColors.ink)
                    .frame(width: 36, height: 36)
                    .background(TradingStoryColors.glass)
                    .clipShape(Circle())
            }
            .buttonStyle(.plain)
        }
    }
}

/// Full Trading Story flow hosted from Memory / Create Business Story.
struct TradingStoryExperience: View {
    let model: OperatingProjectionPayload
    let momentId: String
    let theme: OperatingSheetTheme
    let noun: String
    var onDismiss: () -> Void

    @State private var phase: TradingStoryPhase = .config
    @State private var detailed = false
    @State private var includeGst = false
    @State private var gstAvailable = false
    @State private var period = "THIS_MONTH"
    @State private var generating = false
    @State private var generateProgress = 0
    @State private var error: String?
    @State private var errorCode: String?
    @State private var preview: TradingStoryPreviewPayload?
    @State private var pageIndex = 0
    @State private var savedStoryId: String?
    @State private var savedVersion: Int?
    @State private var savedAt: String?
    @State private var showShare = false
    @State private var cancelGenerate = false
    @State private var shareText = ""

    private var accent: Color { theme.accent }
    private var name: String {
        let t = model.title?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        return t.isEmpty ? noun : t
    }
    private var hasActivity: Bool { !(model.activity ?? []).isEmpty }
    private var periodLabel: String { tradingStoryPeriodDisplay(period, start: model.startDate) }

    var body: some View {
        ZStack {
            TradingStoryColors.night.ignoresSafeArea()
            washes
            switch phase {
            case .config:
                configScreen
            case .generating:
                generatingScreen
            case .viewer:
                viewerScreen
            case .saved:
                savedScreen
            case .share:
                shareOverlay
            case .failed:
                stateScreen(
                    title: "Story couldn’t finish",
                    body: error ?? "Something broke while building the story. Nothing was saved.",
                    code: errorCode ?? "GENERATION_FAILED",
                    primary: "RETRY",
                    secondary: "Cancel",
                    onPrimary: { Task { await generateStory() } },
                    onSecondary: { phase = .config }
                )
            case .unauthorized:
                stateScreen(
                    title: "This story isn’t available to you",
                    body: "Ask the moment owner for access.",
                    code: "ACCESS_DENIED",
                    primary: "Go back",
                    secondary: nil,
                    onPrimary: onDismiss,
                    onSecondary: nil
                )
            case .conflict:
                stateScreen(
                    title: "Numbers need a check before we share",
                    body: "Two sources disagree for this period, so totals are held back until they reconcile.",
                    code: "RECONCILIATION_REQUIRED",
                    primary: "Retry",
                    secondary: "Cancel",
                    onPrimary: { Task { await generateStory() } },
                    onSecondary: { phase = .config }
                )
            case .loading:
                stateScreen(
                    title: "Loading Story…",
                    body: "",
                    code: nil,
                    primary: nil,
                    secondary: nil,
                    onPrimary: nil,
                    onSecondary: nil
                )
            }
        }
        .preferredColorScheme(.dark)
        .task { await loadOptions() }
        .sheet(isPresented: $showShare) {
            TradingStoryShareSheet(
                businessName: name,
                periodLabel: periodLabel,
                shareText: shareText,
                accent: accent,
                onDone: { showShare = false }
            )
            .presentationDetents([.medium, .large])
        }
    }

    private var washes: some View {
        ZStack {
            Circle()
                .fill(accent.opacity(0.18))
                .frame(width: 360, height: 360)
                .blur(radius: 60)
                .offset(x: -80, y: -120)
            Circle()
                .fill(Color.cyan.opacity(0.08))
                .frame(width: 300, height: 300)
                .blur(radius: 50)
                .offset(x: 120, y: 220)
        }
        .allowsHitTesting(false)
    }

    // MARK: - Config (5:19757)

    private var configScreen: some View {
        ScrollView {
            VStack(spacing: 16) {
                HStack {
                    Spacer()
                    Button(action: onDismiss) {
                        Image(systemName: "xmark")
                            .foregroundStyle(TradingStoryColors.ink)
                            .frame(width: 36, height: 36)
                            .background(TradingStoryColors.glass)
                            .clipShape(Circle())
                    }
                    .buttonStyle(.plain)
                }
                .padding(.horizontal, 16)

                storyThumb(height: 300)

                Text("Your \(periodLabel) story is ready to compose")
                    .font(.system(size: 15))
                    .foregroundStyle(TradingStoryColors.muted)
                    .multilineTextAlignment(.center)

                VStack(alignment: .leading, spacing: 16) {
                    Capsule()
                        .fill(Color.white.opacity(0.2))
                        .frame(width: 40, height: 4)
                        .frame(maxWidth: .infinity)

                    HStack(spacing: 12) {
                        Circle().fill(accent).frame(width: 36, height: 36)
                        VStack(alignment: .leading, spacing: 2) {
                            Text("Create Business Story")
                                .font(.system(size: 20, weight: .bold))
                                .foregroundStyle(TradingStoryColors.ink)
                            Text("\(name) · 8 slides")
                                .font(.system(size: 12))
                                .foregroundStyle(TradingStoryColors.muted)
                        }
                    }

                    LazyVGrid(columns: [GridItem(.adaptive(minimum: 90), spacing: 8)], spacing: 8) {
                        periodChip("This week", "THIS_WEEK")
                        periodChip("This month", "THIS_MONTH")
                        periodChip("Last month", "LAST_MONTH")
                        periodChip("Quarter", "QUARTER")
                        periodChip("Year", "YEAR")
                        periodChip("Custom", "CUSTOM")
                    }

                    HStack(spacing: 4) {
                        segment("Summary", selected: !detailed) { detailed = false }
                        segment("Detailed", selected: detailed) { detailed = true }
                    }
                    .padding(4)
                    .background(TradingStoryColors.glass)
                    .clipShape(RoundedRectangle(cornerRadius: 12))

                    if gstAvailable {
                        HStack {
                            VStack(alignment: .leading, spacing: 2) {
                                Text("Include GST snapshot")
                                    .font(.system(size: 14, weight: .semibold))
                                    .foregroundStyle(TradingStoryColors.ink)
                                Text("Shown because GST is enabled · recorded only")
                                    .font(.system(size: 11))
                                    .foregroundStyle(TradingStoryColors.muted)
                            }
                            Spacer()
                            Toggle("", isOn: $includeGst)
                                .labelsHidden()
                                .tint(accent)
                        }
                    }

                    if !hasActivity {
                        noActivityCard
                    }

                    Button {
                        Task { await generateStory() }
                    } label: {
                        HStack(spacing: 8) {
                            Image(systemName: "sparkles")
                            Text(hasActivity ? "Generate story" : "Generate · needs activity")
                                .font(.system(size: 16, weight: .bold))
                        }
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 16)
                        .foregroundStyle(hasActivity ? TradingStoryColors.night : TradingStoryColors.muted)
                        .background(hasActivity ? accent : TradingStoryColors.glass)
                        .clipShape(RoundedRectangle(cornerRadius: 14))
                    }
                    .buttonStyle(.plain)
                    .disabled(!hasActivity || generating)

                    if let error {
                        Text(error)
                            .font(.system(size: 11))
                            .foregroundStyle(Color.red.opacity(0.9))
                    }

                    Text("Every page is built from recorded activity (M01–M18). Nothing is estimated.")
                        .font(.system(size: 10))
                        .foregroundStyle(TradingStoryColors.muted)
                }
                .padding(20)
                .background(Color.white.opacity(0.06))
                .clipShape(RoundedRectangle(cornerRadius: 22, style: .continuous))
                .padding(.horizontal, 0)
            }
            .padding(.bottom, 24)
        }
    }

    private var noActivityCard: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("⚠ No verified activity in this period")
                .font(.system(size: 15, weight: .bold))
                .foregroundStyle(TradingStoryColors.ink)
            Text("We found no reconciled trades, invoices or payouts for \(periodLabel). We won’t show zeros for data we don’t have.")
                .font(.system(size: 12))
                .foregroundStyle(TradingStoryColors.muted)
            ForEach(["Trades", "Revenue", "Payouts"], id: \.self) { row in
                HStack {
                    Text(row).foregroundStyle(TradingStoryColors.muted)
                    Spacer()
                    Text("Not recorded").foregroundStyle(Color.orange.opacity(0.9))
                }
                .font(.system(size: 12))
            }
        }
        .padding(16)
        .background(TradingStoryColors.glass)
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    // MARK: - Generating (5:19892)

    private var generatingScreen: some View {
        VStack(spacing: 18) {
            Spacer()
            ZStack {
                ForEach(0..<3, id: \.self) { ring in
                    Circle()
                        .stroke(accent.opacity(0.25 - Double(ring) * 0.06), lineWidth: 2)
                        .frame(width: CGFloat(160 + ring * 40), height: CGFloat(160 + ring * 40))
                }
                Circle()
                    .fill(accent.opacity(0.9))
                    .frame(width: 136, height: 136)
                    .overlay(
                        Text(String(name.prefix(1)).uppercased())
                            .font(.system(size: 48, weight: .bold))
                            .foregroundStyle(.white)
                    )
            }
            Text("Composing your story")
                .font(.system(size: 22, weight: .bold))
                .foregroundStyle(TradingStoryColors.ink)
            Text("Replaying stock & orders…")
                .font(.system(size: 14))
                .foregroundStyle(TradingStoryColors.muted)
            TradingStoryProgressBar(current: max(generateProgress, 1), total: 8, accent: accent)
                .frame(width: 260)
            Text("\(max(generateProgress, 1)) of 8 slides")
                .font(.system(size: 12))
                .foregroundStyle(TradingStoryColors.muted)
            Spacer()
            Text("Only recorded activity is used — nothing estimated.")
                .font(.system(size: 12))
                .foregroundStyle(TradingStoryColors.muted)
            Button("Cancel") {
                cancelGenerate = true
                generating = false
                phase = .config
            }
            .foregroundStyle(TradingStoryColors.muted)
            .padding(.bottom, 40)
        }
        .padding(.horizontal, 24)
    }

    // MARK: - Viewer P1–P8

    private var viewerScreen: some View {
        let pages = preview?.pages ?? []
        let page = pages.indices.contains(pageIndex) ? pages[pageIndex] : nil
        let total = max(pages.count, 1)

        return ZStack {
            VStack(spacing: 0) {
                TradingStoryProgressBar(current: pageIndex + 1, total: total, accent: accent)
                    .padding(.horizontal, 10)
                    .padding(.top, 8)
                TradingStoryChromeHeader(
                    businessName: preview?.businessName ?? name,
                    subtitle: "\(periodLabel) · \(page?.title ?? "Story") · \(pageIndex + 1)/\(total)",
                    onClose: onDismiss,
                    accent: accent
                )
                .padding(.horizontal, 12)
                .padding(.top, 8)

                ScrollView {
                    TradingStoryPageBody(page: page, accent: accent, currency: preview?.currency ?? model.currency)
                        .padding(.horizontal, 24)
                        .padding(.top, 24)
                        .padding(.bottom, 80)
                }

                HStack {
                    Text((page?.title ?? "STORY").uppercased())
                        .font(.system(size: 11, weight: .semibold))
                        .foregroundStyle(TradingStoryColors.muted)
                    Spacer()
                    Text("Recorded activity only")
                        .font(.system(size: 11))
                        .foregroundStyle(TradingStoryColors.muted.opacity(0.8))
                }
                .padding(.horizontal, 24)
                .padding(.bottom, 12)

                HStack(spacing: 10) {
                    Button {
                        Task { await saveSnapshot(thenShare: false) }
                    } label: {
                        Text(savedStoryId == nil ? "Save story" : "Saved")
                            .font(.system(size: 15, weight: .bold))
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 14)
                            .foregroundStyle(TradingStoryColors.night)
                            .background(accent)
                            .clipShape(RoundedRectangle(cornerRadius: 14))
                    }
                    .buttonStyle(.plain)
                    .disabled(savedStoryId != nil)

                    Button {
                        Task { await saveSnapshot(thenShare: true) }
                    } label: {
                        Image(systemName: "square.and.arrow.up")
                            .font(.system(size: 16, weight: .semibold))
                            .foregroundStyle(TradingStoryColors.ink)
                            .frame(width: 48, height: 48)
                            .background(TradingStoryColors.glass)
                            .clipShape(RoundedRectangle(cornerRadius: 14))
                    }
                    .buttonStyle(.plain)
                }
                .padding(.horizontal, 16)
                .padding(.bottom, 20)
            }

            HStack(spacing: 0) {
                Color.clear
                    .contentShape(Rectangle())
                    .onTapGesture {
                        if pageIndex > 0 { pageIndex -= 1 }
                    }
                    .frame(maxWidth: .infinity)
                    .frame(width: UIScreen.main.bounds.width * 0.3)
                Color.clear
                    .contentShape(Rectangle())
                    .onTapGesture {
                        if pageIndex < pages.count - 1 { pageIndex += 1 }
                    }
                    .frame(maxWidth: .infinity)
            }
            .padding(.top, 120)
            .padding(.bottom, 120)
        }
    }

    // MARK: - Saved (5:20012)

    private var savedScreen: some View {
        VStack(spacing: 16) {
            Spacer()
            storyThumb(height: 270)
            Image(systemName: "checkmark.circle.fill")
                .font(.system(size: 44))
                .foregroundStyle(accent)
                .offset(y: -20)
            Text("Story saved to Memory")
                .font(.system(size: 22, weight: .bold))
                .foregroundStyle(TradingStoryColors.ink)
            Text("Version \(savedVersion ?? 1) · \(savedAt ?? "just now") · immutable snapshot")
                .font(.system(size: 13))
                .foregroundStyle(TradingStoryColors.muted)
                .multilineTextAlignment(.center)
            Text("New or backdated entries create Version 2 with a changed-facts badge.")
                .font(.system(size: 12))
                .foregroundStyle(TradingStoryColors.muted)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 40)
            Spacer()
            Button {
                prepareShare()
                showShare = true
            } label: {
                HStack {
                    Image(systemName: "square.and.arrow.up")
                    Text("Share story").font(.system(size: 16, weight: .bold))
                }
                .frame(maxWidth: .infinity)
                .padding(.vertical, 16)
                .foregroundStyle(TradingStoryColors.night)
                .background(accent)
                .clipShape(RoundedRectangle(cornerRadius: 14))
            }
            .buttonStyle(.plain)
            Button("Back to Memory", action: onDismiss)
                .font(.system(size: 15, weight: .semibold))
                .foregroundStyle(TradingStoryColors.ink)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 14)
                .background(TradingStoryColors.glass)
                .clipShape(RoundedRectangle(cornerRadius: 14))
                .buttonStyle(.plain)
        }
        .padding(24)
    }

    private var shareOverlay: some View {
        savedScreen
    }

    // MARK: - Shared bits

    private func storyThumb(height: CGFloat) -> some View {
        let hero = tradingStoryHeroAmount(
            currency: model.currency,
            sales: model.finance?.sales,
            visible: model.financeVisible == true
        )
        return VStack(alignment: .leading, spacing: 10) {
            TradingStoryProgressBar(current: 1, total: 8, accent: accent)
            Text("THE BUSINESS MOMENT")
                .font(.system(size: 8, weight: .semibold))
                .foregroundStyle(accent)
            Text(hero)
                .font(.system(size: 36, weight: .bold))
                .foregroundStyle(TradingStoryColors.ink)
                .minimumScaleFactor(0.5)
                .lineLimit(1)
            Text("net sales · \(periodLabel)")
                .font(.system(size: 10))
                .foregroundStyle(TradingStoryColors.muted)
            Spacer()
            Capsule().fill(Color.white.opacity(0.5)).frame(width: 56, height: 6)
        }
        .padding(12)
        .frame(width: 148, height: height)
        .background(
            LinearGradient(colors: [Color.white.opacity(0.08), TradingStoryColors.night], startPoint: .top, endPoint: .bottom)
        )
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(accent.opacity(0.35), lineWidth: 1))
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .frame(maxWidth: .infinity)
    }

    private func periodChip(_ label: String, _ code: String) -> some View {
        Button {
            period = code
        } label: {
            Text(label)
                .font(.system(size: 13, weight: period == code ? .bold : .medium))
                .foregroundStyle(period == code ? TradingStoryColors.night : TradingStoryColors.ink)
                .padding(.horizontal, 12)
                .padding(.vertical, 9)
                .frame(maxWidth: .infinity)
                .background(period == code ? accent : TradingStoryColors.glass)
                .clipShape(RoundedRectangle(cornerRadius: 10))
        }
        .buttonStyle(.plain)
    }

    private func segment(_ label: String, selected: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(label)
                .font(.system(size: 14, weight: selected ? .bold : .medium))
                .foregroundStyle(selected ? TradingStoryColors.night : TradingStoryColors.muted)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 10)
                .background(selected ? accent : Color.clear)
                .clipShape(RoundedRectangle(cornerRadius: 10))
        }
        .buttonStyle(.plain)
    }

    private func stateScreen(
        title: String,
        body: String,
        code: String?,
        primary: String?,
        secondary: String?,
        onPrimary: (() -> Void)?,
        onSecondary: (() -> Void)?
    ) -> some View {
        VStack(spacing: 16) {
            TradingStoryChromeHeader(businessName: name, subtitle: "Story", onClose: onDismiss, accent: accent)
                .padding(.horizontal, 16)
                .padding(.top, 12)
            Spacer()
            Circle()
                .stroke(accent.opacity(0.5), lineWidth: 3)
                .frame(width: 120, height: 120)
                .overlay(Circle().fill(accent.opacity(0.85)).frame(width: 90, height: 90))
            Text(title)
                .font(.system(size: 22, weight: .bold))
                .foregroundStyle(TradingStoryColors.ink)
                .multilineTextAlignment(.center)
            if !body.isEmpty {
                Text(body)
                    .font(.system(size: 14))
                    .foregroundStyle(TradingStoryColors.muted)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 28)
            }
            if let code {
                Text(code)
                    .font(.system(size: 11, weight: .semibold, design: .monospaced))
                    .foregroundStyle(TradingStoryColors.muted)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 6)
                    .background(TradingStoryColors.glass)
                    .clipShape(Capsule())
            }
            Spacer()
            if let primary, let onPrimary {
                Button(primary, action: onPrimary)
                    .font(.system(size: 15, weight: .bold))
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 14)
                    .foregroundStyle(TradingStoryColors.night)
                    .background(accent)
                    .clipShape(RoundedRectangle(cornerRadius: 14))
                    .buttonStyle(.plain)
            }
            if let secondary, let onSecondary {
                Button(secondary, action: onSecondary)
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundStyle(TradingStoryColors.ink)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 12)
                    .buttonStyle(.plain)
            }
        }
        .padding(24)
    }

    // MARK: - Networking

    @MainActor
    private func loadOptions() async {
        do {
            let options = try await APIClient.shared.getTradingStoryOptions(momentId: momentId)
            gstAvailable = options.gstAvailable == true
            if options.permissions?.canGenerate == false {
                phase = .unauthorized
            }
        } catch {
            // Options are best-effort; config still works with defaults.
        }
    }

    @MainActor
    private func generateStory() async {
        cancelGenerate = false
        generating = true
        error = nil
        errorCode = nil
        phase = .generating
        generateProgress = 1
        let range = tradingStoryPeriodRange(preset: period, start: model.startDate, end: model.endDate)
        do {
            let start = try await APIClient.shared.startTradingStoryPreview(
                momentId: momentId,
                startDate: range.0,
                endDate: range.1,
                detail: detailed ? "DETAILED" : "SUMMARY",
                includeGst: includeGst && gstAvailable
            )
            if let jobId = start.jobId, !jobId.isEmpty {
                for step in 1...8 {
                    if cancelGenerate { generating = false; return }
                    generateProgress = step
                    try? await Task.sleep(nanoseconds: 120_000_000)
                    if let job = try? await APIClient.shared.getTradingStoryJob(jobId: jobId) {
                        if (job.status ?? "").uppercased() == "FAILED" {
                            throw NSError(domain: "Story", code: 2, userInfo: [NSLocalizedDescriptionKey: "Generation failed"])
                        }
                        if (job.status ?? "").uppercased() == "CONFLICT" {
                            phase = .conflict
                            generating = false
                            return
                        }
                        if let p = job.progress { generateProgress = min(8, max(1, p / 12 + 1)) }
                    }
                }
            } else {
                try? await Task.sleep(nanoseconds: 400_000_000)
                generateProgress = 8
            }
            guard let previewId = start.previewId, !previewId.isEmpty else {
                throw NSError(domain: "Story", code: 1, userInfo: [NSLocalizedDescriptionKey: "No preview id"])
            }
            if cancelGenerate { generating = false; return }
            let loaded = try await APIClient.shared.getTradingStoryPreview(previewId: previewId)
            if (loaded.status ?? "").uppercased() == "CONFLICT" {
                phase = .conflict
                generating = false
                return
            }
            preview = loaded
            pageIndex = 0
            phase = .viewer
        } catch {
            let msg = error.localizedDescription
            self.error = msg
            if msg.lowercased().contains("401") || msg.lowercased().contains("403") || msg.lowercased().contains("unauthor") {
                phase = .unauthorized
            } else {
                errorCode = "GENERATION_FAILED"
                phase = .failed
            }
        }
        generating = false
    }

    @MainActor
    private func saveSnapshot(thenShare: Bool) async {
        guard let previewId = preview?.previewId else { return }
        do {
            let result = try await APIClient.shared.saveTradingStoryPreview(previewId: previewId)
            savedStoryId = result.storyId
            savedVersion = result.version
            savedAt = result.savedAt ?? result.snapshotWatermark
            phase = .saved
            if thenShare {
                prepareShare()
                showShare = true
            }
        } catch {
            self.error = error.localizedDescription
            errorCode = "SAVE_FAILED"
            phase = .failed
        }
    }

    private func prepareShare() {
        let hero = preview?.pages?.first?.hero
        let amount: String = {
            if let v = hero?.value { return String(format: "₹%.0f", v) }
            return "Business Story"
        }()
        shareText = "\(name) · \(periodLabel) · 8 slides\n\(amount) — \(hero?.label ?? "Story")\nRecorded activity only."
    }
}

// MARK: - Page body layouts

struct TradingStoryPageBody: View {
    let page: TradingStoryPagePayload?
    let accent: Color
    let currency: String?

    var body: some View {
        let pageId = (page?.pageId ?? "").uppercased()
        VStack(alignment: .leading, spacing: 14) {
            Text((page?.title ?? "Story").uppercased())
                .font(.system(size: 12, weight: .semibold))
                .foregroundStyle(accent)
                .tracking(1.2)

            if pageId == "P2" {
                p2Body
            } else if pageId == "P5" {
                p5Body
            } else {
                defaultBody
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private var defaultBody: some View {
        VStack(alignment: .leading, spacing: 12) {
            if let title = page?.title {
                Text(title)
                    .font(.system(size: 28, weight: .bold))
                    .foregroundStyle(TradingStoryColors.ink)
            }
            heroBlock
            ForEach(page?.narrative ?? [], id: \.self) { line in
                Text(line)
                    .font(.system(size: 15))
                    .foregroundStyle(TradingStoryColors.muted)
            }
            chipRow(cards: page?.cards ?? [])
            if (page?.state ?? "") == "empty" || (page?.state ?? "") == "partial" {
                Text(page?.state == "empty" ? "Nothing recorded yet" : "Partial · some facts missing")
                    .font(.system(size: 12))
                    .foregroundStyle(Color.orange.opacity(0.9))
            }
        }
    }

    private var p2Body: some View {
        VStack(alignment: .leading, spacing: 14) {
            Text(page?.title ?? "The Business in Motion")
                .font(.system(size: 28, weight: .bold))
                .foregroundStyle(TradingStoryColors.ink)
            ForEach(page?.narrative ?? [], id: \.self) { line in
                Text(line)
                    .font(.system(size: 15))
                    .foregroundStyle(TradingStoryColors.muted)
            }
            VStack(spacing: 8) {
                ForEach(page?.cards ?? []) { card in
                    HStack(spacing: 10) {
                        Text(card.label ?? "")
                            .font(.system(size: 12, weight: .semibold))
                            .foregroundStyle(TradingStoryColors.ink)
                            .padding(.horizontal, 10)
                            .padding(.vertical, 7)
                            .background(TradingStoryColors.glass)
                            .clipShape(RoundedRectangle(cornerRadius: 8))
                        Text(card.value ?? "")
                            .font(.system(size: 14))
                            .foregroundStyle(TradingStoryColors.ink)
                        Spacer()
                    }
                }
            }
            .padding(16)
            .background(TradingStoryColors.glass)
            .overlay(RoundedRectangle(cornerRadius: 16).stroke(TradingStoryColors.glassBorder))
            .clipShape(RoundedRectangle(cornerRadius: 16))
        }
    }

    private var p5Body: some View {
        let cards = page?.cards ?? []
        let mid = max(cards.count / 2, 1)
        let left = Array(cards.prefix(mid))
        let right = Array(cards.suffix(from: min(mid, cards.count)))
        return VStack(alignment: .leading, spacing: 14) {
            heroBlock
            ForEach(page?.narrative ?? [], id: \.self) { line in
                Text(line)
                    .font(.system(size: 15))
                    .foregroundStyle(TradingStoryColors.muted)
            }
            HStack(alignment: .top, spacing: 10) {
                glassColumn(title: "INBOUND", cards: left)
                glassColumn(title: "OUTBOUND", cards: right)
            }
        }
    }

    private var heroBlock: some View {
        Group {
            if let value = page?.hero?.value {
                Text(tradingStoryFormatHero(value, unit: page?.hero?.unit, currency: currency))
                    .font(.system(size: 52, weight: .bold))
                    .foregroundStyle(TradingStoryColors.ink)
                    .minimumScaleFactor(0.4)
                    .lineLimit(1)
            } else {
                Text("Not recorded")
                    .font(.system(size: 36, weight: .bold))
                    .foregroundStyle(TradingStoryColors.ink)
            }
            if let label = page?.hero?.label, !label.isEmpty {
                Text(label)
                    .font(.system(size: 15))
                    .foregroundStyle(TradingStoryColors.muted)
            }
        }
    }

    private func glassColumn(title: String, cards: [TradingStoryCardPayload]) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(title)
                .font(.system(size: 11, weight: .semibold))
                .foregroundStyle(TradingStoryColors.muted)
            ForEach(cards) { card in
                HStack {
                    Text(card.label ?? "")
                        .font(.system(size: 12))
                        .foregroundStyle(TradingStoryColors.muted)
                    Spacer()
                    Text(card.value ?? "—")
                        .font(.system(size: 12, weight: .semibold))
                        .foregroundStyle(TradingStoryColors.ink)
                }
            }
        }
        .padding(14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(TradingStoryColors.glass)
        .clipShape(RoundedRectangle(cornerRadius: 14))
    }

    private func chipRow(cards: [TradingStoryCardPayload]) -> some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(cards) { card in
                    Text("\(card.label ?? "") · \(card.value ?? "")")
                        .font(.system(size: 13, weight: .semibold))
                        .foregroundStyle(TradingStoryColors.ink)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 9)
                        .background(TradingStoryColors.glass)
                        .clipShape(RoundedRectangle(cornerRadius: 10))
                }
            }
        }
    }
}

// MARK: - Share sheet (5:20229)

struct TradingStoryShareSheet: View {
    let businessName: String
    let periodLabel: String
    let shareText: String
    let accent: Color
    var onDone: () -> Void

    var body: some View {
        NavigationStack {
            VStack(alignment: .leading, spacing: 20) {
                HStack(spacing: 12) {
                    Circle().fill(accent).frame(width: 32, height: 32)
                    VStack(alignment: .leading, spacing: 2) {
                        Text("Share your Business Moment")
                            .font(.system(size: 17, weight: .bold))
                        Text("\(businessName) · \(periodLabel) · 8 slides")
                            .font(.system(size: 12))
                            .foregroundStyle(.secondary)
                    }
                }
                LazyVGrid(columns: Array(repeating: GridItem(.flexible()), count: 4), spacing: 16) {
                    shareDest("Your story", system: "book.closed.fill")
                    shareDest("WhatsApp", system: "phone.fill")
                    shareDest("Instagram", system: "camera.fill")
                    shareDest("Copy link", system: "link")
                    shareDest("Export PDF", system: "doc.richtext")
                    shareDest("Save image", system: "arrow.down.to.line")
                    shareDest("Email", system: "envelope.fill")
                    shareDest("More", system: "ellipsis")
                }
                Text("Confidential fields excluded · Owner/Admin can attach register")
                    .font(.system(size: 11))
                    .foregroundStyle(.secondary)
                Button("Done", action: onDone)
                    .font(.system(size: 16, weight: .bold))
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 14)
                    .foregroundStyle(.white)
                    .background(accent)
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                    .buttonStyle(.plain)
                Spacer()
            }
            .padding(20)
            .navigationBarTitleDisplayMode(.inline)
        }
    }

    private func shareDest(_ label: String, system: String) -> some View {
        Button {
            UIPasteboard.general.string = shareText
            let av = UIActivityViewController(activityItems: [shareText], applicationActivities: nil)
            if let scene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
               let root = scene.windows.first?.rootViewController {
                root.present(av, animated: true)
            }
        } label: {
            VStack(spacing: 8) {
                Image(systemName: system)
                    .font(.system(size: 18))
                    .foregroundStyle(accent)
                    .frame(width: 56, height: 56)
                    .background(Color.secondary.opacity(0.12))
                    .clipShape(RoundedRectangle(cornerRadius: 16))
                Text(label)
                    .font(.system(size: 11))
                    .foregroundStyle(.primary)
                    .lineLimit(1)
            }
        }
        .buttonStyle(.plain)
    }
}

// MARK: - Helpers

func tradingStoryHeroAmount(currency: String?, sales: String?, visible: Bool) -> String {
    guard visible, let sales, !sales.isEmpty else { return "—" }
    if let value = Double(sales) {
        return tradingStoryCompactINR(value)
    }
    let trimmed = sales.trimmingCharacters(in: .whitespacesAndNewlines)
    if trimmed.hasPrefix("₹") { return trimmed }
    return "₹\(trimmed)"
}

func tradingStoryFormatHero(_ value: Double, unit: String?, currency: String?) -> String {
    if (unit ?? "").uppercased().contains("COUNT") {
        return String(format: "%.0f", value)
    }
    return tradingStoryCompactINR(value)
}

func tradingStoryCompactINR(_ value: Double) -> String {
    let abs = abs(value)
    if abs >= 100_000 {
        return String(format: "₹%.1fL", value / 100_000).replacingOccurrences(of: ".0L", with: "L")
    }
    if abs >= 1_000 {
        return String(format: "₹%.1fK", value / 1_000).replacingOccurrences(of: ".0K", with: "K")
    }
    return String(format: "₹%.0f", value)
}

func tradingStoryPeriodDisplay(_ preset: String, start: String?) -> String {
    let formatter = DateFormatter()
    formatter.locale = Locale(identifier: "en_US")
    formatter.dateFormat = "MMMM yyyy"
    switch preset {
    case "THIS_WEEK": return "This week"
    case "LAST_MONTH":
        let cal = Calendar.current
        let last = cal.date(byAdding: .month, value: -1, to: Date()) ?? Date()
        return formatter.string(from: last)
    case "QUARTER": return "This quarter"
    case "YEAR": return "This year"
    case "CUSTOM": return "Custom period"
    default:
        let raw = start?.prefix(10) ?? ""
        let parse = DateFormatter()
        parse.locale = Locale(identifier: "en_US_POSIX")
        parse.dateFormat = "yyyy-MM-dd"
        if let d = parse.date(from: String(raw)) { return formatter.string(from: d) }
        return formatter.string(from: Date())
    }
}

func tradingStoryPeriodRange(preset: String, start: String?, end: String?) -> (String, String) {
    let formatter = DateFormatter()
    formatter.locale = Locale(identifier: "en_US_POSIX")
    formatter.dateFormat = "yyyy-MM-dd"
    let today = Date()
    let cal = Calendar(identifier: .gregorian)
    func dayString(_ date: Date) -> String { formatter.string(from: date) }

    switch preset {
    case "THIS_WEEK":
        let from = cal.date(byAdding: .day, value: -6, to: today) ?? today
        return (dayString(from), dayString(today))
    case "LAST_MONTH":
        guard let firstOfThis = cal.date(from: cal.dateComponents([.year, .month], from: today)),
              let firstOfLast = cal.date(byAdding: .month, value: -1, to: firstOfThis),
              let lastOfLast = cal.date(byAdding: DateComponents(month: 1, day: -1), to: firstOfLast) else {
            return (dayString(today), dayString(today))
        }
        return (dayString(firstOfLast), dayString(lastOfLast))
    case "QUARTER":
        let month = cal.component(.month, from: today)
        let qStartMonth = ((month - 1) / 3) * 3 + 1
        let startQ = cal.date(from: DateComponents(year: cal.component(.year, from: today), month: qStartMonth, day: 1)) ?? today
        return (dayString(startQ), dayString(today))
    case "YEAR":
        let startY = cal.date(from: DateComponents(year: cal.component(.year, from: today), month: 1, day: 1)) ?? today
        return (dayString(startY), dayString(today))
    case "CUSTOM":
        let from = String((start ?? "").prefix(10))
        let to = String((end ?? "").prefix(10))
        if !from.isEmpty, !to.isEmpty { return (from, to) }
        fallthrough
    default:
        let firstOfMonth = cal.date(from: cal.dateComponents([.year, .month], from: today)) ?? today
        return (dayString(firstOfMonth), dayString(today))
    }
}
