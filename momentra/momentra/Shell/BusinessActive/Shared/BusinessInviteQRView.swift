import Photos
import SwiftUI
import UIKit

/// Invite & QR — Figma `RTL-MASTER · Invite & QR` `2506:29333` and
/// `RST-MASTER · Invite & QR` `2465:29655`.
///
/// Promoted out of the retail masters tab so both sections share one honest surface.
/// Everything shown is real: the QR is drawn from an invite code minted by
/// `POST /v1/company/invites`, the link is whatever that endpoint returns, and the
/// contact list comes from the device. Until an invite exists the QR block says so
/// instead of drawing a placeholder.
///
/// The frames show five role chips; the backend accepts only
/// `FINANCE | APPROVER | OPERATIONS_LEAD | CONTRIBUTOR` (`mintCompanyInviteSchema`),
/// so those four are the ones offered.
struct BusinessInviteQRView: View {
    let title: String
    let role: String?
    let momentId: String?
    var palette: BusinessInvitePalette = .rose
    var icon: String? = nil
    var noun: String = "store"
    /// `true` when drawn inside a parent sheet that already renders grabber + header.
    var embedded: Bool = true

    @Environment(\.dismiss) private var dismiss

    @State private var companyId: String?
    @State private var roleCode = "OPERATIONS_LEAD"
    @State private var query = ""
    @State private var contacts: [GroupContactsLoader.Row] = []
    @State private var contactsDenied = false
    @State private var invite: CompanyInvite?
    @State private var minting = false
    @State private var mintError: String?
    @State private var mintedRole: String?
    @State private var copied = false
    @State private var saved = false
    @State private var shareItems: [Any] = []
    @State private var showShare = false

    private static let roles: [(code: String, label: String, access: String)] = [
        ("FINANCE", "Finance", "money, payments and finance boards"),
        ("APPROVER", "Approver", "approves money and stock adjustments"),
        ("OPERATIONS_LEAD", "Operations", "sales, stock, purchases and orders"),
        ("CONTRIBUTOR", "Contributor", "records activity; money waits for approval"),
    ]

    private var canInvite: Bool { role == "OWNER_ADMIN" }
    private var trimmedTitle: String {
        let name = title.trimmingCharacters(in: .whitespacesAndNewlines)
        return name.isEmpty ? "this \(noun)" : name
    }

    private var invitePath: String? {
        guard let invite else { return nil }
        let path = invite.invitePath.trimmingCharacters(in: .whitespacesAndNewlines)
        if !path.isEmpty { return path }
        let url = invite.inviteUrl.trimmingCharacters(in: .whitespacesAndNewlines)
        if !url.isEmpty { return url }
        return CompanyJoinLink.displayPath(code: invite.inviteCode)
    }

    private var qrImage: UIImage? {
        guard let code = invite?.inviteCode, !code.isEmpty else { return nil }
        return GroupQRCode.image(from: CompanyJoinLink.qrPayload(code: code), size: 168)
    }

    private var shareMessage: String? {
        guard let invitePath else { return nil }
        return InviteOutboundShare.inviteMessage(title: trimmedTitle, url: invitePath)
    }

    private var filteredContacts: [GroupContactsLoader.Row] {
        let q = query.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !q.isEmpty else { return Array(contacts.prefix(6)) }
        return contacts
            .filter {
                $0.name.localizedCaseInsensitiveContains(q)
                    || $0.subtitle.localizedCaseInsensitiveContains(q)
                    || $0.email.localizedCaseInsensitiveContains(q)
                    || $0.phone.localizedCaseInsensitiveContains(q)
            }
            .prefix(20)
            .map { $0 }
    }

    var body: some View {
        content
            .task { await resolveCompany() }
            .task(id: "\(companyId ?? "")|\(roleCode)") { await mintIfPossible() }
            .task { await loadContacts() }
            .sheet(isPresented: $showShare) {
                ActivityShareSheet(items: shareItems)
            }
    }

