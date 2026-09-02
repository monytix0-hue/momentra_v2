import Photos
import SwiftUI
import UIKit

/// Figma 579:12741 — Popup / Add People bottom sheet.
struct GroupAddPeopleSheet: View {
    let palette: GroupTypePalette
    let experienceTitle: String
    let typeCode: String
    let existingNames: [String]
    var issuedInvitePath: String? = nil
    var issuedInviteUrl: String? = nil
    var issuedInviteCode: String? = nil
    var onAdd: (GroupInviteeDraft) -> Void
    var onDismiss: () -> Void

    @State private var query = ""
    @State private var contacts: [SheetContact] = []
    @State private var permissionDenied = false
    @State private var copied = false
    @State private var saved = false
    @State private var shareItems: [Any] = []
    @State private var showShare = false

    private var inviteCode: String? {
        let minted = issuedInviteCode?.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        if let minted, !minted.isEmpty { return minted }
        return issuedInvitePath?.split(separator: "/").last.map(String.init)
    }

    private var invitePath: String {
        if let inviteCode { return GroupInviteLink.displayPath(code: inviteCode) }
        return "Getting a short invite…"
    }

    private var copyText: String? {
        inviteCode.map { GroupInviteLink.copyText(code: $0) }
    }

    private var qrPayload: String? {
        inviteCode.map { GroupInviteLink.qrPayload(code: $0) }
    }

    private var existingLower: Set<String> {
        Set(existingNames.map { $0.lowercased() })
    }

    private var filtered: [SheetContact] {
        let q = query.trimmingCharacters(in: .whitespacesAndNewlines)
        let source: [SheetContact]
        if q.isEmpty {
            source = displayContacts
        } else {
            source = displayContacts.filter {
                $0.name.localizedCaseInsensitiveContains(q) || $0.subtitle.localizedCaseInsensitiveContains(q)
            }
        }
        if q.isEmpty && source.count > Self.contactPreviewLimit {
            return Array(source.prefix(Self.contactPreviewLimit))
        }
        return source
    }

    private var filteredAllCount: Int {
        let q = query.trimmingCharacters(in: .whitespacesAndNewlines)
        if q.isEmpty { return displayContacts.count }
        return displayContacts.filter {
            $0.name.localizedCaseInsensitiveContains(q) || $0.subtitle.localizedCaseInsensitiveContains(q)
        }.count
    }

    private var hasMoreContacts: Bool {
        query.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
            && filteredAllCount > Self.contactPreviewLimit
    }

    private static let contactPreviewLimit = 5

    private var displayContacts: [SheetContact] {
        if !permissionDenied && !contacts.isEmpty { return contacts }
        return Self.figmaDemoContacts
    }

    private static let figmaDemoContacts: [SheetContact] = [
        .init(
            id: "demo-rahul",
            name: "Rahul Mehta",
            subtitle: "+91 98765 43210",
            photo: nil,
            email: "",
            phone: "+91 98765 43210",
            avatarName: "gap_demo_rahul"
        ),
        .init(
            id: "demo-priya",
            name: "Priya Singh",
            subtitle: "priya.singh@gmail.com",
            photo: nil,
            email: "priya.singh@gmail.com",
            phone: "",
            avatarName: "gap_demo_priya"
        ),
        .init(
            id: "demo-kavita",
            name: "Kavita Joshi",
            subtitle: "kavita.j@yahoo.com",
            photo: nil,
            email: "kavita.j@yahoo.com",
            phone: "",
            avatarName: "gap_demo_kavita"
        ),
    ]

