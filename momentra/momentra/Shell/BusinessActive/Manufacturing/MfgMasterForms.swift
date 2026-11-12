import SwiftUI

enum MfgMasterFormKind: String, Identifiable {
    case vendor, lineStore, part, bom
    var id: String { rawValue }
}

struct MfgMasterFormSheet: View {
    let theme: OperatingTheme
    let kind: MfgMasterFormKind
    @ObservedObject var model: BusinessOperatingModel

    @Environment(\.dismiss) private var dismiss
    @State private var isCreate = true
    @State private var name = ""
    @State private var legal = ""
    @State private var gstin = ""
    @State private var gstReg = "Regular"
    @State private var phone = ""
    @State private var email = ""
    @State private var supplies = "Steel"
    @State private var terms = "Net 30"
    @State private var address = ""
    @State private var status = "Active"
    @State private var type = "Line"
    @State private var printer = "Tablet only"
    @State private var holds = "Raw"
    @State private var category = "Finished"
    @State private var partType = "Finished"
    @State private var sell = ""
    @State private var cost = ""
    @State private var gst = "18%"
    @State private var hsn = ""
    @State private var unit = "nos"
    @State private var drawing = ""
    @State private var trackBatch = "Track batch/heat"
    @State private var routing = "Press"
    @State private var issue = "Backflush on accept"
    @State private var issueFrom = "Raw store"
    @State private var defaultLine = "Line 1"
    @State private var scrapPct = "2"
    @State private var batch = "1"
    @State private var partContext = "Parts master"
    @State private var components: [String] = ["CRC sheet", "M8 bolt set"]
    @State private var saving = false

