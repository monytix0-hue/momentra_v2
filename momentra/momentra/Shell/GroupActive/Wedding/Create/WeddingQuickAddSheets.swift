import SwiftUI
import PhotosUI
import UIKit
import UniformTypeIdentifiers
import CoreTransferable

/// Figma 589:8755 — Wedding Quick Add sheets (exact Android match).
/// Live kinds (expense/contribution/budget) submit when APIs allow;
/// gap kinds render full Figma UI with no-op CTA.
struct WeddingGapQuickAddSheet: View {
    let kind: WeddingQuickAddKind
    var momentId: String? = nil
    var onClose: () -> Void
    var onSaved: () -> Void = {}

    var body: some View {
        NativeSheetScaffold(
            title: kind.label,
            onClose: onClose,
            background: Color(hex: "#1C1A24")
        ) {
            ScrollView {
                sheetBody
                    .padding(.horizontal, 20)
                    .padding(.top, 16)
            }
        }
        .presentationDetents([.large])
        .presentationDragIndicator(.visible)
    }

    @ViewBuilder
    private var sheetBody: some View {
        switch kind {
        case .expense: WeddingExpenseBody(momentId: momentId, onDismiss: onClose, onSaved: onSaved)
        case .contribution: WeddingContributionBody(momentId: momentId, onDismiss: onClose, onSaved: onSaved)
        case .budget: WeddingBudgetBody(momentId: momentId, onDismiss: onClose, onSaved: onSaved)
        case .participant: WeddingParticipantBody(momentId: momentId, onDismiss: onClose, onSaved: onSaved)
        case .vendor: WeddingVendorBody(momentId: momentId, onDismiss: onClose, onSaved: onSaved)
        case .planning: WeddingPlanningBody(momentId: momentId, onDismiss: onClose, onSaved: onSaved)
        case .attendance: WeddingAttendanceBody(momentId: momentId, onDismiss: onClose, onSaved: onSaved)
        case .poll: WeddingPollBody(momentId: momentId, onDismiss: onClose, onSaved: onSaved)
        case .memory: WeddingMemoryBody(momentId: momentId, onDismiss: onClose, onSaved: onSaved)
        case .update: WeddingUpdateBody(momentId: momentId, onDismiss: onClose, onSaved: onSaved)
        case .settle: WeddingSettleBody(momentId: momentId, onDismiss: onClose, onSaved: onSaved)
        }
    }
}

// MARK: - Tokens & Accents

private enum Wq {
    static let sheet = Color(hex: "#1C1A24")
    static let field = Color(hex: "#252230")
    static let border = Color(hex: "#322E40")
    static let muted = Color(hex: "#9E9AA8")
    static let text = Color(hex: "#FFFFFF")
    static let ink = Color(hex: "#14121B")
    static let handle = Color(hex: "#625E70")
    static let avatarColors: [Color] = [
        Color(hex: "#FDBA74"), Color(hex: "#86EFAC"), Color(hex: "#F9A8D4"), Color(hex: "#93C5FD"),
    ]
}

struct SheetAccent {
    let accent: Color
    let accentEnd: Color
    let soft: Color
    var cta: LinearGradient {
        LinearGradient(colors: [accent, accentEnd], startPoint: .leading, endPoint: .trailing)
    }
}

let purpleAccent = SheetAccent(
    accent: Color(hex: "#BF26D4"),
    accentEnd: Color(hex: "#871A8F"),
    soft: Color(hex: "#BF26D4").opacity(0.13)
)

let coralAccent = SheetAccent(
    accent: Color(hex: "#FC7085"),
    accentEnd: Color(hex: "#E83359"),
    soft: Color(hex: "#FC7085").opacity(0.13)
)

let softPinkAccent = SheetAccent(
    accent: Color(hex: "#ED8CB8"),
    accentEnd: Color(hex: "#D14D85"),
    soft: Color(hex: "#ED8CB8").opacity(0.13)
)

let contribAccent = SheetAccent(
    accent: Color(hex: "#D945F0"),
    accentEnd: Color(hex: "#A31CB0"),
    soft: Color(hex: "#D945F0").opacity(0.15)
)

// MARK: - Reusable Components

struct SheetHeader: View {
    let icon: String
    let title: String
    var subtitle: String? = nil
    var accent: SheetAccent = purpleAccent

    var body: some View {
        HStack(spacing: 12) {
            Circle()
                .fill(accent.soft)
                .frame(width: 36, height: 36)
                .overlay(
                    Image(systemName: icon)
                        .font(.system(size: 18, weight: .medium))
                        .foregroundStyle(accent.accent)
                )
            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(.plusJakarta(size: 20, weight: .heavy))
                    .foregroundStyle(Wq.text)
                if let subtitle {
                    Text(subtitle)
                        .font(.plusJakarta(size: 12))
                        .foregroundStyle(Wq.muted)
                }
            }
        }
    }
}

struct FieldLabel: View {
    let text: String
    var body: some View {
        Text(text.uppercased())
            .font(.plusJakarta(size: 12, weight: .semibold))
            .foregroundStyle(Wq.muted)
    }
}

struct SheetField: View {
    @Binding var value: String
    let placeholder: String
    var singleLine: Bool = true
    var minHeight: CGFloat = 48
    var leading: (() -> AnyView)? = nil
    var trailing: (() -> AnyView)? = nil
    var keyboardType: UIKeyboardType = .default
    var textColor: Color = Wq.text

    var body: some View {
        HStack(spacing: 12) {
            if let leading { leading() }
            ZStack(alignment: .leading) {
                if value.isEmpty {
                    Text(placeholder)
                        .font(.plusJakarta(size: 14))
                        .foregroundStyle(Wq.muted.opacity(0.7))
                }
                if singleLine {
                    TextField("", text: $value)
                        .font(.plusJakarta(size: 14, weight: .medium))
                        .foregroundStyle(textColor)
                        .keyboardType(keyboardType)
                        .tint(purpleAccent.accent)
                } else {
                    TextEditor(text: $value)
                        .font(.plusJakarta(size: 14, weight: .medium))
                        .foregroundStyle(textColor)
                        .frame(minHeight: max(minHeight, 72))
                        .scrollContentBackground(.hidden)
                        .tint(purpleAccent.accent)
                }
            }
            if let trailing { trailing() }
        }
        .frame(minHeight: minHeight)
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .background(Wq.field)
        .clipShape(RoundedRectangle(cornerRadius: 12))
        .overlay(RoundedRectangle(cornerRadius: 12).stroke(Wq.border))
    }
}

