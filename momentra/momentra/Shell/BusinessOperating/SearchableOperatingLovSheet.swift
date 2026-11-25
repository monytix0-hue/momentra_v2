import SwiftUI

/// Searchable LOV option for Trading Quick Add (APK `OperatingLovOption` parity).
struct OperatingLovOption: Identifiable, Equatable {
    let id: String
    let name: String
    /// Picker kind (e.g. `PRODUCT`, `BUYER`), not necessarily the master row kind.
    let kind: String
    var subtitle: String?
    var unit: String?
}

enum OperatingLovKindMatch {
    static func masterMatches(pickerKind: String, masterKind: String) -> Bool {
        let picker = pickerKind.uppercased()
        let master = masterKind.uppercased()
        switch picker {
        case "BUYER", "CUSTOMER":
            return ["BUYER", "CUSTOMER", "PARTY", "CONTACT"].contains(master)
        case "SUPPLIER":
            return ["SUPPLIER", "VENDOR", "PARTY", "CONTACT"].contains(master)
        case "PRODUCT":
            return ["PRODUCT", "SKU", "ITEM"].contains(master)
        case "UOM":
            return ["UOM", "UNIT"].contains(master)
        case "CHANNEL":
            return master == "CHANNEL"
        default:
            return picker == master
        }
    }
}

enum OperatingSeedUoms {
    struct Entry: Identifiable, Equatable {
        let code: String
        let label: String
        let category: String
        var id: String { code }
    }

    static let categories = ["Count", "Mass", "Volume", "Length", "Area"]

    static let all: [Entry] = [
        Entry(code: "pc", label: "piece (pc)", category: "Count"),
        Entry(code: "pair", label: "pair", category: "Count"),
        Entry(code: "set", label: "set", category: "Count"),
        Entry(code: "pack", label: "pack", category: "Count"),
        Entry(code: "box", label: "box", category: "Count"),
        Entry(code: "carton", label: "carton", category: "Count"),
        Entry(code: "dozen", label: "dozen (12 pc)", category: "Count"),
        Entry(code: "kg", label: "kg", category: "Mass"),
        Entry(code: "g", label: "g", category: "Mass"),
        Entry(code: "tonne", label: "tonne", category: "Mass"),
        Entry(code: "L", label: "litre (L)", category: "Volume"),
        Entry(code: "mL", label: "mL", category: "Volume"),
        Entry(code: "m", label: "metre (m)", category: "Length"),
        Entry(code: "cm", label: "cm", category: "Length"),
        Entry(code: "foot", label: "foot", category: "Length"),
        Entry(code: "sq ft", label: "sq ft", category: "Area"),
        Entry(code: "sq m", label: "sq m", category: "Area"),
    ]

    static func entries(category: String) -> [Entry] {
        all.filter { $0.category.caseInsensitiveCompare(category) == .orderedSame }
    }

    static func needsPackConversion(_ code: String) -> Bool {
        ["pack", "box", "carton", "dozen"].contains(code.trimmingCharacters(in: .whitespacesAndNewlines).lowercased())
    }
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
    case "OBLIGATION", "LINKED_SOURCE": return "Open record"
    case "SALES_ORDER": return "Sales order"
    case "PURCHASE_ORDER": return "Purchase order"
    default:
        return kind.prefix(1).uppercased() + kind.dropFirst().lowercased()
    }
}

/// Searchable product/party/UOM sheet with category-filtered base units (APK parity).
struct SearchableOperatingLovSheet: View {
    let kind: String
    let title: String
    let options: [OperatingLovOption]
    let canAdd: Bool
    var accent: Color = Color(red: 0.06, green: 0.73, blue: 0.51)
    let onSelect: (OperatingLovOption) -> Void
    /// Name, optional unit, optional conversion-to-base.
    let onAdd: ((String, String?, String?) -> Void)?
    let onDismiss: () -> Void

    @State private var query = ""
    @State private var adding = false
    @State private var newName = ""
    @State private var newUnit = "pc"
    @State private var newConversion = ""
    @State private var unitCategory = "Count"

