import SwiftUI

struct PendingGroupJoin: Identifiable {
    let id: String
    var code: String { id }
}

/// Preview a group invite before the parent redeems it.
struct GroupJoinConfirmSheet: View {
    let code: String
    var onClose: () -> Void
    var onJoin: () -> Void

    @StateObject private var createModel = MomentCreateModel()
    @State private var preview: GroupInvite?
    @State private var loading = true
    @State private var submitting = false
    @State private var error: String?

    private let bg = Color(hex: "#161B26")
    private let field = Color(hex: "#252230")
    private let accent = Color(hex: "#E8621A")

    var body: some View {
        ZStack(alignment: .top) {
            bg.ignoresSafeArea()
            VStack(spacing: 0) {
                Capsule()
                    .fill(Color(hex: "#625E70"))
                    .frame(width: 48, height: 4)
                    .padding(.top, 12)
                VStack(alignment: .leading, spacing: 16) {
                    HStack {
                        Text("Join group moment")
                            .font(.plusJakarta(size: 20, weight: .heavy))
                            .foregroundStyle(.white)
                        Spacer()
                        Button("Close", action: onClose)
                            .font(.plusJakarta(size: 13, weight: .semibold))
                            .foregroundStyle(accent)
                    }
                    if loading {
                        ProgressView().tint(accent)
                    } else if let preview {
                        VStack(alignment: .leading, spacing: 8) {
                            Text(preview.title)
                                .font(.plusJakarta(size: 18, weight: .bold))
                                .foregroundStyle(.white)
                            Text(preview.momentTypeCode)
                                .font(.plusJakarta(size: 12, weight: .semibold))
                                .foregroundStyle(Color(hex: "#9E9AA8"))
                            Text("Status: \(preview.status)")
                                .font(.plusJakarta(size: 12))
                                .foregroundStyle(Color(hex: "#9E9AA8"))
                        }
                        .padding(16)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .background(field)
                        .clipShape(RoundedRectangle(cornerRadius: 14))
                    }
                    if let error {
                        Text(error)
                            .font(.plusJakarta(size: 12, weight: .semibold))
                            .foregroundStyle(Color(hex: "#F87171"))
                    }
                    Button(action: join) {
                        Text(submitting ? "Joining…" : "Join moment")
                            .font(.plusJakarta(size: 15, weight: .heavy))
                            .foregroundStyle(Color(hex: "#14121B"))
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 14)
                            .background(
                                LinearGradient(colors: [accent, Color(hex: "#FFB598")], startPoint: .leading, endPoint: .trailing),
                                in: RoundedRectangle(cornerRadius: 14)
                            )
                    }
                    .buttonStyle(.plain)
                    .disabled(submitting || loading || preview == nil)
                    .opacity(loading || preview == nil ? 0.45 : 1)
                }
                .padding(20)
                Spacer()
            }
        }
        .task(id: code) { await loadPreview() }
    }

    private func loadPreview() async {
        loading = true
        error = nil
        preview = await createModel.previewGroupInvite(code: code)
        if preview == nil {
            error = "Invite not found or no longer valid."
        }
        loading = false
    }

    private func join() {
        guard !submitting, preview != nil else { return }
        submitting = true
        onJoin()
    }
}
