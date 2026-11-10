import Combine
import SwiftUI

/// MTO-TOAST · * — 13 Figma frames. Thin host API for capture success paths.
/// Keep in lockstep with Android `MtoToast.kt`.

enum MtoToastKind: String, CaseIterable {
    case setupActivated
    case saved
    case deleted
    case quoteSent
    case orderAccepted
    case invoicePosted
    case savedConfirmOrder
    case savedBack
    case savedCustomers
    case savedLines
    case savedProducts
    case paused
    case finished
}

enum MtoToastAction {
    case dismiss
    case openPulse
    case confirmOrder
    case goBack
    case openCustomers
    case openLines
    case openProducts
}

struct MtoToastSpec: Equatable {
    var kind: MtoToastKind
    var title: String
    var body: String
    var actionLabel: String
    var accent: Color
    var action: MtoToastAction = .dismiss

    static func == (lhs: MtoToastSpec, rhs: MtoToastSpec) -> Bool {
        lhs.kind == rhs.kind && lhs.title == rhs.title && lhs.body == rhs.body && lhs.actionLabel == rhs.actionLabel
    }
}

enum MtoToastCatalog {
    static let emerald = Color(hex: "#10B981")
    static let amber = Color(hex: "#EA580C")

    static func of(
        _ kind: MtoToastKind,
        entity: String = "this job",
        businessLabel: String = "Make to order"
    ) -> MtoToastSpec {
        switch kind {
        case .setupActivated:
            return MtoToastSpec(
                kind: kind,
                title: "Setup activated",
                body: "\(businessLabel) · Make to order is live · opening Pulse",
                actionLabel: "Pulse ›",
                accent: amber,
                action: .openPulse
            )
        case .saved:
            return MtoToastSpec(kind: kind, title: "Saved", body: "Changes recorded · \(entity) updated", actionLabel: "Done", accent: emerald)
        case .deleted:
            return MtoToastSpec(kind: kind, title: "Deleted", body: "\(entity) removed · nothing else changed", actionLabel: "Done", accent: emerald)
        case .quoteSent:
            return MtoToastSpec(kind: kind, title: "Quote sent", body: "\(entity) · quotation shared with customer", actionLabel: "Done", accent: emerald)
        case .orderAccepted:
            return MtoToastSpec(kind: kind, title: "Order accepted", body: "Job confirmed · production unlocked · not revenue", actionLabel: "Done", accent: emerald)
        case .invoicePosted:
            return MtoToastSpec(kind: kind, title: "Invoice posted", body: "AR recorded · not revenue until collected", actionLabel: "Done", accent: emerald)
        case .savedConfirmOrder:
            return MtoToastSpec(kind: kind, title: "Saved", body: "Changes recorded · \(entity) updated", actionLabel: "Confirm ›", accent: emerald, action: .confirmOrder)
        case .savedBack:
            return MtoToastSpec(kind: kind, title: "Saved", body: "Changes recorded · \(entity) updated", actionLabel: "Back ›", accent: emerald, action: .goBack)
        case .savedCustomers:
            return MtoToastSpec(kind: kind, title: "Saved", body: "Changes recorded · \(entity) updated", actionLabel: "Customers ›", accent: emerald, action: .openCustomers)
        case .savedLines:
            return MtoToastSpec(kind: kind, title: "Saved", body: "Changes recorded · \(entity) updated", actionLabel: "Lines ›", accent: emerald, action: .openLines)
        case .savedProducts:
            return MtoToastSpec(kind: kind, title: "Saved", body: "Changes recorded · \(entity) updated", actionLabel: "Products ›", accent: emerald, action: .openProducts)
        case .paused:
            return MtoToastSpec(kind: kind, title: "Paused", body: "Moment paused · Resume anytime from Manage", actionLabel: "Done", accent: emerald)
        case .finished:
            return MtoToastSpec(kind: kind, title: "Finished", body: "Moment finished · Memory stays available", actionLabel: "Done", accent: emerald)
        }
    }

