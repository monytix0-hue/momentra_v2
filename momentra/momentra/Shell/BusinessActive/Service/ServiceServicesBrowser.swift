import SwiftUI

/// SVC-SHEET · Services browser — Figma `2533:29628`.
/// Compact picker used from Booking / Finalize Charge to Add a saved service or package.
struct ServiceServicesBrowserSheet: View {
    let theme: OperatingTheme
    @ObservedObject var model: BusinessOperatingModel
    var onAdd: (OperatingBrowserItem) -> Void
    var onDismiss: () -> Void

    @State private var query = ""
    @State private var category = "Hair"
    @State private var formKind: ServiceMasterFormKind?

    private let categories = ["Hair", "Skin", "Spa", "Wellness", "Nails"]

    private var items: [ServiceBrowserRow] {
        let source = model.masters.isEmpty ? (model.projection?.masters ?? []) : model.masters
        let kinds: Set<String> = ["PRODUCT", "ITEM", "SERVICE", "PACKAGE", "CONSUMABLE"]
        let mapped = source
            .filter { kinds.contains($0.kind.uppercased()) }
            .map { master in
                ServiceBrowserRow(
                    id: master.id,
                    name: master.name,
                    meta: metaLine(master),
                    priceLabel: master.sellPrice.map { OperatingMoneyFormat.format(model.currency, $0) } ?? "—",
                    unitPrice: master.sellPrice,
                    category: inferCategory(master),
                    sac: master.hsn,
                    masterKind: master.kind,
                    unit: master.unit,
                    isPackage: master.kind.uppercased() == "PACKAGE"
                )
            }
        let fixtures: [ServiceBrowserRow] = mapped.isEmpty ? Self.demoRows : mapped
        let q = query.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        return fixtures.filter { row in
            let catOk = category.isEmpty || row.category.compare(category, options: .caseInsensitive) == .orderedSame
                || row.name.localizedCaseInsensitiveContains(category)
            let qOk = q.isEmpty || row.name.lowercased().contains(q) || (row.sac?.lowercased().contains(q) ?? false)
            return catOk && qOk
        }
        .sorted { $0.name.localizedCaseInsensitiveCompare($1.name) == .orderedAscending }
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 12) {
                TealGrabber()
                HStack(alignment: .top) {
                    Text("Services")
                        .font(.plusJakarta(size: 22, weight: .bold))
                        .foregroundStyle(OperatingV12.title)
                    Spacer(minLength: 8)
                    Text("Tap Add · service or package")
                        .font(.plusJakarta(size: 11, weight: .medium))
                        .foregroundStyle(OperatingV12.subtitle)
                        .multilineTextAlignment(.trailing)
                    Button(action: onDismiss) {
                        Image(systemName: "xmark")
                            .font(.system(size: 13, weight: .bold))
                            .foregroundStyle(OperatingV12.subtitle)
                            .frame(width: 32, height: 32)
                            .background(theme.inset)
                            .clipShape(Circle())
                    }
                    .buttonStyle(.plain)
                    .accessibilityLabel("Close")
                }

                HStack(spacing: 8) {
                    TealTextField(theme: theme, placeholder: "Search service, package or SAC", text: $query)
                        .frame(maxWidth: .infinity)
                    Text("Filter")
                        .font(.plusJakarta(size: 12, weight: .bold))
                        .foregroundStyle(Color(hex: "#FBBF24"))
                        .padding(.horizontal, 12)
                        .padding(.vertical, 12)
                        .background(theme.inset)
                        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                }
                TealChipRow(theme: theme, options: categories, selection: $category, style: .lime)

                if items.isEmpty {
                    TealEmptyCard(
                        theme: theme,
                        title: "No offerings match",
                        bodyText: "Create a service or package — GST posts on Finalize Charge."
                    )
                } else {
                    ForEach(items) { row in
                        Button {
                            onAdd(
                                OperatingBrowserItem(
                                    id: row.id,
                                    name: row.name,
                                    meta: row.meta,
                                    priceLabel: row.priceLabel,
                                    category: row.category,
                                    unit: row.unit,
                                    unitPrice: row.unitPrice,
                                    masterKind: row.masterKind ?? (row.isPackage ? "PACKAGE" : "PRODUCT"),
                                    code: row.sac
                                )
                            )
                            onDismiss()
                        } label: {
                            HStack(alignment: .top, spacing: 10) {
                                VStack(alignment: .leading, spacing: 4) {
                                    Text(row.name)
                                        .font(.plusJakarta(size: 14, weight: .bold))
                                        .foregroundStyle(OperatingV12.title)
                                    Text(row.meta)
                                        .font(.plusJakarta(size: 11, weight: .medium))
                                        .foregroundStyle(OperatingV12.subtitle)
                                }
                                Spacer(minLength: 8)
                                VStack(alignment: .trailing, spacing: 4) {
                                    Text(row.priceLabel)
                                        .font(.plusJakarta(size: 13, weight: .bold))
                                        .foregroundStyle(theme.accent)
                                    Text("Add")
                                        .font(.plusJakarta(size: 11, weight: .bold))
                                        .foregroundStyle(TealSheetChrome.inkOnAccent)
                                        .padding(.horizontal, 12)
                                        .padding(.vertical, 6)
                                        .background(TealSheetChrome.lime)
                                        .clipShape(Capsule())
                                }
                            }
                            .padding(12)
                            .background(theme.surface)
                            .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
                            .overlay(
                                RoundedRectangle(cornerRadius: 14, style: .continuous)
                                    .strokeBorder(theme.accent.opacity(0.28), lineWidth: 1)
                            )
                        }
                        .buttonStyle(.plain)
                    }
                }

                Button { formKind = .service } label: {
                    browserCreateRow(title: "+ New service", hint: "Name · SAC · GST · duration")
                }
                .buttonStyle(.plain)
                Button { formKind = .package } label: {
                    browserCreateRow(title: "+ New package", hint: "Name · SAC · GST · duration")
                }
                .buttonStyle(.plain)
            }
            .padding(16)
            .padding(.bottom, 24)
        }
        .background(theme.canvas)
        .task { if model.masters.isEmpty { await model.loadMasters() } }
        .sheet(item: $formKind) { kind in
            ServiceMasterFormSheet(theme: theme, kind: kind, model: model) {
                formKind = nil
                Task { await model.loadMasters() }
            }
            .presentationDetents([.large])
            .preferredColorScheme(.dark)
        }
    }

    private func browserCreateRow(title: String, hint: String) -> some View {
        HStack {
            Text(title)
                .font(.plusJakarta(size: 14, weight: .bold))
                .foregroundStyle(OperatingV12.title)
            Spacer(minLength: 8)
            Text(hint)
                .font(.plusJakarta(size: 11, weight: .medium))
                .foregroundStyle(OperatingV12.subtitle)
                .lineLimit(1)
        }
        .padding(12)
        .frame(maxWidth: .infinity, alignment: .leading)
        .overlay(
            RoundedRectangle(cornerRadius: 12, style: .continuous)
                .strokeBorder(theme.accent.opacity(0.35), lineWidth: 1)
        )
    }

    private func metaLine(_ master: OperatingMasterPayload) -> String {
        let duration = master.unit?.trimmingCharacters(in: .whitespacesAndNewlines).nilIfEmpty ?? "45 min"
        let sac = master.hsn.map { "SAC \($0)" } ?? "SAC —"
        let gst = master.gstRate.map { "\($0)%" } ?? "—"
        return "\(duration) · \(sac) · \(gst)"
    }

    private func inferCategory(_ master: OperatingMasterPayload) -> String {
        let hay = "\(master.name) \(master.dimension ?? "") \(master.unit ?? "")".lowercased()
        return categories.first { hay.contains($0.lowercased()) } ?? "Hair"
    }

    private static let demoRows: [ServiceBrowserRow] = [
        .init(id: "demo-1", name: "Signature facial", meta: "60 min · SAC 999721 · 18%", priceLabel: "₹4,500", unitPrice: "4500", category: "Skin", sac: "999721", masterKind: "PRODUCT", unit: "60 min", isPackage: false),
        .init(id: "demo-2", name: "Haircut & style", meta: "45 min · SAC 999721 · 18%", priceLabel: "₹899", unitPrice: "899", category: "Hair", sac: "999721", masterKind: "PRODUCT", unit: "45 min", isPackage: false),
        .init(id: "demo-3", name: "Keratin package", meta: "3 sessions · SAC 999721 · 18%", priceLabel: "₹9,999", unitPrice: "9999", category: "Hair", sac: "999721", masterKind: "PACKAGE", unit: "3 sessions", isPackage: true),
    ]
}

private struct ServiceBrowserRow: Identifiable {
    let id: String
    let name: String
    let meta: String
    let priceLabel: String
    let unitPrice: String?
    let category: String
    let sac: String?
    let masterKind: String?
    let unit: String?
    let isPackage: Bool
}

private extension String {
    var nilIfEmpty: String? {
        let t = trimmingCharacters(in: .whitespacesAndNewlines)
        return t.isEmpty ? nil : t
    }
}
