import SwiftUI

enum LifeOpsQuickAddKind: String, Identifiable {
    case recovery
    case mood
    case attention
    case adjust

    var id: String { rawValue }

    var observationType: String {
        switch self {
        case .recovery: return "RECOVERY"
        case .mood: return "MOOD"
        case .attention: return "RHYTHM"
        case .adjust: return "RHYTHM"
        }
    }
}

/// Figma Life Ops quick-add sheets:
/// Recovery `353:11408`, Mood `353:11452`, Attention `353:11361`, Adjust `353:11680`.
struct PersonalLifeOpsQuickAddSheet: View {
    let kind: LifeOpsQuickAddKind
    let momentId: String
    var onClose: () -> Void
    var onSaved: () -> Void

    var body: some View {
        Group {
            switch kind {
            case .recovery:
                RecoveryLifeOpsSheet(momentId: momentId, onClose: onClose, onSaved: onSaved)
            case .mood:
                MoodLifeOpsSheet(momentId: momentId, onClose: onClose, onSaved: onSaved)
            case .attention:
                AttentionLifeOpsSheet(momentId: momentId, onClose: onClose, onSaved: onSaved)
            case .adjust:
                AdjustLifeOpsSheet(momentId: momentId, onClose: onClose, onSaved: onSaved)
            }
        }
        .presentationDetents([.medium, .large])
        .presentationDragIndicator(.visible)
    }
}

// MARK: - Palette

private enum LoColors {
    static let bg = Color(hex: "#14121B")
    static let surface = Color(hex: "#201E28")
    static let elevated = Color(hex: "#3A3842")
    static let text = Color(hex: "#E5E0EE")
    static let secondary = Color(hex: "#C9C4D8")
    static let muted = Color(hex: "#A099B0")
    static let border = Color(hex: "#938EA1")
    static let accent = Color(hex: "#7C5CFC")
    static let brand = Color(hex: "#C9BFFF")
    static let green = Color(hex: "#10B981")
    static let cardBorder = Color.white.opacity(0.08)
    static let error = Color(hex: "#F87171")
    static let saveGradient = LinearGradient(
        colors: [Color(hex: "#8B5CF6"), Color(hex: "#06B6D4")],
        startPoint: .leading,
        endPoint: .trailing
    )
    static let sliderGradient = LinearGradient(
        colors: [
            Color(hex: "#3B82F6"), Color(hex: "#06B6D4"), Color(hex: "#10B981"),
            Color(hex: "#F59E0B"), Color(hex: "#EF4444"),
        ],
        startPoint: .leading,
        endPoint: .trailing
    )
}

private struct LoEmojiChip {
    let emoji: String
    let label: String
}

private struct LoIconChip {
    let icon: String
    let label: String
}

// MARK: - Shared primitives

private struct IntelligenceOsHeader: View {
    var onClose: () -> Void
    var badge: String = "Runtime learning"

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack(alignment: .center) {
                Text("Intelligence OS")
                    .font(.plusJakarta(size: 20, weight: .heavy))
                    .foregroundStyle(LoColors.text)
                Spacer(minLength: 8)
                HStack(spacing: 8) {
                    HStack(spacing: 6) {
                        Circle()
                            .fill(LoColors.green)
                            .frame(width: 6, height: 6)
                        Text(badge)
                            .font(.plusJakarta(size: 11))
                            .foregroundStyle(LoColors.secondary)
                    }
                    .padding(.horizontal, 10)
                    .padding(.vertical, 5)
                    .background(LoColors.surface)
                    .overlay(Capsule().stroke(LoColors.cardBorder, lineWidth: 1))
                    .clipShape(Capsule())

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
            Text("Record what shapes how your day runs.")
                .font(.plusJakarta(size: 12))
                .foregroundStyle(LoColors.secondary)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
    }
}

private struct LoSectionCard<Content: View>: View {
    @ViewBuilder var content: () -> Content

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            content()
        }
        .padding(20)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(LoColors.surface)
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(LoColors.cardBorder, lineWidth: 1))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }
}

private struct LoFieldLabel: View {
    let text: String

