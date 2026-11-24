import PhotosUI
import SwiftUI
import UIKit

struct PersonalExperiencePayload: Decodable {
    let currencyCode: String?
    let expenseCount: Int
    let cold: Bool
    let pulse: PulseBlock
    let moments: MomentsBlock
    let life: LifeBlock
    let memory: MemoryBlock

    struct PulseBlock: Decodable {
        let intro: Intro?
        let recent: [ActivityRow]
        let attention: Attention
        let snapshot: Snapshot
        let signals: [Signal]
        let signalsPrompt: String?
        let signalsMessage: String?
        let mood: MoodPulse
    }
    struct Intro: Decodable { let kicker: String; let title: String; let subtitle: String }
    struct ActivityRow: Decodable, Identifiable {
        let expenseId: String
        let merchantName: String
        let amount: String
        let currencyCode: String
        let effectiveAt: String
        let moodCode: String?
        let moodLabel: String?
        let areaCode: String
        let areaLabel: String
        let subcategoryCode: String
        let subcategoryLabel: String
        let iconKey: String
        var id: String { expenseId }
    }
    struct Attention: Decodable {
        let quiet: Bool
        let issueCount: Int
        let message: String
        let title: String?
        let body: String?
    }
    struct Snapshot: Decodable {
        let periodLabel: String
        let rangeLabel: String?
        let total: String
        let expenseCount: Int
        let previousTotal: String?
        let previousLabel: String?
        let previousRangeLabel: String?
        let previousExpenseCount: Int?
        let comparable: Bool
        let deltaAmount: String?
        let deltaRatio: Double?
        let comparisonNote: String?
        let topAreaLabel: String?
        let topAreaAmount: String?
        let currentShort: String?
        let previousShort: String?
    }
    struct Signal: Decodable, Identifiable {
        let areaLabel: String
        let subcategoryLabel: String?
        let current: String
        let previous: String
        let currentCount: Int?
        let previousCount: Int?
        let direction: String
        let sentence: String?
        var id: String { (subcategoryLabel ?? areaLabel) + current }
    }
    struct MoodCount: Decodable { let moodCode: String; let label: String; let count: Int }
    struct MoodPulse: Decodable {
        let responseCount: Int
        let expenseCount: Int
        let coverage: Double?
        let insufficient: Bool
        let message: String
        let counts: [MoodCount]
    }
    struct MomentCard: Decodable, Identifiable {
        let id: String
        let state: String
        let title: String
        let statusLabel: String?
        let summary: String?
        let rangeLabel: String?
        let areaLabel: String
        let subcategoryLabel: String
        let spend: String
        let activityCount: Int
        let artworkKey: String?
        let tentative: Bool
    }
    struct ProgressBar: Decodable { let effectiveAt: String; let amount: String; let label: String? }
    struct Progress: Decodable {
        let title: String
        let areaLabel: String?
        let subcategoryLabel: String?
        let bars: [ProgressBar]
        let cumulativeLabel: String?
        let footnote: String?
        let message: String
    }
    struct Insight: Decodable {
        let title: String?
        let text: String
        let sufficient: Bool
        let actionLabel: String?
        let footnote: String?
    }
    struct MomentsBlock: Decodable {
        let intro: Intro?
        let heroMonth: String?
        let heroEvidence: String?
        let activeSubtitle: String?
        let emergingSubtitle: String?
        let progressSubtitle: String?
        let recentSubtitle: String?
        let active: [MomentCard]
        let emerging: [MomentCard]
        let progress: Progress?
        let recent: [MomentCard]
        let insight: Insight
    }
    struct AreaSpend: Decodable, Identifiable {
        let areaCode: String
        let label: String
        let layer: String
        let iconKey: String?
        let spend: String
        let activityCount: Int
        let share: Double?
        var id: String { areaCode }
    }
    struct NowBuild: Decodable { let now: String; let build: String; let nowShare: Double?; let buildShare: Double? }
    struct MoneySlice: Decodable, Identifiable {
        let subcategoryCode: String
        let label: String
        let areaLabel: String?
        let spend: String
        let share: Double
        var id: String { subcategoryCode }
    }
    struct Trend: Decodable {
        let label: String
        let areaLabel: String?
        let direction: String
        let detail: String
        let previous: String?
        let current: String?
        let previousCount: Int?
        let currentCount: Int?
        let previousShort: String?
        let currentShort: String?
    }
    struct AreaMood: Decodable {
        let areaLabel: String
        let moodLabel: String?
        let responseCount: Int
        let expenseCount: Int?
        let noMoodRecorded: Bool
        let responses: [String]?
    }
    struct LifeBlock: Decodable {
        let intro: Intro?
        let total: String?
        let expenseCount: Int?
        let monthLabel: String?
        let rangeLabel: String?
        let coverageLabel: String?
        let highlightedAreaCode: String?
        let areas: [AreaSpend]
        let nowBuild: NowBuild
        let moneyGoes: [MoneySlice]
        let otherCount: Int
        let otherSpend: String?
        let otherShare: Double?
        let topFiveSpend: String?
        let topFiveShare: Double?
        let trends: [Trend]
        let trendsMessage: String?
        let trendsFootnote: String?
        let moodSummary: String?
        let moodCoverageLabel: String?
        let moodFootnote: String?
        let moodByArea: [AreaMood]
    }
    struct TimelineItem: Decodable, Identifiable {
        let expenseId: String
        let title: String
        let effectiveAt: String
        let detail: String
        let kicker: String?
        let period: String?
        var id: String { expenseId }
    }
    struct Monthly: Decodable {
        let label: String
        let shortLabel: String?
        let year: Int?
        let closedLabel: String?
        let title: String?
        let spend: String
        let expenseCount: Int
        let completedCount: Int?
        let moodCount: Int?
        let moodCoverage: Double?
        let periodCaption: String?
        let summary: String
        let openLabel: String?
    }
    struct Milestone: Decodable, Identifiable {
        let title: String
        let effectiveAt: String
        let detail: String
        let areaLabel: String?
        let subcategoryLabel: String?
        let spend: String?
        let activityCount: Int?
        let confirmedLabel: String?
        var id: String { title + effectiveAt }
    }
    struct LookBack: Decodable { let title: String; let detail: String; let effectiveAt: String; let kicker: String? }
    struct MemoryBlock: Decodable {
        let intro: Intro?
        let openMonthShort: String?
        let openMonthYear: Int?
        let openMonthChip: String?
        let openRecap: String?
        let openMonthNote: String?
        let timelineOpenKicker: String?
        let currentFilterLabel: String?
        let timeline: [TimelineItem]
        let completed: [MomentCard]
        let monthly: Monthly?
        let milestones: [Milestone]
        let lookBack: LookBack?
        let lookBackMessage: String?
    }
}

struct PersonalLifeExpenseRequest: Encodable {
    let amount: String
    let currencyCode: String
    let merchantName: String?
    let effectiveAt: String?
    let note: String?
    let lifeAreaCode: String
    let lifeSubcategoryCode: String
    let moodCode: String?
    let classificationSource: String
    let origin: String
    let paymentSource: String?
    let economicKind: String
    let photo: PersonalExpensePhotoRequest?
}

struct PersonalExpensePhotoRequest: Encodable {
    let contentType: String
    let dataBase64: String
}

struct PersonalLifeExpenseCorrectionRequest: Encodable {
    let lifeAreaCode: String
    let lifeSubcategoryCode: String
    let moodCode: String?
    let merchantName: String?
    let photo: PersonalExpensePhotoRequest?
}

struct PersonalLifeRefundRequest: Encodable {}

struct PersonalExpenseConfirmationPayload: Decodable {
    let expenseId: String
    let amount: String
    let currencyCode: String
    let merchantName: String?
    let effectiveAt: String
    let lifeAreaCode: String
    let lifeAreaLabel: String
    let lifeSubcategoryCode: String
    let lifeSubcategoryLabel: String
    let layer: String
    let moodCode: String?
    let moodLabel: String?
    let monthTotal: String
    let monthExpenseCount: Int
    let areaMonthTotal: String
    let areaMonthCount: Int
    let moodResponses: Int
    let moodCoverage: Double?
    let photoUploadId: String?
}

struct PersonalLifeRefundPayload: Decodable { let expenseId: String; let reversedExpenseId: String; let amount: String }
struct PersonalLifeVoidPayload: Decodable { let expenseId: String; let status: String }

private struct ExpenseDraft {
    var amount = ""
    var merchant = ""
    var effectiveAt = ISO8601DateFormatter().string(from: Date())
    var areaCode: String?
    var subcategoryCode: String?
    var moodCode: String?
    var note = ""
    var paymentSource = ""
    var showMore = false
    var editingId: String?
    var photoPreview: Data?
}

private enum ExperienceFlow {
    case browse
    case sheet(ExpenseDraft)
    case area(ExpenseDraft)
    case subcategory(ExpenseDraft)
    case saved(PersonalExpenseConfirmationPayload)
}

struct PersonalExperienceHost: View {
    let destination: BottomDestination
    var hasActiveMoment: Bool = false
    var momentTypeCode: String? = nil
    var capabilityCodes: [String]? = nil
    var onOpenLife: () -> Void = {}
    var onOpenPulse: () -> Void = {}
    var onOpenMoments: () -> Void = {}
    var onCloseCreate: () -> Void = {}
    var onSetupMissing: () -> Void = {}
    var onLifeOpsQuickAdd: (LifeOpsQuickAddKind) -> Void = { _ in }
    var onMoneyQuickAdd: (MoneyQuickAddKind) -> Void = { _ in }
    @State private var experience: PersonalExperiencePayload?
    @State private var errorText: String?
    @State private var loading = true
    @State private var flow: ExperienceFlow = .browse
    @State private var busy = false
    @State private var withMood = false
    @State private var showAllActivity = false
    @State private var focusMood = false
    @State private var focusStories = false
    @State private var focusAreas = false
    @State private var pendingMoodScroll = false
    @State private var memoryCurrentOnly = false
    @State private var savedPhotoItem: PhotosPickerItem?

    var body: some View {
        ZStack {
            PersonalTokens.Colors.bgPrimary.ignoresSafeArea()
            content
        }
        .task { await reload() }
        .onChange(of: savedPhotoItem) { _, item in
            guard let item else { return }
            Task { await attachSavedPhoto(item) }
        }
        .sheet(isPresented: expenseSheetPresented) {
            if case .sheet(let draft) = flow {
                PersonalExpenseSheetForm(
                    initial: draft,
                    busy: busy,
                    errorText: errorText,
                    onClose: { flow = .browse },
                    onPickArea: { flow = .area($0) },
                    onSave: { updated in Task { await save(updated) } },
                    onRefund: { id in Task { await refund(id) } },
                    onVoid: { id in Task { await voidExpense(id) } }
                )
                .presentationDetents([.large])
                .presentationDragIndicator(.hidden)
                .presentationCornerRadius(32)
                .presentationBackground(PersonalTokens.Colors.bgPrimary)
            }
        }
    }

    private var expenseSheetPresented: Binding<Bool> {
        Binding(
            get: {
                if case .sheet = flow { return true }
                return false
            },
            set: { presented in
                if !presented, case .sheet = flow { flow = .browse }
            }
        )
    }

    @ViewBuilder
    private var content: some View {
        switch flow {
        case .browse, .sheet(_):
            if loading && experience == nil {
                ProgressView().tint(PersonalTokens.Colors.brandAccent)
            } else {
                ScrollViewReader { proxy in
                ScrollView {
                    VStack(alignment: .leading, spacing: 16) {
                        if let errorText { Text(errorText).foregroundStyle(PersonalTokens.Colors.textSecondary) }
                        switch destination {
                        case .create: hub
                        case .pulse: pulse
                        case .moments: moments
                        case .life: life
                        case .memory: memory
                        }
                    }
                    .padding(20)
                    .padding(.bottom, 88)
                }
                    .onChange(of: focusMood) { _, on in
                        if on {
                            withAnimation { proxy.scrollTo("mood-pulse", anchor: .top) }
                            focusMood = false
                        }
                    }
                    .onChange(of: focusStories) { _, on in
                        if on {
                            withAnimation { proxy.scrollTo("moment-stories", anchor: .top) }
                            focusStories = false
                        }
                    }
                    .onChange(of: focusAreas) { _, on in
                        if on {
                            withAnimation { proxy.scrollTo("life-areas", anchor: .top) }
                            focusAreas = false
                        }
                    }
                    .onChange(of: destination) { _, dest in
                        if dest == .pulse && pendingMoodScroll {
                            pendingMoodScroll = false
                            DispatchQueue.main.async {
                                withAnimation { proxy.scrollTo("mood-pulse", anchor: .top) }
                            }
                        }
                    }
                }
            }
        case .area(let draft):
            PersonalAreaStep(
                draft: draft,
                onBack: { flow = .sheet(draft) },
                onClose: { flow = .browse },
                onNext: { flow = .subcategory($0) }
            )
        case .subcategory(let draft):
            PersonalSubcategoryStep(
                draft: draft,
                onBack: { flow = .area(draft) },
                onClose: { flow = .browse },
                onChangeArea: { flow = .area(draft) },
                onUse: { flow = .sheet($0) }
            )
        case .saved(let confirmation):
            saved(confirmation)
        }
    }

    private var hub: some View {
        PersonalQuickAddHubView(
            hasActiveMoment: hasActiveMoment,
            momentTypeCode: momentTypeCode,
            capabilityCodes: capabilityCodes,
            onSetupMissing: onSetupMissing,
            onClose: onCloseCreate,
            onSpend: { onMoneyQuickAdd(.masterExpense) },
            onIncome: { onMoneyQuickAdd(.income) },
            onRecovery: { onLifeOpsQuickAdd(.recovery) },
            onMood: { onLifeOpsQuickAdd(.mood) },
            onAttention: { onLifeOpsQuickAdd(.attention) },
            onAdjust: { onLifeOpsQuickAdd(.adjust) },
            onTransfer: { onMoneyQuickAdd(.transfer) },
            onSavings: { onMoneyQuickAdd(.savings) },
            onFutureQuickAdd: { _ in onSetupMissing() },
            onLifestyleQuickAdd: { _ in onSetupMissing() },
            onRelationshipsQuickAdd: { _ in onSetupMissing() }
        )
    }

