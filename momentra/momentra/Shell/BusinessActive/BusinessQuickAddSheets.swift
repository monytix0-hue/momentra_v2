import SwiftUI

/// Business Quick Add sheets — theme accent; GAP kinds use disabled CTA.
struct BusinessGapQuickAddSheet: View {
    let theme: BusinessActiveTheme
    let kind: BusinessQuickAddKind
    var momentId: String? = nil
    var onClose: () -> Void
    var onSaved: () -> Void = {}
    var onExpense: () -> Void = {}
    var onRevenue: () -> Void = {}
    var onInvoice: () -> Void = {}

    private var accent: SheetAccent {
        SheetAccent(accent: theme.accent, accentEnd: theme.accentSolid, soft: theme.accentSoft)
    }

    var body: some View {
        ZStack(alignment: .top) {
            Color(hex: "#161B26").ignoresSafeArea()
            VStack(spacing: 0) {
                Capsule()
                    .fill(Color(hex: "#475569"))
                    .frame(width: 48, height: 4)
                    .padding(.top, 12)
                ScrollView {
                    VStack(alignment: .leading, spacing: 16) {
                        sheetBody
                    }
                    .padding(.horizontal, 20)
                    .padding(.top, 16)
                    .padding(.bottom, 28)
                }
            }
        }
    }

    @ViewBuilder
    private var sheetBody: some View {
        switch kind {
        case .expense, .spendEntry:
            redirectLive("Expense is available from the finance sheet.", action: onExpense)
        case .revenue:
            redirectLive("Revenue is available from the finance sheet.", action: onRevenue)
        case .invoice:
            redirectLive("Invoice is available from the finance sheet.", action: onInvoice)
        case .poll:
            WeddingPollBody(momentId: momentId, onDismiss: onClose, onSaved: onSaved, accent: accent)
        case .memory:
            WeddingMemoryBody(momentId: momentId, onDismiss: onClose, onSaved: onSaved, accent: accent)
        case .teamUpdate, .generalUpdate:
            WeddingUpdateBody(momentId: momentId, onDismiss: onClose, onSaved: onSaved, accent: accent)
        default:
            BusinessGapBody(theme: theme, kind: kind, accent: accent)
        }
    }

    private func redirectLive(_ message: String, action: @escaping () -> Void) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(kind.label)
                .font(.plusJakarta(size: 20, weight: .heavy))
                .foregroundStyle(theme.text)
            Text(message)
                .font(.plusJakarta(size: 13))
                .foregroundStyle(theme.secondary)
            Button {
                onClose()
                action()
            } label: {
                Text("Continue")
                    .font(.plusJakarta(size: 15, weight: .bold))
                    .foregroundStyle(.white)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 14)
                    .background(theme.accent)
                    .clipShape(RoundedRectangle(cornerRadius: 14))
            }
            .buttonStyle(.plain)
        }
    }
}

private struct BusinessGapBody: View {
    let theme: BusinessActiveTheme
    let kind: BusinessQuickAddKind
    var accent: SheetAccent

    @State private var title = ""
    @State private var notes = ""

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack(spacing: 10) {
                Text(kind.emoji).font(.system(size: 28))
                VStack(alignment: .leading, spacing: 2) {
                    Text(kind.label)
                        .font(.plusJakarta(size: 20, weight: .heavy))
                        .foregroundStyle(theme.text)
                    Text(kind.subtitle)
                        .font(.plusJakarta(size: 13))
                        .foregroundStyle(theme.secondary)
                }
            }
            TextField("Title", text: $title)
                .font(.plusJakarta(size: 14))
                .foregroundStyle(theme.text)
                .padding(12)
                .background(Color(hex: "#0C0F15"))
                .clipShape(RoundedRectangle(cornerRadius: 12))
            TextField("Notes", text: $notes, axis: .vertical)
                .font(.plusJakarta(size: 14))
                .foregroundStyle(theme.text)
                .frame(minHeight: 80, alignment: .top)
                .padding(12)
                .background(Color(hex: "#0C0F15"))
                .clipShape(RoundedRectangle(cornerRadius: 12))
            Text("API not wired for this Action Center command yet — Coming soon.")
                .font(.plusJakarta(size: 12))
                .foregroundStyle(theme.muted)
            Text("Coming soon")
                .font(.plusJakarta(size: 15, weight: .bold))
                .foregroundStyle(Color.white.opacity(0.55))
                .frame(maxWidth: .infinity)
                .padding(.vertical, 14)
                .background(theme.accent.opacity(0.35))
                .clipShape(RoundedRectangle(cornerRadius: 14))
        }
    }
}
