import SwiftUI

/// Invite & QR — Figma `RTL-MASTER · Invite & QR` `2506:29333`.
///
/// Thin wrapper over the shared `BusinessInviteQRView` so the retail masters tab and the
/// restaurant masters tab render exactly one invite surface. Kept as a named type so the
/// retail masters view keeps reading in its own vocabulary.
struct RetailInviteSheetView: View {
    let storeName: String
    let role: String?
    let momentId: String?
    var embedded: Bool = false

    @Environment(\.dismiss) private var dismiss

    var body: some View {
        if embedded {
            BusinessInviteQRView(
                title: storeName,
                role: role,
                momentId: momentId,
                palette: .rose,
                icon: RetailTileIcons.invite,
                noun: "store",
                embedded: true
            )
        } else {
            RoseScreenBackground {
                ScrollView {
                    VStack(alignment: .leading, spacing: 12) {
                        HStack {
                            Spacer()
                            Capsule()
                                .fill(RetailRose.roseMuted)
                                .frame(width: 40, height: 4)
                            Spacer()
                        }
                        .padding(.top, 4)

                        HStack(spacing: 10) {
                            Text("Invite team · \(trimmedStore)")
                                .font(.plusJakarta(size: 20, weight: .bold))
                                .foregroundStyle(RetailRose.textPrimary)
                                .lineLimit(1)
                                .minimumScaleFactor(0.7)
                                .frame(maxWidth: .infinity, alignment: .leading)
                            Button { dismiss() } label: {
                                Text("✕")
                                    .font(.plusJakarta(size: 13, weight: .bold))
                                    .foregroundStyle(RetailRose.textBody)
                                    .frame(width: 32, height: 32)
                                    .background(RetailRose.chip)
                                    .clipShape(Circle())
                            }
                            .buttonStyle(.plain)
                            .accessibilityLabel("Close")
                        }

                        BusinessInviteQRView(
                            title: storeName,
                            role: role,
                            momentId: momentId,
                            palette: .rose,
                            icon: RetailTileIcons.invite,
                            noun: "store",
                            embedded: false
                        )
                    }
                    .padding(.horizontal, RetailRose.gutter)
                    .padding(.vertical, 12)
                }
            }
            .presentationDragIndicator(.hidden)
        }
    }

    private var trimmedStore: String {
        let name = storeName.trimmingCharacters(in: .whitespacesAndNewlines)
        return name.isEmpty ? "this store" : name
    }
}