struct WeddingDatePickField: View {
    @Binding var value: String
    var placeholder: String = "Select date"

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
            HStack(spacing: 12) {
                Image(systemName: "calendar")
                    .font(.system(size: 16))
                    .foregroundStyle(Wq.muted)
                Text(display.isEmpty ? placeholder : display)
                    .font(.plusJakarta(size: 14, weight: .medium))
                    .foregroundStyle(display.isEmpty ? Wq.muted.opacity(0.7) : Wq.text)
                Spacer(minLength: 0)
            }
            .frame(minHeight: 48)
            .padding(.horizontal, 16)
            .padding(.vertical, 12)
            .background(Wq.field)
            .clipShape(RoundedRectangle(cornerRadius: 12))
            .overlay(RoundedRectangle(cornerRadius: 12).stroke(Wq.border))
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

private struct WeddingTimePickField: View {
    @Binding var value: String
    var placeholder: String = "Select time"

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
            HStack(spacing: 12) {
                Text(display.isEmpty ? placeholder : display)
                    .font(.plusJakarta(size: 14, weight: .medium))
                    .foregroundStyle(display.isEmpty ? Wq.muted.opacity(0.7) : Wq.text)
                Spacer(minLength: 0)
            }
            .frame(minHeight: 48)
            .padding(.horizontal, 16)
            .padding(.vertical, 12)
            .background(Wq.field)
            .clipShape(RoundedRectangle(cornerRadius: 12))
            .overlay(RoundedRectangle(cornerRadius: 12).stroke(Wq.border))
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

struct ChipRow: View {
    let options: [String]
    @Binding var selected: String
    var accent: SheetAccent = purpleAccent

    var body: some View {
        FlowLayout(spacing: 8) {
            ForEach(options, id: \.self) { option in
                let on = selected == option
                Button {
                    selected = option
                } label: {
                    Text(option)
                        .font(.plusJakarta(size: 12, weight: on ? .bold : .medium))
                        .foregroundStyle(on ? accent.accent : Wq.muted)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 6)
                        .background(on ? accent.soft : Wq.field)
                        .clipShape(Capsule())
                        .overlay(Capsule().stroke(on ? accent.accent : Wq.border))
                }
                .buttonStyle(.plain)
            }
        }
    }
}

struct Segmented: View {
    let options: [String]
    @Binding var selected: String
    var accent: SheetAccent = purpleAccent
    var selectedTextLight: Bool = false

    var body: some View {
        HStack(spacing: 4) {
            ForEach(options, id: \.self) { option in
                let on = selected == option
                Button {
                    selected = option
                } label: {
                    Text(option)
                        .font(.plusJakarta(size: 13, weight: on ? .bold : .semibold))
                        .foregroundStyle(on ? (selectedTextLight ? Wq.text : Wq.ink) : Wq.muted)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 8)
                        .background(on ? accent.accent : Color.clear)
                        .clipShape(RoundedRectangle(cornerRadius: 8))
                }
                .buttonStyle(.plain)
            }
        }
        .padding(4)
        .background(Wq.field)
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }
}

struct PrimaryCta: View {
    let label: String
    var enabled: Bool
    var accent: SheetAccent = purpleAccent
    var loading: Bool = false
    var footer: String? = nil
    var lightLabel: Bool = false
    var onClick: () -> Void

    var body: some View {
        VStack(spacing: 8) {
            Button(action: onClick) {
                ZStack {
                    if loading {
                        ProgressView()
                            .progressViewStyle(CircularProgressViewStyle(tint: lightLabel ? Wq.text : Wq.ink))
                    } else {
                        Text(label)
                            .font(.plusJakarta(size: 16, weight: .bold))
                            .foregroundStyle(lightLabel ? Wq.text : Wq.ink)
                    }
                }
                .frame(maxWidth: .infinity)
                .padding(.vertical, 14)
                .background(accent.cta)
                .clipShape(RoundedRectangle(cornerRadius: 14))
            }
            .buttonStyle(.plain)
            .disabled(!enabled || loading)

            if let footer {
                Text(footer)
                    .font(.plusJakarta(size: 12))
                    .foregroundStyle(Wq.handle)
            }
        }
    }
}

private func initialsOf(_ name: String) -> String {
    let parts = name.trimmingCharacters(in: .whitespacesAndNewlines).components(separatedBy: .whitespaces).filter { !$0.isEmpty }
    if parts.count >= 2 {
        return "\(parts[0].prefix(1))\(parts[1].prefix(1))".uppercased()
    } else if let first = parts.first {
        return first.prefix(2).uppercased()
    }
    return "??"
}

struct AvatarPick: View {
    let people: [(id: String, name: String)]
    @Binding var selected: Set<String>
    var accent: SheetAccent = purpleAccent
    var onToggle: (String) -> Void

    var body: some View {
        HStack(spacing: 0) {
            ForEach(Array(people.enumerated()), id: \.element.id) { index, person in
                let on = selected.contains(person.id)
                let color = Wq.avatarColors[index % Wq.avatarColors.count]
                VStack(spacing: 6) {
                    ZStack(alignment: .bottomTrailing) {
                        Circle()
                            .fill(color)
                            .frame(width: 44, height: 44)
                            .overlay(
                                Text(initialsOf(person.name))
                                    .font(.plusJakarta(size: 14, weight: .bold))
                                    .foregroundStyle(Wq.ink)
                            )
                            .overlay(
                                Circle().stroke(on ? accent.accent : Color.clear, lineWidth: 2)
                            )
                        if on {
                            RoundedRectangle(cornerRadius: 8)
                                .fill(accent.accent)
                                .frame(width: 16, height: 16)
                                .overlay(
                                    Text("✓")
                                        .font(.system(size: 9, weight: .bold))
                                        .foregroundStyle(.white)
                                )
                        }
                    }
                    Text(person.name.components(separatedBy: " ").first ?? person.name)
                        .font(.plusJakarta(size: 11, weight: .medium))
                        .foregroundStyle(on ? Wq.text : Wq.muted)
                        .lineLimit(1)
                }
                .frame(maxWidth: .infinity)
                .onTapGesture { onToggle(person.id) }
            }
        }
    }
}

// MARK: - 1. EXPENSE

struct WeddingExpenseBody: View {
    var momentId: String?
    var onDismiss: () -> Void
    var onSaved: () -> Void

    @State private var amount = ""
    @State private var description = ""
    @State private var expenseDate = ""
    @State private var splitType = "Equal"
    @State private var category = GroupExpenseCategoryCatalog.defaultCategory(for: "WEDDING")
    @State private var participants: [APIClient.GroupParticipantPayload] = []
    @State private var selected: Set<String> = []
    @State private var paidBy: String? = nil
    @State private var loading = false
    @State private var submitting = false
    @State private var error: String? = nil

