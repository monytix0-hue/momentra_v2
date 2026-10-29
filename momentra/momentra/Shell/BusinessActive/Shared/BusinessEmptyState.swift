import SwiftUI

/// Shared Business empty state, matching the Group pattern: a title that names the surface,
/// one muted line that says what will appear here, and an optional next action.
/// Card-free — it sits on the sheet/tab background.
///
/// Replaces the 70 inline "Nothing recorded yet." / "Nothing is waiting." strings that were
/// previously bare `Text` calls inside per-module cards.
struct BusinessEmptyState: View {
    let title: String
    let message: String
    var systemIcon: String? = nil
    var accent: Color = Color(hex: "#818CF8")
    var glyph: String? = nil
    var actionLabel: String? = nil
    var onAction: (() -> Void)? = nil
    var compact: Bool = false

    private var tileSize: CGFloat { compact ? 40 : 56 }
    private var tileRadius: CGFloat { compact ? 10 : 14 }

    var body: some View {
        VStack(spacing: compact ? 6 : 10) {
            if let systemIcon {
                tile {
                    Image(systemName: systemIcon)
                        .font(.system(size: compact ? 16 : 22, weight: .semibold))
                        .foregroundStyle(accent)
                }
            } else if let glyph {
                tile {
                    // Emoji glyphs have no Plus Jakarta face — system size keeps them crisp.
                    Text(glyph).font(.system(size: compact ? 16 : 22))
                }
            }
            Text(title)
                .font(.plusJakarta(size: compact ? 13 : 15, weight: .semibold))
                .foregroundStyle(BusinessSheetTokens.text)
                .multilineTextAlignment(.center)
            Text(message)
                .font(.plusJakarta(size: compact ? 12 : 13))
                .foregroundStyle(BusinessSheetTokens.muted)
                .multilineTextAlignment(.center)
                .frame(maxWidth: 320)
            if let actionLabel, let onAction {
                Button(action: onAction) {
                    Text(actionLabel)
                        .font(.plusJakarta(size: 13, weight: .semibold))
                        .foregroundStyle(accent)
                        .padding(.horizontal, 14)
                        .padding(.vertical, 9)
                        .background(accent.opacity(0.14), in: RoundedRectangle(cornerRadius: 12))
                        .overlay(
                            RoundedRectangle(cornerRadius: 12)
                                .stroke(accent.opacity(0.3), lineWidth: 1)
                        )
                }
                .buttonStyle(.plain)
            }
        }
        .padding(.vertical, compact ? 12 : 24)
        .frame(maxWidth: .infinity)
    }

    @ViewBuilder
    private func tile<Content: View>(@ViewBuilder _ content: () -> Content) -> some View {
        content()
            .frame(width: tileSize, height: tileSize)
            .background(accent.opacity(0.12), in: RoundedRectangle(cornerRadius: tileRadius))
            .overlay(
                RoundedRectangle(cornerRadius: tileRadius)
                    .stroke(accent.opacity(0.22), lineWidth: 1)
            )
    }
}
