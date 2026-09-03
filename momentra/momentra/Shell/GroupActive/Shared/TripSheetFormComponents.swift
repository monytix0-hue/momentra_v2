import SwiftUI

/// Figma Trip Quick Add sheet form primitives — native pickers + Figma chrome.
enum TripForm {
    static let bg = TripSheetTokens.bg
    static let field = TripSheetTokens.field
    static let border = TripSheetTokens.border
    static let muted = TripSheetTokens.muted
    static let text = TripSheetTokens.text
    static let accent = TripSheetTokens.accent
    static let accentEnd = TripSheetTokens.accentEnd
    static let teal = Color(hex: "#14B8A6")
    static let purple = Color(hex: "#A855F7")
    static let blue = Color(hex: "#3B82F6")
    static let pink = Color(hex: "#EC4899")
    static let green = Color(hex: "#10B981")
    static let avatarColors: [Color] = [
        Color(hex: "#FDBA74"), Color(hex: "#86EFAC"), Color(hex: "#F9A8D4"), Color(hex: "#93C5FD"),
    ]
}

struct TripFieldLabel: View {
    let text: String
    var body: some View {
        Text(text.uppercased())
            .font(.plusJakarta(size: 11, weight: .bold))
            .foregroundStyle(TripForm.muted)
    }
}

struct TripSheetField: View {
    @Binding var value: String
    let placeholder: String
    var singleLine: Bool = true
    var minHeight: CGFloat = 44
    var leadingIcon: String? = nil
    var trailingLabel: String? = nil
    var keyboardType: UIKeyboardType = .default
    var accent: Color = TripForm.purple

    var body: some View {
        HStack(spacing: 12) {
            if let leadingIcon {
                Image(systemName: leadingIcon)
                    .font(.system(size: 16))
                    .foregroundStyle(TripForm.muted)
            }
            ZStack(alignment: .leading) {
                if value.isEmpty {
                    Text(placeholder)
                        .font(.plusJakarta(size: 14))
                        .foregroundStyle(TripForm.muted.opacity(0.7))
                }
                if singleLine {
                    TextField("", text: $value)
                        .font(.plusJakarta(size: 14))
                        .foregroundStyle(TripForm.text)
                        .keyboardType(keyboardType)
                        .tint(accent)
                } else {
                    TextEditor(text: $value)
                        .font(.plusJakarta(size: 14))
                        .foregroundStyle(TripForm.text)
                        .frame(minHeight: max(minHeight, 72))
                        .scrollContentBackground(.hidden)
                        .tint(accent)
                }
            }
            if let trailingLabel {
                Text(trailingLabel)
                    .font(.plusJakarta(size: 12, weight: .bold))
                    .foregroundStyle(accent)
            }
        }
        .frame(minHeight: minHeight)
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .background(TripForm.field)
        .overlay(RoundedRectangle(cornerRadius: 8).stroke(TripForm.border))
        .clipShape(RoundedRectangle(cornerRadius: 8))
    }
}

struct TripAmountField: View {
    @Binding var value: String
    var currencySymbol: String = "₹"
    var accent: Color = TripForm.accent

    var body: some View {
        HStack(spacing: 12) {
            Text(currencySymbol)
                .font(.plusJakarta(size: 22, weight: .bold))
                .foregroundStyle(accent)
            TextField("0.00", text: $value)
                .font(.plusJakarta(size: 22, weight: .bold))
                .foregroundStyle(TripForm.text)
                .keyboardType(.decimalPad)
                .tint(accent)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 10)
        .background(TripForm.field)
        .overlay(RoundedRectangle(cornerRadius: 12).stroke(TripForm.border))
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }
}

struct TripDatePickField: View {
    @Binding var value: String
    var placeholder: String = "Pick date"
    var accent: Color = TripForm.purple

    @State private var showPicker = false
    @State private var draft = Date()

    private var display: String {
        value.isEmpty ? "" : SetupDateTimeUtils.formatDateDisplay(value)
    }