    var body: some View {
        Text(text)
            .font(.plusJakarta(size: 11, weight: .bold))
            .tracking(0.8)
            .foregroundStyle(LoColors.brand)
    }
}

private struct LoGradientSlider: View {
    @Binding var value: Int
    @State private var animatedFraction: CGFloat = 0

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(alignment: .bottom) {
                Text("\(value)")
                    .font(.plusJakarta(size: 24, weight: .heavy))
                    .foregroundStyle(LoColors.text)
                Spacer()
                Text("/10")
                    .font(.plusJakarta(size: 14))
                    .foregroundStyle(LoColors.muted)
            }

            GeometryReader { geo in
                let width = geo.size.width
                ZStack(alignment: .leading) {
                    RoundedRectangle(cornerRadius: 8)
                        .fill(LoColors.sliderGradient)
                        .frame(height: 16)

                    Circle()
                        .fill(Color.white)
                        .overlay(Circle().stroke(LoColors.accent, lineWidth: 2))
                        .frame(width: 28, height: 28)
                        .overlay {
                            Text("\(value)")
                                .font(.plusJakarta(size: 10, weight: .bold))
                                .foregroundStyle(LoColors.accent)
                        }
                        .offset(x: max(0, min(width - 28, animatedFraction * width - 14)))
                }
                .contentShape(Rectangle())
                .gesture(
                    DragGesture(minimumDistance: 0)
                        .onChanged { gesture in
                            updateValue(from: gesture.location.x, width: width)
                        }
                )
            }
            .frame(height: 28)

            HStack {
                ForEach(0...10, id: \.self) { i in
                    Text("\(i)")
                        .font(.plusJakarta(size: 10))
                        .foregroundStyle(LoColors.muted)
                        .frame(maxWidth: .infinity)
                }
            }
        }
        .onAppear { animatedFraction = CGFloat(value) / 10 }
        .onChange(of: value) { _, newValue in
            withAnimation(.spring(response: 0.35, dampingFraction: 0.75)) {
                animatedFraction = CGFloat(newValue) / 10
            }
        }
    }

    private func updateValue(from x: CGFloat, width: CGFloat) {
        guard width > 0 else { return }
        let raw = Int((x / width * 10).rounded())
        value = min(10, max(0, raw))
    }
}

private struct LoTextChipRow: View {
    let options: [String]
    @Binding var selected: String
    var emojiPrefix: [String: String] = [:]

    var body: some View {
        FlowLayout(spacing: 8) {
            ForEach(options, id: \.self) { option in
                let isOn = selected == option
                Button {
                    withAnimation(.spring(response: 0.3, dampingFraction: 0.75)) {
                        selected = option
                    }
                } label: {
                    HStack(spacing: 6) {
                        if let emoji = emojiPrefix[option] {
                            Text(emoji).font(.plusJakarta(size: 14))
                        }
                        Text(option)
                            .font(.plusJakarta(size: 12, weight: isOn ? .heavy : .semibold))
                            .foregroundStyle(isOn ? .white : LoColors.text)
                    }
                    .padding(.horizontal, 12)
                    .padding(.vertical, 8)
                    .background(isOn ? LoColors.accent : LoColors.elevated)
                    .overlay(Capsule().stroke(isOn ? LoColors.accent : LoColors.border, lineWidth: 1))
                    .clipShape(Capsule())
                }
                .buttonStyle(.plain)
            }
        }
    }
}

private struct LoEmojiChipRow: View {
    let chips: [LoEmojiChip]
    @Binding var selected: String

    var body: some View {
        FlowLayout(spacing: 8) {
            ForEach(chips, id: \.label) { chip in
                let isOn = selected == chip.label
                Button {
                    withAnimation(.spring(response: 0.3, dampingFraction: 0.75)) {
                        selected = chip.label
                    }
                } label: {
                    VStack(spacing: 4) {
                        Text(chip.emoji).font(.plusJakarta(size: 20))
                        Text(chip.label)
                            .font(.plusJakarta(size: 11, weight: isOn ? .heavy : .semibold))
                            .foregroundStyle(isOn ? .white : LoColors.text)
                    }
                    .padding(.horizontal, 10)
                    .padding(.vertical, 8)
                    .background(isOn ? LoColors.accent : LoColors.elevated)
                    .overlay(RoundedRectangle(cornerRadius: 12).stroke(isOn ? LoColors.accent : LoColors.border, lineWidth: 1))
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                }
                .buttonStyle(.plain)
            }
        }
    }
}

