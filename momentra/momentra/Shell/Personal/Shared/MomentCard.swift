import SwiftUI

/// Shared Moment card for Pulse Recent and Moments stream.
struct MomentCard: View {
    let model: MomentCardModel
    var accent: Color = Color(hex: "#7C5CFC")
    var onTap: (() -> Void)? = nil

    var body: some View {
        Button {
            onTap?()
        } label: {
            HStack(alignment: .top, spacing: 10) {
                Image(systemName: model.sourceIconKind.systemImage)
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundStyle(accent)
                    .frame(width: 32, height: 32)
                    .background(accent.opacity(0.12))
                    .clipShape(RoundedRectangle(cornerRadius: 10))
                VStack(alignment: .leading, spacing: 3) {
                    HStack(alignment: .firstTextBaseline, spacing: 8) {
                        Text(model.title)
                            .font(.plusJakarta(size: 13, weight: .semibold))
                            .foregroundStyle(Color(hex: "#E5E0EE"))
                            .lineLimit(1)
                        Spacer(minLength: 4)
                        if let amount = model.amountLabel {
                            Text(amount)
                                .font(.plusJakarta(size: 12, weight: .bold))
                                .foregroundStyle(Color(hex: "#E5E0EE"))
                        }
                    }
                    HStack(spacing: 6) {
                        if let context = model.contextLabel {
                            Text(context)
                                .font(.plusJakarta(size: 10, weight: .semibold))
                                .foregroundStyle(Color(hex: "#C9C4D8"))
                                .padding(.horizontal, 7)
                                .padding(.vertical, 2)
                                .background(Color.white.opacity(0.08))
                                .clipShape(Capsule())
                        }
                        if let subtitle = model.subtitle {
                            Text(subtitle)
                                .font(.plusJakarta(size: 11))
                                .foregroundStyle(Color(hex: "#C9C4D8"))
                                .lineLimit(1)
                        }
                        if let signal = model.signalLabel {
                            Text(signal)
                                .font(.plusJakarta(size: 10, weight: .semibold))
                                .foregroundStyle(accent)
                        }
                        Spacer(minLength: 4)
                        Text(MomentCardTime.label(for: model.occurredAt))
                            .font(.plusJakarta(size: 10))
                            .foregroundStyle(Color(hex: "#C9C4D8"))
                    }
                }
            }
            .padding(.vertical, 4)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .disabled(onTap == nil)
    }
}
