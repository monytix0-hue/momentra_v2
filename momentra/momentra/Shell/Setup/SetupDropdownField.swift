import SwiftUI

struct SetupDropdownField: View {
    let label: String
    var hint: String? = nil
    let options: [String]
    @Binding var value: String
    var accent: Color = SetupTokens.brandPrimary
    var testTag: String = "setup.dropdown"

    @State private var showSheet = false

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            VStack(alignment: .leading, spacing: 4) {
                Text(label)
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundStyle(SetupTokens.textPrimary)
                if let hint {
                    Text(hint)
                        .font(.system(size: 12))
                        .foregroundStyle(SetupTokens.textSecondary)
                }
            }
            Button {
                showSheet = true
            } label: {
                HStack(spacing: 8) {
                    Text(value.isEmpty ? "Select" : value)
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundStyle(value.isEmpty ? SetupTokens.textSecondary : .white)
                    Spacer()
                    Text("▼")
                        .font(.system(size: 12))
                        .foregroundStyle(SetupTokens.textSecondary)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.horizontal, 14)
                .padding(.vertical, 12)
                .background(SetupTokens.bizCard, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
                .overlay(
                    RoundedRectangle(cornerRadius: 12, style: .continuous)
                        .stroke(Color(hex: "#1E293B"), lineWidth: 1)
                )
            }
            .buttonStyle(.plain)
            .accessibilityIdentifier("\(testTag).trigger")
        }
        .padding(.vertical, 6)
        .accessibilityIdentifier(testTag)
        .sheet(isPresented: $showSheet) {
            NavigationStack {
                List(options, id: \.self) { option in
                    Button {
                        value = option
                        showSheet = false
                    } label: {
                        HStack {
                            Text(option)
                                .foregroundStyle(option == value ? accent : .white)
                            Spacer()
                            if option == value {
                                Image(systemName: "checkmark")
                                    .foregroundStyle(accent)
                            }
                        }
                    }
                }
                .scrollContentBackground(.hidden)
                .background(SetupTokens.bizCard)
                .navigationTitle(label)
                .navigationBarTitleDisplayMode(.inline)
                .toolbar {
                    ToolbarItem(placement: .cancellationAction) {
                        Button("Close") { showSheet = false }
                    }
                }
            }
            .presentationDetents([.medium, .large])
            .accessibilityIdentifier("\(testTag).menu")
        }
    }
}
