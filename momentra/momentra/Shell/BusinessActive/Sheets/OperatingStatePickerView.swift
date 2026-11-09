import SwiftUI

/// The one state picker used by every Business surface.
///
/// The list is the canonical India States & UTs (`IndiaStateCatalog`), bundled into the app so it
/// opens instantly offline. States come first and union territories after, each row carrying its
/// two-digit GST code — the code the backend uses to decide CGST + SGST versus IGST.
///
/// A row writes back the canonical full name, never the code, so existing display code is
/// unchanged and a saved value reads the same as it always did.
struct OperatingStatePickerView: View {
    let theme: OperatingTheme
    let selected: String
    let onPick: (IndiaState) -> Void
    let onCancel: () -> Void

    @State private var query = ""

    private var matches: [IndiaState] { IndiaStateCatalog.search(query) }
    private var chosenCode: String { IndiaStateCatalog.codeOf(selected) }

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("State")
                .font(.plusJakarta(size: 22, weight: .semibold))
                .foregroundStyle(theme.ink)

            TextField(
                "",
                text: $query,
                prompt: Text("Search a state or code").foregroundStyle(theme.muted.opacity(0.7))
            )
            .font(.plusJakarta(size: 14))
            .foregroundStyle(theme.ink)
            .autocorrectionDisabled()
            .textInputAutocapitalization(.words)
            .padding(.horizontal, 12)
            .padding(.vertical, 11)
            .background(theme.inset)
            .clipShape(RoundedRectangle(cornerRadius: 12))
            .overlay(
                RoundedRectangle(cornerRadius: 12)
                    .stroke(theme.border, lineWidth: 1)
            )

            if matches.isEmpty {
                Text("No state matches \"\(query)\".")
                    .font(.plusJakarta(size: 13))
                    .foregroundStyle(theme.muted)
            }

            ScrollView {
                LazyVStack(alignment: .leading, spacing: 8) {
                    let states = matches.filter { !$0.isUnionTerritory }
                    let uts = matches.filter(\.isUnionTerritory)
                    if !states.isEmpty {
                        groupTitle("States")
                        ForEach(states) { stateRow($0) }
                    }
                    if !uts.isEmpty {
                        groupTitle("Union territories")
                        ForEach(uts) { stateRow($0) }
                    }
                }
            }

            Button("Cancel", action: onCancel)
                .font(.plusJakarta(size: 14, weight: .semibold))
                .foregroundStyle(theme.body)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 12)
        }
        .padding(18)
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
        .background(theme.canvas.ignoresSafeArea())
    }

    private func groupTitle(_ text: String) -> some View {
        Text(text)
            .font(.plusJakarta(size: 11, weight: .semibold))
            .foregroundStyle(theme.muted)
            .padding(.top, 6)
    }

    private func stateRow(_ state: IndiaState) -> some View {
        let on = !chosenCode.isEmpty && state.code == chosenCode
        return Button {
            onPick(state)
        } label: {
            HStack(spacing: 8) {
                Text(state.name)
                    .font(.plusJakarta(size: 14))
                    .foregroundStyle(theme.ink)
                    .lineLimit(1)
                Spacer(minLength: 4)
                Text(state.code)
                    .font(.plusJakarta(size: 12))
                    .foregroundStyle(theme.muted)
                if on {
                    Text("●")
                        .font(.plusJakarta(size: 10))
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
                    .stroke(on ? theme.accent : theme.border, lineWidth: 1)
            )
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityIdentifier("state.row.\(state.code)")
    }
}
