import Photos
import SwiftUI
import UIKit

/// Active-moment Invite & QR — port of Android `BusinessInviteQr`.
/// Service Figma `2530:29334` uses lime role chips + teal chrome via `OperatingTheme`.
struct BusinessInviteRole: Identifiable, Hashable {
    let code: String
    let label: String
    var id: String { "\(code):\(label)" }
}

enum BusinessInviteMembership {
    static func membership(for roleCode: String) -> String {
        roleCode == "FINANCE" || roleCode == "APPROVER" ? "ADMIN" : "MEMBER"
    }
}

/// Consumer Service invite chips — Figma labels mapped onto the four backend
/// `operatingRole` codes. Therapist shares CONTRIBUTOR with Front desk at the API;
/// UI selection is by label so only one chip is active at a time.
enum ServiceInviteRoles {
    static let all: [BusinessInviteRole] = [
        .init(code: "FINANCE", label: "Owner"),
        .init(code: "APPROVER", label: "Manager"),
        .init(code: "OPERATIONS_LEAD", label: "Stylist"),
        .init(code: "CONTRIBUTOR", label: "Front desk"),
        .init(code: "CONTRIBUTOR", label: "Therapist"),
    ]

    static func accessNote(for roleCode: String) -> String {
        switch roleCode {
        case "FINANCE":
            return "Owner → full access · reports, payouts and invites. The role you pick sets their access when they join"
        case "APPROVER":
            return "Manager → calendar, bookings, charges and approvals · no payouts. The role you pick sets their access when they join"
        case "OPERATIONS_LEAD":
            return "Stylist → own calendar, bookings & charge view · no reports or payouts. The role you pick sets their access when they join"
        default:
            return "Front desk / Therapist → bookings & walk-ins · no reports or payouts. The role you pick sets their access when they join"
        }
    }
}

/// Invite accent chrome — Service lime vs Retail rose (`RoseInvitePalette` parity).
enum BusinessInviteAccent {
    case teal
    case rose
    case ember

    var selectedFill: Color {
        switch self {
        case .teal: return TealSheetChrome.lime
        case .rose: return Color(hex: "#F43F5E")
        case .ember: return Color(hex: "#EA580C")
        }
    }

    var selectedInk: Color {
        switch self {
        case .teal: return TealSheetChrome.inkOnAccent
        case .rose, .ember: return Color(hex: "#0F172A")
        }
    }

    var selectedStroke: Color {
        switch self {
        case .teal: return Color(hex: "#FEF3C7")
        case .rose, .ember: return Color(hex: "#FBBF24")
        }
    }

    var actionFill: Color { selectedFill }
    var highlightBorder: Color { selectedFill }
    var sectionLabel: Color {
        switch self {
        case .teal: return Color(hex: "#FEF3C7")
        case .rose, .ember: return Color(hex: "#FCD34D")
        }
    }

    var avatarFill: Color {
        switch self {
        case .teal: return Color(hex: "#134E4A")
        case .rose, .ember: return Color(hex: "#881337")
        }
    }

    var addedBadgeFill: Color {
        switch self {
        case .teal: return Color(hex: "#FEF3C7")
        case .rose, .ember: return Color(hex: "#FBBF24")
        }
    }
}

struct BusinessInviteQr: View {
    let companyId: String?
    let title: String
    let role: String?
    let theme: OperatingTheme
    var roles: [BusinessInviteRole] = ServiceInviteRoles.all
    var roleAccess: (String) -> String = { ServiceInviteRoles.accessNote(for: $0) }
    var defaultRoleLabel: String = "Stylist"
    var noun: String = "service"
    var accent: BusinessInviteAccent = .teal
    var onError: (String) -> Void = { _ in }

    @State private var roleLabel: String = "Stylist"
    @State private var roleCode: String = "OPERATIONS_LEAD"
    @State private var invite: CompanyInvite?
    @State private var minting = false
    @State private var mintError: String?
    @State private var query = ""
    @State private var contacts: [InviteContact] = []
    @State private var permissionDenied = false
    @State private var addedIds: Set<String> = []
    @State private var selectedContactId: String?
    @State private var copied = false
    @State private var saved = false
    @State private var showShare = false
    @State private var shareItems: [Any] = []

