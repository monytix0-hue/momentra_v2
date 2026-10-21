import SwiftUI

struct OperatingLovOption: Identifiable, Hashable {
    let id: String
    let name: String
    let kind: String
    let unit: String?
    let subtitle: String?
}

struct SearchableOperatingLovSheet: View {
    let title: String
    let kind: String
    let options: [OperatingLovOption]
    let canAdd: Bool
    var accent: Color = Color(hex: "#6DDCFF")
    var onSelect: (OperatingLovOption) -> Void
    var onAdd: ((String, String?) -> Void)?
    var onDismiss: () -> Void

    @State private var query = ""
    @State private var adding = false
    @State private var newName = ""
    @State private var newUnit = ""

    private var filtered: [OperatingLovOption] {
        let q = query.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        let base = options.filter { $0.kind.uppercased() == kind.uppercased() }
        if q.isEmpty { return base }
        return base.filter {
            $0.name.lowercased().contains(q) || ($0.subtitle?.lowercased().contains(q) ?? false)
        }
    }

    private var exactMatch: OperatingLovOption? {
        let q = query.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        guard !q.isEmpty else { return nil }
        return filtered.first { $0.name.lowercased() == q }
    }

    var body: some View {
        NavigationStack {
            VStack(alignment: .leading, spacing: 12) {
                HStack {
                    Image(systemName: "magnifyingglass")
                    TextField("Search \(title.lowercased())", text: $query)
                        .font(.plusJakarta(size: 14))
                }
                .padding(12)
                .background(Color(hex: "#1A2330"))
                .clipShape(RoundedRectangle(cornerRadius: 12))

                if filtered.isEmpty {
                    Text("No matching \(title.lowercased())")
                        .font(.plusJakarta(size: 13))
                        .foregroundStyle(Color(hex: "#9AA3B2"))
                    if canAdd {
                        Button {
                            newName = query
                            adding = true
                        } label: {
                            Label("+ Add \(title)", systemImage: "plus")
                                .font(.plusJakarta(size: 14, weight: .semibold))
                        }
                        .foregroundStyle(accent)
                    } else {
                        Text("Ask an admin to add this.")
                            .font(.plusJakarta(size: 12))
                            .foregroundStyle(Color(hex: "#9AA3B2"))
                    }
                } else {
                    ScrollView {
                        LazyVStack(alignment: .leading, spacing: 8) {
                            ForEach(filtered) { option in
                                Button {
                                    onSelect(option)
                                    onDismiss()
                                } label: {
                                    VStack(alignment: .leading, spacing: 2) {
                                        Text(option.name)
                                            .font(.plusJakarta(size: 15, weight: .semibold))
                                            .foregroundStyle(Color(hex: "#E8EEF7"))
                                        if let subtitle = option.subtitle, !subtitle.isEmpty {
                                            Text(subtitle)
                                                .font(.plusJakarta(size: 12))
                                                .foregroundStyle(Color(hex: "#9AA3B2"))
                                        }
                                    }
                                    .frame(maxWidth: .infinity, alignment: .leading)
                                    .padding(12)
                                    .background(Color(hex: "#15202B"))
                                    .clipShape(RoundedRectangle(cornerRadius: 12))
                                }
                                .buttonStyle(.plain)
                            }
                        }
                    }
                }

                if canAdd && !adding {
                    Button {
                        newName = query
                        adding = true
                    } label: {
                        Text("+ Add \(title)")
                            .font(.plusJakarta(size: 13, weight: .semibold))
                    }
                    .foregroundStyle(accent)
                }

                if adding {
                    VStack(alignment: .leading, spacing: 8) {
                        Text("Add \(title)").font(.plusJakarta(size: 14, weight: .semibold))
                        TextField("Name", text: $newName)
                            .padding(10)
                            .background(Color(hex: "#1A2330"))
                            .clipShape(RoundedRectangle(cornerRadius: 10))
                        if kind.uppercased() == "PRODUCT" || kind.uppercased() == "UOM" {
                            TextField(kind.uppercased() == "UOM" ? "Code / unit" : "Base UOM", text: $newUnit)
                                .padding(10)
                                .background(Color(hex: "#1A2330"))
                                .clipShape(RoundedRectangle(cornerRadius: 10))
                        }
                        if let match = exactMatch {
                            Text("Matching existing: \(match.name). Choose existing or confirm a distinct name.")
                                .font(.plusJakarta(size: 11))
                                .foregroundStyle(Color(hex: "#F0C674"))
                            Button("Use existing") {
                                onSelect(match)
                                onDismiss()
                            }
                            .font(.plusJakarta(size: 13, weight: .semibold))
                        }
                        HStack {
                            Button("Cancel") { adding = false }
                            Spacer()
                            Button("Save") {
                                let name = newName.trimmingCharacters(in: .whitespacesAndNewlines)
                                guard !name.isEmpty else { return }
                                if let match = exactMatch {
                                    onSelect(match)
                                } else {
                                    onAdd?(name, newUnit.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? nil : newUnit)
                                }
                                onDismiss()
                            }
                            .font(.plusJakarta(size: 14, weight: .semibold))
                            .foregroundStyle(accent)
                        }
                    }
                    .padding(12)
                    .background(Color(hex: "#15202B"))
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                }
            }
            .padding(16)
            .background(Color(hex: "#0E1620"))
            .navigationTitle(title)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Close") { onDismiss() }
                }
            }
        }
    }
}

enum OperatingSeedUoms {
    static let all: [OperatingLovOption] = [
        ("pc", "piece (pc)"), ("pair", "pair"), ("set", "set"), ("pack", "pack"),
        ("box", "box"), ("carton", "carton"), ("dozen", "dozen (12 pc)"),
        ("kg", "kg"), ("g", "g"), ("tonne", "tonne"), ("L", "litre (L)"), ("mL", "mL"),
        ("m", "metre (m)"), ("cm", "cm"), ("foot", "foot"), ("sq ft", "sq ft"), ("sq m", "sq m"),
    ].map { OperatingLovOption(id: "seed-uom-\($0.0)", name: $0.0, kind: "UOM", unit: $0.0, subtitle: $0.1) }
}
