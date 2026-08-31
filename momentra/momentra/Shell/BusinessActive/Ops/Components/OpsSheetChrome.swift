import SwiftUI

enum OpsSheetTokens {
    static let sheetBg = Color(hex: "#161B26")
    static let handle = Color(hex: "#625E70")
    static let field = Color(hex: "#252230")
    static let border = Color(hex: "#322E40")
    static let text = Color.white
    static let muted = Color(hex: "#9E9AA8")
    static let footerHint = Color(hex: "#625E70")
    static let accent = Color(hex: "#818CF8")
    static let accentEnd = Color(hex: "#4F46E5")
    static let soft = Color(hex: "#818CF8").opacity(0.13)
    static let ctaText = Color(hex: "#14121B")
    static let error = Color(hex: "#F87171")
    static let closeBg = Color(hex: "#252230")
}

enum OpsAmountFormat {
    static func display(from raw: String) -> String {
        let cleaned = raw.filter { $0.isNumber || $0 == "." }
        guard !cleaned.isEmpty else { return "" }
        let parts = cleaned.split(separator: ".", maxSplits: 1, omittingEmptySubsequences: false)
        let intPart = String(parts.first ?? "0")
        let formatter = NumberFormatter()
        formatter.numberStyle = .decimal
        formatter.groupingSeparator = ","
        formatter.maximumFractionDigits = 0
        let formatted = formatter.string(from: NSNumber(value: Int64(intPart) ?? 0)) ?? intPart
        if parts.count > 1 {
            return "\(formatted).\(String(parts[1].prefix(2)))"
        }
        return formatted
    }

    static func strip(_ display: String) -> String {
        display.filter { $0.isNumber || $0 == "." }
    }
}

struct OpsSheetHeader: View {
    let emoji: String
    let title: String
    let explanation: String
    let onClose: () -> Void

    var body: some View {
        HStack {
            HStack(spacing: 12) {
                Text(emoji)
                    .font(.system(size: 18))
                    .frame(width: 36, height: 36)
                    .background(OpsSheetTokens.soft)
                    .clipShape(RoundedRectangle(cornerRadius: 18))
                VStack(alignment: .leading, spacing: 2) {
                    Text(title)
                        .font(.plusJakarta(size: 20, weight: .heavy))
                        .foregroundStyle(OpsSheetTokens.text)
                    Text(explanation)
                        .font(.plusJakarta(size: 12))
                        .foregroundStyle(OpsSheetTokens.muted)
                }
            }
            Spacer()
            Button(action: onClose) {
                Image(systemName: "xmark")
                    .font(.system(size: 12, weight: .bold))
                    .foregroundStyle(OpsSheetTokens.muted)
                    .frame(width: 32, height: 32)
                    .background(OpsSheetTokens.closeBg)
                    .clipShape(RoundedRectangle(cornerRadius: 16))
            }
            .buttonStyle(.plain)
        }
        .padding(.horizontal, 4)
    }
}

struct OpsFieldLabel: View {
    let text: String
    var body: some View {
        Text(text.uppercased())
            .font(.plusJakarta(size: 11, weight: .semibold))
            .foregroundStyle(OpsSheetTokens.muted)
    }
}

struct OpsTextField: View {
    @Binding var value: String
    var placeholder: String
    var minHeight: CGFloat = 44
    var singleLine: Bool = true

    var body: some View {
        Group {
            if singleLine {
                TextField(placeholder, text: $value)
                    .font(.plusJakarta(size: 14, weight: .medium))
                    .foregroundStyle(OpsSheetTokens.text)
                    .padding(.horizontal, 16)
                    .frame(height: minHeight)
            } else {
                TextField(placeholder, text: $value, axis: .vertical)
                    .font(.plusJakarta(size: 14, weight: .medium))
                    .foregroundStyle(OpsSheetTokens.text)
                    .padding(16)
                    .frame(minHeight: minHeight, alignment: .topLeading)
            }
        }
        .background(OpsSheetTokens.field)
        .overlay(RoundedRectangle(cornerRadius: 12).stroke(OpsSheetTokens.border))
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }
}

struct OpsAmountField: View {
    @Binding var displayValue: String
    var placeholder: String = "₹ Enter amount"

    var body: some View {
        TextField(placeholder, text: Binding(
            get: { displayValue.isEmpty ? "" : "₹ \(displayValue)" },
            set: { raw in
                let stripped = OpsAmountFormat.strip(raw.replacingOccurrences(of: "₹", with: "").trimmingCharacters(in: .whitespaces))
                displayValue = OpsAmountFormat.display(from: stripped)
            }
        ))
        .keyboardType(.decimalPad)
        .font(.plusJakarta(size: 14, weight: .medium))
        .foregroundStyle(OpsSheetTokens.text)
        .padding(.horizontal, 16)
        .frame(height: 44)
        .background(OpsSheetTokens.field)
        .overlay(RoundedRectangle(cornerRadius: 12).stroke(OpsSheetTokens.border))
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }
}

struct OpsDropdownField: View {
    let value: String
    let options: [String]
    let onSelect: (String) -> Void
    var placeholder: String