    private var canInvite: Bool { role == "OWNER_ADMIN" }

    private var displayName: String {
        let trimmed = title.trimmingCharacters(in: .whitespacesAndNewlines)
        return trimmed.isEmpty ? "this \(noun)" : trimmed
    }

    private var inviteCode: String? {
        guard let invite, invite.operatingRole == roleCode else { return nil }
        let code = invite.inviteCode.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        return code.isEmpty ? nil : code
    }

    private var invitePath: String? {
        guard let invite, let code = inviteCode else { return nil }
        if !invite.invitePath.isEmpty { return invite.invitePath }
        if !invite.inviteUrl.isEmpty { return invite.inviteUrl }
        return CompanyJoinLink.displayPath(code: code)
    }

    private var inviteBody: String? {
        guard let invitePath else { return nil }
        return InviteOutboundShare.inviteMessage(title: displayName, url: invitePath)
    }

    private var qrPayload: String? {
        inviteCode.map { CompanyJoinLink.qrPayload(code: $0) }
    }

    private var filteredContacts: [InviteContact] {
        let q = query.trimmingCharacters(in: .whitespacesAndNewlines)
        let source = contacts.filter {
            q.isEmpty
                || $0.name.localizedCaseInsensitiveContains(q)
                || $0.subtitle.localizedCaseInsensitiveContains(q)
                || $0.email.localizedCaseInsensitiveContains(q)
                || $0.phone.localizedCaseInsensitiveContains(q)
        }
        if q.isEmpty { return Array(source.prefix(6)) }
        return Array(source.prefix(20))
    }

