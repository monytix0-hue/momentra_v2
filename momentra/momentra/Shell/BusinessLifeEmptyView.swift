import SwiftUI

/// Figma: life-empty-b (657:10151)
struct BusinessLifeEmptyView: View {
    var onStartCta: () -> Void

    private let stats = ["FTE Nodes", "Burn Rate", "Unit Margin"]

    var body: some View {
        NativeDashboardScaffold(background: BusinessSheetTheme.bg) {
            NativeListSection(insets: EdgeInsets(top: 24, leading: 24, bottom: 40, trailing: 24)) {
                VStack(spacing: 24) {
                    BusinessEmptyPill(label: "LIFE")
                    BusinessEmptyHeadline(
                        title: "See the Full Picture",
                        bodyText: "People, finances, operations — how every thread of your business weaves together."
                    )

                    VStack(spacing: 16) {
                        HStack(spacing: 0) {
                            nodeView(icon: "business_empty_users", title: "People")
                            Spacer(minLength: 8)
                            BusinessEmptyAssetImage(name: "business_empty_connector_h", width: 24, height: 1)
                            Spacer(minLength: 8)
                            nodeView(icon: "business_empty_dollar", title: "Finances")
                        }

                        // Figma connector_v is 16×1; rotate for the vertical gap.
                        BusinessEmptyAssetImage(name: "business_empty_connector_v", width: 16, height: 1)
                            .rotationEffect(.degrees(90))
                            .frame(width: 1, height: 16)

                        HStack(spacing: 0) {
                            nodeView(icon: "business_empty_cpu", title: "Operations")
                            Spacer(minLength: 8)
                            BusinessEmptyAssetImage(name: "business_empty_connector_h", width: 24, height: 1)
                            Spacer(minLength: 8)
                            nodeView(icon: "business_empty_activity", title: "Growth")
                        }
                    }
                    .padding(20)
                    .frame(maxWidth: .infinity)
                    .background(BusinessEmptyTokens.cardFill)
                    .overlay(
                        RoundedRectangle(cornerRadius: 24)
                            .stroke(BusinessEmptyTokens.cardStroke, lineWidth: 1)
                    )
                    .clipShape(RoundedRectangle(cornerRadius: 24))

                    HStack(spacing: 8) {
                        ForEach(stats, id: \.self) { label in
                            VStack(spacing: 4) {
                                Text("—")
                                    .font(.system(size: 15, weight: .bold, design: .monospaced))
                                    .foregroundStyle(BusinessEmptyTokens.textPrimary)
                                Text(label)
                                    .font(.system(size: 10))
                                    .foregroundStyle(BusinessEmptyTokens.textMuted)
                            }
                            .frame(maxWidth: .infinity)
                            .padding(10)
                            .background(BusinessEmptyTokens.cardFill)
                            .overlay(
                                RoundedRectangle(cornerRadius: 12)
                                    .stroke(BusinessEmptyTokens.cardStroke, lineWidth: 1)
                            )
                            .clipShape(RoundedRectangle(cornerRadius: 12))
                        }
                    }

                    BusinessEmptyCTA(label: "Start Journey →", action: onStartCta)
                }
            }
        }
        .background(BusinessSheetTheme.bg.ignoresSafeArea())
        .businessEmptyAppear()
    }

    private func nodeView(icon: String, title: String) -> some View {
        VStack(spacing: 6) {
            ZStack {
                Circle()
                    .fill(BusinessEmptyTokens.accent.opacity(0.15))
                    .overlay(Circle().stroke(BusinessEmptyTokens.accent, lineWidth: 1))
                    .frame(width: 44, height: 44)
                BusinessEmptyAssetIcon(name: icon, size: 20)
            }
            Text(title)
                .font(.system(size: 11, weight: .semibold))
                .foregroundStyle(BusinessEmptyTokens.textPrimary)
        }
    }
}