    var accent: SheetAccent = purpleAccent

    private var people: [(id: String, name: String)] {
        participants.map { (id: $0.participantId, name: $0.displayName ?? String($0.participantId.prefix(8))) }
    }

    private var live: Bool { momentId != nil }

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            SheetHeader(icon: "creditcard.fill", title: "Add Expense", accent: accent)
            
            HStack(alignment: .bottom, spacing: 6) {
                Text("₹")
                    .font(.plusJakarta(size: 28, weight: .bold))
                    .foregroundStyle(accent.accent)
                ZStack(alignment: .leading) {
                    if amount.isEmpty {
                        Text("0.00")
                            .font(.plusJakarta(size: 40, weight: .heavy))
                            .foregroundStyle(Wq.text.opacity(0.35))
                    }
                    TextField("", text: $amount)
                        .font(.plusJakarta(size: 40, weight: .heavy))
                        .foregroundStyle(Wq.text)
                        .keyboardType(.decimalPad)
                        .tint(accent.accent)
                        .onChange(of: amount) { _, new in
                            amount = new.filter { $0.isNumber || $0 == "." }
                        }
                }
            }
            .frame(maxWidth: .infinity)

            VStack(alignment: .leading, spacing: 8) {
                FieldLabel(text: "Description")
                SheetField(value: $description, placeholder: "What was this for?")
            }

            HStack(spacing: 14) {
                VStack(alignment: .leading, spacing: 8) {
                    FieldLabel(text: "Paid By")
                    SheetField(
                        value: .constant(participants.first(where: { $0.participantId == paidBy })?.displayName ?? "Select"),
                        placeholder: "Select",
                        trailing: { AnyView(Image(systemName: "chevron.down").font(.system(size: 12)).foregroundStyle(Wq.muted)) }
                    )
                }
                VStack(alignment: .leading, spacing: 8) {
                    FieldLabel(text: "Date")
                    WeddingDatePickField(value: $expenseDate, placeholder: "Today")
                }
            }

            VStack(alignment: .leading, spacing: 8) {
                FieldLabel(text: "Split Between")
                if loading {
                    ProgressView().tint(accent.accent)
                } else if people.isEmpty {
                    Text("No participants yet")
                        .font(.plusJakarta(size: 12))
                        .foregroundStyle(Wq.muted)
                } else {
                    AvatarPick(people: people, selected: $selected, accent: accent) { id in
                        if selected.contains(id) {
                            selected.remove(id)
                        } else {
                            selected.insert(id)
                        }
                        if paidBy == nil { paidBy = id }
                    }
                }
            }

            VStack(alignment: .leading, spacing: 8) {
                FieldLabel(text: "Split Type")
                Segmented(options: ["Equal", "Custom", "% Percent"], selected: $splitType, accent: accent)
            }

            VStack(alignment: .leading, spacing: 8) {
                FieldLabel(text: "Category")
                ChipRow(
                    options: GroupExpenseCategoryCatalog.categories(for: "WEDDING"),
                    selected: $category,
                    accent: accent
                )
                .accessibilityIdentifier("group.expense.category")
            }

            if let error {
                Text(error)
                    .font(.plusJakarta(size: 12))
                    .foregroundStyle(Color(hex: "#F87171"))
            }

            PrimaryCta(
                label: "Add Expense",
                enabled: live && isValid,
                accent: accent,
                loading: submitting,
                footer: "Everyone will be notified"
            ) {
                submit()
            }
        }
        .onAppear {
            if let momentId {
                Task { await loadParticipants(momentId) }
            }
        }
    }

    private var isValid: Bool {
        guard let amt = Decimal(string: amount), amt > 0, !selected.isEmpty else { return false }
        return true
    }

    private func loadParticipants(_ momentId: String) async {
        loading = true
        do {
            let list = try await APIClient.shared.listGroupParticipants(momentId: momentId)
            let active = list.filter { $0.status == "ACTIVE" || $0.status == "INVITED" }
            participants = active.isEmpty ? list : active
            selected = Set(participants.map(\.participantId))
            paidBy = participants.first?.participantId
        } catch {
            self.error = error.localizedDescription
        }
        loading = false
    }

    private func submit() {
        guard let momentId, let payer = paidBy ?? selected.first, splitType == "Equal" else { return }
        Task {
            submitting = true
            error = nil
            do {
                let splits = selected.map { APIClient.GroupSplitInput(participantId: $0, shares: 1.0) }
                _ = try await APIClient.shared.createGroupExpense(
                    momentId: momentId,
                    amount: amount,
                    currencyCode: "INR",
                    description: GroupExpenseCategoryCatalog.descriptionWithCategory(
                        category: category,
                        userDescription: description
                    ),
                    paidByParticipantId: payer,
                    splitStrategy: "EQUAL",
                    splitInputs: splits
                )
                submitting = false
                onSaved()
                onDismiss()
            } catch {
                submitting = false
                self.error = error.localizedDescription
            }
        }
    }
}

// MARK: - 2. CONTRIBUTION

struct WeddingContributionBody: View {
    var momentId: String?
    var onDismiss: () -> Void
    var onSaved: () -> Void

    @State private var amount = ""
    @State private var pool = ""
    @State private var method = "UPI"
    @State private var status = "Paid"
    @State private var submitting = false
    @State private var error: String? = nil

    var accent: SheetAccent = contribAccent
    private var live: Bool { momentId != nil }

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            SheetHeader(icon: "person.2.fill", title: "Add Contribution", accent: accent)

            VStack(alignment: .leading, spacing: 8) {
                FieldLabel(text: "Amount Contributed")
                SheetField(
                    value: $amount,
                    placeholder: "0.00",
                    leading: {
                        AnyView(
                            Text("₹")
                                .font(.plusJakarta(size: 22, weight: .bold))
                                .foregroundStyle(accent.accent)
                        )
                    },
                    keyboardType: .decimalPad
                )
            }