private struct LoNoteField: View {
    @Binding var text: String
    let placeholder: String
    var minHeight: CGFloat = 100

    var body: some View {
        ZStack(alignment: .topLeading) {
            if text.isEmpty {
                Text(placeholder)
                    .font(.plusJakarta(size: 13))
                    .foregroundStyle(LoColors.secondary)
                    .padding(.top, 4)
                    .padding(.leading, 4)
            }
            TextEditor(text: $text)
                .scrollContentBackground(.hidden)
                .font(.plusJakarta(size: 13))
                .foregroundStyle(LoColors.text)
                .frame(minHeight: minHeight)
        }
        .padding(12)
        .background(LoColors.elevated)
        .overlay(RoundedRectangle(cornerRadius: 12).stroke(LoColors.border, lineWidth: 1))
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }
}

private struct LoSaveButton: View {
    let label: String
    var enabled: Bool = true
    let submitting: Bool
    let testTag: String
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(submitting ? "Saving…" : label)
                .font(.plusJakarta(size: 15, weight: .heavy))
                .foregroundStyle(.white)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 14)
                .background(LoColors.saveGradient)
                .clipShape(RoundedRectangle(cornerRadius: 14))
        }
        .buttonStyle(.plain)
        .disabled(!enabled || submitting)
        .opacity(enabled && !submitting ? 1 : 0.6)
        .accessibilityIdentifier(testTag)
    }
}

// MARK: - Recovery — Figma `353:11408`

private struct RecoveryLifeOpsSheet: View {
    let momentId: String
    var onClose: () -> Void
    var onSaved: () -> Void

    @State private var activity = "Walk"
    @State private var quality = 7
    @State private var duration = "30 min"
    @State private var energy = 6
    @State private var note = ""
    @State private var submitting = false
    @State private var error: String?
    @State private var draftKey = UUID().uuidString

    private let repository = ObservationCreateRepository()
    private let activities = ["Walk", "Nap", "Meditate", "Stretch", "Sleep", "Social"]
    private let durations = ["15 min", "30 min", "60 min"]

