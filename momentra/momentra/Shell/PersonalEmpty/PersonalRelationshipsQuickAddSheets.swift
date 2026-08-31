import SwiftUI

/// Figma Relationships quick-add sheets:
/// Adjust `439:9468`, Support `439:9574`, Connection `439:9666`,
/// Shared Exp `439:9798`, Investment `439:9907`.
struct PersonalRelationshipsQuickAddSheet: View {
    let kind: RelationshipsQuickAddKind
    let momentId: String
    var onClose: () -> Void
    var onSaved: () -> Void

    @State private var headline = ""
    @State private var contextLine = ""
    @State private var selectedPrimary = ""
    @State private var selectedSecondary = ""
    @State private var selectedTertiary = ""
    @State private var selectedQuaternary = ""
    @State private var meter = 0
    @State private var timeMeter = 3
    @State private var note = ""
    @State private var showNoteField = false
    @State private var submitting = false
    @State private var error: String?
    @State private var draftKey = UUID().uuidString

    private let repository = RelationshipActivityCreateRepository()
    private var copy: RelationshipsSheetCopy { kind.sheetCopy }
    private let accent = Color(hex: "#E12A9E")

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 14) {
                header
                if copy.showHero { hero }
                formSections
                if showNoteField || copy.alwaysShowNote {
                    if copy.noteSectionStyle == .card {
                        section(copy.noteLabel) { noteField(placeholder: copy.notePlaceholder) }
                    }
                } else if copy.noteSectionStyle == .link {
                    Button { showNoteField = true } label: {
                        Text("+ Add note - optional")
                            .font(.plusJakarta(size: 14, weight: .semibold))
                            .foregroundStyle(accent)
                    }
                    .buttonStyle(.plain)
                }
                if let error {
                    Text(error)
                        .font(.plusJakarta(size: 12))
                        .foregroundStyle(Color(hex: "#F87171"))
                }
                saveButton
            }
            .padding(.horizontal, 16)
            .padding(.top, 14)
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
        case .adjust:
            section(copy.primaryFieldLabel) { textField(placeholder: copy.primaryPlaceholder, text: $headline) }
            section(copy.primaryLabel) { emojiGrid(copy.emojiPrimary, selected: $selectedPrimary) }
            section(copy.secondaryLabel) { chipFlow(copy.secondaryOptions, selected: $selectedSecondary) }
            section(copy.tertiaryLabel) { segmentedRow(copy.tertiaryOptions, selected: $selectedTertiary, outlined: true) }
            section(copy.quaternaryLabel) { segmentedRow(copy.quaternaryOptions, selected: $selectedQuaternary, outlined: true) }
        case .support:
            section(copy.primaryFieldLabel) { textField(placeholder: copy.primaryPlaceholder, text: $headline) }
            section(copy.primaryLabel) { emojiGrid(copy.emojiPrimary, selected: $selectedPrimary) }
            section(copy.secondaryLabel) {
                segmentedRow(copy.secondaryOptions, selected: $selectedSecondary, useCard: true)
            }
            section(copy.tertiaryLabel) {
                segmentedRow(copy.tertiaryOptions, selected: $selectedTertiary, useCard: true)
            }
        case .connection:
            iconSection("👤", title: "WHO?") {
                textField(placeholder: "Who did you connect with?", text: $headline)
                textField(placeholder: "Caught up after several months", text: $contextLine)
            }
            iconSection("🤝", title: "CONNECTION TYPE") {
                emojiGrid(copy.emojiPrimary, selected: $selectedPrimary)
            }
            iconSection("💕", title: "RELATIONSHIP TYPE") {
                chipFlow(copy.secondaryOptions, selected: $selectedSecondary, includeEmoji: true)
            }
            iconSection("💫", title: "HOW CONNECTED DID IT FEEL?") {
                connectionDepthMeter
            }
            iconSection("🎭", title: "WHAT WAS THE TONE?") {
                chipFlow(copy.tertiaryOptions, selected: $selectedTertiary, includeEmoji: true)
            }
            iconSection("⏱️", title: "TIME INVESTED") {
                chipFlow(copy.quaternaryOptions, selected: $selectedQuaternary)
                    .onChange(of: selectedQuaternary) { _, newValue in
                        if let index = copy.quaternaryOptions.firstIndex(of: newValue) {
                            timeMeter = index
                        }
                    }
                timeTimeline(index: timeMeter, total: copy.quaternaryOptions.count)
            }
            iconSection("✍️", title: "NOTES") {
                noteField(placeholder: "Why did this connection matter?")
            }
        case .shared:
            section(copy.primaryFieldLabel) { textField(placeholder: copy.primaryPlaceholder, text: $headline) }
            section(copy.primaryLabel) { emojiGrid(copy.emojiPrimary, selected: $selectedPrimary) }
            section(copy.secondaryLabel) { chipFlow(copy.secondaryOptions, selected: $selectedSecondary) }
            section(copy.tertiaryLabel) { faceRow(selected: $selectedTertiary) }
            section(copy.quaternaryLabel) { chipFlow(copy.quaternaryOptions, selected: $selectedQuaternary) }
            section(copy.noteLabel) { noteField(placeholder: copy.notePlaceholder) }
        case .investment:
            section(copy.primaryFieldLabel) { textField(placeholder: copy.primaryPlaceholder, text: $headline) }
            section(copy.primaryLabel) { chipFlow(copy.primaryOptions, selected: $selectedPrimary, includeEmoji: true) }
            section(copy.secondaryLabel) { chipFlow(copy.secondaryOptions, selected: $selectedSecondary) }
            section(copy.tertiaryLabel) { chipFlow(copy.tertiaryOptions, selected: $selectedTertiary) }
            section(copy.quaternaryLabel) { segmentedRow(copy.quaternaryOptions, selected: $selectedQuaternary, useCard: true) }
        }
    }

    // MARK: - Chrome

    private var header: some View {
        HStack(alignment: .top) {
            VStack(alignment: .leading, spacing: 2) {
                Text(copy.screenTitle)
                    .font(.plusJakarta(size: copy.useLargeTitle ? 24 : 22, weight: .heavy))
                    .foregroundStyle(copy.pinkTitle ? accent : Color(hex: "#E5E0EE"))
                if !copy.screenSubtitle.isEmpty {
                    Text(copy.screenSubtitle)
                        .font(.plusJakarta(size: 13))
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
                .frame(width: 44, height: 44)
                .background(copy.heroIconBg)
                .overlay(RoundedRectangle(cornerRadius: 12).stroke(copy.heroBorder ?? Color.white.opacity(0.1), lineWidth: 1))
                .clipShape(RoundedRectangle(cornerRadius: 12))
            VStack(alignment: .leading, spacing: 2) {
                Text(copy.heroTitle)
                    .font(.plusJakarta(size: copy.heroTitleSize, weight: .bold))
                    .foregroundStyle(Color(hex: "#E5E0EE"))
                Text(copy.heroBody)
                    .font(.plusJakarta(size: 13))
                    .foregroundStyle(Color(hex: "#A39EB9"))
            }
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color(hex: "#1C1926"))
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(Color.white.opacity(0.08), lineWidth: 1))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    private var saveButton: some View {
        Button(action: save) {
            HStack(spacing: 8) {
                Text(submitting ? "Saving…" : copy.cta)
                    .font(.plusJakarta(size: 15, weight: .heavy))
                if copy.ctaSparkle { Text("✨").font(.plusJakarta(size: 14)) }
            }
            .foregroundStyle(.white)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 14)
            .background(accent)
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
                .font(.plusJakarta(size: 11, weight: .bold))
                .tracking(0.8)
                .foregroundStyle(accent)
            content()
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color(hex: "#1C1926"))
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(Color.white.opacity(0.08), lineWidth: 1))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    private func iconSection<Content: View>(_ glyph: String, title: String, @ViewBuilder content: () -> Content) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(spacing: 10) {
                Text(glyph)
                    .font(.plusJakarta(size: 16))
                    .frame(width: 32, height: 32)
                    .background(accent.opacity(0.1))
                    .overlay(RoundedRectangle(cornerRadius: 16).stroke(accent, lineWidth: 1))
                    .clipShape(RoundedRectangle(cornerRadius: 16))
                Text(title)
                    .font(.plusJakarta(size: 14, weight: .bold))
                    .foregroundStyle(.white)
            }
            content()
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color(hex: "#1C1926"))
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(Color(hex: "#2A2538"), lineWidth: 1))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    private func textField(placeholder: String, text: Binding<String>) -> some View {
        TextField("", text: text, prompt: Text(placeholder).foregroundStyle(Color(hex: "#A39EB9")))
            .font(.plusJakarta(size: 14))
            .foregroundStyle(Color(hex: "#E5E0EE"))
            .padding(.horizontal, 14)
            .padding(.vertical, 12)
            .background(Color(hex: "#14121B"))
            .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color.white.opacity(0.1), lineWidth: 1))
            .clipShape(RoundedRectangle(cornerRadius: 12))
    }

    private func noteField(placeholder: String) -> some View {
        ZStack(alignment: .topLeading) {
            if note.isEmpty {
                Text(placeholder)
                    .font(.plusJakarta(size: 13))
                    .foregroundStyle(Color(hex: "#A39EB9"))
                    .padding(.top, 8)
                    .padding(.leading, 4)
            }
            TextEditor(text: $note)
                .scrollContentBackground(.hidden)
                .font(.plusJakarta(size: 13))
                .foregroundStyle(Color(hex: "#E5E0EE"))
                .frame(minHeight: 88)
        }
        .padding(8)
        .background(Color(hex: "#14121B"))
        .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color.white.opacity(0.1), lineWidth: 1))
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }

    private func chipFlow(_ options: [String], selected: Binding<String>, includeEmoji: Bool = false) -> some View {
        FlowLayout(spacing: 8) {
            ForEach(options, id: \.self) { option in
                let isOn = selected.wrappedValue == option
                Button { selected.wrappedValue = option } label: {
                    Text(option)
                        .font(.plusJakarta(size: 13, weight: isOn ? .bold : .medium))
                        .foregroundStyle(isOn ? .white : Color(hex: "#A39EB9"))
                        .padding(.horizontal, includeEmoji ? 16 : 14)
                        .padding(.vertical, includeEmoji ? 10 : 8)
                        .background(isOn ? accent : Color(hex: "#14121B"))
                        .overlay(Capsule().stroke(isOn ? accent : Color.white.opacity(0.1), lineWidth: 1))
                        .clipShape(Capsule())
                }
                .buttonStyle(.plain)
            }
        }
    }

    private func emojiGrid(_ options: [RelationshipsEmojiChip], selected: Binding<String>) -> some View {
        VStack(spacing: 12) {
            ForEach(Array(stride(from: 0, to: options.count, by: 2)), id: \.self) { index in
                HStack(spacing: 12) {
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

    private func emojiChip(_ chip: RelationshipsEmojiChip, selected: Binding<String>) -> some View {
        let isOn = selected.wrappedValue == chip.label
        return Button { selected.wrappedValue = chip.label } label: {
            HStack(spacing: chip.compact ? 6 : 10) {
                Text(chip.emoji).font(.plusJakarta(size: 16))
                Text(chip.label)
                    .font(.plusJakarta(size: 13, weight: isOn ? .bold : .semibold))
                    .foregroundStyle(isOn ? .white : Color(hex: "#A39EB9"))
                    .lineLimit(1)
                    .minimumScaleFactor(0.85)
            }
            .frame(maxWidth: .infinity)
            .padding(.horizontal, 12)
            .padding(.vertical, chip.compact ? 6 : 10)
            .background(isOn ? accent : Color(hex: "#0F0E14"))
            .overlay(
                RoundedRectangle(cornerRadius: chip.compact ? 999 : 16)
                    .stroke(isOn ? accent : Color.white.opacity(0.1), lineWidth: 1)
            )
            .clipShape(RoundedRectangle(cornerRadius: chip.compact ? 999 : 16))
        }
        .buttonStyle(.plain)
    }

    private func segmentedRow(_ options: [String], selected: Binding<String>, outlined: Bool = false, useCard: Bool = false) -> some View {
        Group {
            if useCard {
                HStack(spacing: 8) {
                    ForEach(options, id: \.self) { option in
                        segmentButton(option, selected: selected, outlined: outlined, flex: option.count > 10)
                    }
                }
                .padding(16)
                .background(Color(hex: "#1C1926"))
                .overlay(RoundedRectangle(cornerRadius: 16).stroke(Color.white.opacity(0.08), lineWidth: 1))
                .clipShape(RoundedRectangle(cornerRadius: 16))
            } else {
                HStack(spacing: 8) {
                    ForEach(options, id: \.self) { option in
                        segmentButton(option, selected: selected, outlined: outlined, flex: false)
                    }
                }
            }
        }
    }

    private func segmentButton(_ option: String, selected: Binding<String>, outlined: Bool, flex: Bool) -> some View {
        let isOn = selected.wrappedValue == option
        return Button { selected.wrappedValue = option } label: {
            Text(option)
                .font(.plusJakarta(size: 13, weight: isOn ? .bold : .semibold))
                .foregroundStyle(isOn ? (outlined ? accent : .white) : Color(hex: "#A39EB9"))
                .frame(maxWidth: flex ? .infinity : nil)
                .frame(maxWidth: .infinity)
                .frame(height: 40)
                .background(isOn ? (outlined ? accent.opacity(0.08) : accent) : Color(hex: "#0F0E14"))
                .overlay(
                    RoundedRectangle(cornerRadius: outlined ? 12 : 999)
                        .stroke(isOn ? accent : Color.white.opacity(0.1), lineWidth: 1)
                )
                .clipShape(RoundedRectangle(cornerRadius: outlined ? 12 : 999))
        }
        .buttonStyle(.plain)
    }

    private func faceRow(selected: Binding<String>) -> some View {
        HStack(spacing: 12) {
            ForEach(copy.faceOptions, id: \.label) { face in
                let isOn = selected.wrappedValue == face.label
                Button { selected.wrappedValue = face.label } label: {
                    VStack(spacing: 6) {
                        Text(face.emoji).font(.plusJakarta(size: 18))
                        Text(face.label)
                            .font(.plusJakarta(size: 11, weight: isOn ? .bold : .regular))
                            .foregroundStyle(isOn ? accent : Color(hex: "#C9C4D8"))
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 8)
                    .background(isOn ? accent.opacity(0.1) : Color(hex: "#14121B"))
                    .overlay(RoundedRectangle(cornerRadius: 22).stroke(isOn ? accent : Color.white.opacity(0.1), lineWidth: 1))
                    .clipShape(RoundedRectangle(cornerRadius: 22))
                }
                .buttonStyle(.plain)
            }
        }
    }

    private var connectionDepthMeter: some View {
        VStack(spacing: 12) {
            GeometryReader { geo in
                ZStack(alignment: .leading) {
                    Capsule().fill(Color.white.opacity(0.08)).frame(height: 6)
                    Capsule()
                        .fill(accent)
                        .frame(width: geo.size.width * CGFloat(meter + 1) / 4.0, height: 6)
                }
            }
            .frame(height: 6)
            HStack(spacing: 12) {
                ForEach(copy.depthLevels.indices, id: \.self) { index in
                    let level = copy.depthLevels[index]
                    let isOn = meter == index
                    Button { meter = index } label: {
                        VStack(spacing: 8) {
                            Text("\(index + 1)")
                                .font(.plusJakarta(size: 12, weight: .bold))
                                .foregroundStyle(.white)
                                .frame(width: level.size, height: level.size)
                                .background(isOn ? accent : Color(hex: "#14121B"))
                                .overlay(Circle().stroke(isOn ? accent : Color(hex: "#2A2538"), lineWidth: 1))
                                .clipShape(Circle())
                            Text(level.label)
                                .font(.plusJakarta(size: 12, weight: isOn ? .bold : .regular))
                                .foregroundStyle(isOn ? accent : Color(hex: "#A099B0"))
                        }
                        .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.plain)
                }
            }
        }
    }

    private func timeTimeline(index: Int, total: Int) -> some View {
        VStack(spacing: 4) {
            GeometryReader { geo in
                ZStack(alignment: .leading) {
                    Capsule().fill(Color.white.opacity(0.08)).frame(height: 6)
                    Capsule()
                        .fill(accent)
                        .frame(width: geo.size.width * CGFloat(index + 1) / CGFloat(max(total, 1)), height: 6)
                }
            }
            .frame(height: 6)
            HStack {
                ForEach(["5m", "15m", "30m", "1h", "2+"], id: \.self) { label in
                    Text(label)
                        .font(.plusJakarta(size: 12))
                        .foregroundStyle(Color(hex: "#A099B0"))
                        .frame(maxWidth: .infinity)
                }
            }
        }
        .padding(.top, 4)
    }

    // MARK: - Save

    private var canSave: Bool {
        switch kind {
        case .connection:
            return !selectedPrimary.isEmpty
        default:
            return !selectedPrimary.isEmpty
        }
    }

    private func applyDefaults() {
        selectedPrimary = copy.defaultPrimary
        selectedSecondary = copy.defaultSecondary
        selectedTertiary = copy.defaultTertiary
        selectedQuaternary = copy.defaultQuaternary
        meter = copy.meterDefault
        timeMeter = copy.quaternaryOptions.firstIndex(of: copy.defaultQuaternary) ?? 3
        showNoteField = copy.alwaysShowNote
    }

    private func save() {
        guard !submitting, canSave else { return }
        submitting = true
        error = nil
        let payload = kind.buildPayload(
            headline: headline,
            contextLine: contextLine,
            primary: selectedPrimary,
            secondary: selectedSecondary,
            tertiary: selectedTertiary,
            quaternary: selectedQuaternary,
            meter: meter,
            depthLabels: copy.depthLevels.map(\.label),
            note: note
        )
        Task {
            do {
                _ = try await repository.createRelationshipActivity(
                    draftKey: draftKey,
                    momentId: momentId,
                    activityKind: kind.apiKind,
                    displayName: payload.displayName,
                    note: payload.note
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

private struct RelationshipsEmojiChip {
    let emoji: String
    let label: String
    var compact: Bool = false
}

private struct RelationshipsDepthLevel {
    let label: String
    let size: CGFloat
}

private struct RelationshipsFaceOption {
    let emoji: String
    let label: String
}

private enum RelationshipsNoteStyle {
    case card
    case link
    case none
}

private struct RelationshipsSheetCopy {
    let screenTitle: String
    var screenSubtitle = ""
    var pinkTitle = true
    var useLargeTitle = true
    var sheetBackground = "#14121B"
    var showHero = true
    let heroTitle: String
    let heroBody: String
    let heroGlyph: String
    var heroIconBg: Color = Color(hex: "#E12A9E").opacity(0.08)
    var heroBorder: Color? = Color(hex: "#E12A9E")
    var heroTitleSize: CGFloat = 16
    var primaryFieldLabel = ""
    var primaryPlaceholder = ""
    var primaryLabel = ""
    var primaryOptions: [String] = []
    var emojiPrimary: [RelationshipsEmojiChip] = []
    var secondaryLabel = ""
    var secondaryOptions: [String] = []
    var tertiaryLabel = ""
    var tertiaryOptions: [String] = []
    var quaternaryLabel = ""
    var quaternaryOptions: [String] = []
    var noteLabel = "NOTES"
    var notePlaceholder = "Add note - optional"
    var noteSectionStyle: RelationshipsNoteStyle = .link
    var alwaysShowNote = false
    var depthLevels: [RelationshipsDepthLevel] = []
    var faceOptions: [RelationshipsFaceOption] = []
    var meterDefault = 0
    let cta: String
    var ctaSparkle = false
    var defaultPrimary = ""
    var defaultSecondary = ""
    var defaultTertiary = ""
    var defaultQuaternary = ""
}

private extension RelationshipsQuickAddKind {
    var sheetCopy: RelationshipsSheetCopy {
        switch self {
        case .adjust:
            return RelationshipsSheetCopy(
                screenTitle: "Capture Relationships",
                sheetBackground: "#0B0A12",
                heroTitle: "Adjust",
                heroBody: "Change a relationship priority.",
                heroGlyph: "💗",
                primaryFieldLabel: "What to change?",
                primaryPlaceholder: "Make more time for close friends",
                primaryLabel: "Adjustment Area",
                emojiPrimary: [
                    .init(emoji: "💬", label: "Better Communication"),
                    .init(emoji: "🙏", label: "More Appreciation"),
                    .init(emoji: "📅", label: "More Consistency"),
                    .init(emoji: "🎉", label: "More Fun"),
                    .init(emoji: "👋", label: "More Presence"),
                    .init(emoji: "🤝", label: "More Shared Experiences"),
                    .init(emoji: "🫂", label: "More Support"),
                    .init(emoji: "⏰", label: "More Time Together"),
                ],
                secondaryLabel: "Who should get more attention?",
                secondaryOptions: ["Child", "Family", "Friend", "Parent", "Partner"],
                tertiaryLabel: "How important is this change?",
                tertiaryOptions: ["Low", "Medium", "High"],
                quaternaryLabel: "How confident are you?",
                quaternaryOptions: ["Not Sure", "Somewhat Sure", "Very Sure"],
                noteSectionStyle: .card,
                alwaysShowNote: true,
                cta: "Update Relationship",
                defaultPrimary: "More Appreciation",
                defaultSecondary: "Friend",
                defaultTertiary: "Medium",
                defaultQuaternary: "Very Sure"
            )
        case .support:
            return RelationshipsSheetCopy(
                screenTitle: "Capture Relationships",
                screenSubtitle: "Record the moments and actions that shape your connections.",
                pinkTitle: false,
                heroTitle: "Support",
                heroBody: "Care or help was given, received, or shared.",
                heroGlyph: "🫶",
                heroIconBg: Color(hex: "#1C1926"),
                heroBorder: Color.white.opacity(0.1),
                heroTitleSize: 20,
                primaryFieldLabel: "WHAT HAPPENED?",
                primaryPlaceholder: "Helped a friend prepare for an interview",
                primaryLabel: "SUPPORT TYPE",
                emojiPrimary: [
                    .init(emoji: "💡", label: "Advice", compact: true),
                    .init(emoji: "💝", label: "Care", compact: true),
                    .init(emoji: "🎊", label: "Celebration", compact: true),
                    .init(emoji: "🫂", label: "Emotional", compact: true),
                    .init(emoji: "💪", label: "Encouragement", compact: true),
                    .init(emoji: "💰", label: "Financial", compact: true),
                    .init(emoji: "🔧", label: "Practical", compact: true),
                ],
                secondaryLabel: "WHICH DIRECTION DID THE SUPPORT FLOW?",
                secondaryOptions: ["➡️ Given", "⬅️ Received", "↔️ Mutual"],
                tertiaryLabel: "HOW HELPFUL WAS IT?",
                tertiaryOptions: ["Small", "Meaningful", "Important", "Transformational"],
                noteSectionStyle: .link,
                cta: "Save Support",
                defaultPrimary: "Care",
                defaultSecondary: "➡️ Given",
                defaultTertiary: "Meaningful"
            )
        case .connection:
            return RelationshipsSheetCopy(
                screenTitle: "Capture Relationships",
                screenSubtitle: "Record the moments and actions that shape your connections.",
                showHero: true,
                heroTitle: "Connection",
                heroBody: "A meaningful contact or presence.",
                heroGlyph: "👥",
                heroTitleSize: 18,
                emojiPrimary: [
                    .init(emoji: "💬", label: "Conversation"),
                    .init(emoji: "🍽️", label: "Meal Together"),
                    .init(emoji: "💌", label: "Message"),
                    .init(emoji: "📞", label: "Call"),
                    .init(emoji: "🎉", label: "Celebration"),
                    .init(emoji: "✅", label: "Check-in"),
                    .init(emoji: "⏰", label: "Shared Time"),
                    .init(emoji: "🏠", label: "Visit"),
                ],
                secondaryOptions: ["👶 Child", "🌍 Community", "👨‍👩‍👧 Family", "🤗 Friend", "🎓 Mentor", "👨‍👦 Parent", "💑 Partner", "💼 Professional"],
                tertiaryOptions: ["🔥 Warm", "😌 Calm", "😊 Joyful", "🧠 Serious", "💚 Sense", "🤗 Supportive"],
                quaternaryOptions: ["5 min", "15 min", "30 min", "1 Hour", "2+ hours"],
                noteSectionStyle: .none,
                alwaysShowNote: true,
                depthLevels: [
                    .init(label: "Distant", size: 28),
                    .init(label: "Meaningful", size: 36),
                    .init(label: "Memorable", size: 44),
                    .init(label: "Routine", size: 52),
                ],
                meterDefault: 3,
                cta: "Save Connection",
                ctaSparkle: true,
                defaultPrimary: "Conversation",
                defaultSecondary: "🤗 Friend",
                defaultTertiary: "🔥 Warm",
                defaultQuaternary: "1 Hour"
            )
        case .shared:
            return RelationshipsSheetCopy(
                screenTitle: "Shared Experience",
                screenSubtitle: "Record a moment that strengthened a connection.",
                pinkTitle: false,
                showHero: true,
                heroTitle: "Shared Experience",
                heroBody: "Capture a shared moment and its emotional return.",
                heroGlyph: "✨",
                primaryFieldLabel: "WHAT DID YOU DO?",
                primaryPlaceholder: "Family dinner, weekend hike...",
                primaryLabel: "EXPERIENCE TYPE",
                emojiPrimary: [
                    .init(emoji: "🏔️", label: "Adventure"),
                    .init(emoji: "🎉", label: "Celebration"),
                    .init(emoji: "💬", label: "Conversation"),
                    .init(emoji: "🎨", label: "Creative"),
                    .init(emoji: "🍽️", label: "Meal"),
                    .init(emoji: "🚗", label: "Outing"),
                    .init(emoji: "⭐", label: "Quality Time"),
                    .init(emoji: "🏃", label: "Sport"),
                    .init(emoji: "✈️", label: "Travel"),
                    .init(emoji: "🤔", label: "Other"),
                ],
                secondaryLabel: "WHO WAS IT WITH?",
                secondaryOptions: ["Child", "Community", "Family", "Friend", "Mentor", "Parent", "Partner"],
                tertiaryLabel: "HOW DID IT FEEL?",
                quaternaryLabel: "EMOTIONAL IMPACT",
                quaternaryOptions: ["Neutral", "Warm", "Uplifting", "Deeply Meaningful"],
                noteLabel: "NOTES",
                notePlaceholder: "Why did this change matter?",
                noteSectionStyle: .none,
                alwaysShowNote: true,
                faceOptions: [
                    .init(emoji: "😐", label: "Ordinary"),
                    .init(emoji: "😊", label: "Enjoyable"),
                    .init(emoji: "🤩", label: "Memorable"),
                    .init(emoji: "🌟", label: "Exceptional"),
                ],
                cta: "Save Shared Experience",
                defaultPrimary: "Conversation",
                defaultSecondary: "Family",
                defaultTertiary: "Memorable",
                defaultQuaternary: "Uplifting"
            )
        case .investment:
            return RelationshipsSheetCopy(
                screenTitle: "Capture Relationships",
                sheetBackground: "#0B0A12",
                heroTitle: "Investment",
                heroBody: "Capture time, care, or resources intentionally invested.",
                heroGlyph: "✨",
                primaryFieldLabel: "What did you invest?",
                primaryPlaceholder: "Anniversary planning session",
                primaryLabel: "Investment Area",
                primaryOptions: ["⏰ Quality Time", "💝 Emotional Support", "💰 Financial", "🎁 Gift", "💪 Effort", "📚 Listening", "📋 Planning", "🔍 Other"],
                secondaryLabel: "Who did you invest in?",
                secondaryOptions: ["Child", "Family", "Friend", "Mentor", "Parent", "Partner", "Professional"],
                tertiaryLabel: "Time Invested",
                tertiaryOptions: ["15 min", "30 min", "1 hour", "2 hours", "Half Day", "Full Day"],
                quaternaryLabel: "How meaningful?",
                quaternaryOptions: ["Small", "Moderate", "Significant", "Transformational"],
                noteSectionStyle: .link,
                cta: "Save Relationship Investment",
                defaultPrimary: "💝 Emotional Support",
                defaultSecondary: "Partner",
                defaultTertiary: "1 hour",
                defaultQuaternary: "Moderate"
            )
        }
    }

    func buildPayload(
        headline: String,
        contextLine: String,
        primary: String,
        secondary: String,
        tertiary: String,
        quaternary: String,
        meter: Int,
        depthLabels: [String],
        note: String
    ) -> (displayName: String, note: String?) {
        let trimmedHeadline = headline.trimmingCharacters(in: .whitespacesAndNewlines)
        let trimmedContext = contextLine.trimmingCharacters(in: .whitespacesAndNewlines)
        let trimmedNote = note.trimmingCharacters(in: .whitespacesAndNewlines)
        var parts: [String] = []
        switch self {
        case .adjust:
            parts = [primary, secondary, tertiary, quaternary]
            if !trimmedHeadline.isEmpty { parts.insert(trimmedHeadline, at: 0) }
        case .support:
            parts = [primary, secondary, tertiary]
            if !trimmedHeadline.isEmpty { parts.insert(trimmedHeadline, at: 0) }
        case .connection:
            if !trimmedHeadline.isEmpty { parts.append(trimmedHeadline) }
            if !trimmedContext.isEmpty { parts.append(trimmedContext) }
            parts.append(contentsOf: [primary, secondary, tertiary, quaternary])
            if meter >= 0, meter < depthLabels.count { parts.append(depthLabels[meter]) }
        case .shared:
            if !trimmedHeadline.isEmpty { parts.append(trimmedHeadline) }
            parts.append(contentsOf: [primary, secondary, tertiary, quaternary])
        case .investment:
            if !trimmedHeadline.isEmpty { parts.append(trimmedHeadline) }
            parts.append(contentsOf: [primary, secondary, tertiary, quaternary])
        }
        let displayName = String(parts.filter { !$0.isEmpty }.joined(separator: " · ").prefix(200))
        let finalNote = trimmedNote.isEmpty ? nil : trimmedNote
        return (displayName.isEmpty ? rawValue : displayName, finalNote)
    }
}