    private func saved(_ confirmation: PersonalExpenseConfirmationPayload) -> some View {
        let month = monthName(confirmation.effectiveAt)
        let title = (confirmation.merchantName?.isEmpty == false) ? confirmation.merchantName! : confirmation.lifeSubcategoryLabel
        let coverage = confirmation.moodCoverage.map { String(format: "%.1f%%", $0 * 100) } ?? "—"
        let hasPhoto = !(confirmation.photoUploadId ?? "").isEmpty
        let sub = PersonalLifeTaxonomy.subcategory(confirmation.lifeSubcategoryCode)
        return ScrollView {
            VStack(alignment: .leading, spacing: PersonalTokens.Space.lg) {
                VStack(spacing: 10) {
                    ZStack {
                        Circle().fill(PersonalTokens.Colors.fillGlow).frame(width: 112, height: 112)
                        Circle().fill(PersonalTokens.Colors.surfaceHighlight).frame(width: 72, height: 72)
                            .overlay(Circle().stroke(PersonalTokens.Colors.brandPrimary, lineWidth: 1))
                        Image("PersonalIcons/check").renderingMode(.template)
                            .foregroundStyle(PersonalTokens.Colors.brandPrimary)
                            .frame(width: 28, height: 28)
                    }
                    Text("A little more life, recorded.")
                        .personalText(PersonalTokens.Typography.hero)
                        .foregroundStyle(PersonalTokens.Colors.textPrimary)
                        .multilineTextAlignment(.center)
                    HStack(spacing: 12) {
                        Image("PersonalIcons/check").renderingMode(.template)
                            .foregroundStyle(PersonalTokens.Colors.signalPositive)
                            .frame(width: 16, height: 16)
                            .frame(width: 28, height: 28)
                            .background(PersonalTokens.Colors.fillIconWellQuiet, in: Circle())
                        Text("Expense saved · counted once")
                            .personalText(PersonalTokens.Typography.body)
                            .foregroundStyle(PersonalTokens.Colors.textPrimary)
                Spacer()
            }
                    .padding(.horizontal, 16)
                    .padding(.vertical, 12)
                    .background(PersonalTokens.Colors.surfaceContainer, in: RoundedRectangle(cornerRadius: 12))
                    .overlay(RoundedRectangle(cornerRadius: 12).stroke(PersonalTokens.Colors.borderHairline, lineWidth: 1))
                }
                .frame(maxWidth: .infinity)
                VStack(alignment: .leading, spacing: 12) {
                    HStack(alignment: .center) {
                        Text(inr(confirmation.amount, confirmation.currencyCode))
                            .personalText(PersonalTokens.Typography.amountCard)
                            .foregroundStyle(PersonalTokens.Colors.textPrimary)
                Spacer()
                        (sub?.icon ?? Image("PersonalIcons/receipt"))
                            .renderingMode(.template)
                            .foregroundStyle(sub?.tint ?? PersonalTokens.Colors.brandPrimary)
                            .frame(width: 32, height: 32)
                    }
                    Text(title).personalText(PersonalTokens.Typography.section).foregroundStyle(PersonalTokens.Colors.textPrimary)
                    Text(formatSheetWhen(confirmation.effectiveAt)).personalText(PersonalTokens.Typography.caption).foregroundStyle(PersonalTokens.Colors.textSecondary)
                    Rectangle().fill(PersonalTokens.Colors.borderDivider).frame(height: 1)
                    Button {
                        flow = .area(ExpenseDraft(
                            amount: confirmation.amount,
                            merchant: confirmation.merchantName ?? "",
                            effectiveAt: confirmation.effectiveAt,
                            areaCode: confirmation.lifeAreaCode,
                            subcategoryCode: confirmation.lifeSubcategoryCode,
                            moodCode: confirmation.moodCode,
                            editingId: confirmation.expenseId
                        ))
                    } label: {
                        HStack {
                            VStack(alignment: .leading, spacing: 4) {
                                Text("\(confirmation.lifeAreaLabel) › \(confirmation.lifeSubcategoryLabel)")
                                    .personalText(PersonalTokens.Typography.body).foregroundStyle(PersonalTokens.Colors.textPrimary)
                                Text("Edit Life context").personalText(PersonalTokens.Typography.caption).foregroundStyle(PersonalTokens.Colors.brandPrimary)
                            }
                            Spacer()
                            Image("PersonalIcons/pencil").renderingMode(.template)
                                .foregroundStyle(PersonalTokens.Colors.textSecondary)
                                .frame(width: 18, height: 18)
                        }
                    }
                    .buttonStyle(.plain)
                    HStack(spacing: 8) {
                        Image("PersonalIcons/smile").renderingMode(.template)
                        .foregroundStyle(PersonalTokens.Colors.signalPositive)
                            .frame(width: 20, height: 20)
                        Text(confirmation.moodLabel.map { "Mood: \($0) · chosen by you" } ?? "No mood recorded")
                            .personalText(PersonalTokens.Typography.metadata)
                            .foregroundStyle(PersonalTokens.Colors.textPrimary)
                    }
                }
                .padding(20)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(PersonalTokens.Colors.surfaceHighlight, in: RoundedRectangle(cornerRadius: PersonalTokens.Radius.card))
                .overlay(RoundedRectangle(cornerRadius: PersonalTokens.Radius.card).stroke(PersonalTokens.Colors.borderDivider, lineWidth: 1))
                savedPhotoRow(hasPhoto: hasPhoto)
                if let errorText {
                    Text(errorText).personalText(PersonalTokens.Typography.caption).foregroundStyle(PersonalTokens.Colors.textSecondary)
                }
                VStack(alignment: .leading, spacing: PersonalTokens.Space.compact) {
                    lifeHeading("\(month), updated", subtitle: nil)
                    VStack(alignment: .leading, spacing: 14) {
                        HStack(alignment: .top) {
                            VStack(alignment: .leading, spacing: 4) {
                                Text("\(month) total").personalText(PersonalTokens.Typography.caption).foregroundStyle(PersonalTokens.Colors.textSecondary)
                                Text(inr(confirmation.monthTotal, confirmation.currencyCode)).personalText(PersonalTokens.Typography.storyTitle).foregroundStyle(PersonalTokens.Colors.textPrimary)
                                Text(lifeExpenseCount(confirmation.monthExpenseCount)).personalText(PersonalTokens.Typography.caption).foregroundStyle(PersonalTokens.Colors.textSecondary)
                            }
                            .frame(maxWidth: .infinity, alignment: .leading)
                            VStack(alignment: .leading, spacing: 4) {
                                Text(confirmation.lifeAreaLabel).personalText(PersonalTokens.Typography.caption).foregroundStyle(PersonalTokens.Colors.textSecondary)
                                Text(inr(confirmation.areaMonthTotal, confirmation.currencyCode)).personalText(PersonalTokens.Typography.storyTitle).foregroundStyle(PersonalTokens.Colors.textPrimary)
                                Text(lifeExpenseCount(confirmation.areaMonthCount)).personalText(PersonalTokens.Typography.caption).foregroundStyle(PersonalTokens.Colors.textSecondary)
                            }
                            .frame(maxWidth: .infinity, alignment: .leading)
                        }
                        Rectangle().fill(PersonalTokens.Colors.borderDivider).frame(height: 1)
                        HStack {
                            Text("Mood · \(confirmation.moodResponses) / \(confirmation.monthExpenseCount) responses")
                                .personalText(PersonalTokens.Typography.body).foregroundStyle(PersonalTokens.Colors.textSecondary)
                            Spacer()
                            Text(coverage).personalText(PersonalTokens.Typography.metadata).foregroundStyle(PersonalTokens.Colors.brandPrimary)
                        }
                    }
                    .padding(20)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(PersonalTokens.Colors.surfaceContainer, in: RoundedRectangle(cornerRadius: PersonalTokens.Radius.card))
                    .overlay(RoundedRectangle(cornerRadius: PersonalTokens.Radius.card).stroke(PersonalTokens.Colors.borderDivider, lineWidth: 1))
                }
                VStack(spacing: 12) {
                    Button {
                        flow = .browse
                        onOpenPulse()
                    } label: {
                        Text("Back to Pulse")
                            .personalText(PersonalTokens.Typography.button)
                            .foregroundStyle(PersonalTokens.Colors.textOnAccent)
                            .frame(maxWidth: .infinity)
                            .frame(height: PersonalTokens.Size.buttonVisual)
                            .background(PersonalTokens.Colors.brandAccent, in: RoundedRectangle(cornerRadius: PersonalTokens.Radius.button))
                    }
                    .buttonStyle(.plain)
                    Button {
                    flow = .sheet(ExpenseDraft(
                        amount: confirmation.amount,
                        merchant: confirmation.merchantName ?? "",
                        effectiveAt: confirmation.effectiveAt,
                        areaCode: confirmation.lifeAreaCode,
                        subcategoryCode: confirmation.lifeSubcategoryCode,
                        moodCode: confirmation.moodCode,
                        editingId: confirmation.expenseId
                    ))
                    } label: {
                        HStack(spacing: 8) {
                            Text("View expense").personalText(PersonalTokens.Typography.body).foregroundStyle(PersonalTokens.Colors.brandPrimary)
                            Image("PersonalIcons/arrow-up-right").renderingMode(.template)
                .foregroundStyle(PersonalTokens.Colors.brandPrimary)
                                .frame(width: 16, height: 16)
                        }
                    }
                    .buttonStyle(.plain)
                    Text("One expense, counted once. No separate Moment payment.")
                        .personalText(PersonalTokens.Typography.caption)
                        .foregroundStyle(PersonalTokens.Colors.textSecondary)
                        .multilineTextAlignment(.center)
                }
                .frame(maxWidth: .infinity)
            }
            .padding(20)
        }
    }

    @ViewBuilder
    private func savedPhotoRow(hasPhoto: Bool) -> some View {
        let row = HStack(spacing: 14) {
            Image(hasPhoto ? "PersonalIcons/check" : "PersonalIcons/camera").renderingMode(.template)
                .foregroundStyle(PersonalTokens.Colors.brandPrimary)
                .frame(width: 26, height: 26)
                .frame(width: 48, height: 60)
                .background(PersonalTokens.Colors.fillIconWellQuiet, in: RoundedRectangle(cornerRadius: 12))
            VStack(alignment: .leading, spacing: 4) {
                Text(hasPhoto ? "Photo added · separate from receipt" : "Add memory photo")
                    .personalText(PersonalTokens.Typography.body).foregroundStyle(PersonalTokens.Colors.brandPrimary)
                if !hasPhoto {
                    Text("Optional · Upload a photo to remember this")
                        .personalText(PersonalTokens.Typography.caption).foregroundStyle(PersonalTokens.Colors.textSecondary)
                    Text("No photo added · separate from receipt")
                        .personalText(PersonalTokens.Typography.caption).foregroundStyle(PersonalTokens.Colors.textSecondary)
                }
            }
            Spacer()
            if !hasPhoto {
                Image("PersonalIcons/plus").renderingMode(.template)
                    .foregroundStyle(PersonalTokens.Colors.brandPrimary)
                    .frame(width: 18, height: 18)
            }
        }
        .padding(16)
        .overlay(RoundedRectangle(cornerRadius: PersonalTokens.Radius.tileSelected).stroke(PersonalTokens.Colors.brandPrimary, style: StrokeStyle(lineWidth: 1, dash: [6, 4])))
        if hasPhoto {
            row
        } else {
            PhotosPicker(selection: $savedPhotoItem, matching: .images) { row }
                .buttonStyle(.plain)
                .disabled(busy)
        }
    }

    private func attachSavedPhoto(_ item: PhotosPickerItem) async {
        guard case .saved(let confirmation) = flow else { return }
        guard let data = try? await item.loadTransferable(type: Data.self), let jpeg = jpegPhotoData(data) else { return }
        busy = true
        defer { busy = false; savedPhotoItem = nil }
        do {
            let updated = try await APIClient.shared.correctPersonalLifeExpense(
                expenseId: confirmation.expenseId,
                body: PersonalLifeExpenseCorrectionRequest(
                    lifeAreaCode: confirmation.lifeAreaCode,
                    lifeSubcategoryCode: confirmation.lifeSubcategoryCode,
                    moodCode: confirmation.moodCode,
                    merchantName: confirmation.merchantName,
                    photo: expensePhoto(jpeg)
                )
            )
            flow = .saved(updated)
            await reload()
        } catch {
            errorText = error.localizedDescription
        }
    }

    private var pulse: some View {
        let cold = experience?.cold != false
        let introBlock = experience?.pulse.intro
        return VStack(alignment: .leading, spacing: 12) {
            intro(
                kicker: introBlock?.kicker ?? (cold ? "A FRESH START" : "YOUR PULSE"),
                title: introBlock?.title ?? (cold ? "Your everyday starts here." : "Your Pulse"),
                subtitle: introBlock?.subtitle ?? (cold ? "Start with one expense. Add a little life context." : "A little context. A clearer everyday."),
                action: cold ? nil : { flow = .sheet(ExpenseDraft()) }
            )
            if cold {
                coldPulse
            } else {
                populatedPulse
            }
        }
    }

    private var coldPulse: some View {
        VStack(alignment: .leading, spacing: 12) {
            section("Recent Activity")
            card {
                HStack(alignment: .center) {
                    VStack(alignment: .leading, spacing: 8) {
                        Text("0 EXPENSES, ALL POSSIBILITY").personalText(PersonalTokens.Typography.micro).foregroundStyle(PersonalTokens.Colors.textSubtle)
                        Text("The little things count.").personalText(PersonalTokens.Typography.storyTitle).foregroundStyle(PersonalTokens.Colors.textPrimary)
                    }
                    Spacer()
                    Image("PersonalIcons/receipt")
                        .renderingMode(.template)
                        .foregroundStyle(PersonalTokens.Colors.textPrimary)
                        .frame(width: 22, height: 22)
                        .frame(width: 56, height: 56)
                        .background(PersonalTokens.Colors.fillIconWell, in: Circle())
                }
                Text("No expenses yet. Begin with the last thing you spent on.").foregroundStyle(PersonalTokens.Colors.textSecondary)
                Button("+ Add your first expense") { flow = .sheet(ExpenseDraft()) }
                    .buttonStyle(.borderedProminent)
                    .tint(PersonalTokens.Colors.brandAccent)
                    .frame(maxWidth: .infinity)
                Text("Amount → Life Area → subcategory. Mood is optional.").font(.caption).foregroundStyle(PersonalTokens.Colors.textSubtle)
            }
            section("Needs Attention")
            quietLine("PersonalIcons/circle-check", "0 issues. Nothing waiting for review.")
            section("Spending Snapshot")
            card {
                HStack {
                    Text((experience?.pulse.snapshot.periodLabel ?? "This month").uppercased())
                        .font(.caption).foregroundStyle(PersonalTokens.Colors.textSubtle)
                    Spacer()
                    Text("0 expenses").font(.caption).foregroundStyle(PersonalTokens.Colors.textSecondary)
                        .padding(.horizontal, 12).padding(.vertical, 8)
                        .overlay(Capsule().stroke(PersonalTokens.Colors.borderDivider, lineWidth: 1))
                }
                Text(inr("0", experience?.currencyCode)).personalText(PersonalTokens.Typography.hero).foregroundStyle(PersonalTokens.Colors.textPrimary)
                Text("No prior period to compare").foregroundStyle(PersonalTokens.Colors.textSecondary)
            }
            section("Life Signals")
            quietLine("PersonalIcons/sparkles", experience?.pulse.signalsMessage ?? "No signals yet. Patterns need enough supporting activity.")
            section("Mood Pulse")
            card {
                HStack {
                    Image("PersonalIcons/smile").renderingMode(.template).foregroundStyle(PersonalTokens.Colors.textPrimary)
                    VStack(alignment: .leading) {
                        Text("0 responses / 0 expenses").fontWeight(.semibold).foregroundStyle(PersonalTokens.Colors.textPrimary)
                        Text("Coverage unavailable").font(.caption).foregroundStyle(PersonalTokens.Colors.textSubtle)
                    }
                }
                Text(experience?.pulse.mood.message ?? "Only feelings you choose to record. Never inferred.")
                    .foregroundStyle(PersonalTokens.Colors.textSecondary)
            }
        }
    }

