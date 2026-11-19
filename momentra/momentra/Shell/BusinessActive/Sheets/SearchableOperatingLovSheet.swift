import SwiftUI

struct OperatingLovOption: Identifiable, Equatable {
    let id: String
    let name: String
  /// Picker kind (e.g. `PRODUCT`, `BUYER`), not necessarily the master row kind.
    let kind: String
    var subtitle: String?
    var unit: String?
}

func tradingLovOptions(
    kind: String,
    masters: [OperatingMasterPayload],
    local: [OperatingLovOption] = []
) -> [OperatingLovOption] {
    let pickerKind = kind.uppercased()
    let fromMasters = masters
        .filter { OperatingLovKindMatch.masterMatches(pickerKind: pickerKind, masterKind: $0.kind) }
        .map { master -> OperatingLovOption in
            let unitLabel: String? = {
                guard pickerKind == "PRODUCT" || pickerKind == "UOM" else { return master.unit }
                let raw = (master.unit ?? (pickerKind == "UOM" ? master.name : nil))?
                    .trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
                return raw.isEmpty ? nil : raw
            }()
            let subtitle: String? = {
                guard pickerKind == "PRODUCT" else { return nil }
                let codePart = master.code.flatMap { raw -> String? in
                    let trimmed = raw.trimmingCharacters(in: .whitespacesAndNewlines)
                    return trimmed.isEmpty ? nil : trimmed
                }
                let parts = [
                    unitLabel.map { "Base unit: \($0)" },
                    codePart,
                ].compactMap { $0 }
                return parts.isEmpty ? nil : parts.joined(separator: " · ")
            }()
            return OperatingLovOption(
                id: master.id,
                name: master.name,
                kind: pickerKind,
                subtitle: subtitle,
                unit: unitLabel
            )
        }
    let seeded = fromMasters + local.filter { $0.kind.uppercased() == pickerKind }
  var seen = Set<String>()
    return seeded.filter { row in
        let key = row.name.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        guard !key.isEmpty, !seen.contains(key) else { return false }
        seen.insert(key)
        return true
    }
}

func filterOperatingLovOptions(
    query: String,
    kind: String,
    options: [OperatingLovOption]
) -> [OperatingLovOption] {
    let pickerKind = kind.uppercased()
    let scoped = options.filter { $0.kind.uppercased() == pickerKind }
    let q = query.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
    guard !q.isEmpty else { return scoped }
    return scoped.filter {
        $0.name.lowercased().contains(q) || ($0.subtitle?.lowercased().contains(q) ?? false)
    }
}

func operatingLovTitle(for kind: String) -> String {
    switch kind.uppercased() {
    case "PRODUCT": return "Product"
    case "BUYER", "CUSTOMER": return "Customer"
    case "SUPPLIER": return "Supplier"
    case "CHANNEL": return "Channel"
    case "UOM": return "UOM"
    default:
        return kind.prefix(1).uppercased() + kind.dropFirst().lowercased()
    }
}

struct SearchableOperatingLovSheet: View {
    let theme: OperatingTheme
    let kind: String
    let title: String
    let options: [OperatingLovOption]
    let canAdd: Bool
    let onSelect: (OperatingLovOption) -> Void
  /// Name only for party rows; product add also supplies optional base unit via the second parameter when non-nil.
    let onAdd: ((String, String?) -> Void)?
    let onDismiss: () -> Void

    @State private var query = ""
    @State private var adding = false
    @State private var newName = ""
    @State private var newUnit = ""

    private var filtered: [OperatingLovOption] {
        filterOperatingLovOptions(query: query, kind: kind, options: options)
    }

    private var exactMatch: OperatingLovOption? {
        let q = query.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !q.isEmpty else { return nil }
        return filtered.first { $0.name.caseInsensitiveCompare(q) == .orderedSame }
    }

    private var productPicker: Bool { kind.uppercased() == "PRODUCT" }