    private var typedQuery: String {
        query.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    private var showTypedAdd: Bool {
        !typedQuery.isEmpty
            && !existingLower.contains(typedQuery.lowercased())
            && !displayContacts.contains(where: { $0.name.compare(typedQuery, options: .caseInsensitive) == .orderedSame })
    }

    var body: some View {
        VStack(spacing: 16) {
            header
            searchField
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    contactsBlock
                    if hasMoreContacts {
                        Text("Search to find more contacts")
                            .font(.plusJakarta(size: 12))
                            .foregroundStyle(GroupSetupTheme.textSecondary)
                            .padding(.top, 4)
                    }
                    inviteLinkBlock
                    Divider().overlay(GroupSetupTheme.border)
                    qrBlock
                    shareSaveRow
                }
            }
            doneButton
        }
        .padding(.top, 12)
        .padding(.horizontal, 16)
        .padding(.bottom, 24)
        .background(GroupSetupTheme.card)
        .task { await loadContacts() }
        .sheet(isPresented: $showShare) {
            ActivityShareSheet(items: shareItems)
        }
    }

    private var header: some View {
        VStack(spacing: 16) {
            Capsule()
                .fill(GroupSetupTheme.border)
                .frame(width: 36, height: 4)
            HStack {
                Text("Add People")
                    .font(.plusJakarta(size: 18, weight: .bold))
                    .foregroundStyle(GroupSetupTheme.textPrimary)
                Spacer()
                Button(action: onDismiss) {
                    ZStack {
                        Circle().fill(GroupSetupTheme.iconSurface).frame(width: 32, height: 32)
                        Image("ges_icon_x_circle")
                            .renderingMode(.template)
                            .resizable()
                            .scaledToFit()
                            .frame(width: 14, height: 14)
                            .foregroundStyle(GroupSetupTheme.textSecondary)
                    }
                }
                .buttonStyle(.plain)
                .accessibilityLabel("Close")
            }
        }
    }

    private var searchField: some View {
        HStack(spacing: 10) {
            Image("ges_icon_search")
                .renderingMode(.template)
                .resizable()
                .scaledToFit()
                .frame(width: 16, height: 16)
                .foregroundStyle(GroupSetupTheme.textSecondary)
            TextField("", text: $query, prompt: Text("Search contacts, email or phone")
                .font(.plusJakarta(size: 13))
                .foregroundStyle(GroupSetupTheme.textSecondary))
                .font(.plusJakarta(size: 13))
                .foregroundStyle(GroupSetupTheme.textPrimary)
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 12)
        .background(GroupSetupTheme.bg, in: RoundedRectangle(cornerRadius: 14))
        .overlay(RoundedRectangle(cornerRadius: 14).stroke(GroupSetupTheme.border, lineWidth: 1))
    }

    private var contactsBlock: some View {
        VStack(alignment: .leading, spacing: 4) {
                Text("From Your Contacts".uppercased())
                    .font(.plusJakarta(size: 11, weight: .bold))
                    .foregroundStyle(palette.accent)
                if filtered.isEmpty && !showTypedAdd {
                    Text("No matching contacts.")
                        .font(.plusJakarta(size: 12))
                        .foregroundStyle(GroupSetupTheme.textSecondary)
                        .padding(.vertical, 8)
                }
                ForEach(Array(filtered.enumerated()), id: \.element.id) { index, contact in
                    if index > 0 { Divider().overlay(GroupSetupTheme.border) }
                    contactRow(
                        name: contact.name,
                        subtitle: contact.subtitle.isEmpty ? "Contact" : contact.subtitle,
                        photo: contact.photo,
                        added: existingLower.contains(contact.name.lowercased()),
                        avatarName: contact.avatarName,
                        onAdd: {
                            onAdd(GroupInviteeDraft(
                                name: contact.name,
                                photo: contact.photo,
                                email: contact.email.isEmpty ? nil : contact.email,
                                phone: contact.phone.isEmpty ? nil : contact.phone,
                                avatarName: contact.avatarName
                            ))
                        }
                    )
                }
                if showTypedAdd {
                    if !filtered.isEmpty { Divider().overlay(GroupSetupTheme.border) }
                    contactRow(
                        name: typedQuery,
                        subtitle: "Add from search",
                        photo: nil,
                        added: false,
                        onAdd: {
                            let looksEmail = typedQuery.contains("@")
                            let digits = typedQuery.filter(\.isNumber)
                            let looksPhone = digits.count >= 7
                            onAdd(GroupInviteeDraft(
                                name: typedQuery,
                                photo: nil,
                                email: looksEmail ? typedQuery : nil,
                                phone: looksPhone && !looksEmail ? typedQuery : nil
                            ))
                            query = ""
                        }
                    )
                }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private func contactRow(
        name: String,
        subtitle: String,
        photo: UIImage?,
        added: Bool,
        avatarName: String? = nil,
        onAdd: @escaping () -> Void
    ) -> some View {
        HStack(spacing: 12) {
            GroupSheetAvatar(name: name, photo: photo, assetName: avatarName, size: 40)
            VStack(alignment: .leading, spacing: 2) {
                Text(name)
                    .font(.plusJakarta(size: 14, weight: .semibold))
                    .foregroundStyle(GroupSetupTheme.textPrimary)
                    .lineLimit(1)
                Text(subtitle)
                    .font(.plusJakarta(size: 12))
                    .foregroundStyle(GroupSetupTheme.textSecondary)
                    .lineLimit(1)
            }
            Spacer(minLength: 8)
            Button(action: onAdd) {
                Text(added ? "Added" : "+ Add")
                    .font(.plusJakarta(size: 12, weight: .bold))
                    .foregroundStyle(added ? GroupSetupTheme.textSecondary : palette.accent)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 6)
                    .background((added ? GroupSetupTheme.border : palette.accent).opacity(added ? 0.2 : 0.13), in: Capsule())
                    .overlay(Capsule().stroke((added ? GroupSetupTheme.border : palette.accent).opacity(added ? 0.35 : 0.2), lineWidth: 1))
            }
            .buttonStyle(.plain)
            .disabled(added)
        }
        .padding(.vertical, 8)
    }

    private var inviteLinkBlock: some View {
        VStack(alignment: .leading, spacing: 8) {
            Divider().overlay(GroupSetupTheme.border)
            Text("Or share invite link")
                .font(.plusJakarta(size: 12, weight: .semibold))
                .foregroundStyle(GroupSetupTheme.textSecondary)
            HStack(spacing: 8) {
                Text(invitePath)
                    .font(.plusJakarta(size: 12))
                    .foregroundStyle(GroupSetupTheme.textSecondary)
                    .lineLimit(1)
                    .truncationMode(.tail)
                Spacer(minLength: 8)
                Button {
                    UIPasteboard.general.string = copyText
                    copied = true
                    DispatchQueue.main.asyncAfter(deadline: .now() + 1.6) { copied = false }
                } label: {
                    Text(copied ? "Copied" : "Copy")
                        .font(.plusJakarta(size: 12, weight: .bold))
                        .foregroundStyle(GroupSetupTheme.ctaText)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 8)
                        .background(palette.accent, in: RoundedRectangle(cornerRadius: 10))
                }
                .buttonStyle(.plain)
                .disabled(copyText == nil)
            }
            .padding(.leading, 12)
            .padding(.trailing, 4)
            .padding(.vertical, 4)
            .background(GroupSetupTheme.bg, in: RoundedRectangle(cornerRadius: 12))
            .overlay(RoundedRectangle(cornerRadius: 12).stroke(GroupSetupTheme.border, lineWidth: 1))
        }
    }

    private var qrBlock: some View {
        VStack(spacing: 12) {
            Text("Or scan to join".uppercased())
                .font(.plusJakarta(size: 11, weight: .bold))
                .foregroundStyle(palette.accent)
            Group {
                if let qrPayload, let qr = GroupQRCode.image(from: qrPayload, size: 144) {
                    Image(uiImage: qr)
                        .interpolation(.none)
                        .resizable()
                        .frame(width: 144, height: 144)
                } else {
                    ProgressView()
                        .tint(palette.accent)
                        .frame(width: 144, height: 144)
                }
            }
            .padding(16)
            .background(Color.white, in: RoundedRectangle(cornerRadius: 16))
            Text("Scan with Momentra app to join instantly")
                .font(.plusJakarta(size: 11))
                .foregroundStyle(GroupSetupTheme.textSecondary)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity)
    }

    private var shareSaveRow: some View {
        HStack(spacing: 10) {
            outlineButton(icon: "ges_icon_share", title: "Share QR", enabled: qrPayload != nil && copyText != nil) {
                if let qrPayload, let qr = GroupQRCode.image(from: qrPayload, size: 512) {
                    shareItems = [qr, copyText as Any].compactMap { $0 }
                    showShare = true
                }
            }
            outlineButton(icon: "ges_icon_download", title: saved ? "Saved" : "Save to Photos", enabled: qrPayload != nil) {
                saveQR()
            }
        }
    }

    private func outlineButton(icon: String, title: String, enabled: Bool = true, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            HStack(spacing: 6) {
                Image(icon)
                    .renderingMode(.template)
                    .resizable()
                    .scaledToFit()
                    .frame(width: 14, height: 14)
                Text(title)
                    .font(.plusJakarta(size: 13, weight: .bold))
                    .lineLimit(1)
            }
            .foregroundStyle(palette.accent.opacity(enabled ? 1 : 0.35))
            .frame(maxWidth: .infinity, minHeight: 44)
            .overlay(RoundedRectangle(cornerRadius: 14).stroke(palette.accent.opacity(enabled ? 1 : 0.35), lineWidth: 1))
        }
        .buttonStyle(.plain)
        .disabled(!enabled)
    }

    private var doneButton: some View {
        Button(action: onDismiss) {
            Text("Done")
                .font(.plusJakarta(size: 16, weight: .heavy))
                .foregroundStyle(GroupSetupTheme.ctaText)
                .frame(maxWidth: .infinity, minHeight: 52)
                .background(palette.accentGradient, in: RoundedRectangle(cornerRadius: 16))
        }
        .buttonStyle(.plain)
    }

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
            contacts = rows.map { row in
                SheetContact(
                    id: row.id,
                    name: row.name,
                    subtitle: row.subtitle,
                    photo: row.photoData.flatMap(UIImage.init(data:)),
                    email: row.email,
                    phone: row.phone
                )
            }
        } catch {
            permissionDenied = true
        }
    }
}

