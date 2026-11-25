import Photos
import SwiftUI
import UIKit

/// Trading / Distribution Invite & QR — APK `BusinessInviteQr.kt` parity.
/// Role chips remint via `POST /v1/company/invites`; Send opens WhatsApp with the link.
struct BusinessInviteQrView: View {
    let momentId: String
    let title: String
    let viewerRole: String?
    var companyIdHint: String? = nil
    var accent: Color = Color(red: 0.06, green: 0.73, blue: 0.51)
    var distribution: Bool = false
    var showIntroHeader: Bool = true
    var onError: (String) -> Void = { _ in }

    @State private var companyId: String?
    @State private var roleCode = "OPERATIONS_LEAD"
    @State private var roleLabel = "Operations"
    @State private var invite: CompanyInvite?
    @State private var minting = false
    @State private var mintError: String?
    @State private var query = ""
    @State private var contacts: [GroupContactsLoader.Row] = []
    @State private var contactsDenied = false
    @State private var contactsLoaded = false
    @State private var copied = false
    @State private var saved = false
    @State private var showShare = false
    @State private var shareItems: [Any] = []

    private var roles: [(code: String, label: String)] {
        distribution
            ? [
                ("FINANCE", "Manager"),
                ("APPROVER", "Warehouse"),
                ("OPERATIONS_LEAD", "Route lead"),
                ("CONTRIBUTOR", "Salesman"),
            ]
            : [
                ("FINANCE", "Finance"),
                ("APPROVER", "Approver"),
                ("OPERATIONS_LEAD", "Operations"),
                ("CONTRIBUTOR", "Contributor"),
            ]
    }

    private var canInvite: Bool {
        (viewerRole ?? "").caseInsensitiveCompare("OWNER_ADMIN") == .orderedSame
    }

    private var displayName: String {
        let t = title.trimmingCharacters(in: .whitespacesAndNewlines)
        return t.isEmpty ? "this moment" : t
    }

    private var invitePath: String? {
        guard let invite else { return nil }
        let path = invite.invitePath.trimmingCharacters(in: .whitespacesAndNewlines)
        if !path.isEmpty { return path }
        let url = invite.inviteUrl.trimmingCharacters(in: .whitespacesAndNewlines)
        if !url.isEmpty { return url }
        let code = invite.inviteCode.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !code.isEmpty else { return nil }
        return CompanyJoinLink.displayPath(code: code)
    }

    private var shareBody: String? {
        invitePath.map { InviteOutboundShare.inviteMessage(title: displayName, url: $0) }
    }

    private var qrPayload: String? {
        let code = invite?.inviteCode.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        guard !code.isEmpty else { return nil }
        return CompanyJoinLink.qrPayload(code: code)
    }

    private var shownContacts: [GroupContactsLoader.Row] {
        let q = query.trimmingCharacters(in: .whitespacesAndNewlines)
        let filtered: [GroupContactsLoader.Row]
        if q.isEmpty {
            filtered = contacts
        } else {
            filtered = contacts.filter {
                $0.name.localizedCaseInsensitiveContains(q)
                    || $0.subtitle.localizedCaseInsensitiveContains(q)
                    || $0.email.localizedCaseInsensitiveContains(q)
                    || $0.phone.localizedCaseInsensitiveContains(q)
            }
        }
        return Array(filtered.prefix(q.isEmpty ? 6 : 20))
    }

