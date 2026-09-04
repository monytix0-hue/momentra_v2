import SwiftUI
import UIKit

struct ActivityShareSheet: UIViewControllerRepresentable {
    let items: [Any]

    func makeUIViewController(context: Context) -> UIActivityViewController {
        UIActivityViewController(activityItems: items, applicationActivities: nil)
    }

    func updateUIViewController(_ uiViewController: UIActivityViewController, context: Context) {}
}

/// Trip invite link row + scannable QR + share actions (Figma 581:13699).
struct TripInviteShareSection: View {
    let minting: Bool
    let displayPath: String?
    let mintError: String?
    @Binding var copied: Bool
    let copyText: String?
    let qrPayload: String?
    var momentTitle: String = "Moment"
    var showMessageChannels: Bool = true

    @State private var shareItems: [Any] = []
    @State private var showShare = false

    private var inviteBody: String? {
        guard let copyText else { return nil }
        return InviteOutboundShare.inviteMessage(title: momentTitle, url: copyText)
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            VStack(alignment: .leading, spacing: 6) {
                TripFieldLabel(text: "Share Invitation Link")
                inviteLinkBlock
            }
            if showMessageChannels {
                InviteSendChannelButtons(
                    enabled: inviteBody != nil && !minting,
                    accent: TripForm.accent,
                    onMessages: {
                        if let body = inviteBody {
                            InviteOutboundShare.sendSms(phone: nil, message: body)
                        }
                    },
                    onWhatsApp: {
                        if let body = inviteBody {
                            InviteOutboundShare.sendWhatsApp(phone: nil, message: body)
                        }
                    }
                )
            }
            qrBlock
            shareQrButton
        }
        .sheet(isPresented: $showShare) {
            ActivityShareSheet(items: shareItems)
        }
    }

    @ViewBuilder
    private var inviteLinkBlock: some View {
        if minting {
            HStack(spacing: 10) {
                ProgressView().tint(TripForm.purple)
                Text("Minting invite…")
                    .font(.plusJakarta(size: 13))
                    .foregroundStyle(TripForm.muted)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(12)
            .background(TripForm.field)
            .overlay(RoundedRectangle(cornerRadius: 8).stroke(TripForm.border))
            .clipShape(RoundedRectangle(cornerRadius: 8))
        } else if let displayPath, let copyText {
            HStack(spacing: 0) {
                Text(displayPath)
                    .font(.plusJakarta(size: 13))
                    .foregroundStyle(TripForm.text)
                    .lineLimit(1)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 12)
                    .frame(maxWidth: .infinity, alignment: .leading)
                Button {
                    UIPasteboard.general.string = copyText
                    copied = true
                } label: {
                    Text(copied ? "Copied" : "Copy")
                        .font(.plusJakarta(size: 12, weight: .bold))
                        .foregroundStyle(TripForm.text)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 8)
                        .background(TripForm.accent)
                        .clipShape(RoundedRectangle(cornerRadius: 8))
                }
                .buttonStyle(.plain)
                .padding(.leading, 4)
                Button {
                    shareItems = [copyText]
                    showShare = true
                } label: {
                    Text("Share")
                        .font(.plusJakarta(size: 12, weight: .bold))
                        .foregroundStyle(TripForm.accent)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 8)
                        .overlay(RoundedRectangle(cornerRadius: 8).stroke(TripForm.accent))
                }
                .buttonStyle(.plain)
                .padding(4)
            }
            .background(TripForm.field)
            .overlay(RoundedRectangle(cornerRadius: 8).stroke(TripForm.border))
            .clipShape(RoundedRectangle(cornerRadius: 8))
        } else {
            Text(mintError ?? "Invite unavailable")
                .font(.plusJakarta(size: 12))
                .foregroundStyle(Color(hex: "#F87171"))
        }
    }

    private var qrBlock: some View {
        VStack(spacing: 12) {
            TripFieldLabel(text: "Or scan to join")
            Group {
                if minting {
                    ProgressView()
                        .tint(TripForm.accent)
                        .frame(width: 144, height: 144)
                } else if let qrPayload, let qr = GroupQRCode.image(from: qrPayload, size: 144) {
                    Image(uiImage: qr)
                        .interpolation(.none)
                        .resizable()
                        .frame(width: 144, height: 144)
                } else {
                    Text("QR unavailable")
                        .font(.plusJakarta(size: 12))
                        .foregroundStyle(TripForm.muted)
                        .frame(width: 144, height: 144)
                }
            }
            .padding(16)
            .background(Color.white, in: RoundedRectangle(cornerRadius: 16))
            Text("Scan with Momentra app to join instantly")
                .font(.plusJakarta(size: 11))
                .foregroundStyle(TripForm.muted)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity)
    }

    private var shareQrButton: some View {
        Button {
            guard let qrPayload, let copyText else { return }
            guard let qr = GroupQRCode.image(from: qrPayload, size: 512) else { return }
            shareItems = [qr, copyText]
            showShare = true
        } label: {
            HStack(spacing: 6) {
                Image(systemName: "square.and.arrow.up")
                    .font(.system(size: 14, weight: .semibold))
                Text("Share QR")
                    .font(.plusJakarta(size: 13, weight: .bold))
            }
            .foregroundStyle(TripForm.accent.opacity(qrPayload != nil && copyText != nil ? 1 : 0.35))
            .frame(maxWidth: .infinity, minHeight: 44)
            .overlay(RoundedRectangle(cornerRadius: 14).stroke(TripForm.accent.opacity(qrPayload != nil && copyText != nil ? 1 : 0.35)))
        }
        .buttonStyle(.plain)
        .disabled(qrPayload == nil || copyText == nil || minting)
    }
}
