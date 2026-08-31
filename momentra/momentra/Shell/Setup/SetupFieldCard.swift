import SwiftUI

struct SetupFieldCard: View {
    let label: String
    let value: String
    var icon: String? = nil

    var body: some View {
        HStack(spacing: 10) {
            if let icon {
                Text(icon).font(.system(size: 16))
            }
            VStack(alignment: .leading, spacing: 2) {
                Text(label)
                    .font(.system(size: 11))
                    .foregroundStyle(SetupTokens.textSecondary)
                Text(value.isEmpty ? "—" : value)
                    .font(.system(size: 14))
                    .foregroundStyle(SetupTokens.textPrimary)
            }
            Spacer(minLength: 0)
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 14)
        .background(SetupTokens.surfaceCard, in: RoundedRectangle(cornerRadius: 12))
        .overlay(RoundedRectangle(cornerRadius: 12).stroke(SetupTokens.borderSubtle, lineWidth: 1))
    }
}

struct SetupEditableFieldCard: View {
    let label: String
    @Binding var text: String
    var icon: String? = nil

    var body: some View {
        HStack(spacing: 10) {
            if let icon {
                Text(icon).font(.system(size: 16))
            }
            VStack(alignment: .leading, spacing: 2) {
                Text(label)
                    .font(.system(size: 11))
                    .foregroundStyle(SetupTokens.textSecondary)
                TextField("", text: $text)
                    .font(.system(size: 14, weight: .medium))
                    .foregroundStyle(SetupTokens.textPrimary)
            }
            Spacer(minLength: 0)
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 14)
        .background(SetupTokens.surfaceCard, in: RoundedRectangle(cornerRadius: 12))
        .overlay(RoundedRectangle(cornerRadius: 12).stroke(SetupTokens.borderSubtle, lineWidth: 1))
    }
}
