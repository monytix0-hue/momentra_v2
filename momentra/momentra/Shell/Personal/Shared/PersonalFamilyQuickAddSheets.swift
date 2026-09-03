import SwiftUI

enum FutureQuickAddKind: String, Identifiable {
    case milestone
    case opportunity
    case pivot
    case progress
    case learning

    var id: String { rawValue }

    var apiKind: String {
        switch self {
        case .milestone: return "MILESTONE"
        case .opportunity: return "OPPORTUNITY"
        case .pivot: return "PIVOT"
        case .progress: return "PROGRESS"
        case .learning: return "LEARNING"
        }
    }
}

enum LifestyleQuickAddKind: String, Identifiable {
    case experience
    case wellbeing
    case discovery
    case expression
    case adjust

    var id: String { rawValue }

    var apiContext: String {
        switch self {
        case .experience: return "EXPERIENCE"
        case .wellbeing: return "WELLBEING"
        case .discovery: return "DISCOVERY"
        case .expression: return "CREATION"
        case .adjust: return "LIFESTYLE"
        }
    }
}

enum RelationshipsQuickAddKind: String, Identifiable {
    case connection
    case shared
    case investment
    case support
    case adjust

    var id: String { rawValue }

    /// Backend `relationshipActivitySchema.activityKind`.
    var apiKind: String {
        switch self {
        case .connection: return "CONNECTION"
        case .shared: return "SHARED_EXPERIENCE"
        case .investment: return "INVESTMENT"
        case .support: return "SUPPORT"
        case .adjust: return "INTERACTION"
        }
    }
}

/// Figma Future Building quick-add sheets:
/// Milestone `353:11724`, Opportunity `353:11768`, Pivot `353:11812`, plus Progress / Learning.
struct PersonalFutureQuickAddSheet: View {
    let kind: FutureQuickAddKind
    let momentId: String
    var onClose: () -> Void
    var onSaved: () -> Void

    @State private var selectedPrimary = ""
    @State private var selectedSecondary = ""
    @State private var selectedTertiary = ""
    @State private var note = ""
    @State private var meter = 0
    @State private var submitting = false
    @State private var error: String?
    @State private var draftKey = UUID().uuidString