    private var populatedPulse: some View {
            let recentAll = experience?.pulse.recent ?? []
            let recent = withMood ? recentAll.filter { $0.moodCode != nil } : recentAll
        let visible = showAllActivity ? recent : Array(recent.prefix(4))
        let total = experience?.expenseCount ?? recentAll.count
        let snap = experience?.pulse.snapshot
        let signals = experience?.pulse.signals ?? []
        let mood = experience?.pulse.mood
        return VStack(alignment: .leading, spacing: 12) {
            section("Recent Activity")
                HStack(spacing: 8) {
                    Button("All activity") { withMood = false }
                        .buttonStyle(.borderedProminent)
                    .tint(withMood ? PersonalTokens.Colors.surfaceHigh : PersonalTokens.Colors.brandAccent)
                    Button("With mood") { withMood = true }
                        .buttonStyle(.borderedProminent)
                    .tint(withMood ? PersonalTokens.Colors.brandAccent : PersonalTokens.Colors.surfaceHigh)
                Text("\(total) expenses").font(.caption).foregroundStyle(PersonalTokens.Colors.textSecondary)
                    .padding(.horizontal, 12).padding(.vertical, 8)
                    .overlay(Capsule().stroke(PersonalTokens.Colors.borderDivider, lineWidth: 1))
            }
            if recent.isEmpty {
                Text("No expenses with a mood in this list.").foregroundStyle(PersonalTokens.Colors.textSecondary)
            } else {
                ForEach(Array(visible.enumerated()), id: \.element.id) { index, row in
                    activityRow(row, emphasized: index == 0 && !withMood)
                }
            }
            if recent.count > 4 {
                exploreRow(showAllActivity ? "Show recent expenses" : "View all \(total) expenses") { showAllActivity.toggle() }
            }
            section("Needs Attention")
            let attention = experience?.pulse.attention
            quietLine(
                "PersonalIcons/circle-check",
                attention?.title ?? attention?.message ?? "All clear.",
                attention?.body
            )
            VStack(alignment: .leading, spacing: 4) {
                section("Spending Snapshot")
                if let range = snap?.rangeLabel, !range.isEmpty {
                    Text(range).font(.caption).foregroundStyle(PersonalTokens.Colors.textSubtle)
                }
            }
                card {
                HStack {
                    Text(inr(snap?.total ?? "0", experience?.currencyCode)).personalText(PersonalTokens.Typography.hero).foregroundStyle(PersonalTokens.Colors.textPrimary)
                    Spacer()
                    Button(action: onOpenLife) {
                        Image(systemName: "arrow.up.right").foregroundStyle(PersonalTokens.Colors.textSecondary)
                    }
                }
                Text(deltaLine(snap)).foregroundStyle(PersonalTokens.Colors.textSecondary)
                if snap?.comparable == true {
                    compareTrack(snap?.total ?? "0", snap?.previousTotal)
                    VStack(alignment: .leading, spacing: 4) {
                        Text(inr(snap?.previousTotal, experience?.currencyCode)).fontWeight(.semibold).foregroundStyle(PersonalTokens.Colors.textPrimary)
                        Text([snap?.previousRangeLabel, "\(snap?.previousExpenseCount ?? 0) expenses"].compactMap { $0 }.joined(separator: " · "))
                            .font(.caption).foregroundStyle(PersonalTokens.Colors.textSubtle)
                    }
                    .padding(12)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(PersonalTokens.Colors.surfaceHighlight, in: RoundedRectangle(cornerRadius: 14))
                }
                if let note = snap?.comparisonNote, !note.isEmpty {
                    Text(note).foregroundStyle(PersonalTokens.Colors.textSecondary)
                }
                exploreRow("Explore spending in Life", action: onOpenLife)
            }
            VStack(alignment: .leading, spacing: 4) {
                section("Life Signals")
                if !signals.isEmpty, let prompt = experience?.pulse.signalsPrompt {
                    Text(prompt).font(.caption).foregroundStyle(PersonalTokens.Colors.textSubtle)
                }
            }
            if signals.isEmpty {
                quietLine("PersonalIcons/sparkles", experience?.pulse.signalsMessage ?? "No signals yet. Patterns need enough supporting activity.")
            } else {
                card {
                    ForEach(signals) { signal in
                        Text(signal.subcategoryLabel ?? signal.areaLabel).fontWeight(.semibold).foregroundStyle(PersonalTokens.Colors.textPrimary)
                        Text(signal.areaLabel).font(.caption).foregroundStyle(PersonalTokens.Colors.textSubtle)
                        signalBar(snap?.previousShort ?? "Prev", inr(signal.previous, experience?.currencyCode), signal.previous, signal.current, muted: true)
                        signalBar(snap?.currentShort ?? "Now", inr(signal.current, experience?.currencyCode), signal.current, signal.previous, muted: false)
                        Text(signal.sentence ?? "").font(.caption).foregroundStyle(PersonalTokens.Colors.textSecondary)
                    }
                    exploreRow("View supporting expenses", action: onOpenLife)
                }
            }
            VStack(alignment: .leading, spacing: 4) {
                section("Mood Pulse")
                Text("The feelings you chose to record").font(.caption).foregroundStyle(PersonalTokens.Colors.textSubtle)
            }
            .id("mood-pulse")
            card {
                HStack {
                    Text("\(mood?.responseCount ?? 0) / \(mood?.expenseCount ?? 0) expenses")
                        .font(.caption).foregroundStyle(PersonalTokens.Colors.textSubtle)
                    Spacer()
                    Text(coverageLabel(mood)).font(.caption).foregroundStyle(PersonalTokens.Colors.brandPrimary)
                }
                let maxCount = max(mood?.counts.map(\.count).max() ?? 1, 1)
                HStack(alignment: .bottom, spacing: 8) {
                    ForEach(mood?.counts ?? [], id: \.moodCode) { count in
                        VStack(spacing: 4) {
                            Text("\(count.count)").font(.caption2).foregroundStyle(PersonalTokens.Colors.textSecondary)
                            RoundedRectangle(cornerRadius: 6)
                                .fill(PersonalTokens.Colors.brandSecondary)
                                .frame(width: 28, height: CGFloat(12 + 72 * count.count / maxCount))
                            Text(count.label).font(.caption2).foregroundStyle(PersonalTokens.Colors.textSubtle)
                        }
                        .frame(maxWidth: .infinity)
                    }
                }
                Text(mood?.message ?? "Only feelings you choose to record. Never inferred.")
                    .foregroundStyle(PersonalTokens.Colors.textSecondary)
                exploreRow("View mood context") { focusMood = true }
            }
        }
    }

    private func activityRow(_ row: PersonalExperiencePayload.ActivityRow, emphasized: Bool) -> some View {
        let icon = PersonalLifeTaxonomy.subcategory(row.subcategoryCode)?.iconAssetName ?? "PersonalIcons/receipt"
        return Button {
                        flow = .sheet(ExpenseDraft(
                            amount: row.amount,
                            merchant: row.merchantName,
                            effectiveAt: row.effectiveAt,
                            areaCode: row.areaCode,
                            subcategoryCode: row.subcategoryCode,
                            moodCode: row.moodCode,
                            editingId: row.expenseId
                        ))
                    } label: {
            VStack(alignment: .leading, spacing: 6) {
                            HStack {
                    Image(icon).renderingMode(.template).foregroundStyle(PersonalTokens.Colors.textPrimary)
                        .frame(width: 36, height: 36)
                        .background(PersonalTokens.Colors.fillIconWell, in: RoundedRectangle(cornerRadius: 10))
                                Text(row.merchantName).foregroundStyle(PersonalTokens.Colors.textPrimary)
                                Spacer()
                                Text(inr(row.amount, row.currencyCode)).foregroundStyle(PersonalTokens.Colors.textPrimary)
                            }
                Text([formatPulseWhen(row.effectiveAt), row.moodLabel.map { "Mood: \($0)" }].compactMap { $0 }.joined(separator: " · "))
                                .font(.caption).foregroundStyle(PersonalTokens.Colors.textSubtle)
                            Text("\(row.areaLabel) · \(row.subcategoryLabel)").font(.caption).foregroundStyle(PersonalTokens.Colors.textSecondary)
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)
            .padding(14)
            .background(emphasized ? PersonalTokens.Colors.surfaceHighlight : PersonalTokens.Colors.surfaceContainer, in: RoundedRectangle(cornerRadius: 16))
        }
        .buttonStyle(.plain)
    }

    private func quietLine(_ icon: String, _ title: String, _ body: String? = nil) -> some View {
        HStack(alignment: .center, spacing: 12) {
            Image(icon).renderingMode(.template).foregroundStyle(PersonalTokens.Colors.textPrimary)
                .frame(width: 36, height: 36)
                .background(PersonalTokens.Colors.fillIconWellQuiet, in: Circle())
            VStack(alignment: .leading, spacing: 2) {
                Text(title).foregroundStyle(PersonalTokens.Colors.textPrimary)
                if let body, !body.isEmpty {
                    Text(body).font(.caption).foregroundStyle(PersonalTokens.Colors.textSecondary)
                }
            }
        }
    }

