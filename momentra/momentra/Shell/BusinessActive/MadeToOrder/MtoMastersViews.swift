import Photos
import SwiftUI
import UIKit

enum MtoMasterListTab: String, Identifiable {
    case invite, vendors, productsBom, linesStores
    var id: String { rawValue }
}

/// Forge roles mapped onto backend operatingRole codes — Figma `2545:29335`.
enum MtoInviteRole: String, CaseIterable, Identifiable {
    case owner, manager, jobLead, estimator, operator
    var id: String { rawValue }
    var label: String {
        switch self {
        case .owner: return "Owner"
        case .manager: return "Manager"
        case .jobLead: return "Job lead"
        case .estimator: return "Estimator"
        case .operator: return "Operator"
        }
    }
    var apiCode: String {
        switch self {
        case .owner: return "FINANCE"
        case .manager: return "APPROVER"
        case .jobLead: return "OPERATIONS_LEAD"
        case .estimator, .operator: return "CONTRIBUTOR"
        }
    }
    var accessCopy: String {
        switch self {
        case .owner:
            return "Access: Owner → full access · quotes, invoices and invites. Join ≠ quote approval ≠ payment."
        case .manager:
            return "Access: Manager → confirm orders and job approvals · no payouts. Join ≠ quote approval ≠ payment."
        case .jobLead:
            return "Access: Job lead → open jobs, production & dispatch · no payouts or pricing. Join ≠ quote approval ≠ payment."
        case .estimator, .operator:
            return "Access: Estimator / Operator → quotes, production and job activity · no reports or payouts. Join ≠ quote approval ≠ payment."
        }
    }
}

struct MtoMastersSheet: View {
    let theme: OperatingTheme
    let tab: MtoMasterListTab
    @ObservedObject var model: BusinessOperatingModel
    var onOpenForm: (MtoMasterFormKind) -> Void = { _ in }
    var onBrowseParts: (() -> Void)? = nil

    @Environment(\.dismiss) private var dismiss
    @State private var query = ""
    @State private var cat = "Finished"
    @State private var form: MtoMasterFormKind?