    var body: some View {
        Button {
            draft = SetupDateTimeUtils.dateFromIso(value.isEmpty ? nil : value)
            showPicker = true
        } label: {
            HStack(spacing: 8) {
                Image(systemName: "calendar")
                    .font(.system(size: 16))
                    .foregroundStyle(TripForm.muted)
                Text(display.isEmpty ? placeholder : display)
                    .font(.plusJakarta(size: 14))
                    .foregroundStyle(display.isEmpty ? TripForm.muted.opacity(0.7) : TripForm.text)
                Spacer(minLength: 0)
            }
            .frame(minHeight: 44)
            .padding(.horizontal, 16)
            .background(TripForm.field)
            .overlay(RoundedRectangle(cornerRadius: 8).stroke(TripForm.border))
            .clipShape(RoundedRectangle(cornerRadius: 8))
        }
        .buttonStyle(.plain)
        .sheet(isPresented: $showPicker) {
            NavigationStack {
                DatePicker("", selection: $draft, displayedComponents: .date)
                    .datePickerStyle(.graphical)
                    .labelsHidden()
                    .padding()
                    .navigationTitle("Select date")
                    .navigationBarTitleDisplayMode(.inline)
                    .toolbar {
                        ToolbarItem(placement: .cancellationAction) {
                            Button("Cancel") { showPicker = false }
                        }
                        ToolbarItem(placement: .confirmationAction) {
                            Button("OK") {
                                value = SetupDateTimeUtils.localDateString(from: draft)
                                showPicker = false
                            }
                        }
                    }
            }
            .presentationDetents([.medium, .large])
        }
    }
}

struct TripTimePickField: View {
    @Binding var value: String
    var placeholder: String = "Pick time"
    var accent: Color = TripForm.purple

    @State private var showPicker = false
    @State private var draft = Date()

    private var display: String {
        value.isEmpty ? "" : SetupDateTimeUtils.formatTimeDisplay(value)
    }

    var body: some View {
        Button {
            draft = SetupDateTimeUtils.timeFromIso(value.isEmpty ? nil : value)
            showPicker = true
        } label: {
            HStack(spacing: 8) {
                Image(systemName: "clock")
                    .font(.system(size: 16))
                    .foregroundStyle(TripForm.muted)
                Text(display.isEmpty ? placeholder : display)
                    .font(.plusJakarta(size: 14))
                    .foregroundStyle(display.isEmpty ? TripForm.muted.opacity(0.7) : TripForm.text)
                Spacer(minLength: 0)
            }
            .frame(minHeight: 44)
            .padding(.horizontal, 16)
            .background(TripForm.field)
            .overlay(RoundedRectangle(cornerRadius: 8).stroke(TripForm.border))
            .clipShape(RoundedRectangle(cornerRadius: 8))
        }
        .buttonStyle(.plain)
        .sheet(isPresented: $showPicker) {
            NavigationStack {
                DatePicker("", selection: $draft, displayedComponents: .hourAndMinute)
                    .datePickerStyle(.wheel)
                    .labelsHidden()
                    .padding()
                    .navigationTitle("Select time")
                    .navigationBarTitleDisplayMode(.inline)
                    .toolbar {
                        ToolbarItem(placement: .cancellationAction) {
                            Button("Cancel") { showPicker = false }
                        }
                        ToolbarItem(placement: .confirmationAction) {
                            Button("OK") {
                                value = SetupDateTimeUtils.localTimeString(from: draft)
                                showPicker = false
                            }
                        }
                    }
            }
            .presentationDetents([.medium])
        }
    }
}

struct TripDeadlineField: View {
    @Binding var date: String
    @Binding var time: String
    var accent: Color = TripForm.purple

    private var combinedDisplay: String {
        let d = date.isEmpty ? "" : SetupDateTimeUtils.formatDateDisplay(date)
        let t = time.isEmpty ? "" : SetupDateTimeUtils.formatTimeDisplay(time)
        if d.isEmpty && t.isEmpty { return "" }
        if t.isEmpty { return d }
        if d.isEmpty { return t }
        return "\(d) · \(t)"
    }

    @State private var showDate = false
    @State private var showTime = false
    @State private var draftDate = Date()
    @State private var draftTime = Date()