    private var field: Color { Color(red: 0.07, green: 0.09, blue: 0.14) }
    private var chipStroke: Color { Color.white.opacity(0.18) }
    private var muted: Color { Color.white.opacity(0.55) }
    private var faint: Color { Color.white.opacity(0.42) }

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            if showIntroHeader { introCard }
            roleSection
            if canInvite {
                searchField
                contactsSection
                linkSection
                qrSection
            } else {
                ownerOnlyBanner
            }
            footnote
        }
        .task(id: momentId) { await resolveCompanyId() }
        .task(id: "\(companyId ?? "")|\(roleCode)|\(canInvite)") { await remint() }
        .task { await loadContacts() }
        .sheet(isPresented: $showShare) {
            ActivityShareSheet(items: shareItems)
        }
        .onAppear {
            if let first = roles.first(where: { $0.code == roleCode }) {
                roleLabel = first.label
            } else if let first = roles.first {
                roleCode = first.code
                roleLabel = first.label
            }
        }
    }

    private var introCard: some View {
        HStack(alignment: .top, spacing: 10) {
            VStack(alignment: .leading, spacing: 4) {
                Text("Invite the team")
                    .font(.system(size: 16, weight: .bold))
                    .foregroundStyle(.white)
                Text("\(displayName) · \(Self.viewerRoleLabel(viewerRole)) · invite grants a role")
                    .font(.system(size: 11, weight: .medium))
                    .foregroundStyle(accent.opacity(0.9))
                    .fixedSize(horizontal: false, vertical: true)
            }
            Spacer(minLength: 0)
            if !canInvite {
                Text("Owner only")
                    .font(.system(size: 10, weight: .bold))
                    .foregroundStyle(Color(red: 0.96, green: 0.72, blue: 0.22))
                    .padding(.horizontal, 9)
                    .padding(.vertical, 4)
                    .background(Color(red: 0.96, green: 0.72, blue: 0.22).opacity(0.16), in: Capsule())
            }
        }
        .padding(14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(field, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .stroke(accent.opacity(0.22), lineWidth: 1)
        )
    }

    private var roleSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("ROLE FOR THE INVITE")
                .font(.system(size: 10, weight: .bold))
                .tracking(0.6)
                .foregroundStyle(faint)
            FlowLayout(spacing: 6) {
                ForEach(roles, id: \.label) { role in
                    let on = role.label == roleLabel
                    Button {
                        guard canInvite else { return }
                        roleCode = role.code
                        roleLabel = role.label
                    } label: {
                        Text(role.label)
                            .font(.system(size: 12, weight: on ? .bold : .medium))
                            .foregroundStyle(on ? Color(red: 0.04, green: 0.05, blue: 0.09) : faint)
                            .padding(.horizontal, 10)
                            .padding(.vertical, 7)
                            .background(on ? accent : field, in: Capsule())
                            .overlay(Capsule().stroke(on ? accent : chipStroke, lineWidth: 1))
                    }
                    .buttonStyle(.plain)
                    .disabled(!canInvite)
                }
            }
            Text("Access: \(Self.roleAccess(roleCode, distribution: distribution)).")
                .font(.system(size: 11))
                .foregroundStyle(faint)
            if let mintError {
                Text(mintError)
                    .font(.system(size: 11))
                    .foregroundStyle(Color(red: 0.95, green: 0.35, blue: 0.35))
            }
        }
    }

    private var searchField: some View {
        TextField(
            "",
            text: $query,
            prompt: Text("Search contacts, email or phone").foregroundStyle(faint)
        )
        .font(.system(size: 13))
        .foregroundStyle(.white)
        .padding(.horizontal, 12)
        .padding(.vertical, 11)
        .background(field, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 12, style: .continuous)
                .stroke(chipStroke, lineWidth: 1)
        )
    }

    @ViewBuilder
    private var contactsSection: some View {
        Text("FROM YOUR CONTACTS")
            .font(.system(size: 10, weight: .bold))
            .tracking(0.6)
            .foregroundStyle(faint)
        if contactsDenied {
            Text("Contacts access is off. Share the invite link instead — it works the same.")
                .font(.system(size: 11))
                .foregroundStyle(muted)
        } else if !contactsLoaded {
            ProgressView().tint(accent)
        } else if shownContacts.isEmpty {
            Text(
                contacts.isEmpty
                    ? "No contacts on this device. Share the link or the QR below."
                    : "No contact matches that search."
            )
            .font(.system(size: 11))
            .foregroundStyle(muted)
        } else {
            VStack(spacing: 0) {
                ForEach(Array(shownContacts.enumerated()), id: \.element.id) { index, person in
                    HStack(spacing: 10) {
                        initialsAvatar(person.name)
                        VStack(alignment: .leading, spacing: 2) {
                            Text(person.name.isEmpty ? "Unnamed contact" : person.name)
                                .font(.system(size: 13, weight: .medium))
                                .foregroundStyle(.white)
                                .lineLimit(1)
                            Text(person.subtitle)
                                .font(.system(size: 11))
                                .foregroundStyle(muted)
                                .lineLimit(1)
                        }
                        Spacer(minLength: 0)
                        Button {
                            guard let shareBody else { return }
                            InviteOutboundShare.sendWhatsApp(phone: person.phone.isEmpty ? nil : person.phone, message: shareBody)
                        } label: {
                            Text("Send")
                                .font(.system(size: 11, weight: .bold))
                                .foregroundStyle(shareBody == nil ? faint : accent)
                                .padding(.horizontal, 10)
                                .padding(.vertical, 6)
                                .overlay(
                                    Capsule().stroke(
                                        shareBody == nil ? chipStroke : accent.opacity(0.55),
                                        lineWidth: 1
                                    )
                                )
                        }
                        .buttonStyle(.plain)
                        .disabled(shareBody == nil)
                    }
                    .padding(.vertical, 9)
                    if index < shownContacts.count - 1 {
                        Rectangle()
                            .fill(Color.white.opacity(0.08))
                            .frame(height: 1)
                    }
                }
            }
            .padding(.horizontal, 10)
            .background(field, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
        }
    }

    @ViewBuilder
    private var linkSection: some View {
        Text("OR SHARE INVITE LINK")
            .font(.system(size: 10, weight: .bold))
            .tracking(0.6)
            .foregroundStyle(faint)
        if minting && invite == nil {
            ProgressView().tint(accent)
        } else if let invitePath {
            HStack(spacing: 8) {
                Text(invitePath)
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundStyle(accent.opacity(0.95))
                    .lineLimit(1)
                    .minimumScaleFactor(0.7)
                Spacer(minLength: 0)
                Button {
                    UIPasteboard.general.string = invitePath
                    copied = true
                } label: {
                    Text(copied ? "Copied" : "Copy")
                        .font(.system(size: 11, weight: .bold))
                        .foregroundStyle(accent)
                        .padding(.horizontal, 10)
                        .padding(.vertical, 6)
                        .overlay(Capsule().stroke(accent.opacity(0.55), lineWidth: 1))
                }
                .buttonStyle(.plain)
            }
            .padding(.horizontal, 10)
            .padding(.vertical, 10)
            .background(field, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
        } else if companyId == nil {
            Text("This moment is not linked to a company yet, so there is nothing to invite into.")
                .font(.system(size: 11))
                .foregroundStyle(muted)
        }
    }

    private var qrSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("OR SCAN TO JOIN")
                .font(.system(size: 10, weight: .bold))
                .tracking(0.6)
                .foregroundStyle(faint)
            VStack(spacing: 10) {
                if let qrPayload, let qr = GroupQRCode.image(from: qrPayload, size: 168) {
                    Image(uiImage: qr)
                        .interpolation(.none)
                        .resizable()
                        .frame(width: 168, height: 168)
                        .padding(10)
                        .background(Color.white, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
                } else {
                    VStack(alignment: .leading, spacing: 4) {
                        Text("QR code needs an invite token")
                            .font(.system(size: 13, weight: .medium))
                            .foregroundStyle(.white)
                        Text("The QR is drawn from the minted invite code — never from a placeholder.")
                            .font(.system(size: 11))
                            .foregroundStyle(muted)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(14)
                    .background(field, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
                    .overlay(
                        RoundedRectangle(cornerRadius: 12, style: .continuous)
                            .stroke(chipStroke, lineWidth: 1)
                    )
                }
                Text("\(displayName) · joins as \(roleLabel.isEmpty ? Self.viewerRoleLabel(invite?.operatingRole ?? roleCode) : roleLabel) by default")
                    .font(.system(size: 10))
                    .foregroundStyle(muted)
                    .multilineTextAlignment(.center)
                HStack(spacing: 10) {
                    outlineButton(saved ? "Saved" : "Save to Photos", enabled: qrPayload != nil) {
                        saveQrToPhotos()
                    }
                    outlineButton("Share QR", enabled: qrPayload != nil) {
                        if let qrPayload, let qr = GroupQRCode.image(from: qrPayload, size: 512) {
                            shareItems = [qr, invitePath as Any].compactMap { $0 }
                            showShare = true
                        }
                    }
                }
            }
            .padding(12)
            .frame(maxWidth: .infinity)
            .background(field, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 16, style: .continuous)
                    .stroke(accent.opacity(0.4), lineWidth: 1)
            )
        }
    }

    private var ownerOnlyBanner: some View {
        Text("Invites are owner-only. Ask the owner or an admin for the link — no role is granted without them.")
            .font(.system(size: 11))
            .foregroundStyle(muted)
            .padding(14)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(field, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 12, style: .continuous)
                    .stroke(chipStroke, lineWidth: 1)
            )
    }

    private var footnote: some View {
        Text("Invite grants a role and its access. Joining ≠ approval ≠ payment.")
            .font(.system(size: 11))
            .foregroundStyle(muted)
            .padding(10)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(accent.opacity(0.10), in: RoundedRectangle(cornerRadius: 10, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 10, style: .continuous)
                    .stroke(accent.opacity(0.35), lineWidth: 1)
            )
    }

    private func initialsAvatar(_ name: String) -> some View {
        let initials = name.split(separator: " ").prefix(2).compactMap { $0.first.map(String.init) }.joined()
        return Text(initials.isEmpty ? "?" : initials.uppercased())
            .font(.system(size: 12, weight: .bold))
            .foregroundStyle(accent.opacity(0.9))
            .frame(width: 32, height: 32)
            .background(accent.opacity(0.16), in: Circle())
    }

    private func outlineButton(_ title: String, enabled: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(title)
                .font(.system(size: 12, weight: .bold))
                .foregroundStyle(enabled ? accent : faint)
                .frame(maxWidth: .infinity, minHeight: 36)
                .overlay(
                    RoundedRectangle(cornerRadius: 12, style: .continuous)
                        .stroke(enabled ? accent.opacity(0.55) : chipStroke, lineWidth: 1)
                )
        }
        .buttonStyle(.plain)
        .disabled(!enabled)
    }

    private func resolveCompanyId() async {
        if let hint = companyIdHint?.trimmingCharacters(in: .whitespacesAndNewlines), !hint.isEmpty {
            companyId = hint
            return
        }
        do {
            let moments = try await APIClient.shared.listBusinessMoments(limit: 50)
            companyId = moments.first(where: { $0.momentId == momentId })?.companyId
        } catch {
            companyId = nil
        }
    }

    private func remint() async {
        guard canInvite, let companyId, !companyId.isEmpty else { return }
        minting = true
        mintError = nil
        defer { minting = false }
        do {
            let minted = try await APIClient.shared.mintCompanyInvite(
                companyId: companyId,
                membershipType: Self.membership(for: roleCode),
                operatingRole: roleCode,
                idempotencyKey: UUID().uuidString
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

    private func loadContacts() async {
        do {
            contacts = try await GroupContactsLoader.fetchRowsIfAuthorized()
            contactsDenied = false
        } catch {
            contactsDenied = true
            contacts = []
        }
        contactsLoaded = true
    }

    private func saveQrToPhotos() {
        guard let qrPayload, let qr = GroupQRCode.image(from: qrPayload, size: 512) else { return }
        PHPhotoLibrary.requestAuthorization(for: .addOnly) { status in
            guard status == .authorized || status == .limited else {
                DispatchQueue.main.async {
                    // Fall back to share sheet when Photos add is denied.
                    shareItems = [qr, invitePath as Any].compactMap { $0 }
                    showShare = true
                }
                return
            }
            PHPhotoLibrary.shared().performChanges({
                PHAssetChangeRequest.creationRequestForAsset(from: qr)
            }) { ok, error in
                DispatchQueue.main.async {
                    if ok {
                        saved = true
                    } else {
                        onError(error?.localizedDescription ?? "Could not save the QR to Photos")
                    }
                }
            }
        }
    }

    static func membership(for role: String) -> String {
        role == "FINANCE" || role == "APPROVER" ? "ADMIN" : "MEMBER"
    }

    static func roleAccess(_ role: String, distribution: Bool) -> String {
        if distribution {
            switch role {
            case "FINANCE": return "money, settlements and finance boards"
            case "APPROVER": return "warehouse stock, GRNs and holds"
            case "OPERATIONS_LEAD": return "routes, dispatches, dealers and orders"
            default: return "records route activity; money waits for approval"
            }
        }
        switch role {
        case "FINANCE": return "money, payments and finance boards"
        case "APPROVER": return "approves money and stock adjustments"
        case "OPERATIONS_LEAD": return "sales, stock, purchases and orders"
        default: return "records activity; money waits for approval"
        }
    }

    static func viewerRoleLabel(_ role: String?) -> String {
        switch (role ?? "").uppercased() {
        case "OWNER_ADMIN": return "Owner admin"
        case "FINANCE": return "Finance"
        case "APPROVER": return "Approver"
        case "OPERATIONS_LEAD": return "Operations"
        case "CONTRIBUTOR": return "Contributor"
        default:
            guard let role, !role.isEmpty else { return "Member" }
            return role.lowercased().replacingOccurrences(of: "_", with: " ").capitalized
        }
    }
}

/// Simple wrapping chip row (APK FlowRow).
private struct FlowLayout: Layout {
    var spacing: CGFloat = 6

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let result = arrange(proposal: proposal, subviews: subviews)
        return result.size
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        let result = arrange(proposal: proposal, subviews: subviews)
        for (index, origin) in result.origins.enumerated() {
            subviews[index].place(
                at: CGPoint(x: bounds.minX + origin.x, y: bounds.minY + origin.y),
                proposal: .unspecified
            )
        }
    }

    private func arrange(proposal: ProposedViewSize, subviews: Subviews) -> (size: CGSize, origins: [CGPoint]) {
        let maxWidth = proposal.width ?? .infinity
        var origins: [CGPoint] = []
        var x: CGFloat = 0
        var y: CGFloat = 0
        var rowHeight: CGFloat = 0
        var width: CGFloat = 0
        for sub in subviews {
            let size = sub.sizeThatFits(.unspecified)
            if x + size.width > maxWidth, x > 0 {
                x = 0
                y += rowHeight + spacing
                rowHeight = 0
            }
            origins.append(CGPoint(x: x, y: y))
            rowHeight = max(rowHeight, size.height)
            x += size.width + spacing
            width = max(width, x - spacing)
        }
        return (CGSize(width: width, height: y + rowHeight), origins)
    }
}