    private func exploreRow(_ label: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            HStack {
                Text(label).foregroundStyle(PersonalTokens.Colors.brandPrimary)
                Spacer()
                Image(systemName: "arrow.up.right").foregroundStyle(PersonalTokens.Colors.brandPrimary)
            }
        }
        .buttonStyle(.plain)
    }

    private func compareTrack(_ current: String, _ previous: String?) -> some View {
        let cur = Double(current) ?? 0
        let prev = Double(previous ?? "") ?? 0
        let maxValue = max(max(cur, prev), 1)
        return ZStack(alignment: .leading) {
            Capsule().fill(PersonalTokens.Colors.textSubtle.opacity(0.45)).frame(width: nil)
            GeometryReader { geo in
                Capsule().fill(PersonalTokens.Colors.textSubtle.opacity(0.45)).frame(width: geo.size.width * prev / maxValue)
                Capsule().fill(PersonalTokens.Colors.brandSecondary).frame(width: geo.size.width * cur / maxValue)
            }
        }
        .frame(height: 10)
    }

    private func signalBar(_ label: String, _ amount: String, _ value: String, _ other: String, muted: Bool) -> some View {
        let mine = Double(value) ?? 0
        let theirs = Double(other) ?? 0
        let maxValue = max(max(mine, theirs), 1)
        return HStack {
            Text(label).font(.caption2).foregroundStyle(PersonalTokens.Colors.textSubtle).frame(width: 36, alignment: .leading)
            GeometryReader { geo in
                ZStack(alignment: .leading) {
                    Capsule().fill(PersonalTokens.Colors.surfaceHigh)
                    Capsule()
                        .fill(muted ? PersonalTokens.Colors.textSubtle : PersonalTokens.Colors.brandSecondary)
                        .frame(width: max(8, geo.size.width * mine / maxValue))
                }
            }
            .frame(height: 8)
            Text(amount).font(.caption).foregroundStyle(PersonalTokens.Colors.textSecondary)
        }
    }

    private func deltaLine(_ snap: PersonalExperiencePayload.Snapshot?) -> String {
        let count = "\(snap?.expenseCount ?? 0) expenses"
        guard snap?.comparable == true, let raw = snap?.deltaAmount, let delta = Double(raw), let ratio = snap?.deltaRatio else { return count }
        let sign = delta >= 0 ? "+" : "−"
        let pct = String(format: "%.1f", abs(ratio) * 100)
        return "\(count) · \(sign)\(inr(String(abs(delta)), experience?.currencyCode)) / \(pct)%"
    }

    private func coverageLabel(_ mood: PersonalExperiencePayload.MoodPulse?) -> String {
        guard let coverage = mood?.coverage else { return "Coverage unavailable" }
        return "\(Int(coverage * 100))% coverage"
    }

    private var moments: some View {
        let active = experience?.moments.active ?? []
        let emerging = experience?.moments.emerging ?? []
        let recent = experience?.moments.recent ?? []
        let cold = active.isEmpty && emerging.isEmpty && recent.isEmpty
        let introBlock = experience?.moments.intro
        return VStack(alignment: .leading, spacing: cold ? 24 : 32) {
            momentsIntro(
                kicker: introBlock?.kicker ?? (cold ? "A FRESH START" : "MOMENTS"),
                title: introBlock?.title ?? (cold ? "Life happens. Stories follow." : "Moments"),
                subtitle: introBlock?.subtitle ?? (cold
                    ? "Your meaningful expenses can connect into Moments automatically."
                    : "Meaningful stories, taking shape in your expenses. Found automatically, not created."),
                action: cold ? nil : { flow = .sheet(ExpenseDraft()) }
            )
            if cold {
                coldMoments
            } else {
                populatedMoments(active: active, emerging: emerging, recent: recent)
            }
        }
    }

    private func momentsIntro(kicker: String, title: String, subtitle: String, action: (() -> Void)?) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(kicker).personalText(PersonalTokens.Typography.metadata).foregroundStyle(PersonalTokens.Colors.brandPrimary)
            HStack {
                Text(title).personalText(PersonalTokens.Typography.hero).foregroundStyle(PersonalTokens.Colors.textPrimary)
                Spacer()
                if let action {
                    Button("+ Expense", action: action)
                        .buttonStyle(.borderedProminent)
                        .tint(PersonalTokens.Colors.brandAccent)
                }
            }
            Text(subtitle).personalText(PersonalTokens.Typography.body).foregroundStyle(PersonalTokens.Colors.textSecondary)
        }
    }

    private var coldMoments: some View {
        VStack(alignment: .leading, spacing: 24) {
            section("Active Moments")
            VStack(alignment: .leading, spacing: 12) {
                Image("PersonalArtwork/art_pers_open_beginning")
                    .resizable().scaledToFill().frame(height: 240).clipped()
                    .clipShape(RoundedRectangle(cornerRadius: 24))
                Text("NO ACTIVE MOMENTS YET").personalText(PersonalTokens.Typography.storyKicker).foregroundStyle(PersonalTokens.Colors.brandPrimary)
                Text("Room for your next chapter.").personalText(PersonalTokens.Typography.storyTitle).foregroundStyle(PersonalTokens.Colors.textPrimary)
                Text("Start with an expense. Let meaningful connections take shape.")
                    .personalText(PersonalTokens.Typography.caption).foregroundStyle(PersonalTokens.Colors.textSecondary)
                Text(experience?.moments.heroMonth ?? "A fresh start")
                    .personalText(PersonalTokens.Typography.caption).foregroundStyle(PersonalTokens.Colors.textSubtle)
                Text(experience?.moments.heroEvidence ?? "0 stories · no supporting activity")
                    .personalText(PersonalTokens.Typography.caption).foregroundStyle(PersonalTokens.Colors.brandPrimary)
            }
            .padding(12)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(PersonalTokens.Colors.surfaceHighlight, in: RoundedRectangle(cornerRadius: 24))
            Button { flow = .sheet(ExpenseDraft()) } label: {
                Text("+ Add your first expense")
                    .personalText(PersonalTokens.Typography.button)
                    .foregroundStyle(PersonalTokens.Colors.textOnAccent)
                    .frame(maxWidth: .infinity)
                    .frame(height: 52)
                    .background(PersonalTokens.Colors.brandAccent, in: RoundedRectangle(cornerRadius: 14))
            }
            .buttonStyle(.plain)
            section("Emerging Moments")
            VStack(alignment: .leading, spacing: 14) {
                HStack(spacing: 8) {
                    Image("PersonalIcons/receipt").renderingMode(.template).frame(width: 22, height: 22)
                    Rectangle().fill(PersonalTokens.Colors.brandPrimary).frame(width: 24, height: 1)
                    Image("PersonalIcons/link").renderingMode(.template).frame(width: 22, height: 22)
                    Rectangle().fill(PersonalTokens.Colors.brandPrimary).frame(width: 24, height: 1)
                    Image("PersonalIcons/layers").renderingMode(.template).frame(width: 22, height: 22)
                }
                .foregroundStyle(PersonalTokens.Colors.textPrimary)
                Text("No connections yet.").personalText(PersonalTokens.Typography.body).foregroundStyle(PersonalTokens.Colors.textPrimary)
                Text("Related expenses need shared meaning. Repeating the same merchant alone isn’t a story.")
                    .personalText(PersonalTokens.Typography.body).foregroundStyle(PersonalTokens.Colors.textSecondary)
            }
            .padding(20)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(PersonalTokens.Colors.surfaceContainer, in: RoundedRectangle(cornerRadius: 24))
            .overlay(RoundedRectangle(cornerRadius: 24).stroke(PersonalTokens.Colors.brandPrimary.opacity(0.15), lineWidth: 1))
            section("Moment Progress")
            quietLine("PersonalIcons/route", "No story activity yet. Progress appears with supporting expenses—not a completion score.")
            section("Recent Moments")
            quietLine("PersonalIcons/book-open", "0 completed stories. Confirmed chapters will be here to revisit.")
            section("Moment Insights")
            quietLine("PersonalIcons/lightbulb", "No insights yet. Observations need meaningful stories first.")
        }
    }

    private func populatedMoments(
        active: [PersonalExperiencePayload.MomentCard],
        emerging: [PersonalExperiencePayload.MomentCard],
        recent: [PersonalExperiencePayload.MomentCard]
    ) -> some View {
        let moments = experience?.moments
        return VStack(alignment: .leading, spacing: 32) {
            VStack(alignment: .leading, spacing: 8) {
                section("Active Moments").id("moment-stories")
                if let subtitle = moments?.activeSubtitle {
                    Text(subtitle).personalText(PersonalTokens.Typography.caption).foregroundStyle(PersonalTokens.Colors.textSubtle)
                }
            }
            ForEach(Array(active.enumerated()), id: \.element.id) { index, card in
                if index == 0 {
                    featuredMoment(card)
                } else {
                    storyCard(card, artwork: false)
                }
            }
            VStack(alignment: .leading, spacing: 8) {
                section("Emerging Moments")
                if let subtitle = moments?.emergingSubtitle {
                    Text(subtitle).personalText(PersonalTokens.Typography.caption).foregroundStyle(PersonalTokens.Colors.textSubtle)
                }
            }
            if emerging.isEmpty {
                quietLine("PersonalIcons/link", "No connections yet.")
            } else {
                ForEach(emerging) { card in storyCard(card, artwork: true) }
            }
            VStack(alignment: .leading, spacing: 8) {
                section("Moment Progress")
                if let subtitle = moments?.progressSubtitle {
                    Text(subtitle).personalText(PersonalTokens.Typography.caption).foregroundStyle(PersonalTokens.Colors.textSubtle)
                }
            }
            if let progress = moments?.progress {
                VStack(alignment: .leading, spacing: 12) {
                    Text(progress.title).personalText(PersonalTokens.Typography.section).foregroundStyle(PersonalTokens.Colors.textPrimary)
                    if let area = progress.areaLabel, let sub = progress.subcategoryLabel {
                        Text("\(area) › \(sub)").personalText(PersonalTokens.Typography.caption).foregroundStyle(PersonalTokens.Colors.textSecondary)
                    }
                    progressBars(progress.bars)
                    if let cumulative = progress.cumulativeLabel, !cumulative.isEmpty {
                        Text(cumulative).personalText(PersonalTokens.Typography.metadata).foregroundStyle(PersonalTokens.Colors.brandPrimary)
                    }
                    Text(progress.footnote?.isEmpty == false ? progress.footnote! : progress.message)
                        .personalText(PersonalTokens.Typography.caption).foregroundStyle(PersonalTokens.Colors.textSecondary)
                }
                .padding(20)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(PersonalTokens.Colors.surfaceContainer, in: RoundedRectangle(cornerRadius: 24))
            } else {
                quietLine("PersonalIcons/route", "No story activity yet. Progress appears with supporting expenses—not a completion score.")
            }
            VStack(alignment: .leading, spacing: 8) {
            section("Recent Moments")
                if let subtitle = moments?.recentSubtitle {
                    Text(subtitle).personalText(PersonalTokens.Typography.caption).foregroundStyle(PersonalTokens.Colors.textSubtle)
                }
            }
            if recent.isEmpty {
                quietLine("PersonalIcons/book-open", "0 completed stories. Confirmed chapters will be here to revisit.")
            } else {
                ForEach(recent) { card in storyCard(card, artwork: false) }
            }
            section("Moment Insights")
            VStack(alignment: .leading, spacing: 10) {
                if let title = moments?.insight.title, !title.isEmpty {
                    Text(title).personalText(PersonalTokens.Typography.section).foregroundStyle(PersonalTokens.Colors.brandPrimary)
                }
                Text(moments?.insight.text ?? "Insights appear when more than one story has enough evidence.")
                    .personalText(PersonalTokens.Typography.body).foregroundStyle(PersonalTokens.Colors.textSecondary)
            }
            .padding(18)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(PersonalTokens.Colors.surfaceContainer, in: RoundedRectangle(cornerRadius: 16))
            if let action = moments?.insight.actionLabel, !action.isEmpty {
                exploreRow(action) { focusStories = true }
            }
            if let footnote = moments?.insight.footnote, !footnote.isEmpty {
                Text(footnote).personalText(PersonalTokens.Typography.caption).foregroundStyle(PersonalTokens.Colors.textSecondary)
            }
        }
    }

    private func featuredMoment(_ card: PersonalExperiencePayload.MomentCard) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Image("PersonalArtwork/\(card.artworkKey ?? "art_pers_open_beginning")")
                .resizable().scaledToFill().frame(height: 240).clipped()
                .clipShape(RoundedRectangle(cornerRadius: 24))
            storyCopy(card)
            Text("\(card.areaLabel) › \(card.subcategoryLabel)")
                .personalText(PersonalTokens.Typography.caption).foregroundStyle(PersonalTokens.Colors.textSecondary)
            exploreRow("Explore \(card.activityCount) supporting expenses", action: onOpenLife)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(PersonalTokens.Colors.surfaceHighlight, in: RoundedRectangle(cornerRadius: 24))
    }

    private func storyCard(_ card: PersonalExperiencePayload.MomentCard, artwork: Bool) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            if artwork, let key = card.artworkKey {
                Image("PersonalArtwork/\(key)")
                    .resizable().scaledToFill().frame(height: 180).clipped()
                    .clipShape(RoundedRectangle(cornerRadius: 20))
            }
            storyCopy(card)
            Text("\(card.areaLabel) › \(card.subcategoryLabel)")
                .personalText(PersonalTokens.Typography.caption).foregroundStyle(PersonalTokens.Colors.textSecondary)
            exploreRow("Explore supporting expenses", action: onOpenLife)
        }
        .padding(20)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(PersonalTokens.Colors.surfaceContainer, in: RoundedRectangle(cornerRadius: 24))
        .overlay(RoundedRectangle(cornerRadius: 24).stroke(PersonalTokens.Colors.brandPrimary.opacity(0.13), lineWidth: 1))
    }

    private func storyCopy(_ card: PersonalExperiencePayload.MomentCard) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            if let status = card.statusLabel, !status.isEmpty {
                Text(status).personalText(PersonalTokens.Typography.metadata).foregroundStyle(PersonalTokens.Colors.brandPrimary)
            }
            Text(card.title).personalText(PersonalTokens.Typography.storyTitle).foregroundStyle(PersonalTokens.Colors.textPrimary)
            if let summary = card.summary, !summary.isEmpty {
                Text(summary).personalText(PersonalTokens.Typography.body).foregroundStyle(PersonalTokens.Colors.textSecondary)
            }
            Text("\(card.activityCount) expenses · \(inr(card.spend, "INR"))")
                .personalText(PersonalTokens.Typography.metadata).foregroundStyle(PersonalTokens.Colors.brandPrimary)
            if let range = card.rangeLabel, !range.isEmpty {
                Text(range).personalText(PersonalTokens.Typography.caption).foregroundStyle(PersonalTokens.Colors.textSecondary)
            }
        }
    }

    private func progressBars(_ bars: [PersonalExperiencePayload.ProgressBar]) -> some View {
        let maxValue = max(bars.map { Double($0.amount) ?? 0 }.max() ?? 1, 1)
        return HStack(alignment: .bottom, spacing: 16) {
            ForEach(Array(bars.enumerated()), id: \.offset) { _, bar in
                let ratio = max((Double(bar.amount) ?? 0) / maxValue, 0.08)
                VStack(spacing: 8) {
                    Text(inr(bar.amount, "INR")).personalText(PersonalTokens.Typography.metadata).foregroundStyle(PersonalTokens.Colors.brandPrimary)
                    RoundedRectangle(cornerRadius: 10)
                        .fill(PersonalTokens.Colors.brandPrimary)
                        .frame(width: 42, height: 72 * ratio)
                    Text(bar.label ?? formatPulseWhen(bar.effectiveAt))
                        .personalText(PersonalTokens.Typography.caption).foregroundStyle(PersonalTokens.Colors.textSecondary)
                }
                .frame(maxWidth: .infinity)
            }
        }
        .frame(height: 134, alignment: .bottom)
    }

    private var life: some View {
        let cold = experience?.cold ?? true
        let block = experience?.life
        let currency = experience?.currencyCode
        return VStack(alignment: .leading, spacing: cold ? PersonalTokens.Space.lg : PersonalTokens.Space.section) {
            lifeIntro(block, cold: cold)
            VStack(alignment: .leading, spacing: PersonalTokens.Space.compact) {
                lifeHeading("Your Life", subtitle: cold ? nil : block?.rangeLabel)
                Text(inr(cold ? "0" : block?.total, currency)).personalText(PersonalTokens.Typography.amountL).foregroundStyle(PersonalTokens.Colors.textPrimary)
                Text(cold ? "0 expenses · \(block?.monthLabel?.isEmpty == false ? block!.monthLabel! : "this month")" : lifeExpenseCount(block?.expenseCount ?? 0))
                    .personalText(PersonalTokens.Typography.body).foregroundStyle(PersonalTokens.Colors.textSecondary)
                if !cold {
                    Text(block?.coverageLabel?.isEmpty == false ? block!.coverageLabel! : "Fully classified")
                        .personalText(PersonalTokens.Typography.caption).foregroundStyle(PersonalTokens.Colors.signalPositive)
                }
                lifeLayer("NOW · Everyday living", PersonalLifeTaxonomy.areas(in: .now), block, cold: cold, currency: currency)
                lifeLayer("BUILD · Longer horizons", PersonalLifeTaxonomy.areas(in: .build), block, cold: cold, currency: currency)
                if !cold {
                    Text("Tap an area to explore activity and subcategories. Bars show its share of \(inr(block?.total, currency)).")
                        .personalText(PersonalTokens.Typography.caption).foregroundStyle(PersonalTokens.Colors.textSubtle)
                }
            }
            .id("life-areas")
            if cold {
                Button { flow = .sheet(ExpenseDraft()) } label: {
                    Text("+ Add your first expense")
                        .personalText(PersonalTokens.Typography.button)
                        .foregroundStyle(PersonalTokens.Colors.textOnAccent)
                        .frame(maxWidth: .infinity)
                        .frame(height: PersonalTokens.Size.buttonVisual)
                        .background(PersonalTokens.Colors.brandAccent, in: RoundedRectangle(cornerRadius: PersonalTokens.Radius.button))
                }
                .buttonStyle(.plain)
            }
            lifeBalance(block, cold: cold, currency: currency)
            lifeMoney(block, cold: cold, currency: currency)
            lifeTrends(block, cold: cold, currency: currency)
            lifeMood(block, cold: cold)
        }
    }

    private func lifeIntro(_ block: PersonalExperiencePayload.LifeBlock?, cold: Bool) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(block?.intro?.kicker.isEmpty == false ? block!.intro!.kicker : (cold ? "A FRESH START" : "YOUR LIFE"))
                .personalText(PersonalTokens.Typography.metadata).foregroundStyle(PersonalTokens.Colors.brandPrimary)
            HStack(alignment: .center) {
                Text(block?.intro?.title.isEmpty == false ? block!.intro!.title : (cold ? "All the parts of you." : "Your money, in life"))
                    .personalText(PersonalTokens.Typography.hero).foregroundStyle(PersonalTokens.Colors.textPrimary)
                Spacer()
                if !cold {
                    Button { flow = .sheet(ExpenseDraft()) } label: {
                        Text("+ Expense")
                            .personalText(PersonalTokens.Typography.chip)
                            .foregroundStyle(PersonalTokens.Colors.textOnAccent)
                            .padding(.horizontal, 12)
                            .padding(.vertical, 8)
                            .background(PersonalTokens.Colors.brandAccent, in: Capsule())
                    }
                    .buttonStyle(.plain)
                }
            }
            Text(block?.intro?.subtitle.isEmpty == false ? block!.intro!.subtitle : (cold ? "Eight areas of life. Explore any one, even before your first expense." : "Eight areas. One connected picture."))
                .personalText(PersonalTokens.Typography.body).foregroundStyle(PersonalTokens.Colors.textSecondary)
        }
    }

    private func lifeHeading(_ title: String, subtitle: String?) -> some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(title).personalText(PersonalTokens.Typography.section).foregroundStyle(PersonalTokens.Colors.textPrimary)
            if let subtitle, !subtitle.isEmpty {
                Text(subtitle).personalText(PersonalTokens.Typography.caption).foregroundStyle(PersonalTokens.Colors.textSubtle)
            }
        }
    }

    private func lifeLayer(_ label: String, _ areas: [PersonalLifeArea], _ block: PersonalExperiencePayload.LifeBlock?, cold: Bool, currency: String?) -> some View {
        VStack(alignment: .leading, spacing: PersonalTokens.Space.sm) {
            Text(label).personalText(PersonalTokens.Typography.metadata).foregroundStyle(PersonalTokens.Colors.brandPrimary)
            LazyVGrid(columns: [GridItem(.flexible(), spacing: 8), GridItem(.flexible())], spacing: 8) {
                ForEach(areas) { area in
                    let spend = block?.areas.first { $0.areaCode == area.code }
                    lifeTile(area, spend: cold ? "0" : (spend?.spend ?? "0"), count: cold ? 0 : (spend?.activityCount ?? 0), share: cold ? 0 : (spend?.share ?? 0), highlighted: !cold && block?.highlightedAreaCode == area.code, cold: cold, currency: currency)
                }
            }
        }
    }

    private func lifeTile(_ area: PersonalLifeArea, spend: String, count: Int, share: Double, highlighted: Bool, cold: Bool, currency: String?) -> some View {
        Button {
            var draft = ExpenseDraft()
            draft.areaCode = area.code
            flow = .subcategory(draft)
        } label: {
            VStack(alignment: .leading, spacing: 12) {
                HStack {
                    area.icon.foregroundStyle(area.tint).frame(width: 18, height: 18)
                    Spacer()
                    Image("PersonalIcons/arrow-up-right").renderingMode(.template)
                        .foregroundStyle(PersonalTokens.Colors.textSubtle)
                        .frame(width: 16, height: 16)
                }
                Text(area.label).personalText(PersonalTokens.Typography.body).foregroundStyle(PersonalTokens.Colors.textPrimary)
                if cold {
                    Text("₹0 · 0 expenses").personalText(PersonalTokens.Typography.caption).foregroundStyle(PersonalTokens.Colors.textSubtle)
            } else {
                    HStack {
                        Text(inr(spend, currency)).personalText(PersonalTokens.Typography.metadata).foregroundStyle(PersonalTokens.Colors.textPrimary)
                        Spacer()
                        Text(lifeExpenseCount(count)).personalText(PersonalTokens.Typography.caption).foregroundStyle(PersonalTokens.Colors.textSubtle)
                    }
                    GeometryReader { geo in
                        ZStack(alignment: .leading) {
                            Capsule().fill(PersonalTokens.Colors.surfaceHigh)
                            Capsule().fill(PersonalTokens.Colors.brandPrimary).frame(width: geo.size.width * CGFloat(min(max(share, 0), 1)))
                        }
                    }
                    .frame(height: 4)
                }
            }
            .padding(12)
            .frame(maxWidth: .infinity, minHeight: cold ? 134 : 156, alignment: .topLeading)
            .background(highlighted ? PersonalTokens.Colors.surfaceHighlight : PersonalTokens.Colors.surfaceContainer, in: RoundedRectangle(cornerRadius: PersonalTokens.Radius.tileSelected))
            .overlay(RoundedRectangle(cornerRadius: PersonalTokens.Radius.tileSelected).stroke(highlighted ? PersonalTokens.Colors.borderSelected : PersonalTokens.Colors.borderHairline, lineWidth: 1))
        }
        .buttonStyle(.plain)
    }

    private func lifeBalance(_ block: PersonalExperiencePayload.LifeBlock?, cold: Bool, currency: String?) -> some View {
        VStack(alignment: .leading, spacing: PersonalTokens.Space.sm) {
            lifeHeading("NOW ↔ BUILD", subtitle: nil)
            card {
                if cold {
                    HStack(alignment: .top) {
                        lifeBalanceColumn("NOW", "Everyday living", "0", currency)
                        lifeBalanceColumn("BUILD", "Longer horizons", "0", currency)
                    }
                    Text("Shares unavailable at ₹0. Derived from Life Areas, never a separate choice.")
                        .personalText(PersonalTokens.Typography.caption).foregroundStyle(PersonalTokens.Colors.textSubtle)
                } else {
                    HStack(alignment: .center, spacing: 16) {
                        lifeRing(block?.nowBuild.nowShare ?? 0, block?.nowBuild.buildShare ?? 0)
                        VStack(alignment: .leading, spacing: 8) {
                            lifeShareLine("NOW", block?.nowBuild.nowShare ?? 0, block?.nowBuild.now, currency, PersonalTokens.Colors.brandPrimary)
                            lifeShareLine("BUILD", block?.nowBuild.buildShare ?? 0, block?.nowBuild.build, currency, PersonalTokens.Colors.brandTertiary)
                        }
                    }
                    Text("Derived from Life Areas. Allocation is context, not a score. Percentages rounded.")
                        .personalText(PersonalTokens.Typography.caption).foregroundStyle(PersonalTokens.Colors.textSubtle)
                }
            }
        }
    }

    private func lifeBalanceColumn(_ kicker: String, _ title: String, _ amount: String, _ currency: String?) -> some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(kicker).personalText(PersonalTokens.Typography.metadata).foregroundStyle(PersonalTokens.Colors.brandPrimary)
            Text(title).personalText(PersonalTokens.Typography.caption).foregroundStyle(PersonalTokens.Colors.textSecondary)
            Text(inr(amount, currency)).personalText(PersonalTokens.Typography.section).foregroundStyle(PersonalTokens.Colors.textPrimary)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private func lifeRing(_ nowShare: Double, _ buildShare: Double) -> some View {
        ZStack {
            Circle()
                .trim(from: 0, to: CGFloat(min(max(nowShare, 0), 1)))
                .stroke(PersonalTokens.Colors.brandPrimary, style: StrokeStyle(lineWidth: 10, lineCap: .butt))
                .rotationEffect(.degrees(-90))
            Circle()
                .trim(from: CGFloat(min(max(nowShare, 0), 1)), to: CGFloat(min(max(nowShare + buildShare, 0), 1)))
                .stroke(PersonalTokens.Colors.brandTertiary, style: StrokeStyle(lineWidth: 10, lineCap: .butt))
                .rotationEffect(.degrees(-90))
            VStack(spacing: 0) {
                Text("100%").personalText(PersonalTokens.Typography.section).foregroundStyle(PersonalTokens.Colors.textPrimary)
                Text("of spend").personalText(PersonalTokens.Typography.caption).foregroundStyle(PersonalTokens.Colors.textSubtle)
            }
        }
        .frame(width: 112, height: 112)
        .padding(6)
    }

    private func lifeShareLine(_ label: String, _ share: Double, _ amount: String?, _ currency: String?, _ color: Color) -> some View {
        VStack(alignment: .leading, spacing: 2) {
            Text("\(label) · \(lifeSharePercent(share))").personalText(PersonalTokens.Typography.metadata).foregroundStyle(color)
            Text(inr(amount, currency)).personalText(PersonalTokens.Typography.body).foregroundStyle(PersonalTokens.Colors.textPrimary)
        }
    }

    private func lifeMoney(_ block: PersonalExperiencePayload.LifeBlock?, cold: Bool, currency: String?) -> some View {
        VStack(alignment: .leading, spacing: PersonalTokens.Space.sm) {
            lifeHeading("Where Your Money Goes", subtitle: nil)
            if cold {
                quietLine("PersonalIcons/wallet", "No classified spend yet.", "Subcategories appear with your first expense.")
            } else {
                card {
                    ForEach(block?.moneyGoes ?? []) { slice in
                        VStack(alignment: .leading, spacing: 6) {
                            HStack(alignment: .top) {
                                VStack(alignment: .leading, spacing: 2) {
                                    Text(slice.label).personalText(PersonalTokens.Typography.body).foregroundStyle(PersonalTokens.Colors.textPrimary)
                                    if let area = slice.areaLabel, !area.isEmpty {
                                        Text(area).personalText(PersonalTokens.Typography.caption).foregroundStyle(PersonalTokens.Colors.textSubtle)
                                    }
                                }
                                Spacer()
                                VStack(alignment: .trailing, spacing: 2) {
                                    Text(inr(slice.spend, currency)).personalText(PersonalTokens.Typography.metadata).foregroundStyle(PersonalTokens.Colors.textPrimary)
                                    Text(lifeSharePercent(slice.share)).personalText(PersonalTokens.Typography.caption).foregroundStyle(PersonalTokens.Colors.textSubtle)
                                }
                            }
                            GeometryReader { geo in
                                ZStack(alignment: .leading) {
                                    Capsule().fill(PersonalTokens.Colors.surfaceHigh)
                                    Capsule().fill(PersonalTokens.Colors.brandAccent).frame(width: geo.size.width * CGFloat(min(max(slice.share, 0), 1)))
                                }
                            }
                            .frame(height: 6)
                        }
                    }
                    if (Double(block?.otherSpend ?? "0") ?? 0) > 0 {
                        VStack(alignment: .leading, spacing: 2) {
                            Text("\(block?.otherCount ?? 0) other subcategories").personalText(PersonalTokens.Typography.body).foregroundStyle(PersonalTokens.Colors.textPrimary)
                            Text("\(inr(block?.otherSpend, currency)) · \(lifeSharePercent(block?.otherShare ?? 0))")
                                .personalText(PersonalTokens.Typography.caption).foregroundStyle(PersonalTokens.Colors.textSubtle)
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(12)
                        .background(PersonalTokens.Colors.fillIconWellQuiet, in: RoundedRectangle(cornerRadius: PersonalTokens.Radius.tile))
                    }
                    Text("Top five · \(inr(block?.topFiveSpend, currency)) · \(lifeSharePercent(block?.topFiveShare ?? 0)) of this period.")
                        .personalText(PersonalTokens.Typography.caption).foregroundStyle(PersonalTokens.Colors.textSubtle)
                    Button { focusAreas = true } label: {
                        Text("See all subcategories").personalText(PersonalTokens.Typography.body).foregroundStyle(PersonalTokens.Colors.brandPrimary)
                    }
                    .buttonStyle(.plain)
                }
            }
        }
    }

    private func lifeTrends(_ block: PersonalExperiencePayload.LifeBlock?, cold: Bool, currency: String?) -> some View {
        VStack(alignment: .leading, spacing: PersonalTokens.Space.sm) {
            lifeHeading("Life Trends", subtitle: nil)
            if cold || block?.trends.isEmpty != false {
                quietLine("PersonalIcons/calendar-days", block?.trendsMessage ?? "No comparable periods yet. There’s no trend to show.")
            } else {
                    card {
                    ForEach(Array((block?.trends ?? []).prefix(2).enumerated()), id: \.offset) { _, trend in
                        VStack(alignment: .leading, spacing: 6) {
                            Text(trend.label).personalText(PersonalTokens.Typography.body).foregroundStyle(PersonalTokens.Colors.textPrimary)
                            if let area = trend.areaLabel, !area.isEmpty {
                                Text(area).personalText(PersonalTokens.Typography.caption).foregroundStyle(PersonalTokens.Colors.textSubtle)
                            }
                            lifeTrendBar(trend.previousShort ?? "—", trend.previous ?? "0", trend.current ?? "0", currency, PersonalTokens.Colors.surfaceHigh)
                            lifeTrendBar(trend.currentShort ?? "—", trend.current ?? "0", trend.previous ?? "0", currency, PersonalTokens.Colors.brandAccent)
                            Text("\(lifeExpenseCount(trend.previousCount ?? 0)) · \(lifeExpenseCount(trend.currentCount ?? 0))")
                                .personalText(PersonalTokens.Typography.caption).foregroundStyle(PersonalTokens.Colors.textSecondary)
                        }
                    }
                    if let footnote = block?.trendsFootnote, !footnote.isEmpty {
                        Text(footnote).personalText(PersonalTokens.Typography.caption).foregroundStyle(PersonalTokens.Colors.textSubtle)
                    }
                    Button(action: onOpenPulse) {
                        Text("Compare period activity").personalText(PersonalTokens.Typography.body).foregroundStyle(PersonalTokens.Colors.brandPrimary)
                    }
                    .buttonStyle(.plain)
                }
            }
        }
    }

    private func lifeTrendBar(_ month: String, _ amount: String, _ other: String, _ currency: String?, _ color: Color) -> some View {
        let value = Double(amount) ?? 0
        let maxValue = max(value, Double(other) ?? 0, 1)
        return HStack(spacing: 8) {
            Text(month).personalText(PersonalTokens.Typography.caption).foregroundStyle(PersonalTokens.Colors.textSubtle).frame(width: 36, alignment: .leading)
            GeometryReader { geo in
                ZStack(alignment: .leading) {
                    Capsule().fill(PersonalTokens.Colors.fillIconWell)
                    Capsule().fill(color).frame(width: geo.size.width * CGFloat(min(max(value / maxValue, 0), 1)))
                }
            }
            .frame(height: 8)
            Text(inr(amount, currency)).personalText(PersonalTokens.Typography.caption).foregroundStyle(PersonalTokens.Colors.textSecondary)
        }
    }

    private func lifeMood(_ block: PersonalExperiencePayload.LifeBlock?, cold: Bool) -> some View {
        VStack(alignment: .leading, spacing: PersonalTokens.Space.sm) {
            lifeHeading("Life & Mood", subtitle: nil)
            card {
                HStack(alignment: .center) {
                    Text(block?.moodSummary?.isEmpty == false ? block!.moodSummary! : (cold ? "0 responses / 0 expenses" : ""))
                        .personalText(PersonalTokens.Typography.body).foregroundStyle(PersonalTokens.Colors.textPrimary)
                    Spacer()
                    Text(block?.moodCoverageLabel?.isEmpty == false ? block!.moodCoverageLabel! : "Coverage unavailable")
                        .personalText(PersonalTokens.Typography.caption).foregroundStyle(PersonalTokens.Colors.brandPrimary)
                }
                if !cold {
                    ForEach(Array((block?.moodByArea ?? []).enumerated()), id: \.offset) { _, area in
                        VStack(alignment: .leading, spacing: 6) {
                            Text(area.areaLabel).personalText(PersonalTokens.Typography.metadata).foregroundStyle(PersonalTokens.Colors.textSecondary)
                            FlowLayout(spacing: 6) {
                                ForEach(Array((area.responses ?? []).enumerated()), id: \.offset) { _, mood in
                                    let recorded = mood != "No mood"
                                    Text(mood)
                                        .personalText(PersonalTokens.Typography.chip)
                                        .foregroundStyle(recorded ? PersonalTokens.Colors.brandPrimary : PersonalTokens.Colors.textSecondary)
                                        .padding(.horizontal, 10)
                                        .padding(.vertical, 4)
                                        .background(recorded ? PersonalTokens.Colors.surfaceHighlight : PersonalTokens.Colors.surfaceHigh, in: RoundedRectangle(cornerRadius: PersonalTokens.Radius.chip))
                                }
                            }
                        }
                    }
                }
                Text(block?.moodFootnote?.isEmpty == false ? block!.moodFootnote! : "Optional, explicitly recorded feelings. Area-level patterns need enough responses.")
                    .personalText(PersonalTokens.Typography.caption).foregroundStyle(PersonalTokens.Colors.textSubtle)
                if !cold {
                    Button {
                        pendingMoodScroll = true
                        onOpenPulse()
                    } label: {
                        Text("Explore recorded moods").personalText(PersonalTokens.Typography.body).foregroundStyle(PersonalTokens.Colors.brandPrimary)
                    }
                    .buttonStyle(.plain)
                }
            }
        }
    }

    private var memory: some View {
        let block = experience?.memory
        let hasHistory = !(block?.timeline.isEmpty ?? true) || !(block?.completed.isEmpty ?? true) || block?.monthly != nil || block?.lookBack != nil
        let currency = experience?.currencyCode
        return VStack(alignment: .leading, spacing: hasHistory ? PersonalTokens.Space.section : PersonalTokens.Space.lg) {
            memoryIntro(block, hasHistory: hasHistory)
            VStack(alignment: .leading, spacing: PersonalTokens.Space.compact) {
                lifeHeading("Life Timeline", subtitle: hasHistory ? "Meaningful events, not a transaction feed" : nil)
                if !hasHistory {
                    memoryOpening(block)
                    memoryRail(block?.timelineOpenKicker ?? "OPEN", "Your timeline begins when a meaningful story takes shape. No events yet.")
                } else {
                    HStack(spacing: 8) {
                        memoryChip("All chapters", selected: !memoryCurrentOnly) { memoryCurrentOnly = false }
                        memoryChip(block?.currentFilterLabel ?? "This month · current", selected: memoryCurrentOnly) { memoryCurrentOnly = true }
                    }
                    let events = (block?.timeline ?? []).filter { !memoryCurrentOnly || $0.period == "current" }
                    if events.isEmpty {
                        Text("No events in this month yet.").personalText(PersonalTokens.Typography.body).foregroundStyle(PersonalTokens.Colors.textSecondary)
                    } else {
                        ForEach(events) { item in
                            memoryRail(item.kicker ?? formatWhen(item.effectiveAt), item.detail)
                        }
                    }
                    memoryLabel("Explore the timeline", action: onOpenMoments)
                }
            }
            VStack(alignment: .leading, spacing: PersonalTokens.Space.compact) {
                lifeHeading("Completed Moments", subtitle: hasHistory ? "Finished stories with confirmed context" : nil)
                if block?.completed.isEmpty != false {
                    quietLine("PersonalIcons/book-check", "0 completed stories. Confirmed chapters will stay here.")
            } else {
                    ForEach(block?.completed ?? []) { card in memoryCompleted(card, currency) }
                }
            }
            VStack(alignment: .leading, spacing: PersonalTokens.Space.compact) {
                lifeHeading("Monthly Memories", subtitle: hasHistory ? "Closed months, ready to revisit" : nil)
                if let monthly = block?.monthly {
                    memoryClosedMonth(monthly, block?.openMonthNote, currency)
                } else {
                    memoryOpenMonth(block)
                }
            }
            VStack(alignment: .leading, spacing: PersonalTokens.Space.compact) {
                lifeHeading("Milestones", subtitle: hasHistory ? "Significant outcomes, confirmed by you" : nil)
                if block?.milestones.isEmpty != false {
                    quietLine("PersonalIcons/flag", "No confirmed outcomes yet. Significant completed chapters belong here—not routine payments.")
                } else {
                    ForEach(block?.milestones ?? []) { item in memoryMilestone(item) }
                }
            }
            VStack(alignment: .leading, spacing: PersonalTokens.Space.compact) {
                lifeHeading("Look Back", subtitle: hasHistory ? "Something worth remembering" : nil)
                if let look = block?.lookBack {
                    memoryLook(look)
                } else {
                    quietLine("PersonalIcons/history", block?.lookBackMessage ?? "No earlier chapter to revisit. Memories will grow from meaningful history.")
                }
            }
        }
    }

    private func memoryIntro(_ block: PersonalExperiencePayload.MemoryBlock?, hasHistory: Bool) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(block?.intro?.kicker.isEmpty == false ? block!.intro!.kicker : (hasHistory ? "HISTORY" : "A FRESH START"))
                .personalText(PersonalTokens.Typography.metadata).foregroundStyle(PersonalTokens.Colors.brandPrimary)
            HStack {
                Text(block?.intro?.title.isEmpty == false ? block!.intro!.title : (hasHistory ? "What stays with you" : "A story worth keeping."))
                    .personalText(PersonalTokens.Typography.hero).foregroundStyle(PersonalTokens.Colors.textPrimary)
                Spacer()
                if hasHistory {
                    Button { flow = .sheet(ExpenseDraft()) } label: {
                        Text("+ Expense")
                            .personalText(PersonalTokens.Typography.chip)
                            .foregroundStyle(PersonalTokens.Colors.textOnAccent)
                            .padding(.horizontal, 12)
                            .padding(.vertical, 8)
                            .background(PersonalTokens.Colors.brandAccent, in: Capsule())
                    }
                    .buttonStyle(.plain)
                }
            }
            Text(block?.intro?.subtitle.isEmpty == false ? block!.intro!.subtitle : (hasHistory ? "The chapters behind your expenses." : "There's room here for the chapters that will matter."))
                .personalText(PersonalTokens.Typography.body).foregroundStyle(PersonalTokens.Colors.textSecondary)
        }
    }

    private func memoryOpening(_ block: PersonalExperiencePayload.MemoryBlock?) -> some View {
        let monthLine = block?.intro?.kicker.components(separatedBy: " ·").first ?? block?.openMonthShort ?? ""
        return VStack(alignment: .leading, spacing: 14) {
            HStack(alignment: .center, spacing: 16) {
                Image("PersonalArtwork/art_pers_open_beginning")
                    .resizable().scaledToFill()
                    .frame(width: 112, height: 112)
                    .clipShape(Circle())
                VStack(alignment: .leading, spacing: 4) {
                    Text(monthLine).personalText(PersonalTokens.Typography.metadata).foregroundStyle(PersonalTokens.Colors.brandPrimary)
                    Text("The first page is yours.").personalText(PersonalTokens.Typography.hero).foregroundStyle(PersonalTokens.Colors.textPrimary)
                }
            }
            Text("No meaningful history yet. Ordinary expenses stay in activity; meaningful events build this timeline.")
                .personalText(PersonalTokens.Typography.body).foregroundStyle(PersonalTokens.Colors.textSecondary)
            Button { flow = .sheet(ExpenseDraft()) } label: {
                Text("+ Add your first expense")
                    .personalText(PersonalTokens.Typography.button)
                    .foregroundStyle(PersonalTokens.Colors.textOnAccent)
                    .frame(maxWidth: .infinity)
                    .frame(height: PersonalTokens.Size.buttonVisual)
                    .background(PersonalTokens.Colors.brandAccent, in: RoundedRectangle(cornerRadius: PersonalTokens.Radius.button))
            }
            .buttonStyle(.plain)
        }
        .padding(20)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(PersonalTokens.Colors.surfaceHighlight, in: RoundedRectangle(cornerRadius: PersonalTokens.Radius.card))
        .overlay(RoundedRectangle(cornerRadius: PersonalTokens.Radius.card).stroke(PersonalTokens.Colors.borderDivider, lineWidth: 1))
    }

    private func memoryRail(_ kicker: String, _ body: String) -> some View {
        HStack(alignment: .top, spacing: 14) {
            VStack(spacing: 4) {
                Circle().fill(PersonalTokens.Colors.brandPrimary).frame(width: 8, height: 8).padding(.top, 4)
                Rectangle().fill(PersonalTokens.Colors.borderHairline).frame(width: 1, height: 36)
            }
            VStack(alignment: .leading, spacing: 2) {
                Text(kicker).personalText(PersonalTokens.Typography.metadata).foregroundStyle(PersonalTokens.Colors.brandPrimary)
                Text(body).personalText(PersonalTokens.Typography.body).foregroundStyle(PersonalTokens.Colors.textSecondary)
            }
        }
    }

    private func memoryLabel(_ label: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            HStack {
                Text(label).personalText(PersonalTokens.Typography.metadata).foregroundStyle(PersonalTokens.Colors.brandPrimary)
                Spacer()
                Image("PersonalIcons/arrow-up-right").renderingMode(.template)
                    .foregroundStyle(PersonalTokens.Colors.brandPrimary)
                    .frame(width: 16, height: 16)
            }
        }
        .buttonStyle(.plain)
    }

    private func memoryChip(_ label: String, selected: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(label)
                .personalText(PersonalTokens.Typography.metadata)
                .foregroundStyle(selected ? PersonalTokens.Colors.textOnAccent : PersonalTokens.Colors.textSecondary)
                .padding(.horizontal, 14)
                .padding(.vertical, 8)
                .background(selected ? PersonalTokens.Colors.brandAccent : PersonalTokens.Colors.surfaceContainer, in: Capsule())
                .overlay(Capsule().stroke(selected ? PersonalTokens.Colors.brandPrimary : PersonalTokens.Colors.borderHairline, lineWidth: 1))
        }
        .buttonStyle(.plain)
    }

    private func memoryCompleted(_ card: PersonalExperiencePayload.MomentCard, _ currency: String?) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Image("PersonalArtwork/\(card.artworkKey ?? "art_pers_open_beginning")")
                .resizable().scaledToFill().frame(maxWidth: .infinity).frame(height: 200).clipped()
            VStack(alignment: .leading, spacing: 8) {
            Text("Illustrated keepsake · not a personal photo").personalText(PersonalTokens.Typography.caption).foregroundStyle(PersonalTokens.Colors.textSubtle)
            if let status = card.statusLabel, !status.isEmpty {
                Text(status).personalText(PersonalTokens.Typography.metadata).foregroundStyle(PersonalTokens.Colors.brandPrimary)
            }
            Text(card.title).personalText(PersonalTokens.Typography.hero).font(.system(size: 26, weight: .bold)).foregroundStyle(PersonalTokens.Colors.textPrimary)
            Text("\(card.areaLabel) › \(card.subcategoryLabel)").personalText(PersonalTokens.Typography.caption).foregroundStyle(PersonalTokens.Colors.textSecondary)
            Text("\(lifeExpenseCount(card.activityCount)) · \(inr(card.spend, currency))").personalText(PersonalTokens.Typography.metadata).foregroundStyle(PersonalTokens.Colors.brandPrimary)
            if let range = card.rangeLabel, !range.isEmpty {
                Text(range).personalText(PersonalTokens.Typography.caption).foregroundStyle(PersonalTokens.Colors.textSecondary)
            }
            memoryLabel("Open supporting story", action: onOpenMoments)
            }
            .padding(20)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(PersonalTokens.Colors.surfaceHighlight, in: RoundedRectangle(cornerRadius: PersonalTokens.Radius.card))
        .overlay(RoundedRectangle(cornerRadius: PersonalTokens.Radius.card).stroke(PersonalTokens.Colors.borderHairline, lineWidth: 1))
        .clipShape(RoundedRectangle(cornerRadius: PersonalTokens.Radius.card))
    }

    private func memoryOpenMonth(_ block: PersonalExperiencePayload.MemoryBlock?) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text(block?.openMonthShort?.isEmpty == false ? block!.openMonthShort! : "—")
                    .personalText(PersonalTokens.Typography.amountL).foregroundStyle(PersonalTokens.Colors.brandPrimary)
                Spacer()
                Text(block?.openMonthChip?.isEmpty == false ? block!.openMonthChip! : "still open")
                    .personalText(PersonalTokens.Typography.metadata).foregroundStyle(PersonalTokens.Colors.textSecondary)
                    .padding(.horizontal, 14)
                    .padding(.vertical, 8)
                    .background(PersonalTokens.Colors.surfaceContainer, in: Capsule())
                    .overlay(Capsule().stroke(PersonalTokens.Colors.borderHairline, lineWidth: 1))
            }
            Text("Let the month unfold.").personalText(PersonalTokens.Typography.body).foregroundStyle(PersonalTokens.Colors.textPrimary)
            Text(block?.openRecap?.isEmpty == false ? block!.openRecap! : "No closed-month history yet.")
                .personalText(PersonalTokens.Typography.caption).foregroundStyle(PersonalTokens.Colors.textSecondary)
        }
        .padding(20)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(PersonalTokens.Colors.surfaceHighlight, in: RoundedRectangle(cornerRadius: PersonalTokens.Radius.card))
    }

    private func memoryClosedMonth(_ monthly: PersonalExperiencePayload.Monthly, _ note: String?, _ currency: String?) -> some View {
        VStack(alignment: .leading, spacing: PersonalTokens.Space.compact) {
            VStack(alignment: .leading, spacing: 12) {
                HStack(alignment: .bottom) {
                    VStack(alignment: .leading, spacing: 2) {
                        Text(monthly.shortLabel?.isEmpty == false ? monthly.shortLabel! : monthly.label)
                            .personalText(PersonalTokens.Typography.amountL).foregroundStyle(PersonalTokens.Colors.brandPrimary)
                        Text(monthly.closedLabel ?? "").personalText(PersonalTokens.Typography.metadata).foregroundStyle(PersonalTokens.Colors.brandPrimary)
                    }
                    Spacer()
                    Image("PersonalIcons/book-open").renderingMode(.template)
                        .foregroundStyle(PersonalTokens.Colors.brandPrimary)
                        .frame(width: 38, height: 38)
                }
                Text(monthly.title?.isEmpty == false ? monthly.title! : monthly.label)
                    .font(.system(size: 26, weight: .bold)).foregroundStyle(PersonalTokens.Colors.textPrimary)
                VStack(alignment: .leading, spacing: 4) {
                    Text(inr(monthly.spend, currency)).personalText(PersonalTokens.Typography.metric).foregroundStyle(PersonalTokens.Colors.textPrimary)
                    Text("\(lifeExpenseCount(monthly.expenseCount)) · \(monthly.periodCaption ?? "")")
                        .personalText(PersonalTokens.Typography.caption).foregroundStyle(PersonalTokens.Colors.textSecondary)
                }
                .padding(14)
                .background(PersonalTokens.Colors.fillIconWell, in: RoundedRectangle(cornerRadius: PersonalTokens.Radius.tile))
                HStack {
                    VStack(alignment: .leading, spacing: 4) {
                        Text("\(monthly.completedCount ?? 0)").personalText(PersonalTokens.Typography.storyTitle).foregroundStyle(PersonalTokens.Colors.textPrimary)
                        Text("completed Moments").personalText(PersonalTokens.Typography.caption).foregroundStyle(PersonalTokens.Colors.textSecondary)
                    }
                    Spacer()
                    VStack(alignment: .leading, spacing: 4) {
                        Text("\(monthly.moodCount ?? 0)").personalText(PersonalTokens.Typography.storyTitle).foregroundStyle(PersonalTokens.Colors.textPrimary)
                        Text("moods · \(Int((monthly.moodCoverage ?? 0) * 100))% coverage")
                            .personalText(PersonalTokens.Typography.caption).foregroundStyle(PersonalTokens.Colors.textSecondary)
                    }
                    Spacer()
                }
                Text(monthly.summary).personalText(PersonalTokens.Typography.caption).foregroundStyle(PersonalTokens.Colors.textSecondary)
                memoryLabel(monthly.openLabel?.isEmpty == false ? monthly.openLabel! : "Open memory", action: onOpenMoments)
            }
            .padding(20)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(PersonalTokens.Colors.surfaceHighlight, in: RoundedRectangle(cornerRadius: PersonalTokens.Radius.card))
            .overlay(RoundedRectangle(cornerRadius: PersonalTokens.Radius.card).stroke(PersonalTokens.Colors.borderHairline, lineWidth: 1))
            if let note, !note.isEmpty {
                HStack(spacing: 10) {
                    Image("PersonalIcons/clock-3").renderingMode(.template)
                        .foregroundStyle(PersonalTokens.Colors.textSecondary)
                        .frame(width: 20, height: 20)
                    Text(note).personalText(PersonalTokens.Typography.caption).foregroundStyle(PersonalTokens.Colors.textSecondary)
                }
            }
        }
    }

    private func memoryMilestone(_ item: PersonalExperiencePayload.Milestone) -> some View {
        HStack(alignment: .top, spacing: 16) {
            Image("PersonalIcons/award").renderingMode(.template)
                .foregroundStyle(PersonalTokens.Colors.brandPrimary)
                .frame(width: 28, height: 28)
                .frame(width: 52, height: 52)
                .background(PersonalTokens.Colors.fillIconWell, in: RoundedRectangle(cornerRadius: 18))
            VStack(alignment: .leading, spacing: 6) {
                Text(item.title).personalText(PersonalTokens.Typography.section).foregroundStyle(PersonalTokens.Colors.textPrimary)
                if let confirmed = item.confirmedLabel, !confirmed.isEmpty {
                    Text(confirmed).personalText(PersonalTokens.Typography.metadata).foregroundStyle(PersonalTokens.Colors.brandPrimary)
                }
                if let area = item.areaLabel, !area.isEmpty {
                    Text("\(area) › \(item.subcategoryLabel ?? "")").personalText(PersonalTokens.Typography.caption).foregroundStyle(PersonalTokens.Colors.textSecondary)
                }
                Text(item.detail).personalText(PersonalTokens.Typography.caption).foregroundStyle(PersonalTokens.Colors.textSecondary)
                memoryLabel("View the completed Moment", action: onOpenMoments)
            }
        }
        .padding(.vertical, 16)
        .overlay(alignment: .top) { Rectangle().fill(PersonalTokens.Colors.borderHairline).frame(height: 1) }
        .overlay(alignment: .bottom) { Rectangle().fill(PersonalTokens.Colors.borderHairline).frame(height: 1) }
    }

    private func memoryLook(_ look: PersonalExperiencePayload.LookBack) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text(look.kicker ?? "").personalText(PersonalTokens.Typography.metadata).foregroundStyle(PersonalTokens.Colors.brandPrimary)
                Spacer()
                Image("PersonalIcons/history").renderingMode(.template)
                    .foregroundStyle(PersonalTokens.Colors.brandPrimary)
                    .frame(width: 20, height: 20)
            }
            Text(look.title).personalText(PersonalTokens.Typography.hero).foregroundStyle(PersonalTokens.Colors.textPrimary)
            Text(look.detail).personalText(PersonalTokens.Typography.body).foregroundStyle(PersonalTokens.Colors.textSecondary)
            memoryLabel("Return to that chapter", action: onOpenMoments)
        }
        .padding(20)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(PersonalTokens.Colors.surfaceContainer, in: RoundedRectangle(cornerRadius: PersonalTokens.Radius.card))
        .overlay(RoundedRectangle(cornerRadius: PersonalTokens.Radius.card).stroke(PersonalTokens.Colors.borderHairline, lineWidth: 1))
    }

    private func momentList(_ cards: [PersonalExperiencePayload.MomentCard], empty: String) -> some View {
        Group {
            if cards.isEmpty {
                card { Text(empty).foregroundStyle(PersonalTokens.Colors.textSecondary) }
            } else {
                ForEach(cards) { card in momentCard(card) }
            }
        }
    }

    private func momentCard(_ card: PersonalExperiencePayload.MomentCard) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            if let key = card.artworkKey {
                Image("PersonalArtwork/\(key)").resizable().scaledToFill().frame(height: 140).clipped().clipShape(RoundedRectangle(cornerRadius: 12))
            }
            Text(card.title).font(.title3).foregroundStyle(PersonalTokens.Colors.textPrimary)
            Text("\(card.areaLabel) · \(card.subcategoryLabel)").foregroundStyle(PersonalTokens.Colors.textSecondary)
            Text("\(card.activityCount) expenses · \(card.spend)").font(.caption).foregroundStyle(PersonalTokens.Colors.textSubtle)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(16)
        .background(PersonalTokens.Colors.surfaceContainer, in: RoundedRectangle(cornerRadius: 16))
    }

    private func intro(kicker: String, title: String, subtitle: String, action: (() -> Void)?) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(kicker).personalText(PersonalTokens.Typography.storyKicker).foregroundStyle(PersonalTokens.Colors.textSubtle)
            HStack {
                Text(title).personalText(PersonalTokens.Typography.hero).foregroundStyle(PersonalTokens.Colors.textPrimary)
                Spacer()
                if let action {
                    Button("+ Expense", action: action)
                        .buttonStyle(.borderedProminent)
                        .tint(PersonalTokens.Colors.brandAccent)
                }
            }
            Text(subtitle).personalText(PersonalTokens.Typography.body).foregroundStyle(PersonalTokens.Colors.textSecondary)
        }
    }

    private func section(_ title: String) -> some View {
        Text(title).personalText(PersonalTokens.Typography.section).foregroundStyle(PersonalTokens.Colors.textPrimary)
    }

    private func card<Content: View>(@ViewBuilder content: () -> Content) -> some View {
        VStack(alignment: .leading, spacing: 6, content: content)
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(16)
            .background(PersonalTokens.Colors.surfaceContainer, in: RoundedRectangle(cornerRadius: 16))
    }

    private func reload() async {
        do {
            experience = try await APIClient.shared.getPersonalExperience()
            errorText = nil
        } catch {
            errorText = error.localizedDescription
        }
        loading = false
    }

    private func save(_ draft: ExpenseDraft) async {
        guard let area = draft.areaCode, let sub = draft.subcategoryCode else { return }
        busy = true
        defer { busy = false }
        do {
            let confirmation: PersonalExpenseConfirmationPayload
            if let editingId = draft.editingId {
                confirmation = try await APIClient.shared.correctPersonalLifeExpense(
                    expenseId: editingId,
                    body: PersonalLifeExpenseCorrectionRequest(
                        lifeAreaCode: area,
                        lifeSubcategoryCode: sub,
                        moodCode: draft.moodCode,
                        merchantName: draft.merchant.isEmpty ? nil : draft.merchant,
                        photo: expensePhoto(draft.photoPreview)
                    )
                )
            } else {
                confirmation = try await APIClient.shared.createPersonalLifeExpense(
                    PersonalLifeExpenseRequest(
                        amount: normalizedAmount(draft.amount),
                        currencyCode: "INR",
                        merchantName: draft.merchant.isEmpty ? nil : draft.merchant,
                        effectiveAt: draft.effectiveAt,
                        note: draft.note.isEmpty ? nil : draft.note,
                        lifeAreaCode: area,
                        lifeSubcategoryCode: sub,
                        moodCode: draft.moodCode,
                        classificationSource: "manual",
                        origin: "QUICK_ADD",
                        paymentSource: draft.paymentSource.isEmpty ? nil : draft.paymentSource,
                        economicKind: "EXPENSE",
                        photo: expensePhoto(draft.photoPreview)
                    )
                )
            }
            await reload()
            flow = .saved(confirmation)
            errorText = nil
        } catch {
            errorText = error.localizedDescription
        }
    }

    private func refund(_ expenseId: String) async {
        do {
            _ = try await APIClient.shared.refundPersonalLifeExpense(expenseId: expenseId)
            await reload()
            flow = .browse
        } catch {
            errorText = error.localizedDescription
        }
    }

    private func voidExpense(_ expenseId: String) async {
        do {
            _ = try await APIClient.shared.voidPersonalLifeExpense(expenseId: expenseId)
            await reload()
            flow = .browse
        } catch {
            errorText = error.localizedDescription
        }
    }
}