            HStack(spacing: 12) {
                VStack(alignment: .leading, spacing: 8) {
                    FieldLabel(text: "Contribution For")
                    SheetField(
                        value: $pool,
                        placeholder: "Label (optional)",
                        trailing: {
                            AnyView(
                                Image(systemName: "chevron.down")
                                    .font(.system(size: 12))
                                    .foregroundStyle(Wq.muted)
                            )
                        }
                    )
                }
                VStack(alignment: .leading, spacing: 8) {
                    FieldLabel(text: "From")
                    HStack(spacing: 8) {
                        RoundedRectangle(cornerRadius: 12)
                            .fill(accent.soft)
                            .frame(width: 24, height: 24)
                            .overlay(
                                Text("Y")
                                    .font(.plusJakarta(size: 10, weight: .bold))
                                    .foregroundStyle(accent.accent)
                            )
                            .overlay(
                                RoundedRectangle(cornerRadius: 12).stroke(accent.accent, lineWidth: 1)
                            )
                        Text("You")
                            .font(.plusJakarta(size: 13, weight: .semibold))
                            .foregroundStyle(Wq.text)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .frame(height: 48)
                    .padding(.horizontal, 8)
                    .background(Wq.field)
                    .clipShape(RoundedRectangle(cornerRadius: 10))
                    .overlay(RoundedRectangle(cornerRadius: 10).stroke(Wq.border))
                }
            }

            VStack(alignment: .leading, spacing: 8) {
                FieldLabel(text: "Payment Method")
                ChipRow(options: ["UPI", "Bank Transfer", "Cash", "Card"], selected: $method, accent: accent)
            }

            HStack(spacing: 12) {
                VStack(alignment: .leading, spacing: 8) {
                    FieldLabel(text: "Status")
                    Segmented(options: ["Paid", "Pending"], selected: $status, accent: accent, selectedTextLight: true)
                }
                VStack(alignment: .leading, spacing: 8) {
                    FieldLabel(text: "Receipt")
                    ZStack {
                        Text("📎 Attach PDF/Img")
                            .font(.plusJakarta(size: 11, weight: .semibold))
                            .foregroundStyle(Wq.muted)
                    }
                    .frame(maxWidth: .infinity)
                    .frame(height: 40)
                    .background(Wq.field)
                    .clipShape(RoundedRectangle(cornerRadius: 10))
                    .overlay(RoundedRectangle(cornerRadius: 10).stroke(Wq.border))
                }
            }

            if let error {
                Text(error)
                    .font(.plusJakarta(size: 12))
                    .foregroundStyle(Color(hex: "#F87171"))
            }

            PrimaryCta(
                label: "Add Contribution",
                enabled: live && isValid,
                accent: accent,
                loading: submitting,
                footer: "Balance will be updated for everyone",
                lightLabel: true
            ) {
                submit()
            }
        }
    }

    private var isValid: Bool {
        guard let amt = Decimal(string: amount.replacingOccurrences(of: ",", with: "")), amt > 0 else { return false }
        return true
    }

    private func submit() {
        guard let momentId else { return }
        Task {
            submitting = true
            error = nil
            do {
                _ = try await APIClient.shared.recordContribution(
                    momentId: momentId,
                    amount: amount.replacingOccurrences(of: ",", with: ""),
                    currencyCode: "INR",
                    label: pool.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? nil : pool
                )
                submitting = false
                onSaved()
                onDismiss()
            } catch {
                submitting = false
                self.error = error.localizedDescription
            }
        }
    }
}

// MARK: - 3. BUDGET

struct WeddingBudgetBody: View {
    var momentId: String?
    var onDismiss: () -> Void
    var onSaved: () -> Void

    @State private var amount = ""
    @State private var strategy = "Increase"
    @State private var submitting = false
    @State private var error: String? = nil
    @State private var currentBudget: String? = nil
    @State private var spent: String? = nil

    var accent: SheetAccent = coralAccent
    private var live: Bool { momentId != nil }

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            SheetHeader(icon: "chart.line.uptrend.xyaxis", title: "Update Budget", accent: accent)

            HStack(spacing: 16) {
                VStack(alignment: .leading, spacing: 4) {
                    Text("Current Budget Status")
                        .font(.plusJakarta(size: 12))
                        .foregroundStyle(Wq.muted)
                    Text(currentBudget.map { "₹\($0)" } ?? "No budget set")
                        .font(.plusJakarta(size: 20, weight: .heavy))
                        .foregroundStyle(Wq.text)
                    Text(spent.map { "₹\($0) spent" } ?? "No expenses yet")
                        .font(.plusJakarta(size: 11))
                        .foregroundStyle(Wq.handle)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(16)
            .background(Wq.field)
            .clipShape(RoundedRectangle(cornerRadius: 16))
            .overlay(RoundedRectangle(cornerRadius: 16).stroke(Wq.border))

            VStack(alignment: .leading, spacing: 8) {
                FieldLabel(text: "New Budget Amount")
                SheetField(
                    value: $amount,
                    placeholder: "0.00",
                    leading: {
                        AnyView(
                            Image(systemName: "creditcard.fill")
                                .font(.system(size: 16))
                                .foregroundStyle(Wq.muted)
                        )
                    },
                    keyboardType: .decimalPad
                )
            }

            VStack(alignment: .leading, spacing: 8) {
                FieldLabel(text: "Adjustment Strategy")
                Segmented(options: ["Increase", "Decrease", "Replace"], selected: $strategy, accent: accent)
            }

            if let error {
                Text(error)
                    .font(.plusJakarta(size: 12))
                    .foregroundStyle(Color(hex: "#F87171"))
            }

            PrimaryCta(
                label: "Update Budget",
                enabled: live && isValid,
                accent: accent,
                loading: submitting,
                footer: "All members will see the update"
            ) {
                submit()
            }
        }
        .onAppear {
            if let momentId {
                Task { await loadFinance(momentId) }
            }
        }
    }

    private var isValid: Bool {
        guard let amt = Decimal(string: amount.replacingOccurrences(of: ",", with: "")), amt > 0 else { return false }
        return true
    }

    private func loadFinance(_ momentId: String) async {
        do {
            let finance = try await APIClient.shared.getGroupFinance(momentId: momentId)
            let total = finance.payload?.totals?.first
            currentBudget = total?.budgetTotal
            spent = total?.expenseTotal
        } catch {
            // Keep empty honest status when finance cannot load.
        }
    }

    private func submit() {
        guard let momentId else { return }
        Task {
            submitting = true
            error = nil
            do {
                _ = try await APIClient.shared.patchGroupBudget(
                    momentId: momentId,
                    budgetAmount: amount.replacingOccurrences(of: ",", with: ""),
                    budgetCurrencyCode: "INR"
                )
                submitting = false
                onSaved()
                onDismiss()
            } catch {
                submitting = false
                self.error = error.localizedDescription
            }
        }
    }
}

// MARK: - 4. PARTICIPANT

private struct WeddingParticipantBody: View {
    var momentId: String?
    var onDismiss: () -> Void = {}
    var onSaved: () -> Void = {}

