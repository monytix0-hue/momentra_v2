import Photos
import SwiftUI
import UIKit

/// §02's invite sheet, modelled on the group setup `Add People` sheet (`579:12741`): the role the
/// invite grants, the device contacts it can be sent to, the link, and the QR.
///
/// Every value is real. The code comes from `POST /v1/company/invites`, minted by the host the
/// first time §02 needs one, so the link and the QR are the same code the invitee redeems. Until
/// it lands the QR block says so rather than drawing a placeholder.
struct BusinessSetupInviteSheet: View {
    let title: String
    let theme: OperatingTheme
    let roleCode: String
    let invite: CompanyInvite?
    let minting: Bool
    let error: String?
    let existingNames: [String]
    var onRoleChange: (String) -> Void
    var onAddPerson: (BusinessSetupInvitee) -> Void
    var onRetry: () -> Void
    var onDismiss: () -> Void

    @State private var query = ""
    @State private var contacts: [SetupContact] = []
    @State private var permissionDenied = false
    @State private var copied = false
    @State private var saved = false
    @State private var showShare = false
    @State private var shareItems: [Any] = []

    private static let contactPreviewLimit = 5

    private var displayName: String {
        let trimmed = title.trimmingCharacters(in: .whitespacesAndNewlines)
        return trimmed.isEmpty ? "this business" : trimmed
    }