    var body: some View {
        ZStack {
            Color.black.opacity(0.55).ignoresSafeArea()
                .onTapGesture(perform: onDismiss)
            VStack(alignment: .leading, spacing: 12) {
                HStack {
                    Text(title)
                        .font(.plusJakarta(size: 18, weight: .bold))
                        .foregroundStyle(theme.ink)
                    Spacer()
                    Button(action: onDismiss) {
                        Text("×")
                            .font(.plusJakarta(size: 16, weight: .bold))
                            .foregroundStyle(theme.muted)
                            .frame(width: 28, height: 28)
                            .background(theme.captureField)
                            .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))
                    }
                    .buttonStyle(.plain)
                }
                TextField(
                    "",
                    text: $query,
                    prompt: Text("Search \(title.lowercased())").foregroundStyle(theme.muted)
                )
                .font(.plusJakarta(size: 14))
                .foregroundStyle(theme.captureValue)
                .padding(.horizontal, 14)
                .padding(.vertical, 12)
                .background(theme.captureField)
                .overlay(
                    RoundedRectangle(cornerRadius: 14, style: .continuous)
                        .stroke(Color.white.opacity(0.08), lineWidth: 1)
                )
                .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))

                if filtered.isEmpty {
                    Text("No matching \(title.lowercased())")
                        .font(.plusJakarta(size: 13))
                        .foregroundStyle(theme.muted)
                    if canAdd {
                        addButton
                    } else {
                        Text("Ask an admin to add this.")
                            .font(.plusJakarta(size: 12))
                            .foregroundStyle(theme.muted)
                    }
                } else {
                    ScrollView {
                        LazyVStack(spacing: 8) {
                            ForEach(filtered) { option in
                                Button {
                                    onSelect(option)
                                    onDismiss()
                                } label: {
                                    VStack(alignment: .leading, spacing: 4) {
                                        Text(option.name)
                                            .font(.plusJakarta(size: 15, weight: .semibold))
                                            .foregroundStyle(theme.ink)
                                        if let subtitle = option.subtitle, !subtitle.isEmpty {
                                            Text(subtitle)
                                                .font(.plusJakarta(size: 12))
                                                .foregroundStyle(theme.muted)
                                        }
                                    }
                                    .frame(maxWidth: .infinity, alignment: .leading)
                                    .padding(12)
                                    .background(theme.captureField)
                                    .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                                }
                                .buttonStyle(.plain)
                            }
                        }
                    }
                    .frame(maxHeight: 420)
                }

                if canAdd, !adding, !filtered.isEmpty {
                    addButton
                }

                if adding {
                    addForm
                }
            }
            .padding(16)
            .frame(maxWidth: 480)
            .background(theme.isOpsQaCapture ? theme.captureSheet : theme.canvas)
            .overlay(
                RoundedRectangle(cornerRadius: 20, style: .continuous)
                    .stroke(theme.border, lineWidth: 1)
            )
            .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
            .shadow(color: theme.accent.opacity(0.35), radius: 24, y: 8)
            .padding(.horizontal, 20)
        }
    }

    private var addButton: some View {
        Button {
            newName = query.trimmingCharacters(in: .whitespacesAndNewlines)
            adding = true
        } label: {
            Text("+ Add \(title)")
                .font(.plusJakarta(size: 13, weight: .semibold))
                .foregroundStyle(theme.accent)
        }
        .buttonStyle(.plain)
    }

    private var addForm: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Add \(title)")
                .font(.plusJakarta(size: 14, weight: .semibold))
                .foregroundStyle(theme.ink)
            TextField("Name", text: $newName)
                .font(.plusJakarta(size: 14))
                .foregroundStyle(theme.captureValue)
                .padding(12)
                .background(theme.captureField)
                .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
            if productPicker {
                TextField("Base UOM (required)", text: $newUnit)
                    .font(.plusJakarta(size: 14))
                    .foregroundStyle(theme.captureValue)
                    .padding(12)
                    .background(theme.captureField)
                    .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
            }
            if let match = exactMatch {
                Text("Matching existing: \(match.name). Choose existing or confirm a distinct name.")
                    .font(.plusJakarta(size: 11))
                    .foregroundStyle(theme.warning)
                Button("Use existing") {
                    onSelect(match)
                    onDismiss()
                }
                .font(.plusJakarta(size: 13, weight: .semibold))
                .foregroundStyle(theme.accent)
            }
            HStack {
                Button("Cancel") { adding = false }
                    .font(.plusJakarta(size: 13))
                    .foregroundStyle(theme.muted)
                Spacer()
                Button("Save", action: saveAdd)
                    .font(.plusJakarta(size: 13, weight: .semibold))
                    .foregroundStyle(theme.accent)
            }
        }
        .padding(12)
        .background(theme.captureField)
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
    }

    private func saveAdd() {
        let name = newName.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !name.isEmpty else { return }
        let unit = newUnit.trimmingCharacters(in: .whitespacesAndNewlines)
        if productPicker, unit.isEmpty { return }
        if let match = exactMatch {
            onSelect(match)
            onDismiss()
            return
        }
        let unitArg = unit.isEmpty ? nil : unit
        onAdd?(name, unitArg)
        adding = false
        onDismiss()
    }
}