private func normalizedAmount(_ raw: String) -> String {
    var value = raw
    if value.hasSuffix(".") { value.removeLast() }
    if let dot = value.firstIndex(of: ".") {
        let decimals = value.distance(from: value.index(after: dot), to: value.endIndex)
        if decimals > 4 {
            let end = value.index(dot, offsetBy: 5)
            value = String(value[..<end])
        }
    }
    return value.isEmpty ? "0" : value
}

private func monthName(_ iso: String) -> String {
    let parser = ISO8601DateFormatter()
    parser.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
    let date = parser.date(from: iso) ?? ISO8601DateFormatter().date(from: iso)
    guard let date else { return "This month" }
    let formatter = DateFormatter()
    formatter.dateFormat = "MMMM"
    return formatter.string(from: date)
}

private func lifeExpenseCount(_ count: Int) -> String {
    count == 1 ? "1 expense" : "\(count) expenses"
}

private func lifeSharePercent(_ share: Double) -> String {
    String(format: "%.1f%%", share * 100)
}

private func inr(_ amount: String?, _ currency: String? = "INR") -> String {
    let number = Double(amount ?? "") ?? 0
    let formatter = NumberFormatter()
    formatter.numberStyle = .decimal
    formatter.maximumFractionDigits = 0
    let formatted = formatter.string(from: NSNumber(value: number)) ?? String(format: "%.0f", number)
    if currency == nil || currency == "INR" { return "₹\(formatted)" }
    return "\(currency ?? "") \(formatted)"
}