    @State private var name = ""
    @State private var contact = ""
    @State private var affiliation = "Bride's Side"
    @State private var rsvp = "Pending"
    @State private var plusOne = false
    @State private var notes = ""
    @State private var submitting = false
    @State private var error: String?

    private let accent = softPinkAccent

    private var canSave: Bool {
        momentId != nil && !name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty && !submitting
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            SheetHeader(icon: "person.2.fill", title: "Add Participant", subtitle: "Invite and manage wedding team and guest list", accent: accent)

            VStack(alignment: .leading, spacing: 8) {
                FieldLabel(text: "Participant Name")
                SheetField(value: $name, placeholder: "Full name", minHeight: 42)
            }

            VStack(alignment: .leading, spacing: 8) {
                FieldLabel(text: "Email or Phone")
                SheetField(value: $contact, placeholder: "Email or phone", minHeight: 42, keyboardType: .emailAddress)
            }

            VStack(alignment: .leading, spacing: 8) {
                FieldLabel(text: "Affiliation / Role")
                ChipRow(options: ["Bride's Side", "Groom's Side", "Family"], selected: $affiliation, accent: accent)
            }

            VStack(alignment: .leading, spacing: 8) {
                FieldLabel(text: "RSVP Status")
                ChipRow(options: ["Confirmed", "Pending", "Declined"], selected: $rsvp, accent: accent)
            }

            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text("Plus One Allowed")
                        .font(.plusJakarta(size: 14, weight: .semibold))
                        .foregroundStyle(Wq.text)
                    Text("Include guest's spouse or partner")
                        .font(.plusJakarta(size: 12))
                        .foregroundStyle(Wq.muted)
                }
                Spacer()
                Toggle("", isOn: $plusOne)
                    .labelsHidden()
                    .tint(accent.accent)
            }
            .padding(.vertical, 4)

            VStack(alignment: .leading, spacing: 8) {
                FieldLabel(text: "Dietary Preferences / Notes")
                SheetField(value: $notes, placeholder: "Optional notes", minHeight: 42)
            }

            if let error {
                Text(error)
                    .font(.plusJakarta(size: 12))
                    .foregroundStyle(Color(hex: "#F87171"))
            }

            PrimaryCta(
                label: submitting ? "Saving…" : "Add Participant",
                enabled: canSave,
                accent: accent,
                loading: submitting,
                lightLabel: true
            ) {
                Task { await save() }
            }
        }
    }

    private func save() async {
        guard let momentId else { return }
        let trimmed = name.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return }
        submitting = true
        error = nil
        let contactTrim = contact.trimmingCharacters(in: .whitespacesAndNewlines)
        let email = contactTrim.contains("@") ? contactTrim : nil
        let phone = email == nil && !contactTrim.isEmpty ? contactTrim : nil
        do {
            _ = try await APIClient.shared.addGroupParticipant(
                momentId: momentId,
                displayName: trimmed,
                roleCode: "PARTICIPANT",
                email: email,
                phone: phone
            )
            submitting = false
            onSaved()
            onDismiss()
        } catch {
            submitting = false
            self.error = error.localizedDescription
        }
    }
}

// MARK: - 5. VENDOR

struct WeddingVendorBody: View {
    var momentId: String?
    var onDismiss: () -> Void = {}
    var onSaved: () -> Void = {}

    @State private var name = ""
    @State private var category = ""
    @State private var price = ""
    @State private var phone = ""
    @State private var email = ""
    @State private var status = "Shortlisted"
    @State private var notes = ""
    @State private var submitting = false
    @State private var error: String? = nil

    var accent: SheetAccent = softPinkAccent
    private var live: Bool { momentId != nil }

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            SheetHeader(icon: "briefcase.fill", title: "Add Vendor", subtitle: "Keep track of wedding service providers", accent: accent)

            VStack(alignment: .leading, spacing: 8) {
                FieldLabel(text: "Vendor Name")
                SheetField(value: $name, placeholder: "Vendor name", minHeight: 42)
            }

            HStack(spacing: 12) {
                VStack(alignment: .leading, spacing: 8) {
                    FieldLabel(text: "Category")
                    SheetField(
                        value: $category,
                        placeholder: "Category",
                        minHeight: 42,
                        trailing: {
                            AnyView(
                                Image(systemName: "chevron.down")
                                    .font(.system(size: 10))
                                    .foregroundStyle(Wq.muted)
                            )
                        }
                    )
                }
                VStack(alignment: .leading, spacing: 8) {
                    FieldLabel(text: "Quoted Price")
                    SheetField(
                        value: $price,
                        placeholder: "0.00",
                        minHeight: 42,
                        leading: {
                            AnyView(
                                Text("₹")
                                    .font(.plusJakarta(size: 16, weight: .bold))
                                    .foregroundStyle(accent.accent)
                            )
                        }
                    )
                }
            }

            HStack(spacing: 12) {
                VStack(alignment: .leading, spacing: 8) {
                    FieldLabel(text: "Contact Number")
                    SheetField(value: $phone, placeholder: "Phone", minHeight: 42, keyboardType: .phonePad)
                }
                VStack(alignment: .leading, spacing: 8) {
                    FieldLabel(text: "Email")
                    SheetField(value: $email, placeholder: "Email", minHeight: 42, keyboardType: .emailAddress)
                }
            }

            VStack(alignment: .leading, spacing: 8) {
                FieldLabel(text: "Status")
                ChipRow(options: ["Shortlisted", "Confirmed", "Rejected"], selected: $status, accent: accent)
            }

            VStack(alignment: .leading, spacing: 8) {
                FieldLabel(text: "Notes")
                SheetField(value: $notes, placeholder: "Notes", singleLine: false, minHeight: 60)
            }

            if let error {
                Text(error)
                    .font(.plusJakarta(size: 12))
                    .foregroundStyle(Color(hex: "#F87171"))
            }

            PrimaryCta(
                label: "Add Vendor",
                enabled: live && !name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty && !submitting,
                accent: accent,
                lightLabel: true
            ) {
                guard let momentId else { return }
                Task {
                    submitting = true
                    error = nil
                    do {
                        let trimmed: (String) -> String? = { $0.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? nil : $0.trimmingCharacters(in: .whitespacesAndNewlines) }
                        try await APIClient.shared.createGroupVendor(
                            momentId: momentId,
                            vendorName: name.trimmingCharacters(in: .whitespacesAndNewlines),
                            vendorType: trimmed(category),
                            phone: trimmed(phone),
                            email: trimmed(email),
                            notes: trimmed(notes),
                            quotedPrice: trimmed(price),
                            statusLabel: status
                        )
                        submitting = false
                        onSaved()
                        onDismiss()
                    } catch {
                        submitting = false
                        self.error = error.localizedDescription
                    }
                }
            }
        }
    }
}

