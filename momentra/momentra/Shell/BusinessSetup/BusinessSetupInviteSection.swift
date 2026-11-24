import SwiftUI
import UIKit

/// Business-themed invite block with Group-style code / QR / share mechanics.
/// Mints via company invite APIs (`CompanyJoinLink`) while presenting Group QR UX.
/// When `embedded` is true (People & Roles), content renders without an outer SectionCard.
struct BusinessSetupInviteSection: View {
    let theme: BusinessSetupTheme
    let companyId: String?
    var embedded: Bool = true
    var mintInvite: (String) async -> CompanyInvite?

    @State private var invite: CompanyInvite?
    @State private var loading = false
    @State private var error: String?
    @State private var showShare = false
    @State private var shareItems: [Any] = []
    @State private var copied = false

    private var code: String? { invite?.inviteCode }
    private var displayPath: String? { code.map { CompanyJoinLink.displayPath(code: $0) } }
    private var copyText: String? { displayPath }
    private var qrPayload: String? { code.map { CompanyJoinLink.qrPayload(code: $0) } }

    var body: some View {
        Group {
            if embedded {
                VStack(alignment: .leading, spacing: 12) {
                    inviteHeader
                    inviteBody
                }
            } else {
                BusinessSetupDocumentKit.SectionCard(
                    theme: theme,
                    index: 99,
                    title: "Invite your team",
                    subtitle: "Share a link or QR so teammates can join this company."
                ) {
                    inviteBody
                }
            }
        }
        .task(id: companyId) {
            guard let companyId, !companyId.isEmpty, invite == nil else { return }
            await mint()
        }
        .sheet(isPresented: $showShare) {
            ActivityShareSheet(items: shareItems)
        }
    }

    private var inviteHeader: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text("Invite your team")
                .font(.system(size: 14, weight: .bold))
                .foregroundStyle(BusinessTokens.Colors.textPrimary)
            Text("Share a link or QR so teammates can join this company.")
                .font(.system(size: 12, weight: .medium))
                .foregroundStyle(theme.hint)
        }
    }

    @ViewBuilder
    private var inviteBody: some View {
        if let error {
            Text(error)
                .font(.system(size: 12, weight: .medium))
                .foregroundStyle(BusinessTokens.Colors.signalDanger)
        }

        if companyId == nil || companyId?.isEmpty == true {
            Text("Activate this moment to mint a company invite, or reopen setup after create.")
                .font(.system(size: 13, weight: .medium))
                .foregroundStyle(theme.hint)
        } else if invite != nil, let displayPath {
            codeBlock(displayPath)
            qrBlock
            shareRow
        } else {
            Button {
                Task { await mint() }
            } label: {
                HStack {
                    if loading { ProgressView().tint(theme.onAccent) }
                    Text(loading ? "Creating invite…" : "Create invite link")
                        .font(.system(size: 14, weight: .bold))
                }
                .foregroundStyle(theme.onAccent)
                .frame(maxWidth: .infinity, minHeight: 44)
                .background(theme.accent, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
            }
            .buttonStyle(.plain)
            .disabled(loading)
        }
    }

    private func codeBlock(_ path: String) -> some View {
        HStack(spacing: 10) {
            Text(path)
                .font(.system(size: 13, weight: .semibold, design: .monospaced))
                .foregroundStyle(BusinessTokens.Colors.textPrimary)
                .lineLimit(1)
                .minimumScaleFactor(0.7)
            Spacer(minLength: 0)
            Button {
                UIPasteboard.general.string = copyText
                copied = true
            } label: {
                Text(copied ? "Copied" : "Copy")
                    .font(.system(size: 12, weight: .bold))
                    .foregroundStyle(theme.accent)
            }
            .buttonStyle(.plain)
        }
        .padding(12)
        .background(theme.inset, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
    }

    private var qrBlock: some View {
        VStack(spacing: 10) {
            Text("Or scan to join".uppercased())
                .font(.system(size: 11, weight: .bold))
                .tracking(0.5)
                .foregroundStyle(theme.accent)
            Group {
                if let qrPayload, let qr = GroupQRCode.image(from: qrPayload, size: 144) {
                    Image(uiImage: qr)
                        .interpolation(.none)
                        .resizable()
                        .frame(width: 144, height: 144)
                } else {
                    ProgressView()
                        .tint(theme.accent)
                        .frame(width: 144, height: 144)
                }
            }
            .padding(16)
            .background(Color.white, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
            Text("Anyone who scans can join this company.")
                .font(.system(size: 11, weight: .medium))
                .foregroundStyle(theme.hint)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity)
    }

    private var shareRow: some View {
        HStack(spacing: 10) {
            outlineButton(title: "Share QR") {
                if let qrPayload, let qr = GroupQRCode.image(from: qrPayload, size: 512) {
                    shareItems = [qr, copyText as Any].compactMap { $0 }
                    showShare = true
                }
            }
            outlineButton(title: "Share link") {
                if let copyText {
                    shareItems = [copyText]
                    showShare = true
                }
            }
        }
    }

    private func outlineButton(title: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(title)
                .font(.system(size: 13, weight: .bold))
                .foregroundStyle(theme.accent)
                .frame(maxWidth: .infinity, minHeight: 44)
                .overlay(RoundedRectangle(cornerRadius: 14).stroke(theme.accent, lineWidth: 1))
        }
        .buttonStyle(.plain)
    }

    private func mint() async {
        guard let companyId, !companyId.isEmpty else { return }
        loading = true
        error = nil
        defer { loading = false }
        if let minted = await mintInvite(companyId) {
            invite = minted
        } else {
            error = "Couldn’t create invite. Try again."
        }
    }
}