    private let repository = FutureItemCreateRepository()
    private var copy: FutureSheetCopy { kind.sheetCopy }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 14) {
                header
                hero
                section(copy.noteLabel) {
                    noteField(placeholder: copy.notePlaceholder)
                }
                section(copy.primaryLabel) {
                    chipFlow(copy.primaryOptions, selected: $selectedPrimary, active: copy.chipActive, activeText: copy.chipActiveText)
                }
                if !copy.secondaryOptions.isEmpty {
                    section(copy.secondaryLabel) {
                        chipFlow(copy.secondaryOptions, selected: $selectedSecondary, active: copy.chipActive, activeText: copy.chipActiveText)
                    }
                }
                if !copy.tertiaryOptions.isEmpty {
                    section(copy.tertiaryLabel) {
                        chipFlow(copy.tertiaryOptions, selected: $selectedTertiary, active: copy.chipActive, activeText: copy.chipActiveText)
                    }
                }
                if copy.showMeter {
                    section(copy.meterLabel) {
                        meterRow
                    }
                }
                if let error {
                    Text(error)
                        .font(.system(size: 12))
                        .foregroundStyle(Color(hex: "#F87171"))
                }
                Button(action: save) {
                    Text(submitting ? "Saving…" : copy.cta)
                        .font(.system(size: 15, weight: .heavy))
                        .foregroundStyle(copy.ctaText)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 14)
                        .background(copy.ctaColor)
                        .clipShape(RoundedRectangle(cornerRadius: 14))
                }
                .buttonStyle(.plain)
                .disabled(submitting || selectedPrimary.isEmpty)
                .opacity(submitting || selectedPrimary.isEmpty ? 0.6 : 1)
            }
            .padding(.horizontal, 16)
            .padding(.bottom, 20)
        }
        .background(Color(hex: "#14121B"))
        .onAppear {
            selectedPrimary = copy.primaryOptions.first ?? ""
            selectedSecondary = copy.secondaryOptions.first ?? ""
            selectedTertiary = copy.tertiaryOptions.first ?? ""
            meter = copy.meterDefault
        }
        .presentationDetents([.medium, .large])
        .presentationDragIndicator(.visible)
    }

    private func save() {
        guard !submitting else { return }
        submitting = true
        error = nil
        let trimmedNote = note.trimmingCharacters(in: .whitespacesAndNewlines)
        let title = trimmedNote.isEmpty ? selectedPrimary : String(trimmedNote.prefix(120))
        var parts: [String] = [selectedPrimary]
        if !selectedSecondary.isEmpty { parts.append(selectedSecondary) }
        if !selectedTertiary.isEmpty { parts.append(selectedTertiary) }
        if copy.showMeter, meter < copy.meterOptions.count {
            parts.append(copy.meterOptions[meter])
        }
        if !trimmedNote.isEmpty, trimmedNote != title {
            parts.append(trimmedNote)
        }
        let description = parts.joined(separator: " · ")
        let progressValue = kind.progressValue(meter: meter, meterOptions: copy.meterOptions)
        Task {
            do {
                _ = try await repository.createFutureItem(
                    draftKey: draftKey,
                    momentId: momentId,
                    kind: kind.apiKind,
                    title: title,
                    description: description,
                    progressValue: progressValue
                )
                await MainActor.run {
                    submitting = false
                    onSaved()
                }
            } catch {
                await MainActor.run {
                    submitting = false
                    self.error = error.localizedDescription
                }
            }
        }
    }

    private var header: some View {
        HStack(alignment: .top) {
            VStack(alignment: .leading, spacing: 2) {
                Text(copy.title)
                    .font(.system(size: 20, weight: .heavy))
                    .foregroundStyle(Color(hex: "#E5E0EE"))
                Text(copy.subtitle)
                    .font(.system(size: 12))
                    .foregroundStyle(Color(hex: "#C9C4D8"))
            }
            Spacer(minLength: 8)
            Button(action: onClose) {
                Text("×")
                    .font(.system(size: 16, weight: .bold))
                    .foregroundStyle(.white)
                    .frame(width: 28, height: 28)
                    .background(Color.white.opacity(0.1))
                    .overlay(RoundedRectangle(cornerRadius: 14).stroke(Color.white.opacity(0.2), lineWidth: 1))
                    .clipShape(RoundedRectangle(cornerRadius: 14))
            }
            .buttonStyle(.plain)
        }
    }

    private var hero: some View {
        HStack(spacing: 12) {
            Text(copy.heroGlyph)
                .font(.system(size: 18))
                .foregroundStyle(.white)
                .frame(width: 44, height: 44)
                .background(copy.heroIconBg)
                .clipShape(RoundedRectangle(cornerRadius: 14))
            VStack(alignment: .leading, spacing: 2) {
                Text(copy.heroTitle)
                    .font(.system(size: 18, weight: .bold))
                    .foregroundStyle(copy.heroTitleColor)
                Text(copy.heroBody)
                    .font(.system(size: 13))
                    .foregroundStyle(Color(hex: "#C9C4D8"))
            }
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color(hex: "#201E28"))
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(Color.white.opacity(0.08), lineWidth: 1))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    private var meterRow: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(spacing: 0) {
                ForEach(copy.meterOptions.indices, id: \.self) { index in
                    Rectangle()
                        .fill(meter == index ? copy.chipActive : Color(hex: "#14121B"))
                        .frame(height: 10)
                        .onTapGesture { meter = index }
                }
            }
            .clipShape(Capsule())
            HStack {
                ForEach(Array(copy.meterOptions.enumerated()), id: \.offset) { index, label in
                    Text(label)
                        .font(.system(size: 10, weight: meter == index ? .bold : .regular))
                        .foregroundStyle(meter == index ? copy.chipActive : Color(hex: "#C9C4D8"))
                        .frame(maxWidth: .infinity, alignment: .leading)
                }
            }
        }
    }

    private func section<Content: View>(_ title: String, @ViewBuilder content: () -> Content) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            Text(title)
                .font(.system(size: 11, weight: .bold))
                .tracking(0.8)
                .foregroundStyle(Color(hex: "#C9BFFF"))
            content()
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color(hex: "#201E28"))
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(Color.white.opacity(0.08), lineWidth: 1))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    private func noteField(placeholder: String) -> some View {
        ZStack(alignment: .topLeading) {
            if note.isEmpty {
                Text(placeholder)
                    .font(.system(size: 13))
                    .foregroundStyle(Color(hex: "#C9C4D8"))
                    .padding(.top, 8)
                    .padding(.leading, 4)
            }
            TextEditor(text: $note)
                .scrollContentBackground(.hidden)
                .foregroundStyle(Color(hex: "#E5E0EE"))
                .frame(minHeight: 72)
        }
        .padding(8)
        .background(Color(hex: "#35333E"))
        .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color(hex: "#938EA1"), lineWidth: 1))
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }

    private func chipFlow(
        _ options: [String],
        selected: Binding<String>,
        active: Color,
        activeText: Color
    ) -> some View {
        FlowLayout(spacing: 8) {
            ForEach(options, id: \.self) { option in
                let isOn = selected.wrappedValue == option
                Button {
                    selected.wrappedValue = option
                } label: {
                    Text(option)
                        .font(.system(size: 12, weight: isOn ? .heavy : .semibold))
                        .foregroundStyle(isOn ? activeText : Color(hex: "#E5E0EE"))
                        .padding(.horizontal, 12)
                        .padding(.vertical, 8)
                        .background(isOn ? active : Color(hex: "#3A3842"))
                        .overlay(
                            Capsule().stroke(isOn ? active : Color(hex: "#938EA1"), lineWidth: 1)
                        )
                        .clipShape(Capsule())
                }
                .buttonStyle(.plain)
            }
        }
    }
}