// MARK: - 6. PLANNING

struct WeddingPlanningBody: View {
    var momentId: String?
    var onDismiss: () -> Void
    var onSaved: () -> Void

    @State private var title = ""
    @State private var date = ""
    @State private var time = ""
    @State private var location = ""
    @State private var participants: [APIClient.GroupParticipantPayload] = []
    @State private var selected: Set<String> = []
    @State private var priority = "Medium"
    @State private var loading = false
    @State private var submitting = false
    @State private var error: String? = nil

    var accent: SheetAccent = purpleAccent
    private var live: Bool { momentId != nil }

    private var people: [(id: String, name: String)] {
        participants.map { (id: $0.participantId, name: $0.displayName ?? String($0.participantId.prefix(8))) }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            SheetHeader(icon: "calendar", title: "Add Planning Item", accent: accent)

            VStack(alignment: .leading, spacing: 8) {
                FieldLabel(text: "Plan Title")
                SheetField(value: $title, placeholder: "Plan title")
            }

            HStack(spacing: 14) {
                VStack(alignment: .leading, spacing: 8) {
                    FieldLabel(text: "Date")
                    WeddingDatePickField(value: $date, placeholder: "Date")
                }
                VStack(alignment: .leading, spacing: 8) {
                    FieldLabel(text: "Time")
                    WeddingTimePickField(value: $time, placeholder: "Time")
                }
            }

            VStack(alignment: .leading, spacing: 8) {
                FieldLabel(text: "Location")
                SheetField(value: $location, placeholder: "Location")
            }

            VStack(alignment: .leading, spacing: 8) {
                FieldLabel(text: "Assign To")
                if loading {
                    ProgressView().tint(accent.accent)
                } else if people.isEmpty {
                    Text("No participants yet")
                        .font(.plusJakarta(size: 12))
                        .foregroundStyle(Wq.muted)
                } else {
                    AvatarPick(people: people, selected: $selected, accent: accent) { id in
                        if selected.contains(id) {
                            selected.remove(id)
                        } else {
                            selected.insert(id)
                        }
                    }
                }
            }

            VStack(alignment: .leading, spacing: 8) {
                FieldLabel(text: "Priority")
                Segmented(options: ["Low", "Medium", "High"], selected: $priority, accent: accent)
            }

            if let error {
                Text(error)
                    .font(.plusJakarta(size: 12))
                    .foregroundStyle(Color(hex: "#F87171"))
            }

            PrimaryCta(
                label: "Add Planning Item",
                enabled: live && !title.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty,
                accent: accent,
                loading: submitting,
                footer: "Everyone will be notified"
            ) {
                submit()
            }
        }
        .onAppear {
            if let momentId {
                Task { await loadParticipants(momentId) }
            }
        }
    }

    private func loadParticipants(_ momentId: String) async {
        loading = true
        do {
            let list = try await APIClient.shared.listGroupParticipants(momentId: momentId)
            let active = list.filter { $0.status == "ACTIVE" || $0.status == "INVITED" }
            participants = active.isEmpty ? list : active
            selected = []
        } catch {
            self.error = error.localizedDescription
        }
        loading = false
    }

    private func submit() {
        guard let momentId else { return }
        let trimmed = title.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return }
        Task {
            submitting = true
            error = nil
            do {
                _ = try await APIClient.shared.createPlanningItem(momentId: momentId, title: trimmed)
                submitting = false
                onSaved()
                onDismiss()
            } catch {
                submitting = false
                self.error = error.localizedDescription
            }
        }
    }
}

// MARK: - 7. ATTENDANCE

struct WeddingAttendanceBody: View {
    var momentId: String?
    var onDismiss: () -> Void = {}
    var onSaved: () -> Void = {}

    @State private var filter = "All"
    @State private var search = ""
    @State private var participants: [APIClient.GroupParticipantPayload] = []
    @State private var selectedId: String? = nil
    @State private var statusLabel = "Confirmed"
    @State private var loading = false
    @State private var submitting = false
    @State private var error: String? = nil

    var accent: SheetAccent = softPinkAccent
    private var live: Bool { momentId != nil }

    private var filtered: [APIClient.GroupParticipantPayload] {
        participants.filter {
            let name = $0.displayName ?? $0.participantId
            return search.isEmpty || name.localizedCaseInsensitiveContains(search)
        }
    }

    private func mapStatus(_ label: String) -> String {
        switch label {
        case "Confirmed": return "CONFIRMED"
        case "Pending": return "EXPECTED"
        case "Declined": return "ABSENT"
        default: return "EXPECTED"
        }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            SheetHeader(icon: "checkmark.square.fill", title: "Track Attendance", subtitle: "Manage guest RSVPs and arrival status", accent: accent)

            SheetField(
                value: $search,
                placeholder: "Search guests...",
                minHeight: 42,
                leading: {
                    AnyView(
                        Image(systemName: "magnifyingglass")
                            .font(.system(size: 14))
                            .foregroundStyle(Wq.muted)
                    )
                }
            )

            ChipRow(options: ["All", "Confirmed", "Pending", "Declined"], selected: $filter, accent: accent)
                .onChange(of: filter) { _, newValue in
                    if newValue != "All" { statusLabel = newValue }
                }

            if loading {
                ProgressView().tint(accent.accent)
            } else if filtered.isEmpty {
                Text("No guests yet — add participants first.")
                    .font(.plusJakarta(size: 13))
                    .foregroundStyle(Wq.muted)
            } else {
                Text("Select a guest")
                    .font(.plusJakarta(size: 12))
                    .foregroundStyle(Wq.muted)
                ForEach(filtered.prefix(12), id: \.participantId) { p in
                    let label = p.displayName ?? String(p.participantId.prefix(8))
                    Button {
                        selectedId = p.participantId
                    } label: {
                        Text(label)
                            .font(.plusJakarta(size: 14, weight: selectedId == p.participantId ? .bold : .regular))
                            .foregroundStyle(selectedId == p.participantId ? accent.accent : Wq.text)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .padding(.vertical, 8)
                    }
                    .buttonStyle(.plain)
                }
            }

            VStack(alignment: .leading, spacing: 8) {
                FieldLabel(text: "Attendance status")
                ChipRow(options: ["Confirmed", "Pending", "Declined"], selected: $statusLabel, accent: accent)
            }

            if let error {
                Text(error)
                    .font(.plusJakarta(size: 12))
                    .foregroundStyle(Color(hex: "#F87171"))
            }

            PrimaryCta(
                label: "Update Attendance",
                enabled: live && selectedId != nil && !submitting,
                accent: accent,
                lightLabel: true
            ) {
                guard let momentId, let selectedId else { return }
                Task {
                    submitting = true
                    error = nil
                    do {
                        try await APIClient.shared.recordAttendance(
                            momentId: momentId,
                            participantId: selectedId,
                            attendanceStatus: mapStatus(statusLabel)
                        )
                        submitting = false
                        onSaved()
                        onDismiss()
                    } catch {
                        submitting = false
                        self.error = error.localizedDescription
                    }
                }
            }
        }
        .task(id: momentId) {
            guard let momentId else { return }
            loading = true
            defer { loading = false }
            do {
                participants = try await APIClient.shared.listGroupParticipants(momentId: momentId)
            } catch {
                self.error = error.localizedDescription
            }
        }
    }
}