    private var inviteCode: String? {
        // A code grants the role it was minted with, so a code minted for another chip is never
        // shown as this role's link — the host re-mints on a role change.
        guard let invite, invite.operatingRole == roleCode else { return nil }
        let code = invite.inviteCode.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        guard !code.isEmpty else { return nil }
        return code
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

    private var existingLower: Set<String> {
        Set(existingNames.map { $0.lowercased() })
    }

    private var displayContacts: [SetupContact] {
        if !permissionDenied && !contacts.isEmpty { return contacts }
        return []
    }

    private var filtered: [SetupContact] {
        let q = query.trimmingCharacters(in: .whitespacesAndNewlines)
        let source = displayContacts.filter {
            q.isEmpty
                || $0.name.localizedCaseInsensitiveContains(q)
                || $0.subtitle.localizedCaseInsensitiveContains(q)
        }
        if q.isEmpty, source.count > Self.contactPreviewLimit {
            return Array(source.prefix(Self.contactPreviewLimit))
        }
        return source
    }

    var body: some View {
        VStack(spacing: 14) {
            header
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    rolesBlock
                    searchField
                    contactsBlock
                    linkBlock
                    Divider().overlay(theme.border)
                    qrBlock
                    if let error, !error.isEmpty {
                        Text(error)
                            .font(.plusJakarta(size: 12))
                            .foregroundStyle(theme.negative)
                            .fixedSize(horizontal: false, vertical: true)
                    }
                    shareSaveRow
                }
            }
            doneButton
        }
        .padding(.top, 12)
        .padding(.horizontal, 16)
        .padding(.bottom, 24)
        .background(theme.canvas)
        .task { await loadContacts() }
        .sheet(isPresented: $showShare) {
            ActivityShareSheet(items: shareItems)
        }
    }

    // MARK: - Blocks

    private var header: some View {
        VStack(spacing: 14) {
            Capsule()
                .fill(theme.border)
                .frame(width: 36, height: 4)
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text("Invite people")
                        .font(.plusJakarta(size: 18, weight: .bold))
                        .foregroundStyle(theme.ink)
                    Text(displayName)
                        .font(.plusJakarta(size: 11))
                        .foregroundStyle(theme.muted)
                        .lineLimit(1)
                }
                Spacer(minLength: 8)
                Button(action: onDismiss) {
                    Text("Close")
                        .font(.plusJakarta(size: 12, weight: .semibold))
                        .foregroundStyle(theme.muted)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 6)
                        .background(theme.inset, in: Capsule())
                        .overlay(Capsule().stroke(theme.border, lineWidth: 1))
                }
                .buttonStyle(.plain)
                .accessibilityLabel("Close")
            }
        }
    }

    /// The invite grants exactly this role, so the access line follows the chip.
    private var rolesBlock: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Role")
                .font(.plusJakarta(size: 11, weight: .bold))
                .foregroundStyle(theme.accent)
            HStack(spacing: 6) {
                ForEach(BusinessSetupDocSpec.roles(vertical: theme.vertical)) { role in
                    let selected = role.code == roleCode
                    Button {
                        onRoleChange(role.code)
                    } label: {
                        Text(role.label)
                            .font(.plusJakarta(size: 12, weight: selected ? .bold : .medium))
                            .foregroundStyle(selected ? theme.onAccent : theme.muted)
                            .lineLimit(1)
                            .padding(.horizontal, 11)
                            .padding(.vertical, 7)
                            .background(selected ? theme.accent : theme.inset, in: Capsule())
                            .overlay(
                                Capsule().stroke(selected ? theme.accent : theme.border, lineWidth: 1)
                            )
                    }
                    .buttonStyle(.plain)
                }
            }
            Text(BusinessSetupDocSpec.inviteAccessNote(roleCode))
                .font(.plusJakarta(size: 11))
                .foregroundStyle(theme.muted)
                .fixedSize(horizontal: false, vertical: true)
        }
    }

    private var searchField: some View {
        HStack(spacing: 10) {
            Image(systemName: "magnifyingglass")
                .font(.system(size: 13, weight: .medium))
                .foregroundStyle(theme.muted)
            TextField(
                "",
                text: $query,
                prompt: Text("Search contacts, email or phone")
                    .font(.plusJakarta(size: 13))
                    .foregroundStyle(theme.muted)
            )
            .font(.plusJakarta(size: 13))
            .foregroundStyle(theme.ink)
            .autocorrectionDisabled()
            .textInputAutocapitalization(.never)
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 12)
        .background(theme.inset, in: RoundedRectangle(cornerRadius: 14))
        .overlay(
            RoundedRectangle(cornerRadius: 14).stroke(theme.border, lineWidth: 1)
        )
    }

    private var contactsBlock: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text("From your contacts")
                .font(.plusJakarta(size: 11, weight: .bold))
                .foregroundStyle(theme.accent)
            if filtered.isEmpty {
                Text(permissionDenied ? "Contacts are not available." : "No matching contacts.")
                    .font(.plusJakarta(size: 12))
                    .foregroundStyle(theme.muted)
                    .padding(.vertical, 8)
            }
            ForEach(Array(filtered.enumerated()), id: \.element.id) { index, contact in
                if index > 0 { Divider().overlay(theme.border) }
                contactRow(contact)
            }
        }
    }

    private func contactRow(_ contact: SetupContact) -> some View {
        let already = existingLower.contains(contact.name.lowercased())
        return HStack(spacing: 12) {
            Text(BusinessSetupDocSpec.initials(contact.name))
                .font(.plusJakarta(size: 13, weight: .bold))
                .foregroundStyle(theme.muted)
                .frame(width: 40, height: 40)
                .background(theme.inset)
                .clipShape(Circle())
            VStack(alignment: .leading, spacing: 2) {
                Text(contact.name)
                    .font(.plusJakarta(size: 14, weight: .semibold))
                    .foregroundStyle(theme.ink)
                    .lineLimit(1)
                Text(contact.subtitle.isEmpty ? "Contact" : contact.subtitle)
                    .font(.plusJakarta(size: 12))
                    .foregroundStyle(theme.muted)
                    .lineLimit(1)
            }
            Spacer(minLength: 4)
            Button {
                guard !already else { return }
                onAddPerson(
                    BusinessSetupInvitee(
                        id: "contact:" + contact.id,
                        name: contact.name,
                        roleCode: roleCode,
                        phone: contact.phone
                    )
                )
                query = ""
                if let body = inviteBody, InviteOutboundShare.looksLikePhone(contact.phone) {
                    InviteOutboundShare.sendWhatsApp(phone: contact.phone, message: body)
                }
            } label: {
                Text(already ? "Invited" : "Send")
                    .font(.plusJakarta(size: 12, weight: .bold))
                    .foregroundStyle(already ? theme.muted : theme.accent)
                    .padding(.horizontal, 14)
                    .padding(.vertical, 6)
                    .background(
                        already ? theme.border : theme.accent.opacity(0.13),
                        in: Capsule()
                    )
                    .overlay(
                        Capsule().stroke(
                            already ? theme.border : theme.accent.opacity(0.4),
                            lineWidth: 1
                        )
                    )
            }
            .buttonStyle(.plain)
            .disabled(already)
        }
        .padding(.vertical, 8)
    }

    private var linkBlock: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Or share invite link")
                .font(.plusJakarta(size: 12, weight: .semibold))
                .foregroundStyle(theme.muted)
            HStack(spacing: 8) {
                Text(invitePath ?? (minting ? "Minting an invite…" : "Could not mint an invite"))
                    .font(.plusJakarta(size: 12))
                    .foregroundStyle(theme.muted)
                    .lineLimit(1)
                    .truncationMode(.middle)
                    .frame(maxWidth: .infinity, alignment: .leading)
                Button {
                    guard let invitePath else { return }
                    UIPasteboard.general.string = invitePath
                    copied = true
                    DispatchQueue.main.asyncAfter(deadline: .now() + 1.6) { copied = false }
                } label: {
                    Text(copied ? "Copied" : "Copy")
                        .font(.plusJakarta(size: 12, weight: .bold))
                        .foregroundStyle(invitePath == nil ? theme.muted : theme.onAccent)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 8)
                        .background(invitePath == nil ? theme.border : theme.accent, in: RoundedRectangle(cornerRadius: 10))
                }
                .buttonStyle(.plain)
                .disabled(invitePath == nil)
            }
            .padding(.leading, 12)
            .padding(.trailing, 4)
            .padding(.vertical, 4)
            .background(theme.inset, in: RoundedRectangle(cornerRadius: 12))
            .overlay(
                RoundedRectangle(cornerRadius: 12).stroke(theme.border, lineWidth: 1)
            )
            if invitePath == nil, !minting {
                Button(action: onRetry) {
                    Text("Retry")
                        .font(.plusJakarta(size: 12, weight: .semibold))
                        .foregroundStyle(theme.accent)
                }
                .buttonStyle(.plain)
            }
            InviteSendChannelButtons(
                enabled: inviteBody != nil,
                accent: theme.accent,
                onMessages: {
                    guard let body = inviteBody else { return }
                    InviteOutboundShare.sendSms(phone: nil, message: body)
                },
                onWhatsApp: {
                    guard let body = inviteBody else { return }
                    InviteOutboundShare.sendWhatsApp(phone: nil, message: body)
                }
            )
        }
    }

    private var qrBlock: some View {
        VStack(spacing: 12) {
            Text("Or scan to join")
                .font(.plusJakarta(size: 11, weight: .bold))
                .foregroundStyle(theme.accent)
            ZStack {
                RoundedRectangle(cornerRadius: 16)
                    .fill(Color.white)
                    .frame(width: 188, height: 188)
                if let qrPayload, let qr = GroupQRCode.image(from: qrPayload, size: 156) {
                    Image(uiImage: qr)
                        .resizable()
                        .interpolation(.none)
                        .frame(width: 156, height: 156)
                } else if minting {
                    ProgressView()
                        .tint(theme.accent)
                } else {
                    Text("No invite yet")
                        .font(.plusJakarta(size: 12))
                        .foregroundStyle(Color(hex: "#64748B"))
                }
            }
            Text("Scan with the Momentra app to join. Joining is not approval and not payment.")
                .font(.plusJakarta(size: 11))
                .foregroundStyle(theme.muted)
                .multilineTextAlignment(.center)
                .fixedSize(horizontal: false, vertical: true)
        }
        .frame(maxWidth: .infinity)
    }

    private var shareSaveRow: some View {
        HStack(spacing: 10) {
            outlineAction("Share QR", enabled: qrPayload != nil) {
                guard let qrPayload, let qr = GroupQRCode.image(from: qrPayload, size: 1024) else { return }
                shareItems = [qr, invitePath ?? ""]
                showShare = true
            }
            outlineAction(saved ? "Saved" : "Save to Photos", enabled: qrPayload != nil) { saveQR() }
        }
    }

    private func outlineAction(_ title: String, enabled: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(title)
                .font(.plusJakarta(size: 13, weight: .bold))
                .foregroundStyle(enabled ? theme.accent : theme.muted)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 12)
                .overlay(
                    RoundedRectangle(cornerRadius: 14)
                        .stroke(enabled ? theme.accent.opacity(0.55) : theme.border, lineWidth: 1)
                )
        }
        .buttonStyle(.plain)
        .disabled(!enabled)
    }

    private var doneButton: some View {
        Button(action: onDismiss) {
            Text("Done")
                .font(.plusJakarta(size: 16, weight: .heavy))
                .foregroundStyle(theme.onAccent)
                .frame(maxWidth: .infinity, minHeight: 52)
                .background(theme.accent, in: RoundedRectangle(cornerRadius: 16))
        }
        .buttonStyle(.plain)
    }

    // MARK: - Work

    private func saveQR() {
        guard let qrPayload, let qr = GroupQRCode.image(from: qrPayload, size: 1024) else { return }
        PHPhotoLibrary.requestAuthorization(for: .addOnly) { status in
            guard status == .authorized || status == .limited else { return }
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
                SetupContact(
                    id: $0.id,
                    name: $0.name,
                    subtitle: $0.subtitle,
                    phone: $0.phone,
                    email: $0.email
                )
            }
        } catch {
            permissionDenied = true
        }
    }
}

private struct SetupContact: Identifiable {
    let id: String
    let name: String
    let subtitle: String
    let phone: String
    let email: String
}