    @ViewBuilder
    private var content: some View {
        VStack(alignment: .leading, spacing: 12) {
            headerCard
            if !canInvite { ownerOnlyNote }
            roleBlock
            if canInvite {
                searchField
                contactsBlock
                inviteLinkBlock
                qrBlock
            } else {
                Text("Invites are owner-only. Ask the owner or an admin for the link — "
                    + "no role is granted without them.")
                    .font(.plusJakarta(size: 11))
                    .foregroundStyle(palette.textBody)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(14)
                    .background(palette.field)
                    .overlay(
                        RoundedRectangle(cornerRadius: 12, style: .continuous)
                            .stroke(palette.chipStroke, lineWidth: 1)
                    )
                    .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
            }

            HStack(alignment: .top, spacing: 8) {
                Text("Why")
                    .font(.plusJakarta(size: 11, weight: .bold))
                    .foregroundStyle(palette.accent)
                Text("Invite grants a role and its access. Joining ≠ approval ≠ payment.")
                    .font(.plusJakarta(size: 11))
                    .foregroundStyle(palette.textBody)
                    .fixedSize(horizontal: false, vertical: true)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
            .padding(10)
            .background(palette.accent.opacity(0.10))
            .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 10, style: .continuous)
                    .stroke(palette.accent.opacity(0.35), lineWidth: 1)
            )

            if !embedded {
                Button("Done") { dismiss() }
                    .font(.plusJakarta(size: 16, weight: .heavy))
                    .foregroundStyle(.white)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 16)
                    .background(palette.accent)
                    .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))

                Text("Roles apply when the person joins · one link per role")
                    .font(.plusJakarta(size: 10))
                    .foregroundStyle(palette.textFaint)
                    .frame(maxWidth: .infinity, alignment: .center)
            }
        }
    }

    // MARK: - Header card

    private var headerCard: some View {
        HStack(spacing: 10) {
            if let icon {
                ZStack {
                    RoundedRectangle(cornerRadius: 12, style: .continuous)
                        .fill(palette.accent.opacity(0.16))
                    RoundedRectangle(cornerRadius: 12, style: .continuous)
                        .stroke(palette.accent.opacity(0.45), lineWidth: 1)
                    Image(icon)
                        .renderingMode(.template)
                        .resizable()
                        .scaledToFit()
                        .frame(width: 18, height: 18)
                        .foregroundStyle(palette.accent)
                }
                .frame(width: 36, height: 36)
            }
            VStack(alignment: .leading, spacing: 2) {
                Text("Invite the team")
                    .font(.plusJakarta(size: 16, weight: .bold))
                    .foregroundStyle(palette.textPrimary)
                    .lineLimit(1)
                Text("\(trimmedTitle) · \(businessInviteRoleLabel(role)) · invite grants a role")
                    .font(.plusJakarta(size: 11))
                    .foregroundStyle(palette.accentMuted)
                    .lineLimit(2)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            if !canInvite {
                Text("Owner only")
                    .font(.plusJakarta(size: 10, weight: .bold))
                    .foregroundStyle(palette.warning)
                    .padding(.horizontal, 9)
                    .padding(.vertical, 4)
                    .background(palette.warning.opacity(0.16))
                    .clipShape(Capsule())
            }
        }
        .padding(14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(palette.card)
        .overlay(
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .stroke(palette.accent.opacity(0.22), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
    }

    private var ownerOnlyNote: some View {
        HStack(spacing: 8) {
            Text("Owner only")
                .font(.plusJakarta(size: 10, weight: .bold))
                .foregroundStyle(palette.warning)
                .padding(.horizontal, 9)
                .padding(.vertical, 4)
                .background(palette.warning.opacity(0.16))
                .clipShape(Capsule())
            Text("Only the owner or an admin can mint an invite. Ask them for the link.")
                .font(.plusJakarta(size: 11))
                .foregroundStyle(palette.textBody)
                .fixedSize(horizontal: false, vertical: true)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(14)
        .background(palette.card)
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
    }

    // MARK: - Role

    private var roleBlock: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("ROLE FOR THE INVITE")
                .font(.plusJakarta(size: 10, weight: .bold))
                .tracking(0.6)
                .foregroundStyle(palette.textFaint)
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 6) {
                    ForEach(Self.roles, id: \.code) { item in
                        let on = item.code == roleCode
                        Button { roleCode = item.code } label: {
                            Text(item.label)
                                .font(.plusJakarta(size: 12, weight: on ? .bold : .medium))
                                .foregroundStyle(on ? palette.textPrimary : palette.textFaint)
                                .padding(.horizontal, 10)
                                .padding(.vertical, 7)
                                .background(on ? palette.accent : palette.chip)
                                .overlay(
                                    Capsule().stroke(on ? palette.accent : palette.chipStroke, lineWidth: 1)
                                )
                                .clipShape(Capsule())
                        }
                        .buttonStyle(.plain)
                        .disabled(!canInvite)
                    }
                }
            }
            if let access = Self.roles.first(where: { $0.code == roleCode })?.access {
                Text("Access: \(access).")
                    .font(.plusJakarta(size: 11))
                    .foregroundStyle(palette.textFaint)
                    .fixedSize(horizontal: false, vertical: true)
            }
            if let mintError {
                Text(mintError)
                    .font(.plusJakarta(size: 11))
                    .foregroundStyle(palette.negative)
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
    }

    // MARK: - Contacts

    private var searchField: some View {
        HStack(spacing: 8) {
            Image(systemName: "magnifyingglass")
                .font(.system(size: 12, weight: .semibold))
                .foregroundStyle(palette.textFaint)
            TextField(
                "",
                text: $query,
                prompt: Text("Search contacts, email or phone")
                    .foregroundStyle(palette.textFaint)
            )
            .font(.plusJakarta(size: 13))
            .foregroundStyle(palette.textPrimary)
            .textInputAutocapitalization(.never)
            .autocorrectionDisabled()
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 11)
        .background(palette.field)
        .overlay(
            RoundedRectangle(cornerRadius: 12, style: .continuous)
                .stroke(palette.chipStroke, lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
    }

    private var contactsBlock: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("FROM YOUR CONTACTS")
                .font(.plusJakarta(size: 10, weight: .bold))
                .tracking(0.6)
                .foregroundStyle(palette.textFaint)

            if contactsDenied {
                Text("Contacts access is off. Share the invite link instead — it works the same.")
                    .font(.plusJakarta(size: 11))
                    .foregroundStyle(palette.textBody)
                    .fixedSize(horizontal: false, vertical: true)
            } else if filteredContacts.isEmpty {
                Text(
                    contacts.isEmpty
                        ? "No contacts on this device. Share the link or the QR below."
                        : "No contact matches that search."
                )
                .font(.plusJakarta(size: 11))
                .foregroundStyle(palette.textBody)
                .fixedSize(horizontal: false, vertical: true)
            } else {
                VStack(spacing: 0) {
                    ForEach(Array(filteredContacts.enumerated()), id: \.element.id) { index, row in
                        contactRow(row)
                        if index < filteredContacts.count - 1 {
                            Rectangle()
                                .fill(palette.divider)
                                .frame(height: 1)
                        }
                    }
                }
                .padding(.horizontal, 10)
                .background(palette.field)
                .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
            }
        }
    }

    private func contactRow(_ row: GroupContactsLoader.Row) -> some View {
        HStack(spacing: 10) {
            ZStack {
                Circle().fill(palette.accent.opacity(0.16))
                Text(initials(row.name))
                    .font(.plusJakarta(size: 12, weight: .bold))
                    .foregroundStyle(palette.accentMuted)
            }
            .frame(width: 32, height: 32)

            VStack(alignment: .leading, spacing: 1) {
                Text(row.name.isEmpty ? "Unnamed contact" : row.name)
                    .font(.plusJakarta(size: 13, weight: .medium))
                    .foregroundStyle(palette.textPrimary)
                    .lineLimit(1)
                Text(row.subtitle)
                    .font(.plusJakarta(size: 11))
                    .foregroundStyle(palette.textMuted)
                    .lineLimit(1)
            }
            .frame(maxWidth: .infinity, alignment: .leading)

            Button {
                sendInvite(to: row)
            } label: {
                Text("Send")
                    .font(.plusJakarta(size: 11, weight: .bold))
                    .foregroundStyle(invitePath == nil ? palette.textFaint : palette.accent)
                    .padding(.horizontal, 10)
                    .padding(.vertical, 6)
                    .overlay(
                        Capsule().stroke(
                            invitePath == nil ? palette.chipStroke : palette.accent.opacity(0.55),
                            lineWidth: 1
                        )
                    )
                    .clipShape(Capsule())
            }
            .buttonStyle(.plain)
            .disabled(invitePath == nil)
        }
        .padding(.vertical, 9)
    }

    private func initials(_ name: String) -> String {
        let parts = name
            .split(separator: " ")
            .prefix(2)
            .compactMap { $0.first.map(String.init) }
        return parts.isEmpty ? "?" : parts.joined().uppercased()
    }

    // MARK: - Link + QR

    private var inviteLinkBlock: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("OR SHARE INVITE LINK")
                .font(.plusJakarta(size: 10, weight: .bold))
                .tracking(0.6)
                .foregroundStyle(palette.textFaint)

            if minting && invite == nil {
                ProgressView().tint(palette.accent).scaleEffect(0.8)
            } else if let invitePath {
                HStack(spacing: 8) {
                    Text(invitePath)
                        .font(.plusJakarta(size: 12, weight: .semibold))
                        .foregroundStyle(palette.accentMuted)
                        .lineLimit(1)
                        .truncationMode(.middle)
                        .frame(maxWidth: .infinity, alignment: .leading)
                    Button {
                        UIPasteboard.general.string = invitePath
                        copied = true
                    } label: {
                        Text(copied ? "Copied" : "Copy")
                            .font(.plusJakarta(size: 11, weight: .bold))
                            .foregroundStyle(palette.accent)
                            .padding(.horizontal, 10)
                            .padding(.vertical, 6)
                            .overlay(Capsule().stroke(palette.accent.opacity(0.55), lineWidth: 1))
                            .clipShape(Capsule())
                    }
                    .buttonStyle(.plain)
                }
                .padding(.horizontal, 10)
                .padding(.vertical, 10)
                .background(palette.field)
                .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
            } else if companyId == nil {
                Text("This \(noun) is not linked to a company yet, so there is nothing to invite into.")
                    .font(.plusJakarta(size: 11))
                    .foregroundStyle(palette.textBody)
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
    }

    private var qrBlock: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("OR SCAN TO JOIN")
                .font(.plusJakarta(size: 10, weight: .bold))
                .tracking(0.6)
                .foregroundStyle(palette.textFaint)

            VStack(spacing: 10) {
                if let qrImage {
                    Image(uiImage: qrImage)
                        .interpolation(.none)
                        .resizable()
                        .frame(width: 168, height: 168)
                        .padding(10)
                        .background(Color.white, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
                } else {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("QR code needs an invite token")
                            .font(.plusJakarta(size: 13, weight: .medium))
                            .foregroundStyle(palette.textPrimary)
                        Text("The QR is drawn from the minted invite code — never from a placeholder.")
                            .font(.plusJakarta(size: 11))
                            .foregroundStyle(palette.textBody)
                            .fixedSize(horizontal: false, vertical: true)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(14)
                    .background(palette.field)
                    .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                }

                Text("\(trimmedTitle) · joins as "
                    + "\(businessInviteRoleLabel(mintedRole ?? roleCode)) by default")
                    .font(.plusJakarta(size: 10))
                    .foregroundStyle(palette.textMuted)
                    .multilineTextAlignment(.center)
                    .fixedSize(horizontal: false, vertical: true)

                HStack(spacing: 10) {
                    outlineAction(title: "Share QR", enabled: qrImage != nil) {
                        guard let qrImage, let invitePath else { return }
                        shareItems = [qrImage, invitePath]
                        showShare = true
                    }
                    outlineAction(title: saved ? "Saved" : "Save to Photos", enabled: qrImage != nil) {
                        saveQR()
                    }
                }
            }
            .frame(maxWidth: .infinity)
            .padding(12)
            .background(palette.qrPlate)
            .overlay(
                RoundedRectangle(cornerRadius: 16, style: .continuous)
                    .stroke(palette.accent.opacity(0.4), lineWidth: 1)
            )
            .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
        }
    }

    private func outlineAction(title: String, enabled: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(title)
                .font(.plusJakarta(size: 12, weight: .bold))
                .foregroundStyle(enabled ? palette.accent : palette.textFaint)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 10)
                .overlay(
                    RoundedRectangle(cornerRadius: 12, style: .continuous)
                        .stroke(enabled ? palette.accent.opacity(0.55) : palette.chipStroke, lineWidth: 1)
                )
        }
        .buttonStyle(.plain)
        .disabled(!enabled)
    }

    // MARK: - Actions

    private func sendInvite(to row: GroupContactsLoader.Row) {
        guard let message = shareMessage else { return }
        let phone = InviteOutboundShare.looksLikePhone(row.phone) ? row.phone : nil
        InviteOutboundShare.sendWhatsApp(phone: phone, message: message)
    }

    private func saveQR() {
        guard let qrImage else { return }
        PHPhotoLibrary.requestAuthorization(for: .addOnly) { status in
            guard status == .authorized || status == .limited else { return }
            UIImageWriteToSavedPhotosAlbum(qrImage, nil, nil, nil)
            DispatchQueue.main.async {
                saved = true
                DispatchQueue.main.asyncAfter(deadline: .now() + 1.6) { saved = false }
            }
        }
    }

    private func resolveCompany() async {
        guard let momentId, !momentId.isEmpty else { return }
        do {
            let moments = try await APIClient.shared.listBusinessMoments(limit: 50)
            companyId = moments.first { $0.momentId == momentId }?.companyId
        } catch {
            companyId = nil
        }
    }

    private func mintIfPossible() async {
        guard canInvite, let companyId else { return }
        minting = true
        mintError = nil
        defer { minting = false }
        do {
            let minted = try await APIClient.shared.mintCompanyInvite(
                companyId: companyId,
                membershipType: businessInviteMembership(roleCode),
                operatingRole: roleCode,
                idempotencyKey: UUID().uuidString
            )
            invite = minted
            mintedRole = roleCode
            copied = false
            saved = false
        } catch {
            // A role change re-mints; a failure keeps the previous link usable.
            if invite == nil {
                mintError = error.localizedDescription
            }
        }
    }

    private func loadContacts() async {
        do {
            contacts = try await GroupContactsLoader.fetchRowsIfAuthorized()
            contactsDenied = false
        } catch {
            contacts = []
            contactsDenied = true
        }
    }
}

