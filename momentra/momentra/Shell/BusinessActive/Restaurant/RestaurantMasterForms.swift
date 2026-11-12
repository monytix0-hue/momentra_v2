import SwiftUI

/// Figma `2461:29655` Dish · `2461:29777` Raw · `2472:29659` Vendor · `2472:29750` Station.
enum RestaurantMasterFormKind: String, Identifiable {
    case dish, raw, vendor, station
    var id: String { rawValue }

    var title: String {
        switch self {
        case .dish: return "Create menu dish"
        case .raw: return "Create raw material"
        case .vendor: return "New vendor"
        case .station: return "Add station"
        }
    }

    var cta: String {
        switch self {
        case .dish: return "Save menu dish"
        case .raw: return "Save raw material"
        case .vendor: return "Save vendor"
        case .station: return "Save station"
        }
    }

    var masterKind: String {
        switch self {
        case .dish: return "MENU_DISH"
        case .raw: return "MATERIAL"
        case .vendor: return "VENDOR"
        case .station: return "STATION"
        }
    }

    var subtitle: String {
        switch self {
        case .dish: return "Master item · typed fields + GST before it appears in Sales Order"
        case .raw: return "Master stock item · unit, rate, HSN & GST before Purchase Order can add it"
        case .vendor: return "Supplier master · GSTIN drives input credit on Purchase / GRN"
        case .station: return "Kitchen & service stations · KOT routing target"
        }
    }

    var why: String {
        switch self {
        case .dish: return RestaurantCaptureCopy.dishWhy
        case .raw: return RestaurantCaptureCopy.rawWhy
        case .vendor: return RestaurantCaptureCopy.vendorFormWhy
        case .station: return RestaurantCaptureCopy.stationFormWhy
        }
    }

    var caveat: String {
        switch self {
        case .vendor: return "Vendor saved · available in Purchase Order"
        case .station: return "Station saved · KOT routing updated"
        default: return "Master saved · available in browser on next open"
        }
    }

    static func from(tab: OperatingMasterTab) -> RestaurantMasterFormKind? {
        switch tab {
        case .catalogue: return .dish
        case .principals: return .vendor
        case .locations: return .station
        default: return nil
        }
    }
}

struct RestaurantMasterFormSheet: View {
    let theme: OperatingTheme
    let kind: RestaurantMasterFormKind
    @ObservedObject var model: BusinessOperatingModel
    var existing: OperatingMasterPayload? = nil
    var onSaved: () -> Void = {}