    private var finishedParts: [OperatingMasterPayload] {
        let source = model.masters.isEmpty ? (model.projection?.masters ?? []) : model.masters
        return source.filter { ["SKU", "PART", "PRODUCT"].contains($0.kind.uppercased()) }
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 12) {
                MfgGrabber()
                MfgSheetHeader(theme: theme, title: title, onClose: { dismiss() })
                MfgCreateEditMode(theme: theme, isCreate: $isCreate, editLocked: true)
                formBody
                MfgWhyBox(theme: theme, text: why)
                MfgStickyFooter(
                    theme: theme,
                    title: saving ? "Saving…" : cta,
                    disabled: name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty,
                    loading: saving,
                    action: { Task { await save() } }
                )
            }
            .padding(16)
            .padding(.bottom, 24)
        }
        .background(theme.canvas)
        .task { if model.masters.isEmpty { await model.loadMasters() } }
    }

    private var title: String {
        switch kind {
        case .vendor: return "New vendor"
        case .lineStore: return "Add line / store"
        case .part: return "Create part / SKU"
        case .bom: return "Create BOM"
        }
    }

    private var cta: String {
        switch kind {
        case .vendor: return "Save vendor"
        case .lineStore: return "Save line / store"
        case .part: return "Save part"
        case .bom: return "Save BOM"
        }
    }

    private var why: String {
        switch kind {
        case .vendor: return "Vendor master save does not post GST, AP, or stock. GSTIN is identity for later invoices."
        case .lineStore: return "Line/store master is capacity chrome. Job-card printer chips are UI-only until a print stack ships."
        case .part: return "Part master save does not post GST. Rate chips are labels — tax posts on Sale invoice / GRN only."
        case .bom: return "Save BOM stores supported master fields only. Stock moves only on issue / accepted production — never on BOM save. GST is informational."
        }
    }

    @ViewBuilder
    private var formBody: some View {
        switch kind {
        case .vendor:
            MfgTextField(theme: theme, placeholder: "Vendor name", text: $name)
            MfgTextField(theme: theme, placeholder: "Legal / trade name", text: $legal)
            MfgTextField(theme: theme, placeholder: "GSTIN", text: $gstin)
            MfgChipRow(theme: theme, options: ["Regular", "Composition", "Unregistered"], selection: $gstReg)
            MfgTextField(theme: theme, placeholder: "Phone", text: $phone, keyboard: .phonePad)
            MfgTextField(theme: theme, placeholder: "Email optional", text: $email, keyboard: .emailAddress)
            MfgChipRow(theme: theme, options: ["Steel", "Raw", "Finished", "Spares", "Consumables", "Job work"], selection: $supplies)
            MfgTextField(theme: theme, placeholder: "Payment terms", text: $terms)
            MfgTextField(theme: theme, placeholder: "Address", text: $address)
            MfgChipRow(theme: theme, options: ["Active", "Preferred", "Inactive"], selection: $status)
        case .lineStore:
            MfgTextField(theme: theme, placeholder: "Name", text: $name)
            MfgChipRow(theme: theme, options: ["Store", "Line", "Station"], selection: $type)
            MfgChipRow(theme: theme, options: ["Shop printer 1", "Label printer", "Tablet only"], selection: $printer)
            MfgChipRow(theme: theme, options: ["Raw", "WIP", "Finished", "Consumables", "Spares"], selection: $holds)
            MfgChipRow(theme: theme, options: ["Active", "Paused"], selection: $status)
            MfgLockedChip(theme: theme, label: "Print SDK · chrome only")
        case .part:
            MfgChipRow(theme: theme, options: ["Forge & Form ERP", "Parts master", "GST-aware"], selection: $partContext)
            MfgTextField(theme: theme, placeholder: "Part name", text: $name)
            MfgChipRow(theme: theme, options: ["Raw", "WIP", "Finished", "Consumables", "Spares"], selection: $category)
            MfgChipRow(theme: theme, options: ["Finished", "Raw", "WIP"], selection: $partType)
            MfgAmountHero(theme: theme, currency: model.currency, amount: $sell, label: "Sell price", caption: nil)
            MfgTextField(theme: theme, placeholder: "Standard cost", text: $cost, keyboard: .decimalPad)
            MfgChipRow(theme: theme, options: ["18%", "5%", "Exempt"], selection: $gst)
            MfgTextField(theme: theme, placeholder: "HSN / SAC", text: $hsn)
            MfgChipRow(theme: theme, options: ["nos", "kg", "set"], selection: $unit)
            MfgTextField(theme: theme, placeholder: "Drawing / revision", text: $drawing)
            MfgChipRow(theme: theme, options: ["Track batch/heat", "No batch"], selection: $trackBatch)
            MfgChipRow(theme: theme, options: ["Active", "Inactive"], selection: $status)
        case .bom:
            MfgTextField(theme: theme, placeholder: "Finished part", text: $name)
            MfgChipRow(theme: theme, options: ["Press", "Weld", "Paint", "QC", "Pack", "Dispatch"], selection: $routing)
            MfgChipRow(theme: theme, options: ["nos", "kg", "set"], selection: $unit)
            MfgAmountHero(theme: theme, label: "Rolled-up cost", amountText: "Est. · UI local", caption: "Components local until BOM write API accepts lines")
            MfgChipRow(theme: theme, options: ["18%", "5%", "Exempt"], selection: $gst)
            MfgTextField(theme: theme, placeholder: "HSN (FG)", text: $hsn)
            MfgTextField(theme: theme, placeholder: "Issue from", text: $issueFrom)
            MfgTextField(theme: theme, placeholder: "Batch size", text: $batch, keyboard: .numberPad)
            MfgTextField(theme: theme, placeholder: "Default line", text: $defaultLine)
            MfgTextField(theme: theme, placeholder: "Scrap %", text: $scrapPct, keyboard: .decimalPad)
            MfgChipRow(theme: theme, options: ["Backflush on accept", "Manual issue"], selection: $issue)
            MfgChipRow(theme: theme, options: ["Active", "Inactive"], selection: $status)
            // D6: UI-local component list parity with Android when BOM explosion API is absent.
            MfgLinesCard(theme: theme, title: "Components", trailing: "\(components.count)") {
                ForEach(components, id: \.self) { row in
                    MfgPickerRow(theme: theme, label: row, value: "UI local")
                }
                Button {
                    if let next = finishedParts.first(where: { p in !components.contains(p.name) }) {
                        components.append(next.name)
                    }
                } label: {
                    Text("+ Add component")
                        .font(.plusJakarta(size: 13, weight: .bold))
                        .foregroundStyle(theme.accentSoft)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(.vertical, 8)
                }
                .buttonStyle(.plain)
            }
        }
    }

    private func save() async {
        saving = true
        defer { saving = false }
        let masterKind: String
        var note: String
        switch kind {
        case .vendor:
            masterKind = "SUPPLIER"
            note = "supplies=\(supplies) · terms=\(terms) · status=\(status) · phone=\(phone) · email=\(email) · \(address) · gst=\(gstReg)"
        case .lineStore:
            masterKind = "LOCATION"
            note = "type=\(type) · printer=\(printer) · holds=\(holds) · status=\(status)"
        case .part:
            masterKind = "SKU"
            note = "category=\(category) · type=\(partType) · cost=\(cost) · drawing=\(drawing) · \(trackBatch) · gst=\(gst) · hsn=\(hsn) · status=\(status)"
        case .bom:
            masterKind = "BOM"
            note = "routing=\(routing) · issueFrom=\(issueFrom) · line=\(defaultLine) · scrap=\(scrapPct)% · batch=\(batch) · \(issue) · components=\(components.joined(separator: ",")) · status=\(status) · hsn=\(hsn)"
        }
        let ok = await model.record(OperatingEventInput(
            momentId: model.momentId,
            eventType: "UPDATE",
            title: name,
            amount: kind == .part && !sell.isEmpty ? sell : nil,
            note: note,
            channelName: kind == .bom ? defaultLine : nil,
            counterpartyName: kind == .vendor ? name : (kind == .bom ? issueFrom : nil),
            unit: kind == .part || kind == .bom ? unit : nil,
            masterKind: masterKind
        ))
        if ok { dismiss() }
    }
}