private struct FutureSheetCopy {
    let title: String
    let subtitle: String
    let heroTitle: String
    let heroBody: String
    let heroGlyph: String
    let heroIconBg: Color
    let heroTitleColor: Color
    let noteLabel: String
    let notePlaceholder: String
    let primaryLabel: String
    let primaryOptions: [String]
    var secondaryLabel: String = ""
    var secondaryOptions: [String] = []
    var tertiaryLabel: String = ""
    var tertiaryOptions: [String] = []
    var showMeter: Bool = false
    var meterLabel: String = ""
    var meterOptions: [String] = []
    var meterDefault: Int = 0
    let chipActive: Color
    let chipActiveText: Color
    let cta: String
    let ctaColor: Color
    let ctaText: Color
}

private extension FutureQuickAddKind {
    var sheetCopy: FutureSheetCopy {
        switch self {
        case .milestone:
            return FutureSheetCopy(
                title: "Quick Add",
                subtitle: "Milestone",
                heroTitle: "Milestone",
                heroBody: "Celebrate an achievement that moved you forward.",
                heroGlyph: "★",
                heroIconBg: Color(hex: "#35333E"),
                heroTitleColor: Color(hex: "#E5E0EE"),
                noteLabel: "WHAT DID YOU ACHIEVE?",
                notePlaceholder: "Career growth, Business growth, Personal reinvention...",
                primaryLabel: "WHAT KIND OF MILESTONE?",
                primaryOptions: [
                    "Achievement", "Recognition", "Completion", "Launch",
                    "Certification", "Promotion", "Breakthrough", "Revenue Event",
                ],
                secondaryLabel: "HOW BIG DOES THIS FEEL?",
                secondaryOptions: ["Personal Win", "Shared Win", "Life Moment"],
                tertiaryLabel: "MEASURABLE OUTCOME",
                tertiaryOptions: [
                    "Income Increase", "Saving Increase", "Revenue Increase",
                    "Cost Reduction", "No Financial Impact",
                ],
                chipActive: Color(hex: "#C9BFFF"),
                chipActiveText: Color(hex: "#2F009C"),
                cta: "Save Milestone",
                ctaColor: Color(hex: "#C9BFFF"),
                ctaText: Color(hex: "#2F009C")
            )
        case .opportunity:
            return FutureSheetCopy(
                title: "Build Momentum",
                subtitle: "Record something that moved you forward.",
                heroTitle: "Opportunity",
                heroBody: "Spot and capture opportunities as they appear.",
                heroGlyph: "◎",
                heroIconBg: Color(hex: "#7C5CFC"),
                heroTitleColor: Color(hex: "#7C5CFC"),
                noteLabel: "WHAT DID YOU SPOT?",
                notePlaceholder: "Describe what happened",
                primaryLabel: "OPPORTUNITY TYPE",
                primaryOptions: [
                    "New Connection", "New Skill", "New Resource", "New Idea",
                    "New Funding", "New Role", "New Client", "Partnership",
                ],
                secondaryLabel: "STATUS",
                secondaryOptions: ["Exploring", "Considering", "Acting", "Captured"],
                showMeter: true,
                meterLabel: "POTENTIAL IMPACT",
                meterOptions: ["Low", "Moderate", "High", "Game Changer"],
                meterDefault: 2,
                chipActive: Color(hex: "#7C5CFC"),
                chipActiveText: .white,
                cta: "Save Opportunity",
                ctaColor: Color(hex: "#7C5CFC"),
                ctaText: .white
            )
        case .pivot:
            return FutureSheetCopy(
                title: "Build Momentum",
                subtitle: "Record something that moved you forward.",
                heroTitle: "Pivot",
                heroBody: "Record a change in direction or strategy.",
                heroGlyph: "↻",
                heroIconBg: Color(hex: "#35333E"),
                heroTitleColor: Color(hex: "#E5E0EE"),
                noteLabel: "WHAT DIRECTION CHANGED?",
                notePlaceholder: "I moved from Developer role to Engineering Management.",
                primaryLabel: "WHAT CHANGED?",
                primaryOptions: [
                    "New Priority", "New Goal", "Reduce Scope",
                    "Increase Focus", "Change Timeline", "Change Direction",
                ],
                secondaryLabel: "WHY DID YOU CHANGE DIRECTION?",
                secondaryOptions: [
                    "New Information", "Opportunity", "Constraint", "Personal Decision", "Market Change",
                ],
                showMeter: true,
                meterLabel: "HOW CONFIDENT ARE YOU?",
                meterOptions: ["40%", "65%", "85%"],
                meterDefault: 2,
                chipActive: Color(hex: "#06B6D4"),
                chipActiveText: .white,
                cta: "Save Pivot",
                ctaColor: Color(hex: "#06B6D4"),
                ctaText: .white
            )
        case .progress:
            return FutureSheetCopy(
                title: "Build Momentum",
                subtitle: "Record something that moved you forward.",
                heroTitle: "Progress",
                heroBody: "Mark forward motion on a goal or habit.",
                heroGlyph: "↗",
                heroIconBg: Color(hex: "#047857"),
                heroTitleColor: Color(hex: "#10B981"),
                noteLabel: "WHAT MOVED FORWARD?",
                notePlaceholder: "Shipped a feature, closed a deal, finished a module…",
                primaryLabel: "PROGRESS TYPE",
                primaryOptions: [
                    "Skill", "Project", "Habit", "Revenue", "Fitness", "Learning Goal",
                ],
                secondaryLabel: "STATUS",
                secondaryOptions: ["Started", "Midway", "Nearly Done", "Complete"],
                showMeter: true,
                meterLabel: "HOW FAR?",
                meterOptions: ["25%", "50%", "75%", "100%"],
                meterDefault: 1,
                chipActive: Color(hex: "#10B981"),
                chipActiveText: .white,
                cta: "Save Progress",
                ctaColor: Color(hex: "#10B981"),
                ctaText: .white
            )
        case .learning:
            return FutureSheetCopy(
                title: "Build Momentum",
                subtitle: "Record something that moved you forward.",
                heroTitle: "Learning",
                heroBody: "Capture a growth signal while it's fresh.",
                heroGlyph: "✎",
                heroIconBg: Color(hex: "#4338CA"),
                heroTitleColor: Color(hex: "#6366F1"),
                noteLabel: "WHAT DID YOU LEARN?",
                notePlaceholder: "A concept, skill, or insight you want to keep…",
                primaryLabel: "LEARNING TYPE",
                primaryOptions: [
                    "Course", "Book", "Mentorship", "Practice", "Reflection", "Experiment",
                ],
                secondaryLabel: "DEPTH",
                secondaryOptions: ["Glance", "Solid", "Deep"],
                tertiaryLabel: "APPLY TO",
                tertiaryOptions: ["Career", "Skills", "Network", "Capital", "Mindset"],
                chipActive: Color(hex: "#6366F1"),
                chipActiveText: .white,
                cta: "Save Learning",
                ctaColor: Color(hex: "#6366F1"),
                ctaText: .white
            )
        }
    }

    func progressValue(meter: Int, meterOptions: [String]) -> Double? {
        guard meter >= 0, meter < meterOptions.count else { return nil }
        let raw = meterOptions[meter]
            .replacingOccurrences(of: "%", with: "")
            .trimmingCharacters(in: .whitespaces)
        if let n = Double(raw) { return n }
        switch raw.lowercased() {
        case "low": return 25
        case "moderate": return 50
        case "high": return 75
        case "game changer": return 95
        default: return nil
        }
    }
}