    var body: some View {
        Menu {
            ForEach(options, id: \.self) { option in
                Button(option) { onSelect(option) }
            }
        } label: {
            HStack(spacing: 12) {
                Text(value.isEmpty ? placeholder : value)
                    .font(.plusJakarta(size: 14, weight: .medium))
                    .foregroundStyle(value.isEmpty ? OpsSheetTokens.muted.opacity(0.7) : OpsSheetTokens.text)
                    .lineLimit(1)
                Spacer()
                Image(systemName: "chevron.down")
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundStyle(OpsSheetTokens.muted)
            }
            .padding(.horizontal, 16)
            .frame(height: 44)
            .background(OpsSheetTokens.field)
            .overlay(RoundedRectangle(cornerRadius: 12).stroke(OpsSheetTokens.border))
            .clipShape(RoundedRectangle(cornerRadius: 12))
        }
    }
}

struct OpsDateField: View {
    @Binding var isoDate: String
    @State private var showPicker = false
    @State private var draft = Date()

    private var display: String {
        let date = SetupDateTimeUtils.dateFromIso(isoDate)
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "en_US")
        formatter.dateFormat = "MMM d, yyyy"
        let formatted = formatter.string(from: date)
        if Calendar.current.isDateInToday(date) {
            return "Today (\(formatted))"
        }
        return formatted
    }

    var body: some View {
        Button {
            draft = SetupDateTimeUtils.dateFromIso(isoDate)
            showPicker = true
        } label: {
            HStack(spacing: 12) {
                Image(systemName: "calendar")
                    .font(.system(size: 14))
                    .foregroundStyle(OpsSheetTokens.muted)
                Text(display)
                    .font(.plusJakarta(size: 14, weight: .medium))
                    .foregroundStyle(OpsSheetTokens.text)
                Spacer()
            }
            .padding(.horizontal, 16)
            .frame(height: 44)
            .background(OpsSheetTokens.field)
            .overlay(RoundedRectangle(cornerRadius: 12).stroke(OpsSheetTokens.border))
            .clipShape(RoundedRectangle(cornerRadius: 12))
        }
        .buttonStyle(.plain)
        .sheet(isPresented: $showPicker) {
            NavigationStack {
                DatePicker("", selection: $draft, displayedComponents: .date)
                    .datePickerStyle(.graphical)
                    .labelsHidden()
                    .padding()
                    .navigationTitle("Date")
                    .navigationBarTitleDisplayMode(.inline)
                    .toolbar {
                        ToolbarItem(placement: .cancellationAction) {
                            Button("Cancel") { showPicker = false }
                        }
                        ToolbarItem(placement: .confirmationAction) {
                            Button("OK") {
                                isoDate = SetupDateTimeUtils.localDateString(from: draft)
                                showPicker = false
                            }
                        }
                    }
            }
            .presentationDetents([.medium])
        }
    }
}

struct OpsChipRow: View {
    let options: [String]
    @Binding var selected: String

    var body: some View {
        FlowLayout(spacing: 8) {
            ForEach(options, id: \.self) { option in
                let isSelected = option.caseInsensitiveCompare(selected) == .orderedSame
                Button {
                    selected = option
                } label: {
                    Text(option)
                        .font(.plusJakarta(size: 12, weight: isSelected ? .bold : .medium))
                        .foregroundStyle(isSelected ? OpsSheetTokens.accent : OpsSheetTokens.muted)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 6)
                        .background(isSelected ? OpsSheetTokens.soft : OpsSheetTokens.field)
                        .overlay(Capsule().stroke(isSelected ? OpsSheetTokens.accent : OpsSheetTokens.border))
                        .clipShape(Capsule())
                }
                .buttonStyle(.plain)
            }
        }
    }
}

struct OpsPrimaryCta: View {
    let label: String
    let enabled: Bool
    let loading: Bool
    let footerHint: String
    let action: () -> Void

    var body: some View {
        VStack(spacing: 8) {
            Button(action: action) {
                Group {
                    if loading {
                        ProgressView().tint(OpsSheetTokens.ctaText)
                    } else {
                        Text(label)
                            .font(.plusJakarta(size: 16, weight: .bold))
                            .foregroundStyle(OpsSheetTokens.ctaText.opacity(enabled ? 1 : 0.55))
                    }
                }
                .frame(maxWidth: .infinity)
                .padding(.vertical, 14)
                .background(
                    LinearGradient(
                        colors: enabled && !loading
                            ? [OpsSheetTokens.accent, OpsSheetTokens.accentEnd]
                            : [OpsSheetTokens.accent.opacity(0.35), OpsSheetTokens.accentEnd.opacity(0.35)],
                        startPoint: .leading,
                        endPoint: .trailing
                    )
                )
                .clipShape(RoundedRectangle(cornerRadius: 14))
            }
            .buttonStyle(.plain)
            .disabled(!enabled || loading)

            Text(footerHint)
                .font(.plusJakarta(size: 12))
                .foregroundStyle(OpsSheetTokens.footerHint)
        }
    }
}