struct GroupInviteeDraft {
    let name: String
    let photo: UIImage?
    let email: String?
    let phone: String?
    var avatarName: String? = nil
}

private struct SheetContact: Identifiable {
    let id: String
    let name: String
    let subtitle: String
    let photo: UIImage?
    let email: String
    let phone: String
    var avatarName: String? = nil
}

struct GroupSheetAvatar: View {
    let name: String
    var photo: UIImage? = nil
    var assetName: String? = nil
    var useInitials: Bool = false
    var size: CGFloat = 44

    var body: some View {
        Group {
            if let photo {
                Image(uiImage: photo).resizable().scaledToFill()
            } else if useInitials {
                initialsView
            } else if let assetName {
                Image(assetName).resizable().scaledToFill()
            } else {
                initialsView
            }
        }
        .frame(width: size, height: size)
        .clipShape(RoundedRectangle(cornerRadius: size / 2))
    }

    private var initialsView: some View {
        ZStack {
            GroupSetupTheme.iconSurface
            Text(String(name.trimmingCharacters(in: .whitespaces).prefix(1)).uppercased())
                .font(.plusJakarta(size: size * 0.38, weight: .bold))
                .foregroundStyle(GroupSetupTheme.textPrimary)
        }
    }
}

enum GroupQRCode {
    static func image(from string: String, size: CGFloat) -> UIImage? {
        guard let data = string.data(using: .ascii),
              let filter = CIFilter(name: "CIQRCodeGenerator") else { return nil }
        filter.setValue(data, forKey: "inputMessage")
        filter.setValue("M", forKey: "inputCorrectionLevel")
        guard let output = filter.outputImage else { return nil }
        let scale = size / output.extent.width
        let scaled = output.transformed(by: CGAffineTransform(scaleX: scale, y: scale))
        let colorFilter = CIFilter(name: "CIFalseColor")
        colorFilter?.setValue(scaled, forKey: "inputImage")
        colorFilter?.setValue(CIColor(red: 20 / 255, green: 18 / 255, blue: 27 / 255), forKey: "inputColor0")
        colorFilter?.setValue(CIColor(color: .white), forKey: "inputColor1")
        guard let colored = colorFilter?.outputImage else { return nil }
        let context = CIContext()
        guard let cg = context.createCGImage(colored, from: colored.extent) else { return nil }
        return UIImage(cgImage: cg)
    }
}