    private var ringProgress: CGFloat {
        switch duration {
        case "15 min": return 0.25
        case "60 min": return 1.0
        default: return 0.5
        }
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 14) {
                IntelligenceOsHeader(onClose: onClose)

                LoSectionCard {
                    Text("Recovery")
                        .font(.plusJakarta(size: 22, weight: .heavy))
                        .foregroundStyle(LoColors.text)
                        .frame(maxWidth: .infinity, alignment: .center)
                    Text("Capture rest and recharge activities.")
                        .font(.plusJakarta(size: 12))
                        .foregroundStyle(LoColors.secondary)
                        .multilineTextAlignment(.center)
                        .frame(maxWidth: .infinity, alignment: .center)
                }

                LoSectionCard {
                    LoFieldLabel(text: "ACTIVITY")
                    LoTextChipRow(options: activities, selected: $activity)
                }

                LoSectionCard {
                    LoFieldLabel(text: "QUALITY")
                    LoGradientSlider(value: $quality)
                }

                LoSectionCard {
                    LoFieldLabel(text: "DURATION")
                    HStack {
                        ZStack {
                            Circle()
                                .stroke(LoColors.elevated, lineWidth: 8)
                                .frame(width: 100, height: 100)
                            Circle()
                                .trim(from: 0, to: ringProgress)
                                .stroke(LoColors.accent, style: StrokeStyle(lineWidth: 8, lineCap: .round))
                                .frame(width: 100, height: 100)
                                .rotationEffect(.degrees(-90))
                                .animation(.spring(response: 0.35, dampingFraction: 0.7), value: duration)
                            Text(duration.replacingOccurrences(of: " min", with: "m"))
                                .font(.plusJakarta(size: 16, weight: .bold))
                                .foregroundStyle(LoColors.text)
                        }
                        Spacer()
                        VStack(spacing: 8) {
                            ForEach(durations, id: \.self) { d in
                                durationChip(d)
                            }
                        }
                    }
                }

                LoSectionCard {
                    LoFieldLabel(text: "ENERGY RESTORED")
                    HStack(spacing: 4) {
                        ForEach(0..<10, id: \.self) { index in
                            Button {
                                withAnimation(.spring(response: 0.3, dampingFraction: 0.75)) {
                                    energy = index + 1
                                }
                            } label: {
                                RoundedRectangle(cornerRadius: 4)
                                    .fill(index < energy ? LoColors.green : LoColors.elevated)
                                    .frame(height: 24)
                            }
                            .buttonStyle(.plain)
                        }
                    }
                }

                LoSectionCard {
                    LoFieldLabel(text: "NOTES")
                    LoNoteField(text: $note, placeholder: "How did this recovery feel?")
                        .accessibilityIdentifier("personal.lifeops.recovery.note")
                }

                if let error {
                    Text(error)
                        .font(.plusJakarta(size: 12))
                        .foregroundStyle(LoColors.error)
                }

                LoSaveButton(
                    label: "Save Recovery",
                    submitting: submitting,
                    testTag: "personal.lifeops.recovery.submit",
                    action: save
                )
            }
            .padding(.horizontal, 16)
            .padding(.bottom, 16)
        }
        .background(LoColors.bg)
    }

    private func durationChip(_ d: String) -> some View {
        let isOn = duration == d
        return Button {
            withAnimation(.spring(response: 0.3, dampingFraction: 0.75)) {
                duration = d
            }
        } label: {
            Text(d)
                .font(.plusJakarta(size: 12, weight: .semibold))
                .foregroundStyle(isOn ? .white : LoColors.text)
                .padding(.horizontal, 14)
                .padding(.vertical, 8)
                .background(isOn ? LoColors.accent : LoColors.elevated)
                .clipShape(Capsule())
        }
        .buttonStyle(.plain)
    }

    private func save() {
        guard !submitting else { return }
        submitting = true
        error = nil
        Task {
            do {
                _ = try await repository.recordObservation(
                    draftKey: draftKey,
                    momentId: momentId,
                    observationType: LifeOpsQuickAddKind.recovery.observationType,
                    numericValue: Double(quality),
                    textValue: "\(activity) · \(duration) · energy:\(energy)",
                    note: note.isEmpty ? nil : note,
                    activityTypeCode: {
                        switch activity {
                        case "Walk", "Stretch": return "EXERCISE"
                        case "Nap", "Sleep": return "SLEEP"
                        case "Meditate": return "MEDITATION"
                        case "Social": return "SOCIAL"
                        default: return "OTHER"
                        }
                    }(),
                    durationMinutes: {
                        switch duration {
                        case "15 min": return 15
                        case "60 min": return 60
                        case "2 hr": return 120
                        default: return 30
                        }
                    }(),
                    energyAfterPct: Double(energy * 10),
                    feelingStateCode: nil,
                    moodDrivers: nil
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

// MARK: - Mood — Figma `353:11452`

private struct MoodLifeOpsSheet: View {
    let momentId: String
    var onClose: () -> Void
    var onSaved: () -> Void

    @State private var feeling = "Calm"
    @State private var intensity = 8
    @State private var shaped = "Relationships"
    @State private var note = ""
    @State private var submitting = false
    @State private var error: String?
    @State private var draftKey = UUID().uuidString

    private let repository = ObservationCreateRepository()
    private let feelings: [LoEmojiChip] = [
        .init(emoji: "😄", label: "Great"),
        .init(emoji: "😌", label: "Calm"),
        .init(emoji: "😐", label: "Neutral"),
        .init(emoji: "😔", label: "Low"),
        .init(emoji: "😰", label: "Stressed"),
    ]
    private let shapedBy = ["Work", "Health", "Relationships", "Money", "Rest", "Weather"]
    private let shapedEmoji: [String: String] = [
        "Work": "💼", "Health": "💪", "Relationships": "💕",
        "Money": "💰", "Rest": "😴", "Weather": "🌤️",
    ]
    private let sparklinePoints: [CGFloat] = [0.65, 0.55, 0.7, 0.45, 0.55, 0.45, 0.45]
    private let weekdays = ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"]

    private var heroEmoji: String {
        feelings.first(where: { $0.label == feeling })?.emoji ?? "😌"
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 14) {
                IntelligenceOsHeader(onClose: onClose)

                LoSectionCard {
                    Text("Mood")
                        .font(.plusJakarta(size: 22, weight: .heavy))
                        .foregroundStyle(LoColors.text)
                        .frame(maxWidth: .infinity, alignment: .center)
                    Text("Reflect on your emotional state and what shaped it.")
                        .font(.plusJakarta(size: 12))
                        .foregroundStyle(LoColors.secondary)
                        .multilineTextAlignment(.center)
                        .frame(maxWidth: .infinity, alignment: .center)
                    Text(heroEmoji)
                        .font(.plusJakarta(size: 56))
                        .frame(maxWidth: .infinity, alignment: .center)
                        .padding(.top, 4)
                        .animation(.spring(response: 0.35, dampingFraction: 0.75), value: feeling)
                }

                LoSectionCard {
                    LoFieldLabel(text: "HOW ARE YOU FEELING?")
                    LoEmojiChipRow(chips: feelings, selected: $feeling)
                }

                LoSectionCard {
                    LoFieldLabel(text: "INTENSITY")
                    LoGradientSlider(value: $intensity)
                }

                LoSectionCard {
                    LoFieldLabel(text: "WHAT SHAPED THIS MOOD?")
                    LoTextChipRow(options: shapedBy, selected: $shaped, emojiPrefix: shapedEmoji)
                }

                LoSectionCard {
                    LoFieldLabel(text: "THIS WEEK")
                    sparklineView
                        .frame(height: 60)
                    HStack {
                        ForEach(weekdays, id: \.self) { day in
                            Text(day)
                                .font(.plusJakarta(size: 10))
                                .foregroundStyle(LoColors.muted)
                                .frame(maxWidth: .infinity)
                        }
                    }
                    HStack {
                        Text("Average: 7.0")
                            .font(.plusJakarta(size: 12))
                            .foregroundStyle(LoColors.secondary)
                        Spacer()
                        Text("Trending: ↑ Up")
                            .font(.plusJakarta(size: 12))
                            .foregroundStyle(LoColors.green)
                    }
                    .padding(.top, 6)
                }

                LoSectionCard {
                    LoFieldLabel(text: "REFLECTION NOTE")
                    LoNoteField(text: $note, placeholder: "Feeling balanced after a good walk...")
                        .accessibilityIdentifier("personal.lifeops.mood.note")
                }

                if let error {
                    Text(error)
                        .font(.plusJakarta(size: 12))
                        .foregroundStyle(LoColors.error)
                }

                LoSaveButton(
                    label: "Save Reflection",
                    submitting: submitting,
                    testTag: "personal.lifeops.mood.submit",
                    action: save
                )
            }
            .padding(.horizontal, 16)
            .padding(.bottom, 16)
        }
        .background(LoColors.bg)
    }

    private var sparklineView: some View {
        GeometryReader { geo in
            let width = geo.size.width
            let height = geo.size.height
            let step = width / CGFloat(sparklinePoints.count - 1)
            Path { path in
                for index in sparklinePoints.indices {
                    let x = CGFloat(index) * step
                    let y = height * (1 - sparklinePoints[index])
                    if index == 0 {
                        path.move(to: CGPoint(x: x, y: y))
                    } else {
                        path.addLine(to: CGPoint(x: x, y: y))
                    }
                }
            }
            .stroke(LoColors.accent, lineWidth: 2)
        }
    }

    private func save() {
        guard !submitting else { return }
        submitting = true
        error = nil
        Task {
            do {
                _ = try await repository.recordObservation(
                    draftKey: draftKey,
                    momentId: momentId,
                    observationType: LifeOpsQuickAddKind.mood.observationType,
                    numericValue: Double(intensity),
                    textValue: "\(feeling) · shaped:\(shaped)",
                    note: note.isEmpty ? nil : note,
                    activityTypeCode: nil,
                    durationMinutes: nil,
                    energyAfterPct: nil,
                    feelingStateCode: {
                        switch feeling {
                        case "Great": return "GREAT"
                        case "Calm": return "CALM"
                        case "Neutral": return "NEUTRAL"
                        case "Low": return "LOW"
                        case "Stressed": return "STRESSED"
                        default: return "OTHER"
                        }
                    }(),
                    moodDrivers: [
                        shaped.uppercased()
                            .replacingOccurrences(of: " ", with: "_")
                            .replacingOccurrences(of: "&", with: "AND")
                    ]
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

// MARK: - Attention — Figma `353:11361`

private struct AttentionLifeOpsSheet: View {
    let momentId: String
    var onClose: () -> Void
    var onSaved: () -> Void

    @State private var target = "Work"
    @State private var depth = 7
    @State private var duration = "60 min"
    @State private var note = ""
    @State private var submitting = false
    @State private var error: String?
    @State private var draftKey = UUID().uuidString

    private let repository = ObservationCreateRepository()
    private let targets = ["Work", "Health", "Family", "Money", "Learning", "Rest"]
    private let durations = ["15 min", "30 min", "60 min", "2 hr"]

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 14) {
                IntelligenceOsHeader(onClose: onClose)

                LoSectionCard {
                    Text("Attention")
                        .font(.plusJakarta(size: 22, weight: .heavy))
                        .foregroundStyle(LoColors.text)
                        .frame(maxWidth: .infinity, alignment: .center)
                    Text("Set where your focus should land next.")
                        .font(.plusJakarta(size: 12))
                        .foregroundStyle(LoColors.secondary)
                        .multilineTextAlignment(.center)
                        .frame(maxWidth: .infinity, alignment: .center)
                }

                LoSectionCard {
                    LoFieldLabel(text: "FOCUS TARGET")
                    LoTextChipRow(options: targets, selected: $target)
                }

                LoSectionCard {
                    LoFieldLabel(text: "DEPTH")
                    LoGradientSlider(value: $depth)
                }

                LoSectionCard {
                    LoFieldLabel(text: "DURATION")
                    LoTextChipRow(options: durations, selected: $duration)
                }

                LoSectionCard {
                    LoFieldLabel(text: "NOTES")
                    LoNoteField(text: $note, placeholder: "What deserves your attention right now?")
                        .accessibilityIdentifier("personal.lifeops.attention.note")
                }

                if let error {
                    Text(error)
                        .font(.plusJakarta(size: 12))
                        .foregroundStyle(LoColors.error)
                }

                LoSaveButton(
                    label: "Save Focus",
                    submitting: submitting,
                    testTag: "personal.lifeops.attention.submit",
                    action: save
                )
            }
            .padding(.horizontal, 16)
            .padding(.bottom, 16)
        }
        .background(LoColors.bg)
    }

    private func save() {
        guard !submitting else { return }
        submitting = true
        error = nil
        Task {
            do {
                let intensityCode: String
                if depth <= 3 { intensityCode = "LIGHT" }
                else if depth <= 7 { intensityCode = "MODERATE" }
                else { intensityCode = "HEAVY" }
                let hour = Calendar.current.component(.hour, from: Date())
                let timeBlock: String
                switch hour {
                case 5..<12: timeBlock = "MORNING"
                case 12..<17: timeBlock = "AFTERNOON"
                case 17..<22: timeBlock = "EVENING"
                default: timeBlock = "NIGHT"
                }
                try await repository.recordAttentionCapture(
                    draftKey: draftKey,
                    momentId: momentId,
                    categoryCode: target.uppercased(),
                    intensityCode: intensityCode,
                    timeBlockCode: timeBlock,
                    energyRemaining: max(0, min(5, 5 - depth / 2)),
                    note: note.isEmpty ? nil : note
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

// MARK: - Adjust — Figma `353:11680`

private struct AdjustLifeOpsSheet: View {
    let momentId: String
    var onClose: () -> Void
    var onSaved: () -> Void

    @State private var rhythmAction = "Reduce load"
    @State private var signal: CGFloat = 0.5
    @State private var animatedSignal: CGFloat = 0.5
    @State private var reason = ""
    @State private var submitting = false
    @State private var error: String?
    @State private var draftKey = UUID().uuidString

    private let repository = ObservationCreateRepository()
    private let rhythmActions: [LoIconChip] = [
        .init(icon: "−", label: "Reduce load"),
        .init(icon: "↑", label: "Increase intensity"),
        .init(icon: "⏸", label: "Pause"),
        .init(icon: "↺", label: "Reset"),
    ]
    private let priorities: [(String, Int)] = [
        ("Health & Energy", 80),
        ("Career", 60),
        ("Relationships", 40),
        ("Finance", 30),
        ("Learning", 20),
    ]

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 14) {
                IntelligenceOsHeader(onClose: onClose)

                LoSectionCard {
                    HStack(spacing: 12) {
                        Text("☰")
                            .font(.plusJakarta(size: 18))
                            .frame(width: 40, height: 40)
                            .background(LoColors.accent.opacity(0.2))
                            .clipShape(RoundedRectangle(cornerRadius: 12))
                        VStack(alignment: .leading, spacing: 2) {
                            Text("Tune Rhythm & Priorities")
                                .font(.plusJakarta(size: 16, weight: .bold))
                                .foregroundStyle(LoColors.text)
                            Text("Change how your current operating rhythm should respond.")
                                .font(.plusJakarta(size: 12))
                                .foregroundStyle(LoColors.secondary)
                        }
                    }
                }

                LoSectionCard {
                    Text("Rhythm Action")
                        .font(.plusJakarta(size: 13, weight: .semibold))
                        .foregroundStyle(LoColors.text)
                    VStack(spacing: 10) {
                        ForEach(Array(stride(from: 0, to: rhythmActions.count, by: 2)), id: \.self) { index in
                            HStack(spacing: 10) {
                                rhythmActionTile(rhythmActions[index])
                                if index + 1 < rhythmActions.count {
                                    rhythmActionTile(rhythmActions[index + 1])
                                } else {
                                    Spacer(minLength: 0)
                                }
                            }
                        }
                    }
                }

                LoSectionCard {
                    Text("Priorities")
                        .font(.plusJakarta(size: 13, weight: .semibold))
                        .foregroundStyle(LoColors.text)
                    ForEach(priorities, id: \.0) { label, pct in
                        HStack {
                            Text(label)
                                .font(.plusJakarta(size: 12))
                                .foregroundStyle(LoColors.secondary)
                            Spacer()
                            Text("\(pct)%")
                                .font(.plusJakarta(size: 12))
                                .foregroundStyle(LoColors.text)
                        }
                        GeometryReader { geo in
                            ZStack(alignment: .leading) {
                                RoundedRectangle(cornerRadius: 4)
                                    .fill(LoColors.elevated)
                                    .frame(height: 8)
                                RoundedRectangle(cornerRadius: 4)
                                    .fill(LoColors.accent)
                                    .frame(width: geo.size.width * CGFloat(pct) / 100, height: 8)
                            }
                        }
                        .frame(height: 8)
                        .padding(.bottom, 6)
                    }
                }

                LoSectionCard {
                    Text("Signal Direction")
                        .font(.plusJakarta(size: 13, weight: .semibold))
                        .foregroundStyle(LoColors.text)
                    HStack(spacing: 0) {
                        signalZone(label: "Decrease", color: Color(hex: "#3B82F6"))
                        signalZone(label: "Maintain", color: LoColors.muted)
                        signalZone(label: "Increase", color: Color(hex: "#EF4444"))
                    }
                    GeometryReader { geo in
                        let width = geo.size.width
                        ZStack(alignment: .leading) {
                            Color.clear.frame(height: 24)
                            Circle()
                                .fill(Color.white)
                                .overlay(Circle().stroke(LoColors.accent, lineWidth: 2))
                                .frame(width: 18, height: 18)
                                .offset(x: max(0, min(width - 18, animatedSignal * width - 9)))
                        }
                        .contentShape(Rectangle())
                        .gesture(
                            DragGesture(minimumDistance: 0)
                                .onChanged { gesture in
                                    let next = min(1, max(0, gesture.location.x / width))
                                    signal = next
                                    withAnimation(.spring(response: 0.35, dampingFraction: 0.75)) {
                                        animatedSignal = next
                                    }
                                }
                        )
                    }
                    .frame(height: 24)
                    .padding(.top, 8)
                }

                LoSectionCard {
                    HStack(spacing: 8) {
                        Text("💡").font(.plusJakarta(size: 16))
                        Text("INSIGHT")
                            .font(.plusJakarta(size: 11, weight: .bold))
                            .tracking(0.8)
                            .foregroundStyle(LoColors.brand)
                    }
                    Text("Based on 3 days of low recovery + high work attention, we suggest: Recovery focused mode with decreased pressure.")
                        .font(.plusJakarta(size: 12))
                        .foregroundStyle(LoColors.secondary)
                }

                LoSectionCard {
                    Text("Adjustment Reason")
                        .font(.plusJakarta(size: 13, weight: .semibold))
                        .foregroundStyle(LoColors.text)
                    LoNoteField(text: $reason, placeholder: "Feeling overwhelmed this week, need to slow down.")
                        .accessibilityIdentifier("personal.lifeops.adjust.note")
                }

                if let error {
                    Text(error)
                        .font(.plusJakarta(size: 12))
                        .foregroundStyle(LoColors.error)
                }

                LoSaveButton(
                    label: "Update Rhythm",
                    submitting: submitting,
                    testTag: "personal.lifeops.adjust.submit",
                    action: save
                )
            }
            .padding(.horizontal, 16)
            .padding(.bottom, 16)
        }
        .background(LoColors.bg)
        .onAppear { animatedSignal = signal }
    }

    private func rhythmActionTile(_ action: LoIconChip) -> some View {
        let isOn = rhythmAction == action.label
        return Button {
            withAnimation(.spring(response: 0.3, dampingFraction: 0.75)) {
                rhythmAction = action.label
            }
        } label: {
            VStack(spacing: 6) {
                Text(action.icon).font(.plusJakarta(size: 18))
                Text(action.label)
                    .font(.plusJakarta(size: 11, weight: .semibold))
                    .foregroundStyle(LoColors.text)
                    .multilineTextAlignment(.center)
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 14)
            .background(isOn ? LoColors.accent.opacity(0.25) : LoColors.elevated)
            .overlay(RoundedRectangle(cornerRadius: 12).stroke(isOn ? LoColors.accent : LoColors.cardBorder, lineWidth: 1))
            .clipShape(RoundedRectangle(cornerRadius: 12))
        }
        .buttonStyle(.plain)
    }

    private func signalZone(label: String, color: Color) -> some View {
        Text(label)
            .font(.plusJakarta(size: 10))
            .foregroundStyle(LoColors.text)
            .frame(maxWidth: .infinity)
            .frame(height: 24)
            .background(color.opacity(0.35))
    }

    private func save() {
        guard !submitting else { return }
        submitting = true
        error = nil
        Task {
            do {
                let actionCode: String?
                switch rhythmAction {
                case "Reduce load": actionCode = "REDUCE_LOAD"
                case "Increase intensity": actionCode = "INCREASE_INTENSITY"
                case "Pause": actionCode = "PAUSE"
                case "Reset": actionCode = "RESET"
                default: actionCode = nil
                }
                let signalCode: String
                if signal < 0.35 { signalCode = "DECREASE_PRESSURE" }
                else if signal > 0.65 { signalCode = "INCREASE_PRESSURE" }
                else { signalCode = "MAINTAIN" }
                try await repository.recordLifeOpsAdjust(
                    draftKey: draftKey,
                    momentId: momentId,
                    rhythmActionCode: actionCode,
                    signalDirectionCode: signalCode,
                    reason: reason.isEmpty ? nil : reason,
                    priorityWeights: [
                        .init(priorityCode: "HEALTH_AND_ENERGY", weightPct: 80),
                        .init(priorityCode: "CAREER", weightPct: 60),
                        .init(priorityCode: "RELATIONSHIPS", weightPct: 40),
                        .init(priorityCode: "FINANCE", weightPct: 30),
                        .init(priorityCode: "LEARNING", weightPct: 20),
                    ]
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
