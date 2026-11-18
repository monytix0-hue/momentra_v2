import Combine
import SwiftUI

/// Shared operating toast host + overlay for all business kits (Retail, Restaurant, MTO, …).
/// Keep in lockstep with Android `OperatingToast.kt`.

@MainActor
final class OperatingToastHost: ObservableObject {
    static let shared = OperatingToastHost()

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

struct OperatingToastOverlay: View {
    @ObservedObject private var host = OperatingToastHost.shared
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