/// The two section palettes the invite surface can wear.
struct BusinessInvitePalette {
    let accent: Color
    let accentMuted: Color
    let card: Color
    let field: Color
    let chip: Color
    let chipStroke: Color
    let divider: Color
    let qrPlate: Color
    let textPrimary: Color
    let textBody: Color
    let textMuted: Color
    let textFaint: Color
    let warning: Color
    let negative: Color

    static let rose = BusinessInvitePalette(
        accent: RetailRose.rose,
        accentMuted: RetailRose.roseMuted,
        card: RetailRose.cardSlate,
        field: RetailRose.field,
        chip: RetailRose.chip,
        chipStroke: RetailRose.chipStroke,
        divider: RetailRose.divider,
        qrPlate: RetailRose.cardSlate,
        textPrimary: RetailRose.textPrimary,
        textBody: RetailRose.textBody,
        textMuted: RetailRose.textMuted,
        textFaint: RetailRose.textFaint,
        warning: RetailRose.warning,
        negative: RetailRose.negative
    )

    static let ember = BusinessInvitePalette(
        accent: RestaurantEmber.ember,
        accentMuted: RestaurantEmber.gold,
        card: RestaurantEmber.card,
        field: RestaurantEmber.surfaceWell,
        chip: RestaurantEmber.chip,
        chipStroke: RestaurantEmber.chipStroke,
        divider: RestaurantEmber.divider,
        qrPlate: RestaurantEmber.surfaceWell,
        textPrimary: RestaurantEmber.textPrimary,
        textBody: RestaurantEmber.textBody,
        textMuted: RestaurantEmber.textMuted,
        textFaint: RestaurantEmber.textFaint,
        warning: RestaurantEmber.warning,
        negative: RestaurantEmber.negative
    )
}
