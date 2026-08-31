import SwiftUI

struct SetupChipOption: Identifiable, Hashable {
    let value: String
    let label: String
    var emoji: String? = nil

    var id: String { value }
}

struct SetupChipGrid: View {
    let options: [SetupChipOption]
    let selected: Set<String>
    let onToggle: (String) -> Void
    var columns: Int = 2
    var selectedColor: Color = SetupTokens.chipSelected
    var unselectedColor: Color = SetupTokens.chipUnselected

    var body: some View {
        VStack(spacing: 8) {
            ForEach(Array(options.chunked(into: columns).enumerated()), id: \.offset) { _, row in
                HStack(spacing: 8) {
                    ForEach(row) { option in
                        SetupChipCell(
                            option: option,
                            isSelected: selected.contains(option.value),
                            onToggle: onToggle,
                            selectedColor: selectedColor,
                            unselectedColor: unselectedColor
                        )
                    }
                    if row.count < columns {
                        ForEach(0..<(columns - row.count), id: \.self) { _ in
                            Color.clear.frame(maxWidth: .infinity)
                        }
                    }
                }
            }
        }
    }
}

private struct SetupChipCell: View {
    let option: SetupChipOption
    let isSelected: Bool
    let onToggle: (String) -> Void
    let selectedColor: Color
    let unselectedColor: Color

    var body: some View {
        Button {
            onToggle(option.value)
        } label: {
            HStack(spacing: 8) {
                if let emoji = option.emoji {
                    Text(emoji).font(.system(size: 18))
                }
                Text(option.label)
                    .font(.system(size: 13, weight: isSelected ? .semibold : .regular))
                    .foregroundStyle(SetupTokens.textPrimary)
                    .multilineTextAlignment(.leading)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 14)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(isSelected ? selectedColor : unselectedColor, in: RoundedRectangle(cornerRadius: 12))
            .overlay(
                RoundedRectangle(cornerRadius: 12)
                    .stroke(isSelected ? selectedColor : SetupTokens.borderSubtle, lineWidth: 1)
            )
        }
        .buttonStyle(.plain)
        .accessibilityLabel("\(option.label), \(isSelected ? "selected" : "not selected")")
    }
}

struct SetupChipRow: View {
    let options: [SetupChipOption]
    let selected: String?
    let onSelect: (String) -> Void

    var body: some View {
        HStack(spacing: 8) {
            ForEach(options) { option in
                let isSelected = option.value == selected
                Button {
                    onSelect(option.value)
                } label: {
                    Text([option.emoji, option.label].compactMap { $0 }.joined(separator: " "))
                        .font(.system(size: 13, weight: isSelected ? .semibold : .regular))
                        .foregroundStyle(SetupTokens.textPrimary)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 12)
                        .padding(.horizontal, 8)
                        .background(isSelected ? SetupTokens.chipSelected : SetupTokens.chipUnselected, in: RoundedRectangle(cornerRadius: 12))
                        .overlay(
                            RoundedRectangle(cornerRadius: 12)
                                .stroke(isSelected ? SetupTokens.chipSelected : SetupTokens.borderSubtle, lineWidth: 1)
                        )
                }
                .buttonStyle(.plain)
                .accessibilityLabel(option.label)
            }
        }
    }
}

private extension Array {
    func chunked(into size: Int) -> [[Element]] {
        guard size > 0 else { return [self] }
        return stride(from: 0, to: count, by: size).map {
            Array(self[$0..<Swift.min($0 + size, count)])
        }
    }
}