// MARK: - 8. POLL

struct WeddingPollBody: View {
    var momentId: String?
    var onDismiss: () -> Void
    var onSaved: () -> Void

    @State private var question = ""
    @State private var optA = ""
    @State private var optB = ""
    @State private var optC = ""
    @State private var pollType = "Single choice"
    @State private var endDate = ""
    @State private var submitting = false
    @State private var error: String? = nil

    var accent: SheetAccent = purpleAccent
    private var live: Bool { momentId != nil }

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            SheetHeader(icon: "chart.bar.fill", title: "Create Poll", subtitle: "Decide together with the wedding party", accent: accent)

            VStack(alignment: .leading, spacing: 8) {
                FieldLabel(text: "Question")
                SheetField(value: $question, placeholder: "Ask a question")
            }

            VStack(alignment: .leading, spacing: 8) {
                FieldLabel(text: "Options")
                SheetField(value: $optA, placeholder: "Option A")
                SheetField(value: $optB, placeholder: "Option B")
                SheetField(value: $optC, placeholder: "Option C (optional)")
            }

            VStack(alignment: .leading, spacing: 8) {
                FieldLabel(text: "Poll type")
                Segmented(options: ["Single choice", "Multi choice"], selected: $pollType, accent: accent)
            }

            VStack(alignment: .leading, spacing: 8) {
                FieldLabel(text: "End date")
                WeddingDatePickField(value: $endDate, placeholder: "Optional")
            }

            if let error {
                Text(error)
                    .font(.plusJakarta(size: 12))
                    .foregroundStyle(Color(hex: "#F87171"))
            }

            PrimaryCta(
                label: "Create Poll",
                enabled: live && isValid,
                accent: accent,
                loading: submitting,
                footer: "Everyone will be notified"
            ) {
                submit()
            }
        }
    }

    private var isValid: Bool {
        !question.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
            && !optA.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
            && !optB.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    }

    private func submit() {
        guard let momentId else { return }
        let q = question.trimmingCharacters(in: .whitespacesAndNewlines)
        let options = [optA, optB, optC]
            .map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }
            .filter { !$0.isEmpty }
        guard options.count >= 2 else { return }
        Task {
            submitting = true
            error = nil
            do {
                _ = try await APIClient.shared.createPoll(momentId: momentId, question: q, options: options)
                submitting = false
                onSaved()
                onDismiss()
            } catch {
                submitting = false
                self.error = error.localizedDescription
            }
        }
    }
}

// MARK: - 9. MEMORY

struct WeddingMemoryBody: View {
    var momentId: String?
    var onDismiss: () -> Void
    var onSaved: () -> Void

    @State private var type = "Photo"
    @State private var title = ""
    @State private var caption = ""
    @State private var tags = ""
    @State private var mood = "Joyful"
    @State private var submitting = false
    @State private var error: String? = nil
    @State private var showSourcePicker = false
    @State private var showCamera = false
    @State private var showLibrary = false
    @State private var pickedItem: PhotosPickerItem? = nil
    @State private var selectedImage: UIImage? = nil

    var accent: SheetAccent = purpleAccent
    private var live: Bool { momentId != nil }

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            SheetHeader(icon: "camera.fill", title: "Capture Memory", subtitle: "Save a moment for the wedding story", accent: accent)

            VStack(alignment: .leading, spacing: 8) {
                FieldLabel(text: "Type")
                Segmented(options: ["Photo", "Milestone", "Lesson", "Reflection"], selected: $type, accent: accent)
            }

            Button {
                showSourcePicker = true
            } label: {
                ZStack {
                    RoundedRectangle(cornerRadius: 14)
                        .fill(Wq.field)
                        .frame(height: 120)
                        .overlay(RoundedRectangle(cornerRadius: 14).stroke(Wq.border))
                    if let selectedImage {
                        Image(uiImage: selectedImage)
                            .resizable()
                            .scaledToFill()
                            .frame(maxWidth: .infinity)
                            .frame(height: 120)
                            .clipShape(RoundedRectangle(cornerRadius: 14))
                    } else {
                        Text("ï¼‹  Upload photo")
                            .font(.plusJakarta(size: 14))
                            .foregroundStyle(Wq.muted)
                    }
                }
            }
            .buttonStyle(.plain)

            if selectedImage != nil {
                Text("Photo attached Â· tap to change")
                    .font(.plusJakarta(size: 11))
                    .foregroundStyle(Wq.handle)
            }

            VStack(alignment: .leading, spacing: 8) {
                FieldLabel(text: "Title")
                SheetField(value: $title, placeholder: "Memory title")
            }

            VStack(alignment: .leading, spacing: 8) {
                FieldLabel(text: "Caption")
                SheetField(value: $caption, placeholder: "What made this special?", singleLine: false, minHeight: 72)
            }

            VStack(alignment: .leading, spacing: 8) {
                FieldLabel(text: "Tags")
                SheetField(value: $tags, placeholder: "Optional tags")
            }

            VStack(alignment: .leading, spacing: 8) {
                FieldLabel(text: "Mood")
                ChipRow(options: ["Joyful", "Emotional", "Fun", "Calm"], selected: $mood, accent: accent)
            }

            if let error {
                Text(error)
                    .font(.plusJakarta(size: 12))
                    .foregroundStyle(Color(hex: "#F87171"))
            }