    @Environment(\.dismiss) private var dismiss
    @State private var isCreate = true
    @State private var name = ""
    @State private var legalName = ""
    @State private var gstin = "27AAACC1234D1Z7"
    @State private var gstRegistration = "Regular"
    @State private var phone = "+91 98200 12345"
    @State private var email = ""
    @State private var category = "Mains"
    @State private var paymentTerms = "Net 7 days"
    @State private var address = "Sassoon Dock, Colaba, Mumbai"
    @State private var status = "Active"
    @State private var unit = "kg"
    @State private var sellPrice = "480"
    @State private var fullPrice = "720"
    @State private var purchasePrice = "480"
    @State private var gstChip = "Rest. 5%"
    @State private var hsn = "996331 · Restaurant service"
    @State private var variant = "Half / Full"
    @State private var stationRoute = "Tandoor"
    @State private var dishType = "Non-veg"
    @State private var modifiers = "Spice level · less oil · note"
    @State private var defaultLocation = "Cold store"
    @State private var parQty = ""
    @State private var preferredSupplier = "Fresh Farm"
    @State private var stationType = "Kitchen"
    @State private var printer = "Kitchen printer 1"
    @State private var displayOrder = "5"
    @State private var routeCats: Set<String> = ["Starters"]
    @State private var stationLead = "Chef Imran"
    @State private var tracked = "Track stock"
    @State private var saving = false
    @State private var showToast = false

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 12) {
                EmberGrabber()
                EmberSheetHeader(
                    title: isCreate ? kind.title : "Edit \(kind.masterKind.lowercased())",
                    subtitle: kind.subtitle,
                    onClose: { dismiss() }
                )
                HStack(spacing: 6) {
                    EmberModeChip(text: "Create", selected: isCreate) { isCreate = true; clear() }
                    EmberModeChip(text: "Edit existing", selected: !isCreate) { isCreate = false }
                }
                fields
                EmberWhyBox(text: kind.why)
                EmberStickyFooter(
                    title: {
                        if saving { return "Saving…" }
                        let trimmed = name.trimmingCharacters(in: .whitespacesAndNewlines)
                        return trimmed.isEmpty ? kind.cta : "\(kind.cta) · \(trimmed)"
                    }(),
                    caption: kind.caveat,
                    disabled: name.trimmingCharacters(in: .whitespaces).isEmpty || saving,
                    loading: saving,
                    action: { Task { await save() } }
                )
                if showToast {
                    Text(RestaurantCaptureCopy.savedToast)
                        .font(.system(size: 12, weight: .semibold))
                        .foregroundStyle(EmberSheetChrome.gold)
                }
            }
            .padding(20)
        }
        .background(EmberSheetChrome.sheet)
        .onAppear { hydrate(existing) }
    }

    @ViewBuilder
    private var fields: some View {
        switch kind {
        case .dish:
            EmberFieldLabel(text: "Dish name")
            EmberTextField(placeholder: "e.g. Butter chicken", text: $name)
            chipRow("Category", RestaurantCaptureCopy.menuCategories, $category)
            chipRow("Variants", ["Half / Full", "Single", "Size set"], $variant)
            priceCard(label: "Sell price", value: "₹\(sellPrice)", caption: "Half plate default · Full can differ")
            EmberFieldLabel(text: "Full price (optional)")
            EmberTextField(placeholder: "₹720", text: $fullPrice)
            chipRow("GST on dish", ["Rest. 5%", "18% + ITC", "Exempt"], $gstChip)
            EmberFieldLabel(text: "HSN / SAC")
            EmberTextField(placeholder: "996331", text: $hsn)
            HsnLookupHints(query: hsn, accent: theme.accent) { code, rate in
                hsn = code
                if let rate, !rate.isEmpty {
                    gstChip = rate == "0" ? "Exempt" : (rate == "18" ? "18% + ITC" : (rate == "5" ? "Rest. 5%" : "\(rate)%"))
                }
            }
            chipRow("Kitchen station", ["Tandoor", "Hot", "Cold", "Bar"], $stationRoute)
            chipRow("Type", ["Veg", "Non-veg", "Egg"], $dishType)
            EmberFieldLabel(text: "Modifiers allowed")
            EmberTextField(placeholder: "Spice level · less oil · note", text: $modifiers)
            chipRow("Status", ["Active", "Inactive"], $status)
        case .raw:
            EmberFieldLabel(text: "Material name")
            EmberTextField(placeholder: "e.g. Paneer", text: $name)
            chipRow("Category", RestaurantCaptureCopy.rawCategories, $category)
            chipRow("Unit of measure", ["kg", "L", "pcs", "bag", "can"], $unit)
            priceCard(label: "Purchase rate", value: "₹\(purchasePrice)", caption: "Default rate on PO lines · editable per order")
            chipRow("GST on purchase", ["0%", "5%", "12%", "18%"], $gstChip)
            EmberFieldLabel(text: "HSN code")
            EmberTextField(placeholder: "0406", text: $hsn)
            HsnLookupHints(query: hsn, accent: theme.accent) { code, rate in
                hsn = code
                if let rate, !rate.isEmpty {
                    gstChip = "\(rate)%"
                }
            }
            EmberFieldLabel(text: "Default location")
            EmberTextField(placeholder: "Cold store", text: $defaultLocation)
            EmberFieldLabel(text: "Par / reorder qty")
            EmberTextField(placeholder: "5 kg", text: $parQty)
            EmberFieldLabel(text: "Preferred supplier")
            EmberTextField(placeholder: "Fresh Farm", text: $preferredSupplier)
            chipRow("Track", ["Track stock", "Non-tracked"], $tracked)
            chipRow("Status", ["Active", "Inactive"], $status)
        case .vendor:
            EmberFieldLabel(text: "Vendor name")
            EmberTextField(placeholder: "e.g. Coastal Fish Co.", text: $name)
            EmberFieldLabel(text: "Legal / trade name")
            EmberTextField(placeholder: "As on GST certificate", text: $legalName)
            EmberFieldLabel(text: "GSTIN")
            EmberTextField(placeholder: "27AAACC1234D1Z7", text: $gstin)
            chipRow("GST registration", ["Regular", "Composition", "Unregistered"], $gstRegistration)
            EmberFieldLabel(text: "Phone")
            EmberTextField(placeholder: "+91 …", text: $phone)
            EmberFieldLabel(text: "Email (optional)")
            EmberTextField(placeholder: "orders@…", text: $email)
            chipRow("Category", RestaurantCaptureCopy.vendorCategories, $category)
            EmberFieldLabel(text: "Payment terms")
            EmberTextField(placeholder: "Net 7 days", text: $paymentTerms)
            EmberFieldLabel(text: "Address")
            EmberTextField(placeholder: "Address", text: $address)
            chipRow("Status", ["Active", "Preferred", "Inactive"], $status)
        case .station:
            EmberFieldLabel(text: "Station name")
            EmberTextField(placeholder: "e.g. Grill", text: $name)
            chipRow("Type", RestaurantCaptureCopy.stationTypes, $stationType)
            chipRow("KOT printer", RestaurantCaptureCopy.stationPrinters, $printer)
            Text("Printer routing is chrome until a kitchen-print endpoint lands.")
                .font(.system(size: 10))
                .foregroundStyle(EmberSheetChrome.amber)
            EmberFieldLabel(text: "Display order")
            EmberTextField(placeholder: "5", text: $displayOrder)
            EmberFieldLabel(text: "Routes dish categories")
            FlowLayout(spacing: 6) {
                ForEach(RestaurantCaptureCopy.menuCategories, id: \.self) { cat in
                    EmberModeChip(text: cat, selected: routeCats.contains(cat)) {
                        if routeCats.contains(cat) { routeCats.remove(cat) } else { routeCats.insert(cat) }
                    }
                }
            }
            EmberFieldLabel(text: "Station lead (optional)")
            EmberTextField(placeholder: "Chef Imran", text: $stationLead)
            chipRow("Status", ["Active", "Paused"], $status)
        }
    }

    private func chipRow(_ label: String, _ options: [String], _ selected: Binding<String>) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            EmberFieldLabel(text: label)
            FlowLayout(spacing: 6) {
                ForEach(options, id: \.self) { opt in
                    EmberModeChip(text: opt, selected: selected.wrappedValue == opt) {
                        selected.wrappedValue = opt
                    }
                }
            }
        }
    }

    private func priceCard(label: String, value: String, caption: String) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack {
                Text(label).font(.system(size: 12)).foregroundStyle(EmberSheetChrome.textBody)
                Spacer()
                Text("INR").font(.system(size: 11, weight: .bold)).foregroundStyle(EmberSheetChrome.gold)
            }
            Text(value).font(.system(size: 32, weight: .bold)).foregroundStyle(EmberSheetChrome.textPrimary)
            Text(caption).font(.system(size: 11)).foregroundStyle(EmberSheetChrome.textBody)
        }
        .padding(16)
        .background(EmberSheetChrome.chip)
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(EmberSheetChrome.ember, lineWidth: 1.5))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    private func hydrate(_ master: OperatingMasterPayload?) {
        guard let master else {
            clear()
            return
        }
        isCreate = false
        name = master.name
        gstin = master.gstin ?? gstin
        unit = master.unit ?? unit
        sellPrice = master.sellPrice ?? sellPrice
        purchasePrice = master.purchasePrice ?? purchasePrice
        hsn = master.hsn ?? hsn
        if let rate = master.gstRate {
            gstChip = kind == .dish ? (rate == "18" ? "18% + ITC" : rate == "0" ? "Exempt" : "Rest. 5%") : "\(rate)%"
        }
    }

    private func clear() {
        name = ""
        category = kind == .raw ? "Dairy" : (kind == .vendor ? "Meat & Fish" : "Mains")
        gstChip = kind == .raw ? "5%" : "Rest. 5%"
        hsn = kind == .raw ? "0406 · Dairy / cheese" : "996331 · Restaurant service"
        status = "Active"
    }

    private func save() async {
        saving = true
        defer { saving = false }
        let gstRate: String? = {
            switch kind {
            case .dish:
                switch gstChip {
                case "18% + ITC": return "18"
                case "Exempt": return "0"
                default: return "5"
                }
            case .raw: return gstChip.replacingOccurrences(of: "%", with: "")
            default: return nil
            }
        }()
        var draft = OperatingMasterDraft(kind: kind.masterKind)
        draft.name = name.trimmingCharacters(in: .whitespacesAndNewlines)
        draft.unit = unit
        if kind == .dish { draft.sellPrice = sellPrice }
        if kind == .raw { draft.purchasePrice = purchasePrice }
        if let gstRate { draft.gstRate = gstRate }
        if kind == .vendor { draft.gstin = gstin }
        draft.hsn = String(hsn.filter(\.isNumber).prefix(8))
        if await model.createMaster(draft) != nil {
            showToast = true
            onSaved()
            dismiss()
        }
    }
}

/// Minimal wrapping flow for chip rows when FlowLayout exists in project.
private struct FlowLayout<Content: View>: View {
    var spacing: CGFloat = 6
    @ViewBuilder var content: () -> Content
    var body: some View {
        // Prefer project FlowLayout if present; else wrap horizontally.
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: spacing, content: content)
        }
    }
}
