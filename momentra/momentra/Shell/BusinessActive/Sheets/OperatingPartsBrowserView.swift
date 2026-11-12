import SwiftUI

/// Parts / Materials browsers for MFG + MTO.
/// Figma: `2706:37762`, `2706:37822`, `2547:29629`.

enum OperatingBrowserKind: String {
    case parts
    case materials
}

struct OperatingBrowserItem: Identifiable {
    var id: String
    var name: String
    var meta: String
    var priceLabel: String?
    var category: String
    var unit: String? = nil
    /// Raw catalogue price (no currency glyph) for capture prefills.
    var unitPrice: String? = nil
    var masterKind: String? = nil
    var code: String? = nil
}

func captureHintForBrowser(kind: OperatingBrowserKind, subtype: String?) -> String {
    switch kind {
    case .materials: return "PURCHASE"
    case .parts:
        switch subtype {
        case "TRADING_WHOLESALE", "DISTRIBUTION_DEALERSHIP": return "SALE"
        default: return "ORDER"
        }
    }
}

/// Projection FEFO line for a selected catalogue master — never invents an expiry date.
func fefoFactLabel(master: OperatingMasterPayload?) -> String {
    guard let master else { return "Unavailable" }
    let batch = master.kind.uppercased() == "BATCH"
    let code = master.code?.trimmingCharacters(in: .whitespacesAndNewlines)
    let hasCode = !(code?.isEmpty ?? true)
    if batch, hasCode, let code { return "Batch \(code) · expiry Unavailable" }
    if batch { return "Batch · expiry Unavailable" }
    if hasCode, let code { return "SKU \(code) · FEFO Unavailable" }
    return "Unavailable"
}

struct OperatingPartsBrowserView: View {
    let theme: OperatingTheme
    let kind: OperatingBrowserKind
    let items: [OperatingBrowserItem]
    var contextLabel: String?
    var onAdd: (OperatingBrowserItem) -> Void
    var onCreateNew: () -> Void
    var onDismiss: () -> Void

    @State private var query = ""
    @State private var selectedCat = ""

    private var distSku: Bool { kind == .parts && theme.isDistributionCapture }

    private var categories: [String] {
        if distSku { return DistributionMasterCopy.skuCategories }
        switch kind {
        case .parts: return ["Raw", "WIP", "Jobs", "Consumables", "Spares"]
        case .materials: return ["Raw", "Consumables", "Finished", "Packing", "Spares"]
        }
    }