    var body: some View {
        HStack(spacing: 12) {
            HStack(spacing: 8) {
                Image(systemName: "clock")
                    .font(.system(size: 16))
                    .foregroundStyle(TripForm.muted)
                Text(combinedDisplay.isEmpty ? "Select deadline" : combinedDisplay)
                    .font(.plusJakarta(size: 14))
                    .foregroundStyle(combinedDisplay.isEmpty ? TripForm.muted.opacity(0.7) : TripForm.text)
            }
            Spacer(minLength: 0)
            Button("Set Time") {
                if date.isEmpty {
                    draftDate = Date()
                    showDate = true
                } else {
                    draftTime = SetupDateTimeUtils.timeFromIso(time.isEmpty ? nil : time)
                    showTime = true
                }
            }
            .font(.plusJakarta(size: 12, weight: .bold))
            .foregroundStyle(accent)
        }
        .frame(minHeight: 44)
        .padding(.horizontal, 16)
        .background(TripForm.field)
        .overlay(RoundedRectangle(cornerRadius: 8).stroke(TripForm.border))
        .clipShape(RoundedRectangle(cornerRadius: 8))
        .sheet(isPresented: $showDate) {
            NavigationStack {
                DatePicker("", selection: $draftDate, displayedComponents: .date)
                    .datePickerStyle(.graphical)
                    .labelsHidden()
                    .padding()
                    .navigationTitle("Poll deadline")
                    .navigationBarTitleDisplayMode(.inline)
                    .toolbar {
                        ToolbarItem(placement: .cancellationAction) { Button("Cancel") { showDate = false } }
                        ToolbarItem(placement: .confirmationAction) {
                            Button("Next") {
                                date = SetupDateTimeUtils.localDateString(from: draftDate)
                                showDate = false
                                draftTime = SetupDateTimeUtils.timeFromIso(time.isEmpty ? nil : time)
                                showTime = true
                            }
                        }
                    }
            }
            .presentationDetents([.medium, .large])
        }
        .sheet(isPresented: $showTime) {
            NavigationStack {
                DatePicker("", selection: $draftTime, displayedComponents: .hourAndMinute)
                    .datePickerStyle(.wheel)
                    .labelsHidden()
                    .padding()
                    .navigationTitle("Set time")
                    .navigationBarTitleDisplayMode(.inline)
                    .toolbar {
                        ToolbarItem(placement: .cancellationAction) { Button("Cancel") { showTime = false } }
                        ToolbarItem(placement: .confirmationAction) {
                            Button("OK") {
                                time = SetupDateTimeUtils.localTimeString(from: draftTime)
                                showTime = false
                            }
                        }
                    }
            }
            .presentationDetents([.medium])
        }
    }
}

struct TripDateRangeField: View {
    @Binding var start: String
    @Binding var end: String

    @State private var showStart = false
    @State private var showEnd = false
    @State private var draft = Date()

    private var display: String {
        SetupDateTimeUtils.formatDateRangeDisplay(startIso: start, endIso: end)
    }

    var body: some View {
        Button {
            if start.isEmpty {
                draft = Date()
                showStart = true
            } else if end.isEmpty {
                draft = SetupDateTimeUtils.dateFromIso(start)
                showEnd = true
            } else {
                draft = SetupDateTimeUtils.dateFromIso(start)
                showStart = true
            }
        } label: {
            HStack(spacing: 8) {
                Image(systemName: "calendar")
                    .font(.system(size: 16))
                    .foregroundStyle(TripForm.muted)
                Text(display == "Select date" ? "Check-in / Check-out" : display)
                    .font(.plusJakarta(size: 14))
                    .foregroundStyle(TripForm.text)
                Spacer(minLength: 0)
            }
            .frame(minHeight: 44)
            .padding(.horizontal, 16)
            .background(TripForm.field)
            .overlay(RoundedRectangle(cornerRadius: 8).stroke(TripForm.border))
            .clipShape(RoundedRectangle(cornerRadius: 8))
        }
        .buttonStyle(.plain)
        .sheet(isPresented: $showStart) {
            NavigationStack {
                DatePicker("Check-in", selection: $draft, displayedComponents: .date)
                    .datePickerStyle(.graphical)
                    .labelsHidden()
                    .padding()
                    .navigationTitle("Check-in")
                    .navigationBarTitleDisplayMode(.inline)
                    .toolbar {
                        ToolbarItem(placement: .cancellationAction) { Button("Cancel") { showStart = false } }
                        ToolbarItem(placement: .confirmationAction) {
                            Button("Next") {
                                start = SetupDateTimeUtils.localDateString(from: draft)
                                showStart = false
                                draft = end.isEmpty ? draft : SetupDateTimeUtils.dateFromIso(end)
                                showEnd = true
                            }
                        }
                    }
            }
            .presentationDetents([.medium, .large])
        }
        .sheet(isPresented: $showEnd) {
            NavigationStack {
                DatePicker("Check-out", selection: $draft, displayedComponents: .date)
                    .datePickerStyle(.graphical)
                    .labelsHidden()
                    .padding()
                    .navigationTitle("Check-out")
                    .navigationBarTitleDisplayMode(.inline)
                    .toolbar {
                        ToolbarItem(placement: .cancellationAction) { Button("Cancel") { showEnd = false } }
                        ToolbarItem(placement: .confirmationAction) {
                            Button("OK") {
                                end = SetupDateTimeUtils.localDateString(from: draft)
                                showEnd = false
                            }
                        }
                    }
            }
            .presentationDetents([.medium, .large])
        }
    }
}

