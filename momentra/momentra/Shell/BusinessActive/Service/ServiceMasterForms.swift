import SwiftUI

/// SVC master create/edit forms — Figma New Client / Room-Staff / Service / Package.
enum ServiceMasterFormKind: String, Identifiable, CaseIterable {
    case client
    case roomStaff
    case service
    case package

    var id: String { rawValue }

    var title: String {
        switch self {
        case .client: return "New client"
        case .roomStaff: return "Add room / staff"
        case .service: return "Create service offering"
        case .package: return "Create package"
        }
    }

    var editTitle: String {
        switch self {
        case .client: return "Edit client"
        case .roomStaff: return "Edit room / staff"
        case .service: return "Edit service offering"
        case .package: return "Edit package"
        }
    }

    var cta: String {
        switch self {
        case .client: return "Save client"
        case .roomStaff: return "Save room / staff"
        case .service: return "Save service"
        case .package: return "Save package"
        }
    }

    var subtitle: String {
        switch self {
        case .client: return "Client master · GSTIN optional (B2B invoice only)"
        case .roomStaff: return "Capacity · calendar colour · categories"
        case .service: return "Offering · SAC, GST, duration, price, default room"
        case .package: return "Sessions · package price · includes · validity"
        }
    }

    var why: String {
        switch self {
        case .client:
            return "Client is a master write. Place of supply decides CGST+SGST vs IGST on Finalize Charge. Booking save is commitment only."
        case .roomStaff:
            return "Rooms and staff are capacity. Booking holds a slot; only Finalize Charge creates revenue."
        case .service:
            return "Create Service is a master write. Booking and Charge only pick saved services; GST posts on Finalize Charge, not on save."
        case .package:
            return "Create Package is a master write. Sessions are redeemed per booking; revenue and GST post per Finalize Charge."
        }
    }

    var defaultMasterKind: String {
        switch self {
        case .client: return "CUSTOMER"
        case .roomStaff: return "LOCATION"
        case .service: return "PRODUCT"
        case .package: return "PACKAGE"
        }
    }

    var toastCaption: String {
        switch self {
        case .client: return "Client saved · available in Booking"
        case .roomStaff: return "Saved · calendar slots ready"
        case .service, .package: return ServiceCaptureCopy.savedToast
        }
    }
}

private let serviceFormCategories = ["Hair", "Skin", "Spa", "Wellness", "Nails"]
private let clientInterests = ["Facial", "Hair", "Spa", "Nails", "Wellness", "Massage"]
private let packageIncludes = ["Keratin", "Hair spa", "Trim", "Wash"]

struct ServiceMasterFormSheet: View {
    let theme: OperatingTheme
    let kind: ServiceMasterFormKind
    @ObservedObject var model: BusinessOperatingModel
    var existing: OperatingMasterPayload? = nil
    var onSaved: () -> Void = {}

    @Environment(\.dismiss) private var dismiss
    @State private var draft: OperatingMasterDraft
    @State private var isCreate = true
    @State private var saving = false
    @State private var editingId: String?
    @State private var editingVersion = 1
    @State private var showToast = false

    // Local Figma fields (persisted via available draft columns / code / unit)
    @State private var legalName = ""
    @State private var phone = ""
    @State private var email = ""
    @State private var gstRegistration = "Regular"
    @State private var interests: Set<String> = ["Nails"]
    @State private var preferredStaff = ""
    @State private var address = ""
    @State private var status = "Active"
    @State private var capacityType = "Chair"
    @State private var calendarColour = "Teal"
    @State private var categories: Set<String> = ["Hair"]
    @State private var duration = "45"
    @State private var defaultRoom = "Chair 1"
    @State private var bufferMins = "10"
    @State private var sessions = "3"
    @State private var validityDays = "90"
    @State private var includes: Set<String> = ["Keratin"]