    // Invite state
    @State private var inviteRole: MtoInviteRole = .jobLead
    @State private var invite: CompanyInvite?
    @State private var minting = false
    @State private var mintError: String?
    @State private var contactQuery = ""
    @State private var contacts: [MtoInviteContact] = []
    @State private var permissionDenied = false
    @State private var selectedContactId: String?
    @State private var copied = false
    @State private var saved = false
    @State private var showShare = false
    @State private var shareItems: [Any] = []

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 12) {
                MtoGrabber()
                MtoSheetHeader(theme: theme, title: title, subtitle: subtitle, onClose: { dismiss() })
                content
            }
            .padding(16)
            .padding(.bottom, 24)
        }
        .background(theme.canvas)
        .sheet(item: $form) { kind in
            MtoMasterFormSheet(theme: theme, kind: kind, model: model)
        }
        .sheet(isPresented: $showShare) {
            ActivityView(activityItems: shareItems)
        }
        .task {
            if model.masters.isEmpty { await model.loadMasters() }
            if tab == .invite {
                await loadContacts()
                await mintInvite()
            }
        }
        .onChange(of: inviteRole) { _, _ in
            Task { await mintInvite() }
        }
    }

    private var title: String {
        switch tab {
        case .invite: return "Invite team · \(model.title.isEmpty ? "Forge & Form" : model.title)"
        case .vendors: return "Customers & suppliers"
        case .productsBom: return "Products & BOM"
        case .linesStores: return "Lines & stores"
        }
    }

    private var subtitle: String? {
        switch tab {
        case .invite: return "\(model.title.isEmpty ? "Forge & Form" : model.title) · invite grants a role"
        case .vendors: return "Customer · Supplier · Job-work"
        case .productsBom: return "Products & BOM · raw, WIP and finished parts with HSN + GST before SO / WO"
        case .linesStores: return "Line · Station · Store · Active/Planned"
        }
    }

    private var filteredParts: [OperatingMasterPayload] {
        parts.filter { mtoPartCategory($0) == cat }
    }

    @ViewBuilder
    private var productsBomBody: some View {
        MtoChipRow(theme: theme, options: ["Raw", "WIP", "Finished", "Consumables", "Spares"], selection: $cat)
        ForEach(filteredParts.prefix(20), id: \.id) { p in
            let category = mtoPartCategory(p)
            let meta: String = {
                if p.kind.uppercased() == "BOM" { return "\(category) · BOM" }
                if let unit = p.unit, !unit.isEmpty { return "\(category) · \(unit)" }
                return category
            }()
            let price: String = {
                guard let sell = p.sellPrice, !sell.isEmpty else { return "—" }
                return "₹\(sell)"
            }()
            MtoCatalogPartRow(
                theme: theme,
                name: p.name,
                meta: meta,
                priceLabel: price,
                gstBadge: mtoGstBadge(p) ?? "GST · —",
                editLocked: true,
                accentDot: category == "Finished" || category == "Raw"
            )
        }
        if filteredParts.isEmpty {
            Text(parts.isEmpty ? "No parts yet" : "No \(cat) parts")
                .font(.plusJakarta(size: 13))
                .foregroundStyle(MtoSheetChrome.textBody)
        }
        MtoDenseCreateCta(theme: theme, title: "+ New part / SKU", hint: "Sell price · HSN · GST chip") {
            form = .part
            onOpenForm(.part)
        }
        MtoDenseCreateCta(theme: theme, title: "+ New BOM", hint: "Routing · scrap % · backflush") {
            form = .bom
            onOpenForm(.bom)
        }
        if let onBrowseParts {
            MtoDenseCreateCta(theme: theme, title: "Browse parts · SO / WO", hint: "Pick into order draft", action: onBrowseParts)
        }
        MtoWhyNote(
            theme: theme,
            text: "Lists saved parts and BOMs. GST badge is the HSN rate — output GST posts on Sale invoice, never on SO or job save. Edit locked without hydrate (D10)."
        )
        MtoStickyFooter(
            theme: theme,
            title: "Done",
            caption: "\(filteredParts.count) of \(parts.count) parts · \(cat)",
            action: { dismiss() }
        )
    }

    @ViewBuilder
    private var content: some View {
        switch tab {
        case .invite:
            inviteBody
        case .vendors:
            MtoTextField(theme: theme, placeholder: "Search parties", text: $query)
            ForEach(vendors, id: \.id) { v in
                let badge = mtoVendorBadge(v)
                let gstin = (v.gstin ?? "").trimmingCharacters(in: .whitespacesAndNewlines)
                let gstinMeta = gstin.isEmpty ? nil : "GSTIN \(gstin)"
                // D9: Edit locked without hydrate — Create only via + New.
                MtoPickerRow(
                    theme: theme,
                    label: v.name,
                    value: [badge, gstinMeta].compactMap { $0 }.joined(separator: " · ")
                )
            }
            if vendors.isEmpty {
                Text("No parties yet")
                    .font(.plusJakarta(size: 13))
                    .foregroundStyle(MtoSheetChrome.textBody)
            }
            MtoDenseCreateCta(theme: theme, title: "+ New customer / supplier", hint: "Role · GSTIN · terms") {
                form = .vendor
                onOpenForm(.vendor)
            }
            MtoLockedChip(theme: theme, label: "Edit existing · Unavailable until hydrate")
            MtoWhyNote(theme: theme, text: "Party list is identity only. GSTIN never posts AP or GST — invoices do.")
            MtoStickyFooter(
                theme: theme,
                title: "Done",
                caption: "\(vendors.count) parties",
                action: { dismiss() }
            )
        case .productsBom:
            productsBomBody
        case .linesStores:
            ForEach(locations, id: \.id) { loc in
                let capacity = mtoLineCapacity(loc)
                let typeLabel: String = {
                    switch loc.kind.uppercased() {
                    case "LINE": return "Line"
                    case "STATION": return "Station"
                    case "STORE": return "Store"
                    default: return loc.kind.capitalized
                    }
                }()
                // D9: Edit locked without hydrate.
                MtoPickerRow(theme: theme, label: loc.name, value: "\(typeLabel) · \(capacity)")
            }
            MtoDenseCreateCta(theme: theme, title: "+ Add line / store", hint: "Type · printer · holds stock") {
                form = .lineStore
                onOpenForm(.lineStore)
            }
            MtoLockedChip(theme: theme, label: "Edit existing · Unavailable until hydrate")
            MtoWhyNote(theme: theme, text: "Lines and stores hold capacity only. Active/Planned never invents stock or WIP balances.")
            MtoStickyFooter(
                theme: theme,
                title: "Done",
                caption: "\(locations.count) lines & stores",
                action: { dismiss() }
            )
        }
    }

    // MARK: - Invite (Figma 2545:29335)

    private var inviteBody: some View {
        VStack(alignment: .leading, spacing: 14) {
            MtoFieldLabel(text: "Search")
            MtoTextField(theme: theme, placeholder: "Search contacts, email or phone", text: $contactQuery)

            Text("FROM YOUR CONTACTS")
                .font(.plusJakarta(size: 11, weight: .bold))
                .foregroundStyle(theme.accentSoft)

            if permissionDenied {
                Text("Contacts access is off. Share the invite link instead — it works the same.")
                    .font(.plusJakarta(size: 12))
                    .foregroundStyle(MtoSheetChrome.textBody)
            } else if filteredContacts.isEmpty {
                Text(contacts.isEmpty ? "No contacts on this device. Share the link or the QR below." : "No contact matches that search.")
                    .font(.plusJakarta(size: 12))
                    .foregroundStyle(MtoSheetChrome.textBody)
            } else {
                ForEach(filteredContacts, id: \.id) { person in
                    contactRow(person)
                }
            }

            Text("OR SHARE INVITE LINK")
                .font(.plusJakarta(size: 11, weight: .bold))
                .foregroundStyle(theme.accentSoft)
            inviteLinkBlock

            Text("OR SCAN TO JOIN")
                .font(.plusJakarta(size: 11, weight: .bold))
                .foregroundStyle(theme.accentSoft)
            qrBlock

            if let mintError, !mintError.isEmpty {
                Text(mintError)
                    .font(.plusJakarta(size: 12))
                    .foregroundStyle(theme.negative)
            }

            MtoWhyNote(theme: theme, text: "Invite grants a role and its access. Joining ≠ approval ≠ payment.")
            MtoStickyFooter(
                theme: theme,
                title: "Done",
                caption: "Pending invites · roles apply on join",
                action: { dismiss() }
            )
        }
    }

    private func contactRow(_ person: MtoInviteContact) -> some View {
        let selected = selectedContactId == person.id
        return VStack(alignment: .leading, spacing: 8) {
            HStack(spacing: 12) {
                ZStack {
                    Circle().fill(theme.inset)
                    Text(initials(person.name))
                        .font(.plusJakarta(size: 12, weight: .bold))
                        .foregroundStyle(theme.accentSoft)
                }
                .frame(width: 36, height: 36)
                VStack(alignment: .leading, spacing: 2) {
                    Text(person.name.isEmpty ? "Unnamed contact" : person.name)
                        .font(.plusJakarta(size: 14, weight: .semibold))
                        .foregroundStyle(OperatingV12.title)
                    Text(person.subtitle)
                        .font(.plusJakarta(size: 11, weight: .medium))
                        .foregroundStyle(MtoSheetChrome.textBody)
                }
                Spacer()
                if selected {
                    Text("Added · \(inviteRole.label)")
                        .font(.plusJakarta(size: 10, weight: .bold))
                        .foregroundStyle(Color(hex: "#18181B"))
                        .padding(.horizontal, 8)
                        .padding(.vertical, 4)
                        .background(theme.accentSoft, in: Capsule())
                } else {
                    Button {
                        selectedContactId = person.id
                    } label: {
                        Text("+ Add")
                            .font(.plusJakarta(size: 12, weight: .bold))
                            .foregroundStyle(Color(hex: "#18181B"))
                            .padding(.horizontal, 12)
                            .padding(.vertical, 8)
                            .background(theme.accent, in: RoundedRectangle(cornerRadius: 10, style: .continuous))
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(12)
            .background(theme.inset)
            .overlay(
                RoundedRectangle(cornerRadius: 14, style: .continuous)
                    .strokeBorder(selected ? theme.accent : theme.border, lineWidth: 1)
            )
            .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))

            if selected {
                MtoFieldLabel(text: "Role for \(person.name)")
                MtoChipRow(
                    theme: theme,
                    options: MtoInviteRole.allCases.map(\.label),
                    selection: Binding(
                        get: { inviteRole.label },
                        set: { label in
                            if let next = MtoInviteRole.allCases.first(where: { $0.label == label }) {
                                inviteRole = next
                            }
                        }
                    )
                )
                Text(inviteRole.accessCopy)
                    .font(.plusJakarta(size: 11, weight: .medium))
                    .foregroundStyle(theme.accentSoft)
            }
        }
    }

    private var inviteLinkBlock: some View {
        Group {
            if minting && invite == nil {
                ProgressView().tint(theme.accent)
            } else if let path = invitePath {
                HStack(spacing: 8) {
                    Text(path)
                        .font(.plusJakarta(size: 13, weight: .semibold))
                        .foregroundStyle(theme.accentSoft)
                        .lineLimit(1)
                    Spacer()
                    Button {
                        UIPasteboard.general.string = path
                        copied = true
                    } label: {
                        Text(copied ? "Copied" : "Copy")
                            .font(.plusJakarta(size: 12, weight: .bold))
                            .foregroundStyle(Color(hex: "#18181B"))
                            .padding(.horizontal, 12)
                            .padding(.vertical, 8)
                            .background(theme.accent, in: RoundedRectangle(cornerRadius: 10, style: .continuous))
                    }
                    .buttonStyle(.plain)
                }
                .padding(12)
                .background(theme.inset)
                .overlay(
                    RoundedRectangle(cornerRadius: 14, style: .continuous)
                        .strokeBorder(theme.border, lineWidth: 1)
                )
                .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
            } else if model.companyId == nil {
                Text("This plant is not linked to a company yet, so there is nothing to invite into.")
                    .font(.plusJakarta(size: 12))
                    .foregroundStyle(MtoSheetChrome.textBody)
            } else {
                Text("Invite token Unavailable — owner must mint a real join code. No fake QR.")
                    .font(.plusJakarta(size: 12))
                    .foregroundStyle(MtoSheetChrome.textBody)
            }
        }
    }

    private var qrBlock: some View {
        VStack(alignment: .leading, spacing: 12) {
            if let image = qrImage {
                Image(uiImage: image)
                    .interpolation(.none)
                    .resizable()
                    .scaledToFit()
                    .frame(width: 168, height: 168)
                    .background(Color.white)
                    .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
            } else {
                RoundedRectangle(cornerRadius: 16, style: .continuous)
                    .fill(Color.white)
                    .frame(width: 168, height: 168)
                    .overlay(
                        Text(minting ? "Minting…" : "QR Unavailable")
                            .font(.plusJakarta(size: 13, weight: .bold))
                            .foregroundStyle(theme.accent)
                    )
            }
            Text("\(model.title.isEmpty ? "Forge & Form" : model.title) · joins as \(inviteRole.label) by default")
                .font(.plusJakarta(size: 11, weight: .medium))
                .foregroundStyle(MtoSheetChrome.textBody)
            HStack(spacing: 8) {
                outlineBtn("Share QR", enabled: qrPayload != nil) {
                    guard let path = invitePath, let image = qrImage else { return }
                    shareItems = [image, path]
                    showShare = true
                }
                outlineBtn(saved ? "Saved" : "Save to Photos", enabled: qrImage != nil) {
                    saveQR()
                }
            }
        }
        .padding(12)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(theme.inset)
        .overlay(
            RoundedRectangle(cornerRadius: 14, style: .continuous)
                .strokeBorder(theme.accent, lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
        .shadow(color: theme.accent.opacity(0.25), radius: 12, y: 0)
    }

    private func outlineBtn(_ title: String, enabled: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(title)
                .font(.plusJakarta(size: 12, weight: .bold))
                .foregroundStyle(OperatingV12.title)
                .padding(.horizontal, 12)
                .padding(.vertical, 8)
                .background(MtoSheetChrome.sheet)
                .overlay(
                    RoundedRectangle(cornerRadius: 10, style: .continuous)
                        .strokeBorder(enabled ? theme.accent : theme.border, lineWidth: 1)
                )
                .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
        }
        .buttonStyle(.plain)
        .disabled(!enabled)
        .opacity(enabled ? 1 : 0.45)
    }

    private var vendors: [OperatingMasterPayload] {
        let source = model.masters.isEmpty ? (model.projection?.masters ?? []) : model.masters
        return source.filter {
            ["SUPPLIER", "VENDOR", "BUYER", "CUSTOMER", "PARTY"].contains($0.kind.uppercased())
                && (query.isEmpty || $0.name.localizedCaseInsensitiveContains(query))
        }
    }

    private var parts: [OperatingMasterPayload] {
        let source = model.masters.isEmpty ? (model.projection?.masters ?? []) : model.masters
        return source.filter { ["SKU", "PART", "PRODUCT", "BOM"].contains($0.kind.uppercased()) }
    }

    private var locations: [OperatingMasterPayload] {
        let source = model.masters.isEmpty ? (model.projection?.masters ?? []) : model.masters
        return source.filter { ["LOCATION", "STORE", "LINE", "STATION"].contains($0.kind.uppercased()) }
    }

    private var filteredContacts: [MtoInviteContact] {
        let q = contactQuery.trimmingCharacters(in: .whitespacesAndNewlines)
        let source = contacts.filter {
            q.isEmpty
                || $0.name.localizedCaseInsensitiveContains(q)
                || $0.subtitle.localizedCaseInsensitiveContains(q)
        }
        return q.isEmpty ? Array(source.prefix(6)) : Array(source.prefix(20))
    }

    private var inviteCode: String? {
        guard let invite, (invite.operatingRole ?? "") == inviteRole.apiCode else { return nil }
        let code = invite.inviteCode.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        return code.isEmpty ? nil : code
    }

    private var invitePath: String? {
        guard let invite, let code = inviteCode else { return nil }
        if !invite.invitePath.isEmpty { return invite.invitePath }
        if !invite.inviteUrl.isEmpty { return invite.inviteUrl }
        return CompanyJoinLink.displayPath(code: code)
    }

    private var qrPayload: String? {
        inviteCode.map { CompanyJoinLink.qrPayload(code: $0) }
    }

    private var qrImage: UIImage? {
        guard let payload = qrPayload else { return nil }
        return GroupQRCode.image(from: payload, size: 168)
    }

    private func initials(_ name: String) -> String {
        let parts = name.split(separator: " ").prefix(2)
        let chars = parts.compactMap { $0.first.map(String.init) }
        return chars.joined().uppercased().ifEmpty("?")
    }

    private func loadContacts() async {
        do {
            let rows = try await GroupContactsLoader.fetchRowsIfAuthorized()
            await MainActor.run {
                permissionDenied = false
                contacts = rows.map {
                    MtoInviteContact(id: $0.id, name: $0.name, subtitle: $0.subtitle, phone: $0.phone, email: $0.email)
                }
            }
        } catch {
            await MainActor.run { permissionDenied = true }
        }
    }

    private func mintInvite() async {
        guard let companyId = model.companyId, !companyId.isEmpty else {
            mintError = "Plant is not linked to a company."
            return
        }
        minting = true
        mintError = nil
        defer { minting = false }
        do {
            let minted = try await APIClient.shared.mintCompanyInvite(
                companyId: companyId,
                membershipType: inviteRole == .owner || inviteRole == .manager ? "ADMIN" : "MEMBER",
                operatingRole: inviteRole.apiCode
            )
            invite = minted
            copied = false
            saved = false
        } catch {
            if invite == nil {
                mintError = error.localizedDescription
            }
        }
    }

    private func saveQR() {
        guard let image = qrImage else { return }
        PHPhotoLibrary.requestAuthorization(for: .addOnly) { status in
            guard status == .authorized || status == .limited else { return }
            UIImageWriteToSavedPhotosAlbum(image, nil, nil, nil)
            DispatchQueue.main.async { saved = true }
        }
    }
}

/// Figma Customers & suppliers: Customer / Supplier / Job-work (+ status when known).
func mtoVendorBadge(_ v: OperatingMasterPayload) -> String {
    let kind = v.kind.uppercased()
    let status = (v.status ?? "").uppercased()
    let note = [v.name, v.code ?? "", v.status ?? ""].joined(separator: " ").uppercased()
    let role: String
    if note.contains("JOB WORK") || note.contains("JOB-WORK") || note.contains("JOBWORK") || status.contains("JOB") {
        role = "Job-work"
    } else if kind.contains("CUSTOMER") || kind.contains("BUYER") || kind.contains("PARTY") {
        role = "Customer"
    } else {
        role = "Supplier"
    }
    let gstin = (v.gstin ?? "").trimmingCharacters(in: .whitespacesAndNewlines)
    let statusBadge: String
    if status.contains("PREFERRED") {
        statusBadge = "Preferred"
    } else if status.contains("INACTIVE") || status.contains("ARCHIVED") {
        statusBadge = "Inactive"
    } else if status.contains("ACTIVE") || status.contains("APPROVED") || !gstin.isEmpty {
        statusBadge = "Active"
    } else if !status.isEmpty {
        statusBadge = status.capitalized
    } else {
        statusBadge = "Unavailable"
    }
    return "\(role) · \(statusBadge)"
}

/// Figma Lines & stores: Active / Planned; Busy when capacity known; else Unavailable.
func mtoLineCapacity(_ loc: OperatingMasterPayload) -> String {
    let status = (loc.status ?? "").uppercased()
    if status.contains("PLANNED") || status.contains("PENDING") || status.contains("DRAFT") { return "Planned" }
    if status.contains("BUSY") || status.contains("PAUSED") || status.contains("HELD") { return "Busy" }
    if status.contains("OPEN") || status.contains("ACTIVE") || status.contains("IDLE") { return "Active" }
    if !status.isEmpty { return status.capitalized }
    return "Unavailable"
}

func mtoGstBadge(_ p: OperatingMasterPayload) -> String? {
    guard let rate = p.gstRate?.trimmingCharacters(in: .whitespacesAndNewlines), !rate.isEmpty else { return nil }
    let trimmed = rate.hasSuffix("%") ? String(rate.dropLast()) : rate
    return "GST \(trimmed)%"
}

private struct MtoInviteContact: Identifiable {
    let id: String
    let name: String
    let subtitle: String
    let phone: String
    let email: String
}

private extension String {
    func ifEmpty(_ fallback: String) -> String { isEmpty ? fallback : self }
}

/// Thin UIKit share sheet wrapper used by MTO Invite QR Share.
private struct ActivityView: UIViewControllerRepresentable {
    let activityItems: [Any]
    func makeUIViewController(context: Context) -> UIActivityViewController {
        UIActivityViewController(activityItems: activityItems, applicationActivities: nil)
    }
    func updateUIViewController(_ uiViewController: UIActivityViewController, context: Context) {}
}
