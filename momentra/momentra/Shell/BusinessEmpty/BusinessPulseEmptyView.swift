import SwiftUI

/// Figma: pulse-empty-b (657:9980)
struct BusinessPulseEmptyView: View {
    var onStartCta: () -> Void

    private let metrics: [(String, Double)] = [
        ("Operational Flow", 0.74),
        ("Capital Efficiency", 0.48),
        ("Velocity Index", 0.91),
    ]

    private let features: [(String, String, String)] = [
        ("business_empty_activity", "Operations Feed", "Live activity diagnostics from integrated pipelines"),
        ("business_empty_bell", "Smart Alerts", "Automated anomaly triggers protecting margins"),
        ("business_empty_trending_up", "Growth Metrics", "High-density correlation arrays for fast decisions"),
    ]

    var body: some View {
        NativeDashboardScaffold(background: BusinessSheetTheme.bg) {
            NativeListSection(insets: EdgeInsets(top: 24, leading: 24, bottom: 40, trailing: 24)) {
                VStack(spacing: 24) {
                    BusinessEmptyPill(label: "PULSE")
                    BusinessEmptyHeadline(
                        title: "Clarity in Real Time",
                        bodyText: "Monitor every operation, expense, and milestone as it happens."
                    )

                    VStack(spacing: 12) {
                        ForEach(metrics, id: \.0) { item in
                            VStack(alignment: .leading, spacing: 4) {
                                HStack {
                                    Text(item.0)
                                        .font(.system(size: 11))
                                        .foregroundStyle(BusinessEmptyTokens.textSecondary)
                                    Spacer()
                                    Text("\(Int(item.1 * 100))%")
                                        .font(.system(size: 11, design: .monospaced))
                                        .foregroundStyle(BusinessEmptyTokens.accent)
                                }
                                GeometryReader { geo in
                                    ZStack(alignment: .leading) {
                                        RoundedRectangle(cornerRadius: 4)
                                            .fill(Color.white.opacity(0.10))
                                        RoundedRectangle(cornerRadius: 4)
                                            .fill(BusinessEmptyTokens.accent)
                                            .frame(width: geo.size.width * item.1)
                                    }
                                }
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

                    BusinessEmptyCTA(label: "Begin Tracking →", action: onStartCta)
                }
            }
        }
        .background(BusinessSheetTheme.bg.ignoresSafeArea())
        .businessEmptyAppear()
    }
}