    init(
        theme: OperatingTheme,
        kind: ServiceMasterFormKind,
        model: BusinessOperatingModel,
        existing: OperatingMasterPayload? = nil,
        onSaved: @escaping () -> Void = {}
    ) {
        self.theme = theme
        self.kind = kind
        self.model = model
        self.existing = existing
        self.onSaved = onSaved
        _draft = State(initialValue: OperatingMasterDraft(kind: kind.defaultMasterKind, name: existing?.name ?? ""))
        _isCreate = State(initialValue: existing == nil)
        if let existing {
            _editingId = State(initialValue: existing.id)
            _editingVersion = State(initialValue: existing.version ?? 1)
        }
    }

    var body: some View {
        ZStack(alignment: .top) {
            ScrollView {
                VStack(alignment: .leading, spacing: 12) {
                    TealGrabber()
                    TealSheetHeader(
                        theme: theme,
                        title: isCreate ? kind.title : kind.editTitle,
                        subtitle: kind.subtitle,
                        onClose: { dismiss() }
                    )

                    TealCreateEditMode(theme: theme, isCreate: $isCreate) {
                        // Edit mode — picker below when peers exist
                    }
                    .onChange(of: isCreate) { _, create in
                        if create { clearForCreate() }
                    }

                    if !isCreate {
                        TealSectionCard(theme: theme, label: "Edit existing") {
                            let peers = peerMasters
                            if peers.isEmpty {
                                TealEmptyCard(
                                    theme: theme,
                                    title: "No masters yet",
                                    bodyText: "Create one first, then edit from here."
                                )
                            } else {
                                ForEach(peers.prefix(8)) { master in
                                    TealPickerRow(theme: theme, label: master.kind, value: master.name) {
                                        hydrate(master)
                                    }
                                }
                            }
                        }
                    }

                    fields

                    TealWhyBox(theme: theme, text: kind.why)
                    TealStickyFooter(
                        theme: theme,
                        title: saving ? "Saving…" : kind.cta,
                        caption: kind.toastCaption,
                        disabled: !draft.canSave || (!isCreate && editingId == nil),
                        loading: saving,
                        action: { Task { await save() } }
                    )
                }
                .padding(16)
                .padding(.bottom, 28)
            }
            .background(theme.canvas)

            if showToast {
                TealSavedToast().padding(.top, 12)
            }
        }
        .task {
            if let existing { hydrate(existing) }
            if model.masters.isEmpty { await model.loadMasters() }
        }
    }

    @ViewBuilder
    private var fields: some View {
        switch kind {
        case .client: clientFields
        case .roomStaff: roomStaffFields
        case .service: serviceFields
        case .package: packageFields
        }
    }

    private var clientFields: some View {
        Group {
            TealFieldLabel(text: "Client name")
            TealTextField(theme: theme, placeholder: "e.g. Ananya Mehta", text: $draft.name)
            TealFieldLabel(text: "Legal / trade name")
            TealTextField(theme: theme, placeholder: "As on GST certificate (optional)", text: $legalName)
            TealFieldLabel(text: "GSTIN")
            TealTextField(theme: theme, placeholder: "27AAACC1234D1Z7", text: $draft.gstin)
            TealFieldLabel(text: "GST registration")
            TealChipRow(theme: theme, options: ["Regular", "Composition", "Unregistered"], selection: $gstRegistration)
            TealFieldLabel(text: "Phone")
            TealTextField(theme: theme, placeholder: "+91 98200 12345", text: $phone, keyboard: .phonePad)
            TealFieldLabel(text: "Email")
            TealTextField(theme: theme, placeholder: "client@example.in", text: $email, keyboard: .emailAddress)
            TealFieldLabel(text: "Interests")
            TealMultiChipRow(theme: theme, options: clientInterests, selection: $interests, style: .lime)
            TealFieldLabel(text: "Preferred staff")
            TealTextField(theme: theme, placeholder: "Priya", text: $preferredStaff)
            TealFieldLabel(text: "Address")
            TealTextField(theme: theme, placeholder: "Bandra West, Mumbai", text: $address)
            TealFieldLabel(text: "Status")
            TealChipRow(theme: theme, options: ["Active", "VIP", "Inactive"], selection: $status)
        }
    }

