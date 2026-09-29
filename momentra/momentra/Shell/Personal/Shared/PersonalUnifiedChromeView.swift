import SwiftUI

/// Slim Personal chrome — Manage preferred + optional Set up Everyday (no family chips).
struct PersonalUnifiedChromeView: View {
    var accent: Color
    var showManage: Bool
    var showSetUpEveryday: Bool
    var onManage: () -> Void
    var onSetUpEveryday: () -> Void

    var body: some View {
        HStack {
            VStack(alignment: .leading, spacing: 4) {
                Text("Personal")
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundStyle(.white)
                if showSetUpEveryday {
                    Button("Set up Everyday", action: onSetUpEveryday)
                        .font(.system(size: 12, weight: .medium))
                        .foregroundStyle(accent)
                }
            }
            Spacer()
            if showManage {
                Button(action: onManage) {
                    Image(systemName: "gearshape")
                        .font(.system(size: 16, weight: .medium))
                        .foregroundStyle(.white.opacity(0.85))
                }
                .accessibilityLabel("Manage Personal")
            }
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 8)
        .background(GlobalTheme.topBarBackground)
    }
}