            PrimaryCta(
                label: "Capture Memory",
                enabled: live && !title.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty,
                accent: accent,
                loading: submitting,
                footer: "Everyone will be notified"
            ) {
                submit()
            }
        }
        .confirmationDialog("Add photo", isPresented: $showSourcePicker, titleVisibility: .visible) {
            Button("Camera") { showCamera = true }
            Button("Photo Library") { showLibrary = true }
            Button("Cancel", role: .cancel) {}
        }
        .photosPicker(isPresented: $showLibrary, selection: $pickedItem, matching: .images)
        .onChange(of: pickedItem) { _, item in
            guard let item else { return }
            Task {
                do {
                    if let data = try await item.loadTransferable(type: WeddingPickedImageData.self) {
                        if let image = UIImage(data: data.data) {
                            selectedImage = image
                            error = nil
                        } else {
                            error = "Could not open that photo"
                        }
                    } else {
                        error = "Could not open that photo"
                    }
                } catch {
                    self.error = error.localizedDescription
                }
            }
        }
        .fullScreenCover(isPresented: $showCamera) {
            WeddingCameraPicker(image: $selectedImage, onCancel: { showCamera = false })
                .ignoresSafeArea()
        }
    }

    private func submit() {
        guard let momentId else { return }
        let trimmed = title.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return }
        Task {
            submitting = true
            error = nil
            do {
                _ = try await APIClient.shared.createGroupMemory(momentId: momentId, title: trimmed)
                submitting = false
                onSaved()
                onDismiss()
            } catch {
                submitting = false
                self.error = error.localizedDescription
            }
        }
    }
}

private struct WeddingPickedImageData: Transferable {
    let data: Data

    static var transferRepresentation: some TransferRepresentation {
        DataRepresentation(importedContentType: .image) { data in
            WeddingPickedImageData(data: data)
        }
    }
}

private struct WeddingCameraPicker: UIViewControllerRepresentable {
    @Binding var image: UIImage?
    var onCancel: () -> Void

    func makeUIViewController(context: Context) -> UIImagePickerController {
        let picker = UIImagePickerController()
        picker.sourceType = UIImagePickerController.isSourceTypeAvailable(.camera) ? .camera : .photoLibrary
        picker.delegate = context.coordinator
        picker.allowsEditing = false
        return picker
    }

    func updateUIViewController(_ uiViewController: UIImagePickerController, context: Context) {}

    func makeCoordinator() -> Coordinator { Coordinator(self) }

    final class Coordinator: NSObject, UINavigationControllerDelegate, UIImagePickerControllerDelegate {
        let parent: WeddingCameraPicker
        init(_ parent: WeddingCameraPicker) { self.parent = parent }

        func imagePickerControllerDidCancel(_ picker: UIImagePickerController) {
            parent.onCancel()
        }

        func imagePickerController(
            _ picker: UIImagePickerController,
            didFinishPickingMediaWithInfo info: [UIImagePickerController.InfoKey: Any]
        ) {
            parent.image = info[.originalImage] as? UIImage
            parent.onCancel()
        }
    }
}

// MARK: - 10. UPDATE

struct WeddingUpdateBody: View {
    var momentId: String?
    var onDismiss: () -> Void
    var onSaved: () -> Void

    @State private var update = ""
    @State private var audience = "Everyone"
    @State private var urgent = false
    @State private var notify = true
    @State private var submitting = false
    @State private var error: String? = nil

    var accent: SheetAccent = purpleAccent
    private var live: Bool { momentId != nil }

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            SheetHeader(icon: "megaphone.fill", title: "Post Update", subtitle: "Share a status with the wedding party", accent: accent)

            VStack(alignment: .leading, spacing: 8) {
                FieldLabel(text: "Update")
                SheetField(value: $update, placeholder: "Share an update…", singleLine: false, minHeight: 88)
            }

            VStack(alignment: .leading, spacing: 8) {
                FieldLabel(text: "Audience")
                ChipRow(options: ["Everyone", "Organizers", "Close family"], selected: $audience, accent: accent)
            }

            Button {
                urgent.toggle()
            } label: {
                HStack {
                    Text("Mark as urgent")
                        .font(.plusJakarta(size: 13))
                        .foregroundStyle(Wq.text)
                    Spacer()
                    Text(urgent ? "ON" : "OFF")
                        .font(.plusJakarta(size: 12, weight: .bold))
                        .foregroundStyle(urgent ? accent.accent : Wq.muted)
                }
                .padding(14)
                .background(Wq.field)
                .clipShape(RoundedRectangle(cornerRadius: 12))
            }
            .buttonStyle(.plain)

            Button {
                notify.toggle()
            } label: {
                HStack {
                    Text("Notify members")
                        .font(.plusJakarta(size: 13))
                        .foregroundStyle(Wq.text)
                    Spacer()
                    Text(notify ? "ON" : "OFF")
                        .font(.plusJakarta(size: 12, weight: .bold))
                        .foregroundStyle(notify ? accent.accent : Wq.muted)
                }
                .padding(14)
                .background(Wq.field)
                .clipShape(RoundedRectangle(cornerRadius: 12))
            }
            .buttonStyle(.plain)

            if let error {
                Text(error)
                    .font(.plusJakarta(size: 12))
                    .foregroundStyle(Color(hex: "#F87171"))
            }

            PrimaryCta(
                label: "Post Update",
                enabled: live && !update.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty,
                accent: accent,
                loading: submitting,
                footer: "Everyone will be notified"
            ) {
                submit()
            }
        }
    }

    private func submit() {
        guard let momentId else { return }
        let trimmed = update.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return }
        Task {
            submitting = true
            error = nil
            do {
                _ = try await APIClient.shared.postGroupUpdate(momentId: momentId, message: trimmed)
                submitting = false
                onSaved()
                onDismiss()
            } catch {
                submitting = false
                self.error = error.localizedDescription
            }
        }
    }
}

// MARK: - 11. SETTLE

struct WeddingSettleBody: View {
    var momentId: String? = nil
    var onDismiss: () -> Void = {}
    var onSaved: () -> Void = {}

    @State private var presentSettlement = false

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            SheetHeader(icon: "scalemass.fill", title: "Settle Up", subtitle: "Record settlements between wedding party")
            Text("Ledger settlement records payments against open balances. Payment rails are not processed.")
                .font(.plusJakarta(size: 13))
                .foregroundStyle(Wq.muted)
            PrimaryCta(label: "Settle Up", enabled: momentId != nil) {
                presentSettlement = true
            }
        }
        .sheet(isPresented: $presentSettlement) {
            if let momentId {
                GroupSettlementSheet(
                    momentId: momentId,
                    momentTypeCode: "WEDDING",
                    isPresented: $presentSettlement,
                    onSaved: {
                        presentSettlement = false
                        onSaved()
                        onDismiss()
                    }
                )
            }
        }
    }
}