    private var roomStaffFields: some View {
        Group {
            TealFieldLabel(text: "Name")
            TealTextField(theme: theme, placeholder: "Chair 2 / Priya", text: $draft.name)
            TealFieldLabel(text: "Type")
            TealChipRow(theme: theme, options: ["Chair", "Room", "Staff"], selection: $capacityType)
            TealFieldLabel(text: "Calendar colour")
            TealChipRow(theme: theme, options: ["Teal", "Lime", "Amber", "Rose"], selection: $calendarColour)
            TealFieldLabel(text: "Categories")
            TealMultiChipRow(theme: theme, options: serviceFormCategories, selection: $categories, style: .lime)
        }
    }

    private var serviceFields: some View {
        Group {
            TealFieldLabel(text: "Service name")
            TealTextField(theme: theme, placeholder: "Signature facial", text: $draft.name)
            TealFieldLabel(text: "Category")
            TealChipRow(
                theme: theme,
                options: serviceFormCategories,
                selection: Binding(
                    get: { categories.first ?? "Hair" },
                    set: { categories = [$0] }
                ),
                style: .lime
            )
            TealFieldLabel(text: "SAC")
            TealTextField(theme: theme, placeholder: "999721", text: $draft.hsn, keyboard: .numberPad)
            TealFieldLabel(text: "GST %")
            TealTextField(theme: theme, placeholder: "18", text: $draft.gstRate, keyboard: .decimalPad)
            TealFieldLabel(text: "Duration (min)")
            TealTextField(theme: theme, placeholder: "45", text: $duration, keyboard: .numberPad)
            TealFieldLabel(text: "Price")
            TealTextField(theme: theme, placeholder: "4500", text: $draft.sellPrice, keyboard: .decimalPad)
            TealFieldLabel(text: "Default room")
            TealTextField(theme: theme, placeholder: "Chair 1", text: $defaultRoom)
            TealFieldLabel(text: "Buffer (min)")
            TealTextField(theme: theme, placeholder: "10", text: $bufferMins, keyboard: .numberPad)
        }
    }

    private var packageFields: some View {
        Group {
            TealFieldLabel(text: "Package name")
            TealTextField(theme: theme, placeholder: "Keratin package", text: $draft.name)
            TealFieldLabel(text: "Sessions")
            TealTextField(theme: theme, placeholder: "3", text: $sessions, keyboard: .numberPad)
            TealFieldLabel(text: "Package price")
            TealTextField(theme: theme, placeholder: "9999", text: $draft.sellPrice, keyboard: .decimalPad)
            TealFieldLabel(text: "Includes")
            TealMultiChipRow(theme: theme, options: packageIncludes, selection: $includes, style: .lime)
            TealFieldLabel(text: "Validity (days)")
            TealTextField(theme: theme, placeholder: "90", text: $validityDays, keyboard: .numberPad)
            TealFieldLabel(text: "SAC")
            TealTextField(theme: theme, placeholder: "999721", text: $draft.hsn, keyboard: .numberPad)
            TealFieldLabel(text: "GST %")
            TealTextField(theme: theme, placeholder: "18", text: $draft.gstRate, keyboard: .decimalPad)
        }
    }

    private var peerMasters: [OperatingMasterPayload] {
        let source = model.masters.isEmpty ? (model.projection?.masters ?? []) : model.masters
        let kinds: Set<String>
        switch kind {
        case .client: kinds = ["CUSTOMER", "CLIENT", "GUEST"]
        case .roomStaff: kinds = ["LOCATION", "STAFF", "ROOM", "CHAIR", "CHANNEL"]
        case .service: kinds = ["PRODUCT", "SERVICE", "ITEM"]
        case .package: kinds = ["PACKAGE"]
        }
        return source
            .filter { kinds.contains($0.kind.uppercased()) }
            .sorted { $0.name.localizedCaseInsensitiveCompare($1.name) == .orderedAscending }
    }

