import SwiftUI

/// Figma Lifestyle quick-add sheets:
/// Experience `433:9723`, Wellbeing `433:9852`, Discovery `433:10057`,
/// Create `433:9969`, Adjust `433:9637`.
struct PersonalLifestyleQuickAddSheet: View {
    let kind: LifestyleQuickAddKind
    let momentId: String
    var onClose: () -> Void
    var onSaved: () -> Void

    @State private var headline = ""
    @State private var selectedPrimary = ""
    @State private var selectedSecondary = ""
    @State private var selectedTertiary = ""
    @State private var selectedQuaternary = ""
    @State private var selectedExtra = ""
    @State private var meter = 0
    @State private var starRating = 4
    @State private var note = ""
    @State private var showNoteField = false
    @State private var submitting = false
    @State private var error: String?
    @State private var draftKey = UUID().uuidString

    private let repository = LifestyleActivityCreateRepository()
    private var copy: LifestyleSheetCopy { kind.sheetCopy }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 14) {
                header
                if copy.showHero { hero }
                formSections
                if let error {
                    Text(error)
                        .font(.plusJakarta(size: 12))
                        .foregroundStyle(Color(hex: "#F87171"))
                }
                saveButton
            }
            .padding(.horizontal, 16)
            .padding(.top, copy.compactHeader ? 8 : 14)
            .padding(.bottom, 20)
        }
        .background(Color(hex: copy.sheetBackground))
        .onAppear(perform: applyDefaults)
        .presentationDetents([.medium, .large])
        .presentationDragIndicator(.visible)
    }

    // MARK: - Form

    @ViewBuilder
    private var formSections: some View {
        switch kind {
        case .experience:
            section(copy.primaryFieldLabel) { textField(placeholder: copy.primaryPlaceholder, text: $headline) }
            section(copy.primaryLabel) { emojiGrid(copy.emojiPrimary, selected: $selectedPrimary) }
            section(copy.secondaryLabel) { faceRow(copy.faceOptions, selected: $selectedSecondary) }
            section(copy.meterLabel) { meterRow(options: copy.meterOptions, labels: copy.meterLabels) }
            section(copy.tertiaryLabel) { chipFlow(copy.tertiaryOptions, selected: $selectedTertiary) }
            section(copy.quaternaryLabel) { chipFlow(copy.quaternaryOptions, selected: $selectedQuaternary) }
            section(copy.noteLabel) { noteField(placeholder: copy.notePlaceholder) }
        case .wellbeing:
            section("How it feels") { noteField(placeholder: "How this part of life feels...", minHeight: 88) }
            section(copy.primaryLabel) { emojiGrid(copy.emojiPrimary, selected: $selectedPrimary) }
            section(copy.meterLabel) { meterRow(options: copy.meterOptions, labels: copy.meterLabels) }
            section(copy.secondaryLabel) { chipFlow(copy.secondaryOptions, selected: $selectedSecondary) }
            section("NOTES") { noteField(placeholder: "Add a note - optional") }
        case .discovery:
            section(copy.primaryFieldLabel) { textField(placeholder: copy.primaryPlaceholder, text: $headline) }
            section(copy.primaryLabel) { emojiGrid(copy.emojiPrimary, selected: $selectedPrimary) }
            section(copy.secondaryLabel) { interestMeter }
            section(copy.tertiaryLabel) { chipFlow(copy.tertiaryOptions, selected: $selectedTertiary) }
            section(copy.noteLabel) { noteField(placeholder: copy.notePlaceholder) }
        case .expression:
            section(copy.primaryFieldLabel) { textField(placeholder: copy.primaryPlaceholder, text: $headline) }
            section(copy.primaryLabel) { chipFlow(copy.primaryOptions, selected: $selectedPrimary) }
            section(copy.secondaryLabel) { starRatingRow }
            section(copy.tertiaryLabel) { chipFlow(copy.tertiaryOptions, selected: $selectedTertiary) }
            section(copy.quaternaryLabel) { flowStateRow }
            if showNoteField {
                section(copy.noteLabel) { noteField(placeholder: copy.notePlaceholder) }
            } else {
                Button { showNoteField = true } label: {
                    Text("Add note - optional")
                        .font(.plusJakarta(size: 14, weight: .semibold))
                        .foregroundStyle(copy.accent)
                        .underline()
                }
                .buttonStyle(.plain)
            }
        case .adjust:
            section(copy.primaryFieldLabel) { textField(placeholder: copy.primaryPlaceholder, text: $headline) }
            section(copy.primaryLabel) { fullWidthEmojiList(copy.emojiPrimary, selected: $selectedPrimary) }
            section(copy.secondaryLabel) { chipFlow(copy.secondaryOptions, selected: $selectedSecondary) }
            section(copy.tertiaryLabel) { chipFlow(copy.tertiaryOptions, selected: $selectedTertiary) }
            section(copy.noteLabel) { noteField(placeholder: copy.notePlaceholder) }
        }
    }

    // MARK: - Chrome

    private var header: some View {
        HStack(alignment: .top) {
            if kind == .discovery {
                HStack(spacing: 12) {
                    Text("🔍")
                        .font(.plusJakarta(size: 18))
                        .frame(width: 48, height: 48)
                        .background(Color(hex: "#1C1926"))
                        .overlay(RoundedRectangle(cornerRadius: 24).stroke(Color.white.opacity(0.1), lineWidth: 1))
                        .clipShape(RoundedRectangle(cornerRadius: 24))
                    VStack(alignment: .leading, spacing: 2) {
                        Text(copy.title)
                            .font(.plusJakarta(size: 24, weight: .heavy))
                            .foregroundStyle(Color(hex: "#E5E0EE"))
                        Text(copy.subtitle)
                            .font(.plusJakarta(size: 13))
                            .foregroundStyle(Color(hex: "#C9C4D8"))
                    }
                }
            } else {
                VStack(alignment: .leading, spacing: 2) {
                    Text(copy.title)
                        .font(.plusJakarta(size: kind == .adjust ? 24 : 20, weight: .heavy))
                        .foregroundStyle(copy.headerTint ?? Color(hex: "#E5E0EE"))
                    Text(copy.subtitle)
                        .font(.plusJakarta(size: kind == .adjust ? 14 : 12))
                        .foregroundStyle(Color(hex: "#C9C4D8"))
                }
            }
            Spacer(minLength: 8)
            Button(action: onClose) {
                Text("×")
                    .font(.plusJakarta(size: 16, weight: .bold))
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
                .font(.plusJakarta(size: 18))
                .foregroundStyle(copy.heroGlyphColor)
                .frame(width: 44, height: 44)
                .background(copy.heroIconBg)
                .overlay(RoundedRectangle(cornerRadius: copy.heroCorner).stroke(Color.white.opacity(0.1), lineWidth: 1))
                .clipShape(RoundedRectangle(cornerRadius: copy.heroCorner))
            VStack(alignment: .leading, spacing: 2) {
                Text(copy.heroTitle)
                    .font(.plusJakarta(size: 18, weight: .bold))
                    .foregroundStyle(copy.heroTitleColor)
                Text(copy.heroBody)
                    .font(.plusJakarta(size: 13))
                    .foregroundStyle(Color(hex: "#C9C4D8"))
            }
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color(hex: "#201E28"))
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(Color.white.opacity(0.08), lineWidth: 1))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    private var saveButton: some View {
        Button(action: save) {
            Text(submitting ? "Saving…" : copy.cta)
                .font(.plusJakarta(size: 15, weight: .heavy))
                .foregroundStyle(copy.ctaText)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 14)
                .background(copy.ctaBackground)
                .clipShape(RoundedRectangle(cornerRadius: 14))
        }
        .buttonStyle(.plain)
        .disabled(submitting || !canSave)
        .opacity(submitting || !canSave ? 0.6 : 1)
    }

    // MARK: - Components

    private func section<Content: View>(_ title: String, @ViewBuilder content: () -> Content) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            Text(title)
                .font(.plusJakarta(size: copy.sectionLabelSize, weight: .bold))
                .tracking(copy.sectionTracking)
                .foregroundStyle(copy.sectionLabelColor ?? copy.accent)
            content()
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color(hex: "#201E28"))
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(Color.white.opacity(0.08), lineWidth: 1))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    private func textField(placeholder: String, text: Binding<String>) -> some View {
        TextField("", text: text, prompt: Text(placeholder).foregroundStyle(Color(hex: "#6B6A73")))
            .font(.plusJakarta(size: 14))
            .foregroundStyle(Color(hex: "#E5E0EE"))
            .padding(.horizontal, 14)
            .padding(.vertical, 12)
            .background(Color(hex: "#14121B"))
            .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color.white.opacity(0.1), lineWidth: 1))
            .clipShape(RoundedRectangle(cornerRadius: 12))
    }

    private func noteField(placeholder: String, minHeight: CGFloat = 72) -> some View {
        ZStack(alignment: .topLeading) {
            if note.isEmpty {
                Text(placeholder)
                    .font(.plusJakarta(size: 13))
                    .foregroundStyle(Color(hex: "#C9C4D8"))
                    .padding(.top, 8)
                    .padding(.leading, 4)
            }
            TextEditor(text: $note)
                .scrollContentBackground(.hidden)
                .font(.plusJakarta(size: 13))
                .foregroundStyle(Color(hex: "#E5E0EE"))
                .frame(minHeight: minHeight)
        }
        .padding(8)
        .background(Color(hex: "#14121B"))
        .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color.white.opacity(0.1), lineWidth: 1))
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }

    private func chipFlow(_ options: [String], selected: Binding<String>) -> some View {
        FlowLayout(spacing: 8) {
            ForEach(options, id: \.self) { option in
                let isOn = selected.wrappedValue == option
                Button { selected.wrappedValue = option } label: {
                    Text(option)
                        .font(.plusJakarta(size: 12, weight: isOn ? .heavy : .semibold))
                        .foregroundStyle(isOn ? copy.chipActiveText : Color(hex: "#E5E0EE"))
                        .padding(.horizontal, 12)
                        .padding(.vertical, 8)
                        .background(isOn ? copy.accent : Color(hex: "#14121B"))
                        .overlay(Capsule().stroke(isOn ? copy.accent : Color.white.opacity(0.1), lineWidth: 1))
                        .clipShape(Capsule())
                }
                .buttonStyle(.plain)
            }
        }
    }

    private func emojiGrid(_ options: [LifestyleEmojiChip], selected: Binding<String>) -> some View {
        VStack(spacing: 10) {
            ForEach(Array(stride(from: 0, to: options.count, by: 2)), id: \.self) { index in
                HStack(spacing: 10) {
                    emojiChip(options[index], selected: selected)
                    if index + 1 < options.count {
                        emojiChip(options[index + 1], selected: selected)
                    } else {
                        Spacer(minLength: 0)
                    }
                }
            }
        }
    }

    private func emojiChip(_ chip: LifestyleEmojiChip, selected: Binding<String>) -> some View {
        let isOn = selected.wrappedValue == chip.label
        return Button { selected.wrappedValue = chip.label } label: {
            HStack(spacing: 8) {
                Text(chip.emoji)
                    .font(.plusJakarta(size: 16))
                Text(chip.label)
                    .font(.plusJakarta(size: 13, weight: isOn ? .bold : .semibold))
                    .foregroundStyle(isOn ? copy.chipActiveText : Color(hex: "#E5E0EE"))
            }
            .frame(maxWidth: .infinity)
            .padding(.horizontal, 12)
            .padding(.vertical, 10)
            .background(isOn ? copy.accent : Color(hex: "#14121B"))
            .overlay(Capsule().stroke(isOn ? copy.accent : Color.white.opacity(0.1), lineWidth: 1))
            .clipShape(Capsule())
        }
        .buttonStyle(.plain)
    }

    private func fullWidthEmojiList(_ options: [LifestyleEmojiChip], selected: Binding<String>) -> some View {
        VStack(spacing: 8) {
            ForEach(options, id: \.label) { chip in
                let isOn = selected.wrappedValue == chip.label
                Button { selected.wrappedValue = chip.label } label: {
                    HStack(spacing: 8) {
                        Text(chip.emoji).font(.plusJakarta(size: 18))
                        Text(chip.label)
                            .font(.plusJakarta(size: 13, weight: isOn ? .bold : .semibold))
                            .foregroundStyle(isOn ? .white : Color(hex: "#BEB9D2"))
                        Spacer()
                    }
                    .padding(.horizontal, 14)
                    .frame(height: 48)
                    .background(isOn ? copy.accent : Color(hex: "#14121B"))
                    .overlay(Capsule().stroke(isOn ? copy.accent : Color.white.opacity(0.1), lineWidth: 1))
                    .clipShape(Capsule())
                }
                .buttonStyle(.plain)
            }
        }
    }

    private func faceRow(_ options: [LifestyleFaceOption], selected: Binding<String>) -> some View {
        HStack(spacing: 10) {
            ForEach(options, id: \.label) { option in
                let isOn = selected.wrappedValue == option.label
                Button { selected.wrappedValue = option.label } label: {
                    VStack(spacing: 6) {
                        Text(option.emoji)
                            .font(.plusJakarta(size: 18))
                        Text(option.label)
                            .font(.plusJakarta(size: 12, weight: isOn ? .bold : .regular))
                            .foregroundStyle(isOn ? copy.accent : Color(hex: "#C9C4D8"))
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 10)
                    .background(isOn ? copy.accent : Color(hex: "#14121B"))
                    .foregroundStyle(isOn ? copy.chipActiveText : Color(hex: "#C9C4D8"))
                    .overlay(Capsule().stroke(isOn ? copy.accent : Color.white.opacity(0.1), lineWidth: 1))
                    .clipShape(Capsule())
                }
                .buttonStyle(.plain)
            }
        }
    }

    private func meterRow(options: [String], labels: [String]) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(spacing: 6) {
                ForEach(options.indices, id: \.self) { index in
                    Rectangle()
                        .fill(meter == index ? copy.accent : Color(hex: "#2E293D"))
                        .frame(height: 6)
                        .onTapGesture { meter = index }
                }
            }
            .clipShape(Capsule())
            HStack {
                ForEach(Array(labels.enumerated()), id: \.offset) { index, label in
                    Text(label)
                        .font(.plusJakarta(size: 12, weight: meter == index ? .bold : .regular))
                        .foregroundStyle(meter == index ? copy.accent : Color(hex: "#6B6A73"))
                        .frame(maxWidth: .infinity, alignment: .leading)
                }
            }
        }
    }

    private var interestMeter: some View {
        VStack(spacing: 10) {
            HStack(spacing: 10) {
                ForEach(copy.interestSizes.indices, id: \.self) { index in
                    let size = copy.interestSizes[index]
                    Circle()
                        .fill(meter == index ? copy.accent : Color.clear)
                        .overlay(Circle().stroke(meter == index ? copy.accent : Color.white.opacity(0.1), lineWidth: 1))
                        .frame(width: size, height: size)
                        .onTapGesture { meter = index }
                }
            }
            HStack {
                ForEach(Array(copy.meterLabels.enumerated()), id: \.offset) { index, label in
                    Text(label)
                        .font(.plusJakarta(size: 12, weight: meter == index ? .bold : .semibold))
                        .foregroundStyle(meter == index ? copy.accent : Color(hex: "#C9C4D8"))
                        .frame(maxWidth: .infinity)
                }
            }
        }
    }

    private var starRatingRow: some View {
        HStack {
            HStack(spacing: 6) {
                ForEach(1...5, id: \.self) { star in
                    Button { starRating = star } label: {
                        Text(star <= starRating ? "★" : "☆")
                            .font(.plusJakarta(size: 18))
                            .foregroundStyle(star <= starRating ? copy.accent : Color(hex: "#6B7280"))
                    }
                    .buttonStyle(.plain)
                }
            }
            Spacer()
            Text("\(starRating)/5 · \(copy.starLabels[min(starRating - 1, copy.starLabels.count - 1)])")
                .font(.plusJakarta(size: 13, weight: .semibold))
                .foregroundStyle(copy.accent)
        }
    }

    private var flowStateRow: some View {
        HStack(spacing: 12) {
            ForEach(copy.flowOptions, id: \.self) { option in
                let isOn = selectedExtra == option
                Button { selectedExtra = option } label: {
                    Group {
                        if option == "Yes" {
                            Text("✓")
                                .font(.plusJakarta(size: 16, weight: .bold))
                        } else {
                            Text(option)
                                .font(.plusJakarta(size: 13, weight: .semibold))
                        }
                    }
                    .foregroundStyle(isOn ? .white : Color(hex: "#9CA3AF"))
                    .frame(maxWidth: .infinity)
                    .frame(height: 44)
                    .background(isOn ? copy.accent : Color(hex: "#14121B"))
                    .overlay(RoundedRectangle(cornerRadius: 12).stroke(isOn ? copy.accent : Color.white.opacity(0.1), lineWidth: 1))
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                }
                .buttonStyle(.plain)
            }
        }
    }

    // MARK: - Save

    private var canSave: Bool {
        switch kind {
        case .experience, .discovery, .expression, .adjust:
            return !selectedPrimary.isEmpty
        case .wellbeing:
            return !selectedPrimary.isEmpty
        }
    }

    private func applyDefaults() {
        selectedPrimary = copy.defaultPrimary
        selectedSecondary = copy.defaultSecondary
        selectedTertiary = copy.defaultTertiary
        selectedQuaternary = copy.defaultQuaternary
        selectedExtra = copy.defaultExtra
        meter = copy.meterDefault
        starRating = 4
    }

    private func save() {
        guard !submitting, canSave else { return }
        submitting = true
        error = nil
        let payload = kind.buildPayload(
            headline: headline,
            primary: selectedPrimary,
            secondary: selectedSecondary,
            tertiary: selectedTertiary,
            quaternary: selectedQuaternary,
            extra: selectedExtra,
            meter: meter,
            meterLabels: copy.meterLabels,
            starRating: starRating,
            note: note
        )
        Task {
            do {
                _ = try await repository.createLifestyleActivity(
                    draftKey: draftKey,
                    momentId: momentId,
                    lifestyleContext: kind.apiContext,
                    title: payload.title,
                    description: payload.description,
                    wellbeingRating: payload.wellbeingRating
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
}

// MARK: - Copy

private struct LifestyleEmojiChip {
    let emoji: String
    let label: String
}

private struct LifestyleFaceOption {
    let emoji: String
    let label: String
}

private struct LifestyleSavePayload {
    let title: String
    let description: String
    let wellbeingRating: Double?
}

private struct LifestyleSheetCopy {
    let title: String
    let subtitle: String
    var headerTint: Color? = nil
    var sheetBackground = "#14121B"
    var compactHeader = false
    var showHero = true
    let heroTitle: String
    let heroBody: String
    let heroGlyph: String
    var heroGlyphColor: Color = .white
    let heroIconBg: Color
    let heroTitleColor: Color
    var heroCorner: CGFloat = 14
    var primaryFieldLabel = ""
    var primaryPlaceholder = ""
    var primaryLabel = ""
    var primaryOptions: [String] = []
    var emojiPrimary: [LifestyleEmojiChip] = []
    var secondaryLabel = ""
    var secondaryOptions: [String] = []
    var tertiaryLabel = ""
    var tertiaryOptions: [String] = []
    var quaternaryLabel = ""
    var quaternaryOptions: [String] = []
    var noteLabel = "ADD NOTE - OPTIONAL"
    var notePlaceholder = "Add note - optional"
    var meterLabel = ""
    var meterOptions: [String] = []
    var meterLabels: [String] = []
    var meterDefault = 0
    var interestSizes: [CGFloat] = []
    var flowOptions: [String] = []
    var starLabels: [String] = []
    var sectionLabelSize: CGFloat = 11
    var sectionTracking: CGFloat = 0.8
    var sectionLabelColor: Color? = nil
    let accent: Color
    let chipActiveText: Color
    let cta: String
    let ctaBackground: AnyShapeStyle
    let ctaText: Color
    var defaultPrimary = ""
    var defaultSecondary = ""
    var defaultTertiary = ""
    var defaultQuaternary = ""
    var defaultExtra = ""
}

private extension LifestyleQuickAddKind {
    var sheetCopy: LifestyleSheetCopy {
        switch self {
        case .experience:
            return LifestyleSheetCopy(
                title: "Capture Lifestyle",
                subtitle: "Record what shapes how you live.",
                heroTitle: "Experience",
                heroBody: "Save a memorable moment.",
                heroGlyph: "✨",
                heroIconBg: Color(hex: "#1C1926"),
                heroTitleColor: Color(hex: "#F8F7F9"),
                primaryFieldLabel: "WHAT DID YOU EXPERIENCE?",
                primaryPlaceholder: "Weekend hike, live music night...",
                primaryLabel: "EXPERIENCE TYPE",
                emojiPrimary: [
                    .init(emoji: "✈️", label: "Travel"), .init(emoji: "🍽️", label: "Food"),
                    .init(emoji: "🌿", label: "Nature"), .init(emoji: "🏔️", label: "Adventure"),
                    .init(emoji: "🎭", label: "Entertainment"), .init(emoji: "👥", label: "Social"),
                    .init(emoji: "👨‍👩‍👧", label: "Family"), .init(emoji: "🧘", label: "Personal"),
                    .init(emoji: "🎨", label: "Hobby"), .init(emoji: "🤔", label: "Other"),
                ],
                secondaryLabel: "HOW WAS IT?",
                tertiaryLabel: "WHO WERE YOU WITH?",
                tertiaryOptions: ["Alone", "Partner", "Friends", "Family", "Group"],
                quaternaryLabel: "WHAT DID YOU GET FROM IT?",
                quaternaryOptions: ["Not Worth It", "Okay", "Worth It", "Excellent Value", "Life Enriching"],
                meterLabel: "HOW DID IT AFFECT YOUR ENERGY?",
                meterOptions: ["1", "2", "3", "4"],
                meterLabels: ["Drained", "Neutral", "Refreshed", "Energized"],
                meterDefault: 2,
                accent: Color(hex: "#EC4899"),
                chipActiveText: Color(hex: "#14121B"),
                cta: "Save Experience",
                ctaBackground: AnyShapeStyle(Color(hex: "#EC4899")),
                ctaText: Color(hex: "#14121B"),
                defaultPrimary: "Travel",
                defaultSecondary: "Memorable",
                defaultTertiary: "Partner",
                defaultQuaternary: "Life Enriching"
            )
        case .wellbeing:
            return LifestyleSheetCopy(
                title: "Capture Lifestyle",
                subtitle: "Record what shapes how you live.",
                heroTitle: "Wellbeing",
                heroBody: "Check-in on a life area.",
                heroGlyph: "💜",
                heroIconBg: Color(hex: "#A78BFA"),
                heroTitleColor: .white,
                primaryLabel: "WHICH LIFE AREA?",
                emojiPrimary: [
                    .init(emoji: "❤️", label: "Health"), .init(emoji: "💕", label: "Relationships"),
                    .init(emoji: "💼", label: "Work"), .init(emoji: "💰", label: "Money"),
                    .init(emoji: "🏠", label: "Home"), .init(emoji: "👥", label: "Social"),
                    .init(emoji: "😴", label: "Rest"), .init(emoji: "📈", label: "Growth"),
                ],
                secondaryLabel: "WHAT IS SHAPING THIS?",
                secondaryOptions: ["Sleep", "Workload", "Relationships", "Money", "Health", "Environment", "Routine"],
                meterLabel: "HOW DOES THIS AREA FEEL RIGHT NOW?",
                meterOptions: ["1", "2", "3", "4"],
                meterLabels: ["Low", "Moderate", "Good", "Excellent"],
                meterDefault: 2,
                accent: Color(hex: "#A78BFA"),
                chipActiveText: .white,
                cta: "Save Wellbeing Check-in",
                ctaBackground: AnyShapeStyle(Color(hex: "#A78BFA")),
                ctaText: .white,
                defaultPrimary: "Health",
                defaultSecondary: "Sleep"
            )
        case .discovery:
            return LifestyleSheetCopy(
                title: "Discovery",
                subtitle: "Capture a new curiosity.",
                compactHeader: true,
                showHero: false,
                heroTitle: "Discovery",
                heroBody: "",
                heroGlyph: "🔍",
                heroIconBg: Color(hex: "#1C1926"),
                heroTitleColor: Color(hex: "#E5E0EE"),
                primaryFieldLabel: "WHAT DID YOU DISCOVER?",
                primaryPlaceholder: "A new podcast, technique, idea...",
                primaryLabel: "DISCOVERY TYPE",
                emojiPrimary: [
                    .init(emoji: "📝", label: "Article"), .init(emoji: "🎬", label: "Video"),
                    .init(emoji: "🎙️", label: "Podcast"), .init(emoji: "📚", label: "Book"),
                    .init(emoji: "💻", label: "Course"), .init(emoji: "🔧", label: "Tool"),
                    .init(emoji: "👤", label: "Person"), .init(emoji: "📍", label: "Place"),
                ],
                secondaryLabel: "HOW INTERESTING WAS IT?",
                tertiaryLabel: "WILL YOU EXPLORE FURTHER?",
                tertiaryOptions: ["Maybe", "Likely", "Definitely"],
                noteLabel: "ADD NOTE - OPTIONAL",
                notePlaceholder: "Add a quick note...",
                meterOptions: ["1", "2", "3", "4"],
                meterLabels: ["Mildly", "Interesting", "Very", "Mind-blowing"],
                meterDefault: 2,
                interestSizes: [28, 34, 40, 46],
                sectionLabelColor: Color(hex: "#C026D3"),
                accent: Color(hex: "#C026D3"),
                chipActiveText: .white,
                cta: "Save Discovery",
                ctaBackground: AnyShapeStyle(Color(hex: "#C026D3")),
                ctaText: .white,
                defaultPrimary: "Podcast",
                defaultTertiary: "Likely"
            )
        case .expression:
            return LifestyleSheetCopy(
                title: "Capture Lifestyle",
                subtitle: "Record what shapes how you live.",
                headerTint: Color(hex: "#F43F5E"),
                heroTitle: "Create",
                heroBody: "Record something you made.",
                heroGlyph: "🎨",
                heroIconBg: Color(hex: "#1C1926"),
                heroTitleColor: Color(hex: "#F43F5E"),
                heroCorner: 12,
                primaryFieldLabel: "WHAT DID YOU CREATE?",
                primaryPlaceholder: "Landing page, sketch, recipe...",
                primaryLabel: "CREATION TYPE",
                primaryOptions: ["Writing", "Art", "Music", "Design", "Photography", "Problem Solving", "Planning", "Content", "Other"],
                secondaryLabel: "HOW SATISFIED ARE YOU?",
                tertiaryLabel: "TIME INVESTED",
                tertiaryOptions: ["Under 30 min", "30-60 min", "1-2 hours", "2+ hours"],
                quaternaryLabel: "DID YOU REACH FLOW STATE?",
                flowOptions: ["No", "Partially", "Yes"],
                starLabels: ["Poor", "Fair", "Good", "Great", "Exceptional"],
                accent: Color(hex: "#F43F5E"),
                chipActiveText: .white,
                cta: "Save Creation",
                ctaBackground: AnyShapeStyle(Color(hex: "#F43F5E")),
                ctaText: .white,
                defaultPrimary: "Art",
                defaultTertiary: "1-2 hours",
                defaultExtra: "Yes"
            )
        case .adjust:
            return LifestyleSheetCopy(
                title: "Capture Lifestyle",
                subtitle: "Record what shapes how you live.",
                headerTint: Color(hex: "#6366F1"),
                sheetBackground: "#0B0A12",
                heroTitle: "Adjust",
                heroBody: "Change your lifestyle priorities and habits.",
                heroGlyph: "🎚️",
                heroIconBg: Color(hex: "#6366F1"),
                heroTitleColor: .white,
                heroCorner: 12,
                primaryFieldLabel: "WHAT TO CHANGE?",
                primaryPlaceholder: "Spend less eating out, cook more...",
                primaryLabel: "WHAT AREA DO YOU WANT TO ADJUST?",
                emojiPrimary: [
                    .init(emoji: "😴", label: "More Rest"), .init(emoji: "✈️", label: "More Travel"),
                    .init(emoji: "🎨", label: "More Creativity"), .init(emoji: "👥", label: "More Social Time"),
                    .init(emoji: "🏋️", label: "More Exercise"), .init(emoji: "⏰", label: "More Personal Time"),
                    .init(emoji: "🌍", label: "More Exploration"), .init(emoji: "⚖️", label: "More Balance"),
                ],
                secondaryLabel: "HOW IMPORTANT IS THIS CHANGE?",
                secondaryOptions: ["Low", "Medium", "High"],
                tertiaryLabel: "HOW CONFIDENT ARE YOU?",
                tertiaryOptions: ["Not Sure", "Somewhat Sure", "Very Sure"],
                noteLabel: "Add note - optional",
                notePlaceholder: "Add note - optional",
                accent: Color(hex: "#6366F1"),
                chipActiveText: .white,
                cta: "Update Lifestyle",
                ctaBackground: AnyShapeStyle(Color(hex: "#6366F1")),
                ctaText: .white,
                defaultPrimary: "More Creativity",
                defaultSecondary: "High",
                defaultTertiary: "Very Sure"
            )
        }
    }

    func buildPayload(
        headline: String,
        primary: String,
        secondary: String,
        tertiary: String,
        quaternary: String,
        extra: String,
        meter: Int,
        meterLabels: [String],
        starRating: Int,
        note: String
    ) -> LifestyleSavePayload {
        let trimmedHeadline = headline.trimmingCharacters(in: .whitespacesAndNewlines)
        let trimmedNote = note.trimmingCharacters(in: .whitespacesAndNewlines)
        var parts: [String] = []
        switch self {
        case .experience:
            parts = [primary, secondary, tertiary, quaternary]
            if meter >= 0, meter < meterLabels.count {
                parts.append(meterLabels[min(meter, meterLabels.count - 1)])
            }
        case .wellbeing:
            parts = [primary, secondary]
            if meter >= 0, meter < meterLabels.count { parts.append(meterLabels[meter]) }
        case .discovery:
            parts = [primary]
            if meter >= 0, meter < meterLabels.count { parts.append(meterLabels[meter]) }
            if !tertiary.isEmpty { parts.append(tertiary) }
        case .expression:
            parts = [primary, "\(starRating)/5", tertiary, extra]
        case .adjust:
            parts = [primary, secondary, tertiary]
        }
        let description = parts.filter { !$0.isEmpty }.joined(separator: " · ")
        let titleSource = trimmedHeadline.isEmpty ? (trimmedNote.isEmpty ? primary : trimmedNote) : trimmedHeadline
        let title = String(titleSource.prefix(120))
        let rating: Double? = {
            switch self {
            case .experience:
                switch secondary {
                case "Ordinary": return 5
                case "Enjoyable": return 7
                case "Memorable": return 8
                case "Exceptional": return 9.5
                default: return nil
                }
            case .wellbeing:
                if meter >= 0, meter < meterLabels.count {
                    switch meterLabels[meter] {
                    case "Low": return 4
                    case "Moderate": return 6
                    case "Good": return 8
                    case "Excellent": return 9.5
                    default: return 7
                    }
                }
                return 7
            case .expression:
                return Double(starRating * 2)
            default:
                return nil
            }
        }()
        var fullDescription = description
        if !trimmedNote.isEmpty, trimmedNote != title { fullDescription += " · \(trimmedNote)" }
        return LifestyleSavePayload(title: title, description: fullDescription, wellbeingRating: rating)
    }
}

private extension LifestyleSheetCopy {
    var faceOptions: [LifestyleFaceOption] {
        [
            .init(emoji: "😐", label: "Ordinary"),
            .init(emoji: "😊", label: "Enjoyable"),
            .init(emoji: "🤩", label: "Memorable"),
            .init(emoji: "🌟", label: "Exceptional"),
        ]
    }
}