    private var selectedContact: InviteContact? {
        filteredContacts.first { $0.id == selectedContactId }
            ?? contacts.first { $0.id == selectedContactId }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            if !canInvite {
                ownerOnlyCard
            } else {
                searchField
                contactsBlock
                roleBlock(for: selectedContact)
                linkBlock
                qrBlock
            }
            if let mintError, !mintError.isEmpty {
                Text(mintError)
                    .font(.plusJakarta(size: 12, weight: .medium))
                    .foregroundStyle(theme.negative)
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
        .onAppear { bootstrapRole() }
        .task { await loadContacts() }
        .task(id: mintKey) { await mintInvite() }
        .sheet(isPresented: $showShare) {
            ActivityShareSheet(items: shareItems)
        }
    }

    private var mintKey: String {
        "\(companyId ?? "")|\(roleCode)|\(canInvite)"
    }

    private func bootstrapRole() {
        if let match = roles.first(where: { $0.label == defaultRoleLabel })
            ?? roles.first(where: { $0.code == "OPERATIONS_LEAD" })
            ?? roles.first
        {
            roleLabel = match.label
            roleCode = match.code
        }
    }

    // MARK: - Blocks

    private var ownerOnlyCard: some View {
        Text("Invites are owner-only. Ask the owner or an admin for the link — no role is granted without them.")
            .font(.plusJakarta(size: 12, weight: .medium))
            .foregroundStyle(OperatingV12.subtitle)
            .padding(12)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(theme.inset)
            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 12, style: .continuous)
                    .strokeBorder(theme.accent.opacity(0.35), lineWidth: 1)
            )
    }

    private var searchField: some View {
        VStack(alignment: .leading, spacing: 6) {
            switch accent {
            case .rose:
                RoseFieldLabel(text: "Search")
                RoseTextField(
                    theme: theme,
                    placeholder: "Search contacts, email or phone",
                    text: $query
                )
            case .ember:
                EmberFieldLabel(text: "Search")
                EmberTextField(placeholder: "Search contacts, email or phone", text: $query)
            case .teal:
                TealFieldLabel(text: "Search")
                TealTextField(
                    theme: theme,
                    placeholder: "Search contacts, email or phone",
                    text: $query
                )
            }
        }
    }

    private var contactsBlock: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("FROM YOUR CONTACTS")
                .font(.plusJakarta(size: 11, weight: .bold))
                .foregroundStyle(accent.sectionLabel)
            if permissionDenied {
                Text("Contacts access is off. Share the invite link instead — it works the same.")
                    .font(.plusJakarta(size: 12, weight: .medium))
                    .foregroundStyle(OperatingV12.subtitle)
            } else if filteredContacts.isEmpty {
                Text(
                    contacts.isEmpty
                        ? "No contacts on this device. Share the link or the QR below."
                        : "No contact matches that search."
                )
                .font(.plusJakarta(size: 12, weight: .medium))
                .foregroundStyle(OperatingV12.subtitle)
            } else {
                ForEach(filteredContacts) { contact in
                    contactRow(contact)
                }
            }
        }
    }

    private func contactRow(_ contact: InviteContact) -> some View {
        let added = addedIds.contains(contact.id)
        let selected = selectedContactId == contact.id
        return HStack(spacing: 12) {
            Text(initials(contact.name))
                .font(.plusJakarta(size: 12, weight: .bold))
                .foregroundStyle(accent.sectionLabel)
                .frame(width: 36, height: 36)
                .background(accent.avatarFill)
                .clipShape(Circle())
            VStack(alignment: .leading, spacing: 2) {
                Text(contact.name)
                    .font(.plusJakarta(size: 14, weight: .semibold))
                    .foregroundStyle(OperatingV12.title)
                    .lineLimit(1)
                Text(contact.subtitle.isEmpty ? "Contact" : contact.subtitle)
                    .font(.plusJakarta(size: 11, weight: .medium))
                    .foregroundStyle(OperatingV12.subtitle)
                    .lineLimit(1)
            }
            Spacer(minLength: 4)
            if added {
                Text("Added · \(roleLabel)")
                    .font(.plusJakarta(size: 10, weight: .bold))
                    .foregroundStyle(accent.selectedInk)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 4)
                    .background(accent.addedBadgeFill)
                    .clipShape(Capsule())
            } else {
                Button {
                    selectedContactId = contact.id
                    addedIds.insert(contact.id)
                    if let body = inviteBody, InviteOutboundShare.looksLikePhone(contact.phone) {
                        InviteOutboundShare.sendWhatsApp(phone: contact.phone, message: body)
                    }
                } label: {
                    Text("+ Add")
                        .font(.plusJakarta(size: 12, weight: .bold))
                        .foregroundStyle(accent.selectedInk)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 8)
                        .background(accent.actionFill)
                        .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                }
                .buttonStyle(.plain)
                .disabled(inviteBody == nil)
            }
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 12)
        .background(theme.inset)
        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 14, style: .continuous)
                .strokeBorder(
                    selected || added ? accent.highlightBorder : theme.accent,
                    lineWidth: 1
                )
        )
        .onTapGesture { selectedContactId = contact.id }
    }

    private func roleBlock(for contact: InviteContact?) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(contact.map { "Role for \($0.name)" } ?? "Role for the invite")
                .font(.plusJakarta(size: 11, weight: .medium))
                .foregroundStyle(OperatingV12.subtitle)
            InviteRoleFlowLayout(spacing: 6) {
                ForEach(roles) { role in
                    let on = role.label == roleLabel
                    Button {
                        roleLabel = role.label
                        roleCode = role.code
                    } label: {
                        Text(role.label)
                            .font(.plusJakarta(size: 12, weight: on ? .bold : .medium))
                            .foregroundStyle(on ? accent.selectedInk : OperatingV12.subtitle)
                            .padding(.horizontal, 12)
                            .padding(.vertical, 8)
                            .background(on ? accent.selectedFill : theme.inset)
                            .overlay(
                                Capsule()
                                    .strokeBorder(
                                        on ? accent.selectedStroke : theme.accent,
                                        lineWidth: 1
                                    )
                            )
                            .clipShape(Capsule())
                    }
                    .buttonStyle(.plain)
                }
            }
            Text("Access: \(roleAccess(roleCode)).")
                .font(.plusJakarta(size: 11, weight: .medium))
                .foregroundStyle(accent.sectionLabel)
                .fixedSize(horizontal: false, vertical: true)
        }
    }

    private var linkBlock: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("OR SHARE INVITE LINK")
                .font(.plusJakarta(size: 11, weight: .bold))
                .foregroundStyle(accent.sectionLabel)
            if minting && invite == nil {
                ProgressView()
                    .tint(theme.accent)
            } else if let invitePath {
                HStack(spacing: 8) {
                    Text(invitePath)
                        .font(.plusJakarta(size: 13, weight: .semibold))
                        .foregroundStyle(accent.sectionLabel)
                        .lineLimit(1)
                        .truncationMode(.middle)
                        .frame(maxWidth: .infinity, alignment: .leading)
                    Button {
                        UIPasteboard.general.string = invitePath
                        copied = true
                        DispatchQueue.main.asyncAfter(deadline: .now() + 1.6) { copied = false }
                    } label: {
                        Text(copied ? "Copied" : "Copy")
                            .font(.plusJakarta(size: 12, weight: .bold))
                            .foregroundStyle(accent.selectedInk)
                            .padding(.horizontal, 12)
                            .padding(.vertical, 8)
                            .background(accent.actionFill)
                            .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                    }
                    .buttonStyle(.plain)
                }
                .padding(.horizontal, 14)
                .padding(.vertical, 12)
                .background(theme.inset)
                .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
                .overlay(
                    RoundedRectangle(cornerRadius: 14, style: .continuous)
                        .strokeBorder(theme.accent, lineWidth: 1)
                )
            } else if companyId == nil || companyId?.isEmpty == true {
                Text("This \(noun) is not linked to a company yet, so there is nothing to invite into.")
                    .font(.plusJakarta(size: 12, weight: .medium))
                    .foregroundStyle(OperatingV12.subtitle)
            } else {
                Button("Retry") {
                    Task { await mintInvite(force: true) }
                }
                .font(.plusJakarta(size: 12, weight: .semibold))
                .foregroundStyle(theme.accent)
                .buttonStyle(.plain)
            }
        }
    }

    private var qrBlock: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("OR SCAN TO JOIN")
                .font(.plusJakarta(size: 11, weight: .bold))
                .foregroundStyle(accent.sectionLabel)
            VStack(alignment: .leading, spacing: 12) {
                ZStack {
                    RoundedRectangle(cornerRadius: 16, style: .continuous)
                        .fill(Color(hex: "#FAFAF9"))
                        .frame(width: 168, height: 168)
                    if let qrPayload, let qr = GroupQRCode.image(from: qrPayload, size: 148) {
                        Image(uiImage: qr)
                            .resizable()
                            .interpolation(.none)
                            .frame(width: 148, height: 148)
                    } else if minting {
                        ProgressView().tint(theme.accent)
                    } else {
                        Text("No invite yet")
                            .font(.plusJakarta(size: 12, weight: .medium))
                            .foregroundStyle(Color(hex: "#64748B"))
                    }
                }
                Text("\(displayName) · joins as \(roleLabel) by default")
                    .font(.plusJakarta(size: 11, weight: .medium))
                    .foregroundStyle(OperatingV12.subtitle)
                HStack(spacing: 8) {
                    outlineAction("Share QR", enabled: qrPayload != nil) {
                        guard let qrPayload, let qr = GroupQRCode.image(from: qrPayload, size: 1024) else { return }
                        shareItems = [qr, invitePath ?? ""]
                        showShare = true
                    }
                    outlineAction(saved ? "Saved" : "Save to Photos", enabled: qrPayload != nil) {
                        saveQR()
                    }
                }
            }
            .padding(14)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(theme.inset)
            .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 14, style: .continuous)
                    .strokeBorder(accent.highlightBorder, lineWidth: 1)
            )
            .shadow(color: accent.highlightBorder.opacity(0.25), radius: 12, y: 0)
        }
    }

    private func outlineAction(_ title: String, enabled: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(title)
                .font(.plusJakarta(size: 12, weight: .bold))
                .foregroundStyle(OperatingV12.title)
                .padding(.horizontal, 12)
                .padding(.vertical, 8)
                .overlay(
                    RoundedRectangle(cornerRadius: 10, style: .continuous)
                        .strokeBorder(
                            enabled ? accent.highlightBorder : theme.accent.opacity(0.4),
                            lineWidth: 1
                        )
                )
        }
        .buttonStyle(.plain)
        .disabled(!enabled)
    }

    // MARK: - Work

    private func mintInvite(force: Bool = false) async {
        guard canInvite else { return }
        guard let companyId, !companyId.isEmpty else { return }
        if !force, let invite, invite.operatingRole == roleCode { return }
        minting = true
        mintError = nil
        defer { minting = false }
        do {
            let minted = try await APIClient.shared.mintCompanyInvite(
                companyId: companyId,
                membershipType: BusinessInviteMembership.membership(for: roleCode),
                operatingRole: roleCode,
                idempotencyKey: UUID().uuidString
            )
            invite = minted
            copied = false
            saved = false
        } catch {
            if invite == nil {
                mintError = error.localizedDescription
                onError(error.localizedDescription)
            }
        }
    }

    private func saveQR() {
        guard let qrPayload, let qr = GroupQRCode.image(from: qrPayload, size: 1024) else { return }
        PHPhotoLibrary.requestAuthorization(for: .addOnly) { status in
            guard status == .authorized || status == .limited else {
                DispatchQueue.main.async { onError("Could not save the QR to Photos") }
                return
            }
            UIImageWriteToSavedPhotosAlbum(qr, nil, nil, nil)
            DispatchQueue.main.async {
                saved = true
                DispatchQueue.main.asyncAfter(deadline: .now() + 1.6) { saved = false }
            }
        }
    }

    private func loadContacts() async {
        do {
            let rows = try await GroupContactsLoader.fetchRowsIfAuthorized()
            permissionDenied = false
            contacts = rows.map {
                InviteContact(
                    id: $0.id,
                    name: $0.name,
                    subtitle: $0.subtitle,
                    phone: $0.phone,
                    email: $0.email
                )
            }
            if selectedContactId == nil {
                selectedContactId = contacts.first?.id
            }
        } catch {
            permissionDenied = true
        }
    }

    private func initials(_ name: String) -> String {
        let parts = name.split(separator: " ").prefix(2)
        let letters = parts.compactMap { $0.first.map(String.init) }
        return letters.joined().uppercased().isEmpty ? "?" : letters.joined().uppercased()
    }
}