    static func forCaptureSuccess(mode: String?, eventType: String?, entity: String) -> MtoToastSpec {
        let m = (mode ?? "").uppercased()
        let t = (eventType ?? "").uppercased()
        let kind: MtoToastKind
        switch true {
        case m == "ORDER" || m == "QUOTATION" || m == "QUOTE": kind = .quoteSent
        case m == "CONFIRM" || m == "CONFIRM_ORDER" || m == "ACCEPT": kind = .orderAccepted
        // Mode wins over eventType so REPRINT (eventType SALE) stays Saved, not Invoice posted.
        case m == "SALE" || m == "INVOICE": kind = .invoicePosted
        case m == "QUOTE_REVISE" || m == "REVISE": kind = .savedConfirmOrder
        case m == "HOLD": kind = .paused
        case m == "JOB_STATUS" && (t.contains("CLOSE") || t.contains("FINISH") || t.contains("COMPLETE")):
            kind = .finished
        default: kind = .saved
        }
        return of(kind, entity: entity)
    }

    static func forMasterCreated(kind: String?, entity: String) -> MtoToastSpec {
        let k = (kind ?? "").uppercased()
        let toast: MtoToastKind
        switch true {
        case ["PRINCIPAL", "CUSTOMER", "VENDOR", "BUYER", "SUPPLIER", "PARTY"].contains(k):
            toast = .savedCustomers
        case ["LOCATION", "LINE", "STORE", "STATION", "FLOOR"].contains(k):
            toast = .savedLines
        case ["SKU", "PRODUCT", "PART", "BOM", "MATERIAL", "CATALOGUE", "CATALOG"].contains(k):
            toast = .savedProducts
        default:
            toast = .saved
        }
        return of(toast, entity: entity)
    }
}

@MainActor
final class MtoToastHost: ObservableObject {
    static let shared = MtoToastHost()

    @Published var current: MtoToastSpec?
    var onAction: ((MtoToastAction) -> Void)?

    func show(_ spec: MtoToastSpec) {
        current = spec
    }

    func show(_ kind: MtoToastKind, entity: String = "this job", businessLabel: String = "Make to order") {
        show(MtoToastCatalog.of(kind, entity: entity, businessLabel: businessLabel))
    }

    func dismiss() {
        current = nil
    }
}

struct MtoToastOverlay: View {
    @ObservedObject private var host = MtoToastHost.shared
    var autoDismissMs: Double = 4.2

    var body: some View {
        VStack {
            if let spec = host.current {
                MtoToastPanel(spec: spec) {
                    let action = spec.action
                    host.dismiss()
                    if action != .dismiss {
                        host.onAction?(action)
                    }
                }
                .padding(.horizontal, 16)
                .padding(.top, 12)
                .transition(.move(edge: .top).combined(with: .opacity))
                .task(id: spec.title + spec.body) {
                    try? await Task.sleep(nanoseconds: UInt64(autoDismissMs * 1_000_000_000))
                    if host.current == spec { host.dismiss() }
                }
            }
            Spacer()
        }
        .animation(.easeInOut(duration: 0.22), value: host.current?.title)
        .allowsHitTesting(host.current != nil)
    }
}

struct MtoToastPanel: View {
    let spec: MtoToastSpec
    var onAction: () -> Void

    var body: some View {
        VStack(spacing: 6) {
            HStack(spacing: 12) {
                ZStack {
                    RoundedRectangle(cornerRadius: 16, style: .continuous)
                        .fill(spec.accent.opacity(0.15))
                        .frame(width: 32, height: 32)
                    Text("✓")
                        .font(.system(size: 15, weight: .bold))
                        .foregroundStyle(spec.accent)
                }
                VStack(alignment: .leading, spacing: 2) {
                    Text(spec.title)
                        .font(.system(size: 15, weight: .bold))
                        .foregroundStyle(Color(hex: "#F8F9FD"))
                    Text(spec.body)
                        .font(.system(size: 12))
                        .foregroundStyle(Color(hex: "#9496A8"))
                }
                Spacer(minLength: 0)
                Button(action: onAction) {
                    Text(spec.actionLabel)
                        .font(.system(size: 12, weight: .semibold))
                        .foregroundStyle(spec.accent)
                        .padding(.horizontal, 10)
                        .padding(.vertical, 6)
                        .background(Color(hex: "#1C1F28"))
                        .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                }
                .buttonStyle(.plain)
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 12)
            .background(Color(hex: "#16181F"))
            .overlay(
                RoundedRectangle(cornerRadius: 16, style: .continuous)
                    .stroke(Color.white.opacity(0.08), lineWidth: 1)
            )
            .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
            .shadow(color: spec.accent.opacity(0.12), radius: 12, y: 4)

            Capsule()
                .fill(spec.accent.opacity(0.6))
                .frame(width: 326, height: 2)
        }
    }
}