    private let fieldBg = Color(red: 0.09, green: 0.11, blue: 0.15)
    private let sheetBg = Color(red: 0.06, green: 0.07, blue: 0.12)
    private let muted = Color.white.opacity(0.55)

    private var filtered: [OperatingLovOption] {
        filterOperatingLovOptions(query: query, kind: kind, options: options)
    }

    private var exactMatch: OperatingLovOption? {
        let q = query.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !q.isEmpty else { return nil }
        return filtered.first { $0.name.caseInsensitiveCompare(q) == .orderedSame }
    }

    private var productPicker: Bool { kind.uppercased() == "PRODUCT" }
    private var uomPicker: Bool { kind.uppercased() == "UOM" }
    private var needsUnit: Bool { productPicker || uomPicker }

    private var categoryUnits: [OperatingSeedUoms.Entry] {
        OperatingSeedUoms.entries(category: unitCategory)
    }

    private var showPackConversion: Bool {
        productPicker && OperatingSeedUoms.needsPackConversion(newUnit)
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 12) {
                    TextField(
                        "",
                        text: $query,
                        prompt: Text("Search \(title.lowercased())").foregroundStyle(muted)
                    )
                    .textFieldStyle(.plain)
                    .foregroundStyle(.white)
                    .padding(.horizontal, 14)
                    .padding(.vertical, 12)
                    .background(fieldBg)
                    .clipShape(RoundedRectangle(cornerRadius: 14))

                    if filtered.isEmpty {
                        Text("No matching \(title.lowercased())")
                            .font(.system(size: 13))
                            .foregroundStyle(muted)
                        if canAdd {
                            addButton
                        } else {
                            Text("Ask an admin to add this.")
                                .font(.system(size: 12))
                                .foregroundStyle(muted)
                        }
                    } else {
                        LazyVStack(spacing: 8) {
                            ForEach(filtered) { option in
                                Button {
                                    onSelect(option)
                                    onDismiss()
                                } label: {
                                    VStack(alignment: .leading, spacing: 4) {
                                        Text(option.name)
                                            .font(.system(size: 15, weight: .semibold))
                                            .foregroundStyle(.white)
                                        if let subtitle = option.subtitle, !subtitle.isEmpty {
                                            Text(subtitle)
                                                .font(.system(size: 12))
                                                .foregroundStyle(muted)
                                        }
                                    }
                                    .frame(maxWidth: .infinity, alignment: .leading)
                                    .padding(12)
                                    .background(fieldBg)
                                    .clipShape(RoundedRectangle(cornerRadius: 12))
                                }
                                .buttonStyle(.plain)
                            }
                        }
                    }

                    if canAdd, !adding, !filtered.isEmpty {
                        addButton
                    }

                    if adding {
                        addForm
                    }
                }
                .padding(16)
            }
            .background(sheetBg.ignoresSafeArea())
            .navigationTitle(title)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Close") { onDismiss() }
                        .foregroundStyle(accent)
                }
            }
        }
        .presentationDetents([.medium, .large])
        .presentationDragIndicator(.visible)
    }

    private var addButton: some View {
        Button {
            newName = query.trimmingCharacters(in: .whitespacesAndNewlines)
            adding = true
        } label: {
            Text("+ Add \(title)")
                .font(.system(size: 13, weight: .semibold))
                .foregroundStyle(accent)
        }
        .buttonStyle(.plain)
    }

    private var addForm: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("Add \(title)")
                .font(.system(size: 14, weight: .semibold))
                .foregroundStyle(.white)
            TextField("Name", text: $newName)
                .textFieldStyle(.plain)
                .foregroundStyle(.white)
                .padding(12)
                .background(sheetBg)
                .overlay(
                    RoundedRectangle(cornerRadius: 12)
                        .stroke(Color.white.opacity(0.12), lineWidth: 1)
                )
                .clipShape(RoundedRectangle(cornerRadius: 12))

            if productPicker {
                Text("Base unit")
                    .font(.system(size: 12))
                    .foregroundStyle(muted)
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        ForEach(OperatingSeedUoms.categories, id: \.self) { cat in
                            Button {
                                unitCategory = cat
                                if let first = OperatingSeedUoms.entries(category: cat).first {
                                    newUnit = first.code
                                }
                            } label: {
                                Text(cat)
                                    .font(.system(size: 12, weight: .semibold))
                                    .foregroundStyle(unitCategory == cat ? Color.black : Color.white.opacity(0.85))
                                    .padding(.horizontal, 12)
                                    .padding(.vertical, 8)
                                    .background(unitCategory == cat ? accent : fieldBg)
                                    .clipShape(Capsule())
                            }
                            .buttonStyle(.plain)
                        }
                    }
                }
                LazyVGrid(columns: [GridItem(.adaptive(minimum: 88), spacing: 8)], spacing: 8) {
                    ForEach(categoryUnits) { entry in
                        Button {
                            newUnit = entry.code
                            if !OperatingSeedUoms.needsPackConversion(entry.code) {
                                newConversion = ""
                            }
                        } label: {
                            Text(entry.label)
                                .font(.system(size: 12, weight: .medium))
                                .foregroundStyle(newUnit == entry.code ? Color.black : Color.white.opacity(0.9))
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 10)
                                .padding(.horizontal, 8)
                                .background(newUnit == entry.code ? accent : sheetBg)
                                .clipShape(RoundedRectangle(cornerRadius: 10))
                        }
                        .buttonStyle(.plain)
                    }
                }
                if showPackConversion {
                    TextField(
                        "",
                        text: $newConversion,
                        prompt: Text("Units of base (e.g. pc) per pack").foregroundStyle(muted)
                    )
                    .textFieldStyle(.plain)
                    .foregroundStyle(.white)
                    .padding(12)
                    .background(sheetBg)
                    .overlay(
                        RoundedRectangle(cornerRadius: 12)
                            .stroke(Color.white.opacity(0.12), lineWidth: 1)
                    )
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                }
            } else if uomPicker {
                TextField(
                    "",
                    text: $newUnit,
                    prompt: Text("Code / unit (required)").foregroundStyle(muted)
                )
                .textFieldStyle(.plain)
                .foregroundStyle(.white)
                .padding(12)
                .background(sheetBg)
                .clipShape(RoundedRectangle(cornerRadius: 12))
            }

            if let match = exactMatch {
                Text("Matching existing: \(match.name). Choose existing or confirm a distinct name.")
                    .font(.system(size: 11))
                    .foregroundStyle(Color(red: 0.94, green: 0.78, blue: 0.45))
                Button("Use existing") {
                    onSelect(match)
                    onDismiss()
                }
                .font(.system(size: 13, weight: .semibold))
                .foregroundStyle(accent)
            }

            HStack {
                Button("Cancel") { adding = false }
                    .font(.system(size: 13))
                    .foregroundStyle(muted)
                Spacer()
                Button("Save", action: saveAdd)
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundStyle(accent)
            }
        }
        .padding(12)
        .background(fieldBg)
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }

    private func saveAdd() {
        let name = newName.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !name.isEmpty else { return }
        let unit = newUnit.trimmingCharacters(in: .whitespacesAndNewlines)
        if needsUnit, unit.isEmpty { return }
        if showPackConversion {
            let conv = newConversion.trimmingCharacters(in: .whitespacesAndNewlines)
            if conv.isEmpty { return }
        }
        if let match = exactMatch {
            onSelect(match)
            onDismiss()
            return
        }
        let unitArg = unit.isEmpty ? nil : unit
        let conversionArg = showPackConversion
            ? newConversion.trimmingCharacters(in: .whitespacesAndNewlines).nilIfBlank
            : nil
        onAdd?(name, unitArg, conversionArg)
        adding = false
        onDismiss()
    }
}

private extension String {
    var nilIfBlank: String? {
        let t = trimmingCharacters(in: .whitespacesAndNewlines)
        return t.isEmpty ? nil : t
    }
}