    private var filtered: [OperatingBrowserItem] {
        let q = query.trimmingCharacters(in: .whitespacesAndNewlines)
        let cat = selectedCat.isEmpty ? categories.first! : selectedCat
        let hasCat = items.contains { $0.category.caseInsensitiveCompare(cat) == .orderedSame }
        return items.filter { item in
            let matchesQuery = q.isEmpty
                || item.name.localizedCaseInsensitiveContains(q)
                || item.meta.localizedCaseInsensitiveContains(q)
            let matchesCat = !hasCat || cat == categories.first || item.category.caseInsensitiveCompare(cat) == .orderedSame
            return matchesQuery && matchesCat
        }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(alignment: .top) {
                VStack(alignment: .leading, spacing: 2) {
                    Text(
                        distSku
                            ? DistributionMasterCopy.skuBrowserTitle
                            : (kind == .parts ? "Parts & products" : "Materials")
                    )
                        .font(.plusJakarta(size: 14, weight: .bold))
                        .foregroundStyle(theme.ink)
                    Text(
                        distSku
                            ? DistributionMasterCopy.skuBrowserHint
                            : (kind == .parts ? "Tap Add · qty per quote / job line" : (contextLabel ?? "Job materials + on-hand"))
                    )
                        .font(.plusJakarta(size: 11))
                        .foregroundStyle(theme.body)
                }
                Spacer()
                Button(action: onDismiss) {
                    Text("✕")
                        .font(.plusJakarta(size: 13, weight: .bold))
                        .foregroundStyle(theme.body)
                        .frame(width: 32, height: 32)
                        .background(theme.inset)
                        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                }
                .buttonStyle(.plain)
            }

            HStack {
                TextField(
                    distSku
                        ? DistributionMasterCopy.skuBrowserSearch
                        : (kind == .parts ? "Search part, drawing or HSN" : "Search material or HSN"),
                    text: $query
                )
                    .font(.plusJakarta(size: 13, weight: .medium))
                    .foregroundStyle(theme.ink)
                Text("Scan")
                    .font(.plusJakarta(size: 12, weight: .bold))
                    .foregroundStyle(theme.accent)
            }
            .padding(12)
            .background(theme.inset)
            .overlay(
                RoundedRectangle(cornerRadius: 12, style: .continuous)
                    .stroke(theme.border, lineWidth: 1)
            )
            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))

            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 6) {
                    ForEach(categories, id: \.self) { cat in
                        let selected = (selectedCat.isEmpty ? categories.first : selectedCat) == cat
                        Button {
                            selectedCat = cat
                        } label: {
                            Text(cat)
                                .font(.plusJakarta(size: 12, weight: selected ? .bold : .medium))
                                .foregroundStyle(selected ? Color(hex: "#16181F") : theme.body)
                                .padding(.horizontal, 12)
                                .padding(.vertical, 8)
                                .background(selected ? theme.accent : theme.inset)
                                .overlay(
                                    Capsule().stroke(selected ? theme.accent : theme.border, lineWidth: 1)
                                )
                                .clipShape(Capsule())
                        }
                        .buttonStyle(.plain)
                    }
                }
            }

            ScrollView {
                LazyVStack(spacing: 8) {
                    if filtered.isEmpty {
                        Text("No parts recorded yet — add a catalogue SKU, or create one below. Missing stock never shows as 0.")
                            .font(.plusJakarta(size: 12))
                            .foregroundStyle(theme.body)
                            .padding(.vertical, 8)
                    } else {
                        ForEach(filtered) { item in
                            row(item)
                                .contentShape(Rectangle())
                                .onTapGesture { onAdd(item) }
                        }
                    }
                }
            }

            Button(action: onCreateNew) {
                HStack {
                    Text(
                        distSku
                            ? DistributionMasterCopy.skuBrowserCreate
                            : (kind == .parts ? "+ New part / SKU" : "+ New BOM")
                    )
                        .font(.plusJakarta(size: 13, weight: .bold))
                        .foregroundStyle(theme.accent)
                    Spacer()
                    Text(
                        distSku
                            ? DistributionMasterCopy.skuBrowserCreateHint
                            : (kind == .parts ? "Name · HSN · GST · UOM" : "Components · qty · scrap %")
                    )
                        .font(.plusJakarta(size: 11))
                        .foregroundStyle(theme.body)
                }
                .padding(12)
                .background(theme.accent.opacity(0.08))
                .overlay(
                    RoundedRectangle(cornerRadius: 12, style: .continuous)
                        .stroke(theme.accent.opacity(0.40), lineWidth: 1)
                )
                .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
            }
            .buttonStyle(.plain)
        }
        .padding(14)
        .background(distSku ? theme.captureSheet : theme.surface)
        .overlay(
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .stroke(theme.accent.opacity(0.40), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
        .onAppear {
            if selectedCat.isEmpty { selectedCat = categories.first ?? "Raw" }
        }
    }

    private func row(_ item: OperatingBrowserItem) -> some View {
        HStack(spacing: 10) {
            VStack(alignment: .leading, spacing: 2) {
                Text(item.name)
                    .font(.plusJakarta(size: 13, weight: .bold))
                    .foregroundStyle(theme.ink)
                    .lineLimit(1)
                Text(item.meta)
                    .font(.plusJakarta(size: 11))
                    .foregroundStyle(theme.body)
                    .lineLimit(1)
            }
            Spacer(minLength: 0)
            if let price = item.priceLabel, !price.isEmpty {
                Text(price)
                    .font(.plusJakarta(size: 13, weight: .bold))
                    .foregroundStyle(theme.accent)
            }
            Button {
                onAdd(item)
            } label: {
                Text("Add")
                    .font(.plusJakarta(size: 12, weight: .bold))
                    .foregroundStyle(Color(hex: "#16181F"))
                    .padding(.horizontal, 12)
                    .padding(.vertical, 8)
                    .background(theme.accent)
                    .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
            }
            .buttonStyle(.plain)
        }
        .padding(12)
        .background(theme.inset)
        .overlay(
            RoundedRectangle(cornerRadius: 14, style: .continuous)
                .stroke(theme.accent.opacity(0.30), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
    }
}

extension BusinessOperatingModel {
    func browserItems(kind: OperatingBrowserKind) -> [OperatingBrowserItem] {
        let source = masters.isEmpty ? (projection?.masters ?? []) : masters
        let kinds: Set<String> = kind == .parts
            ? ["SKU", "PRODUCT", "BATCH", "PART", "BOM"]
            : ["SKU", "PRODUCT", "BATCH", "MATERIAL", "RAW"]
        return source
            .filter { kinds.contains($0.kind.uppercased()) || $0.kind.isEmpty }
            .map { master in
                let name = master.name.isEmpty ? "Unnamed part" : master.name
                let category: String = {
                    let k = master.kind.uppercased()
                    if k == "BATCH" || name.localizedCaseInsensitiveContains("wip") { return "WIP" }
                    if name.localizedCaseInsensitiveContains("spare") { return "Spares" }
                    if name.localizedCaseInsensitiveContains("pack") { return "Packing" }
                    if name.localizedCaseInsensitiveContains("consum") { return "Consumables" }
                    return "Raw"
                }()
                let rawPrice = [master.sellPrice, master.purchasePrice, master.mrp]
                    .compactMap { $0?.nilIfBlank }
                    .first
                    .map { $0.hasPrefix("₹") ? String($0.dropFirst()) : $0 }
                var metaParts: [String] = [
                    master.unit?.nilIfBlank,
                    master.code?.nilIfBlank,
                    master.hsn?.nilIfBlank.map { "HSN \($0)" },
                ].compactMap { $0 }
                if master.kind.uppercased() == "BATCH" {
                    metaParts.append("FEFO · Unavailable")
                }
                let meta = metaParts.joined(separator: " · ")
                return OperatingBrowserItem(
                    id: master.id,
                    name: name,
                    meta: meta.isEmpty ? "Catalogue · details unavailable" : meta,
                    priceLabel: rawPrice.map { $0.hasPrefix("₹") ? $0 : "₹\($0)" },
                    category: category,
                    unit: master.unit?.nilIfBlank,
                    unitPrice: rawPrice,
                    masterKind: master.kind.nilIfBlank,
                    code: master.code?.nilIfBlank
                )
            }
    }
}

private extension String {
    var nilIfBlank: String? {
        let value = trimmingCharacters(in: .whitespacesAndNewlines)
        return value.isEmpty ? nil : value
    }
}