private func formatPulseWhen(_ iso: String) -> String {
    let parser = ISO8601DateFormatter()
    parser.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
    let date = parser.date(from: iso) ?? ISO8601DateFormatter().date(from: iso)
    guard let date else { return iso }
    let formatter = DateFormatter()
    formatter.dateFormat = "dd MMM · h:mm a"
    return formatter.string(from: date)
}

private func formatWhen(_ iso: String) -> String {
    let parser = ISO8601DateFormatter()
    parser.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
    let date = parser.date(from: iso) ?? ISO8601DateFormatter().date(from: iso)
    guard let date else { return iso }
    let formatter = DateFormatter()
    formatter.locale = Locale(identifier: "en_US")
    formatter.dateFormat = "dd MMM yyyy · h:mm a"
    return formatter.string(from: date)
}

private func formatSheetWhen(_ iso: String) -> String {
    formatWhen(iso)
        .replacingOccurrences(of: " AM", with: " am")
        .replacingOccurrences(of: " PM", with: " pm")
}

private func formatSheetAmount(_ raw: String) -> String {
    if raw.isEmpty { return "₹0" }
    let dot = raw.firstIndex(of: ".")
    let wholeText = dot.map { String(raw[..<$0]) } ?? raw
    let fraction = dot.map { String(raw[raw.index(after: $0)...]) } ?? ""
    let formatter = NumberFormatter()
    formatter.locale = Locale(identifier: "en_US")
    formatter.numberStyle = .decimal
    formatter.maximumFractionDigits = 0
    let grouped = formatter.string(from: NSNumber(value: Int(wholeText) ?? 0)) ?? wholeText
    return dot == nil ? "₹\(grouped)" : "₹\(grouped).\(fraction)"
}

