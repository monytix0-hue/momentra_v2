import SwiftUI

struct MfgPartsBrowserSheet: View {
    let theme: OperatingTheme
    @ObservedObject var model: BusinessOperatingModel
    var onAdd: (OperatingMasterPayload) -> Void
    var onNewPart: () -> Void
    var onDismiss: () -> Void = {}

    @Environment(\.dismiss) private var dismiss
    @State private var query = ""
    @State private var cat = "Finished"

    private var parts: [OperatingMasterPayload] {
        let source = model.masters.isEmpty ? (model.projection?.masters ?? []) : model.masters
        return source.filter {
            ["SKU", "PART", "PRODUCT"].contains($0.kind.uppercased())
                && (query.isEmpty
                    || $0.name.localizedCaseInsensitiveContains(query)
                    || ($0.code?.localizedCaseInsensitiveContains(query) ?? false))
        }
    }

    private var filtered: [OperatingMasterPayload] {
        parts.filter { mfgPartCategory($0) == cat }
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 12) {
                MfgGrabber()
                MfgSheetHeader(theme: theme, title: "Parts browser", subtitle: "Add into SO / WO", onClose: {
                    onDismiss()
                    dismiss()
                })
                MfgScanField(theme: theme, placeholder: "Search or scan part", query: $query, action: {})
                MfgChipRow(theme: theme, options: ["Raw", "WIP", "Finished", "Consumables", "Spares"], selection: $cat)
                ForEach(filtered.prefix(20), id: \.id) { p in
                    MfgPickerRow(
                        theme: theme,
                        label: p.name,
                        value: [p.code, p.unit, mfgPartCategory(p)].compactMap { $0 }.joined(separator: " · ")
                    ) {
                        onAdd(p)
                    }
                }
                if filtered.isEmpty {
                    Text(parts.isEmpty ? "No parts" : "No \(cat) matches")
                        .font(.plusJakarta(size: 12))
                        .foregroundStyle(MfgSheetChrome.textBody)
                }
                MfgDenseCreateCta(theme: theme, title: "+ New part / SKU", hint: "Opens parts master", action: onNewPart)
                MfgWhyBox(theme: theme, text: "Selecting a part prefills Order (SO / WO) — commitment only, no revenue or GST.")
                MfgStickyFooter(
                    theme: theme,
                    title: "Done",
                    caption: "\(filtered.count) of \(parts.count) · \(cat)",
                    action: {
                        onDismiss()
                        dismiss()
                    }
                )
            }
            .padding(16)
            .padding(.bottom, 24)
        }
        .background(theme.canvas)
        .task { if model.masters.isEmpty { await model.loadMasters() } }
    }
}

struct MfgMaterialsBrowserSheet: View {
    let theme: OperatingTheme
    @ObservedObject var model: BusinessOperatingModel
    var onAdd: (OperatingMasterPayload) -> Void
    var onNewBom: () -> Void
    var onDismiss: () -> Void = {}

    @Environment(\.dismiss) private var dismiss
    @State private var query = ""

    private var mats: [OperatingMasterPayload] {
        let source = model.masters.isEmpty ? (model.projection?.masters ?? []) : model.masters
        return source.filter {
            ["SKU", "PART", "MATERIAL"].contains($0.kind.uppercased())
                && (query.isEmpty || $0.name.localizedCaseInsensitiveContains(query))
        }
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 12) {
                MfgGrabber()
                MfgSheetHeader(theme: theme, title: "Materials browser", subtitle: "On-hand for PO", onClose: {
                    onDismiss()
                    dismiss()
                })
                MfgScanField(theme: theme, placeholder: "Search materials", query: $query, action: {})
                ForEach(mats.prefix(20), id: \.id) { m in
                    MfgPickerRow(theme: theme, label: m.name, value: "\(m.unit ?? "nos") · on-hand · Unavailable") {
                        onAdd(m)
                    }
                }
                MfgDenseCreateCta(theme: theme, title: "+ New BOM", hint: "Opens BOM master", action: onNewBom)
                MfgWhyBox(theme: theme, text: "On-hand qty stays Unavailable until a stock balance API exists. Selection still prefills PO.")
                MfgStickyFooter(
                    theme: theme,
                    title: "Done",
                    caption: "On-hand · Unavailable",
                    action: {
                        onDismiss()
                        dismiss()
                    }
                )
            }
            .padding(16)
            .padding(.bottom, 24)
        }
        .background(theme.canvas)
        .task { if model.masters.isEmpty { await model.loadMasters() } }
    }
}
