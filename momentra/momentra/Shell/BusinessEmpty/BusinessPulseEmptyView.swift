import SwiftUI

/// Figma: pulse-empty-b (1687:20594)
struct BusinessPulseEmptyView: View {
    var onStartCta: () -> Void

    private let metrics = ["Money Spend", "Purchases tracked", "Expenses"]

    private let features: [(String, String, String)] = [
        ("business_empty_activity", "See every purchase", "Track what your business buys and how much it costs."),
        ("business_empty_bell", "Keep expenses in check", "Know when spending is higher than usual."),
        ("business_empty_trending_up", "See spending clearly", "See where your business spends the most."),
    ]

    var body: some View {
        NativeDashboardScaffold(background: BusinessSheetTheme.bg) {
            NativeListSection(insets: EdgeInsets(top: 24, leading: 24, bottom: 40, trailing: 24)) {
                VStack(spacing: 24) {
                    BusinessEmptyPill(label: "PULSE")
                    BusinessEmptyHeadline(
                        title: "See Where Your Business Money Goes",
                        bodyText: "Track purchases, expenses and spending across your business."
                    )

                    VStack(spacing: 12) {
                        ForEach(metrics, id: \.self) { label in
                            VStack(alignment: .leading, spacing: 4) {
                                HStack {
                                    Text(label)
                                        .font(.system(size: 11))
                                        .foregroundStyle(BusinessEmptyTokens.textSecondary)
                                    Spacer()
                                    Text("0")
                                        .font(.system(size: 11, design: .monospaced))
                                        .foregroundStyle(BusinessEmptyTokens.accent)
                                }
                                RoundedRectangle(cornerRadius: 4)
                                    .fill(Color.white.opacity(0.10))
                                    .frame(height: 6)
                            }
                        }
                    }
                    .padding(16)
                    .background(BusinessEmptyTokens.cardFill)
                    .overlay(
                        RoundedRectangle(cornerRadius: 16)
                            .stroke(BusinessEmptyTokens.cardStroke, lineWidth: 1)
                    )
                    .clipShape(RoundedRectangle(cornerRadius: 16))

                    VStack(spacing: 10) {
                        ForEach(features, id: \.1) { feature in
                            HStack(spacing: 12) {
                                ZStack {
                                    RoundedRectangle(cornerRadius: 16)
                                        .fill(BusinessEmptyTokens.iconWell)
                                        .frame(width: 32, height: 32)
                                    BusinessEmptyAssetIcon(name: feature.0, size: 16)
                                }
                                VStack(alignment: .leading, spacing: 2) {
                                    Text(feature.1)
                                        .font(.system(size: 13, weight: .semibold))
                                        .foregroundStyle(BusinessEmptyTokens.textPrimary)
                                    Text(feature.2)
                                        .font(.system(size: 11))
                                        .foregroundStyle(BusinessEmptyTokens.textSecondary)
                                        .lineLimit(2)
                                }
                                Spacer(minLength: 0)
                            }
                            .padding(12)
                            .background(BusinessEmptyTokens.cardFill)
                            .overlay(
                                RoundedRectangle(cornerRadius: 12)
                                    .stroke(BusinessEmptyTokens.cardStroke, lineWidth: 1)
                            )
                            .clipShape(RoundedRectangle(cornerRadius: 12))
                        }
                    }

                    BusinessEmptyCTA(label: "Start Tracking →", action: onStartCta)
                }
            }
        }
        .background(BusinessSheetTheme.bg.ignoresSafeArea())
        .businessEmptyAppear()
    }
}