struct TripChipRow: View {
    let options: [String]
    @Binding var selected: String
    var accent: Color = TripForm.accent

    var body: some View {
        FlowLayout(spacing: 8) {
            ForEach(options, id: \.self) { option in
                let on = selected == option
                Button { selected = option } label: {
                    Text(option)
                        .font(.plusJakarta(size: 12, weight: .semibold))
                        .foregroundStyle(on ? accent : TripForm.muted)
                        .padding(.horizontal, 14)
                        .padding(.vertical, 8)
                        .background(on ? accent.opacity(0.15) : TripForm.field)
                        .overlay(
                            Capsule().stroke(on ? accent : TripForm.border, lineWidth: on ? 1 : 0)
                        )
                        .clipShape(Capsule())
                }
                .buttonStyle(.plain)
            }
        }
    }
}

struct TripMultiChipRow: View {
    let options: [String]
    @Binding var selected: Set<String>
    var accent: Color = TripForm.accent

    var body: some View {
        FlowLayout(spacing: 8) {
            ForEach(options, id: \.self) { option in
                let on = selected.contains(option)
                Button {
                    if on { selected.remove(option) } else { selected.insert(option) }
                } label: {
                    Text(option)
                        .font(.plusJakarta(size: 11, weight: .semibold))
                        .foregroundStyle(on ? accent : TripForm.muted)
                        .padding(.horizontal, 10)
                        .padding(.vertical, 6)
                        .background(on ? accent.opacity(0.15) : TripForm.field)
                        .overlay(RoundedRectangle(cornerRadius: 8).stroke(on ? accent : TripForm.border))
                        .clipShape(RoundedRectangle(cornerRadius: 8))
                }
                .buttonStyle(.plain)
            }
        }
    }
}

struct TripSegmentedControl: View {
    let options: [String]
    @Binding var selected: String
    var accent: Color = TripForm.accent

    var body: some View {
        HStack(spacing: 2) {
            ForEach(options, id: \.self) { option in
                let on = selected == option
                Button { selected = option } label: {
                    Text(option)
                        .font(.plusJakarta(size: 12, weight: on ? .bold : .semibold))
                        .foregroundStyle(on ? TripForm.text : TripForm.muted)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 8)
                        .background(on ? accent : Color.clear)
                        .clipShape(RoundedRectangle(cornerRadius: 8))
                }
                .buttonStyle(.plain)
            }
        }
        .padding(2)
        .background(TripForm.field)
        .overlay(RoundedRectangle(cornerRadius: 10).stroke(TripForm.border))
        .clipShape(RoundedRectangle(cornerRadius: 10))
    }
}

struct TripPickerField<T: Hashable & CustomStringConvertible>: View {
    let label: String
    let options: [T]
    @Binding var selection: T
    var accent: Color = TripForm.purple

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            TripFieldLabel(text: label)
            Menu {
                ForEach(options, id: \.self) { opt in
                    Button(opt.description) { selection = opt }
                }
            } label: {
                HStack {
                    Text(selection.description)
                        .font(.plusJakarta(size: 13, weight: .semibold))
                        .foregroundStyle(TripForm.text)
                    Spacer()
                    Image(systemName: "chevron.down")
                        .font(.system(size: 12, weight: .semibold))
                        .foregroundStyle(TripForm.muted)
                }
                .padding(12)
                .background(TripForm.field)
                .overlay(RoundedRectangle(cornerRadius: 10).stroke(TripForm.border))
                .clipShape(RoundedRectangle(cornerRadius: 10))
            }
        }
    }
}

struct TripParticipantPicker: View {
    let participants: [APIClient.GroupParticipantPayload]
    @Binding var selectedIds: Set<String>
    var accent: Color = TripForm.accent