private func sanitizeAmount(_ raw: String) -> String {
    let cleaned = raw.filter { $0.isNumber || $0 == "." }
    guard let dot = cleaned.firstIndex(of: ".") else {
        let whole = cleaned.drop(while: { $0 == "0" })
        return whole.isEmpty ? (cleaned.isEmpty ? "" : "0") : String(whole)
    }
    let whole = cleaned[..<dot].drop(while: { $0 == "0" })
    let fraction = cleaned[cleaned.index(after: dot)...].filter(\.isNumber).prefix(2)
    return "\(whole.isEmpty ? "0" : String(whole)).\(fraction)"
}

private func dateFromSheet(_ iso: String) -> Date {
    let parser = ISO8601DateFormatter()
    parser.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
    return parser.date(from: iso) ?? ISO8601DateFormatter().date(from: iso) ?? Date()
}

private func sheetIso(_ date: Date) -> String {
    ISO8601DateFormatter().string(from: date)
}

private func jpegPhotoData(_ data: Data) -> Data? {
    guard let image = UIImage(data: data) else { return nil }
    let largest = max(image.size.width, image.size.height)
    let prepared: UIImage
    if largest > 1600, largest > 0 {
        let ratio = 1600 / largest
        let size = CGSize(width: max(image.size.width * ratio, 1), height: max(image.size.height * ratio, 1))
        let renderer = UIGraphicsImageRenderer(size: size)
        prepared = renderer.image { _ in image.draw(in: CGRect(origin: .zero, size: size)) }
    } else {
        prepared = image
    }
    return prepared.jpegData(compressionQuality: 0.85)
}

private func expensePhoto(_ data: Data?) -> PersonalExpensePhotoRequest? {
    guard let data else { return nil }
    return PersonalExpensePhotoRequest(contentType: "image/jpeg", dataBase64: data.base64EncodedString())
}

private struct PersonalExpenseSheetForm: View {
    let busy: Bool
    let errorText: String?
    let onClose: () -> Void
    let onPickArea: (ExpenseDraft) -> Void
    let onSave: (ExpenseDraft) -> Void
    let onRefund: (String) -> Void
    let onVoid: (String) -> Void
    @State private var draft: ExpenseDraft

    init(
        initial: ExpenseDraft,
        busy: Bool,
        errorText: String?,
        onClose: @escaping () -> Void,
        onPickArea: @escaping (ExpenseDraft) -> Void,
        onSave: @escaping (ExpenseDraft) -> Void,
        onRefund: @escaping (String) -> Void,
        onVoid: @escaping (String) -> Void
    ) {
        self.busy = busy
        self.errorText = errorText
        self.onClose = onClose
        self.onPickArea = onPickArea
        self.onSave = onSave
        self.onRefund = onRefund
        self.onVoid = onVoid
        _draft = State(initialValue: initial)
    }

    @State private var showDatePicker = false
    @State private var pickedDate = Date()
    @State private var photoItem: PhotosPickerItem?

    var body: some View {
        let area = draft.areaCode.flatMap { PersonalLifeTaxonomy.area($0) }
        let sub = draft.subcategoryCode.flatMap { PersonalLifeTaxonomy.subcategory($0) }
        let canSave = !busy && (Double(normalizedAmount(draft.amount)) ?? 0) > 0 && area != nil && sub != nil
        ScrollView {
            VStack(alignment: .leading, spacing: 12) {
                Capsule()
                    .fill(PersonalTokens.Colors.textSubtle)
                    .frame(width: 36, height: 4)
                    .frame(maxWidth: .infinity)
                HStack {
                    Text(draft.editingId == nil ? "Add expense" : "Edit life context")
                        .personalText(PersonalTokens.Typography.section)
                        .foregroundStyle(PersonalTokens.Colors.textPrimary)
                    Spacer()
                    Button(action: onClose) {
                        HStack(spacing: 4) {
                            Text("Close").personalText(PersonalTokens.Typography.metadata)
                            Image("PersonalIcons/x").renderingMode(.template).frame(width: 14, height: 14)
                        }
                        .foregroundStyle(PersonalTokens.Colors.textSecondary)
                        .padding(.horizontal, 10)
                        .padding(.vertical, 8)
                        .background(PersonalTokens.Colors.surfaceContainer, in: Capsule())
                    }
                    .buttonStyle(.plain)
                }
                Text("AMOUNT · INR · REQUIRED")
                    .personalText(PersonalTokens.Typography.metadata)
                    .foregroundStyle(PersonalTokens.Colors.brandPrimary)
                if draft.editingId == nil {
                    HStack(spacing: 4) {
                        Text(formatSheetAmount(draft.amount))
                            .personalText(PersonalTokens.Typography.amountXL)
                                .foregroundStyle(PersonalTokens.Colors.textPrimary)
                        Rectangle().fill(PersonalTokens.Colors.brandPrimary).frame(width: 2, height: 42)
                    }
                    .overlay {
                        TextField(
                            "",
                            text: Binding(get: { draft.amount }, set: { draft.amount = sanitizeAmount($0) })
                        )
                        .keyboardType(.decimalPad)
                        .foregroundStyle(.clear)
                        .tint(.clear)
                    }
                } else {
                    Text(formatSheetAmount(draft.amount))
                        .personalText(PersonalTokens.Typography.amountXL)
                        .foregroundStyle(PersonalTokens.Colors.textPrimary)
                }
                sheetValue(icon: "store", caption: "Paid to / For · recommended", trailing: "chevron-down") {
                    TextField("Studio Fit · strength class", text: $draft.merchant)
                        .personalText(PersonalTokens.Typography.body)
                        .foregroundStyle(PersonalTokens.Colors.textPrimary)
                }
                Button {
                    pickedDate = dateFromSheet(draft.effectiveAt)
                    showDatePicker = true
                } label: {
                    sheetValueLabel(
                        icon: "calendar-days",
                        caption: "Date & time · editable default",
                        value: formatSheetWhen(draft.effectiveAt),
                        trailing: "chevron-down"
                    )
                }
                .buttonStyle(.plain)
                Button { onPickArea(draft) } label: {
                    VStack(alignment: .leading, spacing: 8) {
                        Text("One Life Area + its subcategory · required")
                            .personalText(PersonalTokens.Typography.caption)
                            .foregroundStyle(PersonalTokens.Colors.textSubtle)
                        HStack(spacing: 8) {
                        if let area {
                                area.icon.foregroundStyle(PersonalTokens.Colors.textPrimary).frame(width: 20, height: 20)
                            }
                            Text(area?.label ?? "Choose a Life Area")
                                .personalText(PersonalTokens.Typography.body)
                                .foregroundStyle(PersonalTokens.Colors.textPrimary)
                            Spacer()
                            Image("PersonalIcons/chevron-down").renderingMode(.template)
                                .foregroundStyle(PersonalTokens.Colors.textSecondary)
                                .frame(width: 16, height: 16)
                        }
                        HStack {
                            Text(sub?.label ?? "Then one subcategory")
                                .personalText(PersonalTokens.Typography.body)
                                .foregroundStyle(PersonalTokens.Colors.brandPrimary)
                            Spacer()
                            Image("PersonalIcons/chevron-down").renderingMode(.template)
                                .foregroundStyle(PersonalTokens.Colors.brandPrimary)
                                .frame(width: 16, height: 16)
                        }
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(14)
                    .background(PersonalTokens.Colors.surfaceHighlight, in: RoundedRectangle(cornerRadius: 18))
                    .overlay(RoundedRectangle(cornerRadius: 18).stroke(PersonalTokens.Colors.brandPrimary, lineWidth: 1))
                }
                .buttonStyle(.plain)
                if let area {
                    Text("\(area.layer.rawValue) · derived from \(area.label)")
                        .personalText(PersonalTokens.Typography.caption)
                        .foregroundStyle(PersonalTokens.Colors.textSubtle)
                }
                HStack {
                    HStack(spacing: 6) {
                        Text("How did it feel?").personalText(PersonalTokens.Typography.body).foregroundStyle(PersonalTokens.Colors.textPrimary)
                        Text("Optional").personalText(PersonalTokens.Typography.caption).foregroundStyle(PersonalTokens.Colors.textSubtle)
                    }
                    Spacer()
                    Button("Skip") { draft.moodCode = nil }
                        .personalText(PersonalTokens.Typography.metadata)
                        .foregroundStyle(PersonalTokens.Colors.brandPrimary)
                        .buttonStyle(.plain)
                }
                HStack(spacing: 6) {
                    ForEach(PersonalMood.allCases) { mood in
                        let selected = draft.moodCode == mood.rawValue
                        Button { draft.moodCode = mood.rawValue } label: {
                            VStack(spacing: 4) {
                                mood.icon.frame(width: 20, height: 20)
                                Text(selected ? "\(mood.label) ✓" : mood.label)
                                    .font(.system(size: 10))
                                    .multilineTextAlignment(.center)
                            }
                            .frame(maxWidth: .infinity)
                            .frame(height: 74)
                            .background(selected ? PersonalTokens.Colors.brandAccent : PersonalTokens.Colors.surfaceContainer, in: RoundedRectangle(cornerRadius: 16))
                            .overlay(RoundedRectangle(cornerRadius: 16).stroke(selected ? PersonalTokens.Colors.brandPrimary : PersonalTokens.Colors.borderHairline, lineWidth: 1))
                            .foregroundStyle(selected ? PersonalTokens.Colors.textOnAccent : PersonalTokens.Colors.textSecondary)
                        }
                        .buttonStyle(.plain)
                    }
                }
                Button { draft.showMore.toggle() } label: {
                    sheetLink(icon: "paperclip", title: "More details", caption: "Note · payment source · receipt")
                }
                .buttonStyle(.plain)
                if draft.showMore {
                    sheetValue(icon: "file-text", caption: "Note") {
                        TextField("Add a note", text: $draft.note)
                            .personalText(PersonalTokens.Typography.body)
                            .foregroundStyle(PersonalTokens.Colors.textPrimary)
                    }
                    sheetValue(icon: "wallet", caption: "Payment source") {
                        TextField("UPI · card · cash", text: $draft.paymentSource)
                            .personalText(PersonalTokens.Typography.body)
                            .foregroundStyle(PersonalTokens.Colors.textPrimary)
                    }
                }
                PhotosPicker(selection: $photoItem, matching: .images) {
                    sheetLink(icon: "camera", title: "Add memory photo", caption: "Optional · saved with this expense")
                }
                .buttonStyle(.plain)
                .onChange(of: photoItem) { _, item in
                    Task {
                        if let data = try? await item?.loadTransferable(type: Data.self) {
                            draft.photoPreview = jpegPhotoData(data)
                        }
                    }
                }
                if let data = draft.photoPreview, let image = UIImage(data: data) {
                    Image(uiImage: image)
                        .resizable()
                        .scaledToFill()
                        .frame(maxWidth: .infinity)
                        .frame(height: 120)
                        .clipShape(RoundedRectangle(cornerRadius: 14))
                }
                if let errorText {
                    Text(errorText).personalText(PersonalTokens.Typography.caption).foregroundStyle(PersonalTokens.Colors.textSecondary)
                }
                Button { onSave(draft) } label: {
                    Text(busy ? "Saving…" : "Save expense")
                        .font(.system(size: 14, weight: .bold))
                        .foregroundStyle(PersonalTokens.Colors.textOnAccent)
                        .frame(maxWidth: .infinity)
                        .frame(height: PersonalTokens.Size.buttonHeight)
                        .background(canSave ? PersonalTokens.Colors.brandAccent : PersonalTokens.Colors.surfaceHigh, in: RoundedRectangle(cornerRadius: 14))
                        .shadow(color: canSave ? PersonalTokens.Colors.fillGlow : .clear, radius: 20)
                }
                .buttonStyle(.plain)
                    .disabled(!canSave)
                Text("No Moment required. Counted once.")
                    .personalText(PersonalTokens.Typography.caption)
                    .foregroundStyle(PersonalTokens.Colors.textSubtle)
                    .frame(maxWidth: .infinity)
                    .multilineTextAlignment(.center)
                if let editingId = draft.editingId {
                    Button("Record a linked refund") { onRefund(editingId) }
                        .personalText(PersonalTokens.Typography.body)
                        .foregroundStyle(PersonalTokens.Colors.brandPrimary)
                        .buttonStyle(.plain)
                    Button("Void this expense") { onVoid(editingId) }
                        .personalText(PersonalTokens.Typography.body)
                        .foregroundStyle(PersonalTokens.Colors.textSubtle)
                        .buttonStyle(.plain)
                }
            }
            .padding(.horizontal, 20)
            .padding(.top, 12)
            .padding(.bottom, 16)
        }
        .background(PersonalTokens.Colors.bgPrimary)
        .overlay(
            UnevenRoundedRectangle(topLeadingRadius: 32, bottomLeadingRadius: 0, bottomTrailingRadius: 0, topTrailingRadius: 32)
                .stroke(PersonalTokens.Colors.borderHairline, lineWidth: 1)
        )
        .sheet(isPresented: $showDatePicker) {
            VStack(spacing: 16) {
                DatePicker("Date & time", selection: $pickedDate, displayedComponents: [.date, .hourAndMinute])
                    .datePickerStyle(.graphical)
                    .labelsHidden()
                    .tint(PersonalTokens.Colors.brandAccent)
                Button("Done") {
                    draft.effectiveAt = sheetIso(pickedDate)
                    showDatePicker = false
                }
                .font(.system(size: 15, weight: .semibold))
                .foregroundStyle(PersonalTokens.Colors.textOnAccent)
                .frame(maxWidth: .infinity)
                .frame(height: 48)
                .background(PersonalTokens.Colors.brandAccent, in: RoundedRectangle(cornerRadius: 14))
                .buttonStyle(.plain)
            }
            .padding(20)
            .presentationDetents([.medium, .large])
            .presentationBackground(PersonalTokens.Colors.bgPrimary)
        }
    }

    private func sheetValue<Field: View>(icon: String, caption: String, trailing: String? = nil, @ViewBuilder field: () -> Field) -> some View {
        valueRow(icon: icon, caption: caption, value: nil, trailing: trailing, field: field())
    }

    private func sheetValueLabel(icon: String, caption: String, value: String, trailing: String?) -> some View {
        valueRow(icon: icon, caption: caption, value: value, trailing: trailing, field: EmptyView())
    }

    private func valueRow<Field: View>(icon: String, caption: String, value: String?, trailing: String?, field: Field) -> some View {
        HStack(spacing: 12) {
            Image("PersonalIcons/\(icon)").renderingMode(.template)
                .foregroundStyle(PersonalTokens.Colors.textSecondary)
                .frame(width: 20, height: 20)
            VStack(alignment: .leading, spacing: 2) {
                Text(caption).personalText(PersonalTokens.Typography.caption).foregroundStyle(PersonalTokens.Colors.textSubtle)
                if let value {
                    Text(value).personalText(PersonalTokens.Typography.body).foregroundStyle(PersonalTokens.Colors.textPrimary)
                } else {
                    field
                }
            }
            Spacer(minLength: 0)
            if let trailing {
                Image("PersonalIcons/\(trailing)").renderingMode(.template)
                    .foregroundStyle(PersonalTokens.Colors.textSecondary)
                    .frame(width: 16, height: 16)
            }
        }
        .padding(14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(PersonalTokens.Colors.surfaceContainer, in: RoundedRectangle(cornerRadius: 14))
        .overlay(RoundedRectangle(cornerRadius: 14).stroke(PersonalTokens.Colors.borderHairline, lineWidth: 1))
    }

    private func sheetLink(icon: String, title: String, caption: String) -> some View {
        HStack(spacing: 12) {
            Image("PersonalIcons/\(icon)").renderingMode(.template)
                .foregroundStyle(PersonalTokens.Colors.brandPrimary)
                .frame(width: 20, height: 20)
            VStack(alignment: .leading, spacing: 2) {
                Text(title).personalText(PersonalTokens.Typography.body).foregroundStyle(PersonalTokens.Colors.brandPrimary)
                Text(caption).personalText(PersonalTokens.Typography.caption).foregroundStyle(PersonalTokens.Colors.textSubtle)
            }
            Spacer()
            Image("PersonalIcons/chevron-right").renderingMode(.template)
                .foregroundStyle(PersonalTokens.Colors.textSecondary)
                .frame(width: 16, height: 16)
        }
        .padding(.vertical, 6)
    }
}

private enum AreaSlot {
    case pair(PersonalLifeArea, PersonalLifeArea)
    case wide(PersonalLifeArea, Bool)
}

private func areaSlots(_ areas: [PersonalLifeArea], selected: String?) -> [AreaSlot] {
    var slots: [AreaSlot] = []
    var index = 0
    while index < areas.count {
        let area = areas[index]
        if area.code == selected {
            slots.append(.wide(area, true))
            index += 1
        } else if index + 1 < areas.count, areas[index + 1].code != selected {
            slots.append(.pair(area, areas[index + 1]))
            index += 2
        } else {
            slots.append(.wide(area, false))
            index += 1
        }
    }
    return slots
}

private struct ExpensePickerChrome: View {
    let step: Int
    let left: String
    let right: String
    let amount: String
    let onBack: () -> Void
    let onClose: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 18) {
            HStack {
                Button(action: onBack) {
                    HStack(spacing: 6) {
                        Image("PersonalIcons/arrow-left").renderingMode(.template)
                            .foregroundStyle(PersonalTokens.Colors.textPrimary)
                            .frame(width: 18, height: 18)
                        Text("Back").personalText(PersonalTokens.Typography.metadata).foregroundStyle(PersonalTokens.Colors.textPrimary)
                    }
                    .padding(10)
                    .background(PersonalTokens.Colors.surfaceContainer, in: Capsule())
                }
                .buttonStyle(.plain)
                Spacer()
                Button("Close ×", action: onClose)
                    .personalText(PersonalTokens.Typography.body)
                    .foregroundStyle(PersonalTokens.Colors.brandPrimary)
            }
            Text("EXPENSE CONTEXT · STEP \(step) OF 2")
                .personalText(PersonalTokens.Typography.metadata)
                .foregroundStyle(PersonalTokens.Colors.brandPrimary)
            HStack(spacing: 10) {
                stepChip(left, selected: step == 1)
                Rectangle().fill(PersonalTokens.Colors.brandPrimary).frame(width: 20, height: 1)
                stepChip(right, selected: step == 2)
            }
            HStack {
                Text("EXPENSE DRAFT · NOT SAVED")
                    .personalText(PersonalTokens.Typography.metadata)
                    .foregroundStyle(PersonalTokens.Colors.textSecondary)
                Spacer()
                Text(inr(amount.isEmpty ? "0" : amount))
                    .personalText(PersonalTokens.Typography.section)
                    .foregroundStyle(PersonalTokens.Colors.textPrimary)
            }
            .padding(12)
            .frame(maxWidth: .infinity)
            .background(PersonalTokens.Colors.fillIconWellQuiet, in: RoundedRectangle(cornerRadius: 12))
        }
    }