    private func clearForCreate() {
        editingId = nil
        editingVersion = 1
        draft = OperatingMasterDraft(kind: mappedKind(), name: "")
        legalName = ""
        phone = ""
        email = ""
        gstRegistration = "Regular"
        interests = ["Nails"]
        preferredStaff = ""
        address = ""
        status = "Active"
        capacityType = "Chair"
        calendarColour = "Teal"
        categories = ["Hair"]
        duration = "45"
        defaultRoom = "Chair 1"
        bufferMins = "10"
        sessions = "3"
        validityDays = "90"
        includes = ["Keratin"]
    }

    private func hydrate(_ master: OperatingMasterPayload) {
        editingId = master.id
        editingVersion = master.version ?? 1
        isCreate = false
        draft.kind = master.kind
        draft.name = master.name
        draft.code = master.code ?? ""
        draft.gstin = master.gstin ?? ""
        draft.hsn = master.hsn ?? ""
        draft.gstRate = master.gstRate ?? ""
        draft.sellPrice = master.sellPrice ?? ""
        draft.unit = master.unit ?? ""
        draft.billingState = master.billingState ?? ""
        if kind == .client {
            legalName = master.code ?? ""
            status = (master.code ?? "").uppercased().contains("VIP") ? "VIP" : "Active"
            interests = Set((master.dimension ?? "")
                .split(separator: ",")
                .map { $0.trimmingCharacters(in: .whitespaces) }
                .filter { !$0.isEmpty })
            if interests.isEmpty { interests = ["Nails"] }
        }
        if kind == .roomStaff {
            let upper = master.kind.uppercased()
            capacityType = upper == "STAFF" ? "Staff" : upper == "ROOM" ? "Room" : "Chair"
        }
        if kind == .service || kind == .package {
            duration = master.unit ?? duration
        }
    }

    private func mappedKind() -> String {
        switch kind {
        case .client: return "CUSTOMER"
        case .roomStaff:
            switch capacityType {
            case "Staff": return "STAFF"
            case "Room": return "ROOM"
            default: return "LOCATION"
            }
        case .service: return "PRODUCT"
        case .package: return "PACKAGE"
        }
    }

    private func prepareDraft() {
        draft.kind = mappedKind()
        draft.momentId = model.momentId.isEmpty ? nil : model.momentId
        switch kind {
        case .client:
            draft.code = status == "VIP" ? "VIP" : legalName
            draft.unit = interests.sorted().joined(separator: ",")
        case .roomStaff:
            draft.code = calendarColour
            draft.unit = categories.sorted().joined(separator: ",")
        case .service:
            draft.unit = "\(duration) min"
            draft.code = defaultRoom
            draft.conversionToBase = bufferMins
        case .package:
            draft.unit = "\(sessions) sessions / \(validityDays)d"
            draft.code = includes.sorted().joined(separator: ",")
        }
    }

    private func save() async {
        prepareDraft()
        guard draft.canSave else { return }
        saving = true
        defer { saving = false }
        let ok: Bool
        if isCreate || editingId == nil {
            let created = await model.createMaster(draft)
            ok = created != nil && model.writeError == nil
        } else if let id = editingId {
            let updated = await model.updateMaster(id: id, draft: draft, expectedVersion: editingVersion)
            ok = updated != nil && model.writeError == nil
        } else {
            ok = false
        }
        guard ok else { return }
        withAnimation { showToast = true }
        try? await Task.sleep(nanoseconds: 900_000_000)
        onSaved()
        dismiss()
    }
}
