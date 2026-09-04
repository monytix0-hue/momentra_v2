import Foundation
import MessageUI
import SwiftUI
import UIKit

enum InviteOutboundShare {
    static func inviteMessage(title: String, url: String) -> String {
        let label = title.trimmingCharacters(in: .whitespacesAndNewlines)
        let name = label.isEmpty ? "this Moment" : label
        return "Join \(name) on Momentra: \(url)"
    }

    static func phoneDigits(_ phone: String?) -> String? {
        guard let phone, !phone.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { return nil }
        let digits = phone.filter(\.isNumber)
        return digits.count >= 8 ? digits : nil
    }

    static func looksLikePhone(_ raw: String?) -> Bool {
        guard let raw else { return false }
        let t = raw.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !t.isEmpty, !t.contains("@"), t.contains(where: \.isNumber) else { return false }
        return phoneDigits(t) != nil
    }

    /// Opens Messages with a prefilled body. Falls back to system share if SMS compose is unavailable.
    static func sendSms(phone: String?, message: String, from presenter: UIViewController? = nil) {
        let digits = phoneDigits(phone)
        if MFMessageComposeViewController.canSendText(),
           let host = presenter ?? topViewController() {
            let vc = MFMessageComposeViewController()
            vc.body = message
            if let digits { vc.recipients = [digits] }
            let delegate = MessageComposeDelegate.shared
            vc.messageComposeDelegate = delegate
            host.present(vc, animated: true)
            return
        }
        var components = URLComponents(string: "sms:")
        if let digits {
            components = URLComponents(string: "sms:\(digits)")
        }
        components?.queryItems = [URLQueryItem(name: "body", value: message)]
        if let url = components?.url, UIApplication.shared.canOpenURL(url) {
            UIApplication.shared.open(url)
            return
        }
        presentSystemShare(items: [message], from: presenter)
    }

    /// Opens WhatsApp with a prefilled invite; falls back to system share if WhatsApp is missing.
    static func sendWhatsApp(phone: String?, message: String, from presenter: UIViewController? = nil) {
        let encoded = message.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? message
        let digits = phoneDigits(phone)
        var candidates: [URL] = []
        if let digits {
            if let u = URL(string: "https://wa.me/\(digits)?text=\(encoded)") { candidates.append(u) }
            if let u = URL(string: "whatsapp://send?phone=\(digits)&text=\(encoded)") { candidates.append(u) }
        }
        if let u = URL(string: "https://api.whatsapp.com/send?text=\(encoded)") { candidates.append(u) }
        if let u = URL(string: "whatsapp://send?text=\(encoded)") { candidates.append(u) }

        for url in candidates where UIApplication.shared.canOpenURL(url) {
            UIApplication.shared.open(url)
            return
        }
        presentSystemShare(items: [message], from: presenter)
    }

    static func presentSystemShare(items: [Any], from presenter: UIViewController? = nil) {
        let host = presenter ?? topViewController()
        guard let host else { return }
        let vc = UIActivityViewController(activityItems: items, applicationActivities: nil)
        if let pop = vc.popoverPresentationController {
            pop.sourceView = host.view
            pop.sourceRect = CGRect(x: host.view.bounds.midX, y: host.view.bounds.midY, width: 1, height: 1)
            pop.permittedArrowDirections = []
        }
        host.present(vc, animated: true)
    }

    private static func topViewController(
        base: UIViewController? = UIApplication.shared.connectedScenes
            .compactMap { ($0 as? UIWindowScene)?.keyWindow }.first?.rootViewController
    ) -> UIViewController? {
        if let nav = base as? UINavigationController { return topViewController(base: nav.visibleViewController) }
        if let tab = base as? UITabBarController { return topViewController(base: tab.selectedViewController) }
        if let presented = base?.presentedViewController { return topViewController(base: presented) }
        return base
    }
}

private final class MessageComposeDelegate: NSObject, MFMessageComposeViewControllerDelegate {
    static let shared = MessageComposeDelegate()

    func messageComposeViewController(
        _ controller: MFMessageComposeViewController,
        didFinishWith result: MessageComposeResult
    ) {
        controller.dismiss(animated: true)
    }
}

/// Reusable Messages / WhatsApp / optional Not now row for invite surfaces.
struct InviteSendChannelButtons: View {
    let enabled: Bool
    var accent: Color = Color(hex: "#F97316")
    var showDismiss: Bool = false
    var onMessages: () -> Void
    var onWhatsApp: () -> Void
    var onDismiss: (() -> Void)? = nil

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(spacing: 10) {
                channelButton(title: "Messages", action: onMessages)
                channelButton(title: "WhatsApp", action: onWhatsApp)
            }
            if showDismiss, let onDismiss {
                Button("Not now", action: onDismiss)
                    .font(.plusJakarta(size: 12, weight: .semibold))
                    .foregroundStyle(Color(hex: "#9E9AA8"))
                    .buttonStyle(.plain)
            }
        }
        .opacity(enabled ? 1 : 0.45)
        .disabled(!enabled)
    }

    private func channelButton(title: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(title)
                .font(.plusJakarta(size: 13, weight: .bold))
                .foregroundStyle(accent)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 12)
                .overlay(RoundedRectangle(cornerRadius: 12).stroke(accent, lineWidth: 1))
        }
        .buttonStyle(.plain)
    }
}
