import SwiftUI

struct QuickAddDraftActions: View {
    let submitLabel: String
    var submitEnabled: Bool
    var accent: SheetAccent = purpleAccent
    var loading: Bool = false
    var footer: String? = nil
    var lightLabel: Bool = false
    var onSubmit: () -> Void
    var onSaveDraft: (() -> Void)? = nil
    var draftEnabled: Bool = false

    var body: some View {
        VStack(spacing: 8) {
            Button(action: onSubmit) {
                ZStack {
                    if loading {
                        ProgressView()
                            .progressViewStyle(CircularProgressViewStyle(tint: lightLabel ? Wq.text : Wq.ink))
                    } else {
                        Text(submitLabel)
                            .font(.plusJakarta(size: 16, weight: .bold))
                            .foregroundStyle(lightLabel ? Wq.text : Wq.ink)
                    }
                }
                .frame(maxWidth: .infinity)
                .padding(.vertical, 14)
                .background(accent.cta)
                .clipShape(RoundedRectangle(cornerRadius: 14))
            }
            .buttonStyle(.plain)
            .disabled(!submitEnabled || loading)

            if onSaveDraft != nil {
                Button(action: { onSaveDraft?() }) {
                    Text("Save draft")
                        .font(.plusJakarta(size: 14, weight: .semibold))
                        .foregroundStyle(draftEnabled && !loading ? Wq.text : Wq.muted)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 12)
                        .overlay(
                            RoundedRectangle(cornerRadius: 14)
                                .stroke(Wq.border, lineWidth: 1)
                        )
                }
                .buttonStyle(.plain)
                .disabled(!draftEnabled || loading)
            }

            if let footer {
                Text(footer)
                    .font(.plusJakarta(size: 12))
                    .foregroundStyle(Wq.handle)
            }
        }
    }
}