private struct InviteContact: Identifiable {
    let id: String
    let name: String
    let subtitle: String
    let phone: String
    let email: String
}

/// Lightweight wrap layout for role chips (Figma flow-row).
private struct InviteRoleFlowLayout: Layout {
    var spacing: CGFloat = 6

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let result = arrange(proposal: proposal, subviews: subviews)
        return result.size
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        let result = arrange(proposal: proposal, subviews: subviews)
        for (index, frame) in result.frames.enumerated() {
            subviews[index].place(
                at: CGPoint(x: bounds.minX + frame.minX, y: bounds.minY + frame.minY),
                proposal: ProposedViewSize(frame.size)
            )
        }
    }

    private func arrange(proposal: ProposedViewSize, subviews: Subviews) -> (size: CGSize, frames: [CGRect]) {
        let maxWidth = proposal.width ?? .infinity
        var frames: [CGRect] = []
        var x: CGFloat = 0
        var y: CGFloat = 0
        var rowHeight: CGFloat = 0
        var width: CGFloat = 0
        for subview in subviews {
            let size = subview.sizeThatFits(.unspecified)
            if x + size.width > maxWidth, x > 0 {
                x = 0
                y += rowHeight + spacing
                rowHeight = 0
            }
            frames.append(CGRect(origin: CGPoint(x: x, y: y), size: size))
            rowHeight = max(rowHeight, size.height)
            x += size.width + spacing
            width = max(width, x - spacing)
        }
        return (CGSize(width: width, height: y + rowHeight), frames)
    }
}