    private func stepChip(_ label: String, selected: Bool) -> some View {
        Text(label)
            .personalText(PersonalTokens.Typography.metadata)
            .foregroundStyle(selected ? PersonalTokens.Colors.textOnAccent : PersonalTokens.Colors.textSecondary)
            .padding(.horizontal, 14)
            .padding(.vertical, 8)
            .background(selected ? PersonalTokens.Colors.brandAccent : PersonalTokens.Colors.surfaceContainer, in: Capsule())
            .overlay(Capsule().stroke(PersonalTokens.Colors.borderHairline, lineWidth: 1))
    }
}

private struct PersonalAreaStep: View {
    let draft: ExpenseDraft
    let onBack: () -> Void
    let onClose: () -> Void
    let onNext: (ExpenseDraft) -> Void
    @State private var selected: String?

    init(draft: ExpenseDraft, onBack: @escaping () -> Void, onClose: @escaping () -> Void, onNext: @escaping (ExpenseDraft) -> Void) {
        self.draft = draft
        self.onBack = onBack
        self.onClose = onClose
        self.onNext = onNext
        _selected = State(initialValue: draft.areaCode)
    }

    var body: some View {
        let chosen = selected.flatMap { PersonalLifeTaxonomy.area($0) }
        ScrollView {
            VStack(alignment: .leading, spacing: PersonalTokens.Space.lg) {
                ExpensePickerChrome(step: 1, left: "1 · Life Area", right: "2 · Subcategory", amount: draft.amount, onBack: onBack, onClose: onClose)
                VStack(alignment: .leading, spacing: 8) {
                    Text("What part of life is this for?").personalText(PersonalTokens.Typography.hero).foregroundStyle(PersonalTokens.Colors.textPrimary)
                    Text("Choose one area. Then pick one of its six subcategories.").personalText(PersonalTokens.Typography.body).foregroundStyle(PersonalTokens.Colors.textSecondary)
                }
                layer("NOW · Everyday living", areas: PersonalLifeTaxonomy.areas(in: .now))
                layer("BUILD · Longer horizons", areas: PersonalLifeTaxonomy.areas(in: .build))
                VStack(alignment: .leading, spacing: 12) {
                    Button {
                        guard let chosen else { return }
                        var next = draft
                        if next.areaCode != chosen.code { next.subcategoryCode = nil }
                        next.areaCode = chosen.code
                        onNext(next)
                    } label: {
                        Text("Next: choose a subcategory")
                            .personalText(PersonalTokens.Typography.button)
                            .foregroundStyle(PersonalTokens.Colors.textOnAccent)
                            .frame(maxWidth: .infinity, minHeight: PersonalTokens.Size.buttonVisual)
                    }
                    .buttonStyle(.plain)
                    .background(chosen == nil ? PersonalTokens.Colors.surfaceHigh : PersonalTokens.Colors.brandAccent, in: RoundedRectangle(cornerRadius: PersonalTokens.Radius.button))
                    .disabled(chosen == nil)
                    if let chosen {
                        Text("\(chosen.label) selected. \(inr(draft.amount.isEmpty ? "0" : draft.amount)) draft kept.")
                            .personalText(PersonalTokens.Typography.caption)
                            .foregroundStyle(PersonalTokens.Colors.textSecondary)
                    }
                    Text("NOW / BUILD is automatic, not another decision.")
                        .personalText(PersonalTokens.Typography.caption)
                        .foregroundStyle(PersonalTokens.Colors.textSecondary)
                }
            }
            .padding(20)
        }
    }

    private func layer(_ label: String, areas: [PersonalLifeArea]) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            Text(label).personalText(PersonalTokens.Typography.metadata).foregroundStyle(PersonalTokens.Colors.brandPrimary)
            ForEach(Array(areaSlots(areas, selected: selected).enumerated()), id: \.offset) { _, slot in
                switch slot {
                case let .pair(left, right):
                    HStack(spacing: 10) {
                        tile(left, wide: false, selected: false)
                        tile(right, wide: false, selected: false)
                    }
                case let .wide(area, isSelected):
                    tile(area, wide: true, selected: isSelected)
                }
            }
        }
    }

    private func tile(_ area: PersonalLifeArea, wide: Bool, selected: Bool) -> some View {
        Button { self.selected = area.code } label: {
            Group {
                if wide {
                    HStack(spacing: 12) {
                        mark(area)
                        VStack(alignment: .leading, spacing: 6) {
                            Text(area.label).personalText(PersonalTokens.Typography.body).foregroundStyle(PersonalTokens.Colors.textPrimary)
                            Text(selected ? "Selected · 6 subcategories" : "6 subcategories")
                                .personalText(PersonalTokens.Typography.caption)
                                .foregroundStyle(selected ? PersonalTokens.Colors.brandPrimary : PersonalTokens.Colors.textSubtle)
                        }
                        Spacer()
                        Image(selected ? "PersonalIcons/circle-check" : "PersonalIcons/arrow-up-right").renderingMode(.template)
                            .foregroundStyle(selected ? PersonalTokens.Colors.brandPrimary : PersonalTokens.Colors.textSubtle)
                            .frame(width: 20, height: 20)
                    }
                } else {
                    VStack(alignment: .leading, spacing: 12) {
                        mark(area)
                        VStack(alignment: .leading, spacing: 6) {
                            Text(area.label).personalText(PersonalTokens.Typography.body).foregroundStyle(PersonalTokens.Colors.textPrimary).multilineTextAlignment(.leading)
                            Text("6 subcategories").personalText(PersonalTokens.Typography.caption).foregroundStyle(PersonalTokens.Colors.textSubtle)
                        }
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                }
            }
            .padding(16)
            .frame(maxWidth: .infinity, minHeight: wide ? 108 : 126, alignment: wide ? .center : .topLeading)
            .background(selected ? PersonalTokens.Colors.surfaceHighlight : PersonalTokens.Colors.surfaceContainer, in: RoundedRectangle(cornerRadius: PersonalTokens.Radius.tileSelected))
            .overlay(RoundedRectangle(cornerRadius: PersonalTokens.Radius.tileSelected).stroke(selected ? PersonalTokens.Colors.borderSelected : PersonalTokens.Colors.borderHairline, lineWidth: selected ? 1.5 : 1))
        }
        .buttonStyle(.plain)
    }

    private func mark(_ area: PersonalLifeArea) -> some View {
        area.icon.foregroundStyle(area.tint)
            .frame(width: 22, height: 22)
            .frame(width: 36, height: 36)
            .background(PersonalTokens.Colors.fillIconWellQuiet, in: RoundedRectangle(cornerRadius: 12))
    }
}

private struct PersonalSubcategoryStep: View {
    let draft: ExpenseDraft
    let onBack: () -> Void
    let onClose: () -> Void
    let onChangeArea: () -> Void
    let onUse: (ExpenseDraft) -> Void
    @State private var picked: String?

    init(
        draft: ExpenseDraft,
        onBack: @escaping () -> Void,
        onClose: @escaping () -> Void,
        onChangeArea: @escaping () -> Void,
        onUse: @escaping (ExpenseDraft) -> Void
    ) {
        self.draft = draft
        self.onBack = onBack
        self.onClose = onClose
        self.onChangeArea = onChangeArea
        self.onUse = onUse
        _picked = State(initialValue: draft.subcategoryCode)
    }

    var body: some View {
        let area = draft.areaCode.flatMap { PersonalLifeTaxonomy.area($0) } ?? PersonalLifeTaxonomy.areas[0]
        let chosen = area.subcategories.first { $0.code == picked }
        ScrollView {
            VStack(alignment: .leading, spacing: PersonalTokens.Space.lg) {
                ExpensePickerChrome(step: 2, left: "✓ Life Area", right: "2 · Subcategory", amount: draft.amount, onBack: onBack, onClose: onClose)
            VStack(alignment: .leading, spacing: 12) {
                HStack {
                    area.icon.foregroundStyle(area.tint)
                            .frame(width: 30, height: 30)
                            .frame(width: 56, height: 56)
                            .background(PersonalTokens.Colors.fillIconWellQuiet, in: RoundedRectangle(cornerRadius: 18))
                    Spacer()
                        Text("\(area.layer.rawValue) · automatic")
                            .personalText(PersonalTokens.Typography.metadata)
                            .foregroundStyle(PersonalTokens.Colors.textSecondary)
                            .padding(.horizontal, 14)
                            .padding(.vertical, 8)
                            .background(PersonalTokens.Colors.surfaceContainer, in: Capsule())
                            .overlay(Capsule().stroke(PersonalTokens.Colors.borderHairline, lineWidth: 1))
                    }
                    Text(area.label).personalText(PersonalTokens.Typography.hero).foregroundStyle(PersonalTokens.Colors.textPrimary)
                    Text(area.tagline).personalText(PersonalTokens.Typography.body).foregroundStyle(PersonalTokens.Colors.textSecondary)
                    Button(action: onChangeArea) {
                        HStack(spacing: 6) {
                            Text("Change Life Area").personalText(PersonalTokens.Typography.metadata)
                            Image("PersonalIcons/arrow-up-right").renderingMode(.template).frame(width: 14, height: 14)
                        }
                        .foregroundStyle(PersonalTokens.Colors.brandPrimary)
                    }
                    .buttonStyle(.plain)
                }
                VStack(alignment: .leading, spacing: 12) {
                    Text("Choose one subcategory").personalText(PersonalTokens.Typography.section).foregroundStyle(PersonalTokens.Colors.textPrimary)
                LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 10) {
                    ForEach(area.subcategories) { sub in
                        let selected = picked == sub.code
                        Button { picked = sub.code } label: {
                                VStack(alignment: .leading, spacing: 4) {
                                    Text(sub.label).personalText(PersonalTokens.Typography.body).foregroundStyle(PersonalTokens.Colors.textPrimary).multilineTextAlignment(.leading)
                                    Text(selected ? "Selected" : sub.detail)
                                        .personalText(PersonalTokens.Typography.caption)
                                        .foregroundStyle(selected ? PersonalTokens.Colors.brandPrimary : PersonalTokens.Colors.textSubtle)
                                }
                                .frame(maxWidth: .infinity, minHeight: 142, alignment: .topLeading)
                                .padding(16)
                                .background(selected ? PersonalTokens.Colors.surfaceHighlight : PersonalTokens.Colors.surfaceContainer, in: RoundedRectangle(cornerRadius: PersonalTokens.Radius.tileSelected))
                                .overlay(RoundedRectangle(cornerRadius: PersonalTokens.Radius.tileSelected).stroke(selected ? PersonalTokens.Colors.borderSelected : PersonalTokens.Colors.borderHairline, lineWidth: selected ? 1.5 : 1))
                            }
                            .buttonStyle(.plain)
                        }
                    }
                }
                VStack(alignment: .leading, spacing: 12) {
                    Button {
                    guard let chosen else { return }
                    var next = draft
                    next.areaCode = chosen.areaCode
                    next.subcategoryCode = chosen.code
                    onUse(next)
                    } label: {
                        Text(chosen.map { "Use \($0.label)" } ?? "Choose a subcategory")
                            .personalText(PersonalTokens.Typography.button)
                            .foregroundStyle(PersonalTokens.Colors.textOnAccent)
                            .frame(maxWidth: .infinity, minHeight: PersonalTokens.Size.buttonVisual)
                }
                    .buttonStyle(.plain)
                    .background(chosen == nil ? PersonalTokens.Colors.surfaceHigh : PersonalTokens.Colors.brandAccent, in: RoundedRectangle(cornerRadius: PersonalTokens.Radius.button))
                .disabled(chosen == nil)
                    Text("Returns to your expense form. Nothing is saved yet.")
                        .personalText(PersonalTokens.Typography.caption)
                        .foregroundStyle(PersonalTokens.Colors.textSecondary)
                    Text("\(area.layer.rawValue) is derived from this Life Area. One area, one valid subcategory.")
                        .personalText(PersonalTokens.Typography.caption)
                        .foregroundStyle(PersonalTokens.Colors.textSecondary)
                }
            }
            .padding(20)
        }
    }
}
