import SwiftUI

/// Searchable India States & UTs picker for Business Setup GST state.
/// Stores the canonical full name; shows the two-digit GSTIN code on each row.
struct SetupStatePickerSheet: View {
    let theme: BusinessSetupTheme
    let selected: String
    let onPick: (IndiaState) -> Void
    let onCancel: () -> Void

    @State private var query = ""

    private var matches: [IndiaState] { IndiaStateCatalog.search(query) }
    private var chosenCode: String { IndiaStateCatalog.codeOf(selected) }

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("State")
                .font(.system(size: 22, weight: .semibold))
                .foregroundStyle(BusinessTokens.Colors.textPrimary)
            TextField(
                "",
                text: $query,
                prompt: Text("Search a state or code").foregroundStyle(theme.hint.opacity(0.7))
            )
            .font(.system(size: 14))
            .foregroundStyle(BusinessTokens.Colors.textPrimary)
            .autocorrectionDisabled()
            .textInputAutocapitalization(.words)
            .padding(.horizontal, 12)
            .padding(.vertical, 10)
            .background(theme.inset)
            .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 10, style: .continuous)
                    .stroke(theme.accent.opacity(0.2), lineWidth: 1)
            )
            if matches.isEmpty {
                Text("No state matches \"\(query)\".")
                    .font(.system(size: 13))
                    .foregroundStyle(theme.hint)
            }
            ScrollView {
                LazyVStack(alignment: .leading, spacing: 8) {
                    ForEach(grouped(matches), id: \.title) { group in
                        if let title = group.title {
                            Text(title)
                                .font(.system(size: 11, weight: .semibold))
                                .foregroundStyle(theme.hint)
                                .padding(.top, 6)
                        }
                        ForEach(group.items) { state in
                            stateRow(state)
                        }
                    }
                }
            }
            Button("Cancel", action: onCancel)
                .font(.system(size: 14, weight: .semibold))
                .foregroundStyle(theme.hint)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 12)
        }
        .padding(18)
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
        .background(theme.canvas.ignoresSafeArea())
    }

    private func stateRow(_ state: IndiaState) -> some View {
        let on = !chosenCode.isEmpty && state.code == chosenCode
        return Button {
            onPick(state)
        } label: {
            HStack(spacing: 8) {
                Text(state.name)
                    .font(.system(size: 14))
                    .foregroundStyle(BusinessTokens.Colors.textPrimary)
                    .lineLimit(1)
                Spacer(minLength: 4)
                Text(state.code)
                    .font(.system(size: 12))
                    .foregroundStyle(theme.hint)
                if on {
                    Text("●")
                        .font(.system(size: 10))
                        .foregroundStyle(theme.accent)
                }
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 12)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(on ? theme.accent.opacity(0.14) : theme.inset)
            .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 14, style: .continuous)
                    .stroke(on ? theme.accent : theme.accent.opacity(0.2), lineWidth: 1)
            )
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }

    private func grouped(_ rows: [IndiaState]) -> [(title: String?, items: [IndiaState])] {
        var groups: [(title: String?, items: [IndiaState])] = []
        let states = rows.filter { !$0.isUnionTerritory }
        let uts = rows.filter(\.isUnionTerritory)
        if !states.isEmpty { groups.append((title: "States", items: states)) }
        if !uts.isEmpty { groups.append((title: "Union territories", items: uts)) }
        return groups
    }
}