    var body: some View {
        HStack(spacing: 0) {
            ForEach(Array(participants.enumerated()), id: \.element.participantId) { index, p in
                let id = p.participantId
                let on = selectedIds.contains(id)
                let name = p.displayName ?? String(id.prefix(8))
                let color = TripForm.avatarColors[index % TripForm.avatarColors.count]
                VStack(spacing: 6) {
                    ZStack(alignment: .bottomTrailing) {
                        Circle()
                            .fill(color)
                            .frame(width: 44, height: 44)
                            .overlay(
                                Text(tripInitials(name))
                                    .font(.plusJakarta(size: 14, weight: .bold))
                                    .foregroundStyle(Color(hex: "#14121B"))
                            )
                            .overlay(Circle().stroke(on ? accent : Color.clear, lineWidth: 2))
                        if on {
                            Circle()
                                .fill(accent)
                                .frame(width: 16, height: 16)
                                .overlay(
                                    Image(systemName: "checkmark")
                                        .font(.system(size: 8, weight: .bold))
                                        .foregroundStyle(.white)
                                )
                        }
                    }
                    Text(name.components(separatedBy: " ").first ?? name)
                        .font(.plusJakarta(size: 11, weight: .medium))
                        .foregroundStyle(on ? TripForm.text : TripForm.muted)
                        .lineLimit(1)
                }
                .frame(maxWidth: .infinity)
                .onTapGesture {
                    if on { selectedIds.remove(id) } else { selectedIds.insert(id) }
                }
            }
        }
    }
}

struct TripSheetHeader: View {
    let iconAsset: String
    let title: String
    let subtitle: String
    var accent: Color = TripForm.accent

    var body: some View {
        HStack(alignment: .center, spacing: 12) {
            GroupQaIcons.tileIcon(asset: iconAsset, size: 18)
                .frame(width: 36, height: 36)
                .background(accent.opacity(0.18))
                .overlay(RoundedRectangle(cornerRadius: 18).stroke(accent.opacity(0.35)))
                .clipShape(RoundedRectangle(cornerRadius: 18))
            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(.plusJakarta(size: 18, weight: .heavy))
                    .foregroundStyle(TripForm.text)
                Text(subtitle)
                    .font(.plusJakarta(size: 12))
                    .foregroundStyle(TripForm.muted)
            }
        }
    }
}

struct TripPrimaryCta: View {
    let label: String
    var enabled: Bool = true
    var loading: Bool = false
    var footer: String? = nil
    var colors: [Color] = [TripForm.accent, TripForm.accentEnd]
    var onTap: () -> Void

    var body: some View {
        VStack(spacing: 8) {
            Button(action: onTap) {
                Group {
                    if loading {
                        ProgressView().tint(.white)
                    } else {
                        Text(label)
                            .font(.plusJakarta(size: 15, weight: .bold))
                            .foregroundStyle(.white)
                    }
                }
                .frame(maxWidth: .infinity)
                .padding(.vertical, 14)
                .background(LinearGradient(colors: colors, startPoint: .leading, endPoint: .trailing))
                .clipShape(RoundedRectangle(cornerRadius: 12))
            }
            .buttonStyle(.plain)
            .disabled(!enabled || loading)
            .opacity(enabled ? 1 : 0.55)
            if let footer {
                Text(footer)
                    .font(.plusJakarta(size: 11))
                    .foregroundStyle(TripForm.muted)
                    .multilineTextAlignment(.center)
            }
        }
    }
}

struct TripToggleRow: View {
    let title: String
    let subtitle: String
    @Binding var isOn: Bool
    var accent: Color = TripForm.purple

    var body: some View {
        HStack {
            VStack(alignment: .leading, spacing: 2) {
                Text(title).font(.plusJakarta(size: 14, weight: .semibold)).foregroundStyle(TripForm.text)
                Text(subtitle).font(.plusJakarta(size: 11)).foregroundStyle(TripForm.muted)
            }
            Spacer()
            Toggle("", isOn: $isOn).labelsHidden().tint(accent)
        }
    }
}

struct TripMoodRow: View {
    let moods: [String]
    @Binding var selected: String
    var accent: Color = TripForm.pink

    var body: some View {
        HStack(spacing: 12) {
            ForEach(moods, id: \.self) { mood in
                let on = selected == mood
                Button { selected = mood } label: {
                    Text(mood)
                        .font(.system(size: 18))
                        .frame(width: 38, height: 38)
                        .background(on ? accent.opacity(0.15) : TripForm.field)
                        .overlay(
                            Circle().stroke(on ? accent : TripForm.border, lineWidth: on ? 1 : 0)
                        )
                        .clipShape(Circle())
                }
                .buttonStyle(.plain)
            }
        }
    }
}

func tripInitials(_ name: String) -> String {
    let parts = name.trimmingCharacters(in: .whitespacesAndNewlines)
        .components(separatedBy: .whitespaces).filter { !$0.isEmpty }
    if parts.count >= 2 {
        return "\(parts[0].prefix(1))\(parts[1].prefix(1))".uppercased()
    }
    if let first = parts.first { return first.prefix(2).uppercased() }
    return "??"
